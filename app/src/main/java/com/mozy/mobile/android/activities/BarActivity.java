package com.mozy.mobile.android.activities;

import com.mozy.mobile.android.views.ErrorView;

import com.mozy.mobile.android.R;
import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.TextView;

public abstract class BarActivity extends Activity {

    /**
     * Bar flags
     */
    protected static final int SIMPLE_BAR = 0;
    protected static final int SEARCH_BAR = 1;
    protected static final int NO_BAR = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    protected synchronized void setContentView(int bar_flag, int res_id) {
        switch (bar_flag) {
        case SIMPLE_BAR:
            super.setContentView(R.layout.bar_activity_layout);
            break;
        case SEARCH_BAR:
            super.setContentView(R.layout.search_bar_activity_layout);
            break;
        case NO_BAR:
            super.setContentView(R.layout.no_bar_activity_layout);
            break;
        default:
            super.setContentView(R.layout.bar_activity_layout);
            break;
        }
        ErrorView error_view = (ErrorView)findViewById(R.id.error_view);
        if (error_view != null) {
            error_view.removeViewAt(0);
            LayoutInflater inflater = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View view = inflater.inflate(res_id, null);
            error_view.addView(view, 0);
        }
    }

    @Override
    public synchronized void setContentView(int res_id) {
        super.setContentView(R.layout.bar_activity_layout);

        ErrorView error_view = (ErrorView)findViewById(R.id.error_view);
        if (error_view != null) {
            error_view.removeViewAt(0);
            LayoutInflater inflater = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View view = inflater.inflate(res_id, null);
            error_view.addView(view, 0);
        }
    }

    @Override
    public synchronized void setContentView(View view) {
        super.setContentView(R.layout.bar_activity_layout);

        ErrorView error_view = (ErrorView)findViewById(R.id.error_view);
        if (error_view != null) {
            error_view.removeViewAt(0);
            error_view.addView(view, 0);
        }
    }

    @Override
    public synchronized void setContentView(View view, LayoutParams params) {
        super.setContentView(R.layout.bar_activity_layout);

        ErrorView error_view = (ErrorView)findViewById(R.id.error_view);
        if (error_view != null) {
            error_view.removeViewAt(0);
            error_view.addView(view, 0, params);
        }
    }

    protected void setBarTitle(int res_id) {
        TextView text_view = (TextView)findViewById(R.id.activity_title);
        if (text_view != null) {
            text_view.setText(res_id);
        }
    }

    protected void setBarTitle(String title) {
        TextView text_view = (TextView)findViewById(R.id.activity_title);
        if (text_view != null) {
            text_view.setText(title);
        }
    }

    protected void displayError(int res_id) {
        ErrorView error_view = (ErrorView)findViewById(R.id.error_view);
        if (error_view != null) {
            error_view.displayError(res_id);
        }
    }
    
    protected void displayError(CharSequence cs) {
        ErrorView error_view = (ErrorView)findViewById(R.id.error_view);
        if (error_view != null) {
            error_view.displayError(cs);
        }
    }

    protected void hideError() {
        ErrorView error_view = (ErrorView)findViewById(R.id.error_view);
        if (error_view != null) {
            error_view.hideError();
        }
    }
    
    /**
     * Warning! Should only be set in onCreate.
     * @param errorOnTop Flag to display error on top of content layout.
     */
    protected void setErrorOnTop(boolean errorOnTop) {
        ErrorView error_view = (ErrorView)findViewById(R.id.error_view);
        if (error_view != null) {
            error_view.setErrorOnTop(errorOnTop);
        }
    }
}
