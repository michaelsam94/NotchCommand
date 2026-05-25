package com.michael.notchcommand

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.michael.notchcommand.presentation.settings.SettingsScreen
import com.michael.notchcommand.presentation.settings.SettingsViewModel
import com.michael.notchcommand.presentation.settings.SettingsViewModelFactory
import com.michael.notchcommand.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {

    private val requestPermissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val notifGranted = results[Manifest.permission.POST_NOTIFICATIONS] ?: false
        val audioGranted = results[Manifest.permission.RECORD_AUDIO] ?: false
        Log.d("MainActivity", "Permissions updated: POST_NOTIFICATIONS=$notifGranted, RECORD_AUDIO=$audioGranted")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Edge to edge immersive displays
        enableEdgeToEdge()

        // Request notifications & mic permissions at first launch
        requestStartupPermissions()

        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = androidx.compose.material3.MaterialTheme.colorScheme.background
                ) {
                    // Extract safe centralized DataStore configStore from Custom NotchApp
                    val app = applicationContext as NotchApp
                    val viewModel: SettingsViewModel = viewModel(
                        factory = SettingsViewModelFactory(app.configStore)
                    )

                    SettingsScreen(viewModel = viewModel)
                }
            }
        }
    }

    private fun requestStartupPermissions() {
        val permissionsToRequest = mutableListOf<String>()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.RECORD_AUDIO)
        }

        if (permissionsToRequest.isNotEmpty()) {
            requestPermissionsLauncher.launch(permissionsToRequest.toTypedArray())
        }
    }
}
