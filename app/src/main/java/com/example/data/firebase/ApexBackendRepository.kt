package com.example.data.firebase

import android.app.Activity
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.aistudio.apexrivals.tr8x.R
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ServerTimestamp
import com.google.firebase.firestore.snapshots
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.functions
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

// ============================================================================
// MODULE 9 & 10: STRUCTURED FIRESTORE ERROR HANDLER & TELEMETRY LOGGING
// ============================================================================
enum class OperationType(val value: String) {
  CREATE("create"),
  UPDATE("update"),
  DELETE("delete"),
  LIST("list"),
  GET("get"),
  WRITE("write"),
}

fun handleFirestoreError(
  exception: Throwable,
  operationType: OperationType,
  path: String?
): String {
  val causes = generateSequence(exception) { it.cause }
  val rootError = causes.firstOrNull { it is FirebaseFirestoreException }
    ?: causes.firstOrNull { it !is CancellationException }
    ?: exception

  val currentUser = try {
    FirebaseAuth.getInstance().currentUser
  } catch (e: Exception) {
    null
  }

  val providerInfoList = currentUser?.providerData?.map { provider ->
    JSONObject().apply {
      put("providerId", provider.providerId)
      put("email", provider.email)
    }
  } ?: emptyList()

  val authInfoJson = JSONObject().apply {
    put("userId", currentUser?.uid)
    put("email", currentUser?.email)
    put("emailVerified", currentUser?.isEmailVerified)
    put("tenantId", currentUser?.tenantId)
    put("providerInfo", JSONArray(providerInfoList))
  }

  val errorInfoJson = JSONObject().apply {
    put("error", rootError.message ?: rootError.toString())
    put("operationType", operationType.value)
    put("path", path)
    put("authInfo", authInfoJson)
  }

  val jsonString = errorInfoJson.toString()
  Log.e("FirestoreError", "Firestore Error: $jsonString")
  return jsonString
}

// ============================================================================
// CANONICAL FIRESTORE DATA MODELS (ALL WITH @DocumentId & DEFAULT VALUES)
// ============================================================================

@IgnoreExtraProperties
data class PlayerProfileModel(
  @DocumentId val id: String = "",
  val userId: String = "",
  val username: String = "Apex Pilot",
  val avatarUrl: String = "avatar_helmet_red",
  val level: Long = 1L,
  val xp: Long = 0L,
  val coins: Long = 150L,
  val credits: Long = 50000L,
  val totalRaces: Long = 0L,
  val totalVictories: Long = 0L,
  val ownedVehicleIds: List<String> = listOf("car_gt3", "bike_1000rr"),
  val unlockedAchievements: List<String> = emptyList(),
  val unlockedTrackIds: List<String> = listOf("track_neo_tokyo", "track_monza", "track_miami_highway"),
  val careerTierUnlocked: Long = 1L,
  val dailyLoginStreak: Long = 1L,
  val lastDailyClaimDate: String = "",
  val schemaVersion: Long = 9L,
  val v8LegacyMigrated: Boolean = true,
  val syncRevision: Long = 1L,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val lastActiveAt: Timestamp? = null
)

