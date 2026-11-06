package com.habittracker.feature.statistics

import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Palette lifted from orgins/统计-html/Stats.dc.html, so the app matches the design instead of
// approximating it. Names carry the role, not the hex, because the hex is easy to misread.
private val PageBackground = Color(0xFFFAF7F2)
private val CardWhite = Color(0xFFFFFFFF)
private val Ink = Color(0xFF2B2A27)
private val Muted = Color(0xFF6F6A61)
private val Settle = Color(0xFFEFEAE0)
private val RateCardBackground = Color(0xFFE4EDE6)
private val RateText = Color(0xFF3F6B4D)
private val StreakCardBackground = Color(0xFFFCEBD5)
private val StreakText = Color(0xFF8A4B0C)
private val HeatLevels = listOf(
    Color(0xFFE9E3D6),
    Color(0xFFCFE0D3),
    Color(0xFFA8C8B1),
    Color(0xFF7FA98C),
    Color(0xFF5B8C6A),
)
private val FutureDay = Color(0xFFF4F0E8)
private val TodayOutline = Color(0xFFF2A65A)
private val BarBlue = Color(0xFF8FAFC4)
private val BarToday = Color(0xFF5B8C6A)

/** Statistics page: month heat map plus a weekly trend, matching the design file. */
@Composable
fun StatisticsScreen(presenter: StatisticsPresenter) {
    val state by presenter.state.collectAsState()
    // The page is taller than a 390x844 artboard once the heat map and the trend card are both
    // present; without scrolling the trend chart was clipped and looked like it sat behind the
    // navigation bar.
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PageBackground)
            .verticalScroll(rememberScrollState())
            .padding(start = 20.dp, end = 20.dp, bottom = 16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("统计", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = Ink)
            PeriodSwitch()
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SummaryCard(
                label = "本月完成率",
                value = state.rateText,
                background = RateCardBackground,
                valueColor = RateText,
                modifier = Modifier.weight(1f),
            )
            SummaryCard(
                label = "最长连续",
                value = state.longestStreakText,
                suffix = "天",
                background = StreakCardBackground,
                valueColor = StreakText,
                modifier = Modifier.weight(1f),
            )
        }
        Column(
            modifier = Modifier
                .padding(top = 12.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(CardWhite)
                .padding(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(state.monthLabel, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Ink)
                Legend()
            }
            Row(modifier = Modifier.fillMaxWidth().padding(top = 10.dp)) {
                listOf("一", "二", "三", "四", "五", "六", "日").forEach { label ->
                    Text(
                        text = label,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        fontSize = 12.sp,
                        color = Muted,
                    )
                }
            }
            HeatMap(state.cells)
        }
        Column(
            modifier = Modifier
                .padding(top = 12.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(CardWhite)
                .padding(16.dp),
        ) {
            Text("本周趋势", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Ink)
            Row(
                modifier = Modifier.fillMaxWidth().height(72.dp).padding(top = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.Bottom,
            ) {
                state.bars.forEach { bar ->
                    Box(modifier = Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.BottomCenter) {
                        val height = if (bar.fraction <= 0f) 6.dp else (72.dp * bar.fraction).coerceAtLeast(8.dp)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(height)
                                .clip(RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp, bottomEnd = 6.dp, bottomStart = 6.dp))
                                .background(if (bar.isToday) BarToday else BarBlue),
                        )
                    }
                }
            }
            Row(modifier = Modifier.fillMaxWidth().padding(top = 6.dp)) {
                state.bars.forEach { bar ->
                    Text(
                        text = if (bar.isToday) "今" else bar.label,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        fontSize = 12.sp,
                        color = if (bar.isToday) RateText else Muted,
                        fontWeight = if (bar.isToday) FontWeight.Bold else FontWeight.Normal,
                    )
                }
            }
        }
    }
}

@Composable
private fun PeriodSwitch() {
    Row(
        modifier = Modifier.clip(RoundedCornerShape(999.dp)).background(Settle).padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        listOf("周", "月", "年").forEach { label ->
            val selected = label == "月"
            // 周/年 are drawn but not selectable yet: a tab that does nothing when tapped is a lie,
            // so they are visibly dimmed instead of pretending to work (TASK-031 known gap).
            val enabled = selected
            Surface(
                color = if (selected) CardWhite else Color.Transparent,
                shape = RoundedCornerShape(999.dp),
                shadowElevation = if (selected) 2.dp else 0.dp,
            ) {
                Text(
                    text = label,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    fontSize = 13.sp,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                    color = (if (selected) Ink else Muted).copy(alpha = if (enabled) 1f else 0.35f),
                )
            }
        }
    }
}

@Composable
private fun SummaryCard(
    label: String,
    value: String,
    background: Color,
    valueColor: Color,
    modifier: Modifier = Modifier,
    suffix: String? = null,
) {
    Column(
        modifier = modifier.height(76.dp).clip(RoundedCornerShape(20.dp)).background(background).padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text(label, fontSize = 13.sp, color = Color(0xFF55514A))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(value, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = valueColor)
            if (suffix != null) {
                Text(suffix, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = valueColor)
            }
        }
    }
}

@Composable
private fun Legend() {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        Text("少", fontSize = 12.sp, color = Muted)
        HeatLevels.forEach { color ->
            Box(modifier = Modifier.size(10.dp).clip(RoundedCornerShape(3.dp)).background(color))
        }
        Text("多", fontSize = 12.sp, color = Muted)
    }
}

@Composable
private fun HeatMap(cells: List<MonthCell>) {
    // 7 columns; the design uses 38dp rows and 6dp gaps, so the grid is built with weights.
    Column(modifier = Modifier.fillMaxWidth().padding(top = 6.dp)) {
        cells.chunked(7).forEach { week ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                week.forEach { cell ->
                    val color = when {
                        cell.day == null -> Color.Transparent
                        cell.isFuture -> FutureDay
                        else -> HeatLevels[cell.level]
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(38.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (cell.day == null) Color.Transparent else color)
                            .then(
                                if (cell.isToday) {
                                    Modifier.border(2.dp, TodayOutline, RoundedCornerShape(12.dp))
                                } else {
                                    Modifier
                                },
                            ),
                    )
                }
                // Fill the last row so the trailing cells keep their width.
                repeat(7 - week.size) { Box(modifier = Modifier.weight(1f)) }
            }
        }
    }
}