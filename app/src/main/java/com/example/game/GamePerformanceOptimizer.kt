package com.example.game

import android.app.Activity
import android.app.GameManager
import android.os.Build
import android.os.PerformanceHintManager
import android.os.Process
import android.util.Log
import android.view.View
import android.view.Window
import android.view.WindowManager
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

object GamePerformanceOptimizer {

  private const val TAG = "GameOptimizer"
  private var hintSession: PerformanceHintManager.Session? = null

  /**
   * Applies all maximum performance optimizations, game mode hooks,
   * performance hint session, and cutout window flags.
   */
  fun applyGameOptimizations(activity: Activity) {
    val window = activity.window

    // 1. Official Game Mode API (Android 13+ / API 33+)
    try {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val gameManager = activity.getSystemService(GameManager::class.java)
        if (gameManager != null) {
          val currentMode = gameManager.gameMode
          Log.i(TAG, "Android 13+ Game Mode active: $currentMode")
        }
      }
    } catch (e: Exception) {
      Log.w(TAG, "GameManager not available: ${e.message}")
    }

    // 2. Performance Hint API (Android 12+ / API 31+)
    try {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val hintManager = activity.getSystemService(PerformanceHintManager::class.java)
        if (hintManager != null) {
          // Target frame duration: 16.6ms (~60fps) or 8.3ms (~120fps)
          val targetFrameTimeNs = 16_000_000L
          hintSession = hintManager.createHintSession(
            intArrayOf(Process.myTid()),
            targetFrameTimeNs
          )
          hintSession?.updateTargetWorkDuration(targetFrameTimeNs)
          Log.i(TAG, "PerformanceHintManager Session initialized successfully")
        }
      }
    } catch (e: Exception) {
      Log.w(TAG, "PerformanceHintManager failed: ${e.message}")
    }

    // 3. Window flags for maximum performance, responsiveness and zero-latency fullscreen
    try {
      // Keep screen on during gameplay
      window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

      // Layout cutout for true edge-to-edge display
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        window.attributes = window.attributes.apply {
          layoutInDisplayCutoutMode =
            WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }
      }

      // Maximize refresh rate (90Hz / 120Hz / 144Hz) if available on the device
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        val display = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
          activity.display
        } else {
          @Suppress("DEPRECATION")
          window.windowManager.defaultDisplay
        }
        val modes = display?.supportedModes
        val highestRefreshRateMode = modes?.maxByOrNull { it.refreshRate }
        if (highestRefreshRateMode != null && highestRefreshRateMode.refreshRate > 60f) {
          window.attributes = window.attributes.apply {
            preferredDisplayModeId = highestRefreshRateMode.modeId
          }
          Log.i(TAG, "Set preferred display mode to ${highestRefreshRateMode.refreshRate} Hz")
        }
      }

      // Immersive sticky system bars
      WindowCompat.setDecorFitsSystemWindows(window, false)
      val insetsController = WindowCompat.getInsetsController(window, window.decorView)
      insetsController.systemBarsBehavior =
        WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
      insetsController.hide(WindowInsetsCompat.Type.systemBars())

      // Hardware acceleration guarantee
      window.setFlags(
        WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
        WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED
      )
    } catch (e: Exception) {
      Log.w(TAG, "Window flags setup error: ${e.message}")
    }
  }

  fun reportActualWorkDuration(actualDurationNs: Long) {
    try {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        hintSession?.reportActualWorkDuration(actualDurationNs)
      }
    } catch (_: Exception) {}
  }

  fun closeSession() {
    try {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        hintSession?.close()
        hintSession = null
      }
    } catch (_: Exception) {}
  }
}
