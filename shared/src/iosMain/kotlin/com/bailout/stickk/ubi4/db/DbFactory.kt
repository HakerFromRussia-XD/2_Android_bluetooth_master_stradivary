package com.bailout.stickk.ubi4.db
import app.cash.sqldelight.driver.native.NativeSqliteDriver


fun createDb(): Ubi4Db {
    val driver = NativeSqliteDriver(Ubi4Db.Schema, "ubi4.db")
    return Ubi4Db(driver)
}