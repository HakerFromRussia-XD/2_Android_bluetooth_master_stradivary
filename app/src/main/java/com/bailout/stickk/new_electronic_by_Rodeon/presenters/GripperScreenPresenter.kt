package com.bailout.stickk.new_electronic_by_Rodeon.presenters

import android.content.Context
import android.os.Bundle
import com.bailout.stickk.new_electronic_by_Rodeon.WDApplication
import com.bailout.stickk.new_electronic_by_Rodeon.compose.BasePresenter
import com.bailout.stickk.new_electronic_by_Rodeon.persistence.preference.PreferenceManager
import com.bailout.stickk.new_electronic_by_Rodeon.persistence.sqlite.SqliteManager
import com.bailout.stickk.new_electronic_by_Rodeon.viewTypes.GripperScreenActivityView
import javax.inject.Inject

class GripperScreenPresenter : BasePresenter<GripperScreenActivityView>()  {

    @Inject
    lateinit var sqliteManager: SqliteManager
    @Inject
    lateinit var preferenceManager: PreferenceManager

    override fun onCreate(context: Context, savedInstanceState: Bundle?) {
        super.onCreate(context, savedInstanceState)
        WDApplication.component.inject(this)
    }

}