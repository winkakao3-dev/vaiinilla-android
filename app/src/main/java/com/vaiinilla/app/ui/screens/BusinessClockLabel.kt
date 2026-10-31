package com.vaiinilla.app.ui.screens

import com.vaiinilla.app.domain.time.BusinessTime
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** La hora en la zona del negocio; `createdAt` viene en UTC y antes se mostraba tal cual. */
internal fun receiptLocalTime(
    createdAt: String,
    zone: ZoneId = BusinessTime.zone,
): String =
    runCatching {
        Instant
            .parse(createdAt)
            .atZone(zone)
            .format(DateTimeFormatter.ofPattern("HH:mm"))
    }.getOrDefault("—")
