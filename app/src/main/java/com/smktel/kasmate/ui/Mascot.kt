package com.smktel.kasmate.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import com.smktel.kasmate.R

enum class MascotMood { NEUTRAL, SAD, HAPPY, EXCITED }

@Composable
fun KasMateMascotBanner(
    mood: MascotMood,
    message: String,
    modifier: Modifier = Modifier,
    assistantLabel: String = "KasMate AI"
) {
    val resource = when (mood) {
        MascotMood.SAD -> R.drawable.kasmate_mascot_sad
        MascotMood.HAPPY -> R.drawable.kasmate_mascot_happy
        MascotMood.EXCITED -> R.drawable.kasmate_mascot_excited
        MascotMood.NEUTRAL -> R.drawable.kasmate_mascot_neutral
    }
    var popped by remember(mood) { mutableStateOf(false) }
    LaunchedEffect(mood) { popped = false; delay(45); popped = true }
    val mascotScale by animateFloatAsState(if (popped) 1f else .72f, animationSpec = spring(dampingRatio = .48f, stiffness = 420f), label = "mascot pop")
    Row(
        modifier.fillMaxWidth().animateContentSize().shadow(10.dp, RoundedCornerShape(26.dp))
            .clip(RoundedCornerShape(26.dp))
            .background(Brush.linearGradient(listOf(Red.copy(alpha = .12f), MaterialTheme.colorScheme.surface, MaterialTheme.colorScheme.surface)))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(13.dp)
    ) {
        Box(Modifier.size(82.dp).clip(CircleShape).background(Brush.radialGradient(listOf(Color.White, Red.copy(alpha = .09f)))), contentAlignment = Alignment.Center) {
            AnimatedContent(targetState = resource, label = "mascot expression") { image ->
                Image(painterResource(image), contentDescription = assistantLabel, modifier = Modifier.fillMaxSize().scale(mascotScale).padding(2.dp), contentScale = ContentScale.Fit)
            }
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(assistantLabel, color = Red, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = .35.sp)
                Icon(Icons.Default.AutoAwesome, null, tint = Red, modifier = Modifier.size(13.dp))
            }
            Text(message, color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp, lineHeight = 18.sp, fontWeight = FontWeight.Medium)
        }
    }
}
