package com.doman.wordsteps.ui.home

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties

private val Navy          = Color(0xFF0F1B2D)
private val NavyLight     = Color(0xFF1A2E4A)
private val Teal          = Color(0xFF00C9A7)
private val Rose          = Color(0xFFFF6B8A)
private val Amber         = Color(0xFFFFC044)
private val TextPrimary   = Color(0xFFF0F4FF)
private val TextSecondary = Color(0xFF8A9BB5)

enum class ServerStatus { CHECKING, ONLINE, OFFLINE }


@Composable
fun ServerStatusDot(
    status: ServerStatus,
    modifier: Modifier = Modifier
) {
    var showTooltip by remember { mutableStateOf(false) }

    val dotColor by animateColorAsState(
        targetValue = when (status) {
            ServerStatus.CHECKING -> Amber
            ServerStatus.ONLINE   -> Teal
            ServerStatus.OFFLINE  -> Rose
        },
        animationSpec = tween(500),
        label = "dotColor"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue  = 1f,
        targetValue   = if (status == ServerStatus.OFFLINE) 1f else 1.5f,
        animationSpec = infiniteRepeatable(
            animation  = tween(900, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    Box(contentAlignment = Alignment.Center, modifier = modifier) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .scale(pulseScale)
                .clip(CircleShape)
                .background(dotColor.copy(alpha = 0.25f))
        )
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(dotColor)
                .clickable { showTooltip = !showTooltip }
        )

        if (showTooltip) {
            Popup(
                alignment  = Alignment.TopEnd,
                onDismissRequest = { showTooltip = false },
                properties = PopupProperties(focusable = true)
            ) {
                Box(
                    modifier = Modifier
                        .padding(end = 16.dp, top = 20.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(NavyLight)
                        .border(1.dp, dotColor.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = when (status) {
                            ServerStatus.CHECKING -> "Connecting to ML server..."
                            ServerStatus.ONLINE   -> "ML server online"
                            ServerStatus.OFFLINE  -> "ML server offline – using local patterns"
                        },
                        color      = TextPrimary,
                        fontSize   = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}