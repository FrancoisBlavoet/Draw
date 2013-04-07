package com.interactive.stroke.draw.gestures;

import android.content.Context;
import android.view.MotionEvent;

public abstract class BaseGestureDetector {
    
    protected final Context mContext;
    protected boolean mGestureInProgress;

    protected MotionEvent mPreviousEvent;
    protected MotionEvent mCurrentEvent;

    protected float mCurrentPressure;
    protected float mPreviousPressure;

    /**
     * This value is the threshold ratio between our previous combined pressure
     * and the current combined pressure. We will only fire an onScale event if
     * the computed ratio between the current and previous event pressures is
     * greater than this value. When pressure decreases rapidly between events
     * the position values can often be imprecise, as it usually indicates that
     * the user is in the process of lifting a pointer off of the device. Its
     * value was tuned experimentally. // From AOSP
     */
    protected static final float PRESSURE_THRESHOLD = 0.67f;

    public BaseGestureDetector(Context context) {
	mContext = context;
    }
    
    protected abstract void handleStartProgressEvent(int actionCode,
		                                     MotionEvent event);

    protected abstract void handleInProgressEvent(int actionCode,
		  				  MotionEvent event);

    public boolean onTouchEvent(MotionEvent event) {
	final int actionCode = event.getAction() & MotionEvent.ACTION_MASK;
	if (!mGestureInProgress) {
	    handleStartProgressEvent(actionCode, event);
	} else {
	    handleInProgressEvent(actionCode, event);
	}
	return true;
    }

    protected void updateStateByEvent(MotionEvent event) {
	final MotionEvent previousEvent = mPreviousEvent;

	// Reset mCurrentEvent
	if (mCurrentEvent != null) {
	    mCurrentEvent.recycle();
	    mCurrentEvent = null;
	}
	mCurrentEvent = MotionEvent.obtain(event);

	mCurrentPressure = event.getPressure(event.getActionIndex());
	if (mPreviousEvent != null) {
	    mPreviousPressure = previousEvent.getPressure(previousEvent.getActionIndex());
	}
    }

    protected void resetState() {
	if (mPreviousEvent != null) {
	    mPreviousEvent.recycle();
	    mPreviousEvent = null;
	}
	if (mCurrentEvent != null) {
	    mCurrentEvent.recycle();
	    mCurrentEvent = null;
	}
	mGestureInProgress = false;
    }

    public boolean isInProgress() {
	return mGestureInProgress;
    }

}
