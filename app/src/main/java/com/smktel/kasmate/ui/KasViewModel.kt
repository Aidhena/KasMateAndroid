package com.smktel.kasmate.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smktel.kasmate.data.*
import kotlinx.coroutines.flow.*

class KasViewModel(repo: KasRepository) : ViewModel() {
    val state: StateFlow<KasSummary> = repo.summary.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5_000),
        KasSummary(0, emptyList(), emptyList())
    )
}
