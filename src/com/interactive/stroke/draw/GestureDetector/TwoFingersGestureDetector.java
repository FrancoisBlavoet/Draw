package com.interactive.stroke.draw.GestureDetector;

import android.content.Context;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.ViewConfiguration;

public abstract class TwoFingersGestureDetector extends BaseGestureDetector {

    private final float mEdgeSlop;
    private float mRightSlopEdge;
    private float mBottomSlopEdge;

    protected float mPreviousFingerDX;
    protected float mPreviousFingerDY;
    protected float mCurrentFingerDX;
    protected float mCurrentFingerDY;

    private float mCurrentSpan;
    private float mPreviousSpan;

    public TwoFingersGestureDetector(Context context) {
	super(context);

	ViewConfiguration config = ViewConfiguration.get(context);
	mEdgeSlop = config.getScaledEdgeSlop();
    }

    @Override
    protected abstract void handleStartProgressEvent(int actionCode,
	    					     MotionEvent event);

    @Override
    protected abstract void handleInProgressEvent(int actionCode,
	    					  MotionEvent event);

    protected void updateStateByEvent(MotionEvent event) {
	super.updateStateByEvent(event);

	if (event.getPointerCount() < 2) {
	    return;
	}
	final MotionEvent previousEvent = mPreviousEvent;
	mCurrentSpan = -1;
	mPreviousSpan = -1;

	// Previous Event
	try {
	    final float px0 = previousEvent.getX(0);
	    final float py0 = previousEvent.getY(0);
	    final float px1 = previousEvent.getX(1);
	    final float py1 = previousEvent.getY(1);
	    final float pvx = px1 - px0;
	    final float pvy = py1 - py0;
	    mPreviousFingerDX = pvx;
	    mPreviousFingerDY = pvy;
	} catch (Exception ex) {
	    Log.e("GestureDetectorError", "rare notification shade error");
	    // happens when the user opens the notification tray during the sliding event
	    return;
	}

	// Current
	final float cx0 = event.getX(0);
	final float cy0 = event.getY(0);
	final float cx1 = event.getX(1);
	final float cy1 = event.getY(1);
	final float cvx = cx1 - cx0;
	final float cvy = cy1 - cy0;
	mCurrentFingerDX = cvx;
	mCurrentFingerDY = cvy;
    }

    public float getCurrentSpan() {
	if (mCurrentSpan == -1) {
	    final float cvx = mCurrentFingerDX;
	    final float cvy = mCurrentFingerDY;
	    mCurrentSpan = (float) Math.sqrt(cvx * cvx + cvy * cvy);
	}
	return mCurrentSpan;
    }

    public float getPreviousSpan() {
	if (mPreviousSpan == -1) {
	    final float pvx = mPreviousFingerDX;
	    final float pvy = mPreviousFingerDY;
	    mPreviousSpan = (float) Math.sqrt(pvx * pvx + pvy * pvy);
	}
	return mPreviousSpan;
    }

    protected static float getRawX(MotionEvent event, 
	    			   int pointerIndex) {
	float offset = event.getX() - event.getRawX();
	if (pointerIndex < event.getPointerCount()) {
	    return event.getX(pointerIndex) + offset;
	}
	return 0f;
    }

    protected static float getRawY(MotionEvent event, 
	    			   int pointerIndex) {
	float offset = event.getY() - event.getRawY();
	if (pointerIndex < event.getPointerCount()) {
	    return event.getY(pointerIndex) + offset;
	}
	return 0f;
    }

    protected boolean isSloppyGesture(MotionEvent event) {
	DisplayMetrics metrics = mContext.getResources().getDisplayMetrics();
	mRightSlopEdge = metrics.widthPixels - mEdgeSlop;
	mBottomSlopEdge = metrics.heightPixels - mEdgeSlop;

	final float edgeSlop = mEdgeSlop;
	final float rightSlop = mRightSlopEdge;
	final float bottomSlop = mBottomSlopEdge;

	final float x0 = event.getRawX();
	final float y0 = event.getRawY();
	final float x1 = getRawX(event, 1);
	final float y1 = getRawY(event, 1);

	boolean p0sloppy = x0 < edgeSlop || y0 < edgeSlop || x0 > rightSlop
		|| y0 > bottomSlop;
	boolean p1sloppy = x1 < edgeSlop || y1 < edgeSlop || x1 > rightSlop
		|| y1 > bottomSlop;

	if (p0sloppy && p1sloppy) {
	    return true;
	} else if (p0sloppy) {
	    return true;
	} else if (p1sloppy) {
	    return true;
	}
	return false;
    }

}
