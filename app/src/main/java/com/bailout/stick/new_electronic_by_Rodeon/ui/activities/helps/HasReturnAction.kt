package com.bailout.stick.new_electronic_by_Rodeon.ui.activities.helps

interface HasReturnAction {

    /**
     * @return a custom action specification, see [ReturnAction] class for details
     */
    fun getReturnAction(): ReturnAction

}

class ReturnAction(
    val onReturnAction: Runnable
)