package com.google.android.apps.markers;

import android.content.Context;
import android.graphics.Canvas;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;

import com.google.android.apps.markers.ToolButton.ToolCallback;
import com.larswerkman.colorpicker.ColorPicker;

public class MarkersColorPicker extends com.larswerkman.colorpicker.ColorPicker {
    private ColorPickerCallback mOnColorChangedListener;

    public MarkersColorPicker(Context context, AttributeSet attrs) {
	super(context, attrs);
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
	boolean returnValue = super.onTouchEvent(e);
	if (this.mUserIsMovingPointer) {
	    mOnColorChangedListener.onColorChanged(mCenterNewColor);
	}
	return returnValue;
    }

    public interface ColorPickerCallback {
	public void onColorChanged(int color);
    }

    public void setOnColorChangedListener(ColorPickerCallback listener) {
	mOnColorChangedListener = listener;
    }
    

}
