package com.example.data.gemini

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.ImagePart
import com.google.firebase.ai.type.ResponseModality
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.io.ByteArrayOutputStream

private const val TAG = "GeminiImageService"

data class GeneratedLiveryResult(
  val id: String = java.util.UUID.randomUUID().toString(),
  val prompt: String,
  val base64Image: String? = null,
  val description: String,
  val isEdited: Boolean = false,
  val aspectRatio: String = "1:1",
  val isSimulated: Boolean = false,
  val timestamp: Long = System.currentTimeMillis()
)

class GeminiImageService {

  private val imageModel by lazy {
    val config = generationConfig {
      responseModalities = listOf(ResponseModality.IMAGE)
    }
    Firebase.ai.generativeModel(
      modelName = "gemini-nano-banana-2.1",
      generationConfig = config
    )
  }

  suspend fun generateLiveryOrHelmet(prompt: String): GeneratedLiveryResult {
    return createImage(prompt = prompt, aspectRatio = "1:1", resolution = "1K")
  }

  suspend fun createImage(
    prompt: String,
    aspectRatio: String = "1:1",
    resolution: String = "1K"
  ): GeneratedLiveryResult = withContext(Dispatchers.IO) {
    try {
      val enhancedPrompt = "Motorsport Livery & Vehicle Styling: $prompt, high resolution professional automotive studio render, sharp reflections, carbon fiber details"

      val response = withTimeout(60_000L) {
        imageModel.generateContent(enhancedPrompt)
      }

      val imagePart = response.candidates.firstOrNull()
        ?.content?.parts?.filterIsInstance<ImagePart>()
        ?.firstOrNull()

      val bitmap = imagePart?.image

      if (bitmap != null) {
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 95, stream)
        val base64Str = Base64.encodeToString(stream.toByteArray(), Base64.NO_WRAP)

        GeneratedLiveryResult(
          prompt = prompt,
          base64Image = base64Str,
          description = "Custom livery generated with Gemini Nano Banana 2.1 via Firebase AI Logic.",
          aspectRatio = aspectRatio,
          isSimulated = false
        )
      } else {
        val textDesc = response.text ?: "Model completed image generation request."
        GeneratedLiveryResult(
          prompt = prompt,
          base64Image = null,
          description = textDesc,
          aspectRatio = aspectRatio,
          isSimulated = false
        )
      }
    } catch (e: TimeoutCancellationException) {
      Log.e(TAG, "Image generation timed out", e)
      GeneratedLiveryResult(
        prompt = prompt,
        description = "Image generation timed out after 60 seconds. Please try again.",
        aspectRatio = aspectRatio,
        isSimulated = false
      )
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      Log.e(TAG, "Firebase AI Logic image generation failed", e)
      GeneratedLiveryResult(
        prompt = prompt,
        description = "Image generation failed: ${e.localizedMessage ?: "Unknown error"}",
        aspectRatio = aspectRatio,
        isSimulated = false
      )
    }
  }

  suspend fun editImage(
    prompt: String,
    inputImageBase64: String,
    mimeType: String = "image/jpeg",
    aspectRatio: String = "1:1"
  ): GeneratedLiveryResult = withContext(Dispatchers.IO) {
    try {
      val decodedBytes = Base64.decode(inputImageBase64, Base64.DEFAULT)
      val inputBitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
        ?: throw IllegalArgumentException("Could not decode input image base64.")

      val editInstruction = "Modify and edit this vehicle image according to instructions: $prompt. Retain body proportions, apply professional high-gloss paint, decals, and aerodynamic parts."

      val response = withTimeout(60_000L) {
        imageModel.generateContent(
          content {
            image(inputBitmap)
            text(editInstruction)
          }
        )
      }

      val imagePart = response.candidates.firstOrNull()
        ?.content?.parts?.filterIsInstance<ImagePart>()
        ?.firstOrNull()

      val bitmap = imagePart?.image

      if (bitmap != null) {
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 95, stream)
        val base64Str = Base64.encodeToString(stream.toByteArray(), Base64.NO_WRAP)

        GeneratedLiveryResult(
          prompt = prompt,
          base64Image = base64Str,
          description = "Livery edited with Gemini Nano Banana 2.1 via Firebase AI Logic.",
          isEdited = true,
          aspectRatio = aspectRatio,
          isSimulated = false
        )
      } else {
        val textDesc = response.text ?: "Vehicle livery modifications analyzed."
        GeneratedLiveryResult(
          prompt = prompt,
          base64Image = null,
          description = textDesc,
          isEdited = true,
          aspectRatio = aspectRatio,
          isSimulated = false
        )
      }
    } catch (e: TimeoutCancellationException) {
      Log.e(TAG, "Image editing timed out", e)
      GeneratedLiveryResult(
        prompt = prompt,
        description = "Image edit timed out after 60 seconds.",
        isEdited = true,
        aspectRatio = aspectRatio,
        isSimulated = false
      )
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      Log.e(TAG, "Firebase AI Logic image edit failed", e)
      GeneratedLiveryResult(
        prompt = prompt,
        description = "Image edit failed: ${e.localizedMessage ?: "Unknown error"}",
        isEdited = true,
        aspectRatio = aspectRatio,
        isSimulated = false
      )
    }
  }
}
