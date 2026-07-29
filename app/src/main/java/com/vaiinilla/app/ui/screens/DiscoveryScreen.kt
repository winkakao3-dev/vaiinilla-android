package com.vaiinilla.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vaiinilla.app.domain.model.PublicEstablishment
import com.vaiinilla.app.ui.components.PhysicalPressScale
import com.vaiinilla.app.ui.components.physicalPress
import com.vaiinilla.app.ui.discovery.DiscoveryUiState
import com.vaiinilla.app.ui.theme.LocalVaiinillaColors

@Composable
fun DiscoveryScreen(
    state: DiscoveryUiState,
    onQueryChange: (String) -> Unit,
    onSpaceTokenChange: (String) -> Unit,
    onSelectEstablishment: (PublicEstablishment) -> Unit,
    onResolveSpace: () -> Unit,
    onConfirmSwitch: () -> Unit,
    onDismissSwitch: () -> Unit,
    onContinueSelected: () -> Unit,
    onOpenDemoRoles: () -> Unit,
) {
    val colors = LocalVaiinillaColors.current

    if (state.pendingSwitch != null) {
        AlertDialog(
            onDismissRequest = onDismissSwitch,
            title = { Text("¿Cambiar de cafetería?", fontWeight = FontWeight.Black) },
            text = {
                Text(
                    "Tu carrito pertenece a otra cafetería. Si continúas, no se mezclará con este pedido — " +
                        "quedará guardado por separado en la cafetería anterior.",
                )
            },
            confirmButton = {
                TextButton(onClick = onConfirmSwitch) {
                    Text("Cambiar", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissSwitch) {
                    Text("Cancelar")
                }
            },
        )
    }

    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(colors.paper),
    ) {
        LazyColumn(
            modifier =
                Modifier
                    .fillMaxSize()
                    .statusBarsPadding(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                Text(
                    "¿Dónde comes hoy?",
                    color = colors.ink,
                    fontSize = 28.sp,
                    lineHeight = 32.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-0.8).sp,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    "Escanea el QR del comedor, busca por nombre o elige de la lista. " +
                        "No necesitas cuenta para ver el menú.",
                    color = colors.muted,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                )
            }

            state.selected?.let { selected ->
                item {
                    Surface(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .physicalPress(
                                    scale = PhysicalPressScale.Default,
                                    onClick = onContinueSelected,
                                ),
                        shape = RoundedCornerShape(20.dp),
                        color = colors.accent.copy(alpha = 0.16f),
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Activo", color = colors.muted, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
                            Text(
                                selected.establishment.name,
                                color = colors.ink,
                                fontWeight = FontWeight.Black,
                                fontSize = 18.sp,
                            )
                            selected.space?.let { space ->
                                Text(
                                    "${space.name} · ${space.type}",
                                    color = colors.muted,
                                    fontSize = 13.sp,
                                )
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                "Seguir al menú",
                                color = colors.ink,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                            )
                        }
                    }
                }
            }

            item {
                OutlinedTextField(
                    value = state.query,
                    onValueChange = onQueryChange,
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                    placeholder = { Text("Buscar cafetería") },
                    singleLine = true,
                    shape = RoundedCornerShape(18.dp),
                )
            }

            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    color = colors.paper2,
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.QrCodeScanner, contentDescription = null, tint = colors.ink)
                            Text(
                                "QR de espacio",
                                modifier = Modifier.padding(start = 8.dp),
                                fontWeight = FontWeight.Black,
                                color = colors.ink,
                            )
                        }
                        Text(
                            "Si el cartel es de mesa o cancha, pega el token del QR. " +
                                "En MOCK usa `mesa4`.",
                            color = colors.muted,
                            fontSize = 12.sp,
                            lineHeight = 16.sp,
                        )
                        OutlinedTextField(
                            value = state.spaceTokenInput,
                            onValueChange = onSpaceTokenChange,
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("token opaco") },
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp),
                        )
                        Surface(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .physicalPress(
                                        scale = PhysicalPressScale.Default,
                                        enabled = !state.resolving,
                                        onClick = onResolveSpace,
                                    ),
                            color = colors.ink,
                            shape = RoundedCornerShape(16.dp),
                        ) {
                            Box(
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 14.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                if (state.resolving) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        strokeWidth = 2.dp,
                                        color = colors.paper,
                                    )
                                } else {
                                    Text("Resolver espacio", color = colors.paper, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            if (state.suspendedMessage != null) {
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        color = colors.coral.copy(alpha = 0.12f),
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Cafetería suspendida", color = colors.ink, fontWeight = FontWeight.Black)
                            Text(
                                state.suspendedMessage,
                                color = colors.muted,
                                fontSize = 13.sp,
                                modifier = Modifier.padding(top = 6.dp),
                            )
                        }
                    }
                }
            }

            if (state.errorMessage != null) {
                item {
                    Text(state.errorMessage, color = colors.coral, fontSize = 13.sp)
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom,
                ) {
                    Text("Cafeterías", color = colors.ink, fontWeight = FontWeight.Black, fontSize = 18.sp)
                    TextButton(onClick = onOpenDemoRoles) {
                        Text("Solo pruebas", color = colors.muted, fontWeight = FontWeight.Bold)
                    }
                }
            }

            if (state.loading && state.establishments.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = colors.ink)
                    }
                }
            }

            items(state.establishments, key = { it.id }) { establishment ->
                EstablishmentCard(
                    establishment = establishment,
                    onClick = { onSelectEstablishment(establishment) },
                )
            }
        }
    }
}

@Composable
private fun EstablishmentCard(
    establishment: PublicEstablishment,
    onClick: () -> Unit,
) {
    val colors = LocalVaiinillaColors.current
    Surface(
        modifier =
            Modifier
                .fillMaxWidth()
                .physicalPress(scale = PhysicalPressScale.Default, onClick = onClick),
        shape = RoundedCornerShape(22.dp),
        color = colors.paper2,
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier =
                    Modifier
                        .size(48.dp)
                        .background(colors.ink, RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Outlined.Storefront, contentDescription = null, tint = colors.paper)
            }
            Column(modifier = Modifier.padding(start = 14.dp).weight(1f)) {
                Text(establishment.name, color = colors.ink, fontWeight = FontWeight.Black, fontSize = 17.sp)
                Text(
                    if (establishment.clientIdRequired) {
                        "${establishment.clientIdLabel} requerida al pedir"
                    } else {
                        "${establishment.clientIdLabel} opcional"
                    },
                    color = colors.muted,
                    fontSize = 12.sp,
                )
            }
        }
    }
}
