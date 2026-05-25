package com.example.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.GestureConfigStore
import com.example.domain.model.GestureAction
import com.example.domain.model.GestureConfig
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(private val configStore: GestureConfigStore) : ViewModel() {

    val configState: StateFlow<GestureConfig> = configStore.configFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = GestureConfig()
        )

    val notchXState: StateFlow<Float> = configStore.notchXFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0.5f
        )

    val notchYState: StateFlow<Float> = configStore.notchYFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 35f
        )

    val notchRadiusState: StateFlow<Float> = configStore.notchRadiusFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 35f
        )

    val notchThicknessState: StateFlow<Float> = configStore.notchThicknessFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 5f
        )

    val rgbModeState: StateFlow<Boolean> = configStore.rgbModeFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )

    fun updateSingleTap(action: GestureAction) {
        viewModelScope.launch {
            configStore.updateSingleTap(action)
        }
    }

    fun updateDoubleTap(action: GestureAction) {
        viewModelScope.launch {
            configStore.updateDoubleTap(action)
        }
    }

    fun updateLongPress(action: GestureAction) {
        viewModelScope.launch {
            configStore.updateLongPress(action)
        }
    }

    fun updateNotchX(x: Float) {
        viewModelScope.launch {
            configStore.updateNotchX(x)
        }
    }

    fun updateNotchY(y: Float) {
        viewModelScope.launch {
            configStore.updateNotchY(y)
        }
    }

    fun updateNotchRadius(radius: Float) {
        viewModelScope.launch {
            configStore.updateNotchRadius(radius)
        }
    }

    fun updateNotchThickness(thickness: Float) {
        viewModelScope.launch {
            configStore.updateNotchThickness(thickness)
        }
    }

    fun updateRgbMode(enabled: Boolean) {
        viewModelScope.launch {
            configStore.updateRgbMode(enabled)
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
