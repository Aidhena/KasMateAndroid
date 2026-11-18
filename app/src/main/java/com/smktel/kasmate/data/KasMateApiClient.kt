package com.smktel.kasmate.data

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.DataOutputStream
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

data class StudentProfile(
    val id: Long,
    val name: String,
    val nis: String,
    val className: String,
    val academicYear: String,
    val savingsBalance: Long
)

data class StudentLogin(val token: String)

data class StudentNotification(
    val id: Long,
    val title: String,
    val message: String
)

data class StudentDashboardData(
    val profile: StudentProfile,
    val summary: KasSummary
)

class KasMateApiException(message: String, val statusCode: Int? = null) : Exception(message)

class KasMateApiClient(address: String) {
    val baseUrl: String = normalizeBaseUrl(address)

    suspend fun login(name: String, nis: String, password: String): StudentLogin = withContext(Dispatchers.IO) {
        val body = JSONObject()
            .put("name", name.trim())
            .put("password", password)
        if (nis.isNotBlank()) body.put("nis", nis.trim())

        val data = request("POST", "android/login.php", body = body).asObject("Student sign-in")
        val token = data.textOrEmpty("token")
        if (token.isBlank()) throw KasMateApiException("The server did not return a sign-in token.")
        StudentLogin(token)
    }

    suspend fun loadDashboard(token: String): StudentDashboardData = withContext(Dispatchers.IO) {
        val profileData = request("GET", "android/me.php", token = token).asObject("Student profile")
        val billRows = request("GET", "android/bills.php", token = token).asArray("Student bills")
        val summaryData = request("GET", "android/summary.php", token = token).asObject("Class summary")

        val bills = buildList {
            for (index in 0 until billRows.length()) {
                val row = billRows.optJSONObject(index) ?: continue
                val due = row.optLong("amountDue")
                val paid = row.optLong("amountPaid")
                val assessed = row.optLong("assessedAmount", due + paid)
                val status = when (row.optString("status").uppercase(Locale.ROOT)) {
                    "LUNAS" -> PaymentStatus.LUNAS
                    "SEBAGIAN" -> PaymentStatus.SEBAGIAN
                    "PROSES" -> PaymentStatus.PROSES
                    else -> PaymentStatus.BELUM
                }
                add(
                    Bill(
                        id = row.optLong("billStudentId").toString(),
                        title = row.textOrEmpty("name").ifBlank { "Tagihan kelas" },
                        amount = assessed,
                        dueDateMillis = parseDateMillis(row.textOrEmpty("periodEnd").ifBlank { row.textOrEmpty("createdAt") }),
                        status = status,
                        amountDue = due,
                        amountPaid = paid,
                        assessedAmount = assessed,
                        isArchived = row.optBoolean("isArchived"),
                        isIuran = row.textOrEmpty("type").equals("iuran", ignoreCase = true)
                    )
                )
            }
        }

        val expenseRows = summaryData.optJSONArray("expenses") ?: JSONArray()
        val expenses = buildList {
            for (index in 0 until expenseRows.length()) {
                val row = expenseRows.optJSONObject(index) ?: continue
                add(
                    Expense(
                        id = row.optLong("id").toString(),
                        description = row.textOrEmpty("description").ifBlank { "Pengeluaran kelas" },
                        amount = row.optLong("amount"),
                        dateMillis = parseDateMillis(row.textOrEmpty("createdAt")),
                        noteBy = row.textOrEmpty("createdBy").ifBlank { "Bendahara" }
                    )
                )
            }
        }

        StudentDashboardData(
            profile = StudentProfile(
                id = profileData.optLong("id"),
                name = profileData.textOrEmpty("name"),
                nis = profileData.textOrEmpty("nis"),
                className = profileData.textOrEmpty("className"),
                academicYear = profileData.textOrEmpty("academicYear"),
                savingsBalance = profileData.optLong("savingsBalance")
            ),
            summary = KasSummary(
                totalBalance = summaryData.optLong("classBalance"),
                myBills = bills,
                expenses = expenses,
                totalIncome = summaryData.optLong("classIncome"),
                totalExpenses = summaryData.optLong("classExpensesTotal"),
                totalIuranCollected = summaryData.optLong("classIuranCollected"),
                totalIuranTarget = summaryData.optLong("classIuranTarget")
            )
        )
    }

