package com.bailout.stickk.ubi4.db

import android.content.Context
import app.cash.sqldelight.driver.android.AndroidSqliteDriver

fun createDb(context: Context): Ubi4Db {
    val driver = AndroidSqliteDriver(Ubi4Db.Schema, context, "ubi4.db")
    return Ubi4Db(driver)
}