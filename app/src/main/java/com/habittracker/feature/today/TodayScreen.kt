package com.habittracker.feature.today

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import java.time.format.DateTimeFormatter

/**
 * Today list. All behaviour lives in [TodayPresenter]; this file only renders the state.
 */
@Composable
fun TodayScreen(
    state: TodayUiState,
    lastError: String?,
    onToggle: (Long) -> Unit,
    onDismissError: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize().padding(horizontal = 20.dp)) {
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
            state.habits.isEmpty() -> Text(
                "还没有习惯。先创建一个，今天就会出现。",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 12.dp),
            )
            else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(state.habits, key = { it.id }) { habit ->
                    HabitRow(habit = habit, onToggle = { onToggle(habit.id) })
                }
            }
        }
    }
}

@Composable
private fun HabitRow(habit: TodayHabit, onToggle: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = habit.isScheduledToday) { onToggle() },
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
            }
            Text(
                text = if (habit.isCompleted) "已完成" else "",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

private fun subtitle(habit: TodayHabit): String = when {
    habit.isRestingToday -> "今天不在计划内"
    habit.trackingType.name == "NUMERIC" -> {
        val target = habit.targetValue.toLong()
        val actual = habit.actualValue.toLong()
        actual.toString() + " / " + target + " " + (habit.unit ?: "")
    }
    habit.isCompleted -> "今天已完成"
    else -> "点一下标记完成"
}
