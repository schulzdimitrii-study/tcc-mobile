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
import com.pedroaba.tccmobile.auth.model.UserSession
import com.pedroaba.tccmobile.features.auth.screens.AuthLoadingScreen
import com.pedroaba.tccmobile.auth.AuthManager
import com.pedroaba.tccmobile.auth.AuthResult
import com.pedroaba.tccmobile.auth.AuthState
import com.pedroaba.tccmobile.backend.http.BackendHttpClient
import com.pedroaba.tccmobile.backend.online.OnlineSessionRepository
import com.pedroaba.tccmobile.backend.online.RemoteSessionState
import com.pedroaba.tccmobile.backend.online.SessionApi
import com.pedroaba.tccmobile.backend.online.StompWebSocketClient
import com.pedroaba.tccmobile.features.auth.screens.LoginScreen
import com.pedroaba.tccmobile.features.auth.screens.SignupScreen
import com.pedroaba.tccmobile.features.game.screens.GameScreen
import com.pedroaba.tccmobile.game.models.GameSnapshot
import com.pedroaba.tccmobile.features.home.screens.HomeScreen
import com.pedroaba.tccmobile.telemetry.service.AndroidTelemetryRuntime
import com.pedroaba.tccmobile.telemetry.service.TelemetryForegroundService
import com.pedroaba.tccmobile.telemetry.service.TelemetryRuntimeProvider
import com.pedroaba.tccmobile.ui.components.navigation.FloatingTabBar
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

private const val TAG = "MainActivity"

class MainActivity : ComponentActivity() {

    private lateinit var authManager: AuthManager
    private lateinit var telemetryRuntime: AndroidTelemetryRuntime
    private lateinit var onlineSessionRepository: OnlineSessionRepository
    private lateinit var backendHttpClient: BackendHttpClient
    
    private var hasLocationPermission by mutableStateOf(false)
    private var hasNotificationPermission by mutableStateOf(false)
    private var pendingTelemetryStart by mutableStateOf(false)
    private var isSubmitting by mutableStateOf(false)
    private var authErrorMessage by mutableStateOf<String?>(null)
    private var currentTab by mutableStateOf("home")
    private var showWatchModal by mutableStateOf(false)
    private var latestGameSnapshot by mutableStateOf(GameSnapshot())

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
        backendHttpClient = BackendHttpClient()
        onlineSessionRepository = OnlineSessionRepository(
            sessionApi = SessionApi(backendHttpClient),
            stompWebSocketClient = StompWebSocketClient()
        )
        syncTelemetryAvailability()
        bindTelemetryToOnlineSession()

        Log.d(TAG, "App started, checking auth state...")

