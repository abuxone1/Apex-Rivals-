package com.example.game.sound

import android.media.AudioManager
import android.media.ToneGenerator
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Audio Synthesizer & Sound Effects Manager for Apex Rivals.
 * Fully compliant with Android Automotive OS App Quality Guidelines (Driver Distraction):
 * Automatically halts and silences all tones, pops, and synthesis when vehicle driving motion is detected or audio focus is lost.
 */
class GameSoundManager private constructor() {

  private var toneGenerator: ToneGenerator? = null
  private val scope = CoroutineScope(Dispatchers.Default)
  private var isMuted: Boolean = false

  @Volatile
  private var isVehicleMovingRestricted: Boolean = false

  @Volatile
  private var isAudioLostFocus: Boolean = false

  init {
    try {
      toneGenerator = ToneGenerator(AudioManager.STREAM_MUSIC, 75)
    } catch (e: Exception) {
      Log.e("GameSoundManager", "Failed to initialize ToneGenerator", e)
    }
  }

  companion object {
    @Volatile
    private var instance: GameSoundManager? = null

    fun getInstance(): GameSoundManager {
      return instance ?: synchronized(this) {
        instance ?: GameSoundManager().also { instance = it }
      }
    }
  }

  fun toggleMute(): Boolean {
    isMuted = !isMuted
    RacingAudioEngine.getInstance().toggleMute()
    return isMuted
  }

  fun isAudioMuted(): Boolean = isMuted

  fun isDrivingSilenced(): Boolean = isVehicleMovingRestricted || isAudioLostFocus

  /**
   * CRITICAL AUTOMOTIVE POLICY:
   * Immediately halts any active audio tone and prevents further audio emissions while vehicle is in motion.
   */
  fun silenceAllAudioImmediately(reason: String = "Automotive Driving Restriction") {
    try {
      toneGenerator?.stopTone()
    } catch (e: Exception) {
      Log.w("GameSoundManager", "Error stopping toneGenerator: ${e.message}")
    }
    RacingAudioEngine.getInstance().setSafetyRestricted(true)
    isVehicleMovingRestricted = true
    Log.i("GameSoundManager", "All audio silenced immediately: $reason")
  }

  fun setVehicleMovingRestriction(isMoving: Boolean) {
    isVehicleMovingRestricted = isMoving
    RacingAudioEngine.getInstance().setSafetyRestricted(isMoving)
    if (isMoving) {
      silenceAllAudioImmediately("Vehicle in motion")
    }
  }

  fun setAudioFocusState(hasAudioFocus: Boolean) {
    isAudioLostFocus = !hasAudioFocus
    RacingAudioEngine.getInstance().setSafetyRestricted(!hasAudioFocus)
    if (!hasAudioFocus) {
      silenceAllAudioImmediately("Audio focus lost")
    }
  }

  private fun canPlaySound(): Boolean {
    return !isMuted && !isVehicleMovingRestricted && !isAudioLostFocus
  }

