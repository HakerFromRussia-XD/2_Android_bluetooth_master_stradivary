package com.bailout.stickk.ubi4.bootstrap

import com.bailout.stickk.ubi4.data.local.db.DbProvider
import com.bailout.stickk.ubi4.data.local.db.RoomInit
import com.bailout.stickk.ubi4.data.local.repository.SettingsProfileRepositoryProvider
import com.bailout.stickk.ubi4.data.local.repository.AchievementCelebrationRepositoryProvider
import com.bailout.stickk.ubi4.data.local.repository.AchievementEventRepositoryProvider
import com.bailout.stickk.ubi4.data.local.repository.WidgetRepoProvider

object SharedBootstrapper {

    fun initialize() {
        // 1) создаём и сохраняем инстанс БД
        DbProvider.setInstance(RoomInit.init())

        // 2) создаём репозиторий на DAO
        val db = DbProvider.instance()
        WidgetRepoProvider.init(
            dataParameterDao = db.widgetStateDao(),
            parameterInfoDao = db.baseParameterInfoDao(),
            listWidgetsDao = db.listWidgetsDao(),
            subDeviceDao = db.baseSubDeviceInfoDao(),
            deviceCrcDao = db.deviceCrcDao(),
        )
        SettingsProfileRepositoryProvider.init(db.settingsProfileDao())
        AchievementEventRepositoryProvider.init(db.achievementUniqueEventDao())
        AchievementCelebrationRepositoryProvider.init(db.achievementCelebrationStateDao())
    }
}
