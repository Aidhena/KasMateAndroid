package com.smktel.kasmate.notif

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.*
import java.util.concurrent.TimeUnit

const val CHANNEL_ID = "kas_channel"

fun createChannel(ctx: Context) {
    val ch = NotificationChannel(CHANNEL_ID, "Uang Kas", NotificationManager.IMPORTANCE_DEFAULT)
    ctx.getSystemService(NotificationManager::class.java).createNotificationChannel(ch)
}

fun notify(ctx: Context, id: Int, title: String, text: String) {

    if (ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS)
        != PackageManager.PERMISSION_GRANTED) return
    val n = NotificationCompat.Builder(ctx, CHANNEL_ID)
        .setSmallIcon(android.R.drawable.ic_dialog_info)
        .setContentTitle(title).setContentText(text)
        .setAutoCancel(true).build()
    NotificationManagerCompat.from(ctx).notify(id, n)
}

class ReminderWorker(ctx: Context, p: WorkerParameters) : CoroutineWorker(ctx, p) {
    override suspend fun doWork(): Result {

        notify(applicationContext, 1, "Pengingat Kas", "Ada iuran kas yang mendekati tenggat.")
        return Result.success()
    }
}

fun scheduleDailyReminder(ctx: Context) {
    val req = PeriodicWorkRequestBuilder<ReminderWorker>(1, TimeUnit.DAYS).build()
    WorkManager.getInstance(ctx)
        .enqueueUniquePeriodicWork("kas_reminder", ExistingPeriodicWorkPolicy.KEEP, req)
}