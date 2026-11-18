package com.smktel.kasmate

import android.Manifest
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.smktel.kasmate.data.KasMateApiClient
import com.smktel.kasmate.data.KasMateApiException
import com.smktel.kasmate.data.KasSummary
import com.smktel.kasmate.data.StudentPaymentRequest
import com.smktel.kasmate.data.StudentProfile
import com.smktel.kasmate.notif.cancelLegacyDailyReminder
import com.smktel.kasmate.notif.cancelServerNotificationSync
import com.smktel.kasmate.notif.createChannel
import com.smktel.kasmate.notif.PREF_FCM_INSTALLATION_ID
import com.smktel.kasmate.notif.registerPushDevice
import com.smktel.kasmate.notif.scheduleServerNotificationSync
import com.smktel.kasmate.notif.syncServerNotifications
import com.smktel.kasmate.notif.unregisterPushDevice
import com.smktel.kasmate.ui.AppLanguage
import com.smktel.kasmate.ui.HomeScreen
import com.smktel.kasmate.ui.KasTheme
import com.smktel.kasmate.ui.LoginScreen
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

private const val PREFERENCES = "kasmate_preferences"
private const val PREF_API_BASE_URL = "api_base_url"
private const val PREF_STUDENT_TOKEN = "student_access_token"
private const val DEFAULT_API_ADDRESS = "10.199.241.182"
private const val STUDENT_REFRESH_INTERVAL_MILLIS = 15_000L
private val PREVIOUS_API_ADDRESSES = listOf("192.168.18.9", "10.210.34.103")

private data class ActiveStudentSession(
    val token: String,
    val serverAddress: String,
    val profile: StudentProfile,
    val summary: KasSummary
)

class MainActivity : ComponentActivity() {

    private val askNotifPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        createChannel(this)
        cancelLegacyDailyReminder(this)
        if (Build.VERSION.SDK_INT >= 33) {
            askNotifPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        setContent {
            val prefs = remember { getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE) }
            var darkMode by remember { mutableStateOf(prefs.getBoolean("dark_mode", false)) }
            var language by remember {
                mutableStateOf(
                    runCatching {
                        AppLanguage.valueOf(prefs.getString("language", "INDONESIAN")!!)
                    }.getOrDefault(AppLanguage.INDONESIAN)
                )
            }
            var serverAddress by remember {
                val savedAddress = prefs.getString(PREF_API_BASE_URL, null)
                val initialAddress = when {
                    savedAddress.isNullOrBlank() -> DEFAULT_API_ADDRESS
                    else -> PREVIOUS_API_ADDRESSES.fold(savedAddress) { address, previousAddress ->
                        address.replace(previousAddress, DEFAULT_API_ADDRESS)
                    }
                }
                if (savedAddress != initialAddress) {
                    prefs.edit().putString(PREF_API_BASE_URL, initialAddress).apply()
                }
                mutableStateOf(initialAddress)
            }
            var activeSession by remember { mutableStateOf<ActiveStudentSession?>(null) }
            var paymentRequests by remember { mutableStateOf(emptyList<StudentPaymentRequest>()) }
            var isRestoringSession by remember { mutableStateOf(true) }
            var isSigningIn by remember { mutableStateOf(false) }
            var authError by remember { mutableStateOf<String?>(null) }
            val scope = rememberCoroutineScope()
            val lifecycleOwner = LocalLifecycleOwner.current

            LaunchedEffect(Unit) {
                val token = prefs.getString(PREF_STUDENT_TOKEN, null)
                if (!token.isNullOrBlank()) {
                    try {
                        val client = KasMateApiClient(serverAddress)
                        val remote = client.loadDashboard(token)
                        paymentRequests = runCatching { client.loadPaymentRequests(token) }.getOrDefault(emptyList())
                        scheduleServerNotificationSync(
                            applicationContext,
                            client.baseUrl,
                            token,
                            remote.profile.id
                        )
                        runCatching {
                            syncServerNotifications(
                                applicationContext,
                                client.baseUrl,
                                token,
                                remote.profile.id
                            )
                        }
                        registerPushDevice(applicationContext)
                        activeSession = ActiveStudentSession(token, client.baseUrl, remote.profile, remote.summary)
                        serverAddress = client.baseUrl
                    } catch (exception: Exception) {
                        if (exception is KasMateApiException && exception.statusCode == 401) {
                            prefs.edit().remove(PREF_STUDENT_TOKEN).apply()
                        }
                        authError = errorFor(exception)
                    }
                }
                isRestoringSession = false
            }

            LaunchedEffect(activeSession?.token, lifecycleOwner) {
                val current = activeSession ?: return@LaunchedEffect
                lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                    while (isActive) {
                        val client = KasMateApiClient(current.serverAddress)
                        runCatching { client.loadPaymentRequests(current.token) }
                            .onSuccess { paymentRequests = it }
                        runCatching { client.loadDashboard(current.token) }
                            .onSuccess { remote ->
                                if (activeSession?.token == current.token) {
                                    activeSession = current.copy(profile = remote.profile, summary = remote.summary)
                                }
                            }
                        runCatching {
                            syncServerNotifications(
                                applicationContext,
                                current.serverAddress,
                                current.token,
                                current.profile.id
                            )
                        }
                        delay(STUDENT_REFRESH_INTERVAL_MILLIS)
                    }
                }
            }

