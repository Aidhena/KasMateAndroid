package com.smktel.kasmate.ui

import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.smktel.kasmate.data.*
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.delay

enum class AppPage { HOME, PAYMENTS, LEDGER, ASSISTANT, SETTINGS, PROFILE }
enum class AppLanguage { INDONESIAN, ENGLISH }

fun rupiah(n: Long): String = "Rp " + NumberFormat.getInstance(Locale("id", "ID")).format(n)
fun tanggal(ms: Long): String = SimpleDateFormat("d MMM yyyy", Locale("id", "ID")).format(Date(ms))

@Composable
fun HomeScreen(
    userName: String,
    darkMode: Boolean,
    onDarkModeChange: (Boolean) -> Unit,
    language: AppLanguage,
    onLanguageChange: (AppLanguage) -> Unit,
    vm: KasViewModel = viewModel()
) {
    val s by vm.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val calendarPrefs = remember(context) { context.getSharedPreferences("kasmate_preferences", android.content.Context.MODE_PRIVATE) }
    val calendarNotes = remember(calendarPrefs) { mutableStateListOf<CalendarStickyNote>().apply { addAll(readCalendarNotes(calendarPrefs)) } }
    var page by remember { mutableStateOf(AppPage.HOME) }
    var paymentRequests by remember { mutableStateOf(emptyList<DemoPaymentRequest>()) }
    var disputeTickets by remember { mutableStateOf(emptyList<DemoDispute>()) }
    var calendarOpen by remember { mutableStateOf(false) }
    var paymentCelebration by remember { mutableStateOf(false) }
    val english = language == AppLanguage.ENGLISH
    LaunchedEffect(page, paymentCelebration) {
        if (page == AppPage.HOME && paymentCelebration) {
            delay(8_000)
            paymentCelebration = false
        }
    }
    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { calendarOpen = true }, containerColor = Red, contentColor = Color.White,
                shape = RoundedCornerShape(19.dp)) {
                Icon(Icons.Default.CalendarMonth, if (english) "Open calendar" else "Buka kalender")
            }
        },
        bottomBar = {
            BoxWithConstraints(Modifier.fillMaxWidth()) {
                val compact = maxWidth < 392.dp
                val labelSize = if (compact) 10.sp else 11.sp
                NavigationBar(containerColor = MaterialTheme.colorScheme.surface, tonalElevation = 0.dp) {
                    NavigationBarItem(page == AppPage.HOME, { page = AppPage.HOME }, icon = { Icon(Icons.Default.Home, if (english) "Home" else "Beranda") }, label = { NavLabel(if (english) "Home" else "Beranda", labelSize) }, colors = navColors())
                    NavigationBarItem(page == AppPage.PAYMENTS, { page = AppPage.PAYMENTS }, icon = { Icon(Icons.Default.Payments, if (english) "Pay" else "Bayar") }, label = { NavLabel(if (english) "Pay" else "Bayar", labelSize) }, colors = navColors())
                    NavigationBarItem(page == AppPage.LEDGER, { page = AppPage.LEDGER }, icon = { Icon(Icons.Default.AccountBalance, if (english) "Class ledger" else "Kas kelas") }, label = { NavLabel(if (compact) { if (english) "Class" else "Kas" } else { if (english) "Class" else "Kas kelas" }, labelSize) }, colors = navColors())
                    NavigationBarItem(page == AppPage.ASSISTANT, { page = AppPage.ASSISTANT }, icon = { Icon(Icons.Default.AutoAwesome, if (english) "Assistant" else "Asisten") }, label = { NavLabel(if (compact) "AI" else if (english) "Assistant" else "Asisten", labelSize) }, colors = navColors())
                    NavigationBarItem(page == AppPage.SETTINGS, { page = AppPage.SETTINGS }, icon = { Icon(Icons.Default.Settings, if (english) "Settings" else "Pengaturan") }, label = { NavLabel(if (compact) { if (english) "Settings" else "Setelan" } else { if (english) "Settings" else "Pengaturan" }, labelSize) }, colors = navColors())
                }
            }
        }
    ) { padding ->
        Crossfade(targetState = page, modifier = Modifier.padding(padding), label = "page transition") { currentPage ->
            when (currentPage) {
                AppPage.HOME -> Dashboard(s, english, userName, paymentCelebration, { page = AppPage.SETTINGS }, { page = AppPage.PAYMENTS }, { page = AppPage.ASSISTANT })
                AppPage.PAYMENTS -> StudentPaymentScreen(s, english, paymentRequests, disputeTickets,
                    onRequest = { paymentRequests = paymentRequests + it; paymentCelebration = true },
                    onDispute = { disputeTickets = disputeTickets + it })
                AppPage.LEDGER -> StudentLedgerScreen(s, english)
                AppPage.ASSISTANT -> KasMateAssistantScreen(s, userName, english)
                AppPage.SETTINGS -> SettingsPage(darkMode, onDarkModeChange, language, onLanguageChange, english) { page = AppPage.PROFILE }
                AppPage.PROFILE -> ProfilePage(english, userName)
            }
        }
    }
    if (calendarOpen) {
        CalendarOverlay(
            english = english,
            notes = calendarNotes,
            onClose = { calendarOpen = false },
            onAdd = { note -> calendarNotes.add(note); writeCalendarNotes(calendarPrefs, calendarNotes) },
            onToggle = { note ->
                val index = calendarNotes.indexOfFirst { it.id == note.id }
                if (index >= 0) calendarNotes[index] = note.copy(done = !note.done)
                writeCalendarNotes(calendarPrefs, calendarNotes)
            },
            onDelete = { note -> calendarNotes.removeAll { it.id == note.id }; writeCalendarNotes(calendarPrefs, calendarNotes) }
        )
    }
}

