package com.example.game.v2

import android.content.Context
import com.example.model.Vehicle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

/**
 * PROMPT 3: Wheel / Rim customization options.
 */
enum class WheelRimStyle(
  val displayName: String,
  val spokeCount: Int,
  val weightDeltaKg: Int,
  val handlingBonusG: Float,
  val priceCredits: Int,
  val rimColorHex: Long
) {
  STOCK_FORGED("OEM Forged 10-Spoke", 10, 0, 0.0f, 0, 0xFFB0BEC5),
  BBS_CENTERLOCK("GT3 Centerlock Mesh", 7, -12, 0.08f, 3500, 0xFFFFD700),
  CARBON_AERO_DISC("Carbon Aero Turbofan", 5, -22, 0.14f, 6800, 0xFF00E5FF),
  TITANIUM_MULTISPOKE("Titanium Track Ultralight", 12, -28, 0.20f, 9500, 0xFFFF1744)
}

/**
 * PROMPT 3: Rear Wing / Spoiler customization options.
 */
enum class SpoilerWingStyle(
  val displayName: String,
  val downforceBonusPct: Int,
  val topSpeedDeltaKmh: Int,
  val handlingBonusG: Float,
  val priceCredits: Int
) {
  FACTORY_LIP("Factory Aero Lip", 0, 0, 0.0f, 0),
  GT3_SWAN_NECK("GT3 Swan-Neck Wing", 12, 2, 0.12f, 4200),
  TIME_ATTACK_DUAL("Time Attack Dual-Element", 22, -3, 0.24f, 7500),
  ACTIVE_DRS_CARBON("Active DRS Carbon Wing", 18, 9, 0.20f, 12000)
}

/**
 * PROMPT 3: Body Kit & Aero Package options.
 */
enum class BodyKitSpec(
  val displayName: String,
  val widthBonusPx: Float,
  val topSpeedBonusKmh: Int,
  val handlingBonusG: Float,
  val priceCredits: Int
) {
  FACTORY_SPEC("Factory Motorsport Spec", 0f, 0, 0.0f, 0),
  STREET_WIDEBODY("Street Widebody Fender Kit", 6f, 4, 0.10f, 5500),
  GT_AERO_DIFFUSER("GT Pro Splitter & Diffuser", 10f, 8, 0.18f, 9200),
  HYPER_CARBON_KIT("Full Carbon Venturi Widebody", 14f, 14, 0.28f, 15500)
}

/**
 * PROMPT 3: Curated Motorsport Paint Finishes.
 */
enum class GaragePaintOption(
  val displayName: String,
  val colorHex: Long,
  val finishType: String
) {
  RACING_RED("Apex Rosso Corsa", 0xFFFF1744, "Metallic Gloss"),
  VOLT_CYAN("Neo Tokyo Volt Cyan", 0xFF00E5FF, "Pearl Iridescent"),
  GULF_ORANGE("Papaya GT Orange", 0xFFFF6D00, "Satin Motorsport"),
  MANTHEY_GRELLO("Nürburgring Grello", 0xFF76FF03, "Fluorescent Track"),
  BILSTEIN_GOLD("Championship Gold", 0xFFFFD600, "Chrome Metallic"),
  GRAPHITE_STEALTH("Stealth Carbon Matte", 0xFF37474F, "Dry Carbon Weave"),
  ULTRAVIOLET_PRO("Midnight Ultraviolet", 0xFFD500F9, "Candy Clearcoat"),
  PURE_CHAMPION_WHITE("Fuji Championship White", 0xFFECEFF1, "Ceramic Gloss")
}

/**
 * Stage 3 & 4: 12-Material Paint & Surface Finish Studio options (matching Paint & Finish Studio screenshots).
 */
enum class PaintFinishMaterial(
  val displayName: String,
  val primaryOrbHex: Long,
  val secondaryOrbHex: Long,
  val specularHighlightAlpha: Float
) {
  GLOSS("GLOSS", 0xFF9E9E9E, 0xFFE0E0E0, 0.85f),
  METALLIC("METALLIC", 0xFF757575, 0xFFBDBDBD, 0.78f),
  CHROME("CHROME", 0xFFECEFF1, 0xFF37474F, 0.96f),
  SATIN_METALLIC("SATIN METALLIC", 0xFF616161, 0xFF9E9E9E, 0.50f),
  CARBON("CARBON", 0xFF212121, 0xFF424242, 0.65f),
  MATTE_CARBON("MATTE CARBON", 0xFF1A1A1A, 0xFF303030, 0.25f),
  FORGED_CARBON("FORGED CARBON", 0xFF263238, 0xFF546E7A, 0.55f),
  CHAMELEON_FLAKE("CHAMELEON FLAKE", 0xFF7B1FA2, 0xFF00E676, 0.88f),
  COLOR_SHIFT("COLOR SHIFT", 0xFFD500F9, 0xFFFF9100, 0.86f),
  IRIDESCENT("IRIDESCENT", 0xFF00B0FF, 0xFFFF4081, 0.90f),
  COLOR_SHIFT_CARBON("COLOR SHIFT CARBON", 0xFF4A148C, 0xFF1B5E20, 0.72f),
  DAMASCUS("DAMASCUS", 0xFF37474F, 0xFF90A4AE, 0.75f)
}

/**
 * PROMPT 3: Performance Upgrade Subsystem types.
 */
enum class UpgradeSubsystem(
  val displayName: String,
  val shortCode: String,
  val baseCostCredits: Int
) {
  ENGINE("Engine Block & ECU", "ENG", 3000),
  TURBO("Twin-Turbo & Hybrid ERS", "TRB", 3500),
  BRAKES("Carbon-Ceramic Brakes", "BRK", 2500),
  TIRES("Motorsport Slick Compound", "TIR", 2800)
}

/**
 * Complete customization and upgrade loadout for a specific vehicle.
 */
data class VehicleCustomizationLoadout(
  val vehicleId: String,
  val paintColorHex: Long,
  val wheelRimStyle: WheelRimStyle = WheelRimStyle.STOCK_FORGED,
  val spoilerStyle: SpoilerWingStyle = SpoilerWingStyle.FACTORY_LIP,
  val bodyKitSpec: BodyKitSpec = BodyKitSpec.FACTORY_SPEC,
  val paintFinishMaterial: PaintFinishMaterial = PaintFinishMaterial.METALLIC,
  val engineLevel: Int = 1, // 1..5
  val turboLevel: Int = 1,  // 1..5
  val brakesLevel: Int = 1, // 1..5
  val tiresLevel: Int = 1   // 1..5
) {
  val totalUpgradeLevel: Int
    get() = engineLevel + turboLevel + brakesLevel + tiresLevel

  val maxUpgradeLevel: Int
    get() = 20
}

/**
 * Live computed stats for the 3D Garage display and physics engine.
 */
data class UpgradedVehicleStats(
  val vehicleId: String,
  val maxSpeedKmh: Int,
  val acceleration0to100Sec: Float,
  val handlingCorneringG: Float,
  val brakingRating: Int, // 0..100
  val horsepower: Int,
  val weightKg: Int,
  val basePriceCredits: Int,
  val totalUpgradeLevel: Int,
  val maxUpgradeLevel: Int = 20
)

/**
 * PROMPT 5: Four Career Championship Tiers.
 */
enum class ChampionshipTier(
  val tierNumber: Int,
  val displayName: String,
  val requiredLevel: Int,
  val badgeColorHex: Long,
  val trophyRewardCredits: Int,
  val trophyRewardXp: Int,
  val unlockedTrackName: String,
  val unlockedVehicleName: String,
  val unlockedTrackId: String = "track_miami_highway",
  val requiredPerformanceIndex: Int = 500
) {
  ROOKIE_CHAMPIONSHIP(
    tierNumber = 1,
    displayName = "Rookie Championship",
    requiredLevel = 1,
    badgeColorHex = 0xFF00E676,
    trophyRewardCredits = 10000,
    trophyRewardXp = 1500,
    unlockedTrackName = "Miami Ocean Causeway",
    unlockedVehicleName = "Apex GT3-R Twin-Turbo (#55)",
    unlockedTrackId = "track_miami_highway",
    requiredPerformanceIndex = 500
  ),
  AMATEUR_CHAMPIONSHIP(
    tierNumber = 2,
    displayName = "Amateur Championship",
    requiredLevel = 2,
    badgeColorHex = 0xFF00E5FF,
    trophyRewardCredits = 18000,
    trophyRewardXp = 2800,
    unlockedTrackName = "Suzuka Grand GP",
    unlockedVehicleName = "Scuderia Corsa V8 (#51)",
    unlockedTrackId = "track_suzuka",
    requiredPerformanceIndex = 520
  ),
  PRO_CHAMPIONSHIP(
    tierNumber = 3,
    displayName = "Professional Championship",
    requiredLevel = 4,
    badgeColorHex = 0xFFFFD600,
    trophyRewardCredits = 32000,
    trophyRewardXp = 5000,
    unlockedTrackName = "Tokyo Midnight Expressway",
    unlockedVehicleName = "Manthey Racing 911 GT3 R (#91)",
    unlockedTrackId = "track_tokyo",
    requiredPerformanceIndex = 550
  ),
  ELITE_CHAMPIONSHIP(
    tierNumber = 4,
    displayName = "Elite Championship",
    requiredLevel = 6,
    badgeColorHex = 0xFFFF1744,
    trophyRewardCredits = 60000,
    trophyRewardXp = 9500,
    unlockedTrackName = "Marina Bay Night GP",
    unlockedVehicleName = "Hyperion-7 Hypercar (#42)",
    unlockedTrackId = "track_marina_bay",
    requiredPerformanceIndex = 580
  )
}

/**
 * PROMPT 5: Career & Daily Event Categories.
 */
enum class CareerEventCategory(val displayName: String, val badgeText: String) {
  DAILY_CHALLENGE("Daily Racing Challenge", "DAILY"),
  TIME_TRIAL("Precision Time Trial", "TIME TRIAL"),
  SPRINT_RACE("Highway Sprint Race", "SPRINT"),
  CIRCUIT_RACE("Pro Circuit GP", "CIRCUIT"),
  DRIFT_CHALLENGE("Touge Drift Challenge", "DRIFT")
}

