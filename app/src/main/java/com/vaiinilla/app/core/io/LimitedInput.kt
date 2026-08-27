package com.vaiinilla.app.core.io

import java.io.ByteArrayOutputStream
import java.io.InputStream

/** Reads at most [maxBytes]. Returns null instead of allocating past the limit. */
fun InputStream.readBytesLimited(maxBytes: Int): ByteArray? {
    require(maxBytes > 0) { "maxBytes must be positive" }
    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
    val output = ByteArrayOutputStream(minOf(maxBytes, 64 * 1024))
    var total = 0
    while (true) {
        val count = read(buffer)
        if (count < 0) break
        if (count == 0) continue
        total += count
        if (total > maxBytes) return null
        output.write(buffer, 0, count)
    }
    return output.toByteArray()
}
