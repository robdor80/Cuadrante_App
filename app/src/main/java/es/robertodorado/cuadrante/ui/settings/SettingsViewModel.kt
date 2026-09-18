package es.robertodorado.cuadrante.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import es.robertodorado.cuadrante.data.local.AppSettingsRepository
import es.robertodorado.cuadrante.model.AppSettings
import es.robertodorado.cuadrante.model.DefaultShiftPatterns
import es.robertodorado.cuadrante.model.ShiftPatternType
import es.robertodorado.cuadrante.model.ShiftType
import java.time.LocalDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SettingsUiState(
    val isLoading: Boolean = true,
    val patternType: ShiftPatternType = ShiftPatternType.SIX_BY_SIX,
    val customShifts: List<ShiftType> = emptyList(),
    val referenceDate: LocalDate = LocalDate.now(),
    val isSaving: Boolean = false,
    val saveResult: SaveResult? = null,
) {
    val selectedShifts: List<ShiftType>
        get() = when (patternType) {
            ShiftPatternType.SIX_BY_SIX -> DefaultShiftPatterns.SIX_BY_SIX.shifts
            ShiftPatternType.CUSTOM -> customShifts
        }

    val canSave: Boolean
        get() = !isLoading && !isSaving && selectedShifts.isNotEmpty()
}

enum class SaveResult {
    SUCCESS,
    ERROR,
}

class SettingsViewModel(
    private val repository: AppSettingsRepository,
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = mutableUiState.asStateFlow()

    init {
        viewModelScope.launch {
            val savedSettingsResult = runCatching { repository.settings.first() }
            val savedSettings = savedSettingsResult.getOrNull()
            mutableUiState.update { currentState ->
                if (savedSettings == null) {
                    currentState.copy(
                        isLoading = false,
                        saveResult = if (savedSettingsResult.isFailure) SaveResult.ERROR else null,
                    )
                } else {
                    currentState.copy(
                        isLoading = false,
                        patternType = savedSettings.patternType,
                        customShifts = if (savedSettings.patternType == ShiftPatternType.CUSTOM) {
                            savedSettings.shifts
                        } else {
                            emptyList()
                        },
                        referenceDate = savedSettings.referenceDate,
                    )
                }
            }
        }
    }

    fun selectPattern(type: ShiftPatternType) {
        mutableUiState.update { it.copy(patternType = type, saveResult = null) }
    }

    fun addShift(shift: ShiftType) {
        mutableUiState.update {
            it.copy(customShifts = it.customShifts + shift, saveResult = null)
        }
    }

    fun removeLastShift() {
        mutableUiState.update {
            it.copy(customShifts = it.customShifts.dropLast(1), saveResult = null)
        }
    }

    fun clearCustomShifts() {
        mutableUiState.update { it.copy(customShifts = emptyList(), saveResult = null) }
    }

    fun setReferenceDate(date: LocalDate) {
        mutableUiState.update { it.copy(referenceDate = date, saveResult = null) }
    }

    fun save() {
        val currentState = mutableUiState.value
        if (!currentState.canSave) return

        val settings = AppSettings(
            patternType = currentState.patternType,
            shifts = currentState.selectedShifts,
            referenceDate = currentState.referenceDate,
        )
        mutableUiState.update { it.copy(isSaving = true, saveResult = null) }

        viewModelScope.launch {
            val result = runCatching { repository.save(settings) }
            mutableUiState.update {
                it.copy(
                    isSaving = false,
                    saveResult = if (result.isSuccess) SaveResult.SUCCESS else SaveResult.ERROR,
                )
            }
        }
    }

    companion object {
        fun factory(repository: AppSettingsRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    require(modelClass.isAssignableFrom(SettingsViewModel::class.java))
                    return SettingsViewModel(repository) as T
                }
            }
    }
}
