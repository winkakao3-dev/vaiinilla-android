package com.vaiinilla.app.ui.navigation

object Routes {
    const val SPLASH = "splash"
    const val DISCOVERY = "discovery"
    const val ROLE_SELECTOR = "roles"
    const val DEMO_GALLERY = "demo/gallery"

    const val CATALOG = "catalog"
    const val CART = "cart"
    const val CONFIRMATION = "order-confirmation"
    const val STUDENT_TRACKING = "student/tracking"
    const val ASSISTANT = "assistant"
    const val ASSISTANT_CHAT = "assistant/chat"
    const val WALLET = "wallet"
    const val WALLET_ADD_MONEY = "wallet/add-money?method={method}"
    const val WALLET_METHODS = "wallet/methods"
    const val WALLET_ADD_CARD = "wallet/add-card"
    const val WALLET_ACCOUNT = "wallet/account"
    const val RECEIPT_STICKER = "receipt-sticker?style={style}"

    fun receiptStickerRoute(styleIndex: Int = 0): String = "receipt-sticker?style=$styleIndex"

    const val CASHIER = "cashier"
    const val KITCHEN = "kitchen"
    const val WAITER = "waiter"
}
