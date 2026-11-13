package com.example.game.sound

/**
 * Lightweight Engine & Soundtrack Audio Controller for Apex Rivals.
 * Tracks RPM, throttle, nitro, braking, and drift state while adhering to
 * Android Automotive OS driver distraction safety restrictions.
 */
class RacingAudioEngine private constructor() {

  @Volatile
  private var isSafetyRestricted: Boolean = false

  @Volatile
  private var isMuted: Boolean = false

  @Volatile
  private var musicEnabled: Boolean = true

  @Volatile
  private var engineSoundEnabled: Boolean = true

  @Volatile
  private var isRunning: Boolean = false

  @Volatile
  var currentRpm: Int = 1200
    private set

  @Volatile
  var currentThrottle: Float = 0f
    private set

  fun start() {
    if (!isSafetyRestricted && !isMuted) {
      isRunning = true
    }
  }

  fun pause() {
    isRunning = false
  }

  fun stop() {
    isRunning = false
  }

  fun toggleMute(): Boolean {
    isMuted = !isMuted
    if (isMuted) {
      isRunning = false
    }
    return isMuted
  }

  fun setSafetyRestricted(restricted: Boolean) {
    isSafetyRestricted = restricted
    if (restricted) {
      isRunning = false
    }
  }

  fun updateEngineTelemetry(
    rpm: Int,
    throttle: Float,
    nitro: Boolean,
    braking: Boolean,
    drifting: Boolean
  ) {
    if (isSafetyRestricted || isMuted) return
    currentRpm = rpm
    currentThrottle = throttle
  }

  fun toggleMusic(): Boolean {
    musicEnabled = !musicEnabled
    return musicEnabled
  }

  fun toggleEngineSound(): Boolean {
    engineSoundEnabled = !engineSoundEnabled
    return engineSoundEnabled
  }

  @Volatile
  var musicVolume: Float = 0.75f
    private set

  @Volatile
  var engineVolume: Float = 0.90f
    private set

  @Volatile
  var sfxVolume: Float = 0.85f
    private set

  fun setChannelVolumes(musicVol: Float, engineVol: Float, sfxVol: Float) {
    musicVolume = musicVol.coerceIn(0f, 1f)
    engineVolume = engineVol.coerceIn(0f, 1f)
    sfxVolume = sfxVol.coerceIn(0f, 1f)
  }

  fun isMusicActive(): Boolean = musicEnabled && musicVolume > 0.01f && !isMuted && !isSafetyRestricted

  fun isEngineActive(): Boolean = engineSoundEnabled && engineVolume > 0.01f && !isMuted && !isSafetyRestricted

  companion object {
    @Volatile
    private var instance: RacingAudioEngine? = null

    fun getInstance(): RacingAudioEngine {
      return instance ?: synchronized(this) {
        instance ?: RacingAudioEngine().also { instance = it }
      }
    }
  }
}
