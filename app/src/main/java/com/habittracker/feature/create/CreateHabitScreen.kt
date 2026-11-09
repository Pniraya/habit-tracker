package com.habittracker.feature.create

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.habittracker.R
import com.habittracker.core.designsystem.CardSurface
import com.habittracker.core.designsystem.ChoicePill
import com.habittracker.core.designsystem.HabitIcon
import com.habittracker.core.designsystem.HabitVisuals
import com.habittracker.core.designsystem.MossGreen
import com.habittracker.core.designsystem.MossGreenDark
import com.habittracker.core.designsystem.PillButton
import com.habittracker.core.designsystem.SoftCard
import com.habittracker.core.designsystem.SurfaceTint
import com.habittracker.core.designsystem.TextPrimary
import com.habittracker.core.designsystem.TextTertiary
import com.habittracker.core.designsystem.WarmWhite
import com.habittracker.core.designsystem.WarmShadow
import com.habittracker.data.local.entity.FrequencyType
import com.habittracker.data.local.entity.TrackingType
import java.time.DayOfWeek

private val WEEKDAY_LABELS = listOf(
    DayOfWeek.MONDAY to "一",
    DayOfWeek.TUESDAY to "二",
    DayOfWeek.WEDNESDAY to "三",
    DayOfWeek.THURSDAY to "四",
    DayOfWeek.FRIDAY to "五",
    DayOfWeek.SATURDAY to "六",
    DayOfWeek.SUNDAY to "日",
)

