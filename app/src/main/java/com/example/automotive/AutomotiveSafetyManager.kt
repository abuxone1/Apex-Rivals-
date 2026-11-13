package com.example.automotive

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.concurrent.CopyOnWriteArraySet

data class AutomotiveSafetyState(
  val isVehicleMoving: Boolean = false,
  val isParkedOnlyEnforced: Boolean = true,
  val statusMessage: String = "PARKED • AUDIO & GAMEPLAY ENABLED"
)

class AutomotiveSafetyManager private constructor(context: Context) {

  private val _safetyState = MutableStateFlow(AutomotiveSafetyState())
  val safetyState: StateFlow<AutomotiveSafetyState> = _safetyState.asStateFlow()

  private val callbacks = CopyOnWriteArraySet<(Boolean) -> Unit>()

  fun registerDrivingCallback(callback: (Boolean) -> Unit) {
    callbacks.add(callback)
  }

  fun unregisterDrivingCallback(callback: (Boolean) -> Unit) {
    callbacks.remove(callback)
  }

  fun setSimulatedDrivingState(isMoving: Boolean) {
    _safetyState.update {
      it.copy(
        isVehicleMoving = isMoving,
        statusMessage = if (isMoving) {
          "DRIVING DETECTED • AUDIO SILENCED & LOCKED"
        } else {
          "PARKED • AUDIO & GAMEPLAY ENABLED"
        }
      )
    }
    callbacks.forEach { it.invoke(isMoving) }
  }

  companion object {
    @Volatile
    private var instance: AutomotiveSafetyManager? = null

    fun getInstance(context: Context): AutomotiveSafetyManager {
      return instance ?: synchronized(this) {
        instance ?: AutomotiveSafetyManager(context.applicationContext).also { instance = it }
      }
    }
  }
}
