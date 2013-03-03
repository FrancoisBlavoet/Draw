package com.google.android.apps.markers;

import org.dsandler.apps.markers.R;

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
    float mColorWheelRadius = getResources().getDimension(R.dimen.toolbar_color_button_diameter);
    private int mColor = Color.GRAY;
    float mDensity = getResources().getDisplayMetrics().density;

    public ColorButtonView(Context context) {
	super(context);
    }

    public ColorButtonView(Context context, AttributeSet attrs) {
	super(context, attrs);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
	int height = getDefaultSize(getSuggestedMinimumHeight(), heightMeasureSpec);
	height = (int) Math.min(height, mColorWheelRadius  + getPaddingTop() + getPaddingBottom());
	int width = getDefaultSize(getSuggestedMinimumWidth(), widthMeasureSpec);
	width = (int) Math.min(width, mColorWheelRadius + getPaddingLeft() + getPaddingRight());
	setMeasuredDimension(width, height);
    }

    @Override
    protected void onDraw(Canvas canvas) {
	// canvas.translate(mColorWheelRadius/2, mColorWheelRadius/2);
	mPointerColor.setColor(mColor);
	mColorWheelRectangle.set(this.getPaddingLeft(),
				 this.getPaddingTop(),
				 mColorWheelRadius + this.getPaddingRight() + this.getPaddingLeft(),
				 mColorWheelRadius  + this.getPaddingBottom() + this.getPaddingTop());
	
	canvas.drawOval(mColorWheelRectangle, mPointerColor);

    }
    
    public void setColor(int color)
    {
	mColor = color;
	invalidate();
    }

}