@Composable
private fun NavLabel(text: String, size: androidx.compose.ui.unit.TextUnit) {
    Text(
        text = text,
        fontSize = size,
        maxLines = 1,
        softWrap = false,
        overflow = TextOverflow.Ellipsis
    )
}

@Composable
private fun navColors() = NavigationBarItemDefaults.colors(
    selectedIconColor = Red,
    selectedTextColor = Red,
    indicatorColor = Red.copy(alpha = .1f),
    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
)

@Composable
private fun Dashboard(s: KasSummary, english: Boolean, userName: String, paymentSubmitted: Boolean, openSettings: () -> Unit, openPayments: () -> Unit, openAssistant: () -> Unit) {
    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Row(Modifier.fillMaxWidth().padding(top = 22.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("KASMATE", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, letterSpacing = 2.sp, fontSize = 12.sp)
                    Text(greeting(userName, english), fontSize = 25.sp, fontWeight = FontWeight.ExtraBold)
                }
                IconButton(onClick = openSettings) { Icon(Icons.Default.Tune, if (english) "Settings" else "Pengaturan") }
            }
        }
        item { StudentStatusCard(s, english, openPayments) }
        item {
            val unpaid = s.myBills.firstOrNull { it.status == PaymentStatus.BELUM }
            val today = Calendar.getInstance().apply { set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0) }.timeInMillis
            val mood = when {
                paymentSubmitted -> MascotMood.EXCITED
                unpaid != null && unpaid.dueDateMillis < today -> MascotMood.SAD
                unpaid != null -> MascotMood.NEUTRAL
                s.myBills.any { it.status == PaymentStatus.LUNAS } -> MascotMood.HAPPY
                else -> MascotMood.NEUTRAL
            }
            val message = when (mood) {
                MascotMood.SAD -> if (english) "Looks like a bill is overdue. Let's settle it together." else "Yah, ada kas yang terlewat. Yuk, bereskan bareng-bareng!"
                MascotMood.NEUTRAL -> if (english) "A little reminder: pay class dues before the due date." else "Ingat bayar uang kas sebelum tanggal jatuh tempo, ya!"
                MascotMood.HAPPY -> if (english) "Wonderful, your dues are paid! ${financeFunFact(true)}" else "Yeay, kasmu lunas! ${financeFunFact(false)}"
                MascotMood.EXCITED -> if (english) "Yay! Your payment proof is sent. I'll cheer while it gets reviewed!" else "Yeay! Bukti bayarmu terkirim. Aku ikut senang sambil menunggu konfirmasi!"
            }
            KasMateMascotBanner(mood, message)
        }
        item { BalanceCard(s.totalBalance, english) }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MiniStat(Modifier.weight(1f), "↗", if (english) "Income" else "Pemasukan", rupiah(185_000), Color(0xFF168A62))
                MiniStat(Modifier.weight(1f), "↘", if (english) "Expenses" else "Pengeluaran", rupiah(42_000), MaterialTheme.colorScheme.primary)
            }
        }
        item { ChartCard(english) }
        item { ClassUpdateCard(english, openAssistant) }
        item { ClassGoalPreview(english) }
        item {
            OutlinedButton(onClick = openAssistant, modifier = Modifier.fillMaxWidth(), shape = BigShape) {
                Icon(Icons.Default.AutoAwesome, null, tint = Red); Spacer(Modifier.width(8.dp)); Text(if (english) "Ask KasMate Assistant" else "Tanya Asisten KasMate", color = Red)
            }
        }
        item { SectionTitle(if (english) "My bills" else "Tagihan saya", if (english) "See all" else "Lihat semua") }
        items(s.myBills, key = { it.id }) { BillRow(it, english) }
        item { SectionTitle(if (english) "Recent expenses" else "Pengeluaran terbaru", if (english) "History" else "Riwayat") }
        items(s.expenses, key = { it.id }) { ExpenseRow(it) }
        item { Spacer(Modifier.height(12.dp)) }
    }
}

