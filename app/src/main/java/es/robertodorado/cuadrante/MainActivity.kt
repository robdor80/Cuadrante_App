package es.robertodorado.cuadrante

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import es.robertodorado.cuadrante.data.local.AppSettingsRepository
import es.robertodorado.cuadrante.data.local.appSettingsDataStore
import es.robertodorado.cuadrante.ui.CuadranteApp
import es.robertodorado.cuadrante.ui.calendar.CalendarViewModel
import es.robertodorado.cuadrante.ui.settings.SettingsViewModel
import es.robertodorado.cuadrante.ui.theme.CuadranteTheme

class MainActivity : ComponentActivity() {
    private val settingsRepository by lazy {
        AppSettingsRepository(applicationContext.appSettingsDataStore)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CuadranteTheme {
                val settingsViewModel: SettingsViewModel = viewModel(
                    factory = SettingsViewModel.factory(settingsRepository),
                )
                val calendarViewModel: CalendarViewModel = viewModel()
                CuadranteApp(
                    settingsViewModel = settingsViewModel,
                    calendarViewModel = calendarViewModel,
                )
            }
        }
    }
}
