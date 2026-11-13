package com.example.data.firebase

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.base.FirestoreEmulatorTestBase
import com.example.game.v2.ApexRivals2CareerManager
import com.example.game.v2.ApexRivals2SettingsRepository
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID

class ApexBackendRepositoryRuleTest : FirestoreEmulatorTestBase() {

  @Before
  override fun setUpFirebase() {
    super.setUpFirebase()
    syncNamedDatabaseRules()
  }

  private fun syncNamedDatabaseRules() {
    val rulesFile = listOf(File("../firestore.rules"), File("firestore.rules"), File("/app/applet/firestore.rules"))
      .firstOrNull { it.exists() } ?: return
    val rulesContent = rulesFile.readText(Charsets.UTF_8)
    val bodyJson = JSONObject().apply {
      put(
        "rules",
        JSONObject().apply {
          put(
            "files",
            JSONArray().apply {
              put(
                JSONObject().apply {
                  put("name", "firestore.rules")
                  put("content", rulesContent)
                }
              )
            }
          )
        }
      )
    }.toString()

    val endpoint = "http://127.0.0.1:8085/emulator/v1/projects/demo-no-project/databases/$databaseId:securityRules"
    val conn = (URL(endpoint).openConnection() as HttpURLConnection).apply {
      requestMethod = "PUT"
      setRequestProperty("Authorization", "Bearer owner")
      setRequestProperty("Content-Type", "application/json; charset=UTF-8")
      doOutput = true
      connectTimeout = 4000
      readTimeout = 4000
    }
    OutputStreamWriter(conn.outputStream, Charsets.UTF_8).use { writer ->
      writer.write(bodyJson)
    }
    val code = conn.responseCode
    conn.disconnect()
    assertTrue("Expected 2xx uploading firestore.rules to named emulator database, got $code", code in 200..299)
  }

  private fun seedEmulatorDocument(docPath: String, fieldsJson: String) {
    val collectionPath = docPath.substringBeforeLast('/')
    val docId = docPath.substringAfterLast('/')
    val endpoint = "http://127.0.0.1:8085/v1/projects/demo-no-project/databases/$databaseId/documents/$collectionPath?documentId=$docId"
    val conn = (URL(endpoint).openConnection() as HttpURLConnection).apply {
      requestMethod = "POST"
      setRequestProperty("Authorization", "Bearer owner")
      setRequestProperty("Content-Type", "application/json; charset=UTF-8")
      doOutput = true
      connectTimeout = 4000
      readTimeout = 4000
    }
    OutputStreamWriter(conn.outputStream, Charsets.UTF_8).use { writer ->
      writer.write("""{"fields":$fieldsJson}""")
    }
    val code = conn.responseCode
    conn.disconnect()
    assertTrue("Expected 2xx seeding emulator doc $docPath, got $code", code in 200..299)
  }

  @Test
  fun createStarterProfile_andUpdateMetadata_succeedsWhileDirectCurrencyCheatIsDenied() = runBlocking {
    val userEmail = "alice_${UUID.randomUUID().toString().take(8)}@test.com"
    val uid = signInTestUser(userEmail)
    val repo = ApexBackendRepository(firestore, auth)

    val created = withTimeout(DEFAULT_TIMEOUT_MS) {
      repo.createStarterPlayerProfile(
        username = "AliceRacer",
        coins = 250L,
        credits = 48500L,
        ownedVehicleIds = listOf("car_gt3_apex", "bike_superbike_1000")
      ).getOrThrow()
    }
    assertEquals(uid, created.userId)
    assertEquals("AliceRacer", created.username)
    assertEquals(1L, created.level)
    assertEquals(9L, created.schemaVersion)
    assertEquals(48500L, created.credits)

    // Metadata update (username + avatar) succeeds
    val updated = withTimeout(DEFAULT_TIMEOUT_MS) {
      repo.updateProfileMetadata(
        username = "AliceProGT",
        avatarUrl = "avatar_crown_gold"
      ).getOrThrow()
    }
    assertEquals("AliceProGT", updated.username)
    assertEquals("avatar_crown_gold", updated.avatarUrl)
    assertEquals(48500L, updated.credits)

    // Zero-trust rule: client attempting direct currency/XP inflation must fail with PERMISSION_DENIED
    var cheatError: Throwable? = null
    try {
      withTimeout(DEFAULT_TIMEOUT_MS) {
        firestore.collection("users").document(uid)
          .update(mapOf("credits" to 9_999_999L, "coins" to 99_999L))
          .await()
      }
    } catch (e: Throwable) {
      cheatError = e
    }
    assertNotNull("Direct currency mutation by client must be rejected", cheatError)
    assertEquals(
      FirebaseFirestoreException.Code.PERMISSION_DENIED,
      cheatError?.unwrapFirestoreException()?.code
    )
  }

