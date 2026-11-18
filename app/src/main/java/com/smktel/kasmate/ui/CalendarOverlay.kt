package com.smktel.kasmate.ui

import android.content.SharedPreferences
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.EventNote
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.StickyNote2
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class CalendarStickyNote(
    val id: String,
    val dateKey: String,
    val text: String,
    val color: Int,
    val done: Boolean = false
)

private val stickyColors = listOf(
    Color(0xFFFFE0E0), Color(0xFFFFF0B8), Color(0xFFDDF5E5),
    Color(0xFFDCEBFF), Color(0xFFEDE1FF)
)

fun readCalendarNotes(prefs: SharedPreferences): List<CalendarStickyNote> = runCatching {
    val saved = JSONArray(prefs.getString("calendar_notes", "[]") ?: "[]")
    List(saved.length()) { i ->
        val item = saved.getJSONObject(i)
        CalendarStickyNote(
            id = item.getString("id"), dateKey = item.getString("date"),
            text = item.getString("text"), color = item.getInt("color"),
            done = item.optBoolean("done", false)
        )
    }
}.getOrDefault(emptyList())

fun writeCalendarNotes(prefs: SharedPreferences, notes: List<CalendarStickyNote>) {
    val saved = JSONArray()
    notes.forEach { note ->
        saved.put(JSONObject().put("id", note.id).put("date", note.dateKey)
            .put("text", note.text).put("color", note.color).put("done", note.done))
    }
    prefs.edit().putString("calendar_notes", saved.toString()).apply()
}

