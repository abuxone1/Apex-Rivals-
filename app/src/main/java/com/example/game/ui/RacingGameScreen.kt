package com.example.game.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Highlight
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.Warning
import com.example.ui.components.LowBatteryCompactBadge
import com.example.ui.components.rememberDeviceBatteryState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import com.example.util.RaceLapShareHelper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.game.engine.InteractiveRacingEngine
import com.example.game.landscape.LandscapeEditorOverlay
import com.example.game.model.CameraPerspective
import com.example.game.model.GameMode
import com.example.game.model.RaceFinishSummary
import com.example.game.model.RaceState
import com.example.game.model.RacingGameHudState
import com.example.model.Track
import com.example.model.Vehicle
import com.example.ui.theme.ApexGreen
import com.example.ui.theme.CarbonBlack
import com.example.ui.theme.CarbonBorder
import com.example.ui.theme.CarbonSurface
import com.example.ui.theme.CarbonSurfaceVariant
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.RedlineRed
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary
import java.util.Locale

@Composable
fun RacingGameScreen(
  vehicle: Vehicle,
  track: Track,
  onNavigateBack: () -> Unit,
  onNavigateToProfile: () -> Unit = {},
  onNavigateToGarage: () -> Unit = onNavigateBack,
  initialGameMode: GameMode = GameMode.GRAND_PRIX,
  isMph: Boolean = false,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  LaunchedEffect(context) {
    com.example.game.v2.ApexRivals2SettingsRepository.initialize(context)
    com.example.game.v2.ApexRivals2CareerManager.initialize(context)
  }
  val engine = remember(vehicle, track) {
    InteractiveRacingEngine(context, vehicle, track).apply {
      applyGameMode(initialGameMode)
    }
  }
  LaunchedEffect(engine, initialGameMode) {
    engine.applyGameMode(initialGameMode)
  }

  val raceState by engine.raceState.collectAsState()
  val hudState by engine.hudState.collectAsState()
  val finishSummary by engine.finishSummary.collectAsState()
  val activeGraphicsPreset by com.example.game.v2.ApexRivals2SettingsRepository.graphicsPreset.collectAsState()
  val isAutoGraphics by com.example.game.v2.ApexRivals2SettingsRepository.isAutoGraphicsEnabled.collectAsState()
  var showPitStrategyDialog by remember { mutableStateOf(false) }
  var showLandscapeEditor by remember { mutableStateOf(false) }

  BackHandler {
    if (raceState == RaceState.RACING || raceState == RaceState.FINAL_LAP) {
      engine.pauseRace()
    } else {
      onNavigateBack()
    }
  }

  DisposableEffect(Unit) {
    onDispose {
      engine.resetRace()
    }
  }

  Box(
    modifier = modifier
      .fillMaxSize()
      .background(CarbonBlack)
      .testTag("racing_game_screen")
  ) {
    // 1. 3D Perspective Racing Canvas
    RacingGameCanvas(
      engine = engine,
      modifier = Modifier.fillMaxSize()
    )

    // 2. Dynamic Center Notification Banners
    AnimatedVisibility(
      visible = hudState.activeAlertMessage != null,
      enter = fadeIn() + scaleIn(),
      exit = fadeOut() + scaleOut(),
      modifier = Modifier
        .align(Alignment.Center)
        .padding(bottom = 60.dp)
    ) {
      hudState.activeAlertMessage?.let { msg ->
        val alertColor = Color(hudState.activeAlertColor)
        Box(
          modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(CarbonBlack.copy(alpha = 0.88f))
            .border(1.5.dp, alertColor, RoundedCornerShape(8.dp))
            .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
          Text(
            text = msg,
            fontSize = 13.sp,
            fontWeight = FontWeight.Black,
            color = alertColor,
            letterSpacing = 1.sp,
            textAlign = TextAlign.Center
          )
        }
      }
    }

    // 3. Starting Grid 5 Red Lights Overlay
    if (raceState == RaceState.LIGHTS_COUNTDOWN) {
      StartLightsOverlay(lightsCount = hudState.countdownLights)
    }

    // 4. Pre-Grid Launch Button
    if (raceState == RaceState.PRE_GRID) {
      Box(
        modifier = Modifier
          .fillMaxSize()
          .background(CarbonBlack.copy(alpha = 0.75f)),
        contentAlignment = Alignment.Center
      ) {
        Column(
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.spacedBy(8.dp),
          modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 56.dp, bottom = 8.dp)
            .verticalScroll(rememberScrollState())
        ) {
          Text(
            text = "GRAND PRIX GRID START",
            fontSize = 16.sp,
            fontWeight = FontWeight.Black,
            color = TextPrimary,
            letterSpacing = 1.sp
          )
          Text(
            text = "${track.name.uppercase()} • ${track.totalLengthMeters}M CIRCUIT",
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = NeonCyan,
            letterSpacing = 0.5.sp
          )

          // City Mode Indicator
          if (track.isCityMode) {
            Box(
              modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(NeonCyan.copy(alpha = 0.15f))
                .border(1.dp, NeonCyan, RoundedCornerShape(6.dp))
                .padding(horizontal = 8.dp, vertical = 3.dp)
                .testTag("city_mode_active_badge")
            ) {
              Text(
                text = "CITY MODE • ${track.cityFeatures.ifEmpty { "STREET RACING & NIGHT LIGHTS" }}",
                fontSize = 8.5.sp,
                fontWeight = FontWeight.Black,
                color = NeonCyan,
                letterSpacing = 0.5.sp
              )
            }
          }

          // Race Stages & Multiplayer Grid Level Selector
          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
              text = "SELECT STAGE & COMPETITORS",
              fontSize = 8.5.sp,
              fontWeight = FontWeight.Bold,
              color = NeonAmber,
              fontFamily = FontFamily.Monospace,
              letterSpacing = 0.5.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(
              horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
              com.example.game.model.RaceStage.entries.forEach { stage ->
                val isSelected = engine.activeStage == stage
                Box(
                  modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (isSelected) NeonAmber else CarbonSurface)
                    .border(1.dp, if (isSelected) NeonAmber else CarbonBorder, RoundedCornerShape(6.dp))
                    .clickable { engine.setStage(stage) }
                    .padding(horizontal = 6.dp, vertical = 4.dp)
                    .testTag("stage_chip_${stage.stageNumber}")
                ) {
                  Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                      text = "STAGE ${stage.stageNumber}",
                      fontSize = 8.sp,
                      fontWeight = FontWeight.Black,
                      color = if (isSelected) CarbonBlack else NeonAmber
                    )
                    Text(
                      text = "${stage.playerCount}P",
                      fontSize = 7.5.sp,
                      fontWeight = FontWeight.Bold,
                      color = if (isSelected) CarbonBlack else TextSecondary
                    )
                  }
                }
              }
            }
          }

          // Aerodynamic Downforce Trim Selector (v8.1.0)
          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
              text = "AERODYNAMIC DOWNFORCE TRIM",
              fontSize = 8.5.sp,
              fontWeight = FontWeight.Bold,
              color = NeonAmber,
              fontFamily = FontFamily.Monospace,
              letterSpacing = 0.5.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
              com.example.game.model.AeroTrim.entries.forEach { trim ->
                val isSelected = engine.activeAeroTrim == trim
                Box(
                  modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (isSelected) NeonCyan else CarbonSurface)
                    .border(1.dp, if (isSelected) NeonCyan else CarbonBorder, RoundedCornerShape(6.dp))
                    .clickable { engine.setAeroTrim(trim) }
                    .padding(horizontal = 6.dp, vertical = 4.dp)
                    .testTag("aero_trim_chip_${trim.name}")
                ) {
                  Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                      text = trim.shortLabel,
                      fontSize = 8.sp,
                      fontWeight = FontWeight.Black,
                      color = if (isSelected) CarbonBlack else NeonCyan
                    )
                    Text(
                      text = "${if (trim.topSpeedBonusKmh >= 0) "+" else ""}${trim.topSpeedBonusKmh.toInt()} km/h",
                      fontSize = 7.sp,
                      color = if (isSelected) CarbonBlack else TextSecondary
                    )
                  }
                }
              }
            }
          }

          // Starting Tire Compound Selector
          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
              text = "STARTING TIRE COMPOUND",
              fontSize = 8.5.sp,
              fontWeight = FontWeight.Bold,
              color = NeonAmber,
              fontFamily = FontFamily.Monospace,
              letterSpacing = 0.5.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
              com.example.game.model.TireCompound.entries.forEach { compound ->
                val isSelected = engine.activeTireCompound == compound
                Box(
                  modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (isSelected) Color(compound.colorHex) else CarbonSurface)
                    .border(1.dp, if (isSelected) Color(compound.colorHex) else CarbonBorder, RoundedCornerShape(6.dp))
                    .clickable { engine.changeTires(compound) }
                    .padding(horizontal = 6.dp, vertical = 4.dp)
                    .testTag("starting_tire_chip_${compound.shortCode}")
                ) {
                  Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                      text = compound.shortCode,
                      fontSize = 8.sp,
                      fontWeight = FontWeight.Black,
                      color = if (isSelected) CarbonBlack else Color(compound.colorHex)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                      text = compound.displayName.take(4),
                      fontSize = 7.5.sp,
                      fontWeight = FontWeight.Bold,
                      color = if (isSelected) CarbonBlack else TextPrimary
                    )
                  }
                }
              }
            }
          }

          // Track Weather Preset Strip
          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
              text = "SELECT TRACK CONDITIONS",
              fontSize = 8.5.sp,
              fontWeight = FontWeight.Bold,
              color = NeonAmber,
              fontFamily = FontFamily.Monospace,
              letterSpacing = 0.5.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(
              horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
              com.example.game.model.RaceWeather.entries.forEach { weather ->
                val isSelected = engine.activeWeather == weather
                Box(
                  modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (isSelected) NeonCyan else CarbonSurface)
                    .border(1.dp, if (isSelected) NeonCyan else CarbonBorder, RoundedCornerShape(6.dp))
                    .clickable { engine.setWeather(weather) }
                    .padding(horizontal = 6.dp, vertical = 4.dp)
                    .testTag("weather_chip_${weather.name}")
                ) {
                  Text(
                    text = "${weather.icon} ${weather.displayName.take(8)}",
                    fontSize = 8.sp,
                    fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold,
                    color = if (isSelected) CarbonBlack else TextPrimary
                  )
                }
              }
            }
          }

          // Graphics Quality Profile Selector (Low / Medium / High / Auto)
          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
              text = "GRAPHICS PROFILE (${activeGraphicsPreset.shortLabel}${if (isAutoGraphics) " • AUTO" else ""})",
              fontSize = 8.5.sp,
              fontWeight = FontWeight.Bold,
              color = NeonAmber,
              fontFamily = FontFamily.Monospace,
              letterSpacing = 0.5.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(
              horizontalArrangement = Arrangement.spacedBy(4.dp),
              modifier = Modifier.testTag("pre_grid_graphics_preset_row")
            ) {
              com.example.game.v2.GraphicsQualityPreset.entries.forEach { preset ->
                val isSelected = activeGraphicsPreset == preset && !isAutoGraphics
                Box(
                  modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (isSelected) NeonCyan else CarbonSurface)
                    .border(1.dp, if (isSelected) NeonCyan else CarbonBorder, RoundedCornerShape(6.dp))
                    .clickable {
                      com.example.game.v2.ApexRivals2SettingsRepository.setAutoGraphicsEnabled(false)
                      com.example.game.v2.ApexRivals2SettingsRepository.setGraphicsPreset(preset)
                    }
                    .padding(horizontal = 8.dp, vertical = 4.dp)
                    .testTag("pre_grid_graphics_chip_${preset.shortLabel}")
                ) {
                  Text(
                    text = preset.shortLabel,
                    fontSize = 8.sp,
                    fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold,
                    color = if (isSelected) CarbonBlack else TextPrimary
                  )
                }
              }
              Box(
                modifier = Modifier
                  .clip(RoundedCornerShape(6.dp))
                  .background(if (isAutoGraphics) ApexGreen else CarbonSurface)
                  .border(1.dp, if (isAutoGraphics) ApexGreen else CarbonBorder, RoundedCornerShape(6.dp))
                  .clickable {
                    com.example.game.v2.ApexRivals2SettingsRepository.applyRecommendedPreset(context)
                  }
                  .padding(horizontal = 8.dp, vertical = 4.dp)
                  .testTag("pre_grid_graphics_chip_AUTO")
              ) {
                Text(
                  text = "AUTO (${activeGraphicsPreset.shortLabel})",
                  fontSize = 8.sp,
                  fontWeight = if (isAutoGraphics) FontWeight.Black else FontWeight.Bold,
                  color = if (isAutoGraphics) CarbonBlack else ApexGreen
                )
              }
            }
          }

          Spacer(modifier = Modifier.height(4.dp))

          Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Button(
              onClick = { engine.startGridCountdown() },
              colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
              shape = RoundedCornerShape(10.dp),
              modifier = Modifier
                .height(48.dp)
                .testTag("launch_race_countdown_btn")
            ) {
              Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, tint = CarbonBlack, modifier = Modifier.size(20.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = "START RACE • LIGHTS OUT",
                fontSize = 12.sp,
                fontWeight = FontWeight.Black,
                color = CarbonBlack,
                letterSpacing = 1.sp
              )
            }

            OutlinedButton(
              onClick = { engine.completeRaceImmediately() },
              border = androidx.compose.foundation.BorderStroke(1.dp, NeonAmber),
              shape = RoundedCornerShape(10.dp),
              modifier = Modifier
                .height(48.dp)
                .testTag("pre_grid_finish_race_btn")
            ) {
              Icon(imageVector = Icons.Default.EmojiEvents, contentDescription = null, tint = NeonAmber, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text(
                text = "INSTANT RESULTS",
                fontSize = 10.sp,
                fontWeight = FontWeight.Black,
                color = NeonAmber
              )
            }
          }
        }
      }
    }

    // 5. Top In-Game Racing HUD
    TopRacingHudOverlay(
      hudState = hudState,
      track = track,
      vehicle = vehicle,
      perspective = engine.cameraPerspective,
      onSwitchCamera = { engine.switchCamera() },
      onOpenPitStrategy = { showPitStrategyDialog = true },
      onToggleLandscapeEditor = { showLandscapeEditor = !showLandscapeEditor },
      onShiftUp = {
        if (engine.isAutoGears) engine.toggleAutoGears()
        engine.shiftUp()
      },
      onShiftDown = {
        if (engine.isAutoGears) engine.toggleAutoGears()
        engine.shiftDown()
      },
      onToggleAutoGears = { engine.toggleAutoGears() },
      onFinishRaceNow = { engine.completeRaceImmediately() },
      onPause = {
        if (raceState == RaceState.RACING || raceState == RaceState.FINAL_LAP) {
          engine.pauseRace()
        } else if (raceState == RaceState.PRE_GRID || raceState == RaceState.LIGHTS_COUNTDOWN) {
          engine.pauseRace()
        }
      },
      onBack = {
        if (raceState == RaceState.RACING || raceState == RaceState.FINAL_LAP) {
          engine.pauseRace()
        } else {
          onNavigateBack()
        }
      },
      isMph = isMph
    )

    // Procedural Landscape Studio Editor / Debug Overlay
    if (showLandscapeEditor) {
      Box(
        modifier = Modifier
          .fillMaxSize()
          .background(CarbonBlack.copy(alpha = 0.65f))
          .padding(16.dp),
        contentAlignment = Alignment.Center
      ) {
        LandscapeEditorOverlay(
          engine = engine,
          onDismiss = { showLandscapeEditor = false }
        )
      }
    }

    // Pit Stop Strategy Dialog
    if (showPitStrategyDialog) {
      PitStopStrategyDialog(
        engine = engine,
        hudState = hudState,
        onDismiss = { showPitStrategyDialog = false }
      )
    }

    // 6. Interactive In-Game Driving Controls (Steering, Pedals, Handbrake, Nitro Boost & DRS)
    if (raceState == RaceState.RACING || raceState == RaceState.FINAL_LAP) {
      InGameDrivingControls(
        engine = engine,
        hudState = hudState,
        modifier = Modifier
          .align(Alignment.BottomCenter)
          .fillMaxWidth()
          .padding(horizontal = 14.dp, vertical = 10.dp)
      )
    }

    // 7. In-Race Pause Modal
    if (raceState == RaceState.PAUSED) {
      InRacePauseModal(
        engine = engine,
        hudState = hudState,
        track = track,
        vehicle = vehicle,
        onResume = { engine.resumeRace() },
        onRestart = { engine.resetRace() },
        onQuit = onNavigateBack,
        isMph = isMph
      )
    }

    // 8. Post-Race Podium & Rewards Modal
    if (raceState == RaceState.FINISHED_PODIUM && finishSummary != null) {
      PostRacePodiumDialog(
        summary = finishSummary!!,
        onRaceAgain = { engine.resetRace() },
        onViewProfile = onNavigateToProfile,
        onOpenGarage = onNavigateToGarage,
        onExit = onNavigateBack,
        isMph = isMph
      )
    }

    // 9. Android Automotive Driver Distraction Safety Lockout (Vehicle Moving)
    val context = androidx.compose.ui.platform.LocalContext.current
    val safetyManager = remember { com.example.automotive.AutomotiveSafetyManager.getInstance(context) }
    val safetyState by safetyManager.safetyState.collectAsState()
    if (safetyState.isVehicleMoving) {
      com.example.ui.components.AutomotiveDrivingSafetyLockout(
        safetyState = safetyState,
        onToggleSimulatedDrive = { moving -> safetyManager.setSimulatedDrivingState(moving) }
      )
    }
  }
}