data class CareerChampionshipEvent(
  val eventId: String,
  val title: String,
  val championshipTier: ChampionshipTier,
  val category: CareerEventCategory,
  val environmentName: String,
  val targetObjective: String,
  val rewardCredits: Int,
  val rewardXp: Int,
  val unlockRewardLabel: String,
  val trackId: String = "track_neo_tokyo",
  val requiredLevel: Int = championshipTier.requiredLevel,
  val requiredPerformanceIndex: Int = championshipTier.requiredPerformanceIndex,
  val unlocksTrackId: String? = null
)

/**
 * PROMPT 6: Player Missions, Weekly Challenges & Achievement Rewards.
 */
data class PlayerRewardMission(
  val missionId: String,
  val title: String,
  val missionType: String, // "DAILY_MISSION", "WEEKLY_CHALLENGE", "ACHIEVEMENT"
  val description: String,
  val targetProgress: Int,
  val currentProgress: Int,
  val rewardCredits: Int,
  val rewardXp: Int
)

/**
 * Complete snapshot of Apex Rivals 2.0 Career, Garage & Profile Progression.
 */
data class ApexRivals2CareerState(
  val playerId: String = "pilot_tr8x_default",
  val playerUsername: String = "Apex_Legend_TR8X",
  val avatarBadge: String = "PRO_HELMET_RED",
  val coinsBalance: Int = 250,
  val creditsBalance: Int = 48500,
  val totalEarningsCredits: Int = 148500,
  val totalXp: Int = 8400,
  val playerLevel: Int = 5,
  val totalRacesCompleted: Int = 24,
  val totalVictories: Int = 16,
  val favoriteVehicleName: String = "Hyperion-7 Hypercar (#42 Volt/Nitro)",
  val dailyLoginStreakDays: Int = 4,
  val isDailyLoginClaimedToday: Boolean = false,
  val activeChampionshipTier: ChampionshipTier = ChampionshipTier.PRO_CHAMPIONSHIP,
  val ownedVehicleIds: Set<String> = setOf("car_gt3_apex", "car_gt3", "bike_superbike_1000", "bike_1000rr", "car_hyperion7_nitro", "car_hyper_v12"),
  val unlockedAchievements: Set<String> = setOf("ach_first_victory", "ach_garage_tuner"),
  val claimedEventIds: Set<String> = emptySet(),
  val claimedMissionIds: Set<String> = emptySet(),
  val claimedChampionshipTiers: Set<String> = emptySet(),
  val unlockedTrackIds: Set<String> = setOf(
    "track_neo_tokyo",
    "track_monza",
    "track_miami_highway",
    "track_suzuka",
    "track_tokyo"
  ),
  val loadoutsByVehicleId: Map<String, VehicleCustomizationLoadout> = emptyMap()
)

/**
 * Central manager for Apex Rivals 2.0 3D Garage Customizations, Career Championships,
 * Daily Login Rewards, Unlockable Tracks, and Idempotent Reward Claims.
 */
object ApexRivals2CareerManager {
  private const val PREFS_NAME = "apex_rivals_v2_progression"

  val careerEvents: List<CareerChampionshipEvent> = listOf(
    // ==================== TIER 1: ROOKIE CHAMPIONSHIP ====================
    CareerChampionshipEvent(
      eventId = "evt_rookie_city_sprint",
      title = "Metro Expressway Midnight Sprint",
      championshipTier = ChampionshipTier.ROOKIE_CHAMPIONSHIP,
      category = CareerEventCategory.SPRINT_RACE,
      environmentName = "Modern City Highways • Neo Tokyo",
      targetObjective = "Finish Top 3 across 3.8 km urban highway sprint",
      rewardCredits = 4500,
      rewardXp = 850,
      unlockRewardLabel = "Unlocks Miami Ocean Causeway Track",
      trackId = "track_neo_tokyo",
      requiredLevel = 1,
      requiredPerformanceIndex = 500,
      unlocksTrackId = "track_miami_highway"
    ),
    CareerChampionshipEvent(
      eventId = "evt_rookie_time_trial",
      title = "Bayfront Sector Time Trial",
      championshipTier = ChampionshipTier.ROOKIE_CHAMPIONSHIP,
      category = CareerEventCategory.TIME_TRIAL,
      environmentName = "Monza Speed Autodrome • Qualifying",
      targetObjective = "Set a clean lap under 1:18.000 without barrier contact",
      rewardCredits = 5500,
      rewardXp = 1000,
      unlockRewardLabel = "Unlocks BBS Centerlock Rims",
      trackId = "track_monza",
      requiredLevel = 1,
      requiredPerformanceIndex = 500
    ),
    CareerChampionshipEvent(
      eventId = "evt_rookie_drift_intro",
      title = "Causeway Bayside Drift Showcase",
      championshipTier = ChampionshipTier.ROOKIE_CHAMPIONSHIP,
      category = CareerEventCategory.DRIFT_CHALLENGE,
      environmentName = "Miami Ocean Causeway • Sunset",
      targetObjective = "Score 1,500+ Drift Points through Bayside sweepers",
      rewardCredits = 6000,
      rewardXp = 1150,
      unlockRewardLabel = "Qualifies for Rookie Championship Cup",
      trackId = "track_miami_highway",
      requiredLevel = 1,
      requiredPerformanceIndex = 505,
      unlocksTrackId = "track_miami_highway"
    ),

    // ==================== TIER 2: AMATEUR CHAMPIONSHIP ====================
    CareerChampionshipEvent(
      eventId = "evt_amateur_desert_outrun",
      title = "Mojave Canyon Heatwave Outrun",
      championshipTier = ChampionshipTier.AMATEUR_CHAMPIONSHIP,
      category = CareerEventCategory.SPRINT_RACE,
      environmentName = "Highway Speed Trap • Miami Causeway",
      targetObjective = "Reach 305+ km/h through the highway radar speed trap",
      rewardCredits = 8000,
      rewardXp = 1400,
      unlockRewardLabel = "Unlocks Suzuka Grand GP Circuit",
      trackId = "track_miami_highway",
      requiredLevel = 2,
      requiredPerformanceIndex = 520,
      unlocksTrackId = "track_suzuka"
    ),
    CareerChampionshipEvent(
      eventId = "evt_amateur_suzuka_time_trial",
      title = "Suzuka S-Curves Precision Time Trial",
      championshipTier = ChampionshipTier.AMATEUR_CHAMPIONSHIP,
      category = CareerEventCategory.TIME_TRIAL,
      environmentName = "Suzuka Grand GP • Sector Attack",
      targetObjective = "Post a clean lap under 1:29.500 across the 130R & Esses",
      rewardCredits = 9000,
      rewardXp = 1600,
      unlockRewardLabel = "Unlocks GT3 Swan-Neck Wing",
      trackId = "track_suzuka",
      requiredLevel = 2,
      requiredPerformanceIndex = 525,
      unlocksTrackId = "track_suzuka"
    ),
    CareerChampionshipEvent(
      eventId = "evt_amateur_harbor_drift",
      title = "Shibuya Wet Neon Drift Trial",
      championshipTier = ChampionshipTier.AMATEUR_CHAMPIONSHIP,
      category = CareerEventCategory.DRIFT_CHALLENGE,
      environmentName = "Neo Tokyo Night City • Wet Asphalt",
      targetObjective = "Chain 2,400+ Drift Points using Handbrake apex entries",
      rewardCredits = 9500,
      rewardXp = 1750,
      unlockRewardLabel = "Qualifies for Amateur Championship Cup",
      trackId = "track_neo_tokyo",
      requiredLevel = 2,
      requiredPerformanceIndex = 530
    ),

    // ==================== TIER 3: PROFESSIONAL CHAMPIONSHIP ====================
    CareerChampionshipEvent(
      eventId = "evt_pro_highway_sprint",
      title = "Wangan Viaduct 330 KM/H Sprint",
      championshipTier = ChampionshipTier.PRO_CHAMPIONSHIP,
      category = CareerEventCategory.SPRINT_RACE,
      environmentName = "Tokyo Midnight Expressway • Night",
      targetObjective = "Finish P1 or P2 across the Rainbow Bridge viaduct sprint",
      rewardCredits = 11500,
      rewardXp = 2000,
      unlockRewardLabel = "Unlocks Tokyo Midnight Expressway",
      trackId = "track_tokyo",
      requiredLevel = 4,
      requiredPerformanceIndex = 550,
      unlocksTrackId = "track_tokyo"
    ),
    CareerChampionshipEvent(
      eventId = "evt_pro_mountain_drift",
      title = "Alpine Touge Hairpin Drift Battle",
      championshipTier = ChampionshipTier.PRO_CHAMPIONSHIP,
      category = CareerEventCategory.DRIFT_CHALLENGE,
      environmentName = "Mountain Roads • Suzuka Dunlop & Hairpin",
      targetObjective = "Score 3,500+ Drift Points using Handbrake apex entries",
      rewardCredits = 12500,
      rewardXp = 2200,
      unlockRewardLabel = "Unlocks Time Attack Dual Wing",
      trackId = "track_suzuka",
      requiredLevel = 4,
      requiredPerformanceIndex = 555
    ),
    CareerChampionshipEvent(
      eventId = "evt_pro_monza_time_trial",
      title = "Monza Curva Parabolica Time Trial",
      championshipTier = ChampionshipTier.PRO_CHAMPIONSHIP,
      category = CareerEventCategory.TIME_TRIAL,
      environmentName = "Monza Speed Autodrome • Pro Hotlap",
      targetObjective = "Beat the 1:17.200 Pro Qualifying benchmark",
      rewardCredits = 14000,
      rewardXp = 2500,
      unlockRewardLabel = "Qualifies for Pro Championship Cup",
      trackId = "track_monza",
      requiredLevel = 4,
      requiredPerformanceIndex = 560
    ),

    // ==================== TIER 4: ELITE CHAMPIONSHIP ====================
    CareerChampionshipEvent(
      eventId = "evt_elite_marina_sprint",
      title = "Marina Bay Waterfront Hyper Sprint",
      championshipTier = ChampionshipTier.ELITE_CHAMPIONSHIP,
      category = CareerEventCategory.SPRINT_RACE,
      environmentName = "Marina Bay Night GP • Street Circuit",
      targetObjective = "Win P1 against Elite hypercars on the illuminated street grid",
      rewardCredits = 18000,
      rewardXp = 3200,
      unlockRewardLabel = "Unlocks Marina Bay Night GP Circuit",
      trackId = "track_marina_bay",
      requiredLevel = 6,
      requiredPerformanceIndex = 580,
      unlocksTrackId = "track_marina_bay"
    ),
    CareerChampionshipEvent(
      eventId = "evt_elite_night_drift",
      title = "Harumi Hairpin Master Drift Challenge",
      championshipTier = ChampionshipTier.ELITE_CHAMPIONSHIP,
      category = CareerEventCategory.DRIFT_CHALLENGE,
      environmentName = "Tokyo Midnight Expressway • Drift Masters",
      targetObjective = "Score 5,000+ Drift Points without wall contact",
      rewardCredits = 21000,
      rewardXp = 3800,
      unlockRewardLabel = "Unlocks Active DRS Carbon Wing",
      trackId = "track_tokyo",
      requiredLevel = 6,
      requiredPerformanceIndex = 585
    ),
    CareerChampionshipEvent(
      eventId = "evt_elite_gp_circuit",
      title = "Apex World GT3 Endurance Finale",
      championshipTier = ChampionshipTier.ELITE_CHAMPIONSHIP,
      category = CareerEventCategory.CIRCUIT_RACE,
      environmentName = "Professional Race Circuits • Marina Bay Night GP",
      targetObjective = "Win P1 against 15 Elite AI Rivals with mandatory Pit Stop",
      rewardCredits = 25000,
      rewardXp = 4500,
      unlockRewardLabel = "Unlocks Hyperion-7 Carbon Widebody & Marina Bay",
      trackId = "track_marina_bay",
      requiredLevel = 6,
      requiredPerformanceIndex = 590,
      unlocksTrackId = "track_marina_bay"
    ),

    // ==================== DAILY FEATURED CHALLENGE ====================
    CareerChampionshipEvent(
      eventId = "evt_daily_neon_challenge",
      title = "Daily Challenge: Wet Neon Slipstream",
      championshipTier = ChampionshipTier.PRO_CHAMPIONSHIP,
      category = CareerEventCategory.DAILY_CHALLENGE,
      environmentName = "Nighttime City Racing • Neo Tokyo",
      targetObjective = "Complete 3 slipstream overtakes in Wet Rainstorm conditions",
      rewardCredits = 6500,
      rewardXp = 1200,
      unlockRewardLabel = "Daily Bonus +6,500 CR",
      trackId = "track_neo_tokyo",
      requiredLevel = 1,
      requiredPerformanceIndex = 500
    )
  )

