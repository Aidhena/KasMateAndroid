package com.smktel.kasmate.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.Canvas
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.IconButton
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.Dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.RepeatMode
import androidx.compose.foundation.Image
import com.smktel.kasmate.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Calendar

@Composable
private fun LoginMascot(
    hasName: Boolean,
    nameFocused: Boolean,
    passwordActive: Boolean,
    passwordVisible: Boolean,
    loading: Boolean,
    hasError: Boolean,
    loginSuccess: Boolean,
    phrases: List<String>,
    robotSize: Dp
) {
    val motionEnabled = LocalMotionEnabled.current
    val coverEyes = passwordActive && !passwordVisible && !loading && !loginSuccess
    val resource = when {
        loginSuccess || loading -> R.drawable.kasmate_mascot_excited
        coverEyes -> if (hasName) R.drawable.kasmate_mascot_happy else R.drawable.kasmate_mascot_neutral
        hasError -> R.drawable.kasmate_mascot_sad
        hasName -> R.drawable.kasmate_mascot_happy
        else -> R.drawable.kasmate_mascot_neutral
    }
    val coverProgress by animateFloatAsState(
        targetValue = if (coverEyes) 1f else 0f,
        animationSpec = tween(if (motionEnabled) 380 else 0),
        label = "mascot covers eyes"
    )
    val bob = if (motionEnabled) {
        val transition = rememberInfiniteTransition(label = "login mascot float")
        transition.animateFloat(
            initialValue = -5f,
            targetValue = 5f,
            animationSpec = infiniteRepeatable(tween(1500), RepeatMode.Reverse),
            label = "login mascot float offset"
        ).value
    } else 0f
    val scale by animateFloatAsState(
        if (loading && motionEnabled) 1.04f else 1f,
        animationSpec = tween(if (motionEnabled) 240 else 0),
        label = "login mascot scale"
    )
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        LoginSpeechBubble(phrases)
        Spacer(Modifier.height(2.dp))
        AnimatedVisibility(
            visible = true,
            enter = if (motionEnabled) fadeIn(tween(280)) + slideInVertically(tween(280)) { it / 8 } else fadeIn(tween(0))
        ) {
            AnimatedContent(
                targetState = resource,
                transitionSpec = { if (motionEnabled) fadeIn(tween(180)) togetherWith fadeOut(tween(130)) else fadeIn(tween(0)) togetherWith fadeOut(tween(0)) },
                label = "login mascot expression"
            ) { image ->
                Box(
                    Modifier.size(robotSize).graphicsLayer {
                        translationY = bob
                        translationX = if (nameFocused && motionEnabled) 1.5f else 0f
                        scaleX = scale
                        scaleY = scale
                    },
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(image),
                        contentDescription = "KasMate helper mascot",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize()
                    )
                    if (coverProgress > 0f) {
                        LoginEyeCover(
                            progress = coverProgress,
                            backgroundColor = if (MaterialTheme.colorScheme.background == AppBackground) Color(0xFFFCF7F6) else Color(0xFF181316),
                            modifier = Modifier.fillMaxWidth().aspectRatio(340f / 285f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LoginEyeCover(progress: Float, backgroundColor: Color, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height

        fun maskOpenFlipper(left: Boolean) {
            val direction = if (left) 1f else -1f
            val outerX = w * if (left) .02f else .98f
            val innerX = w * if (left) .33f else .67f
            val mask = Path().apply {
                moveTo(outerX, h * .38f)
                cubicTo(
                    outerX + direction * w * .02f,
                    h * .34f,
                    innerX - direction * w * .04f,
                    h * .35f,
                    innerX,
                    h * .43f
                )
                lineTo(innerX + direction * w * .025f, h * .56f)
                cubicTo(
                    innerX - direction * w * .06f,
                    h * .65f,
                    outerX + direction * w * .1f,
                    h * .68f,
                    outerX,
                    h * .64f
                )
                close()
            }
            drawPath(mask, backgroundColor.copy(alpha = progress.coerceIn(0f, 1f)))
        }

        fun drawFlipper(left: Boolean) {
            val direction = if (left) 1f else -1f
            val shoulderX = w * if (left) .29f else .71f
            val shoulderY = h * .53f
            val targetX = w * if (left) .43f else .57f
            val start = Offset(shoulderX, shoulderY)
            val end = Offset(
                shoulderX + (targetX - shoulderX) * progress,
                shoulderY + (h * .23f - shoulderY) * progress
            )
            val arm = Path().apply {
                moveTo(start.x, start.y)
                cubicTo(
                    start.x + direction * w * .055f,
                    start.y - h * .12f * progress,
                    end.x - direction * w * .045f,
                    end.y + h * .06f,
                    end.x,
                    end.y
                )
                cubicTo(
                    end.x + direction * w * .035f,
                    end.y + h * .015f,
                    end.x + direction * w * .04f,
                    end.y + h * .075f,
                    end.x + direction * w * .005f,
                    end.y + h * .12f
                )
                cubicTo(
                    end.x - direction * w * .03f,
                    end.y + h * .17f,
                    start.x - direction * w * .025f,
                    start.y + h * .07f,
                    start.x,
                    start.y
                )
                close()
            }
            drawPath(arm, color = Color(0xFFF1F4F7))
            drawPath(arm, color = Color(0xFFB8C1CE), style = androidx.compose.ui.graphics.drawscope.Stroke(width = w * .012f))
            drawCircle(Color(0xFFF1F4F7), radius = w * .048f, center = end)
            drawCircle(Color(0xFFB8C1CE), radius = w * .048f, center = end, style = androidx.compose.ui.graphics.drawscope.Stroke(width = w * .009f))
        }

        maskOpenFlipper(left = true)
        maskOpenFlipper(left = false)
        drawFlipper(left = true)
        drawFlipper(left = false)
    }
}

@Composable
private fun LoginSpeechBubble(phrases: List<String>) {
    val motionEnabled = LocalMotionEnabled.current
    val phrasesKey = phrases.joinToString("|")
    var typedText by remember(phrasesKey) { mutableStateOf(phrases.firstOrNull().orEmpty()) }
    LaunchedEffect(phrasesKey, motionEnabled) {
        if (phrases.isEmpty()) return@LaunchedEffect
        if (!motionEnabled) {
            typedText = phrases.first()
            return@LaunchedEffect
        }
        var index = 0
        while (true) {
            val phrase = phrases[index % phrases.size]
            typedText = ""
            phrase.forEachIndexed { characterIndex, _ ->
                delay(58)
                typedText = phrase.take(characterIndex + 1)
            }
            delay(1_500)
            for (characterCount in phrase.length downTo 1) {
                delay(28)
                typedText = phrase.take(characterCount - 1)
            }
            delay(260)
            index = (index + 1) % phrases.size
        }
    }
    Surface(
        modifier = Modifier.widthIn(max = 290.dp),
        shape = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomEnd = 18.dp, bottomStart = 6.dp),
        color = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        shadowElevation = 5.dp
    ) {
        Text(
            text = buildAnnotatedString {
                append(typedText.ifBlank { " " })
                if (motionEnabled) withStyle(SpanStyle(color = Red)) { append("▏") }
            },
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            fontSize = 12.sp,
            lineHeight = 16.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis
        )
    }
}

private fun loginGreeting(english: Boolean): String {
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    return when (hour) {
        in 1..10 -> if (english) "Good morning" else "Selamat pagi"
        in 11..14 -> if (english) "Good afternoon" else "Selamat siang"
        in 15..18 -> if (english) "Good evening" else "Selamat sore"
        else -> if (english) "Good evening" else "Selamat malam"
    }
}

@Composable
private fun AuthField(
    value: String,
    onChange: (String) -> Unit,
    label: String,
    password: Boolean = false,
    icon: ImageVector? = null,
    keyboardType: KeyboardType = if (password) KeyboardType.Password else KeyboardType.Text,
    onPasswordVisibility: (Boolean) -> Unit = {},
    enabled: Boolean = true,
    english: Boolean = false,
    focusRequester: FocusRequester? = null,
    imeAction: ImeAction = ImeAction.Done,
    onImeAction: () -> Unit = {},
    onFocusChange: (Boolean) -> Unit = {}
) {
    var visible by remember { mutableStateOf(false) }
    val trailing: (@Composable () -> Unit)? = if (password) {
        {
            IconButton(onClick = { visible = !visible; onPasswordVisibility(visible) }) {
                Icon(
                    if (visible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                    contentDescription = if (visible) if (english) "Hide password" else "Sembunyikan password" else if (english) "Show password" else "Tampilkan password",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    } else null

    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        placeholder = { Text(label) },
        enabled = enabled,
        singleLine = true,
        shape = RoundedCornerShape(18.dp),
        visualTransformation =
            if (password && !visible) PasswordVisualTransformation() else VisualTransformation.None,
        keyboardOptions = KeyboardOptions(
            keyboardType = keyboardType,
            imeAction = imeAction
        ),
        keyboardActions = KeyboardActions(onNext = { onImeAction() }, onDone = { onImeAction() }),
        leadingIcon = icon?.let { { Icon(it, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) } },
        trailingIcon = trailing,
        modifier = Modifier.fillMaxWidth()
            .then(focusRequester?.let(Modifier::focusRequester) ?: Modifier)
            .onFocusChanged { onFocusChange(it.isFocused) },
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Red,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
            focusedContainerColor = MaterialTheme.colorScheme.surface,
            unfocusedContainerColor = if (MaterialTheme.colorScheme.background == AppBackground) Color(0xFFF7F8FA) else MaterialTheme.colorScheme.surfaceVariant
        )
    )
}

@Composable
private fun AuthButton(text: String, enabled: Boolean = true, trailingIcon: ImageVector? = null, onClick: () -> Unit) {
    val interaction = androidx.compose.runtime.remember { MutableInteractionSource() }
    val pressed = interaction.collectIsPressedAsState().value
    val motionEnabled = LocalMotionEnabled.current
    val scale = animateFloatAsState(if (pressed && motionEnabled) .97f else 1f, animationSpec = tween(if (motionEnabled) 180 else 0), label = "button scale")
    val shape = RoundedCornerShape(16.dp)
    Button(
        onClick = onClick,
        enabled = enabled,
        interactionSource = interaction,
        modifier = Modifier.fillMaxWidth().height(54.dp).graphicsLayer { scaleX = scale.value; scaleY = scale.value }
            .clip(shape).background(Brush.linearGradient(listOf(DeepRed, Red, SoftRed))),
        shape = shape,
        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent, contentColor = Color.White)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(text, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, letterSpacing = .35.sp)
            trailingIcon?.let { Icon(it, contentDescription = null, modifier = Modifier.size(18.dp)) }
        }
    }
}

@Composable
fun LoginScreen(
    initialServerAddress: String,
    isLoading: Boolean,
    errorMessage: String?,
    loginSuccess: Boolean,
    english: Boolean,
    onLogin: (String, String, String, String) -> Unit
) {
    var studentName by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var validationError by remember { mutableStateOf<String?>(null) }
    var passwordVisible by remember { mutableStateOf(false) }
    var nameFocused by remember { mutableStateOf(false) }
    var passwordFocused by remember { mutableStateOf(false) }
    var mascotName by remember { mutableStateOf("") }
    val nameFocus = remember { FocusRequester() }
    val passwordFocus = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val motionEnabled = LocalMotionEnabled.current
    LaunchedEffect(studentName) {
        delay(340)
        mascotName = studentName.trim().substringBefore(' ')
    }
    val greeting = loginGreeting(english)
    val dialogPhrases = when {
        loginSuccess -> listOf("$greeting, ${studentName.trim().ifBlank { if (english) "student" else "siswa" }}!", if (english) "Opening your dashboard…" else "Membuka beranda…")
        isLoading -> listOf(if (english) "Checking your account…" else "Memeriksa akunmu…", if (english) "Almost there!" else "Sebentar lagi!")
        validationError != null || errorMessage != null -> listOf(if (english) "Oops, check again!" else "Ups, periksa lagi!", (validationError ?: errorMessage).orEmpty())
        (passwordFocused || password.isNotEmpty()) && !passwordVisible -> listOf(if (english) "I won't look!" else "Aku tidak akan melihat!", if (english) "Your password stays private." else "Passwordmu tetap rahasia.")
        (passwordFocused || password.isNotEmpty()) && passwordVisible -> listOf(if (english) "I can see it!" else "Aku melihatnya!", if (english) "Ready to sign in?" else "Siap masuk?")
        nameFocused -> listOf("$greeting!", if (english) "What's your name?" else "Siapa namamu?")
        mascotName.isNotBlank() -> listOf("$greeting, $mascotName!", if (english) "Ready for KasMate?" else "Siap pakai KasMate?")
        else -> listOf("$greeting!", if (english) "Welcome to KasMate" else "Selamat datang di KasMate")
    }
    fun submitLogin() {
        validationError = when {
            studentName.isBlank() -> if (english) "Enter your student name." else "Nama siswa wajib diisi."
            password.isBlank() -> if (english) "Enter your password." else "Password wajib diisi."
            else -> null
        }
        if (validationError == null && !isLoading) {
            keyboardController?.hide()
            onLogin(initialServerAddress, studentName.trim(), "", password)
        }
    }

    BoxWithConstraints(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().imePadding()) {
        val availableHeight = maxHeight
        val compact = availableHeight < 760.dp
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = if (compact) 10.dp else 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Kas", color = MaterialTheme.colorScheme.onBackground, fontSize = if (compact) 28.sp else 32.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = (-.6).sp)
                Text("Mate", color = Red, fontSize = if (compact) 28.sp else 32.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = (-.6).sp)
            }
            Spacer(Modifier.height(if (compact) 4.dp else 8.dp))
            LoginMascot(
                hasName = mascotName.isNotBlank(),
                nameFocused = nameFocused,
                passwordActive = passwordFocused || password.isNotEmpty(),
                passwordVisible = passwordVisible,
                loading = isLoading,
                hasError = validationError != null || errorMessage != null,
                loginSuccess = loginSuccess,
                phrases = dialogPhrases,
                robotSize = minOf(184.dp, availableHeight * .23f)
            )
            Spacer(Modifier.height(if (compact) 4.dp else 8.dp))
            if (loginSuccess) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    color = Color(0xFF168A62).copy(alpha = if (MaterialTheme.colorScheme.background == AppBackground) .1f else .15f),
                    border = BorderStroke(1.dp, Color(0xFF168A62).copy(alpha = .28f))
                ) {
                    Column(Modifier.padding(22.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF168A62), modifier = Modifier.size(44.dp))
                        Text(if (english) "Login successful!" else "Berhasil masuk!", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
                        Text(if (english) "Opening your dashboard…" else "Membuka beranda…", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp, textAlign = TextAlign.Center)
                    }
                }
            } else {
                Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(if (compact) 9.dp else 12.dp)) {
                    AuthField(
                        studentName,
                        { studentName = it; validationError = null },
                        if (english) "Student name" else "Nama siswa",
                        icon = Icons.Default.Person,
                        enabled = !isLoading,
                        english = english,
                        focusRequester = nameFocus,
                        imeAction = ImeAction.Next,
                        onImeAction = { passwordFocus.requestFocus() },
                        onFocusChange = { nameFocused = it }
                    )
                    AuthField(
                        password,
                        { password = it; validationError = null },
                        "Password",
                        password = true,
                        icon = Icons.Default.Lock,
                        onPasswordVisibility = { passwordVisible = it },
                        enabled = !isLoading,
                        english = english,
                        focusRequester = passwordFocus,
                        imeAction = ImeAction.Done,
                        onImeAction = ::submitLogin,
                        onFocusChange = { passwordFocused = it }
                    )
                    AnimatedVisibility(
                        visible = validationError != null || errorMessage != null,
                        enter = if (motionEnabled) fadeIn(tween(160)) + slideInVertically(tween(160)) { it / 3 } else fadeIn(tween(0)),
                        exit = if (motionEnabled) fadeOut(tween(120)) + slideOutVertically(tween(120)) { -it / 4 } else fadeOut(tween(0))
                    ) {
                        Text(validationError ?: errorMessage.orEmpty(), color = Red, fontSize = 12.sp, textAlign = TextAlign.Center)
                    }
                    AuthButton(
                        if (isLoading) if (english) "SIGNING IN…" else "MENGHUBUNGKAN…" else if (english) "SIGN IN" else "MASUK",
                        enabled = !isLoading,
                        trailingIcon = Icons.Default.ArrowForward,
                        onClick = ::submitLogin
                    )
                    Text(
                        if (english) "Use the student account provided by your school." else "Gunakan akun siswa yang diberikan admin kelasmu.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 16.sp
                    )
                }
            }
        }
    }
}