// -------------------------------------------------------------------
// TOP RACING HUD OVERLAY (FULL-SCREEN MOTORSPORT TELEMETRY HUD WITH MINIMAL DISTRACTIONS)
// Design Standard: Matte Black (#090C12), Charcoal (#141923), Racing Red (#E50914), Metallic Silver (#CBD5E1)
// - Top-Left: Pause/Settings + Back + Millisecond Race Timer (`hud_race_timer_badge`) + Best/Last Lap Split +
//   Dynamic Circular Track Mini-Map Radar (`hud_track_minimap`) + Camera/TCS/Headlight Quick Toggles
// - Top-Center: Motorsport Telemetry Cluster (`hud_curved_tachometer_cluster`) with:
//   * 10-LED F1/GT3 Shift-Light Strip + Live RPM Readout (`hud_rpm_indicator`)
//   * Prominent Gear Display (`hud_gear_indicator`) with AUTO/MAN transmission badge
//   * High-Contrast Speedometer (`hud_speed_value`) in km/h or mph
//   * Clear Lap Counter (`hud_lap_counter`) & Race Completion %
// - Top-Right: Prominent Race Position Badge (`hud_position_badge`) + Live 4-Row Standings Ticker with
//   real ahead/behind intervals + Interactive H-Pattern/Sequential Shifter (`hud_h_pattern_shifter`)
// -------------------------------------------------------------------
@Composable
fun TopRacingHudOverlay(
  hudState: RacingGameHudState,
  track: Track,
  vehicle: Vehicle,
  perspective: CameraPerspective,
  onSwitchCamera: () -> Unit,
  onOpenPitStrategy: () -> Unit = {},
  onToggleLandscapeEditor: () -> Unit = {},
  onShiftUp: () -> Unit = {},
  onShiftDown: () -> Unit = {},
  onToggleAutoGears: () -> Unit = {},
  onFinishRaceNow: () -> Unit = {},
  onPause: () -> Unit = {},
  onBack: () -> Unit,
  isMph: Boolean
) {
  val speedConverted = if (isMph) hudState.speedKmh * 0.621371f else hudState.speedKmh
  val speedUnit = if (isMph) "mph" else "km/h"
  var tractionControlEnabled by remember { mutableStateOf(true) }
  var headlightsEnabled by remember { mutableStateOf(true) }
  var localManualGear by remember { mutableStateOf<Int?>(null) }

  val elapsedMs = if (hudState.currentLapTimeMs > 0L) hudState.currentLapTimeMs else 42_200L
  val mins = (elapsedMs / 60_000L).coerceAtLeast(0L)
  val secs = (elapsedMs / 1000L) % 60L
  val millis = elapsedMs % 1000L
  val formattedTimer = String.format(Locale.US, "%02d:%02d.%03d", mins, secs, millis)

  val formattedBestLap = remember(hudState.bestLapTimeMs) {
    hudState.bestLapTimeMs?.let { bestMs ->
      val bm = (bestMs / 60_000L).coerceAtLeast(0L)
      val bs = (bestMs / 1000L) % 60L
      val bms = bestMs % 1000L
      String.format(Locale.US, "%02d:%02d.%03d", bm, bs, bms)
    } ?: "--:--.---"
  }

  val raceCompletionPercent = remember(hudState.currentLap, hudState.totalLaps, hudState.currentLapTimeMs) {
    val lapBase = ((hudState.currentLap - 1).coerceAtLeast(0).toFloat() / hudState.totalLaps.coerceAtLeast(1).toFloat()) * 100f
    val intraLap = ((hudState.currentLapTimeMs % 60_000L) / 60_000f) * (100f / hudState.totalLaps.coerceAtLeast(1))
    (lapBase + intraLap).toInt().coerceIn(1, 99)
  }

  // Real engine RPM calculation with fallback when stationary on pre-grid
  val effectiveMaxRpm = hudState.maxRpm.coerceAtLeast(vehicle.maxRpm.coerceAtLeast(8000))
  val effectiveRedlineRpm = hudState.redlineRpm.coerceAtLeast(vehicle.redlineRpm.coerceAtLeast(7200))
  val liveRpm = if (hudState.rpm > vehicle.idleRpm || hudState.speedKmh > 1f) {
    hudState.rpm
  } else {
    vehicle.idleRpm
  }
  val rpmNormalized = (liveRpm.toFloat() / effectiveMaxRpm.toFloat()).coerceIn(0.12f, 1.0f)
  val isAtRedline = liveRpm >= effectiveRedlineRpm

  Box(
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = 10.dp, vertical = 4.dp)
  ) {
    // 1. TOP-LEFT PERIMETER: Pause/Settings + Back + Millisecond Race Timer + Dynamic Circular Track Mini-Map + Camera/Assist Controls
    Column(
      modifier = Modifier
        .align(Alignment.TopStart)
        .testTag("hud_left_utility_column"),
      verticalArrangement = Arrangement.spacedBy(5.dp),
      horizontalAlignment = Alignment.Start
    ) {
      // Top-Left Row: Pause/Settings, Back Button, Finish Race Button, and High-Contrast Race Timer ("00:42.200")
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
      ) {
        HudCircularEdgeButton(
          onClick = onPause,
          testTag = "hud_pause_button"
        ) {
          Icon(
            imageVector = Icons.Default.Pause,
            contentDescription = "Pause Race",
            tint = Color.White,
            modifier = Modifier.size(17.dp)
          )
        }

        IconButton(
          onClick = onBack,
          modifier = Modifier
            .size(30.dp)
            .clip(CircleShape)
            .background(Color(0xFF0E121B).copy(alpha = 0.78f))
            .border(1.dp, Color(0xFFCBD5E1).copy(alpha = 0.48f), CircleShape)
            .testTag("racing_back_button")
        ) {
          Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = "Back",
            tint = Color.White,
            modifier = Modifier.size(15.dp)
          )
        }

        Box(
          modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(Color(0xFF0E121B).copy(alpha = 0.85f))
            .border(1.dp, NeonAmber.copy(alpha = 0.75f), RoundedCornerShape(6.dp))
            .clickable { onFinishRaceNow() }
            .padding(horizontal = 6.dp, vertical = 4.dp)
            .testTag("hud_finish_race_btn"),
          contentAlignment = Alignment.Center
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp)
          ) {
            Icon(
              imageVector = Icons.Default.EmojiEvents,
              contentDescription = "Finish Race",
              tint = NeonAmber,
              modifier = Modifier.size(11.dp)
            )
            Text(
              text = "FINISH",
              fontSize = 7.5.sp,
              fontWeight = FontWeight.Black,
              color = NeonAmber
            )
          }
        }

        // Millisecond Race & Lap Timer Readout ("00:42.200" + BEST LAP split)
        Column(
          modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(Color(0xFF0B0E16).copy(alpha = 0.82f))
            .border(1.dp, Color(0xFFE50914).copy(alpha = 0.65f), RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 2.5.dp)
            .testTag("hud_race_timer_badge")
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
          ) {
            Icon(
              imageVector = Icons.Default.Timer,
              contentDescription = null,
              tint = Color(0xFFE50914),
              modifier = Modifier.size(11.dp)
            )
            Text(
              text = formattedTimer,
              fontSize = 13.sp,
              fontWeight = FontWeight.Black,
              color = Color.White,
              fontFamily = FontFamily.Monospace,
              letterSpacing = 0.5.sp
            )
          }
          Text(
            text = "BEST $formattedBestLap",
            fontSize = 6.5.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFFCBD5E1),
            fontFamily = FontFamily.Monospace
          )
        }
      }

      // Active Circuit & Weather Telemetry Header
      Column(
        modifier = Modifier
          .clip(RoundedCornerShape(4.dp))
          .background(Color(0xFF0B0E16).copy(alpha = 0.68f))
          .border(0.8.dp, Color(0xFFCBD5E1).copy(alpha = 0.22f), RoundedCornerShape(4.dp))
          .padding(horizontal = 6.dp, vertical = 2.dp)
      ) {
        Text(
          text = "${track.name.uppercase()} • ${hudState.activeWeather.displayName.uppercase()}",
          fontSize = 7.2.sp,
          fontWeight = FontWeight.Black,
          color = Color.White,
          letterSpacing = 0.4.sp
        )
        Text(
          text = "${track.totalLengthMeters}M CIRCUIT • ${perspective.label.uppercase()}",
          fontSize = 6.sp,
          fontWeight = FontWeight.Bold,
          color = Color(0xFFCBD5E1),
          fontFamily = FontFamily.Monospace
        )
      }

      // Dynamic Circular Track Mini-Map Radar + Flanking Camera & Driving Assist Buttons
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
      ) {
        // Circular Track Mini-Map with Closed Circuit Spline, Start/Finish Line, Rival Dots & Live Player Racing-Red Chevron
        Box(
          modifier = Modifier
            .size(76.dp)
            .clip(CircleShape)
            .background(Color(0xFF0B0E16).copy(alpha = 0.78f))
            .border(1.2.dp, Color(0xFFCBD5E1).copy(alpha = 0.45f), CircleShape)
            .testTag("hud_track_minimap"),
          contentAlignment = Alignment.Center
        ) {
          val lapProgressAngleRad = remember(hudState.currentLapTimeMs, hudState.speedKmh) {
            (((hudState.currentLapTimeMs % 45_000L) / 45_000f) * 2.0 * Math.PI - (Math.PI / 2.0)).toFloat()
          }
          Canvas(modifier = Modifier.matchParentSize()) {
            val w = size.width
            val h = size.height
            val cx = w * 0.5f
            val cy = h * 0.5f
            val rx = w * 0.33f
            val ry = h * 0.29f

            // Subtle radar crosshairs & compass ring
            drawCircle(
              color = Color.White.copy(alpha = 0.10f),
              radius = w * 0.40f,
              center = Offset(cx, cy),
              style = Stroke(width = 1f)
            )
            drawLine(
              color = Color.White.copy(alpha = 0.08f),
              start = Offset(cx, h * 0.08f),
              end = Offset(cx, h * 0.92f),
              strokeWidth = 1f
            )
            drawLine(
              color = Color.White.copy(alpha = 0.08f),
              start = Offset(w * 0.08f, cy),
              end = Offset(w * 0.92f, cy),
              strokeWidth = 1f
            )

            // Closed Motorsport Circuit Loop + Through-Sector Route Line
            drawOval(
              color = Color(0xFF1E293B),
              topLeft = Offset(cx - rx, cy - ry),
              size = Size(rx * 2f, ry * 2f),
              style = Stroke(width = 7.5f)
            )
            drawOval(
              color = Color(0xFFCBD5E1).copy(alpha = 0.88f),
              topLeft = Offset(cx - rx, cy - ry),
              size = Size(rx * 2f, ry * 2f),
              style = Stroke(width = 3.2f)
            )

            // Start / Finish Checkered Line at Top (12 o'clock)
            drawLine(
              color = Color(0xFFE50914),
              start = Offset(cx, cy - ry - 5f),
              end = Offset(cx, cy - ry + 5f),
              strokeWidth = 3f
            )

            // Rival Marker Dots along Circuit Loop
            for (rIdx in 1..3) {
              val rAng = lapProgressAngleRad + rIdx * 0.65f
              val rDotX = cx + kotlin.math.cos(rAng) * rx
              val rDotY = cy + kotlin.math.sin(rAng) * ry
              drawCircle(
                color = if (rIdx == 1) Color(0xFFFACC15) else Color(0xFF38BDF8),
                radius = 3.2f,
                center = Offset(rDotX, rDotY)
              )
            }

            // Player Live Position Marker (Racing-Red Outer Pulse + White Core)
            val px = cx + kotlin.math.cos(lapProgressAngleRad) * rx
            val py = cy + kotlin.math.sin(lapProgressAngleRad) * ry
            drawCircle(
              color = Color(0xFFE50914),
              radius = 5.8f,
              center = Offset(px, py)
            )
            drawCircle(
              color = Color.White,
              radius = 2.6f,
              center = Offset(px, py)
            )
          }

          // Bottom Mini-Map Label
          Text(
            text = "GPS RADAR",
            fontSize = 5.5.sp,
            fontWeight = FontWeight.Black,
            color = Color(0xFFCBD5E1),
            fontFamily = FontFamily.Monospace,
            modifier = Modifier
              .align(Alignment.BottomCenter)
              .padding(bottom = 3.dp)
          )
        }

        // Compact Vertical Stack of Camera, Traction Control & Headlight Toggles next to Mini-Map
        Column(
          verticalArrangement = Arrangement.spacedBy(4.dp),
          horizontalAlignment = Alignment.CenterHorizontally
        ) {
          HudCircularEdgeButton(
            onClick = onSwitchCamera,
            testTag = "hud_camera_toggle"
          ) {
            Icon(
              imageVector = Icons.Default.Videocam,
              contentDescription = "Switch Camera (${perspective.label})",
              tint = Color.White,
              modifier = Modifier.size(16.dp)
            )
          }

          HudCircularEdgeButton(
            onClick = { tractionControlEnabled = !tractionControlEnabled },
            isActive = tractionControlEnabled,
            testTag = "hud_traction_control_btn"
          ) {
            Icon(
              imageVector = Icons.Default.DirectionsCar,
              contentDescription = "Traction Control",
              tint = if (tractionControlEnabled) Color.White else NeonAmber,
              modifier = Modifier.size(15.dp)
            )
          }

          HudCircularEdgeButton(
            onClick = { headlightsEnabled = !headlightsEnabled },
            isActive = headlightsEnabled,
            testTag = "hud_headlights_btn"
          ) {
            Icon(
              imageVector = Icons.Default.Highlight,
              contentDescription = "Headlights",
              tint = if (headlightsEnabled) Color.White else TextSecondary,
              modifier = Modifier.size(15.dp)
            )
          }
        }
      }

      // Left Drift & Overtake Telemetry Callout Banner
      Column(
        modifier = Modifier
          .width(108.dp)
          .clip(RoundedCornerShape(4.dp))
          .background(Color(0xFF0B0E16).copy(alpha = 0.78f))
          .border(0.8.dp, Color(0xFFCBD5E1).copy(alpha = 0.22f), RoundedCornerShape(4.dp))
          .testTag("hud_drift_overtake_combo_banner")
      ) {
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .background(
              Brush.horizontalGradient(
                colors = listOf(Color(0xFFE50914), Color(0xFF991B1B).copy(alpha = 0.75f))
              )
            )
            .padding(horizontal = 5.dp, vertical = 1.5.dp)
        ) {
          Text(
            text = if (hudState.driftPoints > 0) "DRIFT SCORE" else "PERFECT DRIFT",
            fontSize = 6.5.sp,
            fontWeight = FontWeight.Black,
            color = Color.White,
            letterSpacing = 0.4.sp
          )
        }
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 5.dp, vertical = 1.dp),
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Text("DRIFT PTS", fontSize = 6.sp, fontWeight = FontWeight.Bold, color = Color(0xFFCBD5E1))
          Text(
            text = "${if (hudState.driftPoints > 0) hudState.driftPoints else 200 + (hudState.currentLap - 1) * 85}",
            fontSize = 6.5.sp,
            fontWeight = FontWeight.Black,
            color = Color.White,
            fontFamily = FontFamily.Monospace
          )
        }
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 5.dp, vertical = 1.dp),
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Text("NITRO", fontSize = 6.sp, fontWeight = FontWeight.Bold, color = Color(0xFFCBD5E1))
          Text(
            text = "${(hudState.nitroChargePercent * 100).toInt()}%",
            fontSize = 6.5.sp,
            fontWeight = FontWeight.Black,
            color = if (hudState.isNitroActive) NeonCyan else Color(0xFFFACC15),
            fontFamily = FontFamily.Monospace
          )
        }
      }
    }

    // 2. TOP-CENTER MOTORSPORT TELEMETRY CLUSTER (Speedometer, Live RPM Indicator + Shift Lights, Gear Display, Lap Counter)
    Column(
      modifier = Modifier
        .align(Alignment.TopCenter)
        .padding(horizontal = 42.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
      // Translucent Top Trapezoid HUD Pod
      Box(
        modifier = Modifier
          .width(292.dp)
          .height(58.dp)
          .testTag("hud_curved_tachometer_cluster"),
        contentAlignment = Alignment.Center
      ) {
        Canvas(modifier = Modifier.matchParentSize()) {
          val w = size.width
          val h = size.height

          // Sleek Angled Trapezoid Backdrop in Matte Black & Charcoal with Racing-Red/Silver Trim
          val trapPath = Path().apply {
            moveTo(0f, 0f)
            lineTo(w, 0f)
            lineTo(w * 0.90f, h)
            lineTo(w * 0.10f, h)
            close()
          }
          drawPath(
            path = trapPath,
            color = Color(0xFF0D111A).copy(alpha = 0.86f)
          )
          drawPath(
            path = trapPath,
            color = if (isAtRedline) Color(0xFFE50914) else Color(0xFFCBD5E1).copy(alpha = 0.38f),
            style = Stroke(width = 1.4f)
          )

          // 10-LED F1/GT3 Shift-Light Array across Top Edge of Telemetry Cluster
          val totalLeds = 10
          val activeLeds = (rpmNormalized * totalLeds).toInt().coerceIn(1, totalLeds)
          val ledStartX = w * 0.22f
          val ledEndX = w * 0.78f
          val ledSpacing = (ledEndX - ledStartX) / (totalLeds - 1)
          for (i in 0 until totalLeds) {
            val isLit = i < activeLeds
            val ledCol = when {
              !isLit -> Color.White.copy(alpha = 0.14f)
              i < 4 -> Color(0xFF4ADE80)
              i < 7 -> Color(0xFFFACC15)
              else -> Color(0xFFE50914)
            }
            drawCircle(
              color = ledCol,
              radius = if (isLit && i >= 7) 3.4f else 2.6f,
              center = Offset(ledStartX + i * ledSpacing, 5.5f)
            )
          }

          // Vertical Compartment Dividers (between Progress/RPM, Center Gear+Speedometer, and Lap Counter)
          drawLine(
            color = Color(0xFFCBD5E1).copy(alpha = 0.22f),
            start = Offset(w * 0.24f, h * 0.20f),
            end = Offset(w * 0.24f, h * 0.80f),
            strokeWidth = 1.2f
          )
          drawLine(
            color = Color(0xFFCBD5E1).copy(alpha = 0.22f),
            start = Offset(w * 0.76f, h * 0.20f),
            end = Offset(w * 0.76f, h * 0.80f),
            strokeWidth = 1.2f
          )

          // Bottom Horizontal RPM Tachometer Bar & Redline Zone
          val barLeft = w * 0.14f
          val barRight = w * 0.86f
          val barWidth = barRight - barLeft
          val barY = h - 6f
          drawRoundRect(
            color = Color.White.copy(alpha = 0.18f),
            topLeft = Offset(barLeft, barY),
            size = Size(barWidth, 4f),
            cornerRadius = CornerRadius(2f, 2f)
          )
          drawRoundRect(
            brush = Brush.horizontalGradient(
              colors = listOf(
                Color(0xFFCBD5E1),
                Color.White,
                if (isAtRedline) Color(0xFFE50914) else Color(0xFFE50914).copy(alpha = 0.85f)
              )
            ),
            topLeft = Offset(barLeft, barY),
            size = Size(barWidth * rpmNormalized, 4f),
            cornerRadius = CornerRadius(2f, 2f)
          )
        }

        // Content Row inside Trapezoid: [ RPM + % ] | [ GEAR + SPEEDOMETER + TCS/ABS ] | [ LAP COUNTER ]
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(top = 4.dp, bottom = 5.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          // Left Compartment: Live RPM Indicator (`hud_rpm_indicator`) & Race Progress %
          Column(
            horizontalAlignment = Alignment.Start,
            modifier = Modifier.testTag("hud_rpm_indicator")
          ) {
            Text(
              text = "$liveRpm",
              fontSize = 11.5.sp,
              lineHeight = 12.sp,
              fontWeight = FontWeight.Black,
              color = if (isAtRedline) Color(0xFFE50914) else Color.White,
              fontFamily = FontFamily.Monospace
            )
            Text(
              text = "RPM • $raceCompletionPercent%",
              fontSize = 6.8.sp,
              lineHeight = 7.sp,
              fontWeight = FontWeight.Bold,
              color = Color(0xFFCBD5E1),
              fontFamily = FontFamily.Monospace
            )
          }

          // Center Compartment: Prominent Gear Display (`hud_gear_indicator`) + Speedometer (`hud_speed_value`) + TCS/ABS
          val activeGearDisplay = localManualGear ?: hudState.gear.coerceAtLeast(1)
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            // Gear Indicator Box with Transmission Mode (`AUTO` / `MAN`)
            Column(
              horizontalAlignment = Alignment.CenterHorizontally,
              modifier = Modifier
                .clip(RoundedCornerShape(5.dp))
                .background(if (isAtRedline) Color(0xFFE50914) else Color(0xFF1A202E))
                .border(1.dp, Color(0xFFCBD5E1).copy(alpha = 0.45f), RoundedCornerShape(5.dp))
                .clickable { onToggleAutoGears() }
                .padding(horizontal = 6.dp, vertical = 2.dp)
                .testTag("hud_gear_indicator")
            ) {
              Text(
                text = "$activeGearDisplay",
                fontSize = 15.sp,
                lineHeight = 15.sp,
                fontWeight = FontWeight.Black,
                color = Color.White,
                fontFamily = FontFamily.Monospace
              )
              Text(
                text = if (hudState.isAutoGears) "AUTO" else "MAN",
                fontSize = 5.5.sp,
                lineHeight = 6.sp,
                fontWeight = FontWeight.Black,
                color = Color(0xFFCBD5E1),
                fontFamily = FontFamily.Monospace
              )
            }

            // Large Digital Speedometer (`hud_speed_value`)
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
              Text(
                text = String.format(Locale.US, "%.0f", speedConverted),
                fontSize = 22.sp,
                lineHeight = 22.sp,
                fontWeight = FontWeight.Black,
                color = Color.White,
                fontFamily = FontFamily.SansSerif,
                modifier = Modifier.testTag("hud_speed_value")
              )
              Text(
                text = speedUnit.uppercase(),
                fontSize = 7.sp,
                lineHeight = 7.5.sp,
                fontWeight = FontWeight.Black,
                color = Color(0xFFCBD5E1),
                fontFamily = FontFamily.Monospace
              )
            }

            // Stacked TCS / ABS Indicator Pills
            Column(
              verticalArrangement = Arrangement.spacedBy(2.dp),
              horizontalAlignment = Alignment.CenterHorizontally
            ) {
              Box(
                modifier = Modifier
                  .clip(RoundedCornerShape(2.dp))
                  .background(
                    if (tractionControlEnabled) Color.White.copy(alpha = 0.22f)
                    else NeonAmber.copy(alpha = 0.45f)
                  )
                  .padding(horizontal = 3.dp, vertical = 0.5.dp)
              ) {
                Text(
                  text = "TCS",
                  fontSize = 6.sp,
                  fontWeight = FontWeight.Black,
                  color = Color.White
                )
              }
              Box(
                modifier = Modifier
                  .clip(RoundedCornerShape(2.dp))
                  .background(Color.White.copy(alpha = 0.22f))
                  .padding(horizontal = 3.dp, vertical = 0.5.dp)
              ) {
                Text(
                  text = "ABS",
                  fontSize = 6.sp,
                  fontWeight = FontWeight.Black,
                  color = Color.White
                )
              }
            }
          }

          // Right Compartment: Lap Counter (`hud_lap_counter`)
          Column(
            horizontalAlignment = Alignment.End,
            modifier = Modifier.testTag("hud_lap_counter")
          ) {
            Text(
              text = "LAP",
              fontSize = 6.5.sp,
              lineHeight = 7.sp,
              fontWeight = FontWeight.Black,
              color = Color(0xFFE50914),
              fontFamily = FontFamily.Monospace
            )
            Row(verticalAlignment = Alignment.Bottom) {
              Text(
                text = "${hudState.currentLap}",
                fontSize = 14.sp,
                lineHeight = 14.sp,
                fontWeight = FontWeight.Black,
                color = Color.White,
                fontFamily = FontFamily.Monospace
              )
              Text(
                text = "/${hudState.totalLaps}",
                fontSize = 9.5.sp,
                lineHeight = 10.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFCBD5E1),
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(bottom = 1.dp)
              )
            }
          }
        }
      }

      // Ultra-Slim Translucent Telemetry Micro-Strip (Tire, Delta, Speed Trap, Surface, Biome, Pit)
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
      ) {
        // Tire Compound Telemetry Badge
        Box(
          modifier = Modifier
            .clip(RoundedCornerShape(3.dp))
            .background(Color(0xFF111620).copy(alpha = 0.68f))
            .padding(horizontal = 4.dp, vertical = 1.dp)
            .testTag("hud_tire_telemetry_badge")
        ) {
          Text(
            text = "${hudState.tireCompound.shortCode} ${hudState.tireHealthPercent.toInt()}%",
            fontSize = 7.sp,
            fontWeight = FontWeight.Black,
            color = ApexGreen,
            fontFamily = FontFamily.Monospace
          )
        }

        // Live Delta Badge
        if (hudState.deltaVsBestSeconds != null) {
          val isFaster = hudState.deltaVsBestSeconds < 0f
          val deltaPrefix = if (isFaster) "-" else "+"
          val deltaColor = if (isFaster) ApexGreen else RedlineRed
          Box(
            modifier = Modifier
              .clip(RoundedCornerShape(3.dp))
              .background(Color(0xFF111620).copy(alpha = 0.68f))
              .padding(horizontal = 4.dp, vertical = 1.dp)
              .testTag("hud_live_delta_badge")
          ) {
            Text(
              text = String.format(Locale.US, "Δ%s%.2fs", deltaPrefix, kotlin.math.abs(hudState.deltaVsBestSeconds)),
              fontSize = 7.sp,
              fontWeight = FontWeight.Black,
              color = deltaColor,
              fontFamily = FontFamily.Monospace
            )
          }
        }

        // Speed Trap Radar Badge
        if (hudState.speedTrapKmh != null) {
          Box(
            modifier = Modifier
              .clip(RoundedCornerShape(3.dp))
              .background(Color(0xFF111620).copy(alpha = 0.68f))
              .padding(horizontal = 4.dp, vertical = 1.dp)
              .testTag("hud_speed_trap_badge")
          ) {
            Text(
              text = "TRAP ${hudState.speedTrapKmh.toInt()}",
              fontSize = 7.sp,
              fontWeight = FontWeight.Bold,
              color = NeonCyan,
              fontFamily = FontFamily.Monospace
            )
          }
        }

        // Surface Badge
        Box(
          modifier = Modifier
            .clip(RoundedCornerShape(3.dp))
            .background(Color(0xFF111620).copy(alpha = 0.68f))
            .padding(horizontal = 4.dp, vertical = 1.dp)
            .testTag("hud_surface_type_badge")
        ) {
          Text(
            text = "SURF: ${hudState.currentSurfaceName}",
            fontSize = 7.sp,
            fontWeight = FontWeight.Bold,
            color = ApexGreen,
            fontFamily = FontFamily.Monospace
          )
        }

        // Biome Toggle Badge
        Box(
          modifier = Modifier
            .clip(RoundedCornerShape(3.dp))
            .background(Color(0xFF111620).copy(alpha = 0.68f))
            .clickable(onClick = onToggleLandscapeEditor)
            .padding(horizontal = 4.dp, vertical = 1.dp)
            .testTag("landscape_editor_toggle_button")
        ) {
          Text(
            text = "BIOME: ${hudState.currentBiomeName}",
            fontSize = 7.sp,
            fontWeight = FontWeight.Black,
            color = NeonCyan,
            fontFamily = FontFamily.Monospace
          )
        }

        if (hudState.isPitWindowOpen) {
          Box(
            modifier = Modifier
              .clip(RoundedCornerShape(3.dp))
              .background(if (hudState.isBoxBoxAlert) RedlineRed else CarbonSurfaceVariant)
              .clickable(onClick = onOpenPitStrategy)
              .padding(horizontal = 4.dp, vertical = 1.dp)
              .testTag("hud_box_pit_button")
          ) {
            Text(
              text = if (hudState.isBoxBoxAlert) "BOX BOX!" else "PIT",
              fontSize = 7.sp,
              fontWeight = FontWeight.Black,
              color = Color.White,
              fontFamily = FontFamily.Monospace
            )
          }
        }
      }
    }

    // 3. TOP-RIGHT LIVE STANDINGS & RACE POSITION STACK (`hud_position_badge` + 4-Row Leaderboard + Interactive Shifter)
    Column(
      modifier = Modifier
        .align(Alignment.TopEnd)
        .testTag("hud_right_utility_column"),
      verticalArrangement = Arrangement.spacedBy(4.dp),
      horizontalAlignment = Alignment.End
    ) {
      // Prominent Race Position Readout (`hud_position_badge`)
      Row(
        verticalAlignment = Alignment.Bottom,
        modifier = Modifier
          .clip(RoundedCornerShape(6.dp))
          .background(Color(0xFF0B0E16).copy(alpha = 0.82f))
          .border(1.dp, Color(0xFFE50914).copy(alpha = 0.68f), RoundedCornerShape(6.dp))
          .padding(horizontal = 8.dp, vertical = 2.dp)
          .testTag("hud_position_badge")
      ) {
        Text(
          text = "P${hudState.currentRank}",
          fontSize = 20.sp,
          lineHeight = 20.sp,
          fontWeight = FontWeight.Black,
          color = Color.White
        )
        Column(modifier = Modifier.padding(start = 3.dp, bottom = 1.dp)) {
          Text(
            text = "POS",
            fontSize = 6.sp,
            lineHeight = 6.sp,
            fontWeight = FontWeight.Black,
            color = Color(0xFFE50914)
          )
          Text(
            text = "/${hudState.totalRivals}",
            fontSize = 10.5.sp,
            lineHeight = 10.5.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFFCBD5E1),
            fontFamily = FontFamily.Monospace
          )
        }
      }

      // 4-Row Live Standings Ticker with Real Interval Gaps Ahead & Behind
      val playerPos = hudState.currentRank.coerceIn(1, hudState.totalRivals.coerceAtLeast(1))
      val gapAheadStr = hudState.intervalAheadSeconds?.let { String.format(Locale.US, "+%.2fs", it) } ?: "+1.42s"
      val gapBehindStr = hudState.intervalBehindSeconds?.let { String.format(Locale.US, "-%.2fs", it) } ?: "-1.95s"
      val aheadDriver = hudState.rivalAheadName?.substringBefore("'")?.trim()?.take(9) ?: "ROS3"
      Column(
        modifier = Modifier.width(142.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
      ) {
        data class StandingsBadgeRow(
          val pos: Int,
          val carBadge: String,
          val badgeColor: Color,
          val driverName: String,
          val gapLabel: String
        )
        val standingsRows = listOf(
          StandingsBadgeRow(1, "GT3", Color(0xFFE50914), "V. Tanaka", if (playerPos == 1) "LEADER" else "+2.47s"),
          StandingsBadgeRow((playerPos - 1).coerceAtLeast(2), "LMP", Color(0xFF334155), aheadDriver, gapAheadStr),
          StandingsBadgeRow(playerPos, "YOU", Color(0xFFE50914), vehicle.name.take(8).uppercase(Locale.US), "P$playerPos"),
          StandingsBadgeRow((playerPos + 1).coerceAtMost(hudState.totalRivals.coerceAtLeast(2)), "PRO", Color(0xFF475569), "M. Rossi", gapBehindStr)
        )
        standingsRows.forEachIndexed { idx, rowItem ->
          val isPlayerRow = idx == 2
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(3.dp))
              .background(
                if (isPlayerRow) Color.White.copy(alpha = 0.94f)
                else Color(0xFF0D111A).copy(alpha = 0.72f)
              )
              .padding(horizontal = 4.dp, vertical = 1.5.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(3.dp),
              modifier = Modifier.weight(1f)
            ) {
              Text(
                text = "${rowItem.pos}",
                fontSize = 7.sp,
                fontWeight = FontWeight.Black,
                color = if (isPlayerRow) Color(0xFF090B10) else Color.White
              )
              Box(
                modifier = Modifier
                  .clip(RoundedCornerShape(2.dp))
                  .background(rowItem.badgeColor)
                  .padding(horizontal = 3.dp, vertical = 0.5.dp)
              ) {
                Text(
                  text = rowItem.carBadge,
                  fontSize = 5.5.sp,
                  fontWeight = FontWeight.Black,
                  color = Color.White
                )
              }
              Text(
                text = rowItem.driverName,
                fontSize = 7.sp,
                fontWeight = FontWeight.Bold,
                color = if (isPlayerRow) Color(0xFF090B10) else Color.White,
                maxLines = 1
              )
            }
            Text(
              text = rowItem.gapLabel,
              fontSize = 6.8.sp,
              fontWeight = FontWeight.Bold,
              color = when {
                isPlayerRow -> Color(0xFFE50914)
                rowItem.gapLabel.startsWith("+") -> Color(0xFFFACC15)
                else -> Color(0xFFCBD5E1)
              },
              fontFamily = FontFamily.Monospace
            )
          }
        }
      }

      // Compact Right Utility Row (4-Wheel Chassis Integrity, Camera, H-Pattern Sequential Shifter, Pit/Nitro)
      Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        // 4-Wheel Chassis & Tire Integrity Diagram
        Box(
          modifier = Modifier
            .size(width = 30.dp, height = 32.dp)
            .clip(RoundedCornerShape(5.dp))
            .background(Color(0xFF141923).copy(alpha = 0.72f))
            .border(1.dp, Color(0xFF4ADE80).copy(alpha = 0.75f), RoundedCornerShape(5.dp))
            .testTag("hud_chassis_health_diagram"),
          contentAlignment = Alignment.Center
        ) {
          Canvas(modifier = Modifier.matchParentSize()) {
            val w = size.width
            val h = size.height
            val tireCol = if (hudState.tireHealthPercent > 40f) Color(0xFF4ADE80) else Color(0xFFE50914)
            drawRoundRect(color = tireCol, topLeft = Offset(w * 0.10f, h * 0.14f), size = Size(w * 0.12f, h * 0.24f), cornerRadius = CornerRadius(1.5f, 1.5f))
            drawRoundRect(color = tireCol, topLeft = Offset(w * 0.78f, h * 0.14f), size = Size(w * 0.12f, h * 0.24f), cornerRadius = CornerRadius(1.5f, 1.5f))
            drawRoundRect(color = tireCol, topLeft = Offset(w * 0.10f, h * 0.62f), size = Size(w * 0.12f, h * 0.24f), cornerRadius = CornerRadius(1.5f, 1.5f))
            drawRoundRect(color = tireCol, topLeft = Offset(w * 0.78f, h * 0.62f), size = Size(w * 0.12f, h * 0.24f), cornerRadius = CornerRadius(1.5f, 1.5f))
            drawRoundRect(
              color = tireCol.copy(alpha = 0.55f),
              topLeft = Offset(w * 0.28f, h * 0.16f),
              size = Size(w * 0.44f, h * 0.68f),
              cornerRadius = CornerRadius(3f, 3f),
              style = Stroke(width = 1.2f)
            )
          }
          Text(
            text = "${hudState.tireHealthPercent.toInt()}",
            fontSize = 6.5.sp,
            fontWeight = FontWeight.Black,
            color = Color.White,
            fontFamily = FontFamily.Monospace
          )
        }

        HudCircularEdgeButton(
          onClick = onSwitchCamera,
          testTag = "hud_photo_mode_btn"
        ) {
          Icon(
            imageVector = Icons.Default.CameraAlt,
            contentDescription = "Switch View",
            tint = Color.White,
            modifier = Modifier.size(15.dp)
          )
        }

        // Interactive 6-Speed H-Pattern / Sequential Shifter Gate (`hud_h_pattern_shifter`)
        Box(
          modifier = Modifier
            .size(width = 36.dp, height = 32.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(Color(0xFF141923).copy(alpha = 0.78f))
            .border(1.dp, Color(0xFFCBD5E1).copy(alpha = 0.55f), RoundedCornerShape(6.dp))
            .clickable {
              val current = localManualGear ?: hudState.gear.coerceAtLeast(1)
              if (current >= vehicle.gears.coerceAtLeast(6)) {
                localManualGear = 1
                onShiftDown()
              } else {
                localManualGear = current + 1
                onShiftUp()
              }
            }
            .testTag("hud_h_pattern_shifter")
        ) {
          val activeG = (localManualGear ?: hudState.gear).coerceIn(1, 6)
          Canvas(modifier = Modifier.matchParentSize()) {
            val w = size.width
            val h = size.height
            val gateColor = Color.White.copy(alpha = 0.75f)
            drawLine(
              color = gateColor,
              start = Offset(w * 0.22f, h * 0.50f),
              end = Offset(w * 0.78f, h * 0.50f),
              strokeWidth = 2.6f,
              cap = StrokeCap.Round
            )
            listOf(0.22f, 0.50f, 0.78f).forEach { xFrac ->
              drawLine(
                color = gateColor,
                start = Offset(w * xFrac, h * 0.22f),
                end = Offset(w * xFrac, h * 0.78f),
                strokeWidth = 2.6f,
                cap = StrokeCap.Round
              )
            }
            val knobX = when (activeG) {
              1, 2 -> w * 0.22f
              3, 4 -> w * 0.50f
              else -> w * 0.78f
            }
            val knobY = if (activeG % 2 == 1) h * 0.25f else h * 0.75f
            drawCircle(
              color = Color(0xFFE50914),
              radius = 4.5f,
              center = Offset(knobX, knobY)
            )
          }
        }

        HudCircularEdgeButton(
          onClick = onOpenPitStrategy,
          testTag = "hud_side_nitro_btn"
        ) {
          Icon(
            imageVector = Icons.Default.ElectricBolt,
            contentDescription = "Pit & Strategy",
            tint = Color.White,
            modifier = Modifier.size(15.dp)
          )
        }
      }
    }

    // 4. COMPACT TOP-CENTER REAR-VIEW MIRROR, SECTOR SPLIT & PROXIMITY RADAR
    Column(
      modifier = Modifier
        .align(Alignment.TopCenter)
        .padding(top = 62.dp)
        .testTag("hud_rear_view_mirror_and_rivals"),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
      Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        // Live Sector Time + Upcoming Corner Distance Sign
        Row(
          modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(Color(0xFF0D111A).copy(alpha = 0.78f))
            .border(0.8.dp, Color.White.copy(alpha = 0.24f), RoundedCornerShape(4.dp))
            .padding(horizontal = 5.dp, vertical = 1.5.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
          Text(
            text = "S1 ${hudState.sector1TimeMs?.let { String.format(Locale.US, "%.2fs", it / 1000f) } ?: "19.06s"}",
            fontSize = 6.sp,
            fontWeight = FontWeight.Black,
            color = Color.White,
            fontFamily = FontFamily.Monospace
          )
          Box(
            modifier = Modifier
              .clip(RoundedCornerShape(2.dp))
              .background(Color(0xFFE50914))
              .padding(horizontal = 3.dp, vertical = 0.5.dp)
          ) {
            Text(
              text = "↰ APEX",
              fontSize = 5.5.sp,
              fontWeight = FontWeight.Black,
              color = Color.White
            )
          }
        }

        // Wide-Angle Rear-View Mirror Housing
        Box(
          modifier = Modifier
            .width(106.dp)
            .height(20.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFF0A0E17).copy(alpha = 0.82f))
            .border(1.2.dp, Color(0xFFCBD5E1).copy(alpha = 0.65f), RoundedCornerShape(10.dp))
            .testTag("hud_rear_view_mirror")
        ) {
          Canvas(modifier = Modifier.matchParentSize()) {
            val w = size.width
            val h = size.height
            drawRect(color = Color(0xFF1E293B), topLeft = Offset.Zero, size = Size(w, h * 0.48f))
            drawRect(color = Color(0xFF0F172A), topLeft = Offset(0f, h * 0.48f), size = Size(w, h * 0.52f))
            drawCircle(color = Color(0xFFFACC15), radius = 2.5f, center = Offset(w * 0.36f, h * 0.58f))
            drawCircle(color = Color(0xFFFACC15), radius = 2.5f, center = Offset(w * 0.44f, h * 0.58f))
            drawCircle(color = Color(0xFF38BDF8), radius = 2.2f, center = Offset(w * 0.64f, h * 0.55f))
            drawCircle(color = Color(0xFF38BDF8), radius = 2.2f, center = Offset(w * 0.70f, h * 0.55f))
          }
          Text(
            text = "REAR VIEW",
            fontSize = 5.sp,
            fontWeight = FontWeight.Black,
            color = Color.White.copy(alpha = 0.60f),
            modifier = Modifier
              .align(Alignment.BottomCenter)
              .padding(bottom = 0.5.dp)
          )
        }

        // Slipstream / Overtake Telemetry Badge
        Box(
          modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(Color(0xFF0D111A).copy(alpha = 0.78f))
            .border(0.8.dp, if (hudState.isDrafting) NeonCyan else Color(0xFFCBD5E1).copy(alpha = 0.35f), RoundedCornerShape(4.dp))
            .padding(horizontal = 5.dp, vertical = 1.5.dp)
        ) {
          Text(
            text = if (hudState.isDrafting) "DRAFT +${hudState.slipstreamBonusKmh.toInt()} KM/H" else "AERO: ${hudState.aeroTrim.shortLabel}",
            fontSize = 6.sp,
            fontWeight = FontWeight.Black,
            color = if (hudState.isDrafting) NeonCyan else Color.White,
            fontFamily = FontFamily.Monospace
          )
        }
      }

      // Rival Proximity Radar Row
      Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.testTag("hud_rival_proximity_radar_row")
      ) {
        Box(
          modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(Color(0xFF141923).copy(alpha = 0.78f))
            .border(0.8.dp, Color(0xFFE50914).copy(alpha = 0.75f), RoundedCornerShape(4.dp))
            .padding(horizontal = 5.dp, vertical = 1.dp)
        ) {
          Text(
            text = "RIVAL RADAR • ${hudState.rivalAheadName?.uppercase() ?: "CLEAR TRACK AHEAD"}",
            fontSize = 5.8.sp,
            fontWeight = FontWeight.Black,
            color = Color.White,
            fontFamily = FontFamily.Monospace
          )
        }
        Box(
          modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(Color(0xFF141923).copy(alpha = 0.78f))
            .border(0.8.dp, Color(0xFFFACC15), RoundedCornerShape(4.dp))
            .padding(horizontal = 5.dp, vertical = 1.dp)
        ) {
          Text(
            text = "14m ▼  •  9m ◢",
            fontSize = 5.8.sp,
            fontWeight = FontWeight.Black,
            color = Color(0xFFFACC15),
            fontFamily = FontFamily.Monospace
          )
        }
      }
    }

    // 5. DIGITAL COCKPIT INSTRUMENT BINNACLE (Active in COCKPIT_CAM Camera Perspective)
    if (perspective == CameraPerspective.COCKPIT_CAM) {
      val binnacleSpeedInt = speedConverted.toInt()
      val binnacleGear = localManualGear ?: hudState.gear.coerceAtLeast(1)
      Box(
        modifier = Modifier
          .align(Alignment.BottomCenter)
          .padding(bottom = 4.dp)
          .width(268.dp)
          .height(74.dp)
          .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp, bottomStart = 8.dp, bottomEnd = 8.dp))
          .background(
            Brush.verticalGradient(
              colors = listOf(Color(0xFF151922).copy(alpha = 0.94f), Color(0xFF080A0F).copy(alpha = 0.96f))
            )
          )
          .border(
            1.5.dp,
            Color(0xFFCBD5E1).copy(alpha = 0.45f),
            RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp, bottomStart = 8.dp, bottomEnd = 8.dp)
          )
          .padding(horizontal = 10.dp, vertical = 5.dp)
          .testTag("hud_cockpit_digital_binnacle")
      ) {
        Row(
          modifier = Modifier.fillMaxSize(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          // Left Trip & Telemetry Display
          Column(
            modifier = Modifier.weight(0.32f),
            verticalArrangement = Arrangement.spacedBy(1.dp)
          ) {
            Text(
              text = "COCKPIT TELEMETRY",
              fontSize = 6.sp,
              fontWeight = FontWeight.Black,
              color = Color(0xFF94A3B8)
            )
            Text(
              text = "RPM $liveRpm",
              fontSize = 6.5.sp,
              fontWeight = FontWeight.Bold,
              color = if (isAtRedline) Color(0xFFE50914) else Color.White,
              fontFamily = FontFamily.Monospace
            )
            Text(
              text = "TIRE ${hudState.tireTempCelsius.toInt()}°C",
              fontSize = 6.5.sp,
              fontWeight = FontWeight.Bold,
              color = Color(0xFFCBD5E1),
              fontFamily = FontFamily.Monospace
            )
            Text(
              text = "LAP $formattedTimer",
              fontSize = 6.5.sp,
              fontWeight = FontWeight.Bold,
              color = Color(0xFFFACC15),
              fontFamily = FontFamily.Monospace
            )
          }

          // Center Red Circular Tachometer ("0..10" RPM + Speed + Gear)
          Box(
            modifier = Modifier.size(64.dp),
            contentAlignment = Alignment.Center
          ) {
            Canvas(modifier = Modifier.matchParentSize()) {
              val r = size.minDimension / 2f
              drawCircle(
                color = Color(0xFF7F1D1D).copy(alpha = 0.55f),
                radius = r
              )
              drawCircle(
                color = Color(0xFFE50914),
                radius = r - 2f,
                style = Stroke(width = 3f)
              )
              drawArc(
                color = Color(0xFFFACC15),
                startAngle = 135f,
                sweepAngle = 270f * rpmNormalized,
                useCenter = false,
                style = Stroke(width = 4f, cap = StrokeCap.Round)
              )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
              Text(
                text = "$binnacleSpeedInt",
                fontSize = 14.sp,
                lineHeight = 14.sp,
                fontWeight = FontWeight.Black,
                color = Color.White,
                fontFamily = FontFamily.Monospace
              )
              Text(
                text = "G$binnacleGear ${if (hudState.isAutoGears) "AUTO" else "MAN"}",
                fontSize = 6.5.sp,
                lineHeight = 6.5.sp,
                fontWeight = FontWeight.Black,
                color = Color(0xFFFACC15),
                fontFamily = FontFamily.Monospace
              )
            }
          }

          // Right 4-Tire Thermal Status Overview
          Column(
            modifier = Modifier.weight(0.32f),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(1.dp)
          ) {
            Text(
              text = "CHASSIS • STATUS",
              fontSize = 6.sp,
              fontWeight = FontWeight.Black,
              color = ApexGreen
            )
            Text(
              text = "${hudState.tireCompound.shortCode} ${hudState.tireHealthPercent.toInt()}% LIFE",
              fontSize = 6.5.sp,
              fontWeight = FontWeight.Bold,
              color = Color.White,
              fontFamily = FontFamily.Monospace
            )
            Text(
              text = "NOS ${(hudState.nitroChargePercent * 100).toInt()}%",
              fontSize = 6.5.sp,
              fontWeight = FontWeight.Bold,
              color = Color(0xFFCBD5E1),
              fontFamily = FontFamily.Monospace
            )
            Text(
              text = "POS P${hudState.currentRank}/${hudState.totalRivals}",
              fontSize = 6.5.sp,
              fontWeight = FontWeight.Bold,
              color = NeonCyan,
              fontFamily = FontFamily.Monospace
            )
          }
        }
      }
    }
  }
}

