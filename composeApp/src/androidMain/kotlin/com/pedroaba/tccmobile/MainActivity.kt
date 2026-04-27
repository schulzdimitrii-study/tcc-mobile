package com.pedroaba.tccmobile

import android.Manifest
import android.os.Bundle
import android.os.Build
import android.os.SystemClock
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.pedroaba.tccmobile.auth.AuthManager
import com.pedroaba.tccmobile.auth.AuthResult
import com.pedroaba.tccmobile.auth.AuthState
import com.pedroaba.tccmobile.features.auth.screens.LoginScreen
import com.pedroaba.tccmobile.features.auth.screens.SignupScreen
import com.pedroaba.tccmobile.features.game.screens.GameScreen
import com.pedroaba.tccmobile.features.home.screens.HomeScreen
import com.pedroaba.tccmobile.telemetry.service.AndroidTelemetryRuntime
import com.pedroaba.tccmobile.telemetry.service.TelemetryForegroundService
import com.pedroaba.tccmobile.telemetry.service.TelemetryRuntimeProvider
import com.pedroaba.tccmobile.theme.TccMobileTheme
import com.pedroaba.tccmobile.ui.components.navigation.FloatingTabBar
import kotlinx.coroutines.launch

private const val TAG = "MainActivity"

class MainActivity : ComponentActivity() {

    private lateinit var authManager: AuthManager
    private lateinit var telemetryRuntime: AndroidTelemetryRuntime
    
    private var hasLocationPermission by mutableStateOf(false)
    private var hasNotificationPermission by mutableStateOf(false)
    private var pendingTelemetryStart by mutableStateOf(false)
    private var isSubmitting by mutableStateOf(false)
    private var currentTab by mutableStateOf("home")

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

        authManager = AuthManager(this)
        telemetryRuntime = TelemetryRuntimeProvider.get(this)
        syncTelemetryAvailability()

        Log.d(TAG, "App started, checking auth state...")

        setContent {
            TccMobileTheme {
                val authState by authManager.authState.collectAsStateWithLifecycle()

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    when (val state = authState) {
                        is AuthState.Loading -> {
                            Log.d(TAG, "Auth state: Loading")
                        }
                        is AuthState.Unauthenticated -> {
                            Log.d(TAG, "Auth state: Unauthenticated")
                            AuthScreenContent(
                                isSubmitting = isSubmitting,
                                onSubmittingChanged = { isSubmitting = it },
                                onLoginRequested = { email, password, _ ->
                                    lifecycleScope.launch {
                                        val result = authManager.login(email, password)
                                        isSubmitting = false
                                        if (result is AuthResult.Error) {
                                            Log.e(TAG, "Login error: ${result.message}")
                                        }
                                    }
                                },
                                onSignupRequested = { email, name, password, birthDate, height, weight ->
                                    lifecycleScope.launch {
                                        val result = authManager.register(email, name, password, birthDate, height, weight)
                                        isSubmitting = false
                                        if (result is AuthResult.Error) {
                                            Log.e(TAG, "Register error: ${result.message}")
                                        }
                                    }
                                }
                            )
                        }
                        is AuthState.Authenticated -> {
                            Log.d(TAG, "Auth state: Authenticated as ${state.session.email}")
                            MainGameScreenContent()
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun AuthScreenContent(
        isSubmitting: Boolean,
        onSubmittingChanged: (Boolean) -> Unit,
        onLoginRequested: (email: String, password: String, keepConnected: Boolean) -> Unit,
        onSignupRequested: (email: String, name: String, password: String, birthDate: String?, height: Double?, weight: Double?) -> Unit
    ) {
        var isLoginMode by remember { mutableStateOf(true) }
        
        if (isLoginMode) {
            LoginScreen(
                isSubmitting = isSubmitting,
                onLoginRequested = { email, password, keepConnected ->
                    onSubmittingChanged(true)
                    onLoginRequested(email, password, keepConnected)
                },
                onCreateProfileRequested = { isLoginMode = false }
            )
        } else {
            SignupScreen(
                isSubmitting = isSubmitting,
                onSignupRequested = { email, birthDate, name, _, height, weight, password ->
                    onSubmittingChanged(true)
                    onSignupRequested(email, name, password, birthDate, height?.toDouble(), weight?.toDouble())
                },
                onNavigateToLogin = { isLoginMode = true }
            )
        }
    }

    @Composable
    private fun MainGameScreenContent() {
        Box(modifier = Modifier.fillMaxSize()) {
            when (currentTab) {
                "home" -> HomeScreen(
                    onStartRun = ::ensurePermissionsAndStartTelemetry,
                    onViewProfile = { /* TODO */ },
                    onContinueMission = { /* TODO */ }
                )
                "game" -> GameScreen(
                    telemetryStateFlow = telemetryRuntime.repository.telemetryState,
                    currentTimeMsProvider = { SystemClock.elapsedRealtime() },
                    onStartTelemetrySession = ::ensurePermissionsAndStartTelemetry,
                    onStopTelemetrySession = ::stopTelemetrySession
                )
                else -> HomeScreen(
                    onStartRun = ::ensurePermissionsAndStartTelemetry,
                    onViewProfile = { /* TODO */ },
                    onContinueMission = { /* TODO */ }
                )
            }
            
            FloatingTabBar(
                currentTab = currentTab,
                onTabSelected = { tab ->
                    currentTab = tab
                    Log.d(TAG, "Tab selected: $tab")
                },
                modifier = Modifier.align(Alignment.BottomCenter)
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

    private fun stopTelemetrySession() {
        pendingTelemetryStart = false
        telemetryRuntime.repository.stopSession()
        TelemetryForegroundService.stop(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        authManager.close()
    }
}