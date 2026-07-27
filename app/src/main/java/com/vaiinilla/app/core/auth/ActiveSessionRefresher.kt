package com.vaiinilla.app.core.auth

interface ActiveSessionRefresher {
    fun refreshActiveSession(): Result<Unit>
}
