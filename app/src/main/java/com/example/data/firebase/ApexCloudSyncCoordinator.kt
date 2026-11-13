package com.example.data.firebase

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import com.example.game.v2.ApexRivals2CareerManager
import com.example.game.v2.ApexRivals2SettingsRepository
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.functions.FirebaseFunctionsException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

enum class CloudSyncStatus {
  GUEST,
  SYNCING,
  SYNCED,
  OFFLINE_QUEUED,
  ERROR
}

data class ApexCloudSyncUiState(
  val status: CloudSyncStatus = CloudSyncStatus.GUEST,
  val isAuthenticated: Boolean = false,
  val uid: String? = null,
  val displayName: String? = null,
  val email: String? = null,
  val photoUrl: String? = null,
  val authProviderLabel: String = "Guest Local Cache",
  val syncRevision: Long = 0L,
  val pendingOpsCount: Int = 0,
  val lastSyncTimestampMs: Long = 0L,
  val statusMessage: String = "Sign in with Google to enable Cloud Save & Verified Rewards",
  val simulatedOffline: Boolean = false
)

enum class PendingSyncOpType {
  MIGRATE_V8,
  SUBMIT_RACE,
  CLAIM_REWARD,
  PURCHASE_VEHICLE,
  UPGRADE_SUBSYSTEM,
  SYNC_CLOUD_SAVE,
  UPDATE_METADATA
}

/**
 * Offline-resilient Cloud Synchronization Coordinator for Apex Rivals 2.0.
 *
 * Bridges Firebase Auth (CredentialManager Google Sign-In), Firestore V8->V9 non-destructive
 * migration, server-authoritative Cloud Functions (rewards, purchases, upgrades, competitive
 * race submissions), and an offline-persistent FIFO pending operation queue.
 */
object ApexCloudSyncCoordinator {
  private const val TAG = "ApexCloudSync"
  private const val QUEUE_PREFS_NAME = "apex_rivals_cloud_sync_queue"
  private const val KEY_QUEUE_JSON = "pending_sync_ops_json"
  private const val KEY_SYNC_REVISION = "last_cloud_sync_revision"
  private const val KEY_LAST_SYNC_MS = "last_cloud_sync_ts_ms"
  private const val MAX_QUEUE_SIZE = 100

  private val coordinatorScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
  private val syncMutex = Mutex()

  private val _uiState = MutableStateFlow(ApexCloudSyncUiState())
  val uiState: StateFlow<ApexCloudSyncUiState> = _uiState.asStateFlow()

  @Volatile
  private var authListenerRegistered = false

  @Volatile
  private var authStateListener: FirebaseAuth.AuthStateListener? = null

  fun startObservingAuth(
    context: Context,
    scope: CoroutineScope = coordinatorScope
  ) {
    val appCtx = context.applicationContext
    loadQueueMetadata(appCtx)

    if (!isFirebaseInitialized(appCtx)) {
      _uiState.value = _uiState.value.copy(
        status = if (_uiState.value.pendingOpsCount > 0) CloudSyncStatus.OFFLINE_QUEUED else CloudSyncStatus.GUEST,
        isAuthenticated = false,
        authProviderLabel = "Local Offline Cache",
        statusMessage = "Local progression active (${_uiState.value.pendingOpsCount} queued)"
      )
      return
    }

    val auth = try {
      FirebaseAuth.getInstance()
    } catch (_: Exception) {
      return
    }

    updateAuthFields(auth)

    if (!authListenerRegistered) {
      synchronized(this) {
        if (!authListenerRegistered) {
          val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            updateAuthFields(firebaseAuth)
            val user = firebaseAuth.currentUser
            if (user != null) {
              scope.launch(Dispatchers.IO) {
                syncOnSignIn(appCtx)
              }
            } else {
              loadQueueMetadata(appCtx)
              _uiState.value = _uiState.value.copy(
                status = if (_uiState.value.pendingOpsCount > 0) CloudSyncStatus.OFFLINE_QUEUED else CloudSyncStatus.GUEST,
                isAuthenticated = false,
                uid = null,
                displayName = null,
                email = null,
                photoUrl = null,
                authProviderLabel = "Guest Local Cache",
                statusMessage = "Signed out — local cache preserved"
              )
            }
          }
          authStateListener = listener
          auth.addAuthStateListener(listener)
          authListenerRegistered = true
        }
      }
    }

