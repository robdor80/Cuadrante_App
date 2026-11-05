package es.robertodorado.cuadrante.ui.incidents

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import es.robertodorado.cuadrante.data.local.*
import es.robertodorado.cuadrante.domain.ap.*
import es.robertodorado.cuadrante.model.Incident
import java.time.Instant
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class ApUiState(
    val data: ApState = ApState(),
    val incidents: List<Incident> = emptyList(),
    val proposal: ApProposal? = null,
    val error: String? = null,
    val busy: Boolean = false,
    val loaded: Boolean = false,
)

class ApViewModel(private val repository: ApRepository) : ViewModel() {
    private val mutable = MutableStateFlow(ApUiState())
    val uiState = mutable.asStateFlow()
    init {
        viewModelScope.launch {
            try {
                combine(repository.state, repository.incidents) { state, incidents -> state to incidents }.collect { (data, incidents) ->
                    mutable.update { it.copy(data = data, incidents = incidents, loaded = true) }
                }
            } catch (e: Exception) {
                mutable.update { it.copy(error = "No se pueden leer los datos AP. Se conservan sin cambios: ${e.message}") }
            }
        }
    }
    fun preview(change: ApChange) {
        if (mutable.value.busy || !mutable.value.loaded) return
        mutable.update { it.copy(busy = true, error = null, proposal = null) }
        viewModelScope.launch {
            try {
                val proposal = repository.preview(change, Instant.now())
                mutable.update { it.copy(proposal = proposal) }
            } catch (e: Exception) {
                mutable.update { it.copy(error = e.message ?: "No se puede preparar el cambio") }
            } finally { mutable.update { it.copy(busy = false) } }
        }
    }
    fun clearError() { mutable.update { it.copy(error = null) } }
    fun cancel() { if (!mutable.value.busy) mutable.update { it.copy(proposal = null) } }
    fun confirm() {
        val proposal = mutable.value.proposal ?: return
        if (mutable.value.busy) return
        mutable.update { it.copy(busy = true, error = null) }
        viewModelScope.launch {
            try { repository.confirm(proposal, Confirmation.CONFIRM) }
            catch (e: Exception) { mutable.update { it.copy(error = e.message) } }
            finally { mutable.update { it.copy(busy = false, proposal = null) } }
        }
    }
    companion object {
        fun factory(repository: ApRepository): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                require(modelClass.isAssignableFrom(ApViewModel::class.java))
                return ApViewModel(repository) as T
            }
        }
    }
}
