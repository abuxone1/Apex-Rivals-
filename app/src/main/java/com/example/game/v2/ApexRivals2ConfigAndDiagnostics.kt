package com.example.game.v2

import android.app.ActivityManager
import android.content.Context
import com.example.game.model.DrivingAssistLevel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * PROMPT 1 & PROMPT 9: Graphics Quality Presets with automatic device capability detection.
 */
enum class GraphicsQualityPreset(
  val displayName: String,
  val shortLabel: String,
  val roadSliceCount: Int,
  val enableDynamicReflections: Boolean,
  val enableRealtimeShadows: Boolean,
  val shadowLayers: Int,
  val tireSmokeParticles: Int,
  val weatherParticleCount: Int,
  val lightingRayCount: Int,
  val roadTextureDetailLevel: Int,
  val enableClearcoatMaterials: Boolean,
  val enableHeadlightCones: Boolean,
  val sceneryLodDistanceMultiplier: Float,
  val targetFps: Int
) {
  LOW(
    displayName = "Low (Battery Saver • 60 FPS)",
    shortLabel = "LOW",
    roadSliceCount = 28,
    enableDynamicReflections = false,
    enableRealtimeShadows = false,
    shadowLayers = 1,
    tireSmokeParticles = 3,
    weatherParticleCount = 22,
    lightingRayCount = 0,
    roadTextureDetailLevel = 1,
    enableClearcoatMaterials = false,
    enableHeadlightCones = false,
    sceneryLodDistanceMultiplier = 0.7f,
    targetFps = 60
  ),
  MEDIUM(
    displayName = "Medium (Balanced Motorsport HD)",
    shortLabel = "MEDIUM",
    roadSliceCount = 42,
    enableDynamicReflections = true,
    enableRealtimeShadows = true,
    shadowLayers = 2,
    tireSmokeParticles = 6,
    weatherParticleCount = 46,
    lightingRayCount = 3,
    roadTextureDetailLevel = 2,
    enableClearcoatMaterials = true,
    enableHeadlightCones = true,
    sceneryLodDistanceMultiplier = 1.0f,
    targetFps = 60
  ),
  HIGH(
    displayName = "High (Ultra 3D Reflections & Smoke)",
    shortLabel = "HIGH",
    roadSliceCount = 54,
    enableDynamicReflections = true,
    enableRealtimeShadows = true,
    shadowLayers = 3,
    tireSmokeParticles = 12,
    weatherParticleCount = 78,
    lightingRayCount = 6,
    roadTextureDetailLevel = 3,
    enableClearcoatMaterials = true,
    enableHeadlightCones = true,
    sceneryLodDistanceMultiplier = 1.35f,
    targetFps = 60
  );

  companion object {
    /**
     * Inspects device RAM, JVM heap budget, low-RAM flag, and CPU cores to recommend
     * the optimal graphics preset automatically for different Android devices.
     */
    fun detectRecommendedPreset(context: Context): GraphicsQualityPreset {
      return try {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        val memoryInfo = ActivityManager.MemoryInfo()
        activityManager?.getMemoryInfo(memoryInfo)
        val totalRamMb = if (memoryInfo.totalMem > 0L) {
          memoryInfo.totalMem / (1024L * 1024L)
        } else {
          4096L
        }
        val maxHeapMb = Runtime.getRuntime().maxMemory() / (1024L * 1024L)
        val cpuCores = Runtime.getRuntime().availableProcessors()
        val isLowRamDevice = activityManager?.isLowRamDevice == true || memoryInfo.lowMemory

        when {
          isLowRamDevice || totalRamMb < 3200L || cpuCores <= 4 || maxHeapMb < 192L -> LOW
          totalRamMb >= 5800L && cpuCores >= 8 && maxHeapMb >= 256L -> HIGH
          else -> MEDIUM
        }
      } catch (_: Exception) {
        MEDIUM
      }
    }
  }
}

