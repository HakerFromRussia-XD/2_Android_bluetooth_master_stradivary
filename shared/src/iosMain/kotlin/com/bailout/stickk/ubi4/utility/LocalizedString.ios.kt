package com.bailout.stickk.ubi4.utility

import dev.icerock.moko.resources.StringResource
import dev.icerock.moko.resources.desc.desc

actual fun localizedString(resource: StringResource): String =
    resource.desc().localized()
