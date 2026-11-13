package com.example.game.model

import androidx.compose.ui.graphics.Color
import com.example.model.Track
import com.example.model.Vehicle
import com.example.model.VehicleType

enum class GameMode(val displayName: String, val description: String) {
  GRAND_PRIX("Grand Prix Race", "8-Car grid start, live positions, drafting & checkered flag!"),
  TIME_ATTACK("Time Attack & Ghost", "Qualifying sprint to beat lap records & ghost delta."),
  SPEED_TRAP("Speed Trap Sprint", "Blitz radar checkpoints on straightaways for max speed."),
  DRIFT_CHALLENGE("Apex Drift & Precision", "Slide through apexes and rack up drift score multipliers.")
}

enum class RaceState {
  PRE_GRID,
  LIGHTS_COUNTDOWN,
  RACING,
  FINAL_LAP,
  FINISHED_PODIUM,
  PAUSED
}

enum class CameraPerspective(val label: String) {
  CHASE_CAM("Chase Cam"),
  COCKPIT_CAM("Cockpit Cam"),
  HOOD_CAM("Hood Cam")
}

enum class DrivingAssistLevel(val label: String) {
  ROOKIE("Rookie (Auto-Gears, ABS, TCS)"),
  PRO("Pro (Manual Gears, ABS, Low TCS)"),
  SIMULATION("Simulation (Pure Raw Physics)")
}

enum class RaceStage(
  val stageNumber: Int,
  val stageName: String,
  val subtitle: String,
  val playerCount: Int,
  val defaultTrackId: String,
  val difficulty: String,
  val xpReward: Int,
  val creditsReward: Int,
  val description: String
) {
  STAGE_1_STREET_MEET(
    1,
    "Street Meet & Sprint",
    "4-Player City Meetup & Midnight Sprint",
    4,
    "track_miami_highway",
    "Street Amateur",
    650,
    2000,
    "Race through coastal palm boulevards and highway causeways against 3 underground street competitors."
  ),
  STAGE_2_DOWNTOWN_GP(
    2,
    "Neon Downtown GP",
    "8-Player High-Rise Neon Grid",
    8,
    "track_neo_tokyo",
    "Pro-Am Street",
    1400,
    4500,
    "Battle 7 rivals through illuminated skyscrapers, rain-slicked tarmac, and underground tunnels in Neo Tokyo."
  ),
  STAGE_3_HIGHWAY_OUTRUN(
    3,
    "Highway Velocity Outrun",
    "12-Player Expressway Battle",
    12,
    "track_tokyo",
    "Master Drifter",
    2500,
    7500,
    "Slipstream drafting at 330+ km/h across multi-level bridges and viaducts with 11 fierce rivals."
  ),
  STAGE_4_MARINA_BAY_FINALE(
    4,
    "Marina Bay Grand Finale",
    "16-Player Pro Championship Finale",
    16,
    "track_marina_bay",
    "Apex Legend",
    4500,
    15000,
    "Full 16-car grid championship under waterfront stadium floodlights with glowing brake rotors and hypercars."
  )
}

data class AirRival(
  val id: String,
  val name: String,
  val teamName: String,
  val carName: String,
  val colorHex: Long,
  var laneOffset: Float, // -0.8f (left) to +0.8f (right)
  var distanceMeters: Float,
  var speedKmh: Float,
  var currentLap: Int = 1,
  var currentRank: Int = 2,
  var isOvertaken: Boolean = false
) {
  val composeColor: Color get() = Color(colorHex)
}

enum class RaceWeather(
  val displayName: String,
  val icon: String,
  val gripMultiplier: Float,
  val topSpeedPenaltyKmh: Float,
  val skyTopColor: Long,
  val skyMidColor: Long,
  val skyBottomColor: Long,
  val horizonColor: Long
) {
  CLEAR_SUNSET(
    displayName = "Golden Sunset",
    icon = "☀️",
    gripMultiplier = 1.0f,
    topSpeedPenaltyKmh = 0f,
    skyTopColor = 0xFF210E2E,
    skyMidColor = 0xFF581C43,
    skyBottomColor = 0xFF9E3839,
    horizonColor = 0xFFE07A5F
  ),
  NEON_NIGHT(
    displayName = "Tokyo Midnight",
    icon = "🌙",
    gripMultiplier = 0.98f,
    topSpeedPenaltyKmh = 0f,
    skyTopColor = 0xFF04060E,
    skyMidColor = 0xFF0C142A,
    skyBottomColor = 0xFF162547,
    horizonColor = 0xFF1E355B
  ),
  OVERCAST_COOL(
    displayName = "Overcast GP",
    icon = "⛅",
    gripMultiplier = 1.02f,
    topSpeedPenaltyKmh = 0f,
    skyTopColor = 0xFF1A222D,
    skyMidColor = 0xFF2C394A,
    skyBottomColor = 0xFF455569,
    horizonColor = 0xFF64748B
  ),
  WET_STORM(
    displayName = "Wet Rainstorm",
    icon = "🌧️",
    gripMultiplier = 0.84f,
    topSpeedPenaltyKmh = -16f,
    skyTopColor = 0xFF0A0F14,
    skyMidColor = 0xFF131D24,
    skyBottomColor = 0xFF1D2C37,
    horizonColor = 0xFF2B3F4F
  )
}

