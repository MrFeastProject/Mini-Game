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
  const val ACTION_BATTLE_REMINDER = "com.example.game.ACTION_BATTLE_REMINDER"
  private const val NOTIFICATION_ID = 1001
  private const val REMINDER_NOTIFICATION_ID = 2002
  private const val REMINDER_REQUEST_CODE = 3003
  private const val UPDATE_NOTIFICATION_ID = 4004
  private const val UPDATE_REQUEST_CODE = 4005

  // Default reminder delay: 3 hours of inactivity (in ms)
  // For immediate background testing or long idle periods
  const val DEFAULT_REMINDER_DELAY_MS = 3 * 60 * 60 * 1000L
  const val NEXT_REMINDER_DELAY_MS = 6 * 60 * 60 * 1000L

  private val BATTLE_REMINDER_MESSAGES = listOf(
    Pair(
      "Командир, враги наступают! 🚀",
      "Вы давно не играли! Космические захватчики атакуют галактику — пора идти побеждать врагов!"
    ),
    Pair(
      "Тревога в звездном секторе! ⚔️",
      "Вы давно не выходили на связь! Армада пришельцев наступает — возвращайтесь и уничтожайте врагов!"
    ),
    Pair(
      "Ваш истребитель ждет вас! 🌌",
      "Корабли противника перегруппировались! Пора подниматься на орбиту и сокрушать космических боссов!"
    ),
    Pair(
      "Галактический призыв! 💥",
      "Враги захватывают новые секторы. Возвращайтесь в бой и покажите мощь своего флота!"
    )
  )

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

  fun sendUpdateProgressNotification(
    context: Context,
    version: String,
    percent: Int
  ) {
    createNotificationChannel(context)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      if (ActivityCompat.checkSelfPermission(
          context,
          Manifest.permission.POST_NOTIFICATIONS
        ) != PackageManager.PERMISSION_GRANTED
      ) {
        return
      }
    }

    val intent = Intent(context, MainActivity::class.java).apply {
      flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
    }
    val pendingIntent = PendingIntent.getActivity(
      context,
      UPDATE_REQUEST_CODE,
      intent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    val builder = NotificationCompat.Builder(context, CHANNEL_ID)
      .setSmallIcon(R.mipmap.ic_launcher)
      .setContentTitle("Загрузка обновления Cosmic Battle v$version")
      .setContentText("Скачивание в фоне: $percent%")
      .setProgress(100, percent, false)
      .setOngoing(percent < 100)
      .setPriority(NotificationCompat.PRIORITY_LOW)
      .setContentIntent(pendingIntent)
      .setAutoCancel(false)

    try {
      with(NotificationManagerCompat.from(context)) {
        notify(UPDATE_NOTIFICATION_ID, builder.build())
      }
    } catch (_: SecurityException) {}
  }

  fun sendUpdateReadyNotification(
    context: Context,
    version: String,
    apkFile: java.io.File
  ) {
    createNotificationChannel(context)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      if (ActivityCompat.checkSelfPermission(
          context,
          Manifest.permission.POST_NOTIFICATIONS
        ) != PackageManager.PERMISSION_GRANTED
      ) {
        return
      }
    }

    val installIntent = AppUpdateManager.getInstallIntent(context, apkFile)
      ?: Intent(context, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
      }

    val pendingIntent = PendingIntent.getActivity(
      context,
      UPDATE_REQUEST_CODE,
      installIntent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    val builder = NotificationCompat.Builder(context, CHANNEL_ID)
      .setSmallIcon(R.mipmap.ic_launcher)
      .setContentTitle("🚀 Обновление Cosmic Battle v$version готово!")
      .setContentText("Новая версия загружена в фоне. Нажмите для установки!")
      .setStyle(NotificationCompat.BigTextStyle().bigText("Новая версия v$version загружена. Нажмите для быстрой установки."))
      .setPriority(NotificationCompat.PRIORITY_HIGH)
      .setContentIntent(pendingIntent)
      .setAutoCancel(true)

    try {
      with(NotificationManagerCompat.from(context)) {
        notify(UPDATE_NOTIFICATION_ID, builder.build())
      }
    } catch (_: SecurityException) {}
  }

  fun scheduleBackgroundBattleReminders(
    context: Context,
    delayMillis: Long = DEFAULT_REMINDER_DELAY_MS
  ) {
    try {
      val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? android.app.AlarmManager ?: return
      val intent = Intent(context, BattleReminderReceiver::class.java).apply {
        action = ACTION_BATTLE_REMINDER
      }
      val pendingIntent = PendingIntent.getBroadcast(
        context,
        REMINDER_REQUEST_CODE,
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
      )

      val triggerAtMillis = System.currentTimeMillis() + delayMillis
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        alarmManager.setAndAllowWhileIdle(
          android.app.AlarmManager.RTC_WAKEUP,
          triggerAtMillis,
          pendingIntent
        )
      } else {
        alarmManager.set(
          android.app.AlarmManager.RTC_WAKEUP,
          triggerAtMillis,
          pendingIntent
        )
      }
    } catch (e: Exception) {
      android.util.Log.w("NotificationHelper", "Failed to schedule battle reminder: ${e.message}")
    }
  }

  fun cancelBackgroundBattleReminders(context: Context) {
    try {
      val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? android.app.AlarmManager
      if (alarmManager != null) {
        val intent = Intent(context, BattleReminderReceiver::class.java).apply {
          action = ACTION_BATTLE_REMINDER
        }
        val pendingIntent = PendingIntent.getBroadcast(
          context,
          REMINDER_REQUEST_CODE,
          intent,
          PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
          alarmManager.cancel(pendingIntent)
          pendingIntent.cancel()
        }
      }

      val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
      notificationManager?.cancel(REMINDER_NOTIFICATION_ID)
    } catch (e: Exception) {
      android.util.Log.w("NotificationHelper", "Failed to cancel battle reminders: ${e.message}")
    }
  }

  fun triggerBackgroundBattleNotification(context: Context) {
    createNotificationChannel(context)

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      if (ActivityCompat.checkSelfPermission(
          context,
          Manifest.permission.POST_NOTIFICATIONS
        ) != PackageManager.PERMISSION_GRANTED
      ) {
        return
      }
    }

    val (title, body) = BATTLE_REMINDER_MESSAGES.random()

    val intent = Intent(context, MainActivity::class.java).apply {
      flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
    }
    val pendingIntent = PendingIntent.getActivity(
      context,
      REMINDER_REQUEST_CODE,
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
      .setCategory(NotificationCompat.CATEGORY_REMINDER)
      .setContentIntent(pendingIntent)
      .setAutoCancel(true)

    if (largeIcon != null) {
      builder.setLargeIcon(largeIcon)
    }

    try {
      with(NotificationManagerCompat.from(context)) {
        notify(REMINDER_NOTIFICATION_ID, builder.build())
      }
      scheduleBackgroundBattleReminders(context, NEXT_REMINDER_DELAY_MS)
    } catch (_: SecurityException) {}
  }
}