@Composable
private fun greeting(userName: String, english: Boolean): String {
    var hour by remember { mutableIntStateOf(Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(60_000)
            hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        }
    }
    val salutation = when (hour) {
        in 5..10 -> if (english) "Good morning" else "Selamat pagi"
        in 11..17 -> if (english) "Good afternoon" else "Selamat siang"
        else -> if (english) "Good evening" else "Selamat malam"
    }
    val name = userName.trim().ifBlank { if (english) "Student" else "Siswa" }
        .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
    return "$salutation, $name!"
}

@Composable
private fun StudentStatusCard(summary: KasSummary, english: Boolean, onClick: () -> Unit) {
    val unpaid = summary.myBills.firstOrNull { it.status == PaymentStatus.BELUM }
    StudentPanel(Modifier.clickable(onClick = onClick)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(11.dp)) {
            Box(Modifier.size(40.dp).clip(CircleShape).background(if (unpaid == null) Color(0xFF168A62).copy(alpha = .12f) else Red.copy(alpha = .1f)), contentAlignment = Alignment.Center) {
                Icon(if (unpaid == null) Icons.Default.CheckCircle else Icons.Default.NotificationsActive, null, tint = if (unpaid == null) Color(0xFF168A62) else Red)
            }
            Column(Modifier.weight(1f)) {
                Text(if (unpaid == null) if (english) "Kas this month is paid" else "Kas bulan ini lunas" else if (english) "Payment due this month" else "Kas bulan ini belum lunas", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text(unpaid?.let { "${it.title} · ${rupiah(it.amount)}" } ?: if (english) "You're all caught up" else "Kamu tidak memiliki tunggakan", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
            }
            Icon(Icons.Default.ChevronRight, null, tint = Red)
        }
    }
}

@Composable
private fun ClassUpdateCard(english: Boolean, openAssistant: () -> Unit) {
    StudentPanel(Modifier.clickable(onClick = openAssistant)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Campaign, null, tint = Red)
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Text(if (english) "Cash update" else "Kabar kas", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text(if (english) "Weekly contributions are being collected. Ask the assistant about payment details." else "Iuran mingguan sedang dikumpulkan. Tanya asisten untuk info pembayaran.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
            }
            Icon(Icons.Default.ChevronRight, null, tint = Red)
        }
    }
}

@Composable
private fun ClassGoalPreview(english: Boolean) {
    StudentPanel {
        Row(horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Column { Text(if (english) "Class meeting goal" else "Target dana classmeeting", fontWeight = FontWeight.Bold, fontSize = 14.sp); Text(if (english) "68% collected" else "68% terkumpul", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp) }
            Text("68%", color = Red, fontWeight = FontWeight.ExtraBold)
        }
        LinearProgressIndicator(progress = { .68f }, modifier = Modifier.fillMaxWidth().height(7.dp).clip(CircleShape), color = Red, trackColor = Red.copy(alpha = .12f))
    }
}

@Composable
private fun BalanceCard(total: Long, english: Boolean) {
    var shown by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (shown) 1f else .92f, label = "balance scale")
    LaunchedEffect(Unit) { shown = true }
    Column(Modifier.fillMaxWidth().padding(top = 4.dp).clip(BigShape).animateContentSize()
        .background(Brush.linearGradient(listOf(DeepRed, Red, SoftRed)))
        .padding(24.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(if (english) "TOTAL CLASS BALANCE" else "TOTAL SALDO KELAS", color = Color.White.copy(alpha = .82f), fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            Icon(Icons.Default.AccountBalanceWallet, null, tint = Color.White)
        }
        Text(rupiah(total), color = Color.White, fontSize = (32 * scale).sp, fontWeight = FontWeight.ExtraBold)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.TrendingUp, null, tint = Color.White, modifier = Modifier.size(18.dp))
            Text(if (english) "  +8.2% from last month" else "  +8,2% dari bulan lalu", color = Color.White, fontSize = 13.sp)
        }
    }
}

