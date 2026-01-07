package tn.esprit.sansa.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import tn.esprit.sansa.data.repositories.FirebaseZonesRepository
import tn.esprit.sansa.data.repositories.GeocodingResult
import tn.esprit.sansa.ui.screens.models.*
import tn.esprit.sansa.data.services.WeatherService

class ZonesViewModel : ViewModel() {
    private val repository = FirebaseZonesRepository()
    private val weatherService = WeatherService()

    private val _zones = MutableStateFlow<List<Zone>>(emptyList())
    val zones: StateFlow<List<Zone>> = _zones

    private val _zoneWeather = MutableStateFlow<Map<String, ZoneWeatherInfo>>(emptyList<Pair<String, ZoneWeatherInfo>>().toMap())
    val zoneWeather: StateFlow<Map<String, ZoneWeatherInfo>> = _zoneWeather

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _geocodingResults = MutableStateFlow<List<GeocodingResult>>(emptyList())
    val geocodingResults: StateFlow<List<GeocodingResult>> = _geocodingResults

    init {
        loadZones()
    }

    fun loadZones() {
        viewModelScope.launch {
            _isLoading.value = true
            repository.getZones().collect { zones ->
                _zones.value = zones
                _isLoading.value = false
                fetchWeatherForZones(zones)
            }
        }
    }

    private fun fetchWeatherForZones(zones: List<Zone>) {
        viewModelScope.launch {
            zones.forEach { zone ->
                launch {
                    val weather = weatherService.getWeatherData(zone.latitude, zone.longitude)
                    val airQuality = weatherService.getAirQualityData(zone.latitude, zone.longitude)
                    val info = ZoneWeatherInfo(weather, airQuality)
                    
                    _zoneWeather.update { currentMap ->
                        currentMap + (zone.id to info)
                    }
                }
            }
        }
    }

    fun addZone(zone: Zone, onComplete: (Boolean) -> Unit) {
        repository.addZone(zone, onComplete)
    }

    fun deleteZone(id: String, onComplete: (Boolean) -> Unit) {
        repository.deleteZone(id, onComplete)
    }

    fun searchLocation(query: String) {
        if (query.length < 3) {
            _geocodingResults.value = emptyList()
            return
        }
        viewModelScope.launch {
            val results = repository.searchLocation(query)
            _geocodingResults.value = results
        }
    }

    fun clearGeocodingResults() {
        _geocodingResults.value = emptyList()
    }

    fun refresh() {
        loadZones()
    }

    fun getZoneById(id: String): Zone? {
        return _zones.value.find { it.id == id }
    }

    fun updateZone(zone: Zone, onComplete: (Boolean) -> Unit) {
        repository.addZone(zone, onComplete)
    }

    /**
     * Generate next sequential zone ID (format: Z001, Z002, etc.)
     */
    suspend fun generateNextZoneId(): String {
        val existingIds = _zones.value
            .mapNotNull { it.id.removePrefix("Z").toIntOrNull() }
            .maxOrNull() ?: 0
        
        val nextNumber = existingIds + 1
        return "Z${nextNumber.toString().padStart(3, '0')}"
    }
}