@Composable
fun CalendarOverlay(
    english: Boolean,
    notes: List<CalendarStickyNote>,
    onClose: () -> Unit,
    onAdd: (CalendarStickyNote) -> Unit,
    onToggle: (CalendarStickyNote) -> Unit,
    onDelete: (CalendarStickyNote) -> Unit
) {
    var month by remember { mutableStateOf(Calendar.getInstance().apply { set(Calendar.DAY_OF_MONTH, 1) }) }
    var selectedDay by remember { mutableStateOf<Long?>(null) }

    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        BoxWithConstraints(Modifier.fillMaxWidth().fillMaxHeight(.94f).padding(12.dp)) {
            val narrow = maxWidth < 600.dp
            val day = selectedDay
            Surface(Modifier.fillMaxSize(), shape = RoundedCornerShape(30.dp), color = MaterialTheme.colorScheme.surface,
                shadowElevation = 20.dp) {
                Box(Modifier.fillMaxSize()) {
                    Row(Modifier.fillMaxSize()) {
                        CalendarMonthPane(
                            modifier = Modifier.weight(if (!narrow && day != null) .58f else 1f),
                            month = month, selectedDay = day, notes = notes, english = english,
                            onClose = onClose,
                            onPrevious = { month = (month.clone() as Calendar).apply { add(Calendar.MONTH, -1) } },
                            onNext = { month = (month.clone() as Calendar).apply { add(Calendar.MONTH, 1) } },
                            onSelectDay = { selectedDay = it }
                        )
                        if (!narrow && day != null) {
                            CalendarDaySidebar(Modifier.weight(.42f).fillMaxHeight(), day, notes, english, onAdd, onToggle, onDelete) {
                                selectedDay = null
                            }
                        }
                    }
                    if (narrow) {
                        AnimatedVisibility(
                            visible = day != null,
                            modifier = Modifier.align(Alignment.CenterEnd),
                            enter = slideInHorizontally(initialOffsetX = { it }) + fadeIn(),
                            exit = slideOutHorizontally(targetOffsetX = { it }) + fadeOut()
                        ) {
                            day?.let { selected ->
                                CalendarDaySidebar(
                                    Modifier.fillMaxHeight().fillMaxWidth(.9f).shadow(18.dp, RoundedCornerShape(topStart = 26.dp, bottomStart = 26.dp)),
                                    selected, notes, english, onAdd, onToggle, onDelete
                                ) { selectedDay = null }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CalendarMonthPane(
    modifier: Modifier,
    month: Calendar,
    selectedDay: Long?,
    notes: List<CalendarStickyNote>,
    english: Boolean,
    onClose: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onSelectDay: (Long) -> Unit
) {
    val locale = if (english) Locale.US else Locale("id", "ID")
    val monthLabel = remember(month.timeInMillis, locale) { SimpleDateFormat("MMMM yyyy", locale).format(month.time) }
    val days = remember(month.timeInMillis, locale) {
        val start = month.clone() as Calendar
        val offset = (start.get(Calendar.DAY_OF_WEEK) - Calendar.MONDAY + 7) % 7
        val count = start.getActualMaximum(Calendar.DAY_OF_MONTH)
        List(offset + count) { index -> if (index < offset) null else index - offset + 1 }
    }
    Column(modifier.fillMaxHeight().padding(horizontal = 18.dp, vertical = 18.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(42.dp).clip(RoundedCornerShape(14.dp)).background(Red.copy(alpha = .1f)), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.CalendarMonth, null, tint = Red)
            }
            Spacer(Modifier.width(11.dp))
            Column(Modifier.weight(1f)) {
                Text(if (english) "My calendar" else "Kalenderku", fontWeight = FontWeight.ExtraBold, fontSize = 20.sp)
                Text(if (english) "Plans, reminders, and little wins" else "Catatan, pengingat, dan target kecil", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
            }
            IconButton(onClick = onClose) { Icon(Icons.Default.Close, if (english) "Close calendar" else "Tutup kalender") }
        }
        Spacer(Modifier.height(20.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Text(monthLabel.replaceFirstChar { it.titlecase(locale) }, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Row {
                IconButton(onClick = onPrevious) { Icon(Icons.Default.KeyboardArrowLeft, if (english) "Previous month" else "Bulan sebelumnya") }
                IconButton(onClick = onNext) { Icon(Icons.Default.KeyboardArrowRight, if (english) "Next month" else "Bulan berikutnya") }
            }
        }
        val weekdays = if (english) listOf("M", "T", "W", "T", "F", "S", "S") else listOf("S", "S", "R", "K", "J", "S", "M")
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            weekdays.forEach { Text(it, Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold, fontSize = 11.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center) }
        }
        Spacer(Modifier.height(8.dp))
        days.chunked(7).forEach { week ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                week.forEach { date ->
                    if (date == null) Spacer(Modifier.weight(1f).aspectRatio(1f)) else {
                        val calendar = month.clone() as Calendar
                        calendar.set(Calendar.DAY_OF_MONTH, date)
                        calendar.set(Calendar.HOUR_OF_DAY, 0); calendar.set(Calendar.MINUTE, 0); calendar.set(Calendar.SECOND, 0); calendar.set(Calendar.MILLISECOND, 0)
                        val millis = calendar.timeInMillis
                        val key = dateKey(millis)
                        val noteForDay = notes.filter { it.dateKey == key }
                        val selected = selectedDay?.let { dateKey(it) == key } == true
                        val today = dateKey(System.currentTimeMillis()) == key
                        Box(Modifier.weight(1f).aspectRatio(1f).padding(2.dp)
                            .clip(RoundedCornerShape(15.dp))
                            .background(if (selected) Red else if (noteForDay.isNotEmpty()) Color(noteForDay.first().color).copy(alpha = .9f) else Color.Transparent)
                            .then(if (today && !selected) Modifier.border(1.dp, Red.copy(alpha = .55f), RoundedCornerShape(15.dp)) else Modifier)
                            .clickable { onSelectDay(millis) }, contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(date.toString(), color = if (selected) Color.White else MaterialTheme.colorScheme.onSurface,
                                    fontWeight = if (today || selected || noteForDay.isNotEmpty()) FontWeight.Bold else FontWeight.Normal, fontSize = 13.sp)
                                if (noteForDay.isNotEmpty()) Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                    noteForDay.take(3).forEach { Box(Modifier.size(4.dp).clip(CircleShape).background(if (selected) Color.White else Color(it.color))) }
                                } else Spacer(Modifier.height(4.dp))
                            }
                        }
                    }
                }
                repeat((7 - week.size).coerceAtLeast(0)) { Spacer(Modifier.weight(1f)) }
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Red.copy(alpha = .07f)).padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.StickyNote2, null, tint = Red, modifier = Modifier.size(19.dp))
            Spacer(Modifier.width(9.dp))
            Text(if (english) "Tap a date to open its notes" else "Ketuk tanggal untuk melihat detail dan catatan", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun CalendarDaySidebar(
    modifier: Modifier,
    dayMillis: Long,
    notes: List<CalendarStickyNote>,
    english: Boolean,
    onAdd: (CalendarStickyNote) -> Unit,
    onToggle: (CalendarStickyNote) -> Unit,
    onDelete: (CalendarStickyNote) -> Unit,
    onBack: () -> Unit
) {
    var noteText by remember(dayMillis) { mutableStateOf("") }
    var colorIndex by remember(dayMillis) { mutableIntStateOf(0) }
    val key = dateKey(dayMillis)
    val dayNotes = notes.filter { it.dateKey == key }
    val locale = if (english) Locale.US else Locale("id", "ID")
    val title = remember(dayMillis, locale) { SimpleDateFormat("EEEE, d MMMM yyyy", locale).format(Date(dayMillis)) }
    Column(modifier.fillMaxHeight().background(MaterialTheme.colorScheme.surface).verticalScroll(rememberScrollState()).padding(18.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(if (english) Icons.Default.ArrowBack else Icons.Default.ArrowBack, if (english) "Back to calendar" else "Kembali ke kalender") }
            Text(if (english) "Day details" else "Detail hari", fontWeight = FontWeight.Bold, fontSize = 17.sp)
        }
        Text(title.replaceFirstChar { it.titlecase(locale) }, fontWeight = FontWeight.ExtraBold, fontSize = 21.sp, lineHeight = 27.sp)
        Text(if (english) "${dayNotes.size} notes · mark tasks when done" else "${dayNotes.size} catatan · centang tugas yang selesai", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
        Spacer(Modifier.height(14.dp))
        if (dayNotes.isEmpty()) {
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(MaterialTheme.colorScheme.surface).padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.EventNote, null, tint = Red)
                Spacer(Modifier.width(9.dp))
                Text(if (english) "Nothing planned yet. Add a sticky note below." else "Belum ada agenda. Tambahkan sticky note di bawah.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        dayNotes.forEach { note ->
            Row(Modifier.fillMaxWidth().padding(vertical = 4.dp).clip(RoundedCornerShape(16.dp)).background(Color(note.color)).padding(start = 11.dp, end = 5.dp, top = 8.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.width(4.dp).height(36.dp).clip(CircleShape).background(Color(note.color)))
                Spacer(Modifier.width(9.dp))
                Text(note.text, Modifier.weight(1f), maxLines = 4, overflow = TextOverflow.Ellipsis,
                    color = if (note.done) Color(0xFF5E5962) else Color(0xFF252128),
                    fontSize = 13.sp, fontWeight = FontWeight.Medium)
                IconButton(onClick = { onToggle(note) }) { Icon(if (note.done) Icons.Default.Check else Icons.Default.Check, null, tint = if (note.done) Color(0xFF168A62) else MaterialTheme.colorScheme.onSurfaceVariant) }
                IconButton(onClick = { onDelete(note) }) { Icon(Icons.Default.DeleteOutline, null, tint = Red) }
            }
        }
        Spacer(Modifier.height(12.dp))
        Text(if (english) "Add a sticky note" else "Tambah sticky note", fontWeight = FontWeight.Bold, fontSize = 15.sp)
        OutlinedTextField(value = noteText, onValueChange = { noteText = it }, modifier = Modifier.fillMaxWidth(),
            placeholder = { Text(if (english) "Reminder, bill, or class plan…" else "Pengingat, kas, atau agenda kelas…", fontSize = 12.sp) },
            minLines = 2, maxLines = 4, shape = RoundedCornerShape(17.dp))
        Text(if (english) "Choose a color" else "Pilih warna", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(11.dp), verticalAlignment = Alignment.CenterVertically) {
            stickyColors.forEachIndexed { index, color ->
                Box(Modifier.size(31.dp).clip(CircleShape).background(color)
                    .then(if (colorIndex == index) Modifier.border(2.dp, Red, CircleShape) else Modifier)
                    .clickable { colorIndex = index }, contentAlignment = Alignment.Center) {
                    if (colorIndex == index) Icon(Icons.Default.Check, null, tint = Color(0xFF282828), modifier = Modifier.size(16.dp))
                }
            }
        }
        Button(onClick = {
            if (noteText.isNotBlank()) {
                onAdd(CalendarStickyNote("note-${System.currentTimeMillis()}", key, noteText.trim(), stickyColors[colorIndex].value.toLong().toInt()))
                noteText = ""
            }
        }, enabled = noteText.isNotBlank(), modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Red)) {
            Icon(Icons.Default.Add, null); Spacer(Modifier.width(6.dp)); Text(if (english) "Save note" else "Simpan catatan", fontWeight = FontWeight.Bold)
        }
    }
}

private fun dateKey(millis: Long): String = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(millis))
