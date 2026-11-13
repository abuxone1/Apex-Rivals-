package com.example.game.landscape

import com.example.game.model.RoadSegment
import kotlin.math.abs

/**
 * Scenery prop categories placed procedurally by biome affinity, distance from track, and slope limits.
 */
enum class SceneryPropType(
  val displayName: String,
  val minSlopeDeg: Float,
  val maxSlopeDeg: Float,
  val isLowProfileMotorcycleSafe: Boolean,
  val isCollisionHazard: Boolean,
  val baseHeightMeters: Float
) {
  TREE_PINE(
    displayName = "Alpine/Ardennes Pine",
    minSlopeDeg = 0f,
    maxSlopeDeg = 36f,
    isLowProfileMotorcycleSafe = false,
    isCollisionHazard = true,
    baseHeightMeters = 9.5f
  ),
  TREE_PALM(
    displayName = "Coastal Queen Palm",
    minSlopeDeg = 0f,
    maxSlopeDeg = 24f,
    isLowProfileMotorcycleSafe = false,
    isCollisionHazard = true,
    baseHeightMeters = 8.8f
  ),
  CACTUS_SAGUARO(
    displayName = "Mojave Saguaro Cactus",
    minSlopeDeg = 0f,
    maxSlopeDeg = 30f,
    isLowProfileMotorcycleSafe = false,
    isCollisionHazard = true,
    baseHeightMeters = 6.2f
  ),
  GRASS_TUFT(
    displayName = "Verge Grass / Scrub Cluster",
    minSlopeDeg = 0f,
    maxSlopeDeg = 42f,
    isLowProfileMotorcycleSafe = true,
    isCollisionHazard = false,
    baseHeightMeters = 0.65f
  ),
  ROCK_BOULDER(
    displayName = "Outcrop Boulder",
    minSlopeDeg = 6f,
    maxSlopeDeg = 62f,
    isLowProfileMotorcycleSafe = false,
    isCollisionHazard = true,
    baseHeightMeters = 3.4f
  ),
  SAFETY_BARRIER(
    displayName = "FIA Armco / FIM Air-Fence Barrier",
    minSlopeDeg = 0f,
    maxSlopeDeg = 45f,
    isLowProfileMotorcycleSafe = true,
    isCollisionHazard = true,
    baseHeightMeters = 1.15f
  ),
  BRAKING_SIGN(
    displayName = "Braking Distance Marker (200m/100m)",
    minSlopeDeg = 0f,
    maxSlopeDeg = 30f,
    isLowProfileMotorcycleSafe = true,
    isCollisionHazard = false,
    baseHeightMeters = 1.6f
  ),
  GRANDSTAND(
    displayName = "Circuit Spectator Grandstand",
    minSlopeDeg = 0f,
    maxSlopeDeg = 12f,
    isLowProfileMotorcycleSafe = false,
    isCollisionHazard = true,
    baseHeightMeters = 12.0f
  ),
  CITY_BUILDING(
    displayName = "Outskirts Industrial / Metro Structure",
    minSlopeDeg = 0f,
    maxSlopeDeg = 10f,
    isLowProfileMotorcycleSafe = false,
    isCollisionHazard = true,
    baseHeightMeters = 18.0f
  )
}

/**
 * Mesh/Primitive Level-of-Detail tiers based on distance ahead of the player camera.
 */
enum class LodLevel(
  val maxDistanceMeters: Float,
  val detailScale: Float
) {
  LOD0_HIGH(75f, 1.0f),       // Full multi-part silhouette + specular highlights + shadow
  LOD1_MEDIUM(155f, 0.65f),   // Simplified 2-primitive geometry, no ground shadow
  LOD2_BILLBOARD(245f, 0.35f),// Single lightweight impostor/billboard shape
  CULLED(Float.MAX_VALUE, 0f) // Beyond draw distance or behind camera
}

/**
 * Single placed scenery instance inside a streamed [TerrainChunk].
 */
