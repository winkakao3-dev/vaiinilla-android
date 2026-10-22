package com.vaiinilla.app.ui.parentmode

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vaiinilla.app.domain.guardianship.Dependent
import com.vaiinilla.app.domain.guardianship.compactMoney
import com.vaiinilla.app.ui.components.PhysicalPressScale
import com.vaiinilla.app.ui.components.SkeletonBlock
import com.vaiinilla.app.ui.components.physicalPress
import com.vaiinilla.app.ui.components.reducedMotion
import com.vaiinilla.app.ui.components.rememberVaiinillaHaptics
import com.vaiinilla.app.ui.theme.LocalVaiinillaColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Lo que el host del Modo Padre tiene abierto encima de la cartera. */
private enum class ParentSheetKind { ADD, CONSENT }

/**
 * El Modo Padre dentro de la cartera, solo en escuelas. Pinta la cartera con [content] (que recibe la
 * sección "Mis hijos" para ponerla bajo el saldo) y encima, a pantalla completa, la ficha de un hijo
 * u Opciones avanzadas. Sin escuela, la cartera queda igual que siempre.
 */
@Composable
fun ParentModeWalletHost(
    enabled: Boolean,
    childrenViewModel: ChildrenViewModel,
    /** Una página del Modo Padre tapa la cartera: la barra de abajo se esconde mientras tanto. */
    onFullScreenChange: (Boolean) -> Unit,
    /** Tras reclamar su cuenta, el alumno vuelve a pedir el contexto de cliente con su Google. */
    onAccountClaimed: suspend () -> Unit,
    content: @Composable (familySection: (@Composable () -> Unit)?) -> Unit,
) {
    if (!enabled) {
        content(null)
        return
    }
    val children = childrenViewModel.state
    val reduced = reducedMotion()
    val scope = rememberCoroutineScope()
    var openChildId by rememberSaveable { mutableStateOf<String?>(null) }
    var showsOptions by rememberSaveable { mutableStateOf(false) }
    var sheet by rememberSaveable { mutableStateOf<ParentSheetKind?>(null) }
    val fullScreen = openChildId != null || showsOptions
    LaunchedEffect(fullScreen) { onFullScreenChange(fullScreen) }
    LaunchedEffect(Unit) {
        children.load()
        if (!children.offeredConsent && children.childrenNeedingConsent.isNotEmpty()) {
            children.offeredConsent = true
            sheet = ParentSheetKind.CONSENT
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        content {
            WalletFamilySection(
                children = children,
                onOpen = { openChildId = it.userId },
                onAdd = { sheet = ParentSheetKind.ADD },
                onConsent = { sheet = ParentSheetKind.CONSENT },
                onOptions = { showsOptions = true },
            )
        }
        FullScreenPage(visible = showsOptions && openChildId == null, reduced = reduced) {
            BackHandler { showsOptions = false }
            SchoolOptionsPage(
                children = children,
                onAccountClaimed = onAccountClaimed,
                onBack = { showsOptions = false },
            )
        }
        val openChild = children.children.firstOrNull { it.userId == openChildId && !it.isAwaitingSchool }
        FullScreenPage(visible = openChild != null, reduced = reduced) {
            if (openChild != null) {
                BackHandler { openChildId = null }
                ChildAccountPage(
                    child = openChild,
                    services = children.services,
                    onChildChanged = children::upsert,
                    onUnlinked = { id ->
                        openChildId = null
                        scope.launch {
                            // Que la fila se vaya cuando la ficha ya regresó.
                            delay(450)
                            children.remove(id)
                        }
                    },
                    onBack = { openChildId = null },
                )
            }
        }
    }

    when (sheet) {
        ParentSheetKind.ADD ->
            AddChildSheet(children.services.guardianship, onAdded = children::upsert, onDismiss = {
                sheet =
                    null
            })
        ParentSheetKind.CONSENT -> PrivacyConsentSheet(children = children, onDismiss = { sheet = null })
        null -> Unit
    }
}

@Composable
private fun BoxScope.FullScreenPage(
    visible: Boolean,
    reduced: Boolean,
    page: @Composable () -> Unit,
) {
    AnimatedVisibility(
        visible = visible,
        enter =
            if (reduced) {
                fadeIn(snap())
            } else {
                slideInHorizontally(spring(dampingRatio = 0.9f, stiffness = 400f)) {
                    it / 3
                } + fadeIn()
            },
        exit =
            if (reduced) {
                fadeOut(
                    snap(),
                )
            } else {
                slideOutHorizontally(spring(stiffness = 500f)) { it / 3 } + fadeOut()
            },
        modifier = Modifier.matchParentSize(),
    ) {
        Box(modifier = Modifier.fillMaxSize().background(LocalVaiinillaColors.current.paper)) { page() }
    }
}

/**
 * "Mis hijos" sale solo a quien ya tiene hijos; para los demás, lo de papás y alumnos vive en una
 * fila discreta de Opciones avanzadas, para que nadie vea "¿eres papá?" al pagar.
 */
@Composable
internal fun WalletFamilySection(
    children: ChildrenState,
    onOpen: (Dependent) -> Unit,
    onAdd: () -> Unit,
    onConsent: () -> Unit,
    onOptions: () -> Unit,
) {
    val colors = LocalVaiinillaColors.current
    val haptics = rememberVaiinillaHaptics()
    val reduced = reducedMotion()
    val appear =
        if (reduced) {
            fadeIn(snap())
        } else {
            fadeIn() +
                expandVertically(spring(dampingRatio = 0.86f, stiffness = 380f))
        }
    val leave = if (reduced) fadeOut(snap()) else fadeOut() + shrinkVertically()
    Column(modifier = Modifier.fillMaxWidth().padding(top = 26.dp)) {
        AnimatedVisibility(visible = children.childrenNeedingConsent.isNotEmpty(), enter = appear, exit = leave) {
            Column {
                PendingConsentCard(
                    children = children.childrenNeedingConsent,
                    ink = colors.ink,
                    muted = colors.muted,
                    surface = colors.paper2,
                    line = colors.line,
                    onOpen = onConsent,
                )
                Spacer(Modifier.height(14.dp))
            }
        }
        AnimatedVisibility(
            visible = children.children.isNotEmpty() || children.error != null,
            enter = appear,
            exit = leave,
        ) {
            Column(modifier = Modifier.padding(bottom = 22.dp)) {
                ChildrenList(
                    children = children,
                    onOpen = {
                        haptics.click()
                        onOpen(it)
                    },
                    onAdd = onAdd,
                )
            }
        }
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .testTag("wallet-advanced-options")
                    .physicalPress(scale = PhysicalPressScale.Small, onClick = onOptions)
                    .padding(vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Outlined.Tune, contentDescription = null, tint = colors.muted, modifier = Modifier.size(16.dp))
            Text(
                "Opciones avanzadas",
                color = colors.muted,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                modifier = Modifier.weight(1f).padding(start = 10.dp),
            )
            Icon(
                Icons.Outlined.ChevronRight,
                contentDescription = null,
                tint = colors.muted,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

/** Una fila por hijo con su saldo, y una para agregar otro. */
@Composable
private fun ChildrenList(
    children: ChildrenState,
    onOpen: (Dependent) -> Unit,
    onAdd: () -> Unit,
) {
    val colors = LocalVaiinillaColors.current
    val scope = rememberCoroutineScope()
    Text(
        "Mis hijos",
        color = colors.ink,
        fontWeight = FontWeight.Black,
        fontSize = 19.sp,
        modifier = Modifier.padding(bottom = 4.dp),
    )
    val error = children.error
    when {
        !children.hasLoaded ->
            repeat(
                2,
            ) {
                SkeletonBlock(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp).height(52.dp),
                    corner = 15.dp,
                )
            }
        error != null -> {
            Text(error, color = colors.muted, fontSize = 14.sp, modifier = Modifier.padding(vertical = 8.dp))
            Text(
                "Reintentar",
                color = colors.ink,
                fontWeight = FontWeight.Black,
                fontSize = 15.sp,
                modifier =
                    Modifier.physicalPress(
                        scale = PhysicalPressScale.Small,
                    ) { scope.launch { children.load() } },
            )
        }
        else -> {
            children.children.forEach { child ->
                if (child.isAwaitingSchool) AwaitingRow(child) else ChildRow(child) { onOpen(child) }
            }
            AddRow(isFirst = children.children.isEmpty(), onAdd = onAdd)
        }
    }
}

@Composable
internal fun InitialsAvatar(
    initials: String,
    surface: Color,
    ink: Color,
    line: Color,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
            modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(surface)
                .border(1.dp, line, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(initials, color = ink, fontWeight = FontWeight.Black, fontSize = 15.sp)
    }
}

@Composable
private fun ChildRow(
    child: Dependent,
    onClick: () -> Unit,
) {
    val colors = LocalVaiinillaColors.current
    val balance = child.balance?.let(::compactMoney).orEmpty()
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .testTag("child-${child.userId}")
                .physicalPress(scale = PhysicalPressScale.Small, onClick = onClick)
                .semantics(mergeDescendants = true) {
                    contentDescription =
                        "${child.name}, saldo $balance${if (child.isLowBalance) ", saldo bajo" else ""}"
                },
    ) {
        Row(modifier = Modifier.padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            InitialsAvatar(child.initials, colors.paper2, colors.ink, colors.line)
            Column(modifier = Modifier.weight(1f).padding(horizontal = 13.dp)) {
                Text(
                    child.name,
                    color = colors.ink,
                    fontWeight = FontWeight.Black,
                    fontSize = 15.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    if (child.isLowBalance) "Saldo bajo" else child.schoolLine ?: "Sin grupo",
                    color = if (child.isLowBalance) colors.coral else colors.muted,
                    fontWeight = if (child.isLowBalance) FontWeight.Bold else FontWeight.Normal,
                    fontSize = 12.sp,
                )
            }
            Text(balance, color = colors.ink, fontWeight = FontWeight.Black, fontSize = 17.sp)
            Icon(
                Icons.Outlined.ChevronRight,
                contentDescription = null,
                tint = colors.muted,
                modifier = Modifier.size(18.dp),
            )
        }
        HorizontalDivider(color = colors.line, thickness = 1.dp, modifier = Modifier.padding(start = 57.dp))
    }
}

/** Hasta que la escuela aprueba, solo su nombre: ni saldo ni ficha que abrir. */
@Composable
private fun AwaitingRow(child: Dependent) {
    val colors = LocalVaiinillaColors.current
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .testTag(
                    "child-awaiting-${child.userId}",
                ).semantics(mergeDescendants = true) {
                },
    ) {
        Row(modifier = Modifier.padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            InitialsAvatar(child.initials, colors.paper2, colors.ink, colors.line, Modifier.alpha(0.6f))
            Column(modifier = Modifier.weight(1f).padding(horizontal = 13.dp)) {
                Text(
                    child.name,
                    color = colors.ink,
                    fontWeight = FontWeight.Black,
                    fontSize = 15.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text("Esperando a que la escuela te apruebe", color = colors.muted, fontSize = 12.sp)
            }
            Icon(
                Icons.Outlined.Schedule,
                contentDescription = null,
                tint = colors.muted,
                modifier = Modifier.size(18.dp),
            )
        }
        HorizontalDivider(color = colors.line, thickness = 1.dp, modifier = Modifier.padding(start = 57.dp))
    }
}

@Composable
private fun AddRow(
    isFirst: Boolean,
    onAdd: () -> Unit,
) {
    val colors = LocalVaiinillaColors.current
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .testTag("children-add")
                .physicalPress(scale = PhysicalPressScale.Small, onClick = onAdd)
                .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier =
                Modifier
                    .size(
                        44.dp,
                    ).clip(CircleShape)
                    .background(colors.paper2)
                    .border(1.dp, colors.line, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Outlined.Add, contentDescription = null, tint = colors.ink, modifier = Modifier.size(18.dp))
        }
        Column(modifier = Modifier.padding(start = 13.dp)) {
            Text(
                if (isFirst) "Agregar a mi hijo" else "Agregar otro hijo",
                color = colors.ink,
                fontWeight = FontWeight.Black,
                fontSize = 15.sp,
            )
            Text("Recárgale, ponle límites y apártale la comida", color = colors.muted, fontSize = 12.sp)
        }
    }
}
