package com.habittracker.feature.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.habittracker.R
import com.habittracker.core.designsystem.Apricot
import com.habittracker.core.designsystem.BadgeBackground
import com.habittracker.core.designsystem.BadgeText
import com.habittracker.core.designsystem.BorderSoft
import com.habittracker.core.designsystem.CardSurface
import com.habittracker.core.designsystem.SoftCard
import com.habittracker.core.designsystem.SurfaceTint
import com.habittracker.core.designsystem.TextPrimary
import com.habittracker.core.designsystem.TextSecondary
import com.habittracker.core.designsystem.TextTertiary
import com.habittracker.core.designsystem.WarmShadow
import com.habittracker.core.designsystem.WarmWhite
import com.habittracker.data.local.entity.TrackingType

private val ApricotInk = Color(0xFF5A3510)
private val IdleCell = Color(0xFFF7F4EE)
private val WeekdayLabels = listOf("一", "二", "三", "四", "五", "六", "日")

/** Habit detail page, built to orgins/习惯详情-html/Detail.dc.html. */
@Composable
fun HabitDetailScreen(presenter: HabitDetailPresenter, onBack: () -> Unit) {
    val state by presenter.state.collectAsState()
    val lastError by presenter.lastError.collectAsState()
    var editing by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(WarmWhite)
            .verticalScroll(rememberScrollState())
            .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        DetailTopBar(
            title = state.habit?.name ?: "习惯详情",
            onBack = onBack,
            onEdit = { editing = true },
            editEnabled = state.habit != null,
        )

        when {
            state.isLoading -> Text("读取中…", fontSize = 14.sp, color = TextTertiary)
            state.notFound -> Text("习惯不存在或已被删除。", fontSize = 14.sp, color = TextTertiary)
            else -> {
                val week = state.week
                if (week != null) {
                    StreakCard(
                        current = state.currentStreak,
                        longest = state.longestStreak,
                        month = state.monthCompletions,
                    )
                    WeekCard(week = week)
                    FiveWeeksCard(weeks = state.weeks)
                    StatCards(stats = state.stats)
                }
                ScheduleRow(state.frequencyLabel, state.targetLabel)
            }
        }

        if (lastError != null) {
            Text(
                text = lastError.orEmpty(),
                fontSize = 13.sp,
                color = BadgeText,
                modifier = Modifier.clickable { presenter.dismissError() },
            )
        }
    }

    val habit = state.habit
    if (editing && habit != null) {
        DetailEditDialog(
            name = habit.name,
            numeric = habit.trackingType == TrackingType.NUMERIC,
            target = HabitDetailRules.formatAmount(habit.targetValue),
            unit = habit.unit.orEmpty(),
            onDismiss = { editing = false },
            onConfirm = { name, target, unit ->
                presenter.edit(name, target, unit)
                editing = false
            },
        )
    }
}

@Composable
private fun DetailTopBar(title: String, onBack: () -> Unit, onEdit: () -> Unit, editEnabled: Boolean) {
    Row(modifier = Modifier.fillMaxWidth().height(44.dp), verticalAlignment = Alignment.CenterVertically) {
        CircleButton(icon = R.drawable.ic_arrow_back, description = "返回", onClick = onBack)
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
            Text(
                text = title,
                fontSize = 20.sp,
                lineHeight = 28.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                maxLines = 1,
            )
        }
        CircleButton(
            icon = R.drawable.ic_edit,
            description = "编辑习惯",
            enabled = editEnabled,
            onClick = onEdit,
        )
    }
}

@Composable
private fun CircleButton(
    icon: Int,
    description: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .shadow(4.dp, CircleShape, spotColor = WarmShadow.copy(alpha = 0.08f))
            .clip(CircleShape)
            .background(CardSurface)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = description,
            tint = TextPrimary,
            modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
private fun StreakCard(current: Int, longest: Int, month: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(128.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(BadgeBackground)
            .padding(start = 20.dp, end = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column {
            Text("当前连续", fontSize = 13.sp, lineHeight = 18.sp, color = TextSecondary)
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = current.toString(),
                    fontSize = 60.sp,
                    lineHeight = 68.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = BadgeText,
                )
                Text(
                    text = "天",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = BadgeText,
                    modifier = Modifier.padding(start = 4.dp, bottom = 10.dp),
                )
            }
            Text(
                text = "最长 " + longest + " 天 · 本月 " + month + " 次",
                fontSize = 13.sp,
                lineHeight = 18.sp,
                color = TextSecondary,
            )
        }
        Box(
            modifier = Modifier.size(72.dp).clip(CircleShape).background(Apricot),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_leaf),
                contentDescription = null,
                tint = ApricotInk,
                modifier = Modifier.size(36.dp),
            )
        }
    }
}

