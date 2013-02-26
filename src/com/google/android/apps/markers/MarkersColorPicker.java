package com.google.android.apps.markers;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;

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
