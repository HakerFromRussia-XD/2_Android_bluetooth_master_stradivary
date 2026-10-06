package com.bailout.stickk.ubi4.versions.v3.data.firmware

import com.bailout.stickk.ubi4.data.network.RemoteFirmwareFile
import com.bailout.stickk.ubi4.data.network.SharedFile
import com.bailout.stickk.ubi4.data.network.YandexDiskFirmwareRepository
import com.bailout.stickk.ubi4.data.network.sharedFile
import com.bailout.stickk.ubi4.firmware.FirmwareBoardFamily
import com.bailout.stickk.ubi4.versions.v3.di.createServiceFirmwareForBoardUseCaseV3
import com.bailout.stickk.ubi4.versions.v3.domain.firmware.DownloadServiceFirmwareFilesUseCaseV3
import com.bailout.stickk.ubi4.versions.v3.domain.firmware.LoadServiceFirmwareCatalogUseCaseV3
import com.bailout.stickk.ubi4.versions.v3.domain.firmware.V3ServiceFirmwareFile
import com.bailout.stickk.ubi4.versions.v3.domain.firmware.V3ServiceFirmwareLocalFile
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.confirmVerified
import io.mockk.mockk
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class V3ServiceFirmwareCatalogTest {
    private val forBoard = createServiceFirmwareForBoardUseCaseV3()

    @Test fun `loading maps every remote field and preserves family order and duplicates`() = runBlocking {
        val remote = mockk<YandexDiskFirmwareRepository>()
        val files = mutableListOf(
            RemoteFirmwareFile(FirmwareBoardFamily.GUI, "GUI_v0.4.6.ZIP", "disk:/GUI/current.ZIP", -1),
            RemoteFirmwareFile(FirmwareBoardFamily.BLDC, "DRV_v0.6.13_25.zip", "disk:/BLDC_Driver/file.zip", 123),
            RemoteFirmwareFile(FirmwareBoardFamily.FAM, "FAM_v1.0.zip", "disk:/FAM/file.zip", 0),
            RemoteFirmwareFile(FirmwareBoardFamily.EMG, "EMG_v1.0.zip", "disk:/EMG/file.zip", 4),
        ).apply { add(first().copy(path = "disk:/GUI/duplicate.ZIP", size = 7)) }
        coEvery { remote.loadCatalog() } returns files
        var cacheReads = 0
        val repository = V3ServiceFirmwareCatalogRepositoryImpl(
            cacheDirectory = { cacheReads++; "/unused-cache" }, remote = remote,
        )
        val result = LoadServiceFirmwareCatalogUseCaseV3(repository)()
        val expected = listOf(
            V3ServiceFirmwareFile("GUI", "GUI_v0.4.6.ZIP", "disk:/GUI/current.ZIP", -1),
            V3ServiceFirmwareFile("BLDC", "DRV_v0.6.13_25.zip", "disk:/BLDC_Driver/file.zip", 123),
            V3ServiceFirmwareFile("FAM", "FAM_v1.0.zip", "disk:/FAM/file.zip", 0),
            V3ServiceFirmwareFile("EMG", "EMG_v1.0.zip", "disk:/EMG/file.zip", 4),
            V3ServiceFirmwareFile("GUI", "GUI_v0.4.6.ZIP", "disk:/GUI/duplicate.ZIP", 7),
        )
        files.clear()
        assertEquals(expected, result)
        assertEquals(0, cacheReads)
        coVerify(exactly = 1) { remote.loadCatalog() }
        confirmVerified(remote)
    }

    @Test fun `loading forwards errors and cancellation unchanged without adding retries`() = runBlocking {
        for (failure in listOf(IOException("catalog unavailable"), CancellationException("view detached"))) {
            val remote = mockk<YandexDiskFirmwareRepository>()
            coEvery { remote.loadCatalog() } throws failure
            val load = LoadServiceFirmwareCatalogUseCaseV3(V3ServiceFirmwareCatalogRepositoryImpl(
                cacheDirectory = { error("Catalog loading must not resolve cache") }, remote = remote,
            ))
            assertSame(failure, runCatching { load() }.exceptionOrNull())
            coVerify(exactly = 1) { remote.loadCatalog() }
            confirmVerified(remote)
        }
    }

    @Test fun `downloads all candidates sequentially with exact metadata original names and returned paths`() = runTest {
        val remote = mockk<YandexDiskFirmwareRepository>()
        val candidates = listOf(
            V3ServiceFirmwareFile("BLDC", "DRV_v0.6.13_25.ZIP", "disk:/BLDC_Driver/driver latest.ZIP", 123),
            V3ServiceFirmwareFile("GUI", "catalog/GUI_v0.4.6.zip", "disk:/GUI/source.zip", -1),
            V3ServiceFirmwareFile("FAM", "FAM_v1.0.zip", "disk:/FAM/source.zip", 0),
        ).let { it + it.first() }
        val expectedRemote = listOf(
            RemoteFirmwareFile(FirmwareBoardFamily.BLDC, "DRV_v0.6.13_25.ZIP", "disk:/BLDC_Driver/driver latest.ZIP", 123),
            RemoteFirmwareFile(FirmwareBoardFamily.GUI, "catalog/GUI_v0.4.6.zip", "disk:/GUI/source.zip", -1),
            RemoteFirmwareFile(FirmwareBoardFamily.FAM, "FAM_v1.0.zip", "disk:/FAM/source.zip", 0),
        ).let { it + it.first() }
        val cachePath = "/tmp/service firmware cache"
        val expectedPaths = candidates.indices.map { "$cachePath/yandex_firmware/downloaded-$it.zip" }
        var cacheReads = 0
        val requested = mutableListOf<RemoteFirmwareFile>()
        val directories = mutableListOf<String>()
        val releaseFirst = CompletableDeferred<Unit>()
        coEvery { remote.download(any(), any()) } coAnswers {
            val index = requested.size
            requested += firstArg<RemoteFirmwareFile>()
            directories += secondArg<SharedFile>().path
            if (index == 0) releaseFirst.await()
            sharedFile(expectedPaths[index])
        }
        val download = DownloadServiceFirmwareFilesUseCaseV3(V3ServiceFirmwareCatalogRepositoryImpl(
            cacheDirectory = { cacheReads++; cachePath }, remote = remote,
        ))

        val result = async { download(candidates) }
        runCurrent()

        assertEquals(listOf(expectedRemote.first()), requested)
        assertFalse(result.isCompleted)
        assertEquals(1, cacheReads)
        releaseFirst.complete(Unit)
        assertEquals(candidates.mapIndexed { index, file ->
            V3ServiceFirmwareLocalFile(file.name, expectedPaths[index])
        }, result.await())
        assertEquals(expectedRemote, requested)
        assertEquals(List(candidates.size) { cachePath }, directories)
        assertEquals(1, cacheReads)
        coVerify(exactly = candidates.size) { remote.download(any(), any()) }
        confirmVerified(remote)
    }

    @Test fun `download failure and cancellation stop remaining candidates without retries or partial results`() = runBlocking {
        val candidates = listOf(file("GUI", "first.zip"), file("BLDC", "second.zip"), file("FAM", "third.zip"))
        for (failure in listOf(IOException("download unavailable"), CancellationException("view detached"))) {
            val remote = mockk<YandexDiskFirmwareRepository>()
            val requested = mutableListOf<String>()
            coEvery { remote.download(any(), any()) } coAnswers {
                val name = firstArg<RemoteFirmwareFile>().name
                requested += name
                if (name == "second.zip") throw failure
                sharedFile("/cache/downloaded-$name")
            }
            val download = DownloadServiceFirmwareFilesUseCaseV3(V3ServiceFirmwareCatalogRepositoryImpl(
                cacheDirectory = { "/cache" }, remote = remote,
            ))

            val result = runCatching { download(candidates) }

            assertSame(failure, result.exceptionOrNull())
            assertNull(result.getOrNull())
            assertEquals(listOf("first.zip", "second.zip"), requested)
            coVerify(exactly = 2) { remote.download(any(), any()) }
            confirmVerified(remote)
        }
    }

    @Test fun `cancelling a suspended download never starts the next candidate or returns partial files`() = runTest {
        val remote = mockk<YandexDiskFirmwareRepository>()
        val releaseFirst = CompletableDeferred<Unit>()
        coEvery { remote.download(any(), any()) } coAnswers {
            releaseFirst.await()
            sharedFile("/cache/first.zip")
        }
        val download = DownloadServiceFirmwareFilesUseCaseV3(V3ServiceFirmwareCatalogRepositoryImpl(
            cacheDirectory = { "/cache" }, remote = remote,
        ))
        val result = async { download(listOf(file("GUI", "first.zip"), file("GUI", "second.zip"))) }
        runCurrent()
        assertFalse(result.isCompleted)

        result.cancel(CancellationException("view detached"))
        releaseFirst.complete(Unit)
        val outcome = runCatching { result.await() }

        assertTrue(outcome.exceptionOrNull() is CancellationException)
        assertNull(outcome.getOrNull())
        coVerify(exactly = 1) { remote.download(any(), any()) }
        confirmVerified(remote)
    }

    @Test fun `selection keeps newest versions first case insensitive names and stable ties before invalid versions`() {
        val high = file("FAM", "FW_v10.0.ZIP")
        val lowerCaseTie = file("FAM", "a_v2.0.zip", "first tie")
        val upperCaseTie = file("FAM", "A_v2.0.zip", "second tie")
        val equalVersion = file("FAM", "Z_v2.0.0.zip")
        val low = file("FAM", "FW_v1.9.zip")
        val invalidFirst = file("FAM", "A_unversioned.zip")
        val invalidLast = file("FAM", "b_unversioned.zip")
        val catalog = listOf(
            high, invalidLast, lowerCaseTie, equalVersion, upperCaseTie, low, invalidFirst,
            file("FAM", "FW_v99.0.bin"), file("GUI", "other_v99.0.zip"),
        )
        val original = catalog.toList()

        val selected = forBoard(catalog, 0x00)

        assertEquals(listOf(high, lowerCaseTie, upperCaseTie, equalVersion, low, invalidFirst, invalidLast), selected)
        assertTrue(forBoard.hasUpdate(catalog, 0x00, "1.9"))
        assertEquals(original, catalog)
    }

    @Test fun `BLDC selection and update ignore other addresses other families and missing address suffixes`() {
        val old = file("BLDC", "DRV_v0.6.12_25.zip")
        val newest = file("BLDC", "DRV_v0.6.13.25.zip")
        val catalog = listOf(
            file("BLDC", "DRV_v99.0.0_24.zip"), newest,
            file("BLDC", "DRV_v0.6.99.zip"), old,
            file("FAM", "DRV_v99.0.0_25.zip"),
        )

        val selected = forBoard(catalog, 0x25)

        assertEquals(listOf(newest, old), selected)
        assertTrue(forBoard.hasUpdate(catalog, 0x25, "0.6.12"))
        assertFalse(forBoard.hasUpdate(catalog, 0x25, "0.6.13.25"))
    }

    @Test fun `update availability uses only own family and preserves unknown installed version behavior`() {
        val current = file("FAM", "FH_FAM_v1.0.0.zip")
        val catalog = listOf(file("GUI", "GUI_v99.0.0.zip"), current)

        assertEquals(listOf(current), forBoard(catalog, 0x00))
        assertFalse(forBoard.hasUpdate(catalog, 0x00, "1.0.0"))
        assertFalse(forBoard.hasUpdate(catalog, 0x00, "1.0.1"))
        assertTrue(forBoard.hasUpdate(catalog, 0x00, null))
        assertTrue(forBoard.hasUpdate(catalog, 0x00, "—"))
    }

    @Test fun `unknown addresses empty catalogs and invalid versions have no update`() {
        val catalog = listOf(file("FAM", "FAM_v1.0.0.zip"), file("BLDC", "DRV_v1.0.0_25.zip"))
        val unknown = forBoard(catalog, 0x26)
        val empty = forBoard(emptyList(), 0x00)
        val unversioned = file("FAM", "unversioned.zip")

        assertTrue(unknown.isEmpty())
        assertFalse(forBoard.hasUpdate(catalog, 0x26, null))
        assertTrue(empty.isEmpty())
        assertFalse(forBoard.hasUpdate(emptyList(), 0x00, null))
        assertEquals(listOf(unversioned), forBoard(listOf(unversioned), 0x00))
        assertFalse(forBoard.hasUpdate(listOf(unversioned), 0x00, null))
    }

    private fun file(family: String, name: String, path: String = "disk:/$family/$name") =
        V3ServiceFirmwareFile(family, name, path, 1)
}
