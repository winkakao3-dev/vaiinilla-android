package com.vaiinilla.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vaiinilla.app.domain.model.SpaceCopy
import com.vaiinilla.app.ui.components.EditorialAccentButton
import com.vaiinilla.app.ui.components.EditorialPrimaryButton
import com.vaiinilla.app.ui.components.EditorialTextField
import com.vaiinilla.app.ui.components.arrive
import com.vaiinilla.app.ui.sharedtable.TableWhoUiState
import com.vaiinilla.app.ui.theme.LocalVaiinillaColors

/**
 * "¿Quién eres?": antes de pedir en una mesa compartida se elige el nombre con el que se
 * entra. Mismo texto que la PWA (`table-who-page.tsx`).
 */
@Composable
fun TableWhoScreen(
    state: TableWhoUiState,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onAliasChange: (String) -> Unit,
    onSubmitAlias: () -> Unit,
    onContinueAs: (String) -> Unit,
    onNotMe: () -> Unit,
    onAddMe: () -> Unit,
) {
    val colors = LocalVaiinillaColors.current
    val session = state.session
    val heading =
        if (session != null && state.offerContinue) {
            "¿Continuar como ${session.me?.alias.orEmpty()}?"
        } else {
            "¿Quién eres?"
        }
    val noun = SpaceCopy.kindLabel(session?.spaceType).lowercase()
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .background(colors.paper)
                .statusBarsPadding(),
    ) {
        IconButton(
            onClick = onBack,
            modifier = Modifier.padding(start = 4.dp).defaultMinSize(minWidth = 48.dp, minHeight = 48.dp),
        ) {
            Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Volver", tint = colors.ink)
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                Text(
                    session?.spaceName ?: "Tu $noun",
                    color = colors.accent,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.3.sp,
                )
                Text(
                    heading,
                    color = colors.ink,
                    fontSize = 28.sp,
                    lineHeight = 31.sp,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.padding(top = 8.dp),
                )
                if (session != null && !state.offerContinue) {
                    Text(
                        "Selecciona tu nombre para continuar.",
                        color = colors.muted,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                }
            }
            when {
                state.loading ->
                    item(key = "loading") {
                        Text("Viendo quién está en la mesa…", color = colors.muted, fontSize = 14.sp)
                    }
                state.loadError != null && session == null ->
                    item(key = "load-error") {
                        Text(state.loadError, color = colors.coral, fontSize = 14.sp)
                        EditorialAccentButton(
                            text = "Reintentar",
                            onClick = onRetry,
                            modifier = Modifier.padding(top = 12.dp),
                        )
                    }
                session != null && state.offerContinue ->
                    item(key = "continue") {
                        val me = session.me
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            EditorialAccentButton(
                                text = "Continuar como ${me?.alias.orEmpty()}",
                                onClick = { me?.let { onContinueAs(it.id) } },
                                enabled = !state.busy && me != null,
                            )
                            GhostButton(
                                text = "No soy ${me?.alias.orEmpty()}",
                                enabled = !state.busy,
                                onClick = onNotMe,
                            )
                        }
                    }
                session != null ->
                    item(key = "choose") {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            session.participants.forEachIndexed { index, person ->
                                GhostButton(
                                    text = person.alias,
                                    enabled = !state.busy,
                                    onClick = { onContinueAs(person.id) },
                                    modifier = Modifier.arrive(index, key = person.id),
                                )
                            }
                            if (session.participants.isEmpty() || state.showAdd) {
                                EditorialTextField(
                                    value = state.aliasInput,
                                    onValueChange = onAliasChange,
                                    label = "Tu nombre",
                                    placeholder = "Como te dicen en la mesa",
                                    modifier = Modifier.padding(top = 6.dp),
                                )
                                Text(
                                    "Ej. David R. o David Ramírez",
                                    color = colors.muted,
                                    fontSize = 12.sp,
                                )
                                EditorialAccentButton(
                                    text = "Entrar",
                                    onClick = onSubmitAlias,
                                    enabled = state.canSubmitAlias,
                                    modifier = Modifier.padding(top = 4.dp),
                                )
                            } else {
                                GhostButton(
                                    text = "Agregarme a la mesa",
                                    enabled = !state.busy,
                                    onClick = onAddMe,
                                )
                            }
                        }
                    }
            }
            state.joinError?.let { message ->
                item(key = "join-error") {
                    Text(message, color = colors.coral, fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
private fun GhostButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val colors = LocalVaiinillaColors.current
    EditorialPrimaryButton(
        text = text,
        onClick = onClick,
        enabled = enabled,
        background = Color.Transparent,
        contentColor = colors.ink,
        modifier = modifier.border(1.dp, colors.line, RoundedCornerShape(18.dp)),
    )
}
