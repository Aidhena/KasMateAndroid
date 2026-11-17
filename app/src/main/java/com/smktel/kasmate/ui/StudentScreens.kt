package com.smktel.kasmate.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smktel.kasmate.data.*
import java.util.concurrent.TimeUnit

data class DemoPaymentRequest(
    val id: String,
    val title: String,
    val amount: Long,
    val method: String,
    val proofAttached: Boolean,
    val createdAt: Long = System.currentTimeMillis()
)

data class DemoDispute(val id: String, val message: String, val status: String = "Menunggu bendahara")

@Composable
fun StudentPaymentScreen(
    summary: KasSummary,
    english: Boolean,
    requests: List<DemoPaymentRequest>,
    disputes: List<DemoDispute>,
    onRequest: (DemoPaymentRequest) -> Unit,
    onDispute: (DemoDispute) -> Unit
) {
    val unpaid = summary.myBills.filter { it.status == PaymentStatus.BELUM }
    val nextBill = unpaid.minByOrNull { it.dueDateMillis }
    var showPayment by remember { mutableStateOf(false) }
    var showDispute by remember { mutableStateOf(false) }
    var chosenBill by remember { mutableStateOf(nextBill) }
    var selectedReceipt by remember { mutableStateOf<Bill?>(null) }
    var method by remember { mutableStateOf("QRIS") }
    var proof by remember { mutableStateOf<Uri?>(null) }
    var disputeMessage by remember { mutableStateOf("") }
    var notice by remember { mutableStateOf<String?>(null) }
    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { proof = it }
    val contributions = summary.myBills.filter { it.status == PaymentStatus.LUNAS }.sumOf { it.amount }
    val paidBills = summary.myBills.filter { it.status == PaymentStatus.LUNAS }
    val daysLeft = nextBill?.let { TimeUnit.MILLISECONDS.toDays((it.dueDateMillis - System.currentTimeMillis()).coerceAtLeast(0)) }

    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            PageHeading(if (english) "My payments" else "Pembayaran saya", if (english) "Your bills, proof, and receipts" else "Tagihan, bukti, dan kuitansi kasmu")
        }
        item {
            StudentPanel {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(Modifier.size(44.dp).clip(CircleShape).background(if (nextBill == null) Color(0xFF168A62).copy(alpha = .14f) else Red.copy(alpha = .12f)), contentAlignment = Alignment.Center) {
                        Icon(if (nextBill == null) Icons.Default.CheckCircle else Icons.Default.Schedule, null, tint = if (nextBill == null) Color(0xFF168A62) else Red)
                    }
                    Column(Modifier.weight(1f)) {
                        Text(if (requests.isNotEmpty()) if (english) "Payment submitted" else "Pembayaran diajukan" else if (nextBill == null) if (english) "All caught up" else "Semua lunas" else if (english) "Payment due" else "Ada tagihan kas", fontWeight = FontWeight.Bold)
                        Text(if (requests.isNotEmpty()) if (english) "Waiting for treasurer review" else "Menunggu verifikasi bendahara" else nextBill?.title ?: if (english) "No outstanding bill" else "Tidak ada tagihan tertunda", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                    }
                    if (daysLeft != null) Text(if (english) "${daysLeft}d left" else "$daysLeft hari lagi", color = Red, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }
                Spacer(Modifier.height(16.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column { Text(if (english) "Contributed this term" else "Kontribusi semester ini", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp); Text(rupiah(contributions), fontWeight = FontWeight.ExtraBold, fontSize = 22.sp) }
                    Column(horizontalAlignment = Alignment.End) { Text(if (english) "On-time record" else "Pembayaran tepat waktu", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp); Text("2 / 3", color = Red, fontWeight = FontWeight.ExtraBold, fontSize = 22.sp) }
                }
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(onClick = { chosenBill = nextBill; showPayment = true }, enabled = nextBill != null, modifier = Modifier.weight(1f), shape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp), colors = ButtonDefaults.buttonColors(containerColor = Red)) {
                    Icon(Icons.Default.Payments, null, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(7.dp)); Text(if (english) "Pay bill" else "Bayar kas")
                }
                OutlinedButton(onClick = { showDispute = true }, modifier = Modifier.weight(1f), shape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp)) {
                    Icon(Icons.Default.SupportAgent, null, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(7.dp)); Text(if (english) "Dispute" else "Sanggah")
                }
            }
        }
        notice?.let { item { Text(it, color = Color(0xFF168A62), fontSize = 13.sp, modifier = Modifier.fillMaxWidth().background(Color(0xFF168A62).copy(alpha = .1f), BigShape).padding(14.dp)) } }
        item { PageSectionTitle(if (english) "Outstanding bills" else "Tagihan aktif") }
        if (unpaid.isEmpty()) item { EmptyState(if (english) "No bills due right now" else "Belum ada tagihan yang harus dibayar") }
        items(unpaid, key = { "bill-${it.id}" }) { bill ->
            StudentPanel {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(bill.title, fontWeight = FontWeight.Bold)
                        Text("${if (english) "Due" else "Jatuh tempo"} ${tanggal(bill.dueDateMillis)}", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                    }
                    Column(horizontalAlignment = Alignment.End) { Text(rupiah(bill.amount), color = Red, fontWeight = FontWeight.Bold); Text(if (english) "UNPAID" else "BELUM LUNAS", color = Red, fontSize = 10.sp, fontWeight = FontWeight.Bold) }
                }
                TextButton(onClick = { chosenBill = bill; showPayment = true }, contentPadding = PaddingValues(0.dp)) { Text(if (english) "Choose payment method →" else "Pilih metode pembayaran →", color = Red) }
            }
        }
        if (paidBills.isNotEmpty()) item { PageSectionTitle(if (english) "Digital receipts" else "Kuitansi digital") }
        items(paidBills, key = { "receipt-${it.id}" }) { bill ->
            StudentPanel(Modifier.clickable { selectedReceipt = bill }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(40.dp).clip(CircleShape).background(Color(0xFF168A62).copy(alpha = .12f)), contentAlignment = Alignment.Center) { Icon(Icons.Default.QrCode2, null, tint = Color(0xFF168A62)) }
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) { Text(bill.title, fontWeight = FontWeight.Bold); Text("Ref: KM-${bill.id.uppercase()}-DEMO", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    Column(horizontalAlignment = Alignment.End) { Text(rupiah(bill.amount), fontWeight = FontWeight.Bold); Text(if (english) "PAID" else "LUNAS", color = Color(0xFF168A62), fontSize = 10.sp, fontWeight = FontWeight.Bold) }
                }
            }
        }
        item { PageSectionTitle(if (english) "Personal ledger" else "Riwayat pembayaran pribadi") }
        items(requests, key = { it.id }) { request ->
            StudentPanel {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.HourglassTop, null, tint = Color(0xFFE19A1A))
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) { Text(request.title, fontWeight = FontWeight.Bold); Text("${request.method} · ${tanggal(request.createdAt)}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    Column(horizontalAlignment = Alignment.End) { Text(rupiah(request.amount), fontWeight = FontWeight.Bold); Text(if (english) "PENDING" else "MENUNGGU", color = Color(0xFFE19A1A), fontSize = 10.sp, fontWeight = FontWeight.Bold) }
                }
                if (request.proofAttached) Text(if (english) "Proof attached · manual verification" else "Bukti terlampir · menunggu verifikasi manual", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (requests.isEmpty()) item { EmptyState(if (english) "Submitted payments will appear here" else "Pembayaran yang kamu ajukan akan muncul di sini") }
        disputes.forEach { ticket -> item(key = ticket.id) { StudentPanel { Text("Tiket ${ticket.id}", color = Red, fontWeight = FontWeight.Bold); Text(ticket.message); Text(ticket.status, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp) } } }
        item { Spacer(Modifier.height(16.dp)) }
    }

    if (showPayment && chosenBill != null) {
        AlertDialog(
            onDismissRequest = { showPayment = false },
            title = { Text(if (english) "Pay ${chosenBill!!.title}" else "Bayar ${chosenBill!!.title}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(rupiah(chosenBill!!.amount), fontWeight = FontWeight.ExtraBold, color = Red, fontSize = 22.sp)
                    Text(if (english) "Choose a method" else "Pilih metode pembayaran", fontSize = 13.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("QRIS", "E-Wallet", "Tunai").forEach { option ->
                            FilterChip(selected = method == option, onClick = { method = option }, label = { Text(option, fontSize = 11.sp) })
                        }
                    }
                    Text(if (method == "Tunai") if (english) "Send a cash-record request to your treasurer." else "Kirim permintaan pencatatan tunai kepada bendahara." else if (english) "Demo checkout only. A school merchant account is required to collect real payments." else "Checkout demo. Akun merchant sekolah diperlukan untuk menerima pembayaran sungguhan.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    OutlinedButton(onClick = { imagePicker.launch("image/*") }, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Default.AddPhotoAlternate, null); Spacer(Modifier.width(8.dp)); Text(if (proof == null) if (english) "Attach transfer proof" else "Lampirkan bukti transfer" else if (english) "Proof selected" else "Bukti dipilih") }
                    proof?.let { Text(if (english) "Proof attached for treasurer review" else "Bukti terlampir untuk diperiksa bendahara", fontSize = 11.sp, color = Color(0xFF168A62)) }
                    Text(if (english) "OCR auto-verification is not connected in this demo." else "Verifikasi OCR otomatis belum terhubung pada demo ini.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            confirmButton = { Button(onClick = { onRequest(DemoPaymentRequest("PAY-${System.currentTimeMillis()}", chosenBill!!.title, chosenBill!!.amount, method, proof != null)); notice = if (english) "Request sent. Waiting for treasurer review." else "Permintaan terkirim. Menunggu konfirmasi bendahara."; showPayment = false }, colors = ButtonDefaults.buttonColors(containerColor = Red)) { Text(if (english) "Submit request" else "Kirim permintaan") } },
            dismissButton = { TextButton(onClick = { showPayment = false }) { Text(if (english) "Cancel" else "Batal") } }
        )
    }
    if (showDispute) {
        AlertDialog(
            onDismissRequest = { showDispute = false },
            title = { Text(if (english) "Report a payment issue" else "Ajukan sanggahan") },
            text = { Column(verticalArrangement = Arrangement.spacedBy(10.dp)) { Text(if (english) "Describe what needs to be checked. You can attach proof in the payment flow." else "Ceritakan catatan pembayaran yang perlu diperiksa.", fontSize = 13.sp); OutlinedTextField(disputeMessage, { disputeMessage = it }, label = { Text(if (english) "Message" else "Pesan") }, minLines = 3) } },
            confirmButton = { Button(onClick = { if (disputeMessage.isNotBlank()) { onDispute(DemoDispute("TKT-${System.currentTimeMillis().toString().takeLast(5)}", disputeMessage)); disputeMessage = ""; notice = if (english) "Ticket sent to your treasurer." else "Tiket dikirim ke bendahara."; showDispute = false } }, colors = ButtonDefaults.buttonColors(containerColor = Red)) { Text(if (english) "Send ticket" else "Kirim tiket") } },
            dismissButton = { TextButton(onClick = { showDispute = false }) { Text(if (english) "Cancel" else "Batal") } }
        )
    }
    selectedReceipt?.let { bill ->
        AlertDialog(
            onDismissRequest = { selectedReceipt = null },
            icon = { Icon(Icons.Default.QrCode2, null, tint = Red, modifier = Modifier.size(42.dp)) },
            title = { Text(if (english) "Digital receipt" else "Kuitansi digital") },
            text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { Text(bill.title, fontWeight = FontWeight.Bold); Text(rupiah(bill.amount), fontSize = 20.sp, color = Red, fontWeight = FontWeight.ExtraBold); Text("Ref: KM-${bill.id.uppercase()}-DEMO"); Text(if (english) "Demo receipt. Verifiable QR signatures will be issued by the connected class system." else "Kuitansi demo. QR verifikasi resmi diterbitkan oleh sistem kas kelas saat backend terhubung.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) } },
            confirmButton = { TextButton(onClick = { selectedReceipt = null }) { Text("OK") } }
        )
    }
}

@Composable
fun StudentLedgerScreen(summary: KasSummary, english: Boolean) {
    var query by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Semua") }
    var ageFilter by remember { mutableStateOf("Semua") }
    var selectedExpense by remember { mutableStateOf<Expense?>(null) }
    val categories = listOf("Semua", "Perlengkapan", "Akademik")
    val ages = listOf("Semua", "7 hari", "30 hari")
    val filtered = summary.expenses.filter { expense ->
        val cat = if (expense.description.contains("foto", true)) "Akademik" else "Perlengkapan"
        val ageDays = TimeUnit.MILLISECONDS.toDays((System.currentTimeMillis() - expense.dateMillis).coerceAtLeast(0))
        expense.description.contains(query, true) && (category == "Semua" || cat == category) && when (ageFilter) { "7 hari" -> ageDays <= 7; "30 hari" -> ageDays <= 30; else -> true }
    }
    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(13.dp)) {
        item { PageHeading(if (english) "Class transparency" else "Transparansi kas", if (english) "See how class funds are used" else "Lihat aliran dana kelas secara terbuka") }
        item {
            Column(Modifier.fillMaxWidth().clip(BigShape).background(Brush.linearGradient(listOf(DeepRed, Red, SoftRed))).padding(20.dp)) {
                Text(if (english) "LIVE CLASS BALANCE" else "SALDO KAS KELAS", color = Color.White.copy(alpha = .85f), fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                Text(rupiah(summary.totalBalance), color = Color.White, fontSize = 29.sp, fontWeight = FontWeight.ExtraBold)
                Text(if (english) "Updated with the latest demo ledger" else "Diperbarui dari buku kas demo", color = Color.White.copy(alpha = .8f), fontSize = 12.sp)
            }
        }
        item { PageSectionTitle(if (english) "Find a class expense" else "Cari pengeluaran kelas") }
        item { OutlinedTextField(query, { query = it }, modifier = Modifier.fillMaxWidth(), shape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp), label = { Text(if (english) "Search description" else "Cari keterangan") }, leadingIcon = { Icon(Icons.Default.Search, null) }, singleLine = true) }
        item { FilterStrip(categories, category) { category = it } }
        item { FilterStrip(ages, ageFilter) { ageFilter = it } }
        item { PageSectionTitle(if (english) "Income and expenses" else "Rincian transaksi kelas") }
        if (filtered.isEmpty()) item { EmptyState(if (english) "No transactions match those filters" else "Tidak ada transaksi yang cocok") }
        items(filtered, key = { it.id }) { expense ->
            StudentPanel(Modifier.clickable { selectedExpense = expense }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(42.dp).clip(CircleShape).background(Red.copy(alpha = .1f)), contentAlignment = Alignment.Center) { Icon(Icons.Default.ReceiptLong, null, tint = Red) }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) { Text(expense.description, fontWeight = FontWeight.Bold); Text("${tanggal(expense.dateMillis)} · ${expense.noteBy}", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp) }
                    Text("− ${rupiah(expense.amount)}", color = MaterialTheme.colorScheme.tertiary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
                Text(if (english) "Tap to view expense details and receipt status" else "Ketuk untuk melihat detail dan status nota", color = Red, fontSize = 10.sp, modifier = Modifier.padding(start = 54.dp, top = 6.dp))
            }
        }
        item { ClassMissionCard(english) }
        item { PageSectionTitle(if (english) "Your achievements" else "Badge pencapaianmu") }
        item { Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) { AchievementCard(Modifier.weight(1f), "🏅", if (english) "On-time star" else "Sultan tepat waktu", "2/3"); AchievementCard(Modifier.weight(1f), "💝", if (english) "Loyal donor" else "Donatur setia", "1x") } }
        item { LeaderboardCard(english) }
        item { Spacer(Modifier.height(14.dp)) }
    }
    selectedExpense?.let { expense ->
        AlertDialog(onDismissRequest = { selectedExpense = null }, icon = { Icon(Icons.Default.ReceiptLong, null, tint = Red) }, title = { Text(if (english) "Expense details" else "Detail pengeluaran") }, text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { Text(expense.description, fontWeight = FontWeight.Bold); Text("${if (english) "Amount" else "Nominal"}: ${rupiah(expense.amount)}"); Text("${if (english) "Date" else "Tanggal"}: ${tanggal(expense.dateMillis)}"); Text("${if (english) "Recorded by" else "Dicatat oleh"}: ${expense.noteBy}"); Text(if (english) "No digital receipt is attached to this demo entry yet." else "Nota digital belum terlampir pada transaksi demo ini.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) } }, confirmButton = { TextButton(onClick = { selectedExpense = null }) { Text("OK") } })
    }
}

@Composable
private fun ClassMissionCard(english: Boolean) {
    StudentPanel {
        Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.Flag, null, tint = Red); Spacer(Modifier.width(9.dp)); Text(if (english) "Class meeting fund" else "Misi: Dana classmeeting", fontWeight = FontWeight.Bold) }
        Spacer(Modifier.height(10.dp))
        Text(if (english) "Rp 680,000 of Rp 1,000,000" else "Rp 680.000 dari Rp 1.000.000", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
        Spacer(Modifier.height(8.dp))
        LinearProgressIndicator(progress = { .68f }, modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape), color = Red, trackColor = Red.copy(alpha = .12f))
    }
}

@Composable
private fun AchievementCard(modifier: Modifier, emoji: String, title: String, progress: String) {
    StudentPanel(modifier) {
        Text(emoji, fontSize = 24.sp)
        Text(title, fontWeight = FontWeight.Bold, fontSize = 12.sp)
        Text(progress, color = Red, fontWeight = FontWeight.Bold, fontSize = 11.sp)
    }
}

@Composable
private fun LeaderboardCard(english: Boolean) {
    StudentPanel {
        Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.Leaderboard, null, tint = Red); Spacer(Modifier.width(9.dp)); Column { Text(if (english) "Class leaderboard" else "Papan peringkat kelas", fontWeight = FontWeight.Bold); Text(if (english) "Anonymous view" else "Nama disamarkan untuk menjaga privasi", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) } }
        listOf("🥇  Siswa A" to "5 kali", "🥈  Siswa B" to "4 kali", "🥉  Kamu" to "2 kali").forEach { (name, score) ->
            Row(Modifier.fillMaxWidth().padding(top = 9.dp), horizontalArrangement = Arrangement.SpaceBetween) { Text(name, fontSize = 13.sp); Text(score, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp) }
        }
    }
}

@Composable
fun KasMateAssistantScreen(summary: KasSummary, userName: String, english: Boolean) {
    data class Message(val fromUser: Boolean, val text: String)
    val messages = remember(userName) { mutableStateListOf(Message(false, if (english) "Hi $userName! I can help check bills, class balance, and expense records. This demo assistant uses local sample data." else "Halo $userName! Aku bisa bantu cek tagihan, saldo kelas, dan catatan pengeluaran. Asisten demo ini memakai data contoh di perangkat.")) }
    var input by remember { mutableStateOf("") }
    fun ask(question: String) {
        if (question.isBlank()) return
        messages.add(Message(true, question))
        messages.add(Message(false, localAssistantAnswer(question, summary, english)))
        input = ""
    }
    val overdueBill = summary.myBills.firstOrNull {
        it.status == PaymentStatus.BELUM && it.dueDateMillis < System.currentTimeMillis()
    }
    val hasUnpaidBill = summary.myBills.any { it.status == PaymentStatus.BELUM }
    val hasPaidBill = summary.myBills.any { it.status == PaymentStatus.LUNAS }
    val assistantMood = when {
        overdueBill != null -> MascotMood.SAD
        !hasUnpaidBill && hasPaidBill -> MascotMood.HAPPY
        else -> MascotMood.NEUTRAL
    }
    Column(Modifier.fillMaxSize().padding(horizontal = 18.dp)) {
        PageHeading(if (english) "KasMate assistant" else "Asisten KasMate", if (english) "A private guide to class finances" else "Teman untuk memahami uang kas kelas")
        KasMateMascotBanner(
            mood = assistantMood,
            assistantLabel = if (english) "KasMate AI · Demo" else "KasMate AI · Demo lokal",
            message = when (assistantMood) {
                MascotMood.SAD -> if (english) "A class bill is past due. Ask me what is still unpaid." else "Ada tagihan kas yang lewat tempo. Tanya aku untuk melihat yang belum dibayar."
                MascotMood.HAPPY -> if (english) "Your dues are paid! ${financeFunFact(true)}" else "Kasmu lunas! ${financeFunFact(false)}"
                else -> if (english) "Ask me about your bills, the class balance, or a fun finance fact." else "Tanya soal tagihan, saldo kelas, atau minta fun fact keuangan!"
            }
        )
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(vertical = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(if (english) "My bill?" else "Tagihanku?", if (english) "Class balance?" else "Saldo kelas?", if (english) "Recent expense?" else "Pengeluaran terbaru?", if (english) "Fun fact" else "Fun fact").forEach { prompt -> AssistChip(onClick = { ask(prompt) }, label = { Text(prompt, fontSize = 11.sp) }) }
        }
        LazyColumn(Modifier.weight(1f).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(bottom = 10.dp)) {
            items(messages.size) { index ->
                val message = messages[index]
                Row(Modifier.fillMaxWidth(), horizontalArrangement = if (message.fromUser) Arrangement.End else Arrangement.Start) {
                    Text(message.text, modifier = Modifier.widthIn(max = 300.dp).clip(androidx.compose.foundation.shape.RoundedCornerShape(18.dp)).background(if (message.fromUser) Red else MaterialTheme.colorScheme.surface).padding(14.dp), color = if (message.fromUser) Color.White else MaterialTheme.colorScheme.onSurface, fontSize = 13.sp)
                }
            }
        }
        Row(Modifier.fillMaxWidth().padding(bottom = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(input, { input = it }, modifier = Modifier.weight(1f), shape = androidx.compose.foundation.shape.RoundedCornerShape(18.dp), placeholder = { Text(if (english) "Ask about class funds" else "Tanya tentang uang kas") }, maxLines = 3)
            IconButton(onClick = { ask(input) }, enabled = input.isNotBlank(), modifier = Modifier.size(52.dp).clip(CircleShape).background(Red)) { Icon(Icons.Default.Send, null, tint = Color.White) }
        }
    }
}

private fun localAssistantAnswer(question: String, summary: KasSummary, english: Boolean): String {
    val q = question.lowercase()
    return when {
        listOf("fun fact", "fakta", "tips").any(q::contains) -> financeFunFact(english)
        listOf("saldo", "balance").any(q::contains) -> if (english) "The class balance recorded in this demo is ${rupiah(summary.totalBalance)}." else "Saldo kas kelas yang tercatat di demo ini ${rupiah(summary.totalBalance)}."
        listOf("tagihan", "bill", "bayar", "pay").any(q::contains) -> {
            val bill = summary.myBills.firstOrNull { it.status == PaymentStatus.BELUM }
            if (bill == null) if (english) "You have no unpaid bill in the current sample data." else "Tidak ada tagihan belum lunas pada data contoh saat ini."
            else if (english) "${bill.title} is ${rupiah(bill.amount)}, due ${tanggal(bill.dueDateMillis)}. Use My Payments to send a request; the merchant gateway is not connected in this demo." else "${bill.title} sebesar ${rupiah(bill.amount)}, jatuh tempo ${tanggal(bill.dueDateMillis)}. Buka Pembayaran untuk mengirim permintaan; gateway merchant belum terhubung di demo ini."
        }
        listOf("pengeluaran", "expense", "nota", "receipt").any(q::contains) -> {
            val recent = summary.expenses.firstOrNull()
            if (recent == null) if (english) "There are no expense records yet." else "Belum ada catatan pengeluaran."
            else if (english) "The latest expense is ${recent.description}, ${rupiah(recent.amount)}, recorded ${tanggal(recent.dateMillis)}. Open Class to see its details." else "Pengeluaran terbaru: ${recent.description}, ${rupiah(recent.amount)}, dicatat ${tanggal(recent.dateMillis)}. Buka Kas Kelas untuk detailnya."
        }
        listOf("sanggah", "dispute", "salah", "wrong").any(q::contains) -> if (english) "Open My Payments and choose Dispute to create a review ticket for the treasurer." else "Buka Pembayaran Saya lalu pilih Sanggah untuk membuat tiket pemeriksaan kepada bendahara."
        else -> if (english) "I can help with your bill, class balance, expenses, or payment disputes. Ask a specific question to get an answer from the current demo data." else "Aku bisa bantu soal tagihan, saldo kelas, pengeluaran, atau sanggahan pembayaran. Tanyakan hal spesifik agar aku bisa menjawab dari data demo yang tersedia."
    }
}

fun financeFunFact(english: Boolean): String {
    val facts = if (english) listOf(
        "Fun fact: small, regular contributions make class plans easier to budget.",
        "Money tip: saving receipts makes it easier to track every class expense.",
        "Fun fact: a shared budget helps everyone see how a class goal is progressing."
    ) else listOf(
        "Fun fact: iuran kecil yang rutin bikin rencana kelas lebih mudah diatur.",
        "Tips kas: simpan bukti transaksi supaya setiap pengeluaran mudah dilacak.",
        "Fun fact: anggaran terbuka bikin progres target kelas bisa dipantau bersama."
    )
    return facts[java.util.Calendar.getInstance().get(java.util.Calendar.DAY_OF_YEAR) % facts.size]
}

@Composable
private fun FilterStrip(options: List<String>, selected: String, onSelect: (String) -> Unit) {
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { option -> FilterChip(selected = selected == option, onClick = { onSelect(option) }, label = { Text(option, fontSize = 11.sp) }) }
    }
}

@Composable
fun StudentPanel(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier.fillMaxWidth().clip(BigShape).background(MaterialTheme.colorScheme.surface).padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp), content = content)
}

@Composable
fun PageHeading(title: String, subtitle: String) {
    Column(Modifier.fillMaxWidth().padding(top = 22.dp, bottom = 4.dp)) { Text(title, fontSize = 25.sp, fontWeight = FontWeight.ExtraBold); Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp) }
}

@Composable
fun PageSectionTitle(title: String) { Text(title, fontSize = 17.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 4.dp)) }

@Composable
fun EmptyState(text: String) {
    Text(text, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp, modifier = Modifier.fillMaxWidth().clip(BigShape).background(MaterialTheme.colorScheme.surface).padding(18.dp))
}
