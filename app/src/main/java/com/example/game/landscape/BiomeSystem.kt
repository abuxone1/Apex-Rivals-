package com.example.game.landscape

import androidx.compose.ui.graphics.Color

/**
 * Switchable procedural biomes, each defining distinct ground material palettes,
 * vegetation/prop weights, rock formations, and atmospheric lighting characteristics.
 */
enum class BiomeType(
  val displayName: String,
  val shortLabel: String,
  val badgeIcon: String,
  val defaultTrackIdAffinity: String
) {
  DESERT(
    displayName = "Mojave Redrock Desert",
    shortLabel = "DESERT",
    badgeIcon = "🌵",
    defaultTrackIdAffinity = "track_laguna_seca"
  ),
  FOREST(
    displayName = "Ardennes Pine Forest",
    shortLabel = "FOREST",
    badgeIcon = "🌲",
    defaultTrackIdAffinity = "track_spa"
  ),
  ALPINE(
    displayName = "Dolomites Alpine Pass",
    shortLabel = "ALPINE",
    badgeIcon = "🏔️",
    defaultTrackIdAffinity = "track_fuji"
  ),
  COASTAL(
    displayName = "Oceanic Coastal Causeway",
    shortLabel = "COASTAL",
    badgeIcon = "🌴",
    defaultTrackIdAffinity = "track_miami_highway"
  ),
  CITY_OUTSKIRTS(
    displayName = "Metro Industrial Outskirts",
    shortLabel = "OUTSKIRTS",
    badgeIcon = "🏙️",
    defaultTrackIdAffinity = "track_neo_tokyo"
  )
}

/**
 * Dynamic time-of-day states driving skybox gradients, sun/moon elevation, and ambient light.
 */
enum class TimeOfDay(
  val displayName: String,
  val sunElevationNormalized: Float,
  val ambientIntensity: Float
) {
  DAWN("Golden Dawn", 0.22f, 0.78f),
  NOON("High Noon", 0.92f, 1.00f),
  DUSK("Crimson Dusk", 0.16f, 0.72f),
  NIGHT("Floodlit Night", -0.35f, 0.44f)
}

/**
 * Dynamic weather states affecting visibility, fog density, precipitation, and wet surface grip.
 */
enum class WeatherCondition(
  val displayName: String,
  val wetGripMultiplier: Float,
  val fogDensityBonus: Float,
  val rainIntensity: Float
) {
  CLEAR("Clear Sky", 1.00f, 0.0f, 0.0f),
  RAIN("Heavy Rainstorm", 0.84f, 0.32f, 1.0f),
  FOG("Dense Valley Fog", 0.94f, 0.68f, 0.15f)
}

/**
 * Material & prop distribution profile for a single [BiomeType].
 */
data class BiomeProfile(
  val biome: BiomeType,
  val groundPrimaryHex: Long,
  val groundSecondaryHex: Long,
  val cliffRockHex: Long,
  val gravelRunOffHex: Long,
  val kerbAccentHex: Long,
  val distantPeakHex: Long,
  val nearRidgeHex: Long,
  val baseFogDensity: Float,
  val treeWeight: Float,
  val grassWeight: Float,
  val rockWeight: Float,
  val buildingWeight: Float,
  val grandstandWeight: Float,
  val roughnessAmplitude: Float
) {
  val groundPrimaryColor: Color get() = Color(groundPrimaryHex)
  val groundSecondaryColor: Color get() = Color(groundSecondaryHex)
  val cliffRockColor: Color get() = Color(cliffRockHex)
  val gravelRunOffColor: Color get() = Color(gravelRunOffHex)
  val kerbAccentColor: Color get() = Color(kerbAccentHex)
  val distantPeakColor: Color get() = Color(distantPeakHex)
  val nearRidgeColor: Color get() = Color(nearRidgeHex)
}

/**
 * Resolved per-frame atmospheric and lighting state combining [BiomeProfile], [TimeOfDay], and [WeatherCondition].
 */