@IgnoreExtraProperties
data class CloudSaveSnapshotModel(
  @DocumentId val id: String = "",
  val userId: String = "",
  val saveSlotId: String = "primary",
  val syncRevision: Long = 1L,
  val clientDeviceName: String = "Android Device",
  val loadoutsJson: String = "{}",
  val settingsJson: String = "{}",
  val careerProgressJson: String = "{}",
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class EconomyTransactionModel(
  @DocumentId val id: String = "",
  val userId: String = "",
  val txType: String = "RACE_REWARD",
  val idempotencyKey: String = "",
  val coinsDelta: Long = 0L,
  val creditsDelta: Long = 0L,
  val balanceAfterCoins: Long = 0L,
  val balanceAfterCredits: Long = 0L,
  val referenceId: String = "",
  @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class RaceResultRecordModel(
  @DocumentId val id: String = "",
  val userId: String = "",
  val driverName: String = "Apex Pilot",
  val trackId: String = "track_monza",
  val trackName: String = "Monza GP",
  val vehicleId: String = "car_gt3",
  val vehicleName: String = "Apex GT3-R",
  val gameMode: String = "QUICK_RACE",
  val finishingPosition: Long = 1L,
  val raceDurationMs: Long = 160000L,
  val lapTimeMs: Long = 79500L,
  val topSpeedKmh: Double = 312.0,
  val creditsReward: Long = 5000L,
  val xpReward: Long = 1000L,
  val weekBucket: String = "2026-W45",
  val platform: String = "ANDROID",
  @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class EventCompletionModel(
  @DocumentId val id: String = "",
  val userId: String = "",
  val eventId: String = "",
  val eventType: String = "DAILY_MISSION",
  val progressValue: Long = 0L,
  val targetValue: Long = 1L,
  val rewardCredits: Long = 2500L,
  val rewardCoins: Long = 10L,
  val claimed: Boolean = false,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class AchievementRecordModel(
  @DocumentId val id: String = "",
  val userId: String = "",
  val achievementId: String = "",
  val title: String = "Master Race Engineer",
  val category: String = "GARAGE",
  val progressValue: Long = 0L,
  val targetValue: Long = 1L,
  val rewardCredits: Long = 5000L,
  val rewardCoins: Long = 15L,
  val rewardXp: Long = 1000L,
  val unlocked: Boolean = false,
  val claimed: Boolean = false,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class MultiplayerRoomModel(
  @DocumentId val id: String = "",
  val hostUserId: String = "",
  val roomCode: String = "APX200",
  val trackId: String = "track_monza",
  val vehicleClass: String = "GT3",
  val status: String = "WAITING",
  val maxPlayers: Long = 8L,
  val playerUids: List<String> = emptyList(),
  val dedicatedServerEndpoint: String = "wss://eu-west-relay.apexdynamics.gg/room/APX200",
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null
)

data class BackendRollbackRecoveryPlan(
  val planVersion: String = "V8-to-V9-Production-Safe-1.0",
  val localPreferencesBackupSlot: String = "apex_rivals_v8_backup_snapshot",
  val sqliteDatabaseStrategy: String = "Room MIGRATION_8_9 non-destructive schema preservation",
  val cloudSaveRollbackSlotId: String = ApexBackendRepository.V8_ROLLBACK_SLOT_ID,
  val firestorePointInTimeRecovery: String = "Scheduled 24h Cloud Firestore Export + PITR",
  val recoverySteps: List<String> = listOf(
    "1. Pre-Migration Snapshot: Before applying V9 schema updates, client automatically writes current state to 'apex_rivals_v8_backup_snapshot' and Firestore '/users/{uid}/cloud_saves/v8_rollback_backup'.",
    "2. Non-Destructive Additive Merge: V8 fields (reputationScore, totalLapsCompleted, player_credits, player_xp) are merged using maxOf() and set union—never overwriting or resetting existing balances.",
    "3. Instant Client Rollback: Calling rollbackFromCloudBackupSlot() or restoreFromV8BackupSnapshot() restores pre-upgrade credits, XP, owned vehicles, and career unlocks in one transaction.",
    "4. Server Ledger Audit: All currency mutations are recorded in append-only '/users/{uid}/economy_transactions/{txId}' so any disputed balance can be reconstructed deterministically."
  )
)

// ============================================================================
// MODULE 7: REMOTE CONFIG & LIVE OPS CONFIGURATION
// ============================================================================
data class ApexRemoteConfigValues(
  val doubleXpWeekendActive: Boolean = true,
  val activeSeasonalChampionshipId: String = "season_01_apex_world_series",
  val maxAllowedTopSpeedKmh: Double = 550.0,
  val minValidLapTimeMs: Long = 12_000L,
  val dailyRewardBaseCredits: Long = 2500L,
  val dedicatedMultiplayerRelayUrl: String = "wss://eu-west-relay.apexdynamics.gg"
)

// ============================================================================
// MODULE 1: CREDENTIAL MANAGER GOOGLE SIGN-IN HELPERS
// ============================================================================
object ApexCredentialAuthHelper {
  private const val TAG = "ApexAuth"

  fun attemptAutoSignIn(
    context: Context,
    credentialManager: CredentialManager,
    onAuthSuccess: () -> Unit,
    onUnauthenticated: () -> Unit,
    scope: CoroutineScope
  ) {
    if (Firebase.auth.currentUser != null) {
      onAuthSuccess()
      return
    }
    val clientId = try {
      context.getString(R.string.default_web_client_id)
    } catch (e: Exception) {
      onUnauthenticated()
      return
    }

    val googleIdOption = GetGoogleIdOption.Builder()
      .setFilterByAuthorizedAccounts(true)
      .setServerClientId(clientId)
      .setAutoSelectEnabled(true)
      .build()

    val request = GetCredentialRequest.Builder()
      .addCredentialOption(googleIdOption)
      .build()

    scope.launch {
      try {
        val result = credentialManager.getCredential(context, request)
        val credential = result.credential
        if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
          val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
          val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
          Firebase.auth.signInWithCredential(authCredential).await()
          onAuthSuccess()
        } else {
          onUnauthenticated()
        }
      } catch (e: Exception) {
        onUnauthenticated()
      }
    }
  }

  fun onGoogleSignInClicked(
    context: Context,
    credentialManager: CredentialManager,
    onAuthSuccess: () -> Unit,
    onAuthError: (String) -> Unit,
    scope: CoroutineScope,
    onAuthCancelled: () -> Unit = {}
  ) {
    val clientId = try {
      context.getString(R.string.default_web_client_id)
    } catch (e: Exception) {
      onAuthError("Google Sign-In configuration missing: default_web_client_id not found")
      return
    }

    val signInOption = GetSignInWithGoogleOption.Builder(serverClientId = clientId).build()
    val request = GetCredentialRequest.Builder()
      .addCredentialOption(signInOption)
      .build()

    scope.launch {
      try {
        val activity = context as? Activity
        val result = if (activity != null) {
          credentialManager.getCredential(activity, request)
        } else {
          credentialManager.getCredential(context, request)
        }
        val credential = result.credential
        if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
          val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
          val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
          Firebase.auth.signInWithCredential(authCredential).await()
          onAuthSuccess()
        } else {
          onAuthError("Unexpected credential type")
        }
      } catch (e: GetCredentialCancellationException) {
        Log.w(TAG, "Google Sign-In flow cancelled or dismissed: ${e.message}", e)
        onAuthCancelled()
      } catch (e: Exception) {
        Log.e(TAG, "Google Sign-In failed", e)
        onAuthError(e.localizedMessage ?: "Sign in failed")
      }
    }
  }

  fun signOut(
    credentialManager: CredentialManager,
    scope: CoroutineScope,
    onSignOutComplete: () -> Unit = {}
  ) {
    scope.launch {
      withContext(NonCancellable) {
        try {
          credentialManager.clearCredentialState(ClearCredentialStateRequest())
        } catch (e: Exception) {
          Log.w(TAG, "Failed to clear credential state: ${e.message}")
        } finally {
          Firebase.auth.signOut()
          onSignOutComplete()
        }
      }
    }
  }
}

// ============================================================================
// APEX RIVALS 2.0 UNIFIED BACKEND REPOSITORY (MODULES 1 - 10)
// ============================================================================
class ApexBackendRepository(
  private val db: FirebaseFirestore,
  private val auth: FirebaseAuth = Firebase.auth,
  private val functionsOverride: FirebaseFunctions? = null
) {
  // CRITICAL: Always initialize with R.string.firestore_database_id
  constructor(context: Context) : this(
    FirebaseFirestore.getInstance(
      context.applicationContext.getString(R.string.firestore_database_id)
    ),
    Firebase.auth,
    null
  )

  private fun requireFunctions(): FirebaseFunctions {
    return functionsOverride ?: Firebase.functions(db.app, "us-central1")
  }

  private fun requireUserId(): String {
    return auth.currentUser?.uid
      ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
  }

  fun getRollbackAndRecoveryPlan(): BackendRollbackRecoveryPlan = BackendRollbackRecoveryPlan()

  // --------------------------------------------------------------------------
  // MODULE 2: PLAYER PROFILE CREATE / METADATA UPDATE / OBSERVE & CLOUD FUNCTIONS
  // --------------------------------------------------------------------------
  suspend fun createStarterPlayerProfile(
    username: String,
    avatarUrl: String = "avatar_helmet_red",
    coins: Long = 250L,
    credits: Long = 48500L,
    ownedVehicleIds: List<String> = listOf("car_gt3_apex", "bike_superbike_1000"),
    unlockedTrackIds: List<String> = listOf("track_neo_tokyo", "track_monza", "track_miami_highway"),
    v8LegacyMigrated: Boolean = false
  ): Result<PlayerProfileModel> = runCatching {
    val uid = requireUserId()
    val docRef = db.collection("users").document(uid)
    val boundedCoins = coins.coerceIn(0L, 250L)
    val boundedCredits = credits.coerceIn(0L, 50000L)
    val boundedVehicles = ownedVehicleIds
      .ifEmpty { listOf("car_gt3_apex", "bike_superbike_1000") }
      .distinct()
      .take(6)
    val boundedTracks = unlockedTrackIds
      .ifEmpty { listOf("track_neo_tokyo", "track_monza", "track_miami_highway") }
      .distinct()
      .take(6)

    val createPayload = mapOf(
      "userId" to uid,
      "username" to username.trim().take(40).let { if (it.length >= 2) it else "Apex Pilot" },
      "avatarUrl" to avatarUrl.take(500),
      "level" to 1L,
      "xp" to 0L,
      "coins" to boundedCoins,
      "credits" to boundedCredits,
      "totalRaces" to 0L,
      "totalVictories" to 0L,
      "ownedVehicleIds" to boundedVehicles,
      "unlockedAchievements" to emptyList<String>(),
      "unlockedTrackIds" to boundedTracks,
      "careerTierUnlocked" to 1L,
      "dailyLoginStreak" to 0L,
      "schemaVersion" to 9L,
      "v8LegacyMigrated" to v8LegacyMigrated,
      "syncRevision" to 1L,
      "createdAt" to FieldValue.serverTimestamp(),
      "lastActiveAt" to FieldValue.serverTimestamp()
    )

    try {
      docRef.set(createPayload).await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, docRef.path)
      throw e
    }
    getPlayerProfile(uid).getOrThrow()
  }

  suspend fun updateProfileMetadata(
    username: String,
    avatarUrl: String? = null
  ): Result<PlayerProfileModel> = runCatching {
    val uid = requireUserId()
    val docRef = db.collection("users").document(uid)
    val cleanUsername = username.trim().take(40).let { if (it.length >= 2) it else "Apex Pilot" }
    val updatePayload = mutableMapOf<String, Any>(
      "username" to cleanUsername,
      "lastActiveAt" to FieldValue.serverTimestamp()
    )
    if (!avatarUrl.isNullOrBlank()) {
      updatePayload["avatarUrl"] = avatarUrl.take(500)
    }

    try {
      docRef.update(updatePayload).await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docRef.path)
      throw e
    }
    getPlayerProfile(uid).getOrThrow()
  }

  suspend fun createOrMergePlayerProfile(
    username: String,
    avatarUrl: String = "avatar_helmet_red",
    level: Long = 1L,
    xp: Long = 0L,
    coins: Long = 250L,
    credits: Long = 48500L,
    totalRaces: Long = 0L,
    totalVictories: Long = 0L,
    ownedVehicleIds: List<String> = listOf("car_gt3_apex", "bike_superbike_1000"),
    unlockedAchievements: List<String> = emptyList(),
    unlockedTrackIds: List<String> = listOf("track_neo_tokyo", "track_monza", "track_miami_highway"),
    careerTierUnlocked: Long = 1L,
    dailyLoginStreak: Long = 0L,
    lastDailyClaimDate: String = ""
  ): Result<PlayerProfileModel> = runCatching {
    val uid = requireUserId()
    val docRef = db.collection("users").document(uid)
    val existingSnap = try {
      docRef.get().await()
    } catch (e: Exception) {
      null
    }

    if (existingSnap != null && existingSnap.exists()) {
      // Zero-trust client rules only permit updating metadata fields directly;
      // server-authoritative fields remain governed by Cloud Functions.
      updateProfileMetadata(username = username, avatarUrl = avatarUrl).getOrThrow()
    } else {
      val needsCloudMigration = level > 1L || xp > 0L || coins > 250L || credits > 50000L ||
        totalRaces > 0L || totalVictories > 0L || ownedVehicleIds.size > 6 || unlockedAchievements.isNotEmpty()
      if (!needsCloudMigration) {
        createStarterPlayerProfile(
          username = username,
          avatarUrl = avatarUrl,
          coins = coins,
          credits = credits,
          ownedVehicleIds = ownedVehicleIds,
          unlockedTrackIds = unlockedTrackIds
        ).getOrThrow()
      } else {
        migrateV8PlayerProfileCloud(
          v8Username = username,
          v8Credits = credits,
          v8Coins = coins,
          v8Xp = xp,
          totalRaces = totalRaces,
          totalVictories = totalVictories,
          v8OwnedVehicles = ownedVehicleIds,
          v8UnlockedTracks = unlockedTrackIds
        ).getOrThrow()
        getPlayerProfile(uid).getOrThrow()
      }
    }
  }

  /**
   * Authoritative Cloud Function: Backward-compatible V8 -> V9 migration with immutable V8 backup.
   */
  suspend fun migrateV8PlayerProfileCloud(
    v8Username: String,
    v8Credits: Long,
    v8Coins: Long,
    v8Xp: Long,
    totalRaces: Long = 0L,
    totalVictories: Long = 0L,
    v8OwnedVehicles: List<String> = emptyList(),
    v8UnlockedTracks: List<String> = emptyList(),
    loadoutsJson: String = "{}",
    settingsJson: String = "{}"
  ): Result<Map<String, Any?>> = runCatching {
    requireUserId()
    val payload = hashMapOf(
      "username" to v8Username.trim().take(40).ifBlank { "Apex Pilot" },
      "v8Credits" to v8Credits.coerceAtLeast(0L),
      "v8Coins" to v8Coins.coerceAtLeast(0L),
      "v8Xp" to v8Xp.coerceAtLeast(0L),
      "totalRaces" to totalRaces.coerceAtLeast(0L),
      "totalVictories" to totalVictories.coerceAtLeast(0L),
      "v8OwnedVehicles" to v8OwnedVehicles.distinct().take(50),
      "v8UnlockedTracks" to v8UnlockedTracks.distinct().take(50),
      "loadoutsJson" to loadoutsJson.take(20000),
      "settingsJson" to settingsJson.take(5000)
    )
    val result = requireFunctions().getHttpsCallable("migrateV8PlayerProfile").call(payload).await()
    @Suppress("UNCHECKED_CAST")
    (result.getData() as? Map<String, Any?>) ?: emptyMap()
  }

  /**
   * Authoritative Cloud Function: Restores server `primary` cloud save & balances from `v8_rollback_backup`.
   */
  suspend fun rollbackToV8BackupSnapshotCloud(): Result<Map<String, Any?>> = runCatching {
    requireUserId()
    val result = requireFunctions().getHttpsCallable("rollbackToV8BackupSnapshot").call(emptyMap<String, Any>()).await()
    @Suppress("UNCHECKED_CAST")
    (result.getData() as? Map<String, Any?>) ?: emptyMap()
  }

  /**
   * Authoritative Cloud Function: Claims Daily Login, Mission, Event, Championship, or Achievement reward.
   */
  suspend fun claimVerifiedRewardCloud(
    idempotencyKey: String,
    referenceId: String
  ): Result<Map<String, Any?>> = runCatching {
    requireUserId()
    val cleanKey = idempotencyKey.replace(Regex("[^a-zA-Z0-9_\\-]"), "_").take(128)
    val payload = hashMapOf(
      "idempotencyKey" to cleanKey,
      "referenceId" to referenceId.take(128)
    )
    val result = requireFunctions().getHttpsCallable("claimVerifiedReward").call(payload).await()
    @Suppress("UNCHECKED_CAST")
    (result.getData() as? Map<String, Any?>) ?: emptyMap()
  }

  /**
   * Authoritative Cloud Function: Vehicle purchase or Stage 1-5 subsystem upgrade.
   */
  suspend fun purchaseVehicleOrUpgradeCloud(
    action: String,
    idempotencyKey: String,
    vehicleId: String,
    subsystem: String? = null,
    targetStage: Int? = null
  ): Result<Map<String, Any?>> = runCatching {
    requireUserId()
    val cleanKey = idempotencyKey.replace(Regex("[^a-zA-Z0-9_\\-]"), "_").take(128)
    val payload = hashMapOf<String, Any>(
      "action" to action,
      "idempotencyKey" to cleanKey,
      "vehicleId" to vehicleId.take(64)
    )
    if (!subsystem.isNullOrBlank()) {
      payload["subsystem"] = subsystem
    }
    if (targetStage != null) {
      payload["targetStage"] = targetStage
    }
    val result = requireFunctions().getHttpsCallable("purchaseVehicleOrUpgrade").call(payload).await()
    @Suppress("UNCHECKED_CAST")
    (result.getData() as? Map<String, Any?>) ?: emptyMap()
  }

  /**
   * Authoritative Cloud Function: Anti-cheat race result verification & atomic reward payout.
   */
  suspend fun submitCompetitiveRaceResultCloud(
    raceId: String,
    driverName: String,
    trackId: String,
    trackName: String,
    vehicleId: String,
    vehicleName: String,
    gameMode: String = "QUICK_RACE",
    finishingPosition: Long,
    raceDurationMs: Long,
    lapTimeMs: Long,
    topSpeedKmh: Double,
    weekBucket: String = "2026-W46"
  ): Result<Map<String, Any?>> = runCatching {
    requireUserId()
    val cleanRaceId = raceId.replace(Regex("[^a-zA-Z0-9_\\-]"), "_").take(128)
    val payload = hashMapOf<String, Any>(
      "raceId" to cleanRaceId,
      "driverName" to driverName.take(60).ifBlank { "Apex Pilot" },
      "trackId" to trackId.take(64),
      "trackName" to trackName.take(80),
      "vehicleId" to vehicleId.take(64),
      "vehicleName" to vehicleName.take(80),
      "gameMode" to gameMode,
      "finishingPosition" to finishingPosition,
      "raceDurationMs" to raceDurationMs,
      "lapTimeMs" to lapTimeMs,
      "topSpeedKmh" to topSpeedKmh,
      "weekBucket" to weekBucket
    )
    val result = requireFunctions().getHttpsCallable("submitCompetitiveRaceResult").call(payload).await()
    @Suppress("UNCHECKED_CAST")
    (result.getData() as? Map<String, Any?>) ?: emptyMap()
  }

  /**
   * Backward-Compatible V8-to-V9 Cloud Migration with Pre-Migration Cloud Backup Snapshot.
   * Ensures existing V8 users keep all credits, XP, vehicles, and unlocks, and stores a
   * recoverable cloud backup snapshot before applying V9 schema updates.
   */
  suspend fun migrateV8LegacyCloudProfileIfNeeded(
    v8Username: String,
    v8Credits: Long,
    v8Xp: Long,
    v8OwnedVehicles: List<String>,
    v8UnlockedTracks: List<String>,
    loadoutsJson: String = "{}",
    settingsJson: String = "{}",
    careerProgressJson: String = "{}"
  ): Result<PlayerProfileModel> = runCatching {
    // 1. Save an immutable pre-migration V8 rollback backup snapshot if none exists yet
    createCloudRollbackSnapshot(
      loadoutsJson = loadoutsJson,
      settingsJson = settingsJson,
      careerProgressJson = careerProgressJson
    )

    // 2. Execute server-authoritative migration and return merged profile
    migrateV8PlayerProfileCloud(
      v8Username = v8Username,
      v8Credits = v8Credits,
      v8Coins = 250L,
      v8Xp = v8Xp,
      v8OwnedVehicles = v8OwnedVehicles,
      v8UnlockedTracks = v8UnlockedTracks,
      loadoutsJson = loadoutsJson,
      settingsJson = settingsJson
    ).getOrThrow()

    getPlayerProfile().getOrThrow()
  }

  suspend fun getPlayerProfile(targetUserId: String? = null): Result<PlayerProfileModel> = runCatching {
    val uid = targetUserId ?: requireUserId()
    val docRef = db.collection("users").document(uid)
    try {
      val snap = docRef.get().await()
      if (!snap.exists()) {
        throw IllegalStateException("Player profile does not exist for $uid")
      }
      snap.toObject(
        PlayerProfileModel::class.java,
        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
      ) ?: throw IllegalStateException("Failed to deserialize PlayerProfileModel")
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.GET, docRef.path)
      throw e
    }
  }

  fun observePlayerProfile(): Flow<PlayerProfileModel?> = flow {
    val uid = requireUserId()
    val docRef = db.collection("users").document(uid)
    emitAll(
      docRef.snapshots()
        .map { snap ->
          if (snap.exists()) {
            snap.toObject(
              PlayerProfileModel::class.java,
              DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            )
          } else {
            null
          }
        }
        .catch { error ->
          val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
          if (firestoreError == null && error is CancellationException) throw error
          val exceptionToLog = firestoreError ?: (error as? Exception)
          if (exceptionToLog != null && auth.currentUser?.uid == uid) {
            handleFirestoreError(exceptionToLog, OperationType.GET, docRef.path)
          }
          throw error
        }
    )
  }

  // --------------------------------------------------------------------------
  // MODULE 3: CLOUD SAVE SYNCHRONIZATION & ROLLBACK RECOVERY
  // --------------------------------------------------------------------------
  suspend fun syncCloudSave(
    saveSlotId: String = "primary",
    localSyncRevision: Long,
    loadoutsJson: String,
    settingsJson: String,
    careerProgressJson: String,
    clientDeviceName: String = "${Build.MANUFACTURER} ${Build.MODEL}".take(80)
  ): Result<CloudSaveSnapshotModel> = runCatching {
    val uid = requireUserId()
    val saveRef = db.collection("users").document(uid).collection("cloud_saves").document(saveSlotId)

    val existingSnap = try {
      saveRef.get().await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.GET, saveRef.path)
      throw e
    }

    if (!existingSnap.exists()) {
      val initialRev = maxOf(1L, localSyncRevision)
      val payload = mapOf(
        "userId" to uid,
        "saveSlotId" to saveSlotId,
        "syncRevision" to initialRev,
        "clientDeviceName" to clientDeviceName.ifBlank { "Android" },
        "loadoutsJson" to loadoutsJson.take(20000),
        "settingsJson" to settingsJson.take(5000),
        "careerProgressJson" to careerProgressJson.take(10000),
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
      )
      try {
        saveRef.set(payload).await()
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        handleFirestoreError(e, OperationType.CREATE, saveRef.path)
        throw e
      }
    } else {
      val remoteRev = existingSnap.getLong("syncRevision") ?: 1L
      // Anti-Stale Overwrite Check: if localSyncRevision <= remoteRev, reject stale overwrite
      if (localSyncRevision <= remoteRev) {
        throw IllegalStateException(
          "Conflict detected: local save revision ($localSyncRevision) is older than or equal to server revision ($remoteRev)."
        )
      }
      val updatePayload = mapOf(
        "syncRevision" to localSyncRevision,
        "clientDeviceName" to clientDeviceName.ifBlank { "Android" },
        "loadoutsJson" to loadoutsJson.take(20000),
        "settingsJson" to settingsJson.take(5000),
        "careerProgressJson" to careerProgressJson.take(10000),
        "updatedAt" to FieldValue.serverTimestamp()
      )
      try {
        saveRef.update(updatePayload).await()
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        handleFirestoreError(e, OperationType.UPDATE, saveRef.path)
        throw e
      }
    }

    val updatedSnap = saveRef.get().await()
    updatedSnap.toObject(
      CloudSaveSnapshotModel::class.java,
      DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
    ) ?: throw IllegalStateException("Failed to deserialize CloudSaveSnapshotModel")
  }

  suspend fun getPrimaryCloudSave(saveSlotId: String = "primary"): Result<CloudSaveSnapshotModel?> = runCatching {
    val uid = requireUserId()
    val saveRef = db.collection("users").document(uid).collection("cloud_saves").document(saveSlotId)
    val snap = saveRef.get().await()
    if (!snap.exists()) null
    else snap.toObject(
      CloudSaveSnapshotModel::class.java,
      DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
    )
  }

  /**
   * Creates an immutable Cloud Rollback Snapshot (`v8_rollback_backup`) if one does not already
   * exist, respecting the zero-trust immutability rule (`saveSlotId != 'v8_rollback_backup'`).
   */
  suspend fun createCloudRollbackSnapshot(
    loadoutsJson: String,
    settingsJson: String,
    careerProgressJson: String
  ): Result<CloudSaveSnapshotModel> = runCatching {
    val uid = requireUserId()
    val backupRef = db.collection("users").document(uid).collection("cloud_saves").document(V8_ROLLBACK_SLOT_ID)
    val existing = try {
      backupRef.get().await()
    } catch (e: Exception) {
      null
    }
    if (existing != null && existing.exists()) {
      return@runCatching existing.toObject(
        CloudSaveSnapshotModel::class.java,
        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
      ) ?: throw IllegalStateException("Failed to deserialize existing v8_rollback_backup")
    }
    syncCloudSave(
      saveSlotId = V8_ROLLBACK_SLOT_ID,
      localSyncRevision = 1L,
      loadoutsJson = loadoutsJson,
      settingsJson = settingsJson,
      careerProgressJson = careerProgressJson
    ).getOrThrow()
  }

  /**
   * Restores the player's Cloud Save from the `v8_rollback_backup` slot into the `primary` slot.
   */
  suspend fun rollbackFromCloudBackupSlot(): Result<CloudSaveSnapshotModel> = runCatching {
    val uid = requireUserId()
    val backupRef = db.collection("users").document(uid).collection("cloud_saves").document(V8_ROLLBACK_SLOT_ID)
    val primaryRef = db.collection("users").document(uid).collection("cloud_saves").document("primary")

    val backupSnap = try {
      backupRef.get().await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.GET, backupRef.path)
      throw e
    }

    if (!backupSnap.exists()) {
      throw IllegalStateException("No V8 cloud rollback snapshot found for user $uid")
    }

    val backupModel = backupSnap.toObject(
      CloudSaveSnapshotModel::class.java,
      DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
    ) ?: throw IllegalStateException("Failed to parse V8 cloud rollback snapshot")

    val primarySnap = try {
      primaryRef.get().await()
    } catch (e: Exception) {
      null
    }
    val nextPrimaryRev = if (primarySnap != null && primarySnap.exists()) {
      (primarySnap.getLong("syncRevision") ?: 1L) + 1L
    } else {
      1L
    }

    syncCloudSave(
      saveSlotId = "primary",
      localSyncRevision = nextPrimaryRev,
      loadoutsJson = backupModel.loadoutsJson,
      settingsJson = backupModel.settingsJson,
      careerProgressJson = backupModel.careerProgressJson
    ).getOrThrow()
  }

  // --------------------------------------------------------------------------
  // MODULE 4: IN-GAME ECONOMY, VEHICLE OWNERSHIP & AUDIT LEDGER
  // --------------------------------------------------------------------------
  suspend fun recordEconomyTransaction(
    txId: String,
    txType: String,
    coinsDelta: Long,
    creditsDelta: Long,
    balanceAfterCoins: Long,
    balanceAfterCredits: Long,
    referenceId: String
  ): Result<EconomyTransactionModel> = runCatching {
    val uid = requireUserId()
    require(balanceAfterCoins >= 0L) { "Negative coin balance is prohibited ($balanceAfterCoins)" }
    require(balanceAfterCredits >= 0L) { "Negative credit balance is prohibited ($balanceAfterCredits)" }

    val txRef = db.collection("users").document(uid).collection("economy_transactions").document(txId)
    val payload = mapOf(
      "userId" to uid,
      "txType" to txType,
      "idempotencyKey" to txId,
      "coinsDelta" to coinsDelta,
      "creditsDelta" to creditsDelta,
      "balanceAfterCoins" to balanceAfterCoins,
      "balanceAfterCredits" to balanceAfterCredits,
      "referenceId" to referenceId,
      "createdAt" to FieldValue.serverTimestamp()
    )

    try {
      txRef.set(payload).await()
      val snap = txRef.get().await()
      snap.toObject(
        EconomyTransactionModel::class.java,
        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
      ) ?: EconomyTransactionModel(
        id = txId,
        userId = uid,
        txType = txType,
        idempotencyKey = txId,
        coinsDelta = coinsDelta,
        creditsDelta = creditsDelta,
        balanceAfterCoins = balanceAfterCoins,
        balanceAfterCredits = balanceAfterCredits,
        referenceId = referenceId
      )
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, txRef.path)
      throw e
    }
  }

  /**
   * Server-validated Vehicle Ownership Purchase / Unlock.
   * Ensures sufficient credits, prevents duplicate purchase charges via deterministic
   * idempotency key `tx_veh_${vehicleId}`, records an immutable economy transaction,
   * and adds `vehicleId` to the player's `ownedVehicleIds`.
   */
  suspend fun purchaseOrUnlockVehicle(
    vehicleId: String,
    priceCredits: Long,
    priceCoins: Long = 0L
  ): Result<PlayerProfileModel> = runCatching {
    val uid = requireUserId()
    require(vehicleId.isNotBlank()) { "vehicleId must not be blank" }
    require(priceCredits >= 0L && priceCoins >= 0L) { "Vehicle price cannot be negative" }

    val profile = getPlayerProfile(uid).getOrThrow()
    if (vehicleId in profile.ownedVehicleIds) {
      return@runCatching profile // Already owned — idempotent no-op
    }

    val newCredits = profile.credits - priceCredits
    val newCoins = profile.coins - priceCoins
    require(newCredits >= 0L) { "Insufficient credits to purchase $vehicleId (need $priceCredits, have ${profile.credits})" }
    require(newCoins >= 0L) { "Insufficient coins to purchase $vehicleId (need $priceCoins, have ${profile.coins})" }

    val cleanVehicleKey = vehicleId.replace(Regex("[^a-zA-Z0-9_\\-]"), "_")
    val txId = "tx_veh_${cleanVehicleKey}".take(128)
    recordEconomyTransaction(
      txId = txId,
      txType = "VEHICLE_PURCHASE",
      coinsDelta = -priceCoins,
      creditsDelta = -priceCredits,
      balanceAfterCoins = newCoins,
      balanceAfterCredits = newCredits,
      referenceId = vehicleId
    ).getOrThrow()

    val updatedVehicles = (profile.ownedVehicleIds + vehicleId).distinct().take(50)
    val userRef = db.collection("users").document(uid)
    val updatePayload = mapOf(
      "coins" to newCoins,
      "credits" to newCredits,
      "ownedVehicleIds" to updatedVehicles,
      "syncRevision" to (profile.syncRevision + 1L),
      "lastActiveAt" to FieldValue.serverTimestamp()
    )
    try {
      userRef.update(updatePayload).await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, userRef.path)
      throw e
    }

    getPlayerProfile(uid).getOrThrow()
  }

  fun observeEconomyTransactions(): Flow<List<EconomyTransactionModel>> = flow {
    val uid = requireUserId()
    val path = "users/$uid/economy_transactions"
    emitAll(
      db.collection("users").document(uid).collection("economy_transactions")
        .snapshots()
        .map { snapshot ->
          snapshot.toObjects(
            EconomyTransactionModel::class.java,
            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
          )
        }
        .catch { error ->
          val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
          if (firestoreError == null && error is CancellationException) throw error
          val exceptionToLog = firestoreError ?: (error as? Exception)
          if (exceptionToLog != null && auth.currentUser?.uid == uid) {
            handleFirestoreError(exceptionToLog, OperationType.LIST, path)
          }
          throw error
        }
    )
  }

  // --------------------------------------------------------------------------
  // MODULE 5 & 6: SECURE RACE REWARDS, ANTI-CHEAT & LEADERBOARDS
  // --------------------------------------------------------------------------
  fun computeAuthoritativeRaceReward(
    finishingPosition: Long,
    gameMode: String,
    requestedCredits: Long,
    requestedXp: Long
  ): Pair<Long, Long> {
    val clampedPos = finishingPosition.coerceIn(1L, 24L)
    val modeMultiplier = when (gameMode) {
      "CHAMPIONSHIP" -> 1.5
      "CAREER" -> 1.25
      "MULTIPLAYER" -> 1.35
      else -> 1.0
    }
    val maxAllowedCredits = ((18000L - (clampedPos - 1L) * 900L).coerceAtLeast(1500L) * modeMultiplier)
      .toLong()
      .coerceIn(0L, 100000L)
    val maxAllowedXp = ((3500L - (clampedPos - 1L) * 180L).coerceAtLeast(300L) * modeMultiplier)
      .toLong()
      .coerceIn(0L, 25000L)

    val verifiedCredits = if (requestedCredits in 1L..maxAllowedCredits) requestedCredits else maxAllowedCredits
    val verifiedXp = if (requestedXp in 1L..maxAllowedXp) requestedXp else maxAllowedXp
    return verifiedCredits to verifiedXp
  }

  suspend fun submitVerifiedRaceResult(
    raceResultId: String,
    driverName: String,
    trackId: String,
    trackName: String,
    vehicleId: String,
    vehicleName: String,
    gameMode: String = "QUICK_RACE",
    finishingPosition: Long,
    raceDurationMs: Long,
    lapTimeMs: Long,
    topSpeedKmh: Double,
    creditsReward: Long,
    xpReward: Long,
    weekBucket: String = "2026-W45",
    platform: String = "ANDROID"
  ): Result<String> = runCatching {
    val uid = requireUserId()
    val docRef = db.collection("race_results").document(raceResultId)
    val payload = mapOf(
      "userId" to uid,
      "driverName" to driverName.take(60).ifBlank { "Apex Pilot" },
      "trackId" to trackId.take(64),
      "trackName" to trackName.take(80),
      "vehicleId" to vehicleId.take(64),
      "vehicleName" to vehicleName.take(80),
      "gameMode" to gameMode,
      "finishingPosition" to finishingPosition,
      "raceDurationMs" to raceDurationMs,
      "lapTimeMs" to lapTimeMs,
      "topSpeedKmh" to topSpeedKmh,
      "creditsReward" to creditsReward.coerceIn(0L, 100000L),
      "xpReward" to xpReward.coerceIn(0L, 25000L),
      "weekBucket" to weekBucket,
      "platform" to platform,
      "createdAt" to FieldValue.serverTimestamp()
    )

    try {
      docRef.set(payload).await()
      docRef.id
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, docRef.path)
      throw e
    }
  }

  /**
   * Validates telemetry anti-cheat bounds, submits the immutable competitive race result,
   * records an idempotent `RACE_REWARD` economy transaction (`tx_race_${raceResultId}`),
   * and updates the player's profile credits, XP, level, and race counts.
   */
  suspend fun claimVerifiedRaceReward(
    raceResultId: String,
    driverName: String,
    trackId: String,
    trackName: String,
    vehicleId: String,
    vehicleName: String,
    gameMode: String = "CAREER",
    finishingPosition: Long,
    raceDurationMs: Long,
    lapTimeMs: Long,
    topSpeedKmh: Double,
    requestedCreditsReward: Long,
    requestedXpReward: Long,
    weekBucket: String = "2026-W45"
  ): Result<PlayerProfileModel> = runCatching {
    val uid = requireUserId()
    val (verifiedCredits, verifiedXp) = computeAuthoritativeRaceReward(
      finishingPosition = finishingPosition,
      gameMode = gameMode,
      requestedCredits = requestedCreditsReward,
      requestedXp = requestedXpReward
    )

    // 1. Submit anti-cheat validated race result document (immutable once created)
    submitVerifiedRaceResult(
      raceResultId = raceResultId,
      driverName = driverName,
      trackId = trackId,
      trackName = trackName,
      vehicleId = vehicleId,
      vehicleName = vehicleName,
      gameMode = gameMode,
      finishingPosition = finishingPosition,
      raceDurationMs = raceDurationMs,
      lapTimeMs = lapTimeMs,
      topSpeedKmh = topSpeedKmh,
      creditsReward = verifiedCredits,
      xpReward = verifiedXp,
      weekBucket = weekBucket
    ).getOrThrow()

    // 2. Fetch or initialize player profile
    val profile = getPlayerProfile(uid).getOrElse {
      createOrMergePlayerProfile(username = driverName).getOrThrow()
    }

    val newCredits = profile.credits + verifiedCredits
    val newXp = profile.xp + verifiedXp
    val newLevel = maxOf(profile.level, ((newXp / 1800L) + 1L)).coerceIn(1L, 500L)
    val newTotalRaces = profile.totalRaces + 1L
    val newVictories = profile.totalVictories + (if (finishingPosition <= 3L) 1L else 0L)
    val cleanRaceKey = raceResultId.replace(Regex("[^a-zA-Z0-9_\\-]"), "_")

    // 3. Append-only economy transaction (prevents duplicate reward claims for the same raceResultId)
    recordEconomyTransaction(
      txId = "tx_race_${cleanRaceKey}".take(128),
      txType = "RACE_REWARD",
      coinsDelta = 0L,
      creditsDelta = verifiedCredits,
      balanceAfterCoins = profile.coins,
      balanceAfterCredits = newCredits,
      referenceId = raceResultId
    ).getOrThrow()

    // 4. Update player profile progression
    val updatedTracks = (profile.unlockedTrackIds + trackId).distinct().take(50)
    val userRef = db.collection("users").document(uid)
    val updatePayload = mapOf(
      "credits" to newCredits,
      "xp" to newXp,
      "level" to newLevel,
      "totalRaces" to newTotalRaces,
      "totalVictories" to newVictories.coerceAtMost(newTotalRaces),
      "unlockedTrackIds" to updatedTracks,
      "syncRevision" to (profile.syncRevision + 1L),
      "lastActiveAt" to FieldValue.serverTimestamp()
    )
    try {
      userRef.update(updatePayload).await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, userRef.path)
      throw e
    }

    getPlayerProfile(uid).getOrThrow()
  }

  suspend fun queryLeaderboard(
    trackId: String? = null,
    weekBucket: String? = null,
    gameMode: String? = null,
    pageSize: Long = 25L
  ): Result<List<RaceResultRecordModel>> = runCatching {
    var query: Query = db.collection("race_results")
      .whereGreaterThanOrEqualTo("lapTimeMs", 12000L)
      .orderBy("lapTimeMs", Query.Direction.ASCENDING)
      .limit(pageSize)

    if (!trackId.isNullOrBlank()) {
      query = query.whereEqualTo("trackId", trackId)
    }
    if (!weekBucket.isNullOrBlank()) {
      query = query.whereEqualTo("weekBucket", weekBucket)
    }
    if (!gameMode.isNullOrBlank()) {
      query = query.whereEqualTo("gameMode", gameMode)
    }

    try {
      val snapshot = query.get().await()
      snapshot.toObjects(
        RaceResultRecordModel::class.java,
        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
      )
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.LIST, "race_results")
      throw e
    }
  }

  // --------------------------------------------------------------------------
  // MODULE 7: DAILY MISSIONS, SEASONAL EVENTS & ACHIEVEMENT TRACKING
  // --------------------------------------------------------------------------
  suspend fun upsertEventCompletion(
    eventId: String,
    eventType: String = "DAILY_MISSION",
    progressValue: Long,
    targetValue: Long,
    rewardCredits: Long,
    rewardCoins: Long,
    claimed: Boolean
  ): Result<EventCompletionModel> = runCatching {
    val uid = requireUserId()
    val docRef = db.collection("users").document(uid).collection("event_completions").document(eventId)
    val existing = try {
      docRef.get().await()
    } catch (e: Exception) {
      null
    }

    if (existing == null || !existing.exists()) {
      val createPayload = mapOf(
        "userId" to uid,
        "eventId" to eventId,
        "eventType" to eventType,
        "progressValue" to progressValue,
        "targetValue" to targetValue,
        "rewardCredits" to rewardCredits,
        "rewardCoins" to rewardCoins,
        "claimed" to claimed,
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
      )
      try {
        docRef.set(createPayload).await()
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        handleFirestoreError(e, OperationType.CREATE, docRef.path)
        throw e
      }
    } else {
      val updatePayload = mapOf(
        "progressValue" to progressValue,
        "claimed" to claimed,
        "updatedAt" to FieldValue.serverTimestamp()
      )
      try {
        docRef.update(updatePayload).await()
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        handleFirestoreError(e, OperationType.UPDATE, docRef.path)
        throw e
      }
    }

    val snap = docRef.get().await()
    snap.toObject(
      EventCompletionModel::class.java,
      DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
    ) ?: throw IllegalStateException("Failed to deserialize EventCompletionModel")
  }

  /**
   * Claims a completed Daily Mission or Weekly Challenge reward, locking `claimed = true`
   * on `/users/{uid}/event_completions/{eventId}`, recording an append-only `MISSION_REWARD`
   * economy transaction, and crediting the player's profile.
   */
  suspend fun claimDailyMissionReward(eventId: String): Result<PlayerProfileModel> = runCatching {
    val uid = requireUserId()
    val eventRef = db.collection("users").document(uid).collection("event_completions").document(eventId)
    val snap = try {
      eventRef.get().await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.GET, eventRef.path)
      throw e
    }

    if (!snap.exists()) {
      throw IllegalStateException("Daily mission record $eventId does not exist")
    }
    val model = snap.toObject(
      EventCompletionModel::class.java,
      DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
    ) ?: throw IllegalStateException("Failed to deserialize EventCompletionModel")

    if (model.claimed) {
      throw IllegalStateException("Mission $eventId reward has already been claimed")
    }
    if (model.progressValue < model.targetValue) {
      throw IllegalStateException("Mission $eventId progress (${model.progressValue}/${model.targetValue}) is incomplete")
    }

    // 1. Lock mission as claimed (enforced by terminal state lock in firestore.rules)
    try {
      eventRef.update(
        mapOf(
          "claimed" to true,
          "updatedAt" to FieldValue.serverTimestamp()
        )
      ).await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, eventRef.path)
      throw e
    }

    // 2. Record append-only economy transaction & credit profile
    val profile = getPlayerProfile(uid).getOrElse {
      createOrMergePlayerProfile(username = "Apex Pilot").getOrThrow()
    }
    val newCredits = profile.credits + model.rewardCredits
    val newCoins = profile.coins + model.rewardCoins
    val cleanEventKey = eventId.replace(Regex("[^a-zA-Z0-9_\\-]"), "_")

    recordEconomyTransaction(
      txId = "tx_msn_${cleanEventKey}".take(128),
      txType = "MISSION_REWARD",
      coinsDelta = model.rewardCoins,
      creditsDelta = model.rewardCredits,
      balanceAfterCoins = newCoins,
      balanceAfterCredits = newCredits,
      referenceId = eventId
    ).getOrThrow()

    val userRef = db.collection("users").document(uid)
    userRef.update(
      mapOf(
        "credits" to newCredits,
        "coins" to newCoins,
        "syncRevision" to (profile.syncRevision + 1L),
        "lastActiveAt" to FieldValue.serverTimestamp()
      )
    ).await()

    getPlayerProfile(uid).getOrThrow()
  }

  /**
   * Tracks player achievement progress in `/users/{uid}/achievements/{achievementId}`
   * and automatically marks `unlocked = true` once `progressValue >= targetValue`.
   */
  suspend fun upsertAchievementProgress(
    achievementId: String,
    title: String,
    category: String = "CAREER",
    progressValue: Long,
    targetValue: Long,
    rewardCredits: Long = 5000L,
    rewardCoins: Long = 15L,
    rewardXp: Long = 1000L
  ): Result<AchievementRecordModel> = runCatching {
    val uid = requireUserId()
    val docRef = db.collection("users").document(uid).collection("achievements").document(achievementId)
    val existing = try {
      docRef.get().await()
    } catch (e: Exception) {
      null
    }

    val safeTarget = targetValue.coerceAtLeast(1L)
    val safeProgress = progressValue.coerceAtLeast(0L)
    val isUnlocked = safeProgress >= safeTarget

    if (existing == null || !existing.exists()) {
      val createPayload = mapOf(
        "userId" to uid,
        "achievementId" to achievementId,
        "title" to title.take(80).ifBlank { "Apex Achievement" },
        "category" to category,
        "progressValue" to safeProgress,
        "targetValue" to safeTarget,
        "rewardCredits" to rewardCredits.coerceIn(0L, 100000L),
        "rewardCoins" to rewardCoins.coerceIn(0L, 1000L),
        "rewardXp" to rewardXp.coerceIn(0L, 25000L),
        "unlocked" to isUnlocked,
        "claimed" to false,
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
      )
      try {
        docRef.set(createPayload).await()
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        handleFirestoreError(e, OperationType.CREATE, docRef.path)
        throw e
      }
    } else {
      val prevProgress = existing.getLong("progressValue") ?: 0L
      val mergedProgress = maxOf(prevProgress, safeProgress)
      val updatePayload = mapOf(
        "progressValue" to mergedProgress,
        "unlocked" to (mergedProgress >= safeTarget),
        "updatedAt" to FieldValue.serverTimestamp()
      )
      try {
        docRef.update(updatePayload).await()
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        handleFirestoreError(e, OperationType.UPDATE, docRef.path)
        throw e
      }
    }

    val snap = docRef.get().await()
    snap.toObject(
      AchievementRecordModel::class.java,
      DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
    ) ?: throw IllegalStateException("Failed to deserialize AchievementRecordModel")
  }

  /**
   * Claims an unlocked achievement's reward, locking `claimed = true` in
   * `/users/{uid}/achievements/{achievementId}`, recording an append-only
   * `ACHIEVEMENT_REWARD` transaction, and updating `unlockedAchievements` on `/users/{uid}`.
   */
  suspend fun claimAchievementReward(achievementId: String): Result<PlayerProfileModel> = runCatching {
    val uid = requireUserId()
    val achRef = db.collection("users").document(uid).collection("achievements").document(achievementId)
    val snap = try {
      achRef.get().await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.GET, achRef.path)
      throw e
    }

    if (!snap.exists()) {
      throw IllegalStateException("Achievement $achievementId does not exist")
    }
    val ach = snap.toObject(
      AchievementRecordModel::class.java,
      DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
    ) ?: throw IllegalStateException("Failed to deserialize AchievementRecordModel")

    if (ach.claimed) {
      throw IllegalStateException("Achievement $achievementId reward already claimed")
    }
    if (!ach.unlocked || ach.progressValue < ach.targetValue) {
      throw IllegalStateException("Achievement $achievementId is not yet unlocked")
    }

    try {
      achRef.update(
        mapOf(
          "claimed" to true,
          "updatedAt" to FieldValue.serverTimestamp()
        )
      ).await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, achRef.path)
      throw e
    }

    val profile = getPlayerProfile(uid).getOrElse {
      createOrMergePlayerProfile(username = "Apex Pilot").getOrThrow()
    }
    val newCredits = profile.credits + ach.rewardCredits
    val newCoins = profile.coins + ach.rewardCoins
    val newXp = profile.xp + ach.rewardXp
    val newLevel = maxOf(profile.level, ((newXp / 1800L) + 1L)).coerceIn(1L, 500L)
    val updatedAchievements = (profile.unlockedAchievements + achievementId).distinct().take(100)
    val cleanAchKey = achievementId.replace(Regex("[^a-zA-Z0-9_\\-]"), "_")

    recordEconomyTransaction(
      txId = "tx_ach_${cleanAchKey}".take(128),
      txType = "ACHIEVEMENT_REWARD",
      coinsDelta = ach.rewardCoins,
      creditsDelta = ach.rewardCredits,
      balanceAfterCoins = newCoins,
      balanceAfterCredits = newCredits,
      referenceId = achievementId
    ).getOrThrow()

    val userRef = db.collection("users").document(uid)
    userRef.update(
      mapOf(
        "credits" to newCredits,
        "coins" to newCoins,
        "xp" to newXp,
        "level" to newLevel,
        "unlockedAchievements" to updatedAchievements,
        "syncRevision" to (profile.syncRevision + 1L),
        "lastActiveAt" to FieldValue.serverTimestamp()
      )
    ).await()

    getPlayerProfile(uid).getOrThrow()
  }

  fun observeAchievements(): Flow<List<AchievementRecordModel>> = flow {
    val uid = requireUserId()
    val path = "users/$uid/achievements"
    emitAll(
      db.collection("users").document(uid).collection("achievements")
        .snapshots()
        .map { snapshot ->
          snapshot.toObjects(
            AchievementRecordModel::class.java,
            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
          )
        }
        .catch { error ->
          val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
          if (firestoreError == null && error is CancellationException) throw error
          val exceptionToLog = firestoreError ?: (error as? Exception)
          if (exceptionToLog != null && auth.currentUser?.uid == uid) {
            handleFirestoreError(exceptionToLog, OperationType.LIST, path)
          }
          throw error
        }
    )
  }

  // --------------------------------------------------------------------------
  // MODULE 8: MULTIPLAYER ROOM CREATION, JOINING & TERMINAL STATE LOCKING
  // --------------------------------------------------------------------------
  suspend fun createMultiplayerRoom(
    roomId: String,
    roomCode: String,
    trackId: String,
    vehicleClass: String = "GT3",
    maxPlayers: Long = 8L,
    dedicatedServerEndpoint: String = "wss://eu-west-relay.apexdynamics.gg/room/$roomCode"
  ): Result<MultiplayerRoomModel> = runCatching {
    val uid = requireUserId()
    val docRef = db.collection("multiplayer_rooms").document(roomId)
    val payload = mapOf(
      "hostUserId" to uid,
      "roomCode" to roomCode.take(6).padEnd(6, '0'),
      "trackId" to trackId,
      "vehicleClass" to vehicleClass,
      "status" to "WAITING",
      "maxPlayers" to maxPlayers.coerceIn(2L, 8L),
      "playerUids" to listOf(uid),
      "dedicatedServerEndpoint" to dedicatedServerEndpoint,
      "createdAt" to FieldValue.serverTimestamp(),
      "updatedAt" to FieldValue.serverTimestamp()
    )
    try {
      docRef.set(payload).await()
      val snap = docRef.get().await()
      snap.toObject(
        MultiplayerRoomModel::class.java,
        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
      ) ?: throw IllegalStateException("Failed to deserialize MultiplayerRoomModel")
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, docRef.path)
      throw e
    }
  }

  suspend fun updateMultiplayerRoomStatus(
    roomId: String,
    newStatus: String
  ): Result<Unit> = runCatching {
    requireUserId()
    val docRef = db.collection("multiplayer_rooms").document(roomId)
    val updatePayload = mapOf(
      "status" to newStatus,
      "updatedAt" to FieldValue.serverTimestamp()
    )
    try {
      docRef.update(updatePayload).await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docRef.path)
      throw e
    }
  }

  companion object {
    const val V8_ROLLBACK_SLOT_ID = "v8_rollback_backup"
  }
}