data class SceneryInstance(
  val id: Int,
  val propType: SceneryPropType,
  val zMeters: Float,
  val lateralNormalized: Float, // Negative = left of track, Positive = right of track
  val elevationMeters: Float,
  val scale: Float,
  val slopeDegrees: Float,
  val tintVariant: Float
)

/**
 * Instanced draw batch grouping visible instances by `(SceneryPropType, LodLevel)`
 * so the renderer can batch state and minimize per-frame overhead.
 */
data class SceneryBatch(
  val propType: SceneryPropType,
  val lodLevel: LodLevel,
  val instances: List<VisibleSceneryInstance>
)

/**
 * Camera-relative transformed scenery instance ready for batched drawing.
 */
data class VisibleSceneryInstance(
  val instance: SceneryInstance,
  val distanceAheadMeters: Float,
  val lodLevel: LodLevel
)

/**
 * Streamed world chunk covering `[startMeters, endMeters)` along the circuit.
 */
data class TerrainChunk(
  val chunkIndex: Int,
  val startMeters: Float,
  val endMeters: Float,
  val slices: List<TerrainSliceSample>,
  val sceneryInstances: List<SceneryInstance>
)

/**
 * Modular interface for rule-based scenery placement, LOD classification, and batching.
 */
interface SceneryPlacementEngine {
  fun generateChunk(
    chunkIndex: Int,
    chunkSizeMeters: Float,
    seed: Long,
    densityMultiplier: Float,
    biomeProfile: BiomeProfile,
    vehicleProfile: VehicleTrackProfile,
    terrainGenerator: TerrainHeightmapGenerator,
    roadSegments: List<RoadSegment>
  ): TerrainChunk

  fun resolveLod(distanceAheadMeters: Float, maxDrawDistanceMeters: Float = 245f): LodLevel

  fun buildInstancedBatches(
    activeChunks: List<TerrainChunk>,
    playerDistanceMeters: Float,
    trackLengthMeters: Float,
    maxDrawDistanceMeters: Float = 245f
  ): List<SceneryBatch>
}

/**
 * Rule-based procedural scenery instancer enforcing:
 * 1. Track-aware safety corridor (`abs(lateralNormalized) >= vehicleProfile.safetyCorridorNormalized`).
 * 2. Vehicle-specific low-profile camera edge zone for motorcycles (`MOTORBIKE`).
 * 3. Slope limits per prop type (`minSlopeDeg..maxSlopeDeg`).
 * 4. Instanced batching by `(SceneryPropType, LodLevel)` and distance culling.
 */
class RuleBasedSceneryInstancer : SceneryPlacementEngine {