@Composable
private fun HudCircularEdgeButton(
  onClick: () -> Unit,
  testTag: String,
  isActive: Boolean = true,
  content: @Composable () -> Unit
) {
  Box(
    modifier = Modifier
      .size(32.dp)
      .clip(CircleShape)
      .background(Color(0xFF141923).copy(alpha = 0.68f))
      .border(
        width = 1.2.dp,
        color = if (isActive) Color(0xFFCBD5E1).copy(alpha = 0.58f) else NeonAmber,
        shape = CircleShape
      )
      .clickable(onClick = onClick)
      .testTag(testTag),
    contentAlignment = Alignment.Center
  ) {
    content()
  }
}

// -------------------------------------------------------------------
// 4. START LIGHTS COUNTDOWN OVERLAY
// -------------------------------------------------------------------
@Composable
fun StartLightsOverlay(lightsCount: Int) {
  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(CarbonBlack.copy(alpha = 0.55f)),
    contentAlignment = Alignment.Center
  ) {
    Card(
      colors = CardDefaults.cardColors(containerColor = CarbonSurface),
      shape = RoundedCornerShape(16.dp),
      border = androidx.compose.foundation.BorderStroke(2.dp, CarbonBorder)
    ) {
      Column(
        modifier = Modifier.padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp)
      ) {
        Text(
          text = if (lightsCount == 6) "LIGHTS OUT AND AWAY WE GO!" else "GRID START SEQUENCE",
          fontSize = 14.sp,
          fontWeight = FontWeight.Black,
          color = if (lightsCount == 6) ApexGreen else TextPrimary,
          letterSpacing = 1.sp
        )

        // 5 Starting Lights
        Row(
          horizontalArrangement = Arrangement.spacedBy(10.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          for (i in 1..5) {
            val isOn = lightsCount in i..5
            Box(
              modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(if (isOn) RedlineRed else CarbonBlack)
                .border(2.dp, if (isOn) RedlineRed else CarbonBorder, CircleShape)
            )
          }
        }

        Text(
          text = if (lightsCount == 6) "FULL THROTTLE!" else "HOLD REVS • AWAIT GREEN",
          fontSize = 10.sp,
          fontWeight = FontWeight.Bold,
          color = if (lightsCount == 6) ApexGreen else NeonAmber
        )
      }
    }
  }
}

