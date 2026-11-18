package com.smktel.kasmate.notif

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.media.AudioAttributes
import android.provider.Settings
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.smktel.kasmate.MainActivity
import com.smktel.kasmate.data.KasMateApiClient
import com.smktel.kasmate.data.KasMateApiException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.TimeUnit
import java.text.NumberFormat
import java.util.Locale

const val CHANNEL_ID = "kas_reminder_v2"

private const val NOTIFICATION_PREFERENCES = "kasmate_server_notifications"
private const val LAST_NOTIFICATION_PREFIX = "last_notification_id_"
private const val DELIVERED_NOTIFICATION_PREFIX = "delivered_notifications_"
private const val MAX_DELIVERED_NOTIFICATION_IDS = 1000
private const val INPUT_SERVER_ADDRESS = "server_address"
private const val INPUT_STUDENT_TOKEN = "student_token"
private const val INPUT_STUDENT_ID = "student_id"
private const val MIN_BACKGROUND_SYNC_MINUTES = 15L

private val notificationSyncMutex = Mutex()
private val notificationDeliveryLock = Any()

fun createChannel(ctx: Context) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
    val ch = NotificationChannel(CHANNEL_ID, "Pengingat uang kas", NotificationManager.IMPORTANCE_HIGH).apply {
        description = "Pengingat jumlah tunggakan dan tenggat uang kas kelas"
        setSound(Settings.System.DEFAULT_NOTIFICATION_URI, AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_NOTIFICATION).build())
        enableVibration(true)
    }
    ctx.getSystemService(NotificationManager::class.java).createNotificationChannel(ch)
}

fun notify(ctx: Context, id: Int, title: String, text: String) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
        ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) !=
        PackageManager.PERMISSION_GRANTED
    ) return

    val intent = Intent(ctx, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
    }
    val pendingIntent = PendingIntent.getActivity(
        ctx,
        id,
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
    val notification = NotificationCompat.Builder(ctx, CHANNEL_ID)
        .setSmallIcon(android.R.drawable.ic_dialog_info)
        .setContentTitle(title)
        .setContentText(text)
        .setStyle(NotificationCompat.BigTextStyle().bigText(text))
        .setContentIntent(pendingIntent)
        .setOnlyAlertOnce(true)
        .setDefaults(NotificationCompat.DEFAULT_ALL)
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setCategory(NotificationCompat.CATEGORY_REMINDER)
        .setAutoCancel(true)
        .build()
    NotificationManagerCompat.from(ctx).notify(id, notification)
}

/**
 * Fetches persisted desktop notifications for this student. The first successful fetch on a
 * device establishes a cursor so old notifications are not replayed; later IDs become alerts.
 */
suspend fun syncServerNotifications(
    context: Context,
    serverAddress: String,
    token: String,
    studentId: Long
) = notificationSyncMutex.withLock {
    val preferences = context.getSharedPreferences(NOTIFICATION_PREFERENCES, Context.MODE_PRIVATE)
    val cursorKey = "$LAST_NOTIFICATION_PREFIX$studentId"
    val initialBaselineId = preferences.getLong(cursorKey, -1L)
    val notifications = KasMateApiClient(serverAddress).loadNotifications(token)
    val newestId = notifications.maxOfOrNull { it.id } ?: 0L

    if (initialBaselineId < 0L) {
        preferences.edit().putLong(cursorKey, newestId).apply()
        return@withLock
    }

    val deliveredKey = "$DELIVERED_NOTIFICATION_PREFIX$studentId"
    val deliveredIds = preferences.getStringSet(deliveredKey, emptySet()).orEmpty()
    notifications.asSequence()
        .filter { it.id > initialBaselineId && it.id.toString() !in deliveredIds }
        .sortedBy { it.id }
        .forEach { remote ->
            notify(context, remote.id.toInt(), remote.title, remote.message)
            rememberNotificationDelivered(context, studentId, remote.id)
        }
}

