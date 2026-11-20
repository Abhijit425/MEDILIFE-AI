package com.medilife.ai.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.medilife.ai.ui.theme.LocalMedi
import com.medilife.ai.ui.theme.Palette
import kotlin.math.PI
import kotlin.math.sin

@Composable
fun GradientBackground(content: @Composable BoxScope.() -> Unit) {
    val m = LocalMedi.current
    val bg = if (m.dark) listOf(Palette.BgDark, Palette.BgDark2, Palette.BgDark) else listOf(Palette.BgLight, Palette.BgLight2, Palette.BgLight)
    Box(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(bg))
            .drawBehind {
                drawCircle(Brush.radialGradient(listOf(Palette.Purple.copy(alpha = if (m.dark) 0.22f else 0.10f), Color.Transparent), center = Offset(size.width * 0.9f, size.height * 0.08f), radius = size.width * 0.8f), radius = size.width * 0.8f, center = Offset(size.width * 0.9f, size.height * 0.08f))
                drawCircle(Brush.radialGradient(listOf(Palette.Cyan.copy(alpha = if (m.dark) 0.16f else 0.08f), Color.Transparent), center = Offset(0f, size.height * 0.55f), radius = size.width * 0.9f), radius = size.width * 0.9f, center = Offset(0f, size.height * 0.55f))
            }
    ) {
        if (!m.reduceMotion) FloatingParticles(Modifier.fillMaxSize())
        content()
    }
}

@Composable
fun FloatingParticles(modifier: Modifier) {
    val t = rememberInfiniteTransition(label = "particles")
    val p by t.animateFloat(0f, 1f, infiniteRepeatable(tween(26_000, easing = LinearEasing)), label = "p")
    val seeds = remember { List(22) { i -> Triple((i * 37 % 100) / 100f, (i * 61 % 100) / 100f, 1.5f + (i % 4)) } }
    val dark = LocalMedi.current.dark
    Canvas(modifier) {
        seeds.forEachIndexed { i, (x, y, r) ->
            val yy = ((y - p * (0.3f + (i % 3) * 0.15f)) % 1f + 1f) % 1f
            val xx = x + 0.02f * sin((p * 2 * PI + i).toFloat())
            val c = if (i % 3 == 0) Palette.Cyan else if (i % 3 == 1) Palette.Purple else Palette.Teal
            drawCircle(c.copy(alpha = if (dark) 0.35f else 0.22f), radius = r.dp.toPx(), center = Offset(xx * size.width, yy * size.height))
        }
    }
}

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    padding: Dp = 16.dp,
    accent: Brush? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val m = LocalMedi.current
    val shape = RoundedCornerShape(24.dp)
    Column(
        modifier
            .clip(shape)
            .background(m.glass)
            .then(if (accent != null) Modifier.background(accent, alpha = 0.10f) else Modifier)
            .border(1.dp, Brush.linearGradient(listOf(m.glassBorder, m.glassBorder.copy(alpha = m.glassBorder.alpha * 0.3f))), shape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(padding),
        content = content
    )
}

@Composable
fun SectionHeader(title: String, action: String? = null, onAction: (() -> Unit)? = null) {
    Row(Modifier.fillMaxWidth().padding(top = 18.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
        if (action != null && onAction != null) TextButton(onClick = onAction) { Text(action) }
    }
}

@Composable
fun Pill(text: String, color: Color, modifier: Modifier = Modifier) {
    Text(
        text, color = color, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold,
        modifier = modifier.clip(RoundedCornerShape(50)).background(color.copy(alpha = 0.16f)).padding(horizontal = 8.dp, vertical = 3.dp)
    )
}

@Composable
fun DemoBadge() = Pill("DEMO DATA", Palette.Amber)

@Composable
fun SourceBadge(source: String) =
    if (source == "DEVICE") Pill("DEVICE", Palette.Green) else Pill("MANUAL", Palette.Blue)

@Composable
fun IconBubble(icon: ImageVector, brush: Brush, size: Dp = 40.dp) {
    Box(Modifier.size(size).clip(CircleShape).background(brush), contentAlignment = Alignment.Center) {
        Icon(icon, null, tint = Color.White, modifier = Modifier.size(size * 0.52f))
    }
}

@Composable
fun AnimatedRing(
    progress: Float,
    modifier: Modifier = Modifier,
    size: Dp = 120.dp,
    stroke: Dp = 12.dp,
    colors: List<Color> = listOf(Palette.Cyan, Palette.Purple, Palette.Pink, Palette.Cyan),
    center: @Composable () -> Unit = {}
) {
    val anim by animateFloatAsState(progress.coerceIn(0f, 1f), tween(1400, easing = FastOutSlowInEasing), label = "ring")
    val track = LocalMedi.current.glassBorder
    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val s = stroke.toPx()
            val arcSize = Size(this.size.width - s, this.size.height - s)
            drawArc(track, -90f, 360f, false, topLeft = Offset(s / 2, s / 2), size = arcSize, style = Stroke(s, cap = StrokeCap.Round))
            drawArc(Brush.sweepGradient(colors), -90f, 360f * anim, false, topLeft = Offset(s / 2, s / 2), size = arcSize, style = Stroke(s, cap = StrokeCap.Round))
        }
        center()
    }
}

