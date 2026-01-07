package tn.esprit.sansa.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import tn.esprit.sansa.data.services.BlockchainService

import tn.esprit.sansa.data.repositories.FirebaseCamerasRepository
import tn.esprit.sansa.ui.screens.models.Camera
import tn.esprit.sansa.data.models.VideoCertificate
import tn.esprit.sansa.data.models.VerificationResult
import tn.esprit.sansa.data.repositories.FirebaseInterventionsRepository
import tn.esprit.sansa.ui.screens.models.*
import tn.esprit.sansa.utils.CryptoUtils
import android.util.Log
import android.graphics.Bitmap
import tn.esprit.sansa.data.ai.TFLiteObjectDetectionHelper
import kotlinx.coroutines.delay

class CamerasViewModel : ViewModel() {
    private val repository = FirebaseCamerasRepository()
    private val blockchainService = BlockchainService()
    private val interventionsRepository = FirebaseInterventionsRepository()

    private val _cameras = MutableStateFlow<List<Camera>>(emptyList())
    val cameras: StateFlow<List<Camera>> = _cameras.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // Etat pour le dialog de détails du certificat
    private val _certificateDetails = MutableStateFlow<Pair<VideoCertificate, VerificationResult>?>(null)
    val certificateDetails: StateFlow<Pair<VideoCertificate, VerificationResult>?> = _certificateDetails.asStateFlow()

    init {
        fetchCameras()
        viewModelScope.launch {
            try {
                blockchainService.initializeBlockchain()
                Log.d("BLOCKCHAIN_TEST", "✅ Blockchain initialisée")
            } catch (e: Exception) {
                Log.e("BLOCKCHAIN_TEST", "❌ Erreur init blockchain: ${e.message}")
            }
        }
    }

    fun refresh() {
        fetchCameras()
    }

    fun loadCertificateDetails(cameraId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val cert = blockchainService.getLastCertificate(cameraId)
                if (cert != null) {
                    val verification = blockchainService.verifyCertificate(cert.id)
                    _certificateDetails.value = Pair(cert, verification)
                } else {
                    _certificateDetails.value = null
                }
            } catch (e: Exception) {
                Log.e("CamerasViewModel", "Error loading certificate details", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearCertificateDetails() {
        _certificateDetails.value = null
    }

    private fun fetchCameras() {
        viewModelScope.launch {
            _isLoading.value = true
            repository.getCameras().collect { cameraList ->
                _cameras.value = cameraList
                _isLoading.value = false
            }
        }
    }

    fun addCamera(camera: Camera, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            repository.addCamera(camera, onComplete)
        }
    }

    fun updateCameraStatus(camera: Camera, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            repository.addCamera(camera, onComplete)
        }
    }

    fun deleteCamera(id: String) {
        viewModelScope.launch {
            repository.deleteCamera(id)
        }
    }

    fun getCameraById(id: String): Camera? {
        return _cameras.value.find { it.id == id }
    }

    // ✅ Production Blockchain Functions
    fun createCertificateForCamera(camera: Camera) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                Log.d("CamerasViewModel", "🔄 Creating certificate for ${camera.id}...")
                
                // Simuler un hash video (En production, ceci viendrait de l'ESP32)
                val videoHash = CryptoUtils.calculateSHA256("${camera.id}_${System.currentTimeMillis()}")
                
                val result = blockchainService.createCertificate(
                    cameraId = camera.id,
                    cameraLocation = camera.location,
                    videoHash = videoHash,
                    metadata = mapOf(
                        "type" to camera.type.name,
                        "zone" to camera.zone,
                        "resolution" to camera.resolution
                    )
                )

