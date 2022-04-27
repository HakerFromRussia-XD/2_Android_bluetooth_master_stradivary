package me.start.motorica.new_electronic_by_Rodeon.models


data class GestureStateWithEncoders (val gestureNumber: Int,

                                     val openStage1: Int,
                                     val openStage2: Int,
                                     val openStage3: Int,
                                     val openStage4: Int,
                                     val openStage5: Int,
                                     val openStage6: Int,

                                     val closeStage1: Int,
                                     val closeStage2: Int,
                                     val closeStage3: Int,
                                     val closeStage4: Int,
                                     val closeStage5: Int,
                                     val closeStage6: Int,

                                     val state: Int,

                                     val withChangeGesture: Boolean)