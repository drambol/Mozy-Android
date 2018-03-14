package com.mozy.mobile.android.views;

import java.util.ArrayList;

import android.content.Context;
import android.database.DataSetObserver;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Scroller;

public class SlideView extends AdapterView<BaseAdapter> {
    public interface SlideViewListener {
        public void onNewItemSnapped(int position);
        public void onWrapAnimationStarted(boolean bEnd);
        public void onMoving();
        public void onActionDown();
    }

    private ItemRecycler mItemRecycler = new ItemRecycler();

    /* The user does nothing */
    private final static int STATE_IDLE = 0;

    /* The user moves his/her finger */
    private final static int STATE_SCROLLING = 1;

    /* The user has released/flinged */
    private final static int STATE_FLINGING = 2;

    private final static int STATE_WRAPPING_IN = 3;
    private final static int STATE_WRAPPING_OUT = 4;

    /* Used for animating the scrolling */
    private Scroller mScroller;

    /*
     * Used for determining the flinging velocity. Could probably use a
     * GestureDetector instead.
     */
    private VelocityTracker mVelocityTracker;

    /* The last x coordinate the user pointed at */
    private float mLastMotionX;

    /* The index of the current child being shown */
    protected int mCurrentChild = 0;

    /* The index of the first child, with respect to the adapter */
    protected int mCurrentStartIndex = 0;

    /* Android constant for the touch slop, so we don't scroll by accident */
    private int mTouchSlop;

    /* The current state */
    private int mState = STATE_IDLE;

    /* The previous scroll value, for animations */
    private int oldScrollOffset;

    private int mCurrentIndex;

    private BaseAdapter mAdapter;

    private SlideViewListener onNewItemSnappedListener;

    private DataSetObserver mDataSetObserver;

    public SlideView(Context context, AttributeSet attrs) {
        super(context, attrs);

        mScroller = new Scroller(getContext(), new DecelerateInterpolator(2));

        final ViewConfiguration configuration = ViewConfiguration
                .get(getContext());

        mTouchSlop = configuration.getScaledTouchSlop();
        mCurrentIndex = 0;
    }

    /**
     * Set a new interpolator to be used when flinging between the children.
     * 
     * @param interpolator
     *            The interpolator to use.
     */
    public void setInterpolator(Interpolator interpolator) {
        mScroller = new Scroller(getContext(), interpolator);
    }

    @Override
    public void setAdapter(BaseAdapter adapter) {
        removeAllViewsInLayout();

        this.mAdapter = adapter;

        mDataSetObserver = new DataSetObserver() {

            @Override
            public void onInvalidated() {
                int temp = 0;

                if(getChildCount() >= 1)
                    temp = getChildAt(mCurrentChild).getLeft();
                
                removeAllViewsInLayout();

                int startIndex = mCurrentStartIndex = Math.max(0,
                        mCurrentIndex - 1);
                int endIndex = Math.min(mCurrentIndex + 1,
                        mAdapter.getCount() - 1);

                mCurrentChild = mCurrentIndex - mCurrentStartIndex;

                for (int childIndex = startIndex; childIndex <= endIndex; ++childIndex) {
                    addViewLast();
                }

                if (getChildCount() > 0 && getChildAt(mCurrentChild) != null)
                    offsetChildren(temp - getChildAt(mCurrentChild).getLeft());
                requestLayout();
            }

            @Override
            public void onChanged() {
//                for (int i = 0; i < getChildCount(); i++) 
//                {
//                    final View v = getChildAt(i);
//                    removeViewInLayout(v);
//                    final View newView = mAdapter.getView(mCurrentStartIndex
//                            + i, v, SlideView.this);
//                    final int childWidth = MeasureSpec.makeMeasureSpec(
//                            getMeasuredWidth(), MeasureSpec.EXACTLY);
//
//                    final int childHeight = MeasureSpec.makeMeasureSpec(
//                            getMeasuredHeight(), MeasureSpec.EXACTLY);
//
//                    addViewInLayout(newView, i,
//                            new LayoutParams(LayoutParams.FILL_PARENT,
//                                    LayoutParams.FILL_PARENT), true);
//                    newView.measure(childWidth, childHeight);
//                    newView.layout(newView.getLeft(), newView.getTop(), newView
//                            .getLeft() + newView.getMeasuredWidth(), newView.getTop()
//                            + newView.getMeasuredHeight());
//                }
//                invalidate();
                int temp = 0;

                if(getChildCount() >= 1 && getChildAt(mCurrentChild) != null)
                    temp = getChildAt(mCurrentChild).getLeft();
                
                removeAllViewsInLayout();

                int startIndex = mCurrentStartIndex = Math.max(0,
                        mCurrentIndex - 1);
                int endIndex = Math.min(mCurrentIndex + 1,
                        mAdapter.getCount() - 1);

                mCurrentChild = mCurrentIndex - mCurrentStartIndex;

                for (int childIndex = startIndex; childIndex <= endIndex; ++childIndex) {
                    addViewLast();
                }

                if (getChildCount() > 0 && getChildAt(mCurrentChild) != null)
                    offsetChildren(temp - getChildAt(mCurrentChild).getLeft());
                requestLayout();
            }
        };

        mAdapter.registerDataSetObserver(mDataSetObserver);

        int startIndex = mCurrentStartIndex = Math.max(0, mCurrentIndex - 1);
        int endIndex = Math.min(mCurrentIndex + 1, mAdapter.getCount() - 1);

        mCurrentChild = mCurrentIndex - mCurrentStartIndex;

        for (int childIndex = startIndex; childIndex <= endIndex; ++childIndex) {
            addViewLast();
        }

        if (getChildCount() > 0)
            offsetChildren(-getChildAt(mCurrentChild).getLeft());
        requestLayout();
    }

