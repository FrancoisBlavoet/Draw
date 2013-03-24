package com.interactive.stroke.draw.GestureDetector;

import android.content.Context;
import android.view.MotionEvent;

public class RotationGestureDetector extends TwoFingersGestureDetector {

    public interface OnRotateGestureListener {
	public boolean onRotate(RotationGestureDetector detector);
	public boolean onRotateBegin(RotationGestureDetector detector);
	public void    onRotateEnd(RotationGestureDetector detector);
    }

    public static class SimpleOnRotateGestureListener implements OnRotateGestureListener {
	public boolean onRotate(RotationGestureDetector detector) {
	    return false;
	}

	public boolean onRotateBegin(RotationGestureDetector detector) {
	    return true;
	}

	public void onRotateEnd(RotationGestureDetector detector) {
	}
    }

    private final OnRotateGestureListener mListener;
    private boolean mIsSloppyGesture;

    public RotationGestureDetector(Context context,
	                           OnRotateGestureListener listener) {
	super(context);
	mListener = listener;
    }

    @Override
    protected void handleStartProgressEvent(int actionCode, 
	          			    MotionEvent event) {
	switch (actionCode) {
	case MotionEvent.ACTION_POINTER_DOWN:
	    resetState();
	    mPreviousEvent = MotionEvent.obtain(event);
	    updateStateByEvent(event);

	    mIsSloppyGesture = isSloppyGesture(event);
	    if (!mIsSloppyGesture) {
		mGestureInProgress = mListener.onRotateBegin(this);
	    }
	    break;

	case MotionEvent.ACTION_MOVE:
	   if (!mIsSloppyGesture) {
		break;
	    }

	    mIsSloppyGesture = isSloppyGesture(event);
	    if (!mIsSloppyGesture) {
		mGestureInProgress = mListener.onRotateBegin(this);
	    }

	    break;

	case MotionEvent.ACTION_POINTER_UP:
	    if (!mIsSloppyGesture) {
		break;
	    }

	    break;
	}
    }

    @Override
    protected void handleInProgressEvent(int actionCode, 
	    				 MotionEvent event) {
	switch (actionCode) {

	case MotionEvent.ACTION_MOVE:
	    updateStateByEvent(event);

	    if (mCurrentPressure / mPreviousPressure > PRESSURE_THRESHOLD) {
		final boolean updatePrevious = mListener.onRotate(this);
		if (updatePrevious) {
		    mPreviousEvent.recycle();
		    mPreviousEvent = MotionEvent.obtain(event);
		}
	    }
	    break;
	    
	case MotionEvent.ACTION_POINTER_UP:
	    updateStateByEvent(event);
	    if (!mIsSloppyGesture) {
		mListener.onRotateEnd(this);
	    }
	    resetState();
	    break;
	    
	case MotionEvent.ACTION_CANCEL:
	    if (!mIsSloppyGesture) {
		mListener.onRotateEnd(this);
	    }
	    resetState();
	    break;
	}

    }

    @Override
    protected void resetState() {
	super.resetState();
	mIsSloppyGesture = false;
    }

    public float getRotationDegreesDelta() {
	double angle = Math.atan2(mPreviousFingerDY, mPreviousFingerDX)
		- Math.atan2(mCurrentFingerDY, mCurrentFingerDX);
	return (float) (angle * 180 / Math.PI);
    }
}
