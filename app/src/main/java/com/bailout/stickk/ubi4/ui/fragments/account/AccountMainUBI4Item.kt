package com.bailout.stickk.ubi4.ui.fragments.account

class AccountMainUBI4Item (
    private val avatarUrl: String,
    private val name: String,
    private val surname: String,
    private val patronymic: String,
    private val versionDriver: String,
    private val versionBms: String,
    private val versionSensors: String,
//    private val versionApp: String,
) {
    fun getAvatarUrl(): String { return avatarUrl }
    fun getName(): String { return name }
    fun getSurname(): String { return surname }
    fun getPatronymic(): String { return patronymic }
    fun getVersionDriver(): String { return versionDriver }
    fun getVersionBms(): String { return versionBms }
    fun getVersionSensors(): String { return versionSensors }
}