package com.sousoum.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PointF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.LinearLayout;

import com.sousoum.droneswear.R;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by d.bertrand on 05/02/16.
 */
public class JoystickView extends LinearLayout {
    private static final String TAG = "JoystickView";

    public interface JoystickListener {
        void onValuesUpdated(float percentX, float percentY);
    }

    private List<JoystickListener> mListeners;

    private static final float CIRCLE_EXTERNAL_PADDING = 40; // the circle diameter = view.width - CIRCLE_EXTERNAL_PADDING
    private static final float JOYSTICK_RADIUS = 30;

    private Paint mStrokePaint;
    private Paint mFillPaint;

    private PointF mCenter;
    private PointF mJoystick;
    private float mRadius;
    private float mJoystickRadius;
    private float mJoystickCenterRadius;    // mRadius - mJoystickRadius. only here to avoid calculation

    public JoystickView(Context context) {
        super(context);
        customInit();
    }

    public JoystickView(Context context, AttributeSet attrs) {
        super(context, attrs);
        customInit();
    }

    public JoystickView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        customInit();
    }

    private void customInit() {
        mStrokePaint = new Paint();
        mStrokePaint.setStyle(Paint.Style.STROKE);
        mStrokePaint.setColor(getResources().getColor(R.color.primary_dark));
        mStrokePaint.setStrokeWidth(10);
        mStrokePaint.setAntiAlias(true);

        mFillPaint = new Paint();
        mFillPaint.setStyle(Paint.Style.FILL);
        mFillPaint.setColor(getResources().getColor(R.color.primary));
        mFillPaint.setAntiAlias(true);

        mCenter = new PointF(0, 0);
        mJoystick = new PointF(0, 0);
        mJoystickRadius = JOYSTICK_RADIUS;
        mRadius = 0;

        mListeners = new ArrayList<>();

        this.setWillNotDraw(false);
    }

    private void setJoystickPosition(float x, float y) {
        mJoystick.x = x;
        mJoystick.y = y;
        invalidate();
        notifyValuesUpdated();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        mCenter.x = getMeasuredWidth() / 2.f;
        mCenter.y = getMeasuredHeight() / 2.f;
        mJoystick.x = mCenter.x;
        mJoystick.y = mCenter.y;
        mRadius = (getMeasuredWidth() / 2.f) - CIRCLE_EXTERNAL_PADDING;

        mJoystickCenterRadius = mRadius - mJoystickRadius;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        canvas.drawCircle(mJoystick.x, mJoystick.y, mJoystickRadius, mFillPaint);
        canvas.drawCircle(mCenter.x, mCenter.y, mRadius, mStrokePaint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        boolean result = true;

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                if(isPointInsideCircle(event.getX(), event.getY())) {
                    setJoystickPosition(event.getX(), event.getY());
                } else {
                    // if pointed down outside the circle, don't do anything
                    result = false;
                }

                break;
            case MotionEvent.ACTION_MOVE:
                if(isPointInsideCircle(event.getX(), event.getY())) {
                    setJoystickPosition(event.getX(), event.getY());
                } else {
                    // if pointed down outside the circle, put the point on the edge of the circle
                    float vX = event.getX() - mCenter.x;
                    float vY = event.getY() - mCenter.y;
                    float magV = (float) Math.sqrt(vX * vX + vY * vY);
                    setJoystickPosition(
                            mCenter.x + vX / magV * mJoystickCenterRadius,
                            mCenter.y + vY / magV * mJoystickCenterRadius);
                }

                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                // put the joystick back on the center
                setJoystickPosition(mCenter.x, mCenter.y);
                break;
            default:
                result = false;
        }

        return result;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        boolean shouldDisableParentScroll = isPointInsideCircle(event.getX(), event.getY());

        this.getParent().requestDisallowInterceptTouchEvent(shouldDisableParentScroll);
        return super.onInterceptTouchEvent(event);
    }

    private boolean isPointInsideCircle(float x, float y) {
        return (Math.pow(x - mCenter.x, 2) + Math.pow(y - mCenter.y, 2) < Math.pow(mJoystickCenterRadius, 2));
    }

    public void addListener(JoystickListener listener) {
        mListeners.add(listener);
    }

    public void removeListener(JoystickListener listener) {
        mListeners.remove(listener);
    }

    private void notifyValuesUpdated() {
        float percentX = (mJoystick.x - mCenter.x) / mJoystickCenterRadius;
        float percentY = -(mJoystick.y - mCenter.y) / mJoystickCenterRadius;
        List<JoystickListener> listenersCpy = new ArrayList<>(mListeners);
        for (JoystickListener listener : listenersCpy) {
            listener.onValuesUpdated(percentX, percentY);
        }
    }
}
