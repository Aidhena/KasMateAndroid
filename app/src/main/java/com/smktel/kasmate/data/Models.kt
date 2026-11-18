package com.smktel.kasmate.data

enum class PaymentStatus { LUNAS, BELUM, SEBAGIAN, PROSES }

data class Bill(
    val id: String,
    val title: String,
    val amount: Long,
    val dueDateMillis: Long,
    val status: PaymentStatus,
    val amountDue: Long = amount,
    val amountPaid: Long = 0,
    val assessedAmount: Long = amount,
    val isArchived: Boolean = false,
    val isIuran: Boolean = false
)

data class Expense(
    val id: String,
    val description: String,
    val amount: Long,
    val dateMillis: Long,
    val noteBy: String
)

data class KasSummary(
    val totalBalance: Long,
    val myBills: List<Bill>,
    val expenses: List<Expense>,
    val totalIncome: Long = 0,
    val totalExpenses: Long = 0,
    val totalIuranCollected: Long = 0,
    val totalIuranTarget: Long = 0
)

data class StudentPaymentRequest(
    val id: Long,
    val billStudentId: Long,
    val billName: String,
    val amount: Long,
    val method: String,
    val status: String,
    val proofAttached: Boolean,
    val proofFileName: String = "",
    val createdAtMillis: Long,
    val reviewNote: String
)
