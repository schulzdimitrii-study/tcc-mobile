package com.pedroaba.tccmobile

import android.Manifest
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Bundle
import android.os.Build
import android.provider.Settings
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
import androidx.compose.runtime.LaunchedEffect
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
import com.pedroaba.tccmobile.backend.http.isBackendAuthFailure
import com.pedroaba.tccmobile.backend.online.OnlineSessionRepository
import com.pedroaba.tccmobile.backend.online.RemoteSessionState
import com.pedroaba.tccmobile.backend.online.RemoteSessionStatus
import com.pedroaba.tccmobile.backend.online.SessionApi
import com.pedroaba.tccmobile.backend.online.StompWebSocketClient
import com.pedroaba.tccmobile.backend.online.toSessionConfig
import com.pedroaba.tccmobile.backend.online.UserApi
import com.pedroaba.tccmobile.backend.online.UserProfileState
import com.pedroaba.tccmobile.features.auth.screens.LoginScreen
import com.pedroaba.tccmobile.features.auth.screens.SignupScreen
import com.pedroaba.tccmobile.features.game.screens.GameScreen
import com.pedroaba.tccmobile.game.models.GameSnapshot
import com.pedroaba.tccmobile.game.models.SessionConfig
import com.pedroaba.tccmobile.game.telemetry.model.TelemetrySessionStatus
import com.pedroaba.tccmobile.features.home.screens.HomeScreen
import com.pedroaba.tccmobile.telemetry.service.AndroidTelemetryRuntime
import com.pedroaba.tccmobile.telemetry.service.TelemetryForegroundService
import com.pedroaba.tccmobile.telemetry.service.TelemetryRuntimeProvider
import com.pedroaba.tccmobile.telemetry.wear.WatchGameProgressBridge
import com.pedroaba.tccmobile.ui.components.navigation.FloatingTabBar
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

private const val TAG = "MainActivity"
private const val CONNECTION_LOST_MESSAGE = "Sem conexão com a internet. Jogo encerrado."

class MainActivity : ComponentActivity() {

    private lateinit var authManager: AuthManager
    private lateinit var telemetryRuntime: AndroidTelemetryRuntime
    private lateinit var onlineSessionRepository: OnlineSessionRepository
    private lateinit var backendHttpClient: BackendHttpClient
    private lateinit var userApi: UserApi
    private lateinit var watchGameProgressBridge: WatchGameProgressBridge
    private lateinit var connectivityManager: ConnectivityManager
    private var networkCallback: ConnectivityManager.NetworkCallback? = null
    
