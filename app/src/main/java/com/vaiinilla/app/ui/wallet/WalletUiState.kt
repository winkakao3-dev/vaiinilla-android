package com.vaiinilla.app.ui.wallet

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue

data class SavedCard(
    val brand: String,
    val lastFour: String,
    val holder: String,
    val expiry: String,
)

class WalletUiState(
    initialBalance: Int = 0,
    initialCards: List<SavedCard> = emptyList(),
) {
    var balance by mutableIntStateOf(initialBalance)
    var cards by mutableStateOf(initialCards)
}

@Composable
fun rememberWalletUiState(): WalletUiState {
    val cardsSaver =
        listSaver<WalletUiState, String>(
            save = { state ->
                buildList {
                    add(state.balance.toString())
                    state.cards.forEach { card ->
                        add("${card.brand}|${card.lastFour}|${card.holder}|${card.expiry}")
                    }
                }
            },
            restore = { saved ->
                val balance = saved.firstOrNull()?.toIntOrNull() ?: 0
                val cards =
                    saved
                        .drop(1)
                        .mapNotNull { line ->
                            val parts = line.split("|")
                            if (parts.size == 4) {
                                SavedCard(parts[0], parts[1], parts[2], parts[3])
                            } else {
                                null
                            }
                        }.ifEmpty { emptyList() }
                WalletUiState(initialBalance = balance, initialCards = cards)
            },
        )
    return rememberSaveable(saver = cardsSaver) {
        WalletUiState()
    }
}
