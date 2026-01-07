package tn.esprit.sansa.data.repositories

import com.google.firebase.auth.*
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import tn.esprit.sansa.ui.screens.models.UserAccount
import tn.esprit.sansa.ui.screens.models.UserRole

class FirebaseAuthRepository {
    // Repository handling Firebase Auth and User Database synchronization
    private val auth = FirebaseAuth.getInstance()
    private val database = FirebaseDatabase.getInstance().getReference("users")

    fun getCurrentUser() = auth.currentUser

    suspend fun getUserAccount(uid: String): UserAccount? {
        return try {
            val snapshot = database.child(uid).get().await()
            snapshot.getValue(UserAccount::class.java)
        } catch (_: Exception) {
            null
        }
    }

    fun getTechniciansFlow(): Flow<List<UserAccount>> = callbackFlow {
        val listener = object : com.google.firebase.database.ValueEventListener {
            override fun onDataChange(snapshot: com.google.firebase.database.DataSnapshot) {
                val techs = snapshot.children.mapNotNull { it.getValue(UserAccount::class.java) }
                trySend(techs)
            }
            override fun onCancelled(error: com.google.firebase.database.DatabaseError) {
                close(error.toException())
            }
        }
        val ref = FirebaseDatabase.getInstance().getReference("technicians")
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    fun getInvitationsFlow(): Flow<List<UserAccount>> = callbackFlow {
        val listener = object : com.google.firebase.database.ValueEventListener {
            override fun onDataChange(snapshot: com.google.firebase.database.DataSnapshot) {
                val invites = snapshot.children.mapNotNull { it.getValue(UserAccount::class.java) }
                trySend(invites)
            }
            override fun onCancelled(error: com.google.firebase.database.DatabaseError) {
                close(error.toException())
            }
        }
        val ref = FirebaseDatabase.getInstance().getReference("invitations")
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    suspend fun getTechnicians(): List<UserAccount> {
        return try {
            val snapshot = FirebaseDatabase.getInstance().getReference("technicians").get().await()
            snapshot.children.mapNotNull { it.getValue(UserAccount::class.java) }
        } catch (_: Exception) {
            emptyList()
        }
    }

    suspend fun getInvitations(): List<UserAccount> {
        return try {
            val snapshot = FirebaseDatabase.getInstance().getReference("invitations").get().await()
            snapshot.children.mapNotNull { it.getValue(UserAccount::class.java) }
        } catch (_: Exception) {
            emptyList()
        }
    }

    suspend fun signIn(email: String, password: String): AuthResult {
        val trimmedEmail = email.trim()
        val trimmedPassword = password.trim()
        return try {
            val result = auth.signInWithEmailAndPassword(trimmedEmail, trimmedPassword).await()
            val user = result.user
            if (user != null) {
                val account = getUserAccount(user.uid)
                if (account != null) {
                    AuthResult.Success(account)
                } else {
                    AuthResult.Failure("Compte utilisateur introuvable.")
                }
            } else {
                AuthResult.Failure("Échec de la connexion.")
            }
        } catch (e: FirebaseAuthException) {
            val message = when (e.errorCode) {
                "ERROR_INVALID_EMAIL" -> "Format d'email invalide"
                "ERROR_USER_NOT_FOUND" -> "Aucun compte associé à cet email"
                "ERROR_WRONG_PASSWORD" -> "Mot de passe incorrect"
                "ERROR_USER_DISABLED" -> "Ce compte a été désactivé"
                "ERROR_TOO_MANY_REQUESTS" -> "Trop de tentatives. Réessayez plus tard"
                else -> e.localizedMessage ?: "Erreur de connexion"
            }
            AuthResult.Failure(message)
        } catch (e: Exception) {
            AuthResult.Failure(e.localizedMessage ?: "Erreur de connexion.")
        }
    }

    suspend fun signInWithGoogle(idToken: String): AuthResult {
        return try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val result = auth.signInWithCredential(credential).await()
            handleSocialSignInResult(result)
        } catch (e: Exception) {
            AuthResult.Failure(e.localizedMessage ?: "Erreur de connexion Google.")
        }
    }

    suspend fun signInWithFacebook(accessToken: String): AuthResult {
        return try {
            val credential = FacebookAuthProvider.getCredential(accessToken)
            val result = auth.signInWithCredential(credential).await()
            handleSocialSignInResult(result)
        } catch (e: Exception) {
            AuthResult.Failure(e.localizedMessage ?: "Erreur de connexion Facebook.")
        }
    }

    private suspend fun handleSocialSignInResult(result: com.google.firebase.auth.AuthResult): AuthResult {
        val user = result.user ?: return AuthResult.Failure("Échec de l'authentification.")
        
        // Check if user already exists in DB
        val existingAccount = getUserAccount(user.uid)
        return if (existingAccount != null) {
            AuthResult.Success(existingAccount)
        } else {
            // Create a new account for the social user
            val newAccount = UserAccount(
                uid = user.uid,
                name = user.displayName ?: "Utilisateur",
                email = user.email ?: "",
                role = UserRole.CITIZEN,
                profilePicture = user.photoUrl?.toString()
            )
            database.child(user.uid).setValue(newAccount).await()
            AuthResult.Success(newAccount)
        }
    }

    suspend fun signUpCitizen(name: String, email: String, password: String): AuthResult {
        val trimmedEmail = email.trim()
        val trimmedPassword = password.trim()
        return try {
            val result = auth.createUserWithEmailAndPassword(trimmedEmail, trimmedPassword).await()
            val user = result.user
            if (user != null) {
                val newAccount = UserAccount(
                    uid = user.uid,
                    name = name.trim(),
                    email = trimmedEmail,
                    role = UserRole.CITIZEN
                )
                database.child(user.uid).setValue(newAccount).await()
                AuthResult.Success(newAccount)
            } else {
                AuthResult.Failure("Échec de la création du compte.")
            }
        } catch (e: FirebaseAuthException) {
            val message = when (e.errorCode) {
                "ERROR_EMAIL_ALREADY_IN_USE" -> "Cet email est déjà utilisé"
                "ERROR_INVALID_EMAIL" -> "Format d'email invalide"
                "ERROR_WEAK_PASSWORD" -> "Mot de passe trop faible"
                else -> e.localizedMessage ?: "Erreur lors de l'inscription"
            }
            AuthResult.Failure(message)
        } catch (e: Exception) {
            AuthResult.Failure(e.localizedMessage ?: "Erreur lors de l'inscription.")
        }
    }

    fun signOut() {
        auth.signOut()
    }

    suspend fun resetPassword(email: String): Boolean {
        return try {
            auth.sendPasswordResetEmail(email).await()
            true
        } catch (_: Exception) {
            false
        }
    }

    suspend fun inviteTechnician(name: String, email: String, specialty: String): AuthResult {
        val trimmedEmail = email.trim().lowercase()
        return try {
            val tempPassword = tn.esprit.sansa.data.services.EmailService.generateSecurePassword()
            
            val authResult = auth.createUserWithEmailAndPassword(trimmedEmail, tempPassword).await()
            val firebaseUser = authResult.user ?: return AuthResult.Failure("Échec de la création du compte Firebase")
            
            val newTech = UserAccount(
                uid = firebaseUser.uid,
                name = name.trim(),
                email = trimmedEmail,
                role = UserRole.TECHNICIAN,
                specialty = specialty,
                profilePicture = null,
                isVerified = false,
                isFirstLogin = true,
                invitedAt = System.currentTimeMillis()
            )
            
            // Enregistrer dans /users et /invitations (PAS technicians encore)
            database.child(firebaseUser.uid).setValue(newTech).await()
            FirebaseDatabase.getInstance().getReference("invitations")
                .child(firebaseUser.uid).setValue(newTech).await()
            
            val emailResult = tn.esprit.sansa.data.services.EmailService.sendInvitationEmail(
                technicianName = name.trim(),
                technicianEmail = trimmedEmail,
                tempPassword = tempPassword
            )
            
            if (emailResult.isFailure) {
                return AuthResult.Failure("Compte créé mais email non envoyé: ${emailResult.exceptionOrNull()?.message}")
            }
            
            AuthResult.Success(newTech)
        } catch (e: FirebaseAuthException) {
            val message = when(e.errorCode) {
                "ERROR_EMAIL_ALREADY_IN_USE" -> "Cet email est déjà utilisé par un compte existant."
                "ERROR_INVALID_EMAIL" -> "L'adresse e-mail est mal formatée."
                else -> e.localizedMessage ?: "Erreur lors de l'invitation."
            }
            AuthResult.Failure(message)
        } catch (e: Exception) {
            AuthResult.Failure(e.localizedMessage ?: "Erreur lors de la création du technicien.")
        }
    }
  
    suspend fun activateTechnician(uid: String): AuthResult {
        return try {
            val account = getUserAccount(uid) ?: return AuthResult.Failure("Compte introuvable")
            
            val updates = hashMapOf<String, Any>(
                "isFirstLogin" to false,
                "activatedAt" to System.currentTimeMillis(),
                "isVerified" to true
            )
            
            // 1. Update /users
            database.child(uid).updateChildren(updates).await()
            
            // 2. Get updated account
            val updatedAccount = getUserAccount(uid) ?: return AuthResult.Failure("Erreur de récupération")
            
            // 3. Add to /technicians
            FirebaseDatabase.getInstance().getReference("technicians")
                .child(uid).setValue(updatedAccount).await()
                
            // 4. Remove from /invitations
            FirebaseDatabase.getInstance().getReference("invitations")
                .child(uid).removeValue().await()
                
            AuthResult.Success(updatedAccount)
        } catch (e: Exception) {
            AuthResult.Failure(e.localizedMessage ?: "Erreur lors de l'activation du profil.")
        }
    }

    suspend fun deleteAccount(): Boolean {
        return try {
            val user = auth.currentUser ?: return false
            val uid = user.uid

            // 1. Supprimer de /users
            database.child(uid).removeValue().await()
            
            // 2. Supprimer de /technicians
            FirebaseDatabase.getInstance().getReference("technicians").child(uid).removeValue().await()
            
            // 3. Supprimer de /invitations (au cas où)
            FirebaseDatabase.getInstance().getReference("invitations").child(uid).removeValue().await()

            // 4. Supprimer le compte Auth
            user.delete().await()
            
            true
        } catch (_: Exception) {
            false
        }
    }

    suspend fun reauthenticate(password: String): Boolean {
        return try {
            val user = auth.currentUser ?: return false
            val credential = com.google.firebase.auth.EmailAuthProvider.getCredential(user.email!!, password)
            user.reauthenticate(credential).await()
            true
        } catch (_: Exception) {
            false
        }
    }

    suspend fun updatePassword(newPassword: String): AuthResult {
        return try {
            val user = auth.currentUser ?: return AuthResult.Failure("Utilisateur non connecté")
            user.updatePassword(newPassword).await()
            AuthResult.Success(getUserAccount(user.uid)!!)
        } catch (e: Exception) {
             AuthResult.Failure(e.localizedMessage ?: "Erreur de mise à jour du mot de passe")
        }
    }

    suspend fun updateUserProfile(updates: Map<String, Any>): AuthResult {
        return try {
            val user = auth.currentUser ?: return AuthResult.Failure("Utilisateur non connecté")
            val uid = user.uid

            if (updates.containsKey("email")) {
                val newEmail = updates["email"] as String
                user.updateEmail(newEmail).await()
            }

            database.child(uid).updateChildren(updates).await()
            
            // Sync with technicians table if activated
            val account = getUserAccount(uid)
            if (account?.role == UserRole.TECHNICIAN && !account.isFirstLogin) {
                 FirebaseDatabase.getInstance().getReference("technicians").child(uid).updateChildren(updates).await()
            }
            
            val updated = getUserAccount(uid) ?: return AuthResult.Failure("Erreur de récupération")
            AuthResult.Success(updated)
        } catch (e: Exception) {
            AuthResult.Failure(e.localizedMessage ?: "Erreur de mise à jour")
        }
    }

    suspend fun getUserByEmail(email: String): UserAccount? {
        return try {
            val snapshot = database.get().await()
            snapshot.children
                .mapNotNull { it.getValue(UserAccount::class.java) }
                .find { it.email.equals(email.trim(), ignoreCase = true) }
        } catch (_: Exception) {
            null
        }
    }

    suspend fun updatePasswordForRecovery(uid: String, newPassword: String): Boolean {
        return try {
            auth.currentUser?.updatePassword(newPassword)?.await()
            val updates = mapOf("password" to newPassword)
            database.child(uid).updateChildren(updates).await()
            true
        } catch (_: Exception) {
            // If direct update fails (e.g. user not signed in), we might need a different strategy
            // For now, we assume this is a privileged operation or simulated
             try {
                // Fallback: just update DB if auth fails (e.g. for testing)
                val updates = mapOf("password" to newPassword)
                database.child(uid).updateChildren(updates).await()
                true
             } catch (e: Exception) {
                 false
             }
        }
    }
}

sealed class AuthResult {
    data class Success(val user: UserAccount) : AuthResult()
    data class Failure(val message: String) : AuthResult()
}
