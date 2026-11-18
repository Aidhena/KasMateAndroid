package com.smktel.kasmate.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.animateFloat
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Image
import com.smktel.kasmate.R

@Composable
private fun AuthHeader(title: String, subtitle: String) {
    Column(
        Modifier
            .fillMaxWidth()
            .background(
                Brush.linearGradient(listOf(DeepRed, Red, SoftRed)),
                RoundedCornerShape(bottomStart = 36.dp, bottomEnd = 36.dp)
            )
            .padding(top = 42.dp, bottom = 30.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AnimatedVisibility(visible = true, enter = fadeIn(tween(320)) + slideInVertically(tween(320)) { it / 5 }) {
            Image(
                painter = painterResource(R.drawable.kasmate_finance_wallet),
                contentDescription = "Logo KasMate",
                contentScale = ContentScale.Fit,
                modifier = Modifier.size(144.dp).clip(RoundedCornerShape(30.dp))
            )
        }
        Text(
            title,
            color = Color.White,
            fontSize = 26.sp,
            fontWeight = FontWeight.ExtraBold,
            modifier = Modifier.padding(top = 12.dp)
        )
        Text(subtitle, color = Color.White.copy(alpha = .88f), fontSize = 14.sp)
    }
}

@Composable
private fun AuthField(
    value: String,
    onChange: (String) -> Unit,
    label: String,
    password: Boolean = false,
    icon: ImageVector? = null,
    onPasswordVisibility: (Boolean) -> Unit = {}
) {
    var visible by remember { mutableStateOf(false) }
    val trailing: (@Composable () -> Unit)? = if (password) {
        {
            androidx.compose.material3.IconButton(onClick = { visible = !visible; onPasswordVisibility(visible) }) {
                Icon(if (visible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                    contentDescription = if (visible) "Sembunyikan password" else "Tampilkan password",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    } else null

    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        singleLine = true,
        shape = RoundedCornerShape(14.dp),
        visualTransformation =
            if (password && !visible) PasswordVisualTransformation() else VisualTransformation.None,
        keyboardOptions = KeyboardOptions(
            keyboardType = if (password) KeyboardType.Password else KeyboardType.Text
        ),
        leadingIcon = icon?.let { { Icon(it, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) } },
        trailingIcon = trailing,
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun AuthButton(text: String, onClick: () -> Unit) {
    val interaction = androidx.compose.runtime.remember { MutableInteractionSource() }
    val pressed = interaction.collectIsPressedAsState().value
    val scale = animateFloatAsState(if (pressed) .97f else 1f, animationSpec = tween(180), label = "button scale")
    val shape = RoundedCornerShape(14.dp)
    Button(
        onClick = onClick,
        interactionSource = interaction,
        modifier = Modifier.fillMaxWidth().height(54.dp).graphicsLayer { scaleX = scale.value; scaleY = scale.value }
            .clip(shape).background(Brush.linearGradient(listOf(DeepRed, Red, SoftRed))),
        shape = shape,
        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent, contentColor = Color.White)
    ) {
        Text(text, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
    }
}

@Composable
fun LoginScreen(onLogin: (String) -> Unit) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var peeking by remember { mutableStateOf(false) }
    val floatTransition = rememberInfiniteTransition(label = "login mascot float")
    val floatY by floatTransition.animateFloat(-5f, 5f, infiniteRepeatable(tween(1300), RepeatMode.Reverse), label = "float")

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        AuthHeader("KasMate", "Keuangan kelas, lebih transparan")
        Surface(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(top = 20.dp, bottom = 24.dp),
            shape = BigShape,
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 3.dp
        ) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text("Selamat datang kembali", fontSize = 21.sp, fontWeight = FontWeight.Bold)
                Text("Masuk untuk melihat tagihan dan kabar kas kelas.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                AuthField(username, { username = it }, "Username", icon = Icons.Default.Person)
                Box(Modifier.fillMaxWidth().height(108.dp), contentAlignment = Alignment.Center) {
                    Image(painter = painterResource(if (password.isNotEmpty() && !peeking) R.drawable.kasmate_mascot_sad else R.drawable.kasmate_mascot_neutral),
                        contentDescription = "Maskot KasMate", contentScale = ContentScale.Fit,
                        modifier = Modifier.size(104.dp).graphicsLayer { translationY = floatY })
                }
                AuthField(password, { password = it }, "Password", password = true, icon = Icons.Default.Lock,
                    onPasswordVisibility = { peeking = it })
                error?.let { Text(it, color = MaterialTheme.colorScheme.tertiary) }
                AuthButton("MASUK") {
                    error = if (username.isBlank() || password.isBlank()) {
                        "Username dan password wajib diisi"
                    } else null
                    if (error == null) onLogin(username.trim().substringBefore('@').substringBefore(' '))
                }
                Text("Gunakan akun siswa yang diberikan sekolah", modifier = Modifier.align(Alignment.CenterHorizontally), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
            }
        }
    }
}

@Composable
fun WelcomeScreen(onGetStarted: () -> Unit) {
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(26.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.weight(.7f))
        Image(painterResource(R.drawable.kasmate_finance_wallet), "KasMate", Modifier.fillMaxWidth().height(270.dp), contentScale = ContentScale.Fit)
        Spacer(Modifier.height(28.dp))
        Text("Welcome to KasMate", fontSize = 29.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onBackground)
        Text("Kelola kas kelas dengan lebih transparan, praktis, dan tenang.", modifier = Modifier.padding(top = 10.dp, bottom = 28.dp), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
        AuthButton("GET STARTED", onGetStarted)
        Spacer(Modifier.weight(1f))
    }
}

@Composable
fun RegisterScreen(onRegister: (String) -> Unit, onGoLogin: () -> Unit) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        AuthHeader("Buat akun", "Mulai kelola kas dengan lebih mudah")
        Surface(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(top = 20.dp, bottom = 24.dp),
            shape = BigShape,
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 3.dp
        ) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text("Daftar ke KasMate", fontSize = 21.sp, fontWeight = FontWeight.Bold)
                AuthField(username, { username = it }, "Username", icon = Icons.Default.Person)
                AuthField(password, { password = it }, "Password", password = true, icon = Icons.Default.Lock)
                AuthField(confirm, { confirm = it }, "Ulangi password", password = true, icon = Icons.Default.Lock)
                error?.let { Text(it, color = MaterialTheme.colorScheme.tertiary) }
                AuthButton("DAFTAR") {
                    error = when {
                        username.isBlank() -> "Username wajib diisi"
                        password.length < 6 -> "Password minimal 6 karakter"
                        password != confirm -> "Konfirmasi password tidak sama"
                        else -> null
                    }
                    if (error == null) onRegister(username.trim().substringBefore('@').substringBefore(' '))
                }
                TextButton(onClick = onGoLogin, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                    Text("Sudah punya akun? Masuk", color = Red, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