enum class TireCompound(
  val displayName: String,
  val shortCode: String,
  val gripFactor: Float,
  val wearRate: Float,
  val colorHex: Long
) {
  SOFT("Soft Slick (C3)", "S", 1.08f, 1.6f, 0xFFFF1744),
  MEDIUM("Medium Slick (C2)", "M", 1.00f, 1.0f, 0xFFFFD600),
  HARD("Hard Slick (C1)", "H", 0.94f, 0.55f, 0xFFECEFF1),
  INTERMEDIATE("Intermediate Wet", "I", 0.98f, 0.8f, 0xFF00E676);

  val composeColor: Color get() = Color(colorHex)
}

enum class AeroTrim(
  val displayName: String,
  val shortLabel: String,
  val topSpeedBonusKmh: Float,
  val gripMultiplier: Float,
  val description: String
) {
  HIGH_DOWNFORCE("High Downforce", "HIGH WING", -8f, 1.12f, "Maximum apex cornering grip for twisty street circuits"),
  BALANCED("Balanced Aero", "BALANCED", 0f, 1.0f, "Optimal balance of top-end straightaway speed and stability"),
  LOW_DRAG("Low Drag / Velocity", "LOW DRAG", +14f, 0.91f, "Extreme top speed on long straights with reduced drag")
}

data class RoadSegment(
  val segmentIndex: Int,
  val curvature: Float, // -1.0 (hard left) to +1.0 (hard right)
  val elevation: Float, // hill up / down
  val isBrakingZone: Boolean = false,
  val isSpeedTrap: Boolean = false,
  val hasDistanceBoard: Int? = null, // e.g. 200, 150, 100, 50 meters
  val curbColor: Long = 0xFFE53935
)

data class RacingGameHudState(
  val currentRank: Int = 1,
  val totalRivals: Int = 8,
  val currentLap: Int = 1,
  val totalLaps: Int = 3,
  val intervalAheadSeconds: Float? = null,
  val intervalBehindSeconds: Float? = null,
  val rivalAheadName: String? = null,
  val speedKmh: Float = 0f,
  val rpm: Int = 1200,
  val maxRpm: Int = 9200,
  val redlineRpm: Int = 8500,
  val gear: Int = 1,
  val isAutoGears: Boolean = true,
  val nitroChargePercent: Float = 1.0f,
  val isNitroActive: Boolean = false,
  val isDrsAvailable: Boolean = false,
  val isDrsActive: Boolean = false,
  val isDrafting: Boolean = false,
  val slipstreamBonusKmh: Float = 0f,
  val driftPoints: Int = 0,
  val currentLapTimeMs: Long = 0L,
  val bestLapTimeMs: Long? = null,
  val lastLapTimeMs: Long? = null,
  val sector1TimeMs: Long? = null,
  val sector2TimeMs: Long? = null,
  val sector3TimeMs: Long? = null,
  val deltaVsBestSeconds: Float? = null,
  val activeAlertMessage: String? = null,
  val activeAlertColor: Long = 0xFF00E5FF,
  val countdownLights: Int = 0, // 0 = off, 1..5 = red lights, 6 = green / lights out!
  val activeWeather: RaceWeather = RaceWeather.CLEAR_SUNSET,
  val tireCompound: TireCompound = TireCompound.MEDIUM,
  val tireHealthPercent: Float = 100f,
  val tireTempCelsius: Float = 95f,
  val speedTrapKmh: Float? = null,
  val isSpeedTrapActive: Boolean = false,
  val aeroTrim: AeroTrim = AeroTrim.BALANCED,
  val isPitWindowOpen: Boolean = false,
  val isBoxBoxAlert: Boolean = false,
  val isPitStopActive: Boolean = false,
  val currentSurfaceName: String = "ROAD",
  val currentBiomeName: String = "FOREST"
)

data class LapTelemetrySummary(
  val lapNumber: Int,
  val lapTimeMs: Long,
  val sector1Ms: Long,
  val sector2Ms: Long,
  val sector3Ms: Long,
  val gapToLeaderSeconds: Float,
  val positionOnLap: Int,
  val tireCompoundCode: String = "M"
)

data class RaceFinishSummary(
  val finishingPosition: Int,
  val totalRacers: Int,
  val totalTimeMs: Long,
  val bestLapMs: Long,
  val topSpeedKmh: Float,
  val overtakesCount: Int,
  val driftScore: Int,
  val xpEarned: Int,
  val creditsEarned: Int,
  val trackName: String,
  val vehicleName: String,
  val isNewPersonalBest: Boolean,
  val newlyUnlockedBadges: List<String> = emptyList(),
  val lapSummaries: List<LapTelemetrySummary> = emptyList()
)

