package com.michael.notchcommand.presentation.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.widget.Toast
import com.michael.notchcommand.domain.model.GestureAction
import com.michael.notchcommand.domain.model.GestureConfig
import com.michael.notchcommand.service.NotchAccessibilityService
import com.michael.notchcommand.service.NotchOverlayService

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    modifier: Modifier = Modifier,
    initialSingleExpanded: Boolean = false,
    initialDoubleExpanded: Boolean = false,
    initialLongExpanded: Boolean = false,
    scrollState: ScrollState = rememberScrollState(),
    leftScrollState: ScrollState = rememberScrollState(),
    rightScrollState: ScrollState = rememberScrollState()
) {
    val context = LocalContext.current

    // Observe flow states
    val configState by viewModel.configState.collectAsState()
    val notchX by viewModel.notchXState.collectAsState()
    val notchY by viewModel.notchYState.collectAsState()
    val notchRadius by viewModel.notchRadiusState.collectAsState()
    val notchThickness by viewModel.notchThicknessState.collectAsState()
    val rgbMode by viewModel.rgbModeState.collectAsState()

    // Permissions active state (polled)
    var isOverlayGranted by remember { mutableStateOf(Settings.canDrawOverlays(context)) }
    var isAccessibilityGranted by remember { mutableStateOf(NotchAccessibilityService.isServiceRunning) }
    var isServiceRunning by remember { mutableStateOf(NotchOverlayService.isServiceRunning) }

    // Dropdown expanded states
    var singleExpanded by remember { mutableStateOf(initialSingleExpanded) }
    var doubleExpanded by remember { mutableStateOf(initialDoubleExpanded) }
    var longExpanded by remember { mutableStateOf(initialLongExpanded) }

    // Local trigger to re-poll state when user returns
    LaunchedEffect(Unit) {
        while (true) {
            isOverlayGranted = Settings.canDrawOverlays(context)
            isAccessibilityGranted = NotchAccessibilityService.isServiceRunning
            isServiceRunning = NotchOverlayService.isServiceRunning
            kotlinx.coroutines.delay(1500) // check every 1.5s
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "NotchCommand",
                        fontFamily = FontFamily.SansSerif,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        BoxWithConstraints(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            val isWideScreen = maxWidth > 640.dp

            if (isWideScreen) {
                // Wide / Landscape view (side panels)
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(leftScrollState),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        ServiceControlCard(
                            isOverlayGranted = isOverlayGranted,
                            isAccessibilityActive = isAccessibilityGranted,
                            isServiceRunning = isServiceRunning,
                            onToggleService = {
                                handleServiceToggle(context, isOverlayGranted, isServiceRunning)
                            },
                            onRequestOverlay = { requestOverlayPermission(context) },
                            onRequestAccessibility = { requestAccessibilityPermission(context) }
                        )

                        CalibrationPanel(
                            notchX = notchX,
                            notchY = notchY,
                            notchRadius = notchRadius,
                            notchThickness = notchThickness,
                            rgbMode = rgbMode,
                            onXChange = viewModel::updateNotchX,
                            onYChange = viewModel::updateNotchY,
                            onRadiusChange = viewModel::updateNotchRadius,
                            onThicknessChange = viewModel::updateNotchThickness,
                            onRgbToggle = viewModel::updateRgbMode
                        )
                    }

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rightScrollState),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        GestureSettingsPanel(
                            config = configState,
                            singleExpanded = singleExpanded,
                            doubleExpanded = doubleExpanded,
                            longExpanded = longExpanded,
                            onSingleExpandChange = { singleExpanded = it },
                            onDoubleExpandChange = { doubleExpanded = it },
                            onLongExpandChange = { longExpanded = it },
                            onSelectSingle = viewModel::updateSingleTap,
                            onSelectDouble = viewModel::updateDoubleTap,
                            onSelectLong = viewModel::updateLongPress
                        )

                        QuickInfoPanel()
                    }
                }
            } else {
                // Compact phone View
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp)
                        .verticalScroll(scrollState),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    ServiceControlCard(
                        isOverlayGranted = isOverlayGranted,
                        isAccessibilityActive = isAccessibilityGranted,
                        isServiceRunning = isServiceRunning,
                        onToggleService = {
                            handleServiceToggle(context, isOverlayGranted, isServiceRunning)
                        },
                        onRequestOverlay = { requestOverlayPermission(context) },
                        onRequestAccessibility = { requestAccessibilityPermission(context) }
                    )

                    CalibrationPanel(
                        notchX = notchX,
                        notchY = notchY,
                        notchRadius = notchRadius,
                        notchThickness = notchThickness,
                        rgbMode = rgbMode,
                        onXChange = viewModel::updateNotchX,
                        onYChange = viewModel::updateNotchY,
                        onRadiusChange = viewModel::updateNotchRadius,
                        onThicknessChange = viewModel::updateNotchThickness,
                        onRgbToggle = viewModel::updateRgbMode
                    )

                    GestureSettingsPanel(
                        config = configState,
                        singleExpanded = singleExpanded,
                        doubleExpanded = doubleExpanded,
                        longExpanded = longExpanded,
                        onSingleExpandChange = { singleExpanded = it },
                        onDoubleExpandChange = { doubleExpanded = it },
                        onLongExpandChange = { longExpanded = it },
                        onSelectSingle = viewModel::updateSingleTap,
                        onSelectDouble = viewModel::updateDoubleTap,
                        onSelectLong = viewModel::updateLongPress
                    )

                    QuickInfoPanel()
                    
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}

