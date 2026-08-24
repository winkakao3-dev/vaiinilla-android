package com.vaiinilla.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val OrderDeleteBackground = Color(0xFFFF8686)
private val OrderDeleteInk = Color(0xFF1C1C1A)
private val OrderDeleteShape = RoundedCornerShape(28.dp)

/**
 * Two-way swipe affordance matching the approved order-delete mockup.
 * The action is deliberately local-only; backend order history remains authoritative.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeToDeleteOrder(
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val haptics = rememberVaiinillaHaptics()
    val dismissState =
        rememberSwipeToDismissBoxState(
            positionalThreshold = { distance -> distance * 0.38f },
        )
    LaunchedEffect(dismissState.currentValue) {
        if (dismissState.currentValue != SwipeToDismissBoxValue.Settled) {
            haptics.impact()
            onDelete()
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
                                haptics.impact()
                                onDelete()
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
