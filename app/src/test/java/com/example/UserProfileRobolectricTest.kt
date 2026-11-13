package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.local.PerformanceMetrics
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class UserProfileRobolectricTest {

  private lateinit var db: AppDatabase
  private lateinit var context: Context

  @Before
  fun setUp() {
    context = ApplicationProvider.getApplicationContext()
    db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
      .allowMainThreadQueries()
      .build()
  }

  @After
  fun tearDown() {
    db.close()
  }

  @Test
  fun `room database aggregates total distance average speed and personal records`() = runBlocking {
    val dao = db.performanceMetricsDao()

    val sample1 = PerformanceMetrics(
      raceId = "run_1",
      driverName = "Apex Pilot",
      trackName = "Monza GP",
      vehicleName = "Apex GT3-R Twin-Turbo",
      lapTimeMs = 78000L,
      topSpeedKmh = 320.0f,
      peakAccelerationG = 2.10f,
      zeroToHundredKmhSeconds = 2.80f,
      maxLateralG = 2.50f,
      trackDistanceMeters = 5793,
      avgSpeedKmh = 267.3f
    )

    val sample2 = PerformanceMetrics(
      raceId = "run_2",
      driverName = "Apex Pilot",
      trackName = "Spa-Francorchamps",
      vehicleName = "Formula Apex Hybrid",
      lapTimeMs = 104000L,
      topSpeedKmh = 345.5f,
      peakAccelerationG = 2.40f,
      zeroToHundredKmhSeconds = 2.20f,
      maxLateralG = 3.90f,
      trackDistanceMeters = 7004,
      avgSpeedKmh = 242.4f
    )

    dao.insertAll(listOf(sample1, sample2))

    // Verify session count
    val count = dao.getTotalSessionsCount().first()
    assertEquals(2, count)

    // Verify total distance (5793 + 7004 = 12797 meters)
    val totalDistanceM = dao.getTotalDistanceMeters().first()
    assertNotNull(totalDistanceM)
    assertEquals(12797L, totalDistanceM)

    val totalKm = (totalDistanceM ?: 0L) / 1000f
    assertEquals(12.797f, totalKm, 0.001f)
    val totalMiles = totalKm * 0.621371f
    assertEquals(7.9517f, totalMiles, 0.01f)

    // Verify average speed query
    val avgSpeed = dao.getAverageSpeedKmh().first()
    assertNotNull(avgSpeed)
    val expectedAvg = (267.3f + 242.4f) / 2f
    assertEquals(expectedAvg, avgSpeed ?: 0f, 0.1f)

    // Verify personal records
    val bestLap = dao.getBestLapTimeMs().first()
    assertEquals(78000L, bestLap)

    val maxTopSpeed = dao.getMaxTopSpeedKmh().first()
    assertEquals(345.5f, maxTopSpeed ?: 0f, 0.01f)

    val best0to100 = dao.getBestZeroToHundred().first()
    assertEquals(2.20f, best0to100 ?: 0f, 0.01f)

    val maxLatG = dao.getMaxLateralG().first()
    assertEquals(3.90f, maxLatG ?: 0f, 0.01f)
  }

  @Test
  fun `track personal bests correctly identified per circuit`() = runBlocking {
    val dao = db.performanceMetricsDao()

    val monzaLap1 = PerformanceMetrics(
      raceId = "m1",
      trackName = "Monza GP",
      vehicleName = "Apex GT3-R",
      lapTimeMs = 80000L,
      topSpeedKmh = 310f,
      trackDistanceMeters = 5793,
      avgSpeedKmh = 260f
    )
    val monzaLap2 = PerformanceMetrics(
      raceId = "m2",
      trackName = "Monza GP",
      vehicleName = "Formula Apex",
      lapTimeMs = 74500L, // Better lap
      topSpeedKmh = 335f,
      trackDistanceMeters = 5793,
      avgSpeedKmh = 280f
    )

    dao.insertAll(listOf(monzaLap1, monzaLap2))

    val allRuns = dao.getAllList()
    val monzaRuns = allRuns.filter { it.trackName == "Monza GP" }
    val personalBestMonza = monzaRuns.minByOrNull { it.lapTimeMs }

    assertNotNull(personalBestMonza)
    assertEquals(74500L, personalBestMonza?.lapTimeMs)
    assertEquals("Formula Apex", personalBestMonza?.vehicleName)
    assertEquals(335f, personalBestMonza?.topSpeedKmh ?: 0f, 0.01f)
    assertEquals(5793 * 2, monzaRuns.sumOf { it.getEffectiveDistanceMeters() })
  }

  @Test
  fun `driver handle and profile preferences persist correctly`() {
    val prefs = context.getSharedPreferences("apex_driver_profile", Context.MODE_PRIVATE)

    // Save driver profile
    prefs.edit()
      .putString("driver_handle", "DriftKing_Shibuya")
      .putString("driver_bio", "King of the midnight expressway causeways")
      .putString("license_tier", "S-CLASS ESPORTS PRO")
      .putStringSet("favorite_tracks", setOf("track_neo_tokyo", "track_marina_bay"))
      .commit()

    assertEquals("DriftKing_Shibuya", prefs.getString("driver_handle", null))
    assertEquals("King of the midnight expressway causeways", prefs.getString("driver_bio", null))
    val favs = prefs.getStringSet("favorite_tracks", null)
    assertNotNull(favs)
    assertTrue(favs?.contains("track_neo_tokyo") == true)
    assertTrue(favs?.contains("track_marina_bay") == true)
  }

  @Test
  fun `city mode tracks are configured with street features and available in track list`() {
    val cityTracks = com.example.model.AVAILABLE_TRACKS.filter { it.isCityMode }
    assertTrue("At least 3 city mode tracks should be present", cityTracks.size >= 3)

    val neoTokyo = cityTracks.find { it.id == "track_neo_tokyo" }
    assertNotNull("Neo Tokyo Night City must exist", neoTokyo)
    assertTrue(neoTokyo?.cityFeatures?.contains("Neon") == true)

    val marinaBay = cityTracks.find { it.id == "track_marina_bay" }
    assertNotNull("Marina Bay Night GP must exist", marinaBay)

    val miami = cityTracks.find { it.id == "track_miami_highway" }
    assertNotNull("Miami Ocean Causeway must exist", miami)
  }

  @Test
  fun `modern photo cars and classic golden age cars are present in vehicle catalog`() {
    val vehicles = com.example.model.AVAILABLE_VEHICLES
    assertTrue("Catalog must contain multiple vehicles", vehicles.size >= 8)

    val hyperion = vehicles.find { it.id == "car_hyperion7_nitro" }
    assertNotNull("Hyperion-7 Hypercar (#42 Volt) from Neo Tokyo photo must exist", hyperion)
    assertEquals("#42", hyperion?.liveryNumber)

    val gulfGt3 = vehicles.find { it.id == "car_gt3_apex" }
    assertNotNull("Gulf McLaren 720S #55 from night grid photo must exist", gulfGt3)
    assertEquals("#55", gulfGt3?.liveryNumber)

    val scuderia = vehicles.find { it.id == "car_scuderia_corsa" }
    assertNotNull("Scuderia Corsa #51 from night grid photo must exist", scuderia)

    val manthey = vehicles.find { it.id == "car_manthey_911_gt3" }
    assertNotNull("Manthey Racing 911 GT3 R #91 from night GP photo must exist", manthey)

    val boss = vehicles.find { it.id == "car_classic_boss_69" }
    assertNotNull("Classic 1969 Boss Mach 1 must exist", boss)
    assertTrue("Boss Mach 1 must be marked classic", boss?.isClassic == true)

    val carrera = vehicles.find { it.id == "car_classic_rs_73" }
    assertNotNull("Classic 1973 RS 2.7 Carrera must exist", carrera)
    assertTrue("Carrera RS must be marked classic", carrera?.isClassic == true)
  }
}
