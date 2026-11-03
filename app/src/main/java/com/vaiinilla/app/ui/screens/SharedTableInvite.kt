package com.vaiinilla.app.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vaiinilla.app.domain.model.SpaceCopy
import com.vaiinilla.app.ui.components.ClientPillButton
import com.vaiinilla.app.ui.components.ClientSheetHandle
import com.vaiinilla.app.ui.sharedtable.SharedTableUiState
import com.vaiinilla.app.ui.theme.LocalVaiinillaColors

const val SHARED_TABLE_INVITE_JOIN_TAG = "shared-table-invite-join"
const val SHARED_TABLE_INVITE_ALONE_TAG = "shared-table-invite-alone"

/** Al entrar por el QR de una mesa donde ya pidieron otros: unirse con un nombre o pedir solo (Doop L03). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SharedTableInviteSheet(
    state: SharedTableUiState,
    spaceName: String?,
    spaceType: String?,
    onAliasChange: (String) -> Unit,
    onJoin: () -> Unit,
    onOrderAlone: () -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = LocalVaiinillaColors.current
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = colors.paper,
        scrimColor = Color.Black.copy(alpha = 0.5f),
        shape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp),
        dragHandle = { ClientSheetHandle() },
    ) {
        SharedTableInvite(
            state = state,
            spaceName = spaceName,
            spaceType = spaceType,
            onAliasChange = onAliasChange,
            onJoin = onJoin,
            onOrderAlone = onOrderAlone,
            modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars),
        )
    }
}


@Composable
internal fun SharedTableInvite(
    state: SharedTableUiState,
    spaceName: String?,
    spaceType: String?,
    onAliasChange: (String) -> Unit,
    onJoin: () -> Unit,
    onOrderAlone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalVaiinillaColors.current
    val words = SharedSpaceWords.of(spaceType)
    val place = spaceName?.takeIf { it.isNotBlank() }?.let { SpaceCopy.named(spaceType, it) } ?: words.thePlace
    Column(modifier.fillMaxWidth().padding(bottom = 24.dp)) {
        Column(Modifier.padding(start = 20.dp, end = 20.dp, top = 20.dp)) {
            InviteFaces(state.othersAtTable)
            Text(
                "¿Compartes ${words.thePlace}?",
                color = colors.ink,
                fontSize = 28.sp,
                lineHeight = 32.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = (-1.1).sp,
                modifier = Modifier.padding(top = 14.dp),
            )
            Text(
                listOfNotNull(
                    othersAtTableLine(state.othersAtTable, place),
                    "Únete con un nombre para ver los pedidos de todos y pagar solo tu parte.",
                ).joinToString(" "),
                color = colors.muted,
                fontSize = 14.5.sp,
                lineHeight = 20.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(top = 6.dp),
            )
        }
        AliasField(
            value = state.aliasInput,
            onValueChange = onAliasChange,
            label = "Tu nombre en ${words.thePlace}",
            onDone = { if (state.canJoin) onJoin() },
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 18.dp),
        )
        Text(
            "Los demás solo ven ese nombre.",
            color = colors.muted,
            fontSize = 12.5.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 8.dp),
        )
        state.error?.let {
            Text(
                it,
                color = colors.coral,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 10.dp),
            )
        }
        Spacer(Modifier.height(40.dp))
        Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            ClientPillButton(
                text = "Unirme a ${words.thePlace}",
                onClick = onJoin,
                enabled = state.canJoin,
                modifier = Modifier.testTag(SHARED_TABLE_INVITE_JOIN_TAG),
            )
            ClientPillButton(
                text = "Pedir solo yo",
                onClick = onOrderAlone,
                primary = false,
                enabled = !state.busy,
                height = 52.dp,
                modifier = Modifier.testTag(SHARED_TABLE_INVITE_ALONE_TAG),
            )
        }
    }
}

private val InviteFaceTints =
    listOf(
        Color(0xFFA8D8F6) to Color(0xFF0F2E44),
        Color(0xFFFFD15B) to Color(0xFF3D2B00),
    )

/** Las iniciales de dos de quienes ya están y «+N» por el resto; el texto de abajo ya dice sus nombres. */
@Composable
private fun InviteFaces(names: List<String>) {
    if (names.isEmpty()) return
    val colors = LocalVaiinillaColors.current
    val shown = names.take(2)
    val rest = names.size - shown.size
    Row(
        modifier = Modifier.clearAndSetSemantics {},
        horizontalArrangement = Arrangement.spacedBy((-12).dp),
    ) {
        shown.forEachIndexed { index, name ->
            val (fill, ink) = InviteFaceTints[index]
            InviteFace(name.trim().take(1).uppercase(), fill, ink, 16.sp)
        }
        if (rest > 0) InviteFace("+$rest", lerp(colors.paper2, colors.ink, 0.06f), colors.ink2, 13.sp)
    }
}

@Composable
private fun InviteFace(
    text: String,
    fill: Color,
    ink: Color,
    size: androidx.compose.ui.unit.TextUnit,
) {
    Box(
        modifier =
            Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(fill)
                .border(3.dp, LocalVaiinillaColors.current.paper, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = ink, fontSize = size, fontWeight = FontWeight.Black)
    }
}

@Composable
private fun AliasField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalVaiinillaColors.current
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    // Con nombre escrito el borde ya es el del dibujo: ese nombre es el que verán los demás.
    val border by animateColorAsState(
        if (focused || value.isNotBlank()) colors.accent else colors.line,
        label = "alias-border",
    )
    val shape = RoundedCornerShape(16.dp)
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        textStyle = TextStyle(color = colors.ink, fontSize = 17.sp, fontWeight = FontWeight.ExtraBold),
        cursorBrush = SolidColor(colors.accent),
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { onDone() }),
        interactionSource = interaction,
        modifier = modifier.fillMaxWidth(),
        decorationBox = { field ->
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(shape)
                    .background(colors.paper2)
                    .border(1.5.dp, border, shape)
                    .padding(horizontal = 16.dp, vertical = 10.dp),
            ) {
                Text(label, color = colors.muted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Box(Modifier.padding(top = 3.dp)) {
                    if (value.isEmpty()) {
                        Text("Ana", color = colors.muted.copy(alpha = 0.55f), fontSize = 17.sp, fontWeight = FontWeight.ExtraBold)
                    }
                    field()
                }
            }
        },
    )
}

