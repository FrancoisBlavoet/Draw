package com.interactive.stroke.draw;

import com.interactive.stroke.draw.R;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.LightingColorFilter;
import android.graphics.Paint;
//import android.graphics.PorterDuffColorFilter;
//import android.graphics.PorterDuff.Mode;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.ImageView;

public class MasterBucket extends ImageView {

    private Bitmap mBackground = null;
    private Bitmap mMask = null;
    private Bitmap mBitmap = null;
    private Paint paint = new Paint();
    private int mColor = Color.BLACK;

    public MasterBucket(Context context) {
	super(context);
	mBackground = BitmapFactory.decodeResource(getResources(),
		R.drawable.color_masterbucket_norm);
	mMask = BitmapFactory.decodeResource(getResources(),
		R.drawable.color_masterbucket_mask);
    }

    public MasterBucket(Context context, AttributeSet set) {
	super(context, set);
	mBackground = BitmapFactory.decodeResource(getResources(),
		R.drawable.color_masterbucket_norm);
	mMask = BitmapFactory.decodeResource(getResources(),
		R.drawable.color_masterbucket_mask);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
	switch (event.getAction()) {
	case MotionEvent.ACTION_DOWN:
	    if (mBackground != null) {
		mBackground.recycle();
	    }
	    mBackground = BitmapFactory.decodeResource(getResources(),
		    R.drawable.color_masterbucket_pressed);
	    updateDrawable();
	    break;
	case MotionEvent.ACTION_UP:
	    if (mBackground != null) {
		mBackground.recycle();
	    }
	    mBackground = BitmapFactory.decodeResource(getResources(),
		    R.drawable.color_masterbucket_norm);
	    updateDrawable();
	    break;
	}
	return super.onTouchEvent(event);
    }

    void setColor(int color) {
	mColor = color;
	updateDrawable();
    }

    void updateDrawable() {
	if (mMask != null && mBackground != null) {
	    LightingColorFilter cf = new LightingColorFilter(mColor, 1);
	    //PorterDuffColorFilter cf = new PorterDuffColorFilter(mColor,
		//    Mode.LIGHTEN);
	    Bitmap bm = combineImages(mBackground, mMask, cf);
	    this.setImageBitmap(bm);
	}
    }

    public Bitmap combineImages(Bitmap base, Bitmap mask, ColorFilter filter) {
	if (mBitmap != null) {
	    mBitmap.recycle();
	}
	mBitmap = Bitmap.createBitmap(base.getWidth(), base.getHeight(),
		Bitmap.Config.ARGB_8888);
	Canvas canvas = new Canvas(mBitmap);
	canvas.drawBitmap(base, 0, 0, null);
	paint.setColorFilter(filter);
	canvas.drawBitmap(mask, 0, 0, paint);
	return mBitmap;
    }

}