    suspend fun loadNotifications(token: String): List<StudentNotification> = withContext(Dispatchers.IO) {
        val rows = request("GET", "android/notifications.php", token = token).asArray("Student notifications")
        buildList {
            for (index in 0 until rows.length()) {
                val row = rows.optJSONObject(index) ?: continue
                val id = row.optLong("id")
                val title = row.textOrEmpty("title")
                if (id > 0 && title.isNotBlank()) {
                    add(StudentNotification(id, title, row.textOrEmpty("message")))
                }
            }
        }
    }

    suspend fun loadPaymentRequests(token: String): List<StudentPaymentRequest> = withContext(Dispatchers.IO) {
        val rows = request("GET", "android/payment-requests.php", token = token).asArray("Student payment requests")
        buildList {
            for (index in 0 until rows.length()) {
                val row = rows.optJSONObject(index) ?: continue
                add(
                    StudentPaymentRequest(
                        id = row.optLong("id"),
                        billStudentId = row.optLong("billStudentId"),
                        billName = row.textOrEmpty("billName").ifBlank { "Tagihan kelas" },
                        amount = row.optLong("amount"),
                        method = row.textOrEmpty("method"),
                        status = row.textOrEmpty("status").uppercase(Locale.ROOT),
                        proofAttached = row.optBoolean("proofAttached"),
                        proofFileName = row.textOrEmpty("proofFileName"),
                        createdAtMillis = parseDateMillis(row.textOrEmpty("createdAt")),
                        reviewNote = row.textOrEmpty("reviewNote")
                    )
                )
            }
        }
    }

    suspend fun submitPaymentRequest(
        context: Context,
        token: String,
        billStudentId: Long,
        paymentMethod: String,
        proofUri: Uri?
    ) = withContext(Dispatchers.IO) {
        val boundary = "KasMate-${System.currentTimeMillis()}"
        val connection = (URL(baseUrl + "android/payment-submit.php").openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 12_000
            readTimeout = 25_000
            doOutput = true
            useCaches = false
            setRequestProperty("Accept", "application/json")
            setRequestProperty("Authorization", "Bearer $token")
            setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
        }

        try {
            DataOutputStream(connection.outputStream).use { output ->
                writeMultipartField(output, boundary, "billStudentId", billStudentId.toString())
                writeMultipartField(output, boundary, "paymentMethod", paymentMethod)
                if (proofUri != null) {
                    val selectedName = displayName(context, proofUri)
                    val selectedExtension = selectedName.substringAfterLast('.', "").lowercase(Locale.ROOT)
                    val mimeType = context.contentResolver.getType(proofUri)
                        ?: if (selectedExtension == "pdf") "application/pdf" else "application/octet-stream"
                    val originalFileName = selectedName.ifBlank {
                        if (mimeType == "application/pdf") "payment-proof.pdf" else "payment-proof.jpg"
                    }
                    val uploadFileName = originalFileName
                        .replace(Regex("[\\r\\n\\\\\"]"), "_")
                        .replace(Regex("[^A-Za-z0-9._-]"), "_")
                    writeMultipartField(output, boundary, "proofFileName", originalFileName)
                    output.writeBytes("--$boundary\r\n")
                    output.writeBytes("Content-Disposition: form-data; name=\"proof\"; filename=\"$uploadFileName\"\r\n")
                    output.writeBytes("Content-Type: $mimeType\r\n\r\n")
                    context.contentResolver.openInputStream(proofUri)?.use { input ->
                        val buffer = ByteArray(8 * 1024)
                        var totalBytes = 0
                        while (true) {
                            val count = input.read(buffer)
                            if (count < 0) break
                            totalBytes += count
                            if (totalBytes > 5 * 1024 * 1024) {
                                throw IllegalArgumentException("Payment proof must be smaller than 5 MB.")
                            }
                            output.write(buffer, 0, count)
                        }
                    } ?: throw IllegalArgumentException("The selected proof file could not be opened.")
                    output.writeBytes("\r\n")
                }
                output.writeBytes("--$boundary--\r\n")
            }

            val status = connection.responseCode
            val responseStream = if (status in 200..299) connection.inputStream else connection.errorStream
            val responseText = responseStream?.use { input ->
                BufferedReader(InputStreamReader(input, Charsets.UTF_8)).use { it.readText() }
            }.orEmpty()
            val envelope = try {
                JSONObject(responseText)
            } catch (_: Exception) {
                throw KasMateApiException("The server returned an invalid response.", status)
            }
            if (status !in 200..299 || !envelope.optBoolean("success")) {
                throw KasMateApiException(
                    envelope.optString("error").ifBlank { "Server request failed ($status)." },
                    status
                )
            }
        } finally {
            connection.disconnect()
        }
    }