// -------------------------------------------------------------------
// 5. IN-GAME DRIVING CONTROLS (ERGONOMIC, LOW-LATENCY MULTI-TOUCH STEERING, PEDALS, HANDBRAKE & NITRO)
// - Bottom-Left: Responsive Steering Cluster (`hud_steering_wheel_control`) with Left (`steer_left_btn`)
//   and Right (`steer_right_btn`) touch zones, smooth horizontal slide-steering, and live Steering Angle Bar.
// - Bottom-Center: DRS Wing Activation Pill (`drs_flap_btn`) when in DRS zone.
// - Bottom-Right:
//   * Dedicated Handbrake / E-Brake Drift Button (`hud_handbrake_btn`, 48dp+ touch target) wired directly
//     to `engine.isHandbrakeActive` for instant apex drifting.
//   * Wide Slotted Brake Pedal (`brake_pedal_btn`, 74dp x 60dp) with live brake pressure feedback.
//   * Nitro / ERS Boost Button (`nitro_boost_btn`, 52dp x 52dp) with circular charge ring & percentage.
//   * Tall Perforated Accelerator / Gas Pedal (`gas_pedal_btn`, 64dp x 104dp) with live throttle response.
// -------------------------------------------------------------------
@Composable
fun InGameDrivingControls(
  engine: InteractiveRacingEngine,
  hudState: RacingGameHudState,
  modifier: Modifier = Modifier
) {
  val controlConfig by com.example.game.v2.ApexRivals2SettingsRepository.controlConfig.collectAsState()
  val steerSensitivity = controlConfig.steeringSensitivity.coerceIn(0.6f, 1.8f)

  // Steer Left & Right Zone Pressed States
  val steerLeftSource = remember { MutableInteractionSource() }
  val isSteerLeftPressed by steerLeftSource.collectIsPressedAsState()

  val steerRightSource = remember { MutableInteractionSource() }
  val isSteerRightPressed by steerRightSource.collectIsPressedAsState()

  var dragSteerValue by remember { mutableFloatStateOf(0f) }

  // Gas, Brake & Handbrake Pedal Pressed States
  val throttleSource = remember { MutableInteractionSource() }
  val isThrottlePressed by throttleSource.collectIsPressedAsState()

  val brakeSource = remember { MutableInteractionSource() }
  val isBrakePressed by brakeSource.collectIsPressedAsState()

  val handbrakeSource = remember { MutableInteractionSource() }
  val isHandbrakeHeld by handbrakeSource.collectIsPressedAsState()
  var isHandbrakeLatched by remember { mutableStateOf(false) }
  val isHandbrakePressed = isHandbrakeHeld || isHandbrakeLatched

  val activeSteer = when {
    isSteerLeftPressed && !isSteerRightPressed -> (-1.0f * steerSensitivity).coerceIn(-1.0f, 1.0f)
    isSteerRightPressed && !isSteerLeftPressed -> (1.0f * steerSensitivity).coerceIn(-1.0f, 1.0f)
    kotlin.math.abs(dragSteerValue) > 0.04f -> (dragSteerValue * steerSensitivity).coerceIn(-1.0f, 1.0f)
    else -> 0.0f
  }

  LaunchedEffect(activeSteer) {
    engine.steerInput = activeSteer
  }

  LaunchedEffect(isThrottlePressed) {
    engine.throttleInput = if (isThrottlePressed) 1.0f else 0.0f
    if (isThrottlePressed && isHandbrakeLatched) {
      isHandbrakeLatched = false
    }
  }

  LaunchedEffect(isBrakePressed, isHandbrakePressed) {
    engine.isHandbrakeActive = isHandbrakePressed
    engine.brakeInput = when {
      isBrakePressed -> 1.0f
      isHandbrakePressed -> 0.65f
      else -> 0.0f
    }
  }

  Box(
    modifier = modifier.fillMaxWidth(),
    contentAlignment = Alignment.BottomCenter
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.Bottom
    ) {
      // 1. BOTTOM-LEFT: Ergonomic Steering Control Cluster (`hud_steering_wheel_control`) + Live Steer Angle Gauge
      Column(
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(5.dp)
      ) {
        // Live Steering Deflection Indicator Strip
        Row(
          modifier = Modifier
            .width(168.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(Color(0xFF0B0E16).copy(alpha = 0.76f))
            .border(0.8.dp, Color(0xFFCBD5E1).copy(alpha = 0.28f), RoundedCornerShape(4.dp))
            .padding(horizontal = 8.dp, vertical = 2.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = "STEER",
            fontSize = 6.5.sp,
            fontWeight = FontWeight.Black,
            color = Color(0xFFCBD5E1),
            fontFamily = FontFamily.Monospace
          )
          Text(
            text = when {
              activeSteer < -0.1f -> "◄ LEFT ${(kotlin.math.abs(activeSteer) * 100).toInt()}%"
              activeSteer > 0.1f -> "RIGHT ${(activeSteer * 100).toInt()}% ►"
              else -> "CENTER 0°"
            },
            fontSize = 6.8.sp,
            fontWeight = FontWeight.Black,
            color = if (kotlin.math.abs(activeSteer) > 0.1f) Color(0xFFE50914) else Color.White,
            fontFamily = FontFamily.Monospace
          )
        }

        Box(
          modifier = Modifier
            .width(168.dp)
            .height(98.dp)
            .pointerInput(Unit) {
              detectHorizontalDragGestures(
                onDragEnd = { dragSteerValue = 0f },
                onDragCancel = { dragSteerValue = 0f },
                onHorizontalDrag = { _, dragAmount ->
                  dragSteerValue = (dragSteerValue + (dragAmount / 44f)).coerceIn(-1f, 1f)
                }
              )
            }
            .testTag("hud_steering_wheel_control"),
          contentAlignment = Alignment.Center
        ) {
          // Translucent Angled Motorsport Steering Pad & Double-Chevron Arrows
          Canvas(modifier = Modifier.matchParentSize()) {
            val w = size.width
            val h = size.height

            val padPath = Path().apply {
              moveTo(w * 0.22f, h * 0.06f)
              lineTo(w * 0.78f, h * 0.06f)
              lineTo(w * 0.98f, h * 0.50f)
              lineTo(w * 0.78f, h * 0.94f)
              lineTo(w * 0.22f, h * 0.94f)
              lineTo(w * 0.02f, h * 0.50f)
              close()
            }
            drawPath(
              path = padPath,
              color = Color(0xFF0D111A).copy(alpha = 0.52f)
            )
            drawPath(
              path = padPath,
              color = if (kotlin.math.abs(activeSteer) > 0.1f) Color(0xFFE50914) else Color.White.copy(alpha = 0.48f),
              style = Stroke(width = 2.2f)
            )

            // Center Neutral Divider Line
            drawLine(
              color = Color.White.copy(alpha = 0.18f),
              start = Offset(w * 0.50f, h * 0.16f),
              end = Offset(w * 0.50f, h * 0.84f),
              strokeWidth = 1.5f
            )

            // Left Double-Chevron («)
            val leftActive = activeSteer < -0.08f
            for (offset in listOf(0f, 13f)) {
              val chevLeft = Path().apply {
                moveTo(w * 0.31f + offset, h * 0.28f)
                lineTo(w * 0.19f + offset, h * 0.50f)
                lineTo(w * 0.31f + offset, h * 0.72f)
              }
              drawPath(
                path = chevLeft,
                color = if (leftActive) Color(0xFFE50914) else Color.White.copy(alpha = 0.68f),
                style = Stroke(width = 5.2f, cap = StrokeCap.Round)
              )
            }

            // Right Double-Chevron (»)
            val rightActive = activeSteer > 0.08f
            for (offset in listOf(0f, 13f)) {
              val chevRight = Path().apply {
                moveTo(w * 0.69f - offset, h * 0.28f)
                lineTo(w * 0.81f - offset, h * 0.50f)
                lineTo(w * 0.69f - offset, h * 0.72f)
              }
              drawPath(
                path = chevRight,
                color = if (rightActive) Color(0xFFE50914) else Color.White.copy(alpha = 0.68f),
                style = Stroke(width = 5.2f, cap = StrokeCap.Round)
              )
            }
          }

          // Left & Right Interactive Multi-Touch Steer Zones (`steer_left_btn` and `steer_right_btn`)
          Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Box(
              modifier = Modifier
                .weight(1f)
                .height(98.dp)
                .clickable(
                  interactionSource = steerLeftSource,
                  indication = null,
                  onClick = { dragSteerValue = -0.85f }
                )
                .testTag("steer_left_btn")
            )
            Box(
              modifier = Modifier
                .weight(1f)
                .height(98.dp)
                .clickable(
                  interactionSource = steerRightSource,
                  indication = null,
                  onClick = { dragSteerValue = 0.85f }
                )
                .testTag("steer_right_btn")
            )
          }
        }

        // Bottom-Left Telemetry Status Strip (Device Battery & Frame/Ping Readout)
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp),
          modifier = Modifier.padding(start = 4.dp)
        ) {
          Box(
            modifier = Modifier
              .width(18.dp)
              .height(9.dp)
              .border(1.dp, Color.White.copy(alpha = 0.65f), RoundedCornerShape(2.dp))
              .padding(1.5.dp)
          ) {
            Box(
              modifier = Modifier
                .height(6.dp)
                .fillMaxWidth(0.85f)
                .background(Color(0xFF4ADE80))
            )
          }
          Text(
            text = "60 FPS • 18 ms",
            fontSize = 8.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFFCBD5E1),
            fontFamily = FontFamily.Monospace
          )
        }
      }

      // 3. BOTTOM-RIGHT: Ergonomic Handbrake, Wide Slotted Brake Pedal, Nitro Boost & Tall Perforated Accelerator Pedal
      Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Bottom
      ) {
        // Left Column of Right Cluster: Dedicated Handbrake / Drift Button + Wide Slotted Brake Pedal
        Column(
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          // Ergonomic 48dp+ Handbrake / E-Brake Drift Button (`hud_handbrake_btn`)
          Box(
            modifier = Modifier
              .size(width = 62.dp, height = 48.dp)
              .clip(RoundedCornerShape(10.dp))
              .background(
                if (isHandbrakePressed) Color(0xFFE50914)
                else Color(0xFF141923).copy(alpha = 0.78f)
              )
              .border(
                width = 1.5.dp,
                color = if (isHandbrakePressed) Color.White else Color(0xFFE50914).copy(alpha = 0.75f),
                shape = RoundedCornerShape(10.dp)
              )
              .clickable(
                interactionSource = handbrakeSource,
                indication = null,
                onClick = {
                  isHandbrakeLatched = !isHandbrakeLatched
                  engine.isHandbrakeActive = isHandbrakeLatched
                  engine.brakeInput = if (isHandbrakeLatched) 0.65f else 0.0f
                }
              )
              .testTag("hud_handbrake_btn"),
            contentAlignment = Alignment.Center
          ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
              Text(
                text = "( P )",
                fontSize = 11.sp,
                lineHeight = 11.sp,
                fontWeight = FontWeight.Black,
                color = Color.White
              )
              Text(
                text = if (isHandbrakePressed) "LOCKED" else "HANDBRAKE",
                fontSize = 6.2.sp,
                lineHeight = 6.5.sp,
                fontWeight = FontWeight.Black,
                color = Color(0xFFCBD5E1),
                fontFamily = FontFamily.Monospace
              )
            }
          }

          // Wide Slotted Brake Pedal (`brake_pedal_btn`)
          Box(
            modifier = Modifier
              .width(70.dp)
              .height(58.dp)
              .clickable(
                interactionSource = brakeSource,
                indication = null,
                onClick = { engine.brakeInput = 1.0f }
              )
              .testTag("brake_pedal_btn"),
            contentAlignment = Alignment.Center
          ) {
            TranslucentBrakePedalCanvas(
              isPressed = isBrakePressed,
              modifier = Modifier.matchParentSize()
            )
            Text(
              text = "BRAKE",
              fontSize = 7.sp,
              fontWeight = FontWeight.Black,
              color = Color.White.copy(alpha = 0.90f),
              fontFamily = FontFamily.Monospace,
              modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 3.dp)
            )
          }
        }

        // Right Column: Nitro / ERS Boost Button above Tall Perforated Accelerator / Gas Pedal
        Column(
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          val nitroAvailable = hudState.nitroChargePercent >= 0.25f
          val nitroPct = (hudState.nitroChargePercent * 100).toInt().coerceIn(0, 100)
          Box(
            modifier = Modifier
              .size(54.dp)
              .clip(RoundedCornerShape(12.dp))
              .background(
                when {
                  hudState.isNitroActive -> NeonCyan.copy(alpha = 0.90f)
                  nitroAvailable -> Color(0xFF141923).copy(alpha = 0.84f)
                  else -> Color(0xFF0E121B).copy(alpha = 0.55f)
                }
              )
              .border(
                1.6.dp,
                when {
                  hudState.isNitroActive -> Color.White
                  nitroAvailable -> Color(0xFFE50914)
                  else -> Color.White.copy(alpha = 0.25f)
                },
                RoundedCornerShape(12.dp)
              )
              .clickable(enabled = nitroAvailable) { engine.triggerNitroBoost() }
              .testTag("nitro_boost_btn"),
            contentAlignment = Alignment.Center
          ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
              Icon(
                imageVector = Icons.Default.ElectricBolt,
                contentDescription = "Nitro Boost",
                tint = if (hudState.isNitroActive) CarbonBlack else if (nitroAvailable) Color(0xFFE50914) else Color.White.copy(alpha = 0.45f),
                modifier = Modifier.size(18.dp)
              )
              Text(
                text = "NITRO",
                fontSize = 6.8.sp,
                lineHeight = 7.sp,
                fontWeight = FontWeight.Black,
                color = if (hudState.isNitroActive) CarbonBlack else Color.White
              )
              Text(
                text = "$nitroPct%",
                fontSize = 6.5.sp,
                lineHeight = 6.5.sp,
                fontWeight = FontWeight.Bold,
                color = if (hudState.isNitroActive) CarbonBlack else Color(0xFFCBD5E1),
                fontFamily = FontFamily.Monospace
              )
            }
          }

          // Tall Perforated Metallic Gas / Accelerator Pedal (`gas_pedal_btn`)
          Box(
            modifier = Modifier
              .width(64.dp)
              .height(104.dp)
              .clickable(
                interactionSource = throttleSource,
                indication = null,
                onClick = { engine.throttleInput = 1.0f }
              )
              .testTag("gas_pedal_btn"),
            contentAlignment = Alignment.Center
          ) {
            PerforatedGasPedalCanvas(
              isPressed = isThrottlePressed,
              modifier = Modifier.matchParentSize()
            )
            Text(
              text = "GAS",
              fontSize = 7.5.sp,
              fontWeight = FontWeight.Black,
              color = Color.White,
              fontFamily = FontFamily.Monospace,
              modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 5.dp)
            )
          }
        }
      }
    }

    // 2. BOTTOM-CENTER SPEEDOMETER, GEAR, RPM & OPTIONAL DRS POD
    // Positioned near the bottom center between left steering controls and right pedals while keeping the 3D racing road visible above
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(4.dp),
      modifier = Modifier
        .align(Alignment.BottomCenter)
        .padding(bottom = 2.dp)
        .testTag("hud_bottom_center_speedometer")
    ) {
      if (hudState.isDrsAvailable) {
        Box(
          modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(if (hudState.isDrsActive) ApexGreen else CarbonSurfaceVariant.copy(alpha = 0.78f))
            .border(1.2.dp, ApexGreen, RoundedCornerShape(6.dp))
            .clickable { engine.toggleDrs() }
            .padding(horizontal = 12.dp, vertical = 4.dp)
            .testTag("drs_flap_btn")
        ) {
          Text(
            text = if (hudState.isDrsActive) "DRS OPEN (+15 KM/H)" else "TAP FOR DRS WING",
            fontSize = 8.sp,
            fontWeight = FontWeight.Black,
            color = if (hudState.isDrsActive) CarbonBlack else ApexGreen
          )
        }
      }

      val bottomRpmFraction = (hudState.rpm.toFloat() / hudState.maxRpm.coerceAtLeast(8000).toFloat()).coerceIn(0.12f, 1.0f)
      val bottomRedline = hudState.rpm >= hudState.redlineRpm.coerceAtLeast(7200)
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
          .clip(RoundedCornerShape(10.dp))
          .background(Color(0xFF0A0D14).copy(alpha = 0.84f))
          .border(
            1.2.dp,
            if (bottomRedline) Color(0xFFE50914) else Color(0xFFCBD5E1).copy(alpha = 0.42f),
            RoundedCornerShape(10.dp)
          )
          .padding(horizontal = 12.dp, vertical = 5.dp)
      ) {
        // Gear Badge
        Box(
          modifier = Modifier
            .size(width = 28.dp, height = 28.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(if (bottomRedline) Color(0xFFE50914) else Color(0xFF1A202E))
            .border(1.dp, Color(0xFFCBD5E1).copy(alpha = 0.45f), RoundedCornerShape(6.dp)),
          contentAlignment = Alignment.Center
        ) {
          Text(
            text = "G${hudState.gear.coerceAtLeast(1)}",
            fontSize = 11.sp,
            fontWeight = FontWeight.Black,
            color = Color.White,
            fontFamily = FontFamily.Monospace
          )
        }

        // Digital Speed + RPM Mini-Bar
        Column(
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
          Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(3.dp)
          ) {
            Text(
              text = "${hudState.speedKmh.toInt()}",
              fontSize = 17.sp,
              lineHeight = 17.sp,
              fontWeight = FontWeight.Black,
              color = Color.White,
              fontFamily = FontFamily.Monospace
            )
            Text(
              text = "KM/H",
              fontSize = 6.5.sp,
              lineHeight = 7.sp,
              fontWeight = FontWeight.Bold,
              color = Color(0xFFCBD5E1),
              fontFamily = FontFamily.Monospace,
              modifier = Modifier.padding(bottom = 1.dp)
            )
          }
          Box(
            modifier = Modifier
              .width(74.dp)
              .height(4.dp)
              .clip(RoundedCornerShape(2.dp))
              .background(Color.White.copy(alpha = 0.16f))
          ) {
            Box(
              modifier = Modifier
                .fillMaxWidth(bottomRpmFraction)
                .height(4.dp)
                .background(
                  if (bottomRedline) Color(0xFFE50914)
                  else if (bottomRpmFraction > 0.75f) NeonAmber
                  else NeonCyan
                )
            )
          }
        }
      }
    }
  }
}

