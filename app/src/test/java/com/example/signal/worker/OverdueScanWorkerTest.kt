package com.example.signal.worker

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.work.ListenableWorker
import androidx.work.WorkerParameters
import androidx.work.testing.TestListenableWorkerBuilder
import com.example.signal.data.repository.TaskRepository
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class OverdueScanWorkerTest {

    private lateinit var context: Context
    private val repository = mockk<TaskRepository>(relaxed = true)

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun worker_marksOverdueTasks_andReturnsSuccess() = runTest {
        coEvery { repository.getOverdueCount() } returns 2

        val worker = TestListenableWorkerBuilder<OverdueScanWorker>(context)
            .setWorkerFactory(object : androidx.work.WorkerFactory() {
                override fun createWorker(
                    appContext: Context,
                    workerClassName: String,
                    workerParameters: WorkerParameters
                ): ListenableWorker {
                    return OverdueScanWorker(appContext, workerParameters, repository)
                }
            })
            .build()

        val result = worker.doWork()
        
        coVerify { repository.markOverdueTasks() }
        assertThat(result).isEqualTo(ListenableWorker.Result.success())
    }
}
