package com.mozy.mobile.android.views;

import com.mozy.mobile.android.R;
import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.Scroller;

public class DownloadMoreView extends FrameLayout {
    private final static int STATE_IDLE = 0;
    private final static int STATE_EXPAND = 1;
    private final static int STATE_COLLAPSE = 2;
    private final static int STATE_COLLAPSING = 3;
    private final static int STATE_EXPANDING = 4;
    
    private Scroller mScroller;
    
    private int mState = STATE_IDLE;
    private int nextState = STATE_IDLE;
    private int currentYDelta = 0;
    
    
    public DownloadMoreView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mScroller = new Scroller(context);
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
        // layout the content
        final View content = getChildAt(0);
        final View downloadMore = getChildAt(1);
        int contentBottom = bottom - top;
        contentBottom = contentBottom > content.getMeasuredHeight() ? content.getMeasuredHeight() : contentBottom;
        content.layout(0, 0, content.getMeasuredWidth(), contentBottom);
        downloadMore.layout(0, contentBottom - currentYDelta, downloadMore.getMeasuredWidth(), contentBottom + downloadMore.getMeasuredHeight() - currentYDelta);
        
        // start the animations
        if(mState == STATE_EXPAND) {
            mScroller.startScroll(0, 0, 0, downloadMore.getMeasuredHeight(), 500);
            mState = STATE_EXPANDING;
        }
        else if(mState == STATE_COLLAPSE) {
            mScroller.startScroll(0, downloadMore.getMeasuredHeight(), 0, -downloadMore.getMeasuredHeight(), 500);
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
            
            final int y = mScroller.getCurrY();
            final int delta = y-currentYDelta;
            currentYDelta = y;
            
            error.offsetTopAndBottom(-delta);
            
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
    
    public void setButtonOnClickListener(OnClickListener listener) {
        Button button = (Button)getChildAt(1).findViewById(R.id.download_more_button);
        if (button != null) {
            button.setOnClickListener(listener);
        }
    }
    
    public View getGridView() {
        return getChildAt(0).findViewById(R.id.photo_gridview);
    }
    /**
     * Displays error message.
     * @param res_id resource id to error string.
     * @param onTop not implemented support for ontop error layout.
     */
    public synchronized void displayButton() {
        if(getChildAt(1).getVisibility() == View.GONE || mState == STATE_COLLAPSE || mState == STATE_COLLAPSING) {
            expand();
        }
    }
    
    public synchronized void hideButton() {
        if(getChildAt(1).getVisibility() != View.GONE) {
            collapse();
        }
    }
}
