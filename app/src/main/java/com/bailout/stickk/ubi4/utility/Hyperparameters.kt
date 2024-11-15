package com.bailout.stickk.ubi4.utility

object Hyperparameters {
    const val NUM_CLASSES = 8
    const val NUM_EPOCHS = 100
    const val BATCH_SIZE = 32 * 4
    const val NUM_FEATURES = 48
    const val NUM_TIMESTEPS = 10
    const val WIN_SHIFT = NUM_TIMESTEPS / 2

    // read data
    const val INDEX_START_FEATURES = 2
    const val INDEX_TARGET_STATE = 40
    const val INDEX_TARGET_ID = 41
    const val USE_EMG = true
    const val USE_BNO = false
    const val N_OMG_CH = 16
    const val N_EMG_CH = 8
    const val N_BNO_CH = 3

    // preprocessing
    const val AUTO_SHIFT_RANGE = 0.5
    const val SHIFT_TARGET = true
    const val SCALE_OMG = 1_000_000.0f
    const val SCALE_EMG = 10.0f
    val LP_ALPHAS = floatArrayOf(0.05f)
    const val N_LP_ALPHAS = 1
}

