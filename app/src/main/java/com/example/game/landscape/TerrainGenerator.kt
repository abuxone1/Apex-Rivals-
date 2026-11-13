package com.example.game.landscape

import com.example.model.VehicleType
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/**
 * Physical surface classifications feeding directly into tire grip, rolling resistance,
 * cornering scrub, and force-feedback vibration.
 */
enum class SurfaceType(
  val displayName: String,
  val gripMultiplier: Float,
  val rollingResistance: Float,
  val vibrationIntensity: Float,
  val topSpeedPenaltyKmh: Float
) {
  ROAD(
    displayName = "Tarmac Racing Line",
    gripMultiplier = 1.00f,
    rollingResistance = 1.00f,
    vibrationIntensity = 0.02f,
    topSpeedPenaltyKmh = 0f
  ),
  KERB(
    displayName = "Serrated FIA/FIM Kerb",
    gripMultiplier = 0.88f,
    rollingResistance = 1.15f,
    vibrationIntensity = 0.55f,
    topSpeedPenaltyKmh = -4f
  ),
  GRAVEL(
    displayName = "Gravel Run-Off Trap",
    gripMultiplier = 0.62f,
    rollingResistance = 2.40f,
    vibrationIntensity = 0.82f,
    topSpeedPenaltyKmh = -38f
  ),
  GRASS(
    displayName = "Natural Verge / Turf",
    gripMultiplier = 0.48f,
    rollingResistance = 2.90f,
    vibrationIntensity = 0.68f,
    topSpeedPenaltyKmh = -52f
  )
}

/**
 * Macro topographical classification produced by the low-frequency terrain octave.
 */
enum class TerrainFeatureZone {
  FLAT_VALLEY,
  ROLLING_HILLS,
  ALPINE_RIDGE
}

/**
 * Vehicle-specific track and edge engineering profile (Requirement 5):
 * - Cars (`VehicleType.CAR`): Wider high-speed run-off zones, standard stepped FIA kerbs.
 * - Motorcycles (`VehicleType.MOTORBIKE`): Smoother micro-roughness, banked corners,
 *   beveled low-profile FIM kerbs, and camera-friendly low-profile edge zones so leaning
 *   chase/cockpit cameras never clip into trackside obstacles.
 */
data class VehicleTrackProfile(
  val vehicleType: VehicleType,
  val kerbWidthNormalized: Float,
  val kerbHeightMeters: Float,
  val runOffWidthNormalized: Float,
  val safetyCorridorNormalized: Float,
  val lowProfileCameraEdgeNormalized: Float,
  val cornerBankingMultiplierDeg: Float,
  val surfaceMicroSmoothness: Float
) {
  companion object {
    fun forVehicle(type: VehicleType): VehicleTrackProfile {
      return when (type) {
        VehicleType.CAR -> VehicleTrackProfile(
          vehicleType = VehicleType.CAR,
          kerbWidthNormalized = 0.14f,
          kerbHeightMeters = 0.085f,
          runOffWidthNormalized = 0.42f, // Wider gravel run-off area for four-wheel recovery
          safetyCorridorNormalized = 1.56f,
          lowProfileCameraEdgeNormalized = 1.62f,
          cornerBankingMultiplierDeg = 5.5f,
          surfaceMicroSmoothness = 1.0f
        )
        VehicleType.MOTORBIKE -> VehicleTrackProfile(
          vehicleType = VehicleType.MOTORBIKE,
          kerbWidthNormalized = 0.12f,
          kerbHeightMeters = 0.025f, // Low-profile beveled FIM kerb safe for 36-deg lean angles
          runOffWidthNormalized = 0.32f,
          safetyCorridorNormalized = 1.48f,
          lowProfileCameraEdgeNormalized = 1.85f, // Extra low-profile camera sightline corridor
          cornerBankingMultiplierDeg = 10.5f, // Enhanced corner banking for motorcycles
          surfaceMicroSmoothness = 0.55f // Smoother surface micro-undulations for two-wheel stability
        )
      }
    }
  }
}

/**
 * Precomputed cross-sectional terrain slice at a specific track distance `zMeters`.
 */
data class TerrainSliceSample(
  val zMeters: Float,
  val roadElevationMeters: Float,
  val bankingDegrees: Float,
  val leftRidgeHeightMeters: Float,
  val rightRidgeHeightMeters: Float,
  val leftSlopeDegrees: Float,
  val rightSlopeDegrees: Float,
  val featureZone: TerrainFeatureZone
)

/**
 * Modular interface for seeded heightmap generation and surface classification.
 */
