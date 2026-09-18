package es.robertodorado.cuadrante.ui.calendar

import androidx.lifecycle.ViewModel
import java.time.LocalDate
import java.time.YearMonth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class CalendarViewModel : ViewModel() {
    val today: LocalDate = LocalDate.now()

    private val mutableVisibleMonth = MutableStateFlow(YearMonth.from(today))
    val visibleMonth: StateFlow<YearMonth> = mutableVisibleMonth.asStateFlow()

    fun showPreviousMonth() {
        mutableVisibleMonth.update { it.minusMonths(1) }
    }

    fun showNextMonth() {
        mutableVisibleMonth.update { it.plusMonths(1) }
    }

    fun showToday() {
        mutableVisibleMonth.value = YearMonth.from(today)
    }
}
