package com.habittracker.feature.profile

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.habittracker.core.designsystem.CardSurface
import com.habittracker.core.designsystem.MossGreenDark
import com.habittracker.core.designsystem.SoftCard
import com.habittracker.core.designsystem.TextPrimary
import com.habittracker.core.designsystem.TextTertiary
import com.habittracker.core.designsystem.WarmWhite
import com.habittracker.data.backup.BackupDocument
import com.habittracker.data.backup.BackupExporter
import com.habittracker.data.backup.BackupParseResult
import com.habittracker.data.backup.BackupParser
import com.habittracker.data.backup.BackupRestorer
import com.habittracker.data.local.HabitDatabase
import com.habittracker.domain.model.Habit
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Archived habits, grouped 月 → 日 (ArchiveGroups) and restorable (TASK-028); the archive is also
 * where a habit can be permanently deleted from, after confirmation.
 */
@Composable
fun ProfileScreen(presenter: ProfilePresenter) {
    val state by presenter.state.collectAsState()
    val lastError by presenter.lastError.collectAsState()
    val months = ArchiveGroups.group(state.archived)
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val exporter = remember(context) { BackupExporter(HabitDatabase.getInstance(context)) }
    var exportedCount by remember { mutableStateOf<Int?>(null) }
    var exportError by remember { mutableStateOf<String?>(null) }
    // SAF: the picker creates the document, so no storage permission is involved (TASK-030).
    val restorer = remember(context) { BackupRestorer(HabitDatabase.getInstance(context)) }
    var pendingRestore by remember { mutableStateOf<BackupDocument?>(null) }
    var restoreMessage by remember { mutableStateOf<String?>(null) }
    var restoreError by remember { mutableStateOf<String?>(null) }
    // OpenDocument: the picker grants read access to one file; parsing happens off the main thread.
    val openBackup = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val parsed = runCatching {
                    withContext(Dispatchers.IO) {
                        val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                            ?: error("无法读取所选文件")
                        if (bytes.size > BackupParser.MAX_BYTES) error("文件超过 8 MB")
                        BackupParser.parse(bytes.toString(Charsets.UTF_8))
                    }
                }
                parsed.onSuccess { result ->
                    when (result) {
                        is BackupParseResult.Ready -> {
                            pendingRestore = result.document
                            restoreError = null
                        }
                        is BackupParseResult.Invalid -> {
                            restoreError = result.errors.take(3).joinToString("\n")
                            restoreMessage = null
                        }
                    }
                }.onFailure {
                    restoreError = it.message ?: "读取失败"
                }
            }
        }
    }
    val createBackup = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json"),
    ) { uri ->
        if (uri != null) {
            scope.launch {
                runCatching { exporter.export(context, uri) }
                    .onSuccess {
                        exportedCount = it
                        exportError = null
                    }
                    .onFailure {
                        exportError = it.message ?: "导出失败"
                        exportedCount = null
                    }
            }
        }
    }

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
        SoftCard(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp), corner = 20.dp) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("数据备份", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Text(
                    text = "把全部习惯、计划、日志与提醒导出为 JSON 文件",
                    fontSize = 13.sp,
                    color = TextTertiary,
                    modifier = Modifier.padding(top = 2.dp),
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(
                        onClick = {
                            createBackup.launch(BackupExporter.fileName(LocalDate.now().toString()))
                        },
                    ) {
                        Text("导出备份", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MossGreenDark)
                    }
                    // "*/*" on purpose: a JSON file copied from a PC often reaches the
                    // provider without a MIME type, and the parser validates the content anyway.
                    TextButton(onClick = { openBackup.launch(arrayOf("*/*")) }) {
                        Text("从备份恢复", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextTertiary)
                    }
                }
                exportedCount?.let {
                    Text("已导出 " + it + " 个习惯", fontSize = 13.sp, color = MossGreenDark)
                }
                exportError?.let {
                    Text(it, fontSize = 13.sp, color = Color(0xFFB5523B))
                }
                restoreMessage?.let {
                    Text(it, fontSize = 13.sp, color = MossGreenDark)
                }
                restoreError?.let {
                    Text(it, fontSize = 13.sp, color = Color(0xFFB5523B))
                }
            }
        }
        pendingRestore?.let { document ->
            AlertDialog(
                onDismissRequest = { pendingRestore = null },
                title = { Text("覆盖并恢复？") },
                text = {
                    Text(
                        "将清空当前全部数据，再写入备份中的 " + document.habits.size + " 个习惯、" +
                            document.logs.size + " 条日志。此操作不可撤销。",
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            pendingRestore = null
                            scope.launch {
                                runCatching { restorer.restore(document) }
                                    .onSuccess {
                                        restoreMessage = "已恢复 " + document.habits.size + " 个习惯"
                                        restoreError = null
                                    }
                                    .onFailure {
                                        restoreError = it.message ?: "恢复失败"
                                        restoreMessage = null
                                    }
                            }
                        },
                    ) {
                        Text("覆盖并恢复", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFFB5523B))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { pendingRestore = null }) {
                        Text("取消", fontSize = 14.sp, color = TextTertiary)
                    }
                },
            )
        }
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
