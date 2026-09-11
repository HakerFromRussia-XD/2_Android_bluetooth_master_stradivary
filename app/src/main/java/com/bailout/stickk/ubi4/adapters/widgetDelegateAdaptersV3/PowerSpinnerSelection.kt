package com.bailout.stickk.ubi4.adapters.widgetDelegateAdaptersV3

import com.skydoves.powerspinner.PowerSpinnerInterface

/** PowerSpinner 1.2.7 invokes its selection listener synchronously, including programmatic selection. */
internal fun PowerSpinnerInterface<CharSequence>.selectItemWithoutCallback(index: Int) {
    val listener = onSpinnerItemSelectedListener
    onSpinnerItemSelectedListener = null
    try {
        notifyItemSelected(index)
    } finally {
        onSpinnerItemSelectedListener = listener
    }
}
