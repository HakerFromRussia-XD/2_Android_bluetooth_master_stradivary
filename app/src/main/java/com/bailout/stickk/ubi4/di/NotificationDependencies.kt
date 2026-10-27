package com.bailout.stickk.ubi4.di

import android.content.Context
import android.widget.Toast
import com.bailout.stickk.ubi4.domain.notifications.UserMessageDuration
import com.bailout.stickk.ubi4.domain.notifications.UserMessageNotifier
import com.bailout.stickk.ubi4.ui.notifications.AppToast

object NotificationDependencies {
    fun userMessages(context: Context): UserMessageNotifier {
        val appContext = context.applicationContext
        return UserMessageNotifier { message, duration ->
            AppToast.show(
                appContext,
                message,
                when (duration) {
                    UserMessageDuration.SHORT -> Toast.LENGTH_SHORT
                    UserMessageDuration.LONG -> Toast.LENGTH_LONG
                },
            )
        }
    }
}
