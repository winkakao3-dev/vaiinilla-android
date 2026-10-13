package com.vaiinilla.app.ui.theme

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin

// Espejo de SeaGlassColor.swift (iOS): misma receta, mismos números. Si cambias uno, cambia el otro.
// Matemática pura, sin Compose, para probarla en JVM.

/** Un color en sRGB, 0–1 por canal. */
internal data class SeaGlassRgba(
    val red: Double,
    val green: Double,
    val blue: Double,
    val alpha: Double = 1.0,
) {
    /** Luminancia relativa WCAG. */
    val luminance: Double
        get() {
            fun lin(c: Double) = if (c <= 0.04045) c / 12.92 else ((c + 0.055) / 1.055).pow(2.4)
            return 0.2126 * lin(red) + 0.7152 * lin(green) + 0.0722 * lin(blue)
        }

    fun contrast(other: SeaGlassRgba): Double {
        val a = luminance
        val b = other.luminance
        return (max(a, b) + 0.05) / (min(a, b) + 0.05)
    }
}

/** OKLCH → sRGB. Fuera de gama, baja el croma hasta que quepa: así el tono se conserva. */
internal fun oklch(
    lightness: Double,
    chroma: Double,
    hue: Double,
    alpha: Double = 1.0,
): SeaGlassRgba {
    val l = lightness.coerceIn(0.0, 1.0)
    var c = max(chroma, 0.0)
    for (intento in 0 until 24) {
        val (r, g, b) = oklchToLinear(l, c, hue)
        if (listOf(r, g, b).all { it >= -0.0005 && it <= 1.0005 } || c < 0.0005) {
            return SeaGlassRgba(gamma(r), gamma(g), gamma(b), alpha)
        }
        c *= 0.88
    }
    return SeaGlassRgba(l, l, l, alpha)
}

private fun gamma(x: Double): Double {
    val v = x.coerceIn(0.0, 1.0)
    return if (v <= 0.0031308) 12.92 * v else 1.055 * v.pow(1 / 2.4) - 0.055
}

private fun oklchToLinear(
    l: Double,
    c: Double,
    hue: Double,
): Triple<Double, Double, Double> {
    val radians = hue * PI / 180
    val a = c * cos(radians)
    val b = c * sin(radians)
    val l1 = (l + 0.3963377774 * a + 0.2158037573 * b).pow(3)
    val m1 = (l - 0.1055613458 * a - 0.0638541728 * b).pow(3)
    val s1 = (l - 0.0894841775 * a - 1.2914855480 * b).pow(3)
    return Triple(
        4.0767416621 * l1 - 3.3077115913 * m1 + 0.2309699292 * s1,
        -1.2684380046 * l1 + 2.6097574011 * m1 - 0.3413193965 * s1,
        -0.0041960863 * l1 - 0.7034186147 * m1 + 1.7076127010 * s1,
    )
}

/** El color de una pieza de vidrio de mar: tono (grados OKLCH), croma y cuánto más clara que la base. */
internal data class SeaGlassTone(
    val hue: Double,
    val chroma: Double,
    val lift: Double = 0.0,
) {
    /** La misma pieza, más clara y con poco más de la mitad del color: el segmento elegido dentro de otra. */
    val picked: SeaGlassTone
        get() = SeaGlassTone(hue, chroma * 0.6, lift + 0.06)

    /** Vainilla y horchata: el vidrio de las superficies y los controles secundarios, sin color propio. */
    val isNeutral: Boolean
        get() = chroma < 0.06

    companion object {
        val lima = SeaGlassTone(128.0, 0.13)
        val matcha = SeaGlassTone(145.0, 0.08)
        val menta = SeaGlassTone(175.0, 0.08)
        val vainilla = SeaGlassTone(88.0, 0.045, lift = 0.03)
        val horchata = SeaGlassTone(75.0, 0.035, lift = 0.04)
        val mango = SeaGlassTone(70.0, 0.12)
        val cafe = SeaGlassTone(55.0, 0.07)
        val jamaica = SeaGlassTone(355.0, 0.11)
        val fresa = SeaGlassTone(15.0, 0.1)
        val cielo = SeaGlassTone(232.0, 0.08)
        val lavanda = SeaGlassTone(300.0, 0.1)
        val mora = SeaGlassTone(320.0, 0.12)
    }
}

/**
 * La receta elegida en el laboratorio (2026-10-12), medida sobre las piezas de referencia: color casi parejo,
 * todo el canto aclarado y sombra gris neutra un poco más ancha que la pieza.
 */
internal object SeaGlassRecipe {
    const val DEPTH = 0.13

    /** El canto difuminado, en proporción al alto de la pieza. */
    const val SOFT_EDGE = 1.0 / 56

    /**
     * Cuánto corre el tono el canto: hacia el amarillo (100) o el cian (200) más cercano, como en la referencia.
     * Lo que ya está cerca casi no cambia.
     */
    fun edgeHueShift(hue: Double): Double {
        val h = ((hue % 360) + 360) % 360
        var d = (if (h >= 150 && h < 290) 200.0 else 100.0) - h
        if (d > 180) d -= 360
        if (d < -180) d += 360
        val dist = abs(d)
        val bump = 40 * exp(-((dist - 40) / 28).pow(2))
        return (if (d < 0) -1.0 else 1.0) * min(dist * 0.95, max(8.0, bump))
    }
}

/** Todos los colores de una pieza a una hora del Vainiverso. */
internal data class SeaGlassShades(
    val top: SeaGlassRgba,
    val mid: SeaGlassRgba,
    val low: SeaGlassRgba,
    val bottom: SeaGlassRgba,
    /** La luz que entra por todo el canto, más fuerte abajo. */
    val edge: SeaGlassRgba,
    val ink: SeaGlassRgba,
    val shadow: SeaGlassRgba,
    val rim: SeaGlassRgba,
)

/**
 * [night]: de noche el vidrio se oscurece y la tinta se aclara. El lima no cambia: es el mismo verde de día a
 * cualquier hora. Las piezas neutras de noche son casi negras, sin tono azul ni café.
 */
internal fun seaGlassShades(
    tone: SeaGlassTone,
    night: Boolean,
): SeaGlassShades {
    val dayGlass = !night || tone.hue == SeaGlassTone.lima.hue
    val smoky = !dayGlass && tone.isNeutral
    val l = if (dayGlass) 0.86 + tone.lift else (if (smoky) 0.3 else 0.45) + tone.lift * 0.5
    val c = if (smoky) 0.004 else tone.chroma
    val h = tone.hue
    val shifted = h + SeaGlassRecipe.edgeHueShift(h)
    return SeaGlassShades(
        top = oklch(l + 0.024, c * 0.78, h),
        mid = oklch(l, c, h),
        low = oklch(l + 0.01, c * 0.96, h),
        bottom = oklch(l + 0.055, c * 0.72, shifted),
        edge = oklch(l + 0.07, c * 0.5, shifted, alpha = 0.85),
        ink = oklch(if (dayGlass) 0.36 else 0.95, c * 1.1, h),
        shadow = SeaGlassRgba(0.0, 0.0, 0.0, if (night) 0.55 else 0.14),
        rim = SeaGlassRgba(1.0, 1.0, 1.0, if (night) 0.12 else 0.1),
    )
}