/** Create-habit form, rebuilt to orgins/新建习惯-html. Every rule lives in [CreateHabitPresenter]. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CreateHabitScreen(
    form: CreateHabitForm,
    onNameChange: (String) -> Unit,
    onTrackingTypeChange: (TrackingType) -> Unit,
    onTargetValueChange: (String) -> Unit,
    onUnitChange: (String) -> Unit,
    onFrequencyChange: (FrequencyType) -> Unit,
    onWeekdayToggle: (DayOfWeek) -> Unit,
    onIconChange: (String) -> Unit,
    onColorChange: (String) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val accent = Color(HabitVisuals.parseColor(form.color))
    val context = androidx.compose.ui.platform.LocalContext.current
    // The system photo picker needs no storage permission; the picked image is copied into
    // app-private storage and only the file name is stored in habits.icon (TASK-037).
    val pickCustomIcon = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        if (uri != null) {
            com.habittracker.core.storage.CustomIcons.save(context, uri)?.let(onIconChange)
        }
    }
    Column(modifier = modifier.fillMaxSize().background(WarmWhite)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .shadow(4.dp, CircleShape, spotColor = WarmShadow.copy(alpha = 0.08f))
                    .clip(CircleShape)
                    .background(CardSurface)
                    .clickable(onClick = onCancel),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_close),
                    contentDescription = "关闭",
                    tint = TextPrimary,
                    modifier = Modifier.size(20.dp),
                )
            }
            Spacer(Modifier.weight(1f))
            Text("新建习惯", fontSize = 20.sp, lineHeight = 28.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            Spacer(Modifier.weight(1f))
            Spacer(Modifier.width(44.dp))
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            FormCard(title = "习惯名称") {
                BeigeField(value = form.name, onValueChange = onNameChange, placeholder = "例如：晨跑")
            }

            FormCard(title = "记录方式") {
                // FlowRow: at 320dp with a 1.5x font the two pills no longer fit on one line and
                // the second label used to be clipped to "记录" (TASK-040).
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    ChoicePill(
                        text = "完成 / 未完成",
                        selected = form.trackingType == TrackingType.BOOLEAN,
                        onClick = { onTrackingTypeChange(TrackingType.BOOLEAN) },
                    )
                    ChoicePill(
                        text = "记录数值",
                        selected = form.trackingType == TrackingType.NUMERIC,
                        onClick = { onTrackingTypeChange(TrackingType.NUMERIC) },
                    )
                }
            }

            if (form.needsTargetValue) {
                FormCard(title = "每日目标") {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        BeigeField(
                            value = form.targetValue,
                            onValueChange = onTargetValueChange,
                            modifier = Modifier.weight(1f),
                            keyboardType = KeyboardType.Decimal,
                        )
                        BeigeField(
                            value = form.unit,
                            onValueChange = onUnitChange,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }

            FormCard(title = "图标") {
                // 11 built-ins plus the gallery tile fill two rows of six, so the grid stays even.
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    HabitVisuals.ICONS.chunked(6).forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            row.forEach { icon ->
                                IconChoice(
                                    icon = icon,
                                    selected = HabitVisuals.iconKey(form.icon) == icon,
                                    accent = accent,
                                    onClick = { onIconChange(icon) },
                                )
                            }
                            if (row.size < 6) {
                                CustomIconChoice(
                                    icon = form.icon,
                                    accent = accent,
                                    onClick = {
                                        pickCustomIcon.launch(
                                            androidx.activity.result.PickVisualMediaRequest(
                                                androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia.ImageOnly,
                                            ),
                                        )
                                    },
                                )
                            }
                        }
                    }
                }
            }

            FormCard(title = "颜色") {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    HabitVisuals.COLORS.forEach { hex ->
                        val swatch = Color(HabitVisuals.parseColor(hex))
                        val selected = form.color == hex
                        Box(
                            modifier = Modifier
                                .size(54.dp)
                                .clickable { onColorChange(hex) }
                                .semantics { contentDescription = "颜色 " + hex },
                            contentAlignment = Alignment.Center,
                        ) {
                            if (selected) {
                                Box(Modifier.size(54.dp).border(2.dp, swatch, CircleShape))
                            }
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(swatch)
                                    .then(if (selected) Modifier.border(3.dp, Color.White, CircleShape) else Modifier),
                            )
                        }
                    }
                }
            }

            FormCard(title = "频率") {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    ChoicePill(
                        text = "每天",
                        selected = form.frequency == FrequencyType.DAILY,
                        onClick = { onFrequencyChange(FrequencyType.DAILY) },
                    )
                    ChoicePill(
                        text = "工作日",
                        selected = form.frequency == FrequencyType.WEEKDAYS,
                        onClick = { onFrequencyChange(FrequencyType.WEEKDAYS) },
                    )
                    ChoicePill(
                        text = "自定义",
                        selected = form.frequency == FrequencyType.CUSTOM,
                        onClick = { onFrequencyChange(FrequencyType.CUSTOM) },
                    )
                }
                if (form.needsWeekdays) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        WEEKDAY_LABELS.forEach { (day, label) ->
                            val selected = form.weekdays.contains(day)
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(40.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (selected) MossGreenDark else SurfaceTint)
                                    .clickable { onWeekdayToggle(day) },
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (selected) Color.White else TextPrimary,
                                )
                            }
                        }
                    }
                }
            }

            if (form.error != null) {
                Text(form.error, fontSize = 13.sp, color = Color(0xFFB5523B))
            }

            // Breathing room so the last card can scroll clear of the fixed save button.
            Spacer(Modifier.height(8.dp))
        }

        PillButton(
            text = if (form.isSaving) "保存中…" else "开始养成",
            enabled = !form.isSaving,
            onClick = onSave,
            modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 28.dp),
        )
    }
}

@Composable
private fun FormCard(title: String, content: @Composable () -> Unit) {
    SoftCard(modifier = Modifier.fillMaxWidth(), padding = PaddingValues(16.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(title, fontSize = 13.sp, lineHeight = 18.sp, fontWeight = FontWeight.Bold, color = TextTertiary)
            content()
        }
    }
}

/** The mockup's field: no Material chrome, just a 48dp beige rounded box. */
@Composable
private fun BeigeField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Text,
    placeholder: String? = null,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(SurfaceTint)
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        if (value.isEmpty() && placeholder != null) {
            Text(placeholder, fontSize = 17.sp, color = TextTertiary.copy(alpha = 0.8f))
        }
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = TextStyle(color = TextPrimary, fontSize = 17.sp, fontWeight = FontWeight.Medium),
            cursorBrush = SolidColor(MossGreen),
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
/** One square in the icon grid; shared by the second row’s gallery tile. */
@Composable
private fun RowScope.IconChoice(
    icon: String,
    selected: Boolean,
    accent: Color,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .weight(1f)
            .height(46.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(if (selected) accent.copy(alpha = 0.18f) else SurfaceTint)
            .then(
                if (selected) Modifier.border(2.dp, accent, RoundedCornerShape(14.dp)) else Modifier,
            )
            .clickable(onClick = onClick)
            .semantics { contentDescription = "图标 " + icon },
        contentAlignment = Alignment.Center,
    ) {
        HabitIcon(icon = icon, size = 22.dp, tint = if (selected) accent else TextTertiary)
    }
}

/** The gallery tile: shows the picked picture, or a plus when nothing custom is selected. */
@Composable
private fun RowScope.CustomIconChoice(icon: String, accent: Color, onClick: () -> Unit) {
    val selected = HabitVisuals.isCustom(icon)
    Box(
        modifier = Modifier
            .weight(1f)
            .height(46.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(if (selected) accent.copy(alpha = 0.18f) else SurfaceTint)
            .then(
                if (selected) Modifier.border(2.dp, accent, RoundedCornerShape(14.dp)) else Modifier,
            )
            .clickable(onClick = onClick)
            .semantics { contentDescription = "选择自定义图标" },
        contentAlignment = Alignment.Center,
    ) {
        if (selected) {
            HabitIcon(icon = icon, size = 22.dp)
        } else {
            Icon(
                painter = painterResource(R.drawable.ic_plus),
                contentDescription = null,
                tint = TextTertiary,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}
