package com.michael.notchcommand.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.michael.notchcommand.data.local.GestureConfigStore
import com.michael.notchcommand.domain.model.GestureAction
import com.michael.notchcommand.domain.model.GestureConfig
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

open class SettingsViewModel(private val configStore: GestureConfigStore?) : ViewModel() {

    open val configState: StateFlow<GestureConfig> = configStore?.configFlow
        ?.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = GestureConfig()
        ) ?: kotlinx.coroutines.flow.MutableStateFlow(GestureConfig())

    open val notchXState: StateFlow<Float> = configStore?.notchXFlow
        ?.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0.5f
        ) ?: kotlinx.coroutines.flow.MutableStateFlow(0.5f)

    open val notchYState: StateFlow<Float> = configStore?.notchYFlow
        ?.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 35f
        ) ?: kotlinx.coroutines.flow.MutableStateFlow(35f)

    open val notchRadiusState: StateFlow<Float> = configStore?.notchRadiusFlow
        ?.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 35f
        ) ?: kotlinx.coroutines.flow.MutableStateFlow(35f)

    open val notchThicknessState: StateFlow<Float> = configStore?.notchThicknessFlow
        ?.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 5f
        ) ?: kotlinx.coroutines.flow.MutableStateFlow(5f)

    open val rgbModeState: StateFlow<Boolean> = configStore?.rgbModeFlow
        ?.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        ) ?: kotlinx.coroutines.flow.MutableStateFlow(false)

    open fun updateSingleTap(action: GestureAction) {
        viewModelScope.launch {
            configStore?.updateSingleTap(action)
        }
    }

    open fun updateDoubleTap(action: GestureAction) {
        viewModelScope.launch {
            configStore?.updateDoubleTap(action)
        }
    }

    open fun updateLongPress(action: GestureAction) {
        viewModelScope.launch {
            configStore?.updateLongPress(action)
        }
    }

    open fun updateNotchX(x: Float) {
        viewModelScope.launch {
            configStore?.updateNotchX(x)
        }
    }

    open fun updateNotchY(y: Float) {
        viewModelScope.launch {
            configStore?.updateNotchY(y)
        }
    }

    open fun updateNotchRadius(radius: Float) {
        viewModelScope.launch {
            configStore?.updateNotchRadius(radius)
        }
    }

    open fun updateNotchThickness(thickness: Float) {
        viewModelScope.launch {
            configStore?.updateNotchThickness(thickness)
        }
    }

    open fun updateRgbMode(enabled: Boolean) {
        viewModelScope.launch {
            configStore?.updateRgbMode(enabled)
        }
    }
}

class SettingsViewModelFactory(private val configStore: GestureConfigStore) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SettingsViewModel::class.java)) {
            return SettingsViewModel(configStore) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
