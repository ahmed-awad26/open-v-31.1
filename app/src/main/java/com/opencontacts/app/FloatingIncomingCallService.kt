package com.opencontacts.app

import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.PixelFormat
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.core.net.toUri
import androidx.core.view.isVisible
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

class FloatingIncomingCallService : Service() {
    private var windowManager: WindowManager? = null
    private var overlayView: View? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action.orEmpty()
        if (action == ACTION_HIDE) {
            removeOverlay()
            stopSelf()
            return START_NOT_STICKY
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            stopSelf()
            return START_NOT_STICKY
        }
        val state = IncomingCallOverlayController.state.value ?: intent?.toIncomingUiState() ?: run {
            stopSelf()
            return START_NOT_STICKY
        }
        showOverlay(state)
        return START_STICKY
    }

    override fun onDestroy() {
        removeOverlay()
        super.onDestroy()
    }

    private fun showOverlay(call: IncomingCallUiState) {
        if (overlayView != null) {
            bind(call)
            return
        }
        val settings = runBlocking {
            EntryPointAccessors.fromApplication(applicationContext, IncomingCallEntryPoint::class.java)
                .appLockRepository()
                .settings.first()
        }
        val inflater = LayoutInflater.from(this)
        val view = inflater.inflate(com.opencontacts.app.R.layout.floating_incoming_call, null, false)
        val layoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY else WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = when (settings.incomingCallWindowPosition.uppercase()) {
                "TOP" -> Gravity.TOP or Gravity.CENTER_HORIZONTAL
                "BOTTOM" -> Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
                else -> Gravity.CENTER
            }
            y = 36
        }
        view.alpha = settings.incomingCallWindowTransparency.coerceIn(55, 100) / 100f
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        windowManager?.addView(view, layoutParams)
        overlayView = view
        bind(call)
    }

    private fun bind(call: IncomingCallUiState) {
        val view = overlayView ?: return
        view.findViewById<TextView>(com.opencontacts.app.R.id.callLabel).text = getString(com.opencontacts.app.R.string.incoming_call_label)
        view.findViewById<TextView>(com.opencontacts.app.R.id.callName).text = call.displayName
        view.findViewById<TextView>(com.opencontacts.app.R.id.callNumber).apply {
            text = call.number
            isVisible = call.number.isNotBlank()
        }
        view.findViewById<TextView>(com.opencontacts.app.R.id.callMeta).apply {
            text = buildList {
                call.folderName?.takeIf { it.isNotBlank() }?.let(::add)
                addAll(call.tags.take(3))
            }.joinToString(" • ")
            isVisible = text.isNotBlank()
        }
        val avatar = view.findViewById<ImageView>(com.opencontacts.app.R.id.callAvatar)
        val bitmap = runCatching {
            call.photoUri?.let { uri -> contentResolver.openInputStream(uri.toUri())?.use(BitmapFactory::decodeStream) }
        }.getOrNull()
        if (bitmap != null) {
            avatar.setImageBitmap(bitmap)
        } else {
            avatar.setImageResource(android.R.drawable.sym_def_app_icon)
        }
        view.findViewById<ImageButton>(com.opencontacts.app.R.id.closeButton).setOnClickListener {
            dismissIncomingUi(this)
            dismissFloatingIncomingCall(this)
        }
        view.findViewById<View>(com.opencontacts.app.R.id.declineButton).setOnClickListener {
            TelecomCallCoordinator.decline()
            dismissIncomingUi(this)
            dismissFloatingIncomingCall(this)
        }
        view.findViewById<View>(com.opencontacts.app.R.id.answerButton).setOnClickListener {
            TelecomCallCoordinator.answer()
            dismissIncomingUi(this)
            dismissFloatingIncomingCall(this)
            launchActiveCallControls(this, call.toActiveCallUiState(), forceShow = true)
        }
        view.findViewById<View>(com.opencontacts.app.R.id.rootCard).setOnClickListener {
            launchActiveCallControls(this, call.toActiveCallUiState(), forceShow = true)
        }
    }

    private fun removeOverlay() {
        overlayView?.let { view ->
            runCatching { windowManager?.removeView(view) }
        }
        overlayView = null
        windowManager = null
    }

    companion object {
        private const val ACTION_SHOW = "com.opencontacts.app.action.SHOW_FLOATING_INCOMING_CALL"
        private const val ACTION_HIDE = "com.opencontacts.app.action.HIDE_FLOATING_INCOMING_CALL"

        fun show(context: Context, call: IncomingCallUiState) {
            val intent = Intent(context, FloatingIncomingCallService::class.java)
                .setAction(ACTION_SHOW)
                .putExtra("displayName", call.displayName)
                .putExtra("number", call.number)
                .putExtra("photoUri", call.photoUri)
                .putExtra("folderName", call.folderName)
                .putStringArrayListExtra("tags", ArrayList(call.tags))
                .putExtra("contactId", call.contactId)
            context.startService(intent)
        }

        fun hide(context: Context) {
            context.startService(Intent(context, FloatingIncomingCallService::class.java).setAction(ACTION_HIDE))
        }
    }
}

private fun Intent.toIncomingUiState(): IncomingCallUiState = IncomingCallUiState(
    displayName = getStringExtra("displayName").orEmpty(),
    number = getStringExtra("number").orEmpty(),
    photoUri = getStringExtra("photoUri"),
    folderName = getStringExtra("folderName"),
    tags = getStringArrayListExtra("tags")?.toList().orEmpty(),
    contactId = getStringExtra("contactId"),
)

internal fun dismissFloatingIncomingCall(context: Context) {
    FloatingIncomingCallService.hide(context)
}
