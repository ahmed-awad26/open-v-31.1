package com.opencontacts.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun AppearanceRoute(
    onBack: () -> Unit,
    appViewModel: AppViewModel,
) {
    val settings by appViewModel.appLockSettings.collectAsStateWithLifecycle()
    SettingsScaffold(title = "Appearance", onBack = onBack) { modifier ->
        LazyColumn(modifier = modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                SettingsSection(title = "Theme mode", subtitle = "Choose when OpenContacts uses a light or dark foundation.") {
                    SettingsChoiceRow(
                        title = "Mode",
                        subtitle = "Apply instantly without restarting the app.",
                        selected = settings.themeMode,
                        choices = listOf("SYSTEM", "LIGHT", "DARK"),
                        onSelect = appViewModel::setThemeMode,
                    )
                }
            }
            item {
                SettingsSection(title = "Theme preset", subtitle = "Move between different moods without losing readability.") {
                    SettingsChoiceRow(
                        title = "Preset",
                        subtitle = "Preset changes surfaces, contrast mood, and the overall feel.",
                        selected = settings.themePreset,
                        choices = listOf("CLASSIC", "GLASS", "AMOLED", "SOFT"),
                        onSelect = appViewModel::setThemePreset,
                    )
                    SettingsSpacer()
                    SettingsChoiceRow(
                        title = "Accent palette",
                        subtitle = "This color leads buttons, toggles, and chips across the app.",
                        selected = settings.accentPalette,
                        choices = listOf("BLUE", "EMERALD", "SUNSET", "LAVENDER", "ROSE"),
                        onSelect = appViewModel::setAccentPalette,
                    )
                    SettingsSpacer()
                    SettingsChoiceRow(
                        title = "Corner style",
                        subtitle = "Adjust card and container roundness for a softer or sharper look.",
                        selected = settings.cornerStyle,
                        choices = listOf("ROUNDED", "COMPACT", "SHARP"),
                        onSelect = appViewModel::setCornerStyle,
                    )
                }
            }
            item {
                SettingsSection(title = "Language", subtitle = "Switch the app language independently from the device when needed.") {
                    SettingsChoiceRow(
                        title = "App language",
                        subtitle = "Arabic uses real translated strings and RTL layout when available.",
                        selected = settings.appLanguage,
                        choices = listOf("SYSTEM", "EN", "AR"),
                        onSelect = appViewModel::setAppLanguage,
                    )
                }
            }
            item {
                SettingsSection(title = "Live preview", subtitle = "A compact preview helps you tune the theme without guessing.") {
                    ThemePreviewCard(
                        title = settings.themePreset,
                        subtitle = "${settings.accentPalette} • ${settings.cornerStyle}",
                    )
                }
            }
        }
    }
}

@Composable
private fun ThemePreviewCard(title: String, subtitle: String) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(title, style = MaterialTheme.typography.titleLarge)
            Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(92.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer, MaterialTheme.shapes.large),
            )
        }
    }
}
