package com.example.game.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.game.engine.InteractiveRacingEngine
import com.example.game.landscape.ActiveLandscapeFrameState
import com.example.game.landscape.LodLevel
import com.example.game.landscape.SceneryBatch
import com.example.game.landscape.SceneryPropType
import com.example.game.landscape.WeatherCondition
import com.example.game.model.AirRival
import com.example.game.model.CameraPerspective
import com.example.game.model.RaceState
import com.example.game.model.RaceWeather
import com.example.game.model.RoadSegment
import com.example.model.VehicleType
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

// Motorsport Brand Palette (Racing Red, Graphite Black, Metallic Silver, Crisp White)
private val MotorsportRacingRed = Color(0xFFFF1744)
private val MotorsportCrimson = Color(0xFFB71C1C)
private val GraphiteBlack = Color(0xFF0D0F14)
private val GraphiteDark = Color(0xFF151922)
private val GraphiteCharcoal = Color(0xFF212734)
private val MetallicSilverLight = Color(0xFFECEFF1)
private val MetallicSilverMid = Color(0xFFB0BEC5)
private val MetallicSilverDark = Color(0xFF546E7A)
private val PureMotorsportWhite = Color(0xFFFAFAFA)

@Composable
fun RacingGameCanvas(
  engine: InteractiveRacingEngine,
  modifier: Modifier = Modifier
) {
  val infiniteTransition = rememberInfiniteTransition(label = "gameVisualPulse")
  val pulseAnim by infiniteTransition.animateFloat(
    initialValue = 0f,
    targetValue = 1f,
    animationSpec = infiniteRepeatable(
      animation = tween(1000, easing = LinearEasing),
      repeatMode = RepeatMode.Restart
    ),
    label = "pulseAnim"
  )

  val graphicsPreset = com.example.game.v2.ApexRivals2SettingsRepository.graphicsPreset.value
  val vehicleLoadout = com.example.game.v2.ApexRivals2CareerManager.getLoadoutForVehicle(engine.activeVehicle)

  Canvas(
    modifier = modifier
      .fillMaxSize()
      .testTag("racing_game_canvas")
  ) {
    val canvasW = size.width
    val canvasH = size.height

    val perspective = engine.cameraPerspective
    val segments = engine.getRoadSegments()
    val segCount = segments.size.coerceAtLeast(1)
    val segmentLength = 60f
    val exactSegmentPos = engine.playerTrackDistanceMeters / segmentLength
    val currentSegmentIdx = exactSegmentPos.toInt().mod(segCount)
    val nextSegmentIdx = (currentSegmentIdx + 1).mod(segCount)
    val segmentFraction = exactSegmentPos - exactSegmentPos.toInt()

    val currentSegment = segments[currentSegmentIdx]
    val nextSegment = segments[nextSegmentIdx]

    // Smoothly interpolated curvature and elevation across segment transitions
    val smoothCurvature = currentSegment.curvature +
      (nextSegment.curvature - currentSegment.curvature) * segmentFraction
    val smoothElevation = currentSegment.elevation +
      (nextSegment.elevation - currentSegment.elevation) * segmentFraction

    val curveShift = smoothCurvature * (canvasW * 0.24f)

    // Dynamic camera pitch (dive under braking, squat under throttle/nitro, plus track elevation)
    val brakePitchOffset = engine.brakeInput * 10.dp.toPx()
    val accelPitchOffset = if (engine.isNitroActive) -9.dp.toPx() else -engine.throttleInput * 4.5.dp.toPx()
    val elevationOffset = -smoothElevation * 14.dp.toPx()

    // High-speed aerodynamic micro-vibration
    val speedRatio = (engine.speedKmh / 350f).coerceIn(0f, 1.2f)
    val shakeMagnitude = if (engine.speedKmh > 220f) {
      ((engine.speedKmh - 220f) / 140f).coerceIn(0f, 1f) * 1.8.dp.toPx()
    } else {
      0f
    }
    val cameraShakeX = sin(pulseAnim * 62.83f) * shakeMagnitude
    val cameraShakeY = cos(pulseAnim * 87.96f) * shakeMagnitude

    val horizonY = (canvasH * 0.43f + brakePitchOffset + accelPitchOffset + elevationOffset + cameraShakeY)
      .coerceIn(canvasH * 0.34f, canvasH * 0.52f)

    val landscapeState = engine.landscapeController.currentFrameState
    val bankingRollDeg = smoothCurvature * landscapeState.vehicleProfile.cornerBankingMultiplierDeg * 0.25f

    // Subtle horizon roll during high-speed cornering + vehicle-specific banked corner camber
    val cameraRollDeg = ((-engine.steerInput * 1.8f * speedRatio.coerceAtLeast(0.3f)) + bankingRollDeg)
      .coerceIn(-4.2f, 4.2f)

    rotate(degrees = cameraRollDeg, pivot = Offset(canvasW * 0.5f, horizonY)) {
      // 1. BIOME SKYBOX, CELESTIAL BODY, VOLUMETRIC LIGHTING RAYS, SEEDED MOUNTAIN HEIGHTMAP & FLOODLIGHT TOWERS
      drawSkyAndEnvironment(
        w = canvasW,
        h = canvasH,
        horizonY = horizonY,
        curveShift = curveShift + cameraShakeX,
        playerLaneX = engine.playerLaneX,
        landscapeState = landscapeState,
        engine = engine,
        graphicsPreset = graphicsPreset,
        pulse = pulseAnim
      )

      // 2. MULTI-SEGMENT CURVED 3D ASPHALT ROAD, CONFORMING TERRAIN, VEHICLE RUN-OFF, TEXTURES, REFLECTIONS & KERBS
      drawPerspectiveRoad(
        canvasW = canvasW,
        canvasH = canvasH,
        horizonY = horizonY,
        playerDistance = engine.playerTrackDistanceMeters,
        playerLaneX = engine.playerLaneX,
        curveShift = curveShift + cameraShakeX,
        segments = segments,
        currentSegmentIdx = currentSegmentIdx,
        segmentFraction = segmentFraction,
        isBrakingZone = currentSegment.isBrakingZone || nextSegment.isBrakingZone,
        weather = engine.activeWeather,
        landscapeState = landscapeState,
        engine = engine,
        graphicsPreset = graphicsPreset
      )

      // 2B. INSTANCED & BATCHED PROCEDURAL SCENERY (LOD0 / LOD1 / LOD2)
      drawInstancedSceneryBatches(
        canvasW = canvasW,
        canvasH = canvasH,
        horizonY = horizonY,
        playerLaneX = engine.playerLaneX,
        curveShift = curveShift + cameraShakeX,
        landscapeState = landscapeState,
        graphicsPreset = graphicsPreset
      )

      // 3. TRACKSIDE GANTRY & BRAKING DISTANCE MARKER BOARDS
      drawTracksideAssets(
        canvasW = canvasW,
        canvasH = canvasH,
        horizonY = horizonY,
        playerDistance = engine.playerTrackDistanceMeters,
        trackLengthMeters = engine.activeCircuit.totalLengthMeters.toFloat(),
        playerLaneX = engine.playerLaneX,
        curveShift = curveShift + cameraShakeX,
        segments = segments,
        currentSegmentIdx = currentSegmentIdx,
        segmentFraction = segmentFraction,
        graphicsPreset = graphicsPreset
      )

      // 4. RIVAL AI OPPONENTS (WITH LAP WRAP-AROUND, DIRECTIONAL SHADOWS, REFLECTIONS & METALLIC SHADING)
      drawRivalVehicles(
        canvasW = canvasW,
        canvasH = canvasH,
        horizonY = horizonY,
        playerDistance = engine.playerTrackDistanceMeters,
        trackLengthMeters = engine.activeCircuit.totalLengthMeters.toFloat(),
        playerLaneX = engine.playerLaneX,
        curveShift = curveShift + cameraShakeX,
        engine = engine,
        graphicsPreset = graphicsPreset,
        pulse = pulseAnim
      )
    }

    // 5. PLAYER VEHICLE BASED ON CAMERA & VEHICLE TYPE (CAR vs. MOTORBIKE) WITH MATERIALS, ANIMATION & SMOKE
    when (perspective) {
      CameraPerspective.CHASE_CAM -> {
        drawChaseCamPlayerVehicle(
          canvasW = canvasW,
          canvasH = canvasH,
          vehicleType = engine.activeVehicle.type,
          vehicleColor = Color(vehicleLoadout.paintColorHex),
          loadout = vehicleLoadout,
          graphicsPreset = graphicsPreset,
          playerLaneX = engine.playerLaneX,
          steerInput = engine.steerInput,
          throttleInput = engine.throttleInput,
          brakeInput = engine.brakeInput,
          isHandbrakeActive = engine.isHandbrakeActive,
          isNitroActive = engine.isNitroActive,
          isDrsActive = engine.isDrsActive,
          isOffTrack = abs(engine.playerLaneX) > 1.02f,
          weather = engine.activeWeather,
          speedKmh = engine.speedKmh,
          rpm = engine.rpm,
          playerDistance = engine.playerTrackDistanceMeters,
          pulse = pulseAnim
        )
      }
      CameraPerspective.COCKPIT_CAM -> {
        drawCockpitCam(
          canvasW = canvasW,
          canvasH = canvasH,
          steerInput = engine.steerInput,
          rpm = engine.rpm,
          gear = engine.gear,
          speedKmh = engine.speedKmh,
          isDrsActive = engine.isDrsActive,
          rivals = engine.getRivalsList(),
          playerDistance = engine.playerTrackDistanceMeters,
          trackLengthMeters = engine.activeCircuit.totalLengthMeters.toFloat()
        )
      }
      CameraPerspective.HOOD_CAM -> {
        drawHoodCamOverlay(
          canvasW = canvasW,
          canvasH = canvasH,
          steerInput = engine.steerInput,
          speedKmh = engine.speedKmh,
          brakeInput = engine.brakeInput,
          isNitroActive = engine.isNitroActive,
          vehicleColor = Color(vehicleLoadout.paintColorHex),
          pulse = pulseAnim
        )
      }
    }

    // 6. SPEED WARP LINES & CORNERING G-FORCE VIGNETTE
    if (engine.isNitroActive || engine.speedKmh > 255f) {
      drawSpeedLines(canvasW, canvasH, horizonY, engine.isNitroActive, engine.speedKmh, pulseAnim)
    }

    // 7. WEATHER EFFECTS (WET STORM RAIN STREAKS, CLEAR SUNSET HEAT SHIMMER, NEON NIGHT MIST & VOLUMETRIC FOG)
    if (engine.activeWeather == RaceWeather.WET_STORM || landscapeState.atmosphere.weather == WeatherCondition.RAIN) {
      drawRainStreaks(canvasW, canvasH, graphicsPreset.weatherParticleCount, pulseAnim)
    } else if (engine.activeWeather == RaceWeather.CLEAR_SUNSET && graphicsPreset.enableDynamicReflections) {
      drawHeatShimmerHaze(canvasW, horizonY, pulseAnim)
    } else if (engine.activeWeather == RaceWeather.NEON_NIGHT) {
      drawTwilightHorizonMist(canvasW, horizonY, pulseAnim)
    }
    if (landscapeState.atmosphere.fogDensity > 0.22f) {
      drawRect(
        brush = Brush.verticalGradient(
          colors = listOf(
            landscapeState.atmosphere.fogColor.copy(alpha = landscapeState.atmosphere.fogDensity * 0.48f),
            landscapeState.atmosphere.fogColor.copy(alpha = landscapeState.atmosphere.fogDensity * 0.65f),
            Color.Transparent
          ),
          startY = horizonY * 0.45f,
          endY = horizonY + (canvasH - horizonY) * 0.58f
        ),
        topLeft = Offset(0f, horizonY * 0.45f),
        size = Size(canvasW, canvasH * 0.55f)
      )
    }

    // 8. CHECKERED VICTORY CONFETTI
    if (engine.raceState.value == RaceState.FINISHED_PODIUM) {
      drawVictoryConfetti(canvasW, canvasH, pulseAnim)
    }
  }
}

