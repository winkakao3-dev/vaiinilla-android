package com.vaiinilla.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PointOfSale
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.RoomService
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material.icons.outlined.VpnKey
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vaiinilla.app.domain.mode.AuthorizedMode
import com.vaiinilla.app.domain.mode.AuthorizedModeContext
import com.vaiinilla.app.domain.model.OperationalRole
import com.vaiinilla.app.ui.components.EmptyState
import com.vaiinilla.app.ui.components.SkeletonBlock
import com.vaiinilla.app.ui.components.WalletScreenShell
import com.vaiinilla.app.ui.components.reducedMotion
import com.vaiinilla.app.ui.mode.AuthorizedAccessUiState
import com.vaiinilla.app.ui.theme.LocalVaiinillaColors
import com.vaiinilla.app.ui.theme.VaiinillaColors
import com.vaiinilla.app.ui.theme.VaiinillaTheme
import com.vaiinilla.app.ui.theme.VaiinillaThemeMode
import kotlinx.coroutines.delay
import kotlin.math.absoluteValue

/** Un negocio de la lista de Modos con los roles que la cuenta tiene ahí. */
internal data class ModeBusiness(
    val establishmentId: String,
    val name: String,
    val isSchool: Boolean,
    val active: Boolean,
    val modes: List<AuthorizedMode>,
)

private val ROLE_ORDER =
    listOf(OperationalRole.ADMIN, OperationalRole.CASHIER, OperationalRole.KITCHEN, OperationalRole.WAITER)

/** El negocio donde está la cuenta ahora va primero; los demás conservan el orden del servidor. */
internal fun groupModesByBusiness(
    staffModes: List<AuthorizedMode>,
    activeContext: AuthorizedModeContext?,
): List<ModeBusiness> {
    val businesses =
        staffModes
            .groupBy { it.establishmentId }
            .map { (id, modes) ->
                ModeBusiness(
                    establishmentId = id,
                    name = modes.first().establishmentName,
                    isSchool = modes.any { it.isSchool },
                    active = modes.all { it.establishmentActive },
                    modes =
                        modes.sortedBy {
                            ROLE_ORDER.indexOf(it.role).let { i ->
                                if (i <
                                    0
                                ) {
                                    ROLE_ORDER.size
                                } else {
                                    i
                                }
                            }
                        },
                )
            }
    val activeId = activeContext?.establishmentId ?: return businesses
    return businesses.sortedByDescending { it.establishmentId == activeId }
}

internal fun AuthorizedMode.isActiveIn(context: AuthorizedModeContext?): Boolean =
    context != null &&
        context.role == role &&
        context.establishmentId == establishmentId &&
        context.membershipId == membershipId

private const val COLLAPSED_BUSINESSES = 5

