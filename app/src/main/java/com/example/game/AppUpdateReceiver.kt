package com.example.game

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.MainActivity

/**
 * BroadcastReceiver that triggers when Cosmic Battle is successfully updated/replaced.
 * Automatically restarts the game into the new version.
 */
class AppUpdateReceiver : BroadcastReceiver() {
  override fun onReceive(context: Context, intent: Intent) {
    if (intent.action == Intent.ACTION_MY_PACKAGE_REPLACED) {
      Log.i("AppUpdateReceiver", "Cosmic Battle package replaced/updated! Relaunching game...")
      try {
        val launchIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)
        if (launchIntent != null) {
          launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
          context.startActivity(launchIntent)
        } else {
          val fallbackIntent = Intent(context, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
          }
          context.startActivity(fallbackIntent)
        }
      } catch (e: Exception) {
        Log.e("AppUpdateReceiver", "Error relaunching app after update: ${e.message}")
      }
    }
  }
}
