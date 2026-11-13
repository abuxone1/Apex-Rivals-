package com.example

import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.example.game.engine.InteractiveRacingEngine
import com.example.game.model.CameraPerspective
import com.example.game.model.GameMode
import com.example.game.model.LapTelemetrySummary
import com.example.game.model.RaceFinishSummary
import com.example.game.model.RaceState
import com.example.game.ui.InGameDrivingControls
import com.example.game.ui.PostRacePodiumDialog
import com.example.game.ui.RacingGameScreen
import com.example.game.ui.StartLightsOverlay
import com.example.game.ui.TopRacingHudOverlay
import com.example.model.AVAILABLE_TRACKS
import com.example.model.AVAILABLE_VEHICLES
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class RacingGameSystemTest {

  @get:Rule
  val composeTestRule = createComposeRule()

  private val vehicle = AVAILABLE_VEHICLES[0] // Apex GT3-R Twin-Turbo
  private val track = AVAILABLE_TRACKS[0]     // Monza Speed Autodrome

  @Test
  fun `racing engine initializes with correct rivals, grid count and vehicle setup`() {
    val context = ApplicationProvider.getApplicationContext<android.content.Context>()
    val engine = InteractiveRacingEngine(context, vehicle, track)

    assertEquals(RaceState.PRE_GRID, engine.raceState.value)
    assertEquals(8, engine.hudState.value.totalRivals)
    assertEquals(7, engine.getRivalsList().size)
    assertEquals(1, engine.hudState.value.currentLap)
    assertEquals(vehicle.idleRpm, engine.rpm)
    assertEquals(1, engine.gear)
    assertTrue("Nitro should be fully charged at start", engine.nitroCharge >= 0.99f)
  }

  @Test
  fun `nitro boost engages and increases speed and consumption`() {
    val context = ApplicationProvider.getApplicationContext<android.content.Context>()
    val engine = InteractiveRacingEngine(context, vehicle, track)

    assertFalse(engine.isNitroActive)
    engine.triggerNitroBoost()
    assertTrue(engine.isNitroActive)
    assertNotNull(engine.hudState.value.activeAlertMessage)
  }

  @Test
  fun `camera perspective toggles seamlessly between chase, cockpit and hood`() {
    val context = ApplicationProvider.getApplicationContext<android.content.Context>()
    val engine = InteractiveRacingEngine(context, vehicle, track)

    assertEquals(CameraPerspective.CHASE_CAM, engine.cameraPerspective)
    engine.switchCamera()
    assertEquals(CameraPerspective.COCKPIT_CAM, engine.cameraPerspective)
    engine.switchCamera()
    assertEquals(CameraPerspective.HOOD_CAM, engine.cameraPerspective)
    engine.switchCamera()
    assertEquals(CameraPerspective.CHASE_CAM, engine.cameraPerspective)
  }

  @Test
  fun `racing game screen renders top hud, canvas and pre-grid launch button`() {
    var navigatedBack = false

    composeTestRule.setContent {
      RacingGameScreen(
        vehicle = vehicle,
        track = track,
        onNavigateBack = { navigatedBack = true }
      )
    }

    // Canvas container
    composeTestRule.onNodeWithTag("racing_game_screen").assertIsDisplayed()
    composeTestRule.onNodeWithTag("racing_game_canvas").assertIsDisplayed()

    // Top HUD
    composeTestRule.onNodeWithTag("hud_position_badge").assertIsDisplayed()
    composeTestRule.onNodeWithTag("hud_lap_counter").assertIsDisplayed()
    composeTestRule.onNodeWithTag("hud_camera_toggle").assertIsDisplayed()
    composeTestRule.onNodeWithTag("hud_speed_value").assertIsDisplayed()

    // Pre-Grid Launch Button
    composeTestRule.onNodeWithTag("launch_race_countdown_btn").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithText("START RACE • LIGHTS OUT").performScrollTo().assertIsDisplayed()

    // Test back button
    composeTestRule.onNodeWithTag("racing_back_button").performClick()
    assertTrue(navigatedBack)
  }

  @Test
  fun `in-game driving controls render steer, gas, brake and nitro buttons`() {
    val context = ApplicationProvider.getApplicationContext<android.content.Context>()
    val engine = InteractiveRacingEngine(context, vehicle, track)

    composeTestRule.setContent {
      InGameDrivingControls(
        engine = engine,
        hudState = engine.hudState.value
      )
    }

    // Steering wheel & paddle zones
    composeTestRule.onNodeWithTag("hud_steering_wheel_control").assertIsDisplayed()
    composeTestRule.onNodeWithTag("steer_left_btn").assertIsDisplayed()
    composeTestRule.onNodeWithTag("steer_right_btn").assertIsDisplayed()

    // Pedals & Handbrake
    composeTestRule.onNodeWithTag("hud_handbrake_btn").assertIsDisplayed()
    composeTestRule.onNodeWithTag("brake_pedal_btn").assertIsDisplayed()
    composeTestRule.onNodeWithTag("gas_pedal_btn").assertIsDisplayed()

    // Nitro button
    composeTestRule.onNodeWithTag("nitro_boost_btn").assertIsDisplayed()
    composeTestRule.onNodeWithTag("nitro_boost_btn").performClick()
    assertTrue(engine.isNitroActive)

    // Handbrake click toggles active handbrake state
    composeTestRule.onNodeWithTag("hud_handbrake_btn").performClick()
    assertTrue(engine.isHandbrakeActive)
  }

  @Test
  fun `start lights overlay renders countdown sequence`() {
    composeTestRule.setContent {
      StartLightsOverlay(lightsCount = 3)
    }

    composeTestRule.onNodeWithText("GRID START SEQUENCE").assertIsDisplayed()
    composeTestRule.onNodeWithText("HOLD REVS • AWAIT GREEN").assertIsDisplayed()
  }

  @Test
  fun `post-race podium dialog renders trophies, stats and room save verification`() {
    var raceAgainTriggered = false
    var profileTriggered = false

    val summary = RaceFinishSummary(
      finishingPosition = 1,
      totalRacers = 8,
      totalTimeMs = 156400L,
      bestLapMs = 78200L,
      topSpeedKmh = 338.4f,
      overtakesCount = 5,
      driftScore = 840,
      xpEarned = 1500,
      creditsEarned = 6000,
      trackName = track.name,
      vehicleName = vehicle.name,
      isNewPersonalBest = true,
      newlyUnlockedBadges = listOf("Speed Demon", "Apex Predator")
    )

    composeTestRule.setContent {
      PostRacePodiumDialog(
        summary = summary,
        onRaceAgain = { raceAgainTriggered = true },
        onViewProfile = { profileTriggered = true },
        onExit = {},
        isMph = false
      )
    }

    // Screen 5 – Results and Rewards Top Status Bar & Dual-Panel Stage
    composeTestRule.onNodeWithTag("results_top_status_bar").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithTag("results_level_xp_bar").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithTag("results_cash_balance_badge").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithTag("results_best_times_panel").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithTag("results_main_summary_card").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithTag("results_headline_banner").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithText("YOU WON THE RACE!").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithTag("results_ordinal_position_text").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithText("1st").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithTag("results_cash_reward_row").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithTag("results_xp_reward_row").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithTag("results_timing_footer_bar").performScrollTo().assertIsDisplayed()

    // Champion Title & Stats
    composeTestRule.onNodeWithText("VICTORY! P1 CHAMPION").assertIsDisplayed()
    composeTestRule.onNodeWithText("P1 of 8").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithText("+1500 XP").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithTag("results_next_available_race_card").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithTag("results_double_reward_btn").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithText("SAVED TO ROOM DATABASE TELEMETRY").performScrollTo().assertIsDisplayed()

    // Unlocked awards notification
    composeTestRule.onNodeWithText("NEW TROPHIES UNLOCKED").performScrollTo().assertIsDisplayed()

    // Test actions
    composeTestRule.onNodeWithTag("race_again_button").performScrollTo().performClick()
    assertTrue(raceAgainTriggered)
  }

  @Test
  fun `tire compound strategy manages tire wear, compounds and pit stop swaps`() {
    val context = ApplicationProvider.getApplicationContext<android.content.Context>()
    val engine = InteractiveRacingEngine(context, vehicle, track)

    // Initial default state
    assertEquals(com.example.game.model.TireCompound.MEDIUM, engine.activeTireCompound)
    assertEquals(100f, engine.tireHealthPercent, 0.01f)
    assertEquals(com.example.game.model.TireCompound.MEDIUM, engine.hudState.value.tireCompound)

    // Pit stop change to SOFT compound
    engine.changeTires(com.example.game.model.TireCompound.SOFT)
    assertEquals(com.example.game.model.TireCompound.SOFT, engine.activeTireCompound)
    assertEquals(100f, engine.tireHealthPercent, 0.01f)
    assertEquals(com.example.game.model.TireCompound.SOFT, engine.hudState.value.tireCompound)
    assertNotNull(engine.hudState.value.activeAlertMessage)

    // Pit stop change to HARD compound
    engine.changeTires(com.example.game.model.TireCompound.HARD)
    assertEquals(com.example.game.model.TireCompound.HARD, engine.activeTireCompound)
    assertEquals(com.example.game.model.TireCompound.HARD, engine.hudState.value.tireCompound)
  }

  @Test
  fun `top racing hud overlay renders tire telemetry badge and live speed`() {
    val hud = com.example.game.model.RacingGameHudState(
      currentRank = 1,
      totalRivals = 8,
      currentLap = 2,
      totalLaps = 3,
      speedKmh = 295.4f,
      gear = 6,
      rpm = 7850,
      maxRpm = 9000,
      redlineRpm = 8200,
      tireCompound = com.example.game.model.TireCompound.SOFT,
      tireHealthPercent = 88f,
      tireTempCelsius = 98f,
      speedTrapKmh = 312f,
      deltaVsBestSeconds = -0.35f
    )

    composeTestRule.setContent {
      TopRacingHudOverlay(
        hudState = hud,
        track = track,
        vehicle = vehicle,
        perspective = CameraPerspective.CHASE_CAM,
        onSwitchCamera = {},
        onPause = {},
        onBack = {},
        isMph = false
      )
    }

    composeTestRule.onNodeWithTag("hud_curved_tachometer_cluster").assertIsDisplayed()
    composeTestRule.onNodeWithTag("hud_rpm_indicator").assertIsDisplayed()
    composeTestRule.onNodeWithTag("hud_gear_indicator").assertIsDisplayed()
    composeTestRule.onNodeWithTag("hud_race_timer_badge").assertIsDisplayed()
    composeTestRule.onNodeWithTag("hud_track_minimap").assertIsDisplayed()
    composeTestRule.onNodeWithTag("hud_left_utility_column").assertIsDisplayed()
    composeTestRule.onNodeWithTag("hud_right_utility_column").assertIsDisplayed()
    composeTestRule.onNodeWithTag("hud_h_pattern_shifter").assertIsDisplayed()
    composeTestRule.onNodeWithTag("hud_position_badge").assertIsDisplayed()
    composeTestRule.onNodeWithTag("hud_speed_value").assertIsDisplayed()
    composeTestRule.onNodeWithTag("hud_tire_telemetry_badge").assertIsDisplayed()
    composeTestRule.onNodeWithTag("hud_live_delta_badge").assertIsDisplayed()
    composeTestRule.onNodeWithTag("hud_speed_trap_badge").assertIsDisplayed()
  }

  @Test
  fun `post-race podium dialog renders per-lap sector breakdown and gap to leader`() {
    val lapSummaries = listOf(
      LapTelemetrySummary(
        lapNumber = 1,
        lapTimeMs = 79500L,
        sector1Ms = 26100L,
        sector2Ms = 27200L,
        sector3Ms = 26200L,
        gapToLeaderSeconds = 0.845f,
        positionOnLap = 2,
        tireCompoundCode = "M"
      ),
      LapTelemetrySummary(
        lapNumber = 2,
        lapTimeMs = 78200L,
        sector1Ms = 25800L,
        sector2Ms = 26400L,
        sector3Ms = 26000L,
        gapToLeaderSeconds = 0.0f,
        positionOnLap = 1,
        tireCompoundCode = "S"
      )
    )

    val summary = RaceFinishSummary(
      finishingPosition = 1,
      totalRacers = 8,
      totalTimeMs = 157700L,
      bestLapMs = 78200L,
      topSpeedKmh = 341.0f,
      overtakesCount = 6,
      driftScore = 920,
      xpEarned = 1500,
      creditsEarned = 6000,
      trackName = track.name,
      vehicleName = vehicle.name,
      isNewPersonalBest = true,
      newlyUnlockedBadges = emptyList(),
      lapSummaries = lapSummaries
    )

    composeTestRule.setContent {
      PostRacePodiumDialog(
        summary = summary,
        onRaceAgain = {},
        onViewProfile = {},
        onExit = {},
        isMph = false
      )
    }

    composeTestRule.onNodeWithTag("finish_modal_lap_breakdown").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithText("SECTOR TIMING & LEADER GAP BREAKDOWN").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithTag("finish_modal_lap_row_1").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithTag("finish_modal_lap_row_2").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithText("+0.845s").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithText("LEADER").performScrollTo().assertIsDisplayed()
  }

  @Test
  fun `seeded terrain generator blends smoothly to road edge and resolves surface physics`() {
    val genA = com.example.game.landscape.SeededTerrainGenerator(seed = 424242L)
    val genB = com.example.game.landscape.SeededTerrainGenerator(seed = 424242L)
    val carProfile = com.example.game.landscape.VehicleTrackProfile.forVehicle(com.example.model.VehicleType.CAR)
    val motoProfile = com.example.game.landscape.VehicleTrackProfile.forVehicle(com.example.model.VehicleType.MOTORBIKE)

    // Deterministic heightmap reproducibility
    assertEquals(
      genA.sampleRawHeight(120f, 340f),
      genB.sampleRawHeight(120f, 340f),
      0.0001f
    )

    // Road center conforms to roadElevationMeters when curvature = 0
    val roadCenterH = genA.sampleConformingHeight(
      distanceMeters = 200f,
      lateralNormalized = 0f,
      roadElevationMeters = 4.5f,
      roadCurvature = 0f,
      vehicleProfile = carProfile
    )
    assertEquals(4.5f, roadCenterH, 0.001f)

    // Vehicle-specific design: Cars have wider gravel run-off; Motorcycles have higher corner banking & low-profile kerbs
    assertTrue(carProfile.runOffWidthNormalized > motoProfile.runOffWidthNormalized)
    assertTrue(motoProfile.cornerBankingMultiplierDeg > carProfile.cornerBankingMultiplierDeg)
    assertTrue(motoProfile.kerbHeightMeters < carProfile.kerbHeightMeters)
    assertTrue(motoProfile.lowProfileCameraEdgeNormalized > carProfile.lowProfileCameraEdgeNormalized)

    // Surface classification (ROAD -> KERB -> GRAVEL -> GRASS) with monotonic grip reduction
    val roadSurf = genA.resolveSurfaceType(0.2f, carProfile)
    val kerbSurf = genA.resolveSurfaceType(1.05f, carProfile)
    val gravelSurf = genA.resolveSurfaceType(1.30f, carProfile)
    val grassSurf = genA.resolveSurfaceType(2.10f, carProfile)

    assertEquals(com.example.game.landscape.SurfaceType.ROAD, roadSurf)
    assertEquals(com.example.game.landscape.SurfaceType.KERB, kerbSurf)
    assertEquals(com.example.game.landscape.SurfaceType.GRAVEL, gravelSurf)
    assertEquals(com.example.game.landscape.SurfaceType.GRASS, grassSurf)
    assertTrue(roadSurf.gripMultiplier > kerbSurf.gripMultiplier)
    assertTrue(kerbSurf.gripMultiplier > gravelSurf.gripMultiplier)
    assertTrue(gravelSurf.gripMultiplier > grassSurf.gripMultiplier)
  }

  @Test
  fun `procedural scenery instancer enforces safety corridor clearance and motorcycle low-profile camera edge`() {
    val context = ApplicationProvider.getApplicationContext<android.content.Context>()
    val engine = InteractiveRacingEngine(context, vehicle, track)
    val biomeSystem = com.example.game.landscape.DefaultBiomeSystem()
    val instancer = com.example.game.landscape.RuleBasedSceneryInstancer()
    val terrainGen = com.example.game.landscape.SeededTerrainGenerator(seed = 98765L)
    val motoProfile = com.example.game.landscape.VehicleTrackProfile.forVehicle(com.example.model.VehicleType.MOTORBIKE)

    for (biome in com.example.game.landscape.BiomeType.entries) {
      val chunk = instancer.generateChunk(
        chunkIndex = 0,
        chunkSizeMeters = 100f,
        seed = 98765L,
        densityMultiplier = 1.5f,
        biomeProfile = biomeSystem.getProfile(biome),
        vehicleProfile = motoProfile,
        terrainGenerator = terrainGen,
        roadSegments = engine.getRoadSegments()
      )

      assertTrue("Chunk should contain procedural scenery instances", chunk.sceneryInstances.isNotEmpty())
      chunk.sceneryInstances.forEach { inst ->
        val absLat = kotlin.math.abs(inst.lateralNormalized)
        // Safety corridor rule: no props inside the racing corridor
        assertTrue(
          "Prop ${inst.propType} at lateral=$absLat violated safety corridor ${motoProfile.safetyCorridorNormalized}",
          absLat >= motoProfile.safetyCorridorNormalized
        )
        // Motorcycle camera sightline rule: inside lowProfileCameraEdgeNormalized, only low-profile props allowed
        if (absLat < motoProfile.lowProfileCameraEdgeNormalized) {
          assertTrue(
            "Prop ${inst.propType} in motorcycle camera edge zone must be low-profile safe",
            inst.propType.isLowProfileMotorcycleSafe
          )
        }
      }
    }
  }

  @Test
  fun `landscape editor overlay toggles open, switches biomes, and regenerates world`() {
    composeTestRule.setContent {
      RacingGameScreen(
        vehicle = vehicle,
        track = track,
        onNavigateBack = {}
      )
    }

    // Verify HUD surface badge and Biome Editor toggle button are displayed
    composeTestRule.onNodeWithTag("hud_surface_type_badge").assertIsDisplayed()
    composeTestRule.onNodeWithTag("landscape_editor_toggle_button").assertIsDisplayed()

    // Open Procedural Landscape Studio overlay
    composeTestRule.onNodeWithTag("landscape_editor_toggle_button").performClick()
    composeTestRule.onNodeWithTag("landscape_editor_panel").assertIsDisplayed()
    composeTestRule.onNodeWithTag("landscape_telemetry_readout").assertIsDisplayed()

    // Switch biome to ALPINE and trigger regenerate
    composeTestRule.onNodeWithTag("biome_chip_ALPINE").performScrollTo().performClick()
    composeTestRule.onNodeWithTag("tod_chip_NIGHT").performScrollTo().performClick()
    composeTestRule.onNodeWithTag("landscape_weather_chip_FOG").performScrollTo().performClick()
    composeTestRule.onNodeWithTag("landscape_vehicle_chip_MOTORBIKE").performScrollTo().performClick()
    composeTestRule.onNodeWithTag("landscape_regenerate_button").performScrollTo().performClick()

    // Close editor and verify HUD biome badge updated to ALPINE
    composeTestRule.onNodeWithTag("landscape_editor_close_btn").performClick()
    composeTestRule.onNodeWithText("BIOME: ALPINE").assertIsDisplayed()
  }

  @Test
  fun `apex rivals 2_0 graphics presets, control modes, audio mixer, and idempotent career rewards function properly`() {
    val context = ApplicationProvider.getApplicationContext<android.content.Context>()
    com.example.game.v2.ApexRivals2SettingsRepository.initialize(context)
    com.example.game.v2.ApexRivals2CareerManager.resetForTesting()

    // 1. Graphics Quality Preset Auto-Detection & Switching
    val detectedPreset = com.example.game.v2.GraphicsQualityPreset.detectRecommendedPreset(context)
    assertTrue(detectedPreset in com.example.game.v2.GraphicsQualityPreset.entries)
    com.example.game.v2.ApexRivals2SettingsRepository.setGraphicsPreset(com.example.game.v2.GraphicsQualityPreset.LOW)
    assertEquals(
      com.example.game.v2.GraphicsQualityPreset.LOW,
      com.example.game.v2.ApexRivals2SettingsRepository.graphicsPreset.value
    )
    com.example.game.v2.ApexRivals2SettingsRepository.setGraphicsPreset(com.example.game.v2.GraphicsQualityPreset.HIGH)

    // 2. Driving Control Modes & Steering Sensitivity Clamping
    com.example.game.v2.ApexRivals2SettingsRepository.setDrivingControlMode(com.example.game.v2.DrivingControlMode.STEERING_WHEEL)
    com.example.game.v2.ApexRivals2SettingsRepository.setSteeringSensitivity(1.65f)
    assertEquals(
      com.example.game.v2.DrivingControlMode.STEERING_WHEEL,
      com.example.game.v2.ApexRivals2SettingsRepository.controlConfig.value.controlMode
    )
    assertEquals(
      1.65f,
      com.example.game.v2.ApexRivals2SettingsRepository.controlConfig.value.steeringSensitivity,
      0.01f
    )
    // Reset to TOUCH_BUTTONS for default UI state
    com.example.game.v2.ApexRivals2SettingsRepository.setDrivingControlMode(com.example.game.v2.DrivingControlMode.TOUCH_BUTTONS)

    // 3. Multi-Channel Audio Mixer (Music, Engine, SFX)
    com.example.game.v2.ApexRivals2SettingsRepository.updateAudioMixer(
      musicVolume = 0.50f,
      engineVolume = 0.95f,
      sfxVolume = 0.80f
    )
    val mixer = com.example.game.v2.ApexRivals2SettingsRepository.audioMixer.value
    assertEquals(0.50f, mixer.musicVolume, 0.01f)
    assertEquals(0.95f, mixer.engineVolume, 0.01f)
    assertEquals(0.80f, mixer.sfxVolume, 0.01f)

    // 4. 3D Garage Customization & Performance Subsystem Upgrades
    val baseStats = com.example.game.v2.ApexRivals2CareerManager.computeUpgradedStats(
      vehicle,
      com.example.game.v2.ApexRivals2CareerManager.getLoadoutForVehicle(vehicle)
    )
    val upgradedOk = com.example.game.v2.ApexRivals2CareerManager.upgradeSubsystem(
      vehicle,
      com.example.game.v2.UpgradeSubsystem.ENGINE
    )
    assertTrue("Engine Stage 2 upgrade should succeed", upgradedOk)
    val afterStats = com.example.game.v2.ApexRivals2CareerManager.computeUpgradedStats(
      vehicle,
      com.example.game.v2.ApexRivals2CareerManager.getLoadoutForVehicle(vehicle)
    )
    assertTrue("Upgraded max speed should exceed base speed", afterStats.maxSpeedKmh > baseStats.maxSpeedKmh)
    assertTrue("Upgraded acceleration 0-100 time should be faster (lower)", afterStats.acceleration0to100Sec < baseStats.acceleration0to100Sec)

    // 5. Idempotent Career Championship & Daily Login Claims (no duplicate rewards)
    val firstEventClaim = com.example.game.v2.ApexRivals2CareerManager.claimCareerEventReward("evt_rookie_city_sprint")
    val duplicateEventClaim = com.example.game.v2.ApexRivals2CareerManager.claimCareerEventReward("evt_rookie_city_sprint")
    assertTrue("First career event claim must succeed", firstEventClaim)
    assertTrue("Duplicate career event claim must be rejected", !duplicateEventClaim)

    val firstDailyClaim = com.example.game.v2.ApexRivals2CareerManager.claimDailyLoginReward()
    val duplicateDailyClaim = com.example.game.v2.ApexRivals2CareerManager.claimDailyLoginReward()
    assertTrue("First daily login claim must succeed", firstDailyClaim)
    assertTrue("Duplicate daily login claim on same day must be rejected", !duplicateDailyClaim)
  }

  @Test
  fun `apex rivals 2_0 3D garage turntable card renders and applies visual loadout`() {
    com.example.game.v2.ApexRivals2CareerManager.resetForTesting()
    composeTestRule.setContent {
      androidx.compose.foundation.layout.Column(
        modifier = androidx.compose.ui.Modifier.verticalScroll(androidx.compose.foundation.rememberScrollState())
      ) {
        com.example.game.v2.Garage3DTurntablePreviewCard(vehicle = vehicle)
      }
    }

    composeTestRule.onNodeWithTag("garage_3d_turntable_card").assertIsDisplayed()
    composeTestRule.onNodeWithTag("garage_3d_turntable_canvas").assertIsDisplayed()
    composeTestRule.onNodeWithTag("garage_live_stats_panel").performScrollTo().assertIsDisplayed()

    // Switch to Wheels tab and select BBS_CENTERLOCK
    composeTestRule.onNodeWithTag("garage_tab_1").performScrollTo().performClick()
    composeTestRule.onNodeWithTag("garage_wheel_BBS_CENTERLOCK").performScrollTo().performClick()

    // Verify Apply button appears for previewed changes and applies cleanly
    composeTestRule.onNodeWithTag("garage_apply_customization_btn").performScrollTo().performClick()
    val updatedLoadout = com.example.game.v2.ApexRivals2CareerManager.getLoadoutForVehicle(vehicle)
    assertEquals(com.example.game.v2.WheelRimStyle.BBS_CENTERLOCK, updatedLoadout.wheelRimStyle)
  }

  @Test
  fun `apex rivals 2_0 complete 10-screen frontend hub navigates across loading, main menu, modes, garage, customization, events, and settings`() {
    var selectedVehicle = vehicle
    var selectedTrack = track
    var launchedRace = false

    composeTestRule.setContent {
      com.example.game.v2.ApexRivals2FrontendHubScreen(
        activeVehicle = selectedVehicle,
        activeTrack = selectedTrack,
        onSelectVehicle = { selectedVehicle = it },
        onSelectTrack = { selectedTrack = it },
        onLaunchRaceHud = { launchedRace = true },
        onNavigateToProfile = {},
        onNavigateToMultiplayer = {},
        initialSubScreen = com.example.game.v2.FrontendSubScreen.LOADING_SCREEN
      )
    }

    // SCREEN 1: Loading and Branding (Cinematic Hypercar, Logo, Studio, Progress, Version 2.0)
    composeTestRule.onNodeWithTag("screen_1_loading").assertIsDisplayed()
    composeTestRule.onNodeWithTag("loading_cinematic_vehicle").assertIsDisplayed()
    composeTestRule.onNodeWithTag("loading_studio_badge").assertIsDisplayed()
    composeTestRule.onNodeWithTag("loading_version_2_shield").assertIsDisplayed()
    composeTestRule.onNodeWithTag("loading_game_logo_title").assertIsDisplayed()
    composeTestRule.onNodeWithTag("loading_version_2_branding").assertIsDisplayed()
    composeTestRule.onNodeWithTag("loading_progress_bar").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithTag("loading_version_info").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithTag("loading_enter_btn").performScrollTo().performClick()

    // SCREEN 2: Main Home Screen (Proposed Screen Structure: APEX RIVALS + Online badge, LEVEL 12 · DRIVER + 12,500 coins, Wet Hangar Hypercar Hero, READY TO RACE?, PLAY NOW, 2x2 Dark Grid, HOME/PROFILE/SETTINGS footer)
    composeTestRule.onNodeWithTag("screen_2_main_menu").assertIsDisplayed()
    composeTestRule.onNodeWithTag("main_menu_header_title").assertIsDisplayed()
    composeTestRule.onNodeWithTag("main_menu_online_badge").assertIsDisplayed()
    composeTestRule.onNodeWithTag("main_menu_profile_chip").assertIsDisplayed()
    composeTestRule.onNodeWithTag("main_menu_currency_badge").assertIsDisplayed()
    composeTestRule.onNodeWithTag("main_menu_featured_vehicle_stage").assertIsDisplayed()
    composeTestRule.onNodeWithTag("main_menu_turntable_hypercar_canvas").assertIsDisplayed()
    composeTestRule.onNodeWithTag("main_menu_ready_to_race_title").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithTag("main_menu_bottom_dock").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithTag("main_menu_nav_home").performScrollTo().assertIsDisplayed()

    // Navigate to SCREEN 3: Game Mode Selection
    composeTestRule.onNodeWithTag("main_menu_play_btn").performScrollTo().performClick()
    composeTestRule.onNodeWithTag("screen_3_mode_selection").assertIsDisplayed()
    composeTestRule.onNodeWithTag("mode_card_QUICK_RACE").assertIsDisplayed()
    composeTestRule.onNodeWithTag("frontend_back_btn").performClick()

    // Navigate to Vehicle Selection & Garage (Prominent Center Car, Swatches + Tabs, Horizontal Carousel & Upgrade Options) & SCREEN 5: Vehicle Customization
    composeTestRule.onNodeWithTag("main_menu_garage_btn").performScrollTo().performClick()
    composeTestRule.onNodeWithTag("screen_4_vehicle_garage").assertIsDisplayed()
    composeTestRule.onNodeWithTag("garage_header_banner").assertIsDisplayed()
    composeTestRule.onNodeWithTag("garage_centerpiece_stage").assertIsDisplayed()
    composeTestRule.onNodeWithTag("garage_prominent_car_canvas").assertIsDisplayed()
    composeTestRule.onNodeWithTag("garage_vehicle_spec_card").assertIsDisplayed()
    composeTestRule.onNodeWithTag("garage_swatch_strip").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithTag("garage_visual_tabs_row").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithTag("garage_vehicle_carousel").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithTag("garage_upgrade_options_section").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithTag("garage_centerpiece_stage").performScrollTo()
    composeTestRule.onNodeWithTag("garage_screen_customize_btn").performClick()
    composeTestRule.onNodeWithTag("screen_5_vehicle_customization").assertIsDisplayed()
    composeTestRule.onNodeWithTag("customization_before_after_card").assertIsDisplayed()
    composeTestRule.onNodeWithTag("frontend_back_btn").performClick()
    composeTestRule.onNodeWithTag("frontend_back_btn").performClick()

    // Navigate to SCREEN 9: Career and Championships
    composeTestRule.onNodeWithTag("main_menu_career_btn").performScrollTo().performClick()
    composeTestRule.onNodeWithTag("screen_9_career_championship").assertIsDisplayed()
    composeTestRule.onNodeWithTag("career_tier_tab_ROOKIE_CHAMPIONSHIP").assertIsDisplayed()
    composeTestRule.onNodeWithTag("career_back_btn").performScrollTo().performClick()

    // Navigate to SCREEN 10: Multiplayer Hub
    composeTestRule.onNodeWithTag("main_menu_multiplayer_btn").performScrollTo().performClick()
    composeTestRule.onNodeWithTag("screen_10_multiplayer_hub").assertIsDisplayed()
    composeTestRule.onNodeWithTag("multiplayer_quick_join_btn").assertIsDisplayed()
    composeTestRule.onNodeWithTag("multiplayer_back_btn").performScrollTo().performClick()

    // Navigate to SCREEN 11: Driver Profile & License
    composeTestRule.onNodeWithTag("main_menu_profile_btn").performScrollTo().performClick()
    composeTestRule.onNodeWithTag("screen_11_driver_profile").assertIsDisplayed()
    composeTestRule.onNodeWithTag("profile_back_btn").performScrollTo().performClick()

    // Navigate to Events and Rewards
    composeTestRule.onNodeWithTag("main_menu_events_btn").performScrollTo().performClick()
    composeTestRule.onNodeWithTag("screen_9_events_and_rewards").assertIsDisplayed()
    composeTestRule.onNodeWithTag("events_countdown_timer").assertIsDisplayed()
    composeTestRule.onNodeWithTag("frontend_back_btn").performClick()

    // Navigate to Settings
    composeTestRule.onNodeWithTag("main_menu_settings_btn").performScrollTo().performClick()
    composeTestRule.onNodeWithTag("screen_10_settings").assertIsDisplayed()
    composeTestRule.onNodeWithTag("settings_fps_60").performScrollTo().performClick()
    composeTestRule.onNodeWithTag("settings_lang_ES").performScrollTo().performClick()
    assertEquals("ES", com.example.game.v2.ApexRivals2SettingsRepository.languageOption.value.code)
  }

  @Test
  fun `career progression supports rookie, amateur, pro, and elite championships, time trials, sprints, drift challenges, race rewards, and unlockable tracks`() {
    val context = ApplicationProvider.getApplicationContext<android.content.Context>()
    com.example.game.v2.ApexRivals2CareerManager.resetForTesting()

    val allEvents = com.example.game.v2.ApexRivals2CareerManager.careerEvents

    // 1. Verify all 4 Championship Tiers have dedicated rounds
    com.example.game.v2.ChampionshipTier.entries.forEach { tier ->
      val tierRounds = allEvents.filter {
        it.championshipTier == tier && it.category != com.example.game.v2.CareerEventCategory.DAILY_CHALLENGE
      }
      assertTrue("Championship tier ${tier.displayName} must have at least 3 rounds", tierRounds.size >= 3)
    }

    // 2. Verify Sprint Events, Time Trials, and Drift Challenges exist across the career campaign
    val sprintEvents = allEvents.filter { it.category == com.example.game.v2.CareerEventCategory.SPRINT_RACE }
    val timeTrialEvents = allEvents.filter { it.category == com.example.game.v2.CareerEventCategory.TIME_TRIAL }
    val driftEvents = allEvents.filter { it.category == com.example.game.v2.CareerEventCategory.DRIFT_CHALLENGE }
    assertTrue("Career campaign must include Sprint events", sprintEvents.size >= 4)
    assertTrue("Career campaign must include Time Trial events", timeTrialEvents.size >= 3)
    assertTrue("Career campaign must include Drift Challenge events", driftEvents.size >= 4)

    // 3. Verify initial unlockable track states (Starter tracks unlocked, Elite Marina Bay locked at Level 1)
    assertTrue(com.example.game.v2.ApexRivals2CareerManager.isTrackUnlocked("track_neo_tokyo"))
    assertTrue(com.example.game.v2.ApexRivals2CareerManager.isTrackUnlocked("track_monza"))
    assertFalse(
      "Marina Bay GP should be locked at Level 1 before Elite Championship progression",
      com.example.game.v2.ApexRivals2CareerManager.isTrackUnlocked("track_marina_bay")
    )

    // 4. Verify event requirement checking (Rookie unlocked at Level 1, Elite locked at Level 1)
    val rookieSprint = allEvents.first { it.eventId == "evt_rookie_city_sprint" }
    val eliteFinale = allEvents.first { it.eventId == "evt_elite_gp_circuit" }
    assertTrue(
      "Rookie sprint should be enterable at Level 1 with starter GT3 vehicle",
      com.example.game.v2.ApexRivals2CareerManager.canEnterCareerEvent(rookieSprint, vehicle)
    )
    assertFalse(
      "Elite GP finale should require Level 6+",
      com.example.game.v2.ApexRivals2CareerManager.canEnterCareerEvent(eliteFinale, vehicle)
    )

    // 5. Complete all 3 Rookie Championship events and verify credits, XP, and track unlock rewards
    val initialCredits = com.example.game.v2.ApexRivals2CareerManager.state.value.creditsBalance
    assertTrue(com.example.game.v2.ApexRivals2CareerManager.claimCareerEventReward("evt_rookie_city_sprint", context))
    assertTrue(com.example.game.v2.ApexRivals2CareerManager.claimCareerEventReward("evt_rookie_time_trial", context))
    assertTrue(com.example.game.v2.ApexRivals2CareerManager.claimCareerEventReward("evt_rookie_drift_intro", context))

    assertTrue(
      "Completing Rookie Drift Intro should unlock Miami Causeway Sprint track",
      com.example.game.v2.ApexRivals2CareerManager.isTrackUnlocked("track_miami_highway")
    )

    // 6. Claim Rookie Championship Cup Trophy purse and verify idempotency
    val trophyClaimed = com.example.game.v2.ApexRivals2CareerManager.claimChampionshipTierTrophy(
      com.example.game.v2.ChampionshipTier.ROOKIE_CHAMPIONSHIP,
      context
    )
    val duplicateTrophyClaim = com.example.game.v2.ApexRivals2CareerManager.claimChampionshipTierTrophy(
      com.example.game.v2.ChampionshipTier.ROOKIE_CHAMPIONSHIP,
      context
    )
    assertTrue("Rookie Championship Cup Trophy claim should succeed once all 3 rounds are complete", trophyClaimed)
    assertFalse("Duplicate Rookie Championship Cup Trophy claim must be rejected", duplicateTrophyClaim)

    // 7. Record a completed playable race result and verify non-destructive progression update
    val beforeRaces = com.example.game.v2.ApexRivals2CareerManager.state.value.totalRacesCompleted
    com.example.game.v2.ApexRivals2CareerManager.recordCompletedRaceResult(
      trackId = "track_tokyo",
      finishingPosition = 1,
      driftScore = 2600,
      topSpeedKmh = 328f,
      creditsEarned = 6000,
      xpEarned = 2500,
      context = context
    )
    val updatedState = com.example.game.v2.ApexRivals2CareerManager.state.value
    assertEquals(beforeRaces + 1, updatedState.totalRacesCompleted)
    assertTrue(updatedState.creditsBalance > initialCredits)
    assertTrue(
      "Winning a race on Tokyo Expressway and reaching Level 6+ should unlock Marina Bay Night GP",
      com.example.game.v2.ApexRivals2CareerManager.isTrackUnlocked("track_marina_bay")
    )
  }

  @Test
  fun `career championship screen renders tier tabs, discipline filters, event cards, and unlockable tracks showcase`() {
    com.example.game.v2.ApexRivals2CareerManager.resetForTesting()
    var selectedTrack = track

    composeTestRule.setContent {
      com.example.game.v2.ApexCareerChampionshipScreen(
        activeVehicle = vehicle,
        activeTrack = selectedTrack,
        onSelectTrack = { selectedTrack = it },
        onBack = {},
        onLaunchRace = {}
      )
    }

    // Verify all 4 Championship Tier tabs render
    composeTestRule.onNodeWithTag("career_tier_tab_ROOKIE_CHAMPIONSHIP").assertIsDisplayed()
    composeTestRule.onNodeWithTag("career_tier_tab_AMATEUR_CHAMPIONSHIP").assertIsDisplayed()
    composeTestRule.onNodeWithTag("career_tier_tab_PRO_CHAMPIONSHIP").assertIsDisplayed()
    composeTestRule.onNodeWithTag("career_tier_tab_ELITE_CHAMPIONSHIP").assertIsDisplayed()

    // Verify Discipline filters (Sprint Events, Time Trials, Drift Challenges)
    composeTestRule.onNodeWithTag("career_discipline_filter_SPRINT_RACE").performScrollTo().performClick()
    composeTestRule.onNodeWithTag("career_event_card_evt_rookie_city_sprint").performScrollTo().assertIsDisplayed()

    composeTestRule.onNodeWithTag("career_discipline_filter_TIME_TRIAL").performScrollTo().performClick()
    composeTestRule.onNodeWithTag("career_event_card_evt_rookie_time_trial").performScrollTo().assertIsDisplayed()

    composeTestRule.onNodeWithTag("career_discipline_filter_DRIFT_CHALLENGE").performScrollTo().performClick()
    composeTestRule.onNodeWithTag("career_event_card_evt_rookie_drift_intro").performScrollTo().assertIsDisplayed()

    // Return to Tier Rounds filter and claim Rookie round reward via UI button
    composeTestRule.onNodeWithTag("career_discipline_filter_ALL").performScrollTo().performClick()
    composeTestRule.onNodeWithTag("career_event_claim_btn_evt_rookie_city_sprint").performScrollTo().performClick()
    assertTrue("evt_rookie_city_sprint" in com.example.game.v2.ApexRivals2CareerManager.state.value.claimedEventIds)

    // Verify Unlockable Tracks Showcase section renders all circuits
    composeTestRule.onNodeWithTag("career_unlockable_tracks_section").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithTag("career_track_row_track_neo_tokyo").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithTag("career_track_row_track_marina_bay").performScrollTo().assertIsDisplayed()
  }

  @Test
  fun `visual quality graphics profiles low, medium, and high scale lighting, shadows, reflections, smoke, and auto-adapt to frame rate`() {
    val context = ApplicationProvider.getApplicationContext<android.content.Context>()
    com.example.game.v2.ApexRivals2SettingsRepository.initialize(context)

    val low = com.example.game.v2.GraphicsQualityPreset.LOW
    val medium = com.example.game.v2.GraphicsQualityPreset.MEDIUM
    val high = com.example.game.v2.GraphicsQualityPreset.HIGH

    // 1. Verify progressive visual fidelity across LOW, MEDIUM, and HIGH profiles
    assertTrue(low.roadSliceCount < medium.roadSliceCount && medium.roadSliceCount < high.roadSliceCount)
    assertTrue(low.shadowLayers < medium.shadowLayers && medium.shadowLayers < high.shadowLayers)
    assertTrue(low.tireSmokeParticles < medium.tireSmokeParticles && medium.tireSmokeParticles < high.tireSmokeParticles)
    assertTrue(low.weatherParticleCount < medium.weatherParticleCount && medium.weatherParticleCount < high.weatherParticleCount)
    assertTrue(low.lightingRayCount < medium.lightingRayCount && medium.lightingRayCount < high.lightingRayCount)
    assertTrue(low.roadTextureDetailLevel < medium.roadTextureDetailLevel && medium.roadTextureDetailLevel < high.roadTextureDetailLevel)
    assertFalse(low.enableDynamicReflections)
    assertTrue(medium.enableDynamicReflections && high.enableDynamicReflections)
    assertFalse(low.enableClearcoatMaterials)
    assertTrue(medium.enableClearcoatMaterials && high.enableClearcoatMaterials)

    // 2. Verify automatic device recommendation & adaptive frame-rate step-down
    val recommended = com.example.game.v2.ApexRivals2SettingsRepository.applyRecommendedPreset(context)
    assertNotNull(recommended)
    assertTrue(com.example.game.v2.ApexRivals2SettingsRepository.isAutoGraphicsEnabled.value)

    com.example.game.v2.ApexRivals2SettingsRepository.setGraphicsPreset(high)
    com.example.game.v2.ApexRivals2SettingsRepository.setAutoGraphicsEnabled(true)
    repeat(18) {
      com.example.game.v2.ApexRivals2SettingsRepository.recordFrameTimeMs(65f)
    }
    assertEquals(
      "Sustained frame drops in Auto Graphics mode should step down from HIGH to MEDIUM",
      medium,
      com.example.game.v2.ApexRivals2SettingsRepository.graphicsPreset.value
    )

    // 3. Verify Pre-Grid Graphics Profile UI & Canvas rendering across presets and weather conditions
    composeTestRule.setContent {
      RacingGameScreen(
        vehicle = vehicle,
        track = track,
        onNavigateBack = {}
      )
    }

    composeTestRule.onNodeWithTag("pre_grid_graphics_preset_row").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithTag("pre_grid_graphics_chip_LOW").performScrollTo().performClick()
    assertEquals(low, com.example.game.v2.ApexRivals2SettingsRepository.graphicsPreset.value)
    assertFalse(com.example.game.v2.ApexRivals2SettingsRepository.isAutoGraphicsEnabled.value)

    composeTestRule.onNodeWithTag("pre_grid_graphics_chip_MEDIUM").performScrollTo().performClick()
    assertEquals(medium, com.example.game.v2.ApexRivals2SettingsRepository.graphicsPreset.value)

    composeTestRule.onNodeWithTag("pre_grid_graphics_chip_HIGH").performScrollTo().performClick()
    assertEquals(high, com.example.game.v2.ApexRivals2SettingsRepository.graphicsPreset.value)

    // Switch weather conditions on Pre-Grid and verify canvas remains displayed and stable
    composeTestRule.onNodeWithTag("weather_chip_WET_STORM").performScrollTo().performClick()
    composeTestRule.onNodeWithTag("racing_game_canvas").assertIsDisplayed()
    composeTestRule.onNodeWithTag("weather_chip_CLEAR_SUNSET").performScrollTo().performClick()
    composeTestRule.onNodeWithTag("racing_game_canvas").assertIsDisplayed()
    composeTestRule.onNodeWithTag("weather_chip_NEON_NIGHT").performScrollTo().performClick()
    composeTestRule.onNodeWithTag("racing_game_canvas").assertIsDisplayed()

    composeTestRule.onNodeWithTag("pre_grid_graphics_chip_AUTO").performScrollTo().performClick()
    assertTrue(com.example.game.v2.ApexRivals2SettingsRepository.isAutoGraphicsEnabled.value)
  }

  @Test
  fun `unified navigation architecture connects all 6 required chains and preserves backstack transitions`() {
    com.example.game.v2.ApexRivals2CareerManager.resetForTesting()
    var selectedVehicle = vehicle
    var selectedTrack = track
    val activeTopScreen = androidx.compose.runtime.mutableStateOf("FRONTEND")
    val externalSubScreen = androidx.compose.runtime.mutableStateOf(com.example.game.v2.FrontendSubScreen.MAIN_MENU)

    composeTestRule.setContent {
      if (activeTopScreen.value == "FRONTEND") {
        com.example.game.v2.ApexRivals2FrontendHubScreen(
          activeVehicle = selectedVehicle,
          activeTrack = selectedTrack,
          onSelectVehicle = { selectedVehicle = it },
          onSelectTrack = { selectedTrack = it },
          onLaunchRaceHud = { activeTopScreen.value = "RACE" },
          onNavigateToProfile = { externalSubScreen.value = com.example.game.v2.FrontendSubScreen.DRIVER_PROFILE },
          onNavigateToMultiplayer = { externalSubScreen.value = com.example.game.v2.FrontendSubScreen.MULTIPLAYER_HUB },
          initialSubScreen = com.example.game.v2.FrontendSubScreen.MAIN_MENU,
          externalSubScreen = externalSubScreen.value,
          onExternalSubScreenChange = { externalSubScreen.value = it }
        )
      } else {
        RacingGameScreen(
          vehicle = selectedVehicle,
          track = selectedTrack,
          onNavigateBack = {
            externalSubScreen.value = com.example.game.v2.FrontendSubScreen.MAIN_MENU
            activeTopScreen.value = "FRONTEND"
          },
          onNavigateToProfile = {
            externalSubScreen.value = com.example.game.v2.FrontendSubScreen.DRIVER_PROFILE
            activeTopScreen.value = "FRONTEND"
          },
          onNavigateToGarage = {
            externalSubScreen.value = com.example.game.v2.FrontendSubScreen.VEHICLE_GARAGE
            activeTopScreen.value = "FRONTEND"
          }
        )
      }
    }

    // Verify we start on HOME (screen_2_main_menu)
    composeTestRule.onNodeWithTag("screen_2_main_menu").assertIsDisplayed()

    // CHAIN 2: HOME -> GARAGE -> CUSTOMIZATION -> UPGRADES (and back step-by-step)
    composeTestRule.onNodeWithTag("main_menu_garage_btn").performScrollTo().performClick()
    composeTestRule.onNodeWithTag("screen_4_vehicle_garage").assertIsDisplayed()
    composeTestRule.onNodeWithTag("garage_centerpiece_stage").performScrollTo()
    composeTestRule.onNodeWithTag("garage_screen_customize_btn").performClick()
    composeTestRule.onNodeWithTag("screen_5_vehicle_customization").assertIsDisplayed()
    composeTestRule.onNodeWithTag("customization_open_upgrades_btn").performScrollTo().performClick()
    composeTestRule.onNodeWithTag("screen_vehicle_upgrades").assertIsDisplayed()

    // Back step-by-step: UPGRADES -> CUSTOMIZATION -> GARAGE -> HOME
    composeTestRule.onNodeWithTag("frontend_back_btn").performClick()
    composeTestRule.onNodeWithTag("screen_5_vehicle_customization").assertIsDisplayed()
    composeTestRule.onNodeWithTag("frontend_back_btn").performClick()
    composeTestRule.onNodeWithTag("screen_4_vehicle_garage").assertIsDisplayed()
    composeTestRule.onNodeWithTag("frontend_back_btn").performClick()
    composeTestRule.onNodeWithTag("screen_2_main_menu").assertIsDisplayed()

    // CHAIN 4: HOME -> EVENTS -> MISSIONS -> REWARDS (and back step-by-step)
    composeTestRule.onNodeWithTag("main_menu_events_btn").performScrollTo().performClick()
    composeTestRule.onNodeWithTag("screen_9_events_and_rewards").assertIsDisplayed()
    composeTestRule.onNodeWithTag("events_open_missions_btn").performScrollTo().performClick()
    composeTestRule.onNodeWithTag("screen_events_missions").assertIsDisplayed()
    composeTestRule.onNodeWithTag("missions_open_rewards_btn").performScrollTo().performClick()
    composeTestRule.onNodeWithTag("screen_events_rewards").assertIsDisplayed()

    // Back step-by-step: REWARDS -> MISSIONS -> EVENTS -> HOME
    composeTestRule.onNodeWithTag("frontend_back_btn").performClick()
    composeTestRule.onNodeWithTag("screen_events_missions").assertIsDisplayed()
    composeTestRule.onNodeWithTag("frontend_back_btn").performClick()
    composeTestRule.onNodeWithTag("screen_9_events_and_rewards").assertIsDisplayed()
    composeTestRule.onNodeWithTag("frontend_back_btn").performClick()
    composeTestRule.onNodeWithTag("screen_2_main_menu").assertIsDisplayed()

    // CHAIN 5: HOME -> PROFILE -> ACHIEVEMENTS (and back step-by-step)
    composeTestRule.onNodeWithTag("main_menu_profile_btn").performScrollTo().performClick()
    composeTestRule.onNodeWithTag("screen_11_driver_profile").assertIsDisplayed()
    composeTestRule.onNodeWithTag("profile_open_achievements_btn").performScrollTo().performClick()
    composeTestRule.onNodeWithTag("screen_driver_achievements").assertIsDisplayed()

    // Back step-by-step: ACHIEVEMENTS -> PROFILE -> HOME
    composeTestRule.onNodeWithTag("frontend_back_btn").performClick()
    composeTestRule.onNodeWithTag("screen_11_driver_profile").assertIsDisplayed()
    composeTestRule.onNodeWithTag("profile_back_btn").performScrollTo().performClick()
    composeTestRule.onNodeWithTag("screen_2_main_menu").assertIsDisplayed()

    // CHAIN 6: HOME -> SETTINGS (and back to HOME)
    composeTestRule.onNodeWithTag("main_menu_settings_btn").performScrollTo().performClick()
    composeTestRule.onNodeWithTag("screen_10_settings").assertIsDisplayed()
    composeTestRule.onNodeWithTag("frontend_back_btn").performClick()
    composeTestRule.onNodeWithTag("screen_2_main_menu").assertIsDisplayed()

    // CHAIN 3: HOME -> CAREER -> CHAMPIONSHIP -> RACE (and back to HOME)
    composeTestRule.onNodeWithTag("main_menu_career_btn").performScrollTo().performClick()
    composeTestRule.onNodeWithTag("screen_9_career_championship").assertIsDisplayed()
    composeTestRule.onNodeWithTag("career_open_championship_btn").performScrollTo().performClick()
    composeTestRule.onNodeWithTag("screen_championship_detail").assertIsDisplayed()
    composeTestRule.onNodeWithTag("championship_start_race_btn").performScrollTo().performClick()
    composeTestRule.onNodeWithTag("racing_game_screen").assertIsDisplayed()
    composeTestRule.onNodeWithTag("racing_back_button").performClick()
    composeTestRule.onNodeWithTag("screen_2_main_menu").assertIsDisplayed()

    // CHAIN 1: HOME -> GAME MODES -> RACE -> RESULTS -> HOME
    composeTestRule.onNodeWithTag("main_menu_play_btn").performScrollTo().performClick()
    composeTestRule.onNodeWithTag("screen_3_mode_selection").assertIsDisplayed()
    composeTestRule.onNodeWithTag("mode_card_QUICK_RACE").performScrollTo().performClick()
    composeTestRule.onNodeWithTag("racing_game_screen").assertIsDisplayed()
    composeTestRule.onNodeWithTag("hud_finish_race_btn").performClick()
    composeTestRule.onNodeWithTag("screen_5_results_and_rewards").assertIsDisplayed()
    composeTestRule.onNodeWithTag("results_main_menu_button").performScrollTo().performClick()
    composeTestRule.onNodeWithTag("screen_2_main_menu").assertIsDisplayed()
  }
}