  val rewardMissions: List<PlayerRewardMission> = listOf(
    PlayerRewardMission(
      missionId = "msn_daily_apex_hunter",
      title = "Apex Hunter Daily",
      missionType = "DAILY_MISSION",
      description = "Complete 2 clean laps with 300+ km/h top speed",
      targetProgress = 2,
      currentProgress = 2,
      rewardCredits = 3500,
      rewardXp = 600
    ),
    PlayerRewardMission(
      missionId = "msn_weekly_drift_king",
      title = "Weekly Drift Syndicate",
      missionType = "WEEKLY_CHALLENGE",
      description = "Accumulate 10,000 drift points across Mountain & City tracks",
      targetProgress = 10000,
      currentProgress = 10000,
      rewardCredits = 12000,
      rewardXp = 2000
    ),
    PlayerRewardMission(
      missionId = "msn_ach_garage_tuner",
      title = "Master Race Engineer",
      missionType = "ACHIEVEMENT",
      description = "Upgrade any vehicle's Engine, Turbo, Brakes, or Tires in the 3D Garage",
      targetProgress = 1,
      currentProgress = 1,
      rewardCredits = 7500,
      rewardXp = 1500
    )
  )

  private val _state = MutableStateFlow(ApexRivals2CareerState())
  val state: StateFlow<ApexRivals2CareerState> = _state.asStateFlow()

  private const val V8_BACKUP_PREFS_NAME = "apex_rivals_v8_backup_snapshot"

  private fun serializeLoadouts(loadouts: Map<String, VehicleCustomizationLoadout>): String {
    return loadouts.values.joinToString(separator = ";") { l ->
      listOf(
        l.vehicleId,
        l.paintColorHex.toString(),
        l.wheelRimStyle.name,
        l.spoilerStyle.name,
        l.bodyKitSpec.name,
        l.paintFinishMaterial.name,
        l.engineLevel.toString(),
        l.turboLevel.toString(),
        l.brakesLevel.toString(),
        l.tiresLevel.toString()
      ).joinToString("|")
    }
  }

  private fun deserializeLoadouts(serialized: String?): Map<String, VehicleCustomizationLoadout> {
    if (serialized.isNullOrBlank()) return emptyMap()
    val result = mutableMapOf<String, VehicleCustomizationLoadout>()
    serialized.split(";").filter { it.isNotBlank() }.forEach { entry ->
      val parts = entry.split("|")
      if (parts.size >= 10) {
        val vehicleId = parts[0]
        val paintHex = parts[1].toLongOrNull() ?: 0xFFFF1744L
        val wheel = WheelRimStyle.entries.find { it.name == parts[2] } ?: WheelRimStyle.STOCK_FORGED
        val spoiler = SpoilerWingStyle.entries.find { it.name == parts[3] } ?: SpoilerWingStyle.FACTORY_LIP
        val bodyKit = BodyKitSpec.entries.find { it.name == parts[4] } ?: BodyKitSpec.FACTORY_SPEC
        val finish = PaintFinishMaterial.entries.find { it.name == parts[5] } ?: PaintFinishMaterial.METALLIC
        val eng = (parts[6].toIntOrNull() ?: 1).coerceIn(1, 5)
        val trb = (parts[7].toIntOrNull() ?: 1).coerceIn(1, 5)
        val brk = (parts[8].toIntOrNull() ?: 1).coerceIn(1, 5)
        val tir = (parts[9].toIntOrNull() ?: 1).coerceIn(1, 5)
        result[vehicleId] = VehicleCustomizationLoadout(
          vehicleId = vehicleId,
          paintColorHex = paintHex,
          wheelRimStyle = wheel,
          spoilerStyle = spoiler,
          bodyKitSpec = bodyKit,
          paintFinishMaterial = finish,
          engineLevel = eng,
          turboLevel = trb,
          brakesLevel = brk,
          tiresLevel = tir
        )
      }
    }
    return result
  }

  private fun readLegacyV8Loadouts(legacyPrefs: android.content.SharedPreferences): Map<String, VehicleCustomizationLoadout> {
    val result = deserializeLoadouts(legacyPrefs.getString("vehicle_loadouts_json", null)).toMutableMap()
    val knownVehicleIds = setOf(
      "car_gt3_apex", "car_gt3", "bike_superbike_1000", "bike_1000rr",
      "car_hyperion7_nitro", "car_hyper_v12", "bike_moto_gp", "car_f1_hybrid", "car_drift_s15"
    )
    knownVehicleIds.forEach { vId ->
      val hasLegacyKeys = legacyPrefs.contains("v8_upgrade_${vId}_engine") ||
        legacyPrefs.contains("v8_upgrade_${vId}_turbo") ||
        legacyPrefs.contains("v8_upgrade_${vId}_brakes") ||
        legacyPrefs.contains("v8_upgrade_${vId}_tires") ||
        legacyPrefs.contains("v8_upgrade_${vId}_paint_hex")
      if (hasLegacyKeys) {
        val prev = result[vId]
        val eng = maxOf(prev?.engineLevel ?: 1, legacyPrefs.getInt("v8_upgrade_${vId}_engine", 1).coerceIn(1, 5))
        val trb = maxOf(prev?.turboLevel ?: 1, legacyPrefs.getInt("v8_upgrade_${vId}_turbo", 1).coerceIn(1, 5))
        val brk = maxOf(prev?.brakesLevel ?: 1, legacyPrefs.getInt("v8_upgrade_${vId}_brakes", 1).coerceIn(1, 5))
        val tir = maxOf(prev?.tiresLevel ?: 1, legacyPrefs.getInt("v8_upgrade_${vId}_tires", 1).coerceIn(1, 5))
        val paintHex = legacyPrefs.getLong("v8_upgrade_${vId}_paint_hex", prev?.paintColorHex ?: 0xFFFF1744L)
        val wheelName = legacyPrefs.getString("v8_upgrade_${vId}_wheel", null)
        val spoilerName = legacyPrefs.getString("v8_upgrade_${vId}_spoiler", null)
        val bodyKitName = legacyPrefs.getString("v8_upgrade_${vId}_bodykit", null)
        val finishName = legacyPrefs.getString("v8_upgrade_${vId}_finish", null)
        result[vId] = VehicleCustomizationLoadout(
          vehicleId = vId,
          paintColorHex = paintHex,
          wheelRimStyle = wheelName?.let { n -> WheelRimStyle.entries.find { it.name == n } }
            ?: prev?.wheelRimStyle ?: WheelRimStyle.STOCK_FORGED,
          spoilerStyle = spoilerName?.let { n -> SpoilerWingStyle.entries.find { it.name == n } }
            ?: prev?.spoilerStyle ?: SpoilerWingStyle.FACTORY_LIP,
          bodyKitSpec = bodyKitName?.let { n -> BodyKitSpec.entries.find { it.name == n } }
            ?: prev?.bodyKitSpec ?: BodyKitSpec.FACTORY_SPEC,
          paintFinishMaterial = finishName?.let { n -> PaintFinishMaterial.entries.find { it.name == n } }
            ?: prev?.paintFinishMaterial ?: PaintFinishMaterial.METALLIC,
          engineLevel = eng,
          turboLevel = trb,
          brakesLevel = brk,
          tiresLevel = tir
        )
      }
    }
    // Alias legacy car_gt3 into active car_gt3_apex
    result["car_gt3"]?.let { legacyGt3 ->
      val prevApex = result["car_gt3_apex"]
      result["car_gt3_apex"] = legacyGt3.copy(
        vehicleId = "car_gt3_apex",
        engineLevel = maxOf(legacyGt3.engineLevel, prevApex?.engineLevel ?: 1),
        turboLevel = maxOf(legacyGt3.turboLevel, prevApex?.turboLevel ?: 1),
        brakesLevel = maxOf(legacyGt3.brakesLevel, prevApex?.brakesLevel ?: 1),
        tiresLevel = maxOf(legacyGt3.tiresLevel, prevApex?.tiresLevel ?: 1)
      )
    }
    // Alias legacy bike_1000rr into active bike_superbike_1000
    result["bike_1000rr"]?.let { legacyBike ->
      val prevSuperbike = result["bike_superbike_1000"]
      result["bike_superbike_1000"] = legacyBike.copy(
        vehicleId = "bike_superbike_1000",
        engineLevel = maxOf(legacyBike.engineLevel, prevSuperbike?.engineLevel ?: 1),
        turboLevel = maxOf(legacyBike.turboLevel, prevSuperbike?.turboLevel ?: 1),
        brakesLevel = maxOf(legacyBike.brakesLevel, prevSuperbike?.brakesLevel ?: 1),
        tiresLevel = maxOf(legacyBike.tiresLevel, prevSuperbike?.tiresLevel ?: 1)
      )
    }
    return result
  }

