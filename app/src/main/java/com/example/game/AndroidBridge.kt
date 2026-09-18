package com.example.game

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.webkit.JavascriptInterface

class AndroidBridge(private val context: Context) {

  private val vibrator: Vibrator? by lazy {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
      val vibratorManager =
        context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
      vibratorManager?.defaultVibrator
    } else {
      @Suppress("DEPRECATION")
      context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }
  }

  @JavascriptInterface
  fun getAppVersion(): String = CURRENT_VERSION

  @JavascriptInterface
  fun isHardwareAccelerated(): Boolean = true

  @JavascriptInterface
  fun getGraphicsEngineInfoJson(): String {
    return try {
      val info = GraphicsEngineDetector.getGraphicsEngineInfo(context)
      val json = org.json.JSONObject().apply {
        put("bestEngine", info.bestEngineName)
        put("engineType", info.engineType)
        put("vulkanSupported", info.vulkanSupported)
        put("vulkanVersion", info.vulkanVersion ?: "N/A")
        put("openGlVersion", info.openGlVersion)
        put("gpuRenderer", info.gpuRenderer)
        put("gpuVendor", info.gpuVendor)
        put("isHardwareAccelerated", info.isHardwareAccelerated)
        put("recommendationSummary", info.recommendationSummary)
        put("technicalDetails", info.technicalDetails)
        put("appVersion", CURRENT_VERSION)
      }
      json.toString()
    } catch (e: Exception) {
      "{ \"bestEngine\": \"Hardware GPU Pipeline\", \"appVersion\": \"$CURRENT_VERSION\", \"isHardwareAccelerated\": true }"
    }
  }

  private var onCheckUpdatesRequested: (() -> Unit)? = null

  fun setOnCheckUpdatesListener(listener: () -> Unit) {
    onCheckUpdatesRequested = listener
  }

  @JavascriptInterface
  fun checkUpdates() {
    onCheckUpdatesRequested?.invoke()
  }

  @JavascriptInterface
  fun vibrate(type: String?) {
    val vib = vibrator ?: return
    if (!vib.hasVibrator()) return

    try {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val effect = when (type) {
          "heavy", "error" -> VibrationEffect.createOneShot(45, VibrationEffect.DEFAULT_AMPLITUDE)
          "medium" -> VibrationEffect.createOneShot(25, (VibrationEffect.DEFAULT_AMPLITUDE * 0.7f).toInt())
          "success" -> VibrationEffect.createWaveform(longArrayOf(0, 15, 40, 20), -1)
          else -> VibrationEffect.createOneShot(12, (VibrationEffect.DEFAULT_AMPLITUDE * 0.4f).toInt())
        }
        vib.vibrate(effect)
      } else {
        @Suppress("DEPRECATION")
        val duration = when (type) {
          "heavy", "error" -> 45L
          "medium" -> 25L
          else -> 12L
        }
        @Suppress("DEPRECATION")
        vib.vibrate(duration)
      }
    } catch (_: Exception) {}
  }

  companion object {
    const val CURRENT_VERSION = "1.0.4"
  }
}
