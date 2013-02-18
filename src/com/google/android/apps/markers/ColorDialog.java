package com.google.android.apps.markers;

import org.dsandler.apps.markers.R;

import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;

import com.actionbarsherlock.app.SherlockDialogFragment;

//...
public class ColorDialog extends SherlockDialogFragment
{

	public ColorDialog()
	{
		setStyle(STYLE_NORMAL, R.style.colorPickerStyle);

	}

	@Override
	public View onCreateView(LayoutInflater inflater,
	                         ViewGroup container,
	                         Bundle savedInstanceState)
	{
		View view = inflater.inflate(R.layout.dialog_color_picker, container);

		getDialog().getWindow().requestFeature(Window.FEATURE_NO_TITLE);
		getDialog().getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
		getDialog().getWindow().getAttributes().horizontalMargin = 0.1F;

		// WindowManager.LayoutParams params = getDialog().getWindow().getAttributes();
		WindowManager.LayoutParams params = new WindowManager.LayoutParams();


		getDialog().getWindow().setAttributes(params);
		 WindowManager.LayoutParams wmlp = getDialog().getWindow().getAttributes();
		 wmlp.gravity = Gravity.LEFT;
		 wmlp.x = 0;
		return view;
	}

	public interface ColorDialogListener
	{
		void onFinishColorDialog(String inputText);
	}

}
