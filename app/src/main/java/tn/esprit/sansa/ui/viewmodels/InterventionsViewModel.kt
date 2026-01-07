package tn.esprit.sansa.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import tn.esprit.sansa.data.repositories.FirebaseInterventionsRepository
import tn.esprit.sansa.ui.screens.models.Intervention

class InterventionsViewModel : ViewModel() {
    private val repository = FirebaseInterventionsRepository()

    private val _interventions = MutableStateFlow<List<Intervention>>(emptyList())
    val interventions: StateFlow<List<Intervention>> = _interventions.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        loadInterventions()
    }

    private fun loadInterventions() {
        viewModelScope.launch {
            repository.getInterventions().collect { list ->
                _interventions.value = list
                _isLoading.value = false
            }
        }
    }

    fun getInterventionById(id: String): Intervention? {
        return _interventions.value.find { it.id == id }
    }

    fun addIntervention(intervention: Intervention, onComplete: (Boolean) -> Unit) {
        repository.addIntervention(intervention, onComplete)
    }

    fun updateIntervention(intervention: Intervention, onComplete: (Boolean) -> Unit) {
        repository.addIntervention(intervention, onComplete)
    }

    fun deleteIntervention(id: String) {
        repository.deleteIntervention(id) { success ->
            // Optionally handle error
        }
    }

    fun refresh() {
        _isLoading.value = true
        loadInterventions()
    }
}
