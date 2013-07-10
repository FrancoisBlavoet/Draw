package com.interactive.stroke.draw.utils;

public class BrushSizeHandler {

	private static final float STANDARD_SIZE_1 = 1.0f;
	private static final float STANDARD_SIZE_2 = 5.0f;
	private static final float STANDARD_SIZE_3 = 8.0f;
	private static final float STANDARD_SIZE_4 = 12.0f;
	private static final float STANDARD_SIZE_5 = 16.0f;
	private static final float STANDARD_SIZE_6 = 20.0f;
	private static final float STANDARD_SIZE_7 = 30.0f;
	private static final float STANDARD_SIZE_8 = 40.0f;
	private static final float STANDARD_SIZE_9 = 50.0f;
	private static final float STANDARD_SIZE_10 = 70.0f;
	private static final float STANDARD_SIZE_11 = 90.0f;

	private static double pro;
	private static float max;
	private static float min;

	public static float calculate(int progress) {

		// Very crude... but it will do the trick.
		if (progress >= 0 && progress <= 10) {
			max = STANDARD_SIZE_2;
			min = STANDARD_SIZE_1;
		} else if (progress >= 10 && progress <= 20) {
			max = STANDARD_SIZE_3;
			min = STANDARD_SIZE_2;
		} else if (progress >= 20 && progress <= 30) {
			max = STANDARD_SIZE_4;
			min = STANDARD_SIZE_3;
		} else if (progress >= 30 && progress <= 40) {
			max = STANDARD_SIZE_5;
			min = STANDARD_SIZE_4;
		} else if (progress >= 40 && progress <= 50) {
			max = STANDARD_SIZE_6;
			min = STANDARD_SIZE_5;
		} else if (progress >= 50 && progress <= 60) {
			max = STANDARD_SIZE_7;
			min = STANDARD_SIZE_6;
		} else if (progress >= 60 && progress <= 70) {
			max = STANDARD_SIZE_8;
			min = STANDARD_SIZE_7;
		} else if (progress >= 70 && progress <= 80) {
			max = STANDARD_SIZE_9;
			min = STANDARD_SIZE_8;
		} else if (progress >= 80 && progress <= 90) {
			max = STANDARD_SIZE_10;
			min = STANDARD_SIZE_9;
		} else if (progress >= 90 && progress <= 100) {
			max = STANDARD_SIZE_11;
			min = STANDARD_SIZE_10;
		}

		/* calculates the %.
		 * basically its % = (a - x) / (y - x)
		 * a = progress
		 * x = (progress / 10) * 10  -- to round the interger down. like if progress = 95 then x = 90.
		 * y = ((progress / 10) * 10) + 10 -- to round the interger up. like if progress = 93 then y = 100.
		 */
		pro = (double) (progress - ((progress / 10) * 10))
				/ (double) ((((progress / 10) * 10) + 10) - ((progress / 10) * 10));
		
		// calculate also between points
		return (float) (min + ((max - min) * pro));
	}
}
