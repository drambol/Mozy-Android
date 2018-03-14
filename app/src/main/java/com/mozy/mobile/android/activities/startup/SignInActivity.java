package com.mozy.mobile.android.activities.startup;


import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.widget.TextView;
import com.mozy.mobile.android.R;
import com.mozy.mobile.android.activities.MainSettingsActivity;
import com.mozy.mobile.android.activities.NavigationTabActivity;
import com.mozy.mobile.android.activities.PinManActivity;
import com.mozy.mobile.android.provisioning.Provisioning;
import com.mozy.mobile.android.security.MzCryptoLibAPI;
import com.mozy.mobile.android.utils.FileUtils;
import com.mozy.mobile.android.utils.LogUtil;
import com.mozy.mobile.android.utils.SystemState;
import com.mozy.mobile.android.web.MipAPI;
import com.mozy.mobile.android.web.MipAuthAPI;
import com.mozy.mobile.android.web.uploadFileAPI;


public abstract class SignInActivity extends Activity
{
    protected TextView m_textErrorMessage;
    protected final String TAG = getClass().getSimpleName();

    
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
    }
    
    
  public static boolean handleForCredsExists(Activity activity) {
        
        boolean status = false;
        
        boolean lock = Provisioning.getInstance(activity.getApplicationContext()).getSignInLock();
        
        String token = Provisioning.getInstance(activity).getMipAccountToken();
        
        String tokenSecret = Provisioning.getInstance(activity).getMipAccountTokenSecret();
    
        if((token != null && token.length() != 0 && tokenSecret != null && tokenSecret.length() != 0) && !lock)
        {
            LogUtil.debug(activity, "Credentials already available, continuing to homescreen...");
            
            Intent intent = new Intent(activity, NavigationTabActivity.class);
            activity.startActivityForResult(intent, RequestCodes.HOME_SCREEN_ACTIVITY_RESULT);
            status = true;
        }
        
        return status;

    }


    /**
     * 
     */
    protected void showPinPromptOrHomeScreen(boolean alreadyPinPrompted) {

        if (alreadyPinPrompted == false)
        {    
            // Give the user an option to skip PIN setting
            AlertDialog wantPINDialog = new AlertDialog.Builder(this)
                .setCancelable(false)
                .setTitle(R.string.enable_pin_dialog_title)
                .setMessage(R.string.enable_pin_dialog_message)
                //.setIcon(getResources().getDrawable(android.R.drawable.ic_dialog_info))
                .setPositiveButton(R.string.yes_button_text, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                        // Ask for the PIN
                        Intent intent = new Intent(getApplicationContext(), PinManActivity.class);
                        intent.putExtra(PinManActivity.ACTION, PinManActivity.ACTION_SETNEW);
                        startActivityForResult(intent, RequestCodes.PINENTRY_PAGE_REQUEST_CODE);
                    } })
                .setNegativeButton(R.string.no_button_text, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                        setForNoPassCodeAndProceedToHomeScreen();
                    } })
                .setOnKeyListener(new DialogInterface.OnKeyListener() {
                    @Override
                    public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
                        // tell the system  we handled this so the os does nothing with it
                        if (keyCode == KeyEvent.KEYCODE_SEARCH)
                            return true;

                        return false;
                    }
                })
                .create();
            wantPINDialog.show();
        }
        else
        {
            proceedToHomeScreen();
        }
    }
    

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        switch (requestCode)
        {
            case RequestCodes.HOME_SCREEN_ACTIVITY_RESULT:
            case RequestCodes.AUTHENTICATION_RESULT:
            {
                switch (resultCode)
                {
                     case ServerAPI.RESULT_UNAUTHORIZED:
                         showErrorMessage(R.string.errormessage_authorization_failure);
                         break;

                     case ServerAPI.RESULT_INVALID_PARTNER:
                         showErrorMessage(R.string.invalid_partner_message);                         
                         break;
                    case ServerAPI.RESULT_ACCOUNT_CONFLICT:
                        showErrorMessage(R.string.account_conflict_message); 
                         break;
                    case ServerAPI.RESULT_INVALID_CLIENT_VER:
                        showErrorMessage(R.string.client_upgrade_required); 
                        break;
                     case ServerAPI.RESULT_CONNECTION_FAILED:
                     case ServerAPI.RESULT_UNKNOWN_PARSER:
                     case ServerAPI.RESULT_UNKNOWN_ERROR:  
                         showErrorMessage(R.string.error_not_available);       
                         break;
                     case ResultCodes.HOME_SCREEN_BACKPRESSED:
                     {
                         this.setResult(ResultCodes.HOME_SCREEN_BACKPRESSED);
                         finish();
                         break;
                     }
                     case ServerAPI.RESULT_CERTIFICATE_INVALID: {
                    	 String msg = this.getString(R.string.invalid_certificate);
                    	 Bundle bundle=data.getExtras();  
                         String url=bundle.getString("URL");
                         
                    	 msg = msg.replace("$URL", url);
                    	 showErrorMessage(msg);
                    	 break;
                     }
                     default:
                        if (RequestCodes.AUTHENTICATION_RESULT == requestCode && ServerAPI.RESULT_OK == resultCode)
                        {
                            // No containers or sync available
                            if((SystemState.getDeviceList() == null 
                                  || (SystemState.getDeviceList() != null && SystemState.getDeviceList().size() == 0)))
                            {
                                showErrorMessage( R.string.no_containers);  
                            }
                            else
                            {
                                handleManagedKeyOrRegularAccount();
                            }
                        }
                        else
                        {
                            this.finish();
                        }
                         break;
                }
                break;
            }
            case RequestCodes.PINENTRY_PAGE_REQUEST_CODE:
            {           
                onActivityResultPinEntryPageReqCode(resultCode);
                break;
            }
        }
    }
    
    protected void showHomeScreen(boolean authenticate)
    {
        if(authenticate == false)
        {
            boolean alreadyPrompted = Provisioning.getInstance(getApplicationContext()).getFirstRunPinPrompt();
            Provisioning.getInstance(getApplicationContext()).setFirstRunPinPrompt(alreadyPrompted);
            showPinPromptOrHomeScreen(alreadyPrompted);
        }
    }
    
    
    protected void onActivityResultPinEntryPageReqCode(int resultCode)
    {
        if(resultCode == ResultCodes.PIN_NEWTWO)
        {
            Intent intent = new Intent(this.getApplicationContext(), PinManActivity.class);
            int pinAction = PinManActivity.ACTION_SETNEWTWO;
            intent.putExtra(PinManActivity.ACTION, pinAction);
            startActivityForResult(intent, RequestCodes.PINENTRY_PAGE_REQUEST_CODE);
        }
        else
        {
            switch(resultCode)
            {
                case ResultCodes.PIN_OK:
                    // New PIN set. Provisioning, etc. already handled in the setter.
                    // Don't do first run pin entry again
                    proceedToHomeScreen();
                    break;
                case ResultCodes.CANCEL:
                    setForNoPassCodeAndProceedToHomeScreen();
                    break;
            }
        }
    }


    /**
     * 
     */
    public void proceedToHomeScreen() 
    {
        if(SystemState.isFileSelectedForMozyUpload())
        {
            finish();
        }
        else
        {
            Intent intent = new Intent(this, NavigationTabActivity.class);
            startActivityForResult(intent, RequestCodes.HOME_SCREEN_ACTIVITY_RESULT);
        }
    }

    /**
     * 
     */
    protected void handleManagedKeyOrRegularAccount() {

        String managed_key_url = SystemState.getManagedKeyUrl();
        
        // No managed key account
       if(managed_key_url == null)
       {
           showErrorMessage(R.string.managed_key_empty);
       }
       else if (managed_key_url.equalsIgnoreCase("") == true)
       {
           // Done with signin
           showHomeScreen(false);
       }
       else
       {
           try
           {
            MzCryptoLibAPI mzCryptoInstance = new MzCryptoLibAPI();
            byte[] urlData = FileUtils.readBytesFromUrl(managed_key_url);
            if(urlData == null)
            {
                showErrorMessage(R.string.managed_key_could_not_be_retrieved);
                MainSettingsActivity.cleanupOnSignedOut(this);
            }
            else
            {
                byte [] ckey = mzCryptoInstance.getCkey(urlData);
                if(ckey != null)
                {
                    SystemState.setManagedKey(getApplicationContext(),ckey);
                    showHomeScreen(false);
                }
                else
                {        
                    showErrorMessage(R.string.managed_key_failed_validation);
                    MainSettingsActivity.cleanupOnSignedOut(this);
                } 
            }
           }
           catch(RuntimeException e)
           {
               showErrorMessage(R.string.managed_key_failed_validation);
               MainSettingsActivity.cleanupOnSignedOut(this);
           }
        }
       
       // Reset the url we do not need beyond this point
       SystemState.setManagedKeyUrl(null);
    }

    /**
     * 
     */
    
    protected void showErrorMessage(int msgId) {

        // clear instance 
        MipAPI.setInstance(null);
        MipAuthAPI.setInstance(null);
        uploadFileAPI.setInstance(null);

        setErrorMessageText(this.getString(msgId));
    }
    
    protected void showErrorMessage(String msgstr) {

        // clear instance 
        MipAPI.setInstance(null);
        MipAuthAPI.setInstance(null);
        uploadFileAPI.setInstance(null);

        setErrorMessageText(msgstr);
    }

    protected void setErrorMessageText(String strErrorMessage)
    {
        this.m_textErrorMessage.setVisibility(View.VISIBLE);
        this.m_textErrorMessage.setText(strErrorMessage);
    }

    protected void unSetErrorMessage()
    {
        this.m_textErrorMessage.setText("");
        this.m_textErrorMessage.setVisibility(View.GONE);
    }

 
    /**
     * 
     */
    public void setForNoPassCodeAndProceedToHomeScreen() {
        /*
         * Write PIN to settings
         */
        Provisioning provisioning = Provisioning.getInstance(getApplicationContext());
        provisioning.setSecurityMode(false);
        provisioning.setPin("");
        
        proceedToHomeScreen();
        
    }

}
