package com.michael.notchcommand.playstore

import android.content.Context
import com.michael.notchcommand.domain.model.GestureAction
import com.michael.notchcommand.domain.model.GestureConfig
import com.michael.notchcommand.presentation.settings.SettingsViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class MockSettingsViewModel(
    override val configState: StateFlow<GestureConfig>,
    override val notchXState: StateFlow<Float>,
    override val notchYState: StateFlow<Float>,
    override val notchRadiusState: StateFlow<Float>,
    override val notchThicknessState: StateFlow<Float>,
    override val rgbModeState: StateFlow<Boolean>
) : SettingsViewModel(null) {
    override fun updateSingleTap(action: GestureAction) {}
    override fun updateDoubleTap(action: GestureAction) {}
    override fun updateLongPress(action: GestureAction) {}
    override fun updateNotchX(x: Float) {}
    override fun updateNotchY(y: Float) {}
    override fun updateNotchRadius(radius: Float) {}
    override fun updateNotchThickness(thickness: Float) {}
    override fun updateRgbMode(enabled: Boolean) {}
}

fun createSeededPlayStoreViewModel(context: Context): SettingsViewModel {
    return MockSettingsViewModel(
        configState = MutableStateFlow(
            GestureConfig(
                singleTap = GestureAction.ToggleFlashlight,
                doubleTap = GestureAction.OpenNotifications,
                longPress = GestureAction.TakeScreenshot
            )
        ),
        notchXState = MutableStateFlow(0.48f),
        notchYState = MutableStateFlow(42f),
        notchRadiusState = MutableStateFlow(36f),
        notchThicknessState = MutableStateFlow(6f),
        rgbModeState = MutableStateFlow(true)
    )
}
