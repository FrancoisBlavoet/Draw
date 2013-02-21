package com.google.android.apps.markers;

import org.dsandler.apps.markers.R;

import android.app.AlertDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;

public class ColorDialog extends AlertDialog
{

    public ColorDialog(Context context, int theme)
	{
		super(context,theme);
		LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View view = inflater.inflate(R.layout.dialog_color_picker, null);
		setView(view);
	}

}
