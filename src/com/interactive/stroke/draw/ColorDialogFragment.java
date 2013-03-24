package com.interactive.stroke.draw;

import com.interactive.stroke.draw.R;

import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.actionbarsherlock.app.SherlockDialogFragment;
import com.larswerkman.colorpicker.ColorPicker;
import com.larswerkman.colorpicker.ColorPicker.OnColorChangedListener;
import com.larswerkman.colorpicker.SVBar;

public class ColorDialogFragment extends SherlockDialogFragment implements OnColorChangedListener {

    public ColorPicker mColorPicker;
    private SVBar msvBar;
    public int mOldColor = Color.BLACK ;
   

    public ColorDialogFragment() {

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
	    Bundle savedInstanceState) {
	View view = inflater.inflate(R.layout.dialog_color_picker, container);
	mColorPicker = (ColorPicker) view.findViewById(R.id.picker);
	mColorPicker.setOldCenterColor(mOldColor);
	mColorPicker.setColor(mOldColor);
	mColorPicker.setNewCenterColor(mOldColor);	
	msvBar = (SVBar) view.findViewById(R.id.svbar);
	mColorPicker.addSVBar(msvBar);
	mColorPicker.setOnColorChangedListener(this);
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

    @Override
    public void onColorChanged(int color) {
	mOldColor = color;
	((MarkersActivity) getActivity()).setPenColor(color);
	((MarkersActivity) getActivity()).mMasterBucket.setColor(color);
	((MarkersActivity) getActivity()).mColor = color;
    }

}