data class AtmosphereState(
  val biome: BiomeType,
  val timeOfDay: TimeOfDay,
  val weather: WeatherCondition,
  val skyTopHex: Long,
  val skyMidHex: Long,
  val skyBottomHex: Long,
  val horizonGlowHex: Long,
  val fogHex: Long,
  val fogDensity: Float,
  val ambientLightMultiplier: Float,
  val celestialBodyHex: Long,
  val celestialElevation: Float,
  val rainStreakIntensity: Float
) {
  val skyTopColor: Color get() = Color(skyTopHex)
  val skyMidColor: Color get() = Color(skyMidHex)
  val skyBottomColor: Color get() = Color(skyBottomHex)
  val horizonGlowColor: Color get() = Color(horizonGlowHex)
  val fogColor: Color get() = Color(fogHex)
  val celestialBodyColor: Color get() = Color(celestialBodyHex)
}

/**
 * Modular interface for biome and atmosphere lookup so the biome system can be swapped independently.
 */
interface BiomeProvider {
  fun getProfile(biome: BiomeType): BiomeProfile
  fun resolveAtmosphere(
    biome: BiomeType,
    timeOfDay: TimeOfDay,
    weather: WeatherCondition
  ): AtmosphereState
}

/**
 * Production implementation of [BiomeProvider] supporting all five required racing biomes.
 */
class DefaultBiomeSystem : BiomeProvider {

  private val profiles: Map<BiomeType, BiomeProfile> = mapOf(
    BiomeType.DESERT to BiomeProfile(
      biome = BiomeType.DESERT,
      groundPrimaryHex = 0xFF3E2723,
      groundSecondaryHex = 0xFF4E342E,
      cliffRockHex = 0xFF8D4E38,
      gravelRunOffHex = 0xFF6D4C41,
      kerbAccentHex = 0xFFFF6D00,
      distantPeakHex = 0xFF5D3A2E,
      nearRidgeHex = 0xFF3E251D,
      baseFogDensity = 0.14f,
      treeWeight = 0.28f, // Saguaro cacti & scrub
      grassWeight = 0.35f,
      rockWeight = 0.85f,
      buildingWeight = 0.05f,
      grandstandWeight = 0.30f,
      roughnessAmplitude = 1.15f
    ),
    BiomeType.FOREST to BiomeProfile(
      biome = BiomeType.FOREST,
      groundPrimaryHex = 0xFF102418,
      groundSecondaryHex = 0xFF163020,
      cliffRockHex = 0xFF37474F,
      gravelRunOffHex = 0xFF455A64,
      kerbAccentHex = 0xFFE53935,
      distantPeakHex = 0xFF1B3028,
      nearRidgeHex = 0xFF0E1D15,
      baseFogDensity = 0.18f,
      treeWeight = 0.95f,
      grassWeight = 0.90f,
      rockWeight = 0.45f,
      buildingWeight = 0.05f,
      grandstandWeight = 0.35f,
      roughnessAmplitude = 1.0f
    ),
    BiomeType.ALPINE to BiomeProfile(
      biome = BiomeType.ALPINE,
      groundPrimaryHex = 0xFF263238,
      groundSecondaryHex = 0xFF37474F,
      cliffRockHex = 0xFF90A4AE,
      gravelRunOffHex = 0xFF546E7A,
      kerbAccentHex = 0xFFFF1744,
      distantPeakHex = 0xFFCFD8DC,
      nearRidgeHex = 0xFF455A64,
      baseFogDensity = 0.22f,
      treeWeight = 0.65f,
      grassWeight = 0.40f,
      rockWeight = 0.92f,
      buildingWeight = 0.08f,
      grandstandWeight = 0.28f,
      roughnessAmplitude = 1.55f
    ),
    BiomeType.COASTAL to BiomeProfile(
      biome = BiomeType.COASTAL,
      groundPrimaryHex = 0xFF122824,
      groundSecondaryHex = 0xFF1B3B36,
      cliffRockHex = 0xFF4DB6AC,
      gravelRunOffHex = 0xFF5D534A,
      kerbAccentHex = 0xFF00E5FF,
      distantPeakHex = 0xFF1A3A4A,
      nearRidgeHex = 0xFF0F242E,
      baseFogDensity = 0.12f,
      treeWeight = 0.75f, // Coastal palms
      grassWeight = 0.70f,
      rockWeight = 0.50f,
      buildingWeight = 0.25f,
      grandstandWeight = 0.45f,
      roughnessAmplitude = 0.75f
    ),
    BiomeType.CITY_OUTSKIRTS to BiomeProfile(
      biome = BiomeType.CITY_OUTSKIRTS,
      groundPrimaryHex = 0xFF161920,
      groundSecondaryHex = 0xFF1E222B,
      cliffRockHex = 0xFF37404E,
      gravelRunOffHex = 0xFF2C323D,
      kerbAccentHex = 0xFFFF1744,
      distantPeakHex = 0xFF1C2230,
      nearRidgeHex = 0xFF11151E,
      baseFogDensity = 0.16f,
      treeWeight = 0.25f,
      grassWeight = 0.30f,
      rockWeight = 0.20f,
      buildingWeight = 0.92f,
      grandstandWeight = 0.75f,
      roughnessAmplitude = 0.45f
    )
  )