@Composable
fun HeartbeatIcon(modifier: Modifier = Modifier, size: Dp = 28.dp, color: Color = Palette.Rose) {
    val reduce = LocalMedi.current.reduceMotion
    val t = rememberInfiniteTransition(label = "beat")
    val s by t.animateFloat(1f, if (reduce) 1f else 1.18f, infiniteRepeatable(tween(520, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "s")
    Box(modifier.size(size * 1.6f), contentAlignment = Alignment.Center) {
        Box(Modifier.size(size * 1.4f).scale(s).clip(CircleShape).background(color.copy(alpha = 0.18f)))
        Icon(Icons.Rounded.Favorite, "heartbeat", tint = color, modifier = Modifier.size(size).scale(s))
    }
}

@Composable
fun PulsingDot(color: Color, modifier: Modifier = Modifier) {
    val reduce = LocalMedi.current.reduceMotion
    val t = rememberInfiniteTransition(label = "dot")
    val a by t.animateFloat(0.35f, if (reduce) 0.35f else 1f, infiniteRepeatable(tween(900), RepeatMode.Reverse), label = "a")
    Box(modifier.size(10.dp).clip(CircleShape).background(color.copy(alpha = a)))
}

@Composable
fun EmptyState(icon: ImageVector, title: String, subtitle: String, action: String? = null, onAction: (() -> Unit)? = null) {
    GlassCard(Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().padding(vertical = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            IconBubble(icon, Palette.primaryGradient, 52.dp)
            Spacer(Modifier.height(12.dp))
            Text(title, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = LocalMedi.current.subtle)
            if (action != null && onAction != null) TextButton(onClick = onAction) { Text(action) }
        }
    }
}

/** Animated line chart drawn directly on Canvas (no external chart dependency). */
@Composable
fun LineChart(
    values: List<Float>,
    modifier: Modifier = Modifier,
    color: Color = Palette.Cyan,
    secondValues: List<Float>? = null,
    secondColor: Color = Palette.Purple,
    refLow: Float? = null,
    refHigh: Float? = null,
    description: String = "Trend chart"
) {
    val prog = remember(values) { Animatable(0f) }
    LaunchedEffect(values) { prog.animateTo(1f, tween(1100, easing = FastOutSlowInEasing)) }
    val grid = LocalMedi.current.glassBorder
    Canvas(modifier.fillMaxWidth().height(170.dp).semantics { contentDescription = description }) {
        if (values.isEmpty()) return@Canvas
        val all = values + (secondValues ?: emptyList()) + listOfNotNull(refLow, refHigh)
        var mn = all.min(); var mx = all.max()
        if (mx - mn < 1e-3f) { mx += 1f; mn -= 1f }
        val pad = (mx - mn) * 0.12f; mn -= pad; mx += pad
        fun y(v: Float) = size.height - (v - mn) / (mx - mn) * size.height
        for (i in 0..3) { val yy = size.height * i / 3f; drawLine(grid, Offset(0f, yy), Offset(size.width, yy), 1f) }
        if (refLow != null || refHigh != null) {
            val top = y(refHigh ?: mx); val bot = y(refLow ?: mn)
            drawRect(Palette.Green.copy(alpha = 0.10f), topLeft = Offset(0f, top), size = Size(size.width, bot - top))
        }
        fun series(vs: List<Float>, c: Color, fill: Boolean) {
            if (vs.size == 1) { drawCircle(c, 6f, Offset(size.width / 2, y(vs[0]))); return }
            val step = size.width / (vs.size - 1)
            val path = Path()
            vs.forEachIndexed { i, v ->
                val x = i * step; val yy = y(v)
                if (i == 0) path.moveTo(x, yy) else {
                    val px = (i - 1) * step; val py = y(vs[i - 1])
                    path.cubicTo((px + x) / 2, py, (px + x) / 2, yy, x, yy)
                }
            }
            clipRectReveal(prog.value) {
                if (fill) {
                    val area = Path().apply { addPath(path); lineTo(size.width, size.height); lineTo(0f, size.height); close() }
                    drawPath(area, Brush.verticalGradient(listOf(c.copy(alpha = 0.35f), Color.Transparent)))
                }
                drawPath(path, c, style = Stroke(4f, cap = StrokeCap.Round))
                vs.forEachIndexed { i, v -> drawCircle(c, 5f, Offset(i * step, y(v))) }
            }
        }
        series(values, color, true)
        secondValues?.let { series(it, secondColor, false) }
    }
}

private inline fun androidx.compose.ui.graphics.drawscope.DrawScope.clipRectReveal(p: Float, block: androidx.compose.ui.graphics.drawscope.DrawScope.() -> Unit) {
    drawContext.canvas.save()
    drawContext.canvas.clipRect(0f, 0f, size.width * p, size.height)
    block()
    drawContext.canvas.restore()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediTopBar(title: String, onBack: (() -> Unit)? = null, actions: @Composable () -> Unit = {}) {
    TopAppBar(
        title = { Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        navigationIcon = { if (onBack != null) IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back") } },
        actions = { actions() },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
    )
}

@Composable
fun KeyValue(k: String, v: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
        Text(k, style = MaterialTheme.typography.bodyMedium, color = LocalMedi.current.subtle, modifier = Modifier.width(120.dp))
        Text(v, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
    }
}

@Composable
fun Disclaimer(text: String) {
    Text(text, style = MaterialTheme.typography.bodySmall, color = LocalMedi.current.subtle, modifier = Modifier.padding(vertical = 8.dp))
}

@Composable
fun SpacerH(h: Dp) = Spacer(Modifier.height(h))

@Composable
fun RowCentered(modifier: Modifier = Modifier, content: @Composable () -> Unit) =
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) { content() }
