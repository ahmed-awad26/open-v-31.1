package com.opencontacts.app

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.AssistChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.os.LocaleListCompat
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.opencontacts.core.ui.theme.OpenContactsTheme
import com.opencontacts.feature.contacts.ContactDetailsRoute
import com.opencontacts.feature.contacts.ContactsRoute
import com.opencontacts.feature.vaults.VaultsRoute
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    private val viewModel: AppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ThemedApp(viewModel)
        }
    }

    override fun onStop() {
        super.onStop()
        AppVisibilityTracker.setForeground(false)
        if (!isChangingConfigurations) {
            viewModel.onAppBackgrounded()
        }
    }

    override fun onStart() {
        super.onStart()
        AppVisibilityTracker.setForeground(true)
        if (!isChangingConfigurations) {
            viewModel.onAppForegrounded()
        }
    }
}

@Composable
private fun ThemedApp(viewModel: AppViewModel) {
    val settings by viewModel.appLockSettings.collectAsStateWithLifecycle()
    LaunchedEffect(settings.appLanguage) {
        val localeTags = when (settings.appLanguage.uppercase()) {
            "AR" -> "ar"
            "EN" -> "en"
            else -> ""
        }
        val currentTags = AppCompatDelegate.getApplicationLocales().toLanguageTags()
        if (currentTags != localeTags) {
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(localeTags))
        }
    }
    OpenContactsTheme(themeMode = settings.themeMode, themePreset = settings.themePreset, accentPalette = settings.accentPalette, cornerStyle = settings.cornerStyle) {
        Surface(color = MaterialTheme.colorScheme.background) {
            AppRoot(viewModel)
        }
    }
}

@Composable
private fun AppRoot(viewModel: AppViewModel) {
    val shouldShowUnlock by viewModel.shouldShowUnlock.collectAsStateWithLifecycle()
    if (shouldShowUnlock) {
        UnlockRoute(viewModel = viewModel)
    } else {
        Box(modifier = Modifier.fillMaxSize()) {
            AppNavHost(viewModel)
            IncomingCallInAppHost()
            ActiveCallReturnEntry()
        }
    }
}


@Composable
private fun ActiveCallReturnEntry() {
    val localCall by ActiveCallOverlayController.state.collectAsStateWithLifecycle()
    val telecomCall by TelecomCallCoordinator.activeCall.collectAsStateWithLifecycle()
    val call = telecomCall ?: localCall
    val context = LocalContext.current
    if (call != null) {
        Box(modifier = Modifier.fillMaxSize()) {
            AssistChip(
                onClick = { launchActiveCallControls(context, call!!, forceShow = true) },
                label = { Text(text = call!!.displayName.ifBlank { call!!.number.ifBlank { "Call in progress" } }) },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 24.dp),
            )
        }
    }
}

@Composable
private fun AppNavHost(viewModel: AppViewModel) {
    val navController = rememberNavController()
    val activeVaultName by viewModel.activeVaultName.collectAsStateWithLifecycle()
    val vaults by viewModel.vaults.collectAsStateWithLifecycle()

    NavHost(navController = navController, startDestination = "contacts") {
        composable("contacts") {
            ContactsRoute(
                activeVaultName = activeVaultName,
                vaults = vaults,
                onOpenDetails = { navController.navigate("contact/$it") },
                onOpenWorkspace = { navController.navigate("workspace") },
                onOpenImportExport = { navController.navigate("settings/importexport") },
                onOpenSearch = null,
                onOpenSecurity = { navController.navigate("settings") },
                onOpenBackup = { navController.navigate("settings/backup") },
                onOpenTrash = { navController.navigate("settings/trash") },
                onOpenVaults = { navController.navigate("vaults") },
                onSwitchVault = viewModel::switchVault,
            )
        }
        composable(route = "contact/{contactId}", arguments = listOf(navArgument("contactId") { type = NavType.StringType })) {
            ContactDetailsRoute(onBack = { navController.popBackStack() })
        }
        composable("vaults") { VaultsRoute(onBack = { navController.popBackStack() }) }
        composable("workspace") {
            WorkspaceRoute(
                onBack = { navController.popBackStack() },
                onOpenDetails = { navController.navigate("contact/$it") },
            )
        }
        composable("search") { SearchRoute(onBack = { navController.popBackStack() }) }
        composable("settings") { SettingsHomeRoute(onBack = { navController.popBackStack() }, onNavigate = { navController.navigate(it) }) }
        composable("settings/security") { SecurityRoute(onBack = { navController.popBackStack() }, appViewModel = viewModel) }
        composable("settings/backup") { BackupRoute(onBack = { navController.popBackStack() }, appViewModel = viewModel) }
        composable("settings/importexport") { ImportExportRoute(onBack = { navController.popBackStack() }, appViewModel = viewModel) }
        composable("settings/preferences") { PreferencesRoute(onBack = { navController.popBackStack() }, appViewModel = viewModel) }
        composable("settings/notifications") { NotificationsIncomingCallsRoute(onBack = { navController.popBackStack() }, appViewModel = viewModel) }
        composable("settings/blocked") { BlockedContactsRoute(onBack = { navController.popBackStack() }, appViewModel = viewModel) }
        composable("settings/trash") { TrashRoute(onBack = { navController.popBackStack() }, appViewModel = viewModel) }
        composable("settings/appearance") { AppearanceRoute(onBack = { navController.popBackStack() }, appViewModel = viewModel) }
        composable("settings/icon") { IconCustomizationRoute(onBack = { navController.popBackStack() }, appViewModel = viewModel) }
        composable("settings/about") { AboutRoute(onBack = { navController.popBackStack() }, appViewModel = viewModel) }
    }
}
