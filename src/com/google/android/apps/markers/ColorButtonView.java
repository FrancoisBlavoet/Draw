package com.google.android.apps.markers;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;

public class ColorButtonView extends View {

    private Paint mPointerColor = new Paint(Paint.ANTI_ALIAS_FLAG);
    private RectF mColorWheelRectangle = new RectF();
    int mColorWheelRadius = 100;
    private int mColor = Color.GRAY;

    public ColorButtonView(Context context) {
	super(context);
    }

    public ColorButtonView(Context context, AttributeSet attrs) {
	super(context, attrs);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
	int height = getDefaultSize(getSuggestedMinimumHeight(),
		heightMeasureSpec);
	height = Math.min(height, mColorWheelRadius + getPaddingTop() + getPaddingBottom());
	int width = getDefaultSize(getSuggestedMinimumWidth(), widthMeasureSpec);
	width = Math.min(width, mColorWheelRadius + getPaddingLeft() + getPaddingRight());
	setMeasuredDimension(width, height);
    }

    @Override
    protected void onDraw(Canvas canvas) {
	// canvas.translate(mColorWheelRadius/2, mColorWheelRadius/2);
	mPointerColor.setColor(mColor);
	mColorWheelRectangle.set(getLeft() + this.getPaddingLeft(),
				 getTop()  + this.getPaddingTop(),
				 getLeft() + mColorWheelRadius + this.getPaddingRight(),
				 getTop()  + mColorWheelRadius + this.getPaddingBottom());
	
	canvas.drawOval(mColorWheelRectangle, mPointerColor);

    }
    
    public void setColor(int color)
    {
	mColor = color;
	invalidate();
    }

}
