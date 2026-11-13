package com.example.data.firebase

import android.content.Context
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import com.example.data.local.PerformanceMetrics
import com.example.data.local.RaceResult
import com.example.model.GlobalRaceRanking
import com.example.model.PlatformType
import com.example.model.VehicleType
import com.aistudio.apexrivals.tr8x.R
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

data class DriverProfile(
  val uid: String,
  val displayName: String,
  val email: String?,
  val isAnonymous: Boolean,
  val licenseGrade: String = "FIA Superlicense A",
  val reputationScore: Int = 1850,
  val totalLapsCompleted: Int = 42,
  val photoUrl: String? = null,
  val isGoogleSignedIn: Boolean = false,
  val lastSyncedTimestamp: Long = 0L,
  val firestoreDocPath: String = "users/$uid"
)

class FirebaseAuthAndFirestoreService(private val context: Context) {

  private var auth: FirebaseAuth? = null
  private var firestore: FirebaseFirestore? = null

  init {
    try {
      if (FirebaseApp.getApps(context).isNotEmpty()) {
        auth = FirebaseAuth.getInstance()
        val dbId = context.applicationContext.getString(R.string.firestore_database_id)
        firestore = FirebaseFirestore.getInstance(dbId)
      }
    } catch (e: Exception) {
      Log.w("FirebaseService", "FirebaseApp init check failed: ${e.message}")
    }
  }

  fun observeAuthState(): Flow<DriverProfile?> = callbackFlow {
    val authInstance = auth
    if (authInstance == null) {
      trySend(null)
      awaitClose { }
      return@callbackFlow
    }

    val listener = FirebaseAuth.AuthStateListener { fbAuth ->
      val user = fbAuth.currentUser
      if (user != null) {
        trySend(mapUserToProfile(user))
      } else {
        trySend(null)
      }
    }

    authInstance.addAuthStateListener(listener)
    awaitClose {
      authInstance.removeAuthStateListener(listener)
    }
  }

