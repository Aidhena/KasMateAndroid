package com.smktel.kasmate.ui

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smktel.kasmate.data.*
import java.util.Locale
import java.util.concurrent.TimeUnit

data class DemoDispute(val id: String, val message: String, val status: String = "Demo lokal")

private fun paymentMethodLabel(method: String): String = when (method.uppercase()) {
    "QRIS" -> "QRIS"
    "TF" -> "Transfer / e-wallet"
    else -> "Tunai"
}

private data class PaymentProofFile(val name: String, val type: String)

private fun paymentProofFileInfo(context: Context, uri: Uri): PaymentProofFile {
    val name = context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
        ?.use { cursor ->
            val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (cursor.moveToFirst() && index >= 0) cursor.getString(index) else null
        }
        ?.takeIf(String::isNotBlank)
        ?: uri.lastPathSegment?.substringAfterLast('/')?.ifBlank { null }
        ?: "payment-proof"
    val mime = context.contentResolver.getType(uri).orEmpty().lowercase(Locale.ROOT)
    val extension = name.substringAfterLast('.', "").lowercase(Locale.ROOT)
    val type = when {
        extension == "pdf" || mime == "application/pdf" -> "PDF"
        extension == "png" || mime == "image/png" -> "PNG"
        extension in setOf("jpg", "jpeg", "webp") || mime.startsWith("image/") -> "IMG"
        else -> "FILE"
    }
    return PaymentProofFile(name, type)
}

private fun proofFileType(fileName: String): String = when (
    fileName.substringAfterLast('.', "").lowercase(Locale.ROOT)
) {
    "pdf" -> "PDF"
    "png" -> "PNG"
    "jpg", "jpeg", "webp" -> "IMG"
    else -> "FILE"
}

@Composable
private fun PaymentProofFileIcon(type: String, modifier: Modifier = Modifier) {
    val isPdf = type == "PDF"
    Surface(
        modifier = modifier.size(46.dp),
        shape = RoundedCornerShape(11.dp),
        color = (if (isPdf) Red else Color(0xFF168A62)).copy(alpha = .12f)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Icon(
                if (isPdf) Icons.Default.PictureAsPdf else Icons.Default.Image,
                contentDescription = type,
                modifier = Modifier.size(22.dp),
                tint = if (isPdf) Red else Color(0xFF168A62)
            )
            Text(type, fontSize = 8.sp, fontWeight = FontWeight.Bold, color = if (isPdf) Red else Color(0xFF168A62))
        }
    }
}

