package com.michael.notchcommand.service

import android.accessibilityservice.AccessibilityService
import android.util.Log
import android.view.accessibility.AccessibilityEvent

class NotchAccessibilityService : AccessibilityService() {

    companion object {
        @Volatile
        var instance: NotchAccessibilityService? = null
            private set

        val isServiceRunning: Boolean
            get() = instance != null
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.d("NotchAccessibilityService", "Accessibility service connected")
        instance = this
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Not tracking accessibility events for zero data and zero battery impact
    }

    override fun onInterrupt() {
        Log.d("NotchAccessibilityService", "Accessibility service interrupted")
    }

    override fun onUnbind(intent: android.content.Intent?): Boolean {
        Log.d("NotchAccessibilityService", "Accessibility service unbound")
        instance = null
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
    }

    fun performAction(actionId: Int): Boolean {
        Log.d("NotchAccessibilityService", "Performing global action ID: $actionId")
        return performGlobalAction(actionId)
    }
}