// -------------------------------------------------------------------
// 1. BIOME SKYBOX, VOLUMETRIC LIGHTING RAYS, SEEDED HEIGHTMAP HORIZON & FLOODLIGHTS
// -------------------------------------------------------------------
private fun DrawScope.drawSkyAndEnvironment(
  w: Float,
  h: Float,
  horizonY: Float,
  curveShift: Float,
  playerLaneX: Float,
  landscapeState: ActiveLandscapeFrameState,
  engine: InteractiveRacingEngine,
  graphicsPreset: com.example.game.v2.GraphicsQualityPreset,
  pulse: Float
) {
  val atmosphere = landscapeState.atmosphere
  val biomeProfile = landscapeState.biomeProfile

  // Multi-stop dynamic biome skybox gradient
  drawRect(
    brush = Brush.verticalGradient(
      colors = listOf(
        atmosphere.skyTopColor,
        atmosphere.skyMidColor,
        atmosphere.skyBottomColor,
        atmosphere.horizonGlowColor
      ),
      startY = 0f,
      endY = horizonY
    ),
    topLeft = Offset(0f, 0f),
    size = Size(w, horizonY)
  )

  // Dynamic Sun or Moon disc positioned by TimeOfDay elevation
  val celestialX = (w * 0.72f - curveShift * 0.25f).coerceIn(w * 0.1f, w * 0.9f)
  val celestialY = (horizonY * (0.78f - atmosphere.celestialElevation * 0.48f)).coerceIn(24.dp.toPx(), horizonY - 12.dp.toPx())

  // Volumetric Celestial Light Rays (scaled by graphicsPreset.lightingRayCount)
  if (graphicsPreset.lightingRayCount > 0) {
    val rayCount = graphicsPreset.lightingRayCount
    for (r in 0 until rayCount) {
      val rayAngleOffset = (r - (rayCount - 1) * 0.5f) * (w * 0.14f)
      val rayTargetX = (celestialX + rayAngleOffset).coerceIn(-w * 0.2f, w * 1.2f)
      drawLine(
        brush = Brush.verticalGradient(
          colors = listOf(
            atmosphere.celestialBodyColor.copy(alpha = 0.16f),
            atmosphere.horizonGlowColor.copy(alpha = 0.05f),
            Color.Transparent
          ),
          startY = celestialY,
          endY = horizonY
        ),
        start = Offset(celestialX, celestialY),
        end = Offset(rayTargetX, horizonY),
        strokeWidth = (18.dp.toPx() + (r % 2) * 8.dp.toPx()),
        cap = StrokeCap.Round
      )
    }
  }

  drawCircle(
    color = atmosphere.celestialBodyColor.copy(alpha = 0.24f),
    radius = 34.dp.toPx(),
    center = Offset(celestialX, celestialY)
  )
  drawCircle(
    color = atmosphere.celestialBodyColor.copy(alpha = 0.88f),
    radius = 13.dp.toPx(),
    center = Offset(celestialX, celestialY)
  )

  // Subtle racing red & silver atmospheric horizon band
  drawRect(
    brush = Brush.verticalGradient(
      colors = listOf(
        Color.Transparent,
        MotorsportRacingRed.copy(alpha = 0.12f),
        MetallicSilverMid.copy(alpha = 0.10f)
      ),
      startY = horizonY * 0.72f,
      endY = horizonY
    ),
    topLeft = Offset(0f, horizonY * 0.72f),
    size = Size(w, horizonY * 0.28f)
  )

  // Seeded Procedural Mountain Range (Layer 1 Parallax driven by terrain heightmap)
  val farParallax = -playerLaneX * 28f - curveShift * 0.32f
  val farMountainPath = Path().apply {
    moveTo(-w * 0.2f, horizonY)
    val steps = 10
    for (s in 0..steps) {
      val normX = -0.15f + (s.toFloat() / steps) * 1.3f
      val sampleH = engine.landscapeController.sampleConformingTerrainHeight(
        distanceMeters = engine.playerTrackDistanceMeters + 400f + s * 45f,
        lateralNormalized = (normX - 0.5f) * 6f,
        roadElevationMeters = 0f,
        roadCurvature = 0f
      )
      val peakPx = (32.dp.toPx() + abs(sampleH) * 2.8.dp.toPx()).coerceIn(18.dp.toPx(), 84.dp.toPx())
      lineTo(w * normX + farParallax, horizonY - peakPx)
    }
    lineTo(w * 1.2f, horizonY)
    close()
  }
  drawPath(
    path = farMountainPath,
    brush = Brush.verticalGradient(
      colors = listOf(biomeProfile.distantPeakColor, biomeProfile.nearRidgeColor),
      startY = horizonY - 82.dp.toPx(),
      endY = horizonY
    )
  )

  // Closer Biome Ridge Silhouette (Layer 2 Parallax)
  val nearParallax = -playerLaneX * 48f - curveShift * 0.55f
  val nearRidgePath = Path().apply {
    moveTo(-w * 0.2f, horizonY)
    lineTo(w * 0.18f + nearParallax, horizonY - 28.dp.toPx())
    lineTo(w * 0.38f + nearParallax, horizonY - 14.dp.toPx())
    lineTo(w * 0.62f + nearParallax, horizonY - 32.dp.toPx())
    lineTo(w * 0.82f + nearParallax, horizonY - 18.dp.toPx())
    lineTo(w * 1.2f, horizonY)
    close()
  }
  drawPath(nearRidgePath, color = biomeProfile.nearRidgeColor)

  // Circuit Stadium Floodlight Towers along the horizon + Volumetric Floodlight Cones
  val towerPositions = listOf(0.14f, 0.36f, 0.66f, 0.86f)
  towerPositions.forEachIndexed { idx, normX ->
    val tx = (w * normX + nearParallax).coerceIn(-40f, w + 40f)
    val towerH = if (idx % 2 == 0) 36.dp.toPx() else 28.dp.toPx()
    drawLine(
      color = MetallicSilverDark.copy(alpha = 0.7f),
      start = Offset(tx, horizonY),
      end = Offset(tx, horizonY - towerH),
      strokeWidth = 2.dp.toPx()
    )
    drawRect(
      color = MetallicSilverLight,
      topLeft = Offset(tx - 6.dp.toPx(), horizonY - towerH - 3.dp.toPx()),
      size = Size(12.dp.toPx(), 4.dp.toPx())
    )
    drawCircle(
      color = PureMotorsportWhite.copy(alpha = 0.20f + 0.06f * sin(pulse * 6.28f + idx)),
      radius = 15.dp.toPx(),
      center = Offset(tx, horizonY - towerH)
    )
    // Volumetric Stadium Floodlight Cone on Medium & High Graphics
    if (graphicsPreset.enableHeadlightCones) {
      val coneEndX = if (normX < 0.5f) tx + 34.dp.toPx() else tx - 34.dp.toPx()
      drawLine(
        brush = Brush.verticalGradient(
          colors = listOf(
            PureMotorsportWhite.copy(alpha = 0.14f),
            Color.Transparent
          ),
          startY = horizonY - towerH,
          endY = horizonY + 18.dp.toPx()
        ),
        start = Offset(tx, horizonY - towerH),
        end = Offset(coneEndX, horizonY + 18.dp.toPx()),
        strokeWidth = 14.dp.toPx(),
        cap = StrokeCap.Round
      )
    }
  }

  // Biome Ground Base beneath horizon
  drawRect(
    brush = Brush.verticalGradient(
      colors = listOf(
        biomeProfile.groundPrimaryColor,
        biomeProfile.groundSecondaryColor,
        GraphiteBlack
      ),
      startY = horizonY,
      endY = h
    ),
    topLeft = Offset(0f, horizonY),
    size = Size(w, h - horizonY)
  )
}

