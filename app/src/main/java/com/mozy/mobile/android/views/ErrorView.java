package com.mozy.mobile.android.views;

import com.mozy.mobile.android.R;
import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.Scroller;
import android.widget.TextView;

public class ErrorView extends FrameLayout {
    private final static int STATE_IDLE = 0;
    private final static int STATE_EXPAND = 1;
    private final static int STATE_COLLAPSE = 2;
    private final static int STATE_COLLAPSING = 3;
    private final static int STATE_EXPANDING = 4;
    
    private Scroller mScroller;
    
    private boolean onTop;
    
    private int mState = STATE_IDLE;
    private int nextState = STATE_IDLE;
    private int currentYDelta = 0;
    
    
    public ErrorView(Context context, AttributeSet attrs) {
        super(context, attrs);
        onTop = false;
        mScroller = new Scroller(context);
    }
    
    public void setErrorOnTop(boolean isErrorOnTop) {
        if (onTop != isErrorOnTop) {
            onTop = isErrorOnTop;
            postInvalidate();
        }
    }
    
    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        getChildAt(1).setVisibility(View.GONE);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int titleHeight = 0;
        if (getChildCount() > 2) {
            // measure the title
            measureChild(getChildAt(2), widthMeasureSpec, heightMeasureSpec);
            
            titleHeight = getChildAt(2).getMeasuredHeight();
        }
        
        // measure the error message
        measureChild(getChildAt(1), widthMeasureSpec, heightMeasureSpec);
        
        // measure the content, with respect to the size of the title
        final int contentWidthMS = MeasureSpec.getSize(heightMeasureSpec) - titleHeight;
        measureChild(getChildAt(0), widthMeasureSpec, MeasureSpec.makeMeasureSpec(contentWidthMS, MeasureSpec.getMode(heightMeasureSpec)));
        
        // set our size
        setMeasuredDimension(MeasureSpec.getSize(widthMeasureSpec), MeasureSpec.getSize(heightMeasureSpec));
    }
    
    @Override
    protected void onLayout(boolean changed, int left, int top, int right,
            int bottom) {
        int currentTop = 0;
        if (getChildCount() > 2) {
            // layout the title
            final View title = getChildAt(2);
            title.layout(0, 0, title.getMeasuredWidth(), title.getMeasuredHeight());
            currentTop += title.getMeasuredHeight();
        }
        
        // layout the error message
        final View error = getChildAt(1);
        if(error.getVisibility() == View.VISIBLE) {
            error.layout(0, currentTop + currentYDelta, error.getMeasuredWidth(), currentTop + currentYDelta + error.getMeasuredHeight());
            if (!onTop) {
                currentTop += error.getMeasuredHeight();
            }
        }
        
        // layout the content
        final View content = getChildAt(0);
        content.layout(0, currentTop, content.getMeasuredWidth(), currentTop + content.getMeasuredHeight());
        
        // start the animations
        if(mState == STATE_EXPAND) {
            mScroller.startScroll(0, -error.getMeasuredHeight(), 0, error.getMeasuredHeight(), 500);
            mState = STATE_EXPANDING;
        }
        else if(mState == STATE_COLLAPSE) {
            mScroller.startScroll(0, 0, 0, -error.getMeasuredHeight(), 500);
            mState = STATE_COLLAPSING;
            invalidate();
        }
    }
    
    private void expand() {
        if (mState == STATE_IDLE) {
            getChildAt(1).setVisibility(View.VISIBLE);
            mState = STATE_EXPAND;
        } else if (mState != STATE_EXPAND && mState != STATE_EXPANDING) {
            nextState = STATE_EXPAND;
        }
    }
    
    private void collapse() {
        if (mState == STATE_IDLE) {
            mState = STATE_COLLAPSE;
            requestLayout();
        } else if (mState != STATE_COLLAPSE && mState != STATE_COLLAPSING) {
            nextState = STATE_COLLAPSE;
        }
    }
    
    @Override
    public void computeScroll() {
        if(mScroller.computeScrollOffset()) {
            final View error = getChildAt(1);
            final View content = getChildAt(0);
            
            final int y = mScroller.getCurrY();
            final int delta = y-currentYDelta;
            currentYDelta = y;
            
            error.offsetTopAndBottom(delta);
            if (!onTop) {
                content.offsetTopAndBottom(delta);
            }
            
            invalidate();

            if(mScroller.isFinished()) {
                if(mState == STATE_COLLAPSE || mState == STATE_COLLAPSING) {
                    error.setVisibility(View.GONE);
                }
                mState = STATE_IDLE;
                if (nextState != STATE_IDLE) {
                    int state = nextState;
                    nextState = STATE_IDLE;
                    if (state == STATE_COLLAPSE) {
                        collapse();
                    } else if (state == STATE_EXPAND) {
                        expand();
                    }
                }
            }
        }
    }
    
    /**
     * Displays error message.
     * @param res_id resource id to error string.
     * @param onTop not implemented support for ontop error layout.
     */
    public synchronized void displayError(int res_id) {
        TextView text_view = (TextView)findViewById(R.id.error_text);
        if (text_view != null) {
            text_view.setText(res_id);
        }
        if(getChildAt(1).getVisibility() == View.GONE || mState == STATE_COLLAPSE || mState == STATE_COLLAPSING) {
            expand();
        }
    }
    
    /**
     * Displays error message.
     * @param res_id resource id to error string.
     * @param onTop not implemented support for ontop error layout.
     */
    public synchronized void displayError(CharSequence cs) {
        TextView text_view = (TextView)findViewById(R.id.error_text);
        if (text_view != null) {
            text_view.setText(cs);
        }
        if(getChildAt(1).getVisibility() == View.GONE || mState == STATE_COLLAPSE || mState == STATE_COLLAPSING) {
            expand();
        }
    }
    
    public synchronized void hideError() {
        if(getChildAt(1).getVisibility() != View.GONE) {
            collapse();
        }
    }
}
