package com.bailout.stickk.ubi4.adapters.widgetDelegeteAdapters

import android.view.View
import android.view.animation.RotateAnimation
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.bailout.stickk.R
import com.bailout.stickk.databinding.Ubi4WidgetCalibrationButtonsBinding
import com.bailout.stickk.ubi4.models.CalibrationButtonsItem
import com.bailout.stickk.ubi4.models.CalibrationButton
import com.bailout.stickk.ubi4.models.CalibrationStatus
import com.bailout.stickk.ubi4.models.ConnectionStatus
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.livermor.delegateadapter.delegate.ViewBindingDelegateAdapter

class CalibrationButtonsDelegateAdapter :
    ViewBindingDelegateAdapter<CalibrationButtonsItem, Ubi4WidgetCalibrationButtonsBinding>(
        Ubi4WidgetCalibrationButtonsBinding::inflate
    ) {

    override fun Ubi4WidgetCalibrationButtonsBinding.onBind(item: CalibrationButtonsItem) {
        root.visibility = View.VISIBLE

        // Обновляем данные для каждой кнопки
        item.buttons.forEachIndexed { index, button ->
            when (index) {
                0 -> updateButton(
                    button = button,
                    buttonView = calibration1Btn,
                    content = expandableContent1,
                    arrow = arrowIcon1,
                    calibrationStatus = calibrationStatus1,
                    connectionStatus = connectionStatusValue1,
                    connectionText = connectionStatusText1,
                    totalStepsValue = totalStepsValue1,
                    totalStepsText = totalStepsText1,
                    progressBadge = progressBadge1,
                    progressIndicator = progressIndicator1,
                    progressText = progressText1
                )
                1 -> updateButton(
                    button = button,
                    buttonView = calibration2Btn,
                    content = expandableContent2,
                    arrow = arrowIcon2,
                    calibrationStatus = calibrationStatus2,
                    connectionStatus = connectionStatusValue2,
                    connectionText = connectionStatusText2,
                    totalStepsValue = totalStepsValue2,
                    totalStepsText = totalStepsText2,
                    progressBadge = progressBadge2,
                    progressIndicator = progressIndicator2,
                    progressText = progressText2
                )
                2 -> updateButton(
                    button = button,
                    buttonView = calibration3Btn,
                    content = expandableContent3,
                    arrow = arrowIcon3,
                    calibrationStatus = calibrationStatus3,
                    connectionStatus = connectionStatusValue3,
                    connectionText = connectionStatusText3,
                    totalStepsValue = totalStepsValue3,
                    totalStepsText = totalStepsText3,
                    progressBadge = progressBadge3,
                    progressIndicator = progressIndicator3,
                    progressText = progressText3
                )
                3 -> updateButton(
                    button = button,
                    buttonView = calibration4Btn,
                    content = expandableContent4,
                    arrow = arrowIcon4,
                    calibrationStatus = calibrationStatus4,
                    connectionStatus = connectionStatusValue4,
                    connectionText = connectionStatusText4,
                    totalStepsValue = totalStepsValue4,
                    totalStepsText = totalStepsText4,
                    progressBadge = progressBadge4,
                    progressIndicator = progressIndicator4,
                    progressText = progressText4
                )
                4 -> updateButton(
                    button = button,
                    buttonView = calibration5Btn,
                    content = expandableContent5,
                    arrow = arrowIcon5,
                    calibrationStatus = calibrationStatus5,
                    connectionStatus = connectionStatusValue5,
                    connectionText = connectionStatusText5,
                    totalStepsValue = totalStepsValue5,
                    totalStepsText = totalStepsText5,
                    progressBadge = progressBadge5,
                    progressIndicator = progressIndicator5,
                    progressText = progressText5
                )
                5 -> updateButton(
                    button = button,
                    buttonView = calibration6Btn,
                    content = expandableContent6,
                    arrow = arrowIcon6,
                    calibrationStatus = calibrationStatus6,
                    connectionStatus = connectionStatusValue6,
                    connectionText = connectionStatusText6,
                    totalStepsValue = totalStepsValue6,
                    totalStepsText = totalStepsText6,
                    progressBadge = progressBadge6,
                    progressIndicator = progressIndicator6,
                    progressText = progressText6
                )
            }
        }
    }

    // Функция обновления кнопки
    private fun updateButton(
        button: CalibrationButton,
        buttonView: View,
        content: View,
        arrow: View,

        calibrationStatus: TextView,

        connectionStatus: TextView,
        connectionText: TextView,

        totalStepsValue: TextView,
        totalStepsText: TextView,

        progressIndicator: CircularProgressIndicator,
        progressBadge: TextView,
        progressText: TextView
    ) {

        // Обновляем статус калибровки
        val (calibrationColor, calibrationTextRes) = getCalibrationStatusInfo(button)
        calibrationStatus.apply {
            setText(calibrationTextRes)
            setTextColor(ContextCompat.getColor(context, calibrationColor))
        }

        // Обновляем состояние подключения
        val (connectionColor, connectionTextRes) = getConnectionStatusInfo(button)
        connectionStatus.apply {
            setText(connectionTextRes)
            setTextColor(ContextCompat.getColor(context, connectionColor))
            alpha = if (button.connectionStatus == ConnectionStatus.CONNECTED) 1f else 0.5f
        }
        // Обновляем текст под бейджиком подключения
        connectionText.apply {
            alpha = if (button.connectionStatus == ConnectionStatus.CONNECTED) 1f else 0.5f
            setTextColor(ContextCompat.getColor(context,
                if (button.connectionStatus == ConnectionStatus.CONNECTED)
                    R.color.white
                else
                    R.color.ubi4_deactivate_text
            ))
        }

        // Настраиваем разворачивание/сворачивание
        setupExpandableContent(buttonView, content, arrow)

        // Обновляем информацию о шагах
        updateStepsInfo(button, totalStepsValue, totalStepsText)

        // Обновляем прогресс бар
        updateProgressInfo(button, progressIndicator, progressBadge, progressText)
    }

    // 1. Получение статуса калибровки и цвета
    private fun getCalibrationStatusInfo(button: CalibrationButton): Pair<Int, Int> {
        return when (button.calibrationStatus) {
            CalibrationStatus.NOT_CALIBRATED ->
                R.color.ubi4_deactivate_text to R.string.calibration_status_not_calibrated
            CalibrationStatus.ERROR ->
                R.color.red to R.string.calibration_status_error
            CalibrationStatus.CALIBRATED ->
                R.color.ubi4_active to R.string.calibration_status_calibrated
        }
    }

    // 2. Анимация стрелки и сворачивание
    private fun setupExpandableContent(buttonView: View, contentView: View, arrowView: View) {

        var isExpanded = contentView.visibility == View.VISIBLE

        buttonView.setOnClickListener {
            if (!buttonView.isEnabled) return@setOnClickListener

            val rotateAnimation = if (isExpanded) {
                RotateAnimation(180f, 0f, arrowView.width / 2f, arrowView.height / 2f)
            } else {
                RotateAnimation(0f, 180f, arrowView.width / 2f, arrowView.height / 2f)
            }

            rotateAnimation.apply {
                duration = 200
                fillAfter = true
            }
            arrowView.startAnimation(rotateAnimation)

            contentView.visibility = if (isExpanded) View.GONE else View.VISIBLE
            isExpanded = !isExpanded
        }
    }

    // 3. Получение статуса подключения и цвета
    private fun getConnectionStatusInfo(button: CalibrationButton): Pair<Int, Int> {
        return when (button.connectionStatus) {
            ConnectionStatus.NOT_CONNECTED ->
                R.color.ubi4_deactivate_text to R.string.connection_status_not_connected
            ConnectionStatus.ERROR ->
                R.color.red to R.string.connection_status_error
            ConnectionStatus.CONNECTED ->
                R.color.ubi4_active to R.string.connection_status_connected
        }
    }

    // 4. Обновление информации о шагах
    private fun updateStepsInfo(
        button: CalibrationButton,
        stepsValue: TextView,
        stepsText: TextView
    ) {
        val isConnected = button.connectionStatus == ConnectionStatus.CONNECTED
        val color = if (isConnected) R.color.ubi4_active else R.color.ubi4_deactivate_text

        stepsValue.apply {
            text = button.encoderSteps.toString()
            setTextColor(ContextCompat.getColor(context, color))
            alpha = if (isConnected) 1f else 0.5f
        }

        stepsText.apply {
            setTextColor(ContextCompat.getColor(context,
                if (button.connectionStatus == ConnectionStatus.CONNECTED)
                    R.color.white
                else
                    R.color.ubi4_deactivate_text
            ))
            alpha = if (isConnected) 1f else 0.5f
        }
    }

    // 5. Обновление прогресс бара и бейджа
    private fun updateProgressInfo(
        button: CalibrationButton,
        progressIndicator: CircularProgressIndicator,
        progressBadge: TextView,
        progressText: TextView
    ) {
        val isConnected = button.connectionStatus == ConnectionStatus.CONNECTED
        val color = if (isConnected) R.color.ubi4_active else R.color.ubi4_deactivate_text

        // Проверяем, изменилось ли значение
        if (progressIndicator.progress != button.currentValue) {
            progressIndicator.apply {
                max = button.encoderSteps
                progress = button.currentValue
                isIndeterminate = false
                setIndicatorColor(ContextCompat.getColor(context, color))
                alpha = if (isConnected) 1f else 0.5f
            }

            progressBadge.apply {
                text = button.currentValue.toString()
                setTextColor(ContextCompat.getColor(context, color))
                alpha = if (isConnected) 1f else 0.5f
            }
        }

        progressText.apply {
            setTextColor(ContextCompat.getColor(context,
                if (button.connectionStatus == ConnectionStatus.CONNECTED)
                    R.color.white
                else
                    R.color.ubi4_deactivate_text
            ))
            alpha = if (isConnected) 1f else 0.5f
        }
    }

    override fun isForViewType(item: Any): Boolean = item is CalibrationButtonsItem

    override fun CalibrationButtonsItem.getItemId(): Any = title
}
