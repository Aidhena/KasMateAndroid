package com.smktel.kasmate.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import android.animation.ValueAnimator
import android.content.Context
import android.os.Build
import android.provider.Settings
import com.smktel.kasmate.R

enum class AppFont(val label: String) {
    SANS("Sans serif"), SERIF("Serif"), MONOSPACE("Monospace")
}

val LocalMotionEnabled = staticCompositionLocalOf { true }

private val PlusJakartaSans = FontFamily(
    Font(R.font.plus_jakarta_sans_regular, FontWeight.Normal),
    Font(R.font.plus_jakarta_sans_medium, FontWeight.Medium),
    Font(R.font.plus_jakarta_sans_semibold, FontWeight.SemiBold),
    Font(R.font.plus_jakarta_sans_bold, FontWeight.Bold),
    Font(R.font.plus_jakarta_sans_extrabold, FontWeight.ExtraBold)
)

private fun systemAnimationsEnabled(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        ValueAnimator.areAnimatorsEnabled()
    } else {
        Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) > 0f
    }
}

val Red = Color(0xFFE31E24)
val DeepRed = Color(0xFFB5121B)
val SoftRed = Color(0xFFFF6B70)
val AppBackground = Color(0xFFF7F8FA)
val CardBorder = Color(0xFFEEEEF0)

private val LightColors = lightColorScheme(
    primary = Red,
    onPrimary = Color.White,
    background = AppBackground,
    onBackground = Color(0xFF171717),
    surface = Color.White,
    onSurface = Color(0xFF171717),
    surfaceVariant = Color(0xFFFFF1F1),
    onSurfaceVariant = Color(0xFF858585),
    outline = CardBorder,
    tertiary = DeepRed,
    secondary = Color(0xFF2E7D32)
)

private val DarkColors = darkColorScheme(
    primary = Red,
    onPrimary = Color.White,
    background = Color(0xFF111216),
    onBackground = Color(0xFFF4F4F5),
    surface = Color(0xFF191A20),
    onSurface = Color(0xFFF4F4F5),
    surfaceVariant = Color(0xFF24252C),
    onSurfaceVariant = Color(0xFFB3B3BA),
    outline = Color(0xFF34353C),
    tertiary = Color(0xFFFF8A80),
    secondary = Color(0xFF81C784)
)

val BigShape = RoundedCornerShape(24.dp)

@Composable
fun KasTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    font: AppFont = AppFont.SANS,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val view = LocalView.current
    val motionEnabled = androidx.compose.runtime.remember(context) { systemAnimationsEnabled(context) }
    if (!view.isInEditMode) {
        SideEffect {
            (context as? android.app.Activity)?.window?.let { window ->
                val insetsController = WindowCompat.getInsetsController(window, view)
                insetsController.isAppearanceLightStatusBars = !darkTheme
                insetsController.isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }
    val fontFamily = when (font) {
        AppFont.SANS -> PlusJakartaSans
        AppFont.SERIF -> FontFamily.Serif
        AppFont.MONOSPACE -> FontFamily.Monospace
    }
    val typography = MaterialTheme.typography.let { base ->
        androidx.compose.material3.Typography(
            displayLarge = base.displayLarge.copy(fontFamily = fontFamily, fontWeight = FontWeight.ExtraBold, letterSpacing = (-.7).sp),
            displayMedium = base.displayMedium.copy(fontFamily = fontFamily, fontWeight = FontWeight.Bold, letterSpacing = (-.45).sp),
            headlineLarge = base.headlineLarge.copy(fontFamily = fontFamily, fontWeight = FontWeight.Bold, letterSpacing = (-.4).sp),
            headlineMedium = base.headlineMedium.copy(fontFamily = fontFamily, fontWeight = FontWeight.Bold, letterSpacing = (-.3).sp),
            titleLarge = base.titleLarge.copy(fontFamily = fontFamily, fontWeight = FontWeight.Bold, letterSpacing = (-.25).sp),
            titleMedium = base.titleMedium.copy(fontFamily = fontFamily, fontWeight = FontWeight.SemiBold, letterSpacing = (-.1).sp),
            bodyLarge = TextStyle(fontFamily = fontFamily, fontSize = 16.sp, lineHeight = 24.sp, letterSpacing = .05.sp),
            bodyMedium = TextStyle(fontFamily = fontFamily, fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = .05.sp),
            bodySmall = TextStyle(fontFamily = fontFamily, fontSize = 12.sp, lineHeight = 16.sp),
            labelLarge = base.labelLarge.copy(fontFamily = fontFamily),
            labelMedium = base.labelMedium.copy(fontFamily = fontFamily),
            labelSmall = base.labelSmall.copy(fontFamily = fontFamily)
        )
    }
    CompositionLocalProvider(LocalMotionEnabled provides motionEnabled) {
      MaterialTheme(colorScheme = if (darkTheme) DarkColors else LightColors, typography = typography) {
        val background = if (darkTheme) {
            Brush.linearGradient(
                colors = listOf(Color(0xFF111216), Color(0xFF1B1316), Color(0xFF111216)),
                start = Offset.Zero,
                end = Offset(0f, 1400f)
            )
        } else {
            Brush.linearGradient(
                colors = listOf(Color(0xFFF7F8FA), Color(0xFFFFF7F5), Color(0xFFF7F8FA)),
                start = Offset.Zero,
                end = Offset(0f, 1400f)
            )
        }
        androidx.compose.foundation.layout.Box(Modifier.fillMaxSize().background(background)) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = Color.Transparent,
                contentColor = MaterialTheme.colorScheme.onBackground
            ) {
                content()
            }
        }
      }
    }
}