    public void setSlideViewListener(SlideViewListener listener) {
        this.onNewItemSnappedListener = listener;
    }

    private void addViewLast() {
        final View lastChild = getChildAt(getChildCount() - 1);

        final View newView = mAdapter.getView(mCurrentStartIndex
                + getChildCount(), mItemRecycler.get(), SlideView.this);

        final int childWidth = MeasureSpec.makeMeasureSpec(getMeasuredWidth(),
                MeasureSpec.EXACTLY);

        final int childHeight = MeasureSpec.makeMeasureSpec(
                getMeasuredHeight(), MeasureSpec.EXACTLY);

        addViewInLayout(newView, getChildCount(), new LayoutParams(
                LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT), true);

        newView.measure(childWidth, childHeight);

        if (lastChild == null) {
            newView.layout(0, 0, newView.getMeasuredWidth(), newView
                    .getMeasuredHeight());
        } else {
            newView.layout(lastChild.getRight(), lastChild.getTop(), lastChild
                    .getRight()
                    + newView.getMeasuredWidth(), lastChild.getTop()
                    + newView.getMeasuredHeight());
        }
    }

    private void addViewFirst() {
        final View firstChild = getChildAt(0);

        final View newView = mAdapter.getView(--mCurrentStartIndex,
                mItemRecycler.get(), SlideView.this);

        final int childWidth = MeasureSpec.makeMeasureSpec(getMeasuredWidth(),
                MeasureSpec.EXACTLY);

        final int childHeight = MeasureSpec.makeMeasureSpec(
                getMeasuredHeight(), MeasureSpec.EXACTLY);

        addViewInLayout(newView, 0, new LayoutParams(LayoutParams.FILL_PARENT,
                LayoutParams.FILL_PARENT), true);
        newView.measure(childWidth, childHeight);

        if (firstChild == null)
            newView.layout(0, 0, newView.getMeasuredWidth(), newView
                    .getMeasuredHeight());
        else {
            newView.layout(firstChild.getLeft() - newView.getMeasuredWidth(),
                    firstChild.getTop(), firstChild.getLeft(), firstChild
                            .getTop()
                            + newView.getMeasuredHeight());
        }
    }

    @Override
    public void onFinishInflate() {
        // Should probably center the middle child?
        if (getChildCount() > 0) {
            mCurrentChild = 0;
        }
    }