  fun initialize(context: Context) {
    try {
      val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
      // Stage 5: Pre-migration backup snapshot of V8 data before applying V8 -> V9 (2.0.0) in-place migration
      val alreadyMigratedV8 = prefs.getBoolean("v8_to_v9_in_place_migrated", false)
      if (!alreadyMigratedV8) {
        createV8BackupSnapshot(context)
      }

      // V8 -> V9 (2.0.0) Non-Destructive In-Place Upgrade Migration:
      // Reads any existing V8 account, level, coins, credits, unlocked vehicles, upgrades, race counts, and achievements.
      val legacyV8Prefs = context.getSharedPreferences("apex_rivals_prefs", Context.MODE_PRIVATE)
      val hasLegacyV8Account = legacyV8Prefs.contains("player_username") ||
        legacyV8Prefs.contains("player_id") ||
        legacyV8Prefs.contains("player_credits") ||
        legacyV8Prefs.contains("player_coins") ||
        legacyV8Prefs.contains("player_xp")

      val playerId = prefs.getString("player_id", null)
        ?: legacyV8Prefs.getString("player_id", null)
        ?: "pilot_tr8x_default"
      val playerUsername = prefs.getString("player_username", null)
        ?: legacyV8Prefs.getString("player_username", null)
        ?: "Apex_Legend_TR8X"
      val avatarBadge = prefs.getString("avatar_badge", null)
        ?: legacyV8Prefs.getString("avatar_badge", null)
        ?: "PRO_HELMET_RED"

      val legacyCoins = legacyV8Prefs.getInt("player_coins", 0).coerceAtLeast(0)
      val savedCoins = prefs.getInt("coins_balance", if (legacyCoins > 0) legacyCoins else 250).coerceAtLeast(0)
      val coins = if (!alreadyMigratedV8 && legacyCoins > 0) {
        if (prefs.contains("coins_balance")) savedCoins + legacyCoins else legacyCoins
      } else {
        savedCoins
      }

      val legacyCreditsBonus = legacyV8Prefs.getInt("player_credits", 0).coerceAtLeast(0)
      val baseCredits = prefs.getInt("credits_balance", 48500).coerceAtLeast(0)
      val credits = if (!alreadyMigratedV8 && legacyCreditsBonus > 0) {
        if (prefs.contains("credits_balance")) baseCredits + legacyCreditsBonus else maxOf(baseCredits, legacyCreditsBonus)
      } else {
        baseCredits
      }
      val totalEarnings = (prefs.getInt("total_earnings", 148500) + (if (!alreadyMigratedV8) legacyCreditsBonus else 0)).coerceAtLeast(credits)

      val legacyXpBonus = legacyV8Prefs.getInt("player_xp", 0).coerceAtLeast(0)
      val baseXp = prefs.getInt("total_xp", 8400).coerceAtLeast(0)
      val xp = if (!alreadyMigratedV8 && legacyXpBonus > 0) {
        if (prefs.contains("total_xp")) baseXp + legacyXpBonus else maxOf(baseXp, legacyXpBonus)
      } else {
        baseXp
      }
      val legacyLevel = legacyV8Prefs.getInt("player_level", 1).coerceAtLeast(1)
      val savedLevel = prefs.getInt("player_level", 1).coerceAtLeast(1)
      val computedLevel = maxOf(calculateLevelFromXp(xp), legacyLevel, savedLevel)

      val streak = maxOf(
        prefs.getInt("daily_streak", 4),
        legacyV8Prefs.getInt("daily_streak", 1)
      ).coerceAtLeast(1)
      val todayKey = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
      val lastClaimDate = prefs.getString("last_daily_claim_date", "") ?: ""
      val claimedEvents = (prefs.getStringSet("claimed_event_ids", emptySet()) ?: emptySet()) +
        (legacyV8Prefs.getStringSet("claimed_event_ids", emptySet()) ?: emptySet())
      val claimedMissions = (prefs.getStringSet("claimed_mission_ids", emptySet()) ?: emptySet()) +
        (legacyV8Prefs.getStringSet("claimed_mission_ids", emptySet()) ?: emptySet())
      val claimedTiers = (prefs.getStringSet("claimed_championship_tiers", emptySet()) ?: emptySet()) +
        (legacyV8Prefs.getStringSet("claimed_championship_tiers", emptySet()) ?: emptySet())
      val savedUnlockedTracks = (prefs.getStringSet("unlocked_track_ids", emptySet()) ?: emptySet()) +
        (legacyV8Prefs.getStringSet("unlocked_track_ids", emptySet()) ?: emptySet())

      val savedOwnedVehicles = prefs.getStringSet("owned_vehicle_ids", emptySet()) ?: emptySet()
      val legacyOwnedVehicles = legacyV8Prefs.getStringSet("owned_vehicle_ids", emptySet()) ?: emptySet()
      val mergedOwnedVehicles = (setOf("car_gt3_apex", "car_gt3", "bike_superbike_1000", "bike_1000rr", "car_hyperion7_nitro", "car_hyper_v12") + savedOwnedVehicles + legacyOwnedVehicles)

      val savedAchievements = prefs.getStringSet("unlocked_achievements", emptySet()) ?: emptySet()
      val legacyAchievements = legacyV8Prefs.getStringSet("unlocked_achievements", emptySet()) ?: emptySet()
      val mergedAchievements = (setOf("ach_first_victory", "ach_garage_tuner") + savedAchievements + legacyAchievements)

      val savedLoadouts = deserializeLoadouts(prefs.getString("vehicle_loadouts_json", null))
      val legacyLoadouts = readLegacyV8Loadouts(legacyV8Prefs)
      val mergedLoadouts = mutableMapOf<String, VehicleCustomizationLoadout>().apply {
        putAll(legacyLoadouts)
        savedLoadouts.forEach { (vId, v9Loadout) ->
          val v8Loadout = legacyLoadouts[vId]
          if (v8Loadout != null) {
            put(
              vId,
              v9Loadout.copy(
                engineLevel = maxOf(v9Loadout.engineLevel, v8Loadout.engineLevel),
                turboLevel = maxOf(v9Loadout.turboLevel, v8Loadout.turboLevel),
                brakesLevel = maxOf(v9Loadout.brakesLevel, v8Loadout.brakesLevel),
                tiresLevel = maxOf(v9Loadout.tiresLevel, v8Loadout.tiresLevel)
              )
            )
          } else {
            put(vId, v9Loadout)
          }
        }
      }

      val savedRacesCompleted = maxOf(
        prefs.getInt("total_races_completed", if (hasLegacyV8Account) 0 else 24),
        legacyV8Prefs.getInt("total_races_completed", 0)
      ).coerceAtLeast(0)
      val savedVictories = maxOf(
        prefs.getInt("total_victories", if (hasLegacyV8Account) 0 else 16),
        legacyV8Prefs.getInt("total_victories", 0)
      ).coerceAtLeast(0)

      // Compute unlocked tracks based on level, claimed events, and saved tracks (non-destructive)
      val mergedUnlockedTracks = mutableSetOf(
        "track_neo_tokyo",
        "track_monza",
        "track_miami_highway"
      ).apply {
        addAll(savedUnlockedTracks)
        if (computedLevel >= ChampionshipTier.AMATEUR_CHAMPIONSHIP.requiredLevel) add("track_suzuka")
        if (computedLevel >= ChampionshipTier.PRO_CHAMPIONSHIP.requiredLevel) add("track_tokyo")
        if (computedLevel >= ChampionshipTier.ELITE_CHAMPIONSHIP.requiredLevel) add("track_marina_bay")
        claimedEvents.forEach { evId ->
          careerEvents.find { it.eventId == evId }?.unlocksTrackId?.let { add(it) }
        }
        claimedTiers.forEach { tierName ->
          ChampionshipTier.entries.find { it.name == tierName }?.let { add(it.unlockedTrackId) }
        }
      }

      _state.value = _state.value.copy(
        playerId = playerId,
        playerUsername = playerUsername,
        avatarBadge = avatarBadge,
        coinsBalance = coins,
        creditsBalance = credits,
        totalEarningsCredits = totalEarnings,
        totalXp = xp,
        playerLevel = computedLevel,
        totalRacesCompleted = savedRacesCompleted,
        totalVictories = savedVictories,
        dailyLoginStreakDays = streak,
        isDailyLoginClaimedToday = (lastClaimDate == todayKey),
        ownedVehicleIds = mergedOwnedVehicles,
        unlockedAchievements = mergedAchievements,
        claimedEventIds = claimedEvents.toSet(),
        claimedMissionIds = claimedMissions.toSet(),
        claimedChampionshipTiers = claimedTiers.toSet(),
        unlockedTrackIds = mergedUnlockedTracks.toSet(),
        loadoutsByVehicleId = mergedLoadouts
      )

      if (!alreadyMigratedV8) {
        prefs.edit().putBoolean("v8_to_v9_in_place_migrated", true).apply()
        persistState(context)
      }
    } catch (_: Exception) {
      // Safe fallback in unit test environments
    }
  }