// -------------------------------------------------------------------
// 2. MULTI-SEGMENT CURVED 3D ROAD, TRACK-CONFORMING TERRAIN, TEXTURES, REFLECTIONS & RUN-OFFS
// -------------------------------------------------------------------
private fun DrawScope.drawPerspectiveRoad(
  canvasW: Float,
  canvasH: Float,
  horizonY: Float,
  playerDistance: Float,
  playerLaneX: Float,
  curveShift: Float,
  segments: List<RoadSegment>,
  currentSegmentIdx: Int,
  segmentFraction: Float,
  isBrakingZone: Boolean,
  weather: RaceWeather,
  landscapeState: ActiveLandscapeFrameState,
  engine: InteractiveRacingEngine,
  graphicsPreset: com.example.game.v2.GraphicsQualityPreset
) {
  val roadTopW = canvasW * 0.15f
  val roadBottomW = canvasW * 0.94f
  val numSlices = graphicsPreset.roadSliceCount
  val stripeOffset = (playerDistance * 0.28f)

  val vanishingX = (canvasW * 0.5f) + curveShift - (playerLaneX * canvasW * 0.12f)
  val roadBottomCenterX = (canvasW * 0.5f) - (playerLaneX * canvasW * 0.36f)

  val isWet = weather == RaceWeather.WET_STORM || landscapeState.atmosphere.weather == WeatherCondition.RAIN
  val segCount = segments.size.coerceAtLeast(1)
  val biomeProfile = landscapeState.biomeProfile
  val vehicleProfile = landscapeState.vehicleProfile
  val textureDetail = graphicsPreset.roadTextureDetailLevel
  val enableReflections = graphicsPreset.enableDynamicReflections

  for (i in 0 until numSlices) {
    val lin0 = i.toFloat() / numSlices
    val lin1 = (i + 1).toFloat() / numSlices
    val p0 = lin0 * lin0
    val p1 = lin1 * lin1

    val y0 = horizonY + (canvasH - horizonY) * p0
    val y1 = horizonY + (canvasH - horizonY) * p1

    val lookAhead0 = ((1f - lin0) * 2.5f).toInt()
    val seg0 = segments[(currentSegmentIdx + lookAhead0).mod(segCount)]
    val curveBend0 = (1f - p0) * (1f - p0) * (curveShift + seg0.curvature * canvasW * 0.08f)
    val curveBend1 = (1f - p1) * (1f - p1) * (curveShift + seg0.curvature * canvasW * 0.08f)

    val center0 = roadBottomCenterX + (vanishingX - roadBottomCenterX) * (1f - p0) + curveBend0 * 0.25f
    val center1 = roadBottomCenterX + (vanishingX - roadBottomCenterX) * (1f - p1) + curveBend1 * 0.25f

    val w0 = roadTopW + (roadBottomW - roadTopW) * p0
    val w1 = roadTopW + (roadBottomW - roadTopW) * p1

    val leftX0 = center0 - w0 * 0.5f
    val rightX0 = center0 + w0 * 0.5f
    val leftX1 = center1 - w1 * 0.5f
    val rightX1 = center1 + w1 * 0.5f

    val worldSliceIndex = (i + stripeOffset.toInt())

    // Alternating biome turf bands outside the track
    val turfColor = if (worldSliceIndex % 2 == 0) biomeProfile.groundPrimaryColor else biomeProfile.groundSecondaryColor
    drawRect(
      color = turfColor,
      topLeft = Offset(0f, y0),
      size = Size(canvasW, (y1 - y0) + 1f)
    )

    // Vehicle-Specific Gravel Run-Off Zone (wider for Cars, smooth transition for Motorcycles)
    val runOffFactor = vehicleProfile.runOffWidthNormalized * 0.65f
    val runOffW0 = w0 * runOffFactor
    val runOffW1 = w1 * runOffFactor
    val gravelColor = if (worldSliceIndex % 2 == 0) {
      biomeProfile.gravelRunOffColor
    } else {
      biomeProfile.gravelRunOffColor.copy(alpha = 0.84f)
    }

    // Left Gravel Run-Off Corridor
    drawPath(
      Path().apply {
        moveTo(leftX0 - runOffW0, y0)
        lineTo(leftX0, y0)
        lineTo(leftX1, y1)
        lineTo(leftX1 - runOffW1, y1)
        close()
      },
      color = gravelColor
    )
    // Right Gravel Run-Off Corridor
    drawPath(
      Path().apply {
        moveTo(rightX0, y0)
        lineTo(rightX0 + runOffW0, y0)
        lineTo(rightX1 + runOffW1, y1)
        lineTo(rightX1, y1)
        close()
      },
      color = gravelColor
    )

    // Track-Conforming Procedural Heightmap Embankment Slopes outside the run-off
    val sliceDist = playerDistance + (1f - lin0) * 220f
    val leftTerrainH = engine.landscapeController.sampleConformingTerrainHeight(
      distanceMeters = sliceDist,
      lateralNormalized = -2.4f,
      roadElevationMeters = seg0.elevation * 8f,
      roadCurvature = seg0.curvature
    )
    val rightTerrainH = engine.landscapeController.sampleConformingTerrainHeight(
      distanceMeters = sliceDist,
      lateralNormalized = 2.4f,
      roadElevationMeters = seg0.elevation * 8f,
      roadCurvature = seg0.curvature
    )
    val leftEmbankmentPx = (leftTerrainH.coerceAtLeast(0f) * 2.2.dp.toPx() * p1).coerceAtMost(42.dp.toPx())
    val rightEmbankmentPx = (rightTerrainH.coerceAtLeast(0f) * 2.2.dp.toPx() * p1).coerceAtMost(42.dp.toPx())

    if (leftEmbankmentPx > 1.5f) {
      drawPath(
        Path().apply {
          moveTo(0f, y0 - leftEmbankmentPx * 0.8f)
          lineTo(leftX0 - runOffW0, y0)
          lineTo(leftX1 - runOffW1, y1)
          lineTo(0f, y1 - leftEmbankmentPx)
          close()
        },
        color = biomeProfile.cliffRockColor.copy(alpha = 0.55f)
      )
    }
    if (rightEmbankmentPx > 1.5f) {
      drawPath(
        Path().apply {
          moveTo(rightX0 + runOffW0, y0)
          lineTo(canvasW, y0 - rightEmbankmentPx * 0.8f)
          lineTo(canvasW, y1 - rightEmbankmentPx)
          lineTo(rightX1 + runOffW1, y1)
          close()
        },
        color = biomeProfile.cliffRockColor.copy(alpha = 0.55f)
      )
    }

    // Safety Barriers outside the run-off verge (low-profile camera-friendly height for MOTORBIKE)
    val barrierProfileScale = if (vehicleProfile.vehicleType == VehicleType.MOTORBIKE) 0.58f else 1.0f
    val barrierH0 = (3.dp.toPx() + 12.dp.toPx() * p0) * barrierProfileScale
    val barrierH1 = (3.dp.toPx() + 12.dp.toPx() * p1) * barrierProfileScale
    val barrierColor = if (worldSliceIndex % 4 == 0) MotorsportRacingRed else MetallicSilverMid

    // Left Safety Barrier
    drawPath(
      Path().apply {
        moveTo(leftX0 - runOffW0, y0 - barrierH0)
        lineTo(leftX1 - runOffW1, y1 - barrierH1)
        lineTo(leftX1 - runOffW1, y1)
        lineTo(leftX0 - runOffW0, y0)
        close()
      },
      color = barrierColor.copy(alpha = (0.35f + 0.65f * p1).coerceIn(0f, 1f))
    )

    // Right Safety Barrier
    drawPath(
      Path().apply {
        moveTo(rightX0 + runOffW0, y0 - barrierH0)
        lineTo(rightX1 + runOffW1, y1 - barrierH1)
        lineTo(rightX1 + runOffW1, y1)
        lineTo(rightX0 + runOffW0, y0)
        close()
      },
      color = barrierColor.copy(alpha = (0.35f + 0.65f * p1).coerceIn(0f, 1f))
    )

    // Main Graphite Asphalt Slice with subtle alternating texture bands
    val baseAsphalt = if (worldSliceIndex % 2 == 0) {
      if (isWet) Color(0xFF18202C) else Color(0xFF181C24)
    } else {
      if (isWet) Color(0xFF141B26) else Color(0xFF13171E)
    }

    drawPath(
      Path().apply {
        moveTo(leftX0, y0)
        lineTo(rightX0, y0)
        lineTo(rightX1, y1)
        lineTo(leftX1, y1)
        close()
      },
      color = baseAsphalt
    )

    // High-Frequency Asphalt Micro-Grain Striation Bands (Medium & High Presets)
    if (textureDetail >= 2 && (worldSliceIndex % 3 == 0)) {
      val grainAlpha = if (textureDetail >= 3) 0.08f else 0.05f
      drawLine(
        color = MetallicSilverMid.copy(alpha = grainAlpha * p1),
        start = Offset(leftX0 + w0 * 0.14f, y0),
        end = Offset(leftX1 + w1 * 0.14f, y1),
        strokeWidth = (1.dp.toPx() + 2.dp.toPx() * p1)
      )
      drawLine(
        color = MetallicSilverMid.copy(alpha = grainAlpha * p1),
        start = Offset(rightX0 - w0 * 0.14f, y0),
        end = Offset(rightX1 - w1 * 0.14f, y1),
        strokeWidth = (1.dp.toPx() + 2.dp.toPx() * p1)
      )
    }

    // Rubbered-in dark racing groove & wet sheen highlight down the center
    val grooveW0 = w0 * 0.42f
    val grooveW1 = w1 * 0.42f
    drawPath(
      Path().apply {
        moveTo(center0 - grooveW0 * 0.5f, y0)
        lineTo(center0 + grooveW0 * 0.5f, y0)
        lineTo(center1 + grooveW1 * 0.5f, y1)
        lineTo(center1 - grooveW1 * 0.5f, y1)
        close()
      },
      color = if (isWet) MetallicSilverLight.copy(alpha = 0.09f) else GraphiteBlack.copy(alpha = 0.38f)
    )

    // Wet Road Mirror Reflections (skyline & taillight shimmer on wet or high-reflection surfaces)
    if (enableReflections && (isWet || textureDetail >= 3)) {
      val reflAlpha = if (isWet) 0.14f * p1 else 0.05f * p1
      if (worldSliceIndex % 2 == 0) {
        val puddleW0 = w0 * 0.24f
        val puddleW1 = w1 * 0.24f
        drawPath(
          Path().apply {
            moveTo(center0 - puddleW0, y0)
            lineTo(center0 + puddleW0, y0)
            lineTo(center1 + puddleW1, y1)
            lineTo(center1 - puddleW1, y1)
            close()
          },
          color = landscapeState.atmosphere.horizonGlowColor.copy(alpha = reflAlpha.coerceIn(0f, 0.22f))
        )
      }
    }

    // Braking Zone Twin Tire Skid-Mark Streaks on Tarmac
    if ((seg0.isBrakingZone || isBrakingZone) && textureDetail >= 2) {
      val skidAlpha = (0.28f + 0.30f * p1).coerceIn(0f, 0.55f)
      val skidTrackOffset0 = w0 * 0.16f
      val skidTrackOffset1 = w1 * 0.16f
      val skidW = (1.5.dp.toPx() + 3.5.dp.toPx() * p1)
      drawLine(
        color = GraphiteBlack.copy(alpha = skidAlpha),
        start = Offset(center0 - skidTrackOffset0, y0),
        end = Offset(center1 - skidTrackOffset1, y1),
        strokeWidth = skidW
      )
      drawLine(
        color = GraphiteBlack.copy(alpha = skidAlpha),
        start = Offset(center0 + skidTrackOffset0, y0),
        end = Offset(center1 + skidTrackOffset1, y1),
        strokeWidth = skidW
      )
    }

    // Crisp White Track Edge Boundary Lines
    val edgeStroke = (1.dp.toPx() + 2.5.dp.toPx() * p1)
    drawLine(
      color = PureMotorsportWhite.copy(alpha = 0.85f),
      start = Offset(leftX0, y0),
      end = Offset(leftX1, y1),
      strokeWidth = edgeStroke
    )
    drawLine(
      color = PureMotorsportWhite.copy(alpha = 0.85f),
      start = Offset(rightX0, y0),
      end = Offset(rightX1, y1),
      strokeWidth = edgeStroke
    )

    // Vehicle-Specific FIA/FIM Kerbs (wider stepped kerbs for Cars, smooth beveled kerbs for Motorcycles)
    val kerbFactor = vehicleProfile.kerbWidthNormalized * 0.62f
    val kerbW0 = w0 * kerbFactor
    val kerbW1 = w1 * kerbFactor
    val kerbColor = if (worldSliceIndex % 2 == 0) biomeProfile.kerbAccentColor else PureMotorsportWhite

    // Left Kerb
    drawPath(
      Path().apply {
        moveTo(leftX0, y0)
        lineTo(leftX0 - kerbW0, y0)
        lineTo(leftX1 - kerbW1, y1)
        lineTo(leftX1, y1)
        close()
      },
      color = kerbColor
    )

    // Right Kerb
    drawPath(
      Path().apply {
        moveTo(rightX0, y0)
        lineTo(rightX0 + kerbW0, y0)
        lineTo(rightX1 + kerbW1, y1)
        lineTo(rightX1, y1)
        close()
      },
      color = kerbColor
    )

    // Center Lane Markings (Metallic Silver / White high-speed dashes)
    if (worldSliceIndex % 2 == 0) {
      val dashW = 1.5.dp.toPx() + (5.dp.toPx() * p1)
      drawLine(
        color = MetallicSilverLight.copy(alpha = 0.78f),
        start = Offset(center0, y0),
        end = Offset(center1, y1),
        strokeWidth = dashW
      )
    }

    // Dynamic Telemetry Ideal Racing Line chevrons
    if (worldSliceIndex % 2 == 1) {
      val apexOffset0 = (curveShift / canvasW * 0.55f) * w0
      val apexOffset1 = (curveShift / canvasW * 0.55f) * w1
      val racingLineColor = if (isBrakingZone) {
        MotorsportRacingRed.copy(alpha = 0.78f)
      } else {
        Color(0xFF00E676).copy(alpha = 0.52f)
      }
      drawLine(
        color = racingLineColor,
        start = Offset(center0 + apexOffset0, y0),
        end = Offset(center1 + apexOffset1, y1),
        strokeWidth = (2.dp.toPx() + 5.dp.toPx() * p1),
        cap = StrokeCap.Round
      )
    }
  }
}

// -------------------------------------------------------------------
// 2B. INSTANCED & BATCHED PROCEDURAL SCENERY RENDERER (LOD0 / LOD1 / LOD2)
// -------------------------------------------------------------------
private fun DrawScope.drawInstancedSceneryBatches(
  canvasW: Float,
  canvasH: Float,
  horizonY: Float,
  playerLaneX: Float,
  curveShift: Float,
  landscapeState: ActiveLandscapeFrameState,
  graphicsPreset: com.example.game.v2.GraphicsQualityPreset
) {
  val roadTopW = canvasW * 0.15f
  val roadBottomW = canvasW * 0.94f
  val vanishingX = (canvasW * 0.5f) + curveShift - (playerLaneX * canvasW * 0.12f)
  val roadCenterX = (canvasW * 0.5f) - (playerLaneX * canvasW * 0.36f)
  val maxDist = (landscapeState.config.maxDrawDistanceMeters * graphicsPreset.sceneryLodDistanceMultiplier).coerceAtLeast(160f)
  val biomeProfile = landscapeState.biomeProfile

  for (batch in landscapeState.instancedBatches) {
    for (visible in batch.instances) {
      if (visible.distanceAheadMeters > maxDist) continue
      val inst = visible.instance
      val depthNorm = (1f - (visible.distanceAheadMeters / maxDist)).coerceIn(0.04f, 0.98f)
      val p = depthNorm * depthNorm

      val baseY = horizonY + (canvasH - horizonY) * (p * 0.90f)
      val roadWAtY = roadTopW + (roadBottomW - roadTopW) * p
      val centerAtY = vanishingX + (roadCenterX - vanishingX) * p
      val propX = centerAtY + (inst.lateralNormalized * roadWAtY * 0.45f)

      if (propX < -80f || propX > canvasW + 80f) continue

      val scalePx = (4.dp.toPx() + 36.dp.toPx() * p) * inst.scale
      drawBatchedSceneryProp(
        propType = batch.propType,
        lodLevel = batch.lodLevel,
        x = propX,
        y = baseY,
        scalePx = scalePx,
        cliffColor = biomeProfile.cliffRockColor,
        enableShadows = graphicsPreset.enableRealtimeShadows
      )
    }
  }
}

