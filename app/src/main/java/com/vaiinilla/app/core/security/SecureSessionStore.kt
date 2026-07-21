package com.vaiinilla.app.core.security

interface SecureSessionStore {
    fun saveAccessToken(token: String)
    fun readAccessToken(): String?
    fun clear()
}
