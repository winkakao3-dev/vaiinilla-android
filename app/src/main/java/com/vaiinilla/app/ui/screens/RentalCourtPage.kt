package com.vaiinilla.app.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vaiinilla.app.domain.model.Catalog
import com.vaiinilla.app.domain.model.OrderDetail
import com.vaiinilla.app.domain.model.SpaceCopy
import com.vaiinilla.app.domain.model.SpaceDelivery
import com.vaiinilla.app.domain.repository.CallReason
import com.vaiinilla.app.domain.repository.TableCall
import com.vaiinilla.app.domain.repository.openTableCallSpaceId
import com.vaiinilla.app.ui.components.ClientCap
import com.vaiinilla.app.ui.components.ClientPillButton
import com.vaiinilla.app.ui.components.PhysicalPressScale
import com.vaiinilla.app.ui.components.ProductImage
import com.vaiinilla.app.ui.components.RentalStep
import com.vaiinilla.app.ui.components.RentalTimeLeftCard
import com.vaiinilla.app.ui.components.RentalTrackingTimeline
import com.vaiinilla.app.ui.components.VaiinillaBottomNavClearance
import com.vaiinilla.app.ui.components.WaiterCallHost
import com.vaiinilla.app.ui.components.arrive
import com.vaiinilla.app.ui.components.physicalPress
import com.vaiinilla.app.ui.components.rememberVaiinillaHaptics
import com.vaiinilla.app.ui.components.rentalCourtName
import com.vaiinilla.app.ui.components.rentalStep
import com.vaiinilla.app.ui.components.rentalStepDescription
import com.vaiinilla.app.ui.discovery.QrPayloadParser
import com.vaiinilla.app.ui.order.MenuWaiterCallState
import com.vaiinilla.app.ui.theme.LocalVaiinillaColors
import com.vaiinilla.app.ui.theme.VaiinillaColors

const val RENTAL_COURT_MENU_TAG = "rental-court-menu"
const val RENTAL_COURT_CODE_TAG = "rental-court-code"
const val RENTAL_COURT_WAITER_TAG = "rental-court-waiter"

/** Lo que se ofrece pedir a la cancha: la primera categoría del menú que tiene foto. */
internal data class CourtMenuPick(
    val title: String,
    val imageUrl: String?,
)

internal fun courtMenuPick(catalog: Catalog?): CourtMenuPick {
    val withPhoto = catalog?.products.orEmpty().filter { it.available && !it.archived && it.imageUrl.isNotBlank() }
    val category =
        catalog?.categories.orEmpty().sortedBy { it.order }.firstOrNull { category ->
            withPhoto.any { it.categoryId == category.id }
        }
    val photo = withPhoto.firstOrNull { it.categoryId == category?.id } ?: withPhoto.firstOrNull()
    return CourtMenuPick(title = category?.name ?: "El menú", imageUrl = photo?.imageUrl)
}

/**
 * Una renta abierta desde Pedidos, como el dibujo de Doop (L09): la cancha en grande, cuánto le queda, qué pedirle
 * y los botones del mesero y de tu código. Fuera de juego se ve el seguimiento de la renta en lugar del reloj.
 */
/** Cómo llega lo que pides desde la cancha, según `entrega_en_espacio` del negocio. */
internal fun courtMenuDeliveryLine(
    spaceDelivery: SpaceDelivery,
    place: String,
): String =
    when (spaceDelivery) {
        SpaceDelivery.TABLE -> "Te lo llevan a $place"
        SpaceDelivery.BAR -> "Pides aquí y pasas por él a la barra"
        SpaceDelivery.BOTH -> "Te lo llevan a $place o pasas por él"
    }

