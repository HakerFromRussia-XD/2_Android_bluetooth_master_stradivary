package com.bailout.stickk.ubi4.versions.v3.domain.accountprofile

import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class GetAccountBoardsUseCaseV3(
    private val repository: V3AccountBoardsRepository,
    private val isZeroVersion: (String?) -> Boolean,
) {
    fun cached() = repository.getCachedBoards()

    fun current(missingVersion: String): List<V3AccountBoard> = repository.getBoards()
        .map { it.copy(version = it.version?.takeIf(String::isNotBlank) ?: missingVersion) }
        .distinctBy { it.deviceAddress }.sortedBy { it.deviceAddress }

    fun restore(cached: List<V3AccountBoard>) = cached
        .filterNot { isZeroVersion(it.version) }.map { it.copy(isUpdateAvailable = false) }

    operator fun invoke(previous: List<V3AccountBoard>): V3AccountBoardsSnapshot {
        val source = repository.getBoards()
        val versions = source.distinctBy { it.deviceAddress }
            .associate { it.deviceAddress to (it.version?.takeIf(String::isNotBlank) ?: "—") }
        if (source.isEmpty() && previous.isNotEmpty()) return V3AccountBoardsSnapshot(null, versions)
        val boards = source.map { board ->
            board.copy(version = board.version?.takeIf(String::isNotBlank) ?: "—",
                isInBootloader = previous.firstOrNull { it.deviceAddress == board.deviceAddress }?.isInBootloader ?: false)
        }.distinctBy { it.deviceAddress }.sortedBy { it.deviceAddress }
            .filter { !isZeroVersion(it.version) && it.name != null && !it.name.equals("Unknown", ignoreCase = true) }
        return V3AccountBoardsSnapshot(boards, versions)
    }
}

class CacheAccountBoardsUseCaseV3(private val repository: V3AccountBoardsRepository) {
    operator fun invoke(boards: List<V3AccountBoard>) = repository.cacheBoards(boards)
}

class ObserveAccountBoardsUseCaseV3(private val repository: V3AccountBoardsRepository) {
    private val callbackScope by lazy { MainScope() }

    fun changes() = repository.changes
    fun bootloaderChanges() = repository.bootloaderChanges

    fun observeBootloaderChanges(callback: (Int, Boolean) -> Unit): Job = callbackScope.launch {
        repository.bootloaderChanges.collect { (address, isInBootloader) -> callback(address, isInBootloader) }
    }
}
