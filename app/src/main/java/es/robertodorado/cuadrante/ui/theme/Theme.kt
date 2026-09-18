package es.robertodorado.cuadrante.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColors = lightColorScheme(
    primary = BluePrimary,
    primaryContainer = BlueContainer,
    background = AppBackground,
    surface = AppSurface,
    surfaceVariant = AppSurfaceVariant,
)

private val DarkColors = darkColorScheme(
    primary = BluePrimaryDark,
    primaryContainer = BlueContainerDark,
)

@Composable
fun CuadranteTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content,
    )
}