  override fun generateChunk(
    chunkIndex: Int,
    chunkSizeMeters: Float,
    seed: Long,
    densityMultiplier: Float,
    biomeProfile: BiomeProfile,
    vehicleProfile: VehicleTrackProfile,
    terrainGenerator: TerrainHeightmapGenerator,
    roadSegments: List<RoadSegment>
  ): TerrainChunk {
    val startZ = chunkIndex * chunkSizeMeters
    val endZ = startZ + chunkSizeMeters
    val segCount = roadSegments.size.coerceAtLeast(1)

    // 1. Sample 5 cross-sectional terrain slices per 100m chunk (every 20m)
    val slices = ArrayList<TerrainSliceSample>(5)
    var sliceZ = startZ
    while (sliceZ < endZ) {
      val segIdx = ((sliceZ / 60f).toInt()).mod(segCount)
      val seg = roadSegments[segIdx]
      slices.add(
        terrainGenerator.sampleSlice(
          distanceMeters = sliceZ,
          roadElevationMeters = seg.elevation * 8f,
          roadCurvature = seg.curvature,
          vehicleProfile = vehicleProfile,
          roughnessScale = biomeProfile.roughnessAmplitude
        )
      )
      sliceZ += 20f
    }

    // 2. Rule-based procedural prop placement
    val clampedDensity = densityMultiplier.coerceIn(0.2f, 2.5f)
    val candidateRows = (12 * clampedDensity).toInt().coerceIn(3, 30)
    val rowStepMeters = chunkSizeMeters / candidateRows
    val instances = ArrayList<SceneryInstance>(candidateRows * 2)

    var nextId = chunkIndex * 1000
    for (row in 0 until candidateRows) {
      val z = startZ + row * rowStepMeters + 2.5f
      val segIdx = ((z / 60f).toInt()).mod(segCount)
      val seg = roadSegments[segIdx]
      val roadElev = seg.elevation * 8f

      // Place along both Left (-1) and Right (+1) sides outside the safety zone
      for (side in intArrayOf(-1, 1)) {
        val hash = deterministicHash(seed, chunkIndex, row, side)
        val u1 = ((hash and 0xFFFFL).toFloat() / 65535f)
        val u2 = (((hash shr 16) and 0xFFFFL).toFloat() / 65535f)
        val u3 = (((hash shr 32) and 0xFFFFL).toFloat() / 65535f)

        // Enforce strict track-aware safety corridor: no collision objects inside safetyCorridorNormalized
        val minSafeLateral = vehicleProfile.safetyCorridorNormalized
        val lateralMagnitude = minSafeLateral + u1 * 2.4f
        val lateralNormalized = side * lateralMagnitude

        val elevation = terrainGenerator.sampleConformingHeight(
          distanceMeters = z,
          lateralNormalized = lateralNormalized,
          roadElevationMeters = roadElev,
          roadCurvature = seg.curvature,
          vehicleProfile = vehicleProfile,
          roughnessScale = biomeProfile.roughnessAmplitude
        )
        val slopeDeg = terrainGenerator.sampleSlopeDegrees(
          distanceMeters = z,
          lateralNormalized = lateralNormalized,
          roadElevationMeters = roadElev,
          roadCurvature = seg.curvature,
          vehicleProfile = vehicleProfile,
          roughnessScale = biomeProfile.roughnessAmplitude
        )

        val isInsideLowProfileZone = lateralMagnitude < vehicleProfile.lowProfileCameraEdgeNormalized
        val chosenProp = selectPropByRules(
          biomeProfile = biomeProfile,
          slopeDeg = slopeDeg,
          lateralMagnitude = lateralMagnitude,
          isInsideLowProfileZone = isInsideLowProfileZone,
          isBrakingZone = seg.isBrakingZone,
          selectorRand = u2
        ) ?: continue

        // Final safety corridor invariant check: never allow a collision hazard inside safetyCorridorNormalized
        if (chosenProp.isCollisionHazard && abs(lateralNormalized) < vehicleProfile.safetyCorridorNormalized) {
          continue
        }

        val scale = 0.82f + u3 * 0.42f
        instances.add(
          SceneryInstance(
            id = nextId++,
            propType = chosenProp,
            zMeters = z,
            lateralNormalized = lateralNormalized,
            elevationMeters = elevation,
            scale = scale,
            slopeDegrees = slopeDeg,
            tintVariant = u3
          )
        )
      }
    }

    return TerrainChunk(
      chunkIndex = chunkIndex,
      startMeters = startZ,
      endMeters = endZ,
      slices = slices,
      sceneryInstances = instances
    )
  }