private fun DrawScope.drawBatchedSceneryProp(
  propType: SceneryPropType,
  lodLevel: LodLevel,
  x: Float,
  y: Float,
  scalePx: Float,
  cliffColor: Color,
  enableShadows: Boolean
) {
  if (enableShadows && lodLevel != LodLevel.LOD2_BILLBOARD) {
    drawOval(
      color = Color.Black.copy(alpha = 0.38f),
      topLeft = Offset(x - scalePx * 0.42f, y - scalePx * 0.08f),
      size = Size(scalePx * 0.95f, scalePx * 0.18f)
    )
  }
  when (propType) {
    SceneryPropType.TREE_PINE -> {
      val trunkH = scalePx * 0.35f
      val canopyH = scalePx * 1.45f
      val canopyW = scalePx * 0.85f
      if (lodLevel == LodLevel.LOD2_BILLBOARD) {
        drawRect(
          color = Color(0xFF1B4332),
          topLeft = Offset(x - canopyW * 0.35f, y - canopyH),
          size = Size(canopyW * 0.7f, canopyH)
        )
      } else {
        drawRect(
          color = Color(0xFF3E2723),
          topLeft = Offset(x - scalePx * 0.08f, y - trunkH),
          size = Size(scalePx * 0.16f, trunkH)
        )
        val tierPath = Path().apply {
          moveTo(x, y - canopyH)
          lineTo(x + canopyW * 0.5f, y - trunkH * 0.8f)
          lineTo(x - canopyW * 0.5f, y - trunkH * 0.8f)
          close()
        }
        drawPath(tierPath, color = Color(0xFF1B4332))
        if (lodLevel == LodLevel.LOD0_HIGH) {
          val upperTier = Path().apply {
            moveTo(x, y - canopyH * 1.15f)
            lineTo(x + canopyW * 0.36f, y - canopyH * 0.45f)
            lineTo(x - canopyW * 0.36f, y - canopyH * 0.45f)
            close()
          }
          drawPath(upperTier, color = Color(0xFF2D6A4F))
        }
      }
    }
    SceneryPropType.TREE_PALM -> {
      val trunkH = scalePx * 1.35f
      drawLine(
        color = Color(0xFF5D4037),
        start = Offset(x, y),
        end = Offset(x + scalePx * 0.12f, y - trunkH),
        strokeWidth = (scalePx * 0.12f).coerceAtLeast(2f)
      )
      if (lodLevel != LodLevel.LOD2_BILLBOARD) {
        drawCircle(
          color = Color(0xFF2E7D32),
          radius = scalePx * 0.48f,
          center = Offset(x + scalePx * 0.12f, y - trunkH)
        )
      }
    }
    SceneryPropType.CACTUS_SAGUARO -> {
      val h = scalePx * 1.1f
      val w = (scalePx * 0.16f).coerceAtLeast(2f)
      drawRect(
        color = Color(0xFF2E7D32),
        topLeft = Offset(x - w * 0.5f, y - h),
        size = Size(w, h)
      )
      if (lodLevel == LodLevel.LOD0_HIGH) {
        drawRect(
          color = Color(0xFF388E3C),
          topLeft = Offset(x - w * 1.8f, y - h * 0.65f),
          size = Size(w * 3.6f, w * 0.8f)
        )
      }
    }
    SceneryPropType.ROCK_BOULDER -> {
      val rw = scalePx * 0.9f
      val rh = scalePx * 0.55f
      drawRoundRect(
        color = cliffColor,
        topLeft = Offset(x - rw * 0.5f, y - rh),
        size = Size(rw, rh),
        cornerRadius = CornerRadius(rw * 0.2f, rh * 0.2f)
      )
      if (lodLevel == LodLevel.LOD0_HIGH) {
        drawRoundRect(
          color = MetallicSilverLight.copy(alpha = 0.18f),
          topLeft = Offset(x - rw * 0.38f, y - rh * 0.88f),
          size = Size(rw * 0.5f, rh * 0.28f),
          cornerRadius = CornerRadius(rw * 0.1f, rh * 0.1f)
        )
      }
    }
    SceneryPropType.GRANDSTAND -> {
      val gw = scalePx * 1.9f
      val gh = scalePx * 1.1f
      drawRect(
        color = GraphiteCharcoal,
        topLeft = Offset(x - gw * 0.5f, y - gh),
        size = Size(gw, gh)
      )
      drawRect(
        color = MotorsportRacingRed,
        topLeft = Offset(x - gw * 0.5f, y - gh),
        size = Size(gw, gh * 0.22f)
      )
      if (lodLevel != LodLevel.LOD2_BILLBOARD) {
        drawRect(
          color = PureMotorsportWhite.copy(alpha = 0.45f),
          topLeft = Offset(x - gw * 0.42f, y - gh * 0.68f),
          size = Size(gw * 0.84f, gh * 0.14f)
        )
      }
    }
    SceneryPropType.CITY_BUILDING -> {
      val bw = scalePx * 1.4f
      val bh = scalePx * 2.1f
      drawRect(
        color = GraphiteDark,
        topLeft = Offset(x - bw * 0.5f, y - bh),
        size = Size(bw, bh)
      )
      if (lodLevel != LodLevel.LOD2_BILLBOARD) {
        drawRect(
          color = MetallicSilverLight.copy(alpha = 0.38f),
          topLeft = Offset(x - bw * 0.32f, y - bh * 0.82f),
          size = Size(bw * 0.64f, bh * 0.10f)
        )
        drawRect(
          color = Color(0xFF00E5FF).copy(alpha = 0.28f),
          topLeft = Offset(x - bw * 0.32f, y - bh * 0.56f),
          size = Size(bw * 0.64f, bh * 0.08f)
        )
      }
    }
    SceneryPropType.GRASS_TUFT, SceneryPropType.SAFETY_BARRIER, SceneryPropType.BRAKING_SIGN -> {
      val gh = scalePx * 0.28f
      drawCircle(
        color = cliffColor.copy(alpha = 0.65f),
        radius = gh.coerceAtLeast(2f),
        center = Offset(x, y - gh * 0.5f)
      )
    }
  }
}

// -------------------------------------------------------------------
// 3. TRACKSIDE GANTRY & BRAKING DISTANCE MARKER BOARDS
// -------------------------------------------------------------------
private fun DrawScope.drawTracksideAssets(
  canvasW: Float,
  canvasH: Float,
  horizonY: Float,
  playerDistance: Float,
  trackLengthMeters: Float,
  playerLaneX: Float,
  curveShift: Float,
  segments: List<RoadSegment>,
  currentSegmentIdx: Int,
  segmentFraction: Float,
  graphicsPreset: com.example.game.v2.GraphicsQualityPreset
) {
  val safeTrackLen = trackLengthMeters.coerceAtLeast(600f)
  val distToFinish = (safeTrackLen - (playerDistance % safeTrackLen)) % safeTrackLen

  // 1. Checkered Start / Finish Overhead Gantry (visible within 220m ahead of finish/start line)
  val distFromStart = playerDistance % safeTrackLen
  val gantryAheadMeters = when {
    distToFinish <= 220f -> distToFinish
    distFromStart <= 40f -> 0f // Just crossing under the gantry
    else -> -1f
  }

  if (gantryAheadMeters in 0f..220f) {
    val t = (1f - (gantryAheadMeters / 220f)).coerceIn(0.05f, 1f)
    val p = t * t
    val gantryY = horizonY + (canvasH - horizonY) * (p * 0.88f)
    val gantryW = canvasW * (0.20f + p * 0.74f)
    val gantryH = 24.dp.toPx() + (58.dp.toPx() * p)

    val centerX = (canvasW * 0.5f) + curveShift * (1f - p) - (playerLaneX * canvasW * 0.30f * p)
    val pillarW = 6.dp.toPx() + 14.dp.toPx() * p

    // Projected Overhead Gantry Shadow across Tarmac (when realtime shadows enabled)
    if (graphicsPreset.enableRealtimeShadows) {
      drawRect(
        color = Color.Black.copy(alpha = 0.36f * p),
        topLeft = Offset(centerX - gantryW * 0.5f, gantryY - 4.dp.toPx() * p),
        size = Size(gantryW, 10.dp.toPx() * p)
      )
    }

    // Left & Right Metallic Silver Support Trusses
    drawRect(
      color = GraphiteCharcoal,
      topLeft = Offset(centerX - gantryW * 0.5f, gantryY - gantryH),
      size = Size(pillarW, gantryH)
    )
    drawRect(
      color = GraphiteCharcoal,
      topLeft = Offset(centerX + gantryW * 0.5f - pillarW, gantryY - gantryH),
      size = Size(pillarW, gantryH)
    )
    // Metallic silver vertical highlight on pillars
    drawRect(
      color = MetallicSilverMid,
      topLeft = Offset(centerX - gantryW * 0.5f + pillarW * 0.25f, gantryY - gantryH),
      size = Size(pillarW * 0.3f, gantryH)
    )
    drawRect(
      color = MetallicSilverMid,
      topLeft = Offset(centerX + gantryW * 0.5f - pillarW * 0.75f, gantryY - gantryH),
      size = Size(pillarW * 0.3f, gantryH)
    )

    // Overhead Graphite Black & Racing Red Bridge Fascia
    val bridgeH = gantryH * 0.38f
    drawRect(
      color = GraphiteBlack,
      topLeft = Offset(centerX - gantryW * 0.5f - pillarW * 0.4f, gantryY - gantryH),
      size = Size(gantryW + pillarW * 0.8f, bridgeH)
    )
    // Racing Red top & bottom trim bars
    drawRect(
      color = MotorsportRacingRed,
      topLeft = Offset(centerX - gantryW * 0.5f - pillarW * 0.4f, gantryY - gantryH),
      size = Size(gantryW + pillarW * 0.8f, bridgeH * 0.18f)
    )

    // Checkered Strip along lower gantry edge
    val checks = 16
    val checkW = gantryW / checks
    for (c in 0 until checks) {
      val checkCol = if (c % 2 == 0) PureMotorsportWhite else GraphiteBlack
      drawRect(
        color = checkCol,
        topLeft = Offset(centerX - gantryW * 0.5f + c * checkW, gantryY - gantryH + bridgeH * 0.65f),
        size = Size(checkW, bridgeH * 0.35f)
      )
    }

    // FIA Start/Finish Gantry Signal Lights
    for (l in 0 until 5) {
      val lx = centerX - gantryW * 0.24f + l * (gantryW * 0.12f)
      drawCircle(
        color = MotorsportRacingRed,
        radius = (2.dp.toPx() + 4.dp.toPx() * p),
        center = Offset(lx, gantryY - gantryH + bridgeH * 0.42f)
      )
    }
  }

  // 2. Braking Zone Distance Marker Boards (200m / 150m / 100m) on track verge
  val segCount = segments.size.coerceAtLeast(1)
  val nextSegment = segments[(currentSegmentIdx + 1).mod(segCount)]
  if (nextSegment.isBrakingZone || segments[currentSegmentIdx].isBrakingZone) {
    val boardProgress = segmentFraction.coerceIn(0.05f, 0.95f)
    val p = boardProgress * boardProgress
    val boardY = horizonY + (canvasH - horizonY) * (p * 0.82f)
    val roadWAtY = (canvasW * 0.15f) + (canvasW * 0.79f) * p
    val roadCenterAtY = (canvasW * 0.5f) + curveShift * (1f - p) - (playerLaneX * canvasW * 0.32f * p)

    val boardW = 10.dp.toPx() + 26.dp.toPx() * p
    val boardH = 6.dp.toPx() + 16.dp.toPx() * p
    val leftBoardX = roadCenterAtY - roadWAtY * 0.58f - boardW

    // Braking Marker Board (Graphite Black with Racing Red & White stripes)
    drawRect(
      color = GraphiteBlack,
      topLeft = Offset(leftBoardX, boardY - boardH),
      size = Size(boardW, boardH)
    )
    drawRect(
      color = MotorsportRacingRed,
      topLeft = Offset(leftBoardX, boardY - boardH),
      size = Size(boardW, boardH * 0.35f)
    )
    drawRect(
      color = PureMotorsportWhite,
      topLeft = Offset(leftBoardX + boardW * 0.15f, boardY - boardH * 0.52f),
      size = Size(boardW * 0.7f, boardH * 0.22f)
    )
  }
}

