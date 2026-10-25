package br.com.condominio.escala.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val Azul = Color(0xFF3B82F6)
private val AzulEscuro = Color(0xFF1E40AF)

private val Claro = lightColorScheme(primary = AzulEscuro, secondary = Color(0xFF10B981))
private val Escuro = darkColorScheme(
    primary = Azul,
    secondary = Color(0xFF10B981),
    background = Color(0xFF0A0E17),
    surface = Color(0xFF111827),
)

/** Paleta usada para identificar cada funcionário no calendário. */
val CoresFuncionario = listOf(
    0xFF3B82F6, 0xFF10B981, 0xFFF59E0B, 0xFFEF4444, 0xFF8B5CF6,
    0xFFEC4899, 0xFF14B8A6, 0xFFF97316, 0xFF64748B, 0xFF84CC16,
)

@Composable
fun EscalaTheme(content: @Composable () -> Unit) {
    val escuro = isSystemInDarkTheme()
    val esquema = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val ctx = LocalContext.current
            if (escuro) dynamicDarkColorScheme(ctx) else dynamicLightColorScheme(ctx)
        }
        escuro -> Escuro
        else -> Claro
    }
    MaterialTheme(colorScheme = esquema, content = content)
}
