package com.example.game

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.view.Display
import android.view.WindowManager
import android.webkit.WebView

class GamePreferences(context: Context) {
  private val prefs: SharedPreferences =
    context.getSharedPreferences("cosmic_battle_prefs", Context.MODE_PRIVATE)

  var fpsLimit: Int
    get() = prefs.getInt(KEY_FPS_LIMIT, DEFAULT_FPS)
    set(value) = prefs.edit().putInt(KEY_FPS_LIMIT, value).apply()

  var isNotificationsEnabled: Boolean
    get() = prefs.getBoolean(KEY_NOTIFICATIONS_ENABLED, true)
    set(value) = prefs.edit().putBoolean(KEY_NOTIFICATIONS_ENABLED, value).apply()

  var isKeepScreenOn: Boolean
    get() = prefs.getBoolean(KEY_KEEP_SCREEN_ON, true)
    set(value) = prefs.edit().putBoolean(KEY_KEEP_SCREEN_ON, value).apply()

  var isImmersive: Boolean
    get() = prefs.getBoolean(KEY_IMMERSIVE, true)
    set(value) = prefs.edit().putBoolean(KEY_IMMERSIVE, value).apply()

  fun applyFpsSettings(activity: Activity?, webView: WebView?) {
    val targetFps = fpsLimit

    // 1. Native Display Refresh Rate for Android 11+ (API 30+)
    activity?.let { act ->
      try {
        val window = act.window
        val layoutParams = window.attributes
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
          layoutParams.preferredRefreshRate = targetFps.toFloat()
          window.attributes = layoutParams
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
          // On Android 6.0 - 10.0, select compatible display mode if available
          val display = window.windowManager.defaultDisplay
          val modes = display.supportedModes
          val bestMode = modes.firstOrNull { mode ->
            Math.abs(mode.refreshRate - targetFps) < 1.0f
          }
          if (bestMode != null) {
            layoutParams.preferredDisplayModeId = bestMode.modeId
            window.attributes = layoutParams
          }
        }
      } catch (_: Exception) {
        // Safe fallback for devices with custom display drivers
      }
    }

    // 2. Safe non-blocking JavaScript FPS update (never wraps recursively)
    webView?.let { wv ->
      val js = """
        (function() {
          try {
            var target = $targetFps;
            window.__cosmicFps = target;
            if (!window.__cosmicFpsInstalled) {
              window.__cosmicFpsInstalled = true;
              var nativeRAF = window.requestAnimationFrame.bind(window);
              var lastFrameTime = 0;
              window.requestAnimationFrame = function(cb) {
                return nativeRAF(function(time) {
                  var currentTarget = window.__cosmicFps || 60;
                  if (currentTarget >= 165) {
                    return cb(time);
                  }
                  var minInterval = 1000 / currentTarget;
                  if (time - lastFrameTime >= minInterval - 1.5) {
                    lastFrameTime = time;
                    cb(time);
                  } else {
                    nativeRAF(cb);
                  }
                });
              };
            }
          } catch(e) {}
        })();
      """.trimIndent()
      wv.evaluateJavascript(js, null)
    }
  }

  companion object {
    private const val KEY_FPS_LIMIT = "fps_limit"
    private const val KEY_NOTIFICATIONS_ENABLED = "notifications_enabled"
    private const val KEY_KEEP_SCREEN_ON = "keep_screen_on"
    private const val KEY_IMMERSIVE = "immersive"

    const val DEFAULT_FPS = 60
    val FPS_OPTIONS = listOf(30, 60, 120, 144, 165)
  }
}