    @Override
    protected void onMeasure(int width, int height) {
        super.onMeasure(width, height);

        final int childWidth = MeasureSpec.makeMeasureSpec(getMeasuredWidth(),
                MeasureSpec.EXACTLY);

        final int childHeight = MeasureSpec.makeMeasureSpec(
                getMeasuredHeight(), MeasureSpec.EXACTLY);

        measureChildren(childWidth, childHeight);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right,
            int bottom) {
        final View selected = getChildAt(mCurrentChild);
        if (selected != null) {
            selected
                    .layout(selected.getLeft(), 0, selected.getLeft()
                            + selected.getMeasuredWidth(), selected
                            .getMeasuredHeight());

            int l = selected.getLeft();
            for (int i = mCurrentChild - 1; i >= 0; i--) {
                l -= getChildAt(i).getMeasuredWidth();
                getChildAt(i).layout(l, 0,
                        l + getChildAt(i).getMeasuredWidth(),
                        getChildAt(i).getMeasuredHeight());
            }

            l = selected.getRight();

            for (int i = mCurrentChild + 1; i < getChildCount(); i++) {

                getChildAt(i).layout(l, 0,
                        l + getChildAt(i).getMeasuredWidth(),
                        getChildAt(i).getMeasuredHeight());
                l += getChildAt(i).getMeasuredWidth();
            }
        }
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        final int action = ev.getAction();
        final float x = ev.getX();

        switch (action) {
        case MotionEvent.ACTION_DOWN:
            mLastMotionX = x;
            if (!mScroller.isFinished()) {
                fillItUp();
                mScroller.abortAnimation();
                mState = STATE_SCROLLING;
                return true;
            }
            break;
        case MotionEvent.ACTION_MOVE:
            int deltaX = (int) (mLastMotionX - x);

            if (Math.abs(deltaX) > mTouchSlop) {
                // TODO we don't need the drawing caches for all children.
                setChildrenDrawingCacheEnabled(true);
                setChildrenDrawnWithCacheEnabled(true);
                mLastMotionX = x;
                mState = STATE_SCROLLING;
                return true;
            }

            break;
        }

        return false;
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        if (getChildCount() <= 0)
            return true;

        if (mVelocityTracker == null) {
            mVelocityTracker = VelocityTracker.obtain();
        }

        mVelocityTracker.addMovement(ev);

        final int action = ev.getAction();
        final float x = ev.getX();

        switch (action) {
        // What if the child is Not clickable? Initiate scroll then.
        case MotionEvent.ACTION_DOWN:
            mLastMotionX = x;
            if (!mScroller.isFinished()) {
                fillItUp();
                mScroller.abortAnimation();
                mState = STATE_SCROLLING;
                return true;
            }
            if (onNewItemSnappedListener != null) {
                onNewItemSnappedListener.onActionDown();
            }
            break;
        case MotionEvent.ACTION_MOVE:
            if (onNewItemSnappedListener != null) {
                onNewItemSnappedListener.onMoving();
            }
            final int deltaX = (int) (mLastMotionX - x);

            mLastMotionX = x;

            if ((mCurrentChild == 0 && deltaX < 0 && getChildAt(0).getLeft() > 0)
                    || (mCurrentChild >= getChildCount() - 1 && deltaX > 0 && getChildAt(
                            getChildCount() - 1).getLeft() < 0)) {
                offsetChildren(-deltaX / 2);
            } else {
                offsetChildren(-deltaX);
            }
            invalidate();
            break;
        case MotionEvent.ACTION_UP:
            final View currentChild = getChildAt(mCurrentChild);

            final int closestChild = Math.min(Math.max(0, mCurrentChild
                    + (-currentChild.getLeft() << 1) / getMeasuredWidth()),
                    getChildCount() - 1);

            mVelocityTracker.computeCurrentVelocity(1000);

            if (mAdapter.getCount() > 1 && mCurrentChild == 0 && mVelocityTracker.getXVelocity() > 1300) {
                wrap(true);
            } else if (mAdapter.getCount() > 1 && mCurrentIndex == mAdapter.getCount() - 1
                    && mVelocityTracker.getXVelocity() < -1300) {
                wrap(false);
            } else if (mVelocityTracker.getXVelocity() > 100
                    && mCurrentChild > 0) {
                snapToChild(mCurrentChild - 1, mVelocityTracker.getXVelocity());
            } else if (mVelocityTracker.getXVelocity() < -100
                    && mCurrentChild < getChildCount() - 1) {
                snapToChild(mCurrentChild + 1, mVelocityTracker.getXVelocity());
            } else {
                snapToChild(closestChild);
            }

            if (mVelocityTracker != null) {
                mVelocityTracker.recycle();
                mVelocityTracker = null;
            }

            break;
        }

        return true;
    }

    protected void offsetChildren(int delta) {
        for (int i = 0; i < getChildCount(); getChildAt(i++)
                .offsetLeftAndRight(delta))
            ;
    }

    public boolean animateNext() {
        if (mState != STATE_IDLE) {
            return true;
        }

        if (mCurrentIndex < mAdapter.getCount() - 1) {
            snapToChild(mCurrentChild + 1);
            return true;
        } else {
            return false;
        }
    }

    public boolean animatePrevious() {
        if (mState != STATE_IDLE) {
            return true;
        }

        if (mCurrentIndex > 0) {
            snapToChild(mCurrentChild - 1);
            return true;
        } else {
            return false;
        }
    }

    private void snapToChild(int index) {
        snapToChild(index, 500);
    }