  override fun getProfile(biome: BiomeType): BiomeProfile {
    return profiles[biome] ?: profiles.getValue(BiomeType.FOREST)
  }

  override fun resolveAtmosphere(
    biome: BiomeType,
    timeOfDay: TimeOfDay,
    weather: WeatherCondition
  ): AtmosphereState {
    val profile = getProfile(biome)
    val totalFog = (profile.baseFogDensity + weather.fogDensityBonus).coerceIn(0f, 0.92f)

    val (topHex, midHex, botHex, horizonHex) = when (timeOfDay) {
      TimeOfDay.DAWN -> when (biome) {
        BiomeType.DESERT -> listOf(0xFF1E1028L, 0xFF5A2438L, 0xFFB8543AL, 0xFFFFAB73L)
        BiomeType.ALPINE -> listOf(0xFF141E30L, 0xFF2C3E55L, 0xFF6A859CL, 0xFFF4A261L)
        else -> listOf(0xFF18142CL, 0xFF432344L, 0xFF8E3B46L, 0xFFE07A5FL)
      }
      TimeOfDay.NOON -> when (biome) {
        BiomeType.DESERT -> listOf(0xFF0D2B45L, 0xFF204E6EL, 0xFF548CA8L, 0xFFD9CAB3L)
        BiomeType.ALPINE -> listOf(0xFF0A192FL, 0xFF173A5EL, 0xFF3B6998L, 0xFFCFD8DCL)
        BiomeType.COASTAL -> listOf(0xFF05294AL, 0xFF0D5C75L, 0xFF1995ADL, 0xFFA1D6E2L)
        else -> listOf(0xFF0F2032L, 0xFF1E3C58L, 0xFF3A6073L, 0xFF8FA8B8L)
      }
      TimeOfDay.DUSK -> listOf(0xFF190B28L, 0xFF4A153BL, 0xFF8F2436L, 0xFFFF5722L)
      TimeOfDay.NIGHT -> listOf(0xFF04060EL, 0xFF0A1022L, 0xFF121E38L, 0xFF1C2D4FL)
    }

    // Overcast / fog desaturation blend when Rain or Fog is active
    val weatherOvercastHex = when (weather) {
      WeatherCondition.CLEAR -> horizonHex
      WeatherCondition.RAIN -> 0xFF263238L
      WeatherCondition.FOG -> 0xFF78909CL
    }

    val effectiveHorizonHex = if (weather == WeatherCondition.CLEAR) horizonHex else weatherOvercastHex
    val celestialHex = when (timeOfDay) {
      TimeOfDay.DAWN -> 0xFFFFB74DL
      TimeOfDay.NOON -> 0xFFFFFDE7L
      TimeOfDay.DUSK -> 0xFFFF5252L
      TimeOfDay.NIGHT -> 0xFFB0BEC5L
    }

    val weatherLightFactor = when (weather) {
      WeatherCondition.CLEAR -> 1.0f
      WeatherCondition.RAIN -> 0.72f
      WeatherCondition.FOG -> 0.82f
    }

    return AtmosphereState(
      biome = biome,
      timeOfDay = timeOfDay,
      weather = weather,
      skyTopHex = if (weather == WeatherCondition.RAIN) 0xFF0B1016L else topHex,
      skyMidHex = if (weather == WeatherCondition.RAIN) 0xFF15202BL else midHex,
      skyBottomHex = if (weather == WeatherCondition.FOG) 0xFF455A64L else botHex,
      horizonGlowHex = effectiveHorizonHex,
      fogHex = weatherOvercastHex,
      fogDensity = totalFog,
      ambientLightMultiplier = (timeOfDay.ambientIntensity * weatherLightFactor).coerceIn(0.25f, 1.0f),
      celestialBodyHex = celestialHex,
      celestialElevation = timeOfDay.sunElevationNormalized,
      rainStreakIntensity = weather.rainIntensity
    )
  }
}
