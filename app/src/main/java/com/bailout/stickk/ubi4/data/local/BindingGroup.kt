package com.bailout.stickk.ubi4.data.local

import kotlinx.serialization.Serializable


@Serializable(with = RotationGroupSerializer::class)
data class BindingGestureGroup (
    var gesture1Id: Int = 0, var gesture1ImageId: Int = 0,
    var gesture2Id: Int = 0, var gesture2ImageId: Int = 0,
    var gesture3Id: Int = 0, var gesture3ImageId: Int = 0,
    var gesture4Id: Int = 0, var gesture4ImageId: Int = 0,
    var gesture5Id: Int = 0, var gesture5ImageId: Int = 0,
    var gesture6Id: Int = 0, var gesture6ImageId: Int = 0,
    var gesture7Id: Int = 0, var gesture7ImageId: Int = 0,
    var gesture8Id: Int = 0, var gesture8ImageId: Int = 0,
    var gesture9Id: Int = 0, var gesture9ImageId: Int = 0,
    var gesture10Id: Int = 0, var gesture10ImageId: Int = 0,
    var gesture11Id: Int = 0, var gesture11ImageId: Int = 0,
    var gesture12Id: Int = 0, var gesture12ImageId: Int = 0,
    var gesture13Id: Int = 0, var gesture13ImageId: Int = 0,
) {
    fun toGestureList(): List<Pair<Int, Int>> {
        return listOf(
            gesture1Id to gesture1ImageId,
            gesture2Id to gesture2ImageId,
            gesture3Id to gesture3ImageId,
            gesture4Id to gesture4ImageId,
            gesture5Id to gesture5ImageId,
            gesture6Id to gesture6ImageId,
            gesture7Id to gesture7ImageId,
            gesture8Id to gesture8ImageId,
            gesture9Id to gesture9ImageId,
            gesture10Id to gesture10ImageId,
            gesture11Id to gesture11ImageId,
            gesture12Id to gesture12ImageId,
            gesture13Id to gesture13ImageId,


            )
    }
}
