package com.bailout.stickk.ubi4.adapters.widgetDelegateAdapters

import com.bailout.stickk.databinding.Ubi4WidgetGestureUsageChartBinding
import com.bailout.stickk.ubi4.ui.widgets.GestureUsageChartItem
import com.livermor.delegateadapter.delegate.ViewBindingDelegateAdapter

data class GestureUsageWidgetItem(
    val title: String,
    val items: List<GestureUsageChartItem>
)

class GestureUsageChartDelegateAdapter :
    ViewBindingDelegateAdapter<GestureUsageWidgetItem, Ubi4WidgetGestureUsageChartBinding>(
        Ubi4WidgetGestureUsageChartBinding::inflate
    ) {

    override fun Ubi4WidgetGestureUsageChartBinding.onBind(item: GestureUsageWidgetItem) {
        root.setTitle(item.title)
        root.setItems(item.items)
    }

    override fun isForViewType(item: Any): Boolean = item is GestureUsageWidgetItem

    override fun GestureUsageWidgetItem.getItemId(): Any = "gesture-usage-chart"
}
