package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Cosmic Battle", appName)
    val gameUrl = context.getString(R.string.game_url)
    assertEquals("https://mrfeastproject.github.io/Battle/", gameUrl)
  }

  @Test
  fun `verify fps options and preferences`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val prefs = com.example.game.GamePreferences(context)
    assertEquals(listOf(20, 30, 60, 120, 144, 165), com.example.game.GamePreferences.FPS_OPTIONS)
    prefs.fpsLimit = 120
    assertEquals(120, prefs.fpsLimit)
    prefs.fpsLimit = 165
    assertEquals(165, prefs.fpsLimit)
    // Minimum FPS limit is 20
    prefs.fpsLimit = 10
    assertEquals(20, prefs.fpsLimit)
    prefs.fpsLimit = 15
    assertEquals(20, prefs.fpsLimit)
    prefs.fpsLimit = 20
    assertEquals(20, prefs.fpsLimit)
  }

  @Test
  fun `verify notification channel creation`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    com.example.game.NotificationHelper.createNotificationChannel(context)
  }

  @Test
  fun `verify graphics engine detector`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val engineInfo = com.example.game.GraphicsEngineDetector.getGraphicsEngineInfo(context)
    org.junit.Assert.assertNotNull(engineInfo.bestEngineName)
    org.junit.Assert.assertTrue(engineInfo.isHardwareAccelerated)
    val bridge = com.example.game.AndroidBridge(context)
    assertEquals("1.0.3", bridge.getAppVersion())
    org.junit.Assert.assertTrue(bridge.getGraphicsEngineInfoJson().contains("1.0.3"))
  }

  @Test
  fun `verify app update version comparison`() {
    val manager = com.example.game.AppUpdateManager
    org.junit.Assert.assertTrue(manager.isNewerVersion("1.0.4", "1.0.3"))
    org.junit.Assert.assertTrue(manager.isNewerVersion("v1.1.0", "1.0.3"))
    org.junit.Assert.assertTrue(manager.isNewerVersion("2.0.0", "1.0.3"))
    org.junit.Assert.assertFalse(manager.isNewerVersion("1.0.3", "1.0.3"))
    org.junit.Assert.assertFalse(manager.isNewerVersion("v1.0.3", "1.0.3"))
    org.junit.Assert.assertFalse(manager.isNewerVersion("1.0.2", "1.0.3"))
  }

  @Test
  fun `verify background battle reminders lifecycle`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    com.example.game.NotificationHelper.scheduleBackgroundBattleReminders(context, 1000L)
    com.example.game.NotificationHelper.cancelBackgroundBattleReminders(context)
    val receiver = com.example.game.BattleReminderReceiver()
    org.junit.Assert.assertNotNull(receiver)
  }
}
