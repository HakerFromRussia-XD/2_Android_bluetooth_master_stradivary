package com.bailout.stickk.new_electronic_by_Rodeon.models


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

                                     val openStageDelay1: Int,
                                     val openStageDelay2: Int,
                                     val openStageDelay3: Int,
                                     val openStageDelay4: Int,
                                     val openStageDelay5: Int,
                                     val openStageDelay6: Int,

                                     val closeStageDelay1: Int,
                                     val closeStageDelay2: Int,
                                     val closeStageDelay3: Int,
                                     val closeStageDelay4: Int,
                                     val closeStageDelay5: Int,
                                     val closeStageDelay6: Int,

                                     val state: Int,

                                     val withChangeGesture: Boolean,
                                     val onlyNumberGesture: Boolean)