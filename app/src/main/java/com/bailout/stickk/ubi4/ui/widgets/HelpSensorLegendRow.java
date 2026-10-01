package com.bailout.stickk.ubi4.ui.widgets;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.widget.TextView;

import com.bailout.stickk.R;

/** Native text and a sensor marker matching the Figma instruction legend. */
public final class HelpSensorLegendRow extends TextView {
    private final Paint markerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    public HelpSensorLegendRow(Context context, AttributeSet attributes) {
        super(context, attributes);
        setLayerType(LAYER_TYPE_SOFTWARE, null);
    }

    @Override protected void onFinishInflate() {
        super.onFinishInflate();
        boolean opening = "opening".equals(getTag());
        markerPaint.setColor(getContext().getColor(opening
                ? R.color.help_sensor_opening : R.color.help_sensor_closing));
        if (opening) {
            float density = getResources().getDisplayMetrics().density;
            markerPaint.setShadowLayer(4 * density, 0, 4 * density, 0x40000000);
            setShadowLayer(4 * density, 0, 4 * density, 0x40000000);
        }
    }

    @Override protected void onDraw(Canvas canvas) {
        float radius = 7 * getResources().getDisplayMetrics().density;
        float centerX = getLayoutDirection() == LAYOUT_DIRECTION_RTL ? getWidth() - radius : radius;
        canvas.drawCircle(centerX, getHeight() / 2f, radius, markerPaint);
        super.onDraw(canvas);
    }
}