/**
 * PROMPT 2: Three Driving Control Options (Touch Buttons, Steering Wheel, Gyroscope Steering).
 */
enum class DrivingControlMode(
  val displayName: String,
  val shortLabel: String,
  val description: String
) {
  TOUCH_BUTTONS(
    displayName = "Touch Buttons",
    shortLabel = "BUTTONS",
    description = "Precision left/right directional touch pads with instant apex response."
  ),
  STEERING_WHEEL(
    displayName = "Rotary Steering Wheel",
    shortLabel = "WHEEL",
    description = "Virtual GT3 rotary wheel with continuous analog steering angle."
  ),
  GYROSCOPE(
    displayName = "Gyroscope Tilt",
    shortLabel = "GYRO",
    description = "Device accelerometer/gyroscope tilt steering with deadzone filtering."
  )
}

/**
 * PROMPT 2: Complete driving control & sensitivity configuration.
 */
data class VehicleControlConfig(
  val controlMode: DrivingControlMode = DrivingControlMode.TOUCH_BUTTONS,
  val steeringSensitivity: Float = 1.0f, // 0.5f .. 2.0f
  val assistLevel: DrivingAssistLevel = DrivingAssistLevel.ROOKIE
)

/**
 * PROMPT 8: Independent volume controls for Music, Engine Sounds, and Sound Effects (SFX).
 */
data class AudioMixerSettings(
  val musicVolume: Float = 0.75f,
  val engineVolume: Float = 0.90f,
  val sfxVolume: Float = 0.85f,
  val isMuted: Boolean = false
)

/**
 * PROMPT 7: Backend Environment Selection (Staging vs Production) for safe multiplayer sync.
 */
enum class MultiplayerBackendEnv(val label: String, val endpointTag: String) {
  STAGING("Staging Sandbox", "staging-apexrivals-v2"),
  PRODUCTION("Production Live", "prod-apexrivals-v2")
}

data class DiagnosticLogEntry(
  val timestampMs: Long,
  val formattedTime: String,
  val subsystem: String,
  val message: String,
  val isWarning: Boolean = false
)

enum class FrameRatePreference(val fps: Int, val label: String) {
  FPS_30(30, "30 FPS (Battery Saver)"),
  FPS_60(60, "60 FPS (Pro Motorsport)"),
  FPS_90(90, "90/120 FPS (High Refresh)")
}

enum class AppLanguageOption(val code: String, val displayName: String) {
  ENGLISH("EN", "English (US/UK)"),
  SPANISH("ES", "Español"),
  GERMAN("DE", "Deutsch"),
  FRENCH("FR", "Français"),
  JAPANESE("JA", "日本語"),
  PORTUGUESE("PT", "Português")
}

data class PrivacyPreferences(
  val shareCloudGhosts: Boolean = true,
  val crashDiagnosticsEnabled: Boolean = true,
  val personalizedTelemetryEnabled: Boolean = false
)

/**
 * PROMPT 9 & SCREEN 10: Central Configuration & Diagnostic Telemetry Logger for Apex Rivals 2.0.
 */
object ApexRivals2SettingsRepository {
  private val _graphicsPreset = MutableStateFlow(GraphicsQualityPreset.HIGH)
  val graphicsPreset: StateFlow<GraphicsQualityPreset> = _graphicsPreset.asStateFlow()

  private val _isAutoGraphicsEnabled = MutableStateFlow(true)
  val isAutoGraphicsEnabled: StateFlow<Boolean> = _isAutoGraphicsEnabled.asStateFlow()

  private val _frameRatePref = MutableStateFlow(FrameRatePreference.FPS_60)
  val frameRatePref: StateFlow<FrameRatePreference> = _frameRatePref.asStateFlow()

  private val _languageOption = MutableStateFlow(AppLanguageOption.ENGLISH)
  val languageOption: StateFlow<AppLanguageOption> = _languageOption.asStateFlow()