            KasTheme(darkTheme = darkMode) {
                val session = activeSession
                when {
                    isRestoringSession -> Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        }
                    }
                    session == null -> LoginScreen(
                        initialServerAddress = serverAddress,
                        isLoading = isSigningIn,
                        errorMessage = authError,
                        onLogin = { address, name, nis, password ->
                            if (!isSigningIn) {
                                isSigningIn = true
                                authError = null
                                scope.launch {
                                    try {
                                        val client = KasMateApiClient(address)
                                        serverAddress = client.baseUrl
                                        prefs.edit().putString(PREF_API_BASE_URL, client.baseUrl).apply()
                                        val login = client.login(name, nis, password)
                                        val remote = client.loadDashboard(login.token)
                                        paymentRequests = runCatching { client.loadPaymentRequests(login.token) }.getOrDefault(emptyList())
                                        prefs.edit().putString(PREF_STUDENT_TOKEN, login.token).apply()
                                        scheduleServerNotificationSync(
                                            applicationContext,
                                            client.baseUrl,
                                            login.token,
                                            remote.profile.id
                                        )
                                        runCatching {
                                            syncServerNotifications(
                                                applicationContext,
                                                client.baseUrl,
                                                login.token,
                                                remote.profile.id
                                            )
                                        }
                                        registerPushDevice(applicationContext)
                                        activeSession = ActiveStudentSession(
                                            login.token,
                                            client.baseUrl,
                                            remote.profile,
                                            remote.summary
                                        )
                                    } catch (exception: Exception) {
                                        authError = errorFor(exception)
                                    } finally {
                                        isSigningIn = false
                                    }
                                }
                            }
                        }
                    )
                    else -> HomeScreen(
                        userName = session.profile.name,
                        className = session.profile.className,
                        studentNis = session.profile.nis,
                        summary = session.summary,
                        paymentRequests = paymentRequests,
                        onSubmitPayment = { billStudentId, method, proofUri, onComplete ->
                            val current = activeSession
                            if (current == null) {
                                onComplete("Sesi sudah berakhir. Silakan masuk kembali.")
                            } else {
                                scope.launch {
                                    try {
                                        val client = KasMateApiClient(current.serverAddress)
                                        client.submitPaymentRequest(
                                            applicationContext,
                                            current.token,
                                            billStudentId,
                                            method,
                                            proofUri
                                        )
                                        paymentRequests = runCatching { client.loadPaymentRequests(current.token) }
                                            .getOrDefault(paymentRequests)
                                        runCatching { client.loadDashboard(current.token) }
                                            .onSuccess { remote ->
                                                if (activeSession?.token == current.token) {
                                                    activeSession = current.copy(profile = remote.profile, summary = remote.summary)
                                                }
                                            }
                                        onComplete(null)
                                    } catch (exception: Exception) {
                                        onComplete(errorFor(exception))
                                    }
                                }
                            }
                        },
                        onRefresh = {
                            val current = activeSession
                            if (current != null) {
                                scope.launch {
                                    try {
                                        val client = KasMateApiClient(current.serverAddress)
                                        val remote = client.loadDashboard(current.token)
                                        paymentRequests = runCatching { client.loadPaymentRequests(current.token) }.getOrDefault(paymentRequests)
                                        if (activeSession?.token == current.token) {
                                            activeSession = current.copy(profile = remote.profile, summary = remote.summary)
                                        }
                                    } catch (exception: Exception) {
                                        if (exception is KasMateApiException && exception.statusCode == 401) {
                                            cancelServerNotificationSync(this@MainActivity, current.profile.id)
                                            prefs.edit().remove(PREF_STUDENT_TOKEN).apply()
                                            activeSession = null
                                            authError = "Sesi berakhir. Silakan masuk kembali."
                                        } else {
                                            Toast.makeText(this@MainActivity, errorFor(exception), Toast.LENGTH_LONG).show()
                                        }
                                    }
                                }
                            }
                        },
                        darkMode = darkMode,
                        onDarkModeChange = {
                            darkMode = it
                            prefs.edit().putBoolean("dark_mode", it).apply()
                        },
                        language = language,
                        onLanguageChange = {
                            language = it
                            prefs.edit().putString("language", it.name).apply()
                        },
                        onLogout = {
                            val signedIn = activeSession
                            val installationId = prefs.getString(PREF_FCM_INSTALLATION_ID, null)
                            activeSession = null
                            prefs.edit()
                                .remove(PREF_STUDENT_TOKEN)
                                .remove(PREF_FCM_INSTALLATION_ID)
                                .apply()
                            unregisterPushDevice(applicationContext)
                            if (signedIn != null) {
                                cancelServerNotificationSync(this@MainActivity, signedIn.profile.id)
                                scope.launch {
                                    KasMateApiClient(signedIn.serverAddress)
                                        .logout(signedIn.token, installationId)
                                }
                            }
                        }
                    )
                }
            }
        }
    }

    private fun errorFor(exception: Exception): String = when (exception) {
        is IllegalArgumentException -> exception.message ?: "Alamat server tidak valid."
        is KasMateApiException -> exception.message ?: "Permintaan ke server gagal."
        else -> "Tidak bisa terhubung ke server. Periksa IP, koneksi Wi-Fi, dan Apache di komputer server."
    }
}
