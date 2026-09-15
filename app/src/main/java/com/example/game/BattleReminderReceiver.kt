package com.example.game

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * BroadcastReceiver for background battle reminder notifications
 * when the player has been away from the game for a while.
 */
class BattleReminderReceiver : BroadcastReceiver() {
  override fun onReceive(context: Context, intent: Intent) {
    Log.d("BattleReminderReceiver", "Received battle reminder broadcast: ${intent.action}")
    if (intent.action == NotificationHelper.ACTION_BATTLE_REMINDER) {
      NotificationHelper.triggerBackgroundBattleNotification(context)
    }
  }
}
