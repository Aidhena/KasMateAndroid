package com.smktel.kasmate.notif

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.provider.Settings
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.*
import java.util.concurrent.TimeUnit
import java.text.NumberFormat
import java.util.Locale

const val CHANNEL_ID = "kas_reminder_v2"

fun createChannel(ctx: Context) {
    val ch = NotificationChannel(CHANNEL_ID, "Pengingat uang kas", NotificationManager.IMPORTANCE_HIGH).apply {
        description = "Pengingat jumlah tunggakan dan tenggat uang kas kelas"
        setSound(Settings.System.DEFAULT_NOTIFICATION_URI, AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_NOTIFICATION).build())
        enableVibration(true)
    }
    ctx.getSystemService(NotificationManager::class.java).createNotificationChannel(ch)
}

fun notify(ctx: Context, id: Int, title: String, text: String) {

    if (ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS)
        != PackageManager.PERMISSION_GRANTED) return
    val n = NotificationCompat.Builder(ctx, CHANNEL_ID)
        .setSmallIcon(android.R.drawable.ic_dialog_info)
        .setContentTitle(title).setContentText(text).setStyle(NotificationCompat.BigTextStyle().bigText(text))
        .setDefaults(NotificationCompat.DEFAULT_ALL).setPriority(NotificationCompat.PRIORITY_HIGH)
        .setCategory(NotificationCompat.CATEGORY_REMINDER)
        .setAutoCancel(true).build()
    NotificationManagerCompat.from(ctx).notify(id, n)
}

class ReminderWorker(ctx: Context, p: WorkerParameters) : CoroutineWorker(ctx, p) {
    override suspend fun doWork(): Result {
        val prefs = applicationContext.getSharedPreferences("kasmate_preferences", Context.MODE_PRIVATE)
        val amount = prefs.getLong("overdue_amount", 0L)
        if (amount <= 0L) return Result.success()
        val english = prefs.getString("language", "INDONESIAN") == "ENGLISH"
        val number = NumberFormat.getInstance(if (english) Locale.US else Locale("id", "ID")).format(amount)
        val days = prefs.getInt("overdue_days", 1)
        val title = if (english) "KasMate reminder" else "Pengingat KasMate"
        val message = if (english) "Your class dues of $number rupiah are overdue by $days ${if (days == 1) "day" else "days"}. Let's settle them today!"
            else "Tunggakan kasmu Rp $number sudah lewat $days hari. Yuk, bereskan hari ini!"
        notify(applicationContext, 1, title, message)
        return Result.success()
    }
}

fun scheduleDailyReminder(ctx: Context) {
    val req = PeriodicWorkRequestBuilder<ReminderWorker>(1, TimeUnit.DAYS).build()
    WorkManager.getInstance(ctx)
        .enqueueUniquePeriodicWork("kas_reminder", ExistingPeriodicWorkPolicy.KEEP, req)
}