                if (result.isSuccess) {
                    val cert = result.getOrNull()
                    Log.d("CamerasViewModel", "✅ Certificate created: ${cert?.id}")
                    
                    // Mettre à jour le statut de la caméra dans Firebase
                    // Note: Il faudrait ajouter une fonction dans le repository pour ça idéalement
                    repository.updateCameraStatus(camera.copy(hasCertificate = true))
                    
                    // Recharger la liste pour voir le badge
                    fetchCameras()
                    
                    // Charger les détails pour afficher le dialog direct
                    if (cert != null) {
                        val verification = blockchainService.verifyCertificate(cert.id)
                        _certificateDetails.value = Pair(cert, verification)
                    }
                } else {
                    Log.e("CamerasViewModel", "❌ Error creating certificate: ${result.exceptionOrNull()?.message}")
                }
            } catch (e: Exception) {
                Log.e("CamerasViewModel", "Error in certification process", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    // 🚨 AI Emergency Alerts Logic
    fun triggerAccident(camera: Camera) {
        viewModelScope.launch {
            try {
                // 1. Mettre à jour le statut de la caméra
                val updatedCamera = camera.copy(
                    isAccidentActive = true,
                    alertStatus = "ACCIDENT",
                    safetyScore = 15,
                    aiDescription = "COLLISION DÉTECTÉE - SECOURS REQUIS"
                )
                repository.updateCameraStatus(updatedCamera)

                // 2. Créer automatiquement une intervention d'urgence
                val emergencyIntervention = Intervention(
                    id = "", // Sera généré par push()
                    streetlightId = camera.associatedStreetlight,
                    location = camera.location,
                    description = "🚨 ALERTE IA : Accident détecté par la caméra ${camera.id}. Intervention d'urgence requise.",
                    priority = InterventionPriority.URGENT.displayName,
                    status = InterventionStatus.PENDING,
                    type = InterventionType.EMERGENCY,
                    date = System.currentTimeMillis(),
                    technicianName = "Unité d'intervention d'urgence",
                    assignedBy = "IA Noor Vision"
                )
                
                interventionsRepository.addIntervention(emergencyIntervention) { success ->
                    if (success) Log.d("CamerasViewModel", "✅ Intervention d'urgence créée")
                }

                Log.d("CamerasViewModel", "🚨 Accident simuler pour ${camera.id}")
            } catch (e: Exception) {
                Log.e("CamerasViewModel", "Error triggering accident", e)
            }
        }
    }

    fun resolveAccident(camera: Camera) {
        viewModelScope.launch {
            val resolvedCamera = camera.copy(
                isAccidentActive = false,
                alertStatus = "NORMAL",
                safetyScore = 100,
                aiDescription = "Zone sécurisée et dégagée"
            )
            repository.updateCameraStatus(resolvedCamera)
        }
    }

    fun runAiDiagnostic(camera: Camera) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                delay(2000) // Simuler le temps de calcul IA

                val reports = listOf(
                    "ANALYSE : Éclairage optimal, aucune obstruction détectée. Flux vidéo 4K stable.",
                    "ALERTE : Luminosité faible détectée sur le lampadaire ${camera.associatedStreetlight}. Maintenance préventive suggérée.",
                    "ANALYSE : Zone à haute densité piétonne détectée. Score de sécurité ajusté à 85%.",
                    "AVERTISSEMENT : Accumulation d'objets sur la chaussée. Risque potentiel d'accident."
                )

                val updatedCamera = camera.copy(
                    lastAiDiagnostic = "30/12/2025 15:30",
                    aiSafetyReport = reports.random(),
                    safetyScore = (70..100).random()
                )
                
                repository.updateCameraStatus(updatedCamera)
                _isLoading.value = false
            } catch (e: Exception) {
                Log.e("CamerasViewModel", "Error running AI diagnostic", e)
                _isLoading.value = false
            }
        }
    }

    /**
     * 🧠 Traitement réel par Intelligence Artificielle (TFLite)
     * Analyse un flux d'images et met à jour les données de la caméra.
     */
    fun processCameraFrame(camera: Camera, bitmap: Bitmap, helper: TFLiteObjectDetectionHelper) {
        viewModelScope.launch {
            try {
                val detections = helper.detect(bitmap)
                Log.d("CamerasViewModel", "🔍 Analyse TFLite: ${detections.size} objets trouvés")

                // Filtrer par label (standards mobilenet)
                val peopleCount = detections.count { it.categories.any { cat -> cat.label.equals("person", ignoreCase = true) } }
                val vehicleLabels = listOf("car", "truck", "bus", "motorcycle", "vehicle")
                val vehicleCount = detections.count { it.categories.any { cat -> cat.label.lowercase() in vehicleLabels } }
                
                if (detections.isNotEmpty()) {
                    Log.d("CamerasViewModel", "📊 Résultats: People=$peopleCount, Vehicles=$vehicleCount")
                }

                // Calcul du score de sécurité basé sur l'affluence
                val safetyScore = calculateSafetyScore(peopleCount, vehicleCount)
                
                // Mise à jour de la caméra dans Firebase
                val updatedCamera = camera.copy(
                    detectedPeopleCount = peopleCount,
                    detectedVehicleCount = vehicleCount,
                    safetyScore = safetyScore,
                    aiDescription = if (vehicleCount > 5) "Trafic dense détecté" else "Analyse IA fluide"
                )
                
                repository.updateCameraStatus(updatedCamera)
                
                // Logique simplifiée pour déclencher un accident si on voit des véhicules à très basse confiance (collision ?)
                // ou via un algorithme plus poussé de superposition
                if (vehicleCount > 0 && detections.any { it.categories.any { cat -> cat.score in 0.2f..0.4f } }) {
                     if (!camera.isAccidentActive) {
                         triggerAccident(updatedCamera)
                     }
                }

            } catch (e: Exception) {
                Log.e("CamerasViewModel", "Error processing TFLite frame", e)
            }
        }
    }

    private fun calculateSafetyScore(people: Int, vehicles: Int): Int {
        val total = people + vehicles
        return when {
            total == 0 -> 100
            total < 5 -> 95
            total < 15 -> 80
            else -> 65
        }
    }
}
