package com.bailout.stickk.ubi4.data.network


actual suspend fun uploadTrainingDataSsePlatform(
    token: String,
    serial: String,
    pairs: List<Pair<SharedFile, SharedFile>>,
    onProgress: (Int) -> Unit
): String {
    TODO("Not yet implemented")
}