interface TerrainHeightmapGenerator {
  val seed: Long
  fun sampleRawHeight(worldX: Float, worldZ: Float, roughnessScale: Float = 1.0f): Float
  fun sampleConformingHeight(
    distanceMeters: Float,
    lateralNormalized: Float,
    roadElevationMeters: Float,
    roadCurvature: Float,
    vehicleProfile: VehicleTrackProfile,
    roughnessScale: Float = 1.0f
  ): Float
  fun sampleSlopeDegrees(
    distanceMeters: Float,
    lateralNormalized: Float,
    roadElevationMeters: Float,
    roadCurvature: Float,
    vehicleProfile: VehicleTrackProfile,
    roughnessScale: Float = 1.0f
  ): Float
  fun classifyFeatureZone(distanceMeters: Float): TerrainFeatureZone
  fun resolveSurfaceType(lateralNormalized: Float, vehicleProfile: VehicleTrackProfile): SurfaceType
  fun sampleSlice(
    distanceMeters: Float,
    roadElevationMeters: Float,
    roadCurvature: Float,
    vehicleProfile: VehicleTrackProfile,
    roughnessScale: Float
  ): TerrainSliceSample
}

/**
 * Deterministic multi-octave seeded procedural heightmap generator with smoothstep
 * track-edge blending (`3t^2 - 2t^3`) so terrain conforms seamlessly to the asphalt ribbon.
 */
