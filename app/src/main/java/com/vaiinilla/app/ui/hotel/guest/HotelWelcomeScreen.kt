package com.vaiinilla.app.ui.hotel.guest

import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.CleaningServices
import androidx.compose.material.icons.outlined.Receipt
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vaiinilla.app.domain.hotel.Stay
import com.vaiinilla.app.domain.model.Venue
import com.vaiinilla.app.ui.components.CremeHero
import com.vaiinilla.app.ui.order.OrderFlowViewModel
import com.vaiinilla.app.ui.theme.LocalVaiinillaColors

/**
 * H01: bienvenida del huésped. El nombre y la foto del hotel salen del lugar que abrió el QR; la estancia, del
 * servidor. El contenido sin estado está en [HotelWelcomeContent] para poder verlo con datos de prueba.
 */
@Composable
fun HotelWelcomeScreen(
    onOrder: () -> Unit = {},
    onRequests: () -> Unit = {},
    onAccount: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: HotelStayViewModel = viewModel(),
) {
    LaunchedEffect(Unit) { viewModel.load() }
    val venue = activityOrderFlow()?.uiState?.value?.guestVenue
    val state = viewModel.state.value
    HotelWelcomeContent(
        venueName = venue?.establishment?.name.orEmpty(),
        photoUrl = venue?.establishment?.photoUrl,
        stay = state.stay,
        onOrder = onOrder,
        onRequests = onRequests,
        onAccount = onAccount,
        modifier = modifier,
    )
}

@Composable
fun HotelWelcomeContent(
    venueName: String,
    photoUrl: String?,
    stay: Stay?,
    onOrder: () -> Unit,
    onRequests: () -> Unit,
    onAccount: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalVaiinillaColors.current
    Column(modifier = modifier.fillMaxSize().background(colors.paper)) {
        CremeHero(
            height = 340.dp,
            photoUrl = photoUrl,
            contentDescription = venueName,
            leading = { if (venueName.isNotBlank()) HotelVenuePill(venueName) },
        ) {
            Text(
                stay?.guestName?.let { "Hola, ${firstName(it)}" } ?: "Te damos la bienvenida",
                color = colors.muted,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(
                stay?.roomName ?: Venue.roomTitle,
                color = colors.ink,
                fontSize = 52.sp,
                lineHeight = 52.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = (-2).sp,
                modifier = Modifier.padding(top = 4.dp),
            )
            val details =
                listOfNotNull(
                    stayRange(stay?.checkIn, stay?.checkOut)?.let { "Estancia $it" },
                    stay?.floor?.let { "Piso $it" },
                ).joinToString(" · ")
            if (details.isNotEmpty()) {
                Text(details, color = colors.muted, fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp))
            }
        }
        Column(modifier = Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                "¿Qué necesitas?",
                color = colors.ink,
                fontSize = 20.sp,
                fontWeight = FontWeight.Black,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                HotelActionTile("Room service", "Desayuno, cena y bebidas", Icons.Outlined.Restaurant, onOrder, Modifier.weight(1f), "hotel_tile_room_service")
                HotelActionTile("Limpieza", "Toallas, amenidades", Icons.Outlined.CleaningServices, onRequests, Modifier.weight(1f), "hotel_tile_cleaning")
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                HotelActionTile("Salida tardía", "Pide más tiempo", Icons.Outlined.AccessTime, onRequests, Modifier.weight(1f), "hotel_tile_late")
                HotelActionTile("Mi cuenta", "Cargos a la habitación", Icons.Outlined.Receipt, onAccount, Modifier.weight(1f), "hotel_tile_account")
            }
        }
        Spacer(Modifier.weight(1f))
        HotelPrimaryPill(
            label = "Pedir al cuarto",
            onClick = onOrder,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp).navigationBarsPadding(),
            testTag = "hotel_order_to_room",
        )
    }
}

/** El nombre del hotel en vidrio oscuro sobre la foto. */
@Composable
private fun HotelVenuePill(name: String) {
    Box(
        modifier =
            Modifier
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.42f))
                .padding(horizontal = 16.dp, vertical = 10.dp),
    ) {
        Text(name, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** El flujo del pedido vive en la actividad (lo comparte toda la navegación); aquí solo se lee el lugar abierto. */
@Composable
internal fun activityOrderFlow(): OrderFlowViewModel? {
    val activity = LocalContext.current.findComponentActivity() ?: return null
    return viewModel(viewModelStoreOwner = activity)
}

private tailrec fun android.content.Context.findComponentActivity(): ComponentActivity? =
    when (this) {
        is ComponentActivity -> this
        is android.content.ContextWrapper -> baseContext.findComponentActivity()
        else -> null
    }
