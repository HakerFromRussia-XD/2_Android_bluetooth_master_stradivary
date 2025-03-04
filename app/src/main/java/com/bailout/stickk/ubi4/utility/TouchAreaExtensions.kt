package com.bailout.stickk.ubi4.utility

import android.graphics.Rect
import android.util.Log
import android.view.MotionEvent
import android.view.TouchDelegate
import android.view.View

/**
 * Класс для объединения нескольких TouchDelegate.
 * Он позволяет назначить родительскому View группу делегатов, чтобы расширенные зоны для разных дочерних элементов
 * не перезаписывали друг друга.
 */
class TouchDelegateGroup(private val parent: View) : TouchDelegate(Rect(), parent) {

    private val delegates = mutableListOf<TouchDelegate>()

    /**
     * Добавляет новый делегат в группу.
     */
    fun addDelegate(delegate: TouchDelegate) {
        delegates.add(delegate)
    }

    /**
     * Перебирает все делегаты и возвращает true, если хотя бы один обработал событие.
     */
    override fun onTouchEvent(event: MotionEvent): Boolean {
        var handled = false
        for (delegate in delegates) {
            if (delegate.onTouchEvent(event)) {
                handled = true
            }
        }
        return handled
    }
}

/**
 * Объект с функциями-расширениями для работы с расширением области касания.
 */
object TouchAreaExtensions {

    /**
     * Расширяет область клика (touch area) данного View на указанное количество пикселей.
     * Если у родительского View уже назначена группа делегатов, то новый делегат просто добавляется,
     * иначе создается новая группа.
     *
     * Пример использования:
     * myView.post {
     *     myView.expandTouchArea(20)
     * }
     */
    fun View.expandTouchArea(expansion: Int) {
        (parent as? View)?.post {
            val rect = Rect()
            // Получаем текущие границы кликабельной области
            getHitRect(rect)
            // Расширяем границы на заданное число пикселей со всех сторон
            rect.left -= expansion
            rect.top -= expansion
            rect.right += expansion
            rect.bottom += expansion
            val parentView = parent as View
            // Проверяем, назначена ли уже группа делегатов
            val currentDelegate = parentView.touchDelegate
            if (currentDelegate is TouchDelegateGroup) {
                currentDelegate.addDelegate(TouchDelegate(rect, this))
            } else {
                val group = TouchDelegateGroup(parentView)
                group.addDelegate(TouchDelegate(rect, this))
                parentView.touchDelegate = group
            }
            Log.d("TouchArea", "Expanded rect for ${this.id}: $rect")

        }
    }
}