        setContent {
            App {
                val authState by authManager.authState.collectAsStateWithLifecycle()

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    when (val state = authState) {
                        is AuthState.Loading -> {
                            Log.d(TAG, "Auth state: Loading")
                            AuthLoadingScreen()
                        }
                        is AuthState.Unauthenticated -> {
                            Log.d(TAG, "Auth state: Unauthenticated")
                            AuthScreenContent(
                                isSubmitting = isSubmitting,
                                authErrorMessage = authErrorMessage,
                                onSubmittingChanged = { isSubmitting = it },
                                onLoginRequested = { email, password, _ ->
                                    lifecycleScope.launch {
                                        authErrorMessage = null
                                        val result = authManager.login(email, password)
                                        isSubmitting = false
                                        if (result is AuthResult.Error) {
                                            Log.e(TAG, "Login error: ${result.message}")
                                            authErrorMessage = result.message
                                        }
                                    }
                                },
                                onSignupRequested = { email, name, password, birthDate, height, weight ->
                                    lifecycleScope.launch {
                                        authErrorMessage = null
                                        val result = authManager.register(email, name, password, birthDate, height, weight)
                                        isSubmitting = false
                                        if (result is AuthResult.Error) {
                                            Log.e(TAG, "Register error: ${result.message}")
                                            authErrorMessage = result.message
                                        }
                                    }
                                }
                            )
                        }
                        is AuthState.Authenticated -> {
                            Log.d(TAG, "Auth state: Authenticated as ${state.session.email}")
                            val remoteSessionState by onlineSessionRepository.state.collectAsStateWithLifecycle()
                            MainAppNavigation(
                                session = state.session,
                                telemetryStateFlow = telemetryRuntime.repository.telemetryState,
                                remoteSessionState = remoteSessionState,
                                onStartTelemetry = ::ensurePermissionsAndStartTelemetry,
                                onStopTelemetry = ::stopTelemetrySession
                            )
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun AuthScreenContent(
        isSubmitting: Boolean,
        authErrorMessage: String?,
        onSubmittingChanged: (Boolean) -> Unit,
        onLoginRequested: (email: String, password: String, keepConnected: Boolean) -> Unit,
        onSignupRequested: (email: String, name: String, password: String, birthDate: String?, height: Double?, weight: Double?) -> Unit
    ) {
        var isLoginMode by remember { mutableStateOf(true) }
        
        if (isLoginMode) {
            LoginScreen(
                isSubmitting = isSubmitting,
                backendError = authErrorMessage,
                onLoginRequested = { email, password, keepConnected ->
                    onSubmittingChanged(true)
                    onLoginRequested(email, password, keepConnected)
                },
                onCreateProfileRequested = { isLoginMode = false }
            )
        } else {
            SignupScreen(
                isSubmitting = isSubmitting,
                backendError = authErrorMessage,
                onSignupRequested = { email, birthDate, name, _, height, weight, password ->
                    onSubmittingChanged(true)
                    onSignupRequested(email, name, password, birthDate, height?.toDouble(), weight?.toDouble())
                },
                onNavigateToLogin = { isLoginMode = true }
            )
        }
    }

    @Composable
    private fun MainAppNavigation(
        session: UserSession,
        telemetryStateFlow: kotlinx.coroutines.flow.StateFlow<com.pedroaba.tccmobile.game.telemetry.model.TelemetryState>,
        remoteSessionState: RemoteSessionState,
        onStartTelemetry: () -> Unit,
        onStopTelemetry: () -> Unit
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            when (currentTab) {
                "home" -> {
                    if (showWatchModal) {
                        com.pedroaba.tccmobile.features.home.screens.HomeScreenWithModal(
                            onDismissModal = { showWatchModal = false },
                            onTabSelected = { currentTab = it }
                        )
                    } else {
                        com.pedroaba.tccmobile.features.home.screens.HomeScreen(
                            userName = session.name.substringBefore(" ").ifBlank { session.name },
                            onStartRun = onStartTelemetry,
                            onViewProfile = { currentTab = "perfil" },
                            onShowWatchModal = { showWatchModal = true },
                            onTabSelected = { currentTab = it }
                        )
                    }
                }
                "rank" -> {
                    com.pedroaba.tccmobile.features.ranking.screens.RankingScreen(
                        remoteSessionState = remoteSessionState,
                        currentUserName = session.name,
                        onTabSelected = { currentTab = it }
                    )
                }
                "perfil" -> {
                    com.pedroaba.tccmobile.features.profile.screens.ProfileScreen(
                        userName = session.name,
                        userEmail = session.email,
                        onEditProfile = { currentTab = "edit_profile" },
                        onTabSelected = { currentTab = it }
                    )
                }
                "social" -> {
                    com.pedroaba.tccmobile.features.social.screens.FriendsListScreen(
                        onAddFriends = { currentTab = "add_friends" },
                        onHistory = { currentTab = "history" },
                        onTabSelected = { currentTab = it }
                    )
                }
                "edit_profile" -> {
                    com.pedroaba.tccmobile.features.profile.screens.EditProfileScreen(
                        onBack = { currentTab = "perfil" },
                        onTabSelected = { currentTab = it }
                    )
                }
                "add_friends" -> {
                    com.pedroaba.tccmobile.features.social.screens.AddFriendsScreen(
                        onBack = { currentTab = "social" },
                        onTabSelected = { currentTab = it }
                    )
                }
                "history" -> {
                    com.pedroaba.tccmobile.features.history.screens.HistoryScreen(
                        onBack = { currentTab = "social" },
                        onWatchConnection = { currentTab = "watch_connection" },
                        onTabSelected = { currentTab = it }
                    )
                }
                "watch_connection" -> {
                    com.pedroaba.tccmobile.features.watch.screens.WatchConnectionStatesScreen(
                        onBack = { currentTab = "history" },
                        onWatchDisconnected = { currentTab = "watch_disconnected" }
                    )
                }
                "watch_disconnected" -> {
                    com.pedroaba.tccmobile.features.watch.screens.WatchDisconnectedScreen(
                        onBack = { currentTab = "history" }
                    )
                }
                "game" -> {
                    GameScreen(
                        telemetryStateFlow = telemetryStateFlow,
                        remoteSessionState = remoteSessionState,
                        currentTimeMsProvider = { SystemClock.elapsedRealtime() },
                        onSnapshotChanged = { latestGameSnapshot = it },
                        onStartTelemetrySession = onStartTelemetry,
                        onStopTelemetrySession = onStopTelemetry
                    )
                }
                else -> {
                    com.pedroaba.tccmobile.features.home.screens.HomeScreen(
                        userName = session.name.substringBefore(" ").ifBlank { session.name },
                        onStartRun = onStartTelemetry,
                        onViewProfile = { currentTab = "perfil" },
                        onShowWatchModal = { showWatchModal = true },
                        onTabSelected = { currentTab = it }
                    )
                }
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
        currentTab = "game"
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
        val authenticatedSession = (authManager.authState.value as? AuthState.Authenticated)?.session
        if (authenticatedSession == null) {
            pendingTelemetryStart = false
            return
        }

        lifecycleScope.launch {
            val result = onlineSessionRepository.startSession(authenticatedSession.token)
            if (result.isSuccess) {
                pendingTelemetryStart = false
                telemetryRuntime.repository.startSession()
                TelemetryForegroundService.start(this@MainActivity)
            } else {
                pendingTelemetryStart = false
                Log.e(TAG, "Failed to start online session", result.exceptionOrNull())
            }
        }
    }

    private fun stopTelemetrySession() {
        val authenticatedSession = (authManager.authState.value as? AuthState.Authenticated)?.session
        if (authenticatedSession == null) {
            stopLocalTelemetrySession()
            return
        }
        if (onlineSessionRepository.state.value.sessionId == null) {
            stopLocalTelemetrySession()
            return
        }

        lifecycleScope.launch {
            val result = onlineSessionRepository.endSession(authenticatedSession.token)
            if (result.isSuccess) {
                stopLocalTelemetrySession()
            } else {
                Log.e(TAG, "Failed to end online session", result.exceptionOrNull())
            }
        }
    }

    private fun stopLocalTelemetrySession() {
        pendingTelemetryStart = false
        telemetryRuntime.repository.stopSession()
        TelemetryForegroundService.stop(this)
    }

    private fun bindTelemetryToOnlineSession() {
        lifecycleScope.launch {
            combine(authManager.authState, telemetryRuntime.repository.telemetryState) { authState, telemetryState ->
                authState to telemetryState
            }.collect { (authState, telemetryState) ->
                val session = (authState as? AuthState.Authenticated)?.session ?: return@collect
                onlineSessionRepository.sendTelemetry(
                    userId = session.userId,
                    telemetryState = telemetryState,
                    snapshot = latestGameSnapshot
                )
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        onlineSessionRepository.clear()
        backendHttpClient.close()
        authManager.close()
    }
}
