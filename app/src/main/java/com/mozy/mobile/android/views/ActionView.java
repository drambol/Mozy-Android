package com.mozy.mobile.android.views;

import java.util.Vector;

import android.content.Context;
import android.os.CountDownTimer;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.Button;
import android.widget.LinearLayout;

import com.mozy.mobile.android.R;

/**
 * <code>ActionView</code> is a fading options panel. Clients can register
 * listeners to receive click events. The panel will fade out if it does not
 * receive notifications that there has been some activity. Calling
 * notifyAction() resets the fade-out timer.
 *
 * @author Petter Lidén
 *
 */
public class ActionView extends LinearLayout implements OnClickListener
{
    public static final int ACTION_DOWNLOAD = 1;
    public static final int ACTION_SHARE = 2;
    public static final int ACTION_DELETE = 3;

    private static final String TAG = ActionView.class.getSimpleName();

    /**
     * Share option
     */
    private Button mShareButton;

    /**
     * Download option
     */
    private Button mDownloadButton;

    /**
     * Delete option
     */
    private Button mDeleteButton;

    /**
     * Time before fade-out if no action notification is received
     */
    private long TIMEOUT_FADE_OUT = 3000;

    /**
     * The duration of the fading itself
     */
    private long DURATION_FADE = 500;

    /**
     * Fade-out timer
     */
    private CountDownTimer mFadeOutTimer;

    /**
     * Registered listeners
     */
    private Vector<OnClickListener> listeners = new Vector<OnClickListener>();

    public ActionView(Context context)
    {
        super(context);
    }

    public ActionView(Context context, AttributeSet attrs)
    {
        super(context, attrs);
    }

    @Override
    public void onFinishInflate()
    {
        mShareButton = (Button) findViewById(R.id.button_export);
        mDownloadButton = (Button) findViewById(R.id.button_download);
        mDeleteButton = (Button) findViewById(R.id.button_delete);

        mShareButton.setOnClickListener(this);
        mDownloadButton.setOnClickListener(this);
        mDeleteButton.setOnClickListener(this);

        super.onFinishInflate();
    }

    /**
     * Initialize the view
     */
    public void init()
    {
        mFadeOutTimer = new CountDownTimer(TIMEOUT_FADE_OUT, 500)
        {
            @Override
            public void onTick(long millisUntilFinished) { }

            @Override
            public void onFinish()
            {
                fadeOut();
            }
        }.start();
    }

    /**
     * Notify that there has been some activity and fade-out should be
     * postponed
     */
    public void notifyAction()
    {
        if(mFadeOutTimer != null)
        {
            Log.d(TAG, "***** Got action, resetting timer..");
            mFadeOutTimer.cancel();
            mFadeOutTimer.start();
        }

        if(getVisibility() == View.INVISIBLE)
        {
            fadeIn();
        }
    }

    /*
     * (non-Javadoc)
     * @see android.view.View.OnClickListener#onClick(android.view.View)
     */
    public void onClick(View view)
    {
        fireClickEvent(view);
    }

    /**
     * Add a listener to the view
     *
     * @param listener The listener to receive click events
     */
    public void addOnClickListener(OnClickListener listener)
    {
        listeners.add(listener);
    }

    /**
     * Cause the view to fade out
     */
    private void fadeOut()
    {
        Log.d(TAG, "***** Fade-out triggered");

        Animation animation = new AlphaAnimation(1, 0);
        animation.setDuration(DURATION_FADE);
        startAnimation(animation);
        setVisibility(View.INVISIBLE);
    }

    /**
     * Cause the view to fade in
     */
    private void fadeIn()
    {
        Log.d(TAG, "***** Fade-in triggered");

        Animation animation = new AlphaAnimation(0, 1);
        animation.setDuration(DURATION_FADE);
        startAnimation(animation);
        setVisibility(View.VISIBLE);
    }

    /**
     * Fire events to listeners
     *
     * @param view The view that was clicked
     */
    private void fireClickEvent(View view)
    {
        for(OnClickListener listener : listeners)
        {
            if(view == mShareButton)
            {
                listener.exportClicked();
            }
            else if(view == mDownloadButton)
            {
                listener.HiResClicked();
            }
            else if(view == mDeleteButton)
            {
                listener.deleteClicked();
            }
        }
    }

    /**
     * Allow the main view to control which buttons are shown
     */
    public void showAction(int action, boolean show)
    {
        Button actionButton = null;
        switch (action)
        {
            case ACTION_SHARE:
                actionButton = mShareButton;
                break;
            case ACTION_DELETE:
                actionButton = mDeleteButton;
                break;
            case ACTION_DOWNLOAD:
                actionButton = mDownloadButton;
                break;
        }

        if (actionButton != null)
        {
            actionButton.setVisibility(show ? View.VISIBLE : View.GONE);
        }
    }

    /**
     * Interface that listeners must implement
     */
    public interface OnClickListener
    {
        /**
         * Share option was clicked
         */
        public void exportClicked();

        /**
         * Download options was clicked
         */
        public void HiResClicked();

        /**
         * Delete option was clicked
         */
        public void deleteClicked();
    }
}