@Composable
internal fun RentalCourtPage(
    order: OrderDetail,
    catalog: Catalog?,
    /** Para «Mi código»: el mismo QR personal de Cuenta, con el que Caja cobra o recarga. */
    userId: String?,
    onOpenMenu: () -> Unit,
    onViewRental: () -> Unit,
    onCurrentWaiterCall: (suspend (Int) -> Result<TableCall?>)?,
    onCallWaiter: (suspend (Int, CallReason, String?) -> Result<TableCall>)?,
    onCancelWaiter: (suspend (TableCall) -> Result<TableCall>)?,
    modifier: Modifier = Modifier,
    /** Cómo entrega el negocio en la cancha: a la cancha, en la barra o ambas. */
    spaceDelivery: SpaceDelivery = SpaceDelivery.TABLE,
) {
    val reservation = order.reservation ?: return
    val colors = LocalVaiinillaColors.current
    val step = rentalStep(order)
    val courtName = order.rentalCourtName ?: "Cancha"
    val place = reservation.space?.let { SpaceCopy.named(it.type, it.name) } ?: "tu cancha"
    val qrValue = userId?.trim()?.takeIf { it.isNotEmpty() }?.let(QrPayloadParser::encodeUser)
    var codeOpen by rememberSaveable(order.summary.id) { mutableStateOf(false) }
    val spaceId = openTableCallSpaceId(order)
    val waiter =
        if (spaceId != null && onCurrentWaiterCall != null && onCallWaiter != null && onCancelWaiter != null) {
            remember(spaceId) { MenuWaiterCallState(spaceId, onCurrentWaiterCall, onCallWaiter, onCancelWaiter, null) }
        } else {
            null
        }
    val canOrder = step == RentalStep.PLAYING || step == RentalStep.BOOKED

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .background(colors.paper)
                .verticalScroll(rememberScrollState())
                .padding(bottom = VaiinillaBottomNavClearance + 24.dp),
    ) {
        CourtHero(
            courtName = courtName,
            step = step,
            description =
                step?.let {
                    rentalStepDescription(it, reservation, order.summary.paymentMethod, order.payment?.status)
                        .removeSuffix(".")
                },
            colors = colors,
        )
        if (step == RentalStep.PLAYING) {
            RentalTimeLeftCard(order = order, modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp).arrive(1))
        } else {
            RentalTrackingTimeline(order = order, modifier = Modifier.padding(horizontal = 20.dp).arrive(1))
        }
        if (canOrder) {
            ClientCap("Pedir a la cancha", modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 22.dp).arrive(2))
            CourtMenuCard(
                pick = courtMenuPick(catalog),
                place = place,
                spaceDelivery = spaceDelivery,
                onOpen = onOpenMenu,
                colors = colors,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 10.dp).arrive(2),
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 14.dp).arrive(3),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (waiter != null) {
                ClientPillButton(
                    text = "Llamar al mesero",
                    onClick = waiter::tap,
                    primary = false,
                    height = 50.dp,
                    modifier = Modifier.weight(1f).testTag(RENTAL_COURT_WAITER_TAG),
                )
            }
            if (qrValue != null) {
                ClientPillButton(
                    text = "Mi código",
                    onClick = { codeOpen = true },
                    primary = false,
                    height = 50.dp,
                    modifier = Modifier.weight(1f).testTag(RENTAL_COURT_CODE_TAG),
                )
            }
        }
        // Por pagar o ya terminada no hay reloj ni menú: el recibo sigue a la mano.
        if (!canOrder || step == RentalStep.BOOKED) {
            ClientPillButton(
                text = if (step == RentalStep.TO_PAY) "Ver cómo pagar" else "Ver mi renta",
                onClick = onViewRental,
                primary = step == RentalStep.TO_PAY,
                height = 50.dp,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 10.dp).arrive(4),
            )
        }
    }

    waiter?.let { WaiterCallHost(state = it, place = place) }
    if (codeOpen) {
        WalletQrDialog(
            qrValue = qrValue,
            onDismiss = { codeOpen = false },
            title = "Mi código",
            message = "Muéstralo en Caja para que encuentren tu cuenta.",
        )
    }
}

@Composable
private fun CourtHero(
    courtName: String,
    step: RentalStep?,
    description: String?,
    colors: VaiinillaColors,
) {
    val photo = LocalVenueArt.current.photo
    // Sin foto del negocio no hay qué enseñar arriba: el encabezado se encoge para no dejar un bloque vacío.
    Box(modifier = Modifier.fillMaxWidth().height(if (photo != null) 380.dp else 240.dp)) {
        if (photo != null) {
            Image(
                bitmap = photo,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Box(Modifier.fillMaxSize().background(colors.paper2))
        }
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to Color.Black.copy(alpha = 0.35f),
                        0.3f to colors.paper.copy(alpha = 0f),
                        0.7f to colors.paper.copy(alpha = 0.6f),
                        1f to colors.paper,
                    ),
                ),
        )
        Column(
            modifier = Modifier.align(Alignment.BottomStart).padding(start = 20.dp, end = 20.dp, bottom = 12.dp).arrive(0),
        ) {
            step?.let { StepTag(it, colors) }
            Text(
                courtName,
                color = colors.ink,
                fontSize = 30.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = (-1.2).sp,
                lineHeight = 34.sp,
                modifier = Modifier.padding(top = 8.dp),
            )
            description?.let {
                Text(it, color = colors.ink2, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun StepTag(
    step: RentalStep,
    colors: VaiinillaColors,
) {
    val (background, content) =
        when (step) {
            RentalStep.PLAYING -> colors.accent to colors.accentInk
            RentalStep.BOOKED -> colors.yolk to Color(0xFF3D2B00)
            RentalStep.TO_PAY -> colors.coral to Color.White
            RentalStep.DONE -> colors.paper2 to colors.muted
        }
    Box(
        modifier =
            Modifier
                .height(26.dp)
                .clip(RoundedCornerShape(percent = 50))
                .background(background)
                .padding(horizontal = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(step.title, color = content, fontSize = 12.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
    }
}

@Composable
private fun CourtMenuCard(
    pick: CourtMenuPick,
    place: String,
    spaceDelivery: SpaceDelivery,
    onOpen: () -> Unit,
    colors: VaiinillaColors,
    modifier: Modifier = Modifier,
) {
    val haptics = rememberVaiinillaHaptics()
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .physicalPress(scale = PhysicalPressScale.Small) {
                    haptics.click()
                    onOpen()
                }.clip(RoundedCornerShape(22.dp))
                .background(colors.paper2)
                .semantics { role = Role.Button }
                .testTag(RENTAL_COURT_MENU_TAG)
                .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (pick.imageUrl != null) {
            ProductImage(
                imageUrl = pick.imageUrl,
                contentDescription = "",
                modifier = Modifier.size(62.dp).clip(RoundedCornerShape(14.dp)),
                contentScale = ContentScale.Crop,
            )
        } else {
            Box(
                modifier = Modifier.size(62.dp).clip(RoundedCornerShape(14.dp)).background(colors.paper),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Outlined.Restaurant, contentDescription = null, tint = colors.muted)
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                pick.title,
                color = colors.ink,
                fontSize = 15.sp,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                courtMenuDeliveryLine(spaceDelivery, place),
                color = colors.muted,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
        Box(
            modifier =
                Modifier
                    .height(38.dp)
                    .clip(RoundedCornerShape(percent = 50))
                    .background(colors.ink)
                    .padding(horizontal = 14.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text("Ver menú", color = colors.paper, fontSize = 13.5.sp, fontWeight = FontWeight.Black)
        }
    }
}