  @Test
  fun syncCloudSave_enforcesMonotonicRevision_andProtectsV8RollbackBackup() = runBlocking {
    val userEmail = "v8_racer_${UUID.randomUUID().toString().take(8)}@test.com"
    val uid = signInTestUser(userEmail)
    val repo = ApexBackendRepository(firestore, auth)
    val slotId = "slot_${UUID.randomUUID().toString().take(8)}"

    val initialSave = withTimeout(DEFAULT_TIMEOUT_MS) {
      repo.syncCloudSave(
        saveSlotId = slotId,
        localSyncRevision = 5L,
        loadoutsJson = """{"car_gt3":{"engine":3}}""",
        settingsJson = """{"fps":60}""",
        careerProgressJson = """{"tier":2}"""
      ).getOrThrow()
    }
    assertEquals(5L, initialSave.syncRevision)

    // Attempt stale offline overwrite with revision 3 -> must fail
    val staleAttempt = withTimeout(DEFAULT_TIMEOUT_MS) {
      repo.syncCloudSave(
        saveSlotId = slotId,
        localSyncRevision = 3L,
        loadoutsJson = """{"car_gt3":{"engine":1}}""",
        settingsJson = """{"fps":30}""",
        careerProgressJson = """{"tier":1}"""
      )
    }
    assertTrue(staleAttempt.isFailure)

    // Newer save revision 6 -> succeeds
    val newerSave = withTimeout(DEFAULT_TIMEOUT_MS) {
      repo.syncCloudSave(
        saveSlotId = slotId,
        localSyncRevision = 6L,
        loadoutsJson = """{"car_gt3":{"engine":5}}""",
        settingsJson = """{"fps":120}""",
        careerProgressJson = """{"tier":4}"""
      ).getOrThrow()
    }
    assertEquals(6L, newerSave.syncRevision)

    // Create immutable V8 rollback snapshot
    val backup = withTimeout(DEFAULT_TIMEOUT_MS) {
      repo.createCloudRollbackSnapshot(
        loadoutsJson = """{"car_gt3":{"engine":4}}""",
        settingsJson = """{"graphics":"HIGH"}""",
        careerProgressJson = """{"v8Tier":3}"""
      ).getOrThrow()
    }
    assertEquals(ApexBackendRepository.V8_ROLLBACK_SLOT_ID, backup.saveSlotId)

    // Zero-trust rule: modifying or deleting v8_rollback_backup must fail with PERMISSION_DENIED
    var overwriteBackupErr: Throwable? = null
    try {
      withTimeout(DEFAULT_TIMEOUT_MS) {
        firestore.collection("users").document(uid)
          .collection("cloud_saves").document(ApexBackendRepository.V8_ROLLBACK_SLOT_ID)
          .update(mapOf("syncRevision" to 2L, "loadoutsJson" to "{}"))
          .await()
      }
    } catch (e: Throwable) {
      overwriteBackupErr = e
    }
    assertEquals(
      FirebaseFirestoreException.Code.PERMISSION_DENIED,
      overwriteBackupErr?.unwrapFirestoreException()?.code
    )

    // Rollback from backup slot restores cleanly into primary slot
    val rolledBack = withTimeout(DEFAULT_TIMEOUT_MS) {
      repo.rollbackFromCloudBackupSlot().getOrThrow()
    }
    assertEquals("primary", rolledBack.saveSlotId)
    assertEquals("""{"car_gt3":{"engine":4}}""", rolledBack.loadoutsJson)
    assertTrue(repo.getRollbackAndRecoveryPlan().recoverySteps.size >= 4)
  }