class SeededTerrainGenerator(
  override val seed: Long = 20260409L
) : TerrainHeightmapGenerator {

  private val seedOffsetX = ((seed xor 0x5DEECE66DL) and 0xFFFFL).toFloat() * 0.137f
  private val seedOffsetZ = ((seed shr 16 xor 0xB504F333L) and 0xFFFFL).toFloat() * 0.173f

  override fun sampleRawHeight(worldX: Float, worldZ: Float, roughnessScale: Float): Float {
    // Macro modulation mask separating valleys, flat sections, and hills
    val macroFreq = 0.0022f
    val macroVal = valueNoise2D(worldX * macroFreq + seedOffsetX, worldZ * macroFreq + seedOffsetZ)
    val flatnessMask = when {
      macroVal < -0.25f -> -0.45f // Valley basin
      macroVal in -0.25f..0.18f -> 0.12f // Flat plateau section
      else -> (macroVal - 0.18f) * 1.85f // Rolling hills & alpine ridges
    }

    // 3-Octave Fractal Brownian Motion (fBm)
    val n1 = valueNoise2D(worldX * 0.012f + seedOffsetX, worldZ * 0.012f + seedOffsetZ) * 18.0f
    val n2 = valueNoise2D(worldX * 0.034f - seedOffsetZ, worldZ * 0.034f + seedOffsetX) * 6.5f
    val n3 = valueNoise2D(worldX * 0.085f + seedOffsetX * 2f, worldZ * 0.085f) * 2.1f

    return (n1 + n2 + n3) * flatnessMask * roughnessScale
  }

  override fun sampleConformingHeight(
    distanceMeters: Float,
    lateralNormalized: Float,
    roadElevationMeters: Float,
    roadCurvature: Float,
    vehicleProfile: VehicleTrackProfile,
    roughnessScale: Float
  ): Float {
    val absLat = abs(lateralNormalized)
    val roadEdge = 1.0f
    val kerbOuter = roadEdge + vehicleProfile.kerbWidthNormalized
    val runOffOuter = kerbOuter + vehicleProfile.runOffWidthNormalized

    // Banked corner camber across the road and run-off
    val bankingAngleRad = (roadCurvature * vehicleProfile.cornerBankingMultiplierDeg) * (Math.PI.toFloat() / 180f)
    val bankedRoadOffset = sin(bankingAngleRad) * lateralNormalized * 2.2f

    if (absLat <= roadEdge) {
      return roadElevationMeters + bankedRoadOffset
    }

    if (absLat <= kerbOuter) {
      val kerbT = ((absLat - roadEdge) / vehicleProfile.kerbWidthNormalized).coerceIn(0f, 1f)
      val kerbBump = sin(kerbT * Math.PI.toFloat()) * vehicleProfile.kerbHeightMeters
      return roadElevationMeters + bankedRoadOffset + kerbBump
    }

    // Convert normalized lateral offset into world X meters (assuming 12m road half-width)
    val worldX = lateralNormalized * 12.0f
    val naturalTerrainH = sampleRawHeight(
      worldX = worldX,
      worldZ = distanceMeters,
      roughnessScale = roughnessScale * vehicleProfile.surfaceMicroSmoothness
    )

    // Smoothstep blend (3t^2 - 2t^3) from the outer kerb edge through the run-off into natural terrain
    val blendSpan = (runOffOuter + 1.35f) - kerbOuter
    val t = ((absLat - kerbOuter) / blendSpan).coerceIn(0f, 1f)
    val smoothWeight = t * t * (3f - 2f * t)

    return (roadElevationMeters + bankedRoadOffset) * (1f - smoothWeight) +
      (roadElevationMeters + naturalTerrainH) * smoothWeight
  }

  override fun sampleSlopeDegrees(
    distanceMeters: Float,
    lateralNormalized: Float,
    roadElevationMeters: Float,
    roadCurvature: Float,
    vehicleProfile: VehicleTrackProfile,
    roughnessScale: Float
  ): Float {
    val deltaLat = 0.08f
    val h0 = sampleConformingHeight(
      distanceMeters = distanceMeters,
      lateralNormalized = lateralNormalized,
      roadElevationMeters = roadElevationMeters,
      roadCurvature = roadCurvature,
      vehicleProfile = vehicleProfile,
      roughnessScale = roughnessScale
    )
    val h1 = sampleConformingHeight(
      distanceMeters = distanceMeters,
      lateralNormalized = lateralNormalized + if (lateralNormalized >= 0f) deltaLat else -deltaLat,
      roadElevationMeters = roadElevationMeters,
      roadCurvature = roadCurvature,
      vehicleProfile = vehicleProfile,
      roughnessScale = roughnessScale
    )
    val dxMeters = deltaLat * 12.0f
    val radians = atan2(abs(h1 - h0), dxMeters)
    return (radians * (180f / Math.PI.toFloat())).coerceIn(0f, 85f)
  }

  override fun classifyFeatureZone(distanceMeters: Float): TerrainFeatureZone {
    val macroVal = valueNoise2D(seedOffsetX, distanceMeters * 0.0022f + seedOffsetZ)
    return when {
      macroVal < -0.15f -> TerrainFeatureZone.FLAT_VALLEY
      macroVal < 0.32f -> TerrainFeatureZone.ROLLING_HILLS
      else -> TerrainFeatureZone.ALPINE_RIDGE
    }
  }

  override fun resolveSurfaceType(
    lateralNormalized: Float,
    vehicleProfile: VehicleTrackProfile
  ): SurfaceType {
    val absLat = abs(lateralNormalized)
    val kerbOuter = 1.0f + vehicleProfile.kerbWidthNormalized
    val gravelOuter = kerbOuter + vehicleProfile.runOffWidthNormalized
    return when {
      absLat <= 0.84f -> SurfaceType.ROAD
      absLat <= kerbOuter -> SurfaceType.KERB
      absLat <= gravelOuter -> SurfaceType.GRAVEL
      else -> SurfaceType.GRASS
    }
  }

  override fun sampleSlice(
    distanceMeters: Float,
    roadElevationMeters: Float,
    roadCurvature: Float,
    vehicleProfile: VehicleTrackProfile,
    roughnessScale: Float
  ): TerrainSliceSample {
    val leftRidge = sampleConformingHeight(
      distanceMeters = distanceMeters,
      lateralNormalized = -2.8f,
      roadElevationMeters = roadElevationMeters,
      roadCurvature = roadCurvature,
      vehicleProfile = vehicleProfile,
      roughnessScale = roughnessScale
    )
    val rightRidge = sampleConformingHeight(
      distanceMeters = distanceMeters,
      lateralNormalized = 2.8f,
      roadElevationMeters = roadElevationMeters,
      roadCurvature = roadCurvature,
      vehicleProfile = vehicleProfile,
      roughnessScale = roughnessScale
    )
    val leftSlope = sampleSlopeDegrees(
      distanceMeters = distanceMeters,
      lateralNormalized = -2.1f,
      roadElevationMeters = roadElevationMeters,
      roadCurvature = roadCurvature,
      vehicleProfile = vehicleProfile,
      roughnessScale = roughnessScale
    )
    val rightSlope = sampleSlopeDegrees(
      distanceMeters = distanceMeters,
      lateralNormalized = 2.1f,
      roadElevationMeters = roadElevationMeters,
      roadCurvature = roadCurvature,
      vehicleProfile = vehicleProfile,
      roughnessScale = roughnessScale
    )
    return TerrainSliceSample(
      zMeters = distanceMeters,
      roadElevationMeters = roadElevationMeters,
      bankingDegrees = roadCurvature * vehicleProfile.cornerBankingMultiplierDeg,
      leftRidgeHeightMeters = leftRidge,
      rightRidgeHeightMeters = rightRidge,
      leftSlopeDegrees = leftSlope,
      rightSlopeDegrees = rightSlope,
      featureZone = classifyFeatureZone(distanceMeters)
    )
  }

  /**
   * Fast allocation-free 2D value noise in range `[-1f, +1f]`.
   */
  private fun valueNoise2D(x: Float, z: Float): Float {
    val xi = floor(x).toInt()
    val zi = floor(z).toInt()
    val xf = x - floor(x)
    val zf = z - floor(z)

    val u = xf * xf * (3f - 2f * xf)
    val v = zf * zf * (3f - 2f * zf)

    val n00 = hashGrid(xi, zi)
    val n10 = hashGrid(xi + 1, zi)
    val n01 = hashGrid(xi, zi + 1)
    val n11 = hashGrid(xi + 1, zi + 1)

    val nx0 = n00 + u * (n10 - n00)
    val nx1 = n01 + u * (n11 - n01)
    return nx0 + v * (nx1 - nx0)
  }

  private fun hashGrid(ix: Int, iz: Int): Float {
    var h = (ix * 374761393L) + (iz * 668265263L) + (seed * 1442695040888963407L)
    h = (h xor (h shr 13)) * 1274126177L
    h = h xor (h shr 16)
    val normalized = (h and 0x7FFFFFFFL).toFloat() / Int.MAX_VALUE.toFloat()
    return normalized * 2f - 1f
  }
}
