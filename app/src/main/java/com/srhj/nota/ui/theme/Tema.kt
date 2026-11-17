package com.srhj.nota.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Biru = Color(0xFF0D47A1)
val BiruMuda = Color(0xFFD6E4FF)
val Emas = Color(0xFFFFB300)

private val Skema = lightColorScheme(
    primary = Biru,
    onPrimary = Color.White,
    primaryContainer = BiruMuda,
    onPrimaryContainer = Color(0xFF001A43),
    secondary = Color(0xFF8A5A00),
    secondaryContainer = Color(0xFFFFE0A3),
    onSecondaryContainer = Color(0xFF2B1700),
    background = Color(0xFFF6F8FC),
    surface = Color.White,
    surfaceVariant = Color(0xFFE6EBF3),
    error = Color(0xFFB3261E),
)

@Composable
fun TemaNota(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = Skema, content = content)
}
