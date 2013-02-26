package com.google.android.apps.markers;

import org.dsandler.apps.markers.R;

import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;

import com.actionbarsherlock.app.SherlockDialogFragment;
import com.google.android.apps.markers.MarkersColorPicker.ColorPickerCallback;

public class ColorDialogFragment extends SherlockDialogFragment {

    public MarkersColorPicker mColorPicker;
    public int mOldColor = Color.BLACK ;

    public ColorDialogFragment() {

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
	    Bundle savedInstanceState) {
	View view = inflater.inflate(R.layout.dialog_color_picker, container);
	WindowManager.LayoutParams wmlp = getDialog().getWindow()
		.getAttributes();
	wmlp.gravity = Gravity.LEFT;
	wmlp.x = 180;
	mColorPicker = (MarkersColorPicker) view.findViewById(R.id.picker);
	mColorPicker.setOldCenterColor(mOldColor);
	mColorPicker.setColor(mOldColor);
	mColorPicker.setNewCenterColor(mOldColor);
	mColorPicker.setOnColorChangedListener(new ColorPickerCallback() {
	    @Override
	    public void onColorChanged(int color) {
		mOldColor = color;
		((MarkersActivity) getActivity()).setPenColor(color);
		((MarkersActivity) getActivity()).mColorButton.setColor(color);
		((MarkersActivity) getActivity()).mColor = color;
	    }
	});
	return view;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
	super.onCreate(savedInstanceState);
	setStyle(DialogFragment.STYLE_NO_FRAME, R.style.colorPickerStyle);

    }

    @Override
    public void onResume() {
	super.onResume();
    }

}