  /**
   * Plays a single red light beep for grid countdown (1..5)
   */
  fun playCountdownLightBeep() {
    if (!canPlaySound()) return
    scope.launch {
      try {
        toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP, 120)
      } catch (e: Exception) {
        // Safe fallback
      }
    }
  }

  /**
   * Plays the high-pitched start chime when lights go out!
   */
  fun playLightsOutGo() {
    if (!canPlaySound()) return
    scope.launch {
      try {
        toneGenerator?.startTone(ToneGenerator.TONE_CDMA_ALERT_NETWORK_LITE, 300)
      } catch (e: Exception) {
        // Safe fallback
      }
    }
  }

  /**
   * Plays gear shift sound effect pop
   */
  fun playGearShift() {
    if (!canPlaySound()) return
    scope.launch {
      try {
        toneGenerator?.startTone(ToneGenerator.TONE_PROP_ACK, 70)
      } catch (e: Exception) {
        // Safe fallback
      }
    }
  }

  /**
   * Plays nitro boost ignition rush
   */
  fun playNitroBoost() {
    if (!canPlaySound()) return
    scope.launch {
      try {
        toneGenerator?.startTone(ToneGenerator.TONE_SUP_RINGTONE, 220)
      } catch (e: Exception) {
        // Safe fallback
      }
    }
  }

  /**
   * Plays slipstream drafting whistle
   */
  fun playSlipstreamDraft() {
    if (!canPlaySound()) return
    scope.launch {
      try {
        toneGenerator?.startTone(ToneGenerator.TONE_PROP_PROMPT, 100)
      } catch (e: Exception) {
        // Safe fallback
      }
    }
  }

  /**
   * Plays chime when an overtake is achieved
   */
  fun playOvertakeChime() {
    if (!canPlaySound()) return
    scope.launch {
      try {
        toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP2, 140)
      } catch (e: Exception) {
        // Safe fallback
      }
    }
  }

  /**
   * Plays purple sector celebration sound
   */
  fun playSectorRecord() {
    if (!canPlaySound()) return
    scope.launch {
      try {
        toneGenerator?.startTone(ToneGenerator.TONE_CDMA_ABBR_ALERT, 180)
      } catch (e: Exception) {
        // Safe fallback
      }
    }
  }

  /**
   * Plays victory fanfare when crossing the checkered finish line!
   */
  fun playCheckeredFlagVictory() {
    if (!canPlaySound()) return
    scope.launch {
      try {
        toneGenerator?.startTone(ToneGenerator.TONE_PROP_ACK, 120)
        delay(140)
        if (!canPlaySound()) return@launch
        toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP2, 140)
        delay(160)
        if (!canPlaySound()) return@launch
        toneGenerator?.startTone(ToneGenerator.TONE_CDMA_ALERT_NETWORK_LITE, 400)
      } catch (e: Exception) {
        // Safe fallback
      }
    }
  }

  /**
   * Plays radar pulse chime when blitzing a speed trap line!
   */
  fun playSpeedTrapRadar() {
    if (!canPlaySound()) return
    scope.launch {
      try {
        toneGenerator?.startTone(ToneGenerator.TONE_PROP_PROMPT, 160)
      } catch (e: Exception) {
        // Safe fallback
      }
    }
  }

  /**
   * Plays hydraulic wheel gun & pit lane chime when fitting fresh tires!
   */
  fun playPitLaneChime() {
    if (!canPlaySound()) return
    scope.launch {
      try {
        toneGenerator?.startTone(ToneGenerator.TONE_CDMA_PIP, 120)
        delay(120)
        if (!canPlaySound()) return@launch
        toneGenerator?.startTone(ToneGenerator.TONE_PROP_ACK, 180)
      } catch (e: Exception) {
        // Safe fallback
      }
    }
  }

  /**
   * Plays collision / barrier crunch sound
   */
  fun playBarrierImpact() {
    if (!canPlaySound()) return
    scope.launch {
      try {
        toneGenerator?.startTone(ToneGenerator.TONE_CDMA_EMERGENCY_RINGBACK, 120)
      } catch (e: Exception) {
        // Safe fallback
      }
    }
  }

  /**
   * Plays team radio / pit wall incoming beep
   */
  fun playRadioBeep() {
    if (!canPlaySound()) return
    scope.launch {
      try {
        toneGenerator?.startTone(ToneGenerator.TONE_PROP_PROMPT, 90)
      } catch (e: Exception) {
        // Safe fallback
      }
    }
  }

  fun startRacingAudio() {
    if (canPlaySound()) {
      RacingAudioEngine.getInstance().start()
    }
  }

  fun pauseRacingAudio() {
    RacingAudioEngine.getInstance().pause()
  }

  fun updateRacingEngineTelemetry(
    rpm: Int,
    throttle: Float,
    nitro: Boolean = false,
    braking: Boolean = false,
    drifting: Boolean = false
  ) {
    if (canPlaySound()) {
      RacingAudioEngine.getInstance().updateEngineTelemetry(rpm, throttle, nitro, braking, drifting)
    }
  }

  fun toggleMusic(): Boolean = RacingAudioEngine.getInstance().toggleMusic()

  fun toggleEngineSound(): Boolean = RacingAudioEngine.getInstance().toggleEngineSound()

  fun isMusicActive(): Boolean = RacingAudioEngine.getInstance().isMusicActive()

  fun isEngineSoundActive(): Boolean = RacingAudioEngine.getInstance().isEngineActive()

  fun setChannelVolumes(musicVol: Float, engineVol: Float, sfxVol: Float) {
    RacingAudioEngine.getInstance().setChannelVolumes(musicVol, engineVol, sfxVol)
  }

  fun playTurboBlowOff() {
    if (!canPlaySound() || RacingAudioEngine.getInstance().sfxVolume <= 0.02f) return
    scope.launch {
      try {
        toneGenerator?.startTone(ToneGenerator.TONE_PROP_ACK, 55)
      } catch (_: Exception) {
      }
    }
  }

  fun playTireScreech() {
    if (!canPlaySound() || RacingAudioEngine.getInstance().sfxVolume <= 0.02f) return
    scope.launch {
      try {
        toneGenerator?.startTone(ToneGenerator.TONE_CDMA_PIP, 65)
      } catch (_: Exception) {
      }
    }
  }

  fun release() {
    try {
      toneGenerator?.stopTone()
      toneGenerator?.release()
      toneGenerator = null
      RacingAudioEngine.getInstance().stop()
    } catch (e: Exception) {
      // Safe fallback
    }
  }
}