// -------------------------------------------------------------------
// 4. RIVAL AI OPPONENTS (LAP-AWARE DISTANCE, DIRECTIONAL SHADOWS, REFLECTIONS & METALLIC SHADING)
// -------------------------------------------------------------------
private fun DrawScope.drawRivalVehicles(
  canvasW: Float,
  canvasH: Float,
  horizonY: Float,
  playerDistance: Float,
  trackLengthMeters: Float,
  playerLaneX: Float,
  curveShift: Float,
  engine: InteractiveRacingEngine,
  graphicsPreset: com.example.game.v2.GraphicsQualityPreset,
  pulse: Float
) {
  val rivals = engine.getRivalsList()
  val safeTrackLen = trackLengthMeters.coerceAtLeast(600f)
  val roadTopW = canvasW * 0.15f
  val roadBottomW = canvasW * 0.94f
  val vanishingX = (canvasW * 0.5f) + curveShift - (playerLaneX * canvasW * 0.12f)
  val roadCenterX = (canvasW * 0.5f) - (playerLaneX * canvasW * 0.36f)
  val isWet = engine.activeWeather == RaceWeather.WET_STORM

  // Compute lap-aware relative distance ahead of player so rivals don't pop out across the finish line
  val rivalsWithDelta = rivals.map { rival ->
    val rawDiff = (rival.distanceMeters - playerDistance) % safeTrackLen
    val wrappedDiff = if (rawDiff < -safeTrackLen * 0.5f) rawDiff + safeTrackLen else rawDiff
    Pair(rival, wrappedDiff)
  }.filter { (_, deltaMeters) ->
    deltaMeters in 3f..260f
  }.sortedByDescending { (_, deltaMeters) ->
    deltaMeters
  }

  rivalsWithDelta.forEach { (rival, deltaMeters) ->
    val depthNorm = (1f - (deltaMeters / 260f)).coerceIn(0.05f, 1f)
    val p = depthNorm * depthNorm

    val carY = horizonY + (canvasH - horizonY) * (p * 0.86f)
    val carW = (canvasW * 0.075f) + (canvasW * 0.20f * depthNorm)
    val carH = carW * 0.54f

    val roadWAtY = roadTopW + (roadBottomW - roadTopW) * p
    val roadCenterAtY = vanishingX + (roadCenterX - vanishingX) * p
    val carX = roadCenterAtY + (rival.laneOffset * roadWAtY * 0.43f) - (carW * 0.5f)

    // Multi-Layer Ground Contact & Directional Shadow
    if (graphicsPreset.enableRealtimeShadows && graphicsPreset.shadowLayers >= 2) {
      drawOval(
        color = Color.Black.copy(alpha = 0.38f),
        topLeft = Offset(carX - carW * 0.06f, carY + carH * 0.74f),
        size = Size(carW * 1.12f, carH * 0.34f)
      )
    }
    drawOval(
      color = Color.Black.copy(alpha = 0.76f),
      topLeft = Offset(carX + carW * 0.04f, carY + carH * 0.78f),
      size = Size(carW * 0.92f, carH * 0.26f)
    )

    // Wet Tarmac Taillight Mirror Reflection Streaks beneath Rival
    if (graphicsPreset.enableDynamicReflections && (isWet || graphicsPreset.roadTextureDetailLevel >= 3)) {
      val reflAlpha = if (isWet) 0.32f * depthNorm else 0.14f * depthNorm
      val lightW = carW * 0.20f
      drawRect(
        color = MotorsportRacingRed.copy(alpha = reflAlpha),
        topLeft = Offset(carX + carW * 0.08f, carY + carH * 0.88f),
        size = Size(lightW, carH * 0.28f)
      )
      drawRect(
        color = MotorsportRacingRed.copy(alpha = reflAlpha),
        topLeft = Offset(carX + carW * 0.70f, carY + carH * 0.88f),
        size = Size(lightW, carH * 0.28f)
      )
    }

    // Rear Tires (Left & Right)
    val tireW = carW * 0.17f
    val tireH = carH * 0.36f
    drawRoundRect(
      color = GraphiteBlack,
      topLeft = Offset(carX + carW * 0.02f, carY + carH * 0.48f),
      size = Size(tireW, tireH),
      cornerRadius = CornerRadius(tireW * 0.2f, tireW * 0.2f)
    )
    drawRoundRect(
      color = GraphiteBlack,
      topLeft = Offset(carX + carW - tireW - carW * 0.02f, carY + carH * 0.48f),
      size = Size(tireW, tireH),
      cornerRadius = CornerRadius(tireW * 0.2f, tireW * 0.2f)
    )

    // Sculpted GT3 Rival Bodywork with Metallic Clearcoat Specular Highlight
    val bodyColor = rival.composeColor
    val rivalBodyPath = Path().apply {
      moveTo(carX + carW * 0.14f, carY + carH * 0.18f)
      lineTo(carX + carW * 0.26f, carY + carH * 0.04f)
      lineTo(carX + carW * 0.74f, carY + carH * 0.04f)
      lineTo(carX + carW * 0.86f, carY + carH * 0.18f)
      lineTo(carX + carW * 0.96f, carY + carH * 0.72f)
      lineTo(carX + carW * 0.04f, carY + carH * 0.72f)
      close()
    }
    drawPath(
      path = rivalBodyPath,
      brush = Brush.verticalGradient(
        colors = listOf(
          if (graphicsPreset.enableClearcoatMaterials) PureMotorsportWhite.copy(alpha = 0.92f) else MetallicSilverLight.copy(alpha = 0.80f),
          bodyColor,
          GraphiteDark
        ),
        startY = carY,
        endY = carY + carH * 0.75f
      )
    )

    // Dark Tinted Rear Canopy with Horizon Reflection Band
    drawRoundRect(
      color = GraphiteBlack,
      topLeft = Offset(carX + carW * 0.24f, carY + carH * 0.08f),
      size = Size(carW * 0.52f, carH * 0.28f),
      cornerRadius = CornerRadius(carW * 0.06f, carW * 0.06f)
    )
    if (graphicsPreset.enableDynamicReflections) {
      drawLine(
        color = PureMotorsportWhite.copy(alpha = 0.35f),
        start = Offset(carX + carW * 0.30f, carY + carH * 0.14f),
        end = Offset(carX + carW * 0.70f, carY + carH * 0.14f),
        strokeWidth = (1.5.dp.toPx() * depthNorm).coerceAtLeast(1f)
      )
    }

    // Carbon Rear Wing & Metallic Silver Endplates
    drawRect(
      color = GraphiteBlack,
      topLeft = Offset(carX + carW * 0.06f, carY - carH * 0.06f),
      size = Size(carW * 0.88f, carH * 0.09f)
    )
    drawRect(
      color = MetallicSilverMid,
      topLeft = Offset(carX + carW * 0.05f, carY - carH * 0.08f),
      size = Size(carW * 0.03f, carH * 0.16f)
    )
    drawRect(
      color = MetallicSilverMid,
      topLeft = Offset(carX + carW * 0.92f, carY - carH * 0.08f),
      size = Size(carW * 0.03f, carH * 0.16f)
    )

    // Glowing Racing Red LED Taillight Strips
    val lightW = carW * 0.22f
    val lightH = carH * 0.11f
    drawRoundRect(
      color = MotorsportRacingRed,
      topLeft = Offset(carX + carW * 0.08f, carY + carH * 0.40f),
      size = Size(lightW, lightH),
      cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
    )
    drawRoundRect(
      color = MotorsportRacingRed,
      topLeft = Offset(carX + carW * 0.70f, carY + carH * 0.40f),
      size = Size(lightW, lightH),
      cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
    )

    // Central FIA Rain/Telemetry Strobe Light
    if (pulse > 0.4f) {
      drawCircle(
        color = MotorsportRacingRed,
        radius = (carW * 0.025f).coerceAtLeast(2f),
        center = Offset(carX + carW * 0.5f, carY + carH * 0.62f)
      )
    }

    // Slipstream Aero Wake Ribbons when drafting closely
    if (deltaMeters < 38f && engine.hudState.value.isDrafting) {
      val trailAlpha = (1f - (deltaMeters / 38f)).coerceIn(0.25f, 0.85f)
      drawLine(
        color = MetallicSilverLight.copy(alpha = trailAlpha),
        start = Offset(carX + carW * 0.08f, carY + carH * 0.1f),
        end = Offset(carX - carW * 0.25f, canvasH * 0.82f),
        strokeWidth = 2.dp.toPx()
      )
      drawLine(
        color = MotorsportRacingRed.copy(alpha = trailAlpha * 0.8f),
        start = Offset(carX + carW * 0.92f, carY + carH * 0.1f),
        end = Offset(carX + carW * 1.25f, canvasH * 0.82f),
        strokeWidth = 2.dp.toPx()
      )
    }
  }
}

