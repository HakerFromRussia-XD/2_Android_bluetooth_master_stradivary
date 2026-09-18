package com.bailout.stickk.ubi4.versions.v3.domain.appsettings

/** The existing direct-name and collection-name settings have different missing-value defaults. */
data class V3CustomGestureNames(
    val names: List<String> = emptyList(),
    // Null means the collection uses its localized default; an empty saved string stays empty.
    val collectionNames: List<String?> = emptyList(),
)
