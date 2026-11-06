package com.habittracker.feature.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.collectAsState
import com.habittracker.domain.model.Habit
import java.time.format.DateTimeFormatter

/**
 * Archived habits and the restore action (TASK-028). Archiving without a way back was a
 * one-way door: the row left the Today list and nothing in the app could bring it back.
 */
@Composable
fun ProfileScreen(presenter: ProfilePresenter) {
    val state by presenter.state.collectAsState()
    val lastError by presenter.lastError.collectAsState()
    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        Text(
            text = "我的",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(top = 20.dp, bottom = 12.dp),
        )
        if (lastError != null) {
            Text(
                text = lastError.orEmpty(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(bottom = 8.dp),
            )
        }
        when {
            state.isLoading -> Text("读取中…", style = MaterialTheme.typography.bodyMedium)
            !ProfilePresenter.showsList(state) -> Text(
                text = ProfilePresenter.emptyText(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(state.archived, key = { it.id }) { habit ->
                    ArchivedRow(habit = habit, onRestore = { presenter.restore(habit.id) })
                }
            }
        }
    }
}

@Composable
private fun ArchivedRow(habit: Habit, onRestore: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(habit.name, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = archivedOnText(habit),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            TextButton(onClick = onRestore) { Text("恢复") }
        }
    }
}

/** "归档于 11 月 6 日". Kept out of the composable so the wording is pinned by a JVM test. */
fun archivedOnText(habit: Habit): String {
    val on = habit.archivedOn ?: return "已归档"
    return "归档于 " + on.format(DateTimeFormatter.ofPattern("M 月 d 日"))
}