    if (auth.currentUser != null) {
      scope.launch(Dispatchers.IO) {
        syncOnSignIn(appCtx)
      }
    }
  }

  private fun updateAuthFields(auth: FirebaseAuth) {
    val user = auth.currentUser
    if (user != null) {
      val name = user.displayName?.takeIf { it.isNotBlank() }
        ?: user.email?.substringBefore('@')
        ?: "Apex Pilot"
      _uiState.value = _uiState.value.copy(
        isAuthenticated = true,
        uid = user.uid,
        displayName = name,
        email = user.email,
        photoUrl = user.photoUrl?.toString(),
        authProviderLabel = "Google Account Linked"
      )
    }
  }

  private fun isFirebaseInitialized(context: Context): Boolean {
    return try {
      FirebaseApp.getApps(context).isNotEmpty()
    } catch (_: Exception) {
      false
    }
  }

  fun isNetworkAvailable(context: Context): Boolean {
    if (_uiState.value.simulatedOffline) return false
    return try {
      val cm = context.applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        ?: return true
      val activeNetwork = cm.activeNetwork ?: return false
      val caps = cm.getNetworkCapabilities(activeNetwork) ?: return false
      caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    } catch (_: Exception) {
      true
    }
  }

  fun setSimulatedOffline(
    context: Context,
    offline: Boolean,
    scope: CoroutineScope = coordinatorScope
  ) {
    val appCtx = context.applicationContext
    _uiState.value = _uiState.value.copy(
      simulatedOffline = offline,
      status = when {
        offline -> CloudSyncStatus.OFFLINE_QUEUED
        !_uiState.value.isAuthenticated -> CloudSyncStatus.GUEST
        _uiState.value.pendingOpsCount > 0 -> CloudSyncStatus.SYNCING
        else -> CloudSyncStatus.SYNCED
      },
      statusMessage = if (offline) {
        "Offline simulation enabled — queuing operations locally"
      } else {
        "Back online — flushing pending cloud queue..."
      }
    )
    if (!offline) {
      retryPendingSync(appCtx, scope)
    }
  }

  suspend fun syncOnSignIn(
    context: Context,
    repoOverride: ApexBackendRepository? = null
  ): Boolean {
    val appCtx = context.applicationContext
    val auth = if (repoOverride == null && isFirebaseInitialized(appCtx)) {
      try {
        FirebaseAuth.getInstance()
      } catch (_: Exception) {
        null
      }
    } else null

    val currentUser = auth?.currentUser
    if (repoOverride == null && currentUser == null) {
      return false
    }

    if (!isNetworkAvailable(appCtx)) {
      loadQueueMetadata(appCtx)
      _uiState.value = _uiState.value.copy(
        status = CloudSyncStatus.OFFLINE_QUEUED,
        statusMessage = "Offline — cached locally (${_uiState.value.pendingOpsCount} pending)"
      )
      return false
    }

    return syncMutex.withLock {
      _uiState.value = _uiState.value.copy(
        status = CloudSyncStatus.SYNCING,
        statusMessage = "Synchronizing Apex Cloud Profile..."
      )
      try {
        val repo = repoOverride ?: ApexBackendRepository(appCtx)
        val localState = ApexRivals2CareerManager.state.value
        val displayName = currentUser?.displayName?.takeIf { it.isNotBlank() }
          ?: localState.playerUsername.ifBlank { "Apex Pilot" }

        // 1. Fetch existing profile OR perform non-destructive V8->V9 migration / starter creation
        val existingProfileResult = repo.getPlayerProfile()
        val profile: PlayerProfileModel = if (existingProfileResult.isSuccess) {
          val existing = existingProfileResult.getOrThrow()
          if (!existing.v8LegacyMigrated && (localState.totalXp > existing.xp || localState.creditsBalance > existing.credits)) {
            repo.migrateV8LegacyCloudProfileIfNeeded(
              v8Username = displayName,
              v8Credits = localState.creditsBalance.toLong(),
              v8Xp = localState.totalXp.toLong(),
              v8OwnedVehicles = localState.ownedVehicleIds.toList(),
              v8UnlockedTracks = localState.unlockedTrackIds.toList(),
              loadoutsJson = ApexRivals2CareerManager.exportLoadoutsJson(),
              settingsJson = buildSettingsJson(),
              careerProgressJson = ApexRivals2CareerManager.exportCareerProgressJson()
            ).getOrElse { existing }
          } else {
            existing
          }
        } else {
          val hasLegacyProgress = localState.totalXp > 0 ||
            localState.totalRacesCompleted > 0 ||
            localState.creditsBalance != 48500 ||
            localState.ownedVehicleIds.size > 6
          if (hasLegacyProgress) {
            repo.migrateV8LegacyCloudProfileIfNeeded(
              v8Username = displayName,
              v8Credits = localState.creditsBalance.toLong(),
              v8Xp = localState.totalXp.toLong(),
              v8OwnedVehicles = localState.ownedVehicleIds.toList(),
              v8UnlockedTracks = localState.unlockedTrackIds.toList(),
              loadoutsJson = ApexRivals2CareerManager.exportLoadoutsJson(),
              settingsJson = buildSettingsJson(),
              careerProgressJson = ApexRivals2CareerManager.exportCareerProgressJson()
            ).getOrThrow()
          } else {
            repo.createStarterPlayerProfile(
              username = displayName,
              avatarUrl = currentUser?.photoUrl?.toString() ?: "avatar_helmet_red"
            ).getOrThrow()
          }
        }

        // 2. Fetch primary Cloud Save snapshot
        val cloudSave = repo.getPrimaryCloudSave("primary").getOrNull()

        // 3. Apply authoritative cloud profile & loadouts non-destructively into local state
        ApexRivals2CareerManager.applyAuthoritativeCloudProfile(
          profile = profile,
          cloudSave = cloudSave,
          context = appCtx
        )

        val highestRev = maxOf(
          loadLocalRevision(appCtx),
          profile.syncRevision,
          cloudSave?.syncRevision ?: 0L
        )
        saveSyncMetadata(appCtx, highestRev, System.currentTimeMillis())

        // 4. Seed initial primary Cloud Save if none exists on server yet
        if (cloudSave == null) {
          val initSaveRev = highestRev + 1L
          repo.syncCloudSave(
            saveSlotId = "primary",
            localSyncRevision = initSaveRev,
            loadoutsJson = ApexRivals2CareerManager.exportLoadoutsJson(),
            settingsJson = buildSettingsJson(),
            careerProgressJson = ApexRivals2CareerManager.exportCareerProgressJson()
          ).onSuccess { saved ->
            saveSyncMetadata(appCtx, saved.syncRevision, System.currentTimeMillis())
          }
        }

        // 5. Flush any pending offline queue items FIFO
        val remaining = flushQueueLocked(appCtx, repo)
        val nowRev = loadLocalRevision(appCtx)
        val nowTs = System.currentTimeMillis()
        saveSyncMetadata(appCtx, nowRev, nowTs)

        _uiState.value = _uiState.value.copy(
          status = if (remaining > 0) CloudSyncStatus.OFFLINE_QUEUED else CloudSyncStatus.SYNCED,
          isAuthenticated = true,
          syncRevision = nowRev,
          pendingOpsCount = remaining,
          lastSyncTimestampMs = nowTs,
          statusMessage = if (remaining > 0) {
            "Synced profile • $remaining offline operations waiting"
          } else {
            "Cloud Save & Garage synchronized (Rev #$nowRev)"
          }
        )
        true
      } catch (e: Exception) {
        Log.w(TAG, "syncOnSignIn failed: ${e.message}", e)
        loadQueueMetadata(appCtx)
        _uiState.value = _uiState.value.copy(
          status = if (isTransientError(e)) CloudSyncStatus.OFFLINE_QUEUED else CloudSyncStatus.ERROR,
          statusMessage = e.localizedMessage ?: "Cloud sync unavailable — local cache active"
        )
        false
      }
    }
  }

  // --------------------------------------------------------------------------
  // EVENT & MUTATION TRIGGERS (NON-BLOCKING + OFFLINE-QUEUED)
  // --------------------------------------------------------------------------
  fun onRaceCompleted(
    context: Context,
    raceId: String,
    driverName: String,
    trackId: String,
    trackName: String,
    vehicleId: String,
    vehicleName: String,
    gameMode: String,
    finishingPosition: Int,
    raceDurationMs: Long,
    lapTimeMs: Long,
    topSpeedKmh: Float
  ) {
    val appCtx = context.applicationContext
    val mappedMode = when (gameMode.uppercase(Locale.US)) {
      "GRAND_PRIX", "CAREER" -> "CAREER"
      "TIME_ATTACK", "TIME_TRIAL" -> "TIME_TRIAL"
      "DRIFT_CHALLENGE", "DRIFT" -> "DRIFT"
      "CHAMPIONSHIP" -> "CHAMPIONSHIP"
      "MULTIPLAYER" -> "MULTIPLAYER"
      else -> "QUICK_RACE"
    }
    val payload = JSONObject().apply {
      put("raceId", raceId)
      put("driverName", driverName)
      put("trackId", trackId)
      put("trackName", trackName)
      put("vehicleId", vehicleId)
      put("vehicleName", vehicleName)
      put("gameMode", mappedMode)
      put("finishingPosition", finishingPosition.toLong())
      put("raceDurationMs", raceDurationMs)
      put("lapTimeMs", lapTimeMs)
      put("topSpeedKmh", topSpeedKmh.toDouble())
      put("weekBucket", currentIsoWeekBucket())
    }
    enqueueAndTryFlush(appCtx, PendingSyncOpType.SUBMIT_RACE, "race_$raceId", payload)
  }

  fun onRewardClaimed(
    context: Context,
    referenceId: String,
    idempotencyKey: String = "rwd_${referenceId}_${UUID.randomUUID().toString().take(8)}"
  ) {
    val appCtx = context.applicationContext
    val payload = JSONObject().apply {
      put("referenceId", referenceId)
      put("idempotencyKey", idempotencyKey)
    }
    enqueueAndTryFlush(appCtx, PendingSyncOpType.CLAIM_REWARD, idempotencyKey, payload)
  }

  fun onVehiclePurchased(
    context: Context,
    vehicleId: String
  ) {
    val appCtx = context.applicationContext
    val key = "tx_veh_${vehicleId.replace(Regex("[^a-zA-Z0-9_\\-]"), "_")}".take(128)
    val payload = JSONObject().apply {
      put("vehicleId", vehicleId)
      put("idempotencyKey", key)
    }
    enqueueAndTryFlush(appCtx, PendingSyncOpType.PURCHASE_VEHICLE, key, payload)
  }

  fun onSubsystemUpgraded(
    context: Context,
    vehicleId: String,
    subsystem: String,
    targetStage: Int
  ) {
    val appCtx = context.applicationContext
    val cleanVeh = vehicleId.replace(Regex("[^a-zA-Z0-9_\\-]"), "_")
    val cleanSub = subsystem.uppercase(Locale.US).replace(Regex("[^A-Z0-9_]"), "_")
    val key = "tx_upg_${cleanVeh}_${cleanSub}_lv${targetStage}".take(128)
    val payload = JSONObject().apply {
      put("vehicleId", vehicleId)
      put("subsystem", cleanSub)
      put("targetStage", targetStage)
      put("idempotencyKey", key)
    }
    enqueueAndTryFlush(appCtx, PendingSyncOpType.UPGRADE_SUBSYSTEM, key, payload)
  }

  fun onCustomizationSaved(context: Context) {
    val appCtx = context.applicationContext
    val payload = JSONObject().apply {
      put("loadoutsJson", ApexRivals2CareerManager.exportLoadoutsJson())
      put("settingsJson", buildSettingsJson())
      put("careerProgressJson", ApexRivals2CareerManager.exportCareerProgressJson())
    }
    enqueueAndTryFlush(appCtx, PendingSyncOpType.SYNC_CLOUD_SAVE, "sync_cloud_save_primary", payload)
  }

  fun onProfileMetadataUpdated(
    context: Context,
    username: String,
    avatarUrl: String? = null
  ) {
    val appCtx = context.applicationContext
    val payload = JSONObject().apply {
      put("username", username)
      if (!avatarUrl.isNullOrBlank()) put("avatarUrl", avatarUrl)
    }
    enqueueAndTryFlush(appCtx, PendingSyncOpType.UPDATE_METADATA, "meta_profile_update", payload)
  }

  fun retryPendingSync(
    context: Context,
    scope: CoroutineScope = coordinatorScope
  ) {
    val appCtx = context.applicationContext
    scope.launch(Dispatchers.IO) {
      flushPendingQueue(appCtx)
    }
  }

  fun recoverFromV8Backup(
    context: Context,
    scope: CoroutineScope = coordinatorScope,
    onResult: (Boolean, String) -> Unit = { _, _ -> }
  ) {
    val appCtx = context.applicationContext
    scope.launch(Dispatchers.IO) {
      val localRestored = ApexRivals2CareerManager.restoreFromV8BackupSnapshot(appCtx)
      if (isFirebaseInitialized(appCtx) &&
        FirebaseAuth.getInstance().currentUser != null &&
        isNetworkAvailable(appCtx)
      ) {
        val repo = ApexBackendRepository(appCtx)
        val cloudRes = repo.rollbackToV8BackupSnapshotCloud()
        if (cloudRes.isSuccess) {
          val data = cloudRes.getOrThrow()
          val credits = (data["credits"] as? Number)?.toInt()
          val coins = (data["coins"] as? Number)?.toInt()
          val xp = (data["xp"] as? Number)?.toInt()
          val loadoutsJson = data["loadoutsJson"] as? String
          val syncRev = (data["syncRevision"] as? Number)?.toLong() ?: (loadLocalRevision(appCtx) + 1L)
          ApexRivals2CareerManager.applyAuthoritativeBalances(
            credits = credits,
            coins = coins,
            xp = xp,
            loadoutsJson = loadoutsJson,
            context = appCtx
          )
          saveSyncMetadata(appCtx, syncRev, System.currentTimeMillis())
          _uiState.value = _uiState.value.copy(
            status = CloudSyncStatus.SYNCED,
            syncRevision = syncRev,
            lastSyncTimestampMs = System.currentTimeMillis(),
            statusMessage = "Restored Cloud & Local V8 Backup Snapshot (Rev #$syncRev)"
          )
          withContext(Dispatchers.Main) {
            onResult(true, "Restored V8 Cloud + Local Backup Snapshot!")
          }
          return@launch
        }
      }

      val msg = if (localRestored) {
        "Restored local V8 Backup Snapshot (Cloud rollback requires active sign-in & network)"
      } else {
        "No V8 backup snapshot available yet"
      }
      withContext(Dispatchers.Main) {
        onResult(localRestored, msg)
      }
    }
  }

  // --------------------------------------------------------------------------
  // PERSISTENT FIFO QUEUE IMPLEMENTATION (TESTABLE)
  // --------------------------------------------------------------------------
  private fun enqueueAndTryFlush(
    appCtx: Context,
    opType: PendingSyncOpType,
    idempotencyKey: String,
    payload: JSONObject
  ) {
    enqueueOperation(appCtx, opType, idempotencyKey, payload)
    coordinatorScope.launch(Dispatchers.IO) {
      flushPendingQueue(appCtx)
    }
  }

  fun enqueueOperation(
    context: Context,
    opType: PendingSyncOpType,
    idempotencyKey: String,
    payload: JSONObject
  ): Int {
    val appCtx = context.applicationContext
    val queue = readQueue(appCtx)
    val filtered = JSONArray()
    for (i in 0 until queue.length()) {
      val item = queue.optJSONObject(i) ?: continue
      // Replace duplicate idempotencyKey / coalesce latest cloud save or metadata update
      if (item.optString("idempotencyKey") == idempotencyKey) continue
      filtered.put(item)
    }
    while (filtered.length() >= MAX_QUEUE_SIZE) {
      val trimmed = JSONArray()
      for (i in 1 until filtered.length()) {
        filtered.optJSONObject(i)?.let { trimmed.put(it) }
      }
      writeQueue(appCtx, trimmed)
      break
    }

    val entry = JSONObject().apply {
      put("opType", opType.name)
      put("idempotencyKey", idempotencyKey)
      put("payload", payload)
      put("createdAtMs", System.currentTimeMillis())
    }
    filtered.put(entry)
    writeQueue(appCtx, filtered)

    val count = filtered.length()
    _uiState.value = _uiState.value.copy(
      pendingOpsCount = count,
      status = when {
        !isNetworkAvailable(appCtx) -> CloudSyncStatus.OFFLINE_QUEUED
        !_uiState.value.isAuthenticated -> CloudSyncStatus.GUEST
        else -> _uiState.value.status
      }
    )
    return count
  }

  suspend fun flushPendingQueue(
    context: Context,
    repoOverride: ApexBackendRepository? = null
  ): Int {
    val appCtx = context.applicationContext
    if (!isNetworkAvailable(appCtx)) {
      loadQueueMetadata(appCtx)
      _uiState.value = _uiState.value.copy(
        status = CloudSyncStatus.OFFLINE_QUEUED,
        statusMessage = "Offline — ${_uiState.value.pendingOpsCount} operations queued for retry"
      )
      return _uiState.value.pendingOpsCount
    }

    if (repoOverride == null) {
      if (!isFirebaseInitialized(appCtx)) {
        loadQueueMetadata(appCtx)
        return _uiState.value.pendingOpsCount
      }
      val authUser = try {
        FirebaseAuth.getInstance().currentUser
      } catch (_: Exception) {
        null
      }
      if (authUser == null) {
        loadQueueMetadata(appCtx)
        _uiState.value = _uiState.value.copy(
          status = CloudSyncStatus.GUEST,
          statusMessage = if (_uiState.value.pendingOpsCount > 0) {
            "Sign in with Google to sync ${_uiState.value.pendingOpsCount} pending items"
          } else {
            _uiState.value.statusMessage
          }
        )
        return _uiState.value.pendingOpsCount
      }
    }

    return syncMutex.withLock {
      val repo = repoOverride ?: ApexBackendRepository(appCtx)
      val remaining = flushQueueLocked(appCtx, repo)
      val nowTs = System.currentTimeMillis()
      val rev = loadLocalRevision(appCtx)
      if (remaining == 0) {
        saveSyncMetadata(appCtx, rev, nowTs)
      }
      _uiState.value = _uiState.value.copy(
        status = if (remaining > 0) CloudSyncStatus.OFFLINE_QUEUED else CloudSyncStatus.SYNCED,
        pendingOpsCount = remaining,
        syncRevision = rev,
        lastSyncTimestampMs = if (remaining == 0) nowTs else _uiState.value.lastSyncTimestampMs,
        statusMessage = if (remaining > 0) {
          "Paused sync — $remaining queued ops waiting for connection"
        } else {
          "All changes synced to Apex Cloud (Rev #$rev)"
        }
      )
      remaining
    }
  }

  private suspend fun flushQueueLocked(
    appCtx: Context,
    repo: ApexBackendRepository
  ): Int {
    val queue = readQueue(appCtx)
    if (queue.length() == 0) return 0

    var processedCount = 0
    for (i in 0 until queue.length()) {
      val item = queue.optJSONObject(i)
      if (item == null) {
        processedCount++
        continue
      }
      val opName = item.optString("opType")
      val opType = PendingSyncOpType.entries.find { it.name == opName }
      if (opType == null) {
        processedCount++
        continue
      }
      val payload = item.optJSONObject("payload") ?: JSONObject()

      val outcome = runCatching {
        executeSingleOp(appCtx, repo, opType, payload)
      }

      if (outcome.isSuccess) {
        processedCount++
      } else {
        val err = outcome.exceptionOrNull()
        if (err != null && isTransientError(err)) {
          Log.w(TAG, "Transient sync failure on $opType, halting FIFO flush: ${err.message}")
          break
        } else {
          Log.w(TAG, "Non-retryable/already-applied op $opType skipped: ${err?.message}")
          processedCount++
        }
      }
    }

    if (processedCount > 0) {
      val remainingArray = JSONArray()
      for (i in processedCount until queue.length()) {
        queue.optJSONObject(i)?.let { remainingArray.put(it) }
      }
      writeQueue(appCtx, remainingArray)
      return remainingArray.length()
    }
    return queue.length()
  }

  private suspend fun executeSingleOp(
    appCtx: Context,
    repo: ApexBackendRepository,
    opType: PendingSyncOpType,
    payload: JSONObject
  ) {
    when (opType) {
      PendingSyncOpType.MIGRATE_V8 -> {
        val s = ApexRivals2CareerManager.state.value
        val migrated = repo.migrateV8LegacyCloudProfileIfNeeded(
          v8Username = s.playerUsername,
          v8Credits = s.creditsBalance.toLong(),
          v8Xp = s.totalXp.toLong(),
          v8OwnedVehicles = s.ownedVehicleIds.toList(),
          v8UnlockedTracks = s.unlockedTrackIds.toList(),
          loadoutsJson = ApexRivals2CareerManager.exportLoadoutsJson(),
          settingsJson = buildSettingsJson(),
          careerProgressJson = ApexRivals2CareerManager.exportCareerProgressJson()
        ).getOrThrow()
        ApexRivals2CareerManager.applyAuthoritativeCloudProfile(migrated, null, appCtx)
      }

      PendingSyncOpType.SUBMIT_RACE -> {
        val data = repo.submitCompetitiveRaceResultCloud(
          raceId = payload.optString("raceId"),
          driverName = payload.optString("driverName", "Apex Pilot"),
          trackId = payload.optString("trackId", "track_neo_tokyo"),
          trackName = payload.optString("trackName", "Neo Tokyo"),
          vehicleId = payload.optString("vehicleId", "car_gt3_apex"),
          vehicleName = payload.optString("vehicleName", "Apex GT3"),
          gameMode = payload.optString("gameMode", "CAREER"),
          finishingPosition = payload.optLong("finishingPosition", 1L),
          raceDurationMs = payload.optLong("raceDurationMs", 65000L),
          lapTimeMs = payload.optLong("lapTimeMs", 60000L),
          topSpeedKmh = payload.optDouble("topSpeedKmh", 260.0),
          weekBucket = payload.optString("weekBucket", currentIsoWeekBucket())
        ).getOrThrow()

        val credits = (data["balanceAfterCredits"] as? Number)?.toInt()
        val xp = (data["xp"] as? Number)?.toInt()
        val level = (data["level"] as? Number)?.toInt()
        if (credits != null || xp != null || level != null) {
          ApexRivals2CareerManager.applyAuthoritativeBalances(
            credits = credits,
            xp = xp,
            level = level,
            context = appCtx
          )
        }
      }

      PendingSyncOpType.CLAIM_REWARD -> {
        val data = repo.claimVerifiedRewardCloud(
          idempotencyKey = payload.optString("idempotencyKey"),
          referenceId = payload.optString("referenceId")
        ).getOrThrow()

        ApexRivals2CareerManager.applyAuthoritativeBalances(
          credits = (data["balanceAfterCredits"] as? Number)?.toInt(),
          coins = (data["balanceAfterCoins"] as? Number)?.toInt(),
          xp = (data["xp"] as? Number)?.toInt(),
          level = (data["level"] as? Number)?.toInt(),
          dailyStreak = (data["dailyLoginStreak"] as? Number)?.toInt(),
          context = appCtx
        )
      }

      PendingSyncOpType.PURCHASE_VEHICLE -> {
        val data = repo.purchaseVehicleOrUpgradeCloud(
          action = "PURCHASE_VEHICLE",
          idempotencyKey = payload.optString("idempotencyKey"),
          vehicleId = payload.optString("vehicleId")
        ).getOrThrow()

        @Suppress("UNCHECKED_CAST")
        val vehicles = (data["ownedVehicleIds"] as? List<*>)?.mapNotNull { it as? String }
        ApexRivals2CareerManager.applyAuthoritativeBalances(
          credits = (data["balanceAfterCredits"] as? Number)?.toInt(),
          coins = (data["balanceAfterCoins"] as? Number)?.toInt(),
          ownedVehicles = vehicles,
          context = appCtx
        )
      }

      PendingSyncOpType.UPGRADE_SUBSYSTEM -> {
        val data = repo.purchaseVehicleOrUpgradeCloud(
          action = "UPGRADE_SUBSYSTEM",
          idempotencyKey = payload.optString("idempotencyKey"),
          vehicleId = payload.optString("vehicleId"),
          subsystem = payload.optString("subsystem"),
          targetStage = payload.optInt("targetStage", 2)
        ).getOrThrow()

        ApexRivals2CareerManager.applyAuthoritativeBalances(
          credits = (data["balanceAfterCredits"] as? Number)?.toInt(),
          coins = (data["balanceAfterCoins"] as? Number)?.toInt(),
          xp = (data["xp"] as? Number)?.toInt(),
          level = (data["level"] as? Number)?.toInt(),
          context = appCtx
        )
        // Also sync updated loadout JSON to cloud save primary slot
        val currentRev = maxOf(
          loadLocalRevision(appCtx),
          repo.getPrimaryCloudSave("primary").getOrNull()?.syncRevision ?: 0L
        ) + 1L
        repo.syncCloudSave(
          saveSlotId = "primary",
          localSyncRevision = currentRev,
          loadoutsJson = ApexRivals2CareerManager.exportLoadoutsJson(),
          settingsJson = buildSettingsJson(),
          careerProgressJson = ApexRivals2CareerManager.exportCareerProgressJson()
        ).onSuccess { saved ->
          saveSyncMetadata(appCtx, saved.syncRevision, System.currentTimeMillis())
        }
      }

      PendingSyncOpType.SYNC_CLOUD_SAVE -> {
        val remoteRev = repo.getPrimaryCloudSave("primary").getOrNull()?.syncRevision ?: 0L
        val nextRev = maxOf(loadLocalRevision(appCtx), remoteRev) + 1L
        val saved = repo.syncCloudSave(
          saveSlotId = "primary",
          localSyncRevision = nextRev,
          loadoutsJson = payload.optString("loadoutsJson", ApexRivals2CareerManager.exportLoadoutsJson()),
          settingsJson = payload.optString("settingsJson", buildSettingsJson()),
          careerProgressJson = payload.optString("careerProgressJson", ApexRivals2CareerManager.exportCareerProgressJson())
        ).getOrThrow()
        saveSyncMetadata(appCtx, saved.syncRevision, System.currentTimeMillis())
      }

      PendingSyncOpType.UPDATE_METADATA -> {
        val updated = repo.updateProfileMetadata(
          username = payload.optString("username", "Apex Pilot"),
          avatarUrl = payload.optString("avatarUrl").takeIf { it.isNotBlank() }
        ).getOrThrow()
        ApexRivals2CareerManager.applyAuthoritativeCloudProfile(updated, null, appCtx)
      }
    }
  }

  private fun isTransientError(error: Throwable): Boolean {
    val chain = generateSequence(error) { it.cause }.toList()
    if (chain.any { it is FirebaseNetworkException || it is IOException || it is TimeoutCancellationException }) {
      return true
    }
    val firestoreCode = chain.filterIsInstance<FirebaseFirestoreException>().firstOrNull()?.code
    if (firestoreCode == FirebaseFirestoreException.Code.UNAVAILABLE ||
      firestoreCode == FirebaseFirestoreException.Code.DEADLINE_EXCEEDED ||
      firestoreCode == FirebaseFirestoreException.Code.ABORTED
    ) {
      return true
    }
    val functionsCode = chain.filterIsInstance<FirebaseFunctionsException>().firstOrNull()?.code
    if (functionsCode == FirebaseFunctionsException.Code.UNAVAILABLE ||
      functionsCode == FirebaseFunctionsException.Code.DEADLINE_EXCEEDED ||
      functionsCode == FirebaseFunctionsException.Code.ABORTED
    ) {
      return true
    }
    return false
  }

  private fun readQueue(context: Context): JSONArray {
    return try {
      val prefs = context.getSharedPreferences(QUEUE_PREFS_NAME, Context.MODE_PRIVATE)
      val raw = prefs.getString(KEY_QUEUE_JSON, "[]") ?: "[]"
      JSONArray(raw)
    } catch (_: Exception) {
      JSONArray()
    }
  }

  private fun writeQueue(context: Context, queue: JSONArray) {
    try {
      context.getSharedPreferences(QUEUE_PREFS_NAME, Context.MODE_PRIVATE)
        .edit()
        .putString(KEY_QUEUE_JSON, queue.toString())
        .apply()
    } catch (_: Exception) {
    }
  }

  fun getPendingQueueSize(context: Context): Int = readQueue(context.applicationContext).length()

  fun clearQueueForTesting(context: Context) {
    writeQueue(context.applicationContext, JSONArray())
    _uiState.value = ApexCloudSyncUiState()
  }

  private fun loadQueueMetadata(context: Context) {
    val prefs = context.getSharedPreferences(QUEUE_PREFS_NAME, Context.MODE_PRIVATE)
    val count = readQueue(context).length()
    val rev = prefs.getLong(KEY_SYNC_REVISION, 0L)
    val ts = prefs.getLong(KEY_LAST_SYNC_MS, 0L)
    _uiState.value = _uiState.value.copy(
      pendingOpsCount = count,
      syncRevision = rev,
      lastSyncTimestampMs = ts
    )
  }

  private fun loadLocalRevision(context: Context): Long {
    return context.getSharedPreferences(QUEUE_PREFS_NAME, Context.MODE_PRIVATE)
      .getLong(KEY_SYNC_REVISION, 0L)
  }

  private fun saveSyncMetadata(context: Context, rev: Long, tsMs: Long) {
    context.getSharedPreferences(QUEUE_PREFS_NAME, Context.MODE_PRIVATE)
      .edit()
      .putLong(KEY_SYNC_REVISION, rev)
      .putLong(KEY_LAST_SYNC_MS, tsMs)
      .apply()
  }

  private fun buildSettingsJson(): String {
    return JSONObject().apply {
      put("graphicsPreset", ApexRivals2SettingsRepository.graphicsPreset.value.name)
      put("frameRatePref", ApexRivals2SettingsRepository.frameRatePref.value.fps)
      put("controlMode", ApexRivals2SettingsRepository.controlConfig.value.controlMode.name)
      put("language", ApexRivals2SettingsRepository.languageOption.value.code)
      put("musicVolume", ApexRivals2SettingsRepository.audioMixer.value.musicVolume.toDouble())
      put("engineVolume", ApexRivals2SettingsRepository.audioMixer.value.engineVolume.toDouble())
      put("sfxVolume", ApexRivals2SettingsRepository.audioMixer.value.sfxVolume.toDouble())
      put("isMuted", ApexRivals2SettingsRepository.audioMixer.value.isMuted)
    }.toString()
  }

  private fun currentIsoWeekBucket(): String {
    return try {
      SimpleDateFormat("yyyy-'W'ww", Locale.US).format(Date())
    } catch (_: Exception) {
      "2026-W46"
    }
  }
}
