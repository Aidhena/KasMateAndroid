package com.smktel.kasmate.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.style.TextOverflow
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
    val motionEnabled = LocalMotionEnabled.current
    var popped by remember(mood) { mutableStateOf(false) }
    LaunchedEffect(mood, motionEnabled) {
        popped = false
        if (motionEnabled) delay(45)
        popped = true
    }
    val mascotScale by animateFloatAsState(
        if (!motionEnabled || popped) 1f else .72f,
        animationSpec = tween(if (motionEnabled) 240 else 0),
        label = "mascot pop"
    )
    val (bob, tilt, shake) = if (motionEnabled) {
        val idle = rememberInfiniteTransition(label = "mascot idle")
        val bobAmount = when (mood) { MascotMood.EXCITED -> 6f; MascotMood.HAPPY -> 3.5f; MascotMood.SAD -> 1f; MascotMood.ANGRY -> 1.5f; MascotMood.NEUTRAL -> 2f }
        val tiltAmount = when (mood) { MascotMood.EXCITED -> 2f; MascotMood.HAPPY -> 1f; MascotMood.SAD -> .2f; MascotMood.ANGRY -> .5f; MascotMood.NEUTRAL -> .6f }
        val bobValue by idle.animateFloat(0f, bobAmount, infiniteRepeatable(tween(if (mood == MascotMood.EXCITED) 340 else 1100), RepeatMode.Reverse), label = "mascot bob")
        val tiltValue by idle.animateFloat(-tiltAmount, tiltAmount, infiniteRepeatable(tween(if (mood == MascotMood.EXCITED) 420 else 1500), RepeatMode.Reverse), label = "mascot tilt")
        val shakeValue by idle.animateFloat(-2f, 2f, infiniteRepeatable(tween(125, delayMillis = 700), RepeatMode.Reverse), label = "mascot shake")
        Triple(bobValue, tiltValue, if (mood == MascotMood.ANGRY) shakeValue else 0f)
    } else Triple(0f, 0f, 0f)
    Row(
        modifier.fillMaxWidth().animateContentSize(animationSpec = tween(if (motionEnabled) 220 else 0))
            .clip(RoundedCornerShape(26.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = .72f), RoundedCornerShape(26.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(13.dp)
    ) {
        Box(Modifier.size(78.dp).clip(CircleShape).background(Red.copy(alpha = .08f)), contentAlignment = Alignment.Center) {
            AnimatedContent(
                targetState = resource,
                transitionSpec = {
                    if (motionEnabled) fadeIn(tween(180)) togetherWith fadeOut(tween(130))
                    else fadeIn(tween(0)) togetherWith fadeOut(tween(0))
                },
                label = "mascot expression"
            ) { image ->
                Image(painterResource(image), contentDescription = assistantLabel, modifier = Modifier.fillMaxSize().scale(mascotScale).graphicsLayer { translationY = bob; translationX = shake; rotationZ = tilt }.padding(2.dp), contentScale = ContentScale.Fit)
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

@Composable
fun FloatingPaymentMascot(
    mood: MascotMood,
    phrases: List<String>,
    english: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val motionEnabled = LocalMotionEnabled.current
    val isLight = MaterialTheme.colorScheme.background == AppBackground
    val context = LocalContext.current
    val density = LocalDensity.current
    val mascotPrefs = remember(context) {
        context.getSharedPreferences("kasmate_preferences", android.content.Context.MODE_PRIVATE)
    }
    var xRatio by remember(mascotPrefs) {
        mutableFloatStateOf(mascotPrefs.getFloat("payment_mascot_x_ratio", 1f))
    }
    var yRatio by remember(mascotPrefs) {
        mutableFloatStateOf(mascotPrefs.getFloat("payment_mascot_y_ratio", -1f))
    }
    var hasMoved by remember(mascotPrefs) {
        mutableStateOf(mascotPrefs.contains("payment_mascot_x_ratio"))
    }
    var dragging by remember { mutableStateOf(false) }
    var bubbleWidthPx by remember { mutableIntStateOf(0) }
    var bubbleHeightPx by remember { mutableIntStateOf(0) }
    val imageResource = when (mood) {
        MascotMood.SAD -> R.drawable.kasmate_mascot_sad
        MascotMood.ANGRY -> R.drawable.kasmate_mascot_angry
        MascotMood.HAPPY -> R.drawable.kasmate_mascot_happy
        MascotMood.EXCITED -> R.drawable.kasmate_mascot_excited
        MascotMood.NEUTRAL -> R.drawable.kasmate_mascot_neutral
    }
    val activities = remember(mood) {
        when (mood) {
            MascotMood.ANGRY -> listOf("scolding", "clock_warning")
            MascotMood.SAD -> listOf("worried_note", "clock_warning")
            MascotMood.EXCITED -> listOf("coin_toss", "cheering")
            MascotMood.HAPPY -> listOf("saving_coin", "checklist", "waving")
            MascotMood.NEUTRAL -> listOf("writing_notes", "calculating", "coin_bounce", "waving")
        }
    }
    var activityIndex by remember(mood) { mutableIntStateOf(0) }
    LaunchedEffect(mood, activities, motionEnabled) {
        activityIndex = 0
        if (motionEnabled && activities.size > 1) {
            while (true) {
                delay(4_800)
                activityIndex = (activityIndex + 1) % activities.size
            }
        }
    }
    val activity = activities[activityIndex % activities.size]
    val displayPhrases = if (hasMoved) phrases else listOf(
        if (english) "Drag me anywhere!" else "Geser aku ke mana saja!"
    ) + phrases
    val phrasesKey = displayPhrases.joinToString("|")
    var typedText by remember(phrasesKey) { mutableStateOf(displayPhrases.firstOrNull().orEmpty()) }
    LaunchedEffect(phrasesKey, motionEnabled) {
        if (displayPhrases.isEmpty()) return@LaunchedEffect
        if (!motionEnabled) {
            typedText = displayPhrases.first()
            return@LaunchedEffect
        }
        var phraseIndex = 0
        while (true) {
            val phrase = displayPhrases[phraseIndex % displayPhrases.size]
            typedText = ""
            phrase.forEachIndexed { characterIndex, _ ->
                delay(48)
                typedText = phrase.take(characterIndex + 1)
            }
            delay(1_650)
            for (characterCount in phrase.length downTo 1) {
                delay(25)
                typedText = phrase.take(characterCount - 1)
            }
            delay(220)
            phraseIndex = (phraseIndex + 1) % displayPhrases.size
        }
    }
    val floatTransition = rememberInfiniteTransition(label = "floating payment mascot")
    val bobAmount = when {
        activity == "coin_toss" || activity == "cheering" -> 8f
        activity == "scolding" -> 1.5f
        mood == MascotMood.HAPPY -> 4f
        else -> 3.2f
    }
    val bob by floatTransition.animateFloat(
        initialValue = 0f,
        targetValue = if (motionEnabled) bobAmount else 0f,
        animationSpec = infiniteRepeatable(tween(if (activity == "coin_toss" || activity == "cheering") 480 else 1450), RepeatMode.Reverse),
        label = "floating mascot bob"
    )
    val tilt by floatTransition.animateFloat(
        initialValue = -1.2f,
        targetValue = if (!motionEnabled) 0f else when (activity) {
            "waving" -> 4f
            "calculating" -> 2f
            "coin_toss", "cheering" -> 3f
            "scolding" -> 1.8f
            else -> 1.2f
        },
        animationSpec = infiniteRepeatable(tween(if (activity == "scolding") 120 else 900), RepeatMode.Reverse),
        label = "floating mascot action"
    )
    val scale by animateFloatAsState(
        targetValue = if (dragging) 1.08f else if (activity == "coin_toss" || activity == "cheering") 1.04f else 1f,
        animationSpec = tween(if (motionEnabled) 180 else 0),
        label = "floating mascot scale"
    )
    val coinArc by floatTransition.animateFloat(
        initialValue = -2f,
        targetValue = if (motionEnabled && activity in setOf("coin_toss", "coin_bounce", "saving_coin")) -23f else -2f,
        animationSpec = infiniteRepeatable(tween(620), RepeatMode.Reverse),
        label = "mascot coin toss"
    )
    val bubbleColor = when (mood) {
        MascotMood.ANGRY, MascotMood.SAD -> if (isLight) Color(0xFFFFF5F5) else Color(0xFF2A1518)
        MascotMood.EXCITED, MascotMood.HAPPY -> if (isLight) Color(0xFFF2FBF6) else Color(0xFF14241C)
        MascotMood.NEUTRAL -> MaterialTheme.colorScheme.surface
    }
    val bubbleBorder = when (mood) {
        MascotMood.ANGRY, MascotMood.SAD -> Red.copy(alpha = .4f)
        MascotMood.EXCITED, MascotMood.HAPPY -> Color(0xFF168A62).copy(alpha = .35f)
        MascotMood.NEUTRAL -> MaterialTheme.colorScheme.outline
    }
    val bubbleText = when (mood) {
        MascotMood.ANGRY, MascotMood.SAD -> if (isLight) DeepRed else Color(0xFFFF8A80)
        MascotMood.EXCITED, MascotMood.HAPPY -> Color(0xFF168A62)
        MascotMood.NEUTRAL -> MaterialTheme.colorScheme.onSurface
    }
    BoxWithConstraints(modifier.fillMaxSize()) {
        val widgetSize = 74.dp
        val margin = 12.dp
        val gap = 8.dp
        val maxX = (maxWidth - widgetSize - margin).coerceAtLeast(margin)
        val maxY = (maxHeight - widgetSize - margin).coerceAtLeast(margin)
        val xSpan = (maxX - margin).coerceAtLeast(1.dp)
        val ySpan = (maxY - margin).coerceAtLeast(1.dp)
        val dragWidth = maxWidth
        val dragHeight = maxHeight
        val x = margin + xSpan * xRatio.coerceIn(0f, 1f)
        val y = if (yRatio < 0f) 86.dp.coerceIn(margin, maxY) else margin + ySpan * yRatio.coerceIn(0f, 1f)
        val leftRoom = x - margin - gap
        val rightRoom = maxWidth - (x + widgetSize + gap + margin)
        val bubbleOnLeft = leftRoom >= rightRoom
        val availableBubbleRoom = (if (bubbleOnLeft) leftRoom else rightRoom).coerceAtLeast(72.dp)
        val maxBubbleWidth = availableBubbleRoom.coerceAtMost(175.dp)
        val bubbleAbove = maxHeight - y - widgetSize - margin < y - margin
        val currentX = rememberUpdatedState(x)
        val currentY = rememberUpdatedState(y)
        val currentXRatio = rememberUpdatedState(xRatio)
        val currentYRatio = rememberUpdatedState(yRatio)

        AnimatedVisibility(
            visible = true,
            enter = if (motionEnabled) fadeIn(tween(260)) + slideInVertically(tween(300)) { it / 2 } else fadeIn(tween(0))
        ) {
            Box(
                Modifier.offset(x = x, y = y).size(widgetSize),
                contentAlignment = Alignment.Center
            ) {
                androidx.compose.material3.Surface(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .onSizeChanged {
                            bubbleWidthPx = it.width
                            bubbleHeightPx = it.height
                        }
                        .offset {
                            val bubbleGapPx = with(density) { gap.roundToPx() }
                            val widgetPx = with(density) { widgetSize.roundToPx() }
                            IntOffset(
                                x = if (bubbleOnLeft) -bubbleWidthPx - bubbleGapPx else widgetPx + bubbleGapPx,
                                y = if (bubbleAbove) -bubbleHeightPx - bubbleGapPx else widgetPx + bubbleGapPx
                            )
                        }
                        .widthIn(max = maxBubbleWidth)
                        .clickable(onClick = onClick),
                    shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomEnd = if (bubbleOnLeft) 5.dp else 16.dp, bottomStart = if (bubbleOnLeft) 16.dp else 5.dp),
                    color = bubbleColor,
                    contentColor = bubbleText,
                    border = BorderStroke(1.dp, bubbleBorder),
                    shadowElevation = 5.dp
                ) {
                    Text(
                        text = typedText.ifBlank { " " },
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        maxLines = 5,
                        overflow = TextOverflow.Ellipsis,
                        fontSize = 11.sp,
                        lineHeight = 14.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
                Box(
                    Modifier.fillMaxSize()
                        .clickable(onClick = onClick)
                        .pointerInput(dragWidth, dragHeight, density.density) {
                            detectDragGestures(
                                onDragStart = { dragging = true },
                                onDragEnd = {
                                    dragging = false
                                    hasMoved = true
                                    mascotPrefs.edit()
                                        .putFloat("payment_mascot_x_ratio", currentXRatio.value)
                                        .putFloat("payment_mascot_y_ratio", currentYRatio.value)
                                        .apply()
                                },
                                onDragCancel = { dragging = false },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    val dx = (dragAmount.x / density.density).dp
                                    val dy = (dragAmount.y / density.density).dp
                                    val nextX = (currentX.value + dx).coerceIn(margin, maxX)
                                    val nextY = (currentY.value + dy).coerceIn(margin, maxY)
                                    xRatio = ((nextX - margin) / xSpan).coerceIn(0f, 1f)
                                    yRatio = ((nextY - margin) / ySpan).coerceIn(0f, 1f)
                                }
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(imageResource),
                        contentDescription = if (english) "KasMate payment reminder. Tap to open payments, drag to move." else "Pengingat pembayaran KasMate. Ketuk untuk buka pembayaran, geser untuk memindahkan.",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize().graphicsLayer {
                            translationY = bob
                            rotationZ = if (activity == "scolding") tilt * 1.35f else tilt
                            scaleX = scale
                            scaleY = scale
                        }
                    )
                    if (activity == "coin_toss" || activity == "coin_bounce" || activity == "saving_coin") {
                        Canvas(Modifier.size(24.dp).align(Alignment.TopEnd).offset(y = coinArc.dp)) {
                            val center = Offset(size.width / 2f, size.height / 2f)
                            drawCircle(Color(0xFFFFD166), radius = size.minDimension * .42f, center = center)
                            drawCircle(Color(0xFFB77900), radius = size.minDimension * .3f, center = center, style = androidx.compose.ui.graphics.drawscope.Stroke(width = size.minDimension * .09f))
                        }
                    }
                    if (activity == "cheering" || activity == "waving") {
                        Icon(
                            Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = Color(0xFFFFC84A),
                            modifier = Modifier.size(18.dp).align(Alignment.TopStart).offset(x = 2.dp, y = 4.dp).graphicsLayer {
                                scaleX = scale
                                scaleY = scale
                                rotationZ = tilt * 3f
                            }
                        )
                    }
                }
            }
        }
    }
}
