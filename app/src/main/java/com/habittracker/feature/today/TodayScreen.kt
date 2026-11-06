package com.habittracker.feature.today

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.habittracker.R
import com.habittracker.core.designsystem.BadgeText
import com.habittracker.core.designsystem.CardSurface
import com.habittracker.core.designsystem.DangerRed
import com.habittracker.core.designsystem.CheckCircle
import com.habittracker.core.designsystem.FallingLeaves
import com.habittracker.core.designsystem.HabitIconTile
import com.habittracker.core.designsystem.HabitVisuals
import com.habittracker.core.designsystem.MossGreen
import com.habittracker.core.designsystem.PillButton
import com.habittracker.core.designsystem.ProgressRing
import com.habittracker.core.designsystem.SectionLabel
import com.habittracker.core.designsystem.SoftCard
import com.habittracker.core.designsystem.StreakBadge
import com.habittracker.core.designsystem.SurfaceTint
import com.habittracker.core.designsystem.TextPrimary
import com.habittracker.core.designsystem.TextTertiary
import com.habittracker.core.designsystem.WarmWhite
import com.habittracker.data.local.entity.TrackingType
import java.time.LocalDate
import java.time.format.DateTimeFormatter

// The design places the create button 98dp above the screen bottom, i.e. 14dp above the 84dp nav
// bar. Scaffold already gives us the area above the bar, so only the 20dp margin is left to apply.
private val FabMargin = 20.dp
private val ListBottomPadding = 96.dp

private val monthDay = DateTimeFormatter.ofPattern("M月d日")
private val weekdays = listOf("周一", "周二", "周三", "周四", "周五", "周六", "周日")

/**
 * Today list, rebuilt to orgins/首页 · 今日-html. All behaviour lives in [TodayPresenter]; this
 * file only renders the state.
 */
@Composable
fun TodayScreen(
    state: TodayUiState,
    lastError: String?,
    onToggle: (Long) -> Unit,
    onRecord: (Long, Double) -> Unit,
    onCreate: () -> Unit,
    onArchive: (Long) -> Unit,
    onDelete: (Long) -> Unit,
    onEdit: (Long, String, Double, String?) -> Unit,
    onDismissError: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var numericTarget by remember { mutableStateOf<TodayHabit?>(null) }
    var editing by remember { mutableStateOf<TodayHabit?>(null) }
    var deleting by remember { mutableStateOf<TodayHabit?>(null) }

    val scheduled = state.habits.filter { it.isScheduledToday }
    val doneCount = scheduled.count { it.isCompleted }
    val total = scheduled.size
    val allDone = total > 0 && doneCount == total
    val bestStreak = HabitVisuals.bestStreak(scheduled.map { it.currentStreak })

    Box(modifier = modifier.fillMaxSize().background(WarmWhite)) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 20.dp,
                end = 20.dp,
                top = 16.dp,
                bottom = if (state.habits.isEmpty()) 20.dp else ListBottomPadding,
            ),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item { DateHeader(state.date) }
            item {
                TodayProgressCard(
                    done = doneCount,
                    total = total,
                    streak = bestStreak,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
            if (lastError != null) {
                item {
                    Text(
                        text = lastError,
                        fontSize = 13.sp,
                        color = BadgeText,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onDismissError() }
                            .padding(vertical = 4.dp),
                    )
                }
            }
            item { SectionLabel("我的习惯", modifier = Modifier.padding(top = 10.dp, bottom = 0.dp)) }
            when {
                state.isLoading -> item {
                    Text("读取中…", fontSize = 14.sp, color = TextTertiary)
                }
                state.habits.isEmpty() -> item {
                    EmptyCard(onCreate = onCreate)
                }
                else -> items(state.habits, key = { it.id }) { habit ->
                    HabitRow(
                        habit = habit,
                        onToggle = { onToggle(habit.id) },
                        onRecord = { numericTarget = habit },
                        onArchive = { onArchive(habit.id) },
                        onDelete = { deleting = habit },
                        onEdit = { editing = habit },
                    )
                }
            }
        }

        // The empty state owns its own button; once habits exist this is the only create entry.
        if (TodayPresenter.showsCreateAction(state)) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = FabMargin, bottom = FabMargin)
                    .shadow(10.dp, CircleShape, spotColor = MossGreen.copy(alpha = 0.4f))
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(MossGreen)
                    .clickable(onClick = onCreate),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_plus),
                    contentDescription = "新建习惯",
                    tint = Color.White,
                    modifier = Modifier.size(26.dp),
                )
            }
        }

        // 全部完成: leaves drift down instead of a dialog (mockup: 叶片轻轻飘落，不弹窗打扰).
        FallingLeaves(active = allDone, modifier = Modifier.fillMaxSize())
    }

    val deleteTarget = deleting
    if (deleteTarget != null) {
        DeleteHabitDialog(
            habit = deleteTarget,
            onDismiss = { deleting = null },
            onConfirm = {
                onDelete(deleteTarget.id)
                deleting = null
            },
        )
    }

    val editTarget = editing
    if (editTarget != null) {
        EditHabitDialog(
            habit = editTarget,
            onDismiss = { editing = null },
            onConfirm = { name, target, unit ->
                onEdit(editTarget.id, name, target, unit)
                editing = null
            },
        )
    }

    val target = numericTarget
    if (target != null) {
        NumericInputDialog(
            habit = target,
            onDismiss = { numericTarget = null },
            onConfirm = { value ->
                onRecord(target.id, value)
                numericTarget = null
            },
        )
    }
}

