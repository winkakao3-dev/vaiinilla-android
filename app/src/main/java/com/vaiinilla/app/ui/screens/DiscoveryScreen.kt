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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vaiinilla.app.core.config.DemoFeatures
import com.vaiinilla.app.domain.model.PublicEstablishment
import com.vaiinilla.app.ui.components.EditorialAccentButton
import com.vaiinilla.app.ui.components.EditorialConfirmSheet
import com.vaiinilla.app.ui.components.EditorialHero
import com.vaiinilla.app.ui.components.EditorialPrimaryButton
import com.vaiinilla.app.ui.components.EditorialSearchField
import com.vaiinilla.app.ui.components.EditorialSectionHead
import com.vaiinilla.app.ui.components.PhysicalPressScale
import com.vaiinilla.app.ui.components.physicalPress
import com.vaiinilla.app.ui.discovery.DiscoveryUiState
import com.vaiinilla.app.ui.theme.LocalVaiinillaColors
import com.vaiinilla.app.ui.theme.VaiinillaTheme
import com.vaiinilla.app.ui.theme.VaiinillaThemeMode

@Composable
fun DiscoveryScreen(
    state: DiscoveryUiState,
    onQueryChange: (String) -> Unit,
    onSpaceTokenChange: (String) -> Unit,
    onOpenQrScanner: () -> Unit = {},
    onSelectEstablishment: (PublicEstablishment) -> Unit,
    onResolveSpace: () -> Unit,
    onConfirmSwitch: () -> Unit,
    onDismissSwitch: () -> Unit,
    onContinueSelected: () -> Unit,
    onOpenDemoRoles: () -> Unit,
    showMockHint: Boolean = true,
) {
    val colors = LocalVaiinillaColors.current

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
                EditorialHero(
                    eyebrow = "Comedor conectado",
                    title = "¿Dónde comes hoy?",
                    body =
                        "Escanea el QR del comedor, busca por nombre o elige de la lista. " +
                            "No necesitas cuenta para ver el menú.",
                    watermark = "V",
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
                        shape = RoundedCornerShape(28.dp),
                        color = colors.accent,
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Text(
                                "ACTIVO",
                                color = colors.accentInk.copy(alpha = 0.65f),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 1.sp,
                            )
                            Text(
                                selected.establishment.name,
                                color = colors.accentInk,
                                fontWeight = FontWeight.Black,
                                fontSize = 22.sp,
                                lineHeight = 24.sp,
                                modifier = Modifier.padding(top = 4.dp),
                            )
                            selected.space?.let { space ->
                                Text(
                                    "${space.name} · ${space.type}",
                                    color = colors.accentInk.copy(alpha = 0.72f),
                                    fontSize = 13.sp,
                                    modifier = Modifier.padding(top = 4.dp),
                                )
                            }
                            Spacer(modifier = Modifier.height(14.dp))
                            EditorialPrimaryButton(
                                text = "Seguir al menú",
                                onClick = onContinueSelected,
                                background = colors.ink,
                                contentColor = colors.paper,
                            )
                        }
                    }
                }
            }

            item {
                EditorialSearchField(
                    value = state.query,
                    onValueChange = onQueryChange,
                    placeholder = "Buscar cafetería",
                )
            }

            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(28.dp),
                    color = colors.paper2,
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.QrCodeScanner, contentDescription = null, tint = colors.ink)
                            Text(
                                "QR de espacio",
                                modifier = Modifier.padding(start = 8.dp),
                                fontWeight = FontWeight.Black,
                                color = colors.ink,
                                fontSize = 16.sp,
                            )
                        }
                        Text(
                            if (showMockHint) {
                                "Si el cartel es de mesa o cancha, pega el token del QR. En MOCK usa mesa4."
                            } else {
                                "Si el cartel es de mesa o cancha, pega el token opaco que contiene su QR."
                            },
                            color = colors.muted,
                            fontSize = 12.sp,
                            lineHeight = 16.sp,
                        )
                        EditorialAccentButton(
                            text = "Escanear QR",
                            onClick = onOpenQrScanner,
                        )
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = colors.paper,
                            shape = RoundedCornerShape(19.dp),
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 15.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                BasicTextField(
                                    value = state.spaceTokenInput,
                                    onValueChange = onSpaceTokenChange,
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true,
                                    textStyle = TextStyle(color = colors.ink, fontSize = 14.sp),
                                    decorationBox = { input ->
                                        Box {
                                            if (state.spaceTokenInput.isBlank()) {
                                                Text("token opaco", color = colors.muted, fontSize = 14.sp)
                                            }
                                            input()
                                        }
                                    },
                                )
                            }
                        }
                        if (state.resolving) {
                            Box(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(22.dp),
                                    strokeWidth = 2.dp,
                                    color = colors.ink,
                                )
                            }
                        } else {
                            EditorialAccentButton(text = "Resolver espacio", onClick = onResolveSpace)
                        }
                    }
                }
            }

            if (state.suspendedMessage != null) {
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(28.dp),
                        color = colors.coral.copy(alpha = 0.14f),
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Text(
                                "Cafetería suspendida",
                                color = colors.ink,
                                fontWeight = FontWeight.Black,
                                fontSize = 18.sp,
                            )
                            Text(
                                state.suspendedMessage,
                                color = colors.muted,
                                fontSize = 13.sp,
                                lineHeight = 18.sp,
                                modifier = Modifier.padding(top = 6.dp),
                            )
                        }
                    }
                }
            }

            if (state.errorMessage != null) {
                item {
                    Text(state.errorMessage, color = colors.coral, fontSize = 13.sp, lineHeight = 18.sp)
                }
            }

            item {
                EditorialSectionHead(
                    title = "Cafeterías",
                    trailing = if (DemoFeatures.toolsAvailable) "Solo pruebas" else null,
                    onTrailingClick = if (DemoFeatures.toolsAvailable) onOpenDemoRoles else null,
                    modifier = Modifier.padding(top = 8.dp),
                )
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

        if (state.pendingSwitch != null) {
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .background(colors.paper.copy(alpha = 0.35f)),
                contentAlignment = Alignment.Center,
            ) {
                EditorialConfirmSheet(
                    title = "¿Cambiar de cafetería?",
                    message =
                        "Tu carrito pertenece a otra cafetería. Si continúas, no se mezclará con este pedido — " +
                            "quedará guardado por separado en la cafetería anterior.",
                    confirmLabel = "Cambiar",
                    dismissLabel = "Cancelar",
                    onConfirm = onConfirmSwitch,
                    onDismiss = onDismissSwitch,
                )
            }
        }
    }
}

