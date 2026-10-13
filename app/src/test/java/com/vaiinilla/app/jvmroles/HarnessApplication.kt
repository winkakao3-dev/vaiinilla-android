package com.vaiinilla.app.jvmroles

import android.app.Application
import android.content.SharedPreferences
import dagger.hilt.android.testing.CustomTestApplication
import java.util.concurrent.ConcurrentHashMap

/**
 * En el teléfono cada sabor tiene sus propias preferencias. Aquí los 4 roles comparten una sola
 * Application, así que cada archivo de preferencias se reparte por rol en cada lectura y escritura.
 * Los stores guardan el objeto al construirse; por eso se entrega uno que decide el archivo en cada
 * llamada y no al abrirlo.
 */
open class RoleScopedApplication : Application() {
    private val routed = ConcurrentHashMap<String, SharedPreferences>()

    override fun getSharedPreferences(
        name: String?,
        mode: Int,
    ): SharedPreferences {
        if (name == null) return super.getSharedPreferences(null, mode)
        return routed.getOrPut(name) { RoleRoutedSharedPreferences { role -> realPreferences("${role}__$name", mode) } }
    }

    private fun realPreferences(
        name: String,
        mode: Int,
    ): SharedPreferences = super.getSharedPreferences(name, mode)
}

@CustomTestApplication(RoleScopedApplication::class)
interface HarnessApplication

private class RoleRoutedSharedPreferences(
    private val open: (String) -> SharedPreferences,
) : SharedPreferences {
    private fun current(): SharedPreferences = open(ActiveRole.current?.cliName ?: "sin-rol")

    override fun getAll(): MutableMap<String, *> = current().all

    override fun getString(
        key: String?,
        defValue: String?,
    ): String? = current().getString(key, defValue)

    override fun getStringSet(
        key: String?,
        defValues: MutableSet<String>?,
    ): MutableSet<String>? = current().getStringSet(key, defValues)

    override fun getInt(
        key: String?,
        defValue: Int,
    ): Int = current().getInt(key, defValue)

    override fun getLong(
        key: String?,
        defValue: Long,
    ): Long = current().getLong(key, defValue)

    override fun getFloat(
        key: String?,
        defValue: Float,
    ): Float = current().getFloat(key, defValue)

    override fun getBoolean(
        key: String?,
        defValue: Boolean,
    ): Boolean = current().getBoolean(key, defValue)

    override fun contains(key: String?): Boolean = current().contains(key)

    override fun edit(): SharedPreferences.Editor = current().edit()

    override fun registerOnSharedPreferenceChangeListener(
        listener: SharedPreferences.OnSharedPreferenceChangeListener?,
    ) = current().registerOnSharedPreferenceChangeListener(listener)

    override fun unregisterOnSharedPreferenceChangeListener(
        listener: SharedPreferences.OnSharedPreferenceChangeListener?,
    ) = current().unregisterOnSharedPreferenceChangeListener(listener)
}