  /**
   * Stage 5: Creates a verified local snapshot of V8 / current player progression prior to upgrade or sync.
   */
  fun createV8BackupSnapshot(context: Context): Boolean {
    return try {
      val current = _state.value
      val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
      val legacyV8Prefs = context.getSharedPreferences("apex_rivals_prefs", Context.MODE_PRIVATE)
      val backupPrefs = context.getSharedPreferences(V8_BACKUP_PREFS_NAME, Context.MODE_PRIVATE)

      val snapPlayerId = prefs.getString("player_id", null)
        ?: legacyV8Prefs.getString("player_id", null)
        ?: current.playerId
      val snapUsername = prefs.getString("player_username", null)
        ?: legacyV8Prefs.getString("player_username", null)
        ?: current.playerUsername
      val snapCoins = if (legacyV8Prefs.contains("player_coins")) {
        legacyV8Prefs.getInt("player_coins", current.coinsBalance)
      } else {
        prefs.getInt("coins_balance", current.coinsBalance)
      }
      val snapCredits = if (prefs.contains("credits_balance")) {
        prefs.getInt("credits_balance", current.creditsBalance) +
          legacyV8Prefs.getInt("player_credits", 0).coerceAtLeast(0)
      } else if (legacyV8Prefs.contains("player_credits")) {
        maxOf(current.creditsBalance, legacyV8Prefs.getInt("player_credits", current.creditsBalance))
      } else {
        current.creditsBalance
      }
      val snapEarnings = prefs.getInt("total_earnings", current.totalEarningsCredits).coerceAtLeast(snapCredits)
      val snapXp = if (prefs.contains("total_xp")) {
        prefs.getInt("total_xp", current.totalXp) +
          legacyV8Prefs.getInt("player_xp", 0).coerceAtLeast(0)
      } else if (legacyV8Prefs.contains("player_xp")) {
        maxOf(current.totalXp, legacyV8Prefs.getInt("player_xp", current.totalXp))
      } else {
        current.totalXp
      }
      val snapLevel = maxOf(
        calculateLevelFromXp(snapXp),
        legacyV8Prefs.getInt("player_level", current.playerLevel),
        prefs.getInt("player_level", current.playerLevel)
      )
      val snapStreak = maxOf(
        prefs.getInt("daily_streak", current.dailyLoginStreakDays),
        legacyV8Prefs.getInt("daily_streak", 1)
      )
      val snapEvents = (prefs.getStringSet("claimed_event_ids", current.claimedEventIds) ?: emptySet()) +
        (legacyV8Prefs.getStringSet("claimed_event_ids", emptySet()) ?: emptySet())
      val snapMissions = (prefs.getStringSet("claimed_mission_ids", current.claimedMissionIds) ?: emptySet()) +
        (legacyV8Prefs.getStringSet("claimed_mission_ids", emptySet()) ?: emptySet())
      val snapOwnedVehicles = current.ownedVehicleIds +
        (prefs.getStringSet("owned_vehicle_ids", emptySet()) ?: emptySet()) +
        (legacyV8Prefs.getStringSet("owned_vehicle_ids", emptySet()) ?: emptySet())
      val snapAchievements = current.unlockedAchievements +
        (prefs.getStringSet("unlocked_achievements", emptySet()) ?: emptySet()) +
        (legacyV8Prefs.getStringSet("unlocked_achievements", emptySet()) ?: emptySet())
      val snapTracks = current.unlockedTrackIds +
        (prefs.getStringSet("unlocked_track_ids", emptySet()) ?: emptySet()) +
        (legacyV8Prefs.getStringSet("unlocked_track_ids", emptySet()) ?: emptySet())
      val snapLoadouts = readLegacyV8Loadouts(legacyV8Prefs) +
        deserializeLoadouts(prefs.getString("vehicle_loadouts_json", null)) +
        current.loadoutsByVehicleId

      backupPrefs.edit()
        .putBoolean("has_v8_snapshot", true)
        .putString("snapshot_player_id", snapPlayerId)
        .putString("snapshot_username", snapUsername)
        .putInt("snapshot_coins", snapCoins)
        .putInt("snapshot_credits", snapCredits)
        .putInt("snapshot_earnings", snapEarnings)
        .putInt("snapshot_xp", snapXp)
        .putInt("snapshot_level", snapLevel)
        .putInt("snapshot_streak", snapStreak)
        .putStringSet("snapshot_claimed_events", snapEvents)
        .putStringSet("snapshot_claimed_missions", snapMissions)
        .putStringSet("snapshot_owned_vehicles", snapOwnedVehicles)
        .putStringSet("snapshot_achievements", snapAchievements)
        .putStringSet("snapshot_unlocked_tracks", snapTracks)
        .putString("snapshot_loadouts_json", serializeLoadouts(snapLoadouts))
        .putLong("snapshot_timestamp_ms", System.currentTimeMillis())
        .apply()
      ApexRivals2SettingsRepository.logEvent(
        "V8_BACKUP",
        "Created V8 pre-upgrade backup snapshot (CR=$snapCredits, XP=$snapXp, Vehicles=${snapOwnedVehicles.size})"
      )
      true
    } catch (_: Exception) {
      false
    }
  }

  /**
   * Stage 5: Restores player progression from the saved V8 backup snapshot if needed.
   */
  fun restoreFromV8BackupSnapshot(context: Context): Boolean {
    return try {
      val backupPrefs = context.getSharedPreferences(V8_BACKUP_PREFS_NAME, Context.MODE_PRIVATE)
      if (!backupPrefs.getBoolean("has_v8_snapshot", false)) return false

      val restoredPlayerId = backupPrefs.getString("snapshot_player_id", _state.value.playerId) ?: _state.value.playerId
      val restoredUsername = backupPrefs.getString("snapshot_username", _state.value.playerUsername) ?: _state.value.playerUsername
      val restoredCoins = backupPrefs.getInt("snapshot_coins", _state.value.coinsBalance)
      val restoredCredits = backupPrefs.getInt("snapshot_credits", 48500)
      val restoredEarnings = backupPrefs.getInt("snapshot_earnings", 148500)
      val restoredXp = backupPrefs.getInt("snapshot_xp", 8400)
      val restoredLevel = maxOf(
        calculateLevelFromXp(restoredXp),
        backupPrefs.getInt("snapshot_level", 1)
      )
      val restoredStreak = backupPrefs.getInt("snapshot_streak", 4)
      val restoredEvents = backupPrefs.getStringSet("snapshot_claimed_events", emptySet()) ?: emptySet()
      val restoredMissions = backupPrefs.getStringSet("snapshot_claimed_missions", emptySet()) ?: emptySet()
      val restoredVehicles = backupPrefs.getStringSet("snapshot_owned_vehicles", _state.value.ownedVehicleIds) ?: _state.value.ownedVehicleIds
      val restoredAchievements = backupPrefs.getStringSet("snapshot_achievements", _state.value.unlockedAchievements) ?: _state.value.unlockedAchievements
      val restoredTracks = backupPrefs.getStringSet("snapshot_unlocked_tracks", _state.value.unlockedTrackIds) ?: _state.value.unlockedTrackIds
      val restoredLoadouts = deserializeLoadouts(backupPrefs.getString("snapshot_loadouts_json", null)).ifEmpty {
        _state.value.loadoutsByVehicleId
      }

      _state.value = _state.value.copy(
        playerId = restoredPlayerId,
        playerUsername = restoredUsername,
        coinsBalance = restoredCoins,
        creditsBalance = restoredCredits,
        totalEarningsCredits = restoredEarnings,
        totalXp = restoredXp,
        playerLevel = restoredLevel,
        dailyLoginStreakDays = restoredStreak,
        claimedEventIds = restoredEvents.toSet(),
        claimedMissionIds = restoredMissions.toSet(),
        ownedVehicleIds = restoredVehicles.toSet(),
        unlockedAchievements = restoredAchievements.toSet(),
        unlockedTrackIds = restoredTracks.toSet(),
        loadoutsByVehicleId = restoredLoadouts
      )
      persistState(context)
      ApexRivals2SettingsRepository.logEvent(
        "V8_RESTORE",
        "Restored player profile from V8 backup snapshot (CR=$restoredCredits, XP=$restoredXp)"
      )
      true
    } catch (_: Exception) {
      false
    }
  }

  fun calculateLevelFromXp(xp: Int): Int {
    return ((xp / 1800) + 1).coerceAtLeast(1)
  }

  fun getLoadoutForVehicle(vehicle: Vehicle): VehicleCustomizationLoadout {
    val existing = _state.value.loadoutsByVehicleId[vehicle.id]
    if (existing != null) return existing
    val defaultLoadout = VehicleCustomizationLoadout(
      vehicleId = vehicle.id,
      paintColorHex = vehicle.liveryColorHex
    )
    _state.value = _state.value.copy(
      loadoutsByVehicleId = _state.value.loadoutsByVehicleId + (vehicle.id to defaultLoadout)
    )
    return defaultLoadout
  }

