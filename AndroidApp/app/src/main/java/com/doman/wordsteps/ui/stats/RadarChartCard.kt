package com.doman.wordsteps.ui.stats

import android.annotation.SuppressLint
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.doman.wordsteps.data.models.PatternMastery
import com.doman.wordsteps.ui.home.patternLabels
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

private val Navy          = Color(0xFF0F1B2D)
private val NavyLight     = Color(0xFF1A2E4A)
private val NavyMid       = Color(0xFF142338)
private val Amber         = Color(0xFFFFC044)
private val Teal          = Color(0xFF00C9A7)
private val Rose          = Color(0xFFFF6B8A)
private val Purple        = Color(0xFF9D7FFF)
private val TextPrimary   = Color(0xFFF0F4FF)
private val TextSecondary = Color(0xFF8A9BB5)

@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
fun RadarChartCard(patterns: List<PatternMastery>) {
    if (patterns.size < 3) return

    val animProgress = remember { Animatable(0f) }
    LaunchedEffect(patterns) {
        animProgress.snapTo(0f)
        animProgress.animateTo(
            targetValue   = 1f,
            animationSpec = tween(durationMillis = 1000, easing = EaseOutCubic)
        )
    }
    val progress by animProgress.asState()

    val allPatternKeys = listOf(
        "vowel_swap", "ie_ei_swap", "vowel_drop", "double_to_single",
        "insertion", "transposition", "consonant_drop", "consonant_change",
        "single_to_double", "y_to_ie_ending", "i_y_swap"
    )
    val shortLabels = mapOf(
        "vowel_swap"       to "V.Swap",
        "ie_ei_swap"       to "IE/EI",
        "vowel_drop"       to "V.Drop",
        "double_to_single" to "Dbl→Sng",
        "insertion"        to "Insert",
        "transposition"    to "Transp.",
        "consonant_drop"   to "C.Drop",
        "consonant_change" to "C.Chng",
        "single_to_double" to "Sng→Dbl",
        "y_to_ie_ending"   to "Y→IE",
        "i_y_swap"         to "I/Y"
    )
    val dataPatterns = allPatternKeys.map { key ->
        key to (patterns.firstOrNull { it.pattern == key }?.accuracy ?: 0f)
    }
    val n = dataPatterns.size


    var selectedIndex by remember { mutableStateOf<Int?>(null) }
    val dotPositions = remember { mutableStateListOf<Offset>() }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(NavyMid)
            .border(1.dp, NavyLight, RoundedCornerShape(20.dp))
            .padding(18.dp)
    ) {
        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
            Text("Pattern Radar", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                LegendDot(Teal,  "70%+")
                LegendDot(Amber, "40–70%")
                LegendDot(Rose,  "–40%")
            }
        }
        Text("Tap a dot to see details", color = TextSecondary, fontSize = 11.sp,
            modifier = Modifier.padding(top = 4.dp))

        Spacer(Modifier.height(8.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
        ) {
            val density = LocalDensity.current
            val paddingDp = 44.dp

            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingDp)
                    .pointerInput(Unit) {
                        detectTapGestures { tapOffset ->
                            val hitIndex = dotPositions.indexOfFirst { dotPos ->
                                val dx = dotPos.x - tapOffset.x
                                val dy = dotPos.y - tapOffset.y
                                sqrt(dx * dx + dy * dy) < with(density) { 28.dp.toPx() }
                            }
                            selectedIndex = when {
                                hitIndex < 0          -> null
                                hitIndex == selectedIndex -> null
                                else                  -> hitIndex
                            }
                        }
                    }
            ) {
                val cx = size.width  / 2f
                val cy = size.height / 2f
                val r  = min(cx, cy)

                fun angle(i: Int) = (Math.PI * 2 * i / n - Math.PI / 2).toFloat()
                fun pt(i: Int, frac: Float): Offset {
                    val a = angle(i)
                    return Offset(cx + r * frac * cos(a), cy + r * frac * sin(a))
                }

                listOf(0.2f, 0.4f, 0.6f, 0.8f, 1.0f).forEach { lvl ->
                    val path = Path()
                    repeat(n) { i ->
                        val p = pt(i, lvl)
                        if (i == 0) path.moveTo(p.x, p.y) else path.lineTo(p.x, p.y)
                    }
                    path.close()
                    drawPath(path, NavyLight, style = Stroke(1.dp.toPx()))
                }

                repeat(n) { i ->
                    drawLine(NavyLight, Offset(cx, cy), pt(i, 1f), 1.dp.toPx())
                }

                val dataPath = Path()
                repeat(n) { i ->
                    val p = pt(i, (dataPatterns[i].second * progress).coerceIn(0.01f, 1f))
                    if (i == 0) dataPath.moveTo(p.x, p.y) else dataPath.lineTo(p.x, p.y)
                }
                dataPath.close()
                drawPath(
                    dataPath,
                    Brush.radialGradient(
                        listOf(Purple.copy(alpha = 0.35f), Teal.copy(alpha = 0.15f)),
                        Offset(cx, cy), r
                    )
                )
                drawPath(dataPath, Purple.copy(0.8f), style = Stroke(2.dp.toPx(), cap = StrokeCap.Round))

                dotPositions.clear()
                repeat(n) { i ->
                    val accuracy = dataPatterns[i].second
                    val p = pt(i, (accuracy * progress).coerceIn(0.01f, 1f))
                    dotPositions.add(p)

                    val dotColor = when {
                        accuracy < 0.4f -> Rose
                        accuracy < 0.7f -> Amber
                        else            -> Teal
                    }
                    val isSelected = selectedIndex == i
                    val outerR = if (isSelected) 9.dp.toPx() else 5.dp.toPx()

                    drawCircle(dotColor.copy(alpha = if (isSelected) 0.5f else 0.25f),
                        outerR + 5.dp.toPx(), p, style = Stroke(1.5.dp.toPx()))
                    drawCircle(dotColor, outerR, p)
                    drawCircle(Navy,     if (isSelected) 4.dp.toPx() else 2.5.dp.toPx(), p)
                }
            }

            BoxWithConstraints(Modifier.fillMaxSize()) {
                val paddingPx = with(density) { paddingDp.toPx() }
                val bw = constraints.maxWidth.toFloat()
                val bh = constraints.maxHeight.toFloat()
                val cx = (bw - 2 * paddingPx) / 2f + paddingPx
                val cy = (bh - 2 * paddingPx) / 2f + paddingPx
                val r  = min((bw - 2 * paddingPx) / 2f, (bh - 2 * paddingPx) / 2f)

                dataPatterns.forEachIndexed { i, (key, accuracy) ->
                    val a  = (Math.PI * 2 * i / n - Math.PI / 2).toFloat()
                    val lr = r * 1.24f
                    val lx = cx + lr * cos(a)
                    val ly = cy + lr * sin(a)

                    val color = when {
                        accuracy < 0.4f -> Rose
                        accuracy < 0.7f -> Amber
                        else            -> Teal
                    }
                    with(density) {
                        Text(
                            text       = shortLabels[key] ?: key,
                            color      = color,
                            fontSize   = 9.sp,
                            fontWeight = FontWeight.SemiBold,
                            textAlign  = TextAlign.Center,
                            modifier   = Modifier
                                .offset {
                                    IntOffset(
                                        (lx - 30.dp.toPx()).toInt(),
                                        (ly - 9.dp.toPx()).toInt()
                                    )
                                }
                                .width(60.dp)
                        )
                    }
                }
            }

            selectedIndex?.let { idx ->
                val (key, accuracy) = dataPatterns[idx]
                val fullLabel = patternLabels[key] ?: key
                val pct       = (accuracy * 100).toInt()
                val color     = when {
                    accuracy < 0.4f -> Rose
                    accuracy < 0.7f -> Amber
                    else            -> Teal
                }

                BoxWithConstraints(Modifier.fillMaxSize()) {
                    val paddingPx = with(density) { paddingDp.toPx() }
                    val bw = constraints.maxWidth.toFloat()
                    val bh = constraints.maxHeight.toFloat()
                    val cx = (bw - 2 * paddingPx) / 2f + paddingPx
                    val cy = (bh - 2 * paddingPx) / 2f + paddingPx
                    val r  = min((bw - 2 * paddingPx) / 2f, (bh - 2 * paddingPx) / 2f)

                    val a       = (Math.PI * 2 * idx / n - Math.PI / 2).toFloat()
                    val animated = dataPatterns[idx].second.coerceIn(0.01f, 1f)
                    val dotX    = cx + r * animated * cos(a)
                    val dotY    = cy + r * animated * sin(a)

                    with(density) {
                        val tw = 128.dp.toPx()
                        val th = 52.dp.toPx()
                        val tx = (dotX - tw / 2).coerceIn(0f, bw - tw)
                        val ty = if (dotY > bh / 2)
                            dotY - th - 14.dp.toPx()
                        else
                            dotY + 14.dp.toPx()

                        Box(
                            modifier = Modifier
                                .offset { IntOffset(tx.toInt(), ty.toInt()) }
                                .width(128.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Navy)
                                .border(1.dp, color.copy(alpha = 0.7f), RoundedCornerShape(10.dp))
                                .padding(horizontal = 10.dp, vertical = 7.dp)
                        ) {
                            Column {
                                Text(fullLabel, color = TextPrimary,
                                    fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                Text("$pct%", color = color,
                                    fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(Modifier.size(8.dp).clip(CircleShape).background(color))
        Text(label, color = TextSecondary, fontSize = 10.sp)
    }
}