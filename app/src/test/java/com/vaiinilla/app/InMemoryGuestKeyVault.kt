package com.vaiinilla.app

import com.vaiinilla.app.core.security.GuestKeyVault

/** Sustituye al Keystore, que no existe en pruebas JVM. */
internal class InMemoryGuestKeyVault : GuestKeyVault {
    private var stored: String? = null

    override fun read(): String? = stored

    override fun write(key: String) {
        stored = key
    }

    override fun clear() {
        stored = null
    }
}