@Composable
fun WelcomeScreen(english: Boolean, onGetStarted: () -> Unit) {
    val motionEnabled = LocalMotionEnabled.current
    var exiting by remember { mutableStateOf(false) }
    var entered by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    LaunchedEffect(Unit) { entered = true }
    val heroAlpha by animateFloatAsState(if (entered && !exiting) 1f else 0f, animationSpec = tween(if (motionEnabled) if (exiting) 300 else 700 else 0), label = "welcome hero alpha")
    val contentAlpha by animateFloatAsState(if (entered && !exiting) 1f else 0f, animationSpec = tween(if (motionEnabled) if (exiting) 240 else 500 else 0), label = "welcome content alpha")
    val contentOffset by animateFloatAsState(if (entered && !exiting) 0f else 22f, animationSpec = tween(if (motionEnabled) if (exiting) 260 else 560 else 0), label = "welcome content offset")
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val compact = maxHeight < 720.dp
        val heroHeight = minOf(maxHeight * if (compact) .38f else .54f, if (compact) 260.dp else 480.dp)
        Box(Modifier.fillMaxWidth().height(heroHeight).align(Alignment.TopCenter)) {
            Image(
                painter = painterResource(R.drawable.kasmate_welcome_hero),
                contentDescription = if (english) "Planning class finances" else "Perencanaan keuangan kelas",
                modifier = Modifier.fillMaxSize().graphicsLayer { alpha = heroAlpha },
                contentScale = ContentScale.Crop
            )
            Box(
                Modifier.fillMaxSize().background(
                    Brush.verticalGradient(
                        colorStops = arrayOf(
                            0f to Color.Black.copy(alpha = .18f),
                            .38f to Color.Transparent,
                            1f to MaterialTheme.colorScheme.background
                        )
                    )
                )
            )
        }
        Column(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = if (compact) 24.dp else 28.dp)
                .padding(bottom = if (compact) 14.dp else 28.dp)
                .graphicsLayer { alpha = contentAlpha; translationY = contentOffset },
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("KASMATE", color = Red, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.4.sp)
            Spacer(Modifier.height(8.dp))
            Text(
                buildAnnotatedString {
                    append(if (english) "Welcome to " else "Selamat datang di ")
                    withStyle(SpanStyle(color = Red)) { append("KasMate") }
                },
                fontSize = if (compact) 27.sp else 31.sp,
                lineHeight = if (compact) 32.sp else 36.sp,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                if (english) "Manage class funds transparently, effortlessly, and with peace of mind." else "Kelola kas kelas dengan transparan, praktis, dan tenang.",
                modifier = Modifier.padding(top = if (compact) 8.dp else 12.dp, bottom = if (compact) 16.dp else 26.dp).widthIn(max = 310.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = if (compact) 13.sp else 14.sp,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp
            )
            AuthButton(
                if (english) "GET STARTED" else "MULAI",
                trailingIcon = Icons.Default.ArrowForward,
                onClick = {
                    if (exiting) return@AuthButton
                    exiting = true
                    scope.launch {
                        if (motionEnabled) delay(370)
                        onGetStarted()
                    }
                }
            )
        }
    }
}

@Composable
fun KasMateLaunchScreen(english: Boolean) {
    val motionEnabled = LocalMotionEnabled.current
    val transition = rememberInfiniteTransition(label = "launch mascot")
    val bob by transition.animateFloat(
        initialValue = 0f,
        targetValue = if (motionEnabled) 5f else 0f,
        animationSpec = infiniteRepeatable(tween(1350), RepeatMode.Reverse),
        label = "launch mascot bob"
    )
    Column(
        Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(
            painter = painterResource(R.drawable.kasmate_mascot_neutral),
            contentDescription = if (english) "KasMate mascot" else "Maskot KasMate",
            contentScale = ContentScale.Fit,
            modifier = Modifier.size(132.dp).graphicsLayer { translationY = bob }
        )
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Kas", fontSize = 30.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onBackground)
            Text("Mate", fontSize = 30.sp, fontWeight = FontWeight.ExtraBold, color = Red)
        }
        Spacer(Modifier.height(16.dp))
        Text(if (english) "Opening your class account…" else "Membuka akun kas kelas…", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
        Spacer(Modifier.height(18.dp))
        androidx.compose.material3.CircularProgressIndicator(color = Red, strokeWidth = 2.dp, modifier = Modifier.size(24.dp))
    }
}
