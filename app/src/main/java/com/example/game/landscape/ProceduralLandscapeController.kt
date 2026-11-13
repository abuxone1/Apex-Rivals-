package com.example.game.landscape

import com.example.game.model.RoadSegment
import com.example.model.VehicleType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlin.math.floor

/**
 * Immutable configuration state for the procedural landscaping system.
 */
data class LandscapeConfig(
  val seed: Long = 20260409L,
  val biome: BiomeType = BiomeType.FOREST,
  val sceneryDensity: Float = 1.0f, // 0.25f .. 2.0f
  val timeOfDay: TimeOfDay = TimeOfDay.DUSK,
  val weather: WeatherCondition = WeatherCondition.CLEAR,
  val vehicleType: VehicleType = VehicleType.CAR,
  val chunkSizeMeters: Float = 100f,
  val activeChunkRadius: Int = 3, // Streams chunks behind/ahead of player
  val maxDrawDistanceMeters: Float = 245f
)

/**
 * Live runtime diagnostics & physics readout exposed to the HUD and Landscape Editor/Debug Tool.
 */
data class LandscapeTelemetry(
  val activeChunkIndices: List<Int> = emptyList(),
  val lod0Count: Int = 0,
  val lod1Count: Int = 0,
  val lod2Count: Int = 0,
  val culledCount: Int = 0,
  val activeBatchCount: Int = 0,
  val currentSurface: SurfaceType = SurfaceType.ROAD,
  val effectiveSurfaceGrip: Float = 1.0f,
  val currentElevationMeters: Float = 0f,
  val currentBankingDegrees: Float = 0f,
  val currentFeatureZone: TerrainFeatureZone = TerrainFeatureZone.ROLLING_HILLS,
  val safetyCorridorNormalized: Float = 1.56f
)

/**
 * Snapshot used by `RacingGameCanvas` to render the current frame without locking or allocating.
 */
data class ActiveLandscapeFrameState(
  val config: LandscapeConfig,
  val biomeProfile: BiomeProfile,
  val atmosphere: AtmosphereState,
  val vehicleProfile: VehicleTrackProfile,
  val activeChunks: List<TerrainChunk>,
  val instancedBatches: List<SceneryBatch>,
  val telemetry: LandscapeTelemetry
)

/**
 * Coordinator facade connecting the independently swappable [BiomeProvider],
 * [TerrainHeightmapGenerator], and [SceneryPlacementEngine] modules.
 */