  private fun selectPropByRules(
    biomeProfile: BiomeProfile,
    slopeDeg: Float,
    lateralMagnitude: Float,
    isInsideLowProfileZone: Boolean,
    isBrakingZone: Boolean,
    selectorRand: Float
  ): SceneryPropType? {
    // Near the inner safety edge or inside the motorcycle low-profile camera corridor:
    if (isInsideLowProfileZone) {
      return when {
        isBrakingZone && selectorRand < 0.42f -> SceneryPropType.BRAKING_SIGN
        selectorRand < 0.65f -> SceneryPropType.GRASS_TUFT
        else -> SceneryPropType.SAFETY_BARRIER
      }
    }

    // Steep slopes (> 36 deg) only support rock boulders or hardy scrub
    if (slopeDeg > 36f) {
      return if (slopeDeg <= SceneryPropType.ROCK_BOULDER.maxSlopeDeg && selectorRand < biomeProfile.rockWeight) {
        SceneryPropType.ROCK_BOULDER
      } else {
        SceneryPropType.GRASS_TUFT
      }
    }

    // Flat outer terraces (< 12 deg) can host grandstands or city outskirts buildings
    if (slopeDeg <= 12f && lateralMagnitude > 2.2f) {
      if (selectorRand < biomeProfile.buildingWeight * 0.45f) {
        return SceneryPropType.CITY_BUILDING
      }
      if (selectorRand < (biomeProfile.buildingWeight * 0.45f + biomeProfile.grandstandWeight * 0.35f)) {
        return SceneryPropType.GRANDSTAND
      }
    }

    // Mid-range vegetation & geological props by biome type
    val primaryTree = when (biomeProfile.biome) {
      BiomeType.DESERT -> SceneryPropType.CACTUS_SAGUARO
      BiomeType.COASTAL -> SceneryPropType.TREE_PALM
      BiomeType.FOREST, BiomeType.ALPINE, BiomeType.CITY_OUTSKIRTS -> SceneryPropType.TREE_PINE
    }

    return when {
      selectorRand < biomeProfile.treeWeight * 0.55f && slopeDeg <= primaryTree.maxSlopeDeg -> primaryTree
      selectorRand < (biomeProfile.treeWeight * 0.55f + biomeProfile.rockWeight * 0.30f) -> SceneryPropType.ROCK_BOULDER
      else -> SceneryPropType.GRASS_TUFT
    }
  }

  override fun resolveLod(distanceAheadMeters: Float, maxDrawDistanceMeters: Float): LodLevel {
    if (distanceAheadMeters < 1.5f || distanceAheadMeters > maxDrawDistanceMeters) {
      return LodLevel.CULLED
    }
    return when {
      distanceAheadMeters <= LodLevel.LOD0_HIGH.maxDistanceMeters -> LodLevel.LOD0_HIGH
      distanceAheadMeters <= LodLevel.LOD1_MEDIUM.maxDistanceMeters -> LodLevel.LOD1_MEDIUM
      distanceAheadMeters <= maxDrawDistanceMeters -> LodLevel.LOD2_BILLBOARD
      else -> LodLevel.CULLED
    }
  }

  override fun buildInstancedBatches(
    activeChunks: List<TerrainChunk>,
    playerDistanceMeters: Float,
    trackLengthMeters: Float,
    maxDrawDistanceMeters: Float
  ): List<SceneryBatch> {
    val safeTrackLen = trackLengthMeters.coerceAtLeast(600f)
    val bucketMap = LinkedHashMap<Pair<SceneryPropType, LodLevel>, MutableList<VisibleSceneryInstance>>()

    for (chunk in activeChunks) {
      for (inst in chunk.sceneryInstances) {
        val rawDiff = (inst.zMeters - playerDistanceMeters) % safeTrackLen
        val wrappedAhead = if (rawDiff < -safeTrackLen * 0.5f) rawDiff + safeTrackLen else rawDiff
        val lod = resolveLod(wrappedAhead, maxDrawDistanceMeters)
        if (lod == LodLevel.CULLED) continue

        val key = Pair(inst.propType, lod)
        val list = bucketMap.getOrPut(key) { ArrayList() }
        list.add(
          VisibleSceneryInstance(
            instance = inst,
            distanceAheadMeters = wrappedAhead,
            lodLevel = lod
          )
        )
      }
    }

    // Sort far-to-near within each batch for painter's algorithm ordering
    return bucketMap.map { (key, list) ->
      list.sortByDescending { it.distanceAheadMeters }
      SceneryBatch(
        propType = key.first,
        lodLevel = key.second,
        instances = list
      )
    }
  }

  private fun deterministicHash(seed: Long, chunkIndex: Int, row: Int, side: Int): Long {
    var h = seed xor (chunkIndex * 0x9E3779B97F4A7C15UL.toLong())
    h = (h xor (row * 0xBF58476D1CE4E5B9UL.toLong())) * 0x94D049BB133111EBUL.toLong()
    h = h xor (side * 0x517CC1B727220A95L)
    return h xor (h ushr 31)
  }
}
