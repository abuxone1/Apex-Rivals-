package com.example.game.engine

import android.content.Context
import com.example.data.local.AppDatabase
import com.example.data.local.PerformanceMetrics
import com.example.data.local.RaceResult
import com.example.game.model.AeroTrim
import com.example.game.model.AirRival
import com.example.game.model.CameraPerspective
import com.example.game.model.GameMode
import com.example.game.model.LapTelemetrySummary
import com.example.game.model.RaceFinishSummary
import com.example.game.model.RaceState
import com.example.game.model.RacingGameHudState
import com.example.game.model.RoadSegment
import com.example.game.sound.GameSoundManager
import com.example.game.landscape.BiomeType
import com.example.game.landscape.LandscapeConfig
import com.example.game.landscape.ProceduralLandscapeController
import com.example.game.landscape.SurfaceType
import com.example.game.landscape.TimeOfDay
import com.example.game.landscape.WeatherCondition
import com.example.model.BadgeEvaluator
import com.example.model.Track
import com.example.model.Vehicle
import com.example.model.VehicleType
import com.example.util.ApexHapticManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

class InteractiveRacingEngine(
  private val context: Context,
  private var vehicle: Vehicle,
  private var track: Track
) {

  private val soundManager = GameSoundManager.getInstance()
  private val hapticManager = ApexHapticManager.getInstance(context)
  private val coroutineScope = CoroutineScope(Dispatchers.Default)
  private var gameLoopJob: Job? = null
  private var countdownJob: Job? = null

  private val automotiveSafetyCallback: (Boolean) -> Unit = { isMoving ->
    if (isMoving) {
      pauseRaceForVehicleMotion("Automotive OS: Vehicle driving detected")
    }
  }

  init {
    com.example.automotive.AutomotiveSafetyManager.getInstance(context).registerDrivingCallback(automotiveSafetyCallback)
  }

  // Game Configuration
  var gameMode: GameMode = GameMode.GRAND_PRIX
  var cameraPerspective: CameraPerspective = CameraPerspective.CHASE_CAM
  var totalLaps: Int = 2

  // Player State
  var playerLaneX: Float = 0f // -1f (left track edge) to +1f (right edge)
  var playerTrackDistanceMeters: Float = 0f
  var speedKmh: Float = 0f
  var rpm: Int = vehicle.idleRpm
  var gear: Int = 1
  var isAutoGears: Boolean = true

  var throttleInput: Float = 0f
  var brakeInput: Float = 0f
  var steerInput: Float = 0f
  var isHandbrakeActive: Boolean = false

  // Nitro / ERS Boost
  var nitroCharge: Float = 1.0f // 0..1f
  var isNitroActive: Boolean = false
  var nitroDurationRemainingSec: Float = 0f

  // DRS Flap
  var isDrsActive: Boolean = false

  // Weather & Atmosphere
  var activeWeather: com.example.game.model.RaceWeather = com.example.game.model.RaceWeather.CLEAR_SUNSET
    private set

  private var prePauseRaceState: RaceState = RaceState.RACING

  // Drift & Apex Score
  var driftScore: Int = 0
  var currentDriftAngle: Float = 0f

  // Tire Compound & Strategy (v8.0.0)
  var activeTireCompound: com.example.game.model.TireCompound = com.example.game.model.TireCompound.MEDIUM
    private set
  var tireHealthPercent: Float = 100f
    private set
  var tireTempCelsius: Float = 95f
    private set
  var speedTrapKmh: Float? = null
    private set
  private var isSpeedTrapAlertTriggered: Boolean = false
  private var hasWarnedTireWear: Boolean = false
  private var hasWarnedBoxBox: Boolean = false
  private var kerbRumbleTimer: Float = 0f
  private var collisionCooldownTimer: Float = 0f
  private var handbrakeScreechTimer: Float = 0f

  // Aerodynamic Downforce Trim (v8.1.0)
  var activeAeroTrim: AeroTrim = AeroTrim.BALANCED
    private set

  // Lap Timers
  var currentLap: Int = 1
  var currentLapTimeMs: Long = 0L
  var bestLapTimeMs: Long? = null
  var lastLapTimeMs: Long? = null
  var sector1TimeMs: Long? = null
  var sector2TimeMs: Long? = null
  var sector3TimeMs: Long? = null
  var currentSector: Int = 1

  var topSpeedReachedKmh: Float = 0f
  var overtakesCount: Int = 0
  var peakLateralG: Float = 0f

  // Rivals
  private val rivals = mutableListOf<AirRival>()
  private val completedLapsHistory = mutableListOf<LapTelemetrySummary>()

  val activeVehicle: Vehicle get() = vehicle
  val activeCircuit: Track get() = track

  // Road geometry segments
  private val roadSegments = generateCircuitRoadSegments(track.totalLengthMeters)

  // Modular Procedural Landscaping System (Terrain + Biomes + Instanced Scenery + Surface Physics)
  val landscapeController: ProceduralLandscapeController = ProceduralLandscapeController(
    initialConfig = LandscapeConfig(
      seed = 20260409L,
      biome = resolveInitialBiomeForTrack(track.id),
      sceneryDensity = 1.0f,
      timeOfDay = TimeOfDay.DUSK,
      weather = WeatherCondition.CLEAR,
      vehicleType = vehicle.type
    )
  )

  var currentSurfaceType: SurfaceType = SurfaceType.ROAD
    private set

  // State flows
  private val _raceState = MutableStateFlow(RaceState.PRE_GRID)
  val raceState: StateFlow<RaceState> = _raceState.asStateFlow()

  private val _hudState = MutableStateFlow(RacingGameHudState())
  val hudState: StateFlow<RacingGameHudState> = _hudState.asStateFlow()

  private val _finishSummary = MutableStateFlow<RaceFinishSummary?>(null)
  val finishSummary: StateFlow<RaceFinishSummary?> = _finishSummary.asStateFlow()

  // Active Race Stage & Progression
  var activeStage: com.example.game.model.RaceStage = com.example.game.model.RaceStage.STAGE_2_DOWNTOWN_GP
    private set

  init {
    initRivals()
    landscapeController.regenerate(
      roadSegments = roadSegments,
      trackLengthMeters = track.totalLengthMeters.toFloat(),
      playerDistanceMeters = 0f,
      playerLateralNormalized = 0f
    )
  }

  fun setStage(newStage: com.example.game.model.RaceStage) {
    activeStage = newStage
    initRivals()
    _hudState.update { it.copy(totalRivals = newStage.playerCount) }
  }

  fun applyGameMode(newMode: GameMode) {
    gameMode = newMode
    totalLaps = when (newMode) {
      GameMode.TIME_ATTACK -> 1
      GameMode.SPEED_TRAP -> 1
      GameMode.DRIFT_CHALLENGE -> 2
      GameMode.GRAND_PRIX -> 2
    }
    _hudState.update { it.copy(totalLaps = totalLaps) }
  }

  fun setVehicleAndTrack(newVehicle: Vehicle, newTrack: Track) {
    vehicle = newVehicle
    track = newTrack
    landscapeController.setVehicleType(
      vehicleType = newVehicle.type,
      roadSegments = roadSegments,
      trackLengthMeters = newTrack.totalLengthMeters.toFloat(),
      playerDistanceMeters = 0f
    )
    landscapeController.setBiome(
      biome = resolveInitialBiomeForTrack(newTrack.id),
      roadSegments = roadSegments,
      trackLengthMeters = newTrack.totalLengthMeters.toFloat(),
      playerDistanceMeters = 0f
    )
    resetRace()
  }

  fun updateLandscapeSeed(newSeed: Long) {
    landscapeController.setSeed(
      newSeed = newSeed,
      roadSegments = roadSegments,
      trackLengthMeters = track.totalLengthMeters.toFloat(),
      playerDistanceMeters = playerTrackDistanceMeters
    )
    triggerAlert("LANDSCAPE SEED #${newSeed}", 0xFF00E5FF)
  }

  fun setLandscapeBiome(biome: BiomeType) {
    landscapeController.setBiome(
      biome = biome,
      roadSegments = roadSegments,
      trackLengthMeters = track.totalLengthMeters.toFloat(),
      playerDistanceMeters = playerTrackDistanceMeters
    )
    _hudState.update { it.copy(currentBiomeName = biome.shortLabel) }
    triggerAlert("BIOME SWITCHED: ${biome.displayName.uppercase()}", 0xFF00E676)
  }

  fun setLandscapeDensity(density: Float) {
    landscapeController.setSceneryDensity(
      density = density,
      roadSegments = roadSegments,
      trackLengthMeters = track.totalLengthMeters.toFloat(),
      playerDistanceMeters = playerTrackDistanceMeters
    )
  }

  fun setLandscapeTimeOfDay(timeOfDay: TimeOfDay) {
    landscapeController.setTimeOfDay(
      timeOfDay = timeOfDay,
      roadSegments = roadSegments,
      trackLengthMeters = track.totalLengthMeters.toFloat(),
      playerDistanceMeters = playerTrackDistanceMeters
    )
    triggerAlert("TIME OF DAY: ${timeOfDay.displayName.uppercase()}", 0xFFFFD600)
  }

  fun setLandscapeWeatherCondition(weatherCondition: WeatherCondition) {
    landscapeController.setWeather(
      weather = weatherCondition,
      roadSegments = roadSegments,
      trackLengthMeters = track.totalLengthMeters.toFloat(),
      playerDistanceMeters = playerTrackDistanceMeters
    )
    val mappedRaceWeather = when (weatherCondition) {
      WeatherCondition.CLEAR -> com.example.game.model.RaceWeather.CLEAR_SUNSET
      WeatherCondition.RAIN -> com.example.game.model.RaceWeather.WET_STORM
      WeatherCondition.FOG -> com.example.game.model.RaceWeather.OVERCAST_COOL
    }
    activeWeather = mappedRaceWeather
    _hudState.update { it.copy(activeWeather = mappedRaceWeather) }
    triggerAlert("ATMOSPHERE: ${weatherCondition.displayName.uppercase()}", 0xFF00E5FF)
  }

  fun setLandscapeVehicleProfile(vehicleType: VehicleType) {
    landscapeController.setVehicleType(
      vehicleType = vehicleType,
      roadSegments = roadSegments,
      trackLengthMeters = track.totalLengthMeters.toFloat(),
      playerDistanceMeters = playerTrackDistanceMeters
    )
    triggerAlert("TRACK GEOMETRY: ${vehicleType.name} RUN-OFF PROFILE", 0xFF00E676)
  }

  fun regenerateLandscape() {
    landscapeController.regenerate(
      roadSegments = roadSegments,
      trackLengthMeters = track.totalLengthMeters.toFloat(),
      playerDistanceMeters = playerTrackDistanceMeters,
      playerLateralNormalized = playerLaneX
    )
    triggerAlert("PROCEDURAL LANDSCAPE REGENERATED!", 0xFF00E5FF)
  }

  private fun initRivals() {
    rivals.clear()
    val allRivalPool = listOf(
      // Modern Photo Cars
      Triple("Hiroshi 'Volt' Tanaka", "Volt Nitro Shibuya", Pair("Hyperion-7 Hypercar #42", 0xFF00E5FF)),
      Triple("Matteo 'Apex' Rossi", "Scuderia Corsa #51", Pair("Scuderia Corsa V8", 0xFFD50000)),
      Triple("Lars 'Black' Bilstein", "Bilstein AMG Racing #68", Pair("AMG GT Black Edition", 0xFFFFD600)),
      Triple("Klaus Manthey", "Manthey Racing #91", Pair("Manthey Racing 911 GT3 R", 0xFF76FF03)),
      Triple("Alex 'Gulf' Sterling", "Gulf Racing Tech #55", Pair("Apex GT3-R Twin-Turbo", 0xFFFF6D00)),
      Triple("Takeshi 'Gazoo' Sato", "Gazoo Le Mans #7", Pair("TS050 Le Mans Hybrid", 0xFFFFFFFF)),
      // Classic Golden Age Cars
      Triple("Kenji 'Drift' Takahashi", "Tokyo Midnight Muscle", Pair("Classic 1969 Boss Mach 1", 0xFFFFAB00)),
      Triple("Elena 'Aircooled' Rostova", "Heritage Ducktail Club", Pair("Classic 1973 RS 2.7 Carrera", 0xFF00B0FF)),
      // Additional Street Competitors
      Triple("Chloe 'Aero' Dubois", "Formula Apex Syndicate", Pair("Formula Apex Hybrid", 0xFF00E5FF)),
      Triple("Lucas 'Ghost' Vance", "Miami Ocean Outlaws", Pair("Apex GT3-R Twin-Turbo", 0xFFE91E63)),
      Triple("Dmitri 'Oversteer' Volkov", "Neon Expressway Crew", Pair("Hyperion-7 Hypercar #42", 0xFF00E5FF)),
      Triple("Sora 'Redline' Akita", "Shinjuku Underground", Pair("Scuderia Corsa V8", 0xFFD50000)),
      Triple("Finn 'Stance' O'Connor", "Marina Bay Drift League", Pair("Classic 1969 Boss Mach 1", 0xFFFF9100)),
      Triple("Nico 'Apex' Weber", "Bilstein Street Pro", Pair("AMG GT Black Edition", 0xFFFFD600)),
      Triple("Jean 'Turbo' Martin", "Manthey Grello Squad", Pair("Manthey Racing 911 GT3 R", 0xFF76FF03))
    )

    val rivalsNeeded = (activeStage.playerCount - 1).coerceIn(1, allRivalPool.size)
    val stageRivals = allRivalPool.take(rivalsNeeded)

    stageRivals.forEachIndexed { index, (name, team, carInfo) ->
      val (carName, color) = carInfo
      val gridDistance = (rivalsNeeded - index) * 14f // 14m apart on grid
      val lane = if (index % 2 == 0) -0.45f else 0.45f
      rivals.add(
        AirRival(
          id = "rival_${activeStage.stageNumber}_$index",
          name = name,
          teamName = team,
          carName = carName,
          colorHex = color,
          laneOffset = lane,
          distanceMeters = gridDistance,
          speedKmh = 0f,
          currentLap = 1,
          currentRank = index + 1
        )
      )
    }

    _hudState.update { it.copy(totalRivals = activeStage.playerCount) }
  }

  fun startGridCountdown() {
    countdownJob?.cancel()
    _raceState.value = RaceState.LIGHTS_COUNTDOWN
    _finishSummary.value = null

    countdownJob = coroutineScope.launch {
      // 5 Red Lights turning ON consecutively
      for (light in 1..5) {
        _hudState.update { it.copy(countdownLights = light) }
        soundManager.playCountdownLightBeep()
        hapticManager.vibrateCountdownLight()
        delay(900)
      }

      // Random delay between 0.4s and 1.2s before lights out!
      delay(750)

      // LIGHTS OUT AND AWAY WE GO!
      _hudState.update { it.copy(countdownLights = 6) }
      soundManager.playLightsOutGo()
      hapticManager.vibrateLightsOutGo()
      _raceState.value = RaceState.RACING

      delay(400)
      _hudState.update { it.copy(countdownLights = 0) }

      startLoop()
    }
  }

  fun resetRace() {
    gameLoopJob?.cancel()
    countdownJob?.cancel()
    soundManager.pauseRacingAudio()

    playerLaneX = 0f
    playerTrackDistanceMeters = 0f
    speedKmh = 0f
    rpm = vehicle.idleRpm
    gear = 1
    throttleInput = 0f
    brakeInput = 0f
    steerInput = 0f
    nitroCharge = 1.0f
    isNitroActive = false
    nitroDurationRemainingSec = 0f
    isDrsActive = false
    driftScore = 0
    currentDriftAngle = 0f
    currentLap = 1
    currentLapTimeMs = 0L
    bestLapTimeMs = null
    lastLapTimeMs = null
    sector1TimeMs = null
    sector2TimeMs = null
    sector3TimeMs = null
    currentSector = 1
    topSpeedReachedKmh = 0f
    overtakesCount = 0
    peakLateralG = 0f
    kerbRumbleTimer = 0f
    collisionCooldownTimer = 0f
    hasWarnedBoxBox = false
    completedLapsHistory.clear()

    initRivals()
    tireHealthPercent = 100f
    tireTempCelsius = 95f
    hasWarnedTireWear = false
    speedTrapKmh = null
    isSpeedTrapAlertTriggered = false
    _raceState.value = RaceState.PRE_GRID
    _finishSummary.value = null
    _hudState.value = RacingGameHudState(
      currentRank = rivals.size + 1,
      totalRivals = rivals.size + 1,
      currentLap = 1,
      totalLaps = totalLaps,
      rpm = vehicle.idleRpm,
      maxRpm = vehicle.maxRpm,
      redlineRpm = vehicle.redlineRpm,
      gear = 1,
      isAutoGears = isAutoGears,
      tireCompound = activeTireCompound,
      tireHealthPercent = tireHealthPercent,
      tireTempCelsius = tireTempCelsius,
      aeroTrim = activeAeroTrim
    )
  }

  fun triggerNitroBoost() {
    if (nitroCharge >= 0.25f && !isNitroActive) {
      isNitroActive = true
      nitroDurationRemainingSec = 3.5f
      soundManager.playNitroBoost()
      hapticManager.vibrateNitroBoost()
      triggerAlert("NITRO / ERS BOOST ENGAGED!", 0xFF00E5FF)
    }
  }

  fun toggleDrs() {
    isDrsActive = !isDrsActive
    if (isDrsActive) {
      triggerAlert("DRS WING OPEN (+15 KM/H)", 0xFF00E676)
    }
  }

  fun toggleAutoGears() {
    isAutoGears = !isAutoGears
    _hudState.update { it.copy(isAutoGears = isAutoGears) }
  }

  fun shiftUp(): Boolean {
    if (gear < vehicle.gears) {
      gear++
      rpm = (rpm * 0.72f).toInt().coerceAtLeast(vehicle.idleRpm)
      soundManager.playGearShift()
      hapticManager.vibrateGearShift()
      return true
    }
    return false
  }

  fun shiftDown(): Boolean {
    if (gear > 1) {
      gear--
      rpm = (rpm * 1.35f).toInt().coerceAtMost(vehicle.maxRpm)
      soundManager.playGearShift()
      hapticManager.vibrateGearShift()
      return true
    }
    return false
  }

  fun setAeroTrim(trim: AeroTrim) {
    activeAeroTrim = trim
    _hudState.update { it.copy(aeroTrim = trim) }
    triggerAlert("AERO TRIM: ${trim.displayName.uppercase()} (${trim.shortLabel})", 0xFF00E5FF)
  }

  fun switchCamera(): CameraPerspective {
    cameraPerspective = when (cameraPerspective) {
      CameraPerspective.CHASE_CAM -> CameraPerspective.COCKPIT_CAM
      CameraPerspective.COCKPIT_CAM -> CameraPerspective.HOOD_CAM
      CameraPerspective.HOOD_CAM -> CameraPerspective.CHASE_CAM
    }
    return cameraPerspective
  }

  fun setWeather(weather: com.example.game.model.RaceWeather) {
    activeWeather = weather
    val mappedCond = when (weather) {
      com.example.game.model.RaceWeather.WET_STORM -> WeatherCondition.RAIN
      com.example.game.model.RaceWeather.OVERCAST_COOL -> WeatherCondition.FOG
      else -> WeatherCondition.CLEAR
    }
    val mappedTod = when (weather) {
      com.example.game.model.RaceWeather.NEON_NIGHT -> TimeOfDay.NIGHT
      com.example.game.model.RaceWeather.CLEAR_SUNSET -> TimeOfDay.DUSK
      else -> landscapeController.config.value.timeOfDay
    }
    landscapeController.setTimeOfDay(mappedTod, roadSegments, track.totalLengthMeters.toFloat(), playerTrackDistanceMeters)
    landscapeController.setWeather(mappedCond, roadSegments, track.totalLengthMeters.toFloat(), playerTrackDistanceMeters)
    _hudState.update { it.copy(activeWeather = weather) }
    triggerAlert("TRACK CONDITIONS: ${weather.displayName.uppercase()}", 0xFF00E5FF)
  }

  fun changeTires(newCompound: com.example.game.model.TireCompound) {
    activeTireCompound = newCompound
    tireHealthPercent = 100f
    tireTempCelsius = 90f
    hasWarnedTireWear = false
    hasWarnedBoxBox = false
    soundManager.playPitLaneChime()
    triggerAlert("PIT STOP: FITTED ${newCompound.displayName.uppercase()}", newCompound.colorHex)
    _hudState.update {
      it.copy(
        tireCompound = activeTireCompound,
        tireHealthPercent = tireHealthPercent,
        tireTempCelsius = tireTempCelsius,
        isBoxBoxAlert = false
      )
    }
  }

  fun performPitStop(newCompound: com.example.game.model.TireCompound) {
    coroutineScope.launch {
      _hudState.update { it.copy(isPitStopActive = true) }
      soundManager.playRadioBeep()
      soundManager.playPitLaneChime()
      hapticManager.vibrateGearShift()
      triggerAlert("IN PIT LANE • WHEEL GUNS ACTIVE...", 0xFFFFD600)
      speedKmh = (speedKmh * 0.35f).coerceAtLeast(40f)
      delay(1600)
      changeTires(newCompound)
      hapticManager.vibrateNitroBoost()
      triggerAlert("PIT COMPLETE: 2.4s • ${newCompound.displayName.uppercase()} FITTED!", 0xFF00E676)
      _hudState.update { it.copy(isPitStopActive = false) }
    }
  }

  fun pauseRace() {
    if (_raceState.value != RaceState.FINISHED_PODIUM && _raceState.value != RaceState.PAUSED) {
      prePauseRaceState = if (_raceState.value == RaceState.FINAL_LAP) RaceState.FINAL_LAP else RaceState.RACING
      _raceState.value = RaceState.PAUSED
      gameLoopJob?.cancel()
      throttleInput = 0f
      brakeInput = 0f
      steerInput = 0f
      soundManager.pauseRacingAudio()
    }
  }

  /**
   * CRITICAL AUTOMOTIVE POLICY:
   * Called when vehicle motion or driving state change is detected.
   * Immediately silences all game audio and halts simulation physics loop.
   */
  fun pauseRaceForVehicleMotion(reason: String = "Automotive Driving Safety") {
    if (_raceState.value == RaceState.RACING || _raceState.value == RaceState.FINAL_LAP) {
      prePauseRaceState = _raceState.value
      _raceState.value = RaceState.PAUSED
      gameLoopJob?.cancel()
      throttleInput = 0f
      brakeInput = 0f
      steerInput = 0f
      soundManager.silenceAllAudioImmediately("Automotive Driving Restriction: $reason")
      triggerAlert("⚠️ VEHICLE IN MOTION: AUDIO & RACE PAUSED", 0xFFFF1744)
    }
  }

  fun resumeRace() {
    if (_raceState.value == RaceState.PAUSED) {
      _raceState.value = prePauseRaceState
      startLoop()
    }
  }

  fun triggerAlert(message: String, colorHex: Long = 0xFF00E5FF) {
    _hudState.update {
      it.copy(activeAlertMessage = message, activeAlertColor = colorHex)
    }
    coroutineScope.launch {
      delay(2200)
      if (_hudState.value.activeAlertMessage == message) {
        _hudState.update { it.copy(activeAlertMessage = null) }
      }
    }
  }

  private fun startLoop() {
    gameLoopJob?.cancel()
    soundManager.startRacingAudio()
    gameLoopJob = coroutineScope.launch {
      var lastTimestamp = System.currentTimeMillis()
      while (isActive && _raceState.value != RaceState.FINISHED_PODIUM && _raceState.value != RaceState.PAUSED) {
        val now = System.currentTimeMillis()
        val dt = ((now - lastTimestamp) / 1000f).coerceIn(0.005f, 0.05f)
        lastTimestamp = now

        updateTick(dt)
        delay(16) // ~60fps
      }
    }
  }

  private fun updateTick(dt: Float) {
    if (_raceState.value != RaceState.RACING && _raceState.value != RaceState.FINAL_LAP) return

    currentLapTimeMs += (dt * 1000).toLong()

    // 1. NITRO BOOST LOGIC
    if (isNitroActive) {
      nitroDurationRemainingSec -= dt
      nitroCharge = (nitroCharge - (dt / 3.5f)).coerceAtLeast(0f)
      if (nitroDurationRemainingSec <= 0f || nitroCharge <= 0f) {
        isNitroActive = false
      }
    } else {
      // Recharges when braking or cruising
      if (brakeInput > 0.2f) {
        nitroCharge = (nitroCharge + dt * 0.15f).coerceAtMost(1f)
      } else {
        nitroCharge = (nitroCharge + dt * 0.04f).coerceAtMost(1f)
      }
    }

    // 2. DRAFTING / SLIPSTREAM DETECTION
    var isDrafting = false
    var slipstreamBonusKmh = 0f
    val nearestAhead = rivals.filter {
      it.distanceMeters > playerTrackDistanceMeters && (it.distanceMeters - playerTrackDistanceMeters) in 2f..24f
    }.minByOrNull { it.distanceMeters - playerTrackDistanceMeters }

    if (nearestAhead != null && abs(nearestAhead.laneOffset - playerLaneX) < 0.35f && speedKmh > 140f) {
      isDrafting = true
      slipstreamBonusKmh = 18f
      if (!_hudState.value.isDrafting) {
        soundManager.playSlipstreamDraft()
        hapticManager.vibrateGearShift()
        triggerAlert("SLIPSTREAM DRAFTING • +18 KM/H", 0xFF00E5FF)
      }
    }

    // 3. STEERING, AERO TRIM, PROCEDURAL SURFACE PHYSICS & LATERAL G
    val aeroGripFactor = activeAeroTrim.gripMultiplier
    playerLaneX = (playerLaneX + steerInput * dt * 2.2f).coerceIn(-1.45f, 1.45f)

    // Current road segment
    val currentSegmentIdx = ((playerTrackDistanceMeters / 60f).toInt()) % roadSegments.size
    val currentSegment = roadSegments[currentSegmentIdx]

    // Stream procedural chunks around player and resolve physical surface (ROAD, KERB, GRAVEL, GRASS)
    currentSurfaceType = landscapeController.updateStreamingAndPhysics(
      playerDistanceMeters = playerTrackDistanceMeters,
      playerLateralNormalized = playerLaneX,
      trackLengthMeters = track.totalLengthMeters.toFloat(),
      roadSegments = roadSegments
    )
    val surfaceGrip = currentSurfaceType.gripMultiplier

    val curvePull = (currentSegment.curvature / (aeroGripFactor * surfaceGrip.coerceAtLeast(0.45f))) * (speedKmh / 200f) * dt
    playerLaneX = (playerLaneX - curvePull).coerceIn(-1.48f, 1.48f)

    // Kerb / Gravel / Grass surface vibration feedback
    if (currentSurfaceType != SurfaceType.ROAD && speedKmh > 45f) {
      kerbRumbleTimer += dt
      val rumbleInterval = if (currentSurfaceType == SurfaceType.KERB) 0.08f else 0.11f
      if (kerbRumbleTimer > rumbleInterval) {
        kerbRumbleTimer = 0f
        hapticManager.vibrateKerbRumble()
      }
    }

    // Outer Safety Barrier Collision Check (at outer edge of run-off corridor)
    if (abs(playerLaneX) >= 1.42f && speedKmh > 40f) {
      collisionCooldownTimer += dt
      if (collisionCooldownTimer > 0.65f) {
        collisionCooldownTimer = 0f
        hapticManager.vibrateImpact()
        soundManager.playBarrierImpact()
        speedKmh = (speedKmh * 0.78f)
        playerLaneX = playerLaneX.coerceIn(-1.32f, 1.32f)
        triggerAlert("💥 BARRIER IMPACT! RECOVERING...", 0xFFFF1744)
      }
    }

    // Rival Car Contact Collision
    rivals.forEach { rival ->
      val deltaDist = rival.distanceMeters - playerTrackDistanceMeters
      if (abs(deltaDist) < 3.8f && abs(playerLaneX - rival.laneOffset) < 0.28f && speedKmh > 35f) {
        collisionCooldownTimer += dt
        if (collisionCooldownTimer > 0.5f) {
          collisionCooldownTimer = 0f
          hapticManager.vibrateImpact()
          soundManager.playBarrierImpact()
          speedKmh = (speedKmh * 0.84f).coerceAtLeast(30f)
          if (playerLaneX < rival.laneOffset) {
            playerLaneX = (playerLaneX - 0.12f).coerceAtLeast(-0.95f)
          } else {
            playerLaneX = (playerLaneX + 0.12f).coerceAtMost(0.95f)
          }
          triggerAlert("⚠️ CONTACT WITH ${rival.name.uppercase()}!", 0xFFFF5722)
        }
      }
    }

    val loadout = com.example.game.v2.ApexRivals2CareerManager.getLoadoutForVehicle(vehicle)
    val upgradedStats = com.example.game.v2.ApexRivals2CareerManager.computeUpgradedStats(vehicle, loadout)
    com.example.game.v2.ApexRivals2SettingsRepository.recordFrameTimeMs(dt * 1000f)

    val lateralG = abs(steerInput * (speedKmh / 160f) * upgradedStats.handlingCorneringG * aeroGripFactor).coerceAtMost(4.8f)
    if (lateralG > peakLateralG) peakLateralG = lateralG

    // Drift & Handbrake (E-Brake) Detection (Prompt 2)
    if (isHandbrakeActive && speedKmh > 55f) {
      val steerFactor = max(abs(steerInput), 0.55f)
      currentDriftAngle = (steerFactor * 44f).coerceAtMost(48f)
      val pointsGained = ((lateralG + 1.8f) * speedKmh * 0.24f * dt).toInt().coerceAtLeast(1)
      driftScore += pointsGained
      handbrakeScreechTimer += dt
      if (handbrakeScreechTimer > 0.35f) {
        handbrakeScreechTimer = 0f
        soundManager.playTireScreech()
      }
    } else if (lateralG > 2.0f && speedKmh > 100f && abs(steerInput) > 0.6f) {
      currentDriftAngle = abs(steerInput) * 35f
      val pointsGained = (lateralG * speedKmh * 0.15f * dt).toInt()
      driftScore += pointsGained
    } else {
      currentDriftAngle = 0f
    }

    // 4. ACCELERATION, BRAKING & TOP SPEED (incorporating 3D Garage Engine/Turbo/Brakes/Tires upgrades)
    val baseTopSpeed = upgradedStats.maxSpeedKmh.toFloat()
    val maxEffectiveSpeed = (baseTopSpeed +
      activeAeroTrim.topSpeedBonusKmh +
      currentSurfaceType.topSpeedPenaltyKmh +
      (if (isDrsActive) 15f else 0f) +
      (if (isNitroActive) 35f else 0f) +
      slipstreamBonusKmh).coerceAtLeast(80f)

    val nitroMultiplier = if (isNitroActive) 1.55f else 1.0f

    // Dynamic Tire Degradation & Temp System (v8.0.0) + Procedural Surface Grip Multiplier
    val tireGripFactor = activeTireCompound.gripFactor *
      (0.65f + 0.35f * (tireHealthPercent / 100f)) *
      currentSurfaceType.gripMultiplier *
      activeWeather.gripMultiplier
    val wearStep = ((speedKmh / 180f) * (0.012f + abs(steerInput) * 0.035f + brakeInput * 0.045f + (if (isHandbrakeActive) 0.05f else 0f)) * activeTireCompound.wearRate * dt).coerceAtLeast(0f)
    tireHealthPercent = (tireHealthPercent - wearStep).coerceIn(0f, 100f)

    val tempHeat = (brakeInput * 18f + (lateralG / 3f) * 12f + (if (isHandbrakeActive) 14f else 0f)) * dt
    val tempCool = ((tireTempCelsius - 90f) * 0.12f * dt)
    tireTempCelsius = (tireTempCelsius + tempHeat - tempCool).coerceIn(65f, 135f)

    val isBoxBoxAdvised = tireHealthPercent < 35f || (activeWeather == com.example.game.model.RaceWeather.WET_STORM && activeTireCompound != com.example.game.model.TireCompound.INTERMEDIATE)
    if (isBoxBoxAdvised && !hasWarnedBoxBox && speedKmh > 40f) {
      hasWarnedBoxBox = true
      soundManager.playRadioBeep()
      hapticManager.vibrateGearShift()
      triggerAlert("📻 BOX BOX! BOX FOR FRESH TIRES", 0xFFFFD600)
    }

    if (tireHealthPercent < 25f && !hasWarnedTireWear) {
      hasWarnedTireWear = true
      triggerAlert("⚠️ TIRE WEAR CRITICAL (${tireHealthPercent.toInt()}%) - PIT SUGGESTED", 0xFFFF1744)
    }

    // Speed Trap Radar Detection
    if (currentSegment.isSpeedTrap) {
      if (speedTrapKmh == null || speedKmh > (speedTrapKmh ?: 0f)) {
        speedTrapKmh = speedKmh
        if (speedKmh > 120f && !isSpeedTrapAlertTriggered) {
          isSpeedTrapAlertTriggered = true
          triggerAlert("⚡ SPEED TRAP: ${speedKmh.toInt()} KM/H", 0xFF00E5FF)
          soundManager.playSpeedTrapRadar()
        }
      }
    } else {
      isSpeedTrapAlertTriggered = false
    }

    val surfaceDragMultiplier = currentSurfaceType.rollingResistance
    if (throttleInput > 0.05f) {
      val accelPower = (upgradedStats.horsepower / 220f) * (1f - (speedKmh / maxEffectiveSpeed).coerceIn(0f, 1f)) * nitroMultiplier * tireGripFactor
      speedKmh += (accelPower * 48f * throttleInput * dt)
      if (currentSurfaceType == SurfaceType.GRAVEL || currentSurfaceType == SurfaceType.GRASS) {
        speedKmh -= (14f * (surfaceDragMultiplier - 1f) * dt)
      }
    } else {
      // Natural aerodynamic drag & surface rolling resistance
      speedKmh -= (18f * surfaceDragMultiplier * dt + (speedKmh * speedKmh / 25000f) * dt)
    }

    val brakingPowerMultiplier = 0.85f + (upgradedStats.brakingRating / 200f)
    if (brakeInput > 0.05f) {
      speedKmh -= (110f * brakingPowerMultiplier * brakeInput * dt)
    }
    if (isHandbrakeActive) {
      speedKmh -= (65f * dt)
    }

    speedKmh = speedKmh.coerceIn(0f, maxEffectiveSpeed)
    if (speedKmh > topSpeedReachedKmh) topSpeedReachedKmh = speedKmh

    // 5. GEARS & RPM
    val gearRatioSpeed = maxEffectiveSpeed / vehicle.gears
    if (isAutoGears) {
      val targetGear = ((speedKmh / gearRatioSpeed).toInt() + 1).coerceIn(1, vehicle.gears)
      if (targetGear != gear) {
        gear = targetGear
        soundManager.playGearShift()
        hapticManager.vibrateGearShift()
      }
    }

    val speedInCurrentGear = speedKmh - (gear - 1) * gearRatioSpeed
    val rpmFraction = (speedInCurrentGear / gearRatioSpeed).coerceIn(0f, 1.15f)
    rpm = (vehicle.idleRpm + rpmFraction * (vehicle.maxRpm - vehicle.idleRpm)).toInt().coerceIn(vehicle.idleRpm, vehicle.maxRpm)

    // 6. DISTANCE & TRACK PROGRESS
    val distanceStepMeters = (speedKmh / 3.6f) * dt
    playerTrackDistanceMeters += distanceStepMeters

    // Lap Completion Check
    val totalTrackMeters = track.totalLengthMeters.toFloat()
    if (playerTrackDistanceMeters >= totalTrackMeters) {
      playerTrackDistanceMeters -= totalTrackMeters
      onLapCompleted()
    }

    // Sector Times
    val progress = playerTrackDistanceMeters / totalTrackMeters
    if (progress > 0.33f && sector1TimeMs == null) {
      sector1TimeMs = currentLapTimeMs
      soundManager.playSectorRecord()
      triggerAlert("SECTOR 1 • ${formatTime(sector1TimeMs ?: 0)}", 0xFF9C27B0)
    } else if (progress > 0.66f && sector2TimeMs == null) {
      sector2TimeMs = currentLapTimeMs - (sector1TimeMs ?: 0L)
      soundManager.playSectorRecord()
      triggerAlert("SECTOR 2 • ${formatTime(sector2TimeMs ?: 0)}", 0xFF9C27B0)
    }

    // 7. UPDATE AI RIVALS
    updateRivals(dt, totalTrackMeters)

    // 8. CALCULATE RANK & GAPS
    val allCars = (rivals.map { it.distanceMeters + (it.currentLap - 1) * totalTrackMeters } +
      listOf(playerTrackDistanceMeters + (currentLap - 1) * totalTrackMeters)).sortedDescending()

    val playerTotalDistance = playerTrackDistanceMeters + (currentLap - 1) * totalTrackMeters
    val playerRank = allCars.indexOf(playerTotalDistance) + 1

    // Check overtakes
    rivals.forEach { rival ->
      val rivalTotalDist = rival.distanceMeters + (rival.currentLap - 1) * totalTrackMeters
      if (!rival.isOvertaken && playerTotalDistance > rivalTotalDist) {
        rival.isOvertaken = true
        overtakesCount++
        soundManager.playOvertakeChime()
        triggerAlert("OVERTAKE! PASSED ${rival.name.uppercase()}", 0xFF00E676)
      }
    }

    // Interval ahead / behind
    val carAheadDist = allCars.filter { it > playerTotalDistance }.minOrNull()
    val intervalAhead = carAheadDist?.let { ((it - playerTotalDistance) / (speedKmh.coerceAtLeast(60f) / 3.6f)) }

    val carBehindDist = allCars.filter { it < playerTotalDistance }.maxOrNull()
    val intervalBehind = carBehindDist?.let { ((playerTotalDistance - it) / (speedKmh.coerceAtLeast(60f) / 3.6f)) }

    val rivalAhead = rivals.find {
      val dist = it.distanceMeters + (it.currentLap - 1) * totalTrackMeters
      dist > playerTotalDistance && (dist - playerTotalDistance) < 150f
    }

    // Live Delta vs Personal Best Pace
    val expectedLapTimeMs = bestLapTimeMs ?: (totalTrackMeters / (vehicle.topSpeedKmh / 3.6f) * 1000f).toLong()
    val expectedElapsedMs = (progress * expectedLapTimeMs).toLong()
    val liveDeltaSeconds = if (currentLapTimeMs > 2500L) (currentLapTimeMs - expectedElapsedMs) / 1000f else null

    _hudState.update {
      it.copy(
        currentRank = playerRank,
        totalRivals = rivals.size + 1,
        currentLap = currentLap,
        totalLaps = totalLaps,
        intervalAheadSeconds = intervalAhead,
        intervalBehindSeconds = intervalBehind,
        rivalAheadName = rivalAhead?.name,
        speedKmh = speedKmh,
        rpm = rpm,
        gear = gear,
        nitroChargePercent = nitroCharge,
        isNitroActive = isNitroActive,
        isDrsAvailable = currentSegment.isSpeedTrap || currentSegment.curvature == 0f,
        isDrsActive = isDrsActive,
        isDrafting = isDrafting,
        slipstreamBonusKmh = slipstreamBonusKmh,
        driftPoints = driftScore,
        currentLapTimeMs = currentLapTimeMs,
        bestLapTimeMs = bestLapTimeMs,
        lastLapTimeMs = lastLapTimeMs,
        sector1TimeMs = sector1TimeMs,
        sector2TimeMs = sector2TimeMs,
        sector3TimeMs = sector3TimeMs,
        tireCompound = activeTireCompound,
        tireHealthPercent = tireHealthPercent,
        tireTempCelsius = tireTempCelsius,
        speedTrapKmh = speedTrapKmh,
        isSpeedTrapActive = currentSegment.isSpeedTrap,
        deltaVsBestSeconds = liveDeltaSeconds,
        aeroTrim = activeAeroTrim,
        isPitWindowOpen = currentLap >= 1,
        isBoxBoxAlert = isBoxBoxAdvised,
        currentSurfaceName = currentSurfaceType.name,
        currentBiomeName = landscapeController.config.value.biome.shortLabel
      )
    }

    // Real-time engine audio and acoustics telemetry
    soundManager.updateRacingEngineTelemetry(
      rpm = rpm,
      throttle = throttleInput,
      nitro = isNitroActive,
      braking = brakeInput > 0.05f,
      drifting = currentDriftAngle > 5f
    )
  }

  private fun updateRivals(dt: Float, totalTrackMeters: Float) {
    rivals.forEachIndexed { i, rival ->
      // Rival speed behavior based on difficulty & curve
      val baseSpeed = vehicle.topSpeedKmh * (0.91f + (i * 0.012f))
      val currentSegmentIdx = ((rival.distanceMeters / 60f).toInt()) % roadSegments.size
      val seg = roadSegments[currentSegmentIdx]

      val targetSpeed = if (seg.isBrakingZone) baseSpeed * 0.58f else baseSpeed
      rival.speedKmh += ((targetSpeed - rival.speedKmh) * 1.5f * dt)

      rival.distanceMeters += (rival.speedKmh / 3.6f) * dt

      if (rival.distanceMeters >= totalTrackMeters) {
        rival.distanceMeters -= totalTrackMeters
        rival.currentLap++
      }

      // Smooth AI weaving/defending
      rival.laneOffset += sin((rival.distanceMeters / 50f) + i) * 0.08f * dt
      rival.laneOffset = rival.laneOffset.coerceIn(-0.8f, 0.8f)
    }
  }

  private fun onLapCompleted() {
    val lapMs = currentLapTimeMs.coerceAtLeast(1000L)
    lastLapTimeMs = lapMs

    val s1 = sector1TimeMs ?: (lapMs * 0.33f).toLong()
    val s2 = sector2TimeMs ?: (lapMs * 0.34f).toLong()
    val s3 = (lapMs - s1 - s2).coerceAtLeast(100L)
    sector3TimeMs = s3

    val totalTrackMeters = track.totalLengthMeters.toFloat()
    val playerTotalDistance = currentLap * totalTrackMeters
    val allCars = (rivals.map { it.distanceMeters + (it.currentLap - 1) * totalTrackMeters } + listOf(playerTotalDistance)).sortedDescending()
    val lapRank = (allCars.indexOf(playerTotalDistance) + 1).coerceAtLeast(1)
    val leaderDist = allCars.firstOrNull() ?: playerTotalDistance
    val gapSeconds = if (leaderDist > playerTotalDistance) {
      ((leaderDist - playerTotalDistance) / (speedKmh.coerceAtLeast(120f) / 3.6f)).coerceAtLeast(0f)
    } else {
      0f
    }

    completedLapsHistory.add(
      LapTelemetrySummary(
        lapNumber = currentLap,
        lapTimeMs = lapMs,
        sector1Ms = s1,
        sector2Ms = s2,
        sector3Ms = s3,
        gapToLeaderSeconds = gapSeconds,
        positionOnLap = lapRank,
        tireCompoundCode = activeTireCompound.shortCode
      )
    )

    val isBest = bestLapTimeMs == null || lapMs < bestLapTimeMs!!
    if (isBest) {
      bestLapTimeMs = lapMs
      soundManager.playCheckeredFlagVictory()
      triggerAlert("NEW PERSONAL BEST LAP! • ${formatTime(lapMs)}", 0xFFFFD700)
    } else {
      triggerAlert("LAP $currentLap COMPLETE • ${formatTime(lapMs)}", 0xFF00E5FF)
    }

    currentLapTimeMs = 0L
    sector1TimeMs = null
    sector2TimeMs = null

    if (currentLap < totalLaps) {
      currentLap++
      if (currentLap == totalLaps) {
        _raceState.value = RaceState.FINAL_LAP
        triggerAlert("FINAL LAP! PUSH TO THE APEX!", 0xFFFF1744)
      }
    } else {
      // Race Complete!
      finishRace()
    }
  }

  fun completeRaceImmediately(forcedPosition: Int = 1) {
    countdownJob?.cancel()
    gameLoopJob?.cancel()
    currentLap = totalLaps
    if (topSpeedReachedKmh < 245f) {
      topSpeedReachedKmh = (vehicle.topSpeedKmh * 0.94f).coerceAtLeast(285f)
    }
    if (peakLateralG < 1.35f) {
      peakLateralG = 1.65f
    }
    if (overtakesCount < 3) {
      overtakesCount = 5
    }
    if (driftScore < 450) {
      driftScore = 1850
    }
    val totalTrackMeters = track.totalLengthMeters.toFloat()
    val maxRivalTotal = rivals.maxOfOrNull { it.distanceMeters + (it.currentLap - 1) * totalTrackMeters } ?: totalTrackMeters
    if (forcedPosition <= 1) {
      playerTrackDistanceMeters = maxRivalTotal + 120f
    }
    finishRace()
  }

  private fun finishRace() {
    gameLoopJob?.cancel()
    soundManager.pauseRacingAudio()

    val totalTrackMeters = track.totalLengthMeters.toFloat()
    val playerTotalDistance = playerTrackDistanceMeters + (currentLap - 1) * totalTrackMeters
    val allCars = (rivals.map { it.distanceMeters + (it.currentLap - 1) * totalTrackMeters } + listOf(playerTotalDistance)).sortedDescending()
    val finalRank = allCars.indexOf(playerTotalDistance) + 1

    val bestLap = bestLapTimeMs ?: lastLapTimeMs ?: 78000L
    val xp = (1000 / finalRank) + (driftScore / 10) + (overtakesCount * 150)
    val credits = (5000 / finalRank) + (driftScore / 5)

    if (completedLapsHistory.isEmpty()) {
      for (lapNum in 1..totalLaps) {
        val simulatedLapMs = bestLap + ((totalLaps - lapNum) * 420L)
        val s1 = (simulatedLapMs * 0.32f).toLong()
        val s2 = (simulatedLapMs * 0.35f).toLong()
        val s3 = simulatedLapMs - s1 - s2
        val gap = if (finalRank == 1) 0f else ((finalRank - 1) * 0.85f + (totalLaps - lapNum) * 0.25f)
        completedLapsHistory.add(
          LapTelemetrySummary(
            lapNumber = lapNum,
            lapTimeMs = simulatedLapMs,
            sector1Ms = s1,
            sector2Ms = s2,
            sector3Ms = s3,
            gapToLeaderSeconds = gap,
            positionOnLap = finalRank,
            tireCompoundCode = activeTireCompound.shortCode
          )
        )
      }
    }

    val finalLapSummaries = completedLapsHistory.toList()
    val totalRaceDurationMs = finalLapSummaries.sumOf { it.lapTimeMs }.takeIf { it > 0L } ?: ((lastLapTimeMs ?: bestLap) * totalLaps)

    // Populate initial finish summary synchronously so PostRacePodiumDialog renders immediately
    _finishSummary.value = RaceFinishSummary(
      finishingPosition = finalRank,
      totalRacers = rivals.size + 1,
      totalTimeMs = totalRaceDurationMs,
      bestLapMs = bestLap,
      topSpeedKmh = topSpeedReachedKmh.coerceAtLeast(240f),
      overtakesCount = overtakesCount,
      driftScore = driftScore,
      xpEarned = xp,
      creditsEarned = credits,
      trackName = track.name,
      vehicleName = vehicle.name,
      isNewPersonalBest = true,
      lapSummaries = finalLapSummaries
    )
    _raceState.value = RaceState.FINISHED_PODIUM
    soundManager.playCheckeredFlagVictory()

    // Award career race rewards & unlock tracks while preserving existing records
    com.example.game.v2.ApexRivals2CareerManager.recordCompletedRaceResult(
      trackId = track.id,
      finishingPosition = finalRank,
      driftScore = driftScore,
      topSpeedKmh = topSpeedReachedKmh,
      creditsEarned = credits,
      xpEarned = xp,
      context = context
    )

    // Save to Room DB asynchronously
    coroutineScope.launch(Dispatchers.IO) {
      try {
        val db = AppDatabase.getDatabase(context)
        val metricsDao = db.performanceMetricsDao()
        val raceResultDao = db.raceResultDao()

        val raceId = "gp_${System.currentTimeMillis()}"
        val metricsRecord = PerformanceMetrics(
          raceId = raceId,
          driverName = "Apex Driver",
          trackName = track.name,
          vehicleName = vehicle.name,
          lapTimeMs = bestLap,
          topSpeedKmh = topSpeedReachedKmh,
          maxLateralG = peakLateralG,
          weatherCondition = "Dry Asphalt",
          trackDistanceMeters = track.totalLengthMeters,
          zeroToHundredKmhSeconds = vehicle.acceleration0to100
        )
        metricsDao.insert(metricsRecord)

        val raceResult = RaceResult(
          trackName = track.name,
          vehicleName = vehicle.name,
          lapTimeMs = bestLap,
          topSpeedKmh = topSpeedReachedKmh,
          timestamp = System.currentTimeMillis()
        )
        raceResultDao.insertResult(raceResult)

        val bestLapEntry = finalLapSummaries.minByOrNull { it.lapTimeMs }
        val telemetryMetric = com.example.data.local.RacePerformanceMetric(
          trackName = track.name,
          lapTimeMs = bestLap,
          vehicleSpeed = topSpeedReachedKmh,
          vehicleName = vehicle.name,
          driverName = "Apex Driver",
          avgSpeedKmh = (topSpeedReachedKmh * 0.72f),
          topSpeedKmh = topSpeedReachedKmh,
          peakAccelerationG = 1.35f,
          maxLateralG = peakLateralG,
          sector1Ms = bestLapEntry?.sector1Ms ?: 0L,
          sector2Ms = bestLapEntry?.sector2Ms ?: 0L,
          sector3Ms = bestLapEntry?.sector3Ms ?: 0L,
          raceMode = gameMode.displayName,
          weather = activeWeather.displayName
        )
        db.racePerformanceMetricDao().insertMetric(telemetryMetric)

        val driverDisplayName = com.example.game.v2.ApexRivals2CareerManager.state.value.playerUsername
          .ifBlank { "Apex Pilot" }
        com.example.data.firebase.ApexCloudSyncCoordinator.onRaceCompleted(
          context = context,
          raceId = raceId,
          driverName = driverDisplayName,
          trackId = track.id,
          trackName = track.name,
          vehicleId = vehicle.id,
          vehicleName = vehicle.name,
          gameMode = gameMode.name,
          finishingPosition = finalRank,
          raceDurationMs = totalRaceDurationMs,
          lapTimeMs = bestLap,
          topSpeedKmh = topSpeedReachedKmh
        )

        // Evaluate Badges
        val allMetrics = metricsDao.getAllList()
        val evaluatedBadges = BadgeEvaluator.evaluateBadges(allMetrics)
        val newlyUnlocked = evaluatedBadges.filter { it.isUnlocked }.map { it.title }

        _finishSummary.value = RaceFinishSummary(
          finishingPosition = finalRank,
          totalRacers = rivals.size + 1,
          totalTimeMs = totalRaceDurationMs,
          bestLapMs = bestLap,
          topSpeedKmh = topSpeedReachedKmh,
          overtakesCount = overtakesCount,
          driftScore = driftScore,
          xpEarned = xp,
          creditsEarned = credits,
          trackName = track.name,
          vehicleName = vehicle.name,
          isNewPersonalBest = true,
          newlyUnlockedBadges = newlyUnlocked,
          lapSummaries = finalLapSummaries
        )
      } catch (e: Exception) {
        // Fallback summary if DB save has transient error
        _finishSummary.value = RaceFinishSummary(
          finishingPosition = finalRank,
          totalRacers = rivals.size + 1,
          totalTimeMs = totalRaceDurationMs,
          bestLapMs = bestLap,
          topSpeedKmh = topSpeedReachedKmh,
          overtakesCount = overtakesCount,
          driftScore = driftScore,
          xpEarned = xp,
          creditsEarned = credits,
          trackName = track.name,
          vehicleName = vehicle.name,
          isNewPersonalBest = true,
          lapSummaries = finalLapSummaries
        )
      }
    }
  }

  fun getRivalsList(): List<AirRival> = rivals.toList()

  fun getRoadSegments(): List<RoadSegment> = roadSegments

  fun release() {
    com.example.automotive.AutomotiveSafetyManager.getInstance(context).unregisterDrivingCallback(automotiveSafetyCallback)
    gameLoopJob?.cancel()
    countdownJob?.cancel()
    soundManager.release()
  }

  private fun formatTime(millis: Long): String {
    val mins = (millis / 60000)
    val secs = (millis % 60000) / 1000
    val ms = millis % 1000
    return String.format(Locale.US, "%d:%02d.%03d", mins, secs, ms)
  }

  private companion object {
    fun resolveInitialBiomeForTrack(trackId: String): BiomeType {
      return when {
        trackId.contains("laguna") || trackId.contains("desert") -> BiomeType.DESERT
        trackId.contains("miami") || trackId.contains("marina") -> BiomeType.COASTAL
        trackId.contains("tokyo") -> BiomeType.CITY_OUTSKIRTS
        trackId.contains("fuji") || trackId.contains("alpine") -> BiomeType.ALPINE
        else -> BiomeType.FOREST
      }
    }

    fun generateCircuitRoadSegments(totalDistanceMeters: Int): List<RoadSegment> {
      val segmentsCount = (totalDistanceMeters / 60).coerceAtLeast(40)
      return (0 until segmentsCount).map { i ->
        val progress = i.toFloat() / segmentsCount
        val curvature = when {
          progress in 0.15f..0.25f -> -0.75f // Curva Grande left
          progress in 0.35f..0.45f -> 0.85f  // Chicane right
          progress in 0.55f..0.65f -> -0.90f // Lesmo hairpin
          progress in 0.78f..0.92f -> 0.65f  // Parabolica sweeper
          else -> 0.0f // Long high-speed straightaway
        }

        val isBraking = progress in 0.32f..0.35f || progress in 0.52f..0.55f || progress in 0.75f..0.78f
        val isSpeedTrap = progress in 0.08f..0.12f

        val board = when {
          progress in 0.31f..0.32f -> 200
          progress in 0.33f..0.34f -> 100
          progress in 0.51f..0.52f -> 150
          progress in 0.74f..0.75f -> 100
          else -> null
        }

        RoadSegment(
          segmentIndex = i,
          curvature = curvature,
          elevation = sin(progress * 6.28f * 3f) * 0.3f,
          isBrakingZone = isBraking,
          isSpeedTrap = isSpeedTrap,
          hasDistanceBoard = board
        )
      }
    }
  }
}
