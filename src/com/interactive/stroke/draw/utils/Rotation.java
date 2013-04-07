package com.interactive.stroke.draw.utils;

import android.view.Surface;

public class Rotation {

    public static int convertToAngleInDegrees(int constantToConvert) {

	switch (constantToConvert) {
	case Surface.ROTATION_0:
	    return 0;
	case Surface.ROTATION_180:
	    return 180;
	case Surface.ROTATION_270:
	    return 270;
	case Surface.ROTATION_90:
	    return 90;
	}
	return 0;
    }

}