@Preview(name = "Descubrir cafetería", showBackground = true, widthDp = 411, heightDp = 891)
@Composable
private fun DiscoveryScreenPreview() {
    VaiinillaTheme(themeMode = VaiinillaThemeMode.Light) {
        DiscoveryScreen(
            state = DiscoveryUiState(),
            onQueryChange = {},
            onSpaceTokenChange = {},
            onSelectEstablishment = {},
            onResolveSpace = {},
            onConfirmSwitch = {},
            onDismissSwitch = {},
            onContinueSelected = {},
            onOpenDemoRoles = {},
        )
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
        shape = RoundedCornerShape(28.dp),
        color = colors.paper2,
    ) {
        Row(
            modifier = Modifier.padding(17.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier =
                    Modifier
                        .size(45.dp)
                        .background(colors.ink, RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Outlined.Storefront, contentDescription = null, tint = colors.paper)
            }
            Column(modifier = Modifier.padding(start = 14.dp).weight(1f)) {
                Text(establishment.name, color = colors.ink, fontWeight = FontWeight.Black, fontSize = 19.sp)
                Text(
                    if (establishment.clientIdRequired) {
                        "${establishment.clientIdLabel} requerida al pedir"
                    } else {
                        "${establishment.clientIdLabel} opcional"
                    },
                    color = colors.muted,
                    fontSize = 12.sp,
                    lineHeight = 15.sp,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
    }
}
