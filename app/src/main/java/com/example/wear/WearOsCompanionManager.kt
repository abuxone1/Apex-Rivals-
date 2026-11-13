package com.example.wear

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class WearOsCompanionState(
  val isCompanionConnected: Boolean = true,
  val watchModelName: String = "Pixel Watch 3 • 45mm LTE",
  val hapticShiftAlertsEnabled: Boolean = true,
  val liveHeartRateBpm: Int = 138,
  val wristTelemetrySyncHz: Int = 60
)

class WearOsCompanionManager private constructor(context: Context) {

  private val _state = MutableStateFlow(WearOsCompanionState())
  val state: StateFlow<WearOsCompanionState> = _state.asStateFlow()

  fun toggleHapticShiftAlerts() {
    _state.update { it.copy(hapticShiftAlertsEnabled = !it.hapticShiftAlertsEnabled) }
  }

  fun simulateWatchConnection(connected: Boolean) {
    _state.update { it.copy(isCompanionConnected = connected) }
  }

  companion object {
    @Volatile
    private var instance: WearOsCompanionManager? = null

    fun getInstance(context: Context): WearOsCompanionManager {
      return instance ?: synchronized(this) {
        instance ?: WearOsCompanionManager(context.applicationContext).also { instance = it }
      }
    }
  }
}
