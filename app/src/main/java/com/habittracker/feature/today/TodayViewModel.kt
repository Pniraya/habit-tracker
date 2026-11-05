package com.habittracker.feature.today

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import com.habittracker.data.local.HabitDatabase
import com.habittracker.data.repository.RoomHabitRepository
import com.habittracker.domain.usecase.RecordHabitProgressUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.StateFlow

/**
 * Thin lifecycle wrapper: the state lives in [TodayPresenter] so it can be tested without Android.
 */
class TodayViewModel(
    private val presenter: TodayPresenter,
    private val scope: CoroutineScope,
) : ViewModel() {

    val state: StateFlow<TodayUiState> = presenter.state
    val lastError: StateFlow<String?> = presenter.lastError

    fun toggle(habitId: Long) = presenter.toggle(habitId)
    fun record(habitId: Long, value: Double) = presenter.record(habitId, value)
    fun dismissError() = presenter.dismissError()

    override fun onCleared() {
        scope.cancel()
    }

    companion object {
        fun factory(context: Context): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                val repository = RoomHabitRepository(HabitDatabase.getInstance(context))
                val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
                val presenter = TodayPresenter(
                    repository = repository,
                    recordProgress = RecordHabitProgressUseCase(repository),
                    scope = scope,
                )
                @Suppress("UNCHECKED_CAST")
                return TodayViewModel(presenter, scope) as T
            }
        }
    }
}
