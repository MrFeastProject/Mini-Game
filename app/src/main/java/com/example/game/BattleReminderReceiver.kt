package com.example.game

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * BroadcastReceiver for background battle reminder notifications
 * and background auto-update checks when the player has been away from the game.
 */
class BattleReminderReceiver : BroadcastReceiver() {
  override fun onReceive(context: Context, intent: Intent) {
    Log.d("BattleReminderReceiver", "Received battle reminder broadcast: ${intent.action}")
    if (intent.action == NotificationHelper.ACTION_BATTLE_REMINDER) {
      NotificationHelper.triggerBackgroundBattleNotification(context)

      // Background auto-update check and silent download
      val prefs = GamePreferences(context)
      if (prefs.isAutoUpdateEnabled) {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
          try {
            val currentVer = AndroidBridge.CURRENT_VERSION
            val result = AppUpdateManager.checkForUpdates(currentVer, prefs.githubRepo)
            if (result is AppUpdateManager.UpdateCheckResult.Success) {
              val updateInfo = result.updateInfo
              Log.i("BattleReminderReceiver", "Background update found: ${updateInfo.latestVersion}, starting download...")
              NotificationHelper.sendUpdateProgressNotification(context, updateInfo.latestVersion, 0)
              AppUpdateManager.downloadApk(
                context = context,
                downloadUrl = updateInfo.downloadUrl,
                onProgress = { progress ->
                  when (progress) {
                    is AppUpdateManager.DownloadProgress.Progress -> {
                      NotificationHelper.sendUpdateProgressNotification(
                        context,
                        updateInfo.latestVersion,
                        progress.percent
                      )
                    }
                    is AppUpdateManager.DownloadProgress.Completed -> {
                      NotificationHelper.sendUpdateReadyNotification(
                        context,
                        updateInfo.latestVersion,
                        progress.apkFile
                      )
                    }
                    is AppUpdateManager.DownloadProgress.Failed -> {
                      Log.w("BattleReminderReceiver", "Background download failed: ${progress.error}")
                    }
                  }
                }
              )
            }
          } catch (e: Exception) {
            Log.e("BattleReminderReceiver", "Error during background auto-update check", e)
          } finally {
            pendingResult.finish()
          }
        }
      }
    }
  }
}