@Composable
fun ServiceControlCard(
    isOverlayGranted: Boolean,
    isAccessibilityActive: Boolean,
    isServiceRunning: Boolean,
    onToggleService: () -> Unit,
    onRequestOverlay: () -> Unit,
    onRequestAccessibility: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("service_control_card"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "System Overlay Service",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (isServiceRunning) "Running and intercepting" else "Service offline",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isServiceRunning) Color(0x9913E280) else Color(0x99FF3D00)
                    )
                }

                Switch(
                    checked = isServiceRunning,
                    onCheckedChange = { onToggleService() },
                    modifier = Modifier.testTag("service_status_switch")
                )
            }

            Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

            // Permission status tokens
            Text(
                text = "Required Permissions",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            // 1. Overlay Display Permission
            PermissionIndicatorRow(
                title = "Draw Over Apps (Overlay Permission)",
                description = "Required to render the interactive glowing status ring around the front camera.",
                isGranted = isOverlayGranted,
                onRequest = onRequestOverlay
            )

            // 2. Accessibility Gesture Interface
            PermissionIndicatorRow(
                title = "Accessibility Gestures (Optional)",
                description = "Allows triggering system screenshots, back action, notification pull downs, and home actions.",
                isGranted = isAccessibilityActive,
                onRequest = onRequestAccessibility
            )
        }
    }
}

@Composable
fun PermissionIndicatorRow(
    title: String,
    description: String,
    isGranted: Boolean,
    onRequest: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.4f),
                shape = RoundedCornerShape(14.dp)
            )
            .clickable(onClick = onRequest)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(
                    color = if (isGranted) Color(0x2213E280) else Color(0x22FFA500),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isGranted) Icons.Default.Check else Icons.Default.Warning,
                contentDescription = null,
                tint = if (isGranted) Color(0xFF13E280) else Color(0xFFFFA500),
                modifier = Modifier.size(18.dp)
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
            Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun CalibrationPanel(
    notchX: Float,
    notchY: Float,
    notchRadius: Float,
    notchThickness: Float,
    rgbMode: Boolean,
    onXChange: (Float) -> Unit,
    onYChange: (Float) -> Unit,
    onRadiusChange: (Float) -> Unit,
    onThicknessChange: (Float) -> Unit,
    onRgbToggle: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(
                "Notch Calibration Panel",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                "Manually align the position and size of the status ring with your front camera punch-hole cutout.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Offset X Slider
            Column {
                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Text("Horizontal Center (X)", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                    Text("${(notchX * 100).toInt()}%", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                }
                Slider(
                    value = notchX,
                    onValueChange = onXChange,
                    valueRange = 0.05f..0.95f,
                    modifier = Modifier.testTag("slider_width_fraction")
                )
            }

            // Offset Y Slider
            Column {
                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Text("Vertical Offsets (Y)", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                    Text("${notchY.toInt()} dp", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                }
                Slider(
                    value = notchY,
                    onValueChange = onYChange,
                    valueRange = 10f..180f,
                    modifier = Modifier.testTag("slider_y_depth")
                )
            }

            // Radius Slider
            Column {
                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Text("Ring Radius (Size)", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                    Text("${notchRadius.toInt()} dp", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                }
                Slider(
                    value = notchRadius,
                    onValueChange = onRadiusChange,
                    valueRange = 15f..80f,
                    modifier = Modifier.testTag("slider_ring_radius")
                )
            }

            // Thickness Slider
            Column {
                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Text("Ring Thickness", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                    Text("${notchThickness.toInt()} dp", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                }
                Slider(
                    value = notchThickness,
                    onValueChange = onThicknessChange,
                    valueRange = 2f..15f,
                    modifier = Modifier.testTag("slider_ring_thickness")
                )
            }

            // RGB Mode Toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(12.dp)
                    )
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Dynamic Rainbow RGB Mode", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                    Text("Cycles the status ring through color spectrums", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(
                    checked = rgbMode,
                    onCheckedChange = onRgbToggle,
                    modifier = Modifier.testTag("rgb_status_switch")
                )
            }
        }
    }
}