    private void snapToChild(int index, float velocity) {
        if (index < mCurrentChild) {
            mCurrentIndex--;
        } else if (index > mCurrentChild) {
            mCurrentIndex++;
        }

        final int startX = getChildAt(index).getLeft();

        int duration = (int) Math.abs(startX * 1000 / velocity);

        oldScrollOffset = startX;

        mScroller.startScroll(startX, 0, -startX, 0, duration);

        mCurrentChild = index;

        mState = STATE_FLINGING;

        invalidate();

        if (onNewItemSnappedListener != null) {
            onNewItemSnappedListener.onNewItemSnapped(mCurrentStartIndex
                    + mCurrentChild);
        }
    }

    public void wrap(boolean toEnd) {
        if (onNewItemSnappedListener != null) {
            onNewItemSnappedListener.onWrapAnimationStarted(!toEnd);
        }
        mState = STATE_WRAPPING_IN;
        final int startX = getChildAt(mCurrentChild).getLeft();
        oldScrollOffset = startX;

        mScroller.startScroll(startX, 0, (toEnd ? getMeasuredWidth() : -getMeasuredWidth()) - startX, 0, 800);
        invalidate();
    }

    @Override
    public void computeScroll() {
        if (mScroller.computeScrollOffset()) {
            int deltaX = mScroller.getCurrX() - oldScrollOffset;
            oldScrollOffset = mScroller.getCurrX();
            offsetChildren(deltaX);
            postInvalidate();
        } else {
            if (mState == STATE_WRAPPING_IN) {
                int offset = mCurrentIndex == 0 ? -getMeasuredWidth() : getMeasuredWidth();
                
                if (mCurrentIndex == 0) {
                    // Just to be sure we check that mCurrentStartIndex will be > 0. But we shouldnt be here
                    // in case there's only one element.
                    mCurrentStartIndex = Math.max(0, mAdapter.getCount() - 2);
                    mCurrentIndex = mAdapter.getCount() - 1;
                }
                else {
                    mCurrentStartIndex = 0;
                    mCurrentIndex = 0;
                }
                removeAllViewsInLayout();
                mDataSetObserver.onInvalidated();
                offsetChildren(offset);

                mScroller.startScroll(0, 0, -offset, 0, 1000);
                
                oldScrollOffset = 0;
                mState = STATE_WRAPPING_OUT;
                invalidate();
            } 
            else if (mState != STATE_SCROLLING && mState != STATE_IDLE) {
                if (mState == STATE_WRAPPING_OUT)
                {
                    if (onNewItemSnappedListener != null) {
                        onNewItemSnappedListener.onNewItemSnapped(mCurrentStartIndex
                                + mCurrentChild);
                    }
                }
                
                mState = STATE_IDLE;
                setChildrenDrawingCacheEnabled(false);
                setChildrenDrawnWithCacheEnabled(false);

                fillItUp();
            }
        }
    }

    private void fillItUp() {
        if (mCurrentChild == 0 && getChildCount() >= 3) {
            mItemRecycler.put(getChildAt(getChildCount() - 1));
            removeViews(getChildCount() - 1, 1);
        }
        if (mCurrentChild == getChildCount() - 1 && getChildCount() >= 3) {
            mCurrentChild--;
            mCurrentStartIndex++;

            mItemRecycler.put(getChildAt(0));
            removeViews(0, 1);
        }

        if (mCurrentChild == getChildCount() - 1) {
            if (mCurrentIndex < mAdapter.getCount() - 1)
                addViewLast();
        } else if (mCurrentChild == 0) {
            if (mCurrentIndex > 0) {
                mCurrentChild++;
                addViewFirst();
            }
        }
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        // Optimize for scrolling/flinging, we only need to draw the two visible
        // views.

        if (mState != STATE_IDLE) {
            for (int i = 0; i < getChildCount(); ++i) {
                final View child = getChildAt(i);

                if (child.getRight() >= 0
                        && child.getLeft() < getMeasuredWidth()) {
                    drawChild(canvas, child, getDrawingTime());
                }
            }
        } else {
            super.dispatchDraw(canvas);
        }

    }

    @Override
    public Object getItemAtPosition(int position) {
        if (position >= mCurrentStartIndex
                && position < mCurrentStartIndex + getChildCount()) {
            return getChildAt(position - mCurrentStartIndex);
        }
        return null;
    }

    @Override
    public BaseAdapter getAdapter() {
        return mAdapter;
    }

    @Override
    public View getSelectedView() {
        return getChildAt(mCurrentChild);
    }

    @Override
    public void setSelection(int position) {
        mCurrentIndex = position;
        if (mAdapter != null) {
            setAdapter(mAdapter);
        }
    }

    private class ItemRecycler {
        private ArrayList<View> mItems = new ArrayList<View>();

        public void put(View v) {
            mItems.add(v);
        }

        public View get() {
            return mItems.isEmpty() ? null : mItems.remove(0);
        }
    }
}