  /**
   * Computes live upgraded vehicle performance stats from a base [Vehicle] and [VehicleCustomizationLoadout].
   */
  fun computeUpgradedStats(
    vehicle: Vehicle,
    loadout: VehicleCustomizationLoadout
  ): UpgradedVehicleStats {
    val engineSpeedBonus = (loadout.engineLevel - 1) * 5
    val turboSpeedBonus = (loadout.turboLevel - 1) * 6
    val aeroSpeedDelta = loadout.spoilerStyle.topSpeedDeltaKmh + loadout.bodyKitSpec.topSpeedBonusKmh
    val maxSpeed = (vehicle.topSpeedKmh + engineSpeedBonus + turboSpeedBonus + aeroSpeedDelta).coerceAtLeast(180)

    val accelImprovement = ((loadout.engineLevel - 1) * 0.06f) +
      ((loadout.turboLevel - 1) * 0.08f) +
      ((loadout.tiresLevel - 1) * 0.04f)
    val accel0to100 = (vehicle.acceleration0to100 - accelImprovement).coerceAtLeast(1.45f)

    val handlingBonus = ((loadout.tiresLevel - 1) * 0.11f) +
      ((loadout.brakesLevel - 1) * 0.05f) +
      loadout.wheelRimStyle.handlingBonusG +
      loadout.spoilerStyle.handlingBonusG +
      loadout.bodyKitSpec.handlingBonusG
    val handlingG = ((vehicle.peakCorneringG + handlingBonus) * 100f).roundToInt() / 100f

    val baseBraking = (vehicle.brakeBiasFront + 18).coerceIn(60, 82)
    val brakingRating = (baseBraking + (loadout.brakesLevel - 1) * 4 + (loadout.tiresLevel - 1) * 2).coerceIn(50, 99)

    val hpBonus = (loadout.engineLevel - 1) * 38 + (loadout.turboLevel - 1) * 45
    val upgradedHp = vehicle.horsepower + hpBonus
    val upgradedWeight = (vehicle.weightKg + loadout.wheelRimStyle.weightDeltaKg).coerceAtLeast(155)
    val basePrice = (vehicle.horsepower * 120) + (vehicle.topSpeedKmh * 150)

    return UpgradedVehicleStats(
      vehicleId = vehicle.id,
      maxSpeedKmh = maxSpeed,
      acceleration0to100Sec = accel0to100,
      handlingCorneringG = handlingG,
      brakingRating = brakingRating,
      horsepower = upgradedHp,
      weightKg = upgradedWeight,
      basePriceCredits = basePrice,
      totalUpgradeLevel = loadout.totalUpgradeLevel
    )
  }

  /**
   * Applies a customized loadout (Paint, Wheels, Spoiler, Body Kit) in the 3D Garage.
   */
  fun applyVisualCustomization(
    vehicle: Vehicle,
    paintColorHex: Long,
    wheelRimStyle: WheelRimStyle,
    spoilerStyle: SpoilerWingStyle,
    bodyKitSpec: BodyKitSpec,
    context: Context? = null
  ): VehicleCustomizationLoadout {
    val current = getLoadoutForVehicle(vehicle)
    val updated = current.copy(
      paintColorHex = paintColorHex,
      wheelRimStyle = wheelRimStyle,
      spoilerStyle = spoilerStyle,
      bodyKitSpec = bodyKitSpec
    )
    _state.value = _state.value.copy(
      loadoutsByVehicleId = _state.value.loadoutsByVehicleId + (vehicle.id to updated),
      favoriteVehicleName = vehicle.name
    )
    persistState(context)
    ApexRivals2SettingsRepository.logEvent(
      "GARAGE_3D",
      "Applied customization to ${vehicle.name}: ${wheelRimStyle.displayName}, ${spoilerStyle.displayName}, ${bodyKitSpec.displayName}"
    )
    return updated
  }

  /**
   * Stage 3 & 4: Applies one of the 12 Paint & Surface Finish Studio materials to the vehicle.
   */
  fun applyPaintFinishMaterial(
    vehicle: Vehicle,
    finishMaterial: PaintFinishMaterial,
    context: Context? = null
  ): VehicleCustomizationLoadout {
    val current = getLoadoutForVehicle(vehicle)
    val updated = current.copy(paintFinishMaterial = finishMaterial)
    _state.value = _state.value.copy(
      loadoutsByVehicleId = _state.value.loadoutsByVehicleId + (vehicle.id to updated),
      favoriteVehicleName = vehicle.name
    )
    persistState(context)
    ApexRivals2SettingsRepository.logEvent(
      "PAINT_STUDIO",
      "Applied ${finishMaterial.displayName} surface finish to ${vehicle.name}"
    )
    return updated
  }

  /**
   * Purchases the next upgrade level (1..5) for Engine, Turbo, Brakes, or Tires.
   * Returns true if upgrade succeeded, false if already max level (5) or insufficient credits.
   */
  fun upgradeSubsystem(
    vehicle: Vehicle,
    subsystem: UpgradeSubsystem,
    context: Context? = null
  ): Boolean {
    val current = getLoadoutForVehicle(vehicle)
    val currentLevel = when (subsystem) {
      UpgradeSubsystem.ENGINE -> current.engineLevel
      UpgradeSubsystem.TURBO -> current.turboLevel
      UpgradeSubsystem.BRAKES -> current.brakesLevel
      UpgradeSubsystem.TIRES -> current.tiresLevel
    }
    if (currentLevel >= 5) return false

    val upgradeCost = subsystem.baseCostCredits * currentLevel
    if (_state.value.creditsBalance < upgradeCost) return false

    val updatedLoadout = when (subsystem) {
      UpgradeSubsystem.ENGINE -> current.copy(engineLevel = currentLevel + 1)
      UpgradeSubsystem.TURBO -> current.copy(turboLevel = currentLevel + 1)
      UpgradeSubsystem.BRAKES -> current.copy(brakesLevel = currentLevel + 1)
      UpgradeSubsystem.TIRES -> current.copy(tiresLevel = currentLevel + 1)
    }

    val newCredits = _state.value.creditsBalance - upgradeCost
    val newXp = _state.value.totalXp + 250
    _state.value = _state.value.copy(
      creditsBalance = newCredits,
      totalXp = newXp,
      playerLevel = calculateLevelFromXp(newXp),
      loadoutsByVehicleId = _state.value.loadoutsByVehicleId + (vehicle.id to updatedLoadout)
    )
    persistState(context)
    ApexRivals2SettingsRepository.logEvent(
      "GARAGE_UPGRADE",
      "Upgraded ${vehicle.name} ${subsystem.displayName} to Lv.${currentLevel + 1} (-$upgradeCost CR)"
    )
    return true
  }

  /**
   * Computes the Performance Index (PI) for a vehicle and its current customization/upgrade loadout.
   */
  fun computeVehiclePerformanceIndex(vehicle: Vehicle): Int {
    val loadout = getLoadoutForVehicle(vehicle)
    val stats = computeUpgradedStats(vehicle, loadout)
    return 500 + stats.totalUpgradeLevel * 18 + (stats.maxSpeedKmh - 290).coerceAtLeast(0)
  }

  /**
   * Verifies whether the player meets the Driver Level and Vehicle PI requirements for a career event.
   */
  fun canEnterCareerEvent(event: CareerChampionshipEvent, vehicle: Vehicle? = null): Boolean {
    val currentState = _state.value
    if (currentState.playerLevel < event.requiredLevel) return false
    if (vehicle != null) {
      val vehiclePi = computeVehiclePerformanceIndex(vehicle)
      if (vehiclePi < event.requiredPerformanceIndex) return false
    }
    return true
  }

  /**
   * Returns the championship tier associated with unlocking a specific track.
   */
  fun getTrackRequiredTier(trackId: String): ChampionshipTier {
    return when (trackId) {
      "track_neo_tokyo", "track_monza", "track_miami_highway" -> ChampionshipTier.ROOKIE_CHAMPIONSHIP
      "track_suzuka" -> ChampionshipTier.AMATEUR_CHAMPIONSHIP
      "track_tokyo" -> ChampionshipTier.PRO_CHAMPIONSHIP
      "track_marina_bay" -> ChampionshipTier.ELITE_CHAMPIONSHIP
      else -> ChampionshipTier.ROOKIE_CHAMPIONSHIP
    }
  }

  /**
   * Returns a human-readable unlock condition label for any circuit in AVAILABLE_TRACKS.
   */
  fun getTrackUnlockCondition(trackId: String): String {
    return when (trackId) {
      "track_neo_tokyo" -> "Starter Circuit • Rookie Championship (Lv.1+)"
      "track_monza" -> "Starter Circuit • Rookie Time Trial (Lv.1+)"
      "track_miami_highway" -> "Rookie Championship • Metro Sprint or Lv.1+"
      "track_suzuka" -> "Amateur Championship • Mojave Outrun or Lv.2+"
      "track_tokyo" -> "Professional Championship • Wangan Sprint or Lv.4+"
      "track_marina_bay" -> "Elite Championship • Marina Bay Sprint or Lv.6+"
      else -> "Career Championship Progression"
    }
  }

  /**
   * Checks whether a track is unlocked for the current player.
   */
  fun isTrackUnlocked(trackId: String): Boolean {
    val s = _state.value
    if (trackId in s.unlockedTrackIds) return true
    val reqTier = getTrackRequiredTier(trackId)
    return s.playerLevel >= reqTier.requiredLevel
  }

  /**
   * Unlocks a specific track ID and persists it to player progression.
   */
  fun unlockTrack(trackId: String, context: Context? = null): Boolean {
    val s = _state.value
    if (trackId in s.unlockedTrackIds) return false
    _state.value = s.copy(unlockedTrackIds = s.unlockedTrackIds + trackId)
    persistState(context)
    ApexRivals2SettingsRepository.logEvent("TRACK_UNLOCK", "Unlocked circuit: $trackId")
    return true
  }

  private fun computeAutoUnlockedTracksForLevel(level: Int, existing: Set<String>): Set<String> {
    val updated = existing.toMutableSet()
    updated.add("track_neo_tokyo")
    updated.add("track_monza")
    updated.add("track_miami_highway")
    if (level >= ChampionshipTier.AMATEUR_CHAMPIONSHIP.requiredLevel) updated.add("track_suzuka")
    if (level >= ChampionshipTier.PRO_CHAMPIONSHIP.requiredLevel) updated.add("track_tokyo")
    if (level >= ChampionshipTier.ELITE_CHAMPIONSHIP.requiredLevel) updated.add("track_marina_bay")
    return updated
  }

