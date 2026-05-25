package com.michael.notchcommand.service

import android.accessibilityservice.AccessibilityService
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.PixelFormat
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.os.Looper
import android.os.Process
import android.os.VibrationEffect
import android.os.Vibrator
import android.provider.Settings
import android.util.Log
import android.view.GestureDetector
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.WindowManager
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.michael.notchcommand.MainActivity
import com.michael.notchcommand.R
import com.michael.notchcommand.data.local.GestureConfigStore
import com.michael.notchcommand.data.sensors.AudioLevelMonitor
import com.michael.notchcommand.data.sensors.BatteryMonitor
import com.michael.notchcommand.domain.model.GestureAction
import com.michael.notchcommand.domain.model.GestureConfig
import com.michael.notchcommand.presentation.overlay.NotchRingView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class NotchOverlayService : Service() {

    companion object {
        private const val NOTIF_ID = 8877
        private const val NOTIF_CHANNEL_ID = "notch_command_channel"
        private const val TAG = "NotchOverlayService"
        
        var isServiceRunning = false
            private set
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    // Dedicated background threads
    private var sensorThread: HandlerThread? = null
    private var sensorHandler: Handler? = null

    private var gestureThread: HandlerThread? = null
    private var gestureHandler: Handler? = null

    private lateinit var windowManager: WindowManager
    private lateinit var configStore: GestureConfigStore

    // Overlay Window Elements
    private var overlayContainer: View? = null
    private var ringView: NotchRingView? = null
    private var gestureDetector: GestureDetector? = null

    // Monitors
    private var batteryMonitor: BatteryMonitor? = null
    private var audioMonitor: AudioLevelMonitor? = null

    // Torch support
    private var isFlashlightOn = false

    // Active Gesture configurations
    private var activeConfig = GestureConfig()

    // Struct to hold coordinate state
    private data class LayoutConfig(
        val xFraction: Float,
        val yDp: Float,
        val radiusDp: Float,
        val thicknessDp: Float,
        val rgbMode: Boolean
    )

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "NotchOverlayService onCreate")
        isServiceRunning = true

        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        configStore = GestureConfigStore(this)

        // Bootstrap foreground notifications
        createNotificationChannel()
        val notification = buildForegroundNotification()
        
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(NOTIF_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_CAMERA)
            } else {
                startForeground(NOTIF_ID, notification)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed starting as foreground service", e)
            startForeground(NOTIF_ID, notification)
        }

        // Initialize Background Threads
        sensorThread = HandlerThread("NotchSensorThread", Process.THREAD_PRIORITY_BACKGROUND).apply { start() }
        sensorHandler = Handler(sensorThread!!.looper)

        gestureThread = HandlerThread("NotchGestureThread", Process.THREAD_PRIORITY_DISPLAY).apply { start() }
        gestureHandler = Handler(gestureThread!!.looper)

        // Setup custom layouts & listen to configs reactively
        setupReactiveOverlay()
        
        // Setup hardware triggers
        setupSensors()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIF_CHANNEL_ID,
                getString(R.string.notif_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.notif_channel_desc)
                enableLights(false)
                enableVibration(false)
                setShowBadge(false)
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildForegroundNotification(): Notification {
        val launchIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, launchIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, NOTIF_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_compass) // fallback until custom drawable
            .setContentTitle("NotchCommand is Active")
            .setContentText("Tap overlay to trigger shortcuts & custom ring visuals.")
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setSilent(true)
            .build()
    }

    private fun setupReactiveOverlay() {
        // Collect gesture actions map
        serviceScope.launch {
            configStore.configFlow.collect { config ->
                activeConfig = config
                Log.d(TAG, "Loaded Gesture map: Tap=${config.singleTap.name}, Double=${config.doubleTap.name}, Long=${config.longPress.name}")
            }
        }

        // Collect visual slider preferences and dynamically rebuild the window parameters
        serviceScope.launch {
            combine(
                configStore.notchXFlow,
                configStore.notchYFlow,
                configStore.notchRadiusFlow,
                configStore.notchThicknessFlow,
                configStore.rgbModeFlow
            ) { x, y, radius, thickness, rgb ->
                LayoutConfig(x, y, radius, thickness, rgb)
            }.collect { config ->
                withContext(Dispatchers.Main) {
                    recreateOverlayWindow(config)
                }
            }
        }
    }

    private fun setupSensors() {
        val sensorH = sensorHandler ?: return
        
        // Start battery monitoring
        batteryMonitor = BatteryMonitor(this, sensorH) { percent, charging ->
            ringView?.batteryPercent = percent
            ringView?.isCharging = charging
        }.also { it.start() }

        // Start microphone amp testing
        audioMonitor = AudioLevelMonitor(this, sensorH) { amplitude ->
            ringView?.audioLevel = amplitude
        }.also { it.start() }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun recreateOverlayWindow(config: LayoutConfig) {
        // Double security check: Ensure overlay permission is still active
        if (!Settings.canDrawOverlays(this)) {
            Log.w(TAG, "Cannot render overlay: System alert permission is missing")
            return
        }

        try {
            // Remove previous layouts if exist
            removeOverlayIfExists()

            val density = resources.displayMetrics.density
            val screenWidth = resources.displayMetrics.widthPixels

            // Touch bounding box rules:
            // Expand the touch layout size slightly beyond the physical circle to make tapping extremely accessible.
            val ringRadiusPx = config.radiusDp * density
            val expandedTouchBoundPx = ringRadiusPx * 2f + (36f * density)
            val windowSize = expandedTouchBoundPx.toInt()

            // Calculate exact position based on X fraction and Y offset
            val centerX = (screenWidth * config.xFraction).toInt()
            val centerY = (config.yDp * density).toInt()

            val xPos = centerX - (windowSize / 2)
            val yPos = centerY - (windowSize / 2)

            // Setup custom NotchRingView
            val customRing = NotchRingView(this).apply {
                customRadius = ringRadiusPx
                customThickness = config.thicknessDp * density
                isRgbMode = config.rgbMode
            }
            ringView = customRing

            // Setup Container Frame Layout to host NotchRingView and translate clicks
            overlayContainer = customRing

            // Setup high priority Gesture Listener off the main thread
            val listener = object : GestureDetector.SimpleOnGestureListener() {
                override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                    vibrateFeedback(40L)
                    executeGestureAction(activeConfig.singleTap)
                    return true
                }

                override fun onDoubleTap(e: MotionEvent): Boolean {
                    vibrateFeedback(60L)
                    executeGestureAction(activeConfig.doubleTap)
                    return true
                }

                override fun onLongPress(e: MotionEvent) {
                    vibrateFeedback(100L)
                    executeGestureAction(activeConfig.longPress)
                }
            }

            // Bind Looper of NotchGestureThread to run classifier on background
            gestureDetector = GestureDetector(this, listener, gestureHandler)

            customRing.setOnTouchListener { _, event ->
                gestureDetector?.onTouchEvent(event) == true
            }

            // Apply Material dynamic window layout rules
            val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_SYSTEM_ALERT
            }

            val layoutParams = WindowManager.LayoutParams(
                windowSize,
                windowSize,
                xPos,
                yPos,
                type,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                        or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
                        or WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP or Gravity.START
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
                }
            }

            windowManager.addView(customRing, layoutParams)
            Log.d(TAG, "Notch overlay added successfully at cx=$centerX, cy=$centerY, size=$windowSize")
        } catch (e: Exception) {
            Log.e(TAG, "Recreate overlay window crashed safely", e)
        }
    }

    private fun removeOverlayIfExists() {
        overlayContainer?.let { container ->
            try {
                windowManager.removeView(container)
            } catch (e: Exception) {}
        }
        overlayContainer = null
        ringView = null
    }

    private fun executeGestureAction(action: GestureAction) {
        serviceScope.launch {
            Log.d(TAG, "Executing Gesture Action: ${action.name}")
            when (action) {
                is GestureAction.ToggleFlashlight -> {
                    toggleFlashlight()
                }
                is GestureAction.TakeScreenshot -> {
                    val accService = NotchAccessibilityService.instance
                    if (accService != null) {
                        accService.performAction(AccessibilityService.GLOBAL_ACTION_TAKE_SCREENSHOT)
                    } else {
                        showInstructionToast("Please activate Accessibility Shortcut permissions first.")
                    }
                }
                is GestureAction.OpenNotifications -> {
                    val accService = NotchAccessibilityService.instance
                    if (accService != null) {
                        accService.performAction(AccessibilityService.GLOBAL_ACTION_NOTIFICATIONS)
                    } else {
                        showInstructionToast("Please activate Accessibility Shortcut permissions first.")
                    }
                }
                is GestureAction.ShowQuickSettings -> {
                    val accService = NotchAccessibilityService.instance
                    if (accService != null) {
                        accService.performAction(AccessibilityService.GLOBAL_ACTION_QUICK_SETTINGS)
                    } else {
                        showInstructionToast("Please activate Accessibility Shortcut permissions first.")
                    }
                }
                is GestureAction.GoHome -> {
                    val accService = NotchAccessibilityService.instance
                    if (accService != null) {
                        accService.performAction(AccessibilityService.GLOBAL_ACTION_HOME)
                    } else {
                        val homeIntent = Intent(Intent.ACTION_MAIN).apply {
                            addCategory(Intent.CATEGORY_HOME)
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        startActivity(homeIntent)
                    }
                }
                is GestureAction.GoBack -> {
                    val accService = NotchAccessibilityService.instance
                    if (accService != null) {
                        accService.performAction(AccessibilityService.GLOBAL_ACTION_BACK)
                    } else {
                        showInstructionToast("Please activate Accessibility Shortcut permissions first.")
                    }
                }
                is GestureAction.LaunchSettings -> {
                    val intent = Intent(Settings.ACTION_SETTINGS).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    startActivity(intent)
                }
                is GestureAction.LaunchCamera -> {
                    val intent = Intent(android.provider.MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    try {
                        startActivity(intent)
                    } catch (e: Exception) {
                        showInstructionToast("Failed to launch Camera app.")
                    }
                }
                is GestureAction.DoNothing -> {}
            }
        }
    }

    private fun toggleFlashlight() {
        try {
            val cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
            val cameraId = cameraManager.cameraIdList.firstOrNull { id ->
                cameraManager.getCameraCharacteristics(id)
                    .get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            } ?: return

            isFlashlightOn = !isFlashlightOn
            cameraManager.setTorchMode(cameraId, isFlashlightOn)
            Log.d(TAG, "Flashlight torch set to $isFlashlightOn")
        } catch (e: Exception) {
            Log.e(TAG, "Error toggling torch Mode", e)
        }
    }

    private fun vibrateFeedback(ms: Long) {
        try {
            val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(ms, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(ms)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Vibrator failed", e)
        }
    }

    private fun showInstructionToast(message: String) {
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroy() {
        Log.d(TAG, "NotchOverlayService onDestroy")
        isServiceRunning = false
        
        // Cancel all coroutines safely
        serviceScope.cancel()

        // Turn off torch if left on
        if (isFlashlightOn) {
            try {
                val cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
                cameraManager.cameraIdList.firstOrNull { id ->
                    cameraManager.getCameraCharacteristics(id)
                        .get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
                }?.let { cameraId ->
                    cameraManager.setTorchMode(cameraId, false)
                }
            } catch (e: Exception) {}
        }

        // Deallocate Monitors
        batteryMonitor?.stop()
        audioMonitor?.stop()

        // Remove Display Overlays
        removeOverlayIfExists()

        // Shut down HandlerThreads
        sensorThread?.quitSafely()
        gestureThread?.quitSafely()

        super.onDestroy()
    }
}
