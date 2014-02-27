package com.interactive.stroke.draw;

import android.app.DialogFragment;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.larswerkman.holocolorpicker.ColorPicker;
import com.larswerkman.holocolorpicker.ColorPicker.OnColorChangedListener;
import com.larswerkman.holocolorpicker.SaturationBar;
import com.larswerkman.holocolorpicker.ValueBar;

public class ColorDialogFragment extends DialogFragment implements OnColorChangedListener {

    public ColorPicker mColorPicker;
    public SaturationBar mSaturation;
    public ValueBar mValueBar;
    public int mOldColor = Color.BLACK ;
   

    public ColorDialogFragment() {

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
	    Bundle savedInstanceState) {
	View view = inflater.inflate(R.layout.dialog_color_picker, container);
	mColorPicker = (ColorPicker) view.findViewById(R.id.colorPicker);
	mSaturation = (SaturationBar) view.findViewById(R.id.saturationBar);
	mValueBar = (ValueBar) view.findViewById(R.id.valueBar);
	
	mColorPicker.setOldCenterColor(mOldColor);
	mColorPicker.setColor(mOldColor);
	
	mColorPicker.addSaturationBar(mSaturation);
	mColorPicker.addValueBar(mValueBar);
	mColorPicker.setOnColorChangedListener(this);
	return view;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
	super.onCreate(savedInstanceState);
	setStyle(DialogFragment.STYLE_NO_FRAME, R.style.DialogColorPicker);
    }

    @Override
    public void onResume() {
	super.onResume();
	mColorPicker.setOldCenterColor(mOldColor);
	mColorPicker.setColor(mOldColor);
	}

    @Override
    public void onColorChanged(int color) {
	mOldColor = color;
	((DrawActivity) getActivity()).setPenColor(color);
	((DrawActivity) getActivity()).mMasterBucket.setColor(color);
	((DrawActivity) getActivity()).mSlateFragment.mColor = color;

    }

}
