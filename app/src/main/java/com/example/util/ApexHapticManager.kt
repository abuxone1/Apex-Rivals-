package com.example.util

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

class ApexHapticManager private constructor(context: Context) {

  private val vibrator: Vibrator? = try {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
      val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
      manager?.defaultVibrator
    } else {
      @Suppress("DEPRECATION")
      context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }
  } catch (_: Exception) {
    null
  }

  var isHapticsEnabled: Boolean = true

  fun vibrateCountdownLight() {
    pulse(durationMs = 20L, amplitude = 90)
  }

  fun vibrateLightsOutGo() {
    pulse(durationMs = 55L, amplitude = 200)
  }

  fun vibrateNitroBoost() {
    pulse(durationMs = 45L, amplitude = 180)
  }

  fun vibrateGearShift() {
    pulse(durationMs = 15L, amplitude = 120)
  }

  fun vibrateKerbRumble() {
    pulse(durationMs = 12L, amplitude = 75)
  }

  fun vibrateImpact() {
    pulse(durationMs = 70L, amplitude = 255)
  }

  private fun pulse(durationMs: Long, amplitude: Int) {
    if (!isHapticsEnabled) return
    try {
      val vib = vibrator ?: return
      if (!vib.hasVibrator()) return
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        vib.vibrate(VibrationEffect.createOneShot(durationMs, amplitude.coerceIn(1, 255)))
      } else {
        @Suppress("DEPRECATION")
        vib.vibrate(durationMs)
      }
    } catch (_: Exception) {
      // Ignore vibration permission or hardware exceptions on non-haptic devices
    }
  }

  companion object {
    @Volatile
    private var instance: ApexHapticManager? = null

    fun getInstance(context: Context): ApexHapticManager {
      return instance ?: synchronized(this) {
        instance ?: ApexHapticManager(context.applicationContext).also { instance = it }
      }
    }
  }
}
