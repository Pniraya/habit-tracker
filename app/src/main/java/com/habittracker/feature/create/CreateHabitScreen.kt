package com.habittracker.feature.create

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.habittracker.data.local.entity.FrequencyType
import com.habittracker.data.local.entity.TrackingType
import java.time.DayOfWeek

/** Create-habit form. Rendering only: every rule lives in [CreateHabitPresenter]. */
@Composable
fun CreateHabitScreen(
    form: CreateHabitForm,
    onNameChange: (String) -> Unit,
    onTrackingTypeChange: (TrackingType) -> Unit,
    onTargetValueChange: (String) -> Unit,
    onUnitChange: (String) -> Unit,
    onFrequencyChange: (FrequencyType) -> Unit,
    onWeekdayToggle: (DayOfWeek) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text("新建习惯", style = MaterialTheme.typography.headlineSmall)

        OutlinedTextField(
            value = form.name,
            onValueChange = onNameChange,
            label = { Text("名称") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        Text("记录方式", style = MaterialTheme.typography.labelLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = form.trackingType == TrackingType.BOOLEAN,
                onClick = { onTrackingTypeChange(TrackingType.BOOLEAN) },
                label = { Text("完成 / 未完成") },
            )
            FilterChip(
                selected = form.trackingType == TrackingType.NUMERIC,
                onClick = { onTrackingTypeChange(TrackingType.NUMERIC) },
                label = { Text("记录数值") },
            )
        }

        if (form.needsTargetValue) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = form.targetValue,
                    onValueChange = onTargetValueChange,
                    label = { Text("每天目标") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
                OutlinedTextField(
                    value = form.unit,
                    onValueChange = onUnitChange,
                    label = { Text("单位") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
            }
        }

        Text("频率", style = MaterialTheme.typography.labelLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = form.frequency == FrequencyType.DAILY,
                onClick = { onFrequencyChange(FrequencyType.DAILY) },
                label = { Text("每天") },
            )
            FilterChip(
                selected = form.frequency == FrequencyType.WEEKDAYS,
                onClick = { onFrequencyChange(FrequencyType.WEEKDAYS) },
                label = { Text("工作日") },
            )
            FilterChip(
                selected = form.frequency == FrequencyType.CUSTOM,
                onClick = { onFrequencyChange(FrequencyType.CUSTOM) },
                label = { Text("自定义") },
            )
        }

        if (form.needsWeekdays) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                WEEKDAY_LABELS.forEach { (day, label) ->
                    FilterChip(
                        selected = form.weekdays.contains(day),
                        onClick = { onWeekdayToggle(day) },
                        label = { Text(label) },
                    )
                }
            }
        }

        if (form.error != null) {
            Text(form.error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            Button(onClick = onSave, enabled = !form.isSaving, modifier = Modifier.weight(1f)) {
                Text(if (form.isSaving) "保存中…" else "保存")
            }
            Button(onClick = onCancel, modifier = Modifier.weight(1f)) { Text("取消") }
        }
    }
}

private val WEEKDAY_LABELS: List<Pair<DayOfWeek, String>> = listOf(
    DayOfWeek.MONDAY to "一",
    DayOfWeek.TUESDAY to "二",
    DayOfWeek.WEDNESDAY to "三",
    DayOfWeek.THURSDAY to "四",
    DayOfWeek.FRIDAY to "五",
    DayOfWeek.SATURDAY to "六",
    DayOfWeek.SUNDAY to "日",
)
