package com.vaiinilla.app.ui.hotel.guest

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vaiinilla.app.core.network.toUserFacingMessage
import com.vaiinilla.app.domain.hotel.HotelErrors
import com.vaiinilla.app.domain.hotel.HotelException
import com.vaiinilla.app.ui.components.physicalPress
import com.vaiinilla.app.ui.components.rememberVaiinillaHaptics
import com.vaiinilla.app.ui.theme.LocalVaiinillaColors
import java.time.LocalDate

/*
 * Piezas que comparten las pantallas del huésped (H01, H05, H06 y el código de estancia). Siguen el dibujo de Doop:
 * papel oscuro, tarjetas de 24 dp, botón en cápsula de 58 dp en lima. Los colores salen del tema, no de aquí.
 */

internal val HotelTileShape = RoundedCornerShape(24.dp)

/** Botón principal en cápsula, a lo ancho. */
@Composable
internal fun HotelPrimaryPill(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    testTag: String? = null,
) {
    val colors = LocalVaiinillaColors.current
    val haptics = rememberVaiinillaHaptics()
    val shape = RoundedCornerShape(29.dp)
    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .height(58.dp)
                .then(if (testTag != null) Modifier.testTag(testTag) else Modifier)
                .physicalPress(enabled = enabled && !loading, onClick = {
                    haptics.click()
                    onClick()
                })
                .clip(shape)
                .background(if (enabled) colors.accent else colors.paper2)
                .semantics { role = Role.Button },
        contentAlignment = Alignment.Center,
    ) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(22.dp),
                strokeWidth = 2.5.dp,
                color = colors.accentInk,
            )
        } else {
            Text(
                label,
                color = if (enabled) colors.accentInk else colors.muted,
                fontSize = 17.sp,
                fontWeight = FontWeight.Black,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 20.dp),
            )
        }
    }
}

/** Encabezado de las pantallas internas: lo de arriba en gris y el título grande. */
@Composable
internal fun HotelPageTitle(
    eyebrow: String,
    title: String,
    modifier: Modifier = Modifier,
) {
    val colors = LocalVaiinillaColors.current
    Column(modifier = modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(eyebrow, color = colors.muted, fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 2)
        Text(
            title,
            color = colors.ink,
            fontSize = 36.sp,
            lineHeight = 38.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = (-1).sp,
        )
    }
}

/** Tarjeta de papel con título pequeño en mayúsculas opcional. */
@Composable
internal fun HotelPanel(
    modifier: Modifier = Modifier,
    title: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = LocalVaiinillaColors.current
    Column(
        modifier = modifier.fillMaxWidth().clip(HotelTileShape).background(colors.paper2).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        if (title != null) {
            Text(
                title.uppercase(),
                color = colors.muted,
                fontSize = 12.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 0.8.sp,
            )
        }
        content()
    }
}

/** Mosaico de H01: ícono, título y una línea de apoyo. */
@Composable
internal fun HotelActionTile(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    testTag: String? = null,
) {
    val colors = LocalVaiinillaColors.current
    val haptics = rememberVaiinillaHaptics()
    Column(
        modifier =
            modifier
                .then(if (testTag != null) Modifier.testTag(testTag) else Modifier)
                .physicalPress(onClick = {
                    haptics.click()
                    onClick()
                })
                .clip(HotelTileShape)
                .background(colors.paper2)
                .semantics { role = Role.Button }
                .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(colors.line),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = colors.ink, modifier = Modifier.size(20.dp))
        }
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, color = colors.ink, fontSize = 17.sp, fontWeight = FontWeight.Black, maxLines = 1)
            Text(subtitle, color = colors.muted, fontSize = 13.sp, lineHeight = 16.sp, fontWeight = FontWeight.SemiBold, maxLines = 2)
        }
    }
}