// -------------------------------------------------------------------
// 5. CHASE CAM: PLAYER HYPERCAR OR SUPERBIKE RENDERER (WITH MATERIALS, SHADOWS, REFLECTIONS, ANIMATION & SMOKE)
// -------------------------------------------------------------------
private fun DrawScope.drawChaseCamPlayerVehicle(
  canvasW: Float,
  canvasH: Float,
  vehicleType: VehicleType,
  vehicleColor: Color,
  loadout: com.example.game.v2.VehicleCustomizationLoadout,
  graphicsPreset: com.example.game.v2.GraphicsQualityPreset,
  playerLaneX: Float,
  steerInput: Float,
  throttleInput: Float,
  brakeInput: Float,
  isHandbrakeActive: Boolean,
  isNitroActive: Boolean,
  isDrsActive: Boolean,
  isOffTrack: Boolean,
  weather: RaceWeather,
  speedKmh: Float,
  rpm: Int,
  playerDistance: Float,
  pulse: Float
) {
  if (vehicleType == VehicleType.MOTORBIKE) {
    drawChaseCamSuperbike(
      canvasW = canvasW,
      canvasH = canvasH,
      bikeAccentColor = vehicleColor,
      graphicsPreset = graphicsPreset,
      steerInput = steerInput,
      brakeInput = brakeInput,
      isHandbrakeActive = isHandbrakeActive,
      isNitroActive = isNitroActive,
      weather = weather,
      speedKmh = speedKmh,
      pulse = pulse
    )
    return
  }

  val bodyKitBonusPx = loadout.bodyKitSpec.widthBonusPx.dp.toPx()
  val carW = (canvasW * 0.45f) + bodyKitBonusPx
  val carH = (canvasW * 0.45f) * 0.54f

  // Dynamic lateral weight transfer, suspension heave & engine RPM micro-vibration
  val rpmVibrationPx = if (rpm > 6800) sin(pulse * 125.66f) * 1.2.dp.toPx() else 0f
  val suspensionBounceY = if (isOffTrack) sin(playerDistance * 0.8f) * 2.8.dp.toPx() else sin(playerDistance * 0.25f) * 0.9.dp.toPx()
  val carCenterX = (canvasW * 0.5f) + (steerInput * 14.dp.toPx())
  val pitchShiftY = (brakeInput * 7.dp.toPx()) - (throttleInput * 3.5.dp.toPx()) + suspensionBounceY + rpmVibrationPx
  val carY = canvasH * 0.64f + pitchShiftY
  val carLeft = carCenterX - (carW * 0.5f)

  val rollAngle = steerInput * (if (isHandbrakeActive) 6.4f else 4.2f)

  // Forward Dual LED Headlight Road Illumination Cones (Night / Storm / Overcast or High Preset)
  if (graphicsPreset.enableHeadlightCones &&
    (weather == RaceWeather.NEON_NIGHT || weather == RaceWeather.WET_STORM || weather == RaceWeather.OVERCAST_COOL)
  ) {
    val beamSpread = 48.dp.toPx()
    val beamTopY = carY - carH * 0.45f
    val leftBeamPath = Path().apply {
      moveTo(carLeft + carW * 0.18f, carY + carH * 0.15f)
      lineTo(carLeft - beamSpread, beamTopY)
      lineTo(carCenterX - 8.dp.toPx(), beamTopY)
      close()
    }
    val rightBeamPath = Path().apply {
      moveTo(carLeft + carW * 0.82f, carY + carH * 0.15f)
      lineTo(carCenterX + 8.dp.toPx(), beamTopY)
      lineTo(carLeft + carW + beamSpread, beamTopY)
      close()
    }
    val beamBrush = Brush.verticalGradient(
      colors = listOf(Color.Transparent, Color(0xFF00E5FF).copy(alpha = 0.08f), PureMotorsportWhite.copy(alpha = 0.22f)),
      startY = beamTopY,
      endY = carY + carH * 0.15f
    )
    drawPath(leftBeamPath, brush = beamBrush)
    drawPath(rightBeamPath, brush = beamBrush)
  }

  rotate(degrees = rollAngle, pivot = Offset(carCenterX, carY + carH * 0.55f)) {
    // Multi-Layer Soft Ambient Occlusion + Directional Ground Shadow (scaled by graphicsPreset.shadowLayers)
    if (graphicsPreset.enableRealtimeShadows && graphicsPreset.shadowLayers >= 3) {
      drawOval(
        color = Color.Black.copy(alpha = 0.28f),
        topLeft = Offset(carLeft - carW * 0.05f - steerInput * 6.dp.toPx(), carY + carH * 0.74f),
        size = Size(carW * 1.10f, carH * 0.36f)
      )
    }
    if (graphicsPreset.enableRealtimeShadows && graphicsPreset.shadowLayers >= 2) {
      drawOval(
        color = Color.Black.copy(alpha = 0.52f),
        topLeft = Offset(carLeft - carW * 0.01f, carY + carH * 0.77f),
        size = Size(carW * 1.02f, carH * 0.32f)
      )
    }
    drawOval(
      color = Color.Black.copy(alpha = 0.84f),
      topLeft = Offset(carLeft + carW * 0.03f, carY + carH * 0.80f),
      size = Size(carW * 0.94f, carH * 0.28f)
    )

    // Wide Pirelli-style Racing Slicks (Left & Right) with Custom Rotating Rim Spokes
    val wheelW = carW * 0.17f
    val wheelH = carH * 0.44f
    val leftWheelX = carLeft + 2.dp.toPx()
    val rightWheelX = carLeft + carW - wheelW - 2.dp.toPx()
    val wheelY = carY + carH * 0.42f
    val rimColor = Color(loadout.wheelRimStyle.rimColorHex)
    val spokePhase = (playerDistance * 0.55f) % 6.283f

    listOf(leftWheelX, rightWheelX).forEach { wx ->
      drawRoundRect(
        brush = Brush.horizontalGradient(
          colors = listOf(GraphiteBlack, GraphiteCharcoal, GraphiteBlack),
          startX = wx,
          endX = wx + wheelW
        ),
        topLeft = Offset(wx, wheelY),
        size = Size(wheelW, wheelH),
        cornerRadius = CornerRadius(wheelW * 0.22f, wheelW * 0.22f)
      )

      // Custom Rim Lip & Rotating Spoke Flash
      val wheelCenter = Offset(wx + wheelW * 0.5f, wheelY + wheelH * 0.48f)
      drawCircle(
        color = rimColor.copy(alpha = 0.55f),
        radius = wheelW * 0.30f,
        center = wheelCenter,
        style = Stroke(width = 2.dp.toPx())
      )
      val spokeOffsetY = sin(spokePhase) * (wheelH * 0.16f)
      drawLine(
        color = rimColor.copy(alpha = 0.72f),
        start = Offset(wx + wheelW * 0.22f, wheelCenter.y - spokeOffsetY),
        end = Offset(wx + wheelW * 0.78f, wheelCenter.y + spokeOffsetY),
        strokeWidth = 1.8.dp.toPx()
      )

      // Glowing Carbon-Ceramic Brake Rotor visible inside wheel barrel during braking
      if (brakeInput > 0.05f) {
        val rotorGlowAlpha = (brakeInput * 0.9f).coerceIn(0.2f, 0.95f)
        drawCircle(
          color = MotorsportRacingRed.copy(alpha = rotorGlowAlpha),
          radius = wheelW * 0.34f,
          center = Offset(wx + wheelW * 0.5f, wheelY + wheelH * 0.45f)
        )
      }
    }

    // Lower Carbon & Metallic Silver Rear Diffuser Assembly with Vertical Strakes
    val diffuserY = carY + carH * 0.68f
    val diffuserH = carH * 0.17f
    drawRoundRect(
      color = GraphiteBlack,
      topLeft = Offset(carLeft + carW * 0.14f, diffuserY),
      size = Size(carW * 0.72f, diffuserH),
      cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
    )
    // Metallic Silver Diffuser Fins
    for (fin in 0..4) {
      val finX = carLeft + carW * (0.22f + fin * 0.14f)
      drawLine(
        color = MetallicSilverMid,
        start = Offset(finX, diffuserY + 2.dp.toPx()),
        end = Offset(finX, diffuserY + diffuserH - 2.dp.toPx()),
        strokeWidth = 2.dp.toPx()
      )
    }

    // Sculpted GT3 / Hypercar Main Bodywork using Player's PaintFinishMaterial & Custom Paint Color
    val finishMaterial = loadout.paintFinishMaterial
    val primaryMaterialColor = when (finishMaterial) {
      com.example.game.v2.PaintFinishMaterial.CARBON,
      com.example.game.v2.PaintFinishMaterial.MATTE_CARBON,
      com.example.game.v2.PaintFinishMaterial.FORGED_CARBON -> Color(finishMaterial.primaryOrbHex)
      com.example.game.v2.PaintFinishMaterial.CHAMELEON_FLAKE,
      com.example.game.v2.PaintFinishMaterial.COLOR_SHIFT,
      com.example.game.v2.PaintFinishMaterial.IRIDESCENT -> Color(finishMaterial.primaryOrbHex)
      else -> vehicleColor
    }
    val secondaryMaterialColor = when (finishMaterial) {
      com.example.game.v2.PaintFinishMaterial.CHROME -> PureMotorsportWhite
      com.example.game.v2.PaintFinishMaterial.CHAMELEON_FLAKE,
      com.example.game.v2.PaintFinishMaterial.COLOR_SHIFT,
      com.example.game.v2.PaintFinishMaterial.IRIDESCENT -> Color(finishMaterial.secondaryOrbHex)
      else -> MetallicSilverLight
    }

    val mainBodyPath = Path().apply {
      moveTo(carLeft + carW * 0.12f, carY + carH * 0.22f)
      lineTo(carLeft + carW * 0.27f, carY + carH * 0.04f)
      lineTo(carLeft + carW * 0.73f, carY + carH * 0.04f)
      lineTo(carLeft + carW * 0.88f, carY + carH * 0.22f)
      lineTo(carLeft + carW * 0.96f, carY + carH * 0.72f)
      lineTo(carLeft + carW * 0.04f, carY + carH * 0.72f)
      close()
    }
    drawPath(
      path = mainBodyPath,
      brush = Brush.verticalGradient(
        colors = listOf(
          secondaryMaterialColor.copy(alpha = finishMaterial.specularHighlightAlpha),
          primaryMaterialColor,
          GraphiteCharcoal,
          GraphiteBlack
        ),
        startY = carY,
        endY = carY + carH * 0.74f
      )
    )

    // Clearcoat Horizon Specular Reflection Band across Upper Deck (Medium & High Graphics)
    if (graphicsPreset.enableClearcoatMaterials) {
      val specY = carY + carH * (0.20f + 0.03f * sin(pulse * 6.28f))
      drawLine(
        color = PureMotorsportWhite.copy(alpha = 0.36f * finishMaterial.specularHighlightAlpha),
        start = Offset(carLeft + carW * 0.16f, specY),
        end = Offset(carLeft + carW * 0.84f, specY),
        strokeWidth = 3.dp.toPx(),
        cap = StrokeCap.Round
      )
    }

    // Custom Livery Shoulder & Fender Accents
    val leftHaunchPath = Path().apply {
      moveTo(carLeft + carW * 0.12f, carY + carH * 0.24f)
      lineTo(carLeft + carW * 0.28f, carY + carH * 0.24f)
      lineTo(carLeft + carW * 0.22f, carY + carH * 0.68f)
      lineTo(carLeft + carW * 0.05f, carY + carH * 0.68f)
      close()
    }
    val rightHaunchPath = Path().apply {
      moveTo(carLeft + carW * 0.88f, carY + carH * 0.24f)
      lineTo(carLeft + carW * 0.72f, carY + carH * 0.24f)
      lineTo(carLeft + carW * 0.78f, carY + carH * 0.68f)
      lineTo(carLeft + carW * 0.95f, carY + carH * 0.68f)
      close()
    }
    val accentBrush = Brush.verticalGradient(
      colors = listOf(vehicleColor, MotorsportCrimson, GraphiteBlack),
      startY = carY + carH * 0.24f,
      endY = carY + carH * 0.70f
    )
    drawPath(leftHaunchPath, brush = accentBrush)
    drawPath(rightHaunchPath, brush = accentBrush)

    // Crisp White Center Motorsport Spine Stripe
    drawRect(
      color = PureMotorsportWhite,
      topLeft = Offset(carCenterX - carW * 0.025f, carY + carH * 0.04f),
      size = Size(carW * 0.05f, carH * 0.36f)
    )
    drawRect(
      color = MotorsportRacingRed,
      topLeft = Offset(carCenterX - carW * 0.01f, carY + carH * 0.04f),
      size = Size(carW * 0.02f, carH * 0.36f)
    )

    // Teardrop Cockpit Canopy & Louvered Carbon Engine Deck
    drawRoundRect(
      brush = Brush.verticalGradient(
        colors = listOf(GraphiteCharcoal, GraphiteBlack),
        startY = carY + carH * 0.07f,
        endY = carY + carH * 0.38f
      ),
      topLeft = Offset(carLeft + carW * 0.29f, carY + carH * 0.07f),
      size = Size(carW * 0.42f, carH * 0.32f),
      cornerRadius = CornerRadius(carW * 0.08f, carW * 0.08f)
    )
    // Dynamic Sky Reflection on Rear Glass Canopy
    if (graphicsPreset.enableDynamicReflections) {
      drawLine(
        color = PureMotorsportWhite.copy(alpha = 0.28f),
        start = Offset(carLeft + carW * 0.34f, carY + carH * 0.11f),
        end = Offset(carLeft + carW * 0.66f, carY + carH * 0.11f),
        strokeWidth = 2.5.dp.toPx(),
        cap = StrokeCap.Round
      )
    }
    // Engine bay heat-extractor vents
    for (v in 0..2) {
      val vy = carY + carH * (0.14f + v * 0.07f)
      drawLine(
        color = MetallicSilverDark,
        start = Offset(carLeft + carW * 0.36f, vy),
        end = Offset(carLeft + carW * 0.64f, vy),
        strokeWidth = 2.dp.toPx()
      )
    }

    // Custom Spoiler / Swan-Neck Carbon Rear Wing with Metallic Silver Endplates (DRS flattens flap)
    val wingElevationFactor = when (loadout.spoilerStyle) {
      com.example.game.v2.SpoilerWingStyle.FACTORY_LIP -> 0.03f
      com.example.game.v2.SpoilerWingStyle.GT3_SWAN_NECK -> 0.10f
      com.example.game.v2.SpoilerWingStyle.TIME_ATTACK_DUAL -> 0.13f
      com.example.game.v2.SpoilerWingStyle.ACTIVE_DRS_CARBON -> 0.11f
    }
    val wingY = if (isDrsActive) carY - carH * 0.02f else carY - carH * wingElevationFactor
    val wingH = if (isDrsActive) carH * 0.045f else carH * 0.095f

    // Swan-neck CNC aluminum pylons
    drawRect(
      color = MetallicSilverMid,
      topLeft = Offset(carLeft + carW * 0.35f, wingY),
      size = Size(carW * 0.035f, carH * 0.16f)
    )
    drawRect(
      color = MetallicSilverMid,
      topLeft = Offset(carLeft + carW * 0.615f, wingY),
      size = Size(carW * 0.035f, carH * 0.16f)
    )

    // Main Carbon Wing Plane + Custom Paint / Racing Red Trailing Edge
    drawRoundRect(
      color = GraphiteBlack,
      topLeft = Offset(carLeft + carW * 0.04f, wingY),
      size = Size(carW * 0.92f, wingH),
      cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx())
    )
    drawRect(
      color = vehicleColor,
      topLeft = Offset(carLeft + carW * 0.06f, wingY),
      size = Size(carW * 0.88f, wingH * 0.28f)
    )

    // Metallic Silver Wing Endplates
    drawRoundRect(
      color = MetallicSilverLight,
      topLeft = Offset(carLeft + carW * 0.02f, wingY - carH * 0.02f),
      size = Size(carW * 0.03f, carH * 0.15f),
      cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
    )
    drawRoundRect(
      color = MetallicSilverLight,
      topLeft = Offset(carLeft + carW * 0.95f, wingY - carH * 0.02f),
      size = Size(carW * 0.03f, carH * 0.15f),
      cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
    )

    // Signature Laser-Blade LED Taillights + Braking Halation Glow & Ground Reflection
    val isBraking = brakeInput > 0.08f
    val taillightColor = if (isBraking) Color(0xFFFF1744) else Color(0xFFD50000)
    val barHeight = if (isBraking) carH * 0.10f else carH * 0.055f

    if (isBraking) {
      // Soft red brake halation bloom
      drawOval(
        color = MotorsportRacingRed.copy(alpha = 0.42f),
        topLeft = Offset(carLeft + carW * 0.04f, carY + carH * 0.34f),
        size = Size(carW * 0.38f, carH * 0.24f)
      )
      drawOval(
        color = MotorsportRacingRed.copy(alpha = 0.42f),
        topLeft = Offset(carLeft + carW * 0.58f, carY + carH * 0.34f),
        size = Size(carW * 0.38f, carH * 0.24f)
      )
      // Red Taillight Reflection on Road Behind Car
      if (graphicsPreset.enableDynamicReflections) {
        drawOval(
          color = MotorsportRacingRed.copy(alpha = 0.22f),
          topLeft = Offset(carLeft + carW * 0.06f, carY + carH * 0.88f),
          size = Size(carW * 0.88f, carH * 0.18f)
        )
      }
    }

    drawRoundRect(
      color = taillightColor,
      topLeft = Offset(carLeft + carW * 0.08f, carY + carH * 0.42f),
      size = Size(carW * 0.30f, barHeight),
      cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx())
    )
    drawRoundRect(
      color = taillightColor,
      topLeft = Offset(carLeft + carW * 0.62f, carY + carH * 0.42f),
      size = Size(carW * 0.30f, barHeight),
      cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx())
    )

    // Crisp White LED Core inside brake bars when braking hard
    if (isBraking) {
      drawRect(
        color = PureMotorsportWhite.copy(alpha = 0.85f),
        topLeft = Offset(carLeft + carW * 0.11f, carY + carH * 0.44f),
        size = Size(carW * 0.24f, barHeight * 0.35f)
      )
      drawRect(
        color = PureMotorsportWhite.copy(alpha = 0.85f),
        topLeft = Offset(carLeft + carW * 0.65f, carY + carH * 0.44f),
        size = Size(carW * 0.24f, barHeight * 0.35f)
      )
    }

    // Central FIA Rain/ERS Strobe Light on Diffuser
    drawRoundRect(
      color = if (pulse > 0.5f || isBraking) MotorsportRacingRed else MotorsportCrimson,
      topLeft = Offset(carCenterX - carW * 0.025f, diffuserY + diffuserH * 0.25f),
      size = Size(carW * 0.05f, diffuserH * 0.5f),
      cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
    )

    // Titanium Quad Exhaust Pipes with Metallic Silver Bezel
    val exhaustY = carY + carH * 0.71f
    val exhaustPositions = listOf(
      carLeft + carW * 0.36f,
      carLeft + carW * 0.42f,
      carLeft + carW * 0.58f,
      carLeft + carW * 0.64f
    )
    exhaustPositions.forEach { exX ->
      drawCircle(
        color = MetallicSilverMid,
        radius = carW * 0.027f,
        center = Offset(exX, exhaustY)
      )
      drawCircle(
        color = GraphiteBlack,
        radius = carW * 0.019f,
        center = Offset(exX, exhaustY)
      )
    }

    // Afterburner / Nitro Plumes from Exhausts
    if (isNitroActive || speedKmh > 295f) {
      val flameLength = (carH * 0.36f) * (0.75f + (pulse * 0.45f))
      exhaustPositions.forEach { exX ->
        val flamePath = Path().apply {
          moveTo(exX - carW * 0.018f, exhaustY)
          lineTo(exX, exhaustY + flameLength)
          lineTo(exX + carW * 0.018f, exhaustY)
          close()
        }
        drawPath(
          path = flamePath,
          brush = Brush.verticalGradient(
            colors = listOf(
              PureMotorsportWhite,
              MotorsportRacingRed,
              Color(0xFFFF9100),
              Color.Transparent
            ),
            startY = exhaustY,
            endY = exhaustY + flameLength
          )
        )
      }
    }

    // Volumetric Multi-Puff Tire Smoke & Wet Spray Plumes (scaled by graphicsPreset.tireSmokeParticles)
    val isDriftingOrLocking = isHandbrakeActive || abs(steerInput) > 0.52f || (brakeInput > 0.68f && speedKmh > 90f)
    if (isDriftingOrLocking || isOffTrack || weather == RaceWeather.WET_STORM) {
      val baseSmokeColor = when {
        weather == RaceWeather.WET_STORM -> MetallicSilverLight.copy(alpha = 0.28f)
        isOffTrack -> Color(0xFF8D6E63).copy(alpha = 0.36f)
        isHandbrakeActive -> PureMotorsportWhite.copy(alpha = 0.34f)
        else -> MetallicSilverMid.copy(alpha = 0.26f)
      }
      val puffsPerWheel = (graphicsPreset.tireSmokeParticles / 2).coerceIn(1, 6)
      val baseLeftX = leftWheelX + wheelW * 0.5f
      val baseRightX = rightWheelX + wheelW * 0.5f
      val baseY = wheelY + wheelH * 0.85f

      for (pIdx in 0 until puffsPerWheel) {
        val phase = (pulse + pIdx * 0.21f) % 1f
        val driftSpreadX = -steerInput * (18.dp.toPx() * phase)
        val puffY = baseY + phase * (carH * 0.34f)
        val puffRadius = carW * (0.055f + 0.075f * phase)
        val puffAlpha = (1f - phase * 0.78f).coerceIn(0.1f, 1f)

        drawCircle(
          color = baseSmokeColor.copy(alpha = baseSmokeColor.alpha * puffAlpha),
          radius = puffRadius,
          center = Offset(baseLeftX + driftSpreadX - pIdx * 3.dp.toPx(), puffY)
        )
        drawCircle(
          color = baseSmokeColor.copy(alpha = baseSmokeColor.alpha * puffAlpha),
          radius = puffRadius,
          center = Offset(baseRightX + driftSpreadX + pIdx * 3.dp.toPx(), puffY)
        )
      }
    }
  }
}

