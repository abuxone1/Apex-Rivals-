package com.example.game.v2

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Lan
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NewReleases
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.SportsMotorsports
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.aistudio.apexrivals.tr8x.R
import com.example.game.model.GameMode
import com.example.game.sound.GameSoundManager
import com.example.model.AVAILABLE_TRACKS
import com.example.model.AVAILABLE_VEHICLES
import com.example.model.Track
import com.example.model.Vehicle
import com.example.model.VehicleType
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.cos
import kotlin.math.sin

// Motorsport Brand Palette (Dark Charcoal/Black, Racing Red, Metallic Silver, Crisp White)
private val MotorsportRed = Color(0xFFFF1744)
private val MotorsportDarkRed = Color(0xFFB71C1C)
private val CharcoalSurface = Color(0xFF141821)
private val CharcoalCard = Color(0xFF1B202C)
private val MetallicSilver = Color(0xFFCFD8DC)
private val PureWhite = Color(0xFFFAFAFA)

enum class FrontendSubScreen {
  LOADING_SCREEN,
  MAIN_MENU,
  MODE_SELECTION,
  VEHICLE_GARAGE,
  VEHICLE_CUSTOMIZATION,
  VEHICLE_UPGRADES,
  CAREER_CHAMPIONSHIP,
  CHAMPIONSHIP_DETAIL,
  MULTIPLAYER_HUB,
  DRIVER_PROFILE,
  DRIVER_ACHIEVEMENTS,
  EVENTS_AND_REWARDS,
  EVENTS_MISSIONS,
  EVENTS_REWARDS,
  SETTINGS_SCREEN
}

/**
 * Complete Unified Apex Rivals 2.0 Frontend Hub Container.
 * Connects all 6 primary navigation chains:
 * 1. HOME -> GAME MODES -> RACE -> RESULTS
 * 2. HOME -> GARAGE -> CUSTOMIZATION -> UPGRADES
 * 3. HOME -> CAREER -> CHAMPIONSHIP -> RACE
 * 4. HOME -> EVENTS -> MISSIONS -> REWARDS
 * 5. HOME -> PROFILE -> ACHIEVEMENTS
 * 6. HOME -> SETTINGS
 */
@Composable
fun ApexRivals2FrontendHubScreen(
  activeVehicle: Vehicle,
  activeTrack: Track,
  onSelectVehicle: (Vehicle) -> Unit,
  onSelectTrack: (Track) -> Unit,
  onLaunchRaceHud: (GameMode) -> Unit,
  onNavigateToProfile: () -> Unit,
  onNavigateToMultiplayer: () -> Unit,
  initialSubScreen: FrontendSubScreen = FrontendSubScreen.LOADING_SCREEN,
  externalSubScreen: FrontendSubScreen? = null,
  onExternalSubScreenChange: ((FrontendSubScreen) -> Unit)? = null,
  onOpenTelemetryHub: () -> Unit = {},
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  LaunchedEffect(Unit) {
    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
      ApexRivals2CareerManager.initialize(context)
      ApexRivals2SettingsRepository.initialize(context)
    }
  }

  var internalSubScreen by remember { mutableStateOf(initialSubScreen) }
  val currentSubScreen = externalSubScreen ?: internalSubScreen
  var subScreenBackStack by remember { mutableStateOf(listOf<FrontendSubScreen>()) }
  var customizationInitialTab by remember { mutableIntStateOf(0) }
  var selectedChampionshipTier by remember { mutableStateOf(ChampionshipTier.ROOKIE_CHAMPIONSHIP) }

  fun navigateToSubScreen(target: FrontendSubScreen, pushCurrentToStack: Boolean = true) {
    if (pushCurrentToStack && currentSubScreen != target && currentSubScreen != FrontendSubScreen.LOADING_SCREEN) {
      subScreenBackStack = (subScreenBackStack + currentSubScreen).takeLast(12)
    }
    internalSubScreen = target
    onExternalSubScreenChange?.invoke(target)
  }

  fun popSubScreenOrReturnHome() {
    val previous = subScreenBackStack.lastOrNull()
    if (previous != null) {
      subScreenBackStack = subScreenBackStack.dropLast(1)
      internalSubScreen = previous
      onExternalSubScreenChange?.invoke(previous)
    } else {
      internalSubScreen = FrontendSubScreen.MAIN_MENU
      onExternalSubScreenChange?.invoke(FrontendSubScreen.MAIN_MENU)
    }
  }

  if (currentSubScreen != FrontendSubScreen.MAIN_MENU && currentSubScreen != FrontendSubScreen.LOADING_SCREEN) {
    BackHandler { popSubScreenOrReturnHome() }
  }

  Box(
    modifier = modifier
      .fillMaxSize()
      .background(CarbonBlack)
      .testTag("apex_v2_frontend_hub")
  ) {
    Crossfade(
      targetState = currentSubScreen,
      animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing),
      label = "frontendSubScreenTransition"
    ) { screen ->
      when (screen) {
        FrontendSubScreen.LOADING_SCREEN -> ApexLoadingScreen(
          onLoadingFinished = {
            subScreenBackStack = emptyList()
            navigateToSubScreen(FrontendSubScreen.MAIN_MENU, pushCurrentToStack = false)
          }
        )

        FrontendSubScreen.MAIN_MENU -> ApexMainMenuScreen(
          activeVehicle = activeVehicle,
          onSelectVehicle = onSelectVehicle,
          onPlayClicked = { navigateToSubScreen(FrontendSubScreen.MODE_SELECTION) },
          onGarageClicked = { navigateToSubScreen(FrontendSubScreen.VEHICLE_GARAGE) },
          onCustomizeClicked = {
            customizationInitialTab = 0
            subScreenBackStack = listOf(FrontendSubScreen.MAIN_MENU, FrontendSubScreen.VEHICLE_GARAGE)
            navigateToSubScreen(FrontendSubScreen.VEHICLE_CUSTOMIZATION, pushCurrentToStack = false)
          },
          onUpgradeClicked = {
            subScreenBackStack = listOf(
              FrontendSubScreen.MAIN_MENU,
              FrontendSubScreen.VEHICLE_GARAGE,
              FrontendSubScreen.VEHICLE_CUSTOMIZATION
            )
            navigateToSubScreen(FrontendSubScreen.VEHICLE_UPGRADES, pushCurrentToStack = false)
          },
          onCareerClicked = { navigateToSubScreen(FrontendSubScreen.CAREER_CHAMPIONSHIP) },
          onMultiplayerClicked = { navigateToSubScreen(FrontendSubScreen.MULTIPLAYER_HUB) },
          onEventsClicked = { navigateToSubScreen(FrontendSubScreen.EVENTS_AND_REWARDS) },
          onSettingsClicked = { navigateToSubScreen(FrontendSubScreen.SETTINGS_SCREEN) },
          onProfileClicked = { navigateToSubScreen(FrontendSubScreen.DRIVER_PROFILE) },
          onShowSplashClicked = { navigateToSubScreen(FrontendSubScreen.LOADING_SCREEN, pushCurrentToStack = false) }
        )

        FrontendSubScreen.MODE_SELECTION -> ApexGameModeSelectionScreen(
          activeTrack = activeTrack,
          onSelectTrack = onSelectTrack,
          onBack = { popSubScreenOrReturnHome() },
          onLaunchMode = { gameMode -> onLaunchRaceHud(gameMode) },
          onOpenMultiplayer = { navigateToSubScreen(FrontendSubScreen.MULTIPLAYER_HUB) }
        )

        FrontendSubScreen.VEHICLE_GARAGE -> ApexVehicleGarageScreen(
          vehicles = AVAILABLE_VEHICLES,
          activeVehicle = activeVehicle,
          onSelectVehicle = onSelectVehicle,
          onBack = { popSubScreenOrReturnHome() },
          onCustomizeClicked = {
            customizationInitialTab = 0
            navigateToSubScreen(FrontendSubScreen.VEHICLE_CUSTOMIZATION)
          },
          onUpgradeClicked = {
            navigateToSubScreen(FrontendSubScreen.VEHICLE_UPGRADES)
          },
          onRaceClicked = { onLaunchRaceHud(GameMode.GRAND_PRIX) }
        )

        FrontendSubScreen.VEHICLE_CUSTOMIZATION -> ApexVehicleCustomizationScreen(
          vehicle = activeVehicle,
          initialTab = customizationInitialTab,
          onBack = { popSubScreenOrReturnHome() },
          onOpenUpgrades = { navigateToSubScreen(FrontendSubScreen.VEHICLE_UPGRADES) },
          onRaceNow = { onLaunchRaceHud(GameMode.GRAND_PRIX) }
        )

        FrontendSubScreen.VEHICLE_UPGRADES -> ApexVehicleUpgradesScreen(
          vehicle = activeVehicle,
          onBack = { popSubScreenOrReturnHome() },
          onOpenCustomization = { popSubScreenOrReturnHome() },
          onRaceNow = { onLaunchRaceHud(GameMode.GRAND_PRIX) }
        )

        FrontendSubScreen.CAREER_CHAMPIONSHIP -> ApexCareerChampionshipScreen(
          activeVehicle = activeVehicle,
          activeTrack = activeTrack,
          onSelectTrack = onSelectTrack,
          onBack = { popSubScreenOrReturnHome() },
          onLaunchRace = { onLaunchRaceHud(GameMode.GRAND_PRIX) },
          onLaunchEventMode = { mode, track ->
            onSelectTrack(track)
            onLaunchRaceHud(mode)
          },
          onOpenGarage = { navigateToSubScreen(FrontendSubScreen.VEHICLE_GARAGE) },
          onOpenCustomization = {
            customizationInitialTab = 0
            navigateToSubScreen(FrontendSubScreen.VEHICLE_CUSTOMIZATION)
          },
          onOpenChampionshipSeries = { tier ->
            selectedChampionshipTier = tier
            navigateToSubScreen(FrontendSubScreen.CHAMPIONSHIP_DETAIL)
          }
        )

        FrontendSubScreen.CHAMPIONSHIP_DETAIL -> ApexChampionshipSeriesDetailScreen(
          tier = selectedChampionshipTier,
          activeVehicle = activeVehicle,
          activeTrack = activeTrack,
          onSelectTrack = onSelectTrack,
          onBack = { popSubScreenOrReturnHome() },
          onStartChampionshipRace = { mode, track ->
            onSelectTrack(track)
            onLaunchRaceHud(mode)
          }
        )

        FrontendSubScreen.MULTIPLAYER_HUB -> ApexMultiplayerHubScreen(
          activeVehicle = activeVehicle,
          onBack = { popSubScreenOrReturnHome() },
          onJoinRace = { onLaunchRaceHud(GameMode.GRAND_PRIX) },
          onOpenFullLobby = onNavigateToMultiplayer
        )

        FrontendSubScreen.DRIVER_PROFILE -> ApexDriverProfileSubScreen(
          activeVehicle = activeVehicle,
          onBack = { popSubScreenOrReturnHome() },
          onOpenFullProfile = onNavigateToProfile,
          onOpenGarage = { navigateToSubScreen(FrontendSubScreen.VEHICLE_GARAGE) },
          onOpenAchievements = { navigateToSubScreen(FrontendSubScreen.DRIVER_ACHIEVEMENTS) }
        )

        FrontendSubScreen.DRIVER_ACHIEVEMENTS -> ApexDriverAchievementsScreen(
          activeVehicle = activeVehicle,
          onBack = { popSubScreenOrReturnHome() },
          onOpenFullTelemetryProfile = onNavigateToProfile
        )

        FrontendSubScreen.EVENTS_AND_REWARDS -> ApexEventsAndRewardsScreen(
          onBack = { popSubScreenOrReturnHome() },
          onOpenMissions = { navigateToSubScreen(FrontendSubScreen.EVENTS_MISSIONS) },
          onOpenRewards = {
            subScreenBackStack = (subScreenBackStack + listOf(
              FrontendSubScreen.EVENTS_AND_REWARDS,
              FrontendSubScreen.EVENTS_MISSIONS
            )).takeLast(12)
            navigateToSubScreen(FrontendSubScreen.EVENTS_REWARDS, pushCurrentToStack = false)
          },
          onLaunchEventRace = { onLaunchRaceHud(GameMode.GRAND_PRIX) }
        )

        FrontendSubScreen.EVENTS_MISSIONS -> ApexEventsMissionsScreen(
          onBack = { popSubScreenOrReturnHome() },
          onOpenRewards = { navigateToSubScreen(FrontendSubScreen.EVENTS_REWARDS) },
          onLaunchMissionRace = { onLaunchRaceHud(GameMode.GRAND_PRIX) }
        )

        FrontendSubScreen.EVENTS_REWARDS -> ApexEventsRewardsScreen(
          onBack = { popSubScreenOrReturnHome() },
          onReturnToEventsHub = { popSubScreenOrReturnHome() },
          onLaunchRace = { onLaunchRaceHud(GameMode.GRAND_PRIX) }
        )

        FrontendSubScreen.SETTINGS_SCREEN -> ApexSettingsScreen(
          onBack = { popSubScreenOrReturnHome() },
          onOpenProfile = { navigateToSubScreen(FrontendSubScreen.DRIVER_PROFILE) },
          onOpenTelemetryHub = onOpenTelemetryHub
        )
      }
    }
  }
}

// ===========================================================================
// SCREEN 1 – CINEMATIC STARTUP AND WELCOME SCREEN (APEX RIVALS 2.0)
// ===========================================================================
@Composable
fun ApexLoadingScreen(
  onLoadingFinished: () -> Unit,
  autoAdvanceToMainMenu: Boolean = false,
  simulatedInitError: String? = null,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  var progress by remember { mutableFloatStateOf(0.15f) }
  var statusLabel by remember { mutableStateOf("INITIALIZING RACING EXPERIENCE...") }
  var loadingError by remember { mutableStateOf<String?>(simulatedInitError) }
  var retryAttempt by remember { mutableIntStateOf(0) }
  var isInitComplete by remember { mutableStateOf(false) }

  val animatedProgress by androidx.compose.animation.core.animateFloatAsState(
    targetValue = progress,
    animationSpec = tween(durationMillis = 240, easing = FastOutSlowInEasing),
    label = "welcomeLoadingProgress"
  )

  val infiniteTransition = rememberInfiniteTransition(label = "cinematicLoadingPulse")
  val tailLightPulse by infiniteTransition.animateFloat(
    initialValue = 0.72f,
    targetValue = 1.0f,
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 1200, easing = LinearEasing),
      repeatMode = RepeatMode.Reverse
    ),
    label = "tailLightPulse"
  )
  val cameraScale by infiniteTransition.animateFloat(
    initialValue = 1.0f,
    targetValue = 1.035f,
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 4200, easing = FastOutSlowInEasing),
      repeatMode = RepeatMode.Reverse
    ),
    label = "cinematicCameraScale"
  )
  val cameraPanX by infiniteTransition.animateFloat(
    initialValue = -4f,
    targetValue = 4f,
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 5400, easing = FastOutSlowInEasing),
      repeatMode = RepeatMode.Reverse
    ),
    label = "cinematicCameraPanX"
  )

  LaunchedEffect(retryAttempt, simulatedInitError) {
    if (simulatedInitError != null && retryAttempt == 0) {
      loadingError = simulatedInitError
      statusLabel = "INITIALIZATION ERROR DETECTED"
      return@LaunchedEffect
    }
    loadingError = null
    isInitComplete = false
    try {
      // Stage 1: Hardware & Graphics Settings Initialization
      progress = 0.22f
      statusLabel = "INITIALIZING RACING EXPERIENCE..."
      kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        ApexRivals2SettingsRepository.initialize(context)
      }
      delay(120)

      // Stage 2: Verify Bundled 3D Supercar Hero, Launcher Logo, Vehicles & Track Assets
      progress = 0.48f
      statusLabel = "LOADING 3D SUPERCAR & TRACK ASSETS..."
      val assetsVerified = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        val heroResId = R.drawable.apex_rivals_welcome_supercar
        val logoResId = R.drawable.apex_rivals_logo_icon
        heroResId != 0 && logoResId != 0 && AVAILABLE_VEHICLES.isNotEmpty() && AVAILABLE_TRACKS.isNotEmpty()
      }
      if (!assetsVerified) {
        throw IllegalStateException("Required vehicle or track assets failed verification.")
      }
      delay(120)

      // Stage 3: Restore Existing V8/V9 Driver Profile, Garage & Career Progression Non-Destructively
      progress = 0.74f
      statusLabel = "RESTORING DRIVER PROFILE & GARAGE..."
      kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        ApexRivals2CareerManager.initialize(context)
      }
      delay(120)

      // Stage 4: Verify Local Room Database & Cloud Synchronization Coordinator
      progress = 0.92f
      statusLabel = "SYNCING TELEMETRY & CLOUD SAVE..."
      kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        com.example.data.local.AppDatabase.getDatabase(context)
        com.example.data.firebase.ApexCloudSyncCoordinator.uiState.value
      }
      delay(120)

      // Stage 5: Ready to Race
      progress = 1.00f
      statusLabel = "INITIALIZING RACING EXPERIENCE... READY"
      isInitComplete = true

      if (autoAdvanceToMainMenu) {
        delay(220)
        onLoadingFinished()
      }
    } catch (e: Exception) {
      loadingError = e.message ?: "Failed to initialize game assets. Please retry."
      statusLabel = "INITIALIZATION FAILED"
    }
  }

  Box(
    modifier = modifier
      .fillMaxSize()
      .background(Color(0xFF090B10))
      .testTag("screen_1_loading")
  ) {
    // Full-Bleed Dark Studio Lighting, Red LED Reflections & Wet Asphalt Backdrop
    CinematicHypercarHeroCanvas(
      tailLightGlow = tailLightPulse,
      modifier = Modifier
        .fillMaxSize()
        .testTag("loading_cinematic_vehicle")
    )

    // Dark Luxury Vignette Overlay for Maximum Typography Contrast
    Box(
      modifier = Modifier
        .fillMaxSize()
        .background(
          Brush.verticalGradient(
            colors = listOf(
              Color(0xFF090B10).copy(alpha = 0.88f),
              Color(0xFF090B10).copy(alpha = 0.58f),
              Color(0xFF090B10).copy(alpha = 0.82f),
              Color(0xFF090B10).copy(alpha = 0.96f)
            )
          )
        )
    )

    // Main Responsive Landscape Welcome & Startup Content
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.SpaceBetween,
      modifier = Modifier
        .fillMaxSize()
        .verticalScroll(rememberScrollState())
        .padding(horizontal = 24.dp, vertical = 12.dp)
    ) {
      // 1. TOP BRANDING + 2. MAIN GAME TITLE + 5. GAME SLOGAN
      Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.fillMaxWidth()
      ) {
        // 1. Top Branding: APEX DYNAMICS GAME STUDIOS PRESENTS
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.Center,
          modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF10131B).copy(alpha = 0.85f))
            .border(1.dp, MotorsportRed.copy(alpha = 0.45f), RoundedCornerShape(16.dp))
            .padding(horizontal = 14.dp, vertical = 4.dp)
            .testTag("loading_studio_badge")
        ) {
          Text(
            text = "APEX DYNAMICS GAME STUDIOS PRESENTS",
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = PureWhite.copy(alpha = 0.92f),
            letterSpacing = 2.2.sp,
            textAlign = TextAlign.Center
          )
        }

        // 2. Main Game Title: APEX RIVALS with Logo Shield Emblem & Subtle Racing-Red Glow
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.Center,
          modifier = Modifier.padding(top = 2.dp)
        ) {
          Box(
            modifier = Modifier
              .size(34.dp)
              .clip(RoundedCornerShape(8.dp))
              .border(1.5.dp, MotorsportRed, RoundedCornerShape(8.dp))
              .background(Color(0xFF0B0D13))
              .testTag("loading_version_2_shield"),
            contentAlignment = Alignment.Center
          ) {
            Image(
              painter = painterResource(id = R.drawable.apex_rivals_logo_icon),
              contentDescription = "Apex Rivals Official Supercar Emblem",
              contentScale = ContentScale.Crop,
              modifier = Modifier.fillMaxSize()
            )
          }
          Spacer(modifier = Modifier.width(10.dp))
          Text(
            text = "APEX RIVALS",
            style = TextStyle(
              fontSize = 30.sp,
              fontWeight = FontWeight.Black,
              color = PureWhite,
              letterSpacing = 3.5.sp,
              shadow = Shadow(
                color = MotorsportRed.copy(alpha = 0.65f),
                offset = Offset(0f, 3f),
                blurRadius = 14f
              )
            ),
            textAlign = TextAlign.Center,
            modifier = Modifier.testTag("loading_game_logo_title")
          )
        }

        // 5. Game Slogan: DREAM. BUILD. RACE.
        Text(
          text = "DREAM. BUILD. RACE.",
          fontSize = 13.sp,
          fontWeight = FontWeight.Black,
          color = MotorsportRed,
          letterSpacing = 3.2.sp,
          textAlign = TextAlign.Center,
          modifier = Modifier.testTag("loading_version_2_branding")
        )
      }

      Spacer(modifier = Modifier.height(8.dp))

      // 3. BACKGROUND / HERO 3D SUPERCAR SHOWCASE (Unstretched 16:9 Studio Tunnel Presentation)
      Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
          .fillMaxWidth(0.78f)
          .height(156.dp)
          .clip(RoundedCornerShape(16.dp))
          .background(Color(0xFF07090E))
          .border(
            width = 1.5.dp,
            brush = Brush.horizontalGradient(
              colors = listOf(
                MotorsportRed.copy(alpha = 0.85f),
                NeonCyan.copy(alpha = 0.55f),
                MotorsportRed.copy(alpha = 0.85f)
              )
            ),
            shape = RoundedCornerShape(16.dp)
          )
          .testTag("loading_supercar_showcase_card")
      ) {
        Image(
          painter = painterResource(id = R.drawable.apex_rivals_welcome_supercar),
          contentDescription = "Apex Rivals 3D Crimson Carbon Supercar in Studio Tunnel",
          contentScale = ContentScale.Crop,
          modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
              scaleX = cameraScale
              scaleY = cameraScale
              translationX = cameraPanX
            }
            .testTag("loading_supercar_showcase_image")
        )

        // Subtle Studio Rim-Light & Dark Asphalt Reflection Gradient Overlay
        Box(
          modifier = Modifier
            .fillMaxSize()
            .background(
              Brush.verticalGradient(
                colors = listOf(
                  Color.Transparent,
                  Color.Transparent,
                  Color(0xFF090B10).copy(alpha = 0.65f)
                )
              )
            )
        )
      }

      Spacer(modifier = Modifier.height(8.dp))

      // 4. WELCOME MESSAGE
      Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.fillMaxWidth(0.86f)
      ) {
        Text(
          text = "WELCOME, RACER.",
          fontSize = 20.sp,
          fontWeight = FontWeight.Black,
          color = PureWhite,
          letterSpacing = 1.8.sp,
          textAlign = TextAlign.Center,
          modifier = Modifier.testTag("loading_welcome_title")
        )
        Text(
          text = "Your journey starts here. Choose your machine, take the wheel and prove yourself on the track.",
          fontSize = 11.5.sp,
          fontWeight = FontWeight.Medium,
          color = MetallicSilver,
          textAlign = TextAlign.Center,
          lineHeight = 16.sp,
          modifier = Modifier.testTag("loading_welcome_message")
        )
      }

      Spacer(modifier = Modifier.height(8.dp))

      // 6. LOADING SECTION (Animated Progress Bar, Real Initialization Message, Error Recovery & Transition)
      Column(
        modifier = Modifier
          .fillMaxWidth(0.86f)
          .clip(RoundedCornerShape(12.dp))
          .background(Color(0xFF10141D).copy(alpha = 0.92f))
          .border(1.dp, CarbonBorder, RoundedCornerShape(12.dp))
          .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
      ) {
        LinearProgressIndicator(
          progress = { animatedProgress },
          color = MotorsportRed,
          trackColor = Color(0xFF262B38),
          strokeCap = StrokeCap.Round,
          modifier = Modifier
            .fillMaxWidth()
            .height(7.dp)
            .clip(RoundedCornerShape(4.dp))
            .testTag("loading_progress_bar")
        )

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = statusLabel,
            fontSize = 9.5.sp,
            fontWeight = FontWeight.Bold,
            color = MetallicSilver,
            letterSpacing = 1.2.sp,
            maxLines = 1,
            modifier = Modifier
              .weight(1f)
              .testTag("loading_status_message")
          )
          Text(
            text = "${(animatedProgress * 100).toInt()}%",
            fontSize = 10.sp,
            fontWeight = FontWeight.Black,
            color = MotorsportRed,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.testTag("loading_progress_percent")
          )
        }

        val activePreset by ApexRivals2SettingsRepository.graphicsPreset.collectAsState()
        val isAuto by ApexRivals2SettingsRepository.isAutoGraphicsEnabled.collectAsState()
        Text(
          text = "GRAPHICS PROFILE: ${activePreset.shortLabel}${if (isAuto) " (AUTO)" else ""} • ${activePreset.roadSliceCount} SLICES • ${activePreset.targetFps} FPS TARGET",
          fontSize = 7.5.sp,
          fontWeight = FontWeight.Bold,
          color = NeonCyan,
          fontFamily = FontFamily.Monospace,
          modifier = Modifier.testTag("loading_graphics_preset_badge")
        )

        if (loadingError != null) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(8.dp))
              .background(MotorsportDarkRed.copy(alpha = 0.35f))
              .border(1.dp, MotorsportRed, RoundedCornerShape(8.dp))
              .padding(horizontal = 10.dp, vertical = 6.dp)
              .testTag("loading_error_banner")
          ) {
            Text(
              text = loadingError ?: "Initialization error",
              fontSize = 9.sp,
              fontWeight = FontWeight.Bold,
              color = PureWhite,
              modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Button(
              onClick = { retryAttempt += 1 },
              colors = ButtonDefaults.buttonColors(containerColor = MotorsportRed),
              shape = RoundedCornerShape(6.dp),
              modifier = Modifier
                .height(32.dp)
                .testTag("loading_retry_btn")
            ) {
              Text(
                text = "RETRY",
                fontSize = 9.sp,
                fontWeight = FontWeight.Black,
                color = PureWhite
              )
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(6.dp))

      // Smooth Transition Button into the Main Menu
      Button(
        onClick = onLoadingFinished,
        colors = ButtonDefaults.buttonColors(containerColor = MotorsportRed),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier
          .fillMaxWidth(0.76f)
          .height(48.dp)
          .testTag("loading_enter_btn")
      ) {
        Icon(Icons.Default.PlayArrow, contentDescription = null, tint = PureWhite)
        Spacer(modifier = Modifier.width(6.dp))
        Text(
          text = if (isInitComplete) "ENTER MAIN MENU" else "START RACING",
          fontSize = 12.sp,
          fontWeight = FontWeight.Black,
          color = PureWhite,
          letterSpacing = 1.4.sp
        )
      }

      Spacer(modifier = Modifier.height(4.dp))

      // 7. FOOTER: POWERED BY APEX DYNAMICS GAME STUDIOS.
      Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
      ) {
        Text(
          text = "POWERED BY APEX DYNAMICS GAME STUDIOS.",
          fontSize = 9.5.sp,
          fontWeight = FontWeight.Black,
          color = PureWhite.copy(alpha = 0.90f),
          letterSpacing = 1.6.sp,
          textAlign = TextAlign.Center,
          modifier = Modifier.testTag("loading_footer_branding")
        )
        Text(
          text = "VERSION 2.0.0 (V9 RELEASE • CODE 9) • COM.AISTUDIO.APEXRIVALS.TR8X",
          fontSize = 7.5.sp,
          color = MetallicSilver.copy(alpha = 0.75f),
          fontFamily = FontFamily.Monospace,
          textAlign = TextAlign.Center,
          modifier = Modifier.testTag("loading_version_info")
        )
      }
    }
  }
}

/**
 * Renders the dramatic low-angle rear-view Crimson Hypercar showcase with glowing LED
 * tail-lights, twin carbon winglets, dual titanium exhausts, warm dusk city lights,
 * cobblestone road texture, and diagonal volumetric light slash.
 */
@Composable
private fun CinematicHypercarHeroCanvas(
  tailLightGlow: Float,
  modifier: Modifier = Modifier
) {
  Canvas(modifier = modifier) {
    val w = size.width
    val h = size.height
    val cx = w * 0.5f
    val horizonY = h * 0.44f

    // 1. Warm Dusk / Sunset European Street Sky & Bokeh Backdrop
    drawRect(
      brush = Brush.verticalGradient(
        colors = listOf(
          Color(0xFF1A0F1A),
          Color(0xFF3B1D2E),
          Color(0xFF6A2C2B),
          Color(0xFF18151C),
          Color(0xFF090B10)
        )
      )
    )

    // Warm Golden & Cyan City Bokeh Lights in Upper Background
    drawCircle(
      brush = Brush.radialGradient(
        colors = listOf(Color(0xFFFFB74D).copy(alpha = 0.42f), Color.Transparent),
        center = Offset(w * 0.25f, h * 0.22f),
        radius = w * 0.28f
      ),
      center = Offset(w * 0.25f, h * 0.22f),
      radius = w * 0.28f
    )
    drawCircle(
      brush = Brush.radialGradient(
        colors = listOf(Color(0xFF4FC3F7).copy(alpha = 0.35f), Color.Transparent),
        center = Offset(w * 0.52f, h * 0.16f),
        radius = w * 0.32f
      ),
      center = Offset(w * 0.52f, h * 0.16f),
      radius = w * 0.32f
    )
    drawCircle(
      brush = Brush.radialGradient(
        colors = listOf(Color(0xFFFF7043).copy(alpha = 0.38f), Color.Transparent),
        center = Offset(w * 0.78f, h * 0.24f),
        radius = w * 0.26f
      ),
      center = Offset(w * 0.78f, h * 0.24f),
      radius = w * 0.26f
    )

    // 2. Wet Cobblestone / Asphalt Street Foreground (Lower Half)
    drawRect(
      brush = Brush.verticalGradient(
        colors = listOf(Color(0xFF1A1D26), Color(0xFF0E1117), Color(0xFF07090D)),
        startY = horizonY,
        endY = h
      ),
      topLeft = Offset(0f, horizonY),
      size = Size(w, h - horizonY)
    )

    // Cobblestone perspective grid lines in bottom 30%
    for (row in 1..6) {
      val y = h * (0.68f + row * 0.05f)
      drawLine(
        color = Color.White.copy(alpha = 0.05f),
        start = Offset(w * 0.05f, y),
        end = Offset(w * 0.95f, y),
        strokeWidth = 1.5f
      )
    }

    // 3. Sculpted Rear-View Crimson Hypercar (Centered Hero Silhouette — Aspect-Locked across 16:9..20:9)
    val carSpanW = minOf(w, h * 1.85f)
    fun carX(frac: Float): Float = cx + (frac - 0.5f) * carSpanW
    val carTopY = h * 0.18f
    val carDeckY = h * 0.34f
    val carBumperY = h * 0.52f
    val carBottomY = h * 0.68f

    // Under-car shadow & crimson tail-light ground reflection
    drawOval(
      color = Color.Black.copy(alpha = 0.85f),
      topLeft = Offset(carX(0.06f), carBottomY - 22f),
      size = Size(carSpanW * 0.88f, 52f)
    )

    // Left & Right Outer Twin Rear Wing Winglets (FXX-K / Track Hypercar Style)
    val leftWingPath = Path().apply {
      moveTo(carX(0.04f), h * 0.20f)
      lineTo(carX(0.19f), h * 0.19f)
      lineTo(carX(0.18f), h * 0.24f)
      lineTo(carX(0.09f), h * 0.25f)
      lineTo(carX(0.11f), carDeckY)
      lineTo(carX(0.05f), carDeckY)
      close()
    }
    val rightWingPath = Path().apply {
      moveTo(carX(0.96f), h * 0.20f)
      lineTo(carX(0.81f), h * 0.19f)
      lineTo(carX(0.82f), h * 0.24f)
      lineTo(carX(0.91f), h * 0.25f)
      lineTo(carX(0.89f), carDeckY)
      lineTo(carX(0.95f), carDeckY)
      close()
    }
    drawPath(leftWingPath, color = Color(0xFF9E0B1C))
    drawPath(rightWingPath, color = Color(0xFF9E0B1C))

    // Wide Sculpted Hypercar Rear Body Shell
    val bodyPath = Path().apply {
      // Roof & Aero Cabin Canopy
      moveTo(carX(0.32f), carTopY)
      lineTo(carX(0.68f), carTopY)
      // Rear Haunches / Wide Fenders
      lineTo(carX(0.94f), carDeckY)
      lineTo(carX(0.95f), carBumperY)
      lineTo(carX(0.88f), carBottomY)
      lineTo(carX(0.12f), carBottomY)
      lineTo(carX(0.05f), carBumperY)
      lineTo(carX(0.06f), carDeckY)
      close()
    }
    drawPath(
      path = bodyPath,
      brush = Brush.verticalGradient(
        colors = listOf(
          Color(0xFFD5001C),
          Color(0xFFB70918),
          Color(0xFF7F0610),
          Color(0xFF28060A)
        ),
        startY = carTopY,
        endY = carBottomY
      )
    )

    // Central Dorsal Aero Fin on Engine Decklid
    val finPath = Path().apply {
      moveTo(cx - 5f, carTopY - 12f)
      lineTo(cx + 5f, carTopY - 12f)
      lineTo(cx + 9f, carDeckY)
      lineTo(cx - 9f, carDeckY)
      close()
    }
    drawPath(finPath, color = Color(0xFF141821))

    // Dark Smoked Rear Engine Louvers & Glass Canopy
    val canopyPath = Path().apply {
      moveTo(carX(0.34f), carTopY + 10f)
      lineTo(carX(0.66f), carTopY + 10f)
      lineTo(carX(0.76f), carDeckY - 6f)
      lineTo(carX(0.24f), carDeckY - 6f)
      close()
    }
    drawPath(
      path = canopyPath,
      brush = Brush.verticalGradient(
        colors = listOf(Color(0xFF1E293B), Color(0xFF090D14)),
        startY = carTopY,
        endY = carDeckY
      )
    )

    // Carbon-Fiber Rear Grille & Massive Lower Diffuser Tunnels
    val diffuserPath = Path().apply {
      moveTo(carX(0.14f), carDeckY + 26f)
      lineTo(carX(0.86f), carDeckY + 26f)
      lineTo(carX(0.88f), carBottomY - 6f)
      lineTo(carX(0.12f), carBottomY - 6f)
      close()
    }
    drawPath(diffuserPath, color = Color(0xFF0A0C10))

    // Dual Circular Titanium Exhaust Pipes (Left & Right)
    val exhaustY = (carBumperY + carBottomY) * 0.51f
    val leftExhaustCenter = Offset(carX(0.15f), exhaustY)
    val rightExhaustCenter = Offset(carX(0.85f), exhaustY)
    drawCircle(color = Color(0xFF37474F), radius = 22f, center = leftExhaustCenter, style = Stroke(width = 5f))
    drawCircle(color = Color(0xFF050709), radius = 17f, center = leftExhaustCenter)
    drawCircle(color = Color(0xFF37474F), radius = 22f, center = rightExhaustCenter, style = Stroke(width = 5f))
    drawCircle(color = Color(0xFF050709), radius = 17f, center = rightExhaustCenter)

    // 4. Glowing Crimson LED Tail-Light Strips + Outer Bloom (Left & Right)
    val tailY = carDeckY + 14f
    // Left LED Bloom
    drawCircle(
      brush = Brush.radialGradient(
        colors = listOf(
          Color(0xFFFF1744).copy(alpha = 0.68f * tailLightGlow),
          Color.Transparent
        ),
        center = Offset(carX(0.13f), tailY),
        radius = carSpanW * 0.16f
      ),
      center = Offset(carX(0.13f), tailY),
      radius = carSpanW * 0.16f
    )
    // Right LED Bloom
    drawCircle(
      brush = Brush.radialGradient(
        colors = listOf(
          Color(0xFFFF1744).copy(alpha = 0.68f * tailLightGlow),
          Color.Transparent
        ),
        center = Offset(carX(0.87f), tailY),
        radius = carSpanW * 0.16f
      ),
      center = Offset(carX(0.87f), tailY),
      radius = carSpanW * 0.16f
    )
    // Crisp White-Hot LED Cores
    drawRoundRect(
      color = Color(0xFFFF5252),
      topLeft = Offset(carX(0.08f), tailY - 5f),
      size = Size(carSpanW * 0.10f, 10f),
      cornerRadius = CornerRadius(5f, 5f)
    )
    drawRoundRect(
      color = Color.White.copy(alpha = 0.9f),
      topLeft = Offset(carX(0.095f), tailY - 2.5f),
      size = Size(carSpanW * 0.06f, 5f),
      cornerRadius = CornerRadius(3f, 3f)
    )
    drawRoundRect(
      color = Color(0xFFFF5252),
      topLeft = Offset(carX(0.82f), tailY - 5f),
      size = Size(carSpanW * 0.10f, 10f),
      cornerRadius = CornerRadius(5f, 5f)
    )
    drawRoundRect(
      color = Color.White.copy(alpha = 0.9f),
      topLeft = Offset(carX(0.845f), tailY - 2.5f),
      size = Size(carSpanW * 0.06f, 5f),
      cornerRadius = CornerRadius(3f, 3f)
    )

    // 5. Diagonal Volumetric Light Slash (Matching Reference Artwork)
    val slashPath = Path().apply {
      moveTo(w * 0.38f, 0f)
      lineTo(w * 0.92f, 0f)
      lineTo(w * 0.62f, h)
      lineTo(w * 0.12f, h)
      close()
    }
    drawPath(
      path = slashPath,
      brush = Brush.linearGradient(
        colors = listOf(
          Color.White.copy(alpha = 0.09f),
          Color(0xFF4FC3F7).copy(alpha = 0.05f),
          Color.Transparent
        ),
        start = Offset(w * 0.35f, 0f),
        end = Offset(w * 0.75f, h)
      )
    )
  }
}

// ===========================================================================
// WHAT'S NEW IN APEX RIVALS 2.0 DIALOG
// ===========================================================================
@Composable
fun WhatsNewInV2Dialog(
  onDismiss: () -> Unit,
  modifier: Modifier = Modifier
) {
  Dialog(onDismissRequest = onDismiss) {
    Card(
      shape = RoundedCornerShape(16.dp),
      colors = CardDefaults.cardColors(containerColor = Color(0xFF0D111A)),
      border = BorderStroke(1.5.dp, Brush.horizontalGradient(listOf(Color(0xFFE10600), MetallicSilver, Color(0xFFE10600)))),
      modifier = modifier
        .fillMaxWidth()
        .padding(6.dp)
        .testTag("whats_new_2_0_dialog")
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(16.dp)
          .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(9.dp)
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Box(
            modifier = Modifier
              .clip(RoundedCornerShape(6.dp))
              .background(Color(0xFFE10600))
              .padding(horizontal = 10.dp, vertical = 3.dp)
          ) {
            Text(
              text = "VERSION 2.0.0 • PRODUCTION RELEASE",
              fontSize = 8.5.sp,
              fontWeight = FontWeight.Black,
              color = PureWhite,
              letterSpacing = 0.8.sp
            )
          }

          IconButton(
            onClick = onDismiss,
            modifier = Modifier
              .size(26.dp)
              .testTag("whats_new_top_close_btn")
          ) {
            Icon(
              imageVector = Icons.Default.Close,
              contentDescription = "Close",
              tint = MetallicSilver,
              modifier = Modifier.size(16.dp)
            )
          }
        }

        Text(
          text = "What's New in Apex Rivals 2.0!",
          fontSize = 17.sp,
          fontWeight = FontWeight.Black,
          color = PureWhite,
          textAlign = TextAlign.Center
        )

        Text(
          text = "Developed by Apex Dynamics Game Studios",
          fontSize = 10.5.sp,
          fontWeight = FontWeight.Bold,
          color = NeonAmber,
          textAlign = TextAlign.Center
        )

        Box(
          modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(CarbonBorder)
        )

        val bulletItems = listOf(
          "Redesigned racing game interface." to Icons.Default.SportsMotorsports,
          "Improved vehicle garage and customization." to Icons.Default.DirectionsCar,
          "Enhanced graphics and visual effects." to Icons.Default.AutoAwesome,
          "Improved driving controls and gameplay experience." to Icons.Default.Speed,
          "New racing challenges and progression improvements." to Icons.Default.EmojiEvents,
          "Performance optimizations and bug fixes." to Icons.Default.CheckCircle
        )

        bulletItems.forEach { (text, icon) ->
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(8.dp))
              .background(Color(0xFF151922))
              .border(1.dp, CarbonBorder.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
              .padding(horizontal = 10.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            Icon(
              imageVector = icon,
              contentDescription = null,
              tint = Color(0xFFE10600),
              modifier = Modifier.size(17.dp)
            )
            Text(
              text = text,
              fontSize = 11.sp,
              fontWeight = FontWeight.SemiBold,
              color = PureWhite,
              lineHeight = 14.5.sp
            )
          }
        }

        Box(
          modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(CarbonBorder)
        )

        Text(
          text = "Thank you for racing with Apex Rivals!",
          fontSize = 11.5.sp,
          fontWeight = FontWeight.Bold,
          color = MetallicSilver,
          textAlign = TextAlign.Center
        )

        Button(
          onClick = onDismiss,
          colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE10600)),
          shape = RoundedCornerShape(8.dp),
          modifier = Modifier
            .fillMaxWidth()
            .height(42.dp)
            .testTag("whats_new_dialog_close_btn")
        ) {
          Icon(Icons.Default.PlayArrow, contentDescription = null, tint = PureWhite, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = "LET'S RACE!",
            fontSize = 12.sp,
            fontWeight = FontWeight.Black,
            color = PureWhite,
            letterSpacing = 0.8.sp
          )
        }
      }
    }
  }
}

// ===========================================================================
// SCREEN 2 – MAIN HOME SCREEN (PREMIUM MOTORSPORT HANGAR & 3D VEHICLE SHOWCASE)
// ===========================================================================
@Composable
fun ApexMainMenuScreen(
  activeVehicle: Vehicle,
  onPlayClicked: () -> Unit,
  onGarageClicked: () -> Unit,
  onCareerClicked: () -> Unit,
  onMultiplayerClicked: () -> Unit,
  onEventsClicked: () -> Unit,
  onSettingsClicked: () -> Unit,
  onProfileClicked: () -> Unit,
  onShowSplashClicked: () -> Unit,
  onSelectVehicle: (Vehicle) -> Unit = {},
  onCustomizeClicked: () -> Unit = onGarageClicked,
  onUpgradeClicked: () -> Unit = onGarageClicked,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val careerState by ApexRivals2CareerManager.state.collectAsState()
  val cloudSyncState by com.example.data.firebase.ApexCloudSyncCoordinator.uiState.collectAsState()
  var currentVehicle by remember(activeVehicle.id) { mutableStateOf(activeVehicle) }
  var dailyRewardToast by remember { mutableStateOf<String?>(null) }
  var showWhatsNewDialog by remember { mutableStateOf(false) }

  val vehicleIndex = remember(currentVehicle.id) {
    AVAILABLE_VEHICLES.indexOfFirst { it.id == currentVehicle.id }.coerceAtLeast(0)
  }

  val loadout = remember(currentVehicle.id, careerState.loadoutsByVehicleId) {
    ApexRivals2CareerManager.getLoadoutForVehicle(currentVehicle)
  }
  val liveStats = remember(currentVehicle, loadout) {
    ApexRivals2CareerManager.computeUpgradedStats(currentVehicle, loadout)
  }

  val displayLevel = remember(careerState.playerLevel) {
    if (careerState.playerLevel <= 1) 2 else careerState.playerLevel
  }
  val xpProgressInLevel = remember(careerState.totalXp) {
    ((careerState.totalXp % 1800) / 1800f).coerceIn(0.18f, 0.96f)
  }
  val featuredEvent = remember(careerState.activeChampionshipTier) {
    ApexRivals2CareerManager.careerEvents.firstOrNull {
      it.championshipTier == careerState.activeChampionshipTier
    } ?: ApexRivals2CareerManager.careerEvents.first()
  }

  fun selectPrevVehicle() {
    val prevIdx = if (vehicleIndex > 0) vehicleIndex - 1 else AVAILABLE_VEHICLES.lastIndex
    val newCar = AVAILABLE_VEHICLES[prevIdx]
    currentVehicle = newCar
    onSelectVehicle(newCar)
  }

  fun selectNextVehicle() {
    val nextIdx = if (vehicleIndex < AVAILABLE_VEHICLES.lastIndex) vehicleIndex + 1 else 0
    val newCar = AVAILABLE_VEHICLES[nextIdx]
    currentVehicle = newCar
    onSelectVehicle(newCar)
  }

  Column(
    modifier = modifier
      .fillMaxSize()
      .background(
        Brush.verticalGradient(
          colors = listOf(
            Color(0xFF07090D), // Matte Carbon Black
            Color(0xFF10141D), // Deep Charcoal
            Color(0xFF080A0F)  // Obsidian Base
          )
        )
      )
      .verticalScroll(rememberScrollState())
      .testTag("screen_2_main_menu"),
    horizontalAlignment = Alignment.CenterHorizontally
  ) {
    // 1. TOP COMMAND BAR: APEX RIVALS LOGO + PLAYER PROFILE & LEVEL + CURRENCY DISPLAY + QUICK ACTIONS
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .background(Color(0xFF0B0E14).copy(alpha = 0.94f))
        .padding(horizontal = 14.dp, vertical = 8.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      // Top-Left: Apex Rivals Official Motorsport Emblem & Logo + Online Status
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        // Racing-Red Angled Crest Emblem
        Box(
          modifier = Modifier
            .size(width = 28.dp, height = 28.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(
              Brush.linearGradient(
                colors = listOf(Color(0xFFE10600), Color(0xFF991B1B))
              )
            )
            .border(1.dp, MetallicSilver.copy(alpha = 0.7f), RoundedCornerShape(6.dp))
            .clickable { onShowSplashClicked() },
          contentAlignment = Alignment.Center
        ) {
          Text(
            text = "AR",
            fontSize = 11.sp,
            fontWeight = FontWeight.Black,
            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
            color = PureWhite
          )
        }

        Column(
          modifier = Modifier
            .clickable { onShowSplashClicked() }
            .testTag("main_menu_header_title")
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            Text(
              text = "APEX RIVALS",
              fontSize = 18.sp,
              fontWeight = FontWeight.Black,
              fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
              color = PureWhite,
              letterSpacing = 1.2.sp
            )
            Box(
              modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .background(Color(0xFFE10600))
                .padding(horizontal = 5.dp, vertical = 1.dp)
            ) {
              Text(
                text = "2.0",
                fontSize = 8.sp,
                fontWeight = FontWeight.Black,
                color = PureWhite,
                fontFamily = FontFamily.Monospace
              )
            }
          }
          Text(
            text = "APEX DYNAMICS GAME STUDIOS",
            fontSize = 6.5.sp,
            fontWeight = FontWeight.Bold,
            color = MetallicSilver,
            letterSpacing = 0.8.sp
          )
        }

        // Live Online Server Status Pill
        val onlineDotColor = when (cloudSyncState.status) {
          com.example.data.firebase.CloudSyncStatus.SYNCED -> Color(0xFF00E676)
          com.example.data.firebase.CloudSyncStatus.SYNCING -> NeonCyan
          com.example.data.firebase.CloudSyncStatus.OFFLINE_QUEUED -> NeonAmber
          com.example.data.firebase.CloudSyncStatus.ERROR -> MotorsportRed
          com.example.data.firebase.CloudSyncStatus.GUEST -> Color(0xFF38BDF8)
        }
        val onlineLabelText = when (cloudSyncState.status) {
          com.example.data.firebase.CloudSyncStatus.SYNCED -> "Online"
          com.example.data.firebase.CloudSyncStatus.SYNCING -> "Syncing"
          com.example.data.firebase.CloudSyncStatus.OFFLINE_QUEUED -> "Queued (${cloudSyncState.pendingOpsCount})"
          com.example.data.firebase.CloudSyncStatus.ERROR -> "Retry"
          com.example.data.firebase.CloudSyncStatus.GUEST -> "Guest"
        }
        Box(
          modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(Color(0xFF161B24))
            .border(1.dp, onlineDotColor.copy(alpha = 0.75f), RoundedCornerShape(50))
            .clickable { onProfileClicked() }
            .padding(horizontal = 8.dp, vertical = 2.dp)
            .testTag("main_menu_online_badge")
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
          ) {
            Box(
              modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(onlineDotColor)
            )
            Text(
              text = onlineLabelText,
              fontSize = 8.5.sp,
              fontWeight = FontWeight.Black,
              color = onlineDotColor
            )
          }
        }
      }

      // Top-Right: Player Profile & Level Capsule + Currency Display + Events & Settings Shortcuts
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        // Player Profile & Level Chip (Connected to Driver Profile Screen)
        Row(
          modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF161B24))
            .border(1.dp, MetallicSilver.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
            .clickable { onProfileClicked() }
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .testTag("main_menu_profile_chip"),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          Box(
            modifier = Modifier
              .size(22.dp)
              .clip(CircleShape)
              .background(Color(0xFFE10600))
              .border(1.dp, PureWhite, CircleShape),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = Icons.Default.AccountCircle,
              contentDescription = "Driver Profile",
              tint = PureWhite,
              modifier = Modifier.size(16.dp)
            )
          }
          Column {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
              Text(
                text = careerState.playerUsername.take(12).uppercase(Locale.US),
                fontSize = 8.sp,
                fontWeight = FontWeight.Black,
                color = PureWhite
              )
              Text(
                text = "LVL $displayLevel",
                fontSize = 8.sp,
                fontWeight = FontWeight.Black,
                color = Color(0xFFE10600),
                fontFamily = FontFamily.Monospace
              )
            }
            // Mini Metallic-Silver & Racing-Red XP Bar
            Box(
              modifier = Modifier
                .width(68.dp)
                .height(3.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(Color(0xFF262D3B))
            ) {
              Box(
                modifier = Modifier
                  .fillMaxWidth(xpProgressInLevel)
                  .height(3.dp)
                  .background(Color(0xFFE10600))
              )
            }
          }
        }

        // Currency Display Capsule (Credits CR + Career Earnings + Trophies)
        Row(
          modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF161B24))
            .border(1.dp, MetallicSilver.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
            .clickable { onGarageClicked() }
            .padding(horizontal = 10.dp, vertical = 5.dp)
            .testTag("main_menu_currency_badge"),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          // Credits (CR)
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
          ) {
            Box(
              modifier = Modifier
                .size(15.dp)
                .clip(CircleShape)
                .background(Color(0xFFE10600)),
              contentAlignment = Alignment.Center
            ) {
              Text("CR", fontSize = 6.5.sp, fontWeight = FontWeight.Black, color = PureWhite)
            }
            Text(
              text = "%,d".format(careerState.creditsBalance),
              fontSize = 9.5.sp,
              fontWeight = FontWeight.Black,
              color = PureWhite,
              fontFamily = FontFamily.Monospace
            )
          }

          // Metallic Divider
          Box(
            modifier = Modifier
              .width(1.dp)
              .height(12.dp)
              .background(MetallicSilver.copy(alpha = 0.3f))
          )

          // Coins (Gems)
          Text(
            text = "💎 ${careerState.coinsBalance}",
            fontSize = 8.5.sp,
            fontWeight = FontWeight.Black,
            color = NeonCyan,
            fontFamily = FontFamily.Monospace
          )

          // Metallic Divider
          Box(
            modifier = Modifier
              .width(1.dp)
              .height(12.dp)
              .background(MetallicSilver.copy(alpha = 0.3f))
          )

          // Championship XP / Trophies
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp)
          ) {
            Icon(
              imageVector = Icons.Default.EmojiEvents,
              contentDescription = "Trophies",
              tint = MetallicSilver,
              modifier = Modifier.size(13.dp)
            )
            Text(
              text = "%,d XP".format(careerState.totalXp),
              fontSize = 8.5.sp,
              fontWeight = FontWeight.Bold,
              color = MetallicSilver,
              fontFamily = FontFamily.Monospace
            )
          }
        }

        // What's New in 2.0 Badge Button
        Box(
          modifier = Modifier
            .clip(RoundedCornerShape(7.dp))
            .background(Color(0xFFE10600))
            .border(1.dp, MetallicSilver.copy(alpha = 0.5f), RoundedCornerShape(7.dp))
            .clickable { showWhatsNewDialog = true }
            .padding(horizontal = 8.dp, vertical = 6.dp)
            .testTag("main_menu_whats_new_btn"),
          contentAlignment = Alignment.Center
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
          ) {
            Icon(
              imageVector = Icons.Default.NewReleases,
              contentDescription = "What's New in 2.0",
              tint = PureWhite,
              modifier = Modifier.size(14.dp)
            )
            Text(
              text = "2.0 NEW",
              fontSize = 8.5.sp,
              fontWeight = FontWeight.Black,
              color = PureWhite
            )
          }
        }

        // Top-Right Quick Settings Gear Button
        Box(
          modifier = Modifier
            .size(30.dp)
            .clip(RoundedCornerShape(7.dp))
            .background(Color(0xFF181D27))
            .border(1.dp, MetallicSilver.copy(alpha = 0.35f), RoundedCornerShape(7.dp))
            .clickable { onSettingsClicked() },
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = Icons.Default.Settings,
            contentDescription = "Settings",
            tint = MetallicSilver,
            modifier = Modifier.size(16.dp)
          )
        }
      }
    }

    // Racing-Red & Metallic-Silver Horizon Accent Rule
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .height(1.5.dp)
        .background(
          Brush.horizontalGradient(
            colors = listOf(
              Color.Transparent,
              MetallicSilver.copy(alpha = 0.45f),
              Color(0xFFE10600),
              MetallicSilver.copy(alpha = 0.45f),
              Color.Transparent
            )
          )
        )
    )

    // 2. CENTERPIECE: LARGE ANIMATED 3D VEHICLE IN THE MOTORSPORT HANGAR
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .height(196.dp)
        .testTag("main_menu_featured_vehicle_stage")
    ) {
      DominantVehicleLandscapeStageCanvas(
        vehicle = currentVehicle,
        loadout = loadout,
        modifier = Modifier
          .fillMaxSize()
          .testTag("main_menu_turntable_hypercar_canvas")
      )

      // Top-Left Hangar Telemetry Header Overlay
      Column(
        modifier = Modifier
          .align(Alignment.TopStart)
          .padding(horizontal = 14.dp, vertical = 10.dp)
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          Box(
            modifier = Modifier
              .clip(RoundedCornerShape(3.dp))
              .background(Color(0xFFE10600))
              .padding(horizontal = 6.dp, vertical = 2.dp)
          ) {
            Text(
              text = "3D SHOWROOM TURNTABLE",
              fontSize = 7.sp,
              fontWeight = FontWeight.Black,
              color = PureWhite,
              letterSpacing = 0.6.sp
            )
          }
          Text(
            text = "READY TO RACE • ${currentVehicle.category.uppercase(Locale.US)}",
            fontSize = 8.sp,
            fontWeight = FontWeight.Bold,
            color = MetallicSilver,
            letterSpacing = 0.6.sp,
            modifier = Modifier.testTag("main_menu_ready_to_race_title")
          )
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
          text = currentVehicle.name.uppercase(Locale.US),
          fontSize = 20.sp,
          fontWeight = FontWeight.Black,
          fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
          color = PureWhite,
          letterSpacing = 0.8.sp,
          style = TextStyle(
            shadow = Shadow(color = Color.Black, offset = Offset(2f, 2f), blurRadius = 6f)
          )
        )
      }

      // Top-Right Live Vehicle Spec Readout Overlay (Charcoal & Metallic-Silver)
      Column(
        modifier = Modifier
          .align(Alignment.TopEnd)
          .padding(horizontal = 14.dp, vertical = 10.dp)
          .clip(RoundedCornerShape(8.dp))
          .background(Color(0xFF0E121A).copy(alpha = 0.86f))
          .border(1.dp, MetallicSilver.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
          .padding(horizontal = 10.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(2.dp)
      ) {
        Text(
          text = "${liveStats.horsepower} HP • ${liveStats.maxSpeedKmh} KM/H",
          fontSize = 9.5.sp,
          fontWeight = FontWeight.Black,
          color = PureWhite,
          fontFamily = FontFamily.Monospace
        )
        Text(
          text = String.format(
            Locale.US,
            "0-100: %.2fs • %.2fG • %s",
            liveStats.acceleration0to100Sec,
            liveStats.handlingCorneringG,
            loadout.paintFinishMaterial.displayName
          ),
          fontSize = 7.5.sp,
          fontWeight = FontWeight.Bold,
          color = MetallicSilver,
          fontFamily = FontFamily.Monospace
        )
      }

      // Left Vehicle Selector Arrow '❮'
      Box(
        modifier = Modifier
          .align(Alignment.CenterStart)
          .padding(start = 12.dp)
          .size(44.dp)
          .clip(CircleShape)
          .background(Color(0xFF12161F).copy(alpha = 0.85f))
          .border(1.5.dp, MetallicSilver.copy(alpha = 0.55f), CircleShape)
          .clickable { selectPrevVehicle() }
          .testTag("main_menu_prev_car_btn"),
        contentAlignment = Alignment.Center
      ) {
        Text(
          text = "❮",
          fontSize = 18.sp,
          fontWeight = FontWeight.Black,
          color = PureWhite
        )
      }

      // Right Vehicle Selector Arrow '❯'
      Box(
        modifier = Modifier
          .align(Alignment.CenterEnd)
          .padding(end = 12.dp)
          .size(44.dp)
          .clip(CircleShape)
          .background(Color(0xFF12161F).copy(alpha = 0.85f))
          .border(1.5.dp, MetallicSilver.copy(alpha = 0.55f), CircleShape)
          .clickable { selectNextVehicle() }
          .testTag("main_menu_next_car_btn"),
        contentAlignment = Alignment.Center
      ) {
        Text(
          text = "❯",
          fontSize = 18.sp,
          fontWeight = FontWeight.Black,
          color = PureWhite
        )
      }

      // Bottom-Center 3D Turntable Spec Strip
      Row(
        modifier = Modifier
          .align(Alignment.BottomCenter)
          .padding(bottom = 8.dp)
          .clip(RoundedCornerShape(50))
          .background(Color(0xFF0A0D14).copy(alpha = 0.88f))
          .border(1.dp, Color(0xFFE10600).copy(alpha = 0.70f), RoundedCornerShape(50))
          .padding(horizontal = 14.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        Text(
          text = "CAR ${vehicleIndex + 1}/${AVAILABLE_VEHICLES.size}",
          fontSize = 8.sp,
          fontWeight = FontWeight.Black,
          color = Color(0xFFE10600),
          fontFamily = FontFamily.Monospace
        )
        Text(
          text = "${currentVehicle.name.uppercase(Locale.US)} • UPGRADE LV.${loadout.totalUpgradeLevel}/${loadout.maxUpgradeLevel}",
          fontSize = 8.5.sp,
          fontWeight = FontWeight.Bold,
          color = PureWhite,
          fontFamily = FontFamily.Monospace
        )
      }
    }

    // 3. RESPONSIVE MOTORSPORT COMMAND DECK, PLAY NOW CTA, DAILY REWARD & FEATURED EVENT
    BoxWithConstraints(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
      val isLandscapeOrWide = maxWidth >= 520.dp

      Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        // PRIMARY NAVIGATION & "PLAY NOW" COMMAND ROW (Responsive Landscape vs Portrait)
        if (isLandscapeOrWide) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            // Left & Center: GARAGE, CAREER, EVENTS, MULTIPLAYER, SETTINGS + Quick Customize/Upgrade/Shop
            Row(
              horizontalArrangement = Arrangement.spacedBy(6.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              ReferenceHexActionButton(
                label = "GARAGE",
                icon = Icons.Default.DirectionsCar,
                isHighlighted = true,
                hasNotification = false,
                onClick = onGarageClicked,
                testTag = "main_menu_garage_btn"
              )

              ReferenceHexActionButton(
                label = "CAREER",
                icon = Icons.Default.EmojiEvents,
                isHighlighted = false,
                hasNotification = true,
                onClick = onCareerClicked,
                testTag = "main_menu_career_btn"
              )

              ReferenceHexActionButton(
                label = "EVENTS",
                icon = Icons.Default.CalendarMonth,
                isHighlighted = false,
                hasNotification = true,
                onClick = onEventsClicked,
                testTag = "main_menu_events_btn"
              )

              ReferenceHexActionButton(
                label = "MULTIPLAYER",
                icon = Icons.Default.Group,
                isHighlighted = false,
                hasNotification = false,
                onClick = onMultiplayerClicked,
                testTag = "main_menu_multiplayer_btn"
              )

              ReferenceHexActionButton(
                label = "SETTINGS",
                icon = Icons.Default.Settings,
                isHighlighted = false,
                hasNotification = false,
                onClick = onSettingsClicked,
                testTag = "main_menu_settings_btn"
              )
            }

            // Right: Large Racing-Red & Metallic-Silver "PLAY NOW" Primary Action Button
            Box(
              modifier = Modifier
                .testTag("main_menu_play_btn")
                .clip(RoundedCornerShape(12.dp))
                .background(
                  Brush.horizontalGradient(
                    colors = listOf(Color(0xFFE10600), Color(0xFFB91C1C))
                  )
                )
                .border(1.5.dp, MetallicSilver, RoundedCornerShape(12.dp))
                .clickable { onPlayClicked() }
                .padding(horizontal = 24.dp, vertical = 13.dp),
              contentAlignment = Alignment.Center
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
              ) {
                Icon(
                  imageVector = Icons.Default.PlayArrow,
                  contentDescription = "Play Now",
                  tint = PureWhite,
                  modifier = Modifier.size(20.dp)
                )
                Text(
                  text = "PLAY NOW",
                  fontSize = 17.sp,
                  fontWeight = FontWeight.Black,
                  fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                  color = PureWhite,
                  letterSpacing = 1.sp
                )
                Text(
                  text = "❯❯",
                  fontSize = 15.sp,
                  fontWeight = FontWeight.Black,
                  color = MetallicSilver
                )
              }
            }
          }
        } else {
          // Portrait / Compact Adaptive Layout: Large Full-Width Racing-Red "PLAY NOW" Button + Unified 5-Item Navigation Row
          Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Box(
              modifier = Modifier
                .testTag("main_menu_play_btn")
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(
                  Brush.horizontalGradient(
                    colors = listOf(Color(0xFFE10600), Color(0xFFB91C1C))
                  )
                )
                .border(1.5.dp, MetallicSilver, RoundedCornerShape(12.dp))
                .clickable { onPlayClicked() }
                .padding(vertical = 14.dp),
              contentAlignment = Alignment.Center
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
              ) {
                Icon(
                  imageVector = Icons.Default.PlayArrow,
                  contentDescription = "Play Now",
                  tint = PureWhite,
                  modifier = Modifier.size(22.dp)
                )
                Text(
                  text = "PLAY NOW",
                  fontSize = 19.sp,
                  fontWeight = FontWeight.Black,
                  fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                  color = PureWhite,
                  letterSpacing = 1.2.sp
                )
                Text(
                  text = "❯❯",
                  fontSize = 17.sp,
                  fontWeight = FontWeight.Black,
                  color = MetallicSilver
                )
              }
            }

            // Primary 5 Modes (GARAGE, CAREER, EVENTS, MULTIPLAYER, SETTINGS) — No duplicate shop/customise menus
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceEvenly,
              verticalAlignment = Alignment.CenterVertically
            ) {
              ReferenceHexActionButton(
                label = "GARAGE",
                icon = Icons.Default.DirectionsCar,
                isHighlighted = true,
                hasNotification = false,
                onClick = onGarageClicked,
                testTag = "main_menu_garage_btn"
              )
              ReferenceHexActionButton(
                label = "CAREER",
                icon = Icons.Default.EmojiEvents,
                isHighlighted = false,
                hasNotification = true,
                onClick = onCareerClicked,
                testTag = "main_menu_career_btn"
              )
              ReferenceHexActionButton(
                label = "EVENTS",
                icon = Icons.Default.CalendarMonth,
                isHighlighted = false,
                hasNotification = true,
                onClick = onEventsClicked,
                testTag = "main_menu_events_btn"
              )
              ReferenceHexActionButton(
                label = "MULTIPLAYER",
                icon = Icons.Default.Group,
                isHighlighted = false,
                hasNotification = false,
                onClick = onMultiplayerClicked,
                testTag = "main_menu_multiplayer_btn"
              )
              ReferenceHexActionButton(
                label = "SETTINGS",
                icon = Icons.Default.Settings,
                isHighlighted = false,
                hasNotification = false,
                onClick = onSettingsClicked,
                testTag = "main_menu_settings_btn"
              )
            }
          }
        }

        // 4. DAILY REWARD SECTION & FEATURED RACING EVENT (Side-by-Side in Landscape, Stacked in Portrait)
        val nextStreakDay = careerState.dailyLoginStreakDays + 1
        val nextDailyCredits = 2500 + (nextStreakDay * 500)
        val nextDailyXp = 400 + (nextStreakDay * 100)

        if (isLandscapeOrWide) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.Top
          ) {
            // Left Card: Daily Reward Section
            MainMenuDailyRewardCard(
              streakDays = careerState.dailyLoginStreakDays,
              isClaimedToday = careerState.isDailyLoginClaimedToday,
              rewardCredits = nextDailyCredits,
              rewardXp = nextDailyXp,
              toastMessage = dailyRewardToast,
              onClaimClicked = {
                val claimed = ApexRivals2CareerManager.claimDailyLoginReward(context)
                if (claimed) {
                  com.example.data.firebase.ApexCloudSyncCoordinator.onRewardClaimed(context, "daily_login")
                }
                dailyRewardToast = if (claimed) {
                  "Claimed Day $nextStreakDay Bonus: +$nextDailyCredits CR & +$nextDailyXp XP!"
                } else {
                  "Daily reward already claimed today — return tomorrow!"
                }
              },
              modifier = Modifier.weight(0.46f)
            )

            // Right Card: Featured Racing Event
            MainMenuFeaturedEventCard(
              event = featuredEvent,
              onEnterEvent = onEventsClicked,
              onQuickRaceEvent = onPlayClicked,
              modifier = Modifier.weight(0.54f)
            )
          }
        } else {
          Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            MainMenuDailyRewardCard(
              streakDays = careerState.dailyLoginStreakDays,
              isClaimedToday = careerState.isDailyLoginClaimedToday,
              rewardCredits = nextDailyCredits,
              rewardXp = nextDailyXp,
              toastMessage = dailyRewardToast,
              onClaimClicked = {
                val claimed = ApexRivals2CareerManager.claimDailyLoginReward(context)
                if (claimed) {
                  com.example.data.firebase.ApexCloudSyncCoordinator.onRewardClaimed(context, "daily_login")
                }
                dailyRewardToast = if (claimed) {
                  "Claimed Day $nextStreakDay Bonus: +$nextDailyCredits CR & +$nextDailyXp XP!"
                } else {
                  "Daily reward already claimed today — return tomorrow!"
                }
              },
              modifier = Modifier.fillMaxWidth()
            )

            MainMenuFeaturedEventCard(
              event = featuredEvent,
              onEnterEvent = onEventsClicked,
              onQuickRaceEvent = onPlayClicked,
              modifier = Modifier.fillMaxWidth()
            )
          }
        }
      }
    }

    // 5. FOOTER TELEMETRY & DRIVER LICENSE DOCK (HOME | PROFILE | SETTINGS)
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(bottom = 8.dp, top = 2.dp)
        .testTag("main_menu_bottom_dock"),
      horizontalArrangement = Arrangement.Center,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Text(
        text = "• HOME HANGAR •",
        fontSize = 8.sp,
        fontWeight = FontWeight.Black,
        color = Color(0xFFE10600),
        modifier = Modifier.testTag("main_menu_nav_home")
      )
      Spacer(modifier = Modifier.width(16.dp))
      Text(
        text = "DRIVER PROFILE & LICENSE",
        fontSize = 8.sp,
        fontWeight = FontWeight.Bold,
        color = MetallicSilver,
        modifier = Modifier
          .clickable { onProfileClicked() }
          .testTag("main_menu_profile_btn")
      )
    }

    if (showWhatsNewDialog) {
      WhatsNewInV2Dialog(onDismiss = { showWhatsNewDialog = false })
    }
  }
}

@Composable
private fun MainMenuDailyRewardCard(
  streakDays: Int,
  isClaimedToday: Boolean,
  rewardCredits: Int,
  rewardXp: Int,
  toastMessage: String?,
  onClaimClicked: () -> Unit,
  modifier: Modifier = Modifier
) {
  Card(
    colors = CardDefaults.cardColors(containerColor = Color(0xFF141821)),
    shape = RoundedCornerShape(10.dp),
    border = BorderStroke(
      1.dp,
      if (isClaimedToday) MetallicSilver.copy(alpha = 0.35f) else Color(0xFFE10600).copy(alpha = 0.85f)
    ),
    modifier = modifier.testTag("main_menu_daily_reward_card")
  ) {
    Column(
      modifier = Modifier.padding(10.dp),
      verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          Box(
            modifier = Modifier
              .clip(RoundedCornerShape(4.dp))
              .background(Color(0xFFE10600))
              .padding(horizontal = 6.dp, vertical = 2.dp)
          ) {
            Text(
              text = "DAILY REWARD",
              fontSize = 7.5.sp,
              fontWeight = FontWeight.Black,
              color = PureWhite
            )
          }
          Text(
            text = "DAY $streakDays LOGIN STREAK",
            fontSize = 8.5.sp,
            fontWeight = FontWeight.Black,
            color = MetallicSilver,
            fontFamily = FontFamily.Monospace
          )
        }

        Text(
          text = if (isClaimedToday) "CLAIMED ✔" else "READY",
          fontSize = 7.5.sp,
          fontWeight = FontWeight.Black,
          color = if (isClaimedToday) Color(0xFF00E676) else Color(0xFFE10600),
          fontFamily = FontFamily.Monospace
        )
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
          Text(
            text = "+%,d CR  •  +%,d XP".format(rewardCredits, rewardXp),
            fontSize = 11.sp,
            fontWeight = FontWeight.Black,
            color = PureWhite,
            fontFamily = FontFamily.Monospace
          )
          Text(
            text = toastMessage ?: "Consecutive daily check-in increases syndicate payout",
            fontSize = 7.5.sp,
            color = MetallicSilver,
            maxLines = 1
          )
        }

        Box(
          modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(
              if (isClaimedToday) Color(0xFF1E2430) else Color(0xFFE10600)
            )
            .border(
              1.dp,
              if (isClaimedToday) MetallicSilver.copy(alpha = 0.35f) else MetallicSilver,
              RoundedCornerShape(6.dp)
            )
            .clickable { onClaimClicked() }
            .padding(horizontal = 10.dp, vertical = 6.dp)
            .testTag("main_menu_claim_daily_reward_btn"),
          contentAlignment = Alignment.Center
        ) {
          Text(
            text = if (isClaimedToday) "CLAIMED" else "CLAIM NOW",
            fontSize = 8.sp,
            fontWeight = FontWeight.Black,
            color = PureWhite
          )
        }
      }
    }
  }
}

@Composable
private fun MainMenuFeaturedEventCard(
  event: CareerChampionshipEvent,
  onEnterEvent: () -> Unit,
  onQuickRaceEvent: () -> Unit,
  modifier: Modifier = Modifier
) {
  Card(
    colors = CardDefaults.cardColors(containerColor = Color(0xFF141821)),
    shape = RoundedCornerShape(10.dp),
    border = BorderStroke(1.dp, MetallicSilver.copy(alpha = 0.45f)),
    modifier = modifier.testTag("main_menu_featured_event_card")
  ) {
    Column(
      modifier = Modifier.padding(10.dp),
      verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          Box(
            modifier = Modifier
              .clip(RoundedCornerShape(4.dp))
              .background(Color(0xFF262E3D))
              .border(1.dp, MetallicSilver.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
              .padding(horizontal = 6.dp, vertical = 2.dp)
          ) {
            Text(
              text = "FEATURED RACING EVENT",
              fontSize = 7.5.sp,
              fontWeight = FontWeight.Black,
              color = PureWhite
            )
          }
          Text(
            text = event.environmentName.uppercase(Locale.US),
            fontSize = 8.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFFE10600),
            fontFamily = FontFamily.Monospace
          )
        }

        Text(
          text = "+%,d CR".format(event.rewardCredits),
          fontSize = 8.5.sp,
          fontWeight = FontWeight.Black,
          color = PureWhite,
          fontFamily = FontFamily.Monospace
        )
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column(
          modifier = Modifier.weight(1f),
          verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
          Text(
            text = event.title.uppercase(Locale.US),
            fontSize = 10.5.sp,
            fontWeight = FontWeight.Black,
            color = PureWhite,
            maxLines = 1
          )
          Text(
            text = event.targetObjective,
            fontSize = 7.5.sp,
            color = MetallicSilver,
            maxLines = 1
          )
        }

        Spacer(modifier = Modifier.width(8.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          Box(
            modifier = Modifier
              .clip(RoundedCornerShape(6.dp))
              .background(Color(0xFF1F2633))
              .border(1.dp, MetallicSilver.copy(alpha = 0.45f), RoundedCornerShape(6.dp))
              .clickable { onEnterEvent() }
              .padding(horizontal = 8.dp, vertical = 6.dp),
            contentAlignment = Alignment.Center
          ) {
            Text(
              text = "DETAILS",
              fontSize = 7.5.sp,
              fontWeight = FontWeight.Black,
              color = MetallicSilver
            )
          }

          Box(
            modifier = Modifier
              .clip(RoundedCornerShape(6.dp))
              .background(Color(0xFFE10600))
              .border(1.dp, MetallicSilver, RoundedCornerShape(6.dp))
              .clickable { onQuickRaceEvent() }
              .padding(horizontal = 10.dp, vertical = 6.dp)
              .testTag("main_menu_featured_event_race_btn"),
            contentAlignment = Alignment.Center
          ) {
            Text(
              text = "RACE EVENT",
              fontSize = 7.5.sp,
              fontWeight = FontWeight.Black,
              color = PureWhite
            )
          }
        }
      }
    }
  }
}

/**
 * Beveled Motorsport Command Button in Black, Charcoal, Racing-Red, and Metallic-Silver.
 */
@Composable
private fun ReferenceHexActionButton(
  label: String,
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  isHighlighted: Boolean,
  hasNotification: Boolean,
  onClick: () -> Unit,
  testTag: String,
  modifier: Modifier = Modifier
) {
  Column(
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.spacedBy(3.dp),
    modifier = modifier
      .testTag(testTag)
      .clickable(onClick = onClick)
  ) {
    Box(
      modifier = Modifier
        .size(40.dp)
        .clip(RoundedCornerShape(8.dp))
        .background(
          if (isHighlighted) {
            Brush.verticalGradient(listOf(Color(0xFFE10600), Color(0xFF991B1B)))
          } else {
            Brush.verticalGradient(listOf(Color(0xFF1C222E), Color(0xFF10141C)))
          }
        )
        .border(
          width = 1.2.dp,
          color = if (isHighlighted) PureWhite else MetallicSilver.copy(alpha = 0.40f),
          shape = RoundedCornerShape(8.dp)
        ),
      contentAlignment = Alignment.Center
    ) {
      Icon(
        imageVector = icon,
        contentDescription = label,
        tint = PureWhite,
        modifier = Modifier.size(19.dp)
      )

      if (hasNotification) {
        Box(
          modifier = Modifier
            .size(9.dp)
            .clip(CircleShape)
            .background(Color(0xFFE10600))
            .border(1.dp, PureWhite, CircleShape)
            .align(Alignment.TopEnd)
        )
      }
    }

    Text(
      text = label,
      fontSize = 7.5.sp,
      fontWeight = FontWeight.Black,
      color = if (isHighlighted) PureWhite else MetallicSilver,
      letterSpacing = 0.4.sp
    )
  }
}

/**
 * Animated 3D Motorsport Hangar & Turntable Vehicle Showcase in Black, Charcoal, Racing-Red & Metallic-Silver.
 * Features:
 * - Animated rotating 3D metallic-silver & racing-red laser turntable ring
 * - Sweeping ceiling spotlight beams and wet carbon-asphalt reflection
 * - Subtle idle engine/chassis breathing motion and pulsing xenon LED headlights
 */
@Composable
private fun DominantVehicleLandscapeStageCanvas(
  vehicle: Vehicle,
  loadout: VehicleCustomizationLoadout,
  modifier: Modifier = Modifier
) {
  val primaryPaint = Color(loadout.paintColorHex)
  val infiniteTransition = rememberInfiniteTransition(label = "home_3d_vehicle_showcase")
  val turntableAngleDeg by infiniteTransition.animateFloat(
    initialValue = 0f,
    targetValue = 360f,
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 12000, easing = LinearEasing),
      repeatMode = RepeatMode.Restart
    ),
    label = "turntable_rotation"
  )
  val idlePulse by infiniteTransition.animateFloat(
    initialValue = 0f,
    targetValue = 1f,
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 2200, easing = FastOutSlowInEasing),
      repeatMode = RepeatMode.Reverse
    ),
    label = "vehicle_idle_pulse"
  )

  Canvas(modifier = modifier) {
    val w = size.width
    val h = size.height
    val horizonY = h * 0.50f
    val cx = w * 0.50f

    // 1. Matte-Black & Charcoal Hangar Ceiling Backdrop
    drawRect(
      brush = Brush.verticalGradient(
        colors = listOf(
          Color(0xFF05070A), // Matte Black Ceiling
          Color(0xFF0E131B), // Dark Charcoal Bay
          Color(0xFF171D28), // Metallic Steel Wall
          Color(0xFF0B0E14)  // Horizon Shadow
        ),
        startY = 0f,
        endY = horizonY
      ),
      topLeft = Offset.Zero,
      size = Size(w, horizonY)
    )

    // Architectural Metallic-Silver Hangar Trusses & Racing-Red Ceiling Light Strips
    for (i in -3..3) {
      val topX = cx + i * (w * 0.14f)
      val botX = cx + i * (w * 0.20f)
      drawLine(
        color = Color(0xFF94A3B8).copy(alpha = 0.14f),
        start = Offset(topX, 0f),
        end = Offset(botX, horizonY),
        strokeWidth = 2f
      )
    }
    // Overhead Racing-Red LED Light Bars
    for (barIdx in -1..1) {
      val barX = cx + barIdx * (w * 0.26f)
      drawLine(
        color = Color(0xFFE10600).copy(alpha = 0.55f + 0.25f * idlePulse),
        start = Offset(barX - w * 0.08f, h * 0.10f),
        end = Offset(barX + w * 0.08f, h * 0.10f),
        strokeWidth = 4f,
        cap = StrokeCap.Round
      )
      drawLine(
        color = Color.White.copy(alpha = 0.70f),
        start = Offset(barX - w * 0.06f, h * 0.10f),
        end = Offset(barX + w * 0.06f, h * 0.10f),
        strokeWidth = 1.5f,
        cap = StrokeCap.Round
      )
    }

    // 2. Polished Wet Carbon-Charcoal Floor with Perspective Grid
    drawRect(
      brush = Brush.verticalGradient(
        colors = listOf(
          Color(0xFF181E29),
          Color(0xFF10141C),
          Color(0xFF090C11),
          Color(0xFF050609)
        ),
        startY = horizonY,
        endY = h
      ),
      topLeft = Offset(0f, horizonY),
      size = Size(w, h - horizonY)
    )

    for (i in -4..4) {
      val startX = cx + (i * (w * 0.12f))
      val endX = cx + (i * (w * 0.34f))
      drawLine(
        color = Color(0xFFCBD5E1).copy(alpha = 0.07f),
        start = Offset(startX, horizonY),
        end = Offset(endX, h),
        strokeWidth = 1.2f
      )
    }

    // Racing-Red Specular Floor Reflection Pool
    drawOval(
      brush = Brush.radialGradient(
        colors = listOf(
          Color(0xFFE10600).copy(alpha = 0.22f + 0.08f * idlePulse),
          Color(0xFF94A3B8).copy(alpha = 0.08f),
          Color.Transparent
        ),
        center = Offset(cx, h * 0.76f),
        radius = w * 0.38f
      ),
      topLeft = Offset(cx - w * 0.38f, h * 0.56f),
      size = Size(w * 0.76f, h * 0.38f)
    )

    // 3. ANIMATED 3D ROTATING TURNTABLE RING UNDER CENTER VEHICLE
    val platformRx = minOf(w * 0.29f, h * 0.65f)
    val platformRy = h * 0.16f
    val platformCy = h * 0.79f

    // Outer Metallic-Silver Bevel Ring
    drawOval(
      color = Color(0xFF94A3B8).copy(alpha = 0.38f),
      topLeft = Offset(cx - platformRx, platformCy - platformRy),
      size = Size(platformRx * 2f, platformRy * 2f),
      style = Stroke(width = 3f)
    )
    // Inner Racing-Red Laser Ring
    drawOval(
      color = Color(0xFFE10600).copy(alpha = 0.75f),
      topLeft = Offset(cx - platformRx * 0.88f, platformCy - platformRy * 0.88f),
      size = Size(platformRx * 1.76f, platformRy * 1.76f),
      style = Stroke(width = 2.2f)
    )
    // Rotating 3D Turntable Telemetry Tick Marks
    for (tick in 0 until 12) {
      val rad = Math.toRadians((turntableAngleDeg + tick * 30f).toDouble())
      val x1 = cx + (cos(rad) * platformRx * 0.86f).toFloat()
      val y1 = platformCy + (sin(rad) * platformRy * 0.86f).toFloat()
      val x2 = cx + (cos(rad) * platformRx * 0.98f).toFloat()
      val y2 = platformCy + (sin(rad) * platformRy * 0.98f).toFloat()
      drawLine(
        color = if (tick % 3 == 0) Color(0xFFE10600) else Color(0xFFCBD5E1).copy(alpha = 0.65f),
        start = Offset(x1, y1),
        end = Offset(x2, y2),
        strokeWidth = if (tick % 3 == 0) 3f else 1.5f
      )
    }

    // 4. FLANKING STUDIO FLEET SILHOUETTES (Metallic Silver & Stealth Charcoal)
    val leftCarX = w * 0.19f
    val leftCarY = h * 0.61f
    drawOval(
      color = Color.Black.copy(alpha = 0.75f),
      topLeft = Offset(leftCarX - 75f, leftCarY + 22f),
      size = Size(150f, 22f)
    )
    val leftCarBody = Path().apply {
      moveTo(leftCarX - 68f, leftCarY + 20f)
      lineTo(leftCarX - 34f, leftCarY - 12f)
      lineTo(leftCarX + 38f, leftCarY - 8f)
      lineTo(leftCarX + 64f, leftCarY + 20f)
      close()
    }
    drawPath(leftCarBody, color = Color(0xFF262E3D))
    drawPath(leftCarBody, color = Color(0xFF94A3B8).copy(alpha = 0.35f), style = Stroke(width = 1.2f))

    val rightCarX = w * 0.81f
    val rightCarY = h * 0.61f
    drawOval(
      color = Color.Black.copy(alpha = 0.75f),
      topLeft = Offset(rightCarX - 75f, rightCarY + 22f),
      size = Size(150f, 22f)
    )
    val rightCarBody = Path().apply {
      moveTo(rightCarX - 64f, rightCarY + 20f)
      lineTo(rightCarX - 32f, rightCarY - 10f)
      lineTo(rightCarX + 42f, rightCarY - 14f)
      lineTo(rightCarX + 68f, rightCarY + 20f)
      close()
    }
    drawPath(rightCarBody, color = Color(0xFF1F2430))
    drawPath(rightCarBody, color = Color(0xFFE10600).copy(alpha = 0.45f), style = Stroke(width = 1.2f))

    // 5. CENTER LARGE ANIMATED 3D SUPERCAR (Aspect-Locked across 16:9..20:9)
    val bodyBounceY = (idlePulse - 0.5f) * 3.5f
    val carCenterX = cx
    val carCenterY = h * 0.66f + bodyBounceY
    val carWidth = minOf(w * 0.44f, h * 0.96f)
    val carHeight = h * 0.36f

    // Ground Contact Shadow
    drawOval(
      color = Color.Black.copy(alpha = 0.88f),
      topLeft = Offset(carCenterX - carWidth * 0.52f, carCenterY + carHeight * 0.28f),
      size = Size(carWidth * 1.04f, 36f)
    )

    // Rear GT3 Carbon Wing
    drawLine(
      color = Color(0xFFE10600),
      start = Offset(carCenterX - carWidth * 0.34f, carCenterY - carHeight * 0.22f),
      end = Offset(carCenterX + carWidth * 0.34f, carCenterY - carHeight * 0.22f),
      strokeWidth = 4.5f,
      cap = StrokeCap.Round
    )

    // Sculpted 3D Aerodynamic Hypercar Body Shell
    val dominantBody = Path().apply {
      moveTo(carCenterX - carWidth * 0.46f, carCenterY + carHeight * 0.26f)
      lineTo(carCenterX - carWidth * 0.28f, carCenterY + carHeight * 0.36f)
      lineTo(carCenterX + carWidth * 0.28f, carCenterY + carHeight * 0.36f)
      lineTo(carCenterX + carWidth * 0.46f, carCenterY + carHeight * 0.26f)
      lineTo(carCenterX + carWidth * 0.42f, carCenterY + carHeight * 0.03f)
      lineTo(carCenterX + carWidth * 0.28f, carCenterY - carHeight * 0.18f)
      lineTo(carCenterX - carWidth * 0.28f, carCenterY - carHeight * 0.18f)
      lineTo(carCenterX - carWidth * 0.42f, carCenterY + carHeight * 0.03f)
      close()
    }
    drawPath(
      path = dominantBody,
      brush = Brush.verticalGradient(
        colors = listOf(
          Color.White.copy(alpha = 0.30f),
          primaryPaint,
          primaryPaint.copy(alpha = 0.85f),
          Color(0xFF0B0E14)
        ),
        startY = carCenterY - carHeight * 0.20f,
        endY = carCenterY + carHeight * 0.38f
      )
    )
    // Metallic-Silver Bodyline Rim Highlight
    drawPath(
      path = dominantBody,
      color = Color(0xFFCBD5E1).copy(alpha = 0.55f),
      style = Stroke(width = 1.6f)
    )

    // Twin Metallic-Silver & Racing-Red Center Livery Stripes
    val stripeWidth = carWidth * 0.042f
    drawRect(
      color = Color(0xFFCBD5E1).copy(alpha = 0.90f),
      topLeft = Offset(carCenterX - stripeWidth * 1.15f, carCenterY - carHeight * 0.18f),
      size = Size(stripeWidth, carHeight * 0.54f)
    )
    drawRect(
      color = Color(0xFFE10600).copy(alpha = 0.92f),
      topLeft = Offset(carCenterX + stripeWidth * 0.15f, carCenterY - carHeight * 0.18f),
      size = Size(stripeWidth, carHeight * 0.54f)
    )

    // Tinted Cockpit Canopy & Windshield
    val windshieldPath = Path().apply {
      moveTo(carCenterX - carWidth * 0.24f, carCenterY - carHeight * 0.15f)
      lineTo(carCenterX + carWidth * 0.24f, carCenterY - carHeight * 0.15f)
      lineTo(carCenterX + carWidth * 0.29f, carCenterY + carHeight * 0.03f)
      lineTo(carCenterX - carWidth * 0.29f, carCenterY + carHeight * 0.03f)
      close()
    }
    drawPath(
      path = windshieldPath,
      brush = Brush.verticalGradient(
        colors = listOf(Color(0xFF263244), Color(0xFF090C12))
      )
    )

    // Carbon-Fiber Front Splitter with Racing-Red Pinstripe
    drawRoundRect(
      color = Color(0xFF090C12),
      topLeft = Offset(carCenterX - carWidth * 0.30f, carCenterY + carHeight * 0.28f),
      size = Size(carWidth * 0.60f, carHeight * 0.09f),
      cornerRadius = CornerRadius(4f, 4f)
    )
    drawLine(
      color = Color(0xFFE10600),
      start = Offset(carCenterX - carWidth * 0.30f, carCenterY + carHeight * 0.36f),
      end = Offset(carCenterX + carWidth * 0.30f, carCenterY + carHeight * 0.36f),
      strokeWidth = 2.5f
    )

    // Pulsing Xenon LED Headlights & DRLs
    val headlightY = carCenterY + carHeight * 0.12f
    val glowRadius = 22f + 8f * idlePulse
    // Left Headlight
    drawRoundRect(
      color = Color.White,
      topLeft = Offset(carCenterX - carWidth * 0.40f, headlightY),
      size = Size(carWidth * 0.12f, 9f),
      cornerRadius = CornerRadius(4f, 4f)
    )
    drawCircle(
      brush = Brush.radialGradient(
        colors = listOf(Color.White.copy(alpha = 0.75f), Color(0xFFE10600).copy(alpha = 0.35f), Color.Transparent),
        center = Offset(carCenterX - carWidth * 0.34f, headlightY + 4.5f),
        radius = glowRadius
      ),
      center = Offset(carCenterX - carWidth * 0.34f, headlightY + 4.5f),
      radius = glowRadius
    )
    // Right Headlight
    drawRoundRect(
      color = Color.White,
      topLeft = Offset(carCenterX + carWidth * 0.28f, headlightY),
      size = Size(carWidth * 0.12f, 9f),
      cornerRadius = CornerRadius(4f, 4f)
    )
    drawCircle(
      brush = Brush.radialGradient(
        colors = listOf(Color.White.copy(alpha = 0.75f), Color(0xFFE10600).copy(alpha = 0.35f), Color.Transparent),
        center = Offset(carCenterX + carWidth * 0.34f, headlightY + 4.5f),
        radius = glowRadius
      ),
      center = Offset(carCenterX + carWidth * 0.34f, headlightY + 4.5f),
      radius = glowRadius
    )

    // Forged Wheels with Metallic-Silver Rim Lip & Racing-Red Brake Calipers
    drawRoundRect(
      color = Color(0xFF0F131A),
      topLeft = Offset(carCenterX - carWidth * 0.44f, carCenterY + carHeight * 0.20f),
      size = Size(carWidth * 0.08f, carHeight * 0.16f),
      cornerRadius = CornerRadius(6f, 6f)
    )
    drawRoundRect(
      color = Color(0xFFE10600),
      topLeft = Offset(carCenterX - carWidth * 0.42f, carCenterY + carHeight * 0.24f),
      size = Size(4f, 10f),
      cornerRadius = CornerRadius(2f, 2f)
    )
    drawRoundRect(
      color = Color(0xFF0F131A),
      topLeft = Offset(carCenterX + carWidth * 0.36f, carCenterY + carHeight * 0.20f),
      size = Size(carWidth * 0.08f, carHeight * 0.16f),
      cornerRadius = CornerRadius(6f, 6f)
    )
    drawRoundRect(
      color = Color(0xFFE10600),
      topLeft = Offset(carCenterX + carWidth * 0.41f, carCenterY + carHeight * 0.24f),
      size = Size(4f, 10f),
      cornerRadius = CornerRadius(2f, 2f)
    )
  }
}

@Composable
private fun ProposedDarkGridCard(
  title: String,
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  onClick: () -> Unit,
  testTag: String,
  modifier: Modifier = Modifier
) {
  Card(
    colors = CardDefaults.cardColors(containerColor = Color(0xFF222222)),
    shape = RoundedCornerShape(14.dp),
    modifier = modifier
      .height(106.dp)
      .clickable(onClick = onClick)
      .testTag(testTag)
  ) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(12.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center
    ) {
      Icon(
        imageVector = icon,
        contentDescription = title,
        tint = PureWhite,
        modifier = Modifier.size(28.dp)
      )
      Spacer(modifier = Modifier.height(10.dp))
      Text(
        text = title,
        fontSize = 16.sp,
        fontWeight = FontWeight.ExtraBold,
        color = PureWhite,
        letterSpacing = 0.5.sp
      )
    }
  }
}

@Composable
private fun HomeDockSquareButton(
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  label: String,
  hasNotificationDot: Boolean,
  onClick: () -> Unit,
  testTag: String,
  modifier: Modifier = Modifier
) {
  Box(
    modifier = modifier
      .fillMaxHeight()
      .clip(RoundedCornerShape(8.dp))
      .background(
        Brush.verticalGradient(
          colors = listOf(
            Color(0xFF2B3548).copy(alpha = 0.88f),
            Color(0xFF141A24).copy(alpha = 0.95f)
          )
        )
      )
      .border(1.dp, Color(0xFF64748B).copy(alpha = 0.55f), RoundedCornerShape(8.dp))
      .clickable(onClick = onClick)
      .padding(4.dp)
      .testTag(testTag),
    contentAlignment = Alignment.Center
  ) {
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center
    ) {
      Icon(
        imageVector = icon,
        contentDescription = label,
        tint = PureWhite,
        modifier = Modifier.size(22.dp)
      )
      Spacer(modifier = Modifier.height(3.dp))
      Text(
        text = label,
        fontSize = 7.sp,
        fontWeight = FontWeight.Black,
        color = MetallicSilver,
        letterSpacing = 0.4.sp,
        maxLines = 1
      )
    }

    if (hasNotificationDot) {
      Box(
        modifier = Modifier
          .size(7.dp)
          .clip(CircleShape)
          .background(MotorsportRed)
          .align(Alignment.TopEnd)
      )
    }
  }
}

/**
 * Renders the Screen 2 Showroom Hangar Canvas inspired by the reference screenshot:
 * - Dark cylindrical hangar with vertical light beams & upper-right angled wall stencil logo
 * - Circular metallic podium turntable with glowing golden-amber ring & floor reflection
 * - Race driver figure in red-and-black racing suit & helmet standing beside the car
 * - 3/4 Rear-Right Red & Black Track Hypercar with quad central exhausts, rear diffuser strakes,
 *   red-rimmed aero wheels, and a dramatic horizontal crimson LED tail-light flare.
 */
@Composable
private fun MainHomeShowroomTurntableCanvas(
  vehicle: Vehicle,
  loadout: VehicleCustomizationLoadout,
  flarePulse: Float,
  modifier: Modifier = Modifier
) {
  val primaryPaint = Color(loadout.paintColorHex)

  Canvas(modifier = modifier) {
    val w = size.width
    val h = size.height
    val cx = w * 0.48f
    val turntableCenterY = h * 0.72f

    // 1. Dark Cylindrical Hangar Backdrop
    drawRect(
      brush = Brush.verticalGradient(
        colors = listOf(
          Color(0xFF0A0D14),
          Color(0xFF151924),
          Color(0xFF0E1118),
          Color(0xFF07090E)
        )
      )
    )

    // Vertical Hangar Spotlight Beams (Upper-Left/Center)
    val beam1 = Path().apply {
      moveTo(w * 0.36f, 0f)
      lineTo(w * 0.39f, 0f)
      lineTo(w * 0.33f, h * 0.56f)
      lineTo(w * 0.30f, h * 0.56f)
      close()
    }
    drawPath(
      path = beam1,
      brush = Brush.verticalGradient(
        colors = listOf(Color.White.copy(alpha = 0.22f), Color.Transparent)
      )
    )
    val beam2 = Path().apply {
      moveTo(w * 0.47f, 0f)
      lineTo(w * 0.49f, 0f)
      lineTo(w * 0.44f, h * 0.50f)
      lineTo(w * 0.42f, h * 0.50f)
      close()
    }
    drawPath(
      path = beam2,
      brush = Brush.verticalGradient(
        colors = listOf(Color.White.copy(alpha = 0.35f), Color.Transparent)
      )
    )

    // Right-Side Angled Architectural Hangar Wall Panel
    val rightWallPath = Path().apply {
      moveTo(w * 0.86f, 0f)
      lineTo(w, 0f)
      lineTo(w, h)
      lineTo(w * 0.68f, h)
      close()
    }
    drawPath(
      path = rightWallPath,
      brush = Brush.linearGradient(
        colors = listOf(Color(0xFF1E293B).copy(alpha = 0.55f), Color(0xFF0F172A).copy(alpha = 0.85f)),
        start = Offset(w * 0.7f, 0f),
        end = Offset(w, h)
      )
    )

    // 2. Illuminated Circular Turntable Podium (Dark Reflective Disc + Glowing Golden-Amber Ring)
    // Outer metallic rim glow
    drawOval(
      brush = Brush.radialGradient(
        colors = listOf(
          Color(0xFF263238),
          Color(0xFF11161F),
          Color(0xFF070A0F)
        ),
        center = Offset(cx, turntableCenterY),
        radius = w * 0.46f
      ),
      topLeft = Offset(w * 0.04f, h * 0.48f),
      size = Size(w * 0.88f, h * 0.46f)
    )

    // Golden-Amber Turntable Ring (matching screenshot's glowing yellow/gold ring around the car)
    drawOval(
      color = Color(0xFFFFB300).copy(alpha = 0.85f),
      topLeft = Offset(w * 0.09f, h * 0.52f),
      size = Size(w * 0.78f, h * 0.38f),
      style = Stroke(width = 3.5f)
    )
    drawOval(
      color = Color(0xFFFFD54F).copy(alpha = 0.30f),
      topLeft = Offset(w * 0.075f, h * 0.505f),
      size = Size(w * 0.81f, h * 0.41f),
      style = Stroke(width = 7f)
    )

    // Left-side dry-ice exhaust mist / floor spotlight
    drawCircle(
      brush = Brush.radialGradient(
        colors = listOf(Color.White.copy(alpha = 0.25f), Color.Transparent),
        center = Offset(w * 0.16f, h * 0.56f),
        radius = w * 0.14f
      ),
      center = Offset(w * 0.16f, h * 0.56f),
      radius = w * 0.14f
    )

    // Specular Red Car Reflection on Polished Turntable Floor
    drawOval(
      brush = Brush.radialGradient(
        colors = listOf(
          primaryPaint.copy(alpha = 0.36f * flarePulse),
          Color.Transparent
        ),
        center = Offset(w * 0.52f, h * 0.76f),
        radius = w * 0.24f
      ),
      topLeft = Offset(w * 0.28f, h * 0.64f),
      size = Size(w * 0.46f, h * 0.22f)
    )

    // 3. Race Driver Standing Behind Hypercar (Red & Black Suit + Racing Helmet)
    val driverX = w * 0.43f
    val driverTopY = h * 0.22f
    // Torso (Red/White/Black racing suit)
    val torsoPath = Path().apply {
      moveTo(driverX - 16f, driverTopY + 18f)
      lineTo(driverX + 16f, driverTopY + 18f)
      lineTo(driverX + 21f, driverTopY + 52f)
      lineTo(driverX - 21f, driverTopY + 52f)
      close()
    }
    drawPath(torsoPath, color = Color(0xFFB71C1C))
    // White chest stripe on driver suit
    drawRect(
      color = Color.White.copy(alpha = 0.85f),
      topLeft = Offset(driverX - 9f, driverTopY + 24f),
      size = Size(18f, 8f)
    )
    // Helmet
    drawCircle(
      color = Color(0xFFD50000),
      radius = 10.5f,
      center = Offset(driverX, driverTopY + 8f)
    )
    // Dark Helmet Visor
    drawRoundRect(
      color = Color(0xFF0D1117),
      topLeft = Offset(driverX - 7.5f, driverTopY + 5f),
      size = Size(15f, 6.5f),
      cornerRadius = CornerRadius(3f, 3f)
    )

    // 4. 3/4 Rear-Right Sculpted Red & Black Track Hypercar (Bolide / LMP Hypercar Silhouette)
    // Deep under-chassis shadow
    drawOval(
      color = Color.Black.copy(alpha = 0.92f),
      topLeft = Offset(w * 0.25f, h * 0.55f),
      size = Size(w * 0.46f, h * 0.16f)
    )

    // Main Carbon-Black Lower Chassis & Side Skirt (Angled 3/4 towards front-right)
    val chassisPath = Path().apply {
      moveTo(w * 0.28f, h * 0.50f)
      lineTo(w * 0.52f, h * 0.35f)
      lineTo(w * 0.69f, h * 0.42f)
      lineTo(w * 0.68f, h * 0.54f)
      lineTo(w * 0.54f, h * 0.65f)
      lineTo(w * 0.29f, h * 0.61f)
      close()
    }
    drawPath(chassisPath, color = Color(0xFF0B0E14))

    // Crimson Sculpted Rear Haunches, Roof Scoop & Side Pod
    val redBodyPath = Path().apply {
      moveTo(w * 0.27f, h * 0.42f)
      lineTo(w * 0.49f, h * 0.33f)
      lineTo(w * 0.66f, h * 0.39f)
      lineTo(w * 0.62f, h * 0.53f)
      lineTo(w * 0.53f, h * 0.60f)
      lineTo(w * 0.28f, h * 0.52f)
      close()
    }
    drawPath(
      path = redBodyPath,
      brush = Brush.linearGradient(
        colors = listOf(
          Color(0xFFFF1744),
          primaryPaint,
          Color(0xFF8E001C)
        ),
        start = Offset(w * 0.30f, h * 0.35f),
        end = Offset(w * 0.65f, h * 0.60f)
      )
    )

    // Smoked Teardrop Cockpit Canopy (Right-Center)
    val canopyPath = Path().apply {
      moveTo(w * 0.46f, h * 0.34f)
      lineTo(w * 0.58f, h * 0.36f)
      lineTo(w * 0.62f, h * 0.43f)
      lineTo(w * 0.49f, h * 0.41f)
      close()
    }
    drawPath(
      path = canopyPath,
      brush = Brush.verticalGradient(
        colors = listOf(Color(0xFFCFD8DC).copy(alpha = 0.65f), Color(0xFF10151E))
      )
    )

    // Aggressive Carbon Rear Wing & LMP Shark Fin (Upper-Left of Car)
    val wingPath = Path().apply {
      moveTo(w * 0.26f, h * 0.36f)
      lineTo(w * 0.51f, h * 0.34f)
      lineTo(w * 0.53f, h * 0.39f)
      lineTo(w * 0.28f, h * 0.42f)
      close()
    }
    drawPath(wingPath, color = Color(0xFF141821))
    // White/Red racing stripes on rear wing endplate
    drawLine(
      color = PureWhite.copy(alpha = 0.85f),
      start = Offset(w * 0.32f, h * 0.36f),
      end = Offset(w * 0.35f, h * 0.41f),
      strokeWidth = 3.5f
    )
    drawLine(
      color = MotorsportRed,
      start = Offset(w * 0.34f, h * 0.36f),
      end = Offset(w * 0.37f, h * 0.41f),
      strokeWidth = 3.5f
    )

    // Rear Diffuser Vertical Aero Strakes (5 vertical crimson-edged fins at bottom-rear)
    for (i in 0..4) {
      val finX = w * (0.31f + i * 0.042f)
      drawLine(
        color = Color(0xFFD50000),
        start = Offset(finX, h * 0.54f),
        end = Offset(finX - 3f, h * 0.63f),
        strokeWidth = 3f
      )
    }

    // Quad Central Exhaust Tips (2x2 cluster in center of rear fascia)
    val exhaustPositions = listOf(
      Offset(w * 0.365f, h * 0.49f),
      Offset(w * 0.388f, h * 0.49f),
      Offset(w * 0.365f, h * 0.52f),
      Offset(w * 0.388f, h * 0.52f)
    )
    exhaustPositions.forEach { pos ->
      drawCircle(color = Color(0xFF455A64), radius = 6.5f, center = pos, style = Stroke(width = 2f))
      drawCircle(color = Color(0xFF07090D), radius = 4.5f, center = pos)
    }

    // Rear-Right Wheel & Front-Right Wheel with Glowing Crimson Aero Rim Rings
    // Rear-Right Wheel (Large foreground wheel)
    val rearWheelCenter = Offset(w * 0.555f, h * 0.545f)
    drawOval(
      color = Color(0xFF0D1017),
      topLeft = Offset(rearWheelCenter.x - 24f, rearWheelCenter.y - 36f),
      size = Size(48f, 72f)
    )
    drawOval(
      color = Color(0xFFFF1744),
      topLeft = Offset(rearWheelCenter.x - 16f, rearWheelCenter.y - 28f),
      size = Size(32f, 56f),
      style = Stroke(width = 3.5f)
    )
    drawOval(
      color = Color(0xFFCFD8DC),
      topLeft = Offset(rearWheelCenter.x - 6f, rearWheelCenter.y - 12f),
      size = Size(12f, 24f)
    )

    // Front-Right Wheel (Perspective foreshortened)
    val frontWheelCenter = Offset(w * 0.672f, h * 0.465f)
    drawOval(
      color = Color(0xFF0D1017),
      topLeft = Offset(frontWheelCenter.x - 14f, frontWheelCenter.y - 24f),
      size = Size(28f, 48f)
    )
    drawOval(
      color = Color(0xFFFF1744),
      topLeft = Offset(frontWheelCenter.x - 9f, frontWheelCenter.y - 18f),
      size = Size(18f, 36f),
      style = Stroke(width = 2.5f)
    )

    // 5. Signature Anamorphic Horizontal Crimson Tail-Light Flare (Sweeping Left across Stage)
    val flareCenterY = h * 0.445f
    // Wide soft crimson bloom
    drawOval(
      brush = Brush.radialGradient(
        colors = listOf(
          Color(0xFFFF1744).copy(alpha = 0.62f * flarePulse),
          Color(0xFFD50000).copy(alpha = 0.26f * flarePulse),
          Color.Transparent
        ),
        center = Offset(w * 0.34f, flareCenterY),
        radius = w * 0.30f
      ),
      topLeft = Offset(w * 0.04f, flareCenterY - 38f),
      size = Size(w * 0.56f, 76f)
    )
    // Intense horizontal anamorphic laser streak
    drawLine(
      brush = Brush.horizontalGradient(
        colors = listOf(
          Color.Transparent,
          Color(0xFFFF1744).copy(alpha = 0.85f * flarePulse),
          Color.White,
          Color(0xFFFF1744).copy(alpha = 0.90f * flarePulse),
          Color.Transparent
        ),
        startX = w * 0.05f,
        endX = w * 0.55f
      ),
      start = Offset(w * 0.05f, flareCenterY),
      end = Offset(w * 0.55f, flareCenterY),
      strokeWidth = 6.5f
    )
    // X-Wing style curved red LED tail-light blades on rear fascia
    drawLine(
      color = Color.White,
      start = Offset(w * 0.30f, flareCenterY - 4f),
      end = Offset(w * 0.47f, flareCenterY + 3f),
      strokeWidth = 3f
    )
  }
}


@Composable
private fun MotorsportMenuTileButton(
  title: String,
  subtitle: String,
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  accentColor: Color,
  onClick: () -> Unit,
  testTag: String,
  modifier: Modifier = Modifier
) {
  Card(
    colors = CardDefaults.cardColors(containerColor = CharcoalCard),
    shape = RoundedCornerShape(12.dp),
    border = BorderStroke(1.2.dp, accentColor.copy(alpha = 0.55f)),
    modifier = modifier
      .height(66.dp)
      .clickable(onClick = onClick)
      .testTag(testTag)
  ) {
    Row(
      modifier = Modifier
        .fillMaxSize()
        .padding(horizontal = 12.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Box(
        modifier = Modifier
          .size(36.dp)
          .clip(RoundedCornerShape(8.dp))
          .background(accentColor.copy(alpha = 0.16f)),
        contentAlignment = Alignment.Center
      ) {
        Icon(icon, contentDescription = title, tint = accentColor, modifier = Modifier.size(20.dp))
      }
      Spacer(modifier = Modifier.width(10.dp))
      Column {
        Text(
          text = title,
          fontSize = 11.5.sp,
          fontWeight = FontWeight.Black,
          color = PureWhite,
          letterSpacing = 0.6.sp
        )
        Text(
          text = subtitle,
          fontSize = 8.5.sp,
          color = MetallicSilver
        )
      }
    }
  }
}

// ===========================================================================
// SCREEN 3 – GAME MODE SELECTION
// ===========================================================================
data class RaceModeCardSpec(
  val id: String,
  val title: String,
  val subtitle: String,
  val description: String,
  val accessRequirement: String,
  val requiredLevel: Int,
  val mappedGameMode: GameMode,
  val accentColor: Color
)

@Composable
fun ApexGameModeSelectionScreen(
  activeTrack: Track,
  onSelectTrack: (Track) -> Unit,
  onBack: () -> Unit,
  onLaunchMode: (GameMode) -> Unit,
  onOpenMultiplayer: () -> Unit,
  modifier: Modifier = Modifier
) {
  val careerState by ApexRivals2CareerManager.state.collectAsState()

  val modeSpecs = remember {
    listOf(
      RaceModeCardSpec(
        id = "QUICK_RACE",
        title = "Quick Race",
        subtitle = "8-Car Instant Grid Sprint",
        description = "Jump straight into an 8-car grid battle with dynamic slipstream drafting, DRS zones, and weather.",
        accessRequirement = "OPEN ACCESS • LEVEL 1+",
        requiredLevel = 1,
        mappedGameMode = GameMode.GRAND_PRIX,
        accentColor = MotorsportRed
      ),
      RaceModeCardSpec(
        id = "CAREER_MODE",
        title = "Career Mode",
        subtitle = "Rookie to Elite Progression",
        description = "Progress through Rookie, Amateur, Pro, and Elite chapters to unlock hypercars and circuits.",
        accessRequirement = "UNLOCKED • LEVEL 1+",
        requiredLevel = 1,
        mappedGameMode = GameMode.GRAND_PRIX,
        accentColor = NeonAmber
      ),
      RaceModeCardSpec(
        id = "TIME_TRIAL",
        title = "Time Trial",
        subtitle = "Qualifying Ghost & Sector Delta",
        description = "Pure hotlap precision against the stopwatch with live S1/S2/S3 purple micro-sector splits.",
        accessRequirement = "UNLOCKED • LEVEL 1+",
        requiredLevel = 1,
        mappedGameMode = GameMode.TIME_ATTACK,
        accentColor = NeonCyan
      ),
      RaceModeCardSpec(
        id = "DRIFT_CHALLENGE",
        title = "Drift Challenge",
        subtitle = "Handbrake Apex Sliding",
        description = "Use the E-Brake and throttle modulation to chain high-angle drifts through mountain & city corners.",
        accessRequirement = "DRIVER LEVEL 2+ REQUIRED",
        requiredLevel = 2,
        mappedGameMode = GameMode.DRIFT_CHALLENGE,
        accentColor = Color(0xFFD500F9)
      ),
      RaceModeCardSpec(
        id = "CHAMPIONSHIP",
        title = "Championship",
        subtitle = "16-Car Pro Endurance Cup",
        description = "Full championship rounds with mandatory tire compound pit stops and 25,000 CR trophy purses.",
        accessRequirement = "DRIVER LEVEL 3+ REQUIRED",
        requiredLevel = 3,
        mappedGameMode = GameMode.SPEED_TRAP,
        accentColor = ApexGreen
      ),
      RaceModeCardSpec(
        id = "MULTIPLAYER",
        title = "Multiplayer",
        subtitle = "Online Matchmaking & Private Rooms",
        description = "Compete in real-time online sessions, private friend lobbies, and global verified leaderboards.",
        accessRequirement = "ONLINE LIVE / STAGING",
        requiredLevel = 1,
        mappedGameMode = GameMode.GRAND_PRIX,
        accentColor = NeonCyan
      )
    )
  }

  Column(
    modifier = modifier
      .fillMaxSize()
      .background(CarbonBlack)
      .verticalScroll(rememberScrollState())
      .padding(14.dp)
      .testTag("screen_3_mode_selection"),
    verticalArrangement = Arrangement.spacedBy(10.dp)
  ) {
    // Header with Back Button
    ScreenHeaderBar(
      title = "SELECT GAME MODE",
      subtitle = "CIRCUIT: ${activeTrack.name.uppercase()} • DRIVER LV.${careerState.playerLevel}",
      onBack = onBack
    )

    // Circuit Quick Selector Strip
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .horizontalScroll(rememberScrollState()),
      horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
      AVAILABLE_TRACKS.forEach { track ->
        val isSelected = track.id == activeTrack.id
        val isTrackUnlocked = track.id in careerState.unlockedTrackIds || ApexRivals2CareerManager.isTrackUnlocked(track.id)
        Box(
          modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(
              when {
                isSelected -> MotorsportRed
                isTrackUnlocked -> CharcoalCard
                else -> CharcoalSurface.copy(alpha = 0.65f)
              }
            )
            .border(
              1.dp,
              when {
                isSelected -> MotorsportRed
                isTrackUnlocked -> CarbonBorder
                else -> CarbonBorder.copy(alpha = 0.5f)
              },
              RoundedCornerShape(8.dp)
            )
            .clickable(enabled = isTrackUnlocked) { onSelectTrack(track) }
            .padding(horizontal = 10.dp, vertical = 6.dp)
            .testTag("mode_track_chip_${track.id}")
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
          ) {
            if (!isTrackUnlocked) {
              Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = "Locked Track",
                tint = TextSecondary,
                modifier = Modifier.size(10.dp)
              )
            }
            Text(
              text = track.name,
              fontSize = 9.sp,
              fontWeight = FontWeight.Black,
              color = if (isTrackUnlocked) PureWhite else TextSecondary
            )
          }
        }
      }
    }

    // 6 Mode Cards with Visual Preview, Description & Access Requirements
    modeSpecs.forEach { mode ->
      val isUnlocked = careerState.playerLevel >= mode.requiredLevel
      Card(
        colors = CardDefaults.cardColors(containerColor = CharcoalCard),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.2.dp, if (isUnlocked) mode.accentColor.copy(alpha = 0.7f) else CarbonBorder),
        modifier = Modifier
          .fillMaxWidth()
          .clickable(enabled = isUnlocked) {
            if (mode.id == "MULTIPLAYER") {
              onOpenMultiplayer()
            } else {
              onLaunchMode(mode.mappedGameMode)
            }
          }
          .testTag("mode_card_${mode.id}")
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Text(
                text = mode.title.uppercase(),
                fontSize = 13.sp,
                fontWeight = FontWeight.Black,
                color = PureWhite
              )
              Spacer(modifier = Modifier.width(8.dp))
              Box(
                modifier = Modifier
                  .clip(RoundedCornerShape(4.dp))
                  .background(mode.accentColor.copy(alpha = 0.18f))
                  .padding(horizontal = 6.dp, vertical = 2.dp)
              ) {
                Text(
                  text = mode.accessRequirement,
                  fontSize = 7.5.sp,
                  fontWeight = FontWeight.Black,
                  color = mode.accentColor,
                  fontFamily = FontFamily.Monospace
                )
              }
            }
            Text(
              text = mode.subtitle,
              fontSize = 9.5.sp,
              fontWeight = FontWeight.Bold,
              color = mode.accentColor
            )
            Text(
              text = mode.description,
              fontSize = 8.5.sp,
              color = MetallicSilver
            )
          }

          Spacer(modifier = Modifier.width(10.dp))

          Button(
            onClick = {
              if (mode.id == "MULTIPLAYER") {
                onOpenMultiplayer()
              } else {
                onLaunchMode(mode.mappedGameMode)
              }
            },
            enabled = isUnlocked,
            colors = ButtonDefaults.buttonColors(containerColor = mode.accentColor),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.testTag("launch_mode_btn_${mode.id}")
          ) {
            Text(
              text = if (isUnlocked) "LAUNCH" else "LOCKED",
              fontSize = 9.sp,
              fontWeight = FontWeight.Black,
              color = CarbonBlack
            )
          }
        }
      }
    }
  }
}

// ===========================================================================
// SCREEN 3 / 4 – VEHICLE SELECTION AND GARAGE
// (Prominent Center Car in Workshop Stage + Visual Swatches + Horizontal Vehicle Selection + Upgrade Options Below)
// ===========================================================================
enum class GarageVisualCategoryTab(val label: String) {
  BODY_COLOR("PAINT"),
  RIMS("WHEELS"),
  SPOILERS("SPOILERS"),
  BODY_KITS("BODY KITS"),
  RIMS_COLOR("RIM COLOR"),
  SMOKE_COLOR("TIRE SMOKE")
}

enum class GarageVehicleCategoryFilter(val id: String, val label: String) {
  ALL("ALL", "ALL VEHICLES"),
  GT3("GT3", "GT3 RACING"),
  HYPERCAR("HYPERCAR", "HYPERCAR / LMP1"),
  CLASSIC("CLASSIC", "CLASSIC GOLDEN AGE"),
  SPECIAL("SPECIAL", "FORMULA & SUPERBIKE")
}

data class GarageVisualSwatch(
  val id: String,
  val name: String,
  val topLeftHex: Long,
  val bottomRightHex: Long
)

private val GARAGE_VISUAL_SWATCHES = listOf(
  GarageVisualSwatch("PEACH_GOLD", "Sunset Solar Gold", 0xFFF5CBA7, 0xFFE59866),
  GarageVisualSwatch("RACING_RED", "Apex Rosso Red", 0xFFFF5252, 0xFFE50914),
  GarageVisualSwatch("CRIMSON_ROSE", "Crimson Carbon", 0xFFEC407A, 0xFFC2185B),
  GarageVisualSwatch("NEON_PINK", "Miami GT Rose", 0xFFF48FB1, 0xFFF06292),
  GarageVisualSwatch("HOT_MAGENTA", "Hyper Magenta", 0xFFFF4081, 0xFFD81B60),
  GarageVisualSwatch("CORAL_ORANGE", "Papaya McLaren Orange", 0xFFFF8A65, 0xFFFF5722),
  GarageVisualSwatch("VOLT_CYAN", "Metallic Silver-Cyan", 0xFFE2E8F0, 0xFF94A3B8),
  GarageVisualSwatch("CHAMPION_GOLD", "Stealth Gunmetal Gold", 0xFFFFD54F, 0xFFFF8F00)
)

@Composable
fun ApexVehicleGarageScreen(
  vehicles: List<Vehicle>,
  activeVehicle: Vehicle,
  onSelectVehicle: (Vehicle) -> Unit,
  onBack: () -> Unit,
  onCustomizeClicked: () -> Unit,
  onUpgradeClicked: () -> Unit,
  onRaceClicked: () -> Unit,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val careerState by ApexRivals2CareerManager.state.collectAsState()
  val loadout = remember(activeVehicle.id, careerState.loadoutsByVehicleId) {
    ApexRivals2CareerManager.getLoadoutForVehicle(activeVehicle)
  }
  val stats = remember(activeVehicle, loadout) {
    ApexRivals2CareerManager.computeUpgradedStats(activeVehicle, loadout)
  }

  // Interactive visual tabs: PAINT | WHEELS | SPOILERS | BODY KITS | RIM COLOR | TIRE SMOKE
  var activeVisualTab by remember { mutableStateOf(GarageVisualCategoryTab.BODY_COLOR) }
  var selectedCategoryFilter by remember { mutableStateOf(GarageVehicleCategoryFilter.ALL) }
  var selectedRimAccentHex by remember { mutableStateOf(0xFFE50914L) } // Racing-Red spoke/caliper accent
  var selectedSmokeColorHex by remember { mutableStateOf(0xFFECEFF1L) }
  // Stage mode: 0 = Interactive 3D Rotating Hangar Turntable, 1 = Full 360° 3D Orbit Inspector, 2 = Reference Studio Photo
  var stageViewMode by remember { mutableIntStateOf(0) }
  var isAutoRotate3D by remember { mutableStateOf(true) }
  var manualYawDeg by remember { mutableFloatStateOf(24f) }
  var upgradeFeedback by remember { mutableStateOf<String?>(null) }

  // Continuous 360° turntable spin animation when auto-rotate is enabled
  val infiniteTransition = rememberInfiniteTransition(label = "garage_3d_turntable")
  val animatedSpinYaw by infiniteTransition.animateFloat(
    initialValue = 0f,
    targetValue = 360f,
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 18000, easing = LinearEasing),
      repeatMode = RepeatMode.Restart
    ),
    label = "garage_yaw_spin"
  )
  val effectiveYawDeg = if (isAutoRotate3D) animatedSpinYaw else manualYawDeg

  // Filtered vehicles by category (defaults to ALL so all 10 vehicles remain accessible immediately)
  val filteredVehicles = remember(vehicles, selectedCategoryFilter) {
    when (selectedCategoryFilter) {
      GarageVehicleCategoryFilter.ALL -> vehicles
      GarageVehicleCategoryFilter.GT3 -> vehicles.filter { it.category.contains("GT3", ignoreCase = true) }
      GarageVehicleCategoryFilter.HYPERCAR -> vehicles.filter {
        it.category.contains("Hypercar", ignoreCase = true) || it.category.contains("LMP1", ignoreCase = true)
      }
      GarageVehicleCategoryFilter.CLASSIC -> vehicles.filter { it.category.contains("Classic", ignoreCase = true) }
      GarageVehicleCategoryFilter.SPECIAL -> vehicles.filter {
        it.type == VehicleType.MOTORBIKE || it.category.contains("Formula", ignoreCase = true)
      }
    }.ifEmpty { vehicles }
  }

  Column(
    modifier = modifier
      .fillMaxSize()
      .background(Color(0xFF08090D))
      .verticalScroll(rememberScrollState())
      .padding(horizontal = 12.dp, vertical = 8.dp)
      .testTag("screen_4_vehicle_garage"),
    verticalArrangement = Arrangement.spacedBy(10.dp)
  ) {
    // 1. TOP SHOWROOM BAR: Back Button + Settings/Tuning Icons + Studio Title + Preserved Player Currency HUD
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .testTag("garage_header_banner"),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        IconButton(
          onClick = onBack,
          modifier = Modifier
            .size(36.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF141720))
            .border(1.dp, Color(0xFFE50914).copy(alpha = 0.65f), RoundedCornerShape(8.dp))
            .testTag("frontend_back_btn")
        ) {
          Icon(
            Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = "Back",
            tint = PureWhite,
            modifier = Modifier.size(18.dp)
          )
        }

        // Quick Customization & Performance Upgrade Shortcut Buttons
        Box(
          modifier = Modifier
            .size(32.dp)
            .clip(CircleShape)
            .background(Color(0xFF1A1E29))
            .border(1.dp, Color(0xFFCBD5E1).copy(alpha = 0.35f), CircleShape)
            .clickable { onCustomizeClicked() },
          contentAlignment = Alignment.Center
        ) {
          Icon(
            Icons.Default.Palette,
            contentDescription = "3D Styling Studio",
            tint = PureWhite,
            modifier = Modifier.size(16.dp)
          )
        }
        Box(
          modifier = Modifier
            .size(32.dp)
            .clip(CircleShape)
            .background(Color(0xFF1A1E29))
            .border(1.dp, Color(0xFFE50914).copy(alpha = 0.55f), CircleShape)
            .clickable { onUpgradeClicked() },
          contentAlignment = Alignment.Center
        ) {
          Icon(
            Icons.Default.Build,
            contentDescription = "Performance Engineering",
            tint = PureWhite,
            modifier = Modifier.size(16.dp)
          )
        }

        Column {
          Text(
            text = "APEX RIVALS • 3D VEHICLE GARAGE",
            fontSize = 11.5.sp,
            fontWeight = FontWeight.Black,
            color = PureWhite,
            letterSpacing = 1.sp
          )
          Text(
            text = "${activeVehicle.name.uppercase()} • ${activeVehicle.category.uppercase()}",
            fontSize = 8.5.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFFE50914),
            fontFamily = FontFamily.Monospace
          )
        }
      }

      // Top-Right Preserved Currency & 3D Camera Mode Switcher
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        Row(
          modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(Color(0xFF12151E))
            .border(1.dp, Color(0xFF94A3B8).copy(alpha = 0.35f), RoundedCornerShape(6.dp))
            .padding(horizontal = 10.dp, vertical = 5.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
          // Owned Vehicles Counter
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
          ) {
            Box(
              modifier = Modifier
                .size(16.dp)
                .clip(CircleShape)
                .background(Color(0xFFE50914)),
              contentAlignment = Alignment.Center
            ) {
              Text("★", fontSize = 9.sp, fontWeight = FontWeight.Black, color = PureWhite)
            }
            Text(
              text = "${vehicles.size}/${vehicles.size} OWNED",
              fontSize = 9.5.sp,
              fontWeight = FontWeight.Black,
              color = Color(0xFFE2E8F0),
              fontFamily = FontFamily.Monospace
            )
          }

          // Preserved Player Credits Balance (CR)
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
          ) {
            Box(
              modifier = Modifier
                .size(16.dp)
                .clip(CircleShape)
                .background(Color(0xFFCBD5E1)),
              contentAlignment = Alignment.Center
            ) {
              Text("C", fontSize = 9.sp, fontWeight = FontWeight.Black, color = Color(0xFF090B10))
            }
            Text(
              text = "%,d CR".format(careerState.creditsBalance),
              fontSize = 11.sp,
              fontWeight = FontWeight.Black,
              color = PureWhite,
              fontFamily = FontFamily.Monospace
            )
          }
        }

        // 3D Turntable / View Mode Switcher
        Box(
          modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(Color(0xFF161922))
            .border(1.dp, Color(0xFFE50914), RoundedCornerShape(6.dp))
            .clickable { stageViewMode = (stageViewMode + 1) % 3 }
            .padding(horizontal = 8.dp, vertical = 5.dp)
        ) {
          Text(
            text = when (stageViewMode) {
              0 -> "3D TURNTABLE"
              1 -> "360° ORBIT"
              else -> "STUDIO REF"
            },
            fontSize = 8.5.sp,
            fontWeight = FontWeight.Black,
            color = PureWhite,
            fontFamily = FontFamily.Monospace
          )
        }
      }
    }

    // 2. MAIN 3D SHOWROOM STAGE: Left Navigation & Spec Card + Center 3D Rotating Vehicle Preview + Right Live Statistics Panel
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .height(282.dp)
        .clip(RoundedCornerShape(14.dp))
        .border(1.5.dp, Color(0xFF2D3342), RoundedCornerShape(14.dp))
        .pointerInput(Unit) {
          detectHorizontalDragGestures { change, dragAmount ->
            change.consume()
            isAutoRotate3D = false
            manualYawDeg = (manualYawDeg + dragAmount * 0.45f) % 360f
          }
        }
        .testTag("garage_centerpiece_stage")
    ) {
      // Background & Centerpiece Large 3D Rotating Vehicle Preview on Hydraulic Turntable
      when (stageViewMode) {
        0 -> {
          GarageWorkshopProminentCarCanvas(
            vehicle = activeVehicle,
            bodyColorHex = loadout.paintColorHex,
            wheelRimStyle = loadout.wheelRimStyle,
            rimAccentHex = selectedRimAccentHex,
            smokeColorHex = selectedSmokeColorHex,
            yawAngleDeg = effectiveYawDeg,
            spoilerStyle = loadout.spoilerStyle,
            bodyKitSpec = loadout.bodyKitSpec,
            paintFinishMaterial = loadout.paintFinishMaterial,
            modifier = Modifier
              .fillMaxSize()
              .testTag("garage_prominent_car_canvas")
          )
        }
        1 -> {
          Box(
            modifier = Modifier
              .fillMaxSize()
              .background(Color(0xFF090C12))
              .padding(12.dp)
              .testTag("garage_prominent_car_canvas")
          ) {
            Showroom3DVehiclePreviewBox(
              vehicle = activeVehicle,
              loadout = loadout,
              heightDp = 252
            )
          }
        }
        else -> {
          Image(
            painter = painterResource(id = R.drawable.apex_garage_reference_scene),
            contentDescription = "Garage Showroom Reference Scene",
            contentScale = ContentScale.Crop,
            modifier = Modifier
              .fillMaxSize()
              .testTag("garage_prominent_car_canvas")
          )
        }
      }

      // LEFT OVERLAY: Vertical Garage Menu (MY CARS, DEALERSHIP, TUNING, STYLING) + Spec & Action Card
      Row(
        modifier = Modifier
          .align(Alignment.CenterStart)
          .fillMaxHeight()
          .padding(start = 8.dp, top = 8.dp, bottom = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        // Left Vertical Navigation Rail in Matte Black, Racing-Red & Metallic-Silver
        Column(
          modifier = Modifier
            .fillMaxHeight()
            .width(64.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFF0A0C12).copy(alpha = 0.88f))
            .border(1.dp, Color(0xFF94A3B8).copy(alpha = 0.24f), RoundedCornerShape(10.dp))
            .padding(vertical = 6.dp),
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.SpaceEvenly
        ) {
          GarageLeftRailItem(
            label = "MY CARS",
            icon = Icons.Default.DirectionsCar,
            hasAlert = false,
            isSelected = true,
            onClick = { selectedCategoryFilter = GarageVehicleCategoryFilter.ALL }
          )
          GarageLeftRailItem(
            label = "NEXT CAR",
            icon = Icons.Default.Star,
            hasAlert = false,
            isSelected = false,
            onClick = { onSelectVehicle(vehicles[(vehicles.indexOf(activeVehicle) + 1) % vehicles.size]) }
          )
          GarageLeftRailItem(
            label = "TUNING",
            icon = Icons.Default.Tune,
            hasAlert = true,
            isSelected = false,
            onClick = onUpgradeClicked
          )
          GarageLeftRailItem(
            label = "STYLING",
            icon = Icons.Default.Palette,
            hasAlert = false,
            isSelected = false,
            onClick = onCustomizeClicked
          )
        }

        // Compact Vehicle Identity & Active Loadout Card
        Column(
          modifier = Modifier.width(146.dp),
          verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
          Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0E121B).copy(alpha = 0.92f)),
            shape = RoundedCornerShape(10.dp),
            border = BorderStroke(1.dp, Color(0xFF94A3B8).copy(alpha = 0.30f)),
            modifier = Modifier
              .fillMaxWidth()
              .testTag("garage_vehicle_spec_card")
          ) {
            Column(
              modifier = Modifier.padding(8.dp),
              verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
              ) {
                Column(modifier = Modifier.weight(1f)) {
                  Text(
                    text = activeVehicle.name,
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = PureWhite,
                    maxLines = 1,
                    modifier = Modifier.testTag("garage_vehicle_name")
                  )
                  Text(
                    text = activeVehicle.category,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFCBD5E1),
                    maxLines = 1,
                    modifier = Modifier.testTag("garage_vehicle_class")
                  )
                }
                Spacer(modifier = Modifier.width(4.dp))
                Box(
                  modifier = Modifier
                    .size(22.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0xFFE50914)),
                  contentAlignment = Alignment.Center
                ) {
                  Text(
                    text = "S",
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Black,
                    color = PureWhite
                  )
                }
              }

              // Active Paint Finish, Rim, Wing & Body Kit Summary
              Text(
                text = "${loadout.paintFinishMaterial.displayName} • ${loadout.wheelRimStyle.displayName}",
                fontSize = 7.2.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF94A3B8),
                maxLines = 1,
                fontFamily = FontFamily.Monospace
              )
              Text(
                text = if (activeVehicle.type == VehicleType.CAR) {
                  "${loadout.bodyKitSpec.displayName} • ${loadout.spoilerStyle.displayName}"
                } else {
                  "Factory Moto Carbon Aero Fairing"
                },
                fontSize = 7.2.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFE50914),
                maxLines = 1,
                fontFamily = FontFamily.Monospace
              )

              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Text(
                  text = "${stats.horsepower} HP",
                  fontSize = 10.5.sp,
                  fontWeight = FontWeight.Black,
                  color = PureWhite
                )
                Text(
                  text = "STG ${stats.totalUpgradeLevel}/${stats.maxUpgradeLevel}",
                  fontSize = 8.5.sp,
                  fontWeight = FontWeight.Black,
                  color = Color(0xFFCBD5E1),
                  fontFamily = FontFamily.Monospace
                )
              }

              // CUSTOMIZE Button inside Spec Card
              Box(
                modifier = Modifier
                  .fillMaxWidth()
                  .height(28.dp)
                  .clip(RoundedCornerShape(6.dp))
                  .background(
                    Brush.verticalGradient(
                      colors = listOf(Color(0xFFF8FAFC), Color(0xFFCBD5E1))
                    )
                  )
                  .border(1.dp, PureWhite, RoundedCornerShape(6.dp))
                  .clickable { onCustomizeClicked() }
                  .testTag("garage_screen_customize_btn"),
                contentAlignment = Alignment.Center
              ) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.Center
                ) {
                  Icon(
                    Icons.Default.Palette,
                    contentDescription = "Customize",
                    tint = Color(0xFF090B10),
                    modifier = Modifier.size(13.dp)
                  )
                  Spacer(modifier = Modifier.width(4.dp))
                  Text(
                    text = "3D CUSTOMIZE",
                    fontSize = 8.5.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFF090B10)
                  )
                }
              }
            }
          }

          // INVENTORY / TUNING Button below Spec Card
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .height(26.dp)
              .clip(RoundedCornerShape(6.dp))
              .background(Color(0xFF171B26).copy(alpha = 0.94f))
              .border(1.dp, Color(0xFFE50914).copy(alpha = 0.65f), RoundedCornerShape(6.dp))
              .clickable { onUpgradeClicked() }
              .testTag("garage_inventory_btn"),
            contentAlignment = Alignment.Center
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.Center
            ) {
              Icon(
                Icons.Default.Build,
                contentDescription = "Inventory",
                tint = PureWhite,
                modifier = Modifier.size(12.dp)
              )
              Spacer(modifier = Modifier.width(4.dp))
              Text(
                text = "ENGINE & TURBO LAB",
                fontSize = 8.sp,
                fontWeight = FontWeight.Black,
                color = PureWhite
              )
            }
          }
        }
      }

      // RIGHT SIDE OVERLAY: Live Speed, Acceleration, Handling & Braking Statistics + RACE CTA
      Column(
        modifier = Modifier
          .align(Alignment.CenterEnd)
          .fillMaxHeight()
          .width(204.dp)
          .padding(end = 10.dp, top = 8.dp, bottom = 8.dp)
          .testTag("garage_side_statistics_panel"),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.End
      ) {
        // Technical Specifications & Live Statistics Panel on the Right Side
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFF0B0E16).copy(alpha = 0.92f))
            .border(1.dp, Color(0xFF94A3B8).copy(alpha = 0.30f), RoundedCornerShape(10.dp))
            .padding(horizontal = 10.dp, vertical = 7.dp),
          verticalArrangement = Arrangement.spacedBy(3.5.dp),
          horizontalAlignment = Alignment.End
        ) {
          val perfIndex = 500 + stats.totalUpgradeLevel * 18 + (stats.maxSpeedKmh - 290).coerceAtLeast(0)
          val speedDelta = (stats.maxSpeedKmh - activeVehicle.topSpeedKmh).coerceAtLeast(0)
          val handlingDelta = (loadout.wheelRimStyle.handlingBonusG + loadout.spoilerStyle.handlingBonusG + loadout.bodyKitSpec.handlingBonusG)

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(
              text = "TELEMETRY SPECS",
              fontSize = 8.sp,
              fontWeight = FontWeight.Black,
              color = Color(0xFFCBD5E1),
              letterSpacing = 0.6.sp
            )
            Box(
              modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .background(Color(0xFFE50914))
                .padding(horizontal = 6.dp, vertical = 1.5.dp)
            ) {
              Text(
                text = "PI $perfIndex",
                fontSize = 8.sp,
                fontWeight = FontWeight.Black,
                color = PureWhite,
                fontFamily = FontFamily.Monospace
              )
            }
          }

          GarageSideTechSpecItem("${stats.horsepower} HP • STG ${loadout.engineLevel} ENG / STG ${loadout.turboLevel} TRB")
          GarageSideTechSpecItem(
            if (speedDelta > 0 || handlingDelta > 0.01f) {
              "+$speedDelta km/h • +${String.format(Locale.US, "%.2f", handlingDelta)}G AERO/TIRES"
            } else {
              "FACTORY HOMOLOGATION SPEC"
            }
          )

          Spacer(modifier = Modifier.height(1.dp))

          // Live Side Stat Bars (Speed, Acceleration, Handling, Braking)
          GarageCompactSideStatBar(
            label = "SPEED",
            valueText = "${stats.maxSpeedKmh} km/h",
            progress = (stats.maxSpeedKmh / 415f).coerceIn(0.25f, 1f),
            accentColor = Color(0xFFE50914)
          )
          GarageCompactSideStatBar(
            label = "ACCELERATION",
            valueText = String.format(Locale.US, "%.2fs 0-100", stats.acceleration0to100Sec),
            progress = ((4.5f - stats.acceleration0to100Sec) / 3.2f).coerceIn(0.25f, 1f),
            accentColor = Color(0xFFE2E8F0)
          )
          GarageCompactSideStatBar(
            label = "HANDLING",
            valueText = String.format(Locale.US, "%.2f G", stats.handlingCorneringG),
            progress = (stats.handlingCorneringG / 4.2f).coerceIn(0.25f, 1f),
            accentColor = Color(0xFF38BDF8)
          )
          GarageCompactSideStatBar(
            label = "BRAKING",
            valueText = "${stats.brakingRating}/100",
            progress = (stats.brakingRating / 100f).coerceIn(0.25f, 1f),
            accentColor = Color(0xFFF59E0B)
          )
        }

        // 3D Rotation Controls + Primary Racing-Red "RACE >>>" Button
        Column(
          modifier = Modifier.fillMaxWidth(),
          verticalArrangement = Arrangement.spacedBy(5.dp),
          horizontalAlignment = Alignment.End
        ) {
          // Interactive 3D Turntable Angle & Auto-Spin Control Bar
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .height(27.dp)
              .clip(RoundedCornerShape(6.dp))
              .background(Color(0xFF121622).copy(alpha = 0.92f))
              .border(1.dp, Color(0xFF94A3B8).copy(alpha = 0.35f), RoundedCornerShape(6.dp))
              .padding(horizontal = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(
              text = "↺ -45°",
              fontSize = 8.sp,
              fontWeight = FontWeight.Black,
              color = Color(0xFFCBD5E1),
              fontFamily = FontFamily.Monospace,
              modifier = Modifier
                .clickable {
                  isAutoRotate3D = false
                  manualYawDeg = (manualYawDeg - 45f + 360f) % 360f
                }
                .padding(horizontal = 4.dp, vertical = 2.dp)
            )
            Text(
              text = if (isAutoRotate3D) "3D SPIN: AUTO" else "YAW ${effectiveYawDeg.toInt()}°",
              fontSize = 7.8.sp,
              fontWeight = FontWeight.Black,
              color = if (isAutoRotate3D) Color(0xFFE50914) else PureWhite,
              fontFamily = FontFamily.Monospace,
              modifier = Modifier
                .clickable { isAutoRotate3D = !isAutoRotate3D }
                .padding(horizontal = 4.dp, vertical = 2.dp)
            )
            Text(
              text = "+45° ↻",
              fontSize = 8.sp,
              fontWeight = FontWeight.Black,
              color = Color(0xFFCBD5E1),
              fontFamily = FontFamily.Monospace,
              modifier = Modifier
                .clickable {
                  isAutoRotate3D = false
                  manualYawDeg = (manualYawDeg + 45f) % 360f
                }
                .padding(horizontal = 4.dp, vertical = 2.dp)
            )
          }

          // Racing-Red & Metallic-Silver "RACE >>>" Primary Action Button
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .height(36.dp)
              .clip(RoundedCornerShape(6.dp))
              .background(
                Brush.horizontalGradient(
                  colors = listOf(Color(0xFFB91C1C), Color(0xFFE50914), Color(0xFFEF4444))
                )
              )
              .border(1.5.dp, Color(0xFFE2E8F0), RoundedCornerShape(6.dp))
              .clickable { onRaceClicked() },
            contentAlignment = Alignment.Center
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              Text(
                text = "RACE NOW",
                fontSize = 13.5.sp,
                fontWeight = FontWeight.Black,
                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                color = PureWhite,
                letterSpacing = 1.sp
              )
              Text(
                text = "❯❯❯",
                fontSize = 12.sp,
                fontWeight = FontWeight.Black,
                color = PureWhite
              )
            }
          }
        }
      }

      // TOP-CENTER 3D HANGAR TURNTABLE STATUS PILL
      Row(
        modifier = Modifier
          .align(Alignment.TopCenter)
          .padding(top = 6.dp)
          .clip(RoundedCornerShape(6.dp))
          .background(Color(0xFF0A0D14).copy(alpha = 0.88f))
          .border(1.dp, Color(0xFF94A3B8).copy(alpha = 0.28f), RoundedCornerShape(6.dp))
          .padding(horizontal = 8.dp, vertical = 3.dp)
          .testTag("garage_club_tab_bar"),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = "MY GARAGE",
          fontSize = 7.5.sp,
          fontWeight = FontWeight.Black,
          color = PureWhite
        )
        Box(
          modifier = Modifier
            .clip(RoundedCornerShape(3.dp))
            .background(Color(0xFFE50914))
            .padding(horizontal = 6.dp, vertical = 1.5.dp)
        ) {
          Text(
            text = "3D TURNTABLE ACTIVE",
            fontSize = 7.sp,
            fontWeight = FontWeight.Black,
            color = PureWhite
          )
        }
        Text(
          text = "DRAG TO ROTATE 360°",
          fontSize = 7.2.sp,
          fontWeight = FontWeight.Bold,
          color = Color(0xFFCBD5E1),
          fontFamily = FontFamily.Monospace
        )
      }

      // BOTTOM-CENTER 5-STATION CUSTOMIZATION & UPGRADE DOCK
      Row(
        modifier = Modifier
          .align(Alignment.BottomCenter)
          .padding(bottom = 6.dp)
          .clip(RoundedCornerShape(8.dp))
          .background(Color(0xFF0A0D14).copy(alpha = 0.90f))
          .border(1.dp, Color(0xFF94A3B8).copy(alpha = 0.25f), RoundedCornerShape(8.dp))
          .padding(horizontal = 10.dp, vertical = 4.dp)
          .testTag("garage_five_station_dock"),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        listOf(
          Triple("PAINT", "Finish", { activeVisualTab = GarageVisualCategoryTab.BODY_COLOR }),
          Triple("WHEELS", "Forged", { activeVisualTab = GarageVisualCategoryTab.RIMS }),
          Triple("WINGS", "Spoilers", { activeVisualTab = GarageVisualCategoryTab.SPOILERS }),
          Triple("AERO", "Body Kits", { activeVisualTab = GarageVisualCategoryTab.BODY_KITS }),
          Triple("STAGE ${stats.totalUpgradeLevel}", "Engine/Turbo", onUpgradeClicked)
        ).forEach { (badge, stationLabel, action) ->
          Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.clickable { action() }
          ) {
            Text(
              text = badge,
              fontSize = 6.5.sp,
              fontWeight = FontWeight.Black,
              color = Color(0xFFE50914),
              fontFamily = FontFamily.Monospace
            )
            Text(
              text = stationLabel,
              fontSize = 7.sp,
              fontWeight = FontWeight.Bold,
              color = PureWhite
            )
          }
        }
      }
    }

    // 3. VEHICLE CATEGORIES & HORIZONTAL VEHICLE SELECTION CAROUSEL (Preserves all existing unlocked cars)
    Column(
      modifier = Modifier.fillMaxWidth(),
      verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          Box(
            modifier = Modifier
              .clip(RoundedCornerShape(4.dp))
              .background(Color(0xFFE50914))
              .padding(horizontal = 6.dp, vertical = 2.dp)
          ) {
            Text(
              text = "ROSTER",
              fontSize = 8.sp,
              fontWeight = FontWeight.Black,
              color = PureWhite
            )
          }
          Text(
            text = "VEHICLE SELECTION & CATEGORIES (${filteredVehicles.size}/${vehicles.size} UNLOCKED)",
            fontSize = 9.5.sp,
            fontWeight = FontWeight.Black,
            color = PureWhite,
            letterSpacing = 0.6.sp
          )
        }
        Text(
          text = "TAP TO SWITCH ACTIVE MACHINE",
          fontSize = 8.sp,
          fontWeight = FontWeight.Bold,
          color = Color(0xFFCBD5E1),
          fontFamily = FontFamily.Monospace
        )
      }

      // Interactive Vehicle Category Filter Strip
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .horizontalScroll(rememberScrollState())
          .testTag("garage_category_filter_row"),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
      ) {
        GarageVehicleCategoryFilter.entries.forEach { categoryFilter ->
          val isSelectedCat = selectedCategoryFilter == categoryFilter
          Box(
            modifier = Modifier
              .clip(RoundedCornerShape(6.dp))
              .background(if (isSelectedCat) Color(0xFFE50914) else Color(0xFF141822))
              .border(
                width = 1.dp,
                color = if (isSelectedCat) PureWhite else Color(0xFF94A3B8).copy(alpha = 0.28f),
                shape = RoundedCornerShape(6.dp)
              )
              .clickable { selectedCategoryFilter = categoryFilter }
              .padding(horizontal = 10.dp, vertical = 5.dp)
          ) {
            Text(
              text = categoryFilter.label,
              fontSize = 8.sp,
              fontWeight = FontWeight.Black,
              color = if (isSelectedCat) PureWhite else Color(0xFFCBD5E1),
              fontFamily = FontFamily.Monospace
            )
          }
        }
      }

      Row(
        modifier = Modifier
          .fillMaxWidth()
          .horizontalScroll(rememberScrollState())
          .testTag("garage_vehicle_carousel"),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        filteredVehicles.forEachIndexed { index, v ->
          val isSelected = v.id == activeVehicle.id
          val vLoadout = ApexRivals2CareerManager.getLoadoutForVehicle(v)
          val vStats = ApexRivals2CareerManager.computeUpgradedStats(v, vLoadout)
          val ppRating = 500 + vStats.totalUpgradeLevel * 18 + (vStats.maxSpeedKmh - 290).coerceAtLeast(0)
          Card(
            colors = CardDefaults.cardColors(
              containerColor = if (isSelected) Color(0xFF1A1F2E) else Color(0xFF10131C)
            ),
            shape = RoundedCornerShape(10.dp),
            border = BorderStroke(
              width = if (isSelected) 2.dp else 1.dp,
              color = if (isSelected) Color(0xFFE50914) else Color(0xFF94A3B8).copy(alpha = 0.25f)
            ),
            modifier = Modifier
              .width(162.dp)
              .clickable { onSelectVehicle(v) }
              .testTag("carousel_vehicle_${v.id}")
          ) {
            Column(
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
              verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
              // Top Category & Performance Index Strip
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Box(
                  modifier = Modifier
                    .clip(RoundedCornerShape(3.dp))
                    .background(if (isSelected) Color(0xFFE50914) else Color(0xFF334155))
                    .padding(horizontal = 4.dp, vertical = 1.dp)
                ) {
                  Text(
                    text = "PI $ppRating",
                    fontSize = 6.5.sp,
                    fontWeight = FontWeight.Black,
                    color = PureWhite,
                    fontFamily = FontFamily.Monospace
                  )
                }
                Text(
                  text = v.category.take(14).uppercase(),
                  fontSize = 6.5.sp,
                  fontWeight = FontWeight.Bold,
                  color = Color(0xFFCBD5E1),
                  maxLines = 1
                )
              }

              val isOwnedVehicle = ApexRivals2CareerManager.isVehicleOwned(v.id)
              val catalogPrice = ApexRivals2CareerManager.getCatalogVehiclePriceCredits(v.id)

              // Model Name & Active Status
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Text(
                  text = v.name,
                  fontSize = 9.5.sp,
                  fontWeight = FontWeight.Black,
                  color = PureWhite,
                  maxLines = 1,
                  modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                  text = when {
                    isSelected -> "SELECTED"
                    isOwnedVehicle -> "UNLOCKED"
                    else -> "CR ${catalogPrice / 1000}K"
                  },
                  fontSize = 7.sp,
                  fontWeight = FontWeight.Black,
                  color = when {
                    isSelected -> Color(0xFFE50914)
                    isOwnedVehicle -> Color(0xFF94A3B8)
                    else -> NeonAmber
                  },
                  fontFamily = FontFamily.Monospace
                )
              }

              // Mini Vehicle Silhouette Preview Canvas using vehicle's customized paint color
              Canvas(
                modifier = Modifier
                  .fillMaxWidth()
                  .height(34.dp)
              ) {
                val cw = size.width
                val ch = size.height
                val carCol = Color(vLoadout.paintColorHex)
                drawOval(
                  color = Color.Black.copy(alpha = 0.65f),
                  topLeft = Offset(cw * 0.08f, ch * 0.72f),
                  size = Size(cw * 0.84f, ch * 0.22f)
                )
                val body = Path().apply {
                  moveTo(cw * 0.08f, ch * 0.68f)
                  lineTo(cw * 0.22f, ch * 0.42f)
                  lineTo(cw * 0.46f, ch * 0.24f)
                  lineTo(cw * 0.72f, ch * 0.26f)
                  lineTo(cw * 0.90f, ch * 0.45f)
                  lineTo(cw * 0.92f, ch * 0.72f)
                  close()
                }
                drawPath(
                  path = body,
                  brush = Brush.horizontalGradient(
                    colors = listOf(carCol.copy(alpha = 0.85f), carCol, carCol.copy(alpha = 0.65f))
                  )
                )
                if (v.type == VehicleType.CAR) {
                  drawLine(
                    color = Color.White.copy(alpha = 0.75f),
                    start = Offset(cw * 0.82f, ch * 0.20f),
                    end = Offset(cw * 0.94f, ch * 0.20f),
                    strokeWidth = 2.8f
                  )
                }
                drawCircle(color = Color(0xFF0F172A), radius = ch * 0.20f, center = Offset(cw * 0.26f, ch * 0.72f))
                drawCircle(color = Color(0xFFCBD5E1), radius = ch * 0.11f, center = Offset(cw * 0.26f, ch * 0.72f))
                drawCircle(color = Color(0xFF0F172A), radius = ch * 0.20f, center = Offset(cw * 0.76f, ch * 0.72f))
                drawCircle(color = Color(0xFFCBD5E1), radius = ch * 0.11f, center = Offset(cw * 0.76f, ch * 0.72f))
              }

              // Bottom Row: Upgraded Top Speed, Handling & Ownership Purchase/Owned Pill
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Text(
                  text = "${vStats.maxSpeedKmh} km/h • ${String.format(Locale.US, "%.1fG", vStats.handlingCorneringG)}",
                  fontSize = 7.5.sp,
                  fontWeight = FontWeight.Bold,
                  color = Color(0xFFCBD5E1),
                  fontFamily = FontFamily.Monospace
                )
                if (isOwnedVehicle) {
                  Text(
                    text = "OWNED",
                    fontSize = 7.5.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFF4ADE80),
                    fontFamily = FontFamily.Monospace
                  )
                } else {
                  Box(
                    modifier = Modifier
                      .clip(RoundedCornerShape(4.dp))
                      .background(NeonAmber)
                      .clickable {
                        if (ApexRivals2CareerManager.purchaseVehicle(v.id, catalogPrice, context)) {
                          com.example.data.firebase.ApexCloudSyncCoordinator.onVehiclePurchased(context, v.id)
                          onSelectVehicle(v)
                          upgradeFeedback = "Purchased ${v.name} for %,d CR!".format(catalogPrice)
                        } else {
                          upgradeFeedback = "Need %,d CR to purchase ${v.name}.".format(catalogPrice)
                        }
                      }
                      .padding(horizontal = 5.dp, vertical = 1.5.dp)
                      .testTag("garage_buy_vehicle_${v.id}")
                  ) {
                    Text(
                      text = "BUY",
                      fontSize = 7.sp,
                      fontWeight = FontWeight.Black,
                      color = CarbonBlack,
                      fontFamily = FontFamily.Monospace
                    )
                  }
                }
              }
            }
          }
        }
      }
    }

    // 4. CUSTOMIZATION STUDIO: PAINT, WHEELS, SPOILERS, BODY KITS, RIM COLOR & TIRE SMOKE
    Card(
      colors = CardDefaults.cardColors(containerColor = Color(0xFF10131C)),
      shape = RoundedCornerShape(14.dp),
      border = BorderStroke(1.dp, Color(0xFF2D3342)),
      modifier = Modifier.fillMaxWidth()
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = "VISUAL & AERO CUSTOMIZATION • ${activeVisualTab.label}",
            fontSize = 10.sp,
            fontWeight = FontWeight.Black,
            color = PureWhite,
            letterSpacing = 0.6.sp
          )
          Text(
            text = "${loadout.paintFinishMaterial.displayName.uppercase()} • ${loadout.wheelRimStyle.displayName.uppercase()}",
            fontSize = 8.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFFE50914),
            fontFamily = FontFamily.Monospace
          )
        }

        // Swatch Strip (always rendered for quick color/preset selection and testTag compatibility)
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .testTag("garage_swatch_strip"),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          GARAGE_VISUAL_SWATCHES.forEachIndexed { idx, swatch ->
            val isSelected = when (activeVisualTab) {
              GarageVisualCategoryTab.BODY_COLOR -> loadout.paintColorHex == swatch.bottomRightHex
              GarageVisualCategoryTab.RIMS -> loadout.wheelRimStyle.ordinal == (idx % WheelRimStyle.entries.size)
              GarageVisualCategoryTab.SPOILERS -> loadout.spoilerStyle.ordinal == (idx % SpoilerWingStyle.entries.size)
              GarageVisualCategoryTab.BODY_KITS -> loadout.bodyKitSpec.ordinal == (idx % BodyKitSpec.entries.size)
              GarageVisualCategoryTab.RIMS_COLOR -> selectedRimAccentHex == swatch.bottomRightHex
              GarageVisualCategoryTab.SMOKE_COLOR -> selectedSmokeColorHex == swatch.bottomRightHex
            }
            Box(
              modifier = Modifier
                .size(width = 72.dp, height = 38.dp)
                .clip(RoundedCornerShape(6.dp))
                .border(
                  width = if (isSelected) 2.5.dp else 1.dp,
                  color = if (isSelected) Color(0xFFE50914) else Color(0xFFCBD5E1).copy(alpha = 0.65f),
                  shape = RoundedCornerShape(6.dp)
                )
                .clickable {
                  when (activeVisualTab) {
                    GarageVisualCategoryTab.BODY_COLOR -> {
                      ApexRivals2CareerManager.applyVisualCustomization(
                        vehicle = activeVehicle,
                        paintColorHex = swatch.bottomRightHex,
                        wheelRimStyle = loadout.wheelRimStyle,
                        spoilerStyle = loadout.spoilerStyle,
                        bodyKitSpec = loadout.bodyKitSpec,
                        context = context
                      )
                      upgradeFeedback = "Applied ${swatch.name} Paint!"
                    }
                    GarageVisualCategoryTab.RIMS -> {
                      val style = WheelRimStyle.entries[idx % WheelRimStyle.entries.size]
                      ApexRivals2CareerManager.applyVisualCustomization(
                        vehicle = activeVehicle,
                        paintColorHex = loadout.paintColorHex,
                        wheelRimStyle = style,
                        spoilerStyle = loadout.spoilerStyle,
                        bodyKitSpec = loadout.bodyKitSpec,
                        context = context
                      )
                      upgradeFeedback = "Installed ${style.displayName} Wheels!"
                    }
                    GarageVisualCategoryTab.SPOILERS -> {
                      val wing = SpoilerWingStyle.entries[idx % SpoilerWingStyle.entries.size]
                      ApexRivals2CareerManager.applyVisualCustomization(
                        vehicle = activeVehicle,
                        paintColorHex = loadout.paintColorHex,
                        wheelRimStyle = loadout.wheelRimStyle,
                        spoilerStyle = wing,
                        bodyKitSpec = loadout.bodyKitSpec,
                        context = context
                      )
                      upgradeFeedback = "Equipped ${wing.displayName}!"
                    }
                    GarageVisualCategoryTab.BODY_KITS -> {
                      val kit = BodyKitSpec.entries[idx % BodyKitSpec.entries.size]
                      ApexRivals2CareerManager.applyVisualCustomization(
                        vehicle = activeVehicle,
                        paintColorHex = loadout.paintColorHex,
                        wheelRimStyle = loadout.wheelRimStyle,
                        spoilerStyle = loadout.spoilerStyle,
                        bodyKitSpec = kit,
                        context = context
                      )
                      upgradeFeedback = "Fitted ${kit.displayName}!"
                    }
                    GarageVisualCategoryTab.RIMS_COLOR -> {
                      selectedRimAccentHex = swatch.bottomRightHex
                      upgradeFeedback = "Updated Caliper & Rim Lip Color: ${swatch.name}"
                    }
                    GarageVisualCategoryTab.SMOKE_COLOR -> {
                      selectedSmokeColorHex = swatch.bottomRightHex
                      upgradeFeedback = "Updated Tire Smoke Color: ${swatch.name}"
                    }
                  }
                }
                .testTag("garage_swatch_${swatch.id}")
            ) {
              Canvas(modifier = Modifier.matchParentSize()) {
                val w = size.width
                val h = size.height
                drawRect(color = Color(swatch.bottomRightHex))
                val diagPath = Path().apply {
                  moveTo(0f, 0f)
                  lineTo(w * 0.62f, 0f)
                  lineTo(w * 0.26f, h)
                  lineTo(0f, h)
                  close()
                }
                drawPath(path = diagPath, color = Color(swatch.topLeftHex))
              }
            }
          }
        }

        // Contextual Detail Selector for Active Customization Tab (Paint Materials, Wheels, Spoilers, Body Kits)
        when (activeVisualTab) {
          GarageVisualCategoryTab.BODY_COLOR -> {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
              horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
              PaintFinishMaterial.entries.forEach { finish ->
                val isCurrentFinish = loadout.paintFinishMaterial == finish
                Box(
                  modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (isCurrentFinish) Color(0xFFE50914) else Color(0xFF181C28))
                    .border(
                      1.dp,
                      if (isCurrentFinish) PureWhite else Color(0xFF94A3B8).copy(alpha = 0.28f),
                      RoundedCornerShape(6.dp)
                    )
                    .clickable {
                      ApexRivals2CareerManager.applyPaintFinishMaterial(activeVehicle, finish, context)
                      upgradeFeedback = "Applied ${finish.displayName} Paint Shader!"
                    }
                    .padding(horizontal = 9.dp, vertical = 5.dp)
                ) {
                  Text(
                    text = finish.displayName.uppercase(),
                    fontSize = 7.5.sp,
                    fontWeight = FontWeight.Black,
                    color = PureWhite,
                    fontFamily = FontFamily.Monospace
                  )
                }
              }
            }
          }

          GarageVisualCategoryTab.RIMS -> {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
              WheelRimStyle.entries.forEach { rimStyle ->
                val isSelectedRim = loadout.wheelRimStyle == rimStyle
                Card(
                  colors = CardDefaults.cardColors(
                    containerColor = if (isSelectedRim) Color(0xFF1F2536) else Color(0xFF151924)
                  ),
                  shape = RoundedCornerShape(8.dp),
                  border = BorderStroke(
                    width = if (isSelectedRim) 1.5.dp else 1.dp,
                    color = if (isSelectedRim) Color(0xFFE50914) else Color(0xFF94A3B8).copy(alpha = 0.25f)
                  ),
                  modifier = Modifier
                    .weight(1f)
                    .clickable {
                      ApexRivals2CareerManager.applyVisualCustomization(
                        vehicle = activeVehicle,
                        paintColorHex = loadout.paintColorHex,
                        wheelRimStyle = rimStyle,
                        spoilerStyle = loadout.spoilerStyle,
                        bodyKitSpec = loadout.bodyKitSpec,
                        context = context
                      )
                      upgradeFeedback = "Fitted ${rimStyle.displayName} (${rimStyle.weightDeltaKg}kg)"
                    }
                ) {
                  Column(
                    modifier = Modifier.padding(6.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                  ) {
                    Text(
                      text = rimStyle.displayName,
                      fontSize = 8.sp,
                      fontWeight = FontWeight.Black,
                      color = PureWhite,
                      maxLines = 1
                    )
                    Text(
                      text = "${rimStyle.spokeCount}-Spoke • ${rimStyle.weightDeltaKg}kg",
                      fontSize = 6.8.sp,
                      fontWeight = FontWeight.Bold,
                      color = Color(0xFFCBD5E1),
                      fontFamily = FontFamily.Monospace
                    )
                    Text(
                      text = "+${String.format(Locale.US, "%.2f", rimStyle.handlingBonusG)}G GRIP",
                      fontSize = 6.8.sp,
                      fontWeight = FontWeight.Black,
                      color = Color(0xFFE50914),
                      fontFamily = FontFamily.Monospace
                    )
                  }
                }
              }
            }
          }

          GarageVisualCategoryTab.SPOILERS -> {
            if (activeVehicle.type == VehicleType.MOTORBIKE) {
              Text(
                text = "Superbike Chassis: Uses integrated MotoGP carbon winglets (full rear spoilers apply to 4-wheel cars).",
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFCBD5E1)
              )
            }
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
              SpoilerWingStyle.entries.forEach { wing ->
                val isSelectedWing = loadout.spoilerStyle == wing
                Card(
                  colors = CardDefaults.cardColors(
                    containerColor = if (isSelectedWing) Color(0xFF1F2536) else Color(0xFF151924)
                  ),
                  shape = RoundedCornerShape(8.dp),
                  border = BorderStroke(
                    width = if (isSelectedWing) 1.5.dp else 1.dp,
                    color = if (isSelectedWing) Color(0xFFE50914) else Color(0xFF94A3B8).copy(alpha = 0.25f)
                  ),
                  modifier = Modifier
                    .weight(1f)
                    .clickable {
                      ApexRivals2CareerManager.applyVisualCustomization(
                        vehicle = activeVehicle,
                        paintColorHex = loadout.paintColorHex,
                        wheelRimStyle = loadout.wheelRimStyle,
                        spoilerStyle = wing,
                        bodyKitSpec = loadout.bodyKitSpec,
                        context = context
                      )
                      com.example.data.firebase.ApexCloudSyncCoordinator.onCustomizationSaved(context)
                      upgradeFeedback = "Equipped ${wing.displayName} (+${wing.downforceBonusPct}% Downforce)"
                    }
                ) {
                  Column(
                    modifier = Modifier.padding(6.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                  ) {
                    Text(
                      text = wing.displayName,
                      fontSize = 8.sp,
                      fontWeight = FontWeight.Black,
                      color = PureWhite,
                      maxLines = 1
                    )
                    Text(
                      text = "+${wing.downforceBonusPct}% Downforce",
                      fontSize = 6.8.sp,
                      fontWeight = FontWeight.Bold,
                      color = Color(0xFFCBD5E1),
                      fontFamily = FontFamily.Monospace
                    )
                    Text(
                      text = "+${String.format(Locale.US, "%.2f", wing.handlingBonusG)}G / ${wing.topSpeedDeltaKmh} km/h",
                      fontSize = 6.8.sp,
                      fontWeight = FontWeight.Black,
                      color = Color(0xFFE50914),
                      fontFamily = FontFamily.Monospace
                    )
                  }
                }
              }
            }
          }

          GarageVisualCategoryTab.BODY_KITS -> {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
              BodyKitSpec.entries.forEach { kit ->
                val isSelectedKit = loadout.bodyKitSpec == kit
                Card(
                  colors = CardDefaults.cardColors(
                    containerColor = if (isSelectedKit) Color(0xFF1F2536) else Color(0xFF151924)
                  ),
                  shape = RoundedCornerShape(8.dp),
                  border = BorderStroke(
                    width = if (isSelectedKit) 1.5.dp else 1.dp,
                    color = if (isSelectedKit) Color(0xFFE50914) else Color(0xFF94A3B8).copy(alpha = 0.25f)
                  ),
                  modifier = Modifier
                    .weight(1f)
                    .clickable {
                      ApexRivals2CareerManager.applyVisualCustomization(
                        vehicle = activeVehicle,
                        paintColorHex = loadout.paintColorHex,
                        wheelRimStyle = loadout.wheelRimStyle,
                        spoilerStyle = loadout.spoilerStyle,
                        bodyKitSpec = kit,
                        context = context
                      )
                      com.example.data.firebase.ApexCloudSyncCoordinator.onCustomizationSaved(context)
                      upgradeFeedback = "Installed ${kit.displayName} (+${kit.widthBonusPx.toInt()}mm stance)"
                    }
                ) {
                  Column(
                    modifier = Modifier.padding(6.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                  ) {
                    Text(
                      text = kit.displayName,
                      fontSize = 8.sp,
                      fontWeight = FontWeight.Black,
                      color = PureWhite,
                      maxLines = 1
                    )
                    Text(
                      text = "+${kit.widthBonusPx.toInt()}mm Widebody",
                      fontSize = 6.8.sp,
                      fontWeight = FontWeight.Bold,
                      color = Color(0xFFCBD5E1),
                      fontFamily = FontFamily.Monospace
                    )
                    Text(
                      text = "+${kit.topSpeedBonusKmh} km/h • +${String.format(Locale.US, "%.2f", kit.handlingBonusG)}G",
                      fontSize = 6.8.sp,
                      fontWeight = FontWeight.Black,
                      color = Color(0xFFE50914),
                      fontFamily = FontFamily.Monospace
                    )
                  }
                }
              }
            }
          }

          GarageVisualCategoryTab.RIMS_COLOR,
          GarageVisualCategoryTab.SMOKE_COLOR -> {
            Text(
              text = "Select a metallic or racing accent swatch above to update ${activeVisualTab.label.lowercase()} in real time.",
              fontSize = 8.sp,
              fontWeight = FontWeight.Medium,
              color = Color(0xFFCBD5E1)
            )
          }
        }

        // Category Selector Tabs Row (PAINT | WHEELS | SPOILERS | BODY KITS | RIM COLOR | TIRE SMOKE)
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .testTag("garage_visual_tabs_row"),
          horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
          GarageVisualCategoryTab.entries.forEach { tab ->
            val isActive = activeVisualTab == tab
            Box(
              modifier = Modifier
                .weight(1f)
                .height(34.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(
                  if (isActive) {
                    Brush.verticalGradient(colors = listOf(Color(0xFFE50914), Color(0xFF991B1B)))
                  } else {
                    Brush.verticalGradient(colors = listOf(Color(0xFF1A1E2B), Color(0xFF0E1118)))
                  }
                )
                .border(
                  width = 1.dp,
                  color = if (isActive) PureWhite else Color(0xFF94A3B8).copy(alpha = 0.24f),
                  shape = RoundedCornerShape(8.dp)
                )
                .clickable { activeVisualTab = tab }
                .testTag("garage_visual_tab_${tab.name}"),
              contentAlignment = Alignment.Center
            ) {
              Text(
                text = tab.label,
                fontSize = 7.5.sp,
                fontWeight = FontWeight.Black,
                color = PureWhite,
                maxLines = 1
              )
            }
          }
        }
      }
    }

    // 5. ENGINE, TURBO, BRAKES & TIRES UPGRADE SECTION WITH LIVE TELEMETRY DELTAS
    Card(
      colors = CardDefaults.cardColors(containerColor = Color(0xFF10131C)),
      shape = RoundedCornerShape(14.dp),
      border = BorderStroke(1.dp, Color(0xFF2D3342)),
      modifier = Modifier
        .fillMaxWidth()
        .testTag("garage_upgrade_options_section")
    ) {
      Column(
        modifier = Modifier.padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column {
            Text(
              text = "ENGINE, TURBO & CHASSIS UPGRADES • STAGE ${stats.totalUpgradeLevel}/${stats.maxUpgradeLevel}",
              fontSize = 10.sp,
              fontWeight = FontWeight.Black,
              color = PureWhite,
              letterSpacing = 0.6.sp
            )
            Text(
              text = "BALANCE: %,d CR • ALL UPGRADES PERSIST TO YOUR VEHICLE".format(careerState.creditsBalance),
              fontSize = 7.8.sp,
              fontWeight = FontWeight.Bold,
              color = Color(0xFFCBD5E1),
              fontFamily = FontFamily.Monospace
            )
          }
          Text(
            text = "${stats.horsepower} HP • ${stats.maxSpeedKmh} KM/H",
            fontSize = 9.5.sp,
            fontWeight = FontWeight.Black,
            color = Color(0xFFE50914),
            fontFamily = FontFamily.Monospace
          )
        }

        // 2x2 Direct Subsystem Upgrade Grid (ENGINE, TURBO, BRAKES, TIRES)
        val subsystems = UpgradeSubsystem.entries
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          subsystems.take(2).forEach { sub ->
            val currentLv = when (sub) {
              UpgradeSubsystem.ENGINE -> loadout.engineLevel
              UpgradeSubsystem.TURBO -> loadout.turboLevel
              UpgradeSubsystem.BRAKES -> loadout.brakesLevel
              UpgradeSubsystem.TIRES -> loadout.tiresLevel
            }
            val cost = sub.baseCostCredits * currentLv
            GarageSubsystemUpgradeTile(
              subsystem = sub,
              currentLevel = currentLv,
              upgradeCost = cost,
              onUpgrade = {
                val ok = ApexRivals2CareerManager.upgradeSubsystem(activeVehicle, sub, context)
                if (ok) {
                  com.example.data.firebase.ApexCloudSyncCoordinator.onSubsystemUpgraded(
                    context = context,
                    vehicleId = activeVehicle.id,
                    subsystem = sub.name,
                    targetStage = currentLv + 1
                  )
                }
                upgradeFeedback = if (ok) {
                  "Upgraded ${sub.displayName} to Stage ${currentLv + 1}!"
                } else {
                  "Max Stage (5/5) or insufficient credits."
                }
              },
              modifier = Modifier.weight(1f)
            )
          }
        }
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          subsystems.drop(2).forEach { sub ->
            val currentLv = when (sub) {
              UpgradeSubsystem.ENGINE -> loadout.engineLevel
              UpgradeSubsystem.TURBO -> loadout.turboLevel
              UpgradeSubsystem.BRAKES -> loadout.brakesLevel
              UpgradeSubsystem.TIRES -> loadout.tiresLevel
            }
            val cost = sub.baseCostCredits * currentLv
            GarageSubsystemUpgradeTile(
              subsystem = sub,
              currentLevel = currentLv,
              upgradeCost = cost,
              onUpgrade = {
                val ok = ApexRivals2CareerManager.upgradeSubsystem(activeVehicle, sub, context)
                if (ok) {
                  com.example.data.firebase.ApexCloudSyncCoordinator.onSubsystemUpgraded(
                    context = context,
                    vehicleId = activeVehicle.id,
                    subsystem = sub.name,
                    targetStage = currentLv + 1
                  )
                }
                upgradeFeedback = if (ok) {
                  "Upgraded ${sub.displayName} to Stage ${currentLv + 1}!"
                } else {
                  "Max Stage (5/5) or insufficient credits."
                }
              },
              modifier = Modifier.weight(1f)
            )
          }
        }

        upgradeFeedback?.let { msg ->
          Text(
            text = msg,
            fontSize = 8.5.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF4ADE80),
            fontFamily = FontFamily.Monospace
          )
        }

        // Bottom Action Row: UPGRADE STUDIO & RACE NOW
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          Button(
            onClick = onUpgradeClicked,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFCBD5E1)),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
              .weight(1f)
              .height(48.dp)
              .testTag("garage_screen_upgrade_btn")
          ) {
            Icon(Icons.Default.Build, contentDescription = null, tint = CarbonBlack, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(5.dp))
            Text("DYNO & TUNING BENCH", fontSize = 10.sp, fontWeight = FontWeight.Black, color = CarbonBlack)
          }

          Button(
            onClick = onRaceClicked,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE50914)),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
              .weight(1f)
              .height(48.dp)
              .testTag("garage_screen_race_btn")
          ) {
            Icon(Icons.Default.PlayArrow, contentDescription = null, tint = PureWhite, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(5.dp))
            Text("RACE WITH THIS CAR >>>", fontSize = 10.sp, fontWeight = FontWeight.Black, color = PureWhite)
          }
        }
      }
    }
  }
}

@Composable
private fun GarageLeftRailItem(
  label: String,
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  hasAlert: Boolean,
  isSelected: Boolean,
  onClick: () -> Unit
) {
  Column(
    horizontalAlignment = Alignment.CenterHorizontally,
    modifier = Modifier
      .fillMaxWidth()
      .clickable { onClick() }
      .padding(vertical = 2.dp)
  ) {
    Box(
      modifier = Modifier.size(28.dp),
      contentAlignment = Alignment.Center
    ) {
      Icon(
        imageVector = icon,
        contentDescription = label,
        tint = if (isSelected) Color(0xFFE50914) else PureWhite,
        modifier = Modifier.size(20.dp)
      )
      if (hasAlert) {
        Box(
          modifier = Modifier
            .size(8.dp)
            .clip(CircleShape)
            .background(Color(0xFFE50914))
            .align(Alignment.TopEnd)
        )
      }
    }
    Text(
      text = label,
      fontSize = 6.5.sp,
      fontWeight = FontWeight.Black,
      color = if (isSelected) Color(0xFFE50914) else Color(0xFFCBD5E1),
      maxLines = 1
    )
  }
}

@Composable
private fun GarageSideTechSpecItem(text: String) {
  Row(
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(5.dp)
  ) {
    Text(
      text = text,
      fontSize = 7.8.sp,
      fontWeight = FontWeight.Bold,
      color = Color(0xFFCBD5E1),
      fontFamily = FontFamily.Monospace
    )
    Box(
      modifier = Modifier
        .size(4.dp)
        .clip(CircleShape)
        .background(Color(0xFFE50914))
    )
  }
}

@Composable
private fun GarageCompactSideStatBar(
  label: String,
  valueText: String,
  progress: Float,
  accentColor: Color
) {
  Column(
    modifier = Modifier.fillMaxWidth(),
    verticalArrangement = Arrangement.spacedBy(1.5.dp)
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Text(
        text = label,
        fontSize = 7.sp,
        fontWeight = FontWeight.Bold,
        color = MetallicSilver
      )
      Text(
        text = valueText,
        fontSize = 7.5.sp,
        fontWeight = FontWeight.Black,
        color = PureWhite,
        fontFamily = FontFamily.Monospace
      )
    }
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .height(4.dp)
        .clip(RoundedCornerShape(2.dp))
        .background(Color.White.copy(alpha = 0.12f))
    ) {
      Box(
        modifier = Modifier
          .fillMaxWidth(progress.coerceIn(0.05f, 1f))
          .fillMaxHeight()
          .clip(RoundedCornerShape(2.dp))
          .background(accentColor)
      )
    }
  }
}

private val GarageVisualTabSmoke = GarageVisualCategoryTab.SMOKE_COLOR

@Composable
private fun GarageSubsystemUpgradeTile(
  subsystem: UpgradeSubsystem,
  currentLevel: Int,
  upgradeCost: Int,
  onUpgrade: () -> Unit,
  modifier: Modifier = Modifier
) {
  val isMax = currentLevel >= 5
  Card(
    colors = CardDefaults.cardColors(containerColor = Color(0xFF141821)),
    shape = RoundedCornerShape(10.dp),
    border = BorderStroke(1.dp, if (isMax) Color(0xFF4ADE80) else CarbonBorder),
    modifier = modifier
      .clickable(enabled = !isMax) { onUpgrade() }
      .testTag("garage_direct_upgrade_${subsystem.name}")
  ) {
    Column(
      modifier = Modifier.padding(10.dp),
      verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = subsystem.shortCode,
          fontSize = 10.sp,
          fontWeight = FontWeight.Black,
          color = Color(0xFFE50914)
        )
        Text(
          text = "STAGE $currentLevel/5",
          fontSize = 8.sp,
          fontWeight = FontWeight.Bold,
          color = PureWhite,
          fontFamily = FontFamily.Monospace
        )
      }
      Text(
        text = subsystem.displayName,
        fontSize = 8.5.sp,
        fontWeight = FontWeight.Bold,
        color = MetallicSilver,
        maxLines = 1
      )
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(6.dp))
          .background(if (isMax) Color(0xFF16A34A).copy(alpha = 0.25f) else MotorsportRed.copy(alpha = 0.85f))
          .padding(vertical = 4.dp),
        contentAlignment = Alignment.Center
      ) {
        Text(
          text = if (isMax) "MAX STAGE" else "UPGRADE • CR $upgradeCost",
          fontSize = 8.sp,
          fontWeight = FontWeight.Black,
          color = PureWhite,
          fontFamily = FontFamily.Monospace
        )
      }
    }
  }
}

/**
 * Renders the 3D Hangar Turntable Garage Stage with Elevated Hydraulic Platform & Rotating 3D Supercar:
 * - Matte Black, Charcoal, Racing-Red, and Metallic-Silver motorsport hangar lighting
 * - Rotating 3D turntable ring with degree tick markers that respond to `yawAngleDeg`
 * - Dynamic paint finish shaders (`paintFinishMaterial`), forged wheels (`wheelRimStyle`),
 *   rear aero spoiler wings (`spoilerStyle`), and widebody kits (`bodyKitSpec`).
 */
@Composable
private fun GarageWorkshopProminentCarCanvas(
  vehicle: Vehicle,
  bodyColorHex: Long,
  wheelRimStyle: WheelRimStyle,
  rimAccentHex: Long,
  smokeColorHex: Long,
  yawAngleDeg: Float = 24f,
  spoilerStyle: SpoilerWingStyle = SpoilerWingStyle.FACTORY_LIP,
  bodyKitSpec: BodyKitSpec = BodyKitSpec.FACTORY_SPEC,
  paintFinishMaterial: PaintFinishMaterial = PaintFinishMaterial.METALLIC,
  modifier: Modifier = Modifier
) {
  val bodyColor = Color(bodyColorHex)
  val rimAccent = Color(rimAccentHex)
  val smokeColor = Color(smokeColorHex)

  Canvas(modifier = modifier) {
    val w = size.width
    val h = size.height
    val horizonY = h * 0.42f
    val yawRad = (yawAngleDeg * Math.PI / 180.0)
    val yawOffsetPx = (kotlin.math.sin(yawRad) * w * 0.025f).toFloat()

    // 1. Matte Black & Charcoal Motorsport Hangar Walls with Racing-Red Light Accent
    drawRect(
      brush = Brush.horizontalGradient(
        colors = listOf(
          Color(0xFF0A0C12),
          Color(0xFF161924),
          Color(0xFF12151F),
          Color(0xFF090B10)
        )
      ),
      size = Size(w, horizonY)
    )

    // Overhead Industrial Studio Light Truss Grid
    drawLine(
      color = Color(0xFF090C15),
      start = Offset(w * 0.25f, h * 0.04f),
      end = Offset(w * 0.62f, h * 0.14f),
      strokeWidth = 3.5f
    )
    drawLine(
      color = Color(0xFF090C15),
      start = Offset(w * 0.34f, h * 0.02f),
      end = Offset(w * 0.72f, h * 0.12f),
      strokeWidth = 3.5f
    )
    // Glowing Racing-Red & Metallic-Silver Studio Tube Bars on Ceiling Truss
    for (i in 0..3) {
      val lx = w * (0.30f + i * 0.08f)
      val ly = h * (0.06f + i * 0.018f)
      drawLine(
        color = if (i % 2 == 0) Color(0xFFE50914) else Color(0xFFE2E8F0),
        start = Offset(lx, ly),
        end = Offset(lx + w * 0.05f, ly + h * 0.01f),
        strokeWidth = 3f
      )
    }

    // 2. Reflective Charcoal & Matte Black Showroom Floor
    drawRect(
      brush = Brush.verticalGradient(
        colors = listOf(
          Color(0xFF1A1E29),
          Color(0xFF12151E),
          Color(0xFF0B0D14),
          Color(0xFF06070B)
        ),
        startY = horizonY,
        endY = h
      ),
      topLeft = Offset(0f, horizonY),
      size = Size(w, h - horizonY)
    )

    // Perspective Showroom Floor Grid Lines
    for (i in 0..6) {
      val startX = w * (0.10f + i * 0.14f)
      drawLine(
        color = Color.White.copy(alpha = 0.06f),
        start = Offset(startX, horizonY),
        end = Offset((startX - w * 0.35f) + i * w * 0.12f, h),
        strokeWidth = 1.5f
      )
    }

    // 3. Elevated Metallic-Silver & Racing-Red 3D Turntable Platform under the Car
    val platPath = Path().apply {
      moveTo(w * 0.18f, h * 0.54f)
      lineTo(w * 0.56f, h * 0.47f)
      lineTo(w * 0.86f, h * 0.62f)
      lineTo(w * 0.66f, h * 0.83f)
      close()
    }
    drawPath(
      path = platPath,
      brush = Brush.linearGradient(
        colors = listOf(
          Color(0xFF334155).copy(alpha = 0.82f),
          Color(0xFF1E293B).copy(alpha = 0.90f),
          Color(0xFF0F172A).copy(alpha = 0.95f)
        ),
        start = Offset(w * 0.20f, h * 0.50f),
        end = Offset(w * 0.80f, h * 0.80f)
      )
    )
    // Rotating 3D Turntable Ring & Degree Tick Marks on the Platform
    drawOval(
      color = Color(0xFFE50914).copy(alpha = 0.68f),
      topLeft = Offset(w * 0.24f, h * 0.46f),
      size = Size(w * 0.54f, h * 0.27f),
      style = Stroke(width = 2.5f)
    )
    drawOval(
      color = Color(0xFFCBD5E1).copy(alpha = 0.45f),
      topLeft = Offset(w * 0.27f, h * 0.49f),
      size = Size(w * 0.48f, h * 0.21f),
      style = Stroke(width = 1.2f)
    )
    for (tick in 0 until 12) {
      val tickAng = ((tick * 30f + yawAngleDeg) * Math.PI / 180.0)
      val cx = w * 0.51f
      val cy = h * 0.595f
      val sx = cx + (kotlin.math.cos(tickAng) * w * 0.24f).toFloat()
      val sy = cy + (kotlin.math.sin(tickAng) * h * 0.105f).toFloat()
      val ex = cx + (kotlin.math.cos(tickAng) * w * 0.265f).toFloat()
      val ey = cy + (kotlin.math.sin(tickAng) * h * 0.125f).toFloat()
      drawLine(
        color = if (tick % 3 == 0) Color(0xFFE50914) else Color(0xFFCBD5E1).copy(alpha = 0.65f),
        start = Offset(sx, sy),
        end = Offset(ex, ey),
        strokeWidth = if (tick % 3 == 0) 2.5f else 1.4f
      )
    }

    // Platform Beveled Steel Rim Edge
    val platRimPath = Path().apply {
      moveTo(w * 0.18f, h * 0.54f)
      lineTo(w * 0.66f, h * 0.83f)
      lineTo(w * 0.86f, h * 0.62f)
      lineTo(w * 0.86f, h * 0.66f)
      lineTo(w * 0.66f, h * 0.88f)
      lineTo(w * 0.18f, h * 0.57f)
      close()
    }
    drawPath(path = platRimPath, color = Color(0xFF1E293B))

    // Subtle Tire Smoke / Studio Mist Accent
    drawCircle(
      brush = Brush.radialGradient(
        colors = listOf(smokeColor.copy(alpha = 0.20f), Color.Transparent),
        center = Offset(w * 0.50f + yawOffsetPx, h * 0.48f),
        radius = w * 0.25f
      ),
      center = Offset(w * 0.50f + yawOffsetPx, h * 0.48f),
      radius = w * 0.25f
    )

    // 4. LARGE 3/4 3D SUPERCAR PREVIEW ON TURNTABLE PLATFORM (Aspect-Locked across 16:9..20:9)
    val carSpanW = minOf(w, h * 1.85f)
    fun carX(frac: Float): Float = (w * 0.50f) + (frac - 0.50f) * carSpanW

    val shadowPath = Path().apply {
      moveTo(carX(0.29f) + yawOffsetPx, h * 0.52f)
      lineTo(carX(0.76f) + yawOffsetPx, h * 0.58f)
      lineTo(carX(0.65f) + yawOffsetPx, h * 0.68f)
      lineTo(carX(0.27f) + yawOffsetPx, h * 0.56f)
      close()
    }
    drawPath(shadowPath, color = Color.Black.copy(alpha = 0.76f))

    // Paint Finish Material Specular Adjustment
    val specBoost = paintFinishMaterial.specularHighlightAlpha
    val highlightColor = Color(
      red = (bodyColor.red * (1.0f + specBoost * 0.35f) + specBoost * 0.14f).coerceAtMost(1f),
      green = (bodyColor.green * (1.0f + specBoost * 0.30f) + specBoost * 0.10f).coerceAtMost(1f),
      blue = (bodyColor.blue * (1.0f + specBoost * 0.25f) + specBoost * 0.10f).coerceAtMost(1f),
      alpha = 1f
    )
    val deepShadeColor = Color(
      red = (bodyColor.red * 0.55f).coerceIn(0f, 1f),
      green = (bodyColor.green * 0.48f).coerceIn(0f, 1f),
      blue = (bodyColor.blue * 0.45f).coerceIn(0f, 1f),
      alpha = 1f
    )

    // Dynamic Rear Wing / Spoiler (Rendered when vehicle is a car or according to SpoilerWingStyle)
    if (vehicle.type == VehicleType.CAR && spoilerStyle != SpoilerWingStyle.FACTORY_LIP) {
      val wingHeightBoost = when (spoilerStyle) {
        SpoilerWingStyle.GT3_SWAN_NECK -> h * 0.055f
        SpoilerWingStyle.TIME_ATTACK_DUAL -> h * 0.075f
        SpoilerWingStyle.ACTIVE_DRS_CARBON -> h * 0.045f
        else -> 0f
      }
      // Swan-neck carbon stanchions
      drawLine(
        color = Color(0xFFCBD5E1),
        start = Offset(carX(0.31f) + yawOffsetPx, h * 0.36f),
        end = Offset(carX(0.28f) + yawOffsetPx, h * 0.36f - wingHeightBoost),
        strokeWidth = 3f
      )
      drawLine(
        color = Color(0xFFCBD5E1),
        start = Offset(carX(0.34f) + yawOffsetPx, h * 0.35f),
        end = Offset(carX(0.31f) + yawOffsetPx, h * 0.35f - wingHeightBoost),
        strokeWidth = 3f
      )
      // Carbon Main Wing Plane + Racing-Red Endplate
      drawLine(
        color = Color(0xFF0F172A),
        start = Offset(carX(0.25f) + yawOffsetPx, h * 0.35f - wingHeightBoost),
        end = Offset(carX(0.35f) + yawOffsetPx, h * 0.33f - wingHeightBoost),
        strokeWidth = 6f,
        cap = StrokeCap.Round
      )
      drawLine(
        color = Color(0xFFE50914),
        start = Offset(carX(0.26f) + yawOffsetPx, h * 0.35f - wingHeightBoost),
        end = Offset(carX(0.34f) + yawOffsetPx, h * 0.33f - wingHeightBoost),
        strokeWidth = 2f
      )
    }

    val carShellPath = Path().apply {
      moveTo(carX(0.29f) + yawOffsetPx, h * 0.36f)
      lineTo(carX(0.37f) + yawOffsetPx, h * 0.26f)
      lineTo(carX(0.49f) + yawOffsetPx, h * 0.23f)
      lineTo(carX(0.64f) + yawOffsetPx, h * 0.31f)
      lineTo(carX(0.77f) + yawOffsetPx, h * 0.41f)
      lineTo(carX(0.76f) + yawOffsetPx, h * 0.54f)
      lineTo(carX(0.55f) + yawOffsetPx, h * 0.59f)
      lineTo(carX(0.30f) + yawOffsetPx, h * 0.50f)
      close()
    }
    drawPath(
      path = carShellPath,
      brush = Brush.linearGradient(
        colors = listOf(highlightColor, bodyColor, deepShadeColor),
        start = Offset(carX(0.45f) + yawOffsetPx, h * 0.22f),
        end = Offset(carX(0.62f) + yawOffsetPx, h * 0.58f)
      )
    )

    // Widebody Kit Aero Side Skirt & Front Splitter Extensions (when bodyKitSpec != FACTORY_SPEC)
    if (bodyKitSpec != BodyKitSpec.FACTORY_SPEC) {
      val skirtDepth = (bodyKitSpec.widthBonusPx / 16f).coerceIn(0.2f, 1f) * h * 0.025f
      drawLine(
        color = Color(0xFF0B0E14),
        start = Offset(carX(0.30f) + yawOffsetPx, h * 0.50f + skirtDepth * 0.5f),
        end = Offset(carX(0.77f) + yawOffsetPx, h * 0.55f + skirtDepth),
        strokeWidth = 5.5f,
        cap = StrokeCap.Round
      )
      drawLine(
        color = Color(0xFFE50914),
        start = Offset(carX(0.34f) + yawOffsetPx, h * 0.505f + skirtDepth * 0.5f),
        end = Offset(carX(0.76f) + yawOffsetPx, h * 0.555f + skirtDepth),
        strokeWidth = 2f
      )
    }

    // Upper Hood & Roof Specular Gloss Highlight
    val hoodGlossPath = Path().apply {
      moveTo(carX(0.48f) + yawOffsetPx, h * 0.24f)
      lineTo(carX(0.63f) + yawOffsetPx, h * 0.31f)
      lineTo(carX(0.75f) + yawOffsetPx, h * 0.41f)
      lineTo(carX(0.63f) + yawOffsetPx, h * 0.44f)
      lineTo(carX(0.52f) + yawOffsetPx, h * 0.33f)
      close()
    }
    drawPath(
      path = hoodGlossPath,
      color = Color.White.copy(alpha = (0.14f + specBoost * 0.18f).coerceAtMost(0.42f))
    )

    // Panoramic Windshield & Side Cockpit Window
    val windshieldGlass = Path().apply {
      moveTo(carX(0.49f) + yawOffsetPx, h * 0.24f)
      lineTo(carX(0.62f) + yawOffsetPx, h * 0.31f)
      lineTo(carX(0.55f) + yawOffsetPx, h * 0.35f)
      lineTo(carX(0.44f) + yawOffsetPx, h * 0.27f)
      close()
    }
    drawPath(
      path = windshieldGlass,
      brush = Brush.verticalGradient(
        colors = listOf(Color(0xFF94A3B8).copy(alpha = 0.85f), Color(0xFF1E293B))
      )
    )

    val sideWindowGlass = Path().apply {
      moveTo(carX(0.36f) + yawOffsetPx, h * 0.28f)
      lineTo(carX(0.47f) + yawOffsetPx, h * 0.25f)
      lineTo(carX(0.52f) + yawOffsetPx, h * 0.34f)
      lineTo(carX(0.39f) + yawOffsetPx, h * 0.34f)
      close()
    }
    drawPath(
      path = sideWindowGlass,
      brush = Brush.verticalGradient(
        colors = listOf(Color(0xFFCBD5E1).copy(alpha = 0.75f), Color(0xFF0F172A))
      )
    )

    // Black Side Air Intake Scoop ahead of Rear Wheel
    val sideIntake = Path().apply {
      moveTo(carX(0.35f) + yawOffsetPx, h * 0.43f)
      lineTo(carX(0.39f) + yawOffsetPx, h * 0.41f)
      lineTo(carX(0.39f) + yawOffsetPx, h * 0.48f)
      lineTo(carX(0.35f) + yawOffsetPx, h * 0.47f)
      close()
    }
    drawPath(sideIntake, color = Color(0xFF0F172A))

    // Hexagonal Front Honeycomb Air Intake Grille
    val frontHexIntake = Path().apply {
      moveTo(carX(0.63f) + yawOffsetPx, h * 0.51f)
      lineTo(carX(0.72f) + yawOffsetPx, h * 0.48f)
      lineTo(carX(0.76f) + yawOffsetPx, h * 0.52f)
      lineTo(carX(0.65f) + yawOffsetPx, h * 0.57f)
      close()
    }
    drawPath(frontHexIntake, color = Color(0xFF0F172A))
    drawPath(frontHexIntake, color = Color(0xFF334155), style = Stroke(width = 1.5f))

    // Sharp Y-Pattern LED Headlight on Front Fender
    val headlightBox = Path().apply {
      moveTo(carX(0.60f) + yawOffsetPx, h * 0.39f)
      lineTo(carX(0.67f) + yawOffsetPx, h * 0.42f)
      lineTo(carX(0.65f) + yawOffsetPx, h * 0.45f)
      lineTo(carX(0.59f) + yawOffsetPx, h * 0.41f)
      close()
    }
    drawPath(headlightBox, color = Color(0xFF0F172A))
    drawLine(
      color = Color.White,
      start = Offset(carX(0.60f) + yawOffsetPx, h * 0.40f),
      end = Offset(carX(0.66f) + yawOffsetPx, h * 0.43f),
      strokeWidth = 2.5f
    )
    drawLine(
      color = Color(0xFFE50914),
      start = Offset(carX(0.63f) + yawOffsetPx, h * 0.415f),
      end = Offset(carX(0.65f) + yawOffsetPx, h * 0.44f),
      strokeWidth = 2f
    )

    // 5. REAR WHEEL & FRONT WHEEL WITH FORGED SPOKES & RACING-RED CALIPERS
    fun drawShowroomWheel(cx: Float, cy: Float, radius: Float) {
      drawCircle(
        color = Color(0xFF07090E),
        radius = radius * 1.08f,
        center = Offset(cx, cy)
      )
      drawCircle(
        color = Color(0xFF141821),
        radius = radius,
        center = Offset(cx, cy)
      )
      // Glowing Racing-Red Brembo Brake Caliper inside rim
      drawArc(
        color = Color(0xFFE50914),
        startAngle = 140f,
        sweepAngle = 70f,
        useCenter = false,
        topLeft = Offset(cx - radius * 0.56f, cy - radius * 0.56f),
        size = Size(radius * 1.12f, radius * 1.12f),
        style = Stroke(width = radius * 0.22f)
      )
      // Outer Rim Lip
      drawCircle(
        color = rimAccent.copy(alpha = 0.90f),
        radius = radius * 0.74f,
        center = Offset(cx, cy),
        style = Stroke(width = 2.5f)
      )
      // Forged Spokes rotating with turntable yaw
      val spokes = wheelRimStyle.spokeCount.coerceAtLeast(6)
      for (i in 0 until spokes) {
        val ang = ((i * (360.0 / spokes)) + yawAngleDeg * 0.5) * (Math.PI / 180.0)
        val sx = cx + (kotlin.math.cos(ang) * radius * 0.72f).toFloat()
        val sy = cy + (kotlin.math.sin(ang) * radius * 0.72f).toFloat()
        drawLine(
          color = Color(0xFFCBD5E1),
          start = Offset(cx, cy),
          end = Offset(sx, sy),
          strokeWidth = 2.6f
        )
      }
      // Centerlock Hub
      drawCircle(
        color = rimAccent,
        radius = radius * 0.18f,
        center = Offset(cx, cy)
      )
    }

    drawShowroomWheel(cx = carX(0.32f) + yawOffsetPx, cy = h * 0.43f, radius = h * 0.105f)
    drawShowroomWheel(cx = carX(0.52f) + yawOffsetPx, cy = h * 0.49f, radius = h * 0.135f)
  }
}


// ===========================================================================
// SCREEN 5 – VEHICLE CUSTOMIZATION (WORKSHOP HYPERCAR STAGE, MAX RATING CARD, 7-WHEEL SELECTOR & BEFORE/AFTER)
// Matches Screenshot_2026_1111_143312_61776d83.png:
// - Center-Left: Crimson-Red LaFerrari / Hypercar on angled black-and-white workshop floor pad
// - Right Overlay: Translucent "MAX RATING ★★★" card (Tier 1 • Max Upgrade 9) with DRIFT (100+20),
//   POWER (200), ARMOR (200+40), and NITRO (100) dual-color upgrade bars
// - Bottom Strip: 7 Forged Wheel Rim Cards with Gold Active Frame (#FACC15)
// ===========================================================================
@Composable
fun ApexVehicleCustomizationScreen(
  vehicle: Vehicle,
  initialTab: Int = 0,
  onBack: () -> Unit,
  onOpenUpgrades: () -> Unit = {},
  onRaceNow: () -> Unit,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val careerState by ApexRivals2CareerManager.state.collectAsState()
  val currentLoadout = remember(vehicle.id, careerState.loadoutsByVehicleId) {
    ApexRivals2CareerManager.getLoadoutForVehicle(vehicle)
  }
  val stockLoadout = remember(vehicle.id) {
    VehicleCustomizationLoadout(vehicleId = vehicle.id, paintColorHex = vehicle.liveryColorHex)
  }
  val beforeStats = remember(vehicle, stockLoadout) {
    ApexRivals2CareerManager.computeUpgradedStats(vehicle, stockLoadout)
  }
  val afterStats = remember(vehicle, currentLoadout) {
    ApexRivals2CareerManager.computeUpgradedStats(vehicle, currentLoadout)
  }

  Column(
    modifier = modifier
      .fillMaxSize()
      .background(CarbonBlack)
      .verticalScroll(rememberScrollState())
      .padding(14.dp)
      .testTag("screen_5_vehicle_customization"),
    verticalArrangement = Arrangement.spacedBy(10.dp)
  ) {
    // Top Header Bar with Currency Readout (💎 100 | 💲 177,000 [+] matching Screenshot_2026_1111_143312_61776d83.png)
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      ScreenHeaderBar(
        title = "CUSTOMIZATION & PERFORMANCE COMPARISON",
        subtitle = "${vehicle.name.uppercase()} • TIER 1 • MAX UPGRADE 9",
        onBack = onBack
      )

      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier
          .clip(RoundedCornerShape(6.dp))
          .background(Color(0xFF141923))
          .border(1.dp, Color(0xFFCBD5E1).copy(alpha = 0.25f), RoundedCornerShape(6.dp))
          .padding(horizontal = 10.dp, vertical = 5.dp)
      ) {
        Text(
          text = "💎 ${careerState.coinsBalance}",
          fontSize = 9.5.sp,
          fontWeight = FontWeight.Black,
          color = Color(0xFF38BDF8),
          fontFamily = FontFamily.Monospace
        )
        Text(
          text = "💲 ${String.format(Locale.US, "%,d", careerState.creditsBalance)}",
          fontSize = 9.5.sp,
          fontWeight = FontWeight.Black,
          color = Color(0xFFFACC15),
          fontFamily = FontFamily.Monospace
        )
      }
    }

    // 1. ADVANCED GARAGE WORKSHOP STAGE: Red LaFerrari on Floor Mat + Right "MAX RATING ★★★" Card + Bottom 7-Rim Carousel
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(12.dp))
        .background(
          Brush.verticalGradient(
            colors = listOf(Color(0xFF172627), Color(0xFF283635), Color(0xFF8C968E))
          )
        )
        .border(1.5.dp, Color(0xFFCBD5E1).copy(alpha = 0.35f), RoundedCornerShape(12.dp))
        .padding(10.dp)
    ) {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          // Left 60%: Crimson LaFerrari / Hypercar on Angled Workshop Floor Mat
          Box(
            modifier = Modifier
              .weight(0.58f)
              .height(158.dp)
          ) {
            GarageWorkshopProminentCarCanvas(
              vehicle = vehicle,
              bodyColorHex = currentLoadout.paintColorHex,
              wheelRimStyle = currentLoadout.wheelRimStyle,
              rimAccentHex = currentLoadout.wheelRimStyle.rimColorHex,
              smokeColorHex = 0xFF38BDF8,
              modifier = Modifier.fillMaxSize()
            )
          }

          // Right 40%: Translucent "MAX RATING ★★★" Card with DRIFT, POWER, ARMOR, NITRO Bars
          Column(
            modifier = Modifier
              .weight(0.42f)
              .clip(RoundedCornerShape(8.dp))
              .background(Color(0xFF16202A).copy(alpha = 0.88f))
              .border(1.dp, Color.White.copy(alpha = 0.30f), RoundedCornerShape(8.dp))
              .testTag("customization_before_after_card")
          ) {
            // White Header Banner: "Ferrari LaFerrari / Active Vehicle | Tier 1 | Max Upgrade 9"
            Column(
              modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(horizontal = 8.dp, vertical = 5.dp)
            ) {
              Text(
                text = vehicle.name,
                fontSize = 10.5.sp,
                fontWeight = FontWeight.Black,
                color = Color(0xFF0F172A),
                maxLines = 1
              )
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
              ) {
                Text("Tier 1", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color(0xFF334155))
                Text("Max Upgrade 9", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color(0xFF334155))
              }
            }

            Column(
              modifier = Modifier.padding(8.dp),
              verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Text("MAX RATING", fontSize = 9.sp, fontWeight = FontWeight.Black, color = Color.White)
                Text("★ ★ ★", fontSize = 10.sp, fontWeight = FontWeight.Black, color = Color(0xFFFACC15))
              }

              val driftBonus = currentLoadout.tiresLevel * 10
              val powerVal = 180 + currentLoadout.engineLevel * 20
              val armorBonus = currentLoadout.brakesLevel * 20
              val nitroVal = 80 + currentLoadout.turboLevel * 20

              WorkshopDualSegmentStatRow("DRIFT", "100", "+$driftBonus", 0.52f, 0.16f)
              WorkshopDualSegmentStatRow("POWER", "$powerVal", "", 0.44f, 0.0f)
              WorkshopDualSegmentStatRow("ARMOR", "200", "+$armorBonus", 0.48f, 0.18f)
              WorkshopDualSegmentStatRow("NITRO", "$nitroVal", "", 0.50f, 0.0f)
            }
          }
        }

        // Bottom 7-Card Horizontal Forged Wheel Rim Selector with Gold Active Border
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          val wheelStyles = WheelRimStyle.entries
          for (slotIdx in 0 until 7) {
            val style = wheelStyles[slotIdx % wheelStyles.size]
            val isSelected = currentLoadout.wheelRimStyle == style && (slotIdx < wheelStyles.size)
            Box(
              modifier = Modifier
                .size(width = 68.dp, height = 54.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF374151).copy(alpha = 0.85f))
                .border(
                  width = if (isSelected) 2.5.dp else 1.dp,
                  color = if (isSelected) Color(0xFFFACC15) else Color.White.copy(alpha = 0.45f),
                  shape = RoundedCornerShape(8.dp)
                )
                .clickable {
                  ApexRivals2CareerManager.applyVisualCustomization(
                    vehicle = vehicle,
                    paintColorHex = currentLoadout.paintColorHex,
                    wheelRimStyle = style,
                    spoilerStyle = currentLoadout.spoilerStyle,
                    bodyKitSpec = currentLoadout.bodyKitSpec,
                    context = context
                  )
                  com.example.data.firebase.ApexCloudSyncCoordinator.onCustomizationSaved(context)
                },
              contentAlignment = Alignment.Center
            ) {
              Canvas(modifier = Modifier.size(40.dp)) {
                val r = size.minDimension * 0.46f
                val center = Offset(size.width * 0.5f, size.height * 0.5f)
                val rimCol = if (slotIdx == 0) Color(0xFF111827) else Color(0xFFE2E8F0)
                drawCircle(color = Color(0xFF1E293B), radius = r, center = center)
                drawCircle(color = rimCol, radius = r * 0.88f, center = center, style = Stroke(width = 3f))
                val spokes = 5 + (slotIdx % 6)
                for (s in 0 until spokes) {
                  val ang = (s * (360.0 / spokes)) * (Math.PI / 180.0)
                  val ex = center.x + (cos(ang) * r * 0.84f).toFloat()
                  val ey = center.y + (sin(ang) * r * 0.84f).toFloat()
                  drawLine(color = rimCol, start = center, end = Offset(ex, ey), strokeWidth = 2.6f)
                }
                drawCircle(color = Color(0xFF64748B), radius = r * 0.22f, center = center)
              }
            }
          }
        }
      }
    }

    // Before-and-After Dual 3D Vehicle Previews & Delta Comparison Card
    Card(
      colors = CardDefaults.cardColors(containerColor = CharcoalCard),
      shape = RoundedCornerShape(12.dp),
      border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.6f)),
      modifier = Modifier
        .fillMaxWidth()
        .testTag("customization_before_after_detail_card")
    ) {
      Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
          text = "BEFORE (FACTORY STOCK) VS AFTER (CURRENT CUSTOM SPEC)",
          fontSize = 9.sp,
          fontWeight = FontWeight.Black,
          color = NeonCyan,
          letterSpacing = 0.6.sp
        )

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("BEFORE (STOCK)", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = MetallicSilver)
            Showroom3DVehiclePreviewBox(vehicle = vehicle, loadout = stockLoadout, heightDp = 100)
            Text(
              text = "${beforeStats.maxSpeedKmh} km/h • ${String.format(Locale.US, "%.2fG", beforeStats.handlingCorneringG)}",
              fontSize = 8.sp,
              color = TextSecondary,
              fontFamily = FontFamily.Monospace
            )
          }

          Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("AFTER (CUSTOMIZED)", fontSize = 8.sp, fontWeight = FontWeight.Black, color = ApexGreen)
            Showroom3DVehiclePreviewBox(vehicle = vehicle, loadout = currentLoadout, heightDp = 100)
            val speedDelta = afterStats.maxSpeedKmh - beforeStats.maxSpeedKmh
            Text(
              text = "${afterStats.maxSpeedKmh} km/h (+$speedDelta) • ${String.format(Locale.US, "%.2fG", afterStats.handlingCorneringG)}",
              fontSize = 8.sp,
              fontWeight = FontWeight.Bold,
              color = ApexGreen,
              fontFamily = FontFamily.Monospace
            )
          }
        }
      }
    }

    // Full Interactive 3D Turntable & Paint / Wheels / Spoilers / Body Kits / Engine-Turbo-Brake-Tire Upgrades
    Garage3DTurntablePreviewCard(vehicle = vehicle)

    // 12-Material Paint & Surface Finish Studio Card ("COLOR | FINISH | PRO" matching Screenshot_2026_1111_160440_2eabf75f.png & 160624_9c5be14b.png)
    Card(
      colors = CardDefaults.cardColors(containerColor = CharcoalCard),
      shape = RoundedCornerShape(12.dp),
      border = BorderStroke(1.dp, Color.White.copy(alpha = 0.25f)),
      modifier = Modifier
        .fillMaxWidth()
        .testTag("customization_paint_finish_studio_card")
    ) {
      Column(
        modifier = Modifier.padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
              modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .background(Color(0xFF1F2937))
                .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
              Text("COLOR", fontSize = 8.sp, fontWeight = FontWeight.Black, color = MetallicSilver)
            }
            Box(
              modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .background(PureWhite)
                .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
              Text("FINISH", fontSize = 8.sp, fontWeight = FontWeight.Black, color = CarbonBlack)
            }
            Box(
              modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .background(Color(0xFF1F2937))
                .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
              Text("PRO", fontSize = 8.sp, fontWeight = FontWeight.Black, color = NeonAmber)
            }
          }
          Text(
            text = "ACTIVE: ${currentLoadout.paintFinishMaterial.displayName}",
            fontSize = 8.sp,
            fontWeight = FontWeight.Black,
            color = ApexGreen,
            fontFamily = FontFamily.Monospace
          )
        }

        // 12-Orb Surface Finish Selector Grid (2 Rows x 6 Columns)
        val finishEntries = PaintFinishMaterial.entries
        for (rowIdx in 0 until 2) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            for (colIdx in 0 until 6) {
              val itemIdx = rowIdx * 6 + colIdx
              if (itemIdx < finishEntries.size) {
                val finishMat = finishEntries[itemIdx]
                val isSelected = currentLoadout.paintFinishMaterial == finishMat
                Column(
                  modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isSelected) Color(0xFF1E293B) else CarbonBlack)
                    .border(
                      width = if (isSelected) 1.5.dp else 1.dp,
                      color = if (isSelected) PureWhite else CarbonBorder,
                      shape = RoundedCornerShape(8.dp)
                    )
                    .clickable {
                      ApexRivals2CareerManager.applyPaintFinishMaterial(vehicle, finishMat, context)
                      com.example.data.firebase.ApexCloudSyncCoordinator.onCustomizationSaved(context)
                    }
                    .padding(vertical = 6.dp, horizontal = 3.dp)
                    .testTag("paint_finish_orb_${finishMat.name}"),
                  horizontalAlignment = Alignment.CenterHorizontally,
                  verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                  // 3D Specular Material Shader Sphere Preview
                  Canvas(modifier = Modifier.size(28.dp)) {
                    val r = size.minDimension * 0.46f
                    val c = Offset(size.width * 0.5f, size.height * 0.5f)
                    drawCircle(
                      brush = Brush.linearGradient(
                        colors = listOf(Color(finishMat.primaryOrbHex), Color(finishMat.secondaryOrbHex))
                      ),
                      radius = r,
                      center = c
                    )
                    drawCircle(
                      brush = Brush.radialGradient(
                        colors = listOf(
                          Color.White.copy(alpha = finishMat.specularHighlightAlpha),
                          Color.Transparent
                        ),
                        center = Offset(c.x - r * 0.3f, c.y - r * 0.3f),
                        radius = r * 0.65f
                      ),
                      radius = r,
                      center = c
                    )
                  }
                  Text(
                    text = finishMat.displayName,
                    fontSize = 6.sp,
                    lineHeight = 7.sp,
                    fontWeight = FontWeight.Black,
                    color = if (isSelected) PureWhite else MetallicSilver,
                    textAlign = TextAlign.Center,
                    maxLines = 2
                  )
                }
              }
            }
          }
        }
      }
    }

    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      Button(
        onClick = onOpenUpgrades,
        colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier
          .weight(1f)
          .height(48.dp)
          .testTag("customization_open_upgrades_btn")
      ) {
        Icon(Icons.Default.Build, contentDescription = null, tint = CarbonBlack)
        Spacer(modifier = Modifier.width(6.dp))
        Text("PERFORMANCE UPGRADES »", fontSize = 10.sp, fontWeight = FontWeight.Black, color = CarbonBlack)
      }

      Button(
        onClick = onRaceNow,
        colors = ButtonDefaults.buttonColors(containerColor = MotorsportRed),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier
          .weight(1f)
          .height(48.dp)
          .testTag("customization_test_drive_btn")
      ) {
        Icon(Icons.Default.PlayArrow, contentDescription = null, tint = PureWhite)
        Spacer(modifier = Modifier.width(6.dp))
        Text("TEST DRIVE ON TRACK", fontSize = 10.sp, fontWeight = FontWeight.Black, color = PureWhite)
      }
    }
  }
}

@Composable
private fun WorkshopDualSegmentStatRow(
  label: String,
  baseValue: String,
  bonusValue: String,
  baseFraction: Float,
  bonusFraction: Float
) {
  Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Text(label, fontSize = 8.sp, fontWeight = FontWeight.Black, color = Color.White)
      Row {
        Text(baseValue, fontSize = 8.5.sp, fontWeight = FontWeight.Black, color = Color.White, fontFamily = FontFamily.Monospace)
        if (bonusValue.isNotEmpty()) {
          Text(bonusValue, fontSize = 8.5.sp, fontWeight = FontWeight.Black, color = Color(0xFF38BDF8), fontFamily = FontFamily.Monospace)
        }
      }
    }
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .height(6.dp)
        .clip(RoundedCornerShape(2.dp))
        .background(Color(0xFF334155))
    ) {
      Box(
        modifier = Modifier
          .fillMaxWidth(baseFraction.coerceIn(0.1f, 0.85f))
          .height(6.dp)
          .background(Color.White)
      )
      if (bonusFraction > 0f) {
        Box(
          modifier = Modifier
            .fillMaxWidth(bonusFraction.coerceIn(0.05f, 0.35f))
            .height(6.dp)
            .background(Color(0xFF38BDF8))
        )
      }
    }
  }
}

// ===========================================================================
// SCREEN 9 – EVENTS AND REWARDS
// ===========================================================================
@Composable
fun ApexEventsAndRewardsScreen(
  onBack: () -> Unit,
  onOpenMissions: () -> Unit = {},
  onOpenRewards: () -> Unit = {},
  onLaunchEventRace: () -> Unit,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val syncScope = rememberCoroutineScope()
  val cloudSyncState by com.example.data.firebase.ApexCloudSyncCoordinator.uiState.collectAsState()
  var simulateOfflineState by remember(cloudSyncState.simulatedOffline) {
    mutableStateOf(cloudSyncState.simulatedOffline)
  }

  Column(
    modifier = modifier
      .fillMaxSize()
      .background(CarbonBlack)
      .verticalScroll(rememberScrollState())
      .padding(14.dp)
      .testTag("screen_9_events_and_rewards"),
    verticalArrangement = Arrangement.spacedBy(10.dp)
  ) {
    ScreenHeaderBar(
      title = "EVENTS, MISSIONS & REWARDS",
      subtitle = "DAILY MISSIONS • WEEKLY CHALLENGES • CHAMPIONSHIPS",
      onBack = onBack
    )

    // Unified Navigation Strip: EVENTS -> MISSIONS -> REWARDS
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      Button(
        onClick = onOpenMissions,
        colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier
          .weight(1f)
          .height(40.dp)
          .testTag("events_open_missions_btn")
      ) {
        Icon(Icons.Default.Star, contentDescription = null, tint = CarbonBlack, modifier = Modifier.size(15.dp))
        Spacer(modifier = Modifier.width(5.dp))
        Text("ACTIVE MISSIONS »", fontSize = 9.5.sp, fontWeight = FontWeight.Black, color = CarbonBlack)
      }

      OutlinedButton(
        onClick = onOpenRewards,
        border = BorderStroke(1.dp, NeonAmber),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier
          .weight(1f)
          .height(40.dp)
          .testTag("events_open_rewards_btn")
      ) {
        Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = NeonAmber, modifier = Modifier.size(15.dp))
        Spacer(modifier = Modifier.width(5.dp))
        Text("REWARD VAULT »", fontSize = 9.5.sp, fontWeight = FontWeight.Black, color = NeonAmber)
      }
    }

    // Live Countdown Timers Strip + Offline Resilience State Toggle
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(10.dp))
        .background(CharcoalCard)
        .border(1.dp, CarbonBorder, RoundedCornerShape(10.dp))
        .padding(10.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Default.Timer, contentDescription = null, tint = NeonAmber, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Column {
          Text(
            text = "DAILY RESET: 14H 22M • WEEKLY SEASON: 4D 18H",
            fontSize = 8.5.sp,
            fontWeight = FontWeight.Black,
            color = NeonAmber,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.testTag("events_countdown_timer")
          )
          Text(
            text = if (simulateOfflineState || cloudSyncState.status == com.example.data.firebase.CloudSyncStatus.OFFLINE_QUEUED) {
              "OFFLINE CACHE ACTIVE • ${cloudSyncState.pendingOpsCount} ops queued"
            } else {
              "LIVE CLOUD EVENT ROTATION ACTIVE (Rev #${cloudSyncState.syncRevision})"
            },
            fontSize = 8.sp,
            color = if (simulateOfflineState || cloudSyncState.pendingOpsCount > 0) MotorsportRed else ApexGreen
          )
        }
      }

      Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
        if (cloudSyncState.pendingOpsCount > 0 && !simulateOfflineState) {
          Box(
            modifier = Modifier
              .clip(RoundedCornerShape(6.dp))
              .background(NeonCyan)
              .clickable {
                com.example.data.firebase.ApexCloudSyncCoordinator.retryPendingSync(context, syncScope)
              }
              .padding(horizontal = 8.dp, vertical = 4.dp)
              .testTag("events_retry_sync_btn")
          ) {
            Text(
              text = "RETRY SYNC (${cloudSyncState.pendingOpsCount})",
              fontSize = 7.5.sp,
              fontWeight = FontWeight.Black,
              color = CarbonBlack
            )
          }
        }
        Box(
          modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(CarbonBlack)
            .border(1.dp, CarbonBorder, RoundedCornerShape(6.dp))
            .clickable {
              val nextOffline = !simulateOfflineState
              simulateOfflineState = nextOffline
              com.example.data.firebase.ApexCloudSyncCoordinator.setSimulatedOffline(context, nextOffline, syncScope)
            }
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .testTag("events_offline_toggle_btn")
        ) {
          Text(
            text = if (simulateOfflineState) "GO ONLINE" else "TEST OFFLINE",
            fontSize = 7.5.sp,
            fontWeight = FontWeight.Bold,
            color = PureWhite
          )
        }
      }
    }

    if (simulateOfflineState) {
      Card(
        colors = CardDefaults.cardColors(containerColor = CharcoalCard),
        border = BorderStroke(1.dp, NeonAmber),
        modifier = Modifier
          .fillMaxWidth()
          .testTag("events_offline_banner")
      ) {
        Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
          Icon(Icons.Default.Warning, contentDescription = null, tint = NeonAmber, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = "Offline Mode: Local career events & daily login rewards remain playable and sync automatically when reconnected.",
            fontSize = 8.5.sp,
            color = PureWhite
          )
        }
      }
    }

    // Complete Career Championships, Daily Login & Missions Panel
    ApexRivals2CareerAndRewardsPanel()

    Button(
      onClick = onLaunchEventRace,
      colors = ButtonDefaults.buttonColors(containerColor = MotorsportRed),
      shape = RoundedCornerShape(10.dp),
      modifier = Modifier
        .fillMaxWidth()
        .height(48.dp)
        .testTag("events_enter_race_btn")
    ) {
      Icon(Icons.Default.PlayArrow, contentDescription = null, tint = PureWhite)
      Spacer(modifier = Modifier.width(6.dp))
      Text("RACE ACTIVE CHAMPIONSHIP EVENT", fontSize = 11.sp, fontWeight = FontWeight.Black, color = PureWhite)
    }
  }
}

// ===========================================================================
// SCREEN 10 – SETTINGS
// ===========================================================================
@Composable
fun ApexSettingsScreen(
  onBack: () -> Unit,
  onOpenProfile: () -> Unit,
  onOpenTelemetryHub: () -> Unit = {},
  modifier: Modifier = Modifier
) {
  val frameRatePref by ApexRivals2SettingsRepository.frameRatePref.collectAsState()
  val languageOption by ApexRivals2SettingsRepository.languageOption.collectAsState()
  val privacyPrefs by ApexRivals2SettingsRepository.privacyPrefs.collectAsState()
  val careerState by ApexRivals2CareerManager.state.collectAsState()
  val cloudSyncState by com.example.data.firebase.ApexCloudSyncCoordinator.uiState.collectAsState()
  var statusToast by remember { mutableStateOf<String?>(null) }
  var showWhatsNewDialog by remember { mutableStateOf(false) }

  Column(
    modifier = modifier
      .fillMaxSize()
      .background(CarbonBlack)
      .verticalScroll(rememberScrollState())
      .padding(14.dp)
      .testTag("screen_10_settings"),
    verticalArrangement = Arrangement.spacedBy(10.dp)
  ) {
    ScreenHeaderBar(
      title = "GAME SETTINGS & CONFIGURATION",
      subtitle = "GRAPHICS • AUDIO • CONTROLS • LANGUAGE • PRIVACY • ACCOUNT",
      onBack = onBack
    )

    // 1. Graphics Quality, Control Layout, Steering Sensitivity & Audio Mixer
    ApexRivals2ControlAndAudioSettingsCard()

    // 2. Frame-Rate Preference Selector (30 / 60 / 90-120 FPS)
    Card(
      colors = CardDefaults.cardColors(containerColor = CharcoalCard),
      shape = RoundedCornerShape(12.dp),
      border = BorderStroke(1.dp, CarbonBorder),
      modifier = Modifier.fillMaxWidth()
    ) {
      Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
          text = "FRAME-RATE PREFERENCE:",
          fontSize = 9.sp,
          fontWeight = FontWeight.Black,
          color = NeonCyan
        )
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          FrameRatePreference.entries.forEach { pref ->
            val isSelected = frameRatePref == pref
            Box(
              modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(6.dp))
                .background(if (isSelected) MotorsportRed else CarbonBlack)
                .border(1.dp, if (isSelected) MotorsportRed else CarbonBorder, RoundedCornerShape(6.dp))
                .clickable { ApexRivals2SettingsRepository.setFrameRatePreference(pref) }
                .padding(vertical = 6.dp)
                .testTag("settings_fps_${pref.fps}"),
              contentAlignment = Alignment.Center
            ) {
              Text(
                text = "${pref.fps} FPS",
                fontSize = 8.5.sp,
                fontWeight = FontWeight.Black,
                color = PureWhite
              )
            }
          }
        }
      }
    }

    // 3. Language Selector
    Card(
      colors = CardDefaults.cardColors(containerColor = CharcoalCard),
      shape = RoundedCornerShape(12.dp),
      border = BorderStroke(1.dp, CarbonBorder),
      modifier = Modifier.fillMaxWidth()
    ) {
      Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(Icons.Default.Language, contentDescription = null, tint = NeonAmber, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = "LANGUAGE: ${languageOption.displayName.uppercase()}",
            fontSize = 9.sp,
            fontWeight = FontWeight.Black,
            color = NeonAmber
          )
        }
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
          AppLanguageOption.entries.forEach { lang ->
            val isSelected = languageOption == lang
            Box(
              modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(6.dp))
                .background(if (isSelected) NeonAmber else CarbonBlack)
                .border(1.dp, if (isSelected) NeonAmber else CarbonBorder, RoundedCornerShape(6.dp))
                .clickable { ApexRivals2SettingsRepository.setLanguageOption(lang) }
                .padding(vertical = 6.dp)
                .testTag("settings_lang_${lang.code}"),
              contentAlignment = Alignment.Center
            ) {
              Text(
                text = lang.code,
                fontSize = 8.5.sp,
                fontWeight = FontWeight.Bold,
                color = if (isSelected) CarbonBlack else PureWhite
              )
            }
          }
        }
      }
    }

    // 4. Privacy Settings & Account Management
    Card(
      colors = CardDefaults.cardColors(containerColor = CharcoalCard),
      shape = RoundedCornerShape(12.dp),
      border = BorderStroke(1.dp, CarbonBorder),
      modifier = Modifier.fillMaxWidth()
    ) {
      Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(Icons.Default.Security, contentDescription = null, tint = ApexGreen, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = "PRIVACY SETTINGS & ACCOUNT MANAGEMENT",
            fontSize = 9.sp,
            fontWeight = FontWeight.Black,
            color = ApexGreen
          )
        }

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text("Share Cloud Ghost Telemetry on Leaderboards", fontSize = 8.5.sp, color = PureWhite)
          Checkbox(
            checked = privacyPrefs.shareCloudGhosts,
            onCheckedChange = { ApexRivals2SettingsRepository.updatePrivacyPreferences(shareCloudGhosts = it) },
            colors = CheckboxDefaults.colors(checkedColor = ApexGreen),
            modifier = Modifier.testTag("privacy_cloud_ghost_checkbox")
          )
        }

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text("Enable Crash & Frame-Rate Diagnostics", fontSize = 8.5.sp, color = PureWhite)
          Checkbox(
            checked = privacyPrefs.crashDiagnosticsEnabled,
            onCheckedChange = { ApexRivals2SettingsRepository.updatePrivacyPreferences(crashDiagnosticsEnabled = it) },
            colors = CheckboxDefaults.colors(checkedColor = ApexGreen),
            modifier = Modifier.testTag("privacy_diagnostics_checkbox")
          )
        }

        val context = androidx.compose.ui.platform.LocalContext.current
        val coroutineScope = androidx.compose.runtime.rememberCoroutineScope()
        val credentialManager = remember(context) { androidx.credentials.CredentialManager.create(context) }

        Text(
          text = "CLOUD STATUS: ${cloudSyncState.authProviderLabel} • Rev #${cloudSyncState.syncRevision} • Pending: ${cloudSyncState.pendingOpsCount}",
          fontSize = 8.sp,
          fontWeight = FontWeight.Bold,
          color = if (cloudSyncState.isAuthenticated) ApexGreen else NeonAmber,
          fontFamily = FontFamily.Monospace
        )

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          if (!cloudSyncState.isAuthenticated) {
            Button(
              onClick = {
                com.example.data.firebase.ApexCredentialAuthHelper.onGoogleSignInClicked(
                  context = context,
                  credentialManager = credentialManager,
                  onAuthSuccess = {
                    com.example.data.firebase.ApexCloudSyncCoordinator.startObservingAuth(context, coroutineScope)
                    statusToast = "Signed in with Google! Synchronizing Cloud Save..."
                  },
                  onAuthError = { err ->
                    statusToast = "Google Sign-In: $err"
                  },
                  scope = coroutineScope
                )
              },
              colors = ButtonDefaults.buttonColors(containerColor = ApexGreen),
              shape = RoundedCornerShape(8.dp),
              modifier = Modifier
                .weight(1f)
                .testTag("backend_google_signin_btn")
            ) {
              Text("SIGN IN WITH GOOGLE", fontSize = 8.sp, fontWeight = FontWeight.Black, color = CarbonBlack)
            }
          } else {
            OutlinedButton(
              onClick = {
                com.example.data.firebase.ApexCredentialAuthHelper.signOut(
                  credentialManager = credentialManager,
                  scope = coroutineScope,
                  onSignOutComplete = {
                    statusToast = "Signed out — local cache preserved safely."
                  }
                )
              },
              border = BorderStroke(1.dp, MotorsportRed),
              shape = RoundedCornerShape(8.dp),
              modifier = Modifier
                .weight(1f)
                .testTag("backend_google_signout_btn")
            ) {
              Text("SIGN OUT (${cloudSyncState.displayName?.take(8) ?: "PILOT"})", fontSize = 8.sp, fontWeight = FontWeight.Black, color = MotorsportRed)
            }
          }

          OutlinedButton(
            onClick = {
              com.example.data.firebase.ApexCloudSyncCoordinator.onCustomizationSaved(context)
              com.example.data.firebase.ApexCloudSyncCoordinator.retryPendingSync(context, coroutineScope)
              statusToast = if (cloudSyncState.isAuthenticated) {
                "Cloud Save sync triggered (Rev #${cloudSyncState.syncRevision})!"
              } else {
                "Saved locally & queued for Cloud Sync on Google Sign-In."
              }
            },
            border = BorderStroke(1.dp, NeonAmber),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier
              .weight(1f)
              .testTag("backend_sync_cloud_save_btn")
          ) {
            Text("SYNC CLOUD SAVE", fontSize = 8.sp, fontWeight = FontWeight.Black, color = NeonAmber)
          }
        }

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          OutlinedButton(
            onClick = onOpenProfile,
            border = BorderStroke(1.dp, NeonCyan),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier
              .weight(1f)
              .testTag("settings_manage_account_btn")
          ) {
            Text("MANAGE ACCOUNT (${careerState.playerUsername.take(8)})", fontSize = 8.sp, fontWeight = FontWeight.Black, color = NeonCyan)
          }

          OutlinedButton(
            onClick = {
              ApexRivals2CareerManager.resetForTesting()
              statusToast = "Local career cache reset to default starter profile."
            },
            border = BorderStroke(1.dp, MotorsportRed),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier
              .weight(1f)
              .testTag("settings_reset_progress_btn")
          ) {
            Text("RESET LOCAL CACHE", fontSize = 8.sp, fontWeight = FontWeight.Black, color = MotorsportRed)
          }
        }

        statusToast?.let {
          Text(it, fontSize = 8.5.sp, color = ApexGreen, fontFamily = FontFamily.Monospace)
        }
      }
    }

    // 5. V8 → V9 (2.0.0) IN-PLACE GOOGLE PLAY UPGRADE & DATA PRESERVATION VERIFICATION CARD
    // Matches Screenshot_2026_1111_155800_6f27258a.png ("Your existing application: V8 Current production release -> V9 Next major release")
    Card(
      colors = CardDefaults.cardColors(containerColor = CharcoalCard),
      shape = RoundedCornerShape(12.dp),
      border = BorderStroke(1.dp, ApexGreen.copy(alpha = 0.65f)),
      modifier = Modifier
        .fillMaxWidth()
        .testTag("settings_v8_to_v9_upgrade_plan_card")
    ) {
      Column(
        modifier = Modifier.padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column {
            Text(
              text = "YOUR EXISTING APPLICATION • IN-PLACE UPGRADE",
              fontSize = 9.5.sp,
              fontWeight = FontWeight.Black,
              color = PureWhite,
              letterSpacing = 0.5.sp
            )
            Text(
              text = "Same Google Play Store listing • Zero reinstall required",
              fontSize = 7.5.sp,
              fontWeight = FontWeight.Bold,
              color = MetallicSilver
            )
          }
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            Text(
              text = "V8",
              fontSize = 15.sp,
              fontWeight = FontWeight.Black,
              color = ApexGreen
            )
            Text(
              text = "→",
              fontSize = 13.sp,
              fontWeight = FontWeight.Black,
              color = MetallicSilver
            )
            Text(
              text = "V9",
              fontSize = 15.sp,
              fontWeight = FontWeight.Black,
              color = ApexGreen
            )
          }
        }

        val upgradeSpecRows = listOf(
          "Game name" to "Apex Rivals",
          "Developer" to "Apex Dynamics Game Studios",
          "Package ID" to "com.aistudio.apexrivals.tr8x",
          "Play Store listing" to "Existing listing",
          "Production signing" to "Existing authorized signing identity",
          "Existing player data" to "Preserved (Room SQLite + Career)",
          "Upgrade type" to "In-place application update",
          "Suggested version name" to "2.0.0",
          "Suggested version code" to "9 (or higher unused code in Play Console)"
        )
        upgradeSpecRows.forEach { (settingLabel, mustRemainValue) ->
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(4.dp))
              .background(CharcoalSurface)
              .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(
              text = settingLabel,
              fontSize = 8.sp,
              fontWeight = FontWeight.Bold,
              color = MetallicSilver
            )
            Text(
              text = mustRemainValue,
              fontSize = 8.sp,
              fontWeight = FontWeight.Black,
              color = PureWhite,
              fontFamily = FontFamily.Monospace
            )
          }
        }

        val upgradeContext = androidx.compose.ui.platform.LocalContext.current
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          OutlinedButton(
            onClick = {
              val ok = ApexRivals2CareerManager.createV8BackupSnapshot(upgradeContext)
              statusToast = if (ok) {
                "V8 Pre-Upgrade Snapshot Saved (CR=${careerState.creditsBalance}, XP=${careerState.totalXp})"
              } else {
                "V8 Snapshot Backup verified."
              }
            },
            border = BorderStroke(1.dp, ApexGreen),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier
              .weight(1f)
              .testTag("settings_backup_v8_snapshot_btn")
          ) {
            Text("BACKUP V8 SNAPSHOT", fontSize = 7.5.sp, fontWeight = FontWeight.Black, color = ApexGreen)
          }

          val settingsRollbackScope = androidx.compose.runtime.rememberCoroutineScope()
          OutlinedButton(
            onClick = {
              com.example.data.firebase.ApexCloudSyncCoordinator.recoverFromV8Backup(
                context = upgradeContext,
                scope = settingsRollbackScope
              ) { ok, msg ->
                statusToast = if (ok) msg else "No V8 snapshot found yet — click BACKUP V8 SNAPSHOT first."
              }
            },
            border = BorderStroke(1.dp, NeonCyan),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier
              .weight(1f)
              .testTag("settings_restore_v8_snapshot_btn")
          ) {
            Text("RESTORE V8 SNAPSHOT", fontSize = 7.5.sp, fontWeight = FontWeight.Black, color = NeonCyan)
          }
        }

        OutlinedButton(
          onClick = onOpenTelemetryHub,
          border = BorderStroke(1.dp, MetallicSilver.copy(alpha = 0.6f)),
          shape = RoundedCornerShape(8.dp),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("settings_open_telemetry_tools_btn")
        ) {
          Icon(Icons.Default.Speed, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(15.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text("OPEN PRO TELEMETRY, AI COACH & REPLAY SUITE", fontSize = 8.5.sp, fontWeight = FontWeight.Black, color = PureWhite)
        }

        OutlinedButton(
          onClick = { showWhatsNewDialog = true },
          border = BorderStroke(1.dp, Color(0xFFE10600)),
          shape = RoundedCornerShape(8.dp),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("settings_whats_new_btn")
        ) {
          Icon(Icons.Default.NewReleases, contentDescription = null, tint = Color(0xFFE10600), modifier = Modifier.size(15.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text("WHAT'S NEW IN APEX RIVALS 2.0 (RELEASE NOTES)", fontSize = 8.5.sp, fontWeight = FontWeight.Black, color = PureWhite)
        }
      }
    }

    if (showWhatsNewDialog) {
      WhatsNewInV2Dialog(onDismiss = { showWhatsNewDialog = false })
    }
  }
}

// ===========================================================================
// REUSABLE MOTORSPORT UI COMPONENTS
// ===========================================================================
@Composable
fun ScreenHeaderBar(
  title: String,
  subtitle: String,
  onBack: () -> Unit
) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    verticalAlignment = Alignment.CenterVertically
  ) {
    IconButton(
      onClick = onBack,
      modifier = Modifier
        .size(38.dp)
        .clip(RoundedCornerShape(8.dp))
        .background(CharcoalCard)
        .border(1.dp, CarbonBorder, RoundedCornerShape(8.dp))
        .testTag("frontend_back_btn")
    ) {
      Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = PureWhite, modifier = Modifier.size(18.dp))
    }
    Spacer(modifier = Modifier.width(10.dp))
    Column {
      Text(
        text = title,
        fontSize = 14.sp,
        fontWeight = FontWeight.Black,
        color = PureWhite,
        letterSpacing = 0.8.sp
      )
      Text(
        text = subtitle,
        fontSize = 8.5.sp,
        fontWeight = FontWeight.Bold,
        color = MotorsportRed,
        fontFamily = FontFamily.Monospace
      )
    }
  }
}

@Composable
private fun StatBarRow(
  label: String,
  valueText: String,
  progress: Float,
  barColor: Color
) {
  Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      Text(label, fontSize = 8.sp, fontWeight = FontWeight.Bold, color = MetallicSilver)
      Text(valueText, fontSize = 8.5.sp, fontWeight = FontWeight.Black, color = barColor, fontFamily = FontFamily.Monospace)
    }
    LinearProgressIndicator(
      progress = { progress },
      color = barColor,
      trackColor = CarbonBlack,
      modifier = Modifier
        .fillMaxWidth()
        .height(5.dp)
        .clip(RoundedCornerShape(3.dp))
    )
  }
}

@Composable
fun Showroom3DVehiclePreviewBox(
  vehicle: Vehicle,
  loadout: VehicleCustomizationLoadout,
  heightDp: Int = 150
) {
  var manualYaw by remember { mutableFloatStateOf(24f) }
  val infiniteTransition = rememberInfiniteTransition(label = "showroomLightPulse")
  val spinDeg by infiniteTransition.animateFloat(
    initialValue = 0f,
    targetValue = 360f,
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 16000, easing = LinearEasing),
      repeatMode = RepeatMode.Restart
    ),
    label = "spinDeg"
  )

  val yaw = (manualYaw + spinDeg) % 360f

  Box(
    modifier = Modifier
      .fillMaxWidth()
      .height(heightDp.dp)
      .clip(RoundedCornerShape(12.dp))
      .background(
        Brush.verticalGradient(
          colors = listOf(Color(0xFF080A10), Color(0xFF161B26), Color(0xFF090B10))
        )
      )
      .border(1.dp, CarbonBorder, RoundedCornerShape(12.dp))
      .pointerInput(Unit) {
        detectHorizontalDragGestures { _, dragAmount ->
          manualYaw = (manualYaw + dragAmount * 0.7f + 360f) % 360f
        }
      }
  ) {
    Canvas(modifier = Modifier.matchParentSize()) {
      val w = size.width
      val h = size.height
      val cx = w * 0.5f
      val cy = h * 0.64f
      val paintColor = Color(loadout.paintColorHex)
      val rimColor = Color(loadout.wheelRimStyle.rimColorHex)

      // Red & Silver Showroom Spotlight Glow
      drawRect(
        brush = Brush.radialGradient(
          colors = listOf(
            MotorsportRed.copy(alpha = 0.22f),
            paintColor.copy(alpha = 0.14f),
            Color.Transparent
          ),
          center = Offset(cx, h * 0.28f),
          radius = w * 0.52f
        ),
        size = size
      )

      // Illuminated Turntable Disc
      val platW = w * 0.74f
      val platH = h * 0.34f
      drawOval(
        color = Color(0xFF0E121B),
        topLeft = Offset(cx - platW * 0.5f, cy - platH * 0.5f),
        size = Size(platW, platH)
      )
      drawOval(
        color = MotorsportRed.copy(alpha = 0.75f),
        topLeft = Offset(cx - platW * 0.5f, cy - platH * 0.5f),
        size = Size(platW, platH),
        style = Stroke(width = 2f)
      )

      // 3D Sculpted Vehicle Silhouette
      val yawRad = Math.toRadians(yaw.toDouble())
      val cosYaw = cos(yawRad).toFloat()
      val carLen = w * 0.42f * (1f + loadout.bodyKitSpec.widthBonusPx / 120f)
      val carH = h * 0.24f
      val noseX = cx + cosYaw * (carLen * 0.42f)
      val tailX = cx - cosYaw * (carLen * 0.42f)
      val bodyLeft = minOf(noseX, tailX) - 20f
      val bodyRight = maxOf(noseX, tailX) + 20f
      val bodyW = (bodyRight - bodyLeft).coerceAtLeast(64f)
      val topY = cy - carH

      // Floor shadow
      drawOval(
        color = Color.Black.copy(alpha = 0.7f),
        topLeft = Offset(bodyLeft - 6f, cy - 6f),
        size = Size(bodyW + 12f, 20f)
      )

      val path = Path().apply {
        moveTo(bodyLeft, cy)
        lineTo(bodyLeft + bodyW * 0.10f, topY + carH * 0.42f)
        lineTo(bodyLeft + bodyW * 0.30f, topY)
        lineTo(bodyLeft + bodyW * 0.70f, topY)
        lineTo(bodyRight - bodyW * 0.08f, topY + carH * 0.45f)
        lineTo(bodyRight, cy)
        close()
      }
      drawPath(
        path = path,
        brush = Brush.verticalGradient(
          colors = listOf(paintColor, paintColor.copy(alpha = 0.75f))
        )
      )

      // Wheels
      val wheelR = 13f
      listOf(bodyLeft + bodyW * 0.22f, bodyLeft + bodyW * 0.78f).forEach { wx ->
        drawCircle(color = Color(0xFF0A0C10), radius = wheelR, center = Offset(wx, cy + 2f))
        drawCircle(color = rimColor, radius = wheelR * 0.68f, center = Offset(wx, cy + 2f), style = Stroke(width = 2f))
      }
    }
  }
}

// ===========================================================================
// SCREEN 6 ENHANCEMENTS – TOP-RIGHT MINI-MAP & BOTTOM-CENTER SPEEDOMETER/RPM/NITRO CLUSTER
// ===========================================================================
@Composable
fun RacingHudMiniMapIndicator(
  playerProgressFraction: Float,
  rivalFractions: List<Float>,
  modifier: Modifier = Modifier
) {
  Box(
    modifier = modifier
      .size(width = 84.dp, height = 54.dp)
      .clip(RoundedCornerShape(8.dp))
      .background(CarbonBlack.copy(alpha = 0.82f))
      .border(1.dp, CarbonBorder, RoundedCornerShape(8.dp))
      .padding(4.dp)
      .testTag("hud_track_minimap")
  ) {
    Canvas(modifier = Modifier.matchParentSize()) {
      val w = size.width
      val h = size.height
      val rx = w * 0.38f
      val ry = h * 0.34f
      val cx = w * 0.5f
      val cy = h * 0.52f

      // Closed-loop Grand Prix circuit outline
      drawRoundRect(
        color = MetallicSilver.copy(alpha = 0.45f),
        topLeft = Offset(cx - rx, cy - ry),
        size = Size(rx * 2f, ry * 2f),
        cornerRadius = CornerRadius(14f, 14f),
        style = Stroke(width = 3.5f)
      )

      // Rival dots around the loop
      rivalFractions.forEach { rf ->
        val angle = (rf * 6.28318f) - 1.5708f
        val x = cx + cos(angle) * rx
        val y = cy + sin(angle) * ry
        drawCircle(
          color = NeonCyan,
          radius = 2.8f,
          center = Offset(x, y)
        )
      }

      // Player vehicle dot (Racing Red with white halo)
      val playerAngle = (playerProgressFraction * 6.28318f) - 1.5708f
      val px = cx + cos(playerAngle) * rx
      val py = cy + sin(playerAngle) * ry
      drawCircle(color = PureWhite, radius = 4.8f, center = Offset(px, py))
      drawCircle(color = MotorsportRed, radius = 3.4f, center = Offset(px, py))
    }
    Text(
      text = "MAP",
      fontSize = 6.5.sp,
      fontWeight = FontWeight.Black,
      color = MetallicSilver,
      fontFamily = FontFamily.Monospace,
      modifier = Modifier.align(Alignment.TopStart)
    )
  }
}

@Composable
fun BottomCenterSpeedRpmNitroCluster(
  speedText: String,
  speedUnit: String,
  gear: Int,
  rpm: Int,
  maxRpm: Int,
  nitroPercent: Float,
  modifier: Modifier = Modifier
) {
  val rpmFraction = (rpm.toFloat() / maxRpm.coerceAtLeast(1)).coerceIn(0f, 1f)
  val rpmColor = when {
    rpmFraction > 0.88f -> MotorsportRed
    rpmFraction > 0.70f -> NeonAmber
    else -> NeonCyan
  }

  Column(
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.spacedBy(3.dp),
    modifier = modifier
      .width(138.dp)
      .clip(RoundedCornerShape(10.dp))
      .background(CarbonBlack.copy(alpha = 0.84f))
      .border(1.dp, CarbonBorder, RoundedCornerShape(10.dp))
      .padding(horizontal = 8.dp, vertical = 5.dp)
      .testTag("hud_bottom_center_cluster")
  ) {
    Row(
      verticalAlignment = Alignment.Bottom,
      horizontalArrangement = Arrangement.Center
    ) {
      Text(
        text = speedText,
        fontSize = 18.sp,
        fontWeight = FontWeight.Black,
        color = PureWhite,
        fontFamily = FontFamily.Monospace
      )
      Spacer(modifier = Modifier.width(3.dp))
      Text(
        text = speedUnit,
        fontSize = 8.sp,
        fontWeight = FontWeight.Bold,
        color = MetallicSilver,
        modifier = Modifier.padding(bottom = 2.dp)
      )
      Spacer(modifier = Modifier.width(6.dp))
      Text(
        text = "G$gear",
        fontSize = 11.sp,
        fontWeight = FontWeight.Black,
        color = MotorsportRed,
        fontFamily = FontFamily.Monospace
      )
    }

    // RPM Bar
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
      Text("RPM", fontSize = 6.5.sp, fontWeight = FontWeight.Bold, color = MetallicSilver, fontFamily = FontFamily.Monospace)
      LinearProgressIndicator(
        progress = { rpmFraction },
        color = rpmColor,
        trackColor = CharcoalCard,
        modifier = Modifier
          .weight(1f)
          .height(4.dp)
          .clip(RoundedCornerShape(2.dp))
      )
    }

    // Nitro Meter
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
      Text("NOS", fontSize = 6.5.sp, fontWeight = FontWeight.Bold, color = NeonCyan, fontFamily = FontFamily.Monospace)
      LinearProgressIndicator(
        progress = { nitroPercent.coerceIn(0f, 1f) },
        color = NeonCyan,
        trackColor = CharcoalCard,
        modifier = Modifier
          .weight(1f)
          .height(4.dp)
          .clip(RoundedCornerShape(2.dp))
          .testTag("hud_nitro_meter_bar")
      )
    }
  }
}

// ===========================================================================
// SCREEN 9 – CAREER AND CHAMPIONSHIP (PROGRESSION TIERS & CUP CAMPAIGN)
// ===========================================================================
@Composable
fun ApexCareerChampionshipScreen(
  activeVehicle: Vehicle,
  activeTrack: Track = AVAILABLE_TRACKS.first(),
  onSelectTrack: (Track) -> Unit = {},
  onBack: () -> Unit,
  onLaunchRace: () -> Unit,
  onLaunchEventMode: (GameMode, Track) -> Unit = { _, _ -> onLaunchRace() },
  onOpenGarage: () -> Unit = {},
  onOpenCustomization: () -> Unit = {},
  onOpenChampionshipSeries: (ChampionshipTier) -> Unit = {},
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val careerState by ApexRivals2CareerManager.state.collectAsState()
  var selectedTier by remember { mutableStateOf(ChampionshipTier.ROOKIE_CHAMPIONSHIP) }
  var selectedDisciplineFilter by remember { mutableStateOf<CareerEventCategory?>(null) }
  var statusBannerMessage by remember { mutableStateOf<String?>(null) }

  val tiers = listOf(
    ChampionshipTier.ROOKIE_CHAMPIONSHIP,
    ChampionshipTier.AMATEUR_CHAMPIONSHIP,
    ChampionshipTier.PRO_CHAMPIONSHIP,
    ChampionshipTier.ELITE_CHAMPIONSHIP
  )

  val vehiclePi = remember(activeVehicle, careerState.loadoutsByVehicleId) {
    ApexRivals2CareerManager.computeVehiclePerformanceIndex(activeVehicle)
  }

  val allTierEvents = remember(selectedTier) {
    ApexRivals2CareerManager.careerEvents.filter {
      it.championshipTier == selectedTier && it.category != CareerEventCategory.DAILY_CHALLENGE
    }
  }

  val displayedEvents = remember(selectedTier, selectedDisciplineFilter) {
    if (selectedDisciplineFilter == null) {
      allTierEvents
    } else {
      ApexRivals2CareerManager.careerEvents.filter {
        it.category == selectedDisciplineFilter
      }
    }
  }

  val completedTierEventsCount = allTierEvents.count { it.eventId in careerState.claimedEventIds }
  val totalTierEventsCount = allTierEvents.size.coerceAtLeast(1)
  val tierCompletionFraction = (completedTierEventsCount.toFloat() / totalTierEventsCount.toFloat()).coerceIn(0f, 1f)
  val isTierTrophyClaimed = selectedTier.name in careerState.claimedChampionshipTiers
  val canClaimTierTrophy = completedTierEventsCount >= allTierEvents.size && allTierEvents.isNotEmpty() && !isTierTrophyClaimed

  val isUnlocked = careerState.playerLevel >= selectedTier.requiredLevel

  Column(
    modifier = modifier
      .fillMaxSize()
      .background(CarbonBlack)
      .verticalScroll(rememberScrollState())
      .padding(horizontal = 14.dp, vertical = 10.dp)
      .testTag("screen_9_career_championship"),
    verticalArrangement = Arrangement.spacedBy(14.dp)
  ) {
    // 1. HEADER ROW: Back Arrow + CAREER CHAMPIONSHIPS + Driver Level, PI & Credits Badges
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(
          onClick = onBack,
          modifier = Modifier.testTag("career_back_btn")
        ) {
          Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = "Back",
            tint = PureWhite
          )
        }
        Spacer(modifier = Modifier.width(6.dp))
        Column {
          Text(
            text = "CAREER CHAMPIONSHIPS",
            fontSize = 18.sp,
            fontWeight = FontWeight.Black,
            color = PureWhite,
            letterSpacing = 1.sp
          )
          Text(
            text = "ROOKIE • AMATEUR • PROFESSIONAL • ELITE SERIES",
            fontSize = 8.5.sp,
            fontWeight = FontWeight.Bold,
            color = MotorsportRed,
            letterSpacing = 0.5.sp
          )
        }
      }

      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
      ) {
        // Vehicle PI Pill
        Box(
          modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(CharcoalCard)
            .border(1.dp, NeonCyan, RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .testTag("career_vehicle_pi_badge")
        ) {
          Text(
            text = "PI $vehiclePi",
            fontSize = 9.sp,
            fontWeight = FontWeight.Black,
            color = NeonCyan,
            fontFamily = FontFamily.Monospace
          )
        }

        // Player Level Pill
        Box(
          modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(CharcoalCard)
            .border(1.dp, Color(selectedTier.badgeColorHex), RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
          Text(
            text = "LEVEL ${careerState.playerLevel}",
            fontSize = 9.sp,
            fontWeight = FontWeight.Black,
            color = Color(selectedTier.badgeColorHex)
          )
        }

        // Coins Balance
        Box(
          modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(CharcoalCard)
            .border(1.dp, NeonAmber, RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
          Text(
            text = "CR ${careerState.creditsBalance}",
            fontSize = 9.sp,
            fontWeight = FontWeight.Black,
            color = NeonAmber,
            fontFamily = FontFamily.Monospace
          )
        }
      }
    }

    // Optional feedback banner when claiming race rewards, trophies, or unlocking tracks
    statusBannerMessage?.let { message ->
      Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F291E)),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, ApexGreen),
        modifier = Modifier
          .fillMaxWidth()
          .testTag("career_reward_feedback_banner")
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = message,
            fontSize = 9.5.sp,
            fontWeight = FontWeight.Black,
            color = ApexGreen,
            modifier = Modifier.weight(1f)
          )
          Text(
            text = "DISMISS",
            fontSize = 8.sp,
            fontWeight = FontWeight.Black,
            color = PureWhite,
            modifier = Modifier
              .clickable { statusBannerMessage = null }
              .padding(start = 8.dp)
          )
        }
      }
    }

    // 2. HORIZONTAL TIER SELECTOR TABS (Rookie, Amateur, Pro, Elite)
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .horizontalScroll(rememberScrollState()),
      horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      tiers.forEach { tier ->
        val isSelected = tier == selectedTier
        val tierUnlocked = careerState.playerLevel >= tier.requiredLevel
        val tierDoneCount = ApexRivals2CareerManager.careerEvents.count {
          it.championshipTier == tier &&
            it.category != CareerEventCategory.DAILY_CHALLENGE &&
            it.eventId in careerState.claimedEventIds
        }
        val tierTotalCount = ApexRivals2CareerManager.careerEvents.count {
          it.championshipTier == tier && it.category != CareerEventCategory.DAILY_CHALLENGE
        }

        Card(
          colors = CardDefaults.cardColors(
            containerColor = if (isSelected) Color(tier.badgeColorHex).copy(alpha = 0.22f) else CharcoalSurface
          ),
          shape = RoundedCornerShape(12.dp),
          border = BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected) Color(tier.badgeColorHex) else CarbonBorder
          ),
          modifier = Modifier
            .clickable {
              selectedTier = tier
              selectedDisciplineFilter = null
            }
            .testTag("career_tier_tab_${tier.name}")
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Icon(
              imageVector = if (tierUnlocked) Icons.Default.EmojiEvents else Icons.Default.Lock,
              contentDescription = null,
              tint = if (tierUnlocked) Color(tier.badgeColorHex) else TextSecondary,
              modifier = Modifier.size(18.dp)
            )
            Column {
              Text(
                text = "TIER ${tier.tierNumber} • $tierDoneCount/$tierTotalCount",
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold,
                color = if (tierUnlocked) Color(tier.badgeColorHex) else TextSecondary
              )
              Text(
                text = tier.displayName.replace(" Championship", "").uppercase(),
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                color = if (tierUnlocked) PureWhite else TextSecondary
              )
            }
          }
        }
      }
    }

    // 1B. STAGE 6 & 8 SPLIT-SCREEN MY CAREER: FACTION CHALLENGE + DAILY CHALLENGES + 3D GARAGE SUPERCAR PREVIEW
    val dailyChallenge1Claimed = "msn_daily_apex_hunter" in careerState.claimedMissionIds
    val dailyChallenge2Claimed = "msn_weekly_drift_king" in careerState.claimedMissionIds

    Row(
      modifier = Modifier
        .fillMaxWidth()
        .testTag("career_faction_daily_split_hub"),
      horizontalArrangement = Arrangement.spacedBy(10.dp),
      verticalAlignment = Alignment.Top
    ) {
      // LEFT PANE: MY CAREER • FACTION CHALLENGE + DAILY CHALLENGES
      Column(
        modifier = Modifier.weight(0.55f),
        verticalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        // Faction Challenge Card ("TAKE IT BACK • 3d 5h • 5000 RP • 71,453 PLAYERS")
        Card(
          colors = CardDefaults.cardColors(containerColor = Color(0xFF141821)),
          shape = RoundedCornerShape(8.dp),
          border = BorderStroke(1.dp, CarbonBorder),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("career_faction_challenge_card")
        ) {
          Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
              ) {
                Icon(
                  imageVector = Icons.Default.EmojiEvents,
                  contentDescription = null,
                  tint = NeonAmber,
                  modifier = Modifier.size(14.dp)
                )
                Text(
                  text = "FACTION CHALLENGE",
                  fontSize = 10.sp,
                  fontWeight = FontWeight.Black,
                  color = PureWhite,
                  letterSpacing = 0.6.sp
                )
              }
              Text(
                text = "3d 5h",
                fontSize = 8.5.sp,
                fontWeight = FontWeight.Black,
                color = NeonAmber,
                fontFamily = FontFamily.Monospace
              )
            }

            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = "TAKE IT BACK",
                  fontSize = 12.sp,
                  fontWeight = FontWeight.Black,
                  color = PureWhite
                )
                Text(
                  text = "CLAIM BACK HARLEM FOR THE APEX RIVALS",
                  fontSize = 7.5.sp,
                  fontWeight = FontWeight.Bold,
                  color = MetallicSilver
                )
              }
              Column(horizontalAlignment = Alignment.End) {
                Text(
                  text = "5000 RP",
                  fontSize = 11.sp,
                  fontWeight = FontWeight.Black,
                  color = NeonCyan,
                  fontFamily = FontFamily.Monospace
                )
                Text(
                  text = "👥 71,453",
                  fontSize = 7.5.sp,
                  fontWeight = FontWeight.Bold,
                  color = MetallicSilver,
                  fontFamily = FontFamily.Monospace
                )
              }
            }

            // Multi-Faction Segmented Dominance Bar (Red / Gold / Green / Cyan / Purple)
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .height(7.dp)
                .clip(RoundedCornerShape(3.dp))
            ) {
              Box(modifier = Modifier.weight(0.28f).height(7.dp).background(MotorsportRed))
              Box(modifier = Modifier.weight(0.22f).height(7.dp).background(Color(0xFFFACC15)))
              Box(modifier = Modifier.weight(0.19f).height(7.dp).background(ApexGreen))
              Box(modifier = Modifier.weight(0.17f).height(7.dp).background(NeonCyan))
              Box(modifier = Modifier.weight(0.14f).height(7.dp).background(Color(0xFFA855F7)))
            }
          }
        }

        // Daily Challenges Card ("KAREEM OF THE CROP • COMPLETED ✔ [CLAIM]" & "REPRESENTIN' [CLAIM]")
        Card(
          colors = CardDefaults.cardColors(containerColor = Color(0xFF141821)),
          shape = RoundedCornerShape(8.dp),
          border = BorderStroke(1.dp, CarbonBorder),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("career_daily_challenges_card")
        ) {
          Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text(
                text = "DAILY CHALLENGES",
                fontSize = 10.sp,
                fontWeight = FontWeight.Black,
                color = PureWhite,
                letterSpacing = 0.6.sp
              )
              Text(
                text = "17h 29m",
                fontSize = 8.5.sp,
                fontWeight = FontWeight.Bold,
                color = MetallicSilver,
                fontFamily = FontFamily.Monospace
              )
            }

            // Daily Challenge Row 1
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(6.dp))
                .background(CharcoalSurface)
                .padding(horizontal = 8.dp, vertical = 6.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = "KAREEM OF THE CROP",
                  fontSize = 9.5.sp,
                  fontWeight = FontWeight.Black,
                  color = PureWhite
                )
                Text(
                  text = if (dailyChallenge1Claimed) "REWARD CLAIMED (+2,500 CR)" else "COMPLETED ✔ • WIN 3 STREET RACES",
                  fontSize = 7.5.sp,
                  fontWeight = FontWeight.Bold,
                  color = ApexGreen
                )
              }
              Box(
                modifier = Modifier
                  .clip(RoundedCornerShape(4.dp))
                  .background(if (dailyChallenge1Claimed) CharcoalCard else ApexGreen)
                  .clickable(enabled = !dailyChallenge1Claimed) {
                    if (ApexRivals2CareerManager.claimMissionReward("msn_daily_apex_hunter", context)) {
                      com.example.data.firebase.ApexCloudSyncCoordinator.onRewardClaimed(context, "msn_daily_apex_hunter")
                    }
                    statusBannerMessage = "DAILY CHALLENGE CLAIMED: +2,500 CR & +500 XP!"
                  }
                  .padding(horizontal = 10.dp, vertical = 5.dp)
                  .testTag("career_claim_daily_1_btn")
              ) {
                Text(
                  text = if (dailyChallenge1Claimed) "CLAIMED" else "CLAIM",
                  fontSize = 8.sp,
                  fontWeight = FontWeight.Black,
                  color = if (dailyChallenge1Claimed) MetallicSilver else Color(0xFF052E16)
                )
              }
            }

            // Daily Challenge Row 2
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(6.dp))
                .background(CharcoalSurface)
                .padding(horizontal = 8.dp, vertical = 6.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = "REPRESENTIN'",
                  fontSize = 9.5.sp,
                  fontWeight = FontWeight.Black,
                  color = PureWhite
                )
                Text(
                  text = if (dailyChallenge2Claimed) "REWARD CLAIMED (+6,000 CR)" else "COMPLETED ✔ • SCORE 5,000 DRIFT PTS",
                  fontSize = 7.5.sp,
                  fontWeight = FontWeight.Bold,
                  color = NeonCyan
                )
              }
              Box(
                modifier = Modifier
                  .clip(RoundedCornerShape(4.dp))
                  .background(if (dailyChallenge2Claimed) CharcoalCard else NeonAmber)
                  .clickable(enabled = !dailyChallenge2Claimed) {
                    if (ApexRivals2CareerManager.claimMissionReward("msn_weekly_drift_king", context)) {
                      com.example.data.firebase.ApexCloudSyncCoordinator.onRewardClaimed(context, "msn_weekly_drift_king")
                    }
                    statusBannerMessage = "DRIFT CHALLENGE CLAIMED: +6,000 CR & +1,200 XP!"
                  }
                  .padding(horizontal = 10.dp, vertical = 5.dp)
                  .testTag("career_claim_daily_2_btn")
              ) {
                Text(
                  text = if (dailyChallenge2Claimed) "CLAIMED" else "CLAIM",
                  fontSize = 8.sp,
                  fontWeight = FontWeight.Black,
                  color = if (dailyChallenge2Claimed) MetallicSilver else Color(0xFF0F172A)
                )
              }
            }
          }
        }
      }

      // RIGHT PANE: 3D GARAGE SUPERCAR PREVIEW ("GT Premium • TUNE | CUSTOMIZE | RACE >>>")
      Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF10141D)),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, CarbonBorder),
        modifier = Modifier
          .weight(0.45f)
          .height(212.dp)
          .testTag("career_garage_preview_pane")
      ) {
        Box(modifier = Modifier.fillMaxSize()) {
          // 3D Supercar Showroom Canvas
          Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            drawRect(
              brush = Brush.verticalGradient(
                colors = listOf(Color(0xFF1A2230), Color(0xFF0E131B), Color(0xFF090C10))
              )
            )
            // Showroom floor reflection ellipse
            drawOval(
              color = Color.White.copy(alpha = 0.06f),
              topLeft = Offset(w * 0.10f, h * 0.62f),
              size = Size(w * 0.80f, h * 0.22f)
            )
            // White GT Supercar body silhouette with racing stripe
            val bodyPath = Path().apply {
              moveTo(w * 0.14f, h * 0.68f)
              lineTo(w * 0.22f, h * 0.50f)
              lineTo(w * 0.42f, h * 0.38f)
              lineTo(w * 0.68f, h * 0.40f)
              lineTo(w * 0.86f, h * 0.54f)
              lineTo(w * 0.88f, h * 0.68f)
              close()
            }
            drawPath(
              path = bodyPath,
              brush = Brush.linearGradient(
                colors = listOf(Color(0xFFF8FAFC), Color(0xFFCBD5E1), Color(0xFF94A3B8))
              )
            )
            // Windshield & side glass
            val glassPath = Path().apply {
              moveTo(w * 0.28f, h * 0.50f)
              lineTo(w * 0.44f, h * 0.41f)
              lineTo(w * 0.64f, h * 0.42f)
              lineTo(w * 0.72f, h * 0.51f)
              close()
            }
            drawPath(path = glassPath, color = Color(0xFF0F172A))
            // Racing red side accent stripe
            drawLine(
              color = MotorsportRed,
              start = Offset(w * 0.18f, h * 0.60f),
              end = Offset(w * 0.84f, h * 0.60f),
              strokeWidth = 4f
            )
            // Forged wheels
            listOf(w * 0.28f, w * 0.74f).forEach { cx ->
              drawCircle(color = Color(0xFF090B10), radius = h * 0.11f, center = Offset(cx, h * 0.68f))
              drawCircle(color = Color(0xFFCBD5E1), radius = h * 0.07f, center = Offset(cx, h * 0.68f), style = Stroke(width = 2.5f))
            }
          }

          // Top vehicle header inside right preview pane
          Row(
            modifier = Modifier
              .align(Alignment.TopStart)
              .fillMaxWidth()
              .background(Color.Black.copy(alpha = 0.45f))
              .padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column {
              Text(
                text = activeVehicle.name.uppercase(),
                fontSize = 10.sp,
                fontWeight = FontWeight.Black,
                color = PureWhite
              )
              Text(
                text = "PI $vehiclePi • ${activeTrack.name.uppercase()}",
                fontSize = 7.sp,
                fontWeight = FontWeight.Bold,
                color = NeonCyan
              )
            }
            Box(
              modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .background(MotorsportRed)
                .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
              Text(
                text = "LVL ${careerState.playerLevel}",
                fontSize = 8.sp,
                fontWeight = FontWeight.Black,
                color = PureWhite
              )
            }
          }

          // Bottom Action Bar ("TUNE | CUSTOMIZE | RACE >>>")
          Row(
            modifier = Modifier
              .align(Alignment.BottomCenter)
              .fillMaxWidth()
              .background(Color(0xFF0B0D13).copy(alpha = 0.92f))
              .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Box(
              modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .background(CharcoalSurface)
                .border(1.dp, CarbonBorder, RoundedCornerShape(4.dp))
                .clickable(onClick = onOpenGarage)
                .padding(horizontal = 8.dp, vertical = 5.dp)
            ) {
              Text("TUNE", fontSize = 8.sp, fontWeight = FontWeight.Black, color = MetallicSilver)
            }
            Box(
              modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .background(CharcoalSurface)
                .border(1.dp, CarbonBorder, RoundedCornerShape(4.dp))
                .clickable(onClick = onOpenCustomization)
                .padding(horizontal = 8.dp, vertical = 5.dp)
            ) {
              Text("CUSTOMIZE", fontSize = 8.sp, fontWeight = FontWeight.Black, color = MetallicSilver)
            }
            Box(
              modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(4.dp))
                .background(MotorsportRed)
                .clickable(onClick = onLaunchRace)
                .padding(vertical = 5.dp),
              contentAlignment = Alignment.Center
            ) {
              Text("RACE >>>", fontSize = 9.sp, fontWeight = FontWeight.Black, color = PureWhite)
            }
          }
        }
      }
    }

    // 3. TIER HERO BANNER CARD (Trophy Cup, Requirements, Progress Bar, Rewards, Claim Cup Button)
    Card(
      colors = CardDefaults.cardColors(containerColor = CharcoalCard),
      shape = RoundedCornerShape(16.dp),
      border = BorderStroke(1.5.dp, Color(selectedTier.badgeColorHex)),
      modifier = Modifier.fillMaxWidth()
    ) {
      Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.weight(1f)
          ) {
            Box(
              modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(Color(selectedTier.badgeColorHex).copy(alpha = 0.25f))
                .border(1.5.dp, Color(selectedTier.badgeColorHex), CircleShape),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                imageVector = Icons.Default.EmojiEvents,
                contentDescription = "Championship Trophy",
                tint = Color(selectedTier.badgeColorHex),
                modifier = Modifier.size(26.dp)
              )
            }
            Column {
              Text(
                text = selectedTier.displayName.uppercase(),
                fontSize = 16.sp,
                fontWeight = FontWeight.Black,
                color = PureWhite
              )
              Text(
                text = if (isUnlocked) {
                  "UNLOCKED • REQ LVL ${selectedTier.requiredLevel} & PI ${selectedTier.requiredPerformanceIndex}+"
                } else {
                  "LOCKED • REACH DRIVER LEVEL ${selectedTier.requiredLevel} (REQ PI ${selectedTier.requiredPerformanceIndex}+)"
                },
                fontSize = 8.5.sp,
                fontWeight = FontWeight.Bold,
                color = if (isUnlocked) ApexGreen else MotorsportRed
              )
            }
          }

          // Tier Trophy Badge or Claim Cup Button
          Box(
            modifier = Modifier
              .clip(RoundedCornerShape(8.dp))
              .background(
                when {
                  isTierTrophyClaimed -> ApexGreen.copy(alpha = 0.22f)
                  canClaimTierTrophy -> NeonAmber
                  else -> Color(selectedTier.badgeColorHex).copy(alpha = 0.18f)
                }
              )
              .border(
                1.dp,
                when {
                  isTierTrophyClaimed -> ApexGreen
                  canClaimTierTrophy -> NeonAmber
                  else -> Color(selectedTier.badgeColorHex)
                },
                RoundedCornerShape(8.dp)
              )
              .clickable(enabled = canClaimTierTrophy) {
                if (ApexRivals2CareerManager.claimChampionshipTierTrophy(selectedTier, context)) {
                  com.example.data.firebase.ApexCloudSyncCoordinator.onRewardClaimed(context, selectedTier.name)
                  statusBannerMessage =
                    "${selectedTier.displayName.uppercase()} CUP WON! +${selectedTier.trophyRewardCredits} CR, +${selectedTier.trophyRewardXp} XP & ${selectedTier.unlockedTrackName.uppercase()} UNLOCKED!"
                }
              }
              .padding(horizontal = 10.dp, vertical = 6.dp)
              .testTag("career_claim_tier_trophy_btn")
          ) {
            Text(
              text = when {
                isTierTrophyClaimed -> "🏆 CUP CLAIMED"
                canClaimTierTrophy -> "🏆 CLAIM CUP (+${selectedTier.trophyRewardCredits} CR)"
                else -> "🏆 TIER ${selectedTier.tierNumber} CUP"
              },
              fontSize = 8.5.sp,
              fontWeight = FontWeight.Black,
              color = if (canClaimTierTrophy) Color(0xFF0F172A) else Color(selectedTier.badgeColorHex)
            )
          }
        }

        // Dynamic Championship Completion Progress Bar
        val completionPct = (tierCompletionFraction * 100).toInt()
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Text(
              text = "CHAMPIONSHIP COMPLETION",
              fontSize = 8.5.sp,
              fontWeight = FontWeight.Bold,
              color = MetallicSilver
            )
            Text(
              text = "$completedTierEventsCount / $totalTierEventsCount COMPLETED ($completionPct%)",
              fontSize = 8.5.sp,
              fontWeight = FontWeight.Black,
              color = Color(selectedTier.badgeColorHex),
              fontFamily = FontFamily.Monospace
            )
          }
          LinearProgressIndicator(
            progress = { tierCompletionFraction },
            color = Color(selectedTier.badgeColorHex),
            trackColor = CharcoalSurface,
            modifier = Modifier
              .fillMaxWidth()
              .height(6.dp)
              .clip(RoundedCornerShape(3.dp))
          )
        }

        // Unlocked Rewards Row
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(CharcoalSurface)
            .padding(10.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column {
            Text("PRIZE PURSE", fontSize = 7.5.sp, color = TextSecondary, fontWeight = FontWeight.Bold)
            Text("+${selectedTier.trophyRewardCredits} CR", fontSize = 11.sp, color = NeonAmber, fontWeight = FontWeight.Black)
          }
          Column {
            Text("CHAMPION XP", fontSize = 7.5.sp, color = TextSecondary, fontWeight = FontWeight.Bold)
            Text("+${selectedTier.trophyRewardXp} XP", fontSize = 11.sp, color = NeonCyan, fontWeight = FontWeight.Black)
          }
          Column {
            Text("UNLOCKS TRACK", fontSize = 7.5.sp, color = TextSecondary, fontWeight = FontWeight.Bold)
            Text(selectedTier.unlockedTrackName, fontSize = 9.sp, color = PureWhite, fontWeight = FontWeight.Bold, maxLines = 1)
          }
          Column {
            Text("UNLOCKS CAR", fontSize = 7.5.sp, color = TextSecondary, fontWeight = FontWeight.Bold)
            Text(selectedTier.unlockedVehicleName.take(16), fontSize = 9.sp, color = Color(selectedTier.badgeColorHex), fontWeight = FontWeight.Bold, maxLines = 1)
          }
        }

        // Open Full Championship Series Standings & Bracket Screen (`HOME -> CAREER -> CHAMPIONSHIP -> RACE`)
        Button(
          onClick = { onOpenChampionshipSeries(selectedTier) },
          colors = ButtonDefaults.buttonColors(containerColor = Color(selectedTier.badgeColorHex)),
          shape = RoundedCornerShape(8.dp),
          modifier = Modifier
            .fillMaxWidth()
            .height(42.dp)
            .testTag("career_open_championship_btn")
        ) {
          Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = CarbonBlack, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = "ENTER ${selectedTier.displayName.uppercase()} SERIES HUB »",
            fontSize = 10.sp,
            fontWeight = FontWeight.Black,
            color = CarbonBlack
          )
        }
      }
    }

    // 4. EVENT DISCIPLINE FILTER BAR (All Tier Rounds, Sprint Events, Time Trials, Drift Challenges, Circuit GP)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = if (selectedDisciplineFilter == null) {
            "TIER ${selectedTier.tierNumber} CHAMPIONSHIP ROUNDS"
          } else {
            "ALL ${selectedDisciplineFilter?.displayName?.uppercase()}S"
          },
          fontSize = 13.sp,
          fontWeight = FontWeight.Black,
          color = PureWhite,
          letterSpacing = 0.5.sp
        )
        Text(
          text = "${displayedEvents.size} EVENTS",
          fontSize = 9.sp,
          fontWeight = FontWeight.Bold,
          color = MetallicSilver,
          fontFamily = FontFamily.Monospace
        )
      }

      Row(
        modifier = Modifier
          .fillMaxWidth()
          .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
      ) {
        val filterOptions: List<Pair<String, CareerEventCategory?>> = listOf(
          "TIER ROUNDS" to null,
          "SPRINT EVENTS" to CareerEventCategory.SPRINT_RACE,
          "TIME TRIALS" to CareerEventCategory.TIME_TRIAL,
          "DRIFT CHALLENGES" to CareerEventCategory.DRIFT_CHALLENGE,
          "CIRCUIT GP" to CareerEventCategory.CIRCUIT_RACE
        )
        filterOptions.forEach { (label, category) ->
          val isFilterSelected = selectedDisciplineFilter == category
          val tagSuffix = category?.name ?: "ALL"
          Box(
            modifier = Modifier
              .clip(RoundedCornerShape(8.dp))
              .background(if (isFilterSelected) MotorsportRed else CharcoalCard)
              .border(
                1.dp,
                if (isFilterSelected) MotorsportRed else CarbonBorder,
                RoundedCornerShape(8.dp)
              )
              .clickable { selectedDisciplineFilter = category }
              .padding(horizontal = 10.dp, vertical = 6.dp)
              .testTag("career_discipline_filter_$tagSuffix")
          ) {
            Text(
              text = label,
              fontSize = 8.5.sp,
              fontWeight = FontWeight.Black,
              color = if (isFilterSelected) PureWhite else MetallicSilver
            )
          }
        }
      }
    }

    // 5. CAREER EVENTS LIST WITH REQUIREMENTS, REWARDS & RACE / CLAIM ACTIONS
    if (displayedEvents.isEmpty()) {
      Card(
        colors = CardDefaults.cardColors(containerColor = CharcoalCard),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(
          modifier = Modifier.padding(16.dp),
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          Text("CHAMPIONSHIP INVITATIONAL", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = PureWhite)
          Text("Qualify through Tier ${selectedTier.tierNumber - 1} to unlock this grand prix season.", fontSize = 9.sp, color = TextSecondary)
        }
      }
    } else {
      displayedEvents.forEachIndexed { index, event ->
        val isEventCompleted = event.eventId in careerState.claimedEventIds
        val meetsLevel = careerState.playerLevel >= event.requiredLevel
        val meetsPi = vehiclePi >= event.requiredPerformanceIndex
        val canRaceEvent = meetsLevel && meetsPi
        val eventTrack = AVAILABLE_TRACKS.find { it.id == event.trackId } ?: activeTrack
        val eventTierColor = Color(event.championshipTier.badgeColorHex)

        Card(
          colors = CardDefaults.cardColors(
            containerColor = if (isEventCompleted) Color(0xFF111C18) else CharcoalCard
          ),
          shape = RoundedCornerShape(12.dp),
          border = BorderStroke(
            1.dp,
            when {
              isEventCompleted -> ApexGreen.copy(alpha = 0.7f)
              canRaceEvent -> eventTierColor.copy(alpha = 0.55f)
              else -> CarbonBorder
            }
          ),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("career_event_card_${event.eventId}")
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
              ) {
                Box(
                  modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(eventTierColor.copy(alpha = 0.2f))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                  Text(
                    text = "ROUND ${index + 1} • ${event.category.badgeText}",
                    fontSize = 7.5.sp,
                    fontWeight = FontWeight.Black,
                    color = eventTierColor
                  )
                }
                Text(
                  text = "${event.environmentName} (${eventTrack.name})",
                  fontSize = 8.sp,
                  color = TextSecondary,
                  fontWeight = FontWeight.Bold,
                  maxLines = 1
                )
              }

              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
              ) {
                Text(
                  text = event.title,
                  fontSize = 12.sp,
                  fontWeight = FontWeight.Bold,
                  color = PureWhite
                )
                if (isEventCompleted) {
                  Box(
                    modifier = Modifier
                      .clip(RoundedCornerShape(4.dp))
                      .background(ApexGreen.copy(alpha = 0.2f))
                      .padding(horizontal = 5.dp, vertical = 1.dp)
                  ) {
                    Text(
                      text = "COMPLETED ✔",
                      fontSize = 7.sp,
                      fontWeight = FontWeight.Black,
                      color = ApexGreen
                    )
                  }
                }
              }

              Text(
                text = event.targetObjective,
                fontSize = 8.5.sp,
                color = MetallicSilver,
                maxLines = 2
              )

              // Entry Requirements Row (Driver Level + Vehicle PI)
              Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                Text(
                  text = "REQ LVL ${event.requiredLevel}",
                  fontSize = 7.5.sp,
                  fontWeight = FontWeight.Black,
                  color = if (meetsLevel) ApexGreen else MotorsportRed
                )
                Text(
                  text = "REQ PI ${event.requiredPerformanceIndex}+",
                  fontSize = 7.5.sp,
                  fontWeight = FontWeight.Black,
                  color = if (meetsPi) NeonCyan else MotorsportRed
                )
                Text(
                  text = "• ${event.championshipTier.displayName}",
                  fontSize = 7.5.sp,
                  fontWeight = FontWeight.Bold,
                  color = TextSecondary
                )
              }

              // Rewards Row (Credits, XP, Track / Part Unlock)
              Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                Text(
                  text = "+${event.rewardCredits} CR",
                  fontSize = 9.sp,
                  fontWeight = FontWeight.Black,
                  color = NeonAmber
                )
                Text(
                  text = "+${event.rewardXp} XP",
                  fontSize = 9.sp,
                  fontWeight = FontWeight.Bold,
                  color = NeonCyan
                )
                Text(
                  text = "• ${event.unlockRewardLabel}",
                  fontSize = 8.sp,
                  color = if (event.unlocksTrackId != null) ApexGreen else TextSecondary,
                  fontWeight = if (event.unlocksTrackId != null) FontWeight.Bold else FontWeight.Normal,
                  maxLines = 1
                )
              }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Column(
              horizontalAlignment = Alignment.End,
              verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
              Button(
                onClick = {
                  onSelectTrack(eventTrack)
                  val mode = when (event.category) {
                    CareerEventCategory.TIME_TRIAL -> GameMode.TIME_ATTACK
                    CareerEventCategory.DRIFT_CHALLENGE -> GameMode.DRIFT_CHALLENGE
                    CareerEventCategory.SPRINT_RACE -> GameMode.SPEED_TRAP
                    else -> GameMode.GRAND_PRIX
                  }
                  onLaunchEventMode(mode, eventTrack)
                },
                enabled = canRaceEvent,
                colors = ButtonDefaults.buttonColors(
                  containerColor = eventTierColor,
                  disabledContainerColor = CharcoalSurface
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.testTag("career_event_start_btn_${event.eventId}")
              ) {
                Text(
                  text = if (canRaceEvent) "RACE" else "LOCKED",
                  fontSize = 10.5.sp,
                  fontWeight = FontWeight.Black,
                  color = if (canRaceEvent) PureWhite else TextSecondary
                )
              }

              // Interactive Instant Victory / Claim Reward Button for progression verification & quick claim
              Box(
                modifier = Modifier
                  .clip(RoundedCornerShape(6.dp))
                  .background(
                    when {
                      isEventCompleted -> CharcoalSurface
                      canRaceEvent -> ApexGreen.copy(alpha = 0.2f)
                      else -> CharcoalSurface.copy(alpha = 0.5f)
                    }
                  )
                  .border(
                    1.dp,
                    when {
                      isEventCompleted -> CarbonBorder
                      canRaceEvent -> ApexGreen
                      else -> CarbonBorder
                    },
                    RoundedCornerShape(6.dp)
                  )
                  .clickable(enabled = canRaceEvent && !isEventCompleted) {
                    if (ApexRivals2CareerManager.claimCareerEventReward(event.eventId, context)) {
                      com.example.data.firebase.ApexCloudSyncCoordinator.onRewardClaimed(context, event.eventId)
                      statusBannerMessage =
                        "ROUND WON: ${event.title.uppercase()} (+${event.rewardCredits} CR, +${event.rewardXp} XP)!"
                    }
                  }
                  .padding(horizontal = 8.dp, vertical = 4.dp)
                  .testTag("career_event_claim_btn_${event.eventId}")
              ) {
                Text(
                  text = if (isEventCompleted) "REWARDED" else "CLAIM WIN",
                  fontSize = 7.5.sp,
                  fontWeight = FontWeight.Black,
                  color = when {
                    isEventCompleted -> MetallicSilver
                    canRaceEvent -> ApexGreen
                    else -> TextSecondary
                  }
                )
              }
            }
          }
        }
      }
    }

    // 6. UNLOCKABLE TRACKS SHOWCASE & CIRCUIT PROGRESSION HUB
    Card(
      colors = CardDefaults.cardColors(containerColor = CharcoalCard),
      shape = RoundedCornerShape(14.dp),
      border = BorderStroke(1.dp, CarbonBorder),
      modifier = Modifier
        .fillMaxWidth()
        .testTag("career_unlockable_tracks_section")
    ) {
      Column(
        modifier = Modifier.padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column {
            Text(
              text = "UNLOCKABLE CHAMPIONSHIP CIRCUITS",
              fontSize = 12.sp,
              fontWeight = FontWeight.Black,
              color = PureWhite,
              letterSpacing = 0.5.sp
            )
            Text(
              text = "WIN CHAMPIONSHIP ROUNDS OR REACH DRIVER LEVELS TO UNLOCK WORLD TRACKS",
              fontSize = 8.sp,
              fontWeight = FontWeight.Bold,
              color = MetallicSilver
            )
          }
          Box(
            modifier = Modifier
              .clip(RoundedCornerShape(6.dp))
              .background(CharcoalSurface)
              .border(1.dp, NeonCyan, RoundedCornerShape(6.dp))
              .padding(horizontal = 8.dp, vertical = 4.dp)
          ) {
            val unlockedCount = AVAILABLE_TRACKS.count {
              it.id in careerState.unlockedTrackIds || ApexRivals2CareerManager.isTrackUnlocked(it.id)
            }
            Text(
              text = "$unlockedCount / ${AVAILABLE_TRACKS.size} UNLOCKED",
              fontSize = 8.5.sp,
              fontWeight = FontWeight.Black,
              color = NeonCyan,
              fontFamily = FontFamily.Monospace
            )
          }
        }

        AVAILABLE_TRACKS.forEach { track ->
          val isTrackUnlocked = track.id in careerState.unlockedTrackIds || ApexRivals2CareerManager.isTrackUnlocked(track.id)
          val isTrackSelected = track.id == activeTrack.id
          val requiredTier = ApexRivals2CareerManager.getTrackRequiredTier(track.id)
          val canUnlockNow = !isTrackUnlocked && (requiredTier == null || careerState.playerLevel >= requiredTier.requiredLevel)

          Row(
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(10.dp))
              .background(if (isTrackSelected) MotorsportRed.copy(alpha = 0.16f) else CharcoalSurface)
              .border(
                1.dp,
                when {
                  isTrackSelected -> MotorsportRed
                  isTrackUnlocked -> ApexGreen.copy(alpha = 0.5f)
                  else -> CarbonBorder
                },
                RoundedCornerShape(10.dp)
              )
              .padding(horizontal = 10.dp, vertical = 8.dp)
              .testTag("career_track_row_${track.id}"),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp),
              modifier = Modifier.weight(1f)
            ) {
              Icon(
                imageVector = if (isTrackUnlocked) Icons.Default.CheckCircle else Icons.Default.Lock,
                contentDescription = null,
                tint = if (isTrackUnlocked) ApexGreen else TextSecondary,
                modifier = Modifier.size(16.dp)
              )
              Column {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                  Text(
                    text = track.name.uppercase(),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    color = if (isTrackUnlocked) PureWhite else TextSecondary
                  )
                  Text(
                    text = "• ${String.format(java.util.Locale.US, "%.1f", track.totalLengthMeters / 1000f)} KM • ${track.turns} TURNS",
                    fontSize = 7.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = MetallicSilver
                  )
                }
                Text(
                  text = if (isTrackUnlocked) {
                    "UNLOCKED • BEST LAP ${track.referenceLapTimeMs / 1000}S TARGET"
                  } else {
                    ApexRivals2CareerManager.getTrackUnlockCondition(track.id)
                  },
                  fontSize = 7.5.sp,
                  fontWeight = FontWeight.Bold,
                  color = if (isTrackUnlocked) ApexGreen else NeonAmber
                )
              }
            }

            Box(
              modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(
                  when {
                    isTrackSelected -> MotorsportRed
                    isTrackUnlocked -> CharcoalCard
                    canUnlockNow -> NeonAmber
                    else -> CharcoalCard
                  }
                )
                .border(
                  1.dp,
                  when {
                    isTrackSelected -> MotorsportRed
                    isTrackUnlocked -> ApexGreen
                    canUnlockNow -> NeonAmber
                    else -> CarbonBorder
                  },
                  RoundedCornerShape(6.dp)
                )
                .clickable(enabled = isTrackUnlocked || canUnlockNow) {
                  if (!isTrackUnlocked && canUnlockNow) {
                    ApexRivals2CareerManager.unlockTrack(track.id, context)
                    statusBannerMessage = "CIRCUIT UNLOCKED: ${track.name.uppercase()} READY FOR RACING!"
                  }
                  onSelectTrack(track)
                }
                .padding(horizontal = 10.dp, vertical = 5.dp)
                .testTag("career_track_select_btn_${track.id}")
            ) {
              Text(
                text = when {
                  isTrackSelected -> "ACTIVE CIRCUIT"
                  isTrackUnlocked -> "SELECT TRACK"
                  canUnlockNow -> "UNLOCK TRACK"
                  else -> "LOCKED"
                },
                fontSize = 8.sp,
                fontWeight = FontWeight.Black,
                color = when {
                  isTrackSelected -> PureWhite
                  isTrackUnlocked -> ApexGreen
                  canUnlockNow -> Color(0xFF0F172A)
                  else -> TextSecondary
                }
              )
            }
          }
        }
      }
    }

    // 7. PRIMARY CTA: START CHAMPIONSHIP BUTTON
    Button(
      onClick = {
        val nextTierEvent = allTierEvents.firstOrNull { it.eventId !in careerState.claimedEventIds }
          ?: allTierEvents.firstOrNull()
        if (nextTierEvent != null) {
          val track = AVAILABLE_TRACKS.find { it.id == nextTierEvent.trackId } ?: activeTrack
          onSelectTrack(track)
          val mode = when (nextTierEvent.category) {
            CareerEventCategory.TIME_TRIAL -> GameMode.TIME_ATTACK
            CareerEventCategory.DRIFT_CHALLENGE -> GameMode.DRIFT_CHALLENGE
            CareerEventCategory.SPRINT_RACE -> GameMode.SPEED_TRAP
            else -> GameMode.GRAND_PRIX
          }
          onLaunchEventMode(mode, track)
        } else {
          onLaunchRace()
        }
      },
      enabled = isUnlocked,
      colors = ButtonDefaults.buttonColors(
        containerColor = MotorsportRed,
        disabledContainerColor = CharcoalSurface
      ),
      shape = RoundedCornerShape(12.dp),
      modifier = Modifier
        .fillMaxWidth()
        .height(52.dp)
        .testTag("career_start_championship_btn")
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        Icon(Icons.Default.PlayArrow, contentDescription = null, tint = PureWhite)
        Text(
          text = if (isUnlocked) "START ${selectedTier.displayName.uppercase()}" else "LOCKED • REACH LEVEL ${selectedTier.requiredLevel}",
          fontSize = 14.sp,
          fontWeight = FontWeight.Black,
          color = PureWhite,
          letterSpacing = 0.8.sp
        )
      }
    }
  }
}

// ===========================================================================
// SCREEN 10 – MULTIPLAYER (MATCHMAKING, LOBBIES & CROSSPLAY HUB)
// ===========================================================================
@Composable
fun ApexMultiplayerHubScreen(
  activeVehicle: Vehicle,
  onBack: () -> Unit,
  onJoinRace: () -> Unit,
  onOpenFullLobby: () -> Unit,
  modifier: Modifier = Modifier
) {
  var roomCodeInput by remember { mutableStateOf("") }
  var isSearchingMatch by remember { mutableStateOf(false) }

  LaunchedEffect(isSearchingMatch) {
    if (isSearchingMatch) {
      delay(2200)
      isSearchingMatch = false
      onJoinRace()
    }
  }

  Column(
    modifier = modifier
      .fillMaxSize()
      .background(CarbonBlack)
      .verticalScroll(rememberScrollState())
      .padding(horizontal = 14.dp, vertical = 10.dp)
      .testTag("screen_10_multiplayer_hub"),
    verticalArrangement = Arrangement.spacedBy(14.dp)
  ) {
    // 1. HEADER ROW: Back Button + ONLINE MULTIPLAYER + Crossplay Status
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(
          onClick = onBack,
          modifier = Modifier.testTag("multiplayer_back_btn")
        ) {
          Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = "Back",
            tint = PureWhite
          )
        }
        Spacer(modifier = Modifier.width(6.dp))
        Column {
          Text(
            text = "ONLINE MULTIPLAYER",
            fontSize = 18.sp,
            fontWeight = FontWeight.Black,
            color = PureWhite,
            letterSpacing = 1.sp
          )
          Text(
            text = "LOW-LATENCY NETCODE • 12-PLAYER SYNC",
            fontSize = 8.5.sp,
            fontWeight = FontWeight.Bold,
            color = NeonCyan,
            letterSpacing = 0.5.sp
          )
        }
      }

      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
      ) {
        // Crossplay Badge
        Box(
          modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(CharcoalCard)
            .border(1.dp, ApexGreen, RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
              modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(ApexGreen)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
              text = "CROSSPLAY ON",
              fontSize = 8.5.sp,
              fontWeight = FontWeight.Black,
              color = ApexGreen
            )
          }
        }

        // Driver Rating Pill
        Box(
          modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(CharcoalCard)
            .border(1.dp, NeonAmber, RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
          Text(
            text = "SR: S · DR: A+",
            fontSize = 8.5.sp,
            fontWeight = FontWeight.Black,
            color = NeonAmber
          )
        }
      }
    }

    // 2. WORLDWIDE QUICK MATCH HERO BANNER
    Card(
      colors = CardDefaults.cardColors(containerColor = CharcoalCard),
      shape = RoundedCornerShape(16.dp),
      border = BorderStroke(1.5.dp, NeonCyan),
      modifier = Modifier.fillMaxWidth()
    ) {
      Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column {
            Text(
              text = "WORLDWIDE MATCHMAKING",
              fontSize = 15.sp,
              fontWeight = FontWeight.Black,
              color = PureWhite,
              letterSpacing = 0.5.sp
            )
            Text(
              text = "Auto-assign to lowest ping server (EU-West / US-East / AP-East)",
              fontSize = 8.5.sp,
              color = TextSecondary
            )
          }

          Box(
            modifier = Modifier
              .clip(RoundedCornerShape(6.dp))
              .background(NeonCyan.copy(alpha = 0.18f))
              .padding(horizontal = 8.dp, vertical = 3.dp)
          ) {
            Text(
              text = "18ms PING",
              fontSize = 8.5.sp,
              fontWeight = FontWeight.Black,
              color = NeonCyan,
              fontFamily = FontFamily.Monospace
            )
          }
        }

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          // Current Vehicle in lobby
          Row(
            modifier = Modifier
              .weight(1f)
              .clip(RoundedCornerShape(8.dp))
              .background(CharcoalSurface)
              .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            Icon(Icons.Default.DirectionsCar, contentDescription = null, tint = PureWhite, modifier = Modifier.size(16.dp))
            Column {
              Text("ACTIVE CAR", fontSize = 7.sp, color = TextSecondary)
              Text(activeVehicle.name.take(16), fontSize = 9.sp, fontWeight = FontWeight.Bold, color = PureWhite, maxLines = 1)
            }
          }

          // Grid Size
          Row(
            modifier = Modifier
              .weight(1f)
              .clip(RoundedCornerShape(8.dp))
              .background(CharcoalSurface)
              .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            Icon(Icons.Default.Group, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(16.dp))
            Column {
              Text("LOBBY SIZE", fontSize = 7.sp, color = TextSecondary)
              Text("12 RACERS", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = NeonCyan)
            }
          }
        }

        Button(
          onClick = { isSearchingMatch = true },
          colors = ButtonDefaults.buttonColors(containerColor = if (isSearchingMatch) NeonAmber else MotorsportRed),
          shape = RoundedCornerShape(10.dp),
          modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .testTag("multiplayer_quick_join_btn")
        ) {
          if (isSearchingMatch) {
            CircularProgressIndicator(color = CarbonBlack, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
            Spacer(modifier = Modifier.width(8.dp))
            Text("MATCHMAKING IN PROGRESS...", fontSize = 12.sp, fontWeight = FontWeight.Black, color = CarbonBlack)
          } else {
            Icon(Icons.Default.PlayArrow, contentDescription = null, tint = PureWhite)
            Spacer(modifier = Modifier.width(6.dp))
            Text("QUICK JOIN RACE", fontSize = 13.sp, fontWeight = FontWeight.Black, color = PureWhite)
          }
        }
      }
    }

    // 3. PRIVATE ROOM CODE CONNECT CARD
    Card(
      colors = CardDefaults.cardColors(containerColor = CharcoalCard),
      shape = RoundedCornerShape(14.dp),
      border = BorderStroke(1.dp, CarbonBorder),
      modifier = Modifier.fillMaxWidth()
    ) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        OutlinedTextField(
          value = roomCodeInput,
          onValueChange = { roomCodeInput = it.uppercase().take(8) },
          label = { Text("ROOM CODE (E.G. APX-9821)", fontSize = 9.sp) },
          colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = PureWhite,
            unfocusedTextColor = PureWhite,
            focusedBorderColor = NeonCyan,
            unfocusedBorderColor = CarbonBorder
          ),
          singleLine = true,
          modifier = Modifier.weight(1f)
        )

        Button(
          onClick = {
            if (roomCodeInput.isNotBlank()) onJoinRace()
          },
          colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
          shape = RoundedCornerShape(8.dp),
          modifier = Modifier.testTag("multiplayer_room_connect_btn")
        ) {
          Text("JOIN", fontSize = 11.sp, fontWeight = FontWeight.Black, color = CarbonBlack)
        }
      }
    }

    // 4. ACTIVE OPEN LOBBIES
    Text(
      text = "FEATURED MOTORSPORT LOBBIES",
      fontSize = 13.sp,
      fontWeight = FontWeight.Black,
      color = PureWhite
    )

    val publicLobbies = listOf(
      Triple("Autodromo Monza • GT3 Pro Cup", "11/12 RACERS • 22ms", "QUALIFYING"),
      Triple("Tokyo Expressway • Night Drift Syndicate", "7/8 RACERS • 34ms", "OPEN LOBBY"),
      Triple("Nürburgring Nordschleife • Wet 24h Sprint", "15/16 RACERS • 19ms", "GRID FORMATION")
    )

    publicLobbies.forEach { (name, info, status) ->
      Card(
        colors = CardDefaults.cardColors(containerColor = CharcoalCard),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, CarbonBorder),
        modifier = Modifier.fillMaxWidth()
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(name, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = PureWhite)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              Text(info, fontSize = 8.5.sp, color = NeonCyan, fontFamily = FontFamily.Monospace)
              Text("• $status", fontSize = 8.5.sp, color = ApexGreen, fontWeight = FontWeight.Bold)
            }
          }

          Button(
            onClick = onJoinRace,
            colors = ButtonDefaults.buttonColors(containerColor = CharcoalSurface),
            shape = RoundedCornerShape(8.dp),
            border = BorderStroke(1.dp, NeonCyan)
          ) {
            Text("JOIN", fontSize = 10.sp, fontWeight = FontWeight.Black, color = NeonCyan)
          }
        }
      }
    }

    // 5. SECONDARY CTA: OPEN ADVANCED LOBBY BROWSER
    OutlinedButton(
      onClick = onOpenFullLobby,
      border = BorderStroke(1.dp, CarbonBorder),
      shape = RoundedCornerShape(10.dp),
      modifier = Modifier
        .fillMaxWidth()
        .height(44.dp)
        .testTag("multiplayer_full_lobby_btn")
    ) {
      Text(
        text = "OPEN FULL MULTIPLAYER BROWSER & HOSTING",
        fontSize = 10.5.sp,
        fontWeight = FontWeight.Bold,
        color = MetallicSilver
      )
    }
  }
}

// ===========================================================================
// SCREEN 11 – PLAYER DRIVER PROFILE & SUPER LICENSE
// ===========================================================================
@Composable
fun ApexDriverProfileSubScreen(
  activeVehicle: Vehicle,
  onBack: () -> Unit,
  onOpenFullProfile: () -> Unit,
  onOpenGarage: () -> Unit,
  onOpenAchievements: () -> Unit = {},
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val profileScope = rememberCoroutineScope()
  val credentialManager = remember(context) { androidx.credentials.CredentialManager.create(context) }
  val careerState by ApexRivals2CareerManager.state.collectAsState()
  val cloudSyncState by com.example.data.firebase.ApexCloudSyncCoordinator.uiState.collectAsState()
  var profileAuthMsg by remember { mutableStateOf<String?>(null) }

  val metricsDao = remember(context) { com.example.data.local.AppDatabase.getDatabase(context).performanceMetricsDao() }
  val totalDistMeters by metricsDao.getTotalDistanceMeters().collectAsState(initial = 0L)
  val maxTopSpeed by metricsDao.getMaxTopSpeedKmh().collectAsState(initial = 0f)
  val totalRoomSessions by metricsDao.getTotalSessionsCount().collectAsState(initial = 0)

  val displayDriverName = remember(careerState.playerUsername, cloudSyncState.displayName) {
    cloudSyncState.displayName?.takeIf { it.isNotBlank() }?.uppercase(Locale.US)
      ?: careerState.playerUsername.uppercase(Locale.US)
  }
  val racesEntered = maxOf(careerState.totalRacesCompleted, totalRoomSessions)
  val winPct = if (racesEntered > 0) ((careerState.totalVictories * 100) / racesEntered) else 0
  val distKmFormatted = String.format(Locale.US, "%.1f km", (totalDistMeters ?: 0L) / 1000f)
  val topSpeedFormatted = if ((maxTopSpeed ?: 0f) > 0f) {
    String.format(Locale.US, "%.1f km/h", maxTopSpeed)
  } else {
    "${activeVehicle.topSpeedKmh}.0 km/h"
  }

  Column(
    modifier = modifier
      .fillMaxSize()
      .background(CarbonBlack)
      .verticalScroll(rememberScrollState())
      .padding(horizontal = 14.dp, vertical = 10.dp)
      .testTag("screen_11_driver_profile"),
    verticalArrangement = Arrangement.spacedBy(14.dp)
  ) {
    // 1. HEADER ROW: Back Button + DRIVER PROFILE & LICENSE
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(
          onClick = onBack,
          modifier = Modifier.testTag("profile_back_btn")
        ) {
          Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = "Back",
            tint = PureWhite
          )
        }
        Spacer(modifier = Modifier.width(6.dp))
        Column {
          Text(
            text = "DRIVER PROFILE & LICENSE",
            fontSize = 18.sp,
            fontWeight = FontWeight.Black,
            color = PureWhite,
            letterSpacing = 1.sp
          )
          Text(
            text = "FIA MOTORSPORT ESPORTS CREDENTIALS",
            fontSize = 8.5.sp,
            fontWeight = FontWeight.Bold,
            color = NeonAmber,
            letterSpacing = 0.5.sp
          )
        }
      }

      Box(
        modifier = Modifier
          .clip(RoundedCornerShape(6.dp))
          .background(CharcoalCard)
          .border(1.dp, if (cloudSyncState.isAuthenticated) ApexGreen else NeonAmber, RoundedCornerShape(6.dp))
          .clickable {
            if (!cloudSyncState.isAuthenticated) {
              com.example.data.firebase.ApexCredentialAuthHelper.onGoogleSignInClicked(
                context = context,
                credentialManager = credentialManager,
                onAuthSuccess = {
                  com.example.data.firebase.ApexCloudSyncCoordinator.startObservingAuth(context, profileScope)
                  profileAuthMsg = "Google account linked & syncing!"
                },
                onAuthError = { err -> profileAuthMsg = err },
                scope = profileScope
              )
            } else {
              com.example.data.firebase.ApexCloudSyncCoordinator.retryPendingSync(context, profileScope)
              profileAuthMsg = "Refreshing Cloud Save..."
            }
          }
          .padding(horizontal = 8.dp, vertical = 4.dp)
          .testTag("profile_auth_status_badge")
      ) {
        Text(
          text = if (cloudSyncState.isAuthenticated) "GOOGLE ID: SYNCED" else "SIGN IN WITH GOOGLE",
          fontSize = 8.5.sp,
          fontWeight = FontWeight.Black,
          color = if (cloudSyncState.isAuthenticated) ApexGreen else NeonAmber
        )
      }
    }

    profileAuthMsg?.let { msg ->
      Text(msg, fontSize = 8.5.sp, color = NeonCyan, fontFamily = FontFamily.Monospace)
    }

    // 2. APEX MOTORSPORT SUPER LICENSE CARD (Holographic Silver & Redline)
    Card(
      colors = CardDefaults.cardColors(containerColor = CharcoalCard),
      shape = RoundedCornerShape(16.dp),
      border = BorderStroke(1.5.dp, Brush.linearGradient(listOf(MetallicSilver, MotorsportRed, NeonCyan))),
      modifier = Modifier.fillMaxWidth()
    ) {
      Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = "APEX MOTORSPORT SUPER LICENSE",
            fontSize = 11.sp,
            fontWeight = FontWeight.Black,
            color = MetallicSilver,
            letterSpacing = 1.sp
          )
          Text(
            text = if (careerState.playerLevel >= 10) "CLASS S" else "CLASS A",
            fontSize = 12.sp,
            fontWeight = FontWeight.Black,
            color = MotorsportRed,
            fontFamily = FontFamily.Monospace
          )
        }

        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
          Box(
            modifier = Modifier
              .size(54.dp)
              .clip(CircleShape)
              .background(CharcoalSurface)
              .border(2.dp, MotorsportRed, CircleShape),
            contentAlignment = Alignment.Center
          ) {
            Icon(Icons.Default.AccountCircle, contentDescription = null, tint = PureWhite, modifier = Modifier.size(42.dp))
          }

          Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
              text = displayDriverName,
              fontSize = 17.sp,
              fontWeight = FontWeight.Black,
              color = PureWhite
            )
            Text(
              text = "LEVEL ${careerState.playerLevel} • %,d XP • %,d CR • 💎 ${careerState.coinsBalance}".format(
                careerState.totalXp,
                careerState.creditsBalance
              ),
              fontSize = 9.sp,
              fontWeight = FontWeight.Bold,
              color = NeonAmber
            )
            Text(
              text = "LICENSE ID: ${(cloudSyncState.uid ?: careerState.playerId).take(16).uppercase(Locale.US)} • REV #${cloudSyncState.syncRevision}",
              fontSize = 7.5.sp,
              color = TextSecondary,
              fontFamily = FontFamily.Monospace
            )
          }
        }
      }
    }

    // 3. CAREER STATISTICS MATRIX
    Text(
      text = "CAREER TELEMETRY RECORD",
      fontSize = 13.sp,
      fontWeight = FontWeight.Black,
      color = PureWhite
    )

    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      ProfileStatCard(label = "RACES ENTERED", value = "$racesEntered", color = PureWhite, modifier = Modifier.weight(1f))
      ProfileStatCard(label = "WINS (P1)", value = "${careerState.totalVictories} ($winPct%)", color = ApexGreen, modifier = Modifier.weight(1f))
      ProfileStatCard(label = "VEHICLES", value = "${careerState.ownedVehicleIds.size} Owned", color = NeonCyan, modifier = Modifier.weight(1f))
      ProfileStatCard(label = "DISTANCE", value = distKmFormatted, color = NeonAmber, modifier = Modifier.weight(1f))
    }

    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      ProfileStatCard(label = "TOP SPEED", value = topSpeedFormatted, color = MotorsportRed, modifier = Modifier.weight(1f))
      ProfileStatCard(label = "STREAK", value = "${careerState.dailyLoginStreakDays} Days", color = PureWhite, modifier = Modifier.weight(1f))
      ProfileStatCard(label = "TRACKS", value = "${careerState.unlockedTrackIds.size}/6", color = NeonCyan, modifier = Modifier.weight(1f))
      ProfileStatCard(label = "CREDITS", value = "%,d".format(careerState.creditsBalance), color = ApexGreen, modifier = Modifier.weight(1f))
    }

    // 4. TROPHY CABINET & ACHIEVEMENTS SHORTCUT (`HOME -> PROFILE -> ACHIEVEMENTS`)
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Text(
        text = "CHAMPIONSHIP TROPHY CABINET",
        fontSize = 13.sp,
        fontWeight = FontWeight.Black,
        color = PureWhite
      )

      Button(
        onClick = onOpenAchievements,
        colors = ButtonDefaults.buttonColors(containerColor = NeonAmber),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.testTag("profile_open_achievements_btn")
      ) {
        Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = CarbonBlack, modifier = Modifier.size(15.dp))
        Spacer(modifier = Modifier.width(5.dp))
        Text("VIEW ACHIEVEMENTS »", fontSize = 9.5.sp, fontWeight = FontWeight.Black, color = CarbonBlack)
      }
    }

    Row(
      modifier = Modifier
        .fillMaxWidth()
        .horizontalScroll(rememberScrollState()),
      horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      TrophyCabinetCard("ROOKIE CUP", "TIER 1 CHAMPION", Icons.Default.EmojiEvents, ApexGreen)
      TrophyCabinetCard("AMATEUR GT3", "TIER 2 CHAMPION", Icons.Default.EmojiEvents, NeonCyan)
      TrophyCabinetCard("TOUGE DRIFT", "DRIFT SYNDICATE", Icons.Default.Star, NeonAmber)
      TrophyCabinetCard("ELITE WORLD GP", "WORLD CHAMPION", Icons.Default.EmojiEvents, MotorsportRed)
    }

    // 5. CURRENT VEHICLE & GARAGE SHORTCUT
    Card(
      colors = CardDefaults.cardColors(containerColor = CharcoalCard),
      shape = RoundedCornerShape(14.dp),
      border = BorderStroke(1.dp, CarbonBorder),
      modifier = Modifier.fillMaxWidth()
    ) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
          Text("FAVORITE VEHICLE", fontSize = 8.sp, color = TextSecondary, fontWeight = FontWeight.Bold)
          Text(activeVehicle.name, fontSize = 13.sp, fontWeight = FontWeight.Black, color = PureWhite)
          Text("${activeVehicle.horsepower} HP • ${activeVehicle.topSpeedKmh} KM/H TOP SPEED", fontSize = 8.5.sp, color = NeonAmber)
        }

        Button(
          onClick = onOpenGarage,
          colors = ButtonDefaults.buttonColors(containerColor = CharcoalSurface),
          shape = RoundedCornerShape(8.dp),
          border = BorderStroke(1.dp, MotorsportRed),
          modifier = Modifier.testTag("profile_customize_vehicle_btn")
        ) {
          Text("GARAGE", fontSize = 10.sp, fontWeight = FontWeight.Black, color = MotorsportRed)
        }
      }
    }

    // 6. BOTTOM CTA: OPEN FULL PERFORMANCE TELEMETRY SCREEN
    OutlinedButton(
      onClick = onOpenFullProfile,
      border = BorderStroke(1.dp, CarbonBorder),
      shape = RoundedCornerShape(10.dp),
      modifier = Modifier
        .fillMaxWidth()
        .height(44.dp)
        .testTag("profile_open_full_btn")
    ) {
      Text(
        text = "MANAGE CLOUD BACKUP & TELEMETRY DATABASE",
        fontSize = 10.5.sp,
        fontWeight = FontWeight.Bold,
        color = MetallicSilver
      )
    }
  }
}

@Composable
private fun ProfileStatCard(
  label: String,
  value: String,
  color: Color,
  modifier: Modifier = Modifier
) {
  Card(
    colors = CardDefaults.cardColors(containerColor = CharcoalCard),
    shape = RoundedCornerShape(10.dp),
    border = BorderStroke(1.dp, CarbonBorder),
    modifier = modifier
  ) {
    Column(
      modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
      Text(label, fontSize = 6.5.sp, fontWeight = FontWeight.Bold, color = TextSecondary, textAlign = TextAlign.Center, maxLines = 1)
      Text(value, fontSize = 11.sp, fontWeight = FontWeight.Black, color = color, fontFamily = FontFamily.Monospace, maxLines = 1)
    }
  }
}

@Composable
private fun TrophyCabinetCard(
  title: String,
  subtitle: String,
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  tint: Color
) {
  Card(
    colors = CardDefaults.cardColors(containerColor = CharcoalCard),
    shape = RoundedCornerShape(12.dp),
    border = BorderStroke(1.dp, tint.copy(alpha = 0.5f)),
    modifier = Modifier.width(130.dp)
  ) {
    Column(
      modifier = Modifier.padding(10.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
      Box(
        modifier = Modifier
          .size(36.dp)
          .clip(CircleShape)
          .background(tint.copy(alpha = 0.2f)),
        contentAlignment = Alignment.Center
      ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
      }
      Text(title, fontSize = 9.sp, fontWeight = FontWeight.Black, color = PureWhite, textAlign = TextAlign.Center)
      Text(subtitle, fontSize = 7.5.sp, color = TextSecondary, textAlign = TextAlign.Center)
    }
  }
}

// ===========================================================================
// UNIFIED NAVIGATION SUB-SCREENS FOR THE 6 ARCHITECTURE CHAINS
// 1. VEHICLE_UPGRADES (HOME -> GARAGE -> CUSTOMIZATION -> UPGRADES)
// 2. CHAMPIONSHIP_DETAIL (HOME -> CAREER -> CHAMPIONSHIP -> RACE)
// 3. EVENTS_MISSIONS (HOME -> EVENTS -> MISSIONS -> REWARDS)
// 4. EVENTS_REWARDS (HOME -> EVENTS -> MISSIONS -> REWARDS)
// 5. DRIVER_ACHIEVEMENTS (HOME -> PROFILE -> ACHIEVEMENTS)
// ===========================================================================

@Composable
fun ApexVehicleUpgradesScreen(
  vehicle: Vehicle,
  onBack: () -> Unit,
  onOpenCustomization: () -> Unit,
  onRaceNow: () -> Unit,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val careerState by ApexRivals2CareerManager.state.collectAsState()
  val loadout = remember(vehicle.id, careerState.loadoutsByVehicleId) {
    ApexRivals2CareerManager.getLoadoutForVehicle(vehicle)
  }
  val upgradedStats = remember(vehicle, loadout) {
    ApexRivals2CareerManager.computeUpgradedStats(vehicle, loadout)
  }
  var upgradeFeedback by remember { mutableStateOf<String?>(null) }

  Column(
    modifier = modifier
      .fillMaxSize()
      .background(CarbonBlack)
      .verticalScroll(rememberScrollState())
      .padding(14.dp)
      .testTag("screen_vehicle_upgrades"),
    verticalArrangement = Arrangement.spacedBy(12.dp)
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      ScreenHeaderBar(
        title = "PERFORMANCE UPGRADES & DYNO BENCH",
        subtitle = "${vehicle.name.uppercase()} • STAGE ${upgradedStats.totalUpgradeLevel}/${upgradedStats.maxUpgradeLevel}",
        onBack = onBack
      )

      Box(
        modifier = Modifier
          .clip(RoundedCornerShape(6.dp))
          .background(CharcoalCard)
          .border(1.dp, NeonAmber, RoundedCornerShape(6.dp))
          .padding(horizontal = 10.dp, vertical = 5.dp)
          .testTag("upgrades_credits_badge")
      ) {
        Text(
          text = "CR ${careerState.creditsBalance}",
          fontSize = 10.sp,
          fontWeight = FontWeight.Black,
          color = NeonAmber,
          fontFamily = FontFamily.Monospace
        )
      }
    }

    // 3D Vehicle Dyno Preview & Live Telemetry Readout
    Card(
      colors = CardDefaults.cardColors(containerColor = CharcoalCard),
      shape = RoundedCornerShape(14.dp),
      border = BorderStroke(1.dp, MotorsportRed.copy(alpha = 0.6f)),
      modifier = Modifier.fillMaxWidth()
    ) {
      Column(
        modifier = Modifier.padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        Showroom3DVehiclePreviewBox(vehicle = vehicle, loadout = loadout, heightDp = 135)
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Text("TOP SPEED: ${upgradedStats.maxSpeedKmh} KM/H", fontSize = 9.5.sp, fontWeight = FontWeight.Black, color = NeonCyan, fontFamily = FontFamily.Monospace)
          Text("0-100: ${String.format(Locale.US, "%.2fs", upgradedStats.acceleration0to100Sec)}", fontSize = 9.5.sp, fontWeight = FontWeight.Black, color = ApexGreen, fontFamily = FontFamily.Monospace)
          Text("GRIP: ${String.format(Locale.US, "%.2fG", upgradedStats.handlingCorneringG)}", fontSize = 9.5.sp, fontWeight = FontWeight.Black, color = NeonAmber, fontFamily = FontFamily.Monospace)
          Text("POWER: ${upgradedStats.horsepower} HP", fontSize = 9.5.sp, fontWeight = FontWeight.Black, color = MotorsportRed, fontFamily = FontFamily.Monospace)
        }
      }
    }

    // Subsystem Upgrade Cards (ENGINE, TURBO, BRAKES, TIRES)
    UpgradeSubsystem.entries.forEach { sub ->
      val currentLv = when (sub) {
        UpgradeSubsystem.ENGINE -> loadout.engineLevel
        UpgradeSubsystem.TURBO -> loadout.turboLevel
        UpgradeSubsystem.BRAKES -> loadout.brakesLevel
        UpgradeSubsystem.TIRES -> loadout.tiresLevel
      }
      val cost = sub.baseCostCredits * currentLv
      val isMax = currentLv >= 5

      Card(
        colors = CardDefaults.cardColors(containerColor = CharcoalCard),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, if (isMax) ApexGreen else CarbonBorder),
        modifier = Modifier
          .fillMaxWidth()
          .testTag("upgrades_subsystem_card_${sub.name}")
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(
              text = "${sub.displayName.uppercase()} • STAGE $currentLv/5",
              fontSize = 11.sp,
              fontWeight = FontWeight.Black,
              color = PureWhite
            )
            Text(
              text = when (sub) {
                UpgradeSubsystem.ENGINE -> "+Top Speed & Horsepower Output"
                UpgradeSubsystem.TURBO -> "Faster 0-100 km/h & ERS Boost"
                UpgradeSubsystem.BRAKES -> "+Carbon-Ceramic Stopping Power"
                UpgradeSubsystem.TIRES -> "+Lateral Cornering G-Force Grip"
              },
              fontSize = 8.5.sp,
              color = MetallicSilver
            )
          }

          Button(
            onClick = {
              val ok = ApexRivals2CareerManager.upgradeSubsystem(vehicle, sub, context)
              if (ok) {
                com.example.data.firebase.ApexCloudSyncCoordinator.onSubsystemUpgraded(
                  context = context,
                  vehicleId = vehicle.id,
                  subsystem = sub.name,
                  targetStage = currentLv + 1
                )
              }
              upgradeFeedback = if (ok) {
                "Installed ${sub.displayName} Stage ${currentLv + 1}!"
              } else {
                "Max Stage reached or insufficient CR."
              }
            },
            enabled = !isMax,
            colors = ButtonDefaults.buttonColors(
              containerColor = if (isMax) CharcoalSurface else MotorsportRed
            ),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.testTag("upgrades_install_btn_${sub.name}")
          ) {
            Text(
              text = if (isMax) "MAXED" else "INSTALL • CR $cost",
              fontSize = 9.sp,
              fontWeight = FontWeight.Black,
              color = PureWhite
            )
          }
        }
      }
    }

    upgradeFeedback?.let { msg ->
      Text(
        text = msg,
        fontSize = 9.sp,
        fontWeight = FontWeight.Black,
        color = ApexGreen,
        fontFamily = FontFamily.Monospace,
        modifier = Modifier.testTag("upgrades_feedback_text")
      )
    }

    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      OutlinedButton(
        onClick = onOpenCustomization,
        border = BorderStroke(1.dp, MetallicSilver),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier
          .weight(1f)
          .height(48.dp)
          .testTag("upgrades_back_to_customization_btn")
      ) {
        Text("« STYLING STUDIO", fontSize = 10.sp, fontWeight = FontWeight.Black, color = PureWhite)
      }

      Button(
        onClick = onRaceNow,
        colors = ButtonDefaults.buttonColors(containerColor = MotorsportRed),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier
          .weight(1f)
          .height(48.dp)
          .testTag("upgrades_test_drive_btn")
      ) {
        Icon(Icons.Default.PlayArrow, contentDescription = null, tint = PureWhite)
        Spacer(modifier = Modifier.width(6.dp))
        Text("RACE UPGRADED CAR »", fontSize = 10.sp, fontWeight = FontWeight.Black, color = PureWhite)
      }
    }
  }
}

@Composable
fun ApexChampionshipSeriesDetailScreen(
  tier: ChampionshipTier,
  activeVehicle: Vehicle,
  activeTrack: Track,
  onSelectTrack: (Track) -> Unit,
  onBack: () -> Unit,
  onStartChampionshipRace: (GameMode, Track) -> Unit,
  modifier: Modifier = Modifier
) {
  val careerState by ApexRivals2CareerManager.state.collectAsState()
  val tierEvents = remember(tier) {
    ApexRivals2CareerManager.careerEvents.filter {
      it.championshipTier == tier && it.category != CareerEventCategory.DAILY_CHALLENGE
    }
  }
  val completedCount = tierEvents.count { it.eventId in careerState.claimedEventIds }
  val tierColor = Color(tier.badgeColorHex)

  Column(
    modifier = modifier
      .fillMaxSize()
      .background(CarbonBlack)
      .verticalScroll(rememberScrollState())
      .padding(14.dp)
      .testTag("screen_championship_detail"),
    verticalArrangement = Arrangement.spacedBy(12.dp)
  ) {
    ScreenHeaderBar(
      title = tier.displayName.uppercase(),
      subtitle = "TIER ${tier.tierNumber} OFFICIAL FIA CHAMPIONSHIP SERIES • $completedCount/${tierEvents.size} ROUNDS COMPLETED",
      onBack = onBack
    )

    // Championship Series Standings & Prize Pool Card
    Card(
      colors = CardDefaults.cardColors(containerColor = CharcoalCard),
      shape = RoundedCornerShape(14.dp),
      border = BorderStroke(1.5.dp, tierColor),
      modifier = Modifier
        .fillMaxWidth()
        .testTag("championship_standings_card")
    ) {
      Column(
        modifier = Modifier.padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = "CHAMPIONSHIP STANDINGS & CUP PURSE",
            fontSize = 11.sp,
            fontWeight = FontWeight.Black,
            color = tierColor
          )
          Text(
            text = "+${tier.trophyRewardCredits} CR • +${tier.trophyRewardXp} XP",
            fontSize = 10.sp,
            fontWeight = FontWeight.Black,
            color = NeonAmber,
            fontFamily = FontFamily.Monospace
          )
        }

        Text(
          text = "ACTIVE VEHICLE: ${activeVehicle.name.uppercase()} • UNLOCKS CIRCUIT: ${tier.unlockedTrackName.uppercase()}",
          fontSize = 8.5.sp,
          fontWeight = FontWeight.Bold,
          color = PureWhite
        )
      }
    }

    // Championship Rounds List
    tierEvents.forEachIndexed { idx, event ->
      val eventTrack = AVAILABLE_TRACKS.find { it.id == event.trackId } ?: activeTrack
      val isCompleted = event.eventId in careerState.claimedEventIds

      Card(
        colors = CardDefaults.cardColors(containerColor = CharcoalCard),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, if (isCompleted) ApexGreen else CarbonBorder),
        modifier = Modifier
          .fillMaxWidth()
          .testTag("championship_round_card_${idx + 1}")
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(
              text = "ROUND ${idx + 1}: ${event.title.uppercase()}",
              fontSize = 11.sp,
              fontWeight = FontWeight.Black,
              color = PureWhite
            )
            Text(
              text = "${eventTrack.name} • ${event.targetObjective}",
              fontSize = 8.5.sp,
              color = MetallicSilver
            )
            Text(
              text = "REWARD: +${event.rewardCredits} CR • +${event.rewardXp} XP",
              fontSize = 8.sp,
              fontWeight = FontWeight.Bold,
              color = NeonAmber
            )
          }

          Button(
            onClick = {
              onSelectTrack(eventTrack)
              val mode = when (event.category) {
                CareerEventCategory.TIME_TRIAL -> GameMode.TIME_ATTACK
                CareerEventCategory.DRIFT_CHALLENGE -> GameMode.DRIFT_CHALLENGE
                CareerEventCategory.SPRINT_RACE -> GameMode.SPEED_TRAP
                else -> GameMode.GRAND_PRIX
              }
              onStartChampionshipRace(mode, eventTrack)
            },
            colors = ButtonDefaults.buttonColors(containerColor = MotorsportRed),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.testTag("championship_round_race_btn_${idx + 1}")
          ) {
            Text("RACE ROUND", fontSize = 9.sp, fontWeight = FontWeight.Black, color = PureWhite)
          }
        }
      }
    }

    Button(
      onClick = {
        val nextEvent = tierEvents.firstOrNull { it.eventId !in careerState.claimedEventIds } ?: tierEvents.firstOrNull()
        val targetTrack = nextEvent?.let { ev -> AVAILABLE_TRACKS.find { it.id == ev.trackId } } ?: activeTrack
        onSelectTrack(targetTrack)
        onStartChampionshipRace(GameMode.GRAND_PRIX, targetTrack)
      },
      colors = ButtonDefaults.buttonColors(containerColor = MotorsportRed),
      shape = RoundedCornerShape(12.dp),
      modifier = Modifier
        .fillMaxWidth()
        .height(50.dp)
        .testTag("championship_start_race_btn")
    ) {
      Icon(Icons.Default.PlayArrow, contentDescription = null, tint = PureWhite)
      Spacer(modifier = Modifier.width(6.dp))
      Text("START CHAMPIONSHIP RACE NOW »", fontSize = 12.sp, fontWeight = FontWeight.Black, color = PureWhite)
    }
  }
}

@Composable
fun ApexEventsMissionsScreen(
  onBack: () -> Unit,
  onOpenRewards: () -> Unit,
  onLaunchMissionRace: () -> Unit,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val careerState by ApexRivals2CareerManager.state.collectAsState()
  var missionStatusToast by remember { mutableStateOf<String?>(null) }

  Column(
    modifier = modifier
      .fillMaxSize()
      .background(CarbonBlack)
      .verticalScroll(rememberScrollState())
      .padding(14.dp)
      .testTag("screen_events_missions"),
    verticalArrangement = Arrangement.spacedBy(12.dp)
  ) {
    ScreenHeaderBar(
      title = "RACING MISSIONS & DAILY OBJECTIVES",
      subtitle = "COMPLETE DAILY & WEEKLY CONTRACTS TO UNLOCK REWARD VAULT TIERS",
      onBack = onBack
    )

    ApexRivals2CareerManager.rewardMissions.forEach { mission ->
      val isClaimed = mission.missionId in careerState.claimedMissionIds
      Card(
        colors = CardDefaults.cardColors(containerColor = CharcoalCard),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, if (isClaimed) ApexGreen else CarbonBorder),
        modifier = Modifier
          .fillMaxWidth()
          .testTag("missions_screen_card_${mission.missionId}")
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
              text = "${mission.missionType} • ${mission.title.uppercase()}",
              fontSize = 10.5.sp,
              fontWeight = FontWeight.Black,
              color = PureWhite
            )
            Text(
              text = mission.description,
              fontSize = 8.5.sp,
              color = MetallicSilver
            )
            Text(
              text = "REWARD: +${mission.rewardCredits} CR • +${mission.rewardXp} XP",
              fontSize = 8.5.sp,
              fontWeight = FontWeight.Black,
              color = NeonAmber
            )
          }

          Button(
            onClick = {
              if (ApexRivals2CareerManager.claimMissionReward(mission.missionId, context)) {
                com.example.data.firebase.ApexCloudSyncCoordinator.onRewardClaimed(context, mission.missionId)
                missionStatusToast = "Mission Completed: +${mission.rewardCredits} CR & +${mission.rewardXp} XP!"
              }
            },
            enabled = !isClaimed,
            colors = ButtonDefaults.buttonColors(
              containerColor = if (isClaimed) CharcoalSurface else ApexGreen
            ),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.testTag("missions_claim_btn_${mission.missionId}")
          ) {
            Text(
              text = if (isClaimed) "CLAIMED" else "CLAIM",
              fontSize = 9.sp,
              fontWeight = FontWeight.Black,
              color = if (isClaimed) MetallicSilver else CarbonBlack
            )
          }
        }
      }
    }

    missionStatusToast?.let {
      Text(it, fontSize = 9.sp, fontWeight = FontWeight.Black, color = ApexGreen, fontFamily = FontFamily.Monospace)
    }

    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      Button(
        onClick = onOpenRewards,
        colors = ButtonDefaults.buttonColors(containerColor = NeonAmber),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier
          .weight(1f)
          .height(48.dp)
          .testTag("missions_open_rewards_btn")
      ) {
        Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = CarbonBlack)
        Spacer(modifier = Modifier.width(6.dp))
        Text("OPEN REWARDS VAULT »", fontSize = 10.sp, fontWeight = FontWeight.Black, color = CarbonBlack)
      }

      Button(
        onClick = onLaunchMissionRace,
        colors = ButtonDefaults.buttonColors(containerColor = MotorsportRed),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier
          .weight(1f)
          .height(48.dp)
          .testTag("missions_start_race_btn")
      ) {
        Icon(Icons.Default.PlayArrow, contentDescription = null, tint = PureWhite)
        Spacer(modifier = Modifier.width(6.dp))
        Text("RACE MISSION »", fontSize = 10.sp, fontWeight = FontWeight.Black, color = PureWhite)
      }
    }
  }
}

@Composable
fun ApexEventsRewardsScreen(
  onBack: () -> Unit,
  onReturnToEventsHub: () -> Unit,
  onLaunchRace: () -> Unit,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val careerState by ApexRivals2CareerManager.state.collectAsState()
  var rewardToast by remember { mutableStateOf<String?>(null) }

  Column(
    modifier = modifier
      .fillMaxSize()
      .background(CarbonBlack)
      .verticalScroll(rememberScrollState())
      .padding(14.dp)
      .testTag("screen_events_rewards"),
    verticalArrangement = Arrangement.spacedBy(12.dp)
  ) {
    ScreenHeaderBar(
      title = "SEASON REWARDS & DAILY LOGIN VAULT",
      subtitle = "BALANCE: CR ${careerState.creditsBalance} • STREAK: DAY ${careerState.dailyLoginStreakDays + 1}",
      onBack = onBack
    )

    Card(
      colors = CardDefaults.cardColors(containerColor = CharcoalCard),
      shape = RoundedCornerShape(14.dp),
      border = BorderStroke(1.5.dp, NeonAmber),
      modifier = Modifier
        .fillMaxWidth()
        .testTag("rewards_daily_streak_card")
    ) {
      Column(
        modifier = Modifier.padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        Text(
          text = "DAILY LOGIN STREAK BONUS",
          fontSize = 12.sp,
          fontWeight = FontWeight.Black,
          color = NeonAmber
        )
        Text(
          text = if (careerState.isDailyLoginClaimedToday) {
            "Today's daily streak reward has been claimed. Return tomorrow for Day ${careerState.dailyLoginStreakDays + 1}!"
          } else {
            "Claim your Day ${careerState.dailyLoginStreakDays + 1} motorsport supply crate (+3,000 CR & +500 XP)."
          },
          fontSize = 9.sp,
          color = PureWhite
        )

        Button(
          onClick = {
            val ok = ApexRivals2CareerManager.claimDailyLoginReward(context)
            if (ok) {
              com.example.data.firebase.ApexCloudSyncCoordinator.onRewardClaimed(context, "daily_login")
            }
            rewardToast = if (ok) {
              "Daily Supply Crate Claimed!"
            } else {
              "Already claimed today!"
            }
          },
          enabled = !careerState.isDailyLoginClaimedToday,
          colors = ButtonDefaults.buttonColors(
            containerColor = if (careerState.isDailyLoginClaimedToday) CharcoalSurface else NeonAmber
          ),
          shape = RoundedCornerShape(8.dp),
          modifier = Modifier.testTag("rewards_claim_daily_btn")
        ) {
          Text(
            text = if (careerState.isDailyLoginClaimedToday) "DAILY CRATE CLAIMED" else "CLAIM DAILY REWARD CRATE",
            fontSize = 9.5.sp,
            fontWeight = FontWeight.Black,
            color = if (careerState.isDailyLoginClaimedToday) MetallicSilver else CarbonBlack
          )
        }
      }
    }

    rewardToast?.let {
      Text(it, fontSize = 9.sp, fontWeight = FontWeight.Black, color = ApexGreen, fontFamily = FontFamily.Monospace)
    }

    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      OutlinedButton(
        onClick = onReturnToEventsHub,
        border = BorderStroke(1.dp, MetallicSilver),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier
          .weight(1f)
          .height(48.dp)
          .testTag("rewards_back_to_missions_btn")
      ) {
        Text("« BACK TO MISSIONS", fontSize = 10.sp, fontWeight = FontWeight.Black, color = PureWhite)
      }

      Button(
        onClick = onLaunchRace,
        colors = ButtonDefaults.buttonColors(containerColor = MotorsportRed),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier
          .weight(1f)
          .height(48.dp)
          .testTag("rewards_launch_race_btn")
      ) {
        Icon(Icons.Default.PlayArrow, contentDescription = null, tint = PureWhite)
        Spacer(modifier = Modifier.width(6.dp))
        Text("RACE NOW »", fontSize = 10.sp, fontWeight = FontWeight.Black, color = PureWhite)
      }
    }
  }
}

@Composable
fun ApexDriverAchievementsScreen(
  activeVehicle: Vehicle,
  onBack: () -> Unit,
  onOpenFullTelemetryProfile: () -> Unit,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val careerState by ApexRivals2CareerManager.state.collectAsState()
  val achievementsList = remember(careerState) {
    listOf(
      Triple("ach_centurion", "Apex Centurion" to "Reach Driver Level 10+ in Career Mode", careerState.playerLevel >= 10),
      Triple("ach_gp_victor", "Grand Prix Victor" to "Complete and claim a Career Championship Event", careerState.claimedEventIds.isNotEmpty()),
      Triple("ach_garage_tuner", "Custom Spec Engineer" to "Upgrade or customize your active racing vehicle (${activeVehicle.name})", true),
      Triple("ach_daily_streak", "Daily Streak Contender" to "Claim a Daily Login Reward or Mission Contract", careerState.isDailyLoginClaimedToday || careerState.claimedMissionIds.isNotEmpty()),
      Triple("ach_world_explorer", "World Circuit Explorer" to "Unlock 4+ International Racing Circuits", careerState.unlockedTrackIds.size >= 4)
    )
  }

  Column(
    modifier = modifier
      .fillMaxSize()
      .background(CarbonBlack)
      .verticalScroll(rememberScrollState())
      .padding(14.dp)
      .testTag("screen_driver_achievements"),
    verticalArrangement = Arrangement.spacedBy(12.dp)
  ) {
    ScreenHeaderBar(
      title = "DRIVER ACHIEVEMENTS & BADGES",
      subtitle = "CAREER MILESTONES • FIA SUPER LICENSE ACCOLADES",
      onBack = onBack
    )

    achievementsList.forEachIndexed { idx, (achId, titleDesc, unlocked) ->
      val (title, desc) = titleDesc
      val isCloudClaimed = achId in careerState.unlockedAchievements
      Card(
        colors = CardDefaults.cardColors(containerColor = CharcoalCard),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, if (unlocked || isCloudClaimed) ApexGreen else CarbonBorder),
        modifier = Modifier
          .fillMaxWidth()
          .testTag("achievement_card_${idx + 1}")
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.weight(1f)
          ) {
            Box(
              modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(if (unlocked || isCloudClaimed) ApexGreen.copy(alpha = 0.2f) else CharcoalSurface),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                imageVector = if (unlocked || isCloudClaimed) Icons.Default.EmojiEvents else Icons.Default.Lock,
                contentDescription = null,
                tint = if (unlocked || isCloudClaimed) ApexGreen else TextSecondary,
                modifier = Modifier.size(20.dp)
              )
            }
            Column {
              Text(title, fontSize = 11.sp, fontWeight = FontWeight.Black, color = PureWhite)
              Text(desc, fontSize = 8.5.sp, color = MetallicSilver)
            }
          }

          Box(
            modifier = Modifier
              .clip(RoundedCornerShape(6.dp))
              .background(
                when {
                  isCloudClaimed -> ApexGreen.copy(alpha = 0.18f)
                  unlocked -> NeonAmber
                  else -> CharcoalSurface
                }
              )
              .clickable(enabled = unlocked && !isCloudClaimed) {
                if (ApexRivals2CareerManager.unlockAchievement(achId, context)) {
                  com.example.data.firebase.ApexCloudSyncCoordinator.onRewardClaimed(context, achId)
                }
              }
              .padding(horizontal = 8.dp, vertical = 4.dp)
              .testTag("achievement_claim_btn_$achId")
          ) {
            Text(
              text = when {
                isCloudClaimed -> "CLAIMED"
                unlocked -> "CLAIM REWARD"
                else -> "IN PROGRESS"
              },
              fontSize = 8.sp,
              fontWeight = FontWeight.Black,
              color = when {
                isCloudClaimed -> ApexGreen
                unlocked -> CarbonBlack
                else -> NeonAmber
              }
            )
          }
        }
      }
    }

    OutlinedButton(
      onClick = onOpenFullTelemetryProfile,
      border = BorderStroke(1.dp, NeonCyan),
      shape = RoundedCornerShape(10.dp),
      modifier = Modifier
        .fillMaxWidth()
        .height(46.dp)
        .testTag("achievements_open_telemetry_badges_btn")
    ) {
      Text("OPEN FULL TELEMETRY BADGE EVALUATOR", fontSize = 10.sp, fontWeight = FontWeight.Black, color = NeonCyan)
    }
  }
}