/** Mosaico de H05: lo que se pide, con la cantidad o la marca de elegido arriba a la derecha. */
@Composable
internal fun HotelRequestTile(
    title: String,
    subtitle: String,
    icon: ImageVector,
    badge: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    testTag: String? = null,
) {
    val colors = LocalVaiinillaColors.current
    val haptics = rememberVaiinillaHaptics()
    val selected = badge != null
    Box(
        modifier =
            modifier
                .then(if (testTag != null) Modifier.testTag(testTag) else Modifier)
                .physicalPress(onClick = {
                    haptics.click()
                    onClick()
                })
                .clip(HotelTileShape)
                .background(colors.paper2)
                .then(if (selected) Modifier.border(1.5.dp, colors.accent, HotelTileShape) else Modifier)
                .semantics(mergeDescendants = true) { role = Role.Button },
    ) {
        Box(
            modifier = Modifier.align(Alignment.TopStart).padding(18.dp).size(56.dp).clip(CircleShape).background(colors.line),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = if (selected) colors.accent else colors.ink, modifier = Modifier.size(28.dp))
        }
        if (badge != null) {
            Box(
                modifier = Modifier.align(Alignment.TopEnd).padding(12.dp).size(34.dp).clip(CircleShape).background(colors.accent),
                contentAlignment = Alignment.Center,
            ) {
                Text(badge, color = colors.accentInk, fontSize = 15.sp, fontWeight = FontWeight.Black)
            }
        }
        Column(
            modifier = Modifier.align(Alignment.BottomStart).padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(title, color = colors.ink, fontSize = 19.sp, lineHeight = 21.sp, fontWeight = FontWeight.Black, maxLines = 2)
            Text(subtitle, color = colors.muted, fontSize = 13.sp, lineHeight = 16.sp, fontWeight = FontWeight.SemiBold, maxLines = 2)
        }
    }
}

private val MONTHS_ES = listOf("ene", "feb", "mar", "abr", "may", "jun", "jul", "ago", "sep", "oct", "nov", "dic")

private fun dateOf(iso: String?): LocalDate? = iso?.take(10)?.let { runCatching { LocalDate.parse(it) }.getOrNull() }

/** «9 de nov» para una fecha ISO (`yyyy-MM-dd` o instante); `null` si no se lee. */
internal fun shortDate(iso: String?): String? = dateOf(iso)?.let { "${it.dayOfMonth} de ${MONTHS_ES[it.monthValue - 1]}" }

/** «del 6 al 9 de nov», o «del 30 oct al 2 de nov» si cruza de mes; `null` si alguna fecha no se lee. */
internal fun stayRange(
    checkIn: String?,
    checkOut: String?,
): String? {
    val from = dateOf(checkIn) ?: return null
    val to = dateOf(checkOut) ?: return null
    val toLabel = "${to.dayOfMonth} de ${MONTHS_ES[to.monthValue - 1]}"
    return if (from.month == to.month && from.year == to.year) {
        "del ${from.dayOfMonth} al $toLabel"
    } else {
        "del ${from.dayOfMonth} ${MONTHS_ES[from.monthValue - 1]} al $toLabel"
    }
}

/** El primer nombre del huésped, para saludar sin el apellido. */
internal fun firstName(fullName: String): String = fullName.trim().substringBefore(' ')

/** El mensaje que ve el huésped; los errores del hotel se dicen con sus palabras, los demás con el texto general. */
internal fun hotelErrorMessage(
    error: Throwable?,
    fallback: String = "No se pudo completar. Intenta de nuevo.",
): String {
    val hotel = generateSequence(error) { it.cause }.filterIsInstance<HotelException>().firstOrNull()
    return when (hotel?.code) {
        HotelErrors.ROOM_REQUIRES_STAY -> "Primero escribe el código de estancia que te dio recepción."
        HotelErrors.STAY_NOT_FOUND, "CODIGO_INVALIDO" -> "Ese código no coincide o ya venció. Revísalo con recepción."
        "HABITACION_DISTINTA" -> "Ese código es de otra habitación."
        HotelErrors.ROOM_CHARGE_DISABLED -> "Este hotel no carga pedidos a la habitación. Elige otra forma de pago."
        HotelErrors.CARD_AT_ORDER_DISABLED -> "Este hotel no cobra con tarjeta al pedir."
        HotelErrors.OUTSIDE_ORDER_HOURS -> "La cocina no recibe pedidos a esta hora."
        HotelErrors.REQUESTS_DISABLED -> "Este hotel no recibe solicitudes desde la app."
        HotelErrors.DND_DISABLED -> "Este hotel no tiene «No molestar» desde la app."
        HotelErrors.LATE_CHECKOUT_UNAVAILABLE -> "La salida tardía no está disponible."
        else -> error.toUserFacingMessage(fallback)
    }
}
