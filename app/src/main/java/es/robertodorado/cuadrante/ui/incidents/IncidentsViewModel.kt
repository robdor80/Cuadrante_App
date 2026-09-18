package es.robertodorado.cuadrante.ui.incidents

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import es.robertodorado.cuadrante.data.local.IncidentsRepository
import es.robertodorado.cuadrante.model.DateIncident
import es.robertodorado.cuadrante.model.Incident
import es.robertodorado.cuadrante.model.IncidentType
import es.robertodorado.cuadrante.model.VacationIncident
import java.time.LocalDate
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class IncidentsUiState(
    val incidents: List<Incident> = emptyList(),
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val hasError: Boolean = false,
)

class IncidentsViewModel(
    private val repository: IncidentsRepository,
    private val idGenerator: () -> String = { UUID.randomUUID().toString() },
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(IncidentsUiState())
    val uiState: StateFlow<IncidentsUiState> = mutableUiState.asStateFlow()

    init {
        viewModelScope.launch {
            runCatching {
                repository.incidents.collect { incidents ->
                    mutableUiState.update {
                        it.copy(incidents = incidents, isLoading = false, hasError = false)
                    }
                }
            }.onFailure {
                mutableUiState.update { it.copy(isLoading = false, hasError = true) }
            }
        }
    }

    fun addDates(type: IncidentType, dates: Collection<LocalDate>) {
        require(type == IncidentType.AP || type == IncidentType.PERMISSION)
        if (dates.isEmpty()) return
        persist(
            DateIncident(
                id = idGenerator(),
                type = type,
                dates = dates.distinct().sorted(),
            ),
        )
    }

    fun addVacation(startDate: LocalDate, endDate: LocalDate) {
        persist(VacationIncident(idGenerator(), startDate, endDate))
    }

    fun delete(id: String) {
        mutableUiState.update { it.copy(isSaving = true, hasError = false) }
        viewModelScope.launch {
            runCatching { repository.delete(id) }
                .onFailure { mutableUiState.update { state -> state.copy(hasError = true) } }
            mutableUiState.update { it.copy(isSaving = false) }
        }
    }

    private fun persist(incident: Incident) {
        mutableUiState.update { it.copy(isSaving = true, hasError = false) }
        viewModelScope.launch {
            runCatching { repository.add(incident) }
                .onFailure { mutableUiState.update { state -> state.copy(hasError = true) } }
            mutableUiState.update { it.copy(isSaving = false) }
        }
    }

    companion object {
        fun factory(repository: IncidentsRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    require(modelClass.isAssignableFrom(IncidentsViewModel::class.java))
                    return IncidentsViewModel(repository) as T
                }
            }
    }
}
