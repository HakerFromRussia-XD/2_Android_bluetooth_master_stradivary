package com.bailout.stickk.ubi4.persistence

object WidgetRepoProvider {
    private var repo: WidgetStateRepository? = null

    fun init(repository: WidgetStateRepository) { repo = repository }

    fun get(): WidgetStateRepository =
        repo ?: error("WidgetRepoProvider is not initialized. Call init(...) on app start.")
}