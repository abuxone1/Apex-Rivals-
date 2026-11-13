package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.game.ui.RacingGameScreen
import com.example.ui.navigation.NAV_ITEMS
import com.example.ui.navigation.Screen
import com.example.ui.screens.AiCoachScreen
import com.example.ui.screens.GarageSocialScreen
import com.example.ui.screens.GlobalRankingScreen
import com.example.ui.screens.GooglePlayPublishScreen
import com.example.ui.screens.LeaderboardScreen
import com.example.ui.screens.LiveTelemetryScreen
import com.example.ui.screens.MultiplayerScreen
import com.example.ui.screens.PerformanceMetricsScreen
import com.example.ui.screens.ReplayEngineScreen
import com.example.ui.screens.TelemetryAnalyzerScreen
import com.example.ui.screens.UserProfileScreen
import com.example.ui.theme.ApexGreen
import com.example.ui.theme.CarbonBlack
import com.example.ui.theme.CarbonBorder
import com.example.ui.theme.CarbonSurface
import com.example.ui.theme.CarbonSurfaceVariant
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.PlatformAndroid
import com.example.ui.theme.RedlineRed
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.RaceViewModel
import com.example.util.configureAppCheck

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
    enableEdgeToEdge()
    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
      window.attributes = window.attributes.apply {
        layoutInDisplayCutoutMode =
          android.view.WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
      }
    }
    androidx.core.view.WindowCompat.getInsetsController(window, window.decorView).apply {
      systemBarsBehavior =
        androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    }
    configureAppCheck(this, intent)
    setContent {
      MyApplicationTheme {
        ApexRivalsApp()
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApexRivalsApp(
  viewModel: RaceViewModel = viewModel()
) {
  val context = androidx.compose.ui.platform.LocalContext.current
  val startupScope = androidx.compose.runtime.rememberCoroutineScope()
  val uiState by viewModel.uiState.collectAsState()
  var currentScreen by remember { mutableStateOf<Screen>(Screen.ApexMainMenu) }
  var screenBackStack by remember { mutableStateOf(listOf<Screen>()) }
  var activeFrontendSubScreen by remember {
    mutableStateOf(com.example.game.v2.FrontendSubScreen.LOADING_SCREEN)
  }
  var activeGameMode by remember {
    mutableStateOf(com.example.game.model.GameMode.GRAND_PRIX)
  }

  androidx.compose.runtime.LaunchedEffect(Unit) {
    com.example.game.v2.ApexRivals2CareerManager.initialize(context)
    com.example.game.v2.ApexRivals2SettingsRepository.initialize(context)
    com.example.data.firebase.ApexCloudSyncCoordinator.startObservingAuth(context, startupScope)
    try {
      if (com.google.firebase.FirebaseApp.getApps(context).isNotEmpty()) {
        val credentialManager = androidx.credentials.CredentialManager.create(context)
        com.example.data.firebase.ApexCredentialAuthHelper.attemptAutoSignIn(
          context = context,
          credentialManager = credentialManager,
          onAuthSuccess = {
            com.example.data.firebase.ApexCloudSyncCoordinator.startObservingAuth(context, startupScope)
          },
          onUnauthenticated = {},
          scope = startupScope
        )
      }
    } catch (_: Exception) {
      // Safe fallback in Robolectric / headless test environments
    }
  }

  fun navigateToScreen(target: Screen) {
    if (currentScreen == Screen.ApexMainMenu &&
      activeFrontendSubScreen == com.example.game.v2.FrontendSubScreen.LOADING_SCREEN
    ) {
      activeFrontendSubScreen = com.example.game.v2.FrontendSubScreen.MAIN_MENU
    }
    if (currentScreen != target) {
      screenBackStack = (screenBackStack + currentScreen).takeLast(12)
      currentScreen = target
    }
  }

  fun popScreenOrReturnToMenu(targetSubScreen: com.example.game.v2.FrontendSubScreen? = null) {
    if (targetSubScreen != null) {
      activeFrontendSubScreen = targetSubScreen
      screenBackStack = emptyList()
      currentScreen = Screen.ApexMainMenu
      return
    }
    val previous = screenBackStack.lastOrNull()
    if (previous != null) {
      screenBackStack = screenBackStack.dropLast(1)
      if (previous == Screen.ApexMainMenu &&
        activeFrontendSubScreen == com.example.game.v2.FrontendSubScreen.LOADING_SCREEN
      ) {
        activeFrontendSubScreen = com.example.game.v2.FrontendSubScreen.MAIN_MENU
      }
      currentScreen = previous
    } else {
      if (activeFrontendSubScreen == com.example.game.v2.FrontendSubScreen.LOADING_SCREEN) {
        activeFrontendSubScreen = com.example.game.v2.FrontendSubScreen.MAIN_MENU
      }
      currentScreen = Screen.ApexMainMenu
    }
  }

  val isImmersive = currentScreen == Screen.ApexMainMenu ||
    currentScreen == Screen.RacingGame ||
    currentScreen == Screen.WebGLGame ||
    currentScreen == Screen.StandalonePrototype

  Scaffold(
    modifier = Modifier.fillMaxSize(),
    containerColor = CarbonBlack,
    topBar = {
      if (!isImmersive) {
        TopAppBar(
          colors = TopAppBarDefaults.topAppBarColors(
            containerColor = CarbonBlack,
            titleContentColor = TextPrimary
          ),
          title = {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              // App Title & Tagline
              Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                  modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(NeonCyan),
                  contentAlignment = Alignment.Center
                ) {
                  Icon(
                    imageVector = Icons.Default.Speed,
                    contentDescription = "Logo",
                    tint = CarbonBlack,
                    modifier = Modifier.size(18.dp)
                  )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                  Text(
                    text = "APEX RIVALS",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Black,
                    color = TextPrimary,
                    letterSpacing = 1.sp
                  )
                  Text(
                    text = "ESPORTS PERFORMANCE TELEMETRY",
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    color = NeonCyan,
                    letterSpacing = 0.5.sp
                  )
                }
              }

              // Speed Unit Quick Toggle, AI Coach & NetSync Indicator
              Row(verticalAlignment = Alignment.CenterVertically) {
                // Apex Rivals 2.0 Unified Main Menu Hub Button
                Box(
                  modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (currentScreen == Screen.ApexMainMenu) RedlineRed else CarbonSurfaceVariant)
                    .border(1.dp, RedlineRed, RoundedCornerShape(6.dp))
                    .clickable {
                      popScreenOrReturnToMenu(com.example.game.v2.FrontendSubScreen.MAIN_MENU)
                    }
                    .padding(horizontal = 7.dp, vertical = 4.dp)
                    .testTag("main_menu_hub_button")
                ) {
                  Text(
                    text = "MENU 2.0",
                    fontSize = 8.5.sp,
                    fontWeight = FontWeight.Black,
                    color = TextPrimary
                  )
                }

                Spacer(modifier = Modifier.width(6.dp))

                // 3D Racing Game Quick Launch Button
                Box(
                  modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (currentScreen == Screen.RacingGame) RedlineRed else NeonCyan)
                    .clickable { navigateToScreen(Screen.RacingGame) }
                    .padding(horizontal = 8.dp, vertical = 4.dp)
                    .testTag("play_game_top_button")
                ) {
                  Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                      imageVector = Icons.Default.Speed,
                      contentDescription = "Play Race Game",
                      tint = CarbonBlack,
                      modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                      text = "RACE NOW",
                      fontSize = 9.sp,
                      fontWeight = FontWeight.Black,
                      color = CarbonBlack
                    )
                  }
                }

                Spacer(modifier = Modifier.width(6.dp))

                Box(
                  modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (currentScreen == Screen.TelemetryAnalyzer) NeonAmber else CarbonSurfaceVariant)
                    .border(1.dp, if (currentScreen == Screen.TelemetryAnalyzer) NeonAmber else CarbonBorder, RoundedCornerShape(6.dp))
                    .clickable { navigateToScreen(Screen.TelemetryAnalyzer) }
                    .padding(horizontal = 7.dp, vertical = 4.dp)
                    .testTag("telemetry_analyzer_quick_button")
                ) {
                  Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                      imageVector = Icons.Default.Speed,
                      contentDescription = "Corner & Delta Analyzer",
                      tint = if (currentScreen == Screen.TelemetryAnalyzer) CarbonBlack else NeonAmber,
                      modifier = Modifier.size(11.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                      text = "DELTA",
                      fontSize = 9.sp,
                      fontWeight = FontWeight.Black,
                      color = if (currentScreen == Screen.TelemetryAnalyzer) CarbonBlack else TextPrimary
                    )
                  }
                }

                Spacer(modifier = Modifier.width(6.dp))

                Box(
                  modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (currentScreen == Screen.AiCoach) NeonCyan else CarbonSurfaceVariant)
                    .border(1.dp, if (currentScreen == Screen.AiCoach) NeonCyan else CarbonBorder, RoundedCornerShape(6.dp))
                    .clickable { navigateToScreen(Screen.AiCoach) }
                    .padding(horizontal = 7.dp, vertical = 4.dp)
                    .testTag("ai_coach_quick_button")
                ) {
                  Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                      imageVector = Icons.Default.AutoAwesome,
                      contentDescription = null,
                      tint = if (currentScreen == Screen.AiCoach) CarbonBlack else NeonCyan,
                      modifier = Modifier.size(11.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                      text = "AI COACH",
                      fontSize = 9.sp,
                      fontWeight = FontWeight.Black,
                      color = if (currentScreen == Screen.AiCoach) CarbonBlack else TextPrimary
                    )
                  }
                }

                Spacer(modifier = Modifier.width(6.dp))

                Box(
                  modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (currentScreen == Screen.Profile) NeonCyan else CarbonSurfaceVariant)
                    .border(1.dp, if (currentScreen == Screen.Profile) NeonCyan else CarbonBorder, RoundedCornerShape(6.dp))
                    .clickable { navigateToScreen(Screen.Profile) }
                    .padding(horizontal = 7.dp, vertical = 4.dp)
                    .testTag("profile_button")
                ) {
                  Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                      imageVector = Icons.Default.AccountCircle,
                      contentDescription = "Profile",
                      tint = if (currentScreen == Screen.Profile) CarbonBlack else NeonCyan,
                      modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                      text = "PROFILE",
                      fontSize = 9.sp,
                      fontWeight = FontWeight.Black,
                      color = if (currentScreen == Screen.Profile) CarbonBlack else TextPrimary
                    )
                  }
                }

                Spacer(modifier = Modifier.width(6.dp))

                Box(
                  modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (currentScreen == Screen.GooglePlayPublish) PlatformAndroid else CarbonSurfaceVariant)
                    .border(1.dp, if (currentScreen == Screen.GooglePlayPublish) PlatformAndroid else CarbonBorder, RoundedCornerShape(6.dp))
                    .clickable { navigateToScreen(Screen.GooglePlayPublish) }
                    .padding(horizontal = 7.dp, vertical = 4.dp)
                    .testTag("play_console_button")
                ) {
                  Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                      imageVector = Icons.Default.CloudUpload,
                      contentDescription = "Play Console",
                      tint = if (currentScreen == Screen.GooglePlayPublish) CarbonBlack else PlatformAndroid,
                      modifier = Modifier.size(11.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                      text = "CONSOLE",
                      fontSize = 9.sp,
                      fontWeight = FontWeight.Black,
                      color = if (currentScreen == Screen.GooglePlayPublish) CarbonBlack else TextPrimary
                    )
                  }
                }

                Spacer(modifier = Modifier.width(6.dp))

                Box(
                  modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(CarbonSurfaceVariant)
                    .border(1.dp, CarbonBorder, RoundedCornerShape(6.dp))
                    .clickable { viewModel.toggleSpeedUnit() }
                    .padding(horizontal = 7.dp, vertical = 4.dp)
                    .testTag("unit_toggle_button")
                ) {
                  Text(
                    text = if (uiState.isMph) "MPH" else "KM/H",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    color = NeonAmber,
                    fontFamily = FontFamily.Monospace
                  )
                }

                Spacer(modifier = Modifier.width(6.dp))

                Box(
                  modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(ApexGreen.copy(alpha = 0.15f))
                    .border(1.dp, ApexGreen.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                    .padding(horizontal = 6.dp, vertical = 4.dp)
                ) {
                  Text(
                    text = "60Hz LIVE",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Black,
                    color = ApexGreen,
                    fontFamily = FontFamily.Monospace
                  )
                }
              }
            }
          }
        )
      }
    },
    bottomBar = {
      if (!isImmersive) {
        NavigationBar(
          containerColor = CarbonSurface,
          contentColor = TextPrimary,
          tonalElevation = 8.dp,
          modifier = Modifier
            .border(1.dp, CarbonBorder)
            .testTag("main_navigation_bar")
        ) {
          NAV_ITEMS.forEach { screen ->
            val isSelected = screen.route == currentScreen.route
            NavigationBarItem(
              selected = isSelected,
              onClick = { navigateToScreen(screen) },
              icon = {
                Icon(
                  imageVector = screen.icon,
                  contentDescription = screen.title,
                  modifier = Modifier.size(20.dp)
                )
              },
              label = {
                Text(
                  text = screen.title,
                  fontSize = 9.sp,
                  fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                )
              },
              colors = NavigationBarItemDefaults.colors(
                selectedIconColor = CarbonBlack,
                selectedTextColor = NeonCyan,
                indicatorColor = NeonCyan,
                unselectedIconColor = TextSecondary,
                unselectedTextColor = TextSecondary
              ),
              modifier = Modifier.testTag("nav_item_${screen.route}")
            )
          }
        }
      }
    }
  ) { innerPadding ->
    Box(
      modifier = Modifier
        .fillMaxSize()
        .padding(if (isImmersive) androidx.compose.foundation.layout.PaddingValues(0.dp) else innerPadding)
    ) {
      Crossfade(targetState = currentScreen, label = "screenTransition") { targetScreen ->
        when (targetScreen) {
          Screen.LiveDrive -> {
            androidx.activity.compose.BackHandler { popScreenOrReturnToMenu() }
            LiveTelemetryScreen(
              uiState = uiState,
              viewModel = viewModel,
              onPlayGame = { navigateToScreen(Screen.RacingGame) },
              onNavigateToAnalyzer = { navigateToScreen(Screen.TelemetryAnalyzer) }
            )
          }
          Screen.RacingGame, Screen.WebGLGame -> RacingGameScreen(
            vehicle = uiState.activeVehicle,
            track = uiState.activeTrack,
            onNavigateBack = {
              popScreenOrReturnToMenu(com.example.game.v2.FrontendSubScreen.MAIN_MENU)
            },
            onNavigateToProfile = {
              popScreenOrReturnToMenu(com.example.game.v2.FrontendSubScreen.DRIVER_PROFILE)
            },
            onNavigateToGarage = {
              popScreenOrReturnToMenu(com.example.game.v2.FrontendSubScreen.VEHICLE_GARAGE)
            },
            initialGameMode = activeGameMode,
            isMph = uiState.isMph
          )
          Screen.AiCoach -> {
            androidx.activity.compose.BackHandler { popScreenOrReturnToMenu() }
            AiCoachScreen(
              uiState = uiState,
              viewModel = viewModel
            )
          }
          Screen.Replay -> {
            androidx.activity.compose.BackHandler { popScreenOrReturnToMenu() }
            ReplayEngineScreen(
              uiState = uiState,
              viewModel = viewModel
            )
          }
          Screen.Leaderboards -> {
            androidx.activity.compose.BackHandler { popScreenOrReturnToMenu() }
            LeaderboardScreen(
              uiState = uiState,
              viewModel = viewModel,
              onNavigateToReplay = { navigateToScreen(Screen.Replay) },
              onNavigateToGlobalRankings = { navigateToScreen(Screen.GlobalRankings) }
            )
          }
          Screen.GlobalRankings -> {
            androidx.activity.compose.BackHandler { popScreenOrReturnToMenu() }
            GlobalRankingScreen(
              uiState = uiState,
              viewModel = viewModel,
              onNavigateToReplay = { navigateToScreen(Screen.Replay) }
            )
          }
          Screen.PerformanceHistory -> {
            androidx.activity.compose.BackHandler { popScreenOrReturnToMenu() }
            PerformanceMetricsScreen(
              onNavigateToAnalyzer = { navigateToScreen(Screen.TelemetryAnalyzer) },
              onNavigateBack = { popScreenOrReturnToMenu() }
            )
          }
          Screen.TelemetryAnalyzer -> {
            androidx.activity.compose.BackHandler { popScreenOrReturnToMenu() }
            TelemetryAnalyzerScreen(
              uiState = uiState,
              viewModel = viewModel,
              onNavigateBack = { popScreenOrReturnToMenu() }
            )
          }
          Screen.Multiplayer -> {
            androidx.activity.compose.BackHandler { popScreenOrReturnToMenu() }
            MultiplayerScreen(
              uiState = uiState,
              viewModel = viewModel,
              onJoinRace = { navigateToScreen(Screen.RacingGame) }
            )
          }
          Screen.GarageSocial -> {
            androidx.activity.compose.BackHandler { popScreenOrReturnToMenu() }
            GarageSocialScreen(
              uiState = uiState,
              viewModel = viewModel,
              onNavigateToProfile = { navigateToScreen(Screen.Profile) }
            )
          }
          Screen.Profile -> {
            androidx.activity.compose.BackHandler { popScreenOrReturnToMenu() }
            UserProfileScreen(
              onNavigateBack = { popScreenOrReturnToMenu() },
              onNavigateToTelemetryDb = { navigateToScreen(Screen.PerformanceHistory) },
              onNavigateToPlayConsole = { navigateToScreen(Screen.GooglePlayPublish) },
              initialIsMph = uiState.isMph
            )
          }
          Screen.GooglePlayPublish -> {
            androidx.activity.compose.BackHandler { popScreenOrReturnToMenu() }
            GooglePlayPublishScreen(
              onNavigateBack = { popScreenOrReturnToMenu() }
            )
          }
          Screen.StandalonePrototype -> {
            androidx.activity.compose.BackHandler { popScreenOrReturnToMenu() }
            com.aistudio.apexrivals.tr8x.ApexRivalsGame()
          }
          Screen.ApexMainMenu -> {
            com.example.game.v2.ApexRivals2FrontendHubScreen(
              activeVehicle = uiState.activeVehicle,
              activeTrack = uiState.activeTrack,
              onSelectVehicle = { viewModel.selectVehicle(it) },
              onSelectTrack = { viewModel.selectTrack(it) },
              onLaunchRaceHud = { mode ->
                activeGameMode = mode
                navigateToScreen(Screen.RacingGame)
              },
              onNavigateToProfile = { navigateToScreen(Screen.Profile) },
              onNavigateToMultiplayer = { navigateToScreen(Screen.Multiplayer) },
              initialSubScreen = activeFrontendSubScreen,
              externalSubScreen = activeFrontendSubScreen,
              onExternalSubScreenChange = { activeFrontendSubScreen = it },
              onOpenTelemetryHub = { navigateToScreen(Screen.LiveDrive) }
            )
          }
        }
      }
    }
  }
}