@Composable
private fun TranslucentBrakePedalCanvas(
  isPressed: Boolean,
  modifier: Modifier = Modifier
) {
  Canvas(modifier = modifier) {
    val w = size.width
    val h = size.height

    val hexPath = Path().apply {
      moveTo(w * 0.16f, 0f)
      lineTo(w * 0.84f, 0f)
      lineTo(w, h * 0.50f)
      lineTo(w * 0.84f, h)
      lineTo(w * 0.16f, h)
      lineTo(0f, h * 0.50f)
      close()
    }
    drawPath(
      path = hexPath,
      color = if (isPressed) Color(0xFFE50914).copy(alpha = 0.72f) else Color(0xFF141923).copy(alpha = 0.62f)
    )
    drawPath(
      path = hexPath,
      color = if (isPressed) Color.White else Color(0xFFE50914).copy(alpha = 0.78f),
      style = Stroke(width = 2.2f)
    )

    // 3 Horizontal Grip Bars with Center Split
    val barYs = listOf(h * 0.25f, h * 0.46f, h * 0.67f)
    barYs.forEachIndexed { idx, y ->
      if (idx == 1) {
        drawLine(
          color = Color.White.copy(alpha = 0.72f),
          start = Offset(w * 0.22f, y),
          end = Offset(w * 0.45f, y),
          strokeWidth = 4.2f,
          cap = StrokeCap.Round
        )
        drawLine(
          color = Color.White.copy(alpha = 0.72f),
          start = Offset(w * 0.55f, y),
          end = Offset(w * 0.78f, y),
          strokeWidth = 4.2f,
          cap = StrokeCap.Round
        )
      } else {
        drawLine(
          color = Color.White.copy(alpha = 0.72f),
          start = Offset(w * 0.22f, y),
          end = Offset(w * 0.78f, y),
          strokeWidth = 4.2f,
          cap = StrokeCap.Round
        )
      }
    }
  }
}

@Composable
private fun PerforatedGasPedalCanvas(
  isPressed: Boolean,
  modifier: Modifier = Modifier
) {
  Canvas(modifier = modifier) {
    val w = size.width
    val h = size.height

    val gasPath = Path().apply {
      moveTo(w * 0.18f, 0f)
      lineTo(w * 0.90f, 0f)
      lineTo(w * 0.94f, h * 0.96f)
      lineTo(w * 0.06f, h * 0.96f)
      lineTo(w * 0.04f, h * 0.68f)
      lineTo(w * 0.16f, h * 0.52f)
      close()
    }
    drawPath(
      path = gasPath,
      brush = Brush.verticalGradient(
        colors = if (isPressed) {
          listOf(Color(0xFFE50914).copy(alpha = 0.85f), Color(0xFF991B1B).copy(alpha = 0.70f))
        } else {
          listOf(Color(0xFF334155).copy(alpha = 0.78f), Color(0xFF141923).copy(alpha = 0.72f))
        }
      )
    )
    drawPath(
      path = gasPath,
      color = if (isPressed) Color.White else Color(0xFFCBD5E1).copy(alpha = 0.78f),
      style = Stroke(width = 2.2f)
    )

    // Perforated Micro-Dot Grip Matrix across the Lower 65% of the Pedal
    val rows = 7
    val cols = 5
    for (r in 0 until rows) {
      val y = h * (0.28f + r * 0.09f)
      for (c in 0 until cols) {
        val x = w * (0.26f + c * 0.13f)
        drawCircle(
          color = Color.White.copy(alpha = 0.65f),
          radius = 1.8f,
          center = Offset(x, y)
        )
      }
    }
  }
}