@Composable
private fun DateHeader(date: LocalDate) {
    Column {
        Text(
            text = date.format(monthDay) + " · " + weekdays[date.dayOfWeek.value - 1],
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = TextTertiary,
        )
        Text(
            text = "今天",
            fontSize = 32.sp,
            lineHeight = 38.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary,
        )
    }
}

@Composable
private fun TodayProgressCard(done: Int, total: Int, streak: Int, modifier: Modifier = Modifier) {
    val sweep = HabitVisuals.ringSweep(done, total)
    SoftCard(
        modifier = modifier.fillMaxWidth().heightIn(min = 132.dp),
        padding = PaddingValues(18.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Box(modifier = Modifier.size(96.dp), contentAlignment = Alignment.Center) {
                ProgressRing(progress = sweep, modifier = Modifier.fillMaxSize())
                Row {
                    Text(
                        text = done.toString(),
                        fontSize = 30.sp,
                        lineHeight = 36.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = TextPrimary,
                        modifier = Modifier.alignByBaseline(),
                    )
                    Text(
                        text = "/" + total,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextTertiary,
                        modifier = Modifier.alignByBaseline(),
                    )
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("今日完成", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = TextTertiary)
                Text(
                    text = HabitVisuals.todayMessage(done, total),
                    fontSize = 17.sp,
                    lineHeight = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                )
                if (streak > 0) StreakBadge(HabitVisuals.badgeText(streak))
            }
        }
    }
}

@Composable
private fun EmptyCard(onCreate: () -> Unit) {
    SoftCard(modifier = Modifier.fillMaxWidth(), padding = PaddingValues(20.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(
                text = "还没有习惯。先创建一个，今天就会出现。",
                fontSize = 15.sp,
                lineHeight = 22.sp,
                color = TextTertiary,
            )
            PillButton(text = "新建习惯", onClick = onCreate, modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun HabitRow(
    habit: TodayHabit,
    onToggle: () -> Unit,
    onRecord: () -> Unit,
    onArchive: () -> Unit,
    onDelete: () -> Unit,
    onEdit: () -> Unit,
) {
    var menuOpen by remember(habit.id) { mutableStateOf(false) }
    val haptics = LocalHapticFeedback.current
    val accent = Color(HabitVisuals.parseColor(habit.color))
    val clickable = habit.isScheduledToday

    SoftCard(modifier = Modifier.fillMaxWidth().height(66.dp), corner = 20.dp) {
        Row(
            modifier = Modifier.fillMaxSize().padding(start = 12.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            CheckCircle(
                completed = habit.isCompleted,
                enabled = clickable,
                accent = accent,
                description = if (habit.isCompleted) "取消完成：" + habit.name else "完成：" + habit.name,
                onClick = {
                    // 打卡有触觉反馈，抖动由 CheckCircle 的状态切换承担.
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    if (habit.trackingType == TrackingType.BOOLEAN) onToggle() else onRecord()
                },
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable(enabled = clickable) {
                        if (habit.trackingType == TrackingType.BOOLEAN) onToggle() else onRecord()
                    },
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = habit.name,
                    fontSize = 16.sp,
                    lineHeight = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (clickable) TextPrimary else TextTertiary,
                )
                Text(
                    text = subtitle(habit),
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    color = TextTertiary,
                )
            }
            // Archive and edit need a home; the mockup has no menu, so it stays a compact muted
            // 更多 button rather than a Material overflow on every card.
            Box {
                IconButton(onClick = { menuOpen = true }, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = Icons.Filled.MoreVert,
                        contentDescription = "更多操作",
                        tint = TextTertiary,
                        modifier = Modifier.size(18.dp),
                    )
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    DropdownMenuItem(
                        text = { Text("编辑") },
                        onClick = {
                            menuOpen = false
                            onEdit()
                        },
                    )
                    DropdownMenuItem(
                        text = { Text("归档") },
                        onClick = {
                            menuOpen = false
                            onArchive()
                        },
                    )
                    // Permanent delete is the destructive twin of archive (DATABASE.md 9): it is
                    // behind the same menu and still asks for confirmation.
                    DropdownMenuItem(
                        text = { Text("删除", color = DangerRed) },
                        onClick = {
                            menuOpen = false
                            onDelete()
                        },
                    )
                }
            }
            HabitIconTile(icon = habit.icon)
        }
    }
}

private fun subtitle(habit: TodayHabit): String {
    if (habit.isRestingToday) return "今天不在计划内"
    val streak = if (habit.currentStreak > 0) " · 连续 " + habit.currentStreak + " 天" else ""
    val base = when {
        habit.trackingType == TrackingType.NUMERIC -> TodayPresenter.progressText(habit.actualValue, habit.targetValue, habit.unit)
        habit.isCompleted -> "今天已完成"
        else -> "点一下标记完成"
    }
    return base + streak
}

@Composable
private fun NumericInputDialog(habit: TodayHabit, onDismiss: () -> Unit, onConfirm: (Double) -> Unit) {
    // Decimals must survive the prefill: showing 0.5 as "0" made one tap on 记录 overwrite the
    // stored 0.5 with 0. The rule lives in the presenter so a JVM test can pin it.
    var text by remember(habit.id) { mutableStateOf(TodayPresenter.prefillText(habit.actualValue)) }
    // Negative input is refused here so the dialog does not close and discard what was typed.
    val parsed = text.trim().toDoubleOrNull()?.takeIf { it >= 0.0 }
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        containerColor = CardSurface,
        title = { Text(habit.name, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "今天的目标 " + TodayPresenter.formatQuantity(habit.targetValue) + " " + (habit.unit ?: ""),
                    fontSize = 13.sp,
                    color = TextTertiary,
                )
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = normaliseQuantityInput(it) },
                    label = { Text("实际完成") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    // Decimal, not Number: actualValue is a Double, so 0.5 L must be typeable.
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { if (parsed != null) onConfirm(parsed) }, enabled = parsed != null) {
                Text("记录", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
}

@Composable
private fun DeleteHabitDialog(habit: TodayHabit, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        containerColor = CardSurface,
        title = { Text("删除「" + habit.name + "」？", fontWeight = FontWeight.Bold) },
        text = {
            Text(
                text = "该习惯的所有计划与打卡记录会一并删除，无法恢复。只想收起它的话，用「归档」即可。",
                fontSize = 14.sp,
                lineHeight = 21.sp,
                color = TextTertiary,
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text("删除", color = DangerRed, fontWeight = FontWeight.Bold) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
}

@Composable
private fun EditHabitDialog(
    habit: TodayHabit,
    onDismiss: () -> Unit,
    onConfirm: (String, Double, String?) -> Unit,
) {
    var name by remember(habit.id) { mutableStateOf(habit.name) }
    var target by remember(habit.id) { mutableStateOf(TodayPresenter.formatQuantity(habit.targetValue)) }
    var unit by remember(habit.id) { mutableStateOf(habit.unit.orEmpty()) }
    val numeric = habit.trackingType == TrackingType.NUMERIC
    val parsedTarget = if (numeric) target.trim().toDoubleOrNull() else 1.0
    val canSave = name.trim().isNotEmpty() && parsedTarget != null && parsedTarget > 0.0 &&
        (!numeric || unit.trim().isNotEmpty())
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        containerColor = CardSurface,
        title = { Text("编辑习惯", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("名称") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                )
                if (numeric) {
                    OutlinedTextField(
                        value = target,
                        onValueChange = { target = normaliseQuantityInput(it) },
                        label = { Text("目标值") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    )
                    OutlinedTextField(
                        value = unit,
                        onValueChange = { unit = it },
                        label = { Text("单位") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { if (canSave) onConfirm(name, parsedTarget ?: 1.0, if (numeric) unit else null) },
                enabled = canSave,
            ) { Text("保存", fontWeight = FontWeight.Bold) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
}
