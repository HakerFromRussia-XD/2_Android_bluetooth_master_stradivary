package com.bailout.stickk.ubi4.data.local

import com.bailout.stickk.R
import com.bailout.stickk.new_electronic_by_Rodeon.WDApplication.Companion.applicationContext
import com.bailout.stickk.ubi4.data.local.Gesture
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUBI4.GestureEnum

class CollectionGesturesProvider() {
    companion object {
        fun getCollectionGestures(): ArrayList<Gesture> {
            val collectionGesturesList: ArrayList<Gesture> = ArrayList()
//            collectionGesturesList.add(Gesture(GestureEnum.GESTURE_NO_GESTURE.number, gestureName = "Нет жеста"))
            collectionGesturesList.add(Gesture(GestureEnum.GESTURE_FIST.number, gestureName = applicationContext().getString(R.string.fist), gestureImage = R.drawable.collection_fist_1))
            collectionGesturesList.add(Gesture(GestureEnum.GESTURE_POINT.number, gestureName = applicationContext().getString(R.string.gesture_point), gestureImage = R.drawable.collection_point))
            collectionGesturesList.add(Gesture(GestureEnum.GESTURE_PINCH.number, gestureName = applicationContext().getString(R.string.gesture_pinch), gestureImage = R.drawable.collection_pinch))
            collectionGesturesList.add(Gesture(GestureEnum.GESTURE_FIST_THUMB_OVER.number, gestureName = applicationContext().getString(R.string.gesture_fist_thumb_over), gestureImage = R.drawable.collection_fist_2))
            collectionGesturesList.add(Gesture(GestureEnum.GESTURE_KEY.number, gestureName = applicationContext().getString(R.string.gesture_key), gestureImage = R.drawable.collection_key))
            collectionGesturesList.add(Gesture(GestureEnum.GESTURE_ROCK.number, gestureName =applicationContext().getString(R.string.gesture_rock), gestureImage = R.drawable.collection_rock))
            collectionGesturesList.add(Gesture(GestureEnum.GESTURE_TWIZZERS.number, gestureName = applicationContext().getString(R.string.gesture_twizzers), gestureImage = R.drawable.collection_twizzers))//от Ок отличается тем что пальцы закрыты
            collectionGesturesList.add(Gesture(GestureEnum.GESTURE_CUPHOLDER.number, gestureName = applicationContext().getString(R.string.gesture_cupholder), gestureImage = R.drawable.collection_cupholder))
            collectionGesturesList.add(Gesture(GestureEnum.GESTURE_HALF_GRAB.number, gestureName = applicationContext().getString(R.string.gesture_half_grab), gestureImage = R.drawable.collect_half_grab))
            collectionGesturesList.add(Gesture(GestureEnum.GESTURE_OK.number, gestureName = applicationContext().getString(R.string.gesture_ok), gestureImage = R.drawable.collection_ok))
            collectionGesturesList.add(Gesture(GestureEnum.GESTURE_THUMB_UP.number, gestureName = applicationContext().getString(R.string.gesture_thumb_up), gestureImage = R.drawable.collection_thumb_up))
            collectionGesturesList.add(Gesture(GestureEnum.GESTURE_MIDDLE_FINGER.number, gestureName = applicationContext().getString(R.string.gesture_middle_finger), gestureImage = R.drawable.collection_middle_finger))
            collectionGesturesList.add(Gesture(GestureEnum.GESTURE_DOUBLE_POINT.number, gestureName = applicationContext().getString(R.string.gesture_double_point), gestureImage = R.drawable.collection_double_point))
            collectionGesturesList.add(Gesture(GestureEnum.GESTURE_CALL_ME.number, gestureName = applicationContext().getString(R.string.gesture_call_me), gestureImage = R.drawable.collection_call_me))
            collectionGesturesList.add(Gesture(GestureEnum.GESTURE_NATURAL_POSITION.number, gestureName = applicationContext().getString(R.string.gesture_natural_position), gestureImage = R.drawable.collection_natural_position))
            collectionGesturesList.add(Gesture(GestureEnum.GESTURE_CUSTOM_0.number, gestureName = applicationContext().getString(R.string.gesture_1_btn)))
            collectionGesturesList.add(Gesture(GestureEnum.GESTURE_CUSTOM_1.number, gestureName = applicationContext().getString(R.string.gesture_2_btn)))
            collectionGesturesList.add(Gesture(GestureEnum.GESTURE_CUSTOM_2.number, gestureName = applicationContext().getString(R.string.gesture_3_btn)))
            collectionGesturesList.add(Gesture(GestureEnum.GESTURE_CUSTOM_3.number, gestureName = applicationContext().getString(R.string.gesture_4_btn)))
            collectionGesturesList.add(Gesture(GestureEnum.GESTURE_CUSTOM_4.number, gestureName = applicationContext().getString(R.string.gesture_5_btn)))
            collectionGesturesList.add(Gesture(GestureEnum.GESTURE_CUSTOM_5.number, gestureName = applicationContext().getString(R.string.gesture_6_btn)))
            collectionGesturesList.add(Gesture(GestureEnum.GESTURE_CUSTOM_6.number, gestureName = applicationContext().getString(R.string.gesture_7_btn)))
            collectionGesturesList.add(Gesture(GestureEnum.GESTURE_CUSTOM_7.number, gestureName = applicationContext().getString(R.string.gesture_8_btn)))
            collectionGesturesList.add(Gesture(GestureEnum.GESTURE_CUSTOM_8.number, gestureName = applicationContext().getString(R.string.gesture_9_btn)))
            collectionGesturesList.add(Gesture(GestureEnum.GESTURE_CUSTOM_9.number, gestureName = applicationContext().getString(R.string.gesture_10_btn)))
            collectionGesturesList.add(Gesture(GestureEnum.GESTURE_CUSTOM_10.number, gestureName = applicationContext().getString(R.string.gesture_11_btn)))
            collectionGesturesList.add(Gesture(GestureEnum.GESTURE_CUSTOM_11.number, gestureName = applicationContext().getString(R.string.gesture_12_btn)))
            collectionGesturesList.add(Gesture(GestureEnum.GESTURE_CUSTOM_12.number, gestureName = applicationContext().getString(R.string.gesture_13_btn)))
            collectionGesturesList.add(Gesture(GestureEnum.GESTURE_CUSTOM_13.number, gestureName = applicationContext().getString(R.string.gesture_14_btn)))
            return collectionGesturesList
        }

        fun getGesture(gestureId: Int): Gesture {
            val gestureList = getCollectionGestures().filter{ it.gestureId == gestureId }
            if (gestureList.isNotEmpty()) { return gestureList[0] }
            else { return  Gesture(0)
            }
        }
    }
}