package com.example.data.gemini

import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout

private const val TAG = "GeminiRaceEngineer"

enum class GeminiCoachModel(val modelId: String, val displayName: String, val description: String) {
  FLASH("gemini-3.8-flash", "Gemini 3.8 Flash", "Standard Race Strategy & Real-time Telemetry (Default)"),
  ARGON("gemini-4-argon", "Gemini 4 Argon", "Deep Telemetry & Vehicle Dynamics Reasoning"),
  FLASH_LITE("gemini-3.5-flash-lite", "Gemini 3.5 Flash Lite", "Ultra-Fast Pit-Wall Advice")
}

enum class ChatbotRole(val displayName: String, val roleDescription: String, val systemInstruction: String) {
  CHIEF_ENGINEER(
    "Chief Race Engineer",
    "Pit-wall tactics & overall delta balance",
    "You are the Chief Race Engineer on the team pit wall. Your role is analyzing overall race strategy, driver deltas, sector splits, traffic management, and vehicle setup compromises for maximum race pace. Be concise, technical, and actionable."
  ),
  AERODYNAMICIST(
    "Aero & CFD Specialist",
    "Downforce, drag & dirty air vortices",
    "You are the Lead Aerodynamicist and CFD Specialist. Your role is advising on front/rear wing angles, ground-effect venturi tunnels, drag coefficient, high vs low downforce circuits, and dirty air wake control. Be concise and technical."
  ),
  TIRE_STRATEGIST(
    "Tire & Telemetry Strategist",
    "Thermal degradation & pit windows",
    "You are the Lead Tire and Telemetry Strategist. Your role is calculating tire surface and carcass temperatures, degradation curves, undercut/overcut windows, and compound selections (soft/medium/hard/wet). Be concise and technical."
  )
}

data class ChatMessage(
  val id: String = java.util.UUID.randomUUID().toString(),
  val role: String, // "user" or "model"
  val content: String,
  val timestamp: Long = System.currentTimeMillis(),
  val searchSources: List<String> = emptyList(),
  val modelUsed: String? = null,
  val isError: Boolean = false
)

class GeminiRaceEngineerService {

  /**
   * Lazily cache models per (modelId, role) so we don't recreate them every message,
   * but evaluate upon first inference.
   */
  private val modelCache = mutableMapOf<String, com.google.firebase.ai.GenerativeModel>()

  private fun getGenerativeModel(
    selectedModel: GeminiCoachModel,
    role: ChatbotRole
  ): com.google.firebase.ai.GenerativeModel {
    val key = "${selectedModel.modelId}_${role.name}"
    return modelCache.getOrPut(key) {
      Firebase.ai.generativeModel(
        modelName = selectedModel.modelId,
        generationConfig = generationConfig {
          temperature = 0.7f
        },
        systemInstruction = content {
          text(role.systemInstruction)
        }
      )
    }
  }

  suspend fun sendMessage(
    history: List<ChatMessage>,
    newPrompt: String,
    selectedModel: GeminiCoachModel = GeminiCoachModel.FLASH,
    enableSearchGrounding: Boolean = true,
    enableMapsGrounding: Boolean = false,
    role: ChatbotRole = ChatbotRole.CHIEF_ENGINEER
  ): ChatMessage = withContext(Dispatchers.IO) {
    val generativeModel = getGenerativeModel(selectedModel, role)

    try {
      // Build past history (up to last 10 turns)
      val historyContents = history.takeLast(10).mapNotNull { msg ->
        if (msg.role == "user") {
          content("user") { text(msg.content) }
        } else if (msg.role == "model" && !msg.isError) {
          content("model") { text(msg.content) }
        } else {
          null
        }
      }

      val chat = generativeModel.startChat(history = historyContents)

      val response = withTimeout(60_000L) {
        chat.sendMessage(newPrompt)
      }

      val responseText = response.text
      if (!responseText.isNullOrBlank()) {
        ChatMessage(
          role = "model",
          content = responseText.trim(),
          modelUsed = "${selectedModel.displayName} • ${role.displayName}",
          isError = false
        )
      } else {
        ChatMessage(
          role = "model",
          content = "Telemetry model returned an empty response. Pit telemetry re-evaluating.",
          modelUsed = selectedModel.displayName,
          isError = true
        )
      }
    } catch (e: TimeoutCancellationException) {
      Log.e(TAG, "Gemini generation timed out", e)
      ChatMessage(
        role = "model",
        content = "Pit radio link timed out after 60 seconds. Please retry telemetry query.",
        modelUsed = selectedModel.displayName,
        isError = true
      )
    } catch (e: CancellationException) {
      // ALWAYS rethrow CancellationException to preserve cooperative coroutine cancellation
      throw e
    } catch (e: Exception) {
      Log.e(TAG, "Firebase AI Logic generation failed", e)
      ChatMessage(
        role = "model",
        content = "Pit link error: ${e.localizedMessage ?: "Unknown attestation or network failure"}",
        modelUsed = selectedModel.displayName,
        isError = true
      )
    }
  }

  suspend fun sendMessageStreaming(
    history: List<ChatMessage>,
    newPrompt: String,
    selectedModel: GeminiCoachModel = GeminiCoachModel.FLASH,
    role: ChatbotRole = ChatbotRole.CHIEF_ENGINEER,
    onChunk: (String) -> Unit
  ): ChatMessage = withContext(Dispatchers.IO) {
    val generativeModel = getGenerativeModel(selectedModel, role)
    val accumulated = StringBuilder()

    try {
      val historyContents = history.takeLast(10).mapNotNull { msg ->
        if (msg.role == "user") {
          content("user") { text(msg.content) }
        } else if (msg.role == "model" && !msg.isError) {
          content("model") { text(msg.content) }
        } else {
          null
        }
      }

      val chat = generativeModel.startChat(history = historyContents)

      withTimeout(60_000L) {
        chat.sendMessageStream(newPrompt).collect { chunk ->
          val chunkText = chunk.text.orEmpty()
          accumulated.append(chunkText)
          onChunk(accumulated.toString())
        }
      }

      ChatMessage(
        role = "model",
        content = accumulated.toString().ifBlank { "Empty streaming response." },
        modelUsed = "${selectedModel.displayName} • ${role.displayName}",
        isError = false
      )
    } catch (e: TimeoutCancellationException) {
      Log.e(TAG, "Streaming timed out", e)
      ChatMessage(
        role = "model",
        content = "Pit radio streaming timed out after 60 seconds.",
        modelUsed = selectedModel.displayName,
        isError = true
      )
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      Log.e(TAG, "Firebase AI Logic streaming failed", e)
      ChatMessage(
        role = "model",
        content = "Pit link error: ${e.localizedMessage ?: "Unknown streaming error"}",
        modelUsed = selectedModel.displayName,
        isError = true
      )
    }
  }
}
