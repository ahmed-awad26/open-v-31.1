package com.opencontacts.app

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun SecurityRoute(
    onBack: () -> Unit,
    appViewModel: AppViewModel,
) {
    val settings by appViewModel.appLockSettings.collectAsStateWithLifecycle()
    val pinError by appViewModel.pinError.collectAsStateWithLifecycle()
    val availability = appViewModel.biometricAvailability()
    val context = LocalContext.current
    var pin by remember { mutableStateOf("") }
    var lockMessage by remember(settings.lockScreenMessage) { mutableStateOf(settings.lockScreenMessage) }
    val bgPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let(appViewModel::setLockScreenBackground)
    }

    SettingsScaffold(title = stringResource(R.string.security_title), onBack = onBack) { modifier ->
        LazyColumn(modifier = modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                SettingsSection(title = "App lock", subtitle = "Keep your private contacts workspace protected without causing resume loops.") {
                    SettingsSwitchRow(
                        title = "Enable biometric lock",
                        subtitle = if (availability.canAuthenticate) "Tap the fingerprint icon on the lock screen to authenticate." else availability.message,
                        checked = settings.biometricEnabled,
                        enabled = availability.canAuthenticate,
                        onCheckedChange = { enabled ->
                            if (!enabled) {
                                appViewModel.setBiometricEnabled(false)
                            } else {
                                val activity = context.findFragmentActivity() ?: return@SettingsSwitchRow
                                val prompt = BiometricPrompt(
                                    activity,
                                    ContextCompat.getMainExecutor(activity),
                                    object : BiometricPrompt.AuthenticationCallback() {
                                        override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                                            appViewModel.setBiometricEnabled(true)
                                        }

                                        override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                                            appViewModel.showUiError(errString.toString())
                                        }
                                    },
                                )
                                prompt.authenticate(appViewModel.biometricPromptInfo("Enable app lock"))
                            }
                        },
                    )
                    SettingsSpacer()
                    SettingsSwitchRow(
                        title = "Allow device credentials fallback",
                        subtitle = "Let the system PIN, pattern, or password unlock the vault when biometrics are unavailable.",
                        checked = settings.allowDeviceCredential,
                        onCheckedChange = appViewModel::setAllowDeviceCredential,
                    )
                    SettingsSpacer()
                    SettingsSwitchRow(
                        title = "Lock on app resume",
                        subtitle = "Require authentication again after returning from background.",
                        checked = settings.lockOnAppResume,
                        onCheckedChange = appViewModel::setLockOnAppResume,
                    )
                }
            }
            item {
                SettingsSection(title = "Lock screen appearance", subtitle = "Customize the secure screen without exposing content before authentication.") {
                    SettingsChoiceRow(
                        title = "Lock screen style",
                        subtitle = "Switch between a premium card, glass card, or a cleaner minimal surface.",
                        selected = settings.lockScreenStyle,
                        choices = listOf("PREMIUM", "GLASS", "MINIMAL"),
                        onSelect = appViewModel::setLockScreenStyle,
                    )
                    SettingsSpacer()
                    SettingsChoiceRow(
                        title = "Fingerprint icon style",
                        subtitle = "Choose how the biometric trigger looks on the secure screen.",
                        selected = settings.fingerprintIconStyle,
                        choices = listOf("FILLED", "OUTLINE", "TINTED"),
                        onSelect = appViewModel::setFingerprintIconStyle,
                    )
                    SettingsSpacer()
                    SettingsSwitchRow(
                        title = stringResource(R.string.show_app_name),
                        subtitle = "Display the title above the secure card.",
                        checked = settings.showAppNameOnLockScreen,
                        onCheckedChange = appViewModel::setShowAppNameOnLockScreen,
                    )
                    SettingsSpacer()
                    SettingsSwitchRow(
                        title = stringResource(R.string.show_app_icon),
                        subtitle = "Show a small security badge inside the secure card.",
                        checked = settings.showAppIconOnLockScreen,
                        onCheckedChange = appViewModel::setShowAppIconOnLockScreen,
                    )
                    SettingsSpacer()
                    SettingsSwitchRow(
                        title = stringResource(R.string.show_time),
                        subtitle = "Display the current time in the lock screen header.",
                        checked = settings.showTimeOnLockScreen,
                        onCheckedChange = appViewModel::setShowTimeOnLockScreen,
                    )
                    SettingsSpacer()
                    SettingsSwitchRow(
                        title = stringResource(R.string.show_background),
                        subtitle = "Use a decorative tint or imported background image behind the lock card.",
                        checked = settings.useIllustrationOnLockScreen,
                        onCheckedChange = appViewModel::setUseIllustrationOnLockScreen,
                    )
                    SettingsSpacer()
                    OutlinedTextField(
                        value = lockMessage,
                        onValueChange = { lockMessage = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(stringResource(R.string.custom_message)) },
                        minLines = 2,
                    )
                    SettingsSpacer()
                    Button(onClick = { appViewModel.setLockScreenMessage(lockMessage) }) {
                        Text(stringResource(R.string.save))
                    }
                    SettingsSpacer()
                    androidx.compose.material3.TextButton(onClick = { bgPicker.launch(arrayOf("image/*")) }) { Text("Choose background image") }
                    androidx.compose.material3.TextButton(onClick = appViewModel::clearLockScreenBackground) { Text("Clear background image") }
                }
            }
            item {
                SettingsSection(title = "PIN", subtitle = "Use a local PIN as a fallback when biometrics are disabled or unavailable.") {
                    OutlinedTextField(
                        value = pin,
                        onValueChange = {
                            pin = it.filter(Char::isDigit)
                            appViewModel.clearError()
                        },
                        label = { Text(if (settings.hasPin) "Change PIN" else "Set PIN") },
                        singleLine = true,
                    )
                    pinError?.takeIf { it.isNotBlank() }?.let {
                        Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                    SettingsSpacer()
                    Button(onClick = { appViewModel.setPin(pin) }) {
                        Text(if (settings.hasPin) "Update PIN" else "Save PIN")
                    }
                    if (settings.hasPin) {
                        androidx.compose.material3.TextButton(onClick = appViewModel::clearPin) { Text("Clear PIN") }
                    }
                }
            }
            item {
                SettingsSection(title = "Immediate action") {
                    Button(onClick = appViewModel::lockNow) {
                        Text("Lock app now")
                    }
                }
            }
        }
    }
}

private fun android.content.Context.findFragmentActivity(): FragmentActivity? = when (this) {
    is FragmentActivity -> this
    is android.content.ContextWrapper -> baseContext.findFragmentActivity()
    else -> null
}
