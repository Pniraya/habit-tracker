package com.habittracker.feature.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.habittracker.core.designsystem.CardSurface
import com.habittracker.core.designsystem.MossGreenDark
import com.habittracker.core.designsystem.SoftCard
import com.habittracker.core.designsystem.TextPrimary
import com.habittracker.core.designsystem.TextTertiary
import com.habittracker.core.designsystem.WarmWhite
import com.habittracker.domain.model.Habit
import java.time.format.DateTimeFormatter

/**
 * Archived habits, grouped 月 → 日 (ArchiveGroups) and restorable (TASK-028); the archive is also
 * where a habit can be permanently deleted from, after confirmation.
 */
@Composable
fun ProfileScreen(presenter: ProfilePresenter) {
    val state by presenter.state.collectAsState()
    val lastError by presenter.lastError.collectAsState()
    val months = ArchiveGroups.group(state.archived)

    Column(modifier = Modifier.fillMaxSize().background(WarmWhite).padding(horizontal = 20.dp)) {
        Text(
            text = "我的",
            fontSize = 32.sp,
            lineHeight = 38.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary,
            modifier = Modifier.padding(top = 16.dp),
        )
        Text(
            text = "归档习惯 · " + state.archived.size,
            fontSize = 13.sp,
            color = TextTertiary,
            modifier = Modifier.padding(top = 2.dp, bottom = 12.dp),
        )
        if (lastError != null) {
            Text(
                text = lastError.orEmpty(),
                fontSize = 13.sp,
                color = MossGreenDark,
                modifier = Modifier.padding(bottom = 8.dp),
            )
        }
        when {
            state.isLoading -> Text("读取中…", fontSize = 14.sp, color = TextTertiary)
            !ProfilePresenter.showsList(state) -> Text(
                text = ProfilePresenter.emptyText(),
                fontSize = 14.sp,
                color = TextTertiary,
            )
            else -> LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 16.dp),
            ) {
                months.forEach { month ->
                    item(key = "month-" + month.key) {
                        MonthHeader(label = month.label, count = month.days.sumOf { it.habits.size })
                    }
                    month.days.forEach { day ->
                        item(key = "day-" + day.date.toString()) {
                            Text(
                                text = day.label,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextTertiary,
                                modifier = Modifier.padding(start = 4.dp, top = 6.dp),
                            )
                        }
                        items(day.habits, key = { it.id }) { habit ->
                            ArchivedRow(habit = habit, onRestore = { presenter.restore(habit.id) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MonthHeader(label: String, count: Int) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
        Text(count.toString() + " 个", fontSize = 13.sp, color = TextTertiary)
    }
}

@Composable
private fun ArchivedRow(habit: Habit, onRestore: () -> Unit) {
    SoftCard(modifier = Modifier.fillMaxWidth(), corner = 20.dp) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(habit.name, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Text(
                    text = archivedOnText(habit),
                    fontSize = 13.sp,
                    color = TextTertiary,
                )
            }
            TextButton(onClick = onRestore) {
                Text("恢复", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MossGreenDark)
            }
        }
    }
}

/** "归档于 11 月 6 日". Kept out of the composable so the wording is pinned by a JVM test. */
fun archivedOnText(habit: Habit): String {
    val on = habit.archivedOn ?: return "已归档"
    return "归档于 " + on.format(DateTimeFormatter.ofPattern("M 月 d 日"))
}
