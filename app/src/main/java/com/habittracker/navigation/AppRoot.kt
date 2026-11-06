package com.habittracker.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.habittracker.core.designsystem.CardSurface
import com.habittracker.core.designsystem.NavPill
import com.habittracker.core.designsystem.TextTertiary
import com.habittracker.core.designsystem.TrackGreen
import com.habittracker.data.local.HabitDatabase
import com.habittracker.data.repository.RoomHabitRepository
import com.habittracker.domain.usecase.CreateHabitUseCase
import com.habittracker.domain.usecase.RestoreHabitUseCase
import com.habittracker.feature.create.CreateHabitPresenter
import com.habittracker.feature.create.CreateHabitScreen
import com.habittracker.feature.profile.ProfilePresenter
import com.habittracker.feature.profile.ProfileScreen
import com.habittracker.feature.statistics.StatisticsPresenter
import com.habittracker.feature.statistics.StatisticsScreen
import com.habittracker.feature.today.TodayPresenter
import com.habittracker.feature.today.TodayScreen
import com.habittracker.feature.today.TodayViewModel

@Composable
fun AppRoot() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    Scaffold(
        bottomBar = {
            HabitBottomBar(
                currentRoute = currentRoute,
                onSelect = { destination ->
                    navController.navigate(destination.route) {
                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
            )
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = AppDestination.Today.route,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable(AppDestination.Today.route) {
                val context = LocalContext.current
                val todayViewModel: TodayViewModel = viewModel(factory = TodayViewModel.factory(context))
                val state by todayViewModel.state.collectAsState()
                // A rebuild is only triggered by database changes, and midnight changes nothing in
                // the database: without this the screen would keep showing yesterday's date and
                // yesterday's rows (TASK-024).
                // Deprecated in this Compose BOM, but the replacement (androidx.lifecycle.compose)
                // is not a dependency and adding one for this is not worth it (TASK-024 review).
                @Suppress("DEPRECATION")
                val lifecycleOwner = LocalLifecycleOwner.current
                DisposableEffect(lifecycleOwner) {
                    val observer = LifecycleEventObserver { _, event ->
                        if (TodayPresenter.refreshesOn(event.name)) todayViewModel.refresh()
                    }
                    lifecycleOwner.lifecycle.addObserver(observer)
                    onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
                }
                // Staying in the foreground across 00:00 fires no lifecycle event, so wait for the
                // next local midnight and rebuild then. A manual clock or timezone change while the
                // screen stays on is still not covered (registered as a known gap, TASK-024).
                LaunchedEffect(todayViewModel) {
                    // Entering the destination can also be a date change (another tab was showing
                    // while midnight passed), and that produces no lifecycle event.
                    if (todayViewModel.state.value.date != java.time.LocalDate.now()) {
                        todayViewModel.refresh()
                    }
                    while (true) {
                        kotlinx.coroutines.delay(
                            TodayPresenter.millisUntilNextMidnight(java.time.LocalDateTime.now()) + 1_000L,
                        )
                        todayViewModel.refresh()
                    }
                }
                val lastError by todayViewModel.lastError.collectAsState()
                TodayScreen(
                    state = state,
                    lastError = lastError,
                    onToggle = todayViewModel::toggle,
                    onRecord = todayViewModel::record,
                    onCreate = { navController.navigate(AppRoutes.CREATE_HABIT) },
                    onArchive = todayViewModel::archive,
                    onEdit = todayViewModel::edit,
                    onDismissError = todayViewModel::dismissError,
                )
            }
            composable(AppRoutes.CREATE_HABIT) {
                val context = LocalContext.current
                val scope = rememberCoroutineScope()
                val presenter = remember {
                    val repository = RoomHabitRepository(HabitDatabase.getInstance(context))
                    CreateHabitPresenter(CreateHabitUseCase(repository)::invoke, scope)
                }
                val form by presenter.form.collectAsState()
                LaunchedEffect(form.createdHabitId) {
                    if (form.createdHabitId != null) navController.popBackStack()
                }
                CreateHabitScreen(
                    form = form,
                    onNameChange = presenter::onNameChange,
                    onTrackingTypeChange = presenter::onTrackingTypeChange,
                    onTargetValueChange = presenter::onTargetValueChange,
                    onUnitChange = presenter::onUnitChange,
                    onFrequencyChange = presenter::onFrequencyChange,
                    onWeekdayToggle = presenter::onWeekdayToggle,
                    onIconChange = presenter::onIconChange,
                    onColorChange = presenter::onColorChange,
                    onSave = presenter::save,
                    onCancel = { navController.popBackStack() },
                )
            }
            composable(AppDestination.Statistics.route) {
                val context = LocalContext.current
                val scope = rememberCoroutineScope()
                val presenter = remember {
                    StatisticsPresenter(
                        repository = RoomHabitRepository(HabitDatabase.getInstance(context)),
                        scope = scope,
                    )
                }
                StatisticsScreen(presenter)
            }
            composable(AppDestination.Profile.route) {
                val context = LocalContext.current
                val scope = rememberCoroutineScope()
                val presenter = remember {
                    val repository = RoomHabitRepository(HabitDatabase.getInstance(context))
                    ProfilePresenter(
                        repository = repository,
                        restoreHabit = RestoreHabitUseCase(repository),
                        scope = scope,
                    )
                }
                ProfileScreen(presenter)
            }
        }
    }
}

/**
 * Bottom bar from orgins/: white sheet with 28dp top corners, one soft green pill behind the
 * selected destination. Material's NavigationBar cannot express the pill width/height and the
 * upward shadow of the mockup, so the bar is drawn explicitly.
 */
@Composable
private fun HabitBottomBar(currentRoute: String?, onSelect: (AppDestination) -> Unit) {
    Surface(
        color = CardSurface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        shadowElevation = 8.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .windowInsetsPadding(WindowInsets.navigationBars)
                .fillMaxWidth()
                .height(84.dp)
                .padding(start = 24.dp, end = 24.dp, top = 10.dp, bottom = 22.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            AppDestination.entries.forEach { destination ->
                val selected = currentRoute == destination.route
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (selected) NavPill else Color.Transparent)
                        .clickable { onSelect(destination) },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Icon(
                        painter = painterResource(destination.icon),
                        contentDescription = destination.label,
                        tint = if (selected) TrackGreen else TextTertiary,
                        modifier = Modifier.size(24.dp),
                    )
                    Text(
                        text = destination.label,
                        fontSize = 12.sp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                        color = if (selected) TrackGreen else TextTertiary,
                    )
                }
            }
        }
    }
}
