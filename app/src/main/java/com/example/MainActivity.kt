package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.game.CosmicBattleScreen
import com.example.game.GamePerformanceOptimizer
import com.example.game.NotificationHelper
import com.example.ui.theme.CosmicDark
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()

    // Apply Game Mode API, PerformanceHintManager and high-performance window flags
    GamePerformanceOptimizer.applyGameOptimizations(this)

    setContent {
      MyApplicationTheme {
        Surface(
          modifier = Modifier.fillMaxSize(),
          color = CosmicDark
        ) {
          CosmicBattleScreen(
            onExitApp = { finish() }
          )
        }
      }
    }
  }

  override fun onStart() {
    super.onStart()
    // Cancel background battle reminder notifications while player is active
    NotificationHelper.cancelBackgroundBattleReminders(this)
  }

  override fun onResume() {
    super.onResume()
    NotificationHelper.cancelBackgroundBattleReminders(this)
    GamePerformanceOptimizer.applyGameOptimizations(this)
  }

  override fun onStop() {
    super.onStop()
    // When the app goes to background, schedule reminders that user hasn't played for a while
    NotificationHelper.scheduleBackgroundBattleReminders(this)
  }

  override fun onDestroy() {
    super.onDestroy()
    GamePerformanceOptimizer.closeSession()
  }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
  Text(text = "Hello $name!", modifier = modifier)
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
  MyApplicationTheme { Greeting("Cosmic Battle") }
}

