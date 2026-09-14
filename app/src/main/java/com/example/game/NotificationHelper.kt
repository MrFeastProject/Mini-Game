package com.example.game

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.MainActivity
import com.example.R

object NotificationHelper {
  const val CHANNEL_ID = "cosmic_battle_channel"
  private const val NOTIFICATION_ID = 1001

  fun createNotificationChannel(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val name = context.getString(R.string.notification_channel_name)
      val descriptionText = context.getString(R.string.notification_channel_desc)
      val importance = NotificationManager.IMPORTANCE_HIGH
      val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
        description = descriptionText
        enableLights(true)
        enableVibration(true)
      }
      val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
      notificationManager.createNotificationChannel(channel)
    }
  }

  fun sendBattleNotification(
    context: Context,
    title: String,
    body: String
  ): Boolean {
    createNotificationChannel(context)

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      if (ActivityCompat.checkSelfPermission(
          context,
          Manifest.permission.POST_NOTIFICATIONS
        ) != PackageManager.PERMISSION_GRANTED
      ) {
        return false
      }
    }

    val intent = Intent(context, MainActivity::class.java).apply {
      flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
    }
    val pendingIntent = PendingIntent.getActivity(
      context,
      0,
      intent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    val largeIcon = try {
      BitmapFactory.decodeResource(context.resources, R.drawable.img_cosmic_icon)
    } catch (_: Exception) {
      null
    }

    val builder = NotificationCompat.Builder(context, CHANNEL_ID)
      .setSmallIcon(R.mipmap.ic_launcher)
      .setContentTitle(title)
      .setContentText(body)
      .setStyle(NotificationCompat.BigTextStyle().bigText(body))
      .setPriority(NotificationCompat.PRIORITY_HIGH)
      .setContentIntent(pendingIntent)
      .setAutoCancel(true)

    if (largeIcon != null) {
      builder.setLargeIcon(largeIcon)
    }

    try {
      with(NotificationManagerCompat.from(context)) {
        notify(NOTIFICATION_ID, builder.build())
      }
      return true
    } catch (_: SecurityException) {
      return false
    }
  }
}
