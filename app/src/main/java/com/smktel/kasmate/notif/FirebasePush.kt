package com.smktel.kasmate.notif

import android.content.Context
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.smktel.kasmate.data.KasMateApiClient
import com.smktel.kasmate.data.KasMateServerConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

private const val TAG = "KasMatePush"
private const val APP_PREFERENCES = "kasmate_preferences"
private const val PREF_API_BASE_URL = "api_base_url"
private const val PREF_STUDENT_TOKEN = "student_access_token"
const val PREF_FCM_INSTALLATION_ID = "fcm_installation_id"

/** Registers this signed-in app installation with Firebase when its project config is present. */
fun registerPushDevice(context: Context) {
    val preferences = context.getSharedPreferences(APP_PREFERENCES, Context.MODE_PRIVATE)
    if (preferences.getString(PREF_STUDENT_TOKEN, null).isNullOrBlank()) return
    if (defaultFirebaseApp(context) == null) return

    FirebaseMessaging.getInstance().register()
        .addOnFailureListener { exception ->
            Log.w(TAG, "Could not register with Firebase Cloud Messaging.", exception)
        }
}

fun unregisterPushDevice(context: Context) {
    if (defaultFirebaseApp(context) == null) return
    FirebaseMessaging.getInstance().unregister()
        .addOnFailureListener { exception ->
            Log.w(TAG, "Could not unregister this app installation from FCM.", exception)
        }
}

private fun defaultFirebaseApp(context: Context): FirebaseApp? =
    runCatching { FirebaseApp.getInstance() }.getOrNull()
        ?: runCatching { FirebaseApp.initializeApp(context) }.getOrNull()

private fun registerInstallationWithServer(context: Context, installationId: String) {
    val preferences = context.getSharedPreferences(APP_PREFERENCES, Context.MODE_PRIVATE)
    preferences.edit().putString(PREF_FCM_INSTALLATION_ID, installationId).apply()

    val studentToken = preferences.getString(PREF_STUDENT_TOKEN, null).orEmpty()
    if (studentToken.isBlank()) return
    val savedAddress = preferences.getString(PREF_API_BASE_URL, null)
    val serverAddress = KasMateServerConfig.migrateSavedAddress(savedAddress)
    if (serverAddress != savedAddress) {
        preferences.edit().putString(PREF_API_BASE_URL, serverAddress).apply()
    }

    CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
        runCatching {
            KasMateApiClient(serverAddress).registerPushDevice(studentToken, installationId)
        }.onFailure { exception ->
            Log.w(TAG, "Could not register this device for notifications.", exception)
        }
    }
}

class KasMateFirebaseMessagingService : FirebaseMessagingService() {
    override fun onRegistered(installationId: String) {
        super.onRegistered(installationId)
        registerInstallationWithServer(this, installationId)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        val data = message.data
        val title = data["title"] ?: message.notification?.title ?: "KasMate"
        val body = data["body"] ?: message.notification?.body.orEmpty()
        val notificationId = data["notificationId"]?.toLongOrNull()
        val studentId = data["studentId"]?.toLongOrNull()

        if (notificationId != null && studentId != null && notificationId > 0L && studentId > 0L) {
            rememberNotificationDelivered(this, studentId, notificationId)
        }

        if (body.isNotBlank()) {
            notify(
                this,
                notificationId?.toInt() ?: System.currentTimeMillis().toInt(),
                title,
                body
            )
        }
    }
}