  private val _privacyPrefs = MutableStateFlow(PrivacyPreferences())
  val privacyPrefs: StateFlow<PrivacyPreferences> = _privacyPrefs.asStateFlow()

  private val _controlConfig = MutableStateFlow(VehicleControlConfig())
  val controlConfig: StateFlow<VehicleControlConfig> = _controlConfig.asStateFlow()

  private val _audioMixer = MutableStateFlow(AudioMixerSettings())
  val audioMixer: StateFlow<AudioMixerSettings> = _audioMixer.asStateFlow()

  private val _backendEnv = MutableStateFlow(MultiplayerBackendEnv.PRODUCTION)
  val backendEnv: StateFlow<MultiplayerBackendEnv> = _backendEnv.asStateFlow()

  private val _diagnosticLogs = MutableStateFlow<List<DiagnosticLogEntry>>(emptyList())
  val diagnosticLogs: StateFlow<List<DiagnosticLogEntry>> = _diagnosticLogs.asStateFlow()

  private const val SETTINGS_PREFS_NAME = "apex_rivals_v2_settings"
  private const val LEGACY_V8_PREFS_NAME = "apex_rivals_prefs"
  private var avgFrameTimeMs: Float = 16.2f
  private var autoScaleCooldownFrames: Int = 0
  private var isInitialized = false
  private var appContext: Context? = null