    suspend fun registerPushDevice(token: String, installationId: String) = withContext(Dispatchers.IO) {
        request(
            "POST",
            "android/devices/register.php",
            token = token,
            body = JSONObject().put("installationId", installationId)
        )
    }

    suspend fun logout(token: String, installationId: String? = null) = withContext(Dispatchers.IO) {
        val body = JSONObject()
        if (!installationId.isNullOrBlank()) body.put("installationId", installationId)
        runCatching { request("POST", "android/logout.php", token = token, body = body) }
    }

    private fun request(
        method: String,
        path: String,
        token: String? = null,
        body: JSONObject? = null
    ): Any {
        val connection = (URL(baseUrl + path).openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = 12_000
            readTimeout = 18_000
            useCaches = false
            setRequestProperty("Accept", "application/json")
            if (token != null) setRequestProperty("Authorization", "Bearer $token")
            if (body != null) {
                doOutput = true
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
            }
        }

        try {
            if (body != null) connection.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
            val status = connection.responseCode
            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
            val responseText = stream?.use { input ->
                BufferedReader(InputStreamReader(input, Charsets.UTF_8)).use { it.readText() }
            }.orEmpty()
            val envelope = try {
                JSONObject(responseText)
            } catch (_: Exception) {
                throw KasMateApiException("The server returned an invalid response.", status)
            }
            if (status !in 200..299 || !envelope.optBoolean("success")) {
                val message = envelope.optString("error").ifBlank { "Server request failed ($status)." }
                throw KasMateApiException(message, status)
            }
            return envelope.opt("data") ?: throw KasMateApiException("The server returned no data.", status)
        } finally {
            connection.disconnect()
        }
    }

    private fun writeMultipartField(output: DataOutputStream, boundary: String, name: String, value: String) {
        output.writeBytes("--$boundary\r\n")
        output.writeBytes("Content-Disposition: form-data; name=\"$name\"\r\n\r\n")
        output.write(value.toByteArray(Charsets.UTF_8))
        output.writeBytes("\r\n")
    }

    private fun displayName(context: Context, uri: Uri): String {
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val column = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (column >= 0) return cursor.getString(column).orEmpty()
            }
        }
        return "payment-proof.jpg"
    }

    private fun Any.asObject(label: String): JSONObject = this as? JSONObject
        ?: throw KasMateApiException("The server returned invalid $label data.")

    private fun Any.asArray(label: String): JSONArray = this as? JSONArray
        ?: throw KasMateApiException("The server returned invalid $label data.")

    private fun JSONObject.textOrEmpty(key: String): String {
        val value = opt(key)
        return if (value == null || value === JSONObject.NULL) "" else value.toString().takeUnless { it == "null" }.orEmpty()
    }

    companion object {
        fun normalizeBaseUrl(address: String): String {
            val input = address.trim()
            if (input.isBlank()) throw IllegalArgumentException("Masukkan IP atau alamat server.")
            val withScheme = if (input.contains("://")) input else "http://$input"
            val uri = try {
                URI(withScheme)
            } catch (_: Exception) {
                throw IllegalArgumentException("Alamat server tidak valid.")
            }
            if ((uri.scheme != "http" && uri.scheme != "https") || uri.host.isNullOrBlank()) {
                throw IllegalArgumentException("Gunakan IP server seperti 10.199.241.182 atau alamat http/https.")
            }
            var path = uri.rawPath.orEmpty().trimEnd('/')
            if (path.isBlank() || path == "/") path = "/KasMateAPI"
            return URI(uri.scheme, uri.userInfo, uri.host, uri.port, path + "/", null, null).toASCIIString()
        }

        private fun parseDateMillis(value: String): Long {
            if (value.isBlank()) return System.currentTimeMillis()
            val formats = listOf(
                "yyyy-MM-dd'T'HH:mm:ssXXX",
                "yyyy-MM-dd'T'HH:mm:ss'Z'",
                "yyyy-MM-dd'T'HH:mm:ss",
                "yyyy-MM-dd"
            )
            for (pattern in formats) {
                try {
                    val formatter = SimpleDateFormat(pattern, Locale.US).apply {
                        isLenient = false
                        timeZone = TimeZone.getDefault()
                    }
                    formatter.parse(value)?.let { return it.time }
                } catch (_: Exception) {
                    // Try the next supported API date format.
                }
            }
            return System.currentTimeMillis()
        }
    }
}
