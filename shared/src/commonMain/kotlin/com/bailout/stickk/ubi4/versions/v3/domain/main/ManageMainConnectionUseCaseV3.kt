package com.bailout.stickk.ubi4.versions.v3.domain.main

class ManageMainConnectionUseCaseV3(private val repository: V3MainRepository) {
    fun rememberLastConnection() = repository.rememberLastConnection()
    fun restoreSavedConnection() = repository.restoreSavedConnection()
    fun resetLastConnection() = repository.resetLastConnection()
    fun initializeWidgetStorage() = repository.initializeWidgetStorage()
}
