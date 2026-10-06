package com.bailout.stickk.ubi4.versions.v3.domain.games

class CheckGameInRuStoreUseCaseV3(private val repository: V3GamesRepository) {
    suspend operator fun invoke(packageName: String): Boolean = repository.isPublishedInRuStore(packageName)
}
