package com.bailout.stickk.ubi4.utility

import com.bailout.stickk.ubi4.AndroidContextProvider
import dev.icerock.moko.resources.StringResource

actual fun localizedString(resource: StringResource): String =
    AndroidContextProvider.context.getString(resource.resourceId)
