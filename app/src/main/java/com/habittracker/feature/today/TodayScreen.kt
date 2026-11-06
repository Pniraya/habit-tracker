package com.habittracker.feature.today

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.IconButton
import androidx.compose.material3.Card
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.habittracker.data.local.entity.TrackingType
import java.time.format.DateTimeFormatter

// Material3 FAB container is 56dp. The list has to clear the button plus the margin it sits in,
// so the bottom padding is derived instead of being a number with no visible justification.
private val FabMargin = 20.dp
private val FabSize = 56.dp
private val FabSafetyGap = 12.dp
private val ListBottomPadding = FabSize + FabMargin + FabSafetyGap

/**
 * Today list. All behaviour lives in [TodayPresenter]; this file only renders the state.
 */
@Composable
fun TodayScreen(
    state: TodayUiState,
    lastError: String?,
    onToggle: (Long) -> Unit,
    onRecord: (Long, Double) -> Unit,
    onCreate: () -> Unit,
    onArchive: (Long) -> Unit,
    onDismissError: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var numericTarget by remember { mutableStateOf<TodayHabit?>(null) }

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
            Text(
                text = "今日 · " + state.date.format(DateTimeFormatter.ofPattern("M 月 d 日")),
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(top = 20.dp, bottom = 4.dp),
            )

            if (lastError != null) {
                Text(
                    text = lastError,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.fillMaxWidth().clickable { onDismissError() }.padding(vertical = 8.dp),
                )
            }

            when {
                state.isLoading -> Text("读取中…", style = MaterialTheme.typography.bodyMedium)
                state.habits.isEmpty() -> Column {
                    Text(
                        "还没有习惯。先创建一个，今天就会出现。",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 12.dp, bottom = 12.dp),
                    )
                    Button(onClick = onCreate) { Text("新建习惯") }
                }
                else -> LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    // Leave room so the last row is not hidden behind the create button.
                    contentPadding = PaddingValues(bottom = ListBottomPadding),
                ) {
                    items(state.habits, key = { it.id }) { habit ->
                        HabitRow(
                            habit = habit,
                            onToggle = { onToggle(habit.id) },
                            onRecord = { numericTarget = habit },
                            onArchive = { onArchive(habit.id) },
                        )
                    }
                }
            }
        }

        // The empty state owns its own button; once habits exist this is the only create entry.
        if (TodayPresenter.showsCreateAction(state)) {
            FloatingActionButton(
                onClick = onCreate,
                modifier = Modifier.align(Alignment.BottomEnd).padding(FabMargin),
            ) {
                Icon(Icons.Filled.Add, contentDescription = "新建习惯")
            }
        }
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
private fun NumericInputDialog(habit: TodayHabit, onDismiss: () -> Unit, onConfirm: (Double) -> Unit) {
    // Decimals must survive the prefill: showing 0.5 as "0" made one tap on 记录 overwrite the
    // stored 0.5 with 0. The rule lives in the presenter so a JVM test can pin it.
    var text by remember(habit.id) { mutableStateOf(TodayPresenter.prefillText(habit.actualValue)) }
    // Negative input is refused here so the dialog does not close and discard what was typed.
    val parsed = text.trim().toDoubleOrNull()?.takeIf { it >= 0.0 }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(habit.name) },
        text = {
            Column {
                Text("今天的目标 " + TodayPresenter.formatQuantity(habit.targetValue) + " " + (habit.unit ?: ""))
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = normaliseQuantityInput(it) },
                    label = { Text("实际完成") },
                    singleLine = true,
                    // Decimal, not Number: actualValue is a Double, so 0.5 L must be typeable.
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { if (parsed != null) onConfirm(parsed) }, enabled = parsed != null) { Text("记录") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
}

@Composable
private fun HabitRow(habit: TodayHabit, onToggle: () -> Unit, onRecord: () -> Unit, onArchive: () -> Unit) {
    var menuOpen by remember(habit.id) { mutableStateOf(false) }
    val clickable = habit.isScheduledToday
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = clickable) {
                if (habit.trackingType == TrackingType.BOOLEAN) onToggle() else onRecord()
            },
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = habit.name,
                    style = MaterialTheme.typography.titleMedium,
                    textDecoration = if (habit.isCompleted) TextDecoration.LineThrough else null,
                )
                Text(
                    text = subtitle(habit),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                val streak = TodayPresenter.streakText(habit.currentStreak, habit.longestStreak)
                if (streak != null) {
                    Text(
                        text = streak,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Text(
                text = if (habit.isCompleted) "已完成" else "",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            // Archive needs a home before the habit can leave the list; it is the only destructive
            // action here, so it stays behind a menu instead of a swipe nobody can discover.
            Box {
                IconButton(onClick = { menuOpen = true }) {
                    Icon(Icons.Filled.MoreVert, contentDescription = "更多操作")
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    DropdownMenuItem(
                        text = { Text("归档") },
                        onClick = {
                            menuOpen = false
                            onArchive()
                        },
                    )
                }
            }
        }
    }
}

private fun subtitle(habit: TodayHabit): String = when {
    habit.isRestingToday -> "今天不在计划内"
    habit.trackingType == TrackingType.NUMERIC -> TodayPresenter.progressText(habit.actualValue, habit.targetValue, habit.unit)
    habit.isCompleted -> TodayPresenter.undoHint(true) ?: "今天已完成"
    else -> "点一下标记完成"
}