    private var hasLocationPermission by mutableStateOf(false)
    private var hasNotificationPermission by mutableStateOf(false)
    private var pendingTelemetryStart by mutableStateOf(false)
    private var isSubmitting by mutableStateOf(false)
    private var authErrorMessage by mutableStateOf<String?>(null)
    private var currentTab by mutableStateOf("home")
    private var showWatchModal by mutableStateOf(false)
    private var latestGameSnapshot by mutableStateOf(GameSnapshot())
    private var userProfileState by mutableStateOf(UserProfileState())
    private var connectionErrorMessage by mutableStateOf<String?>(null)

    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        syncTelemetryAvailability()
        if (hasLocationPermission && pendingTelemetryStart) {
            continuePendingTelemetryStart()
        } else if (pendingTelemetryStart) {
            pendingTelemetryStart = false
            currentTab = "telemetry_permissions"
        }
    }

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        syncTelemetryAvailability()
        if (hasNotificationPermission && pendingTelemetryStart) {
            continuePendingTelemetryStart()
        } else if (pendingTelemetryStart) {
            pendingTelemetryStart = false
            currentTab = "telemetry_permissions"
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        authManager = AuthManager(this)
        telemetryRuntime = TelemetryRuntimeProvider.get(this)
        watchGameProgressBridge = WatchGameProgressBridge(this)
        backendHttpClient = BackendHttpClient()
        userApi = UserApi(backendHttpClient)
        connectivityManager = getSystemService(ConnectivityManager::class.java)
        onlineSessionRepository = OnlineSessionRepository(
            sessionApi = SessionApi(backendHttpClient),
            stompWebSocketClient = StompWebSocketClient()
        )
        syncTelemetryAvailability()
        bindTelemetryToOnlineSession()
        observeRemoteSessionFailures()
        registerNetworkMonitor()

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
                            LaunchedEffect(state.session.token) {
                                loadHordesForSession(state.session)
                                loadUserProfile(state.session)
                            }
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

    override fun onResume() {
        super.onResume()
        if (::telemetryRuntime.isInitialized) {
            syncTelemetryAvailability()
            stopTelemetryIfRequirementsWereRevoked()
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
                onSignupRequested = { email, birthDate, name, height, weight, password ->
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
        val navTelemetryState by telemetryStateFlow.collectAsStateWithLifecycle()
        val shouldShowTabBar = currentTab != "game" ||
            navTelemetryState.session.status != TelemetrySessionStatus.RUNNING

        Box(modifier = Modifier.fillMaxSize()) {
            when (currentTab) {
                "home" -> {
                    if (showWatchModal) {
                        com.pedroaba.tccmobile.features.home.screens.HomeScreenWithModal(
                            userName = session.name.substringBefore(" ").ifBlank { session.name },
                            currentUserId = session.userId,
                            remoteSessionState = remoteSessionState,
                            connectionErrorMessage = connectionErrorMessage,
                            onDismissModal = { showWatchModal = false },
                            onTabSelected = { currentTab = it }
                        )
                    } else {
                        com.pedroaba.tccmobile.features.home.screens.HomeScreen(
                            userName = session.name.substringBefore(" ").ifBlank { session.name },
                            currentUserId = session.userId,
                            remoteSessionState = remoteSessionState,
                            hordes = remoteSessionState.hordes,
                            selectedHordeId = remoteSessionState.selectedHorde?.id,
                            hordeCatalogStatus = remoteSessionState.hordeCatalogStatus,
                            hordeErrorMessage = remoteSessionState.errorMessage,
                            onHordeSelected = onlineSessionRepository::selectHorde,
                            onReloadHordes = { loadHordesForSession(session) },
                            onStartRun = onStartTelemetry,
                            onViewProfile = { currentTab = "perfil" },
                            onShowWatchModal = { showWatchModal = true },
                            onTabSelected = { currentTab = it }
                        )
                    }
                }
                "rank" -> {
                    LaunchedEffect(remoteSessionState.sessionId, session.token, session.userId) {
                        loadLeaderboardForSession(session)
                    }
                    com.pedroaba.tccmobile.features.ranking.screens.RankingScreen(
                        remoteSessionState = remoteSessionState,
                        currentUserId = session.userId,
                        currentUserName = session.name,
                        onTabSelected = { currentTab = it }
                    )
                }
                "perfil" -> {
                    com.pedroaba.tccmobile.features.profile.screens.ProfileScreen(
                        userName = session.name,
                        userEmail = session.email,
                        currentUserId = session.userId,
                        remoteSessionState = remoteSessionState,
                        onEditProfile = { currentTab = "edit_profile" },
                        onTabSelected = { currentTab = it }
                    )
                }
                "edit_profile" -> {
                    com.pedroaba.tccmobile.features.profile.screens.EditProfileScreen(
                        userId = session.userId,
                        userName = session.name,
                        userEmail = session.email,
                        userProfileState = userProfileState,
                        onSaveProfile = { request ->
                            updateUserProfile(session, request)
                        },
                        onBack = { currentTab = "perfil" },
                        onTabSelected = { currentTab = it }
                    )
                }
                "telemetry_permissions" -> {
                    val telemetryState by telemetryStateFlow.collectAsStateWithLifecycle()
                    com.pedroaba.tccmobile.features.profile.screens.TelemetryPermissionsScreen(
                        telemetryState = telemetryState,
                        hasNotificationPermission = hasNotificationPermission,
                        onRequestLocationPermission = ::requestTelemetryLocationPermission,
                        onRequestNotificationPermission = ::requestTelemetryNotificationPermission,
                        onOpenAppSettings = ::openAppSettings,
                        onOpenLocationSettings = ::openLocationSettings,
                        onRefreshTelemetryAvailability = ::syncTelemetryAvailability,
                        onBack = { currentTab = "perfil" }
                    )
                }
                "history" -> {
                    com.pedroaba.tccmobile.features.history.screens.HistoryScreen(
                        onBack = { currentTab = "perfil" },
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
                        currentUserId = session.userId,
                        connectionErrorMessage = connectionErrorMessage,
                        gameSessionConfig = remoteSessionState.selectedHorde?.toSessionConfig()
                            ?: SessionConfig(),
                        shouldAutoStartSession = pendingTelemetryStart,
                        currentTimeMsProvider = { SystemClock.elapsedRealtime() },
                        onSnapshotChanged = { latestGameSnapshot = it },
                        onStartTelemetrySession = ::startTelemetryAfterSceneLoaded,
                        onStopTelemetrySession = onStopTelemetry
                    )
                }
                else -> {
                    com.pedroaba.tccmobile.features.home.screens.HomeScreen(
                        userName = session.name.substringBefore(" ").ifBlank { session.name },
                        currentUserId = session.userId,
                        remoteSessionState = remoteSessionState,
                        hordes = remoteSessionState.hordes,
                        selectedHordeId = remoteSessionState.selectedHorde?.id,
                        hordeCatalogStatus = remoteSessionState.hordeCatalogStatus,
                        hordeErrorMessage = remoteSessionState.errorMessage,
                        onHordeSelected = onlineSessionRepository::selectHorde,
                        onReloadHordes = { loadHordesForSession(session) },
                        onStartRun = onStartTelemetry,
                        onViewProfile = { currentTab = "perfil" },
                        onShowWatchModal = { showWatchModal = true },
                        onTabSelected = { currentTab = it }
                    )
                }
            }

            if (shouldShowTabBar) {
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
        if (onlineSessionRepository.state.value.selectedHorde == null) {
            currentTab = "home"
            loadAuthenticatedHordesIfPossible()
            return
        }
        currentTab = "game"
        pendingTelemetryStart = true
    }

    private fun startTelemetryAfterSceneLoaded() {
        if (!pendingTelemetryStart) {
            pendingTelemetryStart = true
        }
        continuePendingTelemetryStart()
    }

    private fun continuePendingTelemetryStart() {
        syncTelemetryAvailability()
        if (!hasLocationPermission) {
            requestLocationPermissions()
            return
        }
        if (!hasNotificationPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            return
        }
        if (!isTelemetryCollectionAllowed()) {
            pendingTelemetryStart = false
            currentTab = "telemetry_permissions"
            return
        }

        startTelemetrySession()
    }

    private fun requestTelemetryLocationPermission() {
        pendingTelemetryStart = false
        if (!hasLocationPermission) {
            requestLocationPermissions()
            return
        }
        syncTelemetryAvailability()
    }

    private fun requestTelemetryNotificationPermission() {
        pendingTelemetryStart = false
        if (!hasNotificationPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            return
        }

        syncTelemetryAvailability()
    }

    private fun openAppSettings() {
        pendingTelemetryStart = false
        startActivity(
            Intent(
                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.fromParts("package", packageName, null)
            )
        )
    }

    private fun openLocationSettings() {
        pendingTelemetryStart = false
        startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
    }

    private fun isTelemetryCollectionAllowed(): Boolean {
        val availability = telemetryRuntime.repository.telemetryState.value.availability
        return hasLocationPermission &&
            hasNotificationPermission &&
            availability.isLocationEnabled &&
            availability.hasMotionSensor
    }

    private fun stopTelemetryIfRequirementsWereRevoked() {
        val status = telemetryRuntime.repository.telemetryState.value.session.status
        val telemetryRunning = status == com.pedroaba.tccmobile.game.telemetry.model.TelemetrySessionStatus.RUNNING ||
            status == com.pedroaba.tccmobile.game.telemetry.model.TelemetrySessionStatus.PAUSED

        if (telemetryRunning && !isTelemetryCollectionAllowed()) {
            stopTelemetrySession()
            currentTab = "telemetry_permissions"
        }
    }

    private fun loadHordesForSession(session: UserSession) {
        lifecycleScope.launch {
            val result = onlineSessionRepository.loadHordes(session.token)
            if (result.isFailure) {
                handleBackendFailure("Failed to load hordes", result.exceptionOrNull())
            }
        }
    }

    private fun loadAuthenticatedHordesIfPossible() {
        val session = (authManager.authState.value as? AuthState.Authenticated)?.session ?: return
        loadHordesForSession(session)
    }

    private fun loadUserProfile(session: UserSession) {
        if (session.userId.isBlank()) return

        lifecycleScope.launch {
            userProfileState = userProfileState.loading()
            userApi.getUser(session.token, session.userId).fold(
                onSuccess = { profile ->
                    userProfileState = userProfileState.loaded(profile)
                },
                onFailure = { error ->
                    val message = error.message ?: "Nao foi possivel carregar o perfil."
                    userProfileState = userProfileState.failed(message)
                    handleBackendFailure("Failed to load user profile", error)
                }
            )
        }
    }

    private fun updateUserProfile(
        session: UserSession,
        request: com.pedroaba.tccmobile.backend.model.UpdateUserProfileRequest
    ) {
        if (session.userId.isBlank()) return

        lifecycleScope.launch {
            userProfileState = userProfileState.saving()
            userApi.updateUser(session.token, session.userId, request).fold(
                onSuccess = { profile ->
                    userProfileState = userProfileState.loaded(profile)
                    currentTab = "perfil"
                },
                onFailure = { error ->
                    val message = error.message ?: "Nao foi possivel salvar o perfil."
                    userProfileState = userProfileState.failed(message)
                    handleBackendFailure("Failed to update user profile", error)
                }
            )
        }
    }

    private fun startTelemetrySession() {
        syncTelemetryAvailability()
        if (!isInternetAvailable()) {
            connectionErrorMessage = CONNECTION_LOST_MESSAGE
            pendingTelemetryStart = false
            return
        }
        if (!isTelemetryCollectionAllowed()) {
            pendingTelemetryStart = false
            currentTab = "telemetry_permissions"
            return
        }

        val authenticatedSession = (authManager.authState.value as? AuthState.Authenticated)?.session
        if (authenticatedSession == null) {
            pendingTelemetryStart = false
            return
        }

        lifecycleScope.launch {
            val result = onlineSessionRepository.startSession(
                token = authenticatedSession.token,
                currentUserId = authenticatedSession.userId
            )
            if (result.isSuccess) {
                connectionErrorMessage = null
                pendingTelemetryStart = false
                loadLeaderboardForSession(authenticatedSession)
                telemetryRuntime.repository.startSession()
                TelemetryForegroundService.start(this@MainActivity)
            } else {
                pendingTelemetryStart = false
                handleBackendFailure("Failed to start online session", result.exceptionOrNull())
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

        stopLocalTelemetrySession()
        lifecycleScope.launch {
            val result = onlineSessionRepository.endSession(authenticatedSession.token)
            if (result.isFailure) {
                handleBackendFailure("Failed to end online session", result.exceptionOrNull())
            }
        }
    }

    private fun loadLeaderboardForSession(session: UserSession) {
        if (onlineSessionRepository.state.value.sessionId == null) return

        lifecycleScope.launch {
            val result = onlineSessionRepository.refreshLeaderboard(session.token, session.userId)
            if (result.isFailure) {
                handleBackendFailure("Failed to load leaderboard", result.exceptionOrNull())
            }
        }
    }

    private fun handleBackendFailure(message: String, error: Throwable?) {
        Log.e(TAG, message, error)
        if (error?.isBackendAuthFailure() == true) {
            lifecycleScope.launch {
                authManager.logout()
            }
        }
    }

    private fun stopLocalTelemetrySession(clearRemoteError: Boolean = true) {
        pendingTelemetryStart = false
        telemetryRuntime.repository.stopSession()
        if (clearRemoteError && onlineSessionRepository.state.value.status == RemoteSessionStatus.ERROR) {
            onlineSessionRepository.clearActiveSession()
        }
        TelemetryForegroundService.stop(this)
    }

    private fun registerNetworkMonitor() {
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onLost(network: Network) {
                runOnUiThread { stopActiveGameForConnectionLoss() }
            }

            override fun onUnavailable() {
                runOnUiThread { stopActiveGameForConnectionLoss() }
            }
        }
        networkCallback = callback
        connectivityManager.registerDefaultNetworkCallback(callback)
    }

    private fun observeRemoteSessionFailures() {
        lifecycleScope.launch {
            onlineSessionRepository.state.collect { state ->
                val status = telemetryRuntime.repository.telemetryState.value.session.status
                val telemetryRunning = status == TelemetrySessionStatus.RUNNING ||
                    status == TelemetrySessionStatus.PAUSED
                if (telemetryRunning && state.status == RemoteSessionStatus.ERROR) {
                    stopActiveGameForConnectionLoss()
                }
            }
        }
    }

    private fun stopActiveGameForConnectionLoss() {
        val status = telemetryRuntime.repository.telemetryState.value.session.status
        val telemetryRunning = status == TelemetrySessionStatus.RUNNING ||
            status == TelemetrySessionStatus.PAUSED
        val remoteRunning = onlineSessionRepository.state.value.sessionId != null

        if (!telemetryRunning && !remoteRunning && !pendingTelemetryStart) return

        connectionErrorMessage = CONNECTION_LOST_MESSAGE
        pendingTelemetryStart = false
        currentTab = "game"
        stopLocalTelemetrySession(clearRemoteError = false)
        onlineSessionRepository.failActiveSession(CONNECTION_LOST_MESSAGE)
    }

    private fun isInternetAvailable(): Boolean {
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    private fun bindTelemetryToOnlineSession() {
        lifecycleScope.launch {
            combine(authManager.authState, telemetryRuntime.repository.telemetryState) { authState, telemetryState ->
                authState to telemetryState
            }.collect { (authState, telemetryState) ->
                val session = (authState as? AuthState.Authenticated)?.session ?: return@collect
                onlineSessionRepository.sendTelemetry(
                    userId = session.userId,
                    telemetryState = telemetryState
                )
                watchGameProgressBridge.publish(
                    telemetryState = telemetryState,
                    snapshot = latestGameSnapshot,
                    sessionConfig = onlineSessionRepository.state.value.selectedHorde?.toSessionConfig()
                        ?: SessionConfig()
                )
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        networkCallback?.let(connectivityManager::unregisterNetworkCallback)
        onlineSessionRepository.clear()
        backendHttpClient.close()
        authManager.close()
    }
}
