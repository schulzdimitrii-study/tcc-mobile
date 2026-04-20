package com.pedroaba.tccmobile

import android.Manifest
import android.os.Bundle
import android.os.Build
import android.os.SystemClock
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.tooling.preview.Preview
import com.pedroaba.tccmobile.telemetry.service.AndroidTelemetryRuntime
import com.pedroaba.tccmobile.telemetry.service.TelemetryForegroundService
import com.pedroaba.tccmobile.telemetry.service.TelemetryRuntimeProvider

class MainActivity : ComponentActivity() {
    private lateinit var telemetryRuntime: AndroidTelemetryRuntime
    private var hasLocationPermission by mutableStateOf(false)
    private var hasNotificationPermission by mutableStateOf(false)
    private var pendingTelemetryStart by mutableStateOf(false)

    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        syncTelemetryAvailability()
        if (hasLocationPermission && pendingTelemetryStart) {
            startTelemetrySession()
        }
    }

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        syncTelemetryAvailability()
        if (hasNotificationPermission && pendingTelemetryStart) {
            startTelemetrySession()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        telemetryRuntime = TelemetryRuntimeProvider.get(this)
        syncTelemetryAvailability()

        setContent {
            App(
                telemetryStateFlow = telemetryRuntime.repository.telemetryState,
                hasLocationPermission = hasLocationPermission,
                currentTimeMsProvider = { SystemClock.elapsedRealtime() },
                onRequestLocationPermission = ::requestLocationPermissions,
                onStartTelemetrySession = ::ensurePermissionsAndStartTelemetry,
                onPauseTelemetrySession = ::pauseTelemetrySession,
                onResumeTelemetrySession = ::resumeTelemetrySession,
                onStopTelemetrySession = ::stopTelemetrySession
            )
        }
    }

    private fun requestLocationPermissions() {
        locationPermissionLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
    }

    private fun syncTelemetryAvailability() {
        hasLocationPermission = telemetryRuntime.permissionChecker.hasLocationPermission()
        hasNotificationPermission = telemetryRuntime.permissionChecker.hasNotificationPermission()
        telemetryRuntime.repository.refreshAvailability(hasLocationPermission)
    }

    private fun ensurePermissionsAndStartTelemetry() {
        pendingTelemetryStart = true
        if (!hasLocationPermission) {
            requestLocationPermissions()
            return
        }
        if (!hasNotificationPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            return
        }

        startTelemetrySession()
    }

    private fun startTelemetrySession() {
        pendingTelemetryStart = false
        telemetryRuntime.repository.startSession()
        TelemetryForegroundService.start(this)
    }

    private fun pauseTelemetrySession() {
        telemetryRuntime.repository.pauseSession()
    }

    private fun resumeTelemetrySession() {
        telemetryRuntime.repository.resumeSession()
        TelemetryForegroundService.start(this)
    }

    private fun stopTelemetrySession() {
        pendingTelemetryStart = false
        telemetryRuntime.repository.stopSession()
        TelemetryForegroundService.stop(this)
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}
