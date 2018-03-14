package com.mozy.mobile.android.activities.startup;


import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.view.View.OnLongClickListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;

import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import com.mozy.mobile.android.R;
import com.mozy.mobile.android.provisioning.Provisioning;
import com.mozy.mobile.android.utils.LogUtil;
import com.mozy.mobile.android.web.containers.ListDownload;


public class SignIn extends SignInActivity
{
    static public final int DIALOG_PROGRESS = 101;
    private static final int INVALID_SIGNIN_TIMEOUT = 1000 * 60 * 10; // 10 minutes
    private EditText m_eTextPassword;
    private AutoCompleteTextView m_eTextEmailId;
    private View m_ViewForgotPassword;
    private ArrayAdapter<String> adapter;

    private final String TAG = getClass().getSimpleName();
    private static final int SIGNIN_RETRY_MAX = 4; //counter begins with 0 so essentially lock out after 5th attempt

    private boolean handlingForgotPassword = false;
    private boolean handlingNextButton = false;
    private AlertDialog alertLock = null;
    
    protected int signinRetry = 0;


    
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.signinpage_layout);

        m_textErrorMessage = (TextView)findViewById(R.id.signinErrorMessageText);
        m_eTextEmailId = (AutoCompleteTextView)findViewById(R.id.signInEmailIdEditTextBox);
        m_eTextPassword = (EditText)findViewById(R.id.signInPasswordEditTextBox);
        final Button buttonNext = (Button) findViewById(R.id.signInNextButton);
       
        View openingFocusView = m_eTextEmailId;
        
        m_eTextEmailId.setOnItemSelectedListener(new HandleOnSelectedEmail()); 
        
        adapter = new EmailIdListAdapter(this, R.layout.dropdown_layout);
        m_eTextEmailId.setAdapter(adapter);

        // Item to have initial focus
        openingFocusView.requestFocus();

        // Make the Enter key on the password also start the login process.
        m_eTextPassword.setOnKeyListener(new HandleKeyListenerForPassword());


        // handles forgot password link
        m_ViewForgotPassword = findViewById(R.id.signin_mozy_password_forgot);
        handlePasswordForgotLink(m_ViewForgotPassword);

        // handles Next Button
        handleNext(buttonNext);
           
        SignIn.handleForCredsExists(this);
    }
    

    /**
     * @param buttonNext
     */
    protected void handleNext(final Button buttonNext) {
        buttonNext.setOnClickListener(new OnClickListener()
            {

                @Override
                public void onClick(View view)
                {
                    SignIn.this.handleNextButton(view);
                }
            });

        buttonNext.setOnLongClickListener(new OnLongClickListener()
        {
            @Override
            public boolean onLongClick(View view)
            {
                SignIn.this.handleNextButton(view);
                return true;
            }
        });
    }

    /**
     * 
     */
    protected void handlePasswordForgotLink(View viewForgotPassword) {
        viewForgotPassword.setOnClickListener(new OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                SignIn.this.handleForgotPassword(v);
            }
        });
        viewForgotPassword.setOnLongClickListener(new OnLongClickListener()
        {
            @Override
            public boolean onLongClick(View v)
            {
                SignIn.this.handleForgotPassword(v);
                return true;
            }
        });
    }

    public void onConfigurationChanged(Configuration newConfig)
    {
        super.onConfigurationChanged(newConfig);
        if (newConfig.hardKeyboardHidden == Configuration.HARDKEYBOARDHIDDEN_NO)
        {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        }
        else
        {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }
    }


    private String validateUserInput()
    {
        String strErrorMessage = validateEmailId();
        if(strErrorMessage.trim().length() != 0) return strErrorMessage;

        strErrorMessage = validatePassword();
        return strErrorMessage;
    }

    private String validatePassword()
    {

        String strPassWord = m_eTextPassword.getText().toString();

        return StartupHelper.validatePassword(strPassWord, getResources());
    }

    private String validateEmailId()
    {

        String strEmail = m_eTextEmailId.getText().toString();

        return StartupHelper.validateEmail(strEmail, getResources());
    }

    
    @Override
    protected void showHomeScreen(boolean authenticate)
    {
        try
        {
            if (authenticate)
            {
                String strPassWord = m_eTextPassword.getText().toString().trim();
                String strEmail = m_eTextEmailId.getText().toString().trim();
                // Go ahead and do the
                showDialog(DIALOG_PROGRESS);
                new AuthenticationTask(strEmail, strPassWord).execute();
            }
            else
            {
                removeDialog(DIALOG_PROGRESS);
                boolean alreadyPrompted = Provisioning.getInstance(getApplicationContext()).getFirstRunPinPrompt();
                Provisioning.getInstance(getApplicationContext()).setFirstRunPinPrompt(alreadyPrompted);
                showPinPromptOrHomeScreen(alreadyPrompted);
            }
        }
        catch (Exception e)
        {
            Log.e(":::ERROR::", e.toString());
            Toast.makeText(this, e.toString(), Toast.LENGTH_LONG).show();
        }
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        
        if(requestCode == RequestCodes.HOME_SCREEN_ACTIVITY_RESULT || requestCode == RequestCodes.AUTHENTICATION_RESULT)
            // Re-enable next button processing
            this.handlingNextButton = false;
        
        if((requestCode == RequestCodes.HOME_SCREEN_ACTIVITY_RESULT || 
                requestCode ==  RequestCodes.AUTHENTICATION_RESULT) && resultCode == ServerAPI.RESULT_UNAUTHORIZED)
        {
            showErrorMessage(R.string.errormessage_authorization_failure_signin);
        }
        else
        {
            super.onActivityResult(requestCode, resultCode, data);
        }
        
        if((requestCode == RequestCodes.HOME_SCREEN_ACTIVITY_RESULT || requestCode == RequestCodes.AUTHENTICATION_RESULT) && resultCode == ServerAPI.RESULT_UNAUTHORIZED)
            lockApp(signinRetry++);
       
    }
    
 
    /**
     * 
     */
    @Override
    public void showErrorMessage(int msgId) {
        
        // Clear out the all text entry.
        m_eTextPassword.setText("");
        
        super.showErrorMessage(msgId);
        
        // Focus back to email text box
        View openingFocusView = m_eTextEmailId;
        if(openingFocusView != null)
            openingFocusView.requestFocus();
        
        removeDialog(DIALOG_PROGRESS);
    }


    private synchronized void handleNextButton(View v)
    {
        if (this.handlingNextButton == false)
        {
            try
            {
                // Dismiss the keyboard if it happens to be up. (It hides any error messaging.)
                InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(m_eTextPassword.getWindowToken(), 0);

                this.handlingNextButton = true;
                unSetErrorMessage();

                String strErrorMessage = validateUserInput();
                if(strErrorMessage.trim().length() != 0)
                {
                    LogUtil.debug(TAG, "Validating User input Failed:" + strErrorMessage);
                    setErrorMessageText(strErrorMessage);
                    this.handlingNextButton = false;

                    return;
                }

                SignIn.this.showHomeScreen(true);

                //Thread.currentThread().sleep(1000);
            }
            catch (Throwable t)
            {
                LogUtil.exception(this, "handleNextButton", t);
            }
        }
    }
    
    protected void lockApp(int signinRetryAttempts)
    {
        boolean lockApp = Provisioning.getInstance(getApplicationContext()).getSignInLock();

        if (lockApp || SIGNIN_RETRY_MAX <= signinRetryAttempts)
        {
            if (!lockApp)
                Provisioning.getInstance(getApplicationContext()).setSignInLock(true);

            // Clear out the password field.
            m_eTextPassword.setText("");

            // Warning error telling the user all about it.
            alertLock = new AlertDialog.Builder(this)
               .setCancelable(false)
               .setMessage(R.string.error_signin_too_many_times)
               .create();
            alertLock.setOnKeyListener(new DialogInterface.OnKeyListener() {
                @Override
                public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
                    // tell the system  we handled this so the os does nothing with it
                    return true;
                }
            });
            ScreenLockTask lock = new ScreenLockTask();
            lock.execute();
            alertLock.show();
         }
    }


    private synchronized void handleForgotPassword(View v)
    {
        if (this.handlingForgotPassword == false)
        {
            try
            {
                this.handlingForgotPassword = true;
                String strForgotPasswordUrl = getString(R.string.forgot_password_link);
                LogUtil.debug(TAG, "Forgot Password Launching the url:"+strForgotPasswordUrl);
                Intent myIntent = new Intent(Intent.ACTION_VIEW);
                myIntent.setData(Uri.parse(strForgotPasswordUrl));
                startActivity(myIntent);
                //Thread.currentThread().sleep(1000);
            }
            catch (Throwable t)
            {
                LogUtil.exception(this, "handleForgotPassword", t);
            }
            finally
            {
                this.handlingForgotPassword = false;
            }
        }
    }


    @Override
    protected Dialog onCreateDialog(int id) 
    {
        if(id == DIALOG_PROGRESS)
        {
            ProgressDialog loadingDialog = new ProgressDialog(this);
            loadingDialog.setMessage(getResources().getString( R.string.mozy_authentication_progress_dialog_text));
            loadingDialog.setIndeterminate(true);
            return loadingDialog;
        }
        
        return super.onCreateDialog(id);
    }
  

    private final class HandleKeyListenerForPassword implements OnKeyListener {
        public boolean onKey(View view, int keyCode, KeyEvent event){
            if (event.getAction()== KeyEvent.ACTION_UP)
            {
                if (keyCode == KeyEvent.KEYCODE_ENTER)
                {
                    SignIn.this.handleNextButton(view);
                    // Still want default handling to happen (ie dismissal)
                    return false;
                }
            }
            return false;
        }
    }

    private final class HandleOnSelectedEmail implements OnItemSelectedListener {
        @Override
        public void onItemSelected(AdapterView<?> arg0, View view, int position, long id) 
        {
            m_eTextEmailId.setText(adapter.getItem(position));
            View openingFocusView = m_eTextEmailId;
            openingFocusView.requestFocus();
        }

        @Override
        public void onNothingSelected(AdapterView<?> arg0)
        {

        }
    }
    

    private class ScreenLockTask extends AsyncTask<Void, Void, java.lang.Boolean> {
        /* (non-Javadoc)
         * @see android.os.AsyncTask#doInBackground(Params[])
         */

        private final Object waitLock = new Object();

        @Override
        protected Boolean doInBackground(Void... params) {
            synchronized (waitLock)
            {
                try
                {
                    this.waitLock.wait(INVALID_SIGNIN_TIMEOUT);
                 }
                catch (InterruptedException e)
                {
                }
            }
            return true;
        }

        public void onPostExecute(Boolean result) {
            alertLock.dismiss();
            alertLock = null;
            signinRetry = 0;
            Provisioning.getInstance(getApplicationContext()).setSignInLock(false);
        }
    }

    private class AuthenticationTask extends AsyncTask<Void, Void, java.lang.Boolean> {
        /* (non-Javadoc)
         * @see android.os.AsyncTask#doInBackground(Params[])
         */
        private int error = ServerAPI.RESULT_OK;
        String mUsername = null;
        String mPassword = null;
        ListDownload ld = null;
        
        
        AuthenticationTask(String username, String password)
        {
            this.mUsername = username;
            this.mPassword = password;
        }
        
        @Override
        protected Boolean doInBackground(Void... params) {
            ld = ServerAPI.getInstance(getApplicationContext()).authenticateUserWithCreds(this.mUsername, this.mPassword);
            error = ld.errorCode;
            if (error == ServerAPI.RESULT_OK)
            {
                ld = ServerAPI.getInstance(getApplicationContext()).getUserInfo();
                error = ld.errorCode;
            }
            
            if (error == ServerAPI.RESULT_OK)
                signinRetry = 0;
                
            return true;
        }
        
        public void onPostExecute(Boolean result) {

            onActivityResult(RequestCodes.AUTHENTICATION_RESULT, error, null);
        }

    }

    
    
    class EmailIdListAdapter extends ArrayAdapter<String>
    {
        private int resourceId;

        public EmailIdListAdapter(Context context, int resourceId) {
            super(context, resourceId);
            this.resourceId = resourceId;
            String [] emails = Provisioning.getInstance(getApplicationContext()).getAllEmailIds();
            
            this.clear();
            for(int i = 0; emails != null && i  < emails.length; i++)
            {
                this.add(emails[i]);
            }
        }
        
        @Override
        public View getView(int position, View convertView, ViewGroup parent)
        {
            final LayoutInflater inflater = LayoutInflater.from(getContext());
            final TextView view = (TextView) inflater.inflate(this.resourceId, parent, false);
            view.setTextColor(getResources().getColor(R.color.main_text_color));  // also useful if you have a color scheme that makes the text show up white
            String data = this.getItem(position);
            view.setText(data);
            return view;
        }
    }
}
