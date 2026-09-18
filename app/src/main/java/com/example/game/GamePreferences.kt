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
    get() = prefs.getInt(KEY_FPS_LIMIT, DEFAULT_FPS).coerceAtLeast(MIN_FPS)
    set(value) = prefs.edit().putInt(KEY_FPS_LIMIT, value.coerceAtLeast(MIN_FPS)).apply()

  var isNotificationsEnabled: Boolean
    get() = prefs.getBoolean(KEY_NOTIFICATIONS_ENABLED, true)
    set(value) = prefs.edit().putBoolean(KEY_NOTIFICATIONS_ENABLED, value).apply()

  var isKeepScreenOn: Boolean
    get() = prefs.getBoolean(KEY_KEEP_SCREEN_ON, true)
    set(value) = prefs.edit().putBoolean(KEY_KEEP_SCREEN_ON, value).apply()

  var isImmersive: Boolean
    get() = prefs.getBoolean(KEY_IMMERSIVE, true)
    set(value) = prefs.edit().putBoolean(KEY_IMMERSIVE, value).apply()

  var githubRepo: String
    get() = prefs.getString(KEY_GITHUB_REPO, AppUpdateManager.DEFAULT_REPO) ?: AppUpdateManager.DEFAULT_REPO
    set(value) = prefs.edit().putString(KEY_GITHUB_REPO, value.trim()).apply()

  fun applyFpsSettings(activity: Activity?, webView: WebView?) {
    val targetFps = fpsLimit.coerceAtLeast(MIN_FPS)

    // IMPORTANT: FPS is limited ONLY inside the game canvas loop.
    // The Android activity / window refresh rate is never downscaled to 20 or 30 Hz,
    // so Compose UI, drawers, and system navigation remain fluid at the device's native refresh rate.
    // Only high refresh rate modes (e.g. 120Hz+) are unlocked if requested.
    activity?.let { act ->
      try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
          act.window.attributes = act.window.attributes.apply {
            preferredRefreshRate = if (targetFps > 60) targetFps.toFloat() else 0f
          }
        }
      } catch (_: Exception) {
        // Safe fallback for devices with custom display drivers
      }
    }

    // Pass the FPS limit to the game running in the WebView
    webView?.let { wv ->
      val js = """
        (function() {
          try {
            var target = $targetFps;
            window.__cosmicFps = target;
            if (typeof setGameFpsLimit === 'function') {
              setGameFpsLimit(target);
            } else if (typeof TARGET_FPS !== 'undefined') {
              TARGET_FPS = target;
            }
          } catch(e) {}
        })();
      """.trimIndent()
      wv.post {
        wv.evaluateJavascript(js, null)
      }
    }
  }

  companion object {
    private const val KEY_FPS_LIMIT = "fps_limit"
    private const val KEY_NOTIFICATIONS_ENABLED = "notifications_enabled"
    private const val KEY_KEEP_SCREEN_ON = "keep_screen_on"
    private const val KEY_IMMERSIVE = "immersive"
    private const val KEY_GITHUB_REPO = "github_repo"

    const val MIN_FPS = 20
    const val DEFAULT_FPS = 60
    val FPS_OPTIONS = listOf(20, 30, 60, 120, 144, 165)
  }
}
