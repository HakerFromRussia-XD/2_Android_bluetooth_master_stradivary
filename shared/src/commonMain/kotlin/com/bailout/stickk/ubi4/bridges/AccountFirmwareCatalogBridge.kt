package com.bailout.stickk.ubi4.resources.com.bailout.stickk.ubi4.bridges

import com.bailout.stickk.ubi4.data.network.RemoteFirmwareFile
import com.bailout.stickk.ubi4.data.network.FirmwareRepositoryProvider
import com.bailout.stickk.ubi4.data.network.sharedFile
import com.bailout.stickk.ubi4.firmware.FirmwareBoardFamily
import com.bailout.stickk.ubi4.firmware.FirmwareCompatibility
import com.bailout.stickk.ubi4.firmware.FirmwareVersionCatalog
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

object AccountFirmwareCatalogPolicy {
    fun filesForBoard(address: Int, catalog: List<RemoteFirmwareFile>): List<RemoteFirmwareFile> =
        catalog.filter { it.family == FirmwareBoardFamily.fromDeviceAddress(address) }
            .filter { FirmwareCompatibility.isCompatible(address, it.name) }
            .sortedWith { left, right ->
                when {
                    FirmwareVersionCatalog.isLocalVersionNewer(right.version, left.version) -> -1
                    FirmwareVersionCatalog.isLocalVersionNewer(left.version, right.version) -> 1
                    else -> left.name.compareTo(right.name, ignoreCase = true)
                }
            }

    fun showBoard(name: String, version: String): Boolean =
        !name.equals("Unknown", ignoreCase = true) && !FirmwareVersionCatalog.isZeroVersion(version)
}

class AccountFirmwareCatalogBridge {
    private val scope = MainScope()
    private val repository = FirmwareRepositoryProvider.active()
    private var catalog: List<RemoteFirmwareFile>? = null

    /** Load only catalog metadata; archives are downloaded after the user selects one. */
    fun refresh(callback: (Boolean) -> Unit): Job {
        catalog = null
        return scope.launch {
            val loaded = try {
                catalog = repository.loadCatalog()
                true
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                false
            }
            callback(loaded)
        }
    }

    fun filesForBoard(address: Int): List<RemoteFirmwareFile> =
        AccountFirmwareCatalogPolicy.filesForBoard(address, catalog.orEmpty())

    fun isLoaded(): Boolean = catalog != null

    fun isUpdateAvailable(address: Int, version: String): Boolean =
        catalog != null && FirmwareCompatibility.isUpdateAvailableFromVersions(
            address, version, filesForBoard(address).map { it.version }
        )

    /** Download exactly the selected archive into the repository's cache subdirectory. */
    fun download(file: RemoteFirmwareFile, cachePath: String, callback: (String?) -> Unit): Job =
        scope.launch {
            val path = try {
                repository.download(file, sharedFile(cachePath)).path
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                null
            }
            callback(path)
        }
}