@Composable
fun AuthorizedModeScreen(
    state: AuthorizedAccessUiState,
    onBack: (() -> Unit)?,
    onSelectMode: (AuthorizedMode) -> Unit,
    onSignOut: () -> Unit = {},
    onRetry: () -> Unit = {},
    isPlatformAuthority: Boolean = false,
    onOpenPlatform: () -> Unit = {},
    onContinueAsClient: () -> Unit = {},
) {
    var confirmingSignOut by remember { mutableStateOf(false) }
    if (confirmingSignOut) {
        SignOutConfirmDialog(onConfirm = onSignOut, onDismiss = { confirmingSignOut = false })
    }
    val colors = LocalVaiinillaColors.current
    val staffModes = state.modes.filter { it.role != OperationalRole.CLIENT }
    val clientMode = state.modes.firstOrNull { it.role == OperationalRole.CLIENT }
    val businesses = groupModesByBusiness(staffModes, state.activeContext)
    val activeMode = staffModes.firstOrNull { it.isActiveIn(state.activeContext) }
    var query by rememberSaveable { mutableStateOf("") }
    var expanded by rememberSaveable { mutableStateOf(false) }
    val searchable = businesses.size > COLLAPSED_BUSINESSES
    val matches =
        if (query.isBlank()) businesses else businesses.filter { it.name.contains(query.trim(), ignoreCase = true) }
    val showAll = expanded || query.isNotBlank()

    WalletScreenShell(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    // L01: las barras van antes del scroll; si no, al desplazarse el contenido pasa bajo la de estado.
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = 28.dp),
        ) {
            if (onBack != null) {
                Box(
                    modifier =
                        Modifier
                            .padding(start = 20.dp, top = 8.dp)
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(colors.paper2)
                            .clickable(onClickLabel = "Volver", onClick = onBack),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Volver", tint = colors.ink)
                }
            }
            Column(modifier = Modifier.padding(horizontal = 20.dp).padding(top = 18.dp)) {
                Column(Modifier.modeArrive(0)) {
                    Text(
                        "MODOS",
                        color = colors.muted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.2.sp,
                    )
                    Text(
                        "¿Dónde trabajas hoy?",
                        color = colors.ink,
                        fontSize = 32.sp,
                        lineHeight = 34.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = (-1.2).sp,
                        modifier = Modifier.padding(top = 8.dp).semantics { heading() },
                    )
                    Text(
                        "Elige el negocio y con qué rol entras.",
                        color = colors.muted,
                        fontSize = 14.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }

                if (isPlatformAuthority) {
                    Column(
                        modifier =
                            Modifier
                                .padding(top = 20.dp)
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(24.dp))
                                .background(colors.accent)
                                .clickable(role = Role.Button, onClick = onOpenPlatform)
                                .padding(18.dp),
                    ) {
                        Text("Plataforma", color = colors.accentInk, fontSize = 18.sp, fontWeight = FontWeight.Black)
                        Text(
                            "Crea tiendas y asígnales su administrador. No pertenece a ningún negocio.",
                            color = colors.accentInk,
                            fontSize = 13.5.sp,
                            lineHeight = 19.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(top = 5.dp),
                        )
                    }
                }

                if (state.loading && staffModes.isEmpty()) {
                    SkeletonBlock(
                        modifier = Modifier.padding(top = 22.dp).fillMaxWidth().height(120.dp),
                        corner = 22.dp,
                    )
                }
                if (!state.loading && staffModes.isEmpty() && state.errorMessage == null && !isPlatformAuthority) {
                    EmptyState(
                        icon = Icons.Outlined.VpnKey,
                        title = "Sin modos de personal",
                        message = "Cuando administración te invite a Caja, Cocina o Mesero, aparecen aquí.",
                        modifier = Modifier.padding(top = 22.dp),
                    )
                }

                if (activeMode != null) {
                    CurrentModeCard(
                        mode = activeMode,
                        enabled = !state.loading,
                        onContinue = { onSelectMode(activeMode) },
                        modifier = Modifier.padding(top = 20.dp).modeArrive(1),
                    )
                }

                if (searchable) {
                    BusinessSearchField(
                        query = query,
                        onQueryChange = { query = it },
                        modifier = Modifier.padding(top = 22.dp).modeArrive(2),
                    )
                }

                if (businesses.isNotEmpty()) {
                    Row(
                        modifier =
                            Modifier.fillMaxWidth().padding(
                                start = 4.dp,
                                end = 4.dp,
                                top = 22.dp,
                                bottom = 10.dp,
                            ),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        SectionCaption("Tus negocios")
                        Text(
                            "${businesses.size}",
                            color = colors.muted,
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
                Column(
                    modifier = Modifier.animateContentSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    matches.forEachIndexed { index, business ->
                        AnimatedVisibility(
                            visible = showAll || index < COLLAPSED_BUSINESSES,
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically(),
                        ) {
                            BusinessModesCard(
                                business = business,
                                activeContext = state.activeContext,
                                enabled = !state.loading,
                                onSelectMode = onSelectMode,
                                modifier = Modifier.modeArrive(3 + index.coerceAtMost(COLLAPSED_BUSINESSES)),
                            )
                        }
                    }
                }
                if (query.isNotBlank() && matches.isEmpty()) {
                    Text(
                        "Ningún negocio se llama así.",
                        color = colors.muted,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(top = 6.dp, start = 4.dp),
                    )
                }
                if (query.isBlank() && businesses.size > COLLAPSED_BUSINESSES) {
                    val hidden = businesses.size - COLLAPSED_BUSINESSES
                    TextButton(
                        onClick = { expanded = !expanded },
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    ) {
                        Text(
                            if (expanded) {
                                "Ver menos"
                            } else if (hidden ==
                                1
                            ) {
                                "Ver el otro negocio"
                            } else {
                                "Ver los otros $hidden negocios"
                            },
                            color = colors.ink2,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                        )
                    }
                }

                adminCoverNote(staffModes)?.let { note ->
                    Text(
                        note,
                        color = colors.muted,
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        modifier = Modifier.padding(top = 14.dp),
                    )
                }
                state.errorMessage?.let { error ->
                    Surface(
                        color = colors.coral.copy(alpha = 0.16f),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.padding(top = 18.dp),
                    ) {
                        Text(
                            error,
                            color = colors.ink,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(14.dp),
                        )
                    }
                    TextButton(onClick = onRetry, enabled = !state.loading, modifier = Modifier.heightIn(min = 48.dp)) {
                        Text("Reintentar", color = colors.ink, fontWeight = FontWeight.Black)
                    }
                }
                state.message?.let { message ->
                    Text(
                        message,
                        color = colors.muted,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(top = 14.dp),
                    )
                }

                // Sin acceso de cliente no hay a dónde volver: antes este botón cerraba la sesión sin avisar.
                Box(Modifier.padding(start = 4.dp, top = 24.dp, bottom = 10.dp)) { SectionCaption("Cliente") }
                ClientModeCard(onClick = { clientMode?.let(onSelectMode) ?: onContinueAsClient() })
                Box(
                    modifier =
                        Modifier
                            .padding(top = 18.dp)
                            .fillMaxWidth()
                            .height(54.dp)
                            .pressClickable { confirmingSignOut = true }
                            .clip(RoundedCornerShape(99.dp))
                            .border(1.5.dp, colors.line, RoundedCornerShape(99.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("Cerrar sesión", color = colors.coral, fontWeight = FontWeight.ExtraBold, fontSize = 15.5.sp)
                }
            }
        }
    }
}

@Composable
private fun SectionCaption(text: String) {
    Text(
        text.uppercase(),
        color = LocalVaiinillaColors.current.muted,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 2.2.sp,
    )
}

@Composable
private fun CurrentModeCard(
    mode: AuthorizedMode,
    enabled: Boolean,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalVaiinillaColors.current
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(colors.paper2)
                .border(1.5.dp, colors.accent, RoundedCornerShape(24.dp))
                .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.size(46.dp).clip(RoundedCornerShape(15.dp)).background(colors.accent),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                modeRoleIcon(mode.role),
                contentDescription = null,
                tint = colors.accentInk,
                modifier = Modifier.size(24.dp),
            )
        }
        Column(Modifier.weight(1f).padding(horizontal = 14.dp)) {
            Text(
                "AHORA ESTÁS EN",
                color = if (colors.isDark) colors.accent else colors.ink2,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 0.3.sp,
            )
            Text(
                mode.role.label,
                color = colors.ink,
                fontWeight = FontWeight.Black,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                // L01: con letra grande «Administración» se cortaba; se encoge antes de cortarse.
                autoSize = TextAutoSize.StepBased(minFontSize = 12.sp, maxFontSize = 17.sp, stepSize = 0.5.sp),
                modifier = Modifier.padding(top = 2.dp),
            )
            Text(
                mode.establishmentName,
                color = colors.muted,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Row(
            modifier =
                Modifier
                    .height(42.dp)
                    .pressClickable(enabled, onClickLabel = "Seguir como ${mode.role.label}", onClick = onContinue)
                    .clip(RoundedCornerShape(99.dp))
                    .background(colors.accent)
                    .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Seguir", color = colors.accentInk, fontSize = 14.sp, fontWeight = FontWeight.Black)
            Spacer(Modifier.width(6.dp))
            Icon(
                Icons.AutoMirrored.Outlined.ArrowForward,
                contentDescription = null,
                tint = colors.accentInk,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

@Composable
private fun BusinessSearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalVaiinillaColors.current
    BasicTextField(
        value = query,
        onValueChange = onQueryChange,
        singleLine = true,
        textStyle = TextStyle(color = colors.ink, fontSize = 15.sp, fontWeight = FontWeight.SemiBold),
        cursorBrush = SolidColor(colors.accent),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        modifier = modifier.fillMaxWidth().semantics { contentDescription = "Buscar negocio" },
        decorationBox = { field ->
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(colors.paper2)
                        .border(1.5.dp, colors.line, RoundedCornerShape(16.dp))
                        .padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.Outlined.Search,
                    contentDescription = null,
                    tint = colors.muted,
                    modifier = Modifier.size(20.dp),
                )
                Box(Modifier.weight(1f).padding(start = 10.dp)) {
                    if (query.isEmpty()) {
                        Text(
                            "Buscar negocio",
                            color = colors.muted.copy(alpha = 0.7f),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                    field()
                }
            }
        },
    )
}

private val AVATAR_TINTS =
    listOf(Color(0xFFB9D86D), Color(0xFFFFD15B), Color(0xFF8FC7E8), Color(0xFFF2A7A0), Color(0xFFDEDBD0))

@Composable
private fun BusinessModesCard(
    business: ModeBusiness,
    activeContext: AuthorizedModeContext?,
    enabled: Boolean,
    onSelectMode: (AuthorizedMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalVaiinillaColors.current
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(colors.paper2)
                .padding(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier =
                    Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(13.dp))
                        .background(
                            AVATAR_TINTS[business.establishmentId.hashCode().absoluteValue % AVATAR_TINTS.size],
                        ),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    business.name
                        .trim()
                        .firstOrNull()
                        ?.uppercase() ?: "·",
                    color = Color(0xFF1D1E1C),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Black,
                )
            }
            Column(Modifier.padding(start = 12.dp)) {
                Text(
                    business.name,
                    color = colors.ink,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                businessNote(business)?.let { note ->
                    Text(
                        note,
                        color = colors.muted,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
            }
        }
        // Cuatro roles no caben a lo ancho de un teléfono angosto con "Administración": van de dos en dos. Con letra
        // grande ni así: uno por renglón, para que «Administración» no se corte (L01).
        val fontScale = LocalDensity.current.fontScale
        val perRow = modeButtonsPerRow(business.modes.size, fontScale)
        business.modes.chunked(perRow).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                row.forEach { mode ->
                    RoleButton(
                        mode = mode,
                        active = mode.isActiveIn(activeContext),
                        enabled = enabled,
                        onClick = { onSelectMode(mode) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

/** Cuántos botones de rol van por renglón: con letra grande, uno por renglón. */
internal fun modeButtonsPerRow(
    count: Int,
    fontScale: Float,
): Int =
    when {
        fontScale >= 1.2f -> 1
        count == 4 -> 2
        else -> 3
    }

private fun businessNote(business: ModeBusiness): String? =
    when {
        !business.active -> "En pausa"
        business.isSchool -> "Escuela"
        else -> null
    }

@Composable
private fun RoleButton(
    mode: AuthorizedMode,
    active: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalVaiinillaColors.current
    val reduce = reducedMotion()
    val background by animateColorAsState(
        if (active) colors.accent else colors.chipFill(),
        animationSpec = if (reduce) snap() else spring(stiffness = 500f),
        label = "role-fill",
    )
    val content by animateColorAsState(
        if (active) colors.accentInk else colors.ink2,
        animationSpec = if (reduce) snap() else spring(stiffness = 500f),
        label = "role-ink",
    )
    val verb = if (active) "Continuar" else "Entrar"
    Row(
        modifier =
            modifier
                .height(46.dp)
                .pressClickable(enabled, onClick = onClick)
                .clip(RoundedCornerShape(14.dp))
                .background(background)
                .semantics { contentDescription = "$verb como ${mode.role.label} en ${mode.establishmentName}" }
                .padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(modeRoleIcon(mode.role), contentDescription = null, tint = content, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(7.dp))
        Text(
            mode.role.label,
            color = content,
            fontWeight = FontWeight.ExtraBold,
            maxLines = 1,
            // «Administración» no cabe a 14 en un teléfono angosto: se encoge en vez de cortarse.
            autoSize = TextAutoSize.StepBased(minFontSize = 11.sp, maxFontSize = 14.sp),
        )
    }
}

private fun VaiinillaColors.chipFill(): Color = ink.copy(alpha = if (isDark) 0.07f else 0.06f)

@Composable
private fun ClientModeCard(onClick: () -> Unit) {
    val colors = LocalVaiinillaColors.current
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .pressClickable(onClick = onClick)
                .clip(RoundedCornerShape(22.dp))
                .background(colors.paper2)
                .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.size(46.dp).clip(RoundedCornerShape(15.dp)).background(colors.chipFill()),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Outlined.Person, contentDescription = null, tint = colors.ink, modifier = Modifier.size(22.dp))
        }
        Column(Modifier.weight(1f).padding(horizontal = 14.dp)) {
            Text("Pedir como cliente", color = colors.ink, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
            Text(
                "Tu menú, tu carrito y tu saldo",
                color = colors.muted,
                fontSize = 12.5.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
        Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, contentDescription = null, tint = colors.muted)
    }
}

private fun modeRoleIcon(role: OperationalRole): ImageVector =
    when (role) {
        OperationalRole.CASHIER -> Icons.Outlined.PointOfSale
        OperationalRole.KITCHEN -> Icons.Outlined.Restaurant
        OperationalRole.WAITER -> Icons.Outlined.RoomService
        OperationalRole.ADMIN -> Icons.Outlined.Storefront
        OperationalRole.CLIENT -> Icons.Outlined.Person
    }

@Composable
private fun Modifier.pressClickable(
    enabled: Boolean = true,
    onClickLabel: String? = null,
    onClick: () -> Unit,
): Modifier {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed && enabled) 0.95f else 1f,
        animationSpec = if (reducedMotion()) snap() else spring(dampingRatio = 0.45f, stiffness = 700f),
        label = "mode-press",
    )
    return this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }.clickable(
            interactionSource = source,
            indication = null,
            enabled = enabled,
            onClickLabel = onClickLabel,
            role = Role.Button,
            onClick = onClick,
        )
}

/** Entrada escalonada: cada bloque sube un poco y aparece, uno detrás de otro. */
@Composable
private fun Modifier.modeArrive(index: Int): Modifier {
    val reduce = reducedMotion()
    // rememberSaveable, como adminRise: la entrada sale una vez y no se repite al volver a componer.
    var shown by rememberSaveable { mutableStateOf(reduce) }
    val progress = remember { Animatable(if (shown) 1f else 0f) }
    LaunchedEffect(Unit) {
        if (!shown) {
            delay(index * 45L)
            progress.animateTo(1f, spring(dampingRatio = 0.85f, stiffness = 320f))
            shown = true
        }
    }
    return graphicsLayer {
        alpha = progress.value.coerceIn(0f, 1f)
        translationY = (1f - progress.value) * 14.dp.toPx()
    }
}

@Preview(name = "Modos autorizados", showBackground = true, widthDp = 411, heightDp = 891)
@Composable
private fun AuthorizedModeScreenPreview() {
    VaiinillaTheme(themeMode = VaiinillaThemeMode.Dark) {
        AuthorizedModeScreen(
            state =
                AuthorizedAccessUiState(
                    modes =
                        listOf(OperationalRole.CASHIER, OperationalRole.KITCHEN, OperationalRole.WAITER).mapIndexed {
                            i,
                            role,
                            ->
                            AuthorizedMode(
                                role,
                                establishmentId = "e1",
                                establishmentName = "Cafetería central",
                                membershipId = "m$i",
                            )
                        },
                ),
            onBack = {},
            onSelectMode = {},
        )
    }
}
