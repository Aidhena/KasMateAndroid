package com.smktel.kasmate.data

import kotlinx.coroutines.flow.Flow

interface KasRepository {
    val summary: Flow<KasSummary>
}
