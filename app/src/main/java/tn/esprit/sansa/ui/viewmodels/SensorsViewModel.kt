package tn.esprit.sansa.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import tn.esprit.sansa.data.repositories.FirebaseSensorsRepository
import tn.esprit.sansa.ui.screens.models.Sensor

class SensorsViewModel : ViewModel() {
    private val repository = FirebaseSensorsRepository()
    
    private val _sensors = MutableStateFlow<List<Sensor>>(emptyList())
    val sensors: StateFlow<List<Sensor>> = _sensors.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        fetchSensors()
    }

    fun refresh() {
        fetchSensors()
    }

    private fun fetchSensors() {
        viewModelScope.launch {
            _isLoading.value = true
            repository.getSensors().collect { sensorList ->
                _sensors.value = sensorList
                _isLoading.value = false
            }
        }
    }

    fun getSensorById(id: String): Sensor? {
        return _sensors.value.find { it.id == id }
    }

    fun addSensor(sensor: Sensor, onComplete: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            repository.addSensor(sensor, onComplete)
        }
    }

    fun updateSensor(sensor: Sensor, onComplete: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            repository.addSensor(sensor, onComplete)
        }
    }

    fun deleteSensor(id: String) {
        viewModelScope.launch {
            repository.deleteSensor(id)
        }
    }
}
