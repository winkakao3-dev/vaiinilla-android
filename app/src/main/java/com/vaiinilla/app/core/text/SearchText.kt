package com.vaiinilla.app.core.text

import java.text.Normalizer
import java.util.Locale

private val NON_SPACING_MARKS_REGEX = Regex("\\p{Mn}+")

/** Normalizes user-entered search text so accents do not block a valid match. */
fun String.normalizeForSearch(): String =
    Normalizer
        .normalize(trim(), Normalizer.Form.NFD)
        .replace(NON_SPACING_MARKS_REGEX, "")
        .lowercase(Locale.ROOT)