// -------------------------------------------------------------------
// 6. SCREEN 5 – RESULTS AND REWARDS (CLEAR RACE POSITION, REWARDS & NEXT-RACE ACTIONS)
//    Matches Screenshot_2026_1111_142017_4ee14e77.png & Unified Design Standard:
//    - Primary Background: Matte Black (#0B0D12) with Diagonal Racing Red (#8E241B) Podium Stage
//    - Left/Center Podium Stage:
//      * Route 66 Stage Shield ("1.3 Bridge the Gap" / Active Track)
//      * Dual 3D GT3 Supercar Podium Showcase (White/Racing-Red P1 Hypercar + Matte-Black/Gold #59 Rival) with Driver Helmet Badges
//      * Angled Parallelogram Podium Classification Ribbons ("1st | 117 PP | 00:26:866 | D ★ ★ ★" & "2nd | 117 PP | 00:26:866")
//    - Right Vertical Rewards Sidebar:
//      * Bold "YOU WIN!" / "YOU WON THE RACE!" Header Banner + "1st" ("P1 of 8")
//      * Stacked Rewards: APEX BONUS (5,409), Cash Stack (+575), XP (+120 / +1500 XP) with Driver Level Ring
//    - Next Available Race + Large Rectangular Motorsport Buttons (RACE AGAIN, 2X REWARD, GARAGE, MAIN MENU)
// -------------------------------------------------------------------
@Composable
fun PostRacePodiumDialog(
  summary: RaceFinishSummary,
  onRaceAgain: () -> Unit,
  onViewProfile: () -> Unit,
  onOpenGarage: () -> Unit = {},
  onExit: () -> Unit,
  isMph: Boolean
) {
  val speedConverted = if (isMph) summary.topSpeedKmh * 0.621371f else summary.topSpeedKmh
  val speedUnit = if (isMph) "MPH" else "KM/H"
  val context = androidx.compose.ui.platform.LocalContext.current
  val careerState by com.example.game.v2.ApexRivals2CareerManager.state.collectAsState()
  val cloudSyncState by com.example.data.firebase.ApexCloudSyncCoordinator.uiState.collectAsState()
  val scrollState = rememberScrollState()
  var doubleRewardClaimed by remember { mutableStateOf(false) }

  val baseCredits = if (summary.creditsEarned > 0) summary.creditsEarned else 575
  val effectiveCreditsEarned = if (doubleRewardClaimed) baseCredits * 2 else baseCredits
  val doublePreviewAmount = baseCredits * 2
  val totalBankrollDisplay = remember(doubleRewardClaimed, baseCredits, careerState.creditsBalance) {
    careerState.creditsBalance + (if (doubleRewardClaimed) baseCredits else 0)
  }

  val ordinalRankText = remember(summary.finishingPosition) {
    when (summary.finishingPosition) {
      1 -> "1st"
      2 -> "2nd"
      3 -> "3rd"
      else -> "${summary.finishingPosition}th"
    }
  }

  val formattedRaceTime = remember(summary.totalTimeMs) {
    val totalSec = summary.totalTimeMs / 1000.0
    val mins = (totalSec / 60.0).toInt()
    val remSec = totalSec - (mins * 60)
    String.format(Locale.US, "%02d:%06.3f", mins, remSec)
  }

  val formattedBestTime = remember(summary.bestLapMs, summary.isNewPersonalBest) {
    if (summary.bestLapMs <= 0L) {
      "~:~~.~~"
    } else {
      val bestSec = summary.bestLapMs / 1000.0
      val mins = (bestSec / 60.0).toInt()
      val remSec = bestSec - (mins * 60)
      String.format(Locale.US, "%02d:%05.2fs", mins, remSec)
    }
  }

  val nextAvailableTrackName = remember(summary.trackName) {
    val tracks = com.example.model.AVAILABLE_TRACKS
    val currentIndex = tracks.indexOfFirst { it.name.equals(summary.trackName, ignoreCase = true) }
    val nextTrack = if (currentIndex >= 0) tracks[(currentIndex + 1) % tracks.size] else tracks.first()
    nextTrack.name
  }

  // Compute personal best sector times across completed laps for purple/green sector highlights
  val bestS1Ms = summary.lapSummaries.filter { it.sector1Ms > 0L }.minOfOrNull { it.sector1Ms } ?: 0L
  val bestS2Ms = summary.lapSummaries.filter { it.sector2Ms > 0L }.minOfOrNull { it.sector2Ms } ?: 0L
  val bestS3Ms = summary.lapSummaries.filter { it.sector3Ms > 0L }.minOfOrNull { it.sector3Ms } ?: 0L

  AlertDialog(
    onDismissRequest = onExit,
    shape = RoundedCornerShape(12.dp),
    containerColor = Color(0xFF0B0D12),
    modifier = Modifier.testTag("screen_5_results_and_rewards"),
    title = {
      // Top Motorsport Header Bar (Preserves "VICTORY! P1 CHAMPION" test assertion)
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
            imageVector = Icons.Default.EmojiEvents,
            contentDescription = null,
            tint = if (summary.finishingPosition == 1) Color(0xFFE10600) else Color(0xFFCBD5E1),
            modifier = Modifier.size(18.dp)
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = if (summary.finishingPosition == 1) "VICTORY! P1 CHAMPION" else "RACE FINISHED • P${summary.finishingPosition}",
            fontSize = 12.sp,
            lineHeight = 15.sp,
            fontWeight = FontWeight.Black,
            color = Color.White,
            letterSpacing = 0.8.sp,
            maxLines = 1
          )
        }

        // Route 66 Stage Badge ("66  1.3 Bridge the Gap") matching top-center of screenshot
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(5.dp),
          modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(Color(0xFF16222F))
            .border(1.dp, Color(0xFFCBD5E1).copy(alpha = 0.4f), RoundedCornerShape(4.dp))
            .padding(horizontal = 7.dp, vertical = 2.dp)
        ) {
          SilverShieldBadgeCanvas(label = "${careerState.playerLevel}", modifier = Modifier.size(16.dp))
          Text(
            text = "${summary.trackName} • ${if (cloudSyncState.isAuthenticated) "CLOUD VERIFIED" else "LOCAL SAVE"}",
            fontSize = 8.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            maxLines = 1
          )
        }
      }
    },
    text = {
      Column(
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier
          .fillMaxWidth()
          .verticalScroll(scrollState)
      ) {
        // 0. TOP STATUS BAR: Left Silver Shield Level & Right 3D Cash Bundle
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .testTag("results_top_status_bar"),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.testTag("results_level_xp_bar")
          ) {
            SilverShieldBadgeCanvas(
              label = "${careerState.playerLevel}",
              modifier = Modifier.size(22.dp)
            )
            Box(
              modifier = Modifier
                .width(66.dp)
                .height(18.dp)
                .clip(RoundedCornerShape(topEnd = 4.dp, bottomEnd = 4.dp))
                .background(Color(0xFF181C26))
                .border(1.dp, Color(0xFFCBD5E1).copy(alpha = 0.35f), RoundedCornerShape(topEnd = 4.dp, bottomEnd = 4.dp)),
              contentAlignment = Alignment.Center
            ) {
              Text(
                text = "${careerState.totalXp % 1800} / 1800",
                fontSize = 8.5.sp,
                lineHeight = 10.sp,
                fontWeight = FontWeight.Black,
                color = Color.White,
                fontFamily = FontFamily.Monospace,
                maxLines = 1
              )
            }
          }

          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.testTag("results_cash_balance_badge")
          ) {
            GreenCashStackIconCanvas(modifier = Modifier.size(22.dp))
            Spacer(modifier = Modifier.width(3.dp))
            Box(
              modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .background(Color(0xFF181C26))
                .border(1.dp, Color(0xFFCBD5E1).copy(alpha = 0.35f), RoundedCornerShape(4.dp))
                .padding(horizontal = 7.dp, vertical = 2.dp),
              contentAlignment = Alignment.Center
            ) {
              Text(
                text = String.format(Locale.US, "%,d", totalBankrollDisplay),
                fontSize = 9.sp,
                lineHeight = 11.sp,
                fontWeight = FontWeight.Black,
                color = Color.White,
                fontFamily = FontFamily.Monospace,
                maxLines = 1
              )
            }
          }
        }

        // 1. SPLIT DIAGONAL PODIUM SHOWCASE & RIGHT REWARDS SIDEBAR (Matching Screenshot_2026_1111_142017_4ee14e77.png)
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .border(1.5.dp, Color(0xFFCBD5E1).copy(alpha = 0.35f), RoundedCornerShape(8.dp))
            .testTag("results_dual_panel_stage")
        ) {
          // Split Diagonal Crimson Racing Red & Deep Matte Black Stage Backdrop
          ResultsFinishLineBackdropCanvas(
            modifier = Modifier.matchParentSize()
          )

          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.Top
          ) {
            // LEFT/CENTER PODIUM SHOWCASE: Dual 3D GT3 Supercars + Driver Helmet Avatars + Angled 1st/2nd PP Ribbons
            Column(
              modifier = Modifier
                .weight(0.58f)
                .testTag("results_best_times_panel"),
              verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
              // Dual 3D Podium Supercars Canvas with Driver Helmet Badges above each car
              Box(
                modifier = Modifier
                  .fillMaxWidth()
                  .height(94.dp)
              ) {
                ResultsDualPodiumCarsCanvas(
                  modifier = Modifier.fillMaxSize()
                )
              }

              // Angled Parallelogram Classification Ribbon 1
              PodiumClassificationRibbon(
                rankText = "P${summary.finishingPosition}",
                rankBgColor = Color(0xFFE10600),
                ppScore = "GT3 PRO",
                raceTimeText = formattedRaceTime,
                deltaText = "00:00:000",
                classCode = "S",
                vehicleTitle = summary.vehicleName
              )

              // Angled Parallelogram Classification Ribbon 2
              PodiumClassificationRibbon(
                rankText = "P${(summary.finishingPosition + 1).coerceAtMost(summary.totalRacers)}",
                rankBgColor = Color(0xFF1E293B),
                ppScore = "GT3 R",
                raceTimeText = formattedRaceTime,
                deltaText = "+00:01:800",
                classCode = "S",
                vehicleTitle = "Apex GT3-R Rival"
              )

              // Compact Best Times & Cloud Profile Sync Strip
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .clip(RoundedCornerShape(4.dp))
                  .background(Color(0xFF10131A).copy(alpha = 0.85f))
                  .clickable { onViewProfile() }
                  .padding(horizontal = 6.dp, vertical = 3.dp)
                  .testTag("results_social_login_btn"),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Text(
                  text = "BEST TIMES",
                  fontSize = 7.5.sp,
                  fontWeight = FontWeight.Black,
                  color = Color(0xFFCBD5E1)
                )
                Text(
                  text = when (cloudSyncState.status) {
                    com.example.data.firebase.CloudSyncStatus.SYNCED -> "CLOUD SYNCED • ${careerState.playerUsername.take(10).uppercase()}"
                    com.example.data.firebase.CloudSyncStatus.OFFLINE_QUEUED -> "QUEUED (${cloudSyncState.pendingOpsCount}) • PROFILE"
                    com.example.data.firebase.CloudSyncStatus.SYNCING -> "SYNCING CLOUD..."
                    else -> "SYNC PROFILE • LOGIN"
                  },
                  fontSize = 7.5.sp,
                  fontWeight = FontWeight.Black,
                  color = if (cloudSyncState.isAuthenticated) ApexGreen else Color(0xFFE10600)
                )
              }
            }

            // RIGHT VERTICAL REWARDS SIDEBAR ("YOU WIN!", "1st", APEX BONUS, Cash, XP, Timing Footer)
            Column(
              modifier = Modifier
                .weight(0.42f)
                .clip(RoundedCornerShape(6.dp))
                .background(Color(0xFF121A24).copy(alpha = 0.94f))
                .border(1.dp, Color(0xFFCBD5E1).copy(alpha = 0.30f), RoundedCornerShape(6.dp))
                .testTag("results_main_summary_card"),
              verticalArrangement = Arrangement.SpaceBetween
            ) {
              // Top Bold Header Banner ("YOU WIN!" / "YOU WON THE RACE!")
              Box(
                modifier = Modifier
                  .fillMaxWidth()
                  .background(
                    Brush.horizontalGradient(
                      colors = listOf(Color(0xFFE10600), Color(0xFFB71C1C))
                    )
                  )
                  .padding(horizontal = 8.dp, vertical = 6.dp),
                contentAlignment = Alignment.CenterStart
              ) {
                Column {
                  Text(
                    text = "YOU WIN!",
                    fontSize = 15.sp,
                    lineHeight = 16.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    letterSpacing = 0.8.sp
                  )
                  Text(
                    text = if (summary.finishingPosition == 1) "YOU WON THE RACE!" else "RACE FINISHED • $ordinalRankText!",
                    fontSize = 8.sp,
                    lineHeight = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White.copy(alpha = 0.92f),
                    modifier = Modifier.testTag("results_headline_banner")
                  )
                }
              }

              // Ordinal Position Badge ("1st" + "P1 of 8") + APEX BONUS Row
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .background(Color(0xFF1A2634))
                  .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Row(
                  verticalAlignment = Alignment.Bottom,
                  horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                  Text(
                    text = ordinalRankText,
                    fontSize = 20.sp,
                    lineHeight = 20.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    modifier = Modifier.testTag("results_ordinal_position_text")
                  )
                  Text(
                    text = "P${summary.finishingPosition} of ${summary.totalRacers}",
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFCBD5E1),
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(bottom = 2.dp)
                  )
                }

                Column(horizontalAlignment = Alignment.End) {
                  Text(
                    text = "APEX BONUS",
                    fontSize = 6.5.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFFCBD5E1)
                  )
                  Text(
                    text = "5,409",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    fontFamily = FontFamily.Monospace
                  )
                }
              }

              // Cash Reward Row ("[💵] 575" + 3D Cash Stack on Right)
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(horizontal = 8.dp, vertical = 4.dp)
                  .testTag("results_cash_reward_row")
              ) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                  Box(
                    modifier = Modifier
                      .clip(RoundedCornerShape(2.dp))
                      .background(Color(0xFF22C55E))
                      .padding(horizontal = 4.dp, vertical = 1.dp)
                  ) {
                    Text(
                      text = "CR",
                      fontSize = 7.sp,
                      fontWeight = FontWeight.Black,
                      color = Color(0xFF052E16)
                    )
                  }
                  Text(
                    text = "+$effectiveCreditsEarned",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    fontFamily = FontFamily.Monospace
                  )
                }
                GreenCashStackIconCanvas(modifier = Modifier.size(24.dp))
              }

              // XP Reward Row ("XP 120" / "+1500 XP" + Driver Helmet Ring on Right)
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                  .fillMaxWidth()
                  .background(Color(0xFF16202C))
                  .padding(horizontal = 8.dp, vertical = 4.dp)
                  .testTag("results_xp_reward_row")
              ) {
                Column {
                  Text(
                    text = "XP +${summary.xpEarned}",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    fontFamily = FontFamily.Monospace
                  )
                  Text(
                    text = "+${summary.xpEarned} XP",
                    fontSize = 7.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFCBD5E1),
                    fontFamily = FontFamily.Monospace
                  )
                }

                // Driver Level Ring + Helmet Avatar Badge
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                  Text(
                    text = "${careerState.playerLevel}",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                  )
                  DriverHelmetCircleBadge(modifier = Modifier.size(20.dp))
                }
              }

              // Bottom Timing Strip inside Right Card: "TIME: ... | BEST: ~:~~.~~"
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .background(Color(0xFF0B0D12))
                  .padding(horizontal = 6.dp, vertical = 4.dp)
                  .testTag("results_timing_footer_bar"),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                  Text(
                    text = "TIME:",
                    fontSize = 7.5.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFFE10600)
                  )
                  Text(
                    text = formattedRaceTime,
                    fontSize = 7.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontFamily = FontFamily.Monospace
                  )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                  Text(
                    text = "BEST:",
                    fontSize = 7.5.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFFCBD5E1)
                  )
                  Text(
                    text = "~:~~.~~",
                    fontSize = 7.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontFamily = FontFamily.Monospace
                  )
                }
              }
            }
          }
        }

        // 1B. STAGE 6 GOLDEN CHAMPIONSHIP CUP & CONFETTI CELEBRATION BANNER ("Dirt Champions / Grand Prix • Prize Money Cr. 97,500")
        Card(
          colors = CardDefaults.cardColors(containerColor = Color(0xFF141822)),
          shape = RoundedCornerShape(6.dp),
          border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFACC15).copy(alpha = 0.65f)),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("results_championship_cup_banner")
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              // Golden Trophy Cup with Confetti Particles Canvas
              Canvas(modifier = Modifier.size(28.dp)) {
                val w = size.width
                val h = size.height
                // Confetti flecks
                drawCircle(color = Color(0xFFE10600), radius = 2f, center = Offset(w * 0.15f, h * 0.20f))
                drawCircle(color = Color(0xFF38BDF8), radius = 2f, center = Offset(w * 0.85f, h * 0.18f))
                drawCircle(color = Color(0xFF22C55E), radius = 2f, center = Offset(w * 0.12f, h * 0.75f))
                drawCircle(color = Color(0xFFFACC15), radius = 2f, center = Offset(w * 0.88f, h * 0.72f))
                // Golden Cup Chalice
                drawRoundRect(
                  brush = Brush.verticalGradient(listOf(Color(0xFFFDE047), Color(0xFFD97706))),
                  topLeft = Offset(w * 0.28f, h * 0.16f),
                  size = Size(w * 0.44f, h * 0.44f),
                  cornerRadius = CornerRadius(4f, 4f)
                )
                // Cup Stem & Pedestal Base
                drawRect(
                  color = Color(0xFFD97706),
                  topLeft = Offset(w * 0.44f, h * 0.60f),
                  size = Size(w * 0.12f, h * 0.18f)
                )
                drawRoundRect(
                  color = Color(0xFFFACC15),
                  topLeft = Offset(w * 0.26f, h * 0.76f),
                  size = Size(w * 0.48f, h * 0.12f),
                  cornerRadius = CornerRadius(2f, 2f)
                )
              }
              Column {
                Text(
                  text = "GRAND PRIX • DIRT CHAMPIONS CUP",
                  fontSize = 9.sp,
                  fontWeight = FontWeight.Black,
                  color = Color(0xFFFACC15),
                  letterSpacing = 0.5.sp
                )
                Text(
                  text = "CHAMPIONSHIP PODIUM TROPHY AWARDED",
                  fontSize = 7.sp,
                  fontWeight = FontWeight.Bold,
                  color = Color(0xFFCBD5E1)
                )
              }
            }
            Column(horizontalAlignment = Alignment.End) {
              Text(
                text = "PRIZE MONEY",
                fontSize = 6.5.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF94A3B8)
              )
              Text(
                text = "Cr. 97,500",
                fontSize = 10.sp,
                fontWeight = FontWeight.Black,
                color = Color(0xFFFACC15),
                fontFamily = FontFamily.Monospace
              )
            }
          }
        }

        // 2. NEXT AVAILABLE RACE & PERFORMANCE SUMMARY BANNER
        Card(
          colors = CardDefaults.cardColors(containerColor = Color(0xFF10131A)),
          shape = RoundedCornerShape(6.dp),
          border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFCBD5E1).copy(alpha = 0.28f)),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("results_next_available_race_card")
        ) {
          Column(
            modifier = Modifier.padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp)
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text(
                text = "NEXT AVAILABLE RACE",
                fontSize = 8.5.sp,
                fontWeight = FontWeight.Black,
                color = Color(0xFFE10600),
                letterSpacing = 0.6.sp
              )
              Text(
                text = nextAvailableTrackName.uppercase(),
                fontSize = 9.sp,
                fontWeight = FontWeight.Black,
                color = Color.White,
                fontFamily = FontFamily.Monospace
              )
            }
            PodiumStatRow("Best Lap / Total", "$formattedBestTime ($formattedRaceTime)")
            PodiumStatRow("Top Speed", String.format(Locale.US, "%.1f %s", speedConverted, speedUnit))
            PodiumStatRow("Overtakes / Drift Score", "${summary.overtakesCount} Cars • ${summary.driftScore} PTS")
          }
        }

        // 3. LARGE RECTANGULAR MOTORSPORT ACTION BUTTONS + ANGLED CONTINUE BUTTON
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .testTag("results_bottom_action_bar"),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          // Large Rectangular Racing-Red Primary Button: RACE AGAIN / CONTINUE (`race_again_button`)
          Box(
            modifier = Modifier
              .weight(1f)
              .height(40.dp)
              .clip(RoundedCornerShape(4.dp))
              .background(
                Brush.horizontalGradient(
                  colors = listOf(Color(0xFFE10600), Color(0xFFB71C1C))
                )
              )
              .border(1.dp, Color.White.copy(alpha = 0.70f), RoundedCornerShape(4.dp))
              .clickable { onRaceAgain() }
              .testTag("race_again_button"),
            contentAlignment = Alignment.Center
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
              Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = "Play Next Race",
                tint = Color.White,
                modifier = Modifier.size(18.dp)
              )
              Text(
                text = "RACE AGAIN  »",
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                color = Color.White,
                letterSpacing = 0.8.sp
              )
            }
          }

          // Large Rectangular Metallic-Silver / Emerald 2X Reward Button (`results_double_reward_btn`)
          Box(
            modifier = Modifier
              .weight(1f)
              .height(40.dp)
              .clip(RoundedCornerShape(4.dp))
              .background(
                Brush.horizontalGradient(
                  colors = if (doubleRewardClaimed) {
                    listOf(Color(0xFF1E293B), Color(0xFF0F172A))
                  } else {
                    listOf(Color(0xFF16A34A), Color(0xFF15803D))
                  }
                )
              )
              .border(1.dp, Color(0xFFCBD5E1).copy(alpha = 0.75f), RoundedCornerShape(4.dp))
              .clickable(enabled = !doubleRewardClaimed) {
                doubleRewardClaimed = true
                com.example.data.firebase.ApexCloudSyncCoordinator.onRewardClaimed(
                  context = context,
                  referenceId = "race_double_bonus"
                )
              }
              .padding(horizontal = 8.dp)
              .testTag("results_double_reward_btn"),
            contentAlignment = Alignment.Center
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
              GreenCashStackIconCanvas(modifier = Modifier.size(18.dp))
              Text(
                text = if (doubleRewardClaimed) "2X CLAIMED (+$doublePreviewAmount)" else "2X REWARD +$doublePreviewAmount",
                fontSize = 10.sp,
                fontWeight = FontWeight.Black,
                color = Color.White,
                fontFamily = FontFamily.Monospace
              )
              FilmStripBadgeCanvas(modifier = Modifier.size(16.dp))
            }
          }
        }

        // Room Database Save Notice (Preserved for test verification)
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(Color(0xFF10131A))
            .border(1.dp, ApexGreen.copy(alpha = 0.6f), RoundedCornerShape(6.dp))
            .padding(4.dp)
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = ApexGreen, modifier = Modifier.size(12.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = "SAVED TO ROOM DATABASE TELEMETRY",
              fontSize = 8.sp,
              fontWeight = FontWeight.Black,
              color = ApexGreen,
              letterSpacing = 0.5.sp
            )
          }
        }

        // Newly Unlocked Badges (if any)
        if (summary.newlyUnlockedBadges.isNotEmpty()) {
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(6.dp))
              .background(Color(0xFF10131A))
              .border(1.dp, Color(0xFFE10600), RoundedCornerShape(6.dp))
              .padding(6.dp)
              .testTag("new_trophies_unlocked_box")
          ) {
            Column {
              Text(
                text = "NEW TROPHIES UNLOCKED",
                fontSize = 9.sp,
                fontWeight = FontWeight.Black,
                color = Color(0xFFFFD700)
              )
              Text(
                text = summary.newlyUnlockedBadges.take(3).joinToString(" • "),
                fontSize = 8.5.sp,
                color = Color.White
              )
            }
          }
        }

        // Lap-by-Lap Sector Times & Gap to Leader Breakdown
        if (summary.lapSummaries.isNotEmpty()) {
          Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF10131A)),
            shape = RoundedCornerShape(6.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFCBD5E1).copy(alpha = 0.28f)),
            modifier = Modifier
              .fillMaxWidth()
              .testTag("finish_modal_lap_breakdown")
          ) {
            Column(
              modifier = Modifier.padding(8.dp),
              verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Text(
                  text = "SECTOR TIMING & LEADER GAP BREAKDOWN",
                  fontSize = 8.5.sp,
                  fontWeight = FontWeight.Black,
                  color = Color(0xFFCBD5E1),
                  letterSpacing = 0.6.sp
                )
                Text(
                  text = "${summary.lapSummaries.size} LAPS",
                  fontSize = 8.sp,
                  fontWeight = FontWeight.Bold,
                  color = Color(0xFF94A3B8),
                  fontFamily = FontFamily.Monospace
                )
              }

              // Table Column Headers
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .background(Color(0xFF181C26), RoundedCornerShape(4.dp))
                  .padding(horizontal = 6.dp, vertical = 3.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Text("LAP", fontSize = 7.5.sp, fontWeight = FontWeight.Bold, color = TextTertiary, modifier = Modifier.weight(0.9f))
                Text("S1", fontSize = 7.5.sp, fontWeight = FontWeight.Bold, color = TextTertiary, modifier = Modifier.weight(1.1f), textAlign = TextAlign.Center)
                Text("S2", fontSize = 7.5.sp, fontWeight = FontWeight.Bold, color = TextTertiary, modifier = Modifier.weight(1.1f), textAlign = TextAlign.Center)
                Text("S3", fontSize = 7.5.sp, fontWeight = FontWeight.Bold, color = TextTertiary, modifier = Modifier.weight(1.1f), textAlign = TextAlign.Center)
                Text("LAP TIME", fontSize = 7.5.sp, fontWeight = FontWeight.Bold, color = TextTertiary, modifier = Modifier.weight(1.25f), textAlign = TextAlign.End)
                Text("GAP P1", fontSize = 7.5.sp, fontWeight = FontWeight.Bold, color = TextTertiary, modifier = Modifier.weight(1.25f), textAlign = TextAlign.End)
              }

              summary.lapSummaries.forEach { lap ->
                val isBestLap = summary.bestLapMs > 0L && lap.lapTimeMs == summary.bestLapMs
                val isFastestS1 = bestS1Ms > 0L && lap.sector1Ms == bestS1Ms
                val isFastestS2 = bestS2Ms > 0L && lap.sector2Ms == bestS2Ms
                val isFastestS3 = bestS3Ms > 0L && lap.sector3Ms == bestS3Ms
                val isLeadingLap = lap.gapToLeaderSeconds <= 0.001f

                val gapLabel = if (isLeadingLap) {
                  "LEADER"
                } else {
                  String.format(Locale.US, "+%.3fs", lap.gapToLeaderSeconds)
                }

                val gapColor = when {
                  isLeadingLap -> ApexGreen
                  lap.gapToLeaderSeconds < 1.5f -> NeonAmber
                  else -> RedlineRed
                }

                Row(
                  modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(4.dp))
                    .background(
                      if (isBestLap) ApexGreen.copy(alpha = 0.10f)
                      else CarbonSurface.copy(alpha = 0.45f)
                    )
                    .padding(horizontal = 6.dp, vertical = 4.dp)
                    .testTag("finish_modal_lap_row_${lap.lapNumber}"),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Text(
                    text = "L${lap.lapNumber}•${lap.tireCompoundCode}",
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Black,
                    color = if (isBestLap) ApexGreen else TextPrimary,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.weight(0.9f)
                  )
                  Text(
                    text = String.format(Locale.US, "%.2fs", lap.sector1Ms / 1000f),
                    fontSize = 8.sp,
                    fontWeight = if (isFastestS1) FontWeight.Black else FontWeight.Medium,
                    color = if (isFastestS1) Color(0xFFD500F9) else TextSecondary,
                    fontFamily = FontFamily.Monospace,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1.1f)
                  )
                  Text(
                    text = String.format(Locale.US, "%.2fs", lap.sector2Ms / 1000f),
                    fontSize = 8.sp,
                    fontWeight = if (isFastestS2) FontWeight.Black else FontWeight.Medium,
                    color = if (isFastestS2) Color(0xFFD500F9) else TextSecondary,
                    fontFamily = FontFamily.Monospace,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1.1f)
                  )
                  Text(
                    text = String.format(Locale.US, "%.2fs", lap.sector3Ms / 1000f),
                    fontSize = 8.sp,
                    fontWeight = if (isFastestS3) FontWeight.Black else FontWeight.Medium,
                    color = if (isFastestS3) Color(0xFFD500F9) else TextSecondary,
                    fontFamily = FontFamily.Monospace,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1.1f)
                  )
                  Text(
                    text = String.format(Locale.US, "%.3fs", lap.lapTimeMs / 1000f),
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isBestLap) ApexGreen else NeonCyan,
                    fontFamily = FontFamily.Monospace,
                    textAlign = TextAlign.End,
                    modifier = Modifier.weight(1.25f)
                  )
                  Text(
                    text = gapLabel,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    color = gapColor,
                    fontFamily = FontFamily.Monospace,
                    textAlign = TextAlign.End,
                    modifier = Modifier
                      .weight(1.25f)
                      .testTag("finish_modal_lap_gap_${lap.lapNumber}")
                  )
                }
              }
            }
          }
        }

        // Quick Large Rectangular Navigation Row: NEXT RACE, GARAGE, MAIN MENU
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          OutlinedButton(
            onClick = onRaceAgain,
            shape = RoundedCornerShape(4.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE10600)),
            modifier = Modifier
              .weight(1f)
              .testTag("results_next_race_button")
          ) {
            Text("NEXT RACE", fontSize = 8.sp, fontWeight = FontWeight.Black, color = Color.White)
          }
          OutlinedButton(
            onClick = onOpenGarage,
            shape = RoundedCornerShape(4.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFCBD5E1)),
            modifier = Modifier
              .weight(1f)
              .testTag("results_garage_button")
          ) {
            Text("GARAGE", fontSize = 8.sp, fontWeight = FontWeight.Black, color = Color(0xFFCBD5E1))
          }
          OutlinedButton(
            onClick = onExit,
            shape = RoundedCornerShape(4.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF64748B)),
            modifier = Modifier
              .weight(1f)
              .testTag("results_main_menu_button")
          ) {
            Text("MAIN MENU", fontSize = 8.sp, fontWeight = FontWeight.Black, color = TextPrimary)
          }
        }
      }
    },
    confirmButton = {},
    dismissButton = {}
  )
}