// -------------------------------------------------------------------
// 5B. CHASE CAM: FACTORY MOTOGP SUPERBIKE & LEANING RIDER
// -------------------------------------------------------------------
private fun DrawScope.drawChaseCamSuperbike(
  canvasW: Float,
  canvasH: Float,
  bikeAccentColor: Color,
  graphicsPreset: com.example.game.v2.GraphicsQualityPreset,
  steerInput: Float,
  brakeInput: Float,
  isHandbrakeActive: Boolean,
  isNitroActive: Boolean,
  weather: RaceWeather,
  speedKmh: Float,
  pulse: Float
) {
  val bikeW = canvasW * 0.22f
  val bikeH = bikeW * 1.18f

  val bikeCenterX = (canvasW * 0.5f) + (steerInput * 18.dp.toPx())
  val bikeBottomY = canvasH * 0.86f + (brakeInput * 6.dp.toPx())
  val bikeTopY = bikeBottomY - bikeH

  // Superbikes lean dramatically into the apex (up to 36 degrees)
  val leanAngle = steerInput * 36f

  // Multi-layer ground shadow beneath rear tire contact patch
  if (graphicsPreset.enableRealtimeShadows && graphicsPreset.shadowLayers >= 2) {
    drawOval(
      color = Color.Black.copy(alpha = 0.42f),
      topLeft = Offset(bikeCenterX - bikeW * 0.56f, bikeBottomY - bikeH * 0.14f),
      size = Size(bikeW * 1.12f, bikeH * 0.22f)
    )
  }
  drawOval(
    color = Color.Black.copy(alpha = 0.78f),
    topLeft = Offset(bikeCenterX - bikeW * 0.45f, bikeBottomY - bikeH * 0.12f),
    size = Size(bikeW * 0.9f, bikeH * 0.18f)
  )

  rotate(degrees = leanAngle, pivot = Offset(bikeCenterX, bikeBottomY - bikeH * 0.08f)) {
    // Wide Rear MotoGP Slick Tire
    val tireW = bikeW * 0.36f
    val tireH = bikeH * 0.44f
    drawRoundRect(
      brush = Brush.horizontalGradient(
        colors = listOf(GraphiteBlack, GraphiteCharcoal, GraphiteBlack),
        startX = bikeCenterX - tireW * 0.5f,
        endX = bikeCenterX + tireW * 0.5f
      ),
      topLeft = Offset(bikeCenterX - tireW * 0.5f, bikeBottomY - tireH),
      size = Size(tireW, tireH),
      cornerRadius = CornerRadius(tireW * 0.45f, tireW * 0.45f)
    )

    // Metallic Silver Dual-Sided Aluminum Swingarm & Rear Brake Rotor
    drawRect(
      color = MetallicSilverMid,
      topLeft = Offset(bikeCenterX - tireW * 0.62f, bikeBottomY - tireH * 0.65f),
      size = Size(tireW * 0.16f, tireH * 0.36f)
    )
    drawRect(
      color = MetallicSilverMid,
      topLeft = Offset(bikeCenterX + tireW * 0.46f, bikeBottomY - tireH * 0.65f),
      size = Size(tireW * 0.16f, tireH * 0.36f)
    )

    // Sculpted Aerodynamic Tail Cowling (Racing Red, Metallic Silver & Graphite)
    val tailPath = Path().apply {
      moveTo(bikeCenterX, bikeTopY + bikeH * 0.18f)
      lineTo(bikeCenterX + bikeW * 0.38f, bikeTopY + bikeH * 0.44f)
      lineTo(bikeCenterX + bikeW * 0.22f, bikeTopY + bikeH * 0.66f)
      lineTo(bikeCenterX - bikeW * 0.22f, bikeTopY + bikeH * 0.66f)
      lineTo(bikeCenterX - bikeW * 0.38f, bikeTopY + bikeH * 0.44f)
      close()
    }
    drawPath(
      path = tailPath,
      brush = Brush.verticalGradient(
        colors = listOf(MotorsportRacingRed, bikeAccentColor, GraphiteBlack),
        startY = bikeTopY + bikeH * 0.18f,
        endY = bikeTopY + bikeH * 0.66f
      )
    )

    // Rider in Aerodynamic Tuck (Graphite Black & Metallic Silver Leather Suit + Helmet)
    val riderShiftX = steerInput * (bikeW * 0.18f)
    drawOval(
      color = GraphiteCharcoal,
      topLeft = Offset(bikeCenterX - bikeW * 0.34f + riderShiftX, bikeTopY + bikeH * 0.16f),
      size = Size(bikeW * 0.68f, bikeH * 0.36f)
    )
    // Racing Red & White Aero Hump on Rider Back
    drawRoundRect(
      color = PureMotorsportWhite,
      topLeft = Offset(bikeCenterX - bikeW * 0.12f + riderShiftX, bikeTopY + bikeH * 0.22f),
      size = Size(bikeW * 0.24f, bikeH * 0.22f),
      cornerRadius = CornerRadius(bikeW * 0.08f, bikeW * 0.08f)
    )
    drawRoundRect(
      color = MotorsportRacingRed,
      topLeft = Offset(bikeCenterX - bikeW * 0.06f + riderShiftX, bikeTopY + bikeH * 0.24f),
      size = Size(bikeW * 0.12f, bikeH * 0.18f),
      cornerRadius = CornerRadius(bikeW * 0.04f, bikeW * 0.04f)
    )

    // Rider Helmet (Metallic Silver & Racing Red Visor Crown)
    drawCircle(
      color = MetallicSilverLight,
      radius = bikeW * 0.19f,
      center = Offset(bikeCenterX + riderShiftX * 1.1f, bikeTopY + bikeH * 0.14f)
    )
    drawCircle(
      color = MotorsportRacingRed,
      radius = bikeW * 0.13f,
      center = Offset(bikeCenterX + riderShiftX * 1.1f, bikeTopY + bikeH * 0.14f)
    )

    // Vertical Tail Brake LED Lamp
    val tailGlow = if (brakeInput > 0.08f) MotorsportRacingRed else MotorsportCrimson
    drawRoundRect(
      color = tailGlow,
      topLeft = Offset(bikeCenterX - bikeW * 0.06f, bikeTopY + bikeH * 0.52f),
      size = Size(bikeW * 0.12f, bikeH * 0.10f),
      cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx())
    )

    // Under-seat Titanium Exhaust & Nitro Afterburner
    val exY = bikeTopY + bikeH * 0.62f
    drawCircle(
      color = MetallicSilverMid,
      radius = bikeW * 0.06f,
      center = Offset(bikeCenterX + bikeW * 0.16f, exY)
    )
    if (isNitroActive || speedKmh > 290f) {
      val flameH = bikeH * (0.28f + 0.15f * pulse)
      drawLine(
        brush = Brush.verticalGradient(
          colors = listOf(PureMotorsportWhite, MotorsportRacingRed, Color.Transparent),
          startY = exY,
          endY = exY + flameH
        ),
        start = Offset(bikeCenterX + bikeW * 0.16f, exY),
        end = Offset(bikeCenterX + bikeW * 0.16f, exY + flameH),
        strokeWidth = 6.dp.toPx(),
        cap = StrokeCap.Round
      )
    }

    // Superbike Rear Tire Smoke / Wet Spray Plumes
    if (isHandbrakeActive || abs(steerInput) > 0.55f || weather == RaceWeather.WET_STORM) {
      val puffCount = (graphicsPreset.tireSmokeParticles / 2).coerceIn(1, 5)
      for (pIdx in 0 until puffCount) {
        val phase = (pulse + pIdx * 0.22f) % 1f
        drawCircle(
          color = MetallicSilverLight.copy(alpha = 0.26f * (1f - phase * 0.7f)),
          radius = bikeW * (0.12f + 0.14f * phase),
          center = Offset(bikeCenterX - steerInput * 12.dp.toPx() * phase, bikeBottomY + phase * 18.dp.toPx())
        )
      }
    }
  }
}

