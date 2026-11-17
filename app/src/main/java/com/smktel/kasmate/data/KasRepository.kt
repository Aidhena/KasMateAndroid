package com.smktel.kasmate.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

interface KasRepository {
    val summary: Flow<KasSummary>
}

class FakeKasRepository : KasRepository {
    private val day = 86_400_000L
    private val now = System.currentTimeMillis()

    override val summary = MutableStateFlow(
        KasSummary(
            totalBalance = 143_000,
            myBills = listOf(
                Bill("1", "Kas Minggu 1", 5_000, now - 7 * day, PaymentStatus.LUNAS),
                Bill("2", "Kas Minggu 2", 5_000, now + 1 * day, PaymentStatus.BELUM),
            ),
            expenses = listOf(
                Expense("e1", "Beli spidol & penghapus", 25_000, now - 3 * day, "Bendahara"),
                Expense("e2", "Fotokopi soal ujian", 40_000, now - 10 * day, "Bendahara"),
            )
        )
    )
}