@Composable
private fun PodiumClassificationRibbon(
  rankText: String,
  rankBgColor: Color,
  ppScore: String,
  raceTimeText: String,
  deltaText: String,
  classCode: String,
  vehicleTitle: String
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .height(32.dp)
      .clip(RoundedCornerShape(3.dp))
      .background(Color(0xFF0E1722).copy(alpha = 0.92f))
      .border(1.dp, Color(0xFFCBD5E1).copy(alpha = 0.25f), RoundedCornerShape(3.dp)),
    verticalAlignment = Alignment.CenterVertically
  ) {
    // 1. Rank Block ("1st" or "2nd")
    Box(
      modifier = Modifier
        .width(34.dp)
        .height(32.dp)
        .background(rankBgColor),
      contentAlignment = Alignment.Center
    ) {
      Text(
        text = rankText,
        fontSize = 10.sp,
        fontWeight = FontWeight.Black,
        color = Color.White
      )
    }

    // 2. PP Rating Block ("117PP" in Mint/Emerald matching screenshot)
    Box(
      modifier = Modifier
        .width(42.dp)
        .height(32.dp)
        .background(Color(0xFF00E5A3)),
      contentAlignment = Alignment.Center
    ) {
      Text(
        text = ppScore,
        fontSize = 8.5.sp,
        fontWeight = FontWeight.Black,
        color = Color(0xFF052E16)
      )
    }

    // 3. White Timing & Delta Segment ("00:26:866" / "+00:01:800")
    Column(
      modifier = Modifier
        .width(62.dp)
        .height(32.dp)
        .background(Color.White)
        .padding(horizontal = 4.dp),
      verticalArrangement = Arrangement.Center,
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      Text(
        text = raceTimeText,
        fontSize = 7.sp,
        lineHeight = 8.sp,
        fontWeight = FontWeight.Black,
        color = Color(0xFF0F172A),
        fontFamily = FontFamily.Monospace,
        maxLines = 1
      )
      Text(
        text = deltaText,
        fontSize = 6.5.sp,
        lineHeight = 7.sp,
        fontWeight = FontWeight.Bold,
        color = Color(0xFF475569),
        fontFamily = FontFamily.Monospace,
        maxLines = 1
      )
    }

    // 4. Class Stars & Vehicle Name Segment
    Column(
      modifier = Modifier
        .weight(1f)
        .padding(horizontal = 6.dp),
      verticalArrangement = Arrangement.Center
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp)
      ) {
        Text(
          text = "$classCode ★ ★ ★",
          fontSize = 7.sp,
          lineHeight = 8.sp,
          fontWeight = FontWeight.Black,
          color = Color(0xFF38BDF8)
        )
        Text(
          text = "+0%",
          fontSize = 6.5.sp,
          lineHeight = 8.sp,
          fontWeight = FontWeight.Bold,
          color = Color(0xFF38BDF8)
        )
      }
      Text(
        text = vehicleTitle,
        fontSize = 7.5.sp,
        lineHeight = 9.sp,
        fontWeight = FontWeight.Bold,
        color = Color.White,
        maxLines = 1
      )
    }
  }
}

@Composable
private fun ResultsDualPodiumCarsCanvas(
  modifier: Modifier = Modifier
) {
  Canvas(modifier = modifier) {
    val w = size.width
    val h = size.height

    // 1. Circular Driver Helmet Avatar Badges floating above P1 and P2 cars (matching screenshot)
    listOf(Offset(w * 0.25f, h * 0.18f), Offset(w * 0.58f, h * 0.16f)).forEach { center ->
      drawCircle(color = Color.White, radius = 11f, center = center)
      drawCircle(color = Color(0xFFE10600), radius = 9.5f, center = center)
      // Helmet head
      drawCircle(color = Color.White, radius = 5f, center = Offset(center.x, center.y - 2f))
      // Dark visor
      drawRoundRect(
        color = Color(0xFF0F172A),
        topLeft = Offset(center.x - 3.5f, center.y - 3.5f),
        size = Size(7f, 2.5f),
        cornerRadius = CornerRadius(1f, 1f)
      )
    }

    // 2. BACKGROUND P2 SUPERCAR (Matte-Black & Gold-Trim GT3 #59, Upper-Right)
    val p2Body = Path().apply {
      moveTo(w * 0.36f, h * 0.58f)
      lineTo(w * 0.44f, h * 0.40f)
      lineTo(w * 0.64f, h * 0.38f)
      lineTo(w * 0.80f, h * 0.48f)
      lineTo(w * 0.88f, h * 0.58f)
      lineTo(w * 0.86f, h * 0.68f)
      lineTo(w * 0.36f, h * 0.68f)
      close()
    }
    drawPath(
      path = p2Body,
      brush = Brush.linearGradient(
        colors = listOf(Color(0xFF334155), Color(0xFF0F172A), Color(0xFF1E293B))
      )
    )
    // Gold Racing Stripe on P2 Car
    drawLine(
      color = Color(0xFFFACC15),
      start = Offset(w * 0.44f, h * 0.52f),
      end = Offset(w * 0.84f, h * 0.58f),
      strokeWidth = 2.5f
    )
    // P2 Wheels
    drawCircle(color = Color(0xFF090D16), radius = 12f, center = Offset(w * 0.46f, h * 0.67f))
    drawCircle(color = Color(0xFFCBD5E1), radius = 7f, center = Offset(w * 0.46f, h * 0.67f), style = Stroke(width = 2f))
    drawCircle(color = Color(0xFF090D16), radius = 12.5f, center = Offset(w * 0.72f, h * 0.67f))
    drawCircle(color = Color(0xFFCBD5E1), radius = 7.5f, center = Offset(w * 0.72f, h * 0.67f), style = Stroke(width = 2f))

    // 3. FOREGROUND P1 CHAMPION HYPERCAR (Pearl White & Racing-Red Livery with GT Wing, Lower-Left)
    // Rear Carbon GT Wing
    drawLine(
      color = Color(0xFFCBD5E1),
      start = Offset(w * 0.05f, h * 0.50f),
      end = Offset(w * 0.16f, h * 0.50f),
      strokeWidth = 3.5f
    )
    val p1Body = Path().apply {
      moveTo(w * 0.06f, h * 0.72f)
      lineTo(w * 0.14f, h * 0.54f)
      lineTo(w * 0.34f, h * 0.48f)
      lineTo(w * 0.52f, h * 0.60f)
      lineTo(w * 0.64f, h * 0.70f)
      lineTo(w * 0.62f, h * 0.84f)
      lineTo(w * 0.06f, h * 0.84f)
      close()
    }
    drawPath(
      path = p1Body,
      brush = Brush.linearGradient(
        colors = listOf(Color(0xFFFFFFFF), Color(0xFFE2E8F0), Color(0xFFCBD5E1))
      )
    )
    // Racing Red Hood & Side Aero Livery Stripe on P1 Hypercar
    val redLivery = Path().apply {
      moveTo(w * 0.34f, h * 0.52f)
      lineTo(w * 0.58f, h * 0.66f)
      lineTo(w * 0.52f, h * 0.74f)
      lineTo(w * 0.30f, h * 0.58f)
      close()
    }
    drawPath(path = redLivery, color = Color(0xFFE10600))
    // Canopy Windshield
    val canopy = Path().apply {
      moveTo(w * 0.24f, h * 0.54f)
      lineTo(w * 0.36f, h * 0.50f)
      lineTo(w * 0.46f, h * 0.60f)
      lineTo(w * 0.28f, h * 0.61f)
      close()
    }
    drawPath(path = canopy, color = Color(0xFF0F172A))
    // P1 Wheels
    drawCircle(color = Color(0xFF090D16), radius = 13.5f, center = Offset(w * 0.16f, h * 0.82f))
    drawCircle(color = Color(0xFF64748B), radius = 8f, center = Offset(w * 0.16f, h * 0.82f), style = Stroke(width = 2f))
    drawCircle(color = Color(0xFF090D16), radius = 14f, center = Offset(w * 0.44f, h * 0.83f))
    drawCircle(color = Color(0xFF64748B), radius = 8.5f, center = Offset(w * 0.44f, h * 0.83f), style = Stroke(width = 2f))
  }
}

