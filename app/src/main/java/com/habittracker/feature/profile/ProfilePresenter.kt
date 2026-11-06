package com.habittracker.feature.profile

import com.habittracker.domain.model.Habit
import com.habittracker.domain.repository.HabitRepository
import com.habittracker.domain.usecase.RestoreHabitUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ProfileUiState(
    val archived: List<Habit> = emptyList(),
    val isLoading: Boolean = true,
)

/**
 * Archived habits and their restore action. Plain Kotlin for the same reason as [TodayPresenter]:
 * the JVM tests can drive it without an Android ViewModel or a main dispatcher.
 */
class ProfilePresenter(
    private val repository: HabitRepository,
    private val restoreHabit: RestoreHabitUseCase,
    private val scope: CoroutineScope,
    private val now: () -> Long = { System.currentTimeMillis() },
) {

    private val _state = MutableStateFlow(ProfileUiState())
    val state: StateFlow<ProfileUiState> = _state.asStateFlow()

    private val _lastError = MutableStateFlow<String?>(null)
    val lastError: StateFlow<String?> = _lastError.asStateFlow()

    init {
        scope.launch {
            repository.observeArchivedHabits().collect { archived ->
                _state.value = ProfileUiState(archived = archived, isLoading = false)
            }
        }
    }

    /**
     * Restores the habit and rebuilds the list locally, so the row disappears even when the
     * repository Flow does not re-emit (pinned by a test with an emit-once repository).
     */
    fun restore(habitId: Long) {
        scope.launch {
            runCatching { restoreHabit(habitId, now()) }
                .onSuccess {
                    _lastError.value = null
                    _state.value = _state.value.copy(
                        archived = _state.value.archived.filterNot { it.id == habitId },
                    )
                }
                .onFailure { _lastError.value = it.message ?: "无法恢复" }
        }
    }

    fun dismissError() {
        _lastError.value = null
    }

    companion object {
        /** Empty state wording: it must not read like an error. */
        fun emptyText(): String = "没有已归档的习惯。"

        /** Restore is offered only when there is something to restore. */
        fun showsList(state: ProfileUiState): Boolean = !state.isLoading && state.archived.isNotEmpty()
    }
}