package com.bailout.stickk.ubi4.data.local

import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUBI4.GestureEnum
import com.bailout.stickk.ubi4.resources.ResourceDrawable
import com.bailout.stickk.ubi4.resources.ResourceProvider
import com.bailout.stickk.ubi4.resources.ResourceString
import com.bailout.stickk.ubi4.utility.logging.platformLog

class CollectionGesturesProvider(private val resourceProvider: ResourceProvider) {
    fun getCollectionGestures(): ArrayList<Gesture> {
        val collectionGesturesList: ArrayList<Gesture> = ArrayList()
        val fistStr = resourceProvider.getString(ResourceString.FIST)
        val fistDrawable = resourceProvider.getDrawable(ResourceDrawable.COLLECTION_FIST_1)
        collectionGesturesList.add(Gesture(GestureEnum.GESTURE_FIST.number, gestureName = resourceProvider.getString(ResourceString.FIST), gestureImage = resourceProvider.getDrawable(ResourceDrawable.COLLECTION_FIST_1)))
        platformLog("CollectionGesturesProvider", "FIST: $fistStr, drawable: $fistDrawable")
        collectionGesturesList.add(Gesture(GestureEnum.GESTURE_POINT.number, gestureName = resourceProvider.getString(ResourceString.GESTURE_POINT), gestureImage = resourceProvider.getDrawable(ResourceDrawable.COLLECTION_POINT)))
        collectionGesturesList.add(Gesture(GestureEnum.GESTURE_PINCH.number, gestureName = resourceProvider.getString(ResourceString.GESTURE_PINCH), gestureImage = resourceProvider.getDrawable(ResourceDrawable.COLLECTION_PINCH)))
        collectionGesturesList.add(Gesture(GestureEnum.GESTURE_FIST_THUMB_OVER.number, gestureName = resourceProvider.getString(ResourceString.GESTURE_FIST_THUMB_OVER), gestureImage = resourceProvider.getDrawable(ResourceDrawable.COLLECTION_FIST_2)))
        collectionGesturesList.add(Gesture(GestureEnum.GESTURE_KEY.number, gestureName = resourceProvider.getString(ResourceString.GESTURE_KEY), gestureImage = resourceProvider.getDrawable(ResourceDrawable.COLLECTION_KEY)))
        collectionGesturesList.add(Gesture(GestureEnum.GESTURE_ROCK.number, gestureName = resourceProvider.getString(ResourceString.GESTURE_ROCK), gestureImage = resourceProvider.getDrawable(ResourceDrawable.COLLECTION_ROCK)))
        collectionGesturesList.add(Gesture(GestureEnum.GESTURE_TWIZZERS.number, gestureName = resourceProvider.getString(ResourceString.GESTURE_TWIZZERS), gestureImage = resourceProvider.getDrawable(ResourceDrawable.COLLECTION_TWIZZERS)))
        collectionGesturesList.add(Gesture(GestureEnum.GESTURE_CUPHOLDER.number, gestureName = resourceProvider.getString(ResourceString.GESTURE_CUPHOLDER), gestureImage = resourceProvider.getDrawable(ResourceDrawable.COLLECTION_CUPHOLDER)))
        collectionGesturesList.add(Gesture(GestureEnum.GESTURE_HALF_GRAB.number, gestureName = resourceProvider.getString(ResourceString.GESTURE_HALF_GRAB), gestureImage = resourceProvider.getDrawable(ResourceDrawable.COLLECT_HALF_GRAB)))
        collectionGesturesList.add(Gesture(GestureEnum.GESTURE_OK.number, gestureName = resourceProvider.getString(ResourceString.GESTURE_OK), gestureImage = resourceProvider.getDrawable(ResourceDrawable.COLLECTION_OK)))
        collectionGesturesList.add(Gesture(GestureEnum.GESTURE_THUMB_UP.number, gestureName = resourceProvider.getString(ResourceString.GESTURE_THUMB_UP), gestureImage = resourceProvider.getDrawable(ResourceDrawable.COLLECTION_THUMB_UP)))
        collectionGesturesList.add(Gesture(GestureEnum.GESTURE_MIDDLE_FINGER.number, gestureName = resourceProvider.getString(ResourceString.GESTURE_MIDDLE_FINGER), gestureImage = resourceProvider.getDrawable(ResourceDrawable.COLLECTION_MIDDLE_FINGER)))
        collectionGesturesList.add(Gesture(GestureEnum.GESTURE_DOUBLE_POINT.number, gestureName = resourceProvider.getString(ResourceString.GESTURE_DOUBLE_POINT), gestureImage = resourceProvider.getDrawable(ResourceDrawable.COLLECTION_DOUBLE_POINT)))
        collectionGesturesList.add(Gesture(GestureEnum.GESTURE_CALL_ME.number, gestureName = resourceProvider.getString(ResourceString.GESTURE_CALL_ME), gestureImage = resourceProvider.getDrawable(ResourceDrawable.COLLECTION_CALL_ME)))
        collectionGesturesList.add(Gesture(GestureEnum.GESTURE_NATURAL_POSITION.number, gestureName = resourceProvider.getString(ResourceString.GESTURE_NATURAL_POSITION), gestureImage = resourceProvider.getDrawable(ResourceDrawable.COLLECTION_NATURAL_POSITION)))
        collectionGesturesList.add(Gesture(GestureEnum.GESTURE_CUSTOM_0.number, gestureName = resourceProvider.getString(ResourceString.GESTURE_1_BTN)))
        collectionGesturesList.add(Gesture(GestureEnum.GESTURE_CUSTOM_1.number, gestureName = resourceProvider.getString(ResourceString.GESTURE_2_BTN)))
        collectionGesturesList.add(Gesture(GestureEnum.GESTURE_CUSTOM_2.number, gestureName = resourceProvider.getString(ResourceString.GESTURE_3_BTN)))
        collectionGesturesList.add(Gesture(GestureEnum.GESTURE_CUSTOM_3.number, gestureName = resourceProvider.getString(ResourceString.GESTURE_4_BTN)))
        collectionGesturesList.add(Gesture(GestureEnum.GESTURE_CUSTOM_4.number, gestureName = resourceProvider.getString(ResourceString.GESTURE_5_BTN)))
        collectionGesturesList.add(Gesture(GestureEnum.GESTURE_CUSTOM_5.number, gestureName = resourceProvider.getString(ResourceString.GESTURE_6_BTN)))
        collectionGesturesList.add(Gesture(GestureEnum.GESTURE_CUSTOM_6.number, gestureName = resourceProvider.getString(ResourceString.GESTURE_7_BTN)))
        collectionGesturesList.add(Gesture(GestureEnum.GESTURE_CUSTOM_7.number, gestureName = resourceProvider.getString(ResourceString.GESTURE_8_BTN)))
        collectionGesturesList.add(Gesture(GestureEnum.GESTURE_CUSTOM_8.number, gestureName = resourceProvider.getString(ResourceString.GESTURE_9_BTN)))
        collectionGesturesList.add(Gesture(GestureEnum.GESTURE_CUSTOM_9.number, gestureName = resourceProvider.getString(ResourceString.GESTURE_10_BTN)))
        collectionGesturesList.add(Gesture(GestureEnum.GESTURE_CUSTOM_10.number, gestureName = resourceProvider.getString(ResourceString.GESTURE_11_BTN)))
        collectionGesturesList.add(Gesture(GestureEnum.GESTURE_CUSTOM_11.number, gestureName = resourceProvider.getString(ResourceString.GESTURE_12_BTN)))
        collectionGesturesList.add(Gesture(GestureEnum.GESTURE_CUSTOM_12.number, gestureName = resourceProvider.getString(ResourceString.GESTURE_13_BTN)))
        collectionGesturesList.add(Gesture(GestureEnum.GESTURE_CUSTOM_13.number, gestureName = resourceProvider.getString(ResourceString.GESTURE_14_BTN)))
        return collectionGesturesList
    }

    fun getGesture(gestureId: Int): Gesture {
        val gestureList = getCollectionGestures().filter { it.gestureId == gestureId }
        return if (gestureList.isNotEmpty()) gestureList[0] else Gesture(0)
    }
}