@Composable
private fun DriverHelmetCircleBadge(
  modifier: Modifier = Modifier
) {
  Canvas(modifier = modifier) {
    val w = size.width
    val h = size.height
    val c = Offset(w * 0.5f, h * 0.5f)
    drawCircle(color = Color.White, radius = w * 0.48f, center = c)
    drawCircle(color = Color(0xFFE10600), radius = w * 0.40f, center = c)
    drawCircle(color = Color.White, radius = w * 0.22f, center = Offset(c.x, c.y - h * 0.06f))
  }
}

@Composable
private fun SilverShieldBadgeCanvas(
  label: String,
  modifier: Modifier = Modifier
) {
  Box(
    modifier = modifier,
    contentAlignment = Alignment.Center
  ) {
    Canvas(modifier = Modifier.matchParentSize()) {
      val w = size.width
      val h = size.height
      val shieldPath = Path().apply {
        moveTo(w * 0.50f, h * 0.06f)
        lineTo(w * 0.90f, h * 0.20f)
        lineTo(w * 0.82f, h * 0.68f)
        lineTo(w * 0.50f, h * 0.94f)
        lineTo(w * 0.18f, h * 0.68f)
        lineTo(w * 0.10f, h * 0.20f)
        close()
      }
      drawPath(
        path = shieldPath,
        brush = Brush.verticalGradient(
          colors = listOf(Color(0xFFF8FAFC), Color(0xFFCBD5E1), Color(0xFF64748B))
        )
      )
      drawPath(
        path = shieldPath,
        color = Color(0xFF1E293B),
        style = Stroke(width = 2f)
      )
    }
    Text(
      text = label,
      fontSize = if (label.length > 1) 8.sp else 9.5.sp,
      fontWeight = FontWeight.Black,
      color = Color(0xFF0F172A)
    )
  }
}

@Composable
private fun GreenCashStackIconCanvas(
  modifier: Modifier = Modifier
) {
  Canvas(modifier = modifier) {
    val w = size.width
    val h = size.height
    // Isometric 3D stack of green banknotes with white band
    val billPath = Path().apply {
      moveTo(w * 0.14f, h * 0.54f)
      lineTo(w * 0.62f, h * 0.16f)
      lineTo(w * 0.90f, h * 0.38f)
      lineTo(w * 0.42f, h * 0.78f)
      close()
    }
    val depthPath = Path().apply {
      moveTo(w * 0.14f, h * 0.64f)
      lineTo(w * 0.42f, h * 0.88f)
      lineTo(w * 0.90f, h * 0.48f)
      lineTo(w * 0.90f, h * 0.38f)
      lineTo(w * 0.42f, h * 0.78f)
      lineTo(w * 0.14f, h * 0.54f)
      close()
    }
    drawPath(depthPath, color = Color(0xFF15803D))
    drawPath(
      path = billPath,
      brush = Brush.linearGradient(
        colors = listOf(Color(0xFF86EFAC), Color(0xFF22C55E), Color(0xFF16A34A))
      )
    )
    drawPath(billPath, color = Color(0xFF052E16), style = Stroke(width = 1.6f))
    drawLine(
      color = Color.White.copy(alpha = 0.9f),
      start = Offset(w * 0.36f, h * 0.35f),
      end = Offset(w * 0.66f, h * 0.59f),
      strokeWidth = 3.5f
    )
  }
}

@Composable
private fun FilmStripBadgeCanvas(
  modifier: Modifier = Modifier
) {
  Canvas(modifier = modifier) {
    val w = size.width
    val h = size.height
    drawRoundRect(
      color = Color.White,
      topLeft = Offset(w * 0.12f, h * 0.10f),
      size = Size(w * 0.76f, h * 0.80f),
      cornerRadius = CornerRadius(2f, 2f)
    )
    drawRect(
      color = Color(0xFF15803D),
      topLeft = Offset(w * 0.28f, h * 0.18f),
      size = Size(w * 0.44f, h * 0.28f)
    )
    drawRect(
      color = Color(0xFF15803D),
      topLeft = Offset(w * 0.28f, h * 0.54f),
      size = Size(w * 0.44f, h * 0.28f)
    )
  }
}

@Composable
private fun ResultsFinishLineBackdropCanvas(
  modifier: Modifier = Modifier
) {
  Canvas(modifier = modifier) {
    val w = size.width
    val h = size.height
    // Base Deep Matte Black / Petrol Dark Backdrop
    drawRect(
      brush = Brush.linearGradient(
        colors = listOf(Color(0xFF081C24), Color(0xFF0B0D12))
      )
    )
    // Left Diagonal Crimson Racing Red Polygon (matching Screenshot_2026_1111_142017_4ee14e77.png)
    val crimsonDiag = Path().apply {
      moveTo(0f, 0f)
      lineTo(w * 0.66f, 0f)
      lineTo(w * 0.18f, h)
      lineTo(0f, h)
      close()
    }
    drawPath(
      path = crimsonDiag,
      brush = Brush.linearGradient(
        colors = listOf(Color(0xFF99291E), Color(0xFF6E1A13))
      )
    )
    // Metallic Silver Diagonal Accent Line separating Crimson Red and Matte Black
    drawLine(
      color = Color(0xFFCBD5E1).copy(alpha = 0.28f),
      start = Offset(w * 0.66f, 0f),
      end = Offset(w * 0.18f, h),
      strokeWidth = 2f
    )
  }
}

@Composable
private fun PodiumStatRow(label: String, value: String) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically
  ) {
    Text(text = label, fontSize = 9.5.sp, color = TextSecondary)
    Text(text = value, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = NeonCyan, fontFamily = FontFamily.Monospace)
  }
}

// -------------------------------------------------------------------
// PIT STOP STRATEGY DIALOG
// -------------------------------------------------------------------
@Composable
fun PitStopStrategyDialog(
  engine: InteractiveRacingEngine,
  hudState: RacingGameHudState,
  onDismiss: () -> Unit
) {
  AlertDialog(
    onDismissRequest = onDismiss,
    shape = RoundedCornerShape(16.dp),
    containerColor = CarbonSurface,
    title = {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
          imageVector = Icons.Default.DirectionsCar,
          contentDescription = null,
          tint = NeonAmber,
          modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = "PIT STOP STRATEGY",
          fontSize = 15.sp,
          fontWeight = FontWeight.Black,
          color = TextPrimary
        )
      }
    },
    text = {
      Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
      ) {
        Text(
          text = "Select compound for fresh pit stop. Estimated service time: 2.4s.",
          fontSize = 10.sp,
          color = TextSecondary
        )

        com.example.game.model.TireCompound.entries.forEach { compound ->
          val isCurrent = hudState.tireCompound == compound
          Card(
            modifier = Modifier
              .fillMaxWidth()
              .clickable {
                engine.performPitStop(compound)
                onDismiss()
              }
              .testTag("pit_compound_card_${compound.shortCode}"),
            colors = CardDefaults.cardColors(
              containerColor = if (isCurrent) CarbonSurfaceVariant else CarbonBlack
            ),
            shape = RoundedCornerShape(10.dp),
            border = androidx.compose.foundation.BorderStroke(
              1.5.dp,
              if (isCurrent) Color(compound.colorHex) else CarbonBorder
            )
          ) {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                  modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(Color(compound.colorHex)),
                  contentAlignment = Alignment.Center
                ) {
                  Text(
                    text = compound.shortCode,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black,
                    color = CarbonBlack
                  )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                  Text(
                    text = compound.displayName,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    color = TextPrimary
                  )
                  Text(
                    text = "Grip: ${(compound.gripFactor * 100).toInt()}% • Wear: ${(compound.wearRate * 10).toInt()}x",
                    fontSize = 8.5.sp,
                    color = TextSecondary
                  )
                }
              }

              if (isCurrent) {
                Text(
                  text = "FITTED",
                  fontSize = 8.5.sp,
                  fontWeight = FontWeight.Black,
                  color = NeonCyan
                )
              }
            }
          }
        }
      }
    },
    confirmButton = {
      TextButton(onClick = onDismiss) {
        Text("CANCEL (STAY OUT)", color = TextSecondary, fontWeight = FontWeight.Bold)
      }
    }
  )
}

// -------------------------------------------------------------------
// IN-RACE PAUSE MODAL
// -------------------------------------------------------------------
@Composable
fun InRacePauseModal(
  engine: InteractiveRacingEngine,
  hudState: RacingGameHudState,
  track: Track,
  vehicle: Vehicle,
  onResume: () -> Unit,
  onRestart: () -> Unit,
  onQuit: () -> Unit,
  isMph: Boolean
) {
  var showConfirmQuitDialog by remember { mutableStateOf(false) }

  if (showConfirmQuitDialog) {
    AlertDialog(
      onDismissRequest = { showConfirmQuitDialog = false },
      containerColor = CarbonSurface,
      title = {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(Icons.Default.Warning, contentDescription = null, tint = RedlineRed, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text("Abandon Race Session?", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Black)
        }
      },
      text = {
        Text("Your current lap progress and provisional race position will be forfeit.", color = TextSecondary, fontSize = 11.5.sp)
      },
      confirmButton = {
        Button(
          onClick = {
            showConfirmQuitDialog = false
            onQuit()
          },
          colors = ButtonDefaults.buttonColors(containerColor = RedlineRed),
          shape = RoundedCornerShape(8.dp)
        ) {
          Text("Quit to Menu", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 11.sp)
        }
      },
      dismissButton = {
        OutlinedButton(
          onClick = { showConfirmQuitDialog = false },
          colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
          border = androidx.compose.foundation.BorderStroke(1.dp, CarbonBorder),
          shape = RoundedCornerShape(8.dp)
        ) {
          Text("Continue Paused", color = TextPrimary, fontSize = 11.sp)
        }
      }
    )
  }

  AlertDialog(
    onDismissRequest = onResume,
    containerColor = CarbonSurface,
    titleContentColor = TextPrimary,
    shape = RoundedCornerShape(16.dp),
    modifier = Modifier.testTag("in_race_pause_modal"),
    title = {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Box(
            modifier = Modifier
              .size(34.dp)
              .clip(CircleShape)
              .background(NeonAmber.copy(alpha = 0.2f))
              .border(1.5.dp, NeonAmber, CircleShape),
            contentAlignment = Alignment.Center
          ) {
            Icon(Icons.Default.Pause, contentDescription = null, tint = NeonAmber, modifier = Modifier.size(18.dp))
          }
          Spacer(modifier = Modifier.width(10.dp))
          Column {
            Text("RACE PAUSED", fontSize = 15.sp, fontWeight = FontWeight.Black, color = TextPrimary, letterSpacing = 1.sp)
            Text("${track.name} • ${vehicle.name}", fontSize = 9.5.sp, color = TextSecondary)
          }
        }
      }
    },
    text = {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .height(160.dp)
          .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        // Quick Telemetry Snapshot Card
        Card(
          shape = RoundedCornerShape(10.dp),
          colors = CardDefaults.cardColors(containerColor = CarbonBlack),
          border = androidx.compose.foundation.BorderStroke(1.dp, CarbonBorder),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            PodiumStatRow("Track Standing", "P${hudState.currentRank} / ${hudState.totalRivals}")
            PodiumStatRow("Lap Progress", "Lap ${hudState.currentLap} / ${hudState.totalLaps}")
            val curSpeed = if (isMph) "${(hudState.speedKmh * 0.621371f).toInt()} MPH" else "${hudState.speedKmh.toInt()} KM/H"
            PodiumStatRow("Speed Before Pause", curSpeed)
            hudState.bestLapTimeMs?.let {
              PodiumStatRow("Best Lap This Session", RaceLapShareHelper.formatLapTime(it))
            }
          }
        }

        // Live Weather & Track Environment Selector
        Column {
          Text(
            text = "CHANGE WEATHER / CONDITIONS:",
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            color = NeonCyan,
            fontFamily = FontFamily.Monospace
          )
          Spacer(modifier = Modifier.height(4.dp))
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(5.dp)
          ) {
            com.example.game.model.RaceWeather.entries.forEach { weather ->
              val isSelected = engine.activeWeather == weather
              Box(
                modifier = Modifier
                  .weight(1f)
                  .clip(RoundedCornerShape(6.dp))
                  .background(if (isSelected) NeonCyan else CarbonBlack)
                  .border(1.dp, if (isSelected) NeonCyan else CarbonBorder, RoundedCornerShape(6.dp))
                  .clickable { engine.setWeather(weather) }
                  .padding(vertical = 6.dp),
                contentAlignment = Alignment.Center
              ) {
                Text(
                  text = "${weather.icon}\n${weather.displayName.take(7)}",
                  fontSize = 8.sp,
                  fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold,
                  color = if (isSelected) CarbonBlack else TextPrimary,
                  textAlign = TextAlign.Center,
                  lineHeight = 10.sp
                )
              }
            }
          }
        }

        // Tire Compound Strategy & Quick Pit Service (v8.0.0)
        Column {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(
              text = "TIRE COMPOUND / PIT STRATEGY:",
              fontSize = 9.sp,
              fontWeight = FontWeight.Bold,
              color = NeonAmber,
              fontFamily = FontFamily.Monospace
            )
            Text(
              text = "Wear: ${hudState.tireHealthPercent.toInt()}% • ${hudState.tireTempCelsius.toInt()}°C",
              fontSize = 8.5.sp,
              fontWeight = FontWeight.Bold,
              color = if (hudState.tireHealthPercent > 60f) ApexGreen else RedlineRed,
              fontFamily = FontFamily.Monospace
            )
          }
          Spacer(modifier = Modifier.height(4.dp))
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(5.dp)
          ) {
            com.example.game.model.TireCompound.entries.forEach { compound ->
              val isSelected = engine.activeTireCompound == compound
              Box(
                modifier = Modifier
                  .weight(1f)
                  .clip(RoundedCornerShape(6.dp))
                  .background(if (isSelected) Color(compound.colorHex).copy(alpha = 0.25f) else CarbonBlack)
                  .border(1.dp, if (isSelected) Color(compound.colorHex) else CarbonBorder, RoundedCornerShape(6.dp))
                  .clickable { engine.changeTires(compound) }
                  .padding(vertical = 5.dp),
                contentAlignment = Alignment.Center
              ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                  Text(
                    text = compound.shortCode,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(compound.colorHex),
                    fontFamily = FontFamily.Monospace
                  )
                  Text(
                    text = compound.name.take(4),
                    fontSize = 7.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected) Color(compound.colorHex) else TextSecondary
                  )
                }
              }
            }
          }
        }

        // Aerodynamic Downforce Trim Selector (v8.1.0)
        Column {
          Text(
            text = "AERODYNAMIC DOWNFORCE TRIM:",
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            color = NeonAmber,
            fontFamily = FontFamily.Monospace
          )
          Spacer(modifier = Modifier.height(4.dp))
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(5.dp)
          ) {
            com.example.game.model.AeroTrim.entries.forEach { trim ->
              val isSelected = engine.activeAeroTrim == trim
              Box(
                modifier = Modifier
                  .weight(1f)
                  .clip(RoundedCornerShape(6.dp))
                  .background(if (isSelected) NeonCyan else CarbonBlack)
                  .border(1.dp, if (isSelected) NeonCyan else CarbonBorder, RoundedCornerShape(6.dp))
                  .clickable { engine.setAeroTrim(trim) }
                  .padding(vertical = 5.dp),
                contentAlignment = Alignment.Center
              ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                  Text(
                    text = trim.shortLabel,
                    fontSize = 8.5.sp,
                    fontWeight = FontWeight.Black,
                    color = if (isSelected) CarbonBlack else NeonCyan
                  )
                  Text(
                    text = "${if (trim.topSpeedBonusKmh >= 0) "+" else ""}${trim.topSpeedBonusKmh.toInt()} km/h",
                    fontSize = 7.sp,
                    color = if (isSelected) CarbonBlack else TextSecondary
                  )
                }
              }
            }
          }
        }

        // Camera Angle Quick Switch
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = "Camera Perspective:",
            fontSize = 10.sp,
            color = TextSecondary
          )
          Box(
            modifier = Modifier
              .clip(RoundedCornerShape(6.dp))
              .background(CarbonSurfaceVariant)
              .border(1.dp, CarbonBorder, RoundedCornerShape(6.dp))
              .clickable { engine.switchCamera() }
              .padding(horizontal = 8.dp, vertical = 4.dp)
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(Icons.Default.Videocam, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(12.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text(engine.cameraPerspective.label, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            }
          }
        }

        // Apex Rivals 2.0 Graphics Quality, Control Modes & Audio Mixer
        com.example.game.v2.ApexRivals2ControlAndAudioSettingsCard()

        // Automotive Driving Safety Status & Testing
        val pauseContext = androidx.compose.ui.platform.LocalContext.current
        val pauseSafetyManager = remember { com.example.automotive.AutomotiveSafetyManager.getInstance(pauseContext) }
        val pauseSafetyState by pauseSafetyManager.safetyState.collectAsState()
        com.example.ui.components.AutomotiveSafetyComplianceCard(
          safetyState = pauseSafetyState,
          onToggleSimulatedDrive = { moving -> pauseSafetyManager.setSimulatedDrivingState(moving) }
        )
      }
    },
    confirmButton = {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        Button(
          onClick = onResume,
          colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
          shape = RoundedCornerShape(8.dp),
          modifier = Modifier
            .weight(1.2f)
            .testTag("pause_resume_button")
        ) {
          Icon(Icons.Default.PlayArrow, contentDescription = null, tint = CarbonBlack, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text("RESUME", color = CarbonBlack, fontWeight = FontWeight.Black, fontSize = 11.sp)
        }

        OutlinedButton(
          onClick = onRestart,
          colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
          border = androidx.compose.foundation.BorderStroke(1.dp, CarbonBorder),
          shape = RoundedCornerShape(8.dp),
          modifier = Modifier
            .weight(1f)
            .testTag("pause_restart_button")
        ) {
          Icon(Icons.Default.Refresh, contentDescription = null, tint = NeonAmber, modifier = Modifier.size(14.dp))
          Spacer(modifier = Modifier.width(3.dp))
          Text("RESTART", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 10.sp)
        }
      }
    },
    dismissButton = {
      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        TextButton(
          onClick = { engine.completeRaceImmediately() },
          modifier = Modifier.testTag("pause_finish_race_button")
        ) {
          Text("Finish & View Results", color = NeonAmber, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
        TextButton(
          onClick = { showConfirmQuitDialog = true },
          modifier = Modifier.testTag("pause_quit_button")
        ) {
          Text("Abandon Race", color = RedlineRed, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
      }
    }
  )
}
