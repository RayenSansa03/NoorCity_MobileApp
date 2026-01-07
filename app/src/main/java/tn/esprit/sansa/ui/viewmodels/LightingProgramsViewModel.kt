package tn.esprit.sansa.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import tn.esprit.sansa.data.repositories.FirebaseLightingRepository
import tn.esprit.sansa.data.repositories.FirebaseStreetlightsRepository
import tn.esprit.sansa.ui.screens.models.LightingProgram
import tn.esprit.sansa.ui.screens.models.Streetlight
import com.google.firebase.database.FirebaseDatabase

class LightingProgramsViewModel : ViewModel() {
    private val lightingRepository = FirebaseLightingRepository()
    private val streetlightRepository = FirebaseStreetlightsRepository()

    private val _programs = MutableStateFlow<List<LightingProgram>>(emptyList())
    val programs: StateFlow<List<LightingProgram>> = _programs.asStateFlow()

    private val _streetlights = MutableStateFlow<List<Streetlight>>(emptyList())
    val streetlights: StateFlow<List<Streetlight>> = _streetlights.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            _isLoading.value = true
            launch {
                lightingRepository.getPrograms().collect {
                    _programs.value = it
                    _isLoading.value = false
                }
            }
            launch {
                streetlightRepository.getStreetlights().collect {
                    _streetlights.value = it
                }
            }
        }
    }

    fun getProgramById(id: String): LightingProgram? {
        return _programs.value.find { it.id == id }
    }

    fun updateProgram(program: LightingProgram, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            lightingRepository.addProgram(program) { success, _ ->
                onComplete(success)
            }
        }
    }

    fun addProgram(program: LightingProgram, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            lightingRepository.addProgram(program) { success, id ->
                if (success && program.technicianId != null && id != null) {
                    sendTechnicianNotification(program.copy(id = id))
                }
                onComplete(success)
            }
        }
    }

    private fun sendTechnicianNotification(program: LightingProgram) {
        val techId = program.technicianId ?: return
        val notifRef = FirebaseDatabase.getInstance().getReference("notifications").child(techId).push()
        
        val notification = mapOf(
            "id" to notifRef.key,
            "title" to "Nouveau Programme assigné",
            "message" to "Vous avez été assigné au programme : ${program.name}",
            "timestamp" to System.currentTimeMillis(),
            "type" to "LIGHTING_ASSIGNMENT",
            "programId" to program.id,
            "read" to false
        )
        
        notifRef.setValue(notification)
    }

    fun deleteProgram(id: String) {
        viewModelScope.launch {
            lightingRepository.deleteProgram(id) { }
        }
    }
}