  @Test
  fun directClientWritesToServerAuthoritativeCollections_failWithPermissionDenied() = runBlocking {
    val racerEmail = "zero_trust_${UUID.randomUUID().toString().take(8)}@test.com"
    val uid = signInTestUser(racerEmail)
    val repo = ApexBackendRepository(firestore, auth)

    // 1. Direct client economy_transactions write -> PERMISSION_DENIED
    val txAttempt = withTimeout(DEFAULT_TIMEOUT_MS) {
      repo.recordEconomyTransaction(
        txId = "tx_${UUID.randomUUID().toString().replace("-", "").take(12)}",
        txType = "MISSION_REWARD",
        coinsDelta = 500L,
        creditsDelta = 50000L,
        balanceAfterCoins = 750L,
        balanceAfterCredits = 98500L,
        referenceId = "spoof"
      )
    }
    assertTrue(txAttempt.isFailure)
    assertEquals(
      FirebaseFirestoreException.Code.PERMISSION_DENIED,
      txAttempt.exceptionOrNull()?.unwrapFirestoreException()?.code
    )

    // 2. Direct client event_completions write -> PERMISSION_DENIED
    val eventAttempt = withTimeout(DEFAULT_TIMEOUT_MS) {
      repo.upsertEventCompletion(
        eventId = "evt_spoof_01",
        eventType = "DAILY_MISSION",
        progressValue = 3L,
        targetValue = 3L,
        rewardCredits = 9000L,
        rewardCoins = 50L,
        claimed = false
      )
    }
    assertTrue(eventAttempt.isFailure)
    assertEquals(
      FirebaseFirestoreException.Code.PERMISSION_DENIED,
      eventAttempt.exceptionOrNull()?.unwrapFirestoreException()?.code
    )

    // 3. Direct client achievements write -> PERMISSION_DENIED
    val achAttempt = withTimeout(DEFAULT_TIMEOUT_MS) {
      repo.upsertAchievementProgress(
        achievementId = "ach_spoof_01",
        title = "Spoofed Achievement",
        category = "DRIFT",
        progressValue = 1L,
        targetValue = 1L,
        rewardCredits = 10000L,
        rewardCoins = 100L,
        rewardXp = 5000L
      )
    }
    assertTrue(achAttempt.isFailure)
    assertEquals(
      FirebaseFirestoreException.Code.PERMISSION_DENIED,
      achAttempt.exceptionOrNull()?.unwrapFirestoreException()?.code
    )

    // 4. Direct client race_results write -> PERMISSION_DENIED
    val raceAttempt = withTimeout(DEFAULT_TIMEOUT_MS) {
      repo.submitVerifiedRaceResult(
        raceResultId = "race_spoof_01",
        driverName = "Spoofer",
        trackId = "track_monza",
        trackName = "Monza GP",
        vehicleId = "car_gt3",
        vehicleName = "Apex GT3-R",
        gameMode = "TIME_TRIAL",
        finishingPosition = 1L,
        raceDurationMs = 158000L,
        lapTimeMs = 78450L,
        topSpeedKmh = 326.4,
        creditsReward = 6000L,
        xpReward = 1200L
      )
    }
    assertTrue(raceAttempt.isFailure)
    assertEquals(
      FirebaseFirestoreException.Code.PERMISSION_DENIED,
      raceAttempt.exceptionOrNull()?.unwrapFirestoreException()?.code
    )

    // 5. Server-seeded authoritative race_results doc IS queryable by leaderboard
    val validRaceId = "race_srv_${UUID.randomUUID().toString().replace("-", "").take(10)}"
    seedEmulatorDocument(
      "race_results/$validRaceId",
      """{
        "userId": {"stringValue": "$uid"},
        "driverName": {"stringValue": "VerifiedServerDriver"},
        "trackId": {"stringValue": "track_monza"},
        "trackName": {"stringValue": "Monza GP"},
        "vehicleId": {"stringValue": "car_gt3"},
        "vehicleName": {"stringValue": "Apex GT3-R"},
        "gameMode": {"stringValue": "TIME_TRIAL"},
        "finishingPosition": {"integerValue": "1"},
        "raceDurationMs": {"integerValue": "158000"},
        "lapTimeMs": {"integerValue": "78450"},
        "topSpeedKmh": {"doubleValue": 326.4},
        "creditsReward": {"integerValue": "6000"},
        "xpReward": {"integerValue": "1200"},
        "weekBucket": {"stringValue": "2026-W46"},
        "platform": {"stringValue": "ANDROID"}
      }"""
    )

    val leaderboard = withTimeout(DEFAULT_TIMEOUT_MS) {
      repo.queryLeaderboard(trackId = "track_monza").getOrThrow()
    }
    assertTrue("Server-verified race result must appear on leaderboard", leaderboard.any { it.id == validRaceId })
  }

