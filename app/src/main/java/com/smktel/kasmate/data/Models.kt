package com.smktel.kasmate.data

enum class PaymentStatus { LUNAS, BELUM }

data class Bill(
    val id: String,
    val title: String,
    val amount: Long,
    val dueDateMillis: Long,
    val status: PaymentStatus
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
    val expenses: List<Expense>
)