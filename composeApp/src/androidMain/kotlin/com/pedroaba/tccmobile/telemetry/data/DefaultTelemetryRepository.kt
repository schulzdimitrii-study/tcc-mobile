package com.pedroaba.tccmobile.telemetry.data

import com.pedroaba.tccmobile.game.telemetry.model.*
import com.pedroaba.tccmobile.game.telemetry.usecase.SelectTelemetryStrategyUseCase
import com.pedroaba.tccmobile.telemetry.location.LocationTrackingService
import com.pedroaba.tccmobile.telemetry.motion.MotionSensorService
import com.pedroaba.tccmobile.telemetry.wear.WearTelemetryBridge
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

class DefaultTelemetryRepository(
    private val locationTrackingService: LocationTrackingService,
    private val motionSensorService: MotionSensorService,
    private val wearTelemetryBridge: WearTelemetryBridge,
    private val selectTelemetryStrategyUseCase: SelectTelemetryStrategyUseCase = SelectTelemetryStrategyUseCase(),
    dispatcher: CoroutineDispatcher = Dispatchers.Default
) : TelemetryRepository {
    private val scope = CoroutineScope(SupervisorJob() + dispatcher)
    private val _telemetryState = MutableStateFlow(TelemetryState())
    override val telemetryState: StateFlow<TelemetryState> = _telemetryState.asStateFlow()

    private var locationJob: Job? = null
    private var motionJob: Job? = null
    private var biofeedbackJob: Job? = null
    private var tickJob: Job? = null
    private var currentBiofeedbackSample: BiofeedbackSample? = null
    private var lastLocationPoint: LocationPoint? = null
    private var totalDistanceMeters: Double = 0.0
    private var lastResumeTimestampMs: Long? = null

    init {
        scope.launch {
            wearTelemetryBridge.isWatchConnected.collectLatest { isConnected ->
                updateStrategy(isConnected = isConnected, biofeedbackSample = currentBiofeedbackSample)
                refreshAvailability(_telemetryState.value.availability.hasLocationPermission)
            }
        }
    }

    override fun refreshAvailability(hasLocationPermission: Boolean) {
        val hasMotionSensor = motionSensorService.isSensorAvailable()
        val isLocationEnabled = locationTrackingService.isLocationEnabled()
        val hasWatch = wearTelemetryBridge.isWatchConnected.value

        val issues = buildSet {
            if (!hasLocationPermission) add(TelemetryIssue.LOCATION_PERMISSION_MISSING)
            if (!isLocationEnabled) add(TelemetryIssue.LOCATION_PROVIDER_DISABLED)
            if (!hasMotionSensor) add(TelemetryIssue.MOTION_SENSOR_UNAVAILABLE)
            if (!hasWatch) add(TelemetryIssue.WATCH_UNAVAILABLE)
        }

        _telemetryState.value = _telemetryState.value.copy(
            availability = TelemetryAvailability(
                hasLocationPermission = hasLocationPermission,
                isLocationEnabled = isLocationEnabled,
                hasMotionSensor = hasMotionSensor,
                hasWatch = hasWatch,
                issues = issues
            )
        )

        if (_telemetryState.value.session.status == TelemetrySessionStatus.RUNNING) {
            cancelCollectionJobs()
            startCollectors()
        }
    }

    override fun startSession() {
        val now = System.currentTimeMillis()
        cancelCollectionJobs()
        lastResumeTimestampMs = now
        currentBiofeedbackSample = null
        lastLocationPoint = null
        totalDistanceMeters = 0.0

        _telemetryState.value = TelemetryState(
            session = MovementSession(
                sessionId = "movement-$now",
                status = TelemetrySessionStatus.RUNNING,
                startedAtEpochMs = now,
                lastUpdatedAtEpochMs = now,
                activeDurationMs = 0L,
                totalDistanceMeters = 0.0,
                sampleCount = 0
            ),
            strategy = TelemetryStrategy.MOVEMENT_ONLY,
            availability = _telemetryState.value.availability
        )

        startCollectors()
    }

    override fun pauseSession() {
        val state = _telemetryState.value
        if (state.session.status != TelemetrySessionStatus.RUNNING) return

        val now = System.currentTimeMillis()
        _telemetryState.value = state.copy(
            session = state.session.copy(
                status = TelemetrySessionStatus.PAUSED,
                activeDurationMs = state.session.activeDurationMs + activeDurationIncrement(now),
                lastUpdatedAtEpochMs = now
            )
        )
        lastResumeTimestampMs = null
        cancelCollectionJobs()
    }

    override fun resumeSession() {
        val state = _telemetryState.value
        if (state.session.status != TelemetrySessionStatus.PAUSED) return

        val now = System.currentTimeMillis()
        lastResumeTimestampMs = now
        _telemetryState.value = state.copy(
            session = state.session.copy(
                status = TelemetrySessionStatus.RUNNING,
                lastUpdatedAtEpochMs = now
            )
        )
        startCollectors()
    }

    override fun stopSession() {
        val state = _telemetryState.value
        if (state.session.status == TelemetrySessionStatus.IDLE || state.session.status == TelemetrySessionStatus.STOPPED) {
            return
        }

        val now = System.currentTimeMillis()
        _telemetryState.value = state.copy(
            session = state.session.copy(
                status = TelemetrySessionStatus.STOPPED,
                activeDurationMs = state.session.activeDurationMs + activeDurationIncrement(now),
                lastUpdatedAtEpochMs = now
            )
        )
        lastResumeTimestampMs = null
        cancelCollectionJobs()
    }

    override fun dispose() {
        cancelCollectionJobs()
        wearTelemetryBridge.close()
        scope.cancel()
    }

    private fun startCollectors() {
        val availability = _telemetryState.value.availability
        if (availability.hasLocationPermission && availability.isLocationEnabled) {
            locationJob = scope.launch {
                locationTrackingService.locationUpdates().collectLatest { location ->
                    publishLocationPoint(location)
                }
            }
        }

        if (availability.hasMotionSensor) {
            motionJob = scope.launch {
                motionSensorService.accelerationUpdates().collectLatest { acceleration ->
                    publishAccelerationSample(acceleration)
                }
            }
        }

        biofeedbackJob = scope.launch {
            wearTelemetryBridge.biofeedbackSamples.collectLatest { sample ->
                currentBiofeedbackSample = sample
                updateStrategy(
                    isConnected = wearTelemetryBridge.isWatchConnected.value,
                    biofeedbackSample = sample
                )
            }
        }

        tickJob = scope.launch {
            while (isActive) {
                delay(1_000L)
                publishHeartbeat(System.currentTimeMillis())
            }
        }
    }

    private fun publishLocationPoint(locationPoint: LocationPoint) {
        val state = _telemetryState.value
        val distanceDeltaMeters = lastLocationPoint
            ?.let { previous -> distanceBetweenMeters(previous, locationPoint) }
            ?.takeIf { it <= MAX_LOCATION_DELTA_METERS }
            ?: 0.0
        totalDistanceMeters += distanceDeltaMeters
        lastLocationPoint = locationPoint

        _telemetryState.value = state.copy(
            latestLocationPoint = locationPoint,
            latestBiofeedbackSample = currentBiofeedbackSample,
            session = state.session.copy(
                lastUpdatedAtEpochMs = locationPoint.timestampMs,
                totalDistanceMeters = totalDistanceMeters,
                sampleCount = state.session.sampleCount + 1
            )
        )
    }

    private fun publishAccelerationSample(accelerationSample: AccelerationSample) {
        val state = _telemetryState.value
        _telemetryState.value = state.copy(
            latestAccelerationSample = accelerationSample,
            latestBiofeedbackSample = currentBiofeedbackSample,
            session = state.session.copy(
                lastUpdatedAtEpochMs = accelerationSample.timestampMs,
                sampleCount = state.session.sampleCount + 1
            )
        )
    }

    private fun publishHeartbeat(timestampMs: Long) {
        val state = _telemetryState.value
        _telemetryState.value = state.copy(
            latestBiofeedbackSample = currentBiofeedbackSample,
            session = state.session.copy(lastUpdatedAtEpochMs = timestampMs)
        )
    }

    private fun updateStrategy(isConnected: Boolean, biofeedbackSample: BiofeedbackSample?) {
        _telemetryState.value = _telemetryState.value.copy(
            strategy = selectTelemetryStrategyUseCase(
                biofeedbackSample = biofeedbackSample,
                isWatchConnected = isConnected
            )
        )
    }

    private fun activeDurationIncrement(now: Long): Long {
        val resumedAt = lastResumeTimestampMs ?: return 0L
        return (now - resumedAt).coerceAtLeast(0L)
    }

    private fun distanceBetweenMeters(start: LocationPoint, end: LocationPoint): Double {
        val earthRadiusMeters = 6_371_000.0
        val lat1 = start.latitude.toRadians()
        val lat2 = end.latitude.toRadians()
        val deltaLat = (end.latitude - start.latitude).toRadians()
        val deltaLon = (end.longitude - start.longitude).toRadians()
        val a = sin(deltaLat / 2.0).pow(2) +
            cos(lat1) * cos(lat2) * sin(deltaLon / 2.0).pow(2)
        val c = 2.0 * atan2(sqrt(a), sqrt(1.0 - a))
        return earthRadiusMeters * c
    }

    private fun Double.toRadians(): Double = this * PI / 180.0

    private fun cancelCollectionJobs() {
        locationJob?.cancel()
        motionJob?.cancel()
        biofeedbackJob?.cancel()
        tickJob?.cancel()
        locationJob = null
        motionJob = null
        biofeedbackJob = null
        tickJob = null
    }

    private companion object {
        const val MAX_LOCATION_DELTA_METERS = 150.0
    }
}
