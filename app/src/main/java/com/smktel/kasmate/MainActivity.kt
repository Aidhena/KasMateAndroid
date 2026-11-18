package com.smktel.kasmate

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.smktel.kasmate.notif.createChannel
import com.smktel.kasmate.notif.scheduleDailyReminder
import com.smktel.kasmate.ui.HomeScreen
import com.smktel.kasmate.ui.KasTheme
import com.smktel.kasmate.ui.LoginScreen
import com.smktel.kasmate.ui.WelcomeScreen
import com.smktel.kasmate.ui.AppLanguage

enum class Screen { WELCOME, LOGIN, HOME }

class MainActivity : ComponentActivity() {

    private val askNotifPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        createChannel(this)
        scheduleDailyReminder(this)
        if (Build.VERSION.SDK_INT >= 33) {
            askNotifPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        setContent {
            val prefs = remember { getSharedPreferences("kasmate_preferences", MODE_PRIVATE) }
            var darkMode by remember { mutableStateOf(prefs.getBoolean("dark_mode", false)) }
            var language by remember { mutableStateOf(runCatching { AppLanguage.valueOf(prefs.getString("language", "INDONESIAN")!!) }.getOrDefault(AppLanguage.INDONESIAN)) }
            KasTheme(darkTheme = darkMode) {
                var screen by remember { mutableStateOf(if (prefs.getBoolean("welcome_seen", false)) Screen.LOGIN else Screen.WELCOME) }
                var activeUserName by remember { mutableStateOf("Aidhena") }
                when (screen) {
                    Screen.WELCOME -> WelcomeScreen(onGetStarted = { prefs.edit().putBoolean("welcome_seen", true).apply(); screen = Screen.LOGIN })
                    Screen.LOGIN -> LoginScreen(
                        onLogin = { username -> activeUserName = username; screen = Screen.HOME },
                    )
                    Screen.HOME -> HomeScreen(
                        userName = activeUserName,
                        darkMode = darkMode,
                        onDarkModeChange = { darkMode = it; prefs.edit().putBoolean("dark_mode", it).apply() },
                        language = language,
                        onLanguageChange = { language = it; prefs.edit().putString("language", it.name).apply() }
                    )
                }
            }
        }
    }
}
