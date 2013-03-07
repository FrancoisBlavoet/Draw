package com.google.android.apps.markers.GestureDetector;

import android.content.Context;
import android.graphics.PointF;
import android.view.MotionEvent;

public class TranslationGestureDetector extends BaseGestureDetector {
    
    private static final PointF FOCUS_DELTA_ZERO = new PointF();
    private final OnMoveGestureListener mListener;

    private PointF mCurrentFocusPointInternal;
    private PointF mPreviousFocusPointInternal;
    
    private PointF mFocusPointExternal = new PointF();
    private PointF mFocusDeltaExternal = new PointF();

    public interface OnMoveGestureListener {
	public boolean onMove(TranslationGestureDetector detector);
	public boolean onMoveBegin(TranslationGestureDetector detector);
	public void    onMoveEnd(TranslationGestureDetector detector);
    }

    public static class SimpleOnMoveGestureListener implements OnMoveGestureListener {
	public boolean onMove(TranslationGestureDetector detector) {
	    return false;
	}

	public boolean onMoveBegin(TranslationGestureDetector detector) {
	    return true;
	}

	public void onMoveEnd(TranslationGestureDetector detector) {
	}
    }

   

    public TranslationGestureDetector(Context context, 
	    			      OnMoveGestureListener listener) {
	super(context);
	mListener = listener;
    }

    @Override
    protected void handleStartProgressEvent(int actionCode,
	    				    MotionEvent event) {
	switch (actionCode) {
	case MotionEvent.ACTION_DOWN:
	    resetState();
	    mPreviousEvent = MotionEvent.obtain(event);
	    updateStateByEvent(event);
	    break;

	case MotionEvent.ACTION_MOVE:
	    mGestureInProgress = mListener.onMoveBegin(this);
	    break;
	}
    }

    @Override
    protected void handleInProgressEvent(int actionCode,
	    				 MotionEvent event) {
	switch (actionCode) {
	case MotionEvent.ACTION_UP:
	case MotionEvent.ACTION_CANCEL:
	    mListener.onMoveEnd(this);
	    resetState();
	    break;

	case MotionEvent.ACTION_MOVE:
	    updateStateByEvent(event);
	    if (mCurrentPressure / mPreviousPressure > PRESSURE_THRESHOLD) {
		final boolean updatePrevious = mListener.onMove(this);
		if (updatePrevious) {
		    mPreviousEvent.recycle();
		    mPreviousEvent = MotionEvent.obtain(event);
		}
	    }
	    break;
	}
    }

    protected void updateStateByEvent(MotionEvent event) {
	super.updateStateByEvent(event);
	final MotionEvent previousEvent = mPreviousEvent;

	mCurrentFocusPointInternal = determineFocalPoint(event);
	mPreviousFocusPointInternal = determineFocalPoint(previousEvent);

	boolean skipNextMoveEvent = previousEvent.getPointerCount() != event.getPointerCount();
	mFocusDeltaExternal = skipNextMoveEvent ? FOCUS_DELTA_ZERO
		: new PointF(mCurrentFocusPointInternal.x - mPreviousFocusPointInternal.x,
			mCurrentFocusPointInternal.y - mPreviousFocusPointInternal.y);

	mFocusPointExternal.x += mFocusDeltaExternal.x;
	mFocusPointExternal.y += mFocusDeltaExternal.y;
    }

    private PointF determineFocalPoint(MotionEvent event) {
	final int pointerCount = event.getPointerCount();
	float xSum = 0.f;
	float ySum = 0.f;

	for (int i = 0; i < pointerCount; i++) {
	    xSum += event.getX(i);
	    ySum += event.getY(i);
	}

	return new PointF(xSum / pointerCount,
			  ySum / pointerCount);
    }

    public float getFocusX() {
	return mFocusPointExternal.x;
    }

    public float getFocusY() {
	return mFocusPointExternal.y;
    }

    public PointF getFocusDelta() {
	return mFocusDeltaExternal;
    }

}
