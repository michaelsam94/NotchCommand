package com.michael.notchcommand.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.michael.notchcommand.domain.model.GestureAction
import com.michael.notchcommand.domain.model.GestureConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "notch_command_prefs")

class GestureConfigStore(private val context: Context) {

    companion object {
        private val KEY_SINGLE_TAP = stringPreferencesKey("single_tap_action")
        private val KEY_DOUBLE_TAP = stringPreferencesKey("double_tap_action")
        private val KEY_LONG_PRESS = stringPreferencesKey("long_press_action")

        private val KEY_NOTCH_X = floatPreferencesKey("notch_x")
        private val KEY_NOTCH_Y = floatPreferencesKey("notch_y")
        private val KEY_NOTCH_RADIUS = floatPreferencesKey("notch_radius")
        private val KEY_NOTCH_THICKNESS = floatPreferencesKey("notch_thickness")
        private val KEY_RGB_MODE = booleanPreferencesKey("rgb_mode")
    }

    val configFlow: Flow<GestureConfig> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(androidx.datastore.preferences.core.emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            val singleTapId = preferences[KEY_SINGLE_TAP] ?: GestureAction.ToggleFlashlight.id
            val doubleTapId = preferences[KEY_DOUBLE_TAP] ?: GestureAction.OpenNotifications.id
            val longPressId = preferences[KEY_LONG_PRESS] ?: GestureAction.TakeScreenshot.id

            GestureConfig(
                singleTap = GestureAction.fromId(singleTapId),
                doubleTap = GestureAction.fromId(doubleTapId),
                longPress = GestureAction.fromId(longPressId)
            )
        }

    val notchXFlow: Flow<Float> = context.dataStore.data.map { it[KEY_NOTCH_X] ?: 0.5f }
    val notchYFlow: Flow<Float> = context.dataStore.data.map { it[KEY_NOTCH_Y] ?: 35f }
    val notchRadiusFlow: Flow<Float> = context.dataStore.data.map { it[KEY_NOTCH_RADIUS] ?: 35f }
    val notchThicknessFlow: Flow<Float> = context.dataStore.data.map { it[KEY_NOTCH_THICKNESS] ?: 5f }
    val rgbModeFlow: Flow<Boolean> = context.dataStore.data.map { it[KEY_RGB_MODE] ?: false }

    suspend fun updateSingleTap(action: GestureAction) {
        context.dataStore.edit { preferences ->
            preferences[KEY_SINGLE_TAP] = action.id
        }
    }

    suspend fun updateDoubleTap(action: GestureAction) {
        context.dataStore.edit { preferences ->
            preferences[KEY_DOUBLE_TAP] = action.id
        }
    }

    suspend fun updateLongPress(action: GestureAction) {
        context.dataStore.edit { preferences ->
            preferences[KEY_LONG_PRESS] = action.id
        }
    }

    suspend fun updateNotchX(x: Float) {
        context.dataStore.edit { preferences ->
            preferences[KEY_NOTCH_X] = x
        }
    }

    suspend fun updateNotchY(y: Float) {
        context.dataStore.edit { preferences ->
            preferences[KEY_NOTCH_Y] = y
        }
    }

    suspend fun updateNotchRadius(radius: Float) {
        context.dataStore.edit { preferences ->
            preferences[KEY_NOTCH_RADIUS] = radius
        }
    }

    suspend fun updateNotchThickness(thickness: Float) {
        context.dataStore.edit { preferences ->
            preferences[KEY_NOTCH_THICKNESS] = thickness
        }
    }

    suspend fun updateRgbMode(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_RGB_MODE] = enabled
        }
    }
}
