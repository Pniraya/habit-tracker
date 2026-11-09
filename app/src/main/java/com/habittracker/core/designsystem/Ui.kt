package com.habittracker.core.designsystem

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.habittracker.R

/**
 * Shared building blocks transcribed from the concept mockups. Every number here comes from an
 * inline style in orgins/ so the screens stay visually identical instead of approximating.
 */

/** White card with the mockup's 0 8 24 rgba(60,50,30,0.06) shadow and 24dp corners. */
@Composable
fun SoftCard(
    modifier: Modifier = Modifier,
    corner: Dp = 24.dp,
    padding: PaddingValues = PaddingValues(0.dp),
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(corner),
                ambientColor = WarmShadow.copy(alpha = 0.06f),
                spotColor = WarmShadow.copy(alpha = 0.06f),
            )
            .clip(RoundedCornerShape(corner))
            .background(CardSurface)
            .padding(padding),
        content = content,
    )
}

/** 96dp ring: 12/120 stroke on the design's viewBox, moss green arc, soft glow once complete. */
@Composable
fun ProgressRing(progress: Float, modifier: Modifier = Modifier) {
    val sweep by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 500),
        label = "ringSweep",
    )
    Canvas(modifier = modifier) {
        val stroke = size.minDimension * (12f / 120f)
        val radius = (size.minDimension - stroke) / 2f
        drawCircle(color = BorderSoft, radius = radius, style = Stroke(width = stroke))
        if (sweep > 0f) {
            if (sweep >= 1f) {
                // The mockup uses drop-shadow; a translucent thicker arc reads the same on Canvas.
                drawCircle(
                    color = MossGreen.copy(alpha = 0.22f),
                    radius = radius,
                    style = Stroke(width = stroke * 2.4f),
                )
            }
            drawArc(
                color = MossGreen,
                startAngle = -90f,
                sweepAngle = 360f * sweep,
                useCenter = false,
                topLeft = Offset(center.x - radius, center.y - radius),
                size = Size(radius * 2f, radius * 2f),
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )
        }
    }
}

/** 连续 N 天 pill: 杏橙底 + 叶片图标 + #8A4B0C 文字. */
@Composable
fun StreakBadge(text: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(999.dp))
            .background(BadgeBackground)
            .padding(start = 8.dp, end = 10.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_leaf),
            contentDescription = null,
            tint = BadgeIcon,
            modifier = Modifier.size(16.dp),
        )
        Spacer(Modifier.width(6.dp))
        Text(text = text, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = BadgeText)
    }
}

/** 44dp tap target with the 40dp check dot: filled + tick when done, 2dp outline when not. */
@Composable
fun CheckCircle(
    completed: Boolean,
    enabled: Boolean,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    description: String? = null,
) {
    Box(
        modifier = modifier
            .size(44.dp)
            .clip(CircleShape)
            .clickable(enabled = enabled, onClick = onClick)
            .semantics { description?.let { contentDescription = it } },
        contentAlignment = Alignment.Center,
    ) {
        if (completed) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .shadow(6.dp, CircleShape, spotColor = accent.copy(alpha = 0.32f))
                    .clip(CircleShape)
                    .background(accent),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_check),
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp),
                )
            }
        } else {
            Box(modifier = Modifier.size(40.dp).border(2.dp, CircleOutline, CircleShape))
        }
    }
}

/** 36dp tinted circle holding the habit's line icon, exactly like the row artwork in orgins/. */
@Composable
fun HabitIconTile(icon: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.size(36.dp).clip(CircleShape).background(SurfaceTint),
        contentAlignment = Alignment.Center,
    ) {
        HabitIcon(icon = icon, size = 20.dp)
    }
}

@Composable
fun HabitIcon(icon: String, size: Dp, modifier: Modifier = Modifier, tint: Color? = null) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val customFile = androidx.compose.runtime.remember(icon) {
        com.habittracker.core.storage.CustomIcons.file(context, icon)
    }
    val bitmap = androidx.compose.runtime.remember(customFile) {
        customFile?.takeIf { it.isFile }?.let { file ->
            runCatching { android.graphics.BitmapFactory.decodeFile(file.path) }.getOrNull()
        }
    }
    if (bitmap != null) {
        androidx.compose.foundation.Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = null,
            contentScale = androidx.compose.ui.layout.ContentScale.Crop,
            modifier = modifier.size(size).clip(CircleShape),
        )
        return
    }
    // A custom icon whose file disappeared falls back to the sprout instead of an empty tile.
    val key = HabitVisuals.iconKey(if (HabitVisuals.isCustom(icon)) HabitVisuals.ICON_SPROUT else icon)
    Icon(
        painter = painterResource(drawableFor(key)),
        contentDescription = null,
        tint = tint ?: tintFor(key),
        modifier = modifier.size(size),
    )
}