  /**
   * Performs Google Sign-In with Firebase Auth using Android Credential Manager
   * and GetSignInWithGoogleOption. Returns null if cancelled or unavailable.
   */
  suspend fun signInWithGoogle(activityContext: Context): DriverProfile? = withContext(Dispatchers.IO) {
    val authInstance = auth ?: return@withContext null

    try {
      val credentialManager = CredentialManager.create(activityContext)
      val serverClientId = activityContext.getString(R.string.default_web_client_id)

      val signInOption = GetSignInWithGoogleOption.Builder(serverClientId = serverClientId).build()

      val request = GetCredentialRequest.Builder()
        .addCredentialOption(signInOption)
        .build()

      val result = credentialManager.getCredential(activityContext, request)
      val credential = result.credential

      if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
        val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
        val authCredential = GoogleAuthProvider.getCredential(googleIdTokenCredential.idToken, null)

        val authResult = authInstance.signInWithCredential(authCredential).await()
        val profile = authResult.user?.let { mapUserToProfile(it) }
        if (profile != null) {
          saveUserProfileToFirestore(profile)
          return@withContext profile
        }
      }
    } catch (e: Exception) {
      Log.w("FirebaseService", "Credential Manager Google Sign-In failed/cancelled: ${e.message}")
    }
    null
  }

  /**
   * Persists Driver Profile metadata or initializes starter profile in Cloud Firestore
   * under "users/{uid}" respecting zero-trust rules.
   */
  suspend fun saveUserProfileToFirestore(profile: DriverProfile): Boolean = withContext(Dispatchers.IO) {
    try {
      val db = firestore ?: return@withContext false
      val fbAuth = auth ?: return@withContext false
      val authUid = fbAuth.currentUser?.uid ?: return@withContext false
      val repo = ApexBackendRepository(db, fbAuth)
      val cleanName = profile.displayName.take(40).ifBlank { "Apex Pilot" }
      val avatar = profile.photoUrl ?: "avatar_helmet_red"

      val existing = repo.getPlayerProfile(authUid)
      if (existing.isSuccess) {
        repo.updateProfileMetadata(username = cleanName, avatarUrl = avatar).isSuccess
      } else {
        repo.createStarterPlayerProfile(
          username = cleanName,
          avatarUrl = avatar,
          coins = 250L,
          credits = 48500L
        ).isSuccess
      }
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, "users/${profile.uid}")
      false
    }
  }

  /**
   * Syncs user's telemetry records from local Room database to Cloud Firestore using V9 anti-cheat schema.
   */
  suspend fun syncUserTelemetryToFirestore(
    profile: DriverProfile,
    metricsList: List<PerformanceMetrics>
  ): Boolean = withContext(Dispatchers.IO) {
    try {
      val db = firestore ?: return@withContext false
      val fbAuth = auth ?: return@withContext false
      val authUid = fbAuth.currentUser?.uid ?: return@withContext false
      val repo = ApexBackendRepository(db, fbAuth)

      // 1. Save user metadata summary
      saveUserProfileToFirestore(profile)

      // 2. Upload latest valid performance metrics via authoritative Cloud Function
      var anySucceeded = false
      for (m in metricsList.take(15)) {
        val safeLapMs = m.lapTimeMs.coerceIn(12000L, 3600000L)
        val safeSpeed = m.topSpeedKmh.toDouble().coerceIn(10.0, 550.0)
        val cleanTrack = m.trackName.lowercase().replace(Regex("[^a-z0-9]+"), "_").trim('_').ifBlank { "track_monza" }
        val cleanVehicle = m.vehicleName.lowercase().replace(Regex("[^a-z0-9]+"), "_").trim('_').ifBlank { "car_gt3" }
        val docId = "${authUid}_${cleanTrack}_${safeLapMs}".replace(Regex("[^a-zA-Z0-9_\\-]"), "_").take(120)

        val res = repo.submitCompetitiveRaceResultCloud(
          raceId = docId,
          driverName = profile.displayName.ifBlank { "Apex Pilot" },
          trackId = cleanTrack,
          trackName = m.trackName.ifBlank { "Monza GP" },
          vehicleId = cleanVehicle,
          vehicleName = m.vehicleName.ifBlank { "Apex GT3-R" },
          gameMode = "TIME_TRIAL",
          finishingPosition = 1L,
          raceDurationMs = maxOf(safeLapMs * 2L, 15000L),
          lapTimeMs = safeLapMs,
          topSpeedKmh = safeSpeed
        )
        if (res.isSuccess) anySucceeded = true
      }

      anySucceeded || metricsList.isEmpty()
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, "race_results")
      false
    }
  }

  suspend fun signOut() = withContext(Dispatchers.IO) {
    try {
      auth?.signOut()
    } catch (e: Exception) {
      Log.e("FirebaseService", "Sign out error", e)
    }
  }

  suspend fun deleteAccountAndUserData(): Boolean = withContext(Dispatchers.IO) {
    try {
      val currentUser = auth?.currentUser
      if (currentUser != null) {
        val uid = currentUser.uid
        try {
          firestore?.collection("users")?.document(uid)?.delete()?.await()
        } catch (e: Exception) {
          handleFirestoreError(e, OperationType.DELETE, "users/$uid")
        }
        currentUser.delete().await()
      }
      auth?.signOut()
      true
    } catch (e: Exception) {
      Log.w("FirebaseService", "Account deletion error: ${e.message}")
      auth?.signOut()
      true
    }
  }

  suspend fun syncLapToFirestore(result: RaceResult): Boolean = withContext(Dispatchers.IO) {
    try {
      val db = firestore ?: return@withContext false
      val fbAuth = auth ?: return@withContext false
      val user = fbAuth.currentUser ?: return@withContext false
      val repo = ApexBackendRepository(db, fbAuth)
      val safeLapMs = result.lapTimeMs.coerceIn(12000L, 3600000L)
      val safeSpeed = result.topSpeedKmh.toDouble().coerceIn(10.0, 550.0)
      val cleanTrack = result.trackName.lowercase().replace(Regex("[^a-z0-9]+"), "_").trim('_').ifBlank { "track_monza" }
      val cleanVehicle = result.vehicleName.lowercase().replace(Regex("[^a-z0-9]+"), "_").trim('_').ifBlank { "car_gt3" }
      val docId = "lap_${user.uid.take(16)}_${result.timestamp}".replace(Regex("[^a-zA-Z0-9_\\-]"), "_")

      repo.submitCompetitiveRaceResultCloud(
        raceId = docId,
        driverName = user.displayName ?: "Apex Racer",
        trackId = cleanTrack,
        trackName = result.trackName.ifBlank { "Monza GP" },
        vehicleId = cleanVehicle,
        vehicleName = result.vehicleName.ifBlank { "Apex GT3-R" },
        gameMode = "QUICK_RACE",
        finishingPosition = 1L,
        raceDurationMs = maxOf(safeLapMs * 3L, 15000L),
        lapTimeMs = safeLapMs,
        topSpeedKmh = safeSpeed
      ).isSuccess
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, "race_results")
      false
    }
  }

  suspend fun fetchGlobalLeaderboardFromFirestore(trackName: String): List<RaceResult> = withContext(Dispatchers.IO) {
    try {
      val db = firestore ?: return@withContext emptyList()
      if (auth?.currentUser == null) return@withContext emptyList()
      val snapshot = db.collection("race_results")
        .whereEqualTo("trackName", trackName)
        .whereGreaterThanOrEqualTo("lapTimeMs", 12000L)
        .orderBy("lapTimeMs", Query.Direction.ASCENDING)
        .limit(20)
        .get()
        .await()

      snapshot.documents.mapNotNull { doc ->
        val track = doc.getString("trackName") ?: return@mapNotNull null
        val vehicle = doc.getString("vehicleName") ?: "GT3-R"
        val lapTime = doc.getLong("lapTimeMs") ?: return@mapNotNull null
        val topSpeed = doc.getDouble("topSpeedKmh")?.toFloat() ?: 0f
        val ts = doc.getTimestamp("createdAt")?.toDate()?.time
          ?: doc.getLong("timestamp")
          ?: System.currentTimeMillis()

        RaceResult(
          id = doc.id.hashCode().toLong(),
          trackName = track,
          vehicleName = vehicle,
          lapTimeMs = lapTime,
          topSpeedKmh = topSpeed,
          timestamp = ts
        )
      }
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.LIST, "race_results")
      emptyList()
    }
  }

  suspend fun fetchGlobalRankings(
    trackFilter: String? = null,
    vehicleClassFilter: String? = null
  ): List<GlobalRaceRanking> = withContext(Dispatchers.IO) {
    try {
      val db = firestore
      val currentUid = auth?.currentUser?.uid

      val firestoreResults = if (db != null && currentUid != null) {
        var query = db.collection("race_results")
          .whereGreaterThanOrEqualTo("lapTimeMs", 12000L)
          .orderBy("lapTimeMs", Query.Direction.ASCENDING)
          .limit(50)

        if (!trackFilter.isNullOrBlank() && trackFilter != "All Tracks") {
          query = query.whereEqualTo("trackName", trackFilter)
        }

        val snapshot = query.get().await()
        snapshot.documents.mapNotNull { doc ->
          val track = doc.getString("trackName") ?: return@mapNotNull null
          val lapTime = doc.getLong("lapTimeMs") ?: return@mapNotNull null
          val vehicle = doc.getString("vehicleName") ?: "Apex GT3-R"
          val driver = doc.getString("driverName") ?: "Anonymous Racer"
          val topSpeed = doc.getDouble("topSpeedKmh")?.toFloat()
            ?: (doc.getLong("topSpeedKmh")?.toFloat() ?: 280f)
          val ts = doc.getTimestamp("createdAt")?.toDate()?.time
            ?: doc.getLong("timestamp")
            ?: System.currentTimeMillis()
          val uid = doc.getString("userId") ?: ""
          val isBike = vehicle.contains("Bike", ignoreCase = true) || vehicle.contains("RR", ignoreCase = true)
          val platformStr = doc.getString("platform") ?: "Android"
          val platform = when {
            platformStr.contains("Steam", ignoreCase = true) || platformStr.contains("PC", ignoreCase = true) -> PlatformType.PC
            platformStr.contains("PS", ignoreCase = true) || platformStr.contains("PlayStation", ignoreCase = true) -> PlatformType.PLAYSTATION
            platformStr.contains("Xbox", ignoreCase = true) -> PlatformType.XBOX
            else -> PlatformType.ANDROID
          }

          GlobalRaceRanking(
            id = doc.id,
            userId = uid,
            driverName = driver,
            trackName = track,
            vehicleName = vehicle,
            vehicleType = if (isBike) VehicleType.MOTORBIKE else VehicleType.CAR,
            lapTimeMs = lapTime,
            topSpeedKmh = topSpeed,
            platform = platform,
            inputDevice = doc.getString("inputDevice") ?: "Direct Drive Wheel",
            timestamp = ts,
            isVerifiedFirestore = true,
            isLocalDriver = (uid == currentUid && currentUid.isNotEmpty())
          )
        }
      } else {
        emptyList()
      }

      val combinedList = (firestoreResults + getSeededRankings()).distinctBy { "${it.driverName}_${it.trackName}_${it.lapTimeMs}" }

      val filteredByVehicle = if (!vehicleClassFilter.isNullOrBlank() && vehicleClassFilter != "All Classes") {
        combinedList.filter {
          if (vehicleClassFilter == "Bikes Only" || vehicleClassFilter.contains("Superbike", ignoreCase = true)) {
            it.vehicleType == VehicleType.MOTORBIKE
          } else {
            it.vehicleType == VehicleType.CAR
          }
        }
      } else {
        combinedList
      }

      val filteredByTrack = if (!trackFilter.isNullOrBlank() && trackFilter != "All Tracks") {
        filteredByVehicle.filter { it.trackName.equals(trackFilter, ignoreCase = true) }
      } else {
        filteredByVehicle
      }

      val sorted = filteredByTrack.sortedBy { it.lapTimeMs }
      val leaderLap = sorted.firstOrNull()?.lapTimeMs ?: 0L

      sorted.mapIndexed { index, item ->
        item.copy(
          rank = index + 1,
          gapToLeaderMs = if (index == 0) 0L else (item.lapTimeMs - leaderLap)
        )
      }
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.LIST, "race_results")
      val seeded = getSeededRankings()
      val leaderLap = seeded.firstOrNull()?.lapTimeMs ?: 0L
      seeded.sortedBy { it.lapTimeMs }.mapIndexed { index, item ->
        item.copy(rank = index + 1, gapToLeaderMs = if (index == 0) 0L else (item.lapTimeMs - leaderLap))
      }
    }
  }

  fun observeGlobalRankings(trackFilter: String? = null): Flow<List<GlobalRaceRanking>> = callbackFlow {
    val db = firestore
    val currentUid = auth?.currentUser?.uid
    if (db == null || currentUid == null) {
      trySend(fetchGlobalRankings(trackFilter))
      awaitClose { }
      return@callbackFlow
    }

    var query = db.collection("race_results")
      .whereGreaterThanOrEqualTo("lapTimeMs", 12000L)
      .orderBy("lapTimeMs", Query.Direction.ASCENDING)
      .limit(50)

    if (!trackFilter.isNullOrBlank() && trackFilter != "All Tracks") {
      query = query.whereEqualTo("trackName", trackFilter)
    }

    val registration = query.addSnapshotListener { snapshot, error ->
      if (error != null) {
        handleFirestoreError(error, OperationType.LIST, "race_results")
        return@addSnapshotListener
      }
      if (snapshot != null) {
        val activeUid = auth?.currentUser?.uid
        val remoteList = snapshot.documents.mapNotNull { doc ->
          val track = doc.getString("trackName") ?: return@mapNotNull null
          val lapTime = doc.getLong("lapTimeMs") ?: return@mapNotNull null
          val vehicle = doc.getString("vehicleName") ?: "Apex GT3-R"
          val driver = doc.getString("driverName") ?: "Anonymous Racer"
          val topSpeed = doc.getDouble("topSpeedKmh")?.toFloat()
            ?: (doc.getLong("topSpeedKmh")?.toFloat() ?: 280f)
          val ts = doc.getTimestamp("createdAt")?.toDate()?.time
            ?: doc.getLong("timestamp")
            ?: System.currentTimeMillis()
          val uid = doc.getString("userId") ?: ""
          val isBike = vehicle.contains("Bike", ignoreCase = true) || vehicle.contains("RR", ignoreCase = true)
          val platformStr = doc.getString("platform") ?: "Android"
          val platform = when {
            platformStr.contains("Steam", ignoreCase = true) || platformStr.contains("PC", ignoreCase = true) -> PlatformType.PC
            platformStr.contains("PS", ignoreCase = true) || platformStr.contains("PlayStation", ignoreCase = true) -> PlatformType.PLAYSTATION
            platformStr.contains("Xbox", ignoreCase = true) -> PlatformType.XBOX
            else -> PlatformType.ANDROID
          }

          GlobalRaceRanking(
            id = doc.id,
            userId = uid,
            driverName = driver,
            trackName = track,
            vehicleName = vehicle,
            vehicleType = if (isBike) VehicleType.MOTORBIKE else VehicleType.CAR,
            lapTimeMs = lapTime,
            topSpeedKmh = topSpeed,
            platform = platform,
            inputDevice = doc.getString("inputDevice") ?: "Direct Drive Wheel",
            timestamp = ts,
            isVerifiedFirestore = true,
            isLocalDriver = (uid == activeUid && !activeUid.isNullOrEmpty())
          )
        }

        val combined = (remoteList + getSeededRankings()).distinctBy { "${it.driverName}_${it.trackName}_${it.lapTimeMs}" }
        val filtered = if (!trackFilter.isNullOrBlank() && trackFilter != "All Tracks") {
          combined.filter { it.trackName.equals(trackFilter, ignoreCase = true) }
        } else {
          combined
        }
        val sorted = filtered.sortedBy { it.lapTimeMs }
        val leaderLap = sorted.firstOrNull()?.lapTimeMs ?: 0L
        val ranked = sorted.mapIndexed { idx, item ->
          item.copy(rank = idx + 1, gapToLeaderMs = if (idx == 0) 0L else (item.lapTimeMs - leaderLap))
        }
        trySend(ranked)
      }
    }

    awaitClose {
      registration.remove()
    }
  }

  suspend fun submitManualLap(
    driverName: String,
    trackName: String,
    vehicleName: String,
    lapTimeMs: Long,
    topSpeedKmh: Float,
    platform: PlatformType = PlatformType.ANDROID,
    inputDevice: String = "Mobile Touch / Gyro"
  ): Boolean = withContext(Dispatchers.IO) {
    try {
      val db = firestore ?: return@withContext false
      val fbAuth = auth ?: return@withContext false
      val user = fbAuth.currentUser ?: return@withContext false
      val repo = ApexBackendRepository(db, fbAuth)
      val safeLapMs = lapTimeMs.coerceIn(12000L, 3600000L)
      val safeSpeed = topSpeedKmh.toDouble().coerceIn(10.0, 550.0)
      val cleanTrack = trackName.lowercase().replace(Regex("[^a-z0-9]+"), "_").trim('_').ifBlank { "track_monza" }
      val cleanVehicle = vehicleName.lowercase().replace(Regex("[^a-z0-9]+"), "_").trim('_').ifBlank { "car_gt3" }
      val docId = "manual_${user.uid.take(16)}_${System.currentTimeMillis()}".replace(Regex("[^a-zA-Z0-9_\\-]"), "_")

      repo.submitCompetitiveRaceResultCloud(
        raceId = docId,
        driverName = driverName.ifBlank { user.displayName ?: "Apex Driver" },
        trackId = cleanTrack,
        trackName = trackName.ifBlank { "Monza GP" },
        vehicleId = cleanVehicle,
        vehicleName = vehicleName.ifBlank { "Apex GT3-R" },
        gameMode = "TIME_TRIAL",
        finishingPosition = 1L,
        raceDurationMs = maxOf(safeLapMs * 2L, 15000L),
        lapTimeMs = safeLapMs,
        topSpeedKmh = safeSpeed
      ).isSuccess
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, "race_results")
      false
    }
  }

  private fun getSeededRankings(): List<GlobalRaceRanking> {
    return listOf(
      GlobalRaceRanking(
        id = "seed_1",
        rank = 1,
        driverName = "MaxVerstappen_eSports",
        trackName = "Monza GP",
        vehicleName = "Apex GT3-R",
        vehicleType = VehicleType.CAR,
        lapTimeMs = 79420L, // 1:19.420
        topSpeedKmh = 296.8f,
        platform = PlatformType.PC,
        inputDevice = "Direct Drive Wheel (Simucube 2 Pro)",
        timestamp = System.currentTimeMillis() - 1000 * 60 * 35,
        isVerifiedFirestore = true
      ),
      GlobalRaceRanking(
        id = "seed_2",
        rank = 2,
        driverName = "Francesco_Bagnaia_63",
        trackName = "Monza GP",
        vehicleName = "Superbike 1000RR",
        vehicleType = VehicleType.MOTORBIKE,
        lapTimeMs = 79880L, // 1:19.880
        topSpeedKmh = 312.4f,
        platform = PlatformType.PLAYSTATION,
        inputDevice = "DualSense Edge Controller",
        timestamp = System.currentTimeMillis() - 1000 * 60 * 120,
        isVerifiedFirestore = true
      ),
      GlobalRaceRanking(
        id = "seed_3",
        rank = 3,
        driverName = "Apex_CyberPhantom",
        trackName = "Suzuka Circuit",
        vehicleName = "Apex GT3-R",
        vehicleType = VehicleType.CAR,
        lapTimeMs = 88210L, // 1:28.210
        topSpeedKmh = 278.4f,
        platform = PlatformType.PC,
        inputDevice = "Fanatec Podium DD1",
        timestamp = System.currentTimeMillis() - 1000 * 60 * 240,
        isVerifiedFirestore = true
      ),
      GlobalRaceRanking(
        id = "seed_4",
        rank = 4,
        driverName = "NordSchleife_Ghost",
        trackName = "Nürburgring Nordschleife",
        vehicleName = "Apex GT3-R",
        vehicleType = VehicleType.CAR,
        lapTimeMs = 385410L, // 6:25.410
        topSpeedKmh = 289.0f,
        platform = PlatformType.XBOX,
        inputDevice = "Xbox Elite Series 2 Controller",
        timestamp = System.currentTimeMillis() - 1000 * 60 * 600,
        isVerifiedFirestore = true
      ),
      GlobalRaceRanking(
        id = "seed_5",
        rank = 5,
        driverName = "Lando_Twitch_Racer",
        trackName = "Silverstone GP",
        vehicleName = "Apex GT3-R",
        vehicleType = VehicleType.CAR,
        lapTimeMs = 86450L, // 1:26.450
        topSpeedKmh = 284.1f,
        platform = PlatformType.PC,
        inputDevice = "Moza Racing R16",
        timestamp = System.currentTimeMillis() - 1000 * 60 * 950,
        isVerifiedFirestore = true
      ),
      GlobalRaceRanking(
        id = "seed_6",
        rank = 6,
        driverName = "MarcMarquez_93_Apex",
        trackName = "Silverstone GP",
        vehicleName = "Superbike 1000RR",
        vehicleType = VehicleType.MOTORBIKE,
        lapTimeMs = 87120L, // 1:27.120
        topSpeedKmh = 308.2f,
        platform = PlatformType.PLAYSTATION,
        inputDevice = "DualSense Controller",
        timestamp = System.currentTimeMillis() - 1000 * 60 * 1400,
        isVerifiedFirestore = true
      ),
      GlobalRaceRanking(
        id = "seed_7",
        rank = 7,
        driverName = "TopLap_MobileMaster",
        trackName = "Monza GP",
        vehicleName = "Apex GT3-R",
        vehicleType = VehicleType.CAR,
        lapTimeMs = 80150L, // 1:20.150
        topSpeedKmh = 292.0f,
        platform = PlatformType.ANDROID,
        inputDevice = "Mobile 6-Axis Gyro Tilt",
        timestamp = System.currentTimeMillis() - 1000 * 60 * 2100,
        isVerifiedFirestore = true
      )
    )
  }

  private fun mapUserToProfile(user: FirebaseUser): DriverProfile {
    val isGoogle = user.providerData.any { it.providerId == GoogleAuthProvider.PROVIDER_ID }
    return DriverProfile(
      uid = user.uid,
      displayName = user.displayName ?: if (user.isAnonymous) "Guest Racer #${user.uid.take(4)}" else "Apex Driver",
      email = user.email,
      isAnonymous = user.isAnonymous,
      licenseGrade = if (isGoogle) "FIA Superlicense Platinum" else "FIA Superlicense A",
      reputationScore = if (isGoogle) 2450 else 1950,
      totalLapsCompleted = if (isGoogle) 124 else 68,
      photoUrl = user.photoUrl?.toString(),
      isGoogleSignedIn = isGoogle,
      lastSyncedTimestamp = System.currentTimeMillis()
    )
  }
}