  /**
   * PROMPT 5: Idempotent Career Event Reward Claim.
   * Prevents duplicate reward claims if an eventId was already claimed, and unlocks associated tracks.
   */
  fun claimCareerEventReward(eventId: String, context: Context? = null): Boolean {
    val currentState = _state.value
    if (eventId in currentState.claimedEventIds) {
      return false // Already claimed — idempotent protection
    }
    val event = careerEvents.find { it.eventId == eventId } ?: return false
    val newCredits = currentState.creditsBalance + event.rewardCredits
    val newTotalEarnings = currentState.totalEarningsCredits + event.rewardCredits
    val newXp = currentState.totalXp + event.rewardXp
    val newLevel = calculateLevelFromXp(newXp)
    val updatedClaimed = currentState.claimedEventIds + eventId

    val updatedTracks = computeAutoUnlockedTracksForLevel(newLevel, currentState.unlockedTrackIds).toMutableSet()
    event.unlocksTrackId?.let { updatedTracks.add(it) }
    updatedTracks.add(event.trackId)

    _state.value = currentState.copy(
      creditsBalance = newCredits,
      totalEarningsCredits = newTotalEarnings,
      totalXp = newXp,
      playerLevel = newLevel,
      totalRacesCompleted = currentState.totalRacesCompleted + 1,
      totalVictories = currentState.totalVictories + 1,
      claimedEventIds = updatedClaimed,
      unlockedTrackIds = updatedTracks
    )
    persistState(context)
    ApexRivals2SettingsRepository.logEvent(
      "CAREER_REWARD",
      "Claimed ${event.title}: +${event.rewardCredits} CR, +${event.rewardXp} XP"
    )
    return true
  }

  /**
   * Claims the grand Championship Cup Trophy purse when a Championship Tier's events are completed
   * or when the player qualifies for the tier cup. Idempotent per ChampionshipTier.
   */
  fun claimChampionshipTierTrophy(tier: ChampionshipTier, context: Context? = null): Boolean {
    val currentState = _state.value
    if (tier.name in currentState.claimedChampionshipTiers) return false
    if (currentState.playerLevel < tier.requiredLevel) return false

    val newCredits = currentState.creditsBalance + tier.trophyRewardCredits
    val newTotalEarnings = currentState.totalEarningsCredits + tier.trophyRewardCredits
    val newXp = currentState.totalXp + tier.trophyRewardXp
    val newLevel = calculateLevelFromXp(newXp)
    val updatedTracks = computeAutoUnlockedTracksForLevel(newLevel, currentState.unlockedTrackIds) + tier.unlockedTrackId

    _state.value = currentState.copy(
      creditsBalance = newCredits,
      totalEarningsCredits = newTotalEarnings,
      totalXp = newXp,
      playerLevel = newLevel,
      claimedChampionshipTiers = currentState.claimedChampionshipTiers + tier.name,
      unlockedTrackIds = updatedTracks
    )
    persistState(context)
    ApexRivals2SettingsRepository.logEvent(
      "CHAMPIONSHIP_CUP",
      "Claimed ${tier.displayName} Trophy: +${tier.trophyRewardCredits} CR, +${tier.trophyRewardXp} XP, Unlocked ${tier.unlockedTrackName}"
    )
    return true
  }

  /**
   * Connects playable race completion in InteractiveRacingEngine directly to player career progression
   * without resetting or overwriting existing Room Database race records.
   */
  fun recordCompletedRaceResult(
    trackId: String,
    finishingPosition: Int,
    driftScore: Int,
    topSpeedKmh: Float,
    creditsEarned: Int,
    xpEarned: Int,
    context: Context? = null
  ) {
    val currentState = _state.value
    val safeCredits = creditsEarned.coerceAtLeast(250)
    val safeXp = xpEarned.coerceAtLeast(100)
    val newCredits = currentState.creditsBalance + safeCredits
    val newTotalEarnings = currentState.totalEarningsCredits + safeCredits
    val newXp = currentState.totalXp + safeXp
    val newLevel = calculateLevelFromXp(newXp)
    val isVictory = finishingPosition <= 3

    // Automatically unlock any tracks earned by reaching the new level or completing objectives
    val updatedTracks = computeAutoUnlockedTracksForLevel(newLevel, currentState.unlockedTrackIds).toMutableSet()
    updatedTracks.add(trackId)
    if (isVictory && trackId == "track_neo_tokyo") updatedTracks.add("track_miami_highway")
    if (topSpeedKmh >= 305f || (isVictory && trackId == "track_miami_highway")) updatedTracks.add("track_suzuka")
    if (driftScore >= 2400 || (isVictory && trackId == "track_suzuka")) updatedTracks.add("track_tokyo")
    if (newLevel >= 6 || (isVictory && trackId == "track_tokyo")) updatedTracks.add("track_marina_bay")

    _state.value = currentState.copy(
      creditsBalance = newCredits,
      totalEarningsCredits = newTotalEarnings,
      totalXp = newXp,
      playerLevel = newLevel,
      totalRacesCompleted = currentState.totalRacesCompleted + 1,
      totalVictories = currentState.totalVictories + (if (isVictory) 1 else 0),
      unlockedTrackIds = updatedTracks
    )
    persistState(context)
  }

  /**
   * PROMPT 6: Idempotent Mission / Achievement Reward Claim.
   */
  fun claimMissionReward(missionId: String, context: Context? = null): Boolean {
    val currentState = _state.value
    if (missionId in currentState.claimedMissionIds) {
      return false
    }
    val mission = rewardMissions.find { it.missionId == missionId } ?: return false
    val newCredits = currentState.creditsBalance + mission.rewardCredits
    val newTotalEarnings = currentState.totalEarningsCredits + mission.rewardCredits
    val newXp = currentState.totalXp + mission.rewardXp
    val newLevel = calculateLevelFromXp(newXp)

    _state.value = currentState.copy(
      creditsBalance = newCredits,
      totalEarningsCredits = newTotalEarnings,
      totalXp = newXp,
      playerLevel = newLevel,
      claimedMissionIds = currentState.claimedMissionIds + missionId,
      unlockedTrackIds = computeAutoUnlockedTracksForLevel(newLevel, currentState.unlockedTrackIds)
    )
    persistState(context)
    ApexRivals2SettingsRepository.logEvent(
      "MISSION_REWARD",
      "Claimed mission '${mission.title}': +${mission.rewardCredits} CR, +${mission.rewardXp} XP"
    )
    return true
  }

  /**
   * PROMPT 6: Daily Login Reward Claim (idempotent per day).
   */
  fun claimDailyLoginReward(context: Context? = null): Boolean {
    val currentState = _state.value
    if (currentState.isDailyLoginClaimedToday) return false

    val newStreak = currentState.dailyLoginStreakDays + 1
    val bonusCredits = 2500 + (newStreak * 500)
    val bonusXp = 400 + (newStreak * 100)
    val newXp = currentState.totalXp + bonusXp
    val newLevel = calculateLevelFromXp(newXp)

    _state.value = currentState.copy(
      creditsBalance = currentState.creditsBalance + bonusCredits,
      totalEarningsCredits = currentState.totalEarningsCredits + bonusCredits,
      totalXp = newXp,
      playerLevel = newLevel,
      dailyLoginStreakDays = newStreak,
      isDailyLoginClaimedToday = true,
      unlockedTrackIds = computeAutoUnlockedTracksForLevel(newLevel, currentState.unlockedTrackIds)
    )
    persistState(context, markDailyClaimed = true)
    ApexRivals2SettingsRepository.logEvent(
      "DAILY_LOGIN",
      "Claimed Day $newStreak login reward: +$bonusCredits CR, +$bonusXp XP"
    )
    return true
  }

  fun selectChampionshipTier(tier: ChampionshipTier) {
    _state.value = _state.value.copy(activeChampionshipTier = tier)
  }

  fun getCatalogVehiclePriceCredits(vehicleId: String): Int {
    return when (vehicleId) {
      "car_hyperion7_nitro", "car_hyper_v12", "car_gt3_apex", "car_gt3",
      "bike_superbike_1000", "bike_1000rr" -> 0
      "car_scuderia_corsa", "car_scuderia_v8" -> 54000
      "car_amg_gt_black", "bike_moto_gp" -> 48000
      "car_manthey_911_gt3" -> 42000
      "car_ts050_hybrid", "car_hyperion_7" -> 85000
      "car_classic_boss_69", "car_drift_s15" -> 32000
      "car_classic_rs_73" -> 28000
      "car_formula_apex" -> 95000
      "car_f1_hybrid" -> 75000
      else -> 35000
    }
  }

  fun isVehicleOwned(vehicleId: String): Boolean {
    return vehicleId in _state.value.ownedVehicleIds
  }

  fun purchaseVehicle(
    vehicleId: String,
    priceCredits: Int = getCatalogVehiclePriceCredits(vehicleId),
    context: Context? = null
  ): Boolean {
    val current = _state.value
    if (vehicleId in current.ownedVehicleIds) return true
    if (priceCredits < 0 || current.creditsBalance < priceCredits) return false

    _state.value = current.copy(
      creditsBalance = current.creditsBalance - priceCredits,
      ownedVehicleIds = current.ownedVehicleIds + vehicleId
    )
    persistState(context)
    ApexRivals2SettingsRepository.logEvent(
      "GARAGE_PURCHASE",
      "Purchased $vehicleId for $priceCredits CR"
    )
    return true
  }

  fun exportLoadoutsJson(): String {
    val root = org.json.JSONObject()
    _state.value.loadoutsByVehicleId.forEach { (vId, loadout) ->
      root.put(
        vId,
        org.json.JSONObject().apply {
          put("paintColorHex", loadout.paintColorHex)
          put("wheelRimStyle", loadout.wheelRimStyle.name)
          put("spoilerStyle", loadout.spoilerStyle.name)
          put("bodyKitSpec", loadout.bodyKitSpec.name)
          put("paintFinishMaterial", loadout.paintFinishMaterial.name)
          put("engineLevel", loadout.engineLevel)
          put("turboLevel", loadout.turboLevel)
          put("brakesLevel", loadout.brakesLevel)
          put("tiresLevel", loadout.tiresLevel)
        }
      )
    }
    return root.toString()
  }

