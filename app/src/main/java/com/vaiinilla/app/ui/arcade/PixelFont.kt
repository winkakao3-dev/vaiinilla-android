package com.vaiinilla.app.ui.arcade

/** 3x5 pixel font used by every arcade HUD and panel. */
internal object PixelFont {
    private val rows =
        mapOf(
            'A' to "010101111101101",
            'B' to "110101110101110",
            'C' to "011100100100011",
            'D' to "110101101101110",
            'E' to "111100110100111",
            'F' to "111100110100100",
            'G' to "011100101101011",
            'H' to "101101111101101",
            'I' to "111010010010111",
            'J' to "001001001101010",
            'K' to "101101110101101",
            'L' to "100100100100111",
            'M' to "101111111101101",
            'N' to "110101101101101",
            'O' to "010101101101010",
            'P' to "111101111100100",
            'Q' to "010101101111011",
            'R' to "110101110101101",
            'S' to "011100010001110",
            'T' to "111010010010010",
            'U' to "101101101101111",
            'V' to "101101101101010",
            'W' to "101101111111101",
            'X' to "101101010101101",
            'Y' to "101101010010010",
            'Z' to "111001010100111",
            '0' to "111101101101111",
            '1' to "010110010010111",
            '2' to "110001010100111",
            '3' to "110001010001110",
            '4' to "101101111001001",
            '5' to "111100110001110",
            '6' to "011100111101111",
            '7' to "111001010010010",
            '8' to "111101111101111",
            '9' to "111101111001110",
            '.' to "000000000000010",
            ':' to "000010000010000",
            '-' to "000000111000000",
            '!' to "010010010000010",
            '\'' to "010010000000000",
            ' ' to "000000000000000",
            '+' to "000010111010000",
            '/' to "001001010100100",
            '?' to "110001010000010",
            '¡' to "010000010010010",
            '♥' to "000101111111010",
        )

    private val accents = mapOf('Á' to 'A', 'É' to 'E', 'Í' to 'I', 'Ó' to 'O', 'Ú' to 'U', 'Ñ' to 'N')

    /** Width in pixels of a text run at a given scale. */
    fun width(
        s: String,
        scale: Double = 1.0,
    ): Double = (s.length * 4 - 1) * scale

    fun draw(
        s: String,
        cv: PixelCanvas,
        x: Double,
        y: Double,
        color: Int,
        alpha: Double = 1.0,
        scale: Double = 1.0,
    ) {
        fun dot(
            i: Double,
            j: Double,
            cursor: Double,
        ) {
            if (scale == 1.0) {
                cv.px(cursor + i, y + j, color, alpha)
            } else {
                cv.rect(cursor + i * scale, y + j * scale, scale, scale, color, alpha)
            }
        }
        var cursor = x
        for (raw in s.uppercase()) {
            var ch = raw
            val base = accents[ch]
            if (base != null) {
                // the accent sits above the cap line: an acute stroke, or a tilde for N
                if (ch == 'Ñ') {
                    dot(0.0, -2.0, cursor)
                    dot(1.0, -2.0, cursor)
                    dot(2.0, -2.0, cursor)
                } else {
                    dot(1.0, -1.0, cursor)
                    dot(2.0, -2.0, cursor)
                }
                ch = base
            }
            val glyph = rows[ch] ?: rows[' ']!!
            for (j in 0 until 5) {
                for (i in 0 until 3) if (glyph[j * 3 + i] == '1') dot(i.toDouble(), j.toDouble(), cursor)
            }
            cursor += 4 * scale
        }
    }
}

internal fun PixelCanvas.text(
    s: String,
    x: Double,
    y: Double,
    color: Int,
    alpha: Double = 1.0,
    scale: Double = 1.0,
) = PixelFont.draw(s, this, x, y, color, alpha, scale)
