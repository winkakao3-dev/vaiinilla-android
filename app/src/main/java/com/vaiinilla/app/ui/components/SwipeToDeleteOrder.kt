package com.vaiinilla.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val OrderDeleteBackground = Color(0xFFFF8686)
private val OrderDeleteInk = Color(0xFF1C1C1A)
private val OrderDeleteDialogBackground = Color(0xFFF8F4E9)
private val OrderDeleteDialogSupporting = Color(0xFF5C5A54)
private val OrderDeleteCancelBackground = Color(0xFFE7E2D8)
private val OrderDeleteShape = RoundedCornerShape(28.dp)
private val OrderDeleteDialogShape = RoundedCornerShape(28.dp)

/**
 * Two-way swipe affordance matching the approved order-delete mockups.
 * A completed swipe only requests deletion; the local dismissal happens after confirmation.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeToDeleteOrder(
    orderFolio: String,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val haptics = rememberVaiinillaHaptics()
    val reduceMotion = reducedMotion()
    var dialogMounted by remember { mutableStateOf(false) }
    var confirmationRequested by remember { mutableStateOf(false) }

    fun requestDeleteConfirmation() {
        if (dialogMounted || confirmationRequested) return
        haptics.impact()
        confirmationRequested = true
    }

    @Suppress("DEPRECATION")
    val dismissState =
        rememberSwipeToDismissBoxState(
            positionalThreshold = { distance -> distance * 0.38f },
            confirmValueChange = { target ->
                if (target == SwipeToDismissBoxValue.Settled) {
                    true
                } else {
                    requestDeleteConfirmation()
                    false
                }
            },
        )

    LaunchedEffect(confirmationRequested) {
        if (confirmationRequested) {
            if (!reduceMotion) delay(65)
            dialogMounted = true
            confirmationRequested = false
        }
    }

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = true,
        enableDismissFromEndToStart = true,
        modifier =
            modifier
                .fillMaxWidth()
                .clip(OrderDeleteShape)
                .semantics {
                    customActions =
                        listOf(
                            CustomAccessibilityAction(label = "Eliminar pedido") {
                                requestDeleteConfirmation()
                                true
                            },
                        )
                },
        backgroundContent = {
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .background(OrderDeleteBackground),
            ) {
                DeleteOrderAction(
                    modifier =
                        Modifier
                            .align(Alignment.CenterStart)
                            .padding(start = 14.dp),
                )
                DeleteOrderAction(
                    modifier =
                        Modifier
                            .align(Alignment.CenterEnd)
                            .padding(end = 14.dp),
                )
            }
        },
        content = {
            Box(modifier = Modifier.fillMaxWidth()) {
                content()
            }
        },
    )

    if (dialogMounted) {
        DeleteOrderConfirmationDialog(
            orderFolio = orderFolio,
            reduceMotion = reduceMotion,
            onCancel = { dialogMounted = false },
            onConfirm = {
                dialogMounted = false
                onDelete()
            },
        )
    }
}

@Composable
private fun DeleteOrderAction(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.width(112.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        Icon(
            imageVector = Icons.Outlined.DeleteOutline,
            contentDescription = null,
            tint = OrderDeleteInk,
            modifier = Modifier.size(25.dp),
        )
        Text(
            text = "Eliminar",
            color = OrderDeleteInk,
            fontSize = 16.sp,
            lineHeight = 20.sp,
            fontWeight = FontWeight.Black,
        )
    }
}

@Composable
private fun DeleteOrderConfirmationDialog(
    orderFolio: String,
    reduceMotion: Boolean,
    onCancel: () -> Unit,
    onConfirm: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var dialogVisible by remember { mutableStateOf(false) }
    var stage by remember { mutableIntStateOf(0) }
    var closing by remember { mutableStateOf(false) }

    val shortDelay = if (reduceMotion) 0L else 55L
    val closeDelay = if (reduceMotion) 0L else 170L

    fun closeDialog(confirm: Boolean) {
        if (closing) return
        closing = true
        stage = 0
        dialogVisible = false
        scope.launch {
            delay(closeDelay)
            if (confirm) onConfirm() else onCancel()
        }
    }

    LaunchedEffect(Unit) {
        dialogVisible = true
        if (!reduceMotion) delay(45)
        stage = 1
        delay(shortDelay)
        stage = 2
    }

    Dialog(
        onDismissRequest = { closeDialog(confirm = false) },
        properties =
            DialogProperties(
                dismissOnBackPress = true,
                dismissOnClickOutside = true,
                usePlatformDefaultWidth = false,
            ),
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .testTag("delete-order-dialog-root")
                    .padding(horizontal = 24.dp),
            contentAlignment = Alignment.Center,
        ) {
            AnimatedVisibility(
                visible = dialogVisible,
                enter =
                    if (reduceMotion) {
                        fadeIn(animationSpec = tween(0))
                    } else {
                        fadeIn(animationSpec = tween(140)) +
                            scaleIn(
                                initialScale = 0.84f,
                                animationSpec = spring(dampingRatio = 0.72f, stiffness = 540f),
                            ) +
                            slideInVertically(
                                initialOffsetY = { height -> height / 8 },
                                animationSpec = spring(dampingRatio = 0.78f, stiffness = 520f),
                            )
                    },
                exit =
                    fadeOut(animationSpec = tween(if (reduceMotion) 0 else 110)) +
                        scaleOut(
                            targetScale = 0.95f,
                            animationSpec = tween(if (reduceMotion) 0 else 145),
                        ) +
                        slideOutVertically(
                            targetOffsetY = { height -> height / 14 },
                            animationSpec = tween(if (reduceMotion) 0 else 145),
                        ),
            ) {
                Surface(
                    color = OrderDeleteDialogBackground,
                    shape = OrderDeleteDialogShape,
                    shadowElevation = 12.dp,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier.padding(start = 28.dp, end = 28.dp, top = 28.dp, bottom = 24.dp),
                    ) {
                        AnimatedVisibility(
                            visible = stage >= 1,
                            enter =
                                fadeIn(animationSpec = tween(if (reduceMotion) 0 else 150)) +
                                    slideInVertically(
                                        initialOffsetY = { height -> height / 3 },
                                        animationSpec =
                                            tween(
                                                durationMillis = if (reduceMotion) 0 else 190,
                                                easing = FastOutSlowInEasing,
                                            ),
                                    ),
                            exit = fadeOut(animationSpec = tween(if (reduceMotion) 0 else 70)),
                        ) {
                            Column {
                                Text(
                                    text = "¿Eliminar este pedido?",
                                    color = OrderDeleteInk,
                                    fontSize = 24.sp,
                                    lineHeight = 29.sp,
                                    fontWeight = FontWeight.Black,
                                )
                                Text(
                                    text = "El pedido #$orderFolio se quitará de Mis pedidos.",
                                    color = OrderDeleteDialogSupporting,
                                    fontSize = 15.sp,
                                    lineHeight = 21.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(top = 10.dp),
                                )
                            }
                        }

                        Row(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .padding(top = 28.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            AnimatedVisibility(
                                visible = stage >= 2,
                                modifier = Modifier.weight(1f),
                                enter =
                                    fadeIn(
                                        animationSpec =
                                            tween(
                                                durationMillis = if (reduceMotion) 0 else 150,
                                                delayMillis = if (reduceMotion) 0 else 20,
                                            ),
                                    ) +
                                        scaleIn(
                                            initialScale = 0.92f,
                                            animationSpec = tween(if (reduceMotion) 0 else 180),
                                        ) +
                                        slideInVertically(
                                            initialOffsetY = { height -> height / 2 },
                                            animationSpec = tween(if (reduceMotion) 0 else 180),
                                        ),
                                exit = fadeOut(animationSpec = tween(if (reduceMotion) 0 else 65)),
                            ) {
                                DeleteDialogButton(
                                    text = "Cancelar",
                                    background = OrderDeleteCancelBackground,
                                    modifier = Modifier.testTag("cancel-delete-order"),
                                    onClick = { closeDialog(confirm = false) },
                                )
                            }
                            AnimatedVisibility(
                                visible = stage >= 2,
                                modifier = Modifier.weight(1f),
                                enter =
                                    fadeIn(
                                        animationSpec =
                                            tween(
                                                durationMillis = if (reduceMotion) 0 else 160,
                                                delayMillis = if (reduceMotion) 0 else 65,
                                            ),
                                    ) +
                                        scaleIn(
                                            initialScale = 0.88f,
                                            animationSpec =
                                                spring(dampingRatio = 0.68f, stiffness = 620f),
                                        ) +
                                        slideInVertically(
                                            initialOffsetY = { height -> height / 2 },
                                            animationSpec = tween(if (reduceMotion) 0 else 200),
                                        ),
                                exit = fadeOut(animationSpec = tween(if (reduceMotion) 0 else 65)),
                            ) {
                                DeleteDialogButton(
                                    text = "Eliminar",
                                    background = OrderDeleteBackground,
                                    modifier = Modifier.testTag("confirm-delete-order"),
                                    onClick = { closeDialog(confirm = true) },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DeleteDialogButton(
    text: String,
    background: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Surface(
        modifier =
            modifier
                .fillMaxWidth()
                .height(56.dp)
                .physicalPress(scale = PhysicalPressScale.Small, onClick = onClick),
        color = background,
        shape = RoundedCornerShape(99.dp),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = text,
                color = OrderDeleteInk,
                fontSize = 16.sp,
                lineHeight = 20.sp,
                fontWeight = FontWeight.Black,
            )
        }
    }
}