@Composable
private fun MiniStat(modifier: Modifier, icon: String, label: String, value: String, tint: Color) {
    Column(modifier.shadow(2.dp, BigShape).clip(BigShape).background(MaterialTheme.colorScheme.surface)
        .border(1.dp, MaterialTheme.colorScheme.outline, BigShape).padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) { Text(icon, color = tint, fontSize = 18.sp, fontWeight = FontWeight.Bold); Text("  $label", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp) }
        Text(value, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
    }
}

@Composable
private fun ChartCard(english: Boolean) {
    var weekly by remember { mutableStateOf(false) }
    val periodTint by animateColorAsState(if (weekly) Red else Red.copy(alpha = .1f), label = "chart period")
    Column(Modifier.fillMaxWidth().shadow(2.dp, BigShape).clip(BigShape).background(MaterialTheme.colorScheme.surface)
        .border(1.dp, MaterialTheme.colorScheme.outline, BigShape).padding(18.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column { Text(if (english) "Cash flow" else "Arus kas", fontSize = 18.sp, fontWeight = FontWeight.Bold); Text(if (english) "Income vs expenses" else "Pemasukan vs pengeluaran", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            Text(if (weekly) "MINGGU" else "BULAN", color = if (weekly) Color.White else Red, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.clip(CircleShape).clickable { weekly = !weekly }.background(periodTint).padding(horizontal = 12.dp, vertical = 8.dp))
        }
        Spacer(Modifier.height(16.dp))
        Canvas(Modifier.fillMaxWidth().height(118.dp)) {
            val values = if (weekly) listOf(.4f, .58f, .42f, .8f, .61f, .92f) else listOf(.35f, .5f, .43f, .72f, .58f, .87f)
            val step = size.width / (values.size - 1)
            for (i in 0..3) drawLine(Color.Gray.copy(alpha = .18f), androidx.compose.ui.geometry.Offset(0f, size.height * i / 3), androidx.compose.ui.geometry.Offset(size.width, size.height * i / 3), 1.dp.toPx())
            val line = Path().apply { values.forEachIndexed { i, v -> val p = androidx.compose.ui.geometry.Offset(i * step, size.height * (1 - v)); if (i == 0) moveTo(p.x, p.y) else quadraticTo((i - .5f) * step, size.height * (1 - values[i - 1]), p.x, p.y) } }
            val area = Path().apply {
                moveTo(0f, size.height)
                values.forEachIndexed { i, v ->
                    val p = androidx.compose.ui.geometry.Offset(i * step, size.height * (1 - v))
                    if (i == 0) lineTo(p.x, p.y) else quadraticTo((i - .5f) * step, size.height * (1 - values[i - 1]), p.x, p.y)
                }
                lineTo(size.width, size.height)
                close()
            }
            drawPath(area, Brush.verticalGradient(listOf(Red.copy(alpha = .22f), SoftRed.copy(alpha = .02f))))
            drawPath(line, Red, style = Stroke(3.dp.toPx(), cap = StrokeCap.Round))
            values.forEachIndexed { i, v -> drawCircle(Red, 4.dp.toPx(), androidx.compose.ui.geometry.Offset(i * step, size.height * (1 - v))) }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { (if (weekly) listOf("Sen", "Sel", "Rab", "Kam", "Jum", "Sab") else listOf("Jun", "Jul", "Agu", "Sep", "Okt", "Nov")).forEach { Text(it, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) } }
    }
}

@Composable
private fun SectionTitle(title: String, action: String) {
    Row(Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(title, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
        Text(action, color = Red, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun BillRow(b: Bill, english: Boolean) {
    val paid = b.status == PaymentStatus.LUNAS
    Row(Modifier.fillMaxWidth().shadow(1.dp, BigShape).clip(BigShape).background(MaterialTheme.colorScheme.surface)
        .border(1.dp, MaterialTheme.colorScheme.outline, BigShape).padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.size(42.dp).clip(CircleShape).background(Red.copy(alpha = .12f)), contentAlignment = Alignment.Center) { Icon(Icons.Default.Receipt, null, tint = Red) }
            Column { Text(b.title, fontWeight = FontWeight.Bold, fontSize = 15.sp); Text("${if (english) "Due" else "Tenggat"} ${tanggal(b.dueDateMillis)}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
        Column(horizontalAlignment = Alignment.End) { Text(rupiah(b.amount), color = MaterialTheme.colorScheme.tertiary, fontWeight = FontWeight.Bold); Text(if (paid) if (english) "PAID" else "LUNAS" else if (english) "UNPAID" else "BELUM", color = if (paid) Color(0xFF168A62) else Red, fontWeight = FontWeight.ExtraBold, fontSize = 10.sp) }
    }
}

@Composable
private fun ExpenseRow(e: Expense) {
    Row(Modifier.fillMaxWidth().shadow(1.dp, BigShape).clip(BigShape).background(MaterialTheme.colorScheme.surface)
        .border(1.dp, MaterialTheme.colorScheme.outline, BigShape).padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) { Text(e.description, fontWeight = FontWeight.Bold, fontSize = 15.sp); Text("${tanggal(e.dateMillis)} · ${e.noteBy}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        Text("− ${rupiah(e.amount)}", color = MaterialTheme.colorScheme.tertiary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
    }
}

@Composable
private fun SettingsPage(dark: Boolean, onDark: (Boolean) -> Unit, language: AppLanguage, onLanguage: (AppLanguage) -> Unit, english: Boolean, onOpenProfile: () -> Unit) {
    var languageMenu by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp).padding(top = 26.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        Text(if (english) "Settings" else "Pengaturan", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)
        Text(if (english) "Personalize your KasMate experience" else "Atur pengalaman KasMate sesuai gayamu", color = MaterialTheme.colorScheme.onSurfaceVariant)
        SettingCard(if (english) "APPEARANCE" else "TAMPILAN") {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) { Icon(if (dark) Icons.Default.DarkMode else Icons.Default.LightMode, null, tint = Red); Column { Text(if (english) "Dark mode" else "Mode gelap", fontWeight = FontWeight.Bold); Text(if (dark) if (english) "On" else "Aktif" else if (english) "Off" else "Nonaktif", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) } }
                Switch(dark, onDark)
            }
        }
        SettingCard(if (english) "LANGUAGE" else "BAHASA") {
            Box {
                Row(Modifier.fillMaxWidth().clickable { languageMenu = true }, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) { Icon(Icons.Default.Language, null, tint = Red); Column { Text(if (english) "App language" else "Bahasa aplikasi", fontWeight = FontWeight.Bold); Text(if (language == AppLanguage.ENGLISH) "English" else "Bahasa Indonesia", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) } }; Icon(Icons.Default.ExpandMore, null)
                }
                DropdownMenu(languageMenu, { languageMenu = false }) { AppLanguage.entries.forEach { l -> DropdownMenuItem(text = { Text(if (l == AppLanguage.ENGLISH) "English" else "Bahasa Indonesia") }, onClick = { onLanguage(l); languageMenu = false }) } }
            }
        }
        SettingCard(if (english) "ACCOUNT" else "AKUN") { Row(Modifier.fillMaxWidth().clickable(onClick = onOpenProfile), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) { Icon(Icons.Default.Person, null, tint = Red); Column(Modifier.weight(1f)) { Text(if (english) "Profile" else "Profil", fontWeight = FontWeight.Bold); Text(if (english) "Manage your personal information" else "Kelola informasi pribadimu", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) }; Icon(Icons.Default.ChevronRight, null) } }
    }
}

@Composable
private fun SettingCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxWidth().shadow(1.dp, BigShape).clip(BigShape).background(MaterialTheme.colorScheme.surface)
        .border(1.dp, MaterialTheme.colorScheme.outline, BigShape).padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text(title, color = Red, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.3.sp)
        content()
    }
}

@Composable
private fun ProfilePage(english: Boolean, userName: String) {
    val displayName = userName.trim().ifBlank { if (english) "Student" else "Siswa" }
    Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(if (english) "My profile" else "Profil saya", Modifier.fillMaxWidth(), fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)
        Spacer(Modifier.height(22.dp))
        Box(Modifier.size(100.dp).clip(CircleShape).background(Red), contentAlignment = Alignment.Center) { Text("A", color = Color.White, fontSize = 40.sp, fontWeight = FontWeight.ExtraBold) }
        Text(displayName, fontSize = 21.sp, fontWeight = FontWeight.Bold)
        Text(if (english) "Student · Class XI PPLG 2" else "Siswa · Kelas XI PPLG 2", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(8.dp))
        SettingCard(if (english) "PERSONAL INFORMATION" else "INFORMASI PRIBADI") {
            ProfileLine(if (english) "Full name" else "Nama lengkap", displayName)
            ProfileLine(if (english) "Role" else "Peran", if (english) "Student" else "Siswa")
            ProfileLine("Kelas", "XI PPLG 2")
        }
    }
}

@Composable
private fun ProfileLine(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant); Text(value, fontWeight = FontWeight.SemiBold) }
}
