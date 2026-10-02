package com.bailout.stickk.ubi4.versions.v3.domain.appsettings

data class V3GesturesPreferences(
    // Keep the stored value unchanged: UBI4 also uses this preference with section 3.
    val selectedSection: Int = 1,
    val isFactoryCollectionExpanded: Boolean = true,
)
