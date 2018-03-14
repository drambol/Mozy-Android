package com.mozy.mobile.android.activities.startup;

import android.app.Activity;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.widget.Button;

import com.mozy.mobile.android.R;
import com.mozy.mobile.android.provisioning.Provisioning;

public class FirstRun extends Activity
{
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        Eula.show(this);
        
        super.onCreate(savedInstanceState);
        
        if(SignIn.handleForCredsExists(FirstRun.this) == false)
        {
            setContentView(R.layout.first_run_layout);
            
            this.getWindow().setFormat(PixelFormat.RGBA_8888);
            this.getWindow().addFlags(WindowManager.LayoutParams.FLAG_DITHER);
    
            final Button buttonSignIn = (Button) findViewById(R.id.signInButton);
            buttonSignIn.setOnClickListener(new OnClickListener()
            {
                @Override
                public void onClick(View v)
                {
                    startSignInActivity(SignIn.class);
                }
            });
            
            
            final Button buttonAltSignIn = (Button) findViewById(R.id.altSignInButton);
            buttonAltSignIn.setOnClickListener(new OnClickListener()
            {
                @Override
                public void onClick(View v)
                {
                    startSignInActivity( AltSignInSubDomain.class);
                }
            });
        }
    }
    
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        switch(requestCode)
        {
            case RequestCodes.HOME_SCREEN_ACTIVITY_RESULT:
            case RequestCodes.SIGNIN_SCREEN_ACTIVITY_RESULT:
            {
                switch (resultCode)
                {
                    case ResultCodes.HOME_SCREEN_BACKPRESSED:
                    case ResultCodes.CANCEL:
                        FirstRun.this.setResult(ResultCodes.CANCEL);
                        finish();
                        break;
                }
            }
            break;   
        }
    }
    
    
    
   // Fixes the crash Bitmaps on the previous activity layout are not properly deallocated by the garbage collector because they have crossed references to their activity. 
    @Override
    protected void onDestroy() 
    {
        super.onDestroy();
    
        unbindDrawables(findViewById(R.id.bloodyMainLinearLayout));
        System.gc();
    }
    
    //Removes callbacks on all the background drawables
    //Removes childs on every viewgroup

    private void unbindDrawables(View view) {
        if(view != null)
        {
            if (view.getBackground() != null) 
            {
                view.getBackground().setCallback(null);
            }
            if (view instanceof ViewGroup) 
            {
                for (int i = 0; i < ((ViewGroup) view).getChildCount(); i++) 
                {
                    unbindDrawables(((ViewGroup) view).getChildAt(i));
                }
                ((ViewGroup) view).removeAllViews();
            }
        }
    }


    /**
     * 
     */
    public void startSignInActivity(Class <?> signInclass) {
        Provisioning.getInstance(getApplicationContext()).setFirstRunShown(true);
        Intent intent = new Intent(getApplicationContext(), signInclass);
        if(intent != null)
            startActivityForResult(intent, RequestCodes.SIGNIN_SCREEN_ACTIVITY_RESULT);
    }
}