fun rememberNotificationDelivered(context: Context, studentId: Long, notificationId: Long) {
    if (studentId < 1L || notificationId < 1L) return
    synchronized(notificationDeliveryLock) {
        val preferences = context.getSharedPreferences(NOTIFICATION_PREFERENCES, Context.MODE_PRIVATE)
        val key = "$DELIVERED_NOTIFICATION_PREFIX$studentId"
        val deliveredIds = preferences.getStringSet(key, emptySet()).orEmpty().toMutableSet()
        deliveredIds += notificationId.toString()
        val boundedIds = if (deliveredIds.size <= MAX_DELIVERED_NOTIFICATION_IDS) {
            deliveredIds
        } else {
            deliveredIds.mapNotNull(String::toLongOrNull)
                .sortedDescending()
                .take(MAX_DELIVERED_NOTIFICATION_IDS)
                .map(Long::toString)
                .toMutableSet()
        }
        preferences.edit().putStringSet(key, boundedIds).apply()
    }
}

fun scheduleServerNotificationSync(
    context: Context,
    serverAddress: String,
    token: String,
    studentId: Long
) {
    val request = PeriodicWorkRequestBuilder<ServerNotificationWorker>(
        MIN_BACKGROUND_SYNC_MINUTES,
        TimeUnit.MINUTES
    )
        .setConstraints(
            Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
        )
        .setInputData(
            Data.Builder()
                .putString(INPUT_SERVER_ADDRESS, serverAddress)
                .putString(INPUT_STUDENT_TOKEN, token)
                .putLong(INPUT_STUDENT_ID, studentId)
                .build()
        )
        .build()

    WorkManager.getInstance(context).enqueueUniquePeriodicWork(
        workName(studentId),
        ExistingPeriodicWorkPolicy.REPLACE,
        request
    )
}

fun cancelServerNotificationSync(context: Context, studentId: Long) {
    WorkManager.getInstance(context).cancelUniqueWork(workName(studentId))
}

fun cancelLegacyDailyReminder(context: Context) {
    WorkManager.getInstance(context).cancelUniqueWork("kas_reminder")
}

class ServerNotificationWorker(context: Context, parameters: WorkerParameters) :
    CoroutineWorker(context, parameters) {

    override suspend fun doWork(): Result {
        val address = inputData.getString(INPUT_SERVER_ADDRESS) ?: return Result.failure()
        val token = inputData.getString(INPUT_STUDENT_TOKEN) ?: return Result.failure()
        val studentId = inputData.getLong(INPUT_STUDENT_ID, -1L)
        if (studentId < 1L) return Result.failure()

        return try {
            syncServerNotifications(applicationContext, address, token, studentId)
            Result.success()
        } catch (exception: KasMateApiException) {
            if (exception.statusCode == 401) Result.failure() else Result.retry()
        } catch (_: Exception) {
            Result.retry()
        }
    }
}

/** Keeps already-enqueued work from older installs loadable until the app cancels it. */
class ReminderWorker(context: Context, parameters: WorkerParameters) :
    CoroutineWorker(context, parameters) {
    override suspend fun doWork(): Result {
        val prefs = applicationContext.getSharedPreferences("kasmate_preferences", Context.MODE_PRIVATE)
        val amount = prefs.getLong("overdue_amount", 0L)
        if (amount <= 0L) return Result.success()
        val english = prefs.getString("language", "INDONESIAN") == "ENGLISH"
        val number = NumberFormat.getInstance(if (english) Locale.US else Locale("id", "ID")).format(amount)
        val days = prefs.getInt("overdue_days", 1)
        val title = if (english) "KasMate reminder" else "Pengingat KasMate"
        val message = if (english) {
            "Your class dues of $number rupiah are overdue by $days ${if (days == 1) "day" else "days"}. Let's settle them today!"
        } else {
            "Tunggakan kasmu Rp $number sudah lewat $days hari. Yuk, bereskan hari ini!"
        }
        notify(applicationContext, 1, title, message)
        return Result.success()
    }
}

fun scheduleDailyReminder(context: Context) {
    val request = PeriodicWorkRequestBuilder<ReminderWorker>(1, TimeUnit.DAYS).build()
    WorkManager.getInstance(context)
        .enqueueUniquePeriodicWork("kas_reminder", ExistingPeriodicWorkPolicy.KEEP, request)
}

private fun workName(studentId: Long) = "kas_server_notifications_$studentId"
