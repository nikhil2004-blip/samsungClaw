package com.example.signal.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.signal.data.repository.TaskRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * Runs every 15 minutes in the background to sweep past-deadline tasks
 * into MISSED status — even when the main app UI is not open.
 *
 * This covers the case where a user schedules a snooze, the alarm fires
 * and they dismiss the overlay without acting, and the original deadline
 * then passes while the app is backgrounded.
 */
@HiltWorker
class SweepWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val repository: TaskRepository
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        repository.sweepMissedTasks()
        repository.markOverdueTasks()
        return Result.success()
    }
}