  fun initialize(context: Context, forceReload: Boolean = false) {
    appContext = context.applicationContext
    if (isInitialized && !forceReload) return
    isInitialized = true
    val recommended = GraphicsQualityPreset.detectRecommendedPreset(context)
    try {
      val prefs = context.getSharedPreferences(SETTINGS_PREFS_NAME, Context.MODE_PRIVATE)
      val legacyPrefs = context.getSharedPreferences(LEGACY_V8_PREFS_NAME, Context.MODE_PRIVATE)
      val alreadyMigratedV8Settings = prefs.getBoolean("v8_settings_migrated", false)

      val savedPresetName = prefs.getString("graphics_preset", null)
        ?: if (!alreadyMigratedV8Settings) legacyPrefs.getString("graphics_preset", null) else null
      val resolvedPreset = savedPresetName?.let { name ->
        GraphicsQualityPreset.entries.find { it.name == name }
      } ?: recommended

      val resolvedAutoGraphics = if (prefs.contains("is_auto_graphics")) {
        prefs.getBoolean("is_auto_graphics", true)
      } else if (!alreadyMigratedV8Settings && legacyPrefs.contains("graphics_preset")) {
        false
      } else {
        true
      }

      val savedControlModeName = prefs.getString("control_mode", null)
        ?: if (!alreadyMigratedV8Settings) legacyPrefs.getString("control_mode", null) else null
      val resolvedControlMode = savedControlModeName?.let { name ->
        DrivingControlMode.entries.find { it.name == name }
      } ?: DrivingControlMode.TOUCH_BUTTONS

      val resolvedSensitivity = when {
        prefs.contains("steering_sensitivity") -> prefs.getFloat("steering_sensitivity", 1.0f)
        !alreadyMigratedV8Settings && legacyPrefs.contains("steering_sensitivity") ->
          legacyPrefs.getFloat("steering_sensitivity", 1.0f)
        else -> 1.0f
      }.coerceIn(0.5f, 2.0f)

      val savedAssistName = prefs.getString("assist_level", null)
        ?: if (!alreadyMigratedV8Settings) legacyPrefs.getString("assist_level", null) else null
      val resolvedAssist = savedAssistName?.let { name ->
        DrivingAssistLevel.entries.find { it.name == name }
      } ?: DrivingAssistLevel.ROOKIE

      val resolvedMusicVol = when {
        prefs.contains("music_volume") -> prefs.getFloat("music_volume", 0.75f)
        !alreadyMigratedV8Settings && legacyPrefs.contains("music_volume") ->
          legacyPrefs.getFloat("music_volume", 0.75f)
        else -> 0.75f
      }.coerceIn(0f, 1f)

      val resolvedEngineVol = when {
        prefs.contains("engine_volume") -> prefs.getFloat("engine_volume", 0.90f)
        !alreadyMigratedV8Settings && legacyPrefs.contains("engine_volume") ->
          legacyPrefs.getFloat("engine_volume", 0.90f)
        else -> 0.90f
      }.coerceIn(0f, 1f)

      val resolvedSfxVol = when {
        prefs.contains("sfx_volume") -> prefs.getFloat("sfx_volume", 0.85f)
        !alreadyMigratedV8Settings && legacyPrefs.contains("sfx_volume") ->
          legacyPrefs.getFloat("sfx_volume", 0.85f)
        else -> 0.85f
      }.coerceIn(0f, 1f)

      val resolvedMuted = when {
        prefs.contains("is_muted") -> prefs.getBoolean("is_muted", false)
        !alreadyMigratedV8Settings && legacyPrefs.contains("is_muted") ->
          legacyPrefs.getBoolean("is_muted", false)
        else -> false
      }

      val savedLangCode = prefs.getString("language_code", null)
        ?: if (!alreadyMigratedV8Settings) legacyPrefs.getString("language_code", null) else null
      val resolvedLang = savedLangCode?.let { code ->
        AppLanguageOption.entries.find { it.code == code || it.name == code }
      } ?: AppLanguageOption.ENGLISH

      val savedFps = if (prefs.contains("frame_rate_fps")) {
        prefs.getInt("frame_rate_fps", 60)
      } else if (!alreadyMigratedV8Settings && legacyPrefs.contains("frame_rate_fps")) {
        legacyPrefs.getInt("frame_rate_fps", 60)
      } else {
        60
      }
      val resolvedFpsPref = FrameRatePreference.entries.find { it.fps == savedFps } ?: FrameRatePreference.FPS_60

      _graphicsPreset.value = resolvedPreset
      _isAutoGraphicsEnabled.value = resolvedAutoGraphics
      _controlConfig.value = VehicleControlConfig(
        controlMode = resolvedControlMode,
        steeringSensitivity = resolvedSensitivity,
        assistLevel = resolvedAssist
      )
      _audioMixer.value = AudioMixerSettings(
        musicVolume = resolvedMusicVol,
        engineVolume = resolvedEngineVol,
        sfxVolume = resolvedSfxVol,
        isMuted = resolvedMuted
      )
      _languageOption.value = resolvedLang
      _frameRatePref.value = resolvedFpsPref

      if (!alreadyMigratedV8Settings) {
        prefs.edit().putBoolean("v8_settings_migrated", true).apply()
        persistCurrentSettings()
      }
    } catch (_: Exception) {
      _graphicsPreset.value = recommended
      _isAutoGraphicsEnabled.value = true
    }
    avgFrameTimeMs = 16.2f
    autoScaleCooldownFrames = 0
    logEvent(
      subsystem = "GRAPHICS_ENGINE",
      message = "Initialized graphics preset: ${_graphicsPreset.value.shortLabel} (${_graphicsPreset.value.roadSliceCount} road slices)"
    )
  }

  private fun persistCurrentSettings() {
    val ctx = appContext ?: return
    try {
      ctx.getSharedPreferences(SETTINGS_PREFS_NAME, Context.MODE_PRIVATE).edit()
        .putString("graphics_preset", _graphicsPreset.value.name)
        .putBoolean("is_auto_graphics", _isAutoGraphicsEnabled.value)
        .putString("control_mode", _controlConfig.value.controlMode.name)
        .putFloat("steering_sensitivity", _controlConfig.value.steeringSensitivity)
        .putString("assist_level", _controlConfig.value.assistLevel.name)
        .putFloat("music_volume", _audioMixer.value.musicVolume)
        .putFloat("engine_volume", _audioMixer.value.engineVolume)
        .putFloat("sfx_volume", _audioMixer.value.sfxVolume)
        .putBoolean("is_muted", _audioMixer.value.isMuted)
        .putString("language_code", _languageOption.value.code)
        .putInt("frame_rate_fps", _frameRatePref.value.fps)
        .apply()
    } catch (_: Exception) {
      // Safe fallback in unit test environments
    }
  }

  fun applyRecommendedPreset(context: Context): GraphicsQualityPreset {
    val recommended = GraphicsQualityPreset.detectRecommendedPreset(context)
    _isAutoGraphicsEnabled.value = true
    _graphicsPreset.value = recommended
    avgFrameTimeMs = 16.2f
    autoScaleCooldownFrames = 0
    persistCurrentSettings()
    logEvent(
      subsystem = "GRAPHICS_ENGINE",
      message = "Auto-selected recommended graphics profile: ${recommended.displayName}"
    )
    return recommended
  }

  fun setAutoGraphicsEnabled(enabled: Boolean, context: Context? = null) {
    _isAutoGraphicsEnabled.value = enabled
    autoScaleCooldownFrames = 0
    if (enabled && context != null) {
      val recommended = GraphicsQualityPreset.detectRecommendedPreset(context)
      _graphicsPreset.value = recommended
      avgFrameTimeMs = 16.2f
      logEvent("GRAPHICS_ENGINE", "Auto graphics enabled -> ${recommended.shortLabel}")
    } else {
      logEvent("GRAPHICS_ENGINE", "Auto graphics mode set to $enabled")
    }
    persistCurrentSettings()
  }

  fun setGraphicsPreset(preset: GraphicsQualityPreset) {
    _graphicsPreset.value = preset
    avgFrameTimeMs = 16.2f
    autoScaleCooldownFrames = 0
    persistCurrentSettings()
    logEvent("GRAPHICS_ENGINE", "Graphics quality changed to ${preset.displayName}")
  }

  fun setFrameRatePreference(pref: FrameRatePreference) {
    _frameRatePref.value = pref
    persistCurrentSettings()
    logEvent("GRAPHICS_ENGINE", "Frame-rate target set to ${pref.label}")
  }

  fun setLanguageOption(lang: AppLanguageOption) {
    _languageOption.value = lang
    persistCurrentSettings()
    logEvent("LOCALIZATION", "Language set to ${lang.displayName} (${lang.code})")
  }

  fun updatePrivacyPreferences(
    shareCloudGhosts: Boolean = _privacyPrefs.value.shareCloudGhosts,
    crashDiagnosticsEnabled: Boolean = _privacyPrefs.value.crashDiagnosticsEnabled,
    personalizedTelemetryEnabled: Boolean = _privacyPrefs.value.personalizedTelemetryEnabled
  ) {
    _privacyPrefs.value = PrivacyPreferences(
      shareCloudGhosts = shareCloudGhosts,
      crashDiagnosticsEnabled = crashDiagnosticsEnabled,
      personalizedTelemetryEnabled = personalizedTelemetryEnabled
    )
    persistCurrentSettings()
    logEvent("PRIVACY", "Updated privacy settings (CloudGhosts=$shareCloudGhosts, Diagnostics=$crashDiagnosticsEnabled)")
  }

  fun setDrivingControlMode(mode: DrivingControlMode) {
    _controlConfig.value = _controlConfig.value.copy(controlMode = mode)
    persistCurrentSettings()
    logEvent("CONTROLS", "Driving control mode switched to ${mode.displayName}")
  }

  fun setSteeringSensitivity(sensitivity: Float) {
    val clamped = sensitivity.coerceIn(0.5f, 2.0f)
    _controlConfig.value = _controlConfig.value.copy(steeringSensitivity = clamped)
    persistCurrentSettings()
    logEvent("CONTROLS", String.format(Locale.US, "Steering sensitivity set to %.2fx", clamped))
  }

