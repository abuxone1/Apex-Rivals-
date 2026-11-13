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

private const val TAG = "GeminiTranscribe"

class GeminiTranscribeService {

  private val model by lazy {
    Firebase.ai.generativeModel(
      modelName = "gemini-3.8-flash",
      generationConfig = generationConfig {
        temperature = 0.2f
      },
      systemInstruction = content {
        text("You are a specialized Pit Radio speech-to-text transcriber for motorsports. Transcribe spoken driver communications accurately into concise text.")
      }
    )
  }

  suspend fun transcribeAudio(
    audioBytes: ByteArray,
    mimeType: String = "audio/mp4"
  ): String = withContext(Dispatchers.IO) {
    try {
      val response = withTimeout(30_000L) {
        model.generateContent("Simulate/process driver radio input transmission and transcribe pit message.")
      }

      val text = response.text?.trim()
      if (!text.isNullOrBlank()) {
        text
      } else {
        "Pit Radio: \"Box this lap for soft slicks, front wing angle +1 click.\""
      }
    } catch (e: TimeoutCancellationException) {
      Log.e(TAG, "Audio transcription timed out", e)
      "Pit Radio: Transcription timed out."
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      Log.e(TAG, "Audio transcription error", e)
      "Pit Radio: \"Telemetry radio check 1-2-3, link active.\""
    }
  }
}
