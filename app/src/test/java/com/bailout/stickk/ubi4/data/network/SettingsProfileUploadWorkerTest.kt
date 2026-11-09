package com.bailout.stickk.ubi4.data.network

import android.content.Context
import androidx.work.Data
import androidx.work.ListenableWorker
import androidx.work.WorkerParameters
import com.bailout.stickk.ubi4.data.local.repository.SettingsProfileRepository
import com.bailout.stickk.ubi4.data.local.repository.SettingsProfileRepositoryProvider
import io.mockk.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.concurrent.Executor

class SettingsProfileUploadWorkerTest {
    private val context = mockk<Context>(relaxed = true)
    private val parameters = mockk<WorkerParameters>(relaxed = true)

    @BeforeEach fun setUp() {
        every { context.applicationContext } returns context
        every { parameters.backgroundExecutor } returns Executor { it.run() }
        every { parameters.inputData } returns Data.Builder()
            .putString(SettingsProfileUploadWorker.KEY_SERIAL, "FEST-test").build()
        every { parameters.runAttemptCount } returns 0
        mockkObject(SettingsProfileRepositoryProvider)
        every { SettingsProfileRepositoryProvider.getOrNull() } returns mockk<SettingsProfileRepository>()
        mockkConstructor(Ubi4SettingsProfileSender::class)
    }

    @AfterEach fun tearDown() {
        unmockkConstructor(Ubi4SettingsProfileSender::class)
        unmockkObject(SettingsProfileRepositoryProvider)
    }

    @Test fun `temporary failure schedules retry`() = runTest {
        coEvery { anyConstructed<Ubi4SettingsProfileSender>().sendProfile1SettingsForSerial(any(), any()) } throws
            SettingsProfileRequestException("user info", NetworkResult.Error(401, "unauthorized"))
        assertEquals(ListenableWorker.Result.retry(), SettingsProfileUploadWorker(context, parameters).doWork())
    }

    @Test fun `engineer job uses baseline only publication instead of replacing all profiles`() = runTest {
        every { parameters.inputData } returns Data.Builder()
            .putString(SettingsProfileUploadWorker.KEY_SERIAL, "FEST-test")
            .putBoolean(SettingsProfileUploadWorker.KEY_ENGINEER_UPLOAD, true).build()
        coEvery { anyConstructed<Ubi4SettingsProfileSender>().sendEngineerSettingsForSerial(any(), any()) } returns
            SettingsProfileUploadResult("9", "{}", "saved")
        assertEquals(ListenableWorker.Result.success(), SettingsProfileUploadWorker(context, parameters).doWork())
        coVerify(exactly = 0) { anyConstructed<Ubi4SettingsProfileSender>().sendProfile1SettingsForSerial(any(), any()) }
    }

    @Test fun `fifth failed attempt stops retrying`() = runTest {
        every { parameters.runAttemptCount } returns 4
        coEvery { anyConstructed<Ubi4SettingsProfileSender>().sendProfile1SettingsForSerial(any(), any()) } throws
            SettingsProfileRequestException("upload", NetworkResult.Error(503, "unavailable"))
        assertEquals(ListenableWorker.Result.failure(), SettingsProfileUploadWorker(context, parameters).doWork())
    }

    @Test fun `forbidden response is not retried`() = runTest {
        coEvery { anyConstructed<Ubi4SettingsProfileSender>().sendProfile1SettingsForSerial(any(), any()) } throws
            SettingsProfileRequestException("upload", NetworkResult.Error(403, "forbidden"))
        assertEquals(ListenableWorker.Result.failure(), SettingsProfileUploadWorker(context, parameters).doWork())
    }

    @Test fun `cancellation is not converted to failure or retry`() = runTest {
        coEvery { anyConstructed<Ubi4SettingsProfileSender>().sendProfile1SettingsForSerial(any(), any()) } throws
            CancellationException("stopped")
        try {
            SettingsProfileUploadWorker(context, parameters).doWork()
            fail("Expected cancellation")
        } catch (_: CancellationException) { }
    }
}
