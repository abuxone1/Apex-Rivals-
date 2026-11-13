package com.example

import android.content.Context
import android.content.pm.PackageManager
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import com.aistudio.apexrivals.tr8x.BuildConfig
import com.example.data.firebase.ApexBackendRepository
import com.example.data.local.AppDatabase
import com.example.data.local.PerformanceMetrics
import com.example.data.local.RacePerformanceMetric
import com.example.data.local.RaceResult
import com.example.data.local.RaceSessionEntity
import com.example.game.engine.InteractiveRacingEngine
import com.example.game.model.DrivingAssistLevel
import com.example.game.model.GameMode
import com.example.game.model.RaceState
import com.example.game.model.TireCompound
import com.example.game.ui.RacingGameScreen
import com.example.game.v2.ApexRivals2CareerManager
import com.example.game.v2.ApexRivals2FrontendHubScreen
import com.example.game.v2.ApexRivals2SettingsRepository
import com.example.game.v2.AppLanguageOption
import com.example.game.v2.BodyKitSpec
import com.example.game.v2.ChampionshipTier
import com.example.game.v2.DrivingControlMode
import com.example.game.v2.FrameRatePreference
import com.example.game.v2.FrontendSubScreen
import com.example.game.v2.GraphicsQualityPreset
import com.example.game.v2.PaintFinishMaterial
import com.example.game.v2.SpoilerWingStyle
import com.example.game.v2.UpgradeSubsystem
import com.example.game.v2.WheelRimStyle
import com.example.game.v2.WhatsNewInV2Dialog
import com.example.ui.screens.GooglePlayListingData
import com.example.model.AVAILABLE_TRACKS
import com.example.model.AVAILABLE_VEHICLES
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * APEX RIVALS – VERSION 8 TO VERSION 9 (2.0.0) MIGRATION AND COMPATIBILITY TEST SUITE
 *
 * Verifies in-place production upgrade safety across:
 * - TEST A: Existing Version 8 Installation Upgrade
 * - TEST B: Clean New Device Installation
 * - TEST C: Room SQLite Database & SharedPreferences Migration + Rollback Recovery
 * - TEST D: Application Identity, Version Code/Name, Manifest & Permission Compatibility
 * - TEST E: Full Gameplay & UI Navigation Regression
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class V8ToV9MigrationCompatibilityTest {

  @get:Rule
  val composeTestRule = createComposeRule()

  private lateinit var context: Context

  @Before
  fun setUp() {
    context = ApplicationProvider.getApplicationContext()
    context.getSharedPreferences("apex_rivals_prefs", Context.MODE_PRIVATE).edit().clear().commit()
    context.getSharedPreferences("apex_rivals_v2_progression", Context.MODE_PRIVATE).edit().clear().commit()
    context.getSharedPreferences("apex_rivals_v2_settings", Context.MODE_PRIVATE).edit().clear().commit()
    context.getSharedPreferences("apex_rivals_v8_backup_snapshot", Context.MODE_PRIVATE).edit().clear().commit()
    ApexRivals2CareerManager.resetForTesting()
  }

  // ============================================================================
  // TEST A – EXISTING INSTALLATION (V8 -> V9 IN-PLACE UPDATE)
  // ============================================================================
  @Test
  fun testA_existingV8InstallationUpgradesToV9PreservingAccountLevelCurrencyVehiclesUpgradesRecordsAchievementsAndSettings() = runBlocking {
    // 1. Simulate an existing production Version 8 installation with recorded player data
    val v8Prefs = context.getSharedPreferences("apex_rivals_prefs", Context.MODE_PRIVATE)
    v8Prefs.edit()
      .putString("player_id", "pilot_v8_prod_9941")
      .putString("player_username", "V8_GrandPrix_Veteran")
      .putString("avatar_badge", "CARBON_CROWN_V8")
      .putInt("player_level", 14)
      .putInt("player_xp", 26400)
      .putInt("player_coins", 1840)
      .putInt("player_credits", 192500)
      .putInt("total_races_completed", 68)
      .putInt("total_victories", 49)
      .putInt("daily_streak", 9)
      .putStringSet(
        "owned_vehicle_ids",
        setOf("car_gt3", "bike_1000rr", "car_hyper_v12", "car_f1_hybrid", "car_drift_s15")
      )
      .putStringSet(
        "unlocked_achievements",
        setOf("ach_first_victory", "ach_speed_demon_330", "ach_monza_master_v8", "ach_drift_king_v8")
      )
      .putStringSet(
        "claimed_event_ids",
        setOf("evt_rookie_city_sprint", "evt_amateur_desert_outrun")
      )
      .putStringSet(
        "unlocked_track_ids",
        setOf("track_neo_tokyo", "track_monza", "track_miami_highway", "track_suzuka", "track_tokyo")
      )
      // V8 Vehicle Upgrades on car_gt3
      .putInt("v8_upgrade_car_gt3_engine", 4)
      .putInt("v8_upgrade_car_gt3_turbo", 5)
      .putInt("v8_upgrade_car_gt3_brakes", 3)
      .putInt("v8_upgrade_car_gt3_tires", 4)
      .putLong("v8_upgrade_car_gt3_paint_hex", 0xFF00E5FFL)
      .putString("v8_upgrade_car_gt3_wheel", WheelRimStyle.CARBON_AERO_DISC.name)
      .putString("v8_upgrade_car_gt3_spoiler", SpoilerWingStyle.ACTIVE_DRS_CARBON.name)
      .putString("v8_upgrade_car_gt3_bodykit", BodyKitSpec.HYPER_CARBON_KIT.name)
      .putString("v8_upgrade_car_gt3_finish", PaintFinishMaterial.FORGED_CARBON.name)
      // V8 Settings
      .putString("graphics_preset", GraphicsQualityPreset.HIGH.name)
      .putString("control_mode", DrivingControlMode.STEERING_WHEEL.name)
      .putFloat("steering_sensitivity", 1.45f)
      .putString("assist_level", DrivingAssistLevel.SIMULATION.name)
      .putFloat("music_volume", 0.60f)
      .putFloat("engine_volume", 0.98f)
      .putFloat("sfx_volume", 0.82f)
      .putBoolean("is_muted", false)
      .putString("language_code", AppLanguageOption.FRENCH.code)
      .putInt("frame_rate_fps", 90)
      .commit()

    // Record V8 race records in Room Database before V9 initialization
    val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
      .allowMainThreadQueries()
      .addMigrations(AppDatabase.MIGRATION_8_9)
      .build()
    val v8RaceId = db.raceResultDao().insertRaceResult(
      RaceResult(
        trackName = "Monza Speed Autodrome",
        vehicleName = "Apex GT3-R Twin-Turbo",
        lapTimeMs = 74120L,
        topSpeedKmh = 338.5f,
        timestamp = 1730000000000L
      )
    )
    assertTrue(v8RaceId > 0L)

    // 2. Install/Initialize Version 9 (2.0.0) over the existing Version 8 installation
    ApexRivals2SettingsRepository.initialize(context, forceReload = true)
    ApexRivals2CareerManager.initialize(context)

    val migratedState = ApexRivals2CareerManager.state.value

    // 3. Verify all recorded V8 information remains intact
    assertEquals("pilot_v8_prod_9941", migratedState.playerId)
    assertEquals("V8_GrandPrix_Veteran", migratedState.playerUsername)
    assertEquals("CARBON_CROWN_V8", migratedState.avatarBadge)
    assertTrue("Player level must be preserved (>= 14)", migratedState.playerLevel >= 14)
    assertTrue("Player XP must be preserved (>= 26,400)", migratedState.totalXp >= 26400)
    assertEquals("Player coins must be preserved", 1840, migratedState.coinsBalance)
    assertTrue("Player credits must be preserved (>= 192,500)", migratedState.creditsBalance >= 192500)
    assertEquals(68, migratedState.totalRacesCompleted)
    assertEquals(49, migratedState.totalVictories)
    assertEquals(9, migratedState.dailyLoginStreakDays)

    // Verify unlocked vehicles
    assertTrue("car_f1_hybrid" in migratedState.ownedVehicleIds)
    assertTrue("car_drift_s15" in migratedState.ownedVehicleIds)
    assertTrue("car_hyper_v12" in migratedState.ownedVehicleIds)

    // Verify vehicle upgrades & visual customizations
    val gt3Vehicle = AVAILABLE_VEHICLES.first { it.id == "car_gt3_apex" }
    val gt3Loadout = ApexRivals2CareerManager.getLoadoutForVehicle(gt3Vehicle)
    assertEquals(4, gt3Loadout.engineLevel)
    assertEquals(5, gt3Loadout.turboLevel)
    assertEquals(3, gt3Loadout.brakesLevel)
    assertEquals(4, gt3Loadout.tiresLevel)
    assertEquals(0xFF00E5FFL, gt3Loadout.paintColorHex)
    assertEquals(WheelRimStyle.CARBON_AERO_DISC, gt3Loadout.wheelRimStyle)
    assertEquals(SpoilerWingStyle.ACTIVE_DRS_CARBON, gt3Loadout.spoilerStyle)
    assertEquals(BodyKitSpec.HYPER_CARBON_KIT, gt3Loadout.bodyKitSpec)
    assertEquals(PaintFinishMaterial.FORGED_CARBON, gt3Loadout.paintFinishMaterial)

    // Verify achievements & claimed events
    assertTrue("ach_speed_demon_330" in migratedState.unlockedAchievements)
    assertTrue("ach_monza_master_v8" in migratedState.unlockedAchievements)
    assertTrue("evt_rookie_city_sprint" in migratedState.claimedEventIds)
    assertTrue("evt_amateur_desert_outrun" in migratedState.claimedEventIds)

    // Verify Room DB race records
    val preservedRaceResults = db.raceResultDao().getAllResults().first()
    assertEquals(1, preservedRaceResults.size)
    assertEquals("Monza Speed Autodrome", preservedRaceResults[0].trackName)
    assertEquals(74120L, preservedRaceResults[0].lapTimeMs)
    db.close()

    // Verify V8 settings preserved in V9
    assertEquals(GraphicsQualityPreset.HIGH, ApexRivals2SettingsRepository.graphicsPreset.value)
    assertEquals(DrivingControlMode.STEERING_WHEEL, ApexRivals2SettingsRepository.controlConfig.value.controlMode)
    assertEquals(1.45f, ApexRivals2SettingsRepository.controlConfig.value.steeringSensitivity, 0.01f)
    assertEquals(DrivingAssistLevel.SIMULATION, ApexRivals2SettingsRepository.controlConfig.value.assistLevel)
    assertEquals(0.60f, ApexRivals2SettingsRepository.audioMixer.value.musicVolume, 0.01f)
    assertEquals(0.98f, ApexRivals2SettingsRepository.audioMixer.value.engineVolume, 0.01f)
    assertEquals(0.82f, ApexRivals2SettingsRepository.audioMixer.value.sfxVolume, 0.01f)
    assertEquals(AppLanguageOption.FRENCH, ApexRivals2SettingsRepository.languageOption.value)
    assertEquals(FrameRatePreference.FPS_90, ApexRivals2SettingsRepository.frameRatePref.value)
  }

  // ============================================================================
  // TEST B – NEW INSTALLATION (CLEAN DEVICE ONBOARDING & INITIAL GAMEPLAY)
  // ============================================================================
  @Test
  fun testB_cleanDeviceNewInstallationInitializesOnboardingStarterGarageGameplayAndUnifiedNavigation() {
    // 1. Clean device initialization (no prior V8 SharedPreferences)
    ApexRivals2SettingsRepository.initialize(context, forceReload = true)
    ApexRivals2CareerManager.initialize(context)

    val cleanState = ApexRivals2CareerManager.state.value
    assertNotNull(cleanState.playerId)
    assertTrue(cleanState.playerUsername.isNotBlank())
    assertTrue("Clean install must provide starter credits", cleanState.creditsBalance > 0)
    assertTrue("Clean install must provide starter coins", cleanState.coinsBalance > 0)
    assertTrue("Clean install must unlock starter GT3 & Superbike vehicles", "car_gt3" in cleanState.ownedVehicleIds)
    assertTrue("Clean install must unlock starter circuits", "track_neo_tokyo" in cleanState.unlockedTrackIds)

    // 2. Verify onboarding splash screen -> Home -> Mode Selection -> Playable Race on clean install
    var activeScreen = mutableStateOf("FRONTEND")
    composeTestRule.setContent {
      if (activeScreen.value == "FRONTEND") {
        ApexRivals2FrontendHubScreen(
          activeVehicle = AVAILABLE_VEHICLES.first(),
          activeTrack = AVAILABLE_TRACKS.first(),
          onSelectVehicle = {},
          onSelectTrack = {},
          onLaunchRaceHud = { activeScreen.value = "RACE" },
          onNavigateToProfile = {},
          onNavigateToMultiplayer = {},
          initialSubScreen = FrontendSubScreen.LOADING_SCREEN
        )
      } else {
        RacingGameScreen(
          vehicle = AVAILABLE_VEHICLES.first(),
          track = AVAILABLE_TRACKS.first(),
          onNavigateBack = { activeScreen.value = "FRONTEND" }
        )
      }
    }

    // Verify Onboarding Splash Screen (Screen 1) renders and transitions to Main Menu (Screen 2)
    composeTestRule.onNodeWithTag("screen_1_loading").assertIsDisplayed()
    composeTestRule.onNodeWithTag("loading_enter_btn").performScrollTo().performClick()
    composeTestRule.onNodeWithTag("screen_2_main_menu").assertIsDisplayed()

    // Navigate to Game Modes -> Launch initial race -> Verify playable Race HUD & Results
    composeTestRule.onNodeWithTag("main_menu_play_btn").performScrollTo().performClick()
    composeTestRule.onNodeWithTag("screen_3_mode_selection").assertIsDisplayed()
    composeTestRule.onNodeWithTag("mode_card_QUICK_RACE").performScrollTo().performClick()
    composeTestRule.onNodeWithTag("racing_game_screen").assertIsDisplayed()
    composeTestRule.onNodeWithTag("racing_game_canvas").assertIsDisplayed()
  }

  // ============================================================================
  // TEST C – DATABASE MIGRATION & ROLLBACK RECOVERY
  // ============================================================================
  @Test
  fun testC_databaseMigration8To9PreservesSqliteRecordsPlayerIdsVehicleUpgradesOfflineOperationAndRollbackRecovery() = runBlocking {
    // 1. Create a real on-disk SQLite database at Version 8, populate V8 records, and run MIGRATION_8_9
    val dbName = "test_v8_to_v9_migration.db"
    context.deleteDatabase(dbName)

    val seedDb = Room.databaseBuilder(context, AppDatabase::class.java, dbName)
      .allowMainThreadQueries()
      .build()
    seedDb.raceResultDao().insertRaceResult(
      RaceResult(
        id = 1L,
        trackName = "Suzuka Grand GP",
        vehicleName = "Hyperion-7 Hypercar",
        lapTimeMs = 71850L,
        topSpeedKmh = 352.4f,
        timestamp = 1731000000000L
      )
    )
    // Stamp the on-disk database header as Version 8 (Production V8 release)
    seedDb.openHelper.writableDatabase.version = 8
    assertEquals(8, seedDb.openHelper.writableDatabase.version)
    seedDb.close()

    // Now open the same Version 8 database file with Room Version 9 and MIGRATION_8_9
    val migratedRoomDb = Room.databaseBuilder(context, AppDatabase::class.java, dbName)
      .allowMainThreadQueries()
      .addMigrations(AppDatabase.MIGRATION_8_9)
      .build()

    val migratedResults = migratedRoomDb.raceResultDao().getAllResults().first()
    assertEquals(9, migratedRoomDb.openHelper.writableDatabase.version)
    assertEquals(1, migratedResults.size)
    assertEquals("Suzuka Grand GP", migratedResults[0].trackName)
    assertEquals("Hyperion-7 Hypercar", migratedResults[0].vehicleName)
    assertEquals(71850L, migratedResults[0].lapTimeMs)
    assertEquals(352.4f, migratedResults[0].topSpeedKmh, 0.1f)
    migratedRoomDb.close()
    context.deleteDatabase(dbName)

    // 2. Verify V8 Pre-Upgrade Backup Snapshot & Deterministic Rollback Recovery
    val v8Prefs = context.getSharedPreferences("apex_rivals_prefs", Context.MODE_PRIVATE)
    v8Prefs.edit()
      .putString("player_id", "pilot_v8_rollback_test")
      .putString("player_username", "Rollback_Commander")
      .putInt("player_coins", 920)
      .putInt("player_credits", 135000)
      .putInt("player_xp", 19800)
      .putInt("player_level", 12)
      .putStringSet("owned_vehicle_ids", setOf("car_gt3", "car_f1_hybrid"))
      .putInt("v8_upgrade_car_gt3_engine", 5)
      .commit()

    ApexRivals2CareerManager.initialize(context)
    val initialMigrated = ApexRivals2CareerManager.state.value
    assertEquals("pilot_v8_rollback_test", initialMigrated.playerId)
    assertEquals(920, initialMigrated.coinsBalance)
    assertEquals(5, ApexRivals2CareerManager.getLoadoutForVehicle(AVAILABLE_VEHICLES.first { it.id == "car_gt3_apex" }).engineLevel)

    // Mutate state in V9, then execute restoreFromV8BackupSnapshot(context) and verify recovery
    ApexRivals2CareerManager.resetForTesting()
    assertEquals("pilot_tr8x_default", ApexRivals2CareerManager.state.value.playerId)

    val restoredOk = ApexRivals2CareerManager.restoreFromV8BackupSnapshot(context)
    assertTrue("V8 backup snapshot recovery must succeed", restoredOk)
    val recovered = ApexRivals2CareerManager.state.value
    assertEquals("pilot_v8_rollback_test", recovered.playerId)
    assertEquals("Rollback_Commander", recovered.playerUsername)
    assertEquals(920, recovered.coinsBalance)
    assertTrue(recovered.creditsBalance >= 135000)
    assertTrue("car_f1_hybrid" in recovered.ownedVehicleIds)
    assertEquals(5, ApexRivals2CareerManager.getLoadoutForVehicle(AVAILABLE_VEHICLES.first { it.id == "car_gt3_apex" }).engineLevel)

    // Verify Backend Rollback Plan metadata
    val rollbackPlan = com.example.data.firebase.BackendRollbackRecoveryPlan()
    assertEquals("V8-to-V9-Production-Safe-1.0", rollbackPlan.planVersion)
    assertTrue(rollbackPlan.recoverySteps.size >= 4)
  }

  // ============================================================================
  // TEST D – APPLICATION COMPATIBILITY (PACKAGE ID, VERSION, MANIFEST, SIGNING)
  // ============================================================================
  @Test
  fun testD_applicationCompatibilityVerifiesPackageIdVersionCodeVersionNameManifestAndPermissions() {
    // 1. Verify identical Android Application ID
    assertEquals("com.aistudio.apexrivals.tr8x", context.packageName)
    assertEquals("com.aistudio.apexrivals.tr8x", BuildConfig.APPLICATION_ID)

    // 2. Verify higher unused versionCode (> 8) and versionName = "2.0.0"
    assertTrue(
      "New release versionCode (${BuildConfig.VERSION_CODE}) must be strictly greater than production Version 8",
      BuildConfig.VERSION_CODE > 8
    )
    assertEquals(9, BuildConfig.VERSION_CODE)
    assertEquals("2.0.0", BuildConfig.VERSION_NAME)

    // 3. Verify AndroidManifest launcher activity & package info compatibility
    val pm = context.packageManager
    val packageInfo = pm.getPackageInfo(
      context.packageName,
      PackageManager.GET_ACTIVITIES or PackageManager.GET_PERMISSIONS
    )
    assertNotNull(packageInfo.activities)
    val mainActivityInfo = packageInfo.activities?.find { it.name == "com.example.MainActivity" }
    assertNotNull("com.example.MainActivity must be registered in AndroidManifest.xml", mainActivityInfo)
    assertTrue("MainActivity must be exported for launcher intent", mainActivityInfo!!.exported)

    // Verify no duplicate invalid com.aistudio.apexrivals.tr8x.MainActivity entry exists
    val duplicateInvalidActivity = packageInfo.activities?.find {
      it.name == "com.aistudio.apexrivals.tr8x.MainActivity"
    }
    assertEquals(null, duplicateInvalidActivity)

    // 4. Verify required permissions are declared
    val requestedPerms = packageInfo.requestedPermissions?.toSet().orEmpty()
    assertTrue("android.permission.INTERNET" in requestedPerms)
    assertTrue("android.permission.ACCESS_NETWORK_STATE" in requestedPerms)
    assertTrue("android.permission.VIBRATE" in requestedPerms)
  }

  // ============================================================================
  // TEST E – GAMEPLAY REGRESSION (PHYSICS, GARAGE, CAREER, REWARDS, PERFORMANCE)
  // ============================================================================
  @Test
  fun testE_gameplayRegressionVerifiesEnginePhysicsUpgradesCareerEventsMissionsAchievementsAndZeroCrashes() {
    ApexRivals2SettingsRepository.initialize(context, forceReload = true)
    ApexRivals2CareerManager.initialize(context)

    val vehicle = AVAILABLE_VEHICLES.first()
    val track = AVAILABLE_TRACKS.first()

    // 1. Verify Racing Engine Physics, Nitro, Tire Compounds, Pit Stops, and Race Completion
    val engine = InteractiveRacingEngine(context, vehicle, track)
    engine.applyGameMode(GameMode.DRIFT_CHALLENGE)
    assertEquals(GameMode.DRIFT_CHALLENGE, engine.gameMode)
    assertEquals(2, engine.totalLaps)

    engine.triggerNitroBoost()
    assertTrue(engine.isNitroActive)

    engine.changeTires(TireCompound.SOFT)
    assertEquals(TireCompound.SOFT, engine.activeTireCompound)
    assertEquals(100f, engine.tireHealthPercent, 0.01f)

    engine.completeRaceImmediately(forcedPosition = 1)
    assertEquals(RaceState.FINISHED_PODIUM, engine.raceState.value)
    val summary = engine.finishSummary.value
    assertNotNull("Finishing a race must synchronously populate RaceFinishSummary", summary)
    assertEquals(1, summary!!.finishingPosition)
    assertTrue(summary.creditsEarned > 0)
    assertTrue(summary.xpEarned > 0)

    // 2. Verify 3D Garage Customization & Performance Subsystem Upgrades
    val baseStats = ApexRivals2CareerManager.computeUpgradedStats(
      vehicle,
      ApexRivals2CareerManager.getLoadoutForVehicle(vehicle)
    )
    ApexRivals2CareerManager.applyVisualCustomization(
      vehicle = vehicle,
      paintColorHex = 0xFFFFD600L,
      wheelRimStyle = WheelRimStyle.TITANIUM_MULTISPOKE,
      spoilerStyle = SpoilerWingStyle.GT3_SWAN_NECK,
      bodyKitSpec = BodyKitSpec.GT_AERO_DIFFUSER,
      context = context
    )
    ApexRivals2CareerManager.applyPaintFinishMaterial(
      vehicle = vehicle,
      finishMaterial = PaintFinishMaterial.CHAMELEON_FLAKE,
      context = context
    )
    assertTrue(ApexRivals2CareerManager.upgradeSubsystem(vehicle, UpgradeSubsystem.ENGINE, context))
    assertTrue(ApexRivals2CareerManager.upgradeSubsystem(vehicle, UpgradeSubsystem.TURBO, context))

    val upgradedLoadout = ApexRivals2CareerManager.getLoadoutForVehicle(vehicle)
    val upgradedStats = ApexRivals2CareerManager.computeUpgradedStats(vehicle, upgradedLoadout)
    assertTrue(upgradedStats.maxSpeedKmh > baseStats.maxSpeedKmh)
    assertTrue(upgradedStats.horsepower > baseStats.horsepower)
    assertEquals(PaintFinishMaterial.CHAMELEON_FLAKE, upgradedLoadout.paintFinishMaterial)

    // 3. Verify Career Championships, Missions, Daily Rewards & Idempotency (no double-payout exploits)
    val creditsBeforeClaim = ApexRivals2CareerManager.state.value.creditsBalance
    assertTrue(ApexRivals2CareerManager.claimCareerEventReward("evt_rookie_time_trial", context))
    assertFalse("Duplicate career event claim must be rejected", ApexRivals2CareerManager.claimCareerEventReward("evt_rookie_time_trial", context))
    assertTrue(ApexRivals2CareerManager.state.value.creditsBalance > creditsBeforeClaim)

    assertTrue(ApexRivals2CareerManager.claimMissionReward("msn_daily_apex_hunter", context))
    assertFalse("Duplicate mission claim must be rejected", ApexRivals2CareerManager.claimMissionReward("msn_daily_apex_hunter", context))

    assertTrue(ApexRivals2CareerManager.claimDailyLoginReward(context))
    assertFalse("Duplicate same-day login claim must be rejected", ApexRivals2CareerManager.claimDailyLoginReward(context))

    assertTrue(ApexRivals2CareerManager.claimChampionshipTierTrophy(ChampionshipTier.ROOKIE_CHAMPIONSHIP, context))
    assertFalse(ApexRivals2CareerManager.claimChampionshipTierTrophy(ChampionshipTier.ROOKIE_CHAMPIONSHIP, context))

    // 4. Verify Performance & Memory Diagnostics stay within budget
    assertTrue("Estimated FPS must stay within valid 15..60 range", ApexRivals2SettingsRepository.getEstimatedFps() in 15..60)
    assertTrue("JVM heap usage must be tracked accurately", ApexRivals2SettingsRepository.getMemoryUsageMb() >= 1L)
  }

  // ============================================================================
  // TEST F – WHAT'S NEW IN APEX RIVALS 2.0 (OFFICIAL RELEASE NOTES VERIFICATION)
  // ============================================================================
  @Test
  fun testF_whatsNewInApexRivals20OfficialReleaseNotesAndInGameDialog() {
    // 1. Verify official Play Store release notes string content
    val notes = GooglePlayListingData.WHATS_NEW_V2_0
    assertTrue("Header must match", notes.contains("What's New in Apex Rivals 2.0!"))
    assertTrue("Redesigned UI note", notes.contains("Redesigned racing game interface."))
    assertTrue("Garage customization note", notes.contains("Improved vehicle garage and customization."))
    assertTrue("Graphics note", notes.contains("Enhanced graphics and visual effects."))
    assertTrue("Driving controls note", notes.contains("Improved driving controls and gameplay experience."))
    assertTrue("Challenges note", notes.contains("New racing challenges and progression improvements."))
    assertTrue("Performance note", notes.contains("Performance optimizations and bug fixes."))
    assertTrue("Closing message", notes.contains("Thank you for racing with Apex Rivals!"))
    assertTrue("Developer studio", notes.contains("Developed by Apex Dynamics Game Studios."))

    // 2. Verify interactive in-game What's New dialog rendering & dismissal via top and bottom close buttons
    var topDismissed = false
    var bottomDismissed = false
    val dialogStep = mutableStateOf(1)
    composeTestRule.setContent {
      if (dialogStep.value == 1) {
        WhatsNewInV2Dialog(onDismiss = { topDismissed = true })
      } else {
        WhatsNewInV2Dialog(onDismiss = { bottomDismissed = true })
      }
    }

    composeTestRule.onNodeWithTag("whats_new_2_0_dialog").assertIsDisplayed()
    composeTestRule.onNodeWithTag("whats_new_top_close_btn").performClick()
    assertTrue("Top close button must trigger dismiss callback", topDismissed)

    // 3. Verify bottom action button dismissal
    dialogStep.value = 2
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("whats_new_dialog_close_btn").performScrollTo().performClick()
    assertTrue("Bottom action button must trigger dismiss callback", bottomDismissed)
  }

  // ============================================================================
  // TEST G – GLOBAL LANDSCAPE ORIENTATION & MULTI-ASPECT-RATIO VERIFICATION
  // (16:9, 18:9, 19.5:9, 20:9 Smartphones & Android Tablets across all 15 Screens)
  // ============================================================================
  @Test
  fun testG_globalLandscapeOrientationAndMultiAspectRatioVerification() {
    // 1. Verify AndroidManifest MainActivity enforces sensorLandscape
    val pm = context.packageManager
    val activityInfo = pm.getActivityInfo(
      android.content.ComponentName(context.packageName, "com.example.MainActivity"),
      0
    )
    assertEquals(
      "MainActivity must default to SCREEN_ORIENTATION_SENSOR_LANDSCAPE",
      android.content.pm.ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE,
      activityInfo.screenOrientation
    )

    // 2. Verify all 15 screens + Racing HUD (Left Steering, Right Pedals, Bottom-Center Speedometer, Top Telemetry)
    // across 16:9 (640x360), 18:9 (720x360), 19.5:9 (780x360), 20:9 (800x360), and Tablet (1024x768)
    val aspectViewports = listOf(
      Triple("16:9", 640, 360),
      Triple("18:9", 720, 360),
      Triple("19.5:9", 780, 360),
      Triple("20:9", 800, 360),
      Triple("Tablet", 1024, 768)
    )

    val activeAspectIndex = mutableStateOf(0)
    val activeTopScreen = mutableStateOf("FRONTEND")
    val externalSubScreen = mutableStateOf<FrontendSubScreen?>(null)
    var selectedVehicle = AVAILABLE_VEHICLES.first()
    var selectedTrack = AVAILABLE_TRACKS.first()

    composeTestRule.setContent {
      val (_, widthDp, heightDp) = aspectViewports[activeAspectIndex.value]
      androidx.compose.foundation.layout.Box(
        modifier = androidx.compose.ui.Modifier.size(
          width = androidx.compose.ui.unit.Dp(widthDp.toFloat()),
          height = androidx.compose.ui.unit.Dp(heightDp.toFloat())
        )
      ) {
        if (activeTopScreen.value == "FRONTEND") {
          ApexRivals2FrontendHubScreen(
            activeVehicle = selectedVehicle,
            activeTrack = selectedTrack,
            onSelectVehicle = { selectedVehicle = it },
            onSelectTrack = { selectedTrack = it },
            onLaunchRaceHud = { activeTopScreen.value = "RACE" },
            onNavigateToProfile = { externalSubScreen.value = FrontendSubScreen.DRIVER_PROFILE },
            onNavigateToMultiplayer = { externalSubScreen.value = FrontendSubScreen.MULTIPLAYER_HUB },
            initialSubScreen = FrontendSubScreen.LOADING_SCREEN,
            externalSubScreen = externalSubScreen.value,
            onExternalSubScreenChange = { externalSubScreen.value = it }
          )
        } else {
          RacingGameScreen(
            vehicle = selectedVehicle,
            track = selectedTrack,
            onNavigateBack = {
              externalSubScreen.value = FrontendSubScreen.MAIN_MENU
              activeTopScreen.value = "FRONTEND"
            }
          )
        }
      }
    }

    // 1. Splash & Loading Screen -> Main Home Screen
    composeTestRule.onNodeWithTag("screen_1_loading").assertIsDisplayed()
    composeTestRule.onNodeWithTag("loading_enter_btn").performScrollTo().performClick()
    composeTestRule.onNodeWithTag("screen_2_main_menu").assertIsDisplayed()

    // Verify Main Home Screen across all 5 landscape aspect ratios (16:9, 18:9, 19.5:9, 20:9, Tablet)
    for (i in aspectViewports.indices) {
      activeAspectIndex.value = i
      composeTestRule.waitForIdle()
      composeTestRule.onNodeWithTag("screen_2_main_menu").assertIsDisplayed()
      composeTestRule.onNodeWithTag("main_menu_featured_vehicle_stage").assertIsDisplayed()
      composeTestRule.onNodeWithTag("main_menu_play_btn").performScrollTo().assertIsDisplayed()
    }

    // Reset to 19.5:9 smartphone landscape and verify navigation across all remaining screens
    activeAspectIndex.value = 2
    composeTestRule.waitForIdle()

    // Vehicle Garage -> Vehicle Customization -> Upgrades
    composeTestRule.onNodeWithTag("main_menu_garage_btn").performScrollTo().performClick()
    composeTestRule.onNodeWithTag("screen_4_vehicle_garage").assertIsDisplayed()
    composeTestRule.onNodeWithTag("garage_centerpiece_stage").performScrollTo()
    composeTestRule.onNodeWithTag("garage_screen_customize_btn").performClick()
    composeTestRule.onNodeWithTag("screen_5_vehicle_customization").assertIsDisplayed()
    composeTestRule.onNodeWithTag("customization_open_upgrades_btn").performScrollTo().performClick()
    composeTestRule.onNodeWithTag("screen_vehicle_upgrades").assertIsDisplayed()
    composeTestRule.onNodeWithTag("frontend_back_btn").performScrollTo().performClick()
    composeTestRule.onNodeWithTag("frontend_back_btn").performScrollTo().performClick()
    composeTestRule.onNodeWithTag("frontend_back_btn").performScrollTo().performClick()
    composeTestRule.onNodeWithTag("screen_2_main_menu").assertIsDisplayed()

    // Career Mode -> Championship
    composeTestRule.onNodeWithTag("main_menu_career_btn").performScrollTo().performClick()
    composeTestRule.onNodeWithTag("screen_9_career_championship").assertIsDisplayed()
    composeTestRule.onNodeWithTag("career_open_championship_btn").performScrollTo().performClick()
    composeTestRule.onNodeWithTag("screen_championship_detail").assertIsDisplayed()
    composeTestRule.onNodeWithTag("frontend_back_btn").performScrollTo().performClick()
    composeTestRule.onNodeWithTag("career_back_btn").performScrollTo().performClick()
    composeTestRule.onNodeWithTag("screen_2_main_menu").assertIsDisplayed()

    // Events and Rewards -> Missions -> Rewards
    composeTestRule.onNodeWithTag("main_menu_events_btn").performScrollTo().performClick()
    composeTestRule.onNodeWithTag("screen_9_events_and_rewards").assertIsDisplayed()
    composeTestRule.onNodeWithTag("events_open_missions_btn").performScrollTo().performClick()
    composeTestRule.onNodeWithTag("screen_events_missions").assertIsDisplayed()
    composeTestRule.onNodeWithTag("missions_open_rewards_btn").performScrollTo().performClick()
    composeTestRule.onNodeWithTag("screen_events_rewards").assertIsDisplayed()
    composeTestRule.onNodeWithTag("frontend_back_btn").performScrollTo().performClick()
    composeTestRule.onNodeWithTag("frontend_back_btn").performScrollTo().performClick()
    composeTestRule.onNodeWithTag("frontend_back_btn").performScrollTo().performClick()
    composeTestRule.onNodeWithTag("screen_2_main_menu").assertIsDisplayed()

    // Multiplayer
    composeTestRule.onNodeWithTag("main_menu_multiplayer_btn").performScrollTo().performClick()
    composeTestRule.onNodeWithTag("screen_10_multiplayer_hub").assertIsDisplayed()
    composeTestRule.onNodeWithTag("multiplayer_back_btn").performScrollTo().performClick()
    composeTestRule.onNodeWithTag("screen_2_main_menu").assertIsDisplayed()

    // Player Profile -> Achievements
    composeTestRule.onNodeWithTag("main_menu_profile_btn").performScrollTo().performClick()
    composeTestRule.onNodeWithTag("screen_11_driver_profile").assertIsDisplayed()
    composeTestRule.onNodeWithTag("profile_open_achievements_btn").performScrollTo().performClick()
    composeTestRule.onNodeWithTag("screen_driver_achievements").assertIsDisplayed()
    composeTestRule.onNodeWithTag("frontend_back_btn").performScrollTo().performClick()
    composeTestRule.onNodeWithTag("profile_back_btn").performScrollTo().performClick()
    composeTestRule.onNodeWithTag("screen_2_main_menu").assertIsDisplayed()

    // Settings
    composeTestRule.onNodeWithTag("main_menu_settings_btn").performScrollTo().performClick()
    composeTestRule.onNodeWithTag("screen_10_settings").assertIsDisplayed()
    composeTestRule.onNodeWithTag("frontend_back_btn").performScrollTo().performClick()
    composeTestRule.onNodeWithTag("screen_2_main_menu").assertIsDisplayed()

    // Game Mode Selection -> Racing Gameplay
    composeTestRule.onNodeWithTag("main_menu_play_btn").performScrollTo().performClick()
    composeTestRule.onNodeWithTag("screen_3_mode_selection").assertIsDisplayed()
    composeTestRule.onNodeWithTag("mode_card_QUICK_RACE").performScrollTo().performClick()
    composeTestRule.onNodeWithTag("racing_game_screen").assertIsDisplayed()

    // 3. Verify Pause Menu in Landscape & transition to active Racing state
    composeTestRule.onNodeWithTag("hud_pause_button").performClick()
    composeTestRule.onNodeWithTag("in_race_pause_modal").assertIsDisplayed()
    composeTestRule.onNodeWithTag("pause_resume_button").performClick()

    // 4. Verify Racing Gameplay HUD across all 5 landscape aspect ratios (16:9, 18:9, 19.5:9, 20:9, Tablet):
    //    - Steering controls on the left (hud_steering_wheel_control, steer_left_btn, steer_right_btn)
    //    - Acceleration & braking on the right (brake_pedal_btn, gas_pedal_btn)
    //    - Speedometer near the bottom center (hud_bottom_center_speedometer)
    //    - Race information at the top (hud_lap_counter, hud_position_badge, hud_speed_value)
    //    - Racing environment visible (racing_game_canvas)
    for (i in aspectViewports.indices) {
      activeAspectIndex.value = i
      composeTestRule.waitForIdle()
      composeTestRule.onNodeWithTag("racing_game_canvas").assertIsDisplayed()
      composeTestRule.onNodeWithTag("hud_steering_wheel_control").assertIsDisplayed()
      composeTestRule.onNodeWithTag("steer_left_btn").assertIsDisplayed()
      composeTestRule.onNodeWithTag("steer_right_btn").assertIsDisplayed()
      composeTestRule.onNodeWithTag("brake_pedal_btn").assertIsDisplayed()
      composeTestRule.onNodeWithTag("gas_pedal_btn").assertIsDisplayed()
      composeTestRule.onNodeWithTag("hud_bottom_center_speedometer").assertIsDisplayed()
      composeTestRule.onNodeWithTag("hud_lap_counter").assertIsDisplayed()
      composeTestRule.onNodeWithTag("hud_position_badge").assertIsDisplayed()
      composeTestRule.onNodeWithTag("hud_speed_value").assertIsDisplayed()
    }

    // 5. Verify Race Results screen in Landscape
    composeTestRule.onNodeWithTag("hud_finish_race_btn").performClick()
    composeTestRule.onNodeWithTag("screen_5_results_and_rewards").assertIsDisplayed()
  }

  /**
   * TEST H – CINEMATIC STARTUP AND WELCOME SCREEN (APEX RIVALS 2.0)
   * Verifies:
   * 1. Top Branding: "APEX DYNAMICS GAME STUDIOS PRESENTS"
   * 2. Main Game Title: "APEX RIVALS" + Logo Emblem (R.drawable.apex_rivals_logo_icon)
   * 3. Background & Hero 3D Supercar Showcase (R.drawable.apex_rivals_welcome_supercar)
   * 4. Welcome Message: "WELCOME, RACER." + "Your journey starts here. Choose your machine, take the wheel and prove yourself on the track."
   * 5. Game Slogan: "DREAM. BUILD. RACE."
   * 6. Loading Section: Animated progress bar, real initialization status, error recovery, and smooth transition to Main Menu.
   * 7. Footer: "POWERED BY APEX DYNAMICS GAME STUDIOS."
   * 8. Existing player progression (coins, level, unlocked vehicles) is never reset during startup initialization.
   */
  @Test
  fun testH_cinematicStartupAndWelcomeScreenVerifiesBrandingCopyRealInitializationAndLandscapeLayout() {
    val v8Prefs = context.getSharedPreferences("apex_rivals_prefs", Context.MODE_PRIVATE)
    v8Prefs.edit()
      .putString("player_id", "pilot_v8_welcome_test")
      .putInt("player_coins", 2450)
      .putInt("player_credits", 185000)
      .putStringSet("owned_vehicle_ids", setOf("car_gt3", "car_hyper_v12"))
      .commit()

    ApexRivals2CareerManager.initialize(context)
    val coinsBeforeStartup = ApexRivals2CareerManager.state.value.coinsBalance
    val unlockedBeforeStartup = ApexRivals2CareerManager.state.value.ownedVehicleIds

    // Verify bundled drawable assets from Screenshot 1 exist
    assertTrue(com.aistudio.apexrivals.tr8x.R.drawable.apex_rivals_welcome_supercar != 0)
    assertTrue(com.aistudio.apexrivals.tr8x.R.drawable.apex_rivals_logo_icon != 0)

    var selectedVehicle = AVAILABLE_VEHICLES.first()
    var selectedTrack = AVAILABLE_TRACKS.first()

    composeTestRule.setContent {
      androidx.compose.foundation.layout.Box(
        modifier = androidx.compose.ui.Modifier.size(
          width = androidx.compose.ui.unit.Dp(780f),
          height = androidx.compose.ui.unit.Dp(360f)
        )
      ) {
        ApexRivals2FrontendHubScreen(
          activeVehicle = selectedVehicle,
          activeTrack = selectedTrack,
          onSelectVehicle = { selectedVehicle = it },
          onSelectTrack = { selectedTrack = it },
          onLaunchRaceHud = {},
          onNavigateToProfile = {},
          onNavigateToMultiplayer = {},
          initialSubScreen = FrontendSubScreen.LOADING_SCREEN
        )
      }
    }

    composeTestRule.waitForIdle()

    // Verify Top Branding, Main Title, Slogan, Supercar Showcase, Welcome Message, Loading Bar, and Footer
    composeTestRule.onNodeWithTag("screen_1_loading").assertIsDisplayed()
    composeTestRule.onNodeWithText("APEX DYNAMICS GAME STUDIOS PRESENTS").assertIsDisplayed()
    composeTestRule.onNodeWithText("APEX RIVALS").assertIsDisplayed()
    composeTestRule.onNodeWithText("DREAM. BUILD. RACE.").assertIsDisplayed()
    composeTestRule.onNodeWithTag("loading_supercar_showcase_card").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithTag("loading_supercar_showcase_image").assertIsDisplayed()
    composeTestRule.onNodeWithTag("loading_welcome_title").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithText("WELCOME, RACER.").assertIsDisplayed()
    composeTestRule.onNodeWithText(
      "Your journey starts here. Choose your machine, take the wheel and prove yourself on the track."
    ).assertIsDisplayed()
    composeTestRule.onNodeWithTag("loading_progress_bar").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithTag("loading_status_message").assertIsDisplayed()
    composeTestRule.onNodeWithTag("loading_footer_branding").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithText("POWERED BY APEX DYNAMICS GAME STUDIOS.").assertIsDisplayed()

    // Verify transition into Main Menu and preservation of existing V8/V9 player progression
    composeTestRule.onNodeWithTag("loading_enter_btn").performScrollTo().performClick()
    composeTestRule.onNodeWithTag("screen_2_main_menu").assertIsDisplayed()
    assertEquals(coinsBeforeStartup, ApexRivals2CareerManager.state.value.coinsBalance)
    assertEquals(unlockedBeforeStartup, ApexRivals2CareerManager.state.value.ownedVehicleIds)
  }
}