private fun drawableFor(key: String): Int = when (key) {
    HabitVisuals.ICON_WATER -> R.drawable.ic_habit_water
    HabitVisuals.ICON_BOOK -> R.drawable.ic_habit_book
    HabitVisuals.ICON_BREATHE -> R.drawable.ic_habit_breathe
    HabitVisuals.ICON_MOON -> R.drawable.ic_habit_moon
    HabitVisuals.ICON_HEART -> R.drawable.ic_habit_heart
    HabitVisuals.ICON_SUN -> R.drawable.ic_habit_sun
    HabitVisuals.ICON_COFFEE -> R.drawable.ic_habit_coffee
    HabitVisuals.ICON_DUMBBELL -> R.drawable.ic_habit_dumbbell
    HabitVisuals.ICON_PEN -> R.drawable.ic_habit_pen
    HabitVisuals.ICON_MUSIC -> R.drawable.ic_habit_music
    else -> R.drawable.ic_habit_sprout
}

/** Stroke colours from the mockup's row icons, so the tile keeps the design's warm accents. */
private fun tintFor(key: String): Color = when (key) {
    HabitVisuals.ICON_WATER, HabitVisuals.ICON_BREATHE -> Color(0xFF4F7A94)
    HabitVisuals.ICON_BOOK, HabitVisuals.ICON_MOON -> Color(0xFFA35A10)
    HabitVisuals.ICON_HEART -> Color(0xFFB5523B)
    HabitVisuals.ICON_SUN -> Color(0xFFB5741B)
    HabitVisuals.ICON_COFFEE, HabitVisuals.ICON_DUMBBELL -> Color(0xFF8A5A3B)
    HabitVisuals.ICON_PEN, HabitVisuals.ICON_MUSIC -> Color(0xFF6C5F8C)
    else -> Color(0xFF4A7A59)
}

/** Section label above the habit list ("我的习惯"). */
@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(text = text, modifier = modifier, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextTertiary)
}

/** Full-round primary button (开始养成 / 新建习惯). */
@Composable
fun PillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Box(
        modifier = modifier
            .height(56.dp)
            .shadow(10.dp, RoundedCornerShape(999.dp), spotColor = MossGreenDark.copy(alpha = 0.32f))
            .clip(RoundedCornerShape(999.dp))
            .background(if (enabled) MossGreenDark else SwitchOff)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = text, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Color.White)
    }
}

/** Frequency / tracking-type choice. Selected: 深绿底白字; unselected: 暖白底深字. */
@Composable
fun ChoicePill(text: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            // min height keeps the 44dp mockup at 1x and lets the pill grow when the system
            // font is enlarged, instead of clipping the label (TASK-040).
            .heightIn(min = 48.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(if (selected) MossGreenDark else SurfaceTint)
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = if (selected) Color.White else TextPrimary,
        )
    }
}

/**
 * 全部完成态: leaves drift down and fade, no dialog. The mockup animates each leaf over 6s with a
 * different delay; phase offset reproduces that without six separate animations.
 */
@Composable
fun FallingLeaves(active: Boolean, modifier: Modifier = Modifier) {
    if (!active) return
    val transition = rememberInfiniteTransition(label = "leaves")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(durationMillis = 6000, easing = LinearEasing)),
        label = "leafFall",
    )
    val palette = listOf(MossGreen, Apricot, MistBlue)
    Canvas(modifier = modifier.fillMaxSize()) {
        val leaf = Path().apply {
            moveTo(5f, 19f)
            cubicTo(5f, 11f, 10f, 6f, 19f, 5f)
            cubicTo(19f, 14f, 14f, 19f, 6f, 19f)
            close()
        }
        repeat(6) { index ->
            val phase = (progress + index * 0.19f) % 1f
            val leafSize = (14f + (index % 3) * 4f).dp.toPx()
            val scale = leafSize / 24f
            val x = size.width * (0.09f + 0.155f * index)
            val y = -40.dp.toPx() + phase * (size.height + 80.dp.toPx())
            val fadeIn = if (phase < 0.12f) phase / 0.12f else 1f
            val alpha = (fadeIn * (1f - phase)).coerceIn(0f, 1f)
            withTransform({
                translate(x, y)
                rotate(240f * phase, pivot = Offset(12f * scale, 12f * scale))
                scale(scale, scale, pivot = Offset.Zero)
            }) {
                drawPath(leaf, color = palette[index % palette.size].copy(alpha = alpha))
            }
        }
    }
}