@Composable
fun StudentPaymentScreen(
    summary: KasSummary,
    english: Boolean,
    requests: List<StudentPaymentRequest>,
    disputes: List<DemoDispute>,
    onSubmitPayment: (Long, String, Uri?, (String?) -> Unit) -> Unit,
    onDispute: (DemoDispute) -> Unit
) {
    val unpaid = summary.myBills.filter { it.amountDue > 0 && it.status != PaymentStatus.PROSES }
    val pendingBillIds = requests.filter { it.status == "PENDING" }.map { it.billStudentId }.toSet()
    val nextBill = unpaid.filter { bill -> bill.id.toLongOrNull()?.let(pendingBillIds::contains) != true }
        .minByOrNull { it.dueDateMillis }
    val newestPendingRequest = requests.firstOrNull { it.status == "PENDING" }
    var showPayment by remember { mutableStateOf(false) }
    var showDispute by remember { mutableStateOf(false) }
    var chosenBill by remember { mutableStateOf(nextBill) }
    var selectedReceiptId by remember { mutableStateOf<String?>(null) }
    var method by remember { mutableStateOf("QRIS") }
    var proof by remember { mutableStateOf<Uri?>(null) }
    var isSubmitting by remember { mutableStateOf(false) }
    var submitError by remember { mutableStateOf<String?>(null) }
    var disputeMessage by remember { mutableStateOf("") }
    var notice by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    val proofFile = remember(proof, context) { proof?.let { paymentProofFileInfo(context, it) } }
    val proofPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { proof = it }
    val contributions = summary.myBills.sumOf { it.amountPaid }
    val settledBills = summary.myBills.filter { it.status == PaymentStatus.LUNAS }
    val receiptBills = summary.myBills.filter { it.amountPaid > 0 }
    val selectedReceipt = selectedReceiptId?.let { receiptId ->
        receiptBills.firstOrNull { it.id == receiptId }
    }
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
                        Text(if (newestPendingRequest != null) if (english) "Payment awaiting review" else "Menunggu verifikasi admin" else if (nextBill == null) if (unpaid.isEmpty()) if (english) "All caught up" else "Semua lunas" else if (english) "Request submitted" else "Permintaan sedang diproses" else if (english) "Payment due" else "Ada tagihan kas", fontWeight = FontWeight.Bold)
                        Text(newestPendingRequest?.billName ?: nextBill?.title ?: if (english) "No outstanding bill" else "Tidak ada tagihan tertunda", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                    }
                    if (daysLeft != null) Text(if (english) "${daysLeft}d left" else "$daysLeft hari lagi", color = Red, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }
                Spacer(Modifier.height(16.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column { Text(if (english) "Contributed this term" else "Kontribusi semester ini", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp); Text(rupiah(contributions), fontWeight = FontWeight.ExtraBold, fontSize = 22.sp) }
                    Column(horizontalAlignment = Alignment.End) { Text(if (english) "Paid bills" else "Tagihan lunas", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp); Text("${settledBills.size} / ${summary.myBills.size}", color = Red, fontWeight = FontWeight.ExtraBold, fontSize = 22.sp) }
                }
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(onClick = { chosenBill = nextBill; method = "QRIS"; proof = null; submitError = null; showPayment = true }, enabled = nextBill != null, modifier = Modifier.weight(1f), shape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp), colors = ButtonDefaults.buttonColors(containerColor = Red)) {
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
                    Column(horizontalAlignment = Alignment.End) { Text(rupiah(bill.amountDue), color = Red, fontWeight = FontWeight.Bold); Text(if (bill.status == PaymentStatus.SEBAGIAN) if (english) "PARTIAL" else "SEBAGIAN" else if (english) "UNPAID" else "BELUM LUNAS", color = Red, fontSize = 10.sp, fontWeight = FontWeight.Bold) }
                }
                if (bill.id.toLongOrNull()?.let(pendingBillIds::contains) == true) {
                    Text(if (english) "Payment request awaiting admin review" else "Permintaan menunggu verifikasi admin", color = Color(0xFFE19A1A), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                } else {
                    TextButton(onClick = { chosenBill = bill; method = "QRIS"; proof = null; submitError = null; showPayment = true }, contentPadding = PaddingValues(0.dp)) { Text(if (english) "Choose payment method →" else "Pilih metode pembayaran →", color = Red) }
                }
            }
        }
        if (receiptBills.isNotEmpty()) item { PageSectionTitle(if (english) "Digital receipts" else "Kuitansi digital") }
        items(receiptBills, key = { "receipt-${it.id}" }) { bill ->
            StudentPanel(Modifier.clickable { selectedReceiptId = bill.id }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(40.dp).clip(CircleShape).background(Color(0xFF168A62).copy(alpha = .12f)), contentAlignment = Alignment.Center) { Icon(Icons.Default.QrCode2, null, tint = Color(0xFF168A62)) }
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) { Text(bill.title, fontWeight = FontWeight.Bold); Text("Ref: KM-${bill.id.uppercase()}", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(rupiah(bill.amountPaid), fontWeight = FontWeight.Bold)
                        val isPaid = bill.status == PaymentStatus.LUNAS
                        Text(
                            if (isPaid) if (english) "PAID" else "LUNAS" else if (english) "PARTIAL" else "SEBAGIAN",
                            color = if (isPaid) Color(0xFF168A62) else Color(0xFFE19A1A),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
        item { PageSectionTitle(if (english) "Payment request history" else "Riwayat permintaan pembayaran") }
        items(requests, key = { it.id }) { request ->
            StudentPanel {
                val statusColor = when (request.status) {
                    "APPROVED" -> Color(0xFF168A62)
                    "REJECTED" -> Red
                    else -> Color(0xFFE19A1A)
                }
                val statusLabel = when (request.status) {
                    "APPROVED" -> if (english) "APPROVED" else "DISETUJUI"
                    "REJECTED" -> if (english) "REJECTED" else "DITOLAK"
                    else -> if (english) "PENDING REVIEW" else "MENUNGGU VERIFIKASI"
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(if (request.status == "APPROVED") Icons.Default.CheckCircle else if (request.status == "REJECTED") Icons.Default.Cancel else Icons.Default.HourglassTop, null, tint = statusColor)
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) { Text(request.billName, fontWeight = FontWeight.Bold); Text("${paymentMethodLabel(request.method)} · ${tanggal(request.createdAtMillis)}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    Column(horizontalAlignment = Alignment.End) { Text(rupiah(request.amount), fontWeight = FontWeight.Bold); Text(statusLabel, color = statusColor, fontSize = 10.sp, fontWeight = FontWeight.Bold) }
                }
                if (request.proofAttached) {
                    Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        PaymentProofFileIcon(proofFileType(request.proofFileName))
                        Spacer(Modifier.width(9.dp))
                        Column(Modifier.weight(1f)) {
                            Text(request.proofFileName.ifBlank { if (english) "Payment proof" else "Bukti pembayaran" }, maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            Text(if (request.status == "PENDING") if (english) "Awaiting admin review" else "Menunggu verifikasi admin" else if (english) "Payment proof attached" else "Bukti pembayaran terlampir", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                if (request.reviewNote.isNotBlank() && request.status != "PENDING") Text(request.reviewNote, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 6.dp))
            }
        }
        if (requests.isEmpty()) item { EmptyState(if (english) "Payment requests will appear here" else "Permintaan pembayaranmu akan muncul di sini") }
        disputes.forEach { ticket -> item(key = ticket.id) { StudentPanel { Text("Tiket ${ticket.id}", color = Red, fontWeight = FontWeight.Bold); Text(ticket.message); Text(ticket.status, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp) } } }
        item { Spacer(Modifier.height(16.dp)) }
    }

    if (showPayment && chosenBill != null) {
        AlertDialog(
            onDismissRequest = { if (!isSubmitting) showPayment = false },
            title = { Text(if (english) "Pay ${chosenBill!!.title}" else "Bayar ${chosenBill!!.title}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(rupiah(chosenBill!!.amountDue), fontWeight = FontWeight.ExtraBold, color = Red, fontSize = 22.sp)
                    Text(if (english) "Choose a method" else "Pilih metode pembayaran", fontSize = 13.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("QRIS" to "QRIS", "TF" to "Transfer / e-wallet", "TUNAI" to "Tunai").forEach { (code, label) ->
                            FilterChip(selected = method == code, onClick = { method = code }, label = { Text(label, fontSize = 11.sp) })
                        }
                    }
                    Text(if (english) "Choose the payment method and attach a proof image for the class administrator to review." else "Pilih metode pembayaran dan lampirkan foto bukti untuk diperiksa admin kelas.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    OutlinedButton(onClick = { proofPicker.launch(arrayOf("image/jpeg", "image/png", "image/webp", "application/pdf")) }, modifier = Modifier.fillMaxWidth(), enabled = !isSubmitting) {
                        Icon(Icons.Default.AttachFile, null)
                        Spacer(Modifier.width(8.dp))
                        Text(if (proof == null) if (english) "Attach proof file (required)" else "Lampirkan bukti pembayaran (wajib)" else if (english) "Change proof file" else "Ganti file bukti")
                    }
                    proofFile?.let { file ->
                        Row(
                            Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .45f)).padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            PaymentProofFileIcon(file.type)
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(file.name, maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                Text(if (english) "Ready to send for admin review" else "Siap dikirim untuk diperiksa admin", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            IconButton(onClick = { proof = null }, enabled = !isSubmitting) {
                                Icon(Icons.Default.Close, contentDescription = if (english) "Remove proof" else "Hapus bukti")
                            }
                        }
                    }
                    submitError?.let { Text(it, color = Red, fontSize = 12.sp) }
                }
            },
            confirmButton = { Button(onClick = {
                val bill = chosenBill ?: return@Button
                isSubmitting = true
                submitError = null
                onSubmitPayment(bill.id.toLongOrNull() ?: 0L, method, proof) { error ->
                    isSubmitting = false
                    if (error == null) {
                        notice = if (english) "Request sent. The bill will update after admin approval." else "Permintaan dikirim. Tagihan diperbarui setelah disetujui admin."
                        showPayment = false
                        proof = null
                    } else {
                        submitError = error
                    }
                }
            }, enabled = !isSubmitting && proof != null, colors = ButtonDefaults.buttonColors(containerColor = Red)) {
                Text(if (isSubmitting) if (english) "Sending..." else "Mengirim..." else if (english) "Send payment request" else "Kirim permintaan pembayaran")
            } },
            dismissButton = { TextButton(onClick = { showPayment = false }) { Text(if (english) "Cancel" else "Batal") } }
        )
    }
    if (showDispute) {
        AlertDialog(
            onDismissRequest = { showDispute = false },
            title = { Text(if (english) "Report a payment issue" else "Ajukan sanggahan") },
            text = { Column(verticalArrangement = Arrangement.spacedBy(10.dp)) { Text(if (english) "Describe what needs to be checked. You can attach proof in the payment flow." else "Ceritakan catatan pembayaran yang perlu diperiksa.", fontSize = 13.sp); OutlinedTextField(disputeMessage, { disputeMessage = it }, label = { Text(if (english) "Message" else "Pesan") }, minLines = 3) } },
            confirmButton = { Button(onClick = { if (disputeMessage.isNotBlank()) { onDispute(DemoDispute("TKT-${System.currentTimeMillis().toString().takeLast(5)}", disputeMessage)); disputeMessage = ""; notice = if (english) "Demo ticket saved on this device only." else "Tiket demo hanya tersimpan di perangkat ini."; showDispute = false } }, colors = ButtonDefaults.buttonColors(containerColor = Red)) { Text(if (english) "Save demo ticket" else "Simpan tiket demo") } },
            dismissButton = { TextButton(onClick = { showDispute = false }) { Text(if (english) "Cancel" else "Batal") } }
        )
    }
    selectedReceipt?.let { bill ->
        AlertDialog(
            onDismissRequest = { selectedReceiptId = null },
            icon = { Icon(Icons.Default.QrCode2, null, tint = Red, modifier = Modifier.size(42.dp)) },
            title = { Text(if (english) "Digital receipt" else "Kuitansi digital") },
            text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { Text(bill.title, fontWeight = FontWeight.Bold); Text(rupiah(bill.amountPaid), fontSize = 20.sp, color = Red, fontWeight = FontWeight.ExtraBold); Text("Ref: KM-${bill.id.uppercase()}"); Text(if (english) "This receipt reflects the payment recorded by your class administrator." else "Kuitansi ini mengikuti pembayaran yang dicatat oleh admin kelas.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) } },
            confirmButton = { TextButton(onClick = { selectedReceiptId = null }) { Text("OK") } }
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
                Text(if (english) "Updated from the class account" else "Diperbarui dari data kas kelas", color = Color.White.copy(alpha = .8f), fontSize = 12.sp)
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
        item { PaymentProgressCard(summary, english) }
        item { Spacer(Modifier.height(14.dp)) }
    }
    selectedExpense?.let { expense ->
        AlertDialog(onDismissRequest = { selectedExpense = null }, icon = { Icon(Icons.Default.ReceiptLong, null, tint = Red) }, title = { Text(if (english) "Expense details" else "Detail pengeluaran") }, text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { Text(expense.description, fontWeight = FontWeight.Bold); Text("${if (english) "Amount" else "Nominal"}: ${rupiah(expense.amount)}"); Text("${if (english) "Date" else "Tanggal"}: ${tanggal(expense.dateMillis)}"); Text("${if (english) "Recorded by" else "Dicatat oleh"}: ${expense.noteBy}"); Text(if (english) "No digital receipt is attached to this class record." else "Nota digital belum terlampir pada transaksi kas ini.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) } }, confirmButton = { TextButton(onClick = { selectedExpense = null }) { Text("OK") } })
    }
}

@Composable
private fun PaymentProgressCard(summary: KasSummary, english: Boolean) {
    val totalAmount = summary.myBills.sumOf { it.amountPaid + it.amountDue }
    val paidAmount = summary.myBills.sumOf { it.amountPaid }
    val progress = if (totalAmount > 0) {
        (paidAmount.toFloat() / totalAmount.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }
    val paidCount = summary.myBills.count { it.status == PaymentStatus.LUNAS }
    val contributedCount = summary.myBills.count { it.amountPaid > 0 }

    StudentPanel {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Flag, contentDescription = null, tint = Red)
            Spacer(Modifier.width(9.dp))
            Text(
                if (english) "Your payment progress" else "Progres pembayaranmu",
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                if (english) "Amount paid" else "Jumlah dibayar",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp
            )
            Text(
                if (totalAmount > 0) "${rupiah(paidAmount)} / ${rupiah(totalAmount)}" else "—",
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
        Spacer(Modifier.height(8.dp))
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape),
            color = Red,
            trackColor = Red.copy(alpha = .12f)
        )
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column {
                Text(if (english) "Bills paid" else "Tagihan lunas", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                Text("$paidCount / ${summary.myBills.size}", color = Red, fontWeight = FontWeight.Bold)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(if (english) "Bills contributed to" else "Tagihan pernah dibayar", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                Text("$contributedCount", color = Red, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun KasMateAssistantScreen(summary: KasSummary, userName: String, english: Boolean) {
    data class Message(val fromUser: Boolean, val text: String)
    val messages = remember(userName) { mutableStateListOf(Message(false, if (english) "Hi $userName! I can help check your bills, class balance, and expense records." else "Halo $userName! Aku bisa bantu cek tagihan, saldo kelas, dan catatan pengeluaran.")) }
    var input by remember { mutableStateOf("") }
    fun ask(question: String) {
        if (question.isBlank()) return
        messages.add(Message(true, question))
        messages.add(Message(false, localAssistantAnswer(question, summary, english)))
        input = ""
    }
    val overdueBill = summary.myBills.firstOrNull {
        it.amountDue > 0 && it.status != PaymentStatus.PROSES && it.dueDateMillis < System.currentTimeMillis()
    }
    val overdueDays = overdueBill?.let { ((System.currentTimeMillis() - it.dueDateMillis) / 86_400_000L).toInt().coerceAtLeast(1) } ?: 0
    val hasUnpaidBill = summary.myBills.any { it.amountDue > 0 && it.status != PaymentStatus.PROSES }
    val hasPaidBill = summary.myBills.any { it.status == PaymentStatus.LUNAS }
    val assistantMood = when {
        overdueDays >= 7 -> MascotMood.ANGRY
        overdueBill != null -> MascotMood.SAD
        !hasUnpaidBill && hasPaidBill -> MascotMood.HAPPY
        else -> MascotMood.NEUTRAL
    }
    Column(Modifier.fillMaxSize().padding(horizontal = 18.dp)) {
        PageHeading(if (english) "KasMate assistant" else "Asisten KasMate", if (english) "A private guide to class finances" else "Teman untuk memahami uang kas kelas")
        KasMateMascotBanner(
            mood = assistantMood,
            assistantLabel = "KasMate AI",
            message = when (assistantMood) {
                MascotMood.ANGRY -> if (english) "I'm worried: ${rupiah(overdueBill?.amount ?: 0)} has been overdue for $overdueDays days. Let's fix it." else "Aku khawatir: kas ${rupiah(overdueBill?.amount ?: 0)} sudah menunggak $overdueDays hari. Yuk, kita bereskan."
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
        listOf("saldo", "balance").any(q::contains) -> if (english) "The current class balance is ${rupiah(summary.totalBalance)}." else "Saldo kas kelas saat ini ${rupiah(summary.totalBalance)}."
        listOf("tagihan", "bill", "bayar", "pay").any(q::contains) -> {
            val bill = summary.myBills.firstOrNull { it.amountDue > 0 && it.status != PaymentStatus.PROSES }
            if (bill == null) if (english) "You have no unpaid bill right now." else "Tidak ada tagihan belum lunas saat ini."
            else if (english) "${bill.title} has ${rupiah(bill.amountDue)} remaining, due ${tanggal(bill.dueDateMillis)}." else "Sisa ${bill.title} sebesar ${rupiah(bill.amountDue)}, jatuh tempo ${tanggal(bill.dueDateMillis)}."
        }
        listOf("pengeluaran", "expense", "nota", "receipt").any(q::contains) -> {
            val recent = summary.expenses.firstOrNull()
            if (recent == null) if (english) "There are no expense records yet." else "Belum ada catatan pengeluaran."
            else if (english) "The latest expense is ${recent.description}, ${rupiah(recent.amount)}, recorded ${tanggal(recent.dateMillis)}. Open Class to see its details." else "Pengeluaran terbaru: ${recent.description}, ${rupiah(recent.amount)}, dicatat ${tanggal(recent.dateMillis)}. Buka Kas Kelas untuk detailnya."
        }
        listOf("sanggah", "dispute", "salah", "wrong").any(q::contains) -> if (english) "The dispute form is a local demo and does not send a ticket to staff yet. Contact your class treasurer directly." else "Formulir sanggahan masih demo lokal dan belum mengirim tiket ke admin. Hubungi bendahara kelas secara langsung."
        else -> if (english) "I can help with your bill, class balance, or expenses. Ask a specific question about your class account." else "Aku bisa bantu soal tagihan, saldo kelas, atau pengeluaran. Tanyakan hal spesifik tentang akun kas kelasmu."
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
