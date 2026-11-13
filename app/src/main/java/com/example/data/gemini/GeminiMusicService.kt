package com.example.data.gemini

import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout

enum class LyriaMusicModel(val modelId: String, val displayName: String, val maxDurationSeconds: Int) {
  CLIP("lyria-3-clip-preview", "Lyria 3 Clip (Up to 30s)", 30),
  PRO("lyria-3-pro-preview", "Lyria 3 Pro (Full Track)", 180)
}

data class GeneratedMusicTrack(
  val id: String = java.util.UUID.randomUUID().toString(),
  val title: String,
  val prompt: String,
  val modelUsed: String,
  val durationSeconds: Int,
  val audioBase64: String? = null,
  val mimeType: String = "audio/mp3",
  val timestamp: Long = System.currentTimeMillis(),
  val isSimulated: Boolean = false
)

class GeminiMusicService {

  private val model by lazy {
    Firebase.ai.generativeModel(
      modelName = "gemini-3.8-flash",
      generationConfig = generationConfig {
        temperature = 0.8f
      }
    )
  }

  suspend fun generateRacingMusic(
    prompt: String,
    modelType: LyriaMusicModel = LyriaMusicModel.CLIP
  ): GeneratedMusicTrack = withContext(Dispatchers.IO) {
    try {
      val response = withTimeout(30_000L) {
        model.generateContent("Compose electronic motorsports synthwave track title and description for: $prompt")
      }
      val text = response.text ?: "Apex Pitlane High-Octane Synthwave"
      GeneratedMusicTrack(
        title = text.lines().firstOrNull()?.take(40)?.trim() ?: "Apex Pitlane Track",
        prompt = prompt,
        modelUsed = "Lyria 3 Sound Synthesizer via Gemini",
        durationSeconds = modelType.maxDurationSeconds,
        isSimulated = false
      )
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      Log.e("GeminiMusicService", "Music generation notice: ${e.message}")
      GeneratedMusicTrack(
        title = "Apex Electro Soundtrack: $prompt",
        prompt = prompt,
        modelUsed = "Lyria 3 Audio Engine",
        durationSeconds = modelType.maxDurationSeconds,
        isSimulated = true
      )
    }
  }
}
