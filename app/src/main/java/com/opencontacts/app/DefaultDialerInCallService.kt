package com.opencontacts.app

import android.app.KeyguardManager
import android.content.Context
import android.os.Build
import android.telecom.Call
import android.telecom.CallAudioState
import android.telecom.InCallService
import android.telecom.VideoProfile
import com.opencontacts.core.model.ContactSummary
import com.opencontacts.domain.contacts.ContactRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

private val telecomScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

@EntryPoint
@InstallIn(SingletonComponent::class)
interface TelecomEntryPoint {
    fun contactRepository(): ContactRepository
}

class DefaultDialerInCallService : InCallService() {
    private val callbacks = mutableMapOf<Call, Call.Callback>()

    override fun onCreate() {
        super.onCreate()
        TelecomCallCoordinator.attachService(this)
    }

    override fun onDestroy() {
        callbacks.forEach { (call, callback) -> runCatching { call.unregisterCallback(callback) } }
        callbacks.clear()
        TelecomCallCoordinator.detachService(this)
        super.onDestroy()
    }

    override fun onCallAdded(call: Call) {
        super.onCallAdded(call)
        telecomScope.launch {
            val ui = lookupIncomingCallUi(applicationContext, call)
            val activeUi = ui.toActiveCallUiState()
            when (call.state) {
                Call.STATE_RINGING -> {
                    TelecomCallCoordinator.onIncoming(call, ui)
                    presentIncomingCallExperience(applicationContext, ui)
                }
                Call.STATE_ACTIVE,
                Call.STATE_DIALING,
                Call.STATE_CONNECTING,
                Call.STATE_HOLDING -> {
                    TelecomCallCoordinator.onCallActive(call, activeUi)
                    postOngoingCallNotification(applicationContext, activeUi)
                }
            }
        }
        val callback = object : Call.Callback() {
            override fun onStateChanged(call: Call, state: Int) {
                telecomScope.launch {
                    val ui = lookupIncomingCallUi(applicationContext, call)
                    val activeUi = ui.toActiveCallUiState()
                    when (state) {
                        Call.STATE_RINGING -> {
                            TelecomCallCoordinator.onIncoming(call, ui)
                            presentIncomingCallExperience(applicationContext, ui)
                        }
                        Call.STATE_ACTIVE,
                        Call.STATE_DIALING,
                        Call.STATE_CONNECTING,
                        Call.STATE_HOLDING -> {
                            TelecomCallCoordinator.onCallActive(call, activeUi)
                            postOngoingCallNotification(applicationContext, activeUi)
                            dismissFloatingIncomingCall(applicationContext)
                        }
                        Call.STATE_DISCONNECTED,
                        Call.STATE_DISCONNECTING -> {
                            dismissIncomingUi(applicationContext)
                            dismissFloatingIncomingCall(applicationContext)
                            ActiveCallOverlayController.clear()
                            TelecomCallCoordinator.clearAll()
                            cancelOngoingCallNotification(applicationContext)
                        }
                        else -> TelecomCallCoordinator.onCallStateChanged(call)
                    }
                }
            }

            override fun onDetailsChanged(call: Call, details: Call.Details) {
                telecomScope.launch {
                    val ui = lookupIncomingCallUi(applicationContext, call)
                    if (call.state == Call.STATE_RINGING) {
                        TelecomCallCoordinator.onIncoming(call, ui)
                    } else {
                        TelecomCallCoordinator.onCallActive(call, ui.toActiveCallUiState())
                    }
                }
            }
        }
        callbacks[call] = callback
        call.registerCallback(callback)
    }

    override fun onCallRemoved(call: Call) {
        callbacks.remove(call)?.let { runCatching { call.unregisterCallback(it) } }
        dismissIncomingUi(applicationContext)
        dismissFloatingIncomingCall(applicationContext)
        ActiveCallOverlayController.clear()
        TelecomCallCoordinator.clearAll()
        cancelOngoingCallNotification(applicationContext)
        super.onCallRemoved(call)
    }

    override fun onCallAudioStateChanged(audioState: CallAudioState?) {
        super.onCallAudioStateChanged(audioState)
        TelecomCallCoordinator.updateAudioState(audioState)
    }
}

private suspend fun lookupIncomingCallUi(context: Context, call: Call): IncomingCallUiState {
    val entryPoint = EntryPointAccessors.fromApplication(context.applicationContext, IncomingCallEntryPoint::class.java)
    val vaultId = entryPoint.vaultSessionManager().activeVaultId.value
    val rawNumber = call.details.handle?.schemeSpecificPart.orEmpty()
    val matched = if (vaultId == null) null else {
        val contacts = EntryPointAccessors.fromApplication(context.applicationContext, TelecomEntryPoint::class.java)
            .contactRepository()
            .observeContacts(vaultId)
            .first()
        val normalized = normalizeIncomingNumber(rawNumber)
        contacts.firstOrNull { normalizeIncomingNumber(it.primaryPhone) == normalized }
    }
    return matched.toIncomingCallUiState(rawNumber)
}

internal fun presentIncomingCallExperience(context: Context, ui: IncomingCallUiState) {
    val settings = runCatching { kotlinx.coroutines.runBlocking { entryPoint(context).appLockRepository().settings.first() } }.getOrDefault(com.opencontacts.core.crypto.AppLockSettings.DEFAULT)
    IncomingCallOverlayController.show(ui)
    postIncomingCallNotification(context, ui, settings)
    val keyguardManager = context.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
    val locked = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) keyguardManager.isDeviceLocked else keyguardManager.inKeyguardRestrictedInputMode()
    when {
        locked -> {
            runCatching {
                context.startActivity(
                    android.content.Intent(context, IncomingCallOverlayActivity::class.java)
                        .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_SINGLE_TOP or android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP),
                )
            }
        }
        settings.overlayPopupMode.equals("OVERLAY_WINDOW", ignoreCase = true) && canPresentCallUiOutsideApp(context) -> {
            FloatingIncomingCallService.show(context, ui)
        }
        AppVisibilityTracker.isForeground.value -> Unit
        else -> {
            runCatching {
                context.startActivity(
                    android.content.Intent(context, IncomingCallOverlayActivity::class.java)
                        .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_SINGLE_TOP or android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP),
                )
            }
        }
    }
}