  fun exportCareerProgressJson(): String {
    val s = _state.value
    return org.json.JSONObject().apply {
      put("playerId", s.playerId)
      put("playerUsername", s.playerUsername)
      put("playerLevel", s.playerLevel)
      put("totalXp", s.totalXp)
      put("creditsBalance", s.creditsBalance)
      put("coinsBalance", s.coinsBalance)
      put("totalRacesCompleted", s.totalRacesCompleted)
      put("totalVictories", s.totalVictories)
      put("activeChampionshipTier", s.activeChampionshipTier.name)
      put("claimedEventIds", org.json.JSONArray(s.claimedEventIds.toList()))
      put("claimedMissionIds", org.json.JSONArray(s.claimedMissionIds.toList()))
      put("claimedChampionshipTiers", org.json.JSONArray(s.claimedChampionshipTiers.toList()))
      put("unlockedTrackIds", org.json.JSONArray(s.unlockedTrackIds.toList()))
    }.toString()
  }

  private fun parseLoadoutsFromJsonObject(jsonStr: String?): Map<String, VehicleCustomizationLoadout> {
    if (jsonStr.isNullOrBlank() || jsonStr == "{}") return emptyMap()
    return try {
      val obj = org.json.JSONObject(jsonStr)
      val out = mutableMapOf<String, VehicleCustomizationLoadout>()
      val keys = obj.keys()
      while (keys.hasNext()) {
        val vId = keys.next()
        val item = obj.optJSONObject(vId) ?: continue
        out[vId] = VehicleCustomizationLoadout(
          vehicleId = vId,
          paintColorHex = item.optLong("paintColorHex", 0xFFFF1744L),
          wheelRimStyle = WheelRimStyle.entries.find { it.name == item.optString("wheelRimStyle") }
            ?: WheelRimStyle.STOCK_FORGED,
          spoilerStyle = SpoilerWingStyle.entries.find { it.name == item.optString("spoilerStyle") }
            ?: SpoilerWingStyle.FACTORY_LIP,
          bodyKitSpec = BodyKitSpec.entries.find { it.name == item.optString("bodyKitSpec") }
            ?: BodyKitSpec.FACTORY_SPEC,
          paintFinishMaterial = PaintFinishMaterial.entries.find { it.name == item.optString("paintFinishMaterial") }
            ?: PaintFinishMaterial.METALLIC,
          engineLevel = item.optInt("engineLevel", 1).coerceIn(1, 5),
          turboLevel = item.optInt("turboLevel", 1).coerceIn(1, 5),
          brakesLevel = item.optInt("brakesLevel", 1).coerceIn(1, 5),
          tiresLevel = item.optInt("tiresLevel", 1).coerceIn(1, 5)
        )
      }
      out
    } catch (_: Exception) {
      emptyMap()
    }
  }

  /**
   * Merges authoritative Firestore PlayerProfileModel + optional CloudSaveSnapshotModel
   * non-destructively into local state.
   */
  fun applyAuthoritativeCloudProfile(
    profile: com.example.data.firebase.PlayerProfileModel,
    cloudSave: com.example.data.firebase.CloudSaveSnapshotModel? = null,
    context: Context? = null
  ) {
    val current = _state.value
    val cloudLoadouts = parseLoadoutsFromJsonObject(cloudSave?.loadoutsJson)
    val mergedLoadouts = mutableMapOf<String, VehicleCustomizationLoadout>().apply {
      putAll(current.loadoutsByVehicleId)
      cloudLoadouts.forEach { (vId, cLoadout) ->
        val local = current.loadoutsByVehicleId[vId]
        if (local == null) {
          put(vId, cLoadout)
        } else {
          put(
            vId,
            cLoadout.copy(
              engineLevel = maxOf(local.engineLevel, cLoadout.engineLevel),
              turboLevel = maxOf(local.turboLevel, cLoadout.turboLevel),
              brakesLevel = maxOf(local.brakesLevel, cLoadout.brakesLevel),
              tiresLevel = maxOf(local.tiresLevel, cLoadout.tiresLevel)
            )
          )
        }
      }
    }

    val mergedXp = maxOf(current.totalXp, profile.xp.toInt())
    val mergedLevel = maxOf(current.playerLevel, profile.level.toInt(), calculateLevelFromXp(mergedXp))
    val mergedVehicles = (current.ownedVehicleIds + profile.ownedVehicleIds).toSet()
    val mergedTracks = computeAutoUnlockedTracksForLevel(
      mergedLevel,
      current.unlockedTrackIds + profile.unlockedTrackIds
    )
    val mergedAchievements = (current.unlockedAchievements + profile.unlockedAchievements).toSet()
    val mergedRaces = maxOf(current.totalRacesCompleted, profile.totalRaces.toInt())
    val mergedVictories = maxOf(current.totalVictories, profile.totalVictories.toInt())

    // Respect server-authoritative balances once profile has progressed on server (syncRevision > 1)
    val resolvedCredits = if (profile.syncRevision > 1L) profile.credits.toInt() else maxOf(current.creditsBalance, profile.credits.toInt())
    val resolvedCoins = if (profile.syncRevision > 1L) profile.coins.toInt() else maxOf(current.coinsBalance, profile.coins.toInt())

    _state.value = current.copy(
      playerId = profile.userId.ifBlank { current.playerId },
      playerUsername = profile.username.ifBlank { current.playerUsername },
      creditsBalance = resolvedCredits,
      coinsBalance = resolvedCoins,
      totalEarningsCredits = maxOf(current.totalEarningsCredits, resolvedCredits),
      totalXp = mergedXp,
      playerLevel = mergedLevel,
      totalRacesCompleted = mergedRaces,
      totalVictories = mergedVictories,
      dailyLoginStreakDays = maxOf(current.dailyLoginStreakDays, profile.dailyLoginStreak.toInt()),
      ownedVehicleIds = mergedVehicles,
      unlockedAchievements = mergedAchievements,
      unlockedTrackIds = mergedTracks,
      loadoutsByVehicleId = mergedLoadouts
    )
    persistState(context)
  }

  /**
   * Applies authoritative server balances/progression directly after a Cloud Function mutation
   * (e.g., purchaseVehicleOrUpgrade, claimVerifiedReward, submitCompetitiveRaceResult, rollbackToV8BackupSnapshot).
   */
  fun applyAuthoritativeBalances(
    credits: Int? = null,
    coins: Int? = null,
    xp: Int? = null,
    level: Int? = null,
    ownedVehicles: Collection<String>? = null,
    unlockedTracks: Collection<String>? = null,
    unlockedAchievements: Collection<String>? = null,
    dailyStreak: Int? = null,
    totalRaces: Int? = null,
    totalVictories: Int? = null,
    loadoutsJson: String? = null,
    context: Context? = null
  ) {
    val current = _state.value
    val nextXp = xp ?: current.totalXp
    val nextLevel = level ?: maxOf(current.playerLevel, calculateLevelFromXp(nextXp))
    val parsedLoadouts = parseLoadoutsFromJsonObject(loadoutsJson)
    _state.value = current.copy(
      creditsBalance = credits ?: current.creditsBalance,
      coinsBalance = coins ?: current.coinsBalance,
      totalEarningsCredits = if (credits != null) maxOf(current.totalEarningsCredits, credits) else current.totalEarningsCredits,
      totalXp = nextXp,
      playerLevel = nextLevel,
      ownedVehicleIds = ownedVehicles?.toSet() ?: current.ownedVehicleIds,
      unlockedTrackIds = unlockedTracks?.toSet() ?: current.unlockedTrackIds,
      unlockedAchievements = unlockedAchievements?.toSet() ?: current.unlockedAchievements,
      dailyLoginStreakDays = dailyStreak ?: current.dailyLoginStreakDays,
      totalRacesCompleted = totalRaces ?: current.totalRacesCompleted,
      totalVictories = totalVictories ?: current.totalVictories,
      loadoutsByVehicleId = if (parsedLoadouts.isNotEmpty()) parsedLoadouts else current.loadoutsByVehicleId
    )
    persistState(context)
  }

  fun unlockVehicle(vehicleId: String, context: Context? = null): Boolean {
    val currentState = _state.value
    if (vehicleId in currentState.ownedVehicleIds) return false
    _state.value = currentState.copy(ownedVehicleIds = currentState.ownedVehicleIds + vehicleId)
    persistState(context)
    return true
  }

  fun unlockAchievement(achievementId: String, context: Context? = null): Boolean {
    val currentState = _state.value
    if (achievementId in currentState.unlockedAchievements) return false
    _state.value = currentState.copy(unlockedAchievements = currentState.unlockedAchievements + achievementId)
    persistState(context)
    return true
  }

  fun resetForTesting() {
    _state.value = ApexRivals2CareerState()
  }

  private fun persistState(context: Context?, markDailyClaimed: Boolean = false) {
    if (context == null) return
    try {
      val s = _state.value
      val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
      val editor = prefs.edit()
        .putString("player_id", s.playerId)
        .putString("player_username", s.playerUsername)
        .putString("avatar_badge", s.avatarBadge)
        .putInt("coins_balance", s.coinsBalance)
        .putInt("credits_balance", s.creditsBalance)
        .putInt("total_earnings", s.totalEarningsCredits)
        .putInt("total_xp", s.totalXp)
        .putInt("player_level", s.playerLevel)
        .putInt("daily_streak", s.dailyLoginStreakDays)
        .putInt("total_races_completed", s.totalRacesCompleted)
        .putInt("total_victories", s.totalVictories)
        .putStringSet("owned_vehicle_ids", s.ownedVehicleIds)
        .putStringSet("unlocked_achievements", s.unlockedAchievements)
        .putStringSet("claimed_event_ids", s.claimedEventIds)
        .putStringSet("claimed_mission_ids", s.claimedMissionIds)
        .putStringSet("claimed_championship_tiers", s.claimedChampionshipTiers)
        .putStringSet("unlocked_track_ids", s.unlockedTrackIds)
        .putString("vehicle_loadouts_json", serializeLoadouts(s.loadoutsByVehicleId))
      if (markDailyClaimed) {
        val todayKey = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        editor.putString("last_daily_claim_date", todayKey)
      }
      editor.apply()
    } catch (_: Exception) {
      // Safe fallback
    }
  }
}
