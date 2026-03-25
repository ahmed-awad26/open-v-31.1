package com.opencontacts.core.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val OpenContactsTypography = Typography(
    headlineLarge = TextStyle(fontSize = 32.sp, lineHeight = 38.sp, fontWeight = FontWeight.Bold),
    headlineMedium = TextStyle(fontSize = 28.sp, lineHeight = 34.sp, fontWeight = FontWeight.Bold),
    headlineSmall = TextStyle(fontSize = 24.sp, lineHeight = 30.sp, fontWeight = FontWeight.SemiBold),
    titleLarge = TextStyle(fontSize = 20.sp, lineHeight = 26.sp, fontWeight = FontWeight.SemiBold),
    titleMedium = TextStyle(fontSize = 16.sp, lineHeight = 22.sp, fontWeight = FontWeight.SemiBold),
    bodyLarge = TextStyle(fontSize = 15.sp, lineHeight = 22.sp),
    bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall = TextStyle(fontSize = 12.sp, lineHeight = 18.sp),
    labelLarge = TextStyle(fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.Medium),
)

@Composable
fun OpenContactsTheme(
    themeMode: String = "SYSTEM",
    themePreset: String = "CLASSIC",
    accentPalette: String = "BLUE",
    cornerStyle: String = "ROUNDED",
    content: @Composable () -> Unit,
) {
    val dark = when (themeMode.uppercase()) {
        "LIGHT" -> false
        "DARK" -> true
        else -> isSystemInDarkTheme()
    }
    val colors = if (dark) darkScheme(themePreset, accentPalette) else lightScheme(themePreset, accentPalette)
    val radius = when (cornerStyle.uppercase()) {
        "COMPACT" -> 14.dp
        "SHARP" -> 6.dp
        else -> 22.dp
    }
    MaterialTheme(
        colorScheme = colors,
        typography = OpenContactsTypography,
        shapes = Shapes(
            extraSmall = androidx.compose.foundation.shape.RoundedCornerShape(radius * 0.4f),
            small = androidx.compose.foundation.shape.RoundedCornerShape(radius * 0.7f),
            medium = androidx.compose.foundation.shape.RoundedCornerShape(radius),
            large = androidx.compose.foundation.shape.RoundedCornerShape(radius * 1.2f),
            extraLarge = androidx.compose.foundation.shape.RoundedCornerShape(radius * 1.4f),
        ),
        content = content,
    )
}

private fun lightScheme(preset: String, accent: String) = lightColorScheme(
    primary = primary(accent, false),
    onPrimary = Color.White,
    primaryContainer = primaryContainer(accent, false),
    onPrimaryContainer = onPrimaryContainer(accent, false),
    secondary = secondary(accent, false),
    onSecondary = Color.White,
    secondaryContainer = secondaryContainer(accent, false),
    onSecondaryContainer = onPrimaryContainer(accent, false),
    tertiary = tertiary(accent, false),
    background = when (preset.uppercase()) {
        "AMOLED" -> Color(0xFFF3F5FA)
        "GLASS" -> Color(0xFFF6F8FC)
        "SOFT" -> Color(0xFFFFF8FB)
        else -> Color(0xFFF7F8FC)
    },
    onBackground = Color(0xFF0F172A),
    surface = when (preset.uppercase()) {
        "GLASS" -> Color(0xFFFDFEFF)
        "AMOLED" -> Color(0xFFFAFBFF)
        else -> Color(0xFFFCFDFF)
    },
    onSurface = Color(0xFF0F172A),
    surfaceVariant = Color(0xFFE7ECF5),
    onSurfaceVariant = Color(0xFF475569),
    outline = Color(0xFF94A3B8),
)

private fun darkScheme(preset: String, accent: String) = darkColorScheme(
    primary = primary(accent, true),
    onPrimary = Color(0xFF08111C),
    primaryContainer = primaryContainer(accent, true),
    onPrimaryContainer = onPrimaryContainer(accent, true),
    secondary = secondary(accent, true),
    onSecondary = Color(0xFF05221F),
    secondaryContainer = secondaryContainer(accent, true),
    onSecondaryContainer = Color(0xFFE5FFFA),
    tertiary = tertiary(accent, true),
    background = when (preset.uppercase()) {
        "AMOLED" -> Color(0xFF000000)
        "GLASS" -> Color(0xFF080D17)
        "SOFT" -> Color(0xFF11111A)
        else -> Color(0xFF09101D)
    },
    onBackground = Color(0xFFE5EAF4),
    surface = when (preset.uppercase()) {
        "AMOLED" -> Color(0xFF030303)
        "GLASS" -> Color(0xFF111827)
        else -> Color(0xFF111827)
    },
    onSurface = Color(0xFFE5EAF4),
    surfaceVariant = Color(0xFF1E293B),
    onSurfaceVariant = Color(0xFFB9C5D9),
    outline = Color(0xFF7E8BA1),
)

private fun primary(accent: String, dark: Boolean): Color = when (accent.uppercase()) {
    "EMERALD" -> if (dark) Color(0xFF6EE7B7) else Color(0xFF059669)
    "SUNSET" -> if (dark) Color(0xFFFDBA74) else Color(0xFFEA580C)
    "LAVENDER" -> if (dark) Color(0xFFD8B4FE) else Color(0xFF8B5CF6)
    "ROSE" -> if (dark) Color(0xFFF9A8D4) else Color(0xFFE11D48)
    else -> if (dark) Color(0xFF93C5FD) else Color(0xFF2563EB)
}

private fun primaryContainer(accent: String, dark: Boolean): Color = when (accent.uppercase()) {
    "EMERALD" -> if (dark) Color(0xFF065F46) else Color(0xFFD1FAE5)
    "SUNSET" -> if (dark) Color(0xFF7C2D12) else Color(0xFFFFEDD5)
    "LAVENDER" -> if (dark) Color(0xFF5B21B6) else Color(0xFFEDE9FE)
    "ROSE" -> if (dark) Color(0xFF881337) else Color(0xFFFFE4E6)
    else -> if (dark) Color(0xFF2647C7) else Color(0xFFD9E2FF)
}

private fun onPrimaryContainer(accent: String, dark: Boolean): Color = if (dark) Color.White else Color(0xFF0A1F5A)
private fun secondary(accent: String, dark: Boolean): Color = when (accent.uppercase()) {
    "EMERALD" -> if (dark) Color(0xFF5EEAD4) else Color(0xFF0F766E)
    "SUNSET" -> if (dark) Color(0xFFF9A8D4) else Color(0xFFDB2777)
    "LAVENDER" -> if (dark) Color(0xFFA5B4FC) else Color(0xFF4F46E5)
    "ROSE" -> if (dark) Color(0xFFFDA4AF) else Color(0xFFBE123C)
    else -> if (dark) Color(0xFF5EEAD4) else Color(0xFF14B8A6)
}
private fun secondaryContainer(accent: String, dark: Boolean): Color = if (dark) Color(0xFF123A37) else Color(0xFFC7FFF7)
private fun tertiary(accent: String, dark: Boolean): Color = when (accent.uppercase()) {
    "EMERALD" -> if (dark) Color(0xFFBBF7D0) else Color(0xFF16A34A)
    "SUNSET" -> if (dark) Color(0xFFFCD34D) else Color(0xFFD97706)
    "LAVENDER" -> if (dark) Color(0xFFE9D5FF) else Color(0xFF7C3AED)
    "ROSE" -> if (dark) Color(0xFFFBCFE8) else Color(0xFFDB2777)
    else -> if (dark) Color(0xFFE9B6FF) else Color(0xFF8B5CF6)
}
