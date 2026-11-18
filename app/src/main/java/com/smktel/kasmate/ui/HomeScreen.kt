package com.smktel.kasmate.ui

import android.net.Uri
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.asImageBitmap
import android.graphics.BitmapFactory
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smktel.kasmate.data.*
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.delay

enum class AppPage { HOME, PAYMENTS, LEDGER, ASSISTANT, SETTINGS, PROFILE }
enum class AppLanguage { INDONESIAN, ENGLISH }

fun rupiah(n: Long): String = "Rp " + NumberFormat.getInstance(Locale.forLanguageTag("id-ID")).format(n)
fun tanggal(ms: Long): String = SimpleDateFormat("d MMM yyyy", Locale.forLanguageTag("id-ID")).format(Date(ms))

@Composable
fun HomeScreen(
    userName: String,
    className: String,
    studentNis: String,
    summary: KasSummary,
    paymentRequests: List<StudentPaymentRequest>,
    onSubmitPayment: (Long, String, Uri?, (String?) -> Unit) -> Unit,
    darkMode: Boolean,
    onDarkModeChange: (Boolean) -> Unit,
    font: AppFont,
    onFontChange: (AppFont) -> Unit,
    notificationsAllowed: Boolean,
    onRequestNotifications: () -> Unit,
    language: AppLanguage,
    onLanguageChange: (AppLanguage) -> Unit,
    onLogout: () -> Unit
) {
    val s = summary
    val context = LocalContext.current
    val calendarPrefs = remember(context) { context.getSharedPreferences("kasmate_preferences", android.content.Context.MODE_PRIVATE) }
    val calendarNotes = remember(calendarPrefs) { mutableStateListOf<CalendarStickyNote>().apply { addAll(readCalendarNotes(calendarPrefs)) } }
    var page by remember { mutableStateOf(AppPage.HOME) }
    var calendarOpen by remember { mutableStateOf(false) }
    var profileImageUri by remember { mutableStateOf(calendarPrefs.getString("profile_image_uri", null)) }
    val profilePicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) { profileImageUri = uri.toString(); calendarPrefs.edit().putString("profile_image_uri", uri.toString()).apply() }
    }
    var paymentCelebration by remember { mutableStateOf(false) }
    var requestedBillPaymentId by remember { mutableStateOf<String?>(null) }
    var observedPaymentStatuses by remember { mutableStateOf<Map<Long, String>?>(null) }
    var rejectedPaymentNotices by remember { mutableStateOf(emptyList<StudentPaymentRequest>()) }
    val english = language == AppLanguage.ENGLISH
    LaunchedEffect(paymentRequests) {
        val previousStatuses = observedPaymentStatuses
        if (previousStatuses != null) {
            // A request can be submitted and rejected between the app's 60-second refreshes.
            val newlyRejected = paymentRequests.filter { request ->
                request.status == "REJECTED" &&
                    (previousStatuses[request.id] == "PENDING" || request.id !in previousStatuses)
            }
            rejectedPaymentNotices = (rejectedPaymentNotices + newlyRejected).distinctBy { it.id }
        }
        observedPaymentStatuses = paymentRequests.associate { it.id to it.status }
    }
    val startOfToday = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
    val pendingReviewRequests = paymentRequests.filter { it.status == "PENDING" }
    val pendingReviewBillIds = pendingReviewRequests.map { it.billStudentId }.toSet()
    val overdueBills = s.myBills.filter {
        it.amountDue > 0 && it.status != PaymentStatus.PROSES && it.dueDateMillis < startOfToday &&
            it.id.toLongOrNull()?.let(pendingReviewBillIds::contains) != true
    }
    val overdueAmount = overdueBills.sumOf { it.amountDue }
    val maxOverdueDays = overdueBills.maxOfOrNull {
        ((startOfToday - it.dueDateMillis) / 86_400_000L).toInt().coerceAtLeast(1)
    } ?: 0
    val billsToRemind = s.myBills.filter { bill ->
        bill.amountDue > 0 && bill.status != PaymentStatus.PROSES &&
            bill.id.toLongOrNull()?.let(pendingReviewBillIds::contains) != true
    }
    val reminderBill = billsToRemind.minByOrNull { it.dueDateMillis }
    val reminderDaysLate = reminderBill?.takeIf { it.dueDateMillis < startOfToday }?.let {
        ((startOfToday - it.dueDateMillis) / 86_400_000L).toInt().coerceAtLeast(1)
    } ?: 0
    val mascotMood = when {
        paymentCelebration || pendingReviewRequests.isNotEmpty() -> MascotMood.EXCITED
        reminderDaysLate >= 7 -> MascotMood.ANGRY
        reminderDaysLate > 0 -> MascotMood.SAD
        reminderBill != null -> MascotMood.NEUTRAL
        s.myBills.any { it.status == PaymentStatus.LUNAS } -> MascotMood.HAPPY
        else -> MascotMood.NEUTRAL
    }
    val mascotPhrases = when {
        paymentCelebration -> listOf(
            if (english) "Request sent!" else "Permintaan terkirim!",
            if (english) "Waiting for admin approval" else "Menunggu persetujuan admin"
        )
        pendingReviewRequests.isNotEmpty() -> listOf(
            if (english) "Waiting for admin review" else "Menunggu verifikasi admin",
            if (english) "Your bill updates after approval" else "Tagihan berubah setelah disetujui"
        )
        reminderBill != null && reminderDaysLate > 0 -> listOf(
            if (english) "Tap to pay" else "Ketuk untuk bayar",
            if (english) "$reminderDaysLate days overdue" else "Terlambat $reminderDaysLate hari",
            "${reminderBill.title} · ${rupiah(reminderBill.amountDue)}"
        )
        reminderBill != null -> listOf(
            if (english) "Tap to pay" else "Ketuk untuk bayar",
            if (english) "${reminderBill.title} is still due" else "${reminderBill.title} belum lunas"
        )
        s.myBills.any { it.status == PaymentStatus.LUNAS } -> listOf(
            if (english) "Tap to check bills" else "Ketuk cek tagihan",
            if (english) "Your dues are paid!" else "Kas kamu sudah lunas!"
        )
        else -> listOf(
            if (english) "Tap to view bills" else "Ketuk untuk cek tagihan",
            if (english) "No bills due right now" else "Belum ada tagihan saat ini"
        )
    }
    LaunchedEffect(overdueAmount, maxOverdueDays) {
        calendarPrefs.edit().putLong("overdue_amount", overdueAmount).putInt("overdue_days", maxOverdueDays).apply()
    }
    LaunchedEffect(page, paymentCelebration) {
        if (page == AppPage.HOME && paymentCelebration) {
            delay(8_000)
            paymentCelebration = false
        }
    }
    Scaffold(
        containerColor = Color.Transparent,
        floatingActionButton = {
            FloatingActionButton(onClick = { calendarOpen = true }, containerColor = Red, contentColor = Color.White,
                shape = RoundedCornerShape(19.dp)) {
                Icon(Icons.Default.CalendarMonth, if (english) "Open calendar" else "Buka kalender")
            }
        },
        bottomBar = {
            BoxWithConstraints(Modifier.fillMaxWidth().navigationBarsPadding()) {
                val compact = maxWidth < 392.dp
                val labelSize = if (compact) 10.sp else 11.sp
                NavigationBar(Modifier.padding(horizontal = 14.dp, vertical = 8.dp).shadow(12.dp, RoundedCornerShape(30.dp)).clip(RoundedCornerShape(30.dp)), containerColor = MaterialTheme.colorScheme.surface, tonalElevation = 0.dp, windowInsets = WindowInsets(0)) {
                    val selectedPage = if (page == AppPage.PROFILE) AppPage.SETTINGS else page
                    NavigationBarItem(selectedPage == AppPage.HOME, { page = AppPage.HOME }, icon = { AnimatedNavIcon(selectedPage == AppPage.HOME) { Icon(Icons.Default.Home, if (english) "Home" else "Beranda") } }, label = { NavLabel(if (english) "Home" else "Beranda", labelSize) }, colors = navColors())
                    NavigationBarItem(selectedPage == AppPage.PAYMENTS, { page = AppPage.PAYMENTS }, icon = { AnimatedNavIcon(selectedPage == AppPage.PAYMENTS) { Icon(Icons.Default.Payments, if (english) "Pay" else "Bayar") } }, label = { NavLabel(if (english) "Pay" else "Bayar", labelSize) }, colors = navColors())
                    NavigationBarItem(selectedPage == AppPage.LEDGER, { page = AppPage.LEDGER }, icon = { AnimatedNavIcon(selectedPage == AppPage.LEDGER) { Icon(Icons.Default.AccountBalance, if (english) "Class ledger" else "Kas kelas") } }, label = { NavLabel(if (compact) { if (english) "Class" else "Kas" } else { if (english) "Class" else "Kas kelas" }, labelSize) }, colors = navColors())
                    NavigationBarItem(selectedPage == AppPage.ASSISTANT, { page = AppPage.ASSISTANT }, icon = { AnimatedNavIcon(selectedPage == AppPage.ASSISTANT) { Icon(Icons.Default.AutoAwesome, if (english) "Assistant" else "Asisten") } }, label = { NavLabel(if (compact) "AI" else if (english) "Assistant" else "Asisten", labelSize) }, colors = navColors())
                    NavigationBarItem(selectedPage == AppPage.SETTINGS, { page = AppPage.SETTINGS }, icon = { AnimatedNavIcon(selectedPage == AppPage.SETTINGS) { Icon(Icons.Default.Settings, if (english) "Settings" else "Pengaturan") } }, label = { NavLabel(if (compact) { if (english) "Settings" else "Setelan" } else { if (english) "Settings" else "Pengaturan" }, labelSize) }, colors = navColors())
                }
            }
        }
    ) { padding ->
        val motionEnabled = LocalMotionEnabled.current
        Box(Modifier.fillMaxSize().padding(padding)) {
            AnimatedContent(
                targetState = page,
                modifier = Modifier.fillMaxSize(),
                transitionSpec = {
                    val direction = if (targetState.ordinal >= initialState.ordinal) 1 else -1
                    val enterMs = if (motionEnabled) 230 else 0
                    val exitMs = if (motionEnabled) 150 else 0
                    (fadeIn(tween(enterMs)) + slideInHorizontally(tween(enterMs)) { direction * it / 14 }) togetherWith
                        (fadeOut(tween(exitMs)) + slideOutHorizontally(tween(exitMs)) { -direction * it / 18 })
                },
                label = "page transition"
            ) { currentPage ->
                when (currentPage) {
                    AppPage.HOME -> Dashboard(
                        s,
                        paymentRequests,
                        english,
                        userName,
                        paymentCelebration,
                        openSettings = { page = AppPage.SETTINGS },
                        openPayments = { page = AppPage.PAYMENTS },
                        openLedger = { page = AppPage.LEDGER },
                        openAssistant = { page = AppPage.ASSISTANT }
                    )
                    AppPage.PAYMENTS -> StudentPaymentScreen(
                        summary = s,
                        english = english,
                        requests = paymentRequests,
                        initialBillId = requestedBillPaymentId,
                        onInitialBillHandled = { requestedBillPaymentId = null },
                        onSubmitPayment = { billId, method, proofUri, complete ->
                            onSubmitPayment(billId, method, proofUri) { error ->
                                if (error == null) paymentCelebration = true
                                complete(error)
                            }
                        })
                    AppPage.LEDGER -> StudentLedgerScreen(
                        summary = s,
                        english = english,
                        requests = paymentRequests,
                        onPayIuran = { billId ->
                            requestedBillPaymentId = billId
                            page = AppPage.PAYMENTS
                        }
                    )
                    AppPage.ASSISTANT -> KasMateAssistantScreen(s, userName, english)
                    AppPage.SETTINGS -> SettingsPage(darkMode, onDarkModeChange, font, onFontChange, notificationsAllowed, onRequestNotifications, language, onLanguageChange, english) { page = AppPage.PROFILE }
                    AppPage.PROFILE -> ProfilePage(
                        english = english,
                        userName = userName,
                        className = className,
                        studentNis = studentNis,
                        imageUri = profileImageUri,
                        onPickImage = {
                            profilePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        },
                        onLogout = onLogout
                    )
                }
            }
            if (page != AppPage.ASSISTANT && page != AppPage.PROFILE) {
                FloatingPaymentMascot(
                    mood = mascotMood,
                    phrases = mascotPhrases,
                    english = english,
                    onClick = { page = AppPage.PAYMENTS },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
    rejectedPaymentNotices.firstOrNull()?.let { rejected ->
        val closeRejectionNotice = { rejectedPaymentNotices = rejectedPaymentNotices.drop(1) }
        AlertDialog(
            onDismissRequest = closeRejectionNotice,
            icon = { Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = Red) },
            title = { Text(if (english) "Payment request rejected" else "Permintaan pembayaran ditolak") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(rejected.billName, fontWeight = FontWeight.Bold)
                    Text(
                        rejected.reviewNote.ifBlank {
                            if (english) "Ask your class administrator for details." else "Hubungi admin kelas untuk mengetahui alasannya."
                        }
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { closeRejectionNotice(); page = AppPage.PAYMENTS }) {
                    Text(if (english) "View payment requests" else "Lihat pembayaran")
                }
            },
            dismissButton = {
                TextButton(onClick = closeRejectionNotice) {
                    Text(if (english) "Close" else "Tutup")
                }
            }
        )
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
private fun AnimatedNavIcon(selected: Boolean, content: @Composable () -> Unit) {
    val motionEnabled = LocalMotionEnabled.current
    val scale by animateFloatAsState(
        targetValue = if (selected && motionEnabled) 1.1f else 1f,
        animationSpec = tween(if (motionEnabled) 190 else 0),
        label = "navigation icon scale"
    )
    Box(Modifier.graphicsLayer { scaleX = scale; scaleY = scale }, contentAlignment = Alignment.Center) {
        content()
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
private fun Dashboard(
    s: KasSummary,
    paymentRequests: List<StudentPaymentRequest>,
    english: Boolean,
    userName: String,
    paymentSubmitted: Boolean,
    openSettings: () -> Unit,
    openPayments: () -> Unit,
    openLedger: () -> Unit,
    openAssistant: () -> Unit
) {
    val regularBills = s.myBills.filterNot { it.isIuran }
    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Row(Modifier.fillMaxWidth().padding(top = 22.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("KASMATE", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, letterSpacing = 2.sp, fontSize = 12.sp)
                    Text(greeting(userName, english), fontSize = 25.sp, fontWeight = FontWeight.ExtraBold)
                }
                Row {
                    IconButton(onClick = openSettings) { Icon(Icons.Default.Tune, if (english) "Settings" else "Pengaturan") }
                }
            }
        }
        item { StudentStatusCard(s, paymentRequests, paymentSubmitted, english, openPayments) }
        item { BalanceCard(s.totalBalance, english) }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MiniStat(Modifier.weight(1f), Icons.Default.TrendingUp, if (english) "Income" else "Pemasukan", rupiah(s.totalIncome), Color(0xFF168A62))
                MiniStat(Modifier.weight(1f), Icons.Default.TrendingDown, if (english) "Expenses" else "Pengeluaran", rupiah(s.totalExpenses), MaterialTheme.colorScheme.primary)
            }
        }
        item { ExpenseTrendCard(s.expenses, english) }
        item { ClassUpdateCard(s.expenses, english, openLedger) }
        item {
            OutlinedButton(onClick = openAssistant, modifier = Modifier.fillMaxWidth(), shape = BigShape) {
                Icon(Icons.Default.AutoAwesome, null, tint = Red); Spacer(Modifier.width(8.dp)); Text(if (english) "Ask KasMate Assistant" else "Tanya Asisten KasMate", color = Red)
            }
        }
        item { SectionTitle(if (english) "Weekly bills" else "Tagihan mingguan", if (english) "See all" else "Lihat semua", openPayments) }
        if (regularBills.isEmpty()) item { EmptyState(if (english) "No weekly bills have been assigned yet." else "Belum ada tagihan mingguan yang diberikan.") }
        items(regularBills, key = { "bill-${it.id}" }) { BillRow(it, english, openPayments) }
        item { SectionTitle(if (english) "Recent expenses" else "Pengeluaran terbaru", if (english) "History" else "Riwayat", openLedger) }
        if (s.expenses.isEmpty()) item { EmptyState(if (english) "Class expenses will appear here when recorded." else "Pengeluaran kelas akan tampil di sini setelah dicatat.") }
        items(s.expenses.take(3), key = { it.id }) { ExpenseRow(it, openLedger) }
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
private fun StudentStatusCard(
    summary: KasSummary,
    requests: List<StudentPaymentRequest>,
    paymentSubmitted: Boolean,
    english: Boolean,
    onClick: () -> Unit
) {
    val pendingRequest = requests.firstOrNull { it.status == "PENDING" }
    val pendingBillIds = requests.filter { it.status == "PENDING" }.map { it.billStudentId }.toSet()
    val unpaid = summary.myBills.firstOrNull { bill ->
        bill.amountDue > 0 && bill.status != PaymentStatus.PROSES &&
            bill.id.toLongOrNull()?.let(pendingBillIds::contains) != true
    }
    val waitingForReview = paymentSubmitted || pendingRequest != null
    val overdueDays = unpaid?.takeIf { it.dueDateMillis < System.currentTimeMillis() }?.let {
        ((System.currentTimeMillis() - it.dueDateMillis) / 86_400_000L).toInt().coerceAtLeast(1)
    } ?: 0
    StudentPanel(Modifier.clickable(onClick = onClick)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(11.dp)) {
            Box(Modifier.size(40.dp).clip(CircleShape).background(if (unpaid == null || waitingForReview) Color(0xFF168A62).copy(alpha = .12f) else Red.copy(alpha = .1f)), contentAlignment = Alignment.Center) {
                Icon(if (unpaid == null && !waitingForReview) Icons.Default.CheckCircle else if (waitingForReview) Icons.Default.Schedule else Icons.Default.NotificationsActive, null, tint = if (unpaid == null || waitingForReview) Color(0xFF168A62) else Red)
            }
            Column(Modifier.weight(1f)) {
                Text(
                    when {
                        waitingForReview -> if (english) "Payment awaiting admin review" else "Menunggu verifikasi admin"
                        unpaid == null -> if (english) "Class dues paid" else "Kas kelas lunas"
                        overdueDays > 0 -> if (english) "Overdue $overdueDays days" else "Terlambat $overdueDays hari"
                        else -> if (english) "Payment due this month" else "Kas bulan ini belum lunas"
                    },
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Text(
                    pendingRequest?.billName ?: unpaid?.let { "${it.title} · ${rupiah(it.amountDue)}" }
                        ?: if (english) "You're all caught up" else "Kamu tidak memiliki tunggakan",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp
                )
            }
            Icon(Icons.Default.ChevronRight, null, tint = Red)
        }
    }
}

@Composable
private fun ClassUpdateCard(expenses: List<Expense>, english: Boolean, openLedger: () -> Unit) {
    val latestExpense = expenses.maxByOrNull { it.dateMillis }
    StudentPanel(Modifier.clickable(onClick = openLedger)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Campaign, null, tint = Red)
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Text(if (english) "Class cash update" else "Kabar kas kelas", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text(
                    latestExpense?.let { if (english) "Latest expense: ${it.description} · ${tanggal(it.dateMillis)}" else "Pengeluaran terbaru: ${it.description} · ${tanggal(it.dateMillis)}" }
                        ?: if (english) "No class expenses have been recorded yet." else "Belum ada pengeluaran kas kelas yang dicatat.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Icon(Icons.Default.ChevronRight, null, tint = Red)
        }
    }
}

@Composable
private fun BalanceCard(total: Long, english: Boolean) {
    var shown by remember { mutableStateOf(false) }
    val motionEnabled = LocalMotionEnabled.current
    val scale by animateFloatAsState(if (!motionEnabled || shown) 1f else .94f, animationSpec = tween(if (motionEnabled) 260 else 0), label = "balance scale")
    LaunchedEffect(Unit) { shown = true }
    Column(Modifier.fillMaxWidth().padding(top = 4.dp).clip(BigShape).animateContentSize(animationSpec = tween(if (motionEnabled) 220 else 0))
        .background(Brush.linearGradient(listOf(DeepRed, Red, SoftRed)))
        .padding(24.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(if (english) "TOTAL CLASS BALANCE" else "TOTAL SALDO KELAS", color = Color.White.copy(alpha = .82f), fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            Icon(Icons.Default.AccountBalanceWallet, null, tint = Color.White)
        }
        Text(rupiah(total), color = Color.White, fontSize = (32 * scale).sp, fontWeight = FontWeight.ExtraBold)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Sync, null, tint = Color.White, modifier = Modifier.size(17.dp))
            Text(if (english) "  Latest class account balance" else "  Saldo terkini dari kas kelas", color = Color.White, fontSize = 12.sp)
        }
    }
}

@Composable
private fun MiniStat(modifier: Modifier, icon: ImageVector, label: String, value: String, tint: Color) {
    Column(modifier.clip(BigShape).background(MaterialTheme.colorScheme.surface)
        .border(1.dp, MaterialTheme.colorScheme.outline, BigShape).padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) { Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(18.dp)); Text("  $label", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp) }
        Text(value, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
    }
}

private data class ExpenseChartBucket(val label: String, val amount: Long)

private fun expenseChartBuckets(expenses: List<Expense>, weekly: Boolean, english: Boolean): List<ExpenseChartBucket> {
    val locale = if (english) Locale.US else Locale.forLanguageTag("id-ID")
    val now = Calendar.getInstance()
    val starts = (5 downTo 0).map { back ->
        (now.clone() as Calendar).apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (weekly) {
                val offset = (get(Calendar.DAY_OF_WEEK) + 5) % 7
                add(Calendar.DAY_OF_MONTH, -offset)
                add(Calendar.WEEK_OF_YEAR, -back)
            } else {
                set(Calendar.DAY_OF_MONTH, 1)
                add(Calendar.MONTH, -back)
            }
        }
    }
    val format = SimpleDateFormat(if (weekly) "d MMM" else "MMM", locale)
    return starts.mapIndexed { index, start ->
        val end = (start.clone() as Calendar).apply { add(if (weekly) Calendar.WEEK_OF_YEAR else Calendar.MONTH, 1) }
        val total = expenses.asSequence()
            .filter { it.dateMillis >= start.timeInMillis && it.dateMillis < end.timeInMillis }
            .sumOf { it.amount }
        ExpenseChartBucket(format.format(start.time).replaceFirstChar { if (it.isLowerCase()) it.titlecase(locale) else it.toString() }, total)
    }
}

@Composable
private fun ExpenseTrendCard(expenses: List<Expense>, english: Boolean) {
    var weekly by remember { mutableStateOf(false) }
    val motionEnabled = LocalMotionEnabled.current
    val buckets = remember(expenses, weekly, english) { expenseChartBuckets(expenses, weekly, english) }
    var chartTarget by remember(weekly, expenses) { mutableStateOf(0f) }
    val reveal by animateFloatAsState(
        targetValue = chartTarget,
        animationSpec = tween(if (motionEnabled) 420 else 0),
        label = "expense chart reveal"
    )
    LaunchedEffect(buckets, motionEnabled) {
        chartTarget = 0f
        if (motionEnabled) delay(30)
        chartTarget = 1f
    }
    val periodTint by animateColorAsState(
        if (weekly) Red else Red.copy(alpha = .1f),
        animationSpec = tween(if (motionEnabled) 160 else 0),
        label = "chart period"
    )
    val hasExpenses = expenses.isNotEmpty()
    val maximum = buckets.maxOfOrNull { it.amount }?.coerceAtLeast(1L) ?: 1L
    val gridColor = MaterialTheme.colorScheme.outline.copy(alpha = .55f)
    Column(Modifier.fillMaxWidth().clip(BigShape).background(MaterialTheme.colorScheme.surface)
        .border(1.dp, MaterialTheme.colorScheme.outline, BigShape).padding(18.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(if (english) "Class expenses" else "Pengeluaran kas", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text(if (english) "Recorded transactions" else "Transaksi yang sudah dicatat", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(
                text = if (weekly) if (english) "WEEK" else "MINGGU" else if (english) "MONTH" else "BULAN",
                color = if (weekly) Color.White else Red,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.clip(CircleShape).clickable { weekly = !weekly }.background(periodTint).padding(horizontal = 12.dp, vertical = 8.dp)
            )
        }
        Spacer(Modifier.height(14.dp))
        if (hasExpenses) {
            Canvas(Modifier.fillMaxWidth().height(118.dp)) {
                val step = size.width / (buckets.size - 1)
                for (i in 0..3) {
                    drawLine(
                        gridColor,
                        androidx.compose.ui.geometry.Offset(0f, size.height * i / 3),
                        androidx.compose.ui.geometry.Offset(size.width, size.height * i / 3),
                        1.dp.toPx()
                    )
                }
                val points = buckets.mapIndexed { index, bucket ->
                    val amountScale = (bucket.amount.toFloat() / maximum.toFloat()).coerceIn(0f, 1f)
                    androidx.compose.ui.geometry.Offset(
                        index * step,
                        size.height * (1f - (.15f + amountScale * .72f * reveal))
                    )
                }
                val line = Path().apply {
                    points.forEachIndexed { index, point ->
                        if (index == 0) moveTo(point.x, point.y)
                        else quadraticTo((points[index - 1].x + point.x) / 2f, points[index - 1].y, point.x, point.y)
                    }
                }
                val area = Path().apply {
                    moveTo(points.first().x, size.height)
                    lineTo(points.first().x, points.first().y)
                    points.drop(1).forEachIndexed { index, point ->
                        val previous = points[index]
                        quadraticTo((previous.x + point.x) / 2f, previous.y, point.x, point.y)
                    }
                    lineTo(points.last().x, size.height)
                    close()
                }
                drawPath(area, Brush.verticalGradient(listOf(Red.copy(alpha = .18f), SoftRed.copy(alpha = .015f))))
                drawPath(line, Red, style = Stroke(3.dp.toPx(), cap = StrokeCap.Round))
                points.forEach { drawCircle(Red, 4.dp.toPx(), it) }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                buckets.forEach { Text(it.label, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
        } else {
            Text(
                if (english) "Expense history will appear after the class records its first transaction." else "Riwayat pengeluaran tampil setelah transaksi pertama dicatat.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
                lineHeight = 18.sp,
                modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp)
            )
        }
    }
}

@Composable
private fun SectionTitle(title: String, action: String, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(title, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
        Box(Modifier.defaultMinSize(minHeight = 48.dp).clip(CircleShape).clickable(onClick = onClick).padding(horizontal = 8.dp), contentAlignment = Alignment.Center) {
            Text(action, color = Red, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun BillRow(b: Bill, english: Boolean, onClick: () -> Unit) {
    val paid = b.status == PaymentStatus.LUNAS
    Row(Modifier.fillMaxWidth().clip(BigShape).clickable(onClick = onClick).background(MaterialTheme.colorScheme.surface)
        .border(1.dp, MaterialTheme.colorScheme.outline, BigShape).padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.size(42.dp).clip(CircleShape).background(Red.copy(alpha = .12f)), contentAlignment = Alignment.Center) { Icon(Icons.Default.Receipt, null, tint = Red) }
            Column { Text(b.title, fontWeight = FontWeight.Bold, fontSize = 15.sp); Text("${if (english) "Due" else "Tenggat"} ${tanggal(b.dueDateMillis)}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
        Column(horizontalAlignment = Alignment.End) { Text(rupiah(if (paid) b.amountPaid else b.amountDue), color = MaterialTheme.colorScheme.tertiary, fontWeight = FontWeight.Bold); Text(if (paid) if (english) "PAID" else "LUNAS" else if (b.status == PaymentStatus.SEBAGIAN) if (english) "PARTIAL" else "SEBAGIAN" else if (english) "UNPAID" else "BELUM", color = if (paid) Color(0xFF168A62) else Red, fontWeight = FontWeight.ExtraBold, fontSize = 10.sp) }
    }
}

@Composable
private fun ExpenseRow(e: Expense, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clip(BigShape).clickable(onClick = onClick).background(MaterialTheme.colorScheme.surface)
        .border(1.dp, MaterialTheme.colorScheme.outline, BigShape).padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) { Text(e.description, fontWeight = FontWeight.Bold, fontSize = 15.sp); Text("${tanggal(e.dateMillis)} · ${e.noteBy}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        Text("− ${rupiah(e.amount)}", color = MaterialTheme.colorScheme.tertiary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
    }
}

@Composable
private fun SettingsPage(dark: Boolean, onDark: (Boolean) -> Unit, font: AppFont, onFontChange: (AppFont) -> Unit, notificationsAllowed: Boolean, onRequestNotifications: () -> Unit, language: AppLanguage, onLanguage: (AppLanguage) -> Unit, english: Boolean, onOpenProfile: () -> Unit) {
    var languageMenu by remember { mutableStateOf(false) }
    var fontMenu by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(top = 26.dp, bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
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
                Row(Modifier.fillMaxWidth().defaultMinSize(minHeight = 48.dp).clickable { languageMenu = true }, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) { Icon(Icons.Default.Language, null, tint = Red); Column { Text(if (english) "App language" else "Bahasa aplikasi", fontWeight = FontWeight.Bold); Text(if (language == AppLanguage.ENGLISH) "English" else "Bahasa Indonesia", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) } }; Icon(Icons.Default.ExpandMore, null)
                }
                DropdownMenu(languageMenu, { languageMenu = false }) { AppLanguage.entries.forEach { l -> DropdownMenuItem(text = { Text(if (l == AppLanguage.ENGLISH) "English" else "Bahasa Indonesia") }, onClick = { onLanguage(l); languageMenu = false }) } }
            }
        }
        SettingCard(if (english) "FONT" else "HURUF") {
            Box {
                Row(Modifier.fillMaxWidth().defaultMinSize(minHeight = 48.dp).clickable { fontMenu = true }, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Icon(Icons.Default.TextFields, null, tint = Red)
                        Column {
                            Text(if (english) "Font style" else "Gaya font", fontWeight = FontWeight.Bold)
                            Text(font.label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Icon(Icons.Default.ExpandMore, null)
                }
                DropdownMenu(fontMenu, { fontMenu = false }) {
                    AppFont.entries.forEach { choice ->
                        DropdownMenuItem(
                            text = { Text(choice.label) },
                            trailingIcon = if (choice == font) ({ Icon(Icons.Default.Check, null, tint = Red) }) else null,
                            onClick = { onFontChange(choice); fontMenu = false }
                        )
                    }
                }
            }
        }
        SettingCard(if (english) "PHONE NOTIFICATIONS" else "NOTIFIKASI HP") {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(Modifier.size(40.dp).clip(CircleShape).background(if (notificationsAllowed) Color(0xFF168A62).copy(alpha = .12f) else Red.copy(alpha = .1f)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.NotificationsActive, null, tint = if (notificationsAllowed) Color(0xFF168A62) else Red)
                }
                Column(Modifier.weight(1f)) {
                    Text(if (english) "Bill and reminder alerts" else "Notifikasi tagihan dan pengingat", fontWeight = FontWeight.Bold)
                    Text(
                        if (notificationsAllowed) if (english) "Android notifications are allowed" else "Izin notifikasi Android aktif" else if (english) "Allow alerts while KasMate is closed" else "Izinkan pemberitahuan saat KasMate ditutup",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            OutlinedButton(onClick = onRequestNotifications, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                Icon(if (notificationsAllowed) Icons.Default.Settings else Icons.Default.Notifications, null, tint = Red)
                Spacer(Modifier.width(8.dp))
                Text(if (notificationsAllowed) if (english) "Manage notification permission" else "Kelola izin notifikasi" else if (english) "Enable notifications" else "Aktifkan notifikasi", color = Red)
            }
        }
        SettingCard(if (english) "ACCOUNT" else "AKUN") { Row(Modifier.fillMaxWidth().defaultMinSize(minHeight = 48.dp).clickable(onClick = onOpenProfile), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) { Icon(Icons.Default.Person, null, tint = Red); Column(Modifier.weight(1f)) { Text(if (english) "Profile" else "Profil", fontWeight = FontWeight.Bold); Text(if (english) "Manage your personal information" else "Kelola informasi pribadimu", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) }; Icon(Icons.Default.ChevronRight, null) } }
    }
}

@Composable
private fun SettingCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxWidth().clip(BigShape).background(MaterialTheme.colorScheme.surface)
        .border(1.dp, MaterialTheme.colorScheme.outline, BigShape).padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text(title, color = Red, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.3.sp)
        content()
    }
}

@Composable
private fun ProfilePage(
    english: Boolean,
    userName: String,
    className: String,
    studentNis: String,
    imageUri: String?,
    onPickImage: () -> Unit,
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    val displayName = userName.trim().ifBlank { if (english) "Student" else "Siswa" }
    val avatar = remember(imageUri) { runCatching { imageUri?.let { context.contentResolver.openInputStream(Uri.parse(it))?.use(BitmapFactory::decodeStream) } }.getOrNull() }
    Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(if (english) "My profile" else "Profil saya", Modifier.fillMaxWidth(), fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)
        Spacer(Modifier.height(22.dp))
        Box(Modifier.size(112.dp).clip(CircleShape).background(Red).clickable(onClick = onPickImage), contentAlignment = Alignment.Center) {
            if (avatar != null) Image(avatar.asImageBitmap(), contentDescription = "Foto profil", modifier = Modifier.fillMaxSize(), contentScale = androidx.compose.ui.layout.ContentScale.Crop)
            else Text(displayName.take(1).uppercase(), color = Color.White, fontSize = 40.sp, fontWeight = FontWeight.ExtraBold)
        }
        Text(if (english) "Tap photo to change" else "Ketuk foto untuk mengganti", color = Red, fontSize = 12.sp, modifier = Modifier.clickable(onClick = onPickImage))
        Text(displayName, fontSize = 21.sp, fontWeight = FontWeight.Bold)
        Text(if (english) "Student · ${className.ifBlank { "No class" }}" else "Siswa · Kelas ${className.ifBlank { "Belum ditetapkan" }}", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(8.dp))
        SettingCard(if (english) "PERSONAL INFORMATION" else "INFORMASI PRIBADI") {
            ProfileLine(if (english) "Full name" else "Nama lengkap", displayName)
            if (studentNis.isNotBlank()) ProfileLine("NIS", studentNis)
            ProfileLine(if (english) "Role" else "Peran", if (english) "Student" else "Siswa")
            ProfileLine("Kelas", className.ifBlank { if (english) "Not assigned" else "Belum ditetapkan" })
        }
        Button(onClick = onLogout, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Red)) {
            Icon(Icons.Default.Logout, null)
            Spacer(Modifier.width(8.dp))
            Text(if (english) "Sign out" else "Keluar")
        }
    }
}

@Composable
private fun ProfileLine(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant); Text(value, fontWeight = FontWeight.SemiBold) }
}
