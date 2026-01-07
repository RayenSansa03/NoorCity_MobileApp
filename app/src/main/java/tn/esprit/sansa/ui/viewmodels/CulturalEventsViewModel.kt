package tn.esprit.sansa.ui.viewmodels

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import tn.esprit.sansa.ui.screens.models.UserAccount
import tn.esprit.sansa.ui.screens.models.Technician
import tn.esprit.sansa.ui.screens.models.CulturalEvent
import tn.esprit.sansa.ui.screens.models.AmbianceType
import tn.esprit.sansa.ui.screens.models.EventStatus
import tn.esprit.sansa.ui.screens.models.LightingProgram
import tn.esprit.sansa.ui.screens.models.TechnicianStatus
import tn.esprit.sansa.ui.screens.models.TechnicianAssignmentStatus
import tn.esprit.sansa.ui.screens.models.mockTechnicians
import tn.esprit.sansa.ui.screens.models.ProgramStatus
import tn.esprit.sansa.ui.screens.models.TechnicianSpecialty
import java.util.Date
import androidx.lifecycle.viewModelScope

class CulturalEventsViewModel : ViewModel() {
    private val _events = MutableStateFlow<List<CulturalEvent>>(emptyList())
    val events: StateFlow<List<CulturalEvent>> = _events.asStateFlow()

    private val authRepository = tn.esprit.sansa.data.repositories.FirebaseAuthRepository()
    private val _realTechnicians = MutableStateFlow<List<Technician>>(emptyList())



    fun addEvent(event: CulturalEvent) {
        _events.update { currentList ->
            currentList + event
        }
    }

    fun updateEventStatus(event: CulturalEvent, newStatus: EventStatus) {
        _events.update { currentList ->
            currentList.map { 
                if (it.id == event.id) it.copy(status = newStatus) else it 
            }
        }
    }
    
    fun deleteEvent(event: CulturalEvent) {
        _events.update { currentList ->
            currentList.filter { it.id != event.id }
        }
    }

    // --- Lighting Program Logic ---
    private val lightingRepository = tn.esprit.sansa.data.repositories.FirebaseLightingRepository()

    // State for lighting programs
    private val _lightingPrograms = MutableStateFlow<List<LightingProgram>>(emptyList())
    val lightingPrograms: StateFlow<List<LightingProgram>> = _lightingPrograms.asStateFlow()

    init {
        // Initialize with default mock data for Events
        _events.value = listOf(
            CulturalEvent(
                "EVT001",
                "Festival Lumières",
                Date(System.currentTimeMillis() + 5 * 24 * 60 * 60 * 1000),
                listOf("Zone A", "Zone B", "Centre-ville"),
                AmbianceType.FESTIVE,
                EventStatus.UPCOMING,
                6,
                "Grand festival de musique et d'arts avec spectacles internationaux",
                5000,
                "Ministère de la Culture"
            ),
            CulturalEvent(
                "EVT002",
                "Nuit des Musées",
                Date(System.currentTimeMillis()),
                listOf("Zone historique", "Médina"),
                AmbianceType.ARTISTIC,
                EventStatus.ACTIVE,
                8,
                "Ouverture nocturne exceptionnelle des musées avec éclairage artistique",
                1200,
                "Association des Musées"
            )
        )
        
        // Fetch Real Lighting Programs
        viewModelScope.launch {
             lightingRepository.getPrograms().collect {
                 _lightingPrograms.value = it
             }
        }

        // Fetch Real Technicians
        viewModelScope.launch {
            val users = authRepository.getTechnicians()
            _realTechnicians.value = users.map { user ->
                val specialty = when(user.specialty?.lowercase()) {
                    "électricité", "electrical" -> TechnicianSpecialty.ELECTRICAL
                    "réseau", "network" -> TechnicianSpecialty.NETWORK
                    "maintenance" -> TechnicianSpecialty.MAINTENANCE
                    "surveillance" -> TechnicianSpecialty.SURVEILLANCE
                    else -> TechnicianSpecialty.TECHNICAL_SUPPORT
                }
                Technician(
                    id = user.uid,
                    name = user.name,
                    email = user.email,
                    specialty = specialty,
                    status = if (user.isVerified) TechnicianStatus.AVAILABLE else TechnicianStatus.OFFLINE,
                    phone = user.phoneNumber,
                    interventionsCount = 0,
                    successRate = 100f,
                    joinDate = "Récent",
                    lastActivity = "En ligne",
                    zoneIds = if (user.workingZone.isNotBlank()) listOf(user.workingZone) else emptyList()
                )
            }
        }
    }

    // Mock Streetlights (In real app, fetch from Repo)
    val availableStreetlights = listOf("LAMP-001", "LAMP-002", "LAMP-003", "LAMP-A10", "LAMP-B20", "LAMP-C30")

    fun getAvailableTechnicians(zoneName: String, date: Date): List<Technician> {
        val allTechs = _realTechnicians.value.ifEmpty { mockTechnicians }
        return allTechs.filter { tech ->
            // 1. Check Zone Coverage (Optional)
            val matchesZone = zoneName.isBlank() || tech.zoneIds.any { it.equals(zoneName, ignoreCase = true) }
            
            // 2. Check Availability
            val isBusy = _lightingPrograms.value.any { program -> 
                program.technicianId == tech.id && 
                (program.technicianStatus == TechnicianAssignmentStatus.WAITING || 
                 program.technicianStatus == TechnicianAssignmentStatus.ACCEPTED)
            }

            matchesZone && !isBusy && tech.status != TechnicianStatus.OFFLINE
        }
    }

    fun getAllTechnicians(): List<Technician> {
        return _realTechnicians.value.ifEmpty { mockTechnicians }
    }

    fun confirmLightingProgram(programId: String) {
        val program = _lightingPrograms.value.find { it.id == programId } ?: return
        val updatedProgram = program.copy(
            technicianStatus = TechnicianAssignmentStatus.ACCEPTED, 
            status = ProgramStatus.ACTIVE
        )
        // Update in DB
        viewModelScope.launch {
            lightingRepository.addProgram(updatedProgram) { _, _ -> }
        }
    }

    private fun isSameDay(date1: Date, date2: Date): Boolean {
        // Simple helper
        val fmt = java.text.SimpleDateFormat("yyyyMMdd", java.util.Locale.getDefault())
        return fmt.format(date1) == fmt.format(date2)
    }
}