@Composable
fun GestureSettingsPanel(
    config: GestureConfig,
    singleExpanded: Boolean,
    doubleExpanded: Boolean,
    longExpanded: Boolean,
    onSingleExpandChange: (Boolean) -> Unit,
    onDoubleExpandChange: (Boolean) -> Unit,
    onLongExpandChange: (Boolean) -> Unit,
    onSelectSingle: (GestureAction) -> Unit,
    onSelectDouble: (GestureAction) -> Unit,
    onSelectLong: (GestureAction) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(
                "Gesture Action Setup",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            // Single Tap Assigner
            GestureMapCard(
                gestureLabel = "Single Tap Gesture",
                selectedValueName = config.singleTap.name,
                isExpanded = singleExpanded,
                onExpandChange = onSingleExpandChange,
                onSelect = onSelectSingle,
                tag = "single_tap_assigner"
            )

            // Double Tap Assigner
            GestureMapCard(
                gestureLabel = "Double Tap Gesture",
                selectedValueName = config.doubleTap.name,
                isExpanded = doubleExpanded,
                onExpandChange = onDoubleExpandChange,
                onSelect = onSelectDouble,
                tag = "double_tap_assigner"
            )

            // Long Press Assigner
            GestureMapCard(
                gestureLabel = "Long Press Gesture",
                selectedValueName = config.longPress.name,
                isExpanded = longExpanded,
                onExpandChange = onLongExpandChange,
                onSelect = onSelectLong,
                tag = "long_press_assigner"
            )
        }
    }
}

@Composable
fun GestureMapCard(
    gestureLabel: String,
    selectedValueName: String,
    isExpanded: Boolean,
    onExpandChange: (Boolean) -> Unit,
    onSelect: (GestureAction) -> Unit,
    tag: String
) {
    val actions = listOf(
        GestureAction.ToggleFlashlight,
        GestureAction.OpenNotifications,
        GestureAction.TakeScreenshot,
        GestureAction.ShowQuickSettings,
        GestureAction.GoHome,
        GestureAction.GoBack,
        GestureAction.LaunchSettings,
        GestureAction.LaunchCamera,
        GestureAction.DoNothing
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.4f),
                shape = RoundedCornerShape(14.dp)
            )
            .clickable { onExpandChange(!isExpanded) }
            .padding(14.dp)
            .testTag(tag)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(gestureLabel, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(selectedValueName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            }

            Text(
                if (isExpanded) "CLOSE" else "CHANGE",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.secondary
            )
        }

        if (isExpanded) {
            Column(
                modifier = Modifier
                    .padding(top = 10.dp)
                    .fillMaxWidth()
            ) {
                Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f), modifier = Modifier.padding(bottom = 8.dp))
                actions.forEach { action ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onSelect(action)
                                onExpandChange(false)
                            }
                            .padding(vertical = 10.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = action.name,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (action.name == selectedValueName) FontWeight.Bold else FontWeight.Normal,
                            color = if (action.name == selectedValueName) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun QuickInfoPanel() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )

            Column {
                Text(
                    "Energy Saving Blueprint",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    "NotchCommand uses passive gravity hardware listeners and a micro-sample sound gating loop that turns off automatically to avoid draining battery when your device is inactive or silent.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private fun handleServiceToggle(
    context: Context,
    isOverlayGranted: Boolean,
    isServiceRunning: Boolean
) {
    if (!isOverlayGranted) {
        Toast.makeText(context, "Please grant Overlay permission first!", Toast.LENGTH_LONG).show()
        requestOverlayPermission(context)
        return
    }

    val intent = Intent(context, NotchOverlayService::class.java)
    if (isServiceRunning) {
        context.stopService(intent)
        Toast.makeText(context, "NotchCommand stopped successfully.", Toast.LENGTH_SHORT).show()
    } else {
        context.startForegroundService(intent)
        Toast.makeText(context, "NotchCommand active. Enjoy shortcuts!", Toast.LENGTH_SHORT).show()
    }
}

private fun requestOverlayPermission(context: Context) {
    if (!Settings.canDrawOverlays(context)) {
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:${context.packageName}")
        ).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
        Toast.makeText(context, "Please allow NotchCommand to display over other apps.", Toast.LENGTH_LONG).show()
    } else {
        Toast.makeText(context, "Draw over apps permission is already active!", Toast.LENGTH_SHORT).show()
    }
}

private fun requestAccessibilityPermission(context: Context) {
    val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK
    }
    context.startActivity(intent)
    Toast.makeText(context, "Please scroll and activate NotchCommand inside 'Downloaded Services/Apps'.", Toast.LENGTH_LONG).show()
}