@Composable
private fun WeekCard(week: DetailWeek) {
    SoftCard(modifier = Modifier.fillMaxWidth(), padding = PaddingValues(16.dp)) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth().height(22.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("本周", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Text(
                    text = week.done.toString() + " / " + week.due,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = BadgeText,
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                week.days.forEach { day ->
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        DayDot(day)
                    }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                week.days.forEachIndexed { index, day ->
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        Text(
                            text = if (day.isToday) "今" else WeekdayLabels[index],
                            fontSize = 12.sp,
                            lineHeight = 16.sp,
                            fontWeight = if (day.isToday) FontWeight.Bold else FontWeight.Normal,
                            color = if (day.isToday) BadgeText else TextTertiary,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DayDot(day: DetailDay) {
    val fill = when (day.state) {
        DayState.DONE -> Apricot
        DayState.MISSED -> BorderSoft
        DayState.NOT_DUE, DayState.FUTURE -> IdleCell
    }
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(fill)
            .then(if (day.isToday) Modifier.border(2.dp, BadgeText, CircleShape) else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        if (day.state == DayState.DONE) {
            Icon(
                painter = painterResource(R.drawable.ic_check),
                contentDescription = null,
                tint = ApricotInk,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
private fun FiveWeeksCard(weeks: List<List<DetailDay>>) {
    SoftCard(modifier = Modifier.fillMaxWidth(), padding = PaddingValues(16.dp)) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth().height(22.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("近 5 周", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    LegendDot(BorderSoft, "未完成")
                    Box(modifier = Modifier.width(6.dp))
                    LegendDot(Apricot, "完成")
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                WeekdayLabels.forEach { label ->
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        Text(label, fontSize = 12.sp, lineHeight = 16.sp, color = TextTertiary)
                    }
                }
            }
            Column(modifier = Modifier.padding(top = 6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                weeks.forEach { week ->
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        week.forEach { day ->
                            val fill = when (day.state) {
                                DayState.DONE -> Apricot
                                DayState.MISSED -> BorderSoft
                                DayState.NOT_DUE, DayState.FUTURE -> IdleCell
                            }
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(26.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(fill)
                                    .then(
                                        if (day.isToday) {
                                            Modifier.border(2.dp, BadgeText, RoundedCornerShape(8.dp))
                                        } else {
                                            Modifier
                                        },
                                    ),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(10.dp).clip(RoundedCornerShape(3.dp)).background(color))
        Text(
            text = label,
            fontSize = 12.sp,
            color = TextTertiary,
            modifier = Modifier.padding(start = 4.dp),
        )
    }
}

@Composable
private fun StatCards(stats: DetailStats) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        StatCard(label = "累计", value = stats.totalCompletions.toString(), suffix = "次", modifier = Modifier.weight(1f))
        StatCard(label = "近 5 周", value = stats.fiveWeekRatePercent.toString(), suffix = "%", modifier = Modifier.weight(1f))
        StatCard(label = "本月", value = stats.monthCompletions.toString(), suffix = "次", modifier = Modifier.weight(1f))
    }
}

@Composable
private fun StatCard(label: String, value: String, suffix: String, modifier: Modifier = Modifier) {
    SoftCard(modifier = modifier.height(76.dp), corner = 20.dp, padding = PaddingValues(horizontal = 14.dp, vertical = 12.dp)) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
        ) {
            Text(label, fontSize = 12.sp, lineHeight = 16.sp, color = TextTertiary)
            Row(verticalAlignment = Alignment.Bottom) {
                Text(value, fontSize = 24.sp, lineHeight = 32.sp, fontWeight = FontWeight.ExtraBold, color = TextPrimary)
                Text(
                    text = suffix,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    modifier = Modifier.padding(start = 2.dp, bottom = 4.dp),
                )
            }
        }
    }
}

@Composable
private fun ScheduleRow(frequency: String, target: String) {
    SoftCard(modifier = Modifier.fillMaxWidth(), corner = 20.dp) {
        Row(
            modifier = Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier.size(32.dp).clip(CircleShape).background(SurfaceTint),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_calendar),
                    contentDescription = null,
                    tint = TextTertiary,
                    modifier = Modifier.size(18.dp),
                )
            }
            Text(
                text = frequency + " · " + target,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                fontWeight = FontWeight.Medium,
                color = TextPrimary,
                modifier = Modifier.padding(start = 12.dp),
            )
        }
    }
}

@Composable
private fun DetailEditDialog(
    name: String,
    numeric: Boolean,
    target: String,
    unit: String,
    onDismiss: () -> Unit,
    onConfirm: (String, Double, String?) -> Unit,
) {
    var editedName by remember(name) { mutableStateOf(name) }
    var editedTarget by remember(target) { mutableStateOf(target) }
    var editedUnit by remember(unit) { mutableStateOf(unit) }
    val parsedTarget = if (numeric) editedTarget.trim().toDoubleOrNull() else 1.0
    val canSave = editedName.trim().isNotEmpty() && parsedTarget != null && parsedTarget > 0.0 &&
        (!numeric || editedUnit.trim().isNotEmpty())
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        containerColor = CardSurface,
        title = { Text("编辑习惯", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = editedName,
                    onValueChange = { editedName = it },
                    label = { Text("名称") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                )
                if (numeric) {
                    OutlinedTextField(
                        value = editedTarget,
                        onValueChange = { editedTarget = it },
                        label = { Text("目标值") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    )
                    OutlinedTextField(
                        value = editedUnit,
                        onValueChange = { editedUnit = it },
                        label = { Text("单位") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { if (canSave) onConfirm(editedName, parsedTarget ?: 1.0, if (numeric) editedUnit else null) },
                enabled = canSave,
            ) { Text("保存", fontWeight = FontWeight.Bold) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
}
