package com.vaiinilla.app.ui.hotel.guest

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vaiinilla.app.domain.model.Venue
import com.vaiinilla.app.ui.theme.LocalVaiinillaColors

internal const val STAY_CODE_LENGTH = 6

/**
 * El código de estancia: seis números que recepción le da al huésped. Liga su cuenta con la habitación del QR;
 * sin él, el servidor no deja pedir al cuarto (ROOM_REQUIRES_STAY).
 */
@Composable
fun HotelStayCodeScreen(
    onLinked: () -> Unit = {},
    onBack: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: HotelStayViewModel = viewModel(),
) {
    val space =
        activityOrderFlow()
            ?.uiState
            ?.value
            ?.guestVenue
            ?.space
    val state = viewModel.state.value
    HotelStayCodeContent(
        linking = state.linking,
        error = state.linkError,
        onSubmit = { code -> viewModel.link(code, space?.id, space?.qrToken, onLinked) },
        onCodeEdited = viewModel::clearLinkError,
        modifier = modifier,
    )
}

@Composable
fun HotelStayCodeContent(
    linking: Boolean,
    error: String?,
    onSubmit: (String) -> Unit,
    modifier: Modifier = Modifier,
    onCodeEdited: () -> Unit = {},
    initialCode: String = "",
    autoFocus: Boolean = true,
) {
    val colors = LocalVaiinillaColors.current
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .background(
                    colors.paper,
                ).statusBarsPadding()
                .imePadding()
                .padding(top = 12.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        HotelStayCodeForm(linking, error, onSubmit, onCodeEdited, initialCode, autoFocus, fillHeight = true)
    }
}

/** El mismo código de estancia dentro de una hoja, cuando el servidor lo pide al hacer un pedido (ROOM_REQUIRES_STAY). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HotelStayCodeSheet(
    linking: Boolean,
    error: String?,
    onSubmit: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = LocalVaiinillaColors.current
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = colors.paper,
        shape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .imePadding()
                    .navigationBarsPadding()
                    .padding(bottom = 8.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            HotelStayCodeForm(
                linking,
                error,
                onSubmit,
                onCodeEdited = {},
                initialCode = "",
                autoFocus = true,
                fillHeight = false,
            )
        }
    }
}

@Composable
private fun ColumnScope.HotelStayCodeForm(
    linking: Boolean,
    error: String?,
    onSubmit: (String) -> Unit,
    onCodeEdited: () -> Unit,
    initialCode: String,
    autoFocus: Boolean,
    fillHeight: Boolean,
) {
    val colors = LocalVaiinillaColors.current
    var code by remember { mutableStateOf(initialCode) }
    val focus = remember { FocusRequester() }
    LaunchedEffect(autoFocus) { if (autoFocus) runCatching { focus.requestFocus() } }
    val complete = code.length == STAY_CODE_LENGTH
    HotelPageTitle(eyebrow = "Tu ${Venue.room}", title = "Escribe tu código de estancia")
    Text(
        "Recepción te lo dio al registrarte. Son $STAY_CODE_LENGTH números.",
        color = colors.muted,
        fontSize = 15.sp,
        lineHeight = 21.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(horizontal = 20.dp),
    )
    BasicTextField(
        value = code,
        onValueChange = {
            val digits = it.filter(Char::isDigit).take(STAY_CODE_LENGTH)
            if (digits != code) {
                code = digits
                onCodeEdited()
            }
        },
        modifier =
            Modifier
                .padding(horizontal = 16.dp)
                .focusRequester(focus)
                .testTag("hotel_stay_code_input")
                .semantics { contentDescription = "Código de estancia" },
        enabled = !linking,
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword, imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { if (complete) onSubmit(code) }),
        cursorBrush = SolidColor(Color.Transparent),
        textStyle = TextStyle(color = Color.Transparent, fontSize = 1.sp),
        // El campo real va encima, invisible y del tamaño de las casillas: tocar una casilla abre el teclado.
        decorationBox = { innerTextField ->
            Box {
                CodeBoxes(code = code, hasError = error != null)
                Box(Modifier.matchParentSize().alpha(0f)) { innerTextField() }
            }
        },
    )
    error?.let {
        Text(
            it,
            color = colors.coral,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 24.dp).testTag("hotel_stay_code_error"),
        )
    }
    if (fillHeight) Spacer(Modifier.weight(1f))
    Text(
        "¿No lo tienes? Pídelo en recepción.",
        color = colors.muted,
        fontSize = 14.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(horizontal = 24.dp),
    )
    HotelPrimaryPill(
        label = "Entrar a mi ${Venue.room}",
        onClick = { onSubmit(code) },
        enabled = complete,
        loading = linking,
        modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 16.dp).navigationBarsPadding(),
        testTag = "hotel_stay_code_submit",
    )
}

@Composable
private fun CodeBoxes(
    code: String,
    hasError: Boolean,
) {
    val colors = LocalVaiinillaColors.current
    val shape = RoundedCornerShape(16.dp)
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        repeat(STAY_CODE_LENGTH) { index ->
            val active = index == code.length
            Box(
                modifier =
                    Modifier
                        .weight(1f)
                        .aspectRatio(0.8f)
                        .background(colors.paper2, shape)
                        .border(
                            width = if (active || hasError) 1.5.dp else 1.dp,
                            color =
                                when {
                                    hasError -> colors.coral
                                    active -> colors.accent
                                    else -> colors.line
                                },
                            shape = shape,
                        ),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    code.getOrNull(index)?.toString().orEmpty(),
                    color = colors.ink,
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Black,
                    style = TextStyle(fontFeatureSettings = "tnum"),
                )
            }
        }
    }
}
