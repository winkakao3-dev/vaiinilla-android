package com.vaiinilla.app.core.text

import java.text.Normalizer
import java.util.Locale

/** Normalizes user-entered search text so accents do not block a valid match. */
fun String.normalizeForSearch(): String =
    Normalizer
        .normalize(trim(), Normalizer.Form.NFD)
        .replace("\\p{Mn}+".toRegex(), "")
        .lowercase(Locale.ROOT)
