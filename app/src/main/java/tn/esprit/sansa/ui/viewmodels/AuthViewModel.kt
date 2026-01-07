package tn.esprit.sansa.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import tn.esprit.sansa.data.repositories.AuthResult
import tn.esprit.sansa.data.repositories.FirebaseAuthRepository
import tn.esprit.sansa.ui.screens.models.UserAccount
import tn.esprit.sansa.ui.screens.models.UserRole

class AuthViewModel(
    private val repository: FirebaseAuthRepository = FirebaseAuthRepository()
) : ViewModel() {

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private val _currentUser = MutableStateFlow<UserAccount?>(null)
    val currentUser: StateFlow<UserAccount?> = _currentUser.asStateFlow()

    private var sessionJob: Job? = null

    private val _technicians = MutableStateFlow<List<UserAccount>>(emptyList())
    val technicians: StateFlow<List<UserAccount>> = _technicians.asStateFlow()

    private val _invitations = MutableStateFlow<List<UserAccount>>(emptyList())
    val invitations: StateFlow<List<UserAccount>> = _invitations.asStateFlow()

    init {
        checkSession()
        observeData()
    }

    private fun observeData() {
        viewModelScope.launch {
            repository.getTechniciansFlow().collect {
                _technicians.value = it
            }
        }
        viewModelScope.launch {
            repository.getInvitationsFlow().collect {
                _invitations.value = it
            }
        }
    }

    private fun checkSession() {
        val firebaseUser = repository.getCurrentUser()
        if (firebaseUser != null) {
            sessionJob?.cancel()
            sessionJob = viewModelScope.launch {
                val account = repository.getUserAccount(firebaseUser.uid)
                if (account != null) {
                    _currentUser.value = account
                    _authState.value = AuthState.Authenticated(account)
                } else {
                    _authState.value = AuthState.Unauthenticated
                }
            }
        } else {
            _authState.value = AuthState.Unauthenticated
        }
    }

    fun fetchData() {
        fetchTechnicians()
        fetchInvitations()
    }

    fun fetchTechnicians() {
        viewModelScope.launch {
            _technicians.value = repository.getTechnicians()
        }
    }

    fun fetchInvitations() {
        viewModelScope.launch {
            _invitations.value = repository.getInvitations()
        }
    }

    fun loginWithEmail(email: String, password: String) {
        _authState.value = AuthState.Loading
        
        viewModelScope.launch {
            // Admin shortcuts
            if ((email.trim().lowercase() == "admin" || email.trim().lowercase() == "admin@admin.com") && password == "admin") {
                kotlinx.coroutines.delay(500) // Small delay for UI to register Loading state
                val adminUser = UserAccount(
                    uid = "ADMIN_BACKDOOR",
                    name = "Administrateur Système",
                    email = "admin@noorcity.tn",
                    role = tn.esprit.sansa.ui.screens.models.UserRole.ADMIN
                )
                _currentUser.value = adminUser
                _authState.value = AuthState.Authenticated(adminUser)
                return@launch
            }

            if (email.trim().lowercase() == "tech" && password == "tech") {
                kotlinx.coroutines.delay(500)
                val techUser = UserAccount(
                    uid = "TECH001",
                    name = "Ahmed Ben Salem",
                    email = "ahmed.bensalem@sansa.tn",
                    role = tn.esprit.sansa.ui.screens.models.UserRole.TECHNICIAN,
                    specialty = "Électricité",
                    workingZone = "Zone A",
                    isVerified = true,
                    isFirstLogin = false
                )
                _currentUser.value = techUser
                _authState.value = AuthState.Authenticated(techUser)
                return@launch
            }

            when (val result = repository.signIn(email, password)) {
                is AuthResult.Success -> {
                    _currentUser.value = result.user
                    _authState.value = AuthState.Authenticated(result.user)
                }
                is AuthResult.Failure -> {
                    // Sign out to clear any partial Firebase session
                    repository.signOut()
                    _authState.value = AuthState.Error(result.message)
                }
            }
        }
    }

    fun loginWithGoogle(idToken: String) {
        _authState.value = AuthState.Loading
        viewModelScope.launch {
            when (val result = repository.signInWithGoogle(idToken)) {
                is AuthResult.Success -> {
                    _currentUser.value = result.user
                    _authState.value = AuthState.Authenticated(result.user)
                }
                is AuthResult.Failure -> {
                    _authState.value = AuthState.Error(result.message)
                }
            }
        }
    }

    fun loginWithFacebook(accessToken: String) {
        _authState.value = AuthState.Loading
        viewModelScope.launch {
            when (val result = repository.signInWithFacebook(accessToken)) {
                is AuthResult.Success -> {
                    _currentUser.value = result.user
                    _authState.value = AuthState.Authenticated(result.user)
                }
                is AuthResult.Failure -> {
                    _authState.value = AuthState.Error(result.message)
                }
            }
        }
    }


    fun registerCitizen(name: String, email: String, password: String) {
        _authState.value = AuthState.Loading
        viewModelScope.launch {
            when (val result = repository.signUpCitizen(name, email, password)) {
                is AuthResult.Success -> {
                    _currentUser.value = result.user
                    _authState.value = AuthState.Authenticated(result.user)
                }
                is AuthResult.Failure -> {
                    _authState.value = AuthState.Error(result.message)
                }
            }
        }
    }

    fun logout() {
        sessionJob?.cancel()
        repository.signOut()
        _currentUser.value = null
        _authState.value = AuthState.Unauthenticated
    }

    fun clearError() {
        if (_authState.value is AuthState.Error) {
            _authState.value = AuthState.Idle
        }
    }

    fun inviteTechnician(name: String, email: String, specialty: String) {
        _authState.value = AuthState.Loading
        viewModelScope.launch {
            when (val result = repository.inviteTechnician(name.trim(), email.trim(), specialty)) {
                is AuthResult.Success -> {
                    _authState.value = AuthState.Idle 
                    fetchData() // Refresh both lists
                }
                is AuthResult.Failure -> {
                    _authState.value = AuthState.Error(result.message)
                }
            }
        }
    }

    fun activateTechnicianAccount() {
        val user = _currentUser.value ?: return
        _authState.value = AuthState.Loading
        viewModelScope.launch {
            when (val result = repository.activateTechnician(user.uid)) {
                is AuthResult.Success -> {
                    _currentUser.value = result.user
                    _authState.value = AuthState.Authenticated(result.user)
                }
                is AuthResult.Failure -> {
                    _authState.value = AuthState.Error(result.message)
                }
            }
        }
    }

    fun deleteAccount() {
        _authState.value = AuthState.Loading
        viewModelScope.launch {
            if (repository.deleteAccount()) {
                logout()
            } else {
                _authState.value = AuthState.Error("Échec de la suppression du compte.")
            }
        }
    }

    fun updateUserProfile(updates: Map<String, Any>) {
        _authState.value = AuthState.Loading
        viewModelScope.launch {
            when (val result = repository.updateUserProfile(updates)) {
                is AuthResult.Success -> {
                    _currentUser.value = result.user
                    _authState.value = AuthState.Authenticated(result.user)
                }
                is AuthResult.Failure -> {
                    _authState.value = AuthState.Error(result.message)
                }
            }
        }
    }

    fun updatePassword(current: String, new: String, onSuccess: () -> Unit) {
        _authState.value = AuthState.Loading
        viewModelScope.launch {
            if (repository.reauthenticate(current)) {
                when (val result = repository.updatePassword(new)) {
                    is AuthResult.Success -> {
                        _currentUser.value = result.user
                        _authState.value = AuthState.Authenticated(result.user)
                        onSuccess()
                    }
                    is AuthResult.Failure -> {
                        _authState.value = AuthState.Error(result.message)
                    }
                }
            } else {
                _authState.value = AuthState.Error("Mot de passe actuel incorrect.")
            }
        }
    }

    fun completeOnboarding(phone: String, zone: String) {
        val user = _currentUser.value ?: return
        _authState.value = AuthState.Loading
        viewModelScope.launch {
            // First update the profile details
            val updates = mapOf(
                "phone" to phone,
                "workingZone" to zone
            )
            repository.updateUserProfile(updates)
            
            // Then activate the account (which sets isFirstLogin to false and moves to /technicians)
            when (val result = repository.activateTechnician(user.uid)) {
                is AuthResult.Success -> {
                    _currentUser.value = result.user
                    _authState.value = AuthState.Authenticated(result.user)
                }
                is AuthResult.Failure -> {
                    _authState.value = AuthState.Error(result.message)
                }
            }
        }
    }

    val predefinedQuestions = listOf(
        "Quel était le nom de votre premier animal de compagnie ?",
        "Quelle est la ville de naissance de votre mère ?",
        "Comment s'appelait votre école primaire ?",
        "Quelle est votre couleur préférée ?",
        "Quel est le nom de votre meilleur ami d'enfance ?",
        "Quel était votre premier numéro de téléphone ?",
        "Dans quelle ville vos parents se sont-ils rencontrés ?",
        "Quel est le nom de la rue où vous avez grandi ?",
        "Quelle est la marque de votre première voiture ?",
        "Quel est votre plat préféré ?"
    )

    fun updateSecurityQuestions(questions: List<tn.esprit.sansa.ui.screens.models.SecurityQuestion>, onSuccess: () -> Unit) {
        val user = _currentUser.value ?: return
        _authState.value = AuthState.Loading
        viewModelScope.launch {
            val updates = mapOf("securityQuestions" to questions)
            when (val result = repository.updateUserProfile(updates)) {
                is AuthResult.Success -> {
                    _currentUser.value = result.user
                    _authState.value = AuthState.Authenticated(result.user)
                    onSuccess()
                }
                is AuthResult.Failure -> {
                    _authState.value = AuthState.Error(result.message)
                }
            }
        }
    }

    private val _recoveryUser = MutableStateFlow<UserAccount?>(null)
    val recoveryUser: StateFlow<UserAccount?> = _recoveryUser.asStateFlow()

    fun findUserForRecovery(email: String) {
        _authState.value = AuthState.Loading
        viewModelScope.launch {
            val user = repository.getUserByEmail(email)
            if (user != null) {
                _recoveryUser.value = user
                _authState.value = AuthState.Idle
            } else {
                _authState.value = AuthState.Error("Nous n'avons pas trouvé de compte associé à cet email.")
            }
        }
    }

    fun resetRecovery() {
        _recoveryUser.value = null
        _authState.value = AuthState.Idle
    }

    fun resetPasswordWithRecovery(uid: String, newPassword: String, onSuccess: () -> Unit) {
        _authState.value = AuthState.Loading
        viewModelScope.launch {
            if (repository.updatePasswordForRecovery(uid, newPassword)) {
                _authState.value = AuthState.Idle
                onSuccess()
            } else {
                _authState.value = AuthState.Error("Échec de la réinitialisation du mot de passe.")
            }
        }
    }
}

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    object Unauthenticated : AuthState()
    data class Authenticated(val user: UserAccount) : AuthState()
    data class Error(val message: String) : AuthState()
}