class ProceduralLandscapeController(
  private var biomeProvider: BiomeProvider = DefaultBiomeSystem(),
  private var sceneryEngine: SceneryPlacementEngine = RuleBasedSceneryInstancer(),
  initialConfig: LandscapeConfig = LandscapeConfig()
) {

  private val _config = MutableStateFlow(initialConfig)
  val config: StateFlow<LandscapeConfig> = _config.asStateFlow()

  private var terrainGenerator: TerrainHeightmapGenerator = SeededTerrainGenerator(initialConfig.seed)

  // Streamed chunk cache keyed by chunkIndex
  private val chunkCache = LinkedHashMap<Int, TerrainChunk>(16, 0.75f, true)
  private val maxCachedChunks = 12

  private val _telemetry = MutableStateFlow(LandscapeTelemetry())
  val telemetry: StateFlow<LandscapeTelemetry> = _telemetry.asStateFlow()

  @Volatile
  var currentFrameState: ActiveLandscapeFrameState = buildInitialFrameState(initialConfig)
    private set

  /**
   * Swaps the underlying [BiomeProvider], [TerrainHeightmapGenerator], or [SceneryPlacementEngine]
   * at runtime while keeping all other modules untouched.
   */
  fun swapModules(
    newBiomeProvider: BiomeProvider = this.biomeProvider,
    newTerrainGenerator: TerrainHeightmapGenerator = this.terrainGenerator,
    newSceneryEngine: SceneryPlacementEngine = this.sceneryEngine
  ) {
    this.biomeProvider = newBiomeProvider
    this.terrainGenerator = newTerrainGenerator
    this.sceneryEngine = newSceneryEngine
    chunkCache.clear()
  }

  fun setSeed(newSeed: Long, roadSegments: List<RoadSegment>, trackLengthMeters: Float, playerDistanceMeters: Float) {
    terrainGenerator = SeededTerrainGenerator(newSeed)
    _config.update { it.copy(seed = newSeed) }
    regenerate(roadSegments, trackLengthMeters, playerDistanceMeters)
  }

  fun setBiome(biome: BiomeType, roadSegments: List<RoadSegment>, trackLengthMeters: Float, playerDistanceMeters: Float) {
    _config.update { it.copy(biome = biome) }
    regenerate(roadSegments, trackLengthMeters, playerDistanceMeters)
  }

  fun setSceneryDensity(density: Float, roadSegments: List<RoadSegment>, trackLengthMeters: Float, playerDistanceMeters: Float) {
    _config.update { it.copy(sceneryDensity = density.coerceIn(0.25f, 2.0f)) }
    regenerate(roadSegments, trackLengthMeters, playerDistanceMeters)
  }

  fun setTimeOfDay(timeOfDay: TimeOfDay, roadSegments: List<RoadSegment>, trackLengthMeters: Float, playerDistanceMeters: Float) {
    _config.update { it.copy(timeOfDay = timeOfDay) }
    updateStreamingAndPhysics(
      playerDistanceMeters = playerDistanceMeters,
      playerLateralNormalized = 0f,
      trackLengthMeters = trackLengthMeters,
      roadSegments = roadSegments
    )
  }

  fun setWeather(weather: WeatherCondition, roadSegments: List<RoadSegment>, trackLengthMeters: Float, playerDistanceMeters: Float) {
    _config.update { it.copy(weather = weather) }
    updateStreamingAndPhysics(
      playerDistanceMeters = playerDistanceMeters,
      playerLateralNormalized = 0f,
      trackLengthMeters = trackLengthMeters,
      roadSegments = roadSegments
    )
  }

  fun setVehicleType(vehicleType: VehicleType, roadSegments: List<RoadSegment>, trackLengthMeters: Float, playerDistanceMeters: Float) {
    if (_config.value.vehicleType != vehicleType) {
      _config.update { it.copy(vehicleType = vehicleType) }
      regenerate(roadSegments, trackLengthMeters, playerDistanceMeters)
    }
  }

  /**
   * Clears streamed chunks and rebuilds the procedural world around the player.
   */
  fun regenerate(
    roadSegments: List<RoadSegment>,
    trackLengthMeters: Float,
    playerDistanceMeters: Float,
    playerLateralNormalized: Float = 0f
  ) {
    chunkCache.clear()
    terrainGenerator = SeededTerrainGenerator(_config.value.seed)
    updateStreamingAndPhysics(
      playerDistanceMeters = playerDistanceMeters,
      playerLateralNormalized = playerLateralNormalized,
      trackLengthMeters = trackLengthMeters,
      roadSegments = roadSegments
    )
  }

  /**
   * Streams active chunks around `playerDistanceMeters`, batches LOD scenery, and resolves
   * the current tire surface physics (`ROAD`, `KERB`, `GRAVEL`, `GRASS`).
   */
  fun updateStreamingAndPhysics(
    playerDistanceMeters: Float,
    playerLateralNormalized: Float,
    trackLengthMeters: Float,
    roadSegments: List<RoadSegment>
  ): SurfaceType {
    val cfg = _config.value
    val biomeProfile = biomeProvider.getProfile(cfg.biome)
    val atmosphere = biomeProvider.resolveAtmosphere(cfg.biome, cfg.timeOfDay, cfg.weather)
    val vehicleProfile = VehicleTrackProfile.forVehicle(cfg.vehicleType)

    val safeTrackLen = trackLengthMeters.coerceAtLeast(600f)
    val totalChunksOnTrack = (safeTrackLen / cfg.chunkSizeMeters).toInt().coerceAtLeast(4)
    val currentChunkIdx = floor(playerDistanceMeters / cfg.chunkSizeMeters).toInt().mod(totalChunksOnTrack)

    val activeChunks = ArrayList<TerrainChunk>(cfg.activeChunkRadius + 1)
    val activeIndices = ArrayList<Int>(cfg.activeChunkRadius + 1)

    for (offset in 0..cfg.activeChunkRadius) {
      val chunkIdx = (currentChunkIdx + offset).mod(totalChunksOnTrack)
      activeIndices.add(chunkIdx)
      val chunk = chunkCache.getOrPut(chunkIdx) {
        sceneryEngine.generateChunk(
          chunkIndex = chunkIdx,
          chunkSizeMeters = cfg.chunkSizeMeters,
          seed = cfg.seed,
          densityMultiplier = cfg.sceneryDensity,
          biomeProfile = biomeProfile,
          vehicleProfile = vehicleProfile,
          terrainGenerator = terrainGenerator,
          roadSegments = roadSegments
        )
      }
      activeChunks.add(chunk)
    }

    // Evict oldest chunks beyond cache budget
    while (chunkCache.size > maxCachedChunks) {
      val eldestKey = chunkCache.keys.firstOrNull() ?: break
      if (eldestKey !in activeIndices) {
        chunkCache.remove(eldestKey)
      } else {
        break
      }
    }

    val batches = sceneryEngine.buildInstancedBatches(
      activeChunks = activeChunks,
      playerDistanceMeters = playerDistanceMeters,
      trackLengthMeters = safeTrackLen,
      maxDrawDistanceMeters = cfg.maxDrawDistanceMeters
    )

    var lod0 = 0
    var lod1 = 0
    var lod2 = 0
    val totalInstancesInActiveChunks = activeChunks.sumOf { it.sceneryInstances.size }
    for (b in batches) {
      when (b.lodLevel) {
        LodLevel.LOD0_HIGH -> lod0 += b.instances.size
        LodLevel.LOD1_MEDIUM -> lod1 += b.instances.size
        LodLevel.LOD2_BILLBOARD -> lod2 += b.instances.size
        LodLevel.CULLED -> Unit
      }
    }
    val visibleTotal = lod0 + lod1 + lod2
    val culled = (totalInstancesInActiveChunks - visibleTotal).coerceAtLeast(0)

    val segCount = roadSegments.size.coerceAtLeast(1)
    val segIdx = ((playerDistanceMeters / 60f).toInt()).mod(segCount)
    val seg = if (roadSegments.isNotEmpty()) roadSegments[segIdx] else RoadSegment(0, 0f, 0f)

    val surface = terrainGenerator.resolveSurfaceType(playerLateralNormalized, vehicleProfile)
    val effectiveGrip = (surface.gripMultiplier * cfg.weather.wetGripMultiplier).coerceIn(0.32f, 1.15f)
    val currentHeight = terrainGenerator.sampleConformingHeight(
      distanceMeters = playerDistanceMeters,
      lateralNormalized = playerLateralNormalized,
      roadElevationMeters = seg.elevation * 8f,
      roadCurvature = seg.curvature,
      vehicleProfile = vehicleProfile,
      roughnessScale = biomeProfile.roughnessAmplitude
    )

    val newTelemetry = LandscapeTelemetry(
      activeChunkIndices = activeIndices,
      lod0Count = lod0,
      lod1Count = lod1,
      lod2Count = lod2,
      culledCount = culled,
      activeBatchCount = batches.size,
      currentSurface = surface,
      effectiveSurfaceGrip = effectiveGrip,
      currentElevationMeters = currentHeight,
      currentBankingDegrees = seg.curvature * vehicleProfile.cornerBankingMultiplierDeg,
      currentFeatureZone = terrainGenerator.classifyFeatureZone(playerDistanceMeters),
      safetyCorridorNormalized = vehicleProfile.safetyCorridorNormalized
    )
    _telemetry.value = newTelemetry

    currentFrameState = ActiveLandscapeFrameState(
      config = cfg,
      biomeProfile = biomeProfile,
      atmosphere = atmosphere,
      vehicleProfile = vehicleProfile,
      activeChunks = activeChunks,
      instancedBatches = batches,
      telemetry = newTelemetry
    )

    return surface
  }

  fun sampleConformingTerrainHeight(
    distanceMeters: Float,
    lateralNormalized: Float,
    roadElevationMeters: Float,
    roadCurvature: Float
  ): Float {
    val state = currentFrameState
    return terrainGenerator.sampleConformingHeight(
      distanceMeters = distanceMeters,
      lateralNormalized = lateralNormalized,
      roadElevationMeters = roadElevationMeters,
      roadCurvature = roadCurvature,
      vehicleProfile = state.vehicleProfile,
      roughnessScale = state.biomeProfile.roughnessAmplitude
    )
  }

  private fun buildInitialFrameState(cfg: LandscapeConfig): ActiveLandscapeFrameState {
    val profile = biomeProvider.getProfile(cfg.biome)
    val atmosphere = biomeProvider.resolveAtmosphere(cfg.biome, cfg.timeOfDay, cfg.weather)
    val vehicleProfile = VehicleTrackProfile.forVehicle(cfg.vehicleType)
    return ActiveLandscapeFrameState(
      config = cfg,
      biomeProfile = profile,
      atmosphere = atmosphere,
      vehicleProfile = vehicleProfile,
      activeChunks = emptyList(),
      instancedBatches = emptyList(),
      telemetry = LandscapeTelemetry(
        safetyCorridorNormalized = vehicleProfile.safetyCorridorNormalized
      )
    )
  }
}