  @Test
  fun getPlayerProfile_crossUserAccess_failsWithPermissionDenied() = runBlocking {
    val aliceUid = signInTestUser("alice_${UUID.randomUUID().toString().take(8)}@test.com")
    val aliceRepo = ApexBackendRepository(firestore, auth)
    withTimeout(DEFAULT_TIMEOUT_MS) {
      aliceRepo.createStarterPlayerProfile(username = "Alice").getOrThrow()
    }

    // Bob signs in and attempts to read Alice's private profile document
    signInTestUser("bob_${UUID.randomUUID().toString().take(8)}@test.com")
    val bobRepo = ApexBackendRepository(firestore, auth)
    val result = withTimeout(DEFAULT_TIMEOUT_MS) {
      bobRepo.getPlayerProfile(targetUserId = aliceUid)
    }
    assertTrue(result.isFailure)
    val fsEx = result.exceptionOrNull()?.unwrapFirestoreException()
    assertNotNull(fsEx)
    assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, fsEx?.code)
  }

  @Test
  fun observePlayerProfile_unauthenticatedUser_failsFast() = runBlocking {
    auth.signOut()
    val repo = ApexBackendRepository(firestore, auth)
    var caught: Throwable? = null
    try {
      withTimeout(FLOW_TIMEOUT_MS) {
        repo.observePlayerProfile().first()
      }
    } catch (e: Throwable) {
      caught = e
    }
    assertNotNull(caught)
  }

  @Test
  fun createAndFinishMultiplayerRoom_enforcesTerminalStateLock() = runBlocking {
    signInTestUser(ALICE_EMAIL)
    val repo = ApexBackendRepository(firestore, auth)
    val roomId = "room_${UUID.randomUUID().toString().replace("-", "").take(12)}"

    val room = withTimeout(DEFAULT_TIMEOUT_MS) {
      repo.createMultiplayerRoom(
        roomId = roomId,
        roomCode = "APX200",
        trackId = "track_monza"
      ).getOrThrow()
    }
    assertEquals("WAITING", room.status)

    // Host marks room FINISHED
    val finishRes = withTimeout(DEFAULT_TIMEOUT_MS) {
      repo.updateMultiplayerRoomStatus(roomId, "FINISHED")
    }
    assertTrue(finishRes.isSuccess)

    // Attempting to reopen a FINISHED room fails due to terminal state lock in rules
    val reopenAttempt = withTimeout(DEFAULT_TIMEOUT_MS) {
      repo.updateMultiplayerRoomStatus(roomId, "WAITING")
    }
    assertTrue(reopenAttempt.isFailure)
  }

  @Test
  fun cloudSyncCoordinator_offlineFifoQueueAndAuthoritativeMerge_preservesProgression() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    ApexRivals2SettingsRepository.initialize(context, forceReload = true)
    ApexRivals2CareerManager.resetForTesting()
    ApexCloudSyncCoordinator.clearQueueForTesting(context)

    // 1. Enable simulated offline mode and queue operations across race, reward, purchase, upgrade, metadata
    ApexCloudSyncCoordinator.setSimulatedOffline(context, true)
    ApexCloudSyncCoordinator.onRaceCompleted(
      context = context,
      raceId = "race_offline_01",
      driverName = "OfflinePilot",
      trackId = "track_monza",
      trackName = "Monza GP",
      vehicleId = "car_gt3_apex",
      vehicleName = "Apex GT3-R",
      gameMode = "CAREER",
      finishingPosition = 1,
      raceDurationMs = 140000L,
      lapTimeMs = 69500L,
      topSpeedKmh = 328f
    )
    ApexCloudSyncCoordinator.onRewardClaimed(context, "daily_login_claim", "rwd_daily_test")
    ApexCloudSyncCoordinator.onVehiclePurchased(context, "car_manthey_911_gt3")
    ApexCloudSyncCoordinator.onVehiclePurchased(context, "car_manthey_911_gt3") // idempotent coalesce
    ApexCloudSyncCoordinator.onSubsystemUpgraded(context, "car_gt3_apex", "ENGINE", 3)
    ApexCloudSyncCoordinator.onProfileMetadataUpdated(context, "OfflinePilotPro", "avatar_gold")

    // Must have 5 queued operations (duplicate purchase coalesced)
    assertEquals(5, ApexCloudSyncCoordinator.getPendingQueueSize(context))

    // Flushing while offline must NOT drop any queued operations
    val remainingWhileOffline = ApexCloudSyncCoordinator.flushPendingQueue(context)
    assertEquals(5, remainingWhileOffline)
    assertEquals(CloudSyncStatus.OFFLINE_QUEUED, ApexCloudSyncCoordinator.uiState.value.status)

    // 2. Non-destructive merge of server PlayerProfileModel into CareerManager
    val serverProfile = PlayerProfileModel(
      userId = "uid_server_99",
      username = "CloudMaster",
      level = 12L,
      xp = 21000L,
      coins = 520L,
      credits = 145000L,
      totalRaces = 32L,
      totalVictories = 19L,
      ownedVehicleIds = listOf("car_gt3_apex", "car_scuderia_corsa", "car_formula_apex"),
      unlockedAchievements = listOf("ach_first_win", "ach_pro_champ"),
      unlockedTrackIds = listOf("track_neo_tokyo", "track_monza", "track_suzuka"),
      syncRevision = 7L
    )
    ApexRivals2CareerManager.applyAuthoritativeCloudProfile(serverProfile, null, context)
    val merged = ApexRivals2CareerManager.state.value
    assertEquals("uid_server_99", merged.playerId)
    assertEquals("CloudMaster", merged.playerUsername)
    assertEquals(12, merged.playerLevel)
    assertEquals(145000, merged.creditsBalance)
    assertEquals(520, merged.coinsBalance)
    assertTrue("car_scuderia_corsa" in merged.ownedVehicleIds)
    assertTrue("car_formula_apex" in merged.ownedVehicleIds)
    assertTrue("track_suzuka" in merged.unlockedTrackIds)

    ApexCloudSyncCoordinator.clearQueueForTesting(context)
  }

  private companion object {
    const val ALICE_EMAIL = "alice.apex@test.com"
    const val BOB_EMAIL = "bob.apex@test.com"
    const val DEFAULT_TIMEOUT_MS = 5000L
    const val FLOW_TIMEOUT_MS = 3000L
  }
}
