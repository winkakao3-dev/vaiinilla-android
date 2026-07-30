package com.vaiinilla.app.ui.navigation

object Routes {
    const val SPLASH = "splash"
    const val DISCOVERY = "discovery"
    const val ROLE_SELECTOR = "roles"
    const val DEMO_GALLERY = "demo/gallery"

    const val CATALOG = "catalog"
    const val CART = "cart"
    const val CONFIRMATION = "order-confirmation"
    const val AUTH_LANDING = "auth/landing?returnRoute={returnRoute}"
    const val AUTH_REGISTER = "auth/register?returnRoute={returnRoute}"
    const val AUTH_LOGIN = "auth/login?returnRoute={returnRoute}"
    const val AUTH_VERIFY = "auth/verify?returnRoute={returnRoute}"
    const val AUTH_FORGOT = "auth/forgot?returnRoute={returnRoute}"
    const val STUDENT_TRACKING = "student/tracking"
    const val ASSISTANT = "assistant"
    const val ASSISTANT_HUB = "assistant/hub"
    const val ASSISTANT_CHAT = "assistant/chat"
    const val WALLET = "wallet"
    const val WALLET_ADD_MONEY = "wallet/add-money?method={method}"
    const val WALLET_METHODS = "wallet/methods"
    const val WALLET_ADD_CARD = "wallet/add-card"
    const val WALLET_ACCOUNT = "wallet/account"
    const val RECEIPT_STICKER = "receipt-sticker?style={style}"

    fun receiptStickerRoute(styleIndex: Int = 0): String = "receipt-sticker?style=$styleIndex"

    fun authLandingRoute(returnRoute: String = CART): String = "auth/landing?returnRoute=$returnRoute"

    fun authRegisterRoute(returnRoute: String = CART): String = "auth/register?returnRoute=$returnRoute"

    fun authLoginRoute(returnRoute: String = CART): String = "auth/login?returnRoute=$returnRoute"

    fun authVerifyRoute(returnRoute: String = CART): String = "auth/verify?returnRoute=$returnRoute"

    fun authForgotRoute(returnRoute: String = CART): String = "auth/forgot?returnRoute=$returnRoute"

    const val CASHIER = "cashier"
    const val KITCHEN = "kitchen"
    const val WAITER = "waiter"
}
