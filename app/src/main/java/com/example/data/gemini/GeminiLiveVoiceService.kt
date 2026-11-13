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

private const val TAG = "GeminiLiveVoice"

data class LiveVoiceMessage(
  val id: String = java.util.UUID.randomUUID().toString(),
  val sender: String, // "Driver" or "Race Engineer (Gemini 3.8 Live)"
  val text: String,
  val isAudioPlaying: Boolean = false,
  val timestamp: Long = System.currentTimeMillis()
)

class GeminiLiveVoiceService {

  private val systemInstruction = """
    You are the Chief Race Engineer on the team pit-wall radio speaking in real-time over live audio with your racing driver using Gemini 3.8.
    Be fast, precise, calm, and assertive like an F1 or MotoGP race engineer (e.g. 'Understood, copy that', 'Delta is plus 0.4', 'Watch tire surface temps in turn 4', 'Box box for soft slicks').
    Keep spoken radio transmissions to 1-2 sharp, tactical sentences.
  """.trimIndent()

  private val model by lazy {
    Firebase.ai.generativeModel(
      modelName = "gemini-3.8-flash",
      generationConfig = generationConfig {
        temperature = 0.5f
      },
      systemInstruction = content {
        text(systemInstruction)
      }
    )
  }

  suspend fun conductLiveVoiceTurn(
    driverSpokenPrompt: String,
    conversationHistory: List<LiveVoiceMessage>
  ): LiveVoiceMessage = withContext(Dispatchers.IO) {
    try {
      val historyContents = conversationHistory.takeLast(6).map { msg ->
        if (msg.sender == "Driver") {
          content("user") { text(msg.text) }
        } else {
          content("model") { text(msg.text) }
        }
      }

      val chat = model.startChat(history = historyContents)

      val response = withTimeout(30_000L) {
        chat.sendMessage(driverSpokenPrompt)
      }

      val replyText = response.text?.trim() ?: "Copy that, driver."

      LiveVoiceMessage(
        sender = "Race Engineer (Gemini 3.8)",
        text = replyText
      )
    } catch (e: TimeoutCancellationException) {
      Log.e(TAG, "Live voice turn timed out", e)
      LiveVoiceMessage(
        sender = "Pit Radio",
        text = "Pit radio static... Transmission timed out."
      )
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      Log.e(TAG, "Live voice turn failed", e)
      LiveVoiceMessage(
        sender = "Pit Radio",
        text = "Pit communication notice: ${e.localizedMessage ?: "Signal lost"}"
      )
    }
  }
}
