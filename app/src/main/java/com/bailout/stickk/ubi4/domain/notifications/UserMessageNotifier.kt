package com.bailout.stickk.ubi4.domain.notifications

enum class UserMessageDuration {
    SHORT,
    LONG,
}

fun interface UserMessageNotifier {
    fun show(message: String, duration: UserMessageDuration)
}
