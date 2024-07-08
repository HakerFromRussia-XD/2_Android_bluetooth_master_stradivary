package com.bailout.stickk.new_electronic_by_Rodeon.models

import com.google.gson.annotations.SerializedName

data class AllOptions (
    @SerializedName("GAME_LAUNCH_RATE"   )  var gameLaunchRate           : String? = null,
    @SerializedName("MAXIMUM_POINTS"  )  var maximumPoints          : String? = null,
    @SerializedName("NUMBER_OF_CUPS"  )  var numberOfCups         : String? = null,
//    @SerializedName("value_my" )  var valueMy         : Int? = null
)