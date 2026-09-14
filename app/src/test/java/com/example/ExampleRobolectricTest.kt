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

}
