package com.example.domain.model

sealed class NotchType {
    object PunchHole : NotchType()
    object Waterdrop : NotchType()
    object WideNotch : NotchType()
    object None : NotchType()
}

data class NotchBounds(
    val left: Int,
    val top: Int,
    val right: Int,
    val bottom: Int,
    val type: NotchType
)

sealed class GestureAction(val id: String, val name: String) {
    object TakeScreenshot : GestureAction("screenshot", "Take Screenshot")
    object ToggleFlashlight : GestureAction("flashlight", "Toggle Flashlight")
    object OpenNotifications : GestureAction("notifications", "Expand Notification Panel")
    object ShowQuickSettings : GestureAction("quick_settings", "Open Quick Settings")
    object GoHome : GestureAction("home", "Go Home")
    object GoBack : GestureAction("back", "Go Back")
    object LaunchSettings : GestureAction("settings", "Open System Settings")
    object LaunchCamera : GestureAction("camera", "Launch Camera")
    object DoNothing : GestureAction("nothing", "Do Nothing")

    companion object {
        fun fromId(id: String): GestureAction {
            return when (id) {
                "screenshot" -> TakeScreenshot
                "flashlight" -> ToggleFlashlight
                "notifications" -> OpenNotifications
                "quick_settings" -> ShowQuickSettings
                "home" -> GoHome
                "back" -> GoBack
                "settings" -> LaunchSettings
                "camera" -> LaunchCamera
                else -> DoNothing
            }
        }
    }
}

data class GestureConfig(
    val singleTap: GestureAction = GestureAction.ToggleFlashlight,
    val doubleTap: GestureAction = GestureAction.OpenNotifications,
    val longPress: GestureAction = GestureAction.TakeScreenshot
)

sealed class AppResult<out T> {
    data class Success<out T>(val data: T) : AppResult<T>()
    data class Error(val exception: Throwable, val message: String) : AppResult<Nothing>()
}
