package com.bailout.stickk.ubi4.data.local

import com.bailout.stickk.ubi4.models.dialog.SprGestureItem
import com.bailout.stickk.ubi4.resources.ResourceProvider
import com.bailout.stickk.ubi4.resources.ResourceString
import com.bailout.stickk.ubi4.resources.ResourceRaw

class SprGestureItemsProvider(private val resourceProvider: ResourceProvider) {

    fun getAnimationIdByKeyNameGesture(keyNameGesture: String): Int {
        val sprGestureItem = getSprGestureItemList().find { it.keyNameGesture == keyNameGesture }
        return sprGestureItem?.animationId ?: resourceProvider.getRaw(ResourceRaw.OPEN)
    }

    fun getNameGestureByKeyName(keyNameGesture: String): String {
        val sprGestureItem = getSprGestureItemList().find { it.keyNameGesture == keyNameGesture }
        return sprGestureItem?.title ?: ""
    }

    fun getSprGestureItemList(): ArrayList<SprGestureItem> {
        val sprGestureItemList = listOf(
            SprGestureItem(
                sprGestureId = 1,
                title = resourceProvider.getString(ResourceString.THUMB_FINGER),
                animationId = resourceProvider.getRaw(ResourceRaw.THUMB_FINGERS),
                check = false,
                keyNameGesture = "ThumbFingers"
            ),
            SprGestureItem(
                sprGestureId = 2,
                title = resourceProvider.getString(ResourceString.FLEXION),
                animationId = resourceProvider.getRaw(ResourceRaw.WRIST_FLEX),
                check = false,
                keyNameGesture = "Wrist_Flex"
            ),
            SprGestureItem(
                sprGestureId = 3,
                title = resourceProvider.getString(ResourceString.EXTENSION),
                animationId = resourceProvider.getRaw(ResourceRaw.WRIST_EXTEND),
                check = false,
                keyNameGesture = "Wrist_Extend"
            ),
            SprGestureItem(
                sprGestureId = 4,
                title = resourceProvider.getString(ResourceString.PALM_CLOSING),
                animationId = resourceProvider.getRaw(ResourceRaw.CLOSE),
                check = false,
                keyNameGesture = "Close"
            ),
            SprGestureItem(
                sprGestureId = 5,
                title = resourceProvider.getString(ResourceString.PALM_OPENING),
                animationId = resourceProvider.getRaw(ResourceRaw.OPEN),
                check = false,
                keyNameGesture = "Open"
            ),
            SprGestureItem(
                sprGestureId = 6,
                title = resourceProvider.getString(ResourceString.OK_PINCH),
                animationId = resourceProvider.getRaw(ResourceRaw.PINCH),
                check = false,
                keyNameGesture = "Pinch"
            ),
            SprGestureItem(
                sprGestureId = 7,
                title = resourceProvider.getString(ResourceString.PISTOL_POINTER_GESTURE),
                animationId = resourceProvider.getRaw(ResourceRaw.INDICATION),
                check = false,
                keyNameGesture = "Indication"
            ),
            // TODO: уточнить оставшиеся ID жестов
            SprGestureItem(
                sprGestureId = 8,
                title = resourceProvider.getString(ResourceString.GESTURE_KEY),
                animationId = resourceProvider.getRaw(ResourceRaw.KEY),
                check = false,
                keyNameGesture = "Key"
            ),
            SprGestureItem(
                sprGestureId = 9,
                title = resourceProvider.getString(ResourceString.ADDUCTION),
                animationId = resourceProvider.getRaw(ResourceRaw.ADDUCTION),
                check = false,
                keyNameGesture = "Adduction"
            ),
            SprGestureItem(
                sprGestureId = 10,
                title = resourceProvider.getString(ResourceString.ABDUCTION),
                animationId = resourceProvider.getRaw(ResourceRaw.ABDUCTION),
                check = false,
                keyNameGesture = "Abduction"
            ),
            SprGestureItem(
                sprGestureId = 11,
                title = resourceProvider.getString(ResourceString.PRONATION),
                animationId = resourceProvider.getRaw(ResourceRaw.PRONATION),
                check = false,
                keyNameGesture = "Pronation"
            ),
            SprGestureItem(
                sprGestureId = 12,
                title = resourceProvider.getString(ResourceString.SUPINATION),
                animationId = resourceProvider.getRaw(ResourceRaw.SUPINATION),
                check = false,
                keyNameGesture = "Supination"
            )
        )
        return ArrayList(sprGestureItemList)
    }

    fun getSprGesture(sprGestureId: Int): SprGestureItem {
        val sprGestureList = getSprGestureItemList().filter { it.sprGestureId == sprGestureId }
        return if (sprGestureList.isNotEmpty()) sprGestureList[0] else SprGestureItem()
    }

}