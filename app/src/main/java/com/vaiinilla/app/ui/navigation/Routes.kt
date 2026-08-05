package com.vaiinilla.app.ui.navigation

import android.net.Uri

object Routes {
    const val SPLASH = "splash"
    const val DISCOVERY = "discovery"

    const val CATALOG = "catalog"
    const val CART = "cart"
    const val CONFIRMATION = "order-confirmation"
    const val AUTH_LANDING = "auth/landing?returnRoute={returnRoute}"
    const val AUTH_REGISTER = "auth/register?returnRoute={returnRoute}"
    const val AUTH_LOGIN = "auth/login?returnRoute={returnRoute}"
    const val AUTH_VERIFY = "auth/verify?returnRoute={returnRoute}"
    const val AUTH_FORGOT = "auth/forgot?returnRoute={returnRoute}"
    const val VAI27_INVITATION = "vai27/invitation?token={token}"
    const val VAI27_MODES = "vai27/modes"
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

    fun authLandingRoute(returnRoute: String = CART): String = "auth/landing?returnRoute=${Uri.encode(returnRoute)}"

    fun authRegisterRoute(returnRoute: String = CART): String = "auth/register?returnRoute=${Uri.encode(returnRoute)}"

    fun authLoginRoute(returnRoute: String = CART): String = "auth/login?returnRoute=${Uri.encode(returnRoute)}"

    fun authVerifyRoute(returnRoute: String = CART): String = "auth/verify?returnRoute=${Uri.encode(returnRoute)}"

    fun authForgotRoute(returnRoute: String = CART): String = "auth/forgot?returnRoute=${Uri.encode(returnRoute)}"

    fun vai27InvitationRoute(token: String): String = "vai27/invitation?token=${Uri.encode(token)}"

    const val CASHIER = "cashier"
    const val KITCHEN = "kitchen"
    const val WAITER = "waiter"
}
