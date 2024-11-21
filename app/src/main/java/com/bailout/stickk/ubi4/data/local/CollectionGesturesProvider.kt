package com.bailout.stickk.ubi4.data.local

import com.bailout.stickk.R
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUBI4.GestureEnum

class CollectionGesturesProvider() {
    companion object {
        fun getCollectionGestures(): ArrayList<Gesture> {
            val collectionGesturesList: ArrayList<Gesture> = ArrayList()
//            collectionGesturesList.add(Gesture(GestureEnum.GESTURE_NO_GESTURE.number, gestureName = "Нет жеста"))
            collectionGesturesList.add(Gesture(GestureEnum.GESTURE_FIST.number, gestureName = "Кулак", gestureImage = R.drawable.collection_fist_1))
            collectionGesturesList.add(Gesture(GestureEnum.GESTURE_POINT.number, gestureName = "Указательный", gestureImage = R.drawable.collection_point))
            collectionGesturesList.add(Gesture(GestureEnum.GESTURE_PINCH.number, gestureName = "Щепоть", gestureImage = R.drawable.collection_pinch))
            collectionGesturesList.add(Gesture(GestureEnum.GESTURE_FIST_THUMB_OVER.number, gestureName = "Кулак 2", gestureImage = R.drawable.collection_fist_2))
            collectionGesturesList.add(Gesture(GestureEnum.GESTURE_KEY.number, gestureName = "Ключ", gestureImage = R.drawable.collection_key))
            collectionGesturesList.add(Gesture(GestureEnum.GESTURE_ROCK.number, gestureName = "Коза", gestureImage = R.drawable.collection_rock))
            collectionGesturesList.add(Gesture(GestureEnum.GESTURE_TWIZZERS.number, gestureName = "Пинцет", gestureImage = R.drawable.collection_twizzers))//от Ок отличается тем что пальцы закрыты
            collectionGesturesList.add(Gesture(GestureEnum.GESTURE_CUPHOLDER.number, gestureName = "Хват стакана", gestureImage = R.drawable.collection_cupholder))
            collectionGesturesList.add(Gesture(GestureEnum.GESTURE_HALF_GRAB.number, gestureName = "Хват доски", gestureImage = R.drawable.collect_half_grab))
            collectionGesturesList.add(Gesture(GestureEnum.GESTURE_OK.number, gestureName = "Ок", gestureImage = R.drawable.collection_ok))
            collectionGesturesList.add(Gesture(GestureEnum.GESTURE_THUMB_UP.number, gestureName = "Класс", gestureImage = R.drawable.collection_thumb_up))
            collectionGesturesList.add(Gesture(GestureEnum.GESTURE_MIDDLE_FINGER.number, gestureName = "Средний палец", gestureImage = R.drawable.collection_middle_finger))
            collectionGesturesList.add(Gesture(GestureEnum.GESTURE_DOUBLE_POINT.number, gestureName = "Пистолет", gestureImage = R.drawable.collection_double_point))
            collectionGesturesList.add(Gesture(GestureEnum.GESTURE_CALL_ME.number, gestureName = "Позвони мне", gestureImage = R.drawable.collection_call_me))
            collectionGesturesList.add(Gesture(GestureEnum.GESTURE_NATURAL_POSITION.number, gestureName = "Натуральное положение", gestureImage = R.drawable.collection_natural_position))
            collectionGesturesList.add(Gesture(GestureEnum.GESTURE_CUSTOM_0.number, gestureName = "Жест №1"))
            collectionGesturesList.add(Gesture(GestureEnum.GESTURE_CUSTOM_1.number, gestureName = "Жест №2"))
            collectionGesturesList.add(Gesture(GestureEnum.GESTURE_CUSTOM_2.number, gestureName = "Жест №3"))
            collectionGesturesList.add(Gesture(GestureEnum.GESTURE_CUSTOM_3.number, gestureName = "Жест №4"))
            collectionGesturesList.add(Gesture(GestureEnum.GESTURE_CUSTOM_4.number, gestureName = "Жест №5"))
            collectionGesturesList.add(Gesture(GestureEnum.GESTURE_CUSTOM_5.number, gestureName = "Жест №6"))
            collectionGesturesList.add(Gesture(GestureEnum.GESTURE_CUSTOM_6.number, gestureName = "Жест №7"))
            collectionGesturesList.add(Gesture(GestureEnum.GESTURE_CUSTOM_7.number, gestureName = "Жест №8"))
            collectionGesturesList.add(Gesture(GestureEnum.GESTURE_CUSTOM_8.number, gestureName = "Жест №9"))
            collectionGesturesList.add(Gesture(GestureEnum.GESTURE_CUSTOM_9.number, gestureName = "Жест №10"))
            collectionGesturesList.add(Gesture(GestureEnum.GESTURE_CUSTOM_10.number, gestureName = "Жест №11"))
            collectionGesturesList.add(Gesture(GestureEnum.GESTURE_CUSTOM_11.number, gestureName = "Жест №12"))
            collectionGesturesList.add(Gesture(GestureEnum.GESTURE_CUSTOM_12.number, gestureName = "Жест №13"))
            collectionGesturesList.add(Gesture(GestureEnum.GESTURE_CUSTOM_13.number, gestureName = "Жест №14"))
            return collectionGesturesList
        }

        fun getGesture(gestureId: Int): Gesture {
            val gestureList = getCollectionGestures().filter{ it.gestureId == gestureId }
            if (gestureList.size != 0) { return gestureList[0] }
            else { return  Gesture(0)}
        }
    }
}