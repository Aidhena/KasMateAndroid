package com.smktel.kasmate.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import com.smktel.kasmate.R

enum class MascotMood { NEUTRAL, SAD, ANGRY, HAPPY, EXCITED }

@Composable
fun KasMateMascotBanner(
    mood: MascotMood,
    message: String,
    modifier: Modifier = Modifier,
    assistantLabel: String = "KasMate AI"
) {
    val resource = when (mood) {
        MascotMood.SAD -> R.drawable.kasmate_mascot_sad
        MascotMood.ANGRY -> R.drawable.kasmate_mascot_angry
        MascotMood.HAPPY -> R.drawable.kasmate_mascot_happy
        MascotMood.EXCITED -> R.drawable.kasmate_mascot_excited
        MascotMood.NEUTRAL -> R.drawable.kasmate_mascot_neutral
    }
    var popped by remember(mood) { mutableStateOf(false) }
    LaunchedEffect(mood) { popped = false; delay(45); popped = true }
    val mascotScale by animateFloatAsState(if (popped) 1f else .72f, animationSpec = spring(dampingRatio = .48f, stiffness = 420f), label = "mascot pop")
    val idle = rememberInfiniteTransition(label = "mascot idle")
    val bobAmount = when (mood) { MascotMood.EXCITED -> 7f; MascotMood.HAPPY -> 4f; MascotMood.SAD -> 1.5f; MascotMood.ANGRY -> 2f; MascotMood.NEUTRAL -> 2.5f }
    val tiltAmount = when (mood) { MascotMood.EXCITED -> 2.5f; MascotMood.HAPPY -> 1.2f; MascotMood.SAD -> .25f; MascotMood.ANGRY -> .65f; MascotMood.NEUTRAL -> .7f }
    val bob by idle.animateFloat(0f, bobAmount, infiniteRepeatable(tween(if (mood == MascotMood.EXCITED) 270 else 950), RepeatMode.Reverse), label = "mascot bob")
    val tilt by idle.animateFloat(-tiltAmount, tiltAmount, infiniteRepeatable(tween(if (mood == MascotMood.EXCITED) 340 else 1250), RepeatMode.Reverse), label = "mascot tilt")
    Row(
        modifier.fillMaxWidth().animateContentSize()
            .clip(RoundedCornerShape(26.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = .72f), RoundedCornerShape(26.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(13.dp)
    ) {
        Box(Modifier.size(78.dp).clip(CircleShape).background(Red.copy(alpha = .08f)), contentAlignment = Alignment.Center) {
            AnimatedContent(targetState = resource, label = "mascot expression") { image ->
                Image(painterResource(image), contentDescription = assistantLabel, modifier = Modifier.fillMaxSize().scale(mascotScale).graphicsLayer { translationY = bob; rotationZ = tilt }.padding(2.dp), contentScale = ContentScale.Fit)
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