// -------------------------------------------------------------------
// 6. COCKPIT CAM: GRAPHITE CARBON CABIN, SHIFT PADDLES & LIVE MIRROR
// -------------------------------------------------------------------
private fun DrawScope.drawCockpitCam(
  canvasW: Float,
  canvasH: Float,
  steerInput: Float,
  rpm: Int,
  gear: Int,
  speedKmh: Float,
  isDrsActive: Boolean,
  rivals: List<AirRival>,
  playerDistance: Float,
  trackLengthMeters: Float
) {
  // Carbon A-Pillars Left & Right
  val leftPillar = Path().apply {
    moveTo(0f, 0f)
    lineTo(canvasW * 0.08f, 0f)
    lineTo(canvasW * 0.14f, canvasH * 0.64f)
    lineTo(0f, canvasH * 0.64f)
    close()
  }
  val rightPillar = Path().apply {
    moveTo(canvasW, 0f)
    lineTo(canvasW * 0.92f, 0f)
    lineTo(canvasW * 0.86f, canvasH * 0.64f)
    lineTo(canvasW, canvasH * 0.64f)
    close()
  }
  drawPath(leftPillar, color = GraphiteBlack)
  drawPath(rightPillar, color = GraphiteBlack)

  // Sculpted Graphite Black & Metallic Silver Cockpit Binnacle
  val dashY = canvasH * 0.62f
  val dashPath = Path().apply {
    moveTo(0f, dashY)
    lineTo(canvasW * 0.28f, dashY + canvasH * 0.06f)
    lineTo(canvasW * 0.72f, dashY + canvasH * 0.06f)
    lineTo(canvasW, dashY)
    lineTo(canvasW, canvasH)
    lineTo(0f, canvasH)
    close()
  }
  drawPath(
    path = dashPath,
    brush = Brush.verticalGradient(
      colors = listOf(GraphiteCharcoal, GraphiteDark, GraphiteBlack),
      startY = dashY,
      endY = canvasH
    )
  )
  // Racing Red Stitching Line across Dashboard Top
  drawLine(
    color = MotorsportRacingRed,
    start = Offset(0f, dashY + 2.dp.toPx()),
    end = Offset(canvasW, dashY + 2.dp.toPx()),
    strokeWidth = 2.dp.toPx()
  )

  // Rotating Formula/GT3 Alcantara Steering Wheel with Metallic Silver Shift Paddles
  val wheelRadius = canvasW * 0.27f
  val wheelCenter = Offset(canvasW * 0.5f, canvasH * 0.88f)
  val wheelRotation = steerInput * 48f

  rotate(degrees = wheelRotation, pivot = wheelCenter) {
    // Metallic Silver CNC Shift Paddles behind the wheel
    drawRoundRect(
      color = MetallicSilverMid,
      topLeft = Offset(wheelCenter.x - wheelRadius * 1.05f, wheelCenter.y - wheelRadius * 0.45f),
      size = Size(wheelRadius * 0.22f, wheelRadius * 0.55f),
      cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
    )
    drawRoundRect(
      color = MetallicSilverMid,
      topLeft = Offset(wheelCenter.x + wheelRadius * 0.83f, wheelCenter.y - wheelRadius * 0.45f),
      size = Size(wheelRadius * 0.22f, wheelRadius * 0.55f),
      cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
    )

    // Outer Wheel Rim
    drawCircle(
      color = GraphiteCharcoal,
      radius = wheelRadius,
      center = wheelCenter,
      style = Stroke(width = canvasW * 0.062f)
    )

    // Center 12 o'clock Racing Red Top-Dead-Center Marker Strip
    drawRect(
      color = MotorsportRacingRed,
      topLeft = Offset(wheelCenter.x - 5.dp.toPx(), wheelCenter.y - wheelRadius - 8.dp.toPx()),
      size = Size(10.dp.toPx(), 16.dp.toPx())
    )

    // Center Hub with Metallic Silver Bezel & Racing Red Emblem
    drawCircle(
      color = GraphiteBlack,
      radius = wheelRadius * 0.46f,
      center = wheelCenter
    )
    drawCircle(
      color = MetallicSilverMid,
      radius = wheelRadius * 0.46f,
      center = wheelCenter,
      style = Stroke(width = 2.dp.toPx())
    )
    drawCircle(
      color = MotorsportRacingRed,
      radius = wheelRadius * 0.13f,
      center = wheelCenter
    )
  }

  // FIA Rev Limiter Shift LEDs on Dash (Green -> White -> Racing Red)
  val ledsCount = 12
  val ledW = canvasW * 0.024f
  val startLedX = canvasW * 0.34f
  val rpmRatio = (rpm / 9200f).coerceIn(0f, 1f)

  for (i in 0 until ledsCount) {
    val activeRatio = (i.toFloat() / ledsCount)
    val isActive = rpmRatio >= activeRatio
    val ledCol = when {
      !isActive -> GraphiteCharcoal
      i < 4 -> Color(0xFF00E676)
      i < 8 -> PureMotorsportWhite
      else -> MotorsportRacingRed
    }
    drawRoundRect(
      color = ledCol,
      topLeft = Offset(startLedX + i * (ledW + 3.dp.toPx()), dashY + 12.dp.toPx()),
      size = Size(ledW, 8.dp.toPx()),
      cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
    )
  }

  // Live Rearview Mirror at Top Center showing Rivals Behind
  val mirrorW = canvasW * 0.38f
  val mirrorH = mirrorW * 0.28f
  val mirrorLeft = (canvasW - mirrorW) * 0.5f
  val mirrorTop = 8.dp.toPx()

  drawRoundRect(
    brush = Brush.verticalGradient(
      colors = listOf(GraphiteCharcoal, GraphiteBlack),
      startY = mirrorTop,
      endY = mirrorTop + mirrorH
    ),
    topLeft = Offset(mirrorLeft + 2.dp.toPx(), mirrorTop + 2.dp.toPx()),
    size = Size(mirrorW - 4.dp.toPx(), mirrorH - 4.dp.toPx()),
    cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
  )

  // Render following rivals inside the rearview mirror
  val safeTrackLen = trackLengthMeters.coerceAtLeast(600f)
  rivals.forEach { rival ->
    val behindMeters = ((playerDistance - rival.distanceMeters) % safeTrackLen + safeTrackLen) % safeTrackLen
    if (behindMeters in 2f..90f) {
      val prox = 1f - (behindMeters / 90f)
      val dotW = (mirrorW * 0.08f) + (mirrorW * 0.14f * prox)
      val dotH = dotW * 0.55f
      val dotX = mirrorLeft + mirrorW * 0.5f - (rival.laneOffset * mirrorW * 0.32f) - dotW * 0.5f
      val dotY = mirrorTop + mirrorH * (0.35f + 0.35f * prox)
      drawRoundRect(
        color = rival.composeColor,
        topLeft = Offset(dotX.coerceIn(mirrorLeft + 6f, mirrorLeft + mirrorW - dotW - 6f), dotY),
        size = Size(dotW, dotH),
        cornerRadius = CornerRadius(3f, 3f)
      )
    }
  }

  // Metallic Silver Mirror Frame
  drawRoundRect(
    color = MetallicSilverMid,
    topLeft = Offset(mirrorLeft, mirrorTop),
    size = Size(mirrorW, mirrorH),
    cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx()),
    style = Stroke(width = 2.dp.toPx())
  )
}

// -------------------------------------------------------------------
// 7. HOOD CAM OVERLAY: SCULPTED MOTORSPORT NOSE CONE & FENDER LOUVERS
// -------------------------------------------------------------------
private fun DrawScope.drawHoodCamOverlay(
  canvasW: Float,
  canvasH: Float,
  steerInput: Float,
  speedKmh: Float,
  brakeInput: Float,
  isNitroActive: Boolean,
  vehicleColor: Color,
  pulse: Float
) {
  val hoodH = canvasH * 0.17f
  val steerShift = -steerInput * 10.dp.toPx()

  val hoodPath = Path().apply {
    moveTo(canvasW * 0.18f + steerShift, canvasH)
    lineTo(canvasW * 0.38f + steerShift, canvasH - hoodH)
    lineTo(canvasW * 0.62f + steerShift, canvasH - hoodH)
    lineTo(canvasW * 0.82f + steerShift, canvasH)
    close()
  }
  drawPath(
    path = hoodPath,
    brush = Brush.verticalGradient(
      colors = listOf(MetallicSilverLight, vehicleColor, GraphiteCharcoal, GraphiteBlack),
      startY = canvasH - hoodH,
      endY = canvasH
    )
  )

  // Center Racing Red & Crisp White Motorsport Livery Stripe on Hood
  drawRect(
    color = PureMotorsportWhite,
    topLeft = Offset(canvasW * 0.475f + steerShift, canvasH - hoodH),
    size = Size(canvasW * 0.05f, hoodH)
  )
  drawRect(
    color = MotorsportRacingRed,
    topLeft = Offset(canvasW * 0.488f + steerShift, canvasH - hoodH),
    size = Size(canvasW * 0.024f, hoodH)
  )
}

// -------------------------------------------------------------------
// 8. HIGH-SPEED AERODYNAMIC STREAMLINES
// -------------------------------------------------------------------
private fun DrawScope.drawSpeedLines(
  w: Float,
  h: Float,
  horizonY: Float,
  isNitro: Boolean,
  speedKmh: Float,
  pulse: Float
) {
  val lines = 18
  val alpha = ((speedKmh - 240f) / 120f).coerceIn(0.2f, 0.7f)
  val lineColor = if (isNitro) {
    MotorsportRacingRed.copy(alpha = 0.65f)
  } else {
    MetallicSilverLight.copy(alpha = alpha)
  }
  val cx = w * 0.5f

  for (i in 0 until lines) {
    val angle = (i.toFloat() / lines) * 3.14159f
    val r0 = w * 0.24f + (((pulse + i * 0.13f) % 1f) * w * 0.20f)
    val r1 = r0 + (w * 0.22f)

    val x0 = cx + cos(angle) * r0
    val y0 = horizonY + sin(angle) * r0 * 0.65f
    val x1 = cx + cos(angle) * r1
    val y1 = horizonY + sin(angle) * r1 * 0.65f

    drawLine(
      color = lineColor,
      start = Offset(x0, y0),
      end = Offset(x1, y1),
      strokeWidth = if (isNitro) 2.5.dp.toPx() else 1.5.dp.toPx(),
      cap = StrokeCap.Round
    )
  }
}

// -------------------------------------------------------------------
// 9. WEATHER EFFECTS (RAIN STREAKS, HEAT SHIMMER, TWILIGHT MIST) & VICTORY CONFETTI
// -------------------------------------------------------------------
private fun DrawScope.drawRainStreaks(w: Float, h: Float, particleCount: Int, pulse: Float) {
  val rainColor = MetallicSilverLight.copy(alpha = 0.40f)
  val count = particleCount.coerceIn(16, 90)
  for (i in 0 until count) {
    val startX = ((i * 47f + pulse * 600f) % (w + 100f)) - 50f
    val startY = ((i * 39f + pulse * 1400f) % h)
    drawLine(
      color = rainColor,
      start = Offset(startX, startY),
      end = Offset(startX - 8f, startY + 28f),
      strokeWidth = 1.4f
    )
    // Wet tarmac droplet splash ring on High preset
    if (count >= 60 && i % 6 == 0 && startY > h * 0.55f) {
      drawOval(
        color = PureMotorsportWhite.copy(alpha = 0.22f),
        topLeft = Offset(startX - 12f, startY + 26f),
        size = Size(10f, 3.5f)
      )
    }
  }
}

private fun DrawScope.drawHeatShimmerHaze(w: Float, horizonY: Float, pulse: Float) {
  for (band in 0..2) {
    val yOffset = horizonY + (band * 8.dp.toPx()) + sin(pulse * 6.28f + band) * 3.dp.toPx()
    drawLine(
      color = Color(0xFFFFD54F).copy(alpha = 0.08f),
      start = Offset(0f, yOffset),
      end = Offset(w, yOffset),
      strokeWidth = 5.dp.toPx()
    )
  }
}

private fun DrawScope.drawTwilightHorizonMist(w: Float, horizonY: Float, pulse: Float) {
  val mistAlpha = 0.10f + 0.04f * sin(pulse * 6.28f)
  drawRect(
    brush = Brush.verticalGradient(
      colors = listOf(
        Color.Transparent,
        MotorsportRacingRed.copy(alpha = mistAlpha),
        Color(0xFF7C4DFF).copy(alpha = mistAlpha * 0.8f),
        Color.Transparent
      ),
      startY = horizonY - 22.dp.toPx(),
      endY = horizonY + 26.dp.toPx()
    ),
    topLeft = Offset(0f, horizonY - 22.dp.toPx()),
    size = Size(w, 48.dp.toPx())
  )
}

private fun DrawScope.drawVictoryConfetti(w: Float, h: Float, pulse: Float) {
  val colors = listOf(
    MotorsportRacingRed,
    PureMotorsportWhite,
    MetallicSilverLight,
    Color(0xFFFFD700)
  )
  for (i in 0 until 40) {
    val cX = ((i * 37f + pulse * 400f) % w)
    val cY = ((i * 53f + pulse * 700f) % h)
    val col = colors[i % colors.size]
    drawCircle(color = col, radius = 4.dp.toPx(), center = Offset(cX, cY))
  }
}
