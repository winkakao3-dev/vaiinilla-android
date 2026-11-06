package com.vaiinilla.app.ui.hotel.staff

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vaiinilla.app.core.network.toUserFacingMessage
import com.vaiinilla.app.domain.hotel.HotelException
import com.vaiinilla.app.ui.components.physicalPress
import com.vaiinilla.app.ui.theme.LocalVaiinillaColors
import java.math.BigDecimal
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.util.Locale

/** Los dos tonos del lienzo de Doop que el tema no trae: «ocupada» (azul) y «por limpiar» (ámbar). */
internal val HotelBlue = Color(0xFF7FB4E8)
internal val HotelAmber = Color(0xFFE8B94A)

/**
 * La página de una pantalla del personal del hotel, tal como la dibuja Doop: encabezado con la línea de contexto
 * en gris y el título grande, y debajo tarjetas de 26 dp separadas 12 dp. [bottomBar] queda fijo abajo (el botón
 * principal de S02 y S04).
 */
@Composable
internal fun HotelStaffPage(
    kicker: String,
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    titleSize: Int = 34,
    onBack: (() -> Unit)? = null,
    onKicker: (() -> Unit)? = null,
    bottomBar: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = LocalVaiinillaColors.current
    Box(modifier = modifier.fillMaxSize().background(colors.paper)) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = if (bottomBar != null) 104.dp else 28.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 22.dp, end = 16.dp, top = 10.dp, bottom = 14.dp),
                verticalAlignment = Alignment.Top,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        kicker,
                        color = colors.muted,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        modifier =
                            if (onKicker != null) {
                                Modifier.physicalPress(onClick = onKicker)
                            } else {
                                Modifier
                            },
                    )
                    Text(
                        title,
                        color = colors.ink,
                        fontSize = titleSize.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = (-titleSize * 0.047f).sp,
                        lineHeight = (titleSize + 3).sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (subtitle != null) {
                        Text(
                            subtitle,
                            color = colors.muted,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 2.dp),
                        )
                    }
                }
                if (onBack != null) {
                    Box(
                        modifier =
                            Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(colors.paper2)
                                .physicalPress(onClick = onBack)
                                .semantics { contentDescription = "Cerrar" },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Outlined.Close, contentDescription = null, tint = colors.ink, modifier = Modifier.size(18.dp))
                    }
                }
            }
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                content = content,
            )
        }
        if (bottomBar != null) {
            Box(
                modifier =
                    Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(start = 14.dp, end = 14.dp, bottom = 26.dp),
            ) {
                bottomBar()
            }
        }
    }
}

/** Tarjeta de 26 dp sobre el papel 2; con [border] se marca (la comanda atrasada va con borde coral). */
@Composable
internal fun HotelCard(
    modifier: Modifier = Modifier,
    border: Color? = null,
    padding: Dp = 18.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = LocalVaiinillaColors.current
    val shape = RoundedCornerShape(26.dp)
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .clip(shape)
                .background(colors.paper2)
                .then(if (border != null) Modifier.border(1.5.dp, border, shape) else Modifier)
                .padding(padding),
        content = content,
    )
}

/** «ESTANCIA», «CARGOS A LA HABITACIÓN»: la etiqueta en mayúsculas de una tarjeta. */
@Composable
internal fun HotelCaps(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text,
        color = LocalVaiinillaColors.current.muted,
        fontSize = 12.sp,
        fontWeight = FontWeight.ExtraBold,
        letterSpacing = 0.72.sp,
        modifier = modifier,
    )
}

/** Un renglón de cuenta: concepto a la izquierda, importe a la derecha. */
@Composable
internal fun HotelRow(
    left: String,
    right: String?,
    modifier: Modifier = Modifier,
    muted: Boolean = false,
) {
    val colors = LocalVaiinillaColors.current
    val ink = if (muted) colors.muted else colors.ink
    Row(
        modifier = modifier.fillMaxWidth().padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(left, color = ink, fontSize = 15.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f, fill = false))
        if (right != null) {
            Text(right, color = ink, fontSize = 15.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 12.dp))
        }
    }
}

@Composable
internal fun HotelDivider(modifier: Modifier = Modifier) {
    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp)
                .height(1.dp)
                .background(LocalVaiinillaColors.current.ink.copy(alpha = 0.1f)),
    )
}

/** La etiqueta en cápsula de una tarjeta («Hace 14 min», «Programado 8:30»). */
@Composable
internal fun HotelTag(
    text: String,
    background: Color,
    foreground: Color,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.height(26.dp).clip(CircleShape).background(background).padding(horizontal = 11.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = foreground, fontSize = 12.sp, fontWeight = FontWeight.Black, maxLines = 1)
    }
}

internal enum class HotelButtonKind { Ink, Lime, Outline }