  fun setDrivingAssistLevel(assist: DrivingAssistLevel) {
    _controlConfig.value = _controlConfig.value.copy(assistLevel = assist)
    persistCurrentSettings()
    logEvent("PHYSICS", "Driving assist profile set to ${assist.label}")
  }

  fun updateAudioMixer(
    musicVolume: Float = _audioMixer.value.musicVolume,
    engineVolume: Float = _audioMixer.value.engineVolume,
    sfxVolume: Float = _audioMixer.value.sfxVolume,
    isMuted: Boolean = _audioMixer.value.isMuted
  ) {
    _audioMixer.value = AudioMixerSettings(
      musicVolume = musicVolume.coerceIn(0f, 1f),
      engineVolume = engineVolume.coerceIn(0f, 1f),
      sfxVolume = sfxVolume.coerceIn(0f, 1f),
      isMuted = isMuted
    )
    persistCurrentSettings()
  }

  fun toggleBackendEnvironment(): MultiplayerBackendEnv {
    val next = if (_backendEnv.value == MultiplayerBackendEnv.PRODUCTION) {
      MultiplayerBackendEnv.STAGING
    } else {
      MultiplayerBackendEnv.PRODUCTION
    }
    _backendEnv.value = next
    logEvent("MULTIPLAYER_SYNC", "Switched multiplayer environment to ${next.label}")
    return next
  }

  fun recordFrameTimeMs(frameDurationMs: Float) {
    val clamped = frameDurationMs.coerceIn(4f, 120f)
    if (autoScaleCooldownFrames > 0) {
      autoScaleCooldownFrames--
      avgFrameTimeMs = (avgFrameTimeMs * 0.94f) + (clamped.coerceAtMost(24f) * 0.06f)
      return
    }
    avgFrameTimeMs = (avgFrameTimeMs * 0.9f) + (clamped * 0.1f)
    if (clamped > 42f) {
      logEvent("PERF_MONITOR", String.format(Locale.US, "Frame spike detected: %.1f ms", clamped), isWarning = true)
    }
    if (_isAutoGraphicsEnabled.value && avgFrameTimeMs > 36f) {
      val downgraded = when (_graphicsPreset.value) {
        GraphicsQualityPreset.HIGH -> GraphicsQualityPreset.MEDIUM
        GraphicsQualityPreset.MEDIUM -> GraphicsQualityPreset.LOW
        GraphicsQualityPreset.LOW -> null
      }
      if (downgraded != null) {
        _graphicsPreset.value = downgraded
        avgFrameTimeMs = 16.5f
        autoScaleCooldownFrames = 15
        logEvent(
          subsystem = "PERF_MONITOR",
          message = "Auto-scaled graphics preset to ${downgraded.shortLabel} to maintain target FPS",
          isWarning = true
        )
      }
    }
  }

  fun getEstimatedFps(): Int {
    return (1000f / avgFrameTimeMs.coerceAtLeast(8f)).toInt().coerceIn(15, 60)
  }

  fun getMemoryUsageMb(): Long {
    val runtime = Runtime.getRuntime()
    val usedBytes = runtime.totalMemory() - runtime.freeMemory()
    return (usedBytes / (1024L * 1024L)).coerceAtLeast(1L)
  }

  fun logEvent(subsystem: String, message: String, isWarning: Boolean = false) {
    val now = System.currentTimeMillis()
    val formatter = SimpleDateFormat("HH:mm:ss.SSS", Locale.US)
    val entry = DiagnosticLogEntry(
      timestampMs = now,
      formattedTime = formatter.format(Date(now)),
      subsystem = subsystem,
      message = message,
      isWarning = isWarning
    )
    _diagnosticLogs.value = (listOf(entry) + _diagnosticLogs.value).take(40)
  }
}