/** El botón de cápsula de Doop: tinta, lima o con contorno. */
@Composable
internal fun HotelButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    kind: HotelButtonKind = HotelButtonKind.Ink,
    enabled: Boolean = true,
    height: Dp = 50.dp,
    fontSize: Int = 15,
) {
    val colors = LocalVaiinillaColors.current
    val (background, foreground) =
        when (kind) {
            HotelButtonKind.Ink -> colors.ink to colors.onInk
            HotelButtonKind.Lime -> colors.accent to colors.accentInk
            HotelButtonKind.Outline -> colors.paper2 to colors.ink
        }
    Box(
        modifier =
            modifier
                .height(height)
                .clip(CircleShape)
                .background(background)
                .then(
                    if (kind == HotelButtonKind.Outline) {
                        Modifier.border(1.5.dp, colors.ink.copy(alpha = 0.2f), CircleShape)
                    } else {
                        Modifier
                    },
                ).physicalPress(enabled = enabled, onClick = onClick)
                .semantics { role = Role.Button },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            color = if (enabled) foreground else foreground.copy(alpha = 0.45f),
            fontSize = fontSize.sp,
            fontWeight = FontWeight.Black,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 18.dp),
        )
    }
}

/** El chip de filtro de Doop: tinta si está elegido, papel 2 si no. */
@Composable
internal fun HotelChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalVaiinillaColors.current
    Box(
        modifier =
            modifier
                .height(34.dp)
                .clip(CircleShape)
                .background(if (selected) colors.ink else colors.paper2)
                .physicalPress(onClick = onClick)
                .semantics { role = Role.Button }
                .padding(horizontal = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            color = if (selected) colors.onInk else colors.muted,
            fontSize = 13.sp,
            fontWeight = FontWeight.ExtraBold,
            maxLines = 1,
        )
    }
}

/** Un mensaje de error o de aviso dentro de la página, sin salirse del dibujo. */
@Composable
internal fun HotelNotice(
    text: String,
    modifier: Modifier = Modifier,
    tone: Color = LocalVaiinillaColors.current.coral,
) {
    Text(
        text,
        color = tone,
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
        modifier = modifier.fillMaxWidth().padding(horizontal = 8.dp),
    )
}

/** El texto del error para el personal: el que mandó el servidor si es del hotel, o el general de la app. */
internal fun staffErrorMessage(
    error: Throwable?,
    fallback: String,
): String {
    val hotel = generateSequence(error) { it.cause }.filterIsInstance<HotelException>().firstOrNull()
    return hotel?.message?.takeIf { it.isNotBlank() } ?: error.toUserFacingMessage(fallback)
}

private val money = DecimalFormat("#,##0.00", DecimalFormatSymbols(Locale.US))
private val moneyShort = DecimalFormat("#,##0.##", DecimalFormatSymbols(Locale.US))

/** «4950.00» → «$4,950.00»; un texto que no es número se muestra tal cual. */
internal fun hotelMoney(value: String?): String = value?.toBigDecimalOrNull()?.let { "$" + money.format(it) } ?: "$0.00"

/** «1650.00» → «$1,650»: el precio de una noche, sin centavos cuando no hay. */
internal fun hotelMoneyShort(value: String?): String = value?.toBigDecimalOrNull()?.let { "$" + moneyShort.format(it) } ?: "$0"

internal fun hotelMoney(value: BigDecimal): String = "$" + money.format(value)

private val months = listOf("ene", "feb", "mar", "abr", "may", "jun", "jul", "ago", "sep", "oct", "nov", "dic")

internal fun hotelDate(iso: String?): LocalDate? = iso?.takeIf { it.length >= 10 }?.take(10)?.let { runCatching { LocalDate.parse(it) }.getOrNull() }

/** «6 al 9 de nov»; si cruza de mes, «30 de oct al 2 de nov». */
internal fun hotelDateRange(
    from: String?,
    to: String?,
): String {
    val a = hotelDate(from) ?: return ""
    val b = hotelDate(to) ?: return "${a.dayOfMonth} de ${months[a.monthValue - 1]}"
    return if (a.month == b.month && a.year == b.year) {
        "${a.dayOfMonth} al ${b.dayOfMonth} de ${months[b.monthValue - 1]}"
    } else {
        "${a.dayOfMonth} de ${months[a.monthValue - 1]} al ${b.dayOfMonth} de ${months[b.monthValue - 1]}"
    }
}

/** «hace 6 min», «hace 2 h», «hace 3 d»; sin fecha legible, null. */
internal fun hotelAge(
    createdAt: String?,
    now: Instant,
): String? {
    val then = createdAt?.let { runCatching { Instant.parse(it) }.getOrNull() } ?: return null
    val minutes = Duration.between(then, now).toMinutes().coerceAtLeast(0)
    return when {
        minutes < 1 -> "ahora"
        minutes < 60 -> "hace $minutes min"
        minutes < 60 * 24 -> "hace ${minutes / 60} h"
        else -> "hace ${minutes / (60 * 24)} d"
    }
}

/** «Habitación 304» o «Hab. 304» → «304». */
internal fun hotelRoomNumber(name: String?): String =
    name.orEmpty().trim().replace(Regex("^Habitaci[óo]n\\s*|^Hab\\.?\\s*", RegexOption.IGNORE_CASE), "").ifBlank { name.orEmpty() }
