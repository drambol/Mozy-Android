package com.mozy.mobile.android.activities;

import com.mozy.mobile.android.activities.startup.RequestCodes;
import com.mozy.mobile.android.activities.startup.ResultCodes;
import com.mozy.mobile.android.provisioning.Provisioning;
import com.mozy.mobile.android.utils.LogUtil;
import com.mozy.mobile.android.utils.SystemState;
import com.mozy.mobile.android.views.PinControl;

import com.mozy.mobile.android.R;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.widget.TextView;
import android.widget.Toast;

public class PinManActivity extends ErrorActivity {

    public static final String ACTION = "PinManAction";
    public static final int ACTION_VALIDATE = 1;
    // Special case validation: if password is used, reset the PIN.
    public static final int ACTION_VALIDATE_WITH_RESET = 2;
    public static final int ACTION_RESET = 3;
    public static final int ACTION_SETNEW = 4;
    public static final int ACTION_SETNEWTWO = 5;

    private static final int SIGNIN_RETRY_MAX = 6;
    private int signinRetry = 0;

    private enum PinManState {RESET_OLD, 
                                RESET_NEW, 
                                RESET_NEWTWO,
                                VALIDATE,
                                UNKNOWN};
    private static PinManState currentState = PinManState.UNKNOWN;
    private int actionRequested = 0;

    private PinControl pinControl;
    private  static String newPinString = "";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.pin_man_layout);

        pinControl = (PinControl)findViewById(R.id.pinEntryControl);
        pinControl.setOnPinReadyListener(new PinControl.OnPinReadyListener() 
        {
            @Override
            public void onPinReady()
            {
                handlePinReady();
            }
        });

        View forgotPin = (View) findViewById(R.id.pin_login_forgot_pin);
        forgotPin.setOnClickListener(new OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                onForgotPIN(false);
            }
        });

        PinManState startState = PinManState.RESET_OLD;
        
        if(currentState == PinManState.VALIDATE)
            startState = PinManState.VALIDATE;
        
        actionRequested = ACTION_RESET;
        Bundle extras = getIntent().getExtras();
        if (extras != null && extras.containsKey(ACTION))
        {
            actionRequested = extras.getInt(ACTION);
            switch (actionRequested)
            {
                case ACTION_VALIDATE:
                case ACTION_VALIDATE_WITH_RESET:
                    startState = PinManState.VALIDATE;
                    break;
                case ACTION_RESET:
                    startState = PinManState.RESET_OLD;
                    break;
                case ACTION_SETNEW:
                    startState = PinManState.RESET_NEW;
                    break;
                case ACTION_SETNEWTWO:
                    startState = PinManState.RESET_NEWTWO;
                    break;
                default:
                    LogUtil.error(this, "PinManActivity started with invalid action.");
                    break;
            }
        }
        showState(startState);
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    // Hitting the 'back' key  while in the pin lock screen will pop us out of the app
    public boolean onKeyDown(int keyCode, KeyEvent event)
    {
        if (keyCode == KeyEvent.KEYCODE_BACK && ACTION_VALIDATE_WITH_RESET == actionRequested)
        {
            moveTaskToBack(true);
        }
        return super.onKeyDown(keyCode, event);
    }

    private void showState(PinManState state)
    {
        if (currentState != state)
        {
            int forgotLinkVisiblity = View.GONE;
            int introTextString = R.string.form_field_currentpin_text;
            switch (state)
            {
                case RESET_OLD:
                    //findViewById(R.id.pin_login_forgot_pin).setVisibility(View.VISIBLE);
                    introTextString = R.string.form_field_currentpin_text;
                    break;
                case RESET_NEW:
                    introTextString = R.string.form_field_newpin_text;
                    break;
                case RESET_NEWTWO:
                    introTextString = R.string.form_field_confirm_newpin_text;
                    break;
                case VALIDATE:
                    introTextString = R.string.login_page_heading_text;
                    forgotLinkVisiblity = View.VISIBLE;
                    break;
            }
            ((TextView)findViewById(R.id.change_pin_intro)).setText(introTextString);
            findViewById(R.id.pin_login_forgot_pin).setVisibility(forgotLinkVisiblity);
            pinControl.clearPin();
            currentState = state;
        }
    }

    private void handlePinReady()
    {
        String pin = pinControl.getPin();
        // Generic error checking
        if (pin.length() == 0 )
        {
            showError(R.string.errormessage_pins_pin_empty);
        }
        else if (pin.length() != 4)
        {
            showError(R.string.errormessage_pins_pin_not_4digit);
        }
        else
        {
            switch (currentState)
            {
                case RESET_OLD:
                case VALIDATE:
                    // Validate current PIN
                    if (validatePIN(pin))
                    {
                        signinRetry = 0;
                        if (currentState == PinManState.RESET_OLD)
                        {
                            this.setResult(ResultCodes.PIN_NEW);
                            // And now finish ourself to be relaunched
                            finish();
                        }
                        else
                        {
                            // Only validating PIN. Return success.
                            this.setResult(ResultCodes.PIN_OK);
                            SystemState.setResume();
                            // Go ahead and reset the alarm
                            // This catches the case where the result from this activity
                            // Doesn't get reported to the caller because the parent activity is 
                            // Destroyed before this can be finished.
                            InactivityAlarmManager.getInstance(getApplicationContext()).reset();
                            // And now finish ourself
                            finish();
                        }
                    }
                    else
                    {
                        if (ACTION_VALIDATE_WITH_RESET == actionRequested)
                        {
                            signinRetry++;
                        }
                        if (signinRetry >= SIGNIN_RETRY_MAX)
                        {              
                            // sign user out       
                            MainSettingsActivity.signOff(this);
                        }
                        else
                        {
                            showError(R.string.errormessage_invalid_pin);
                            findViewById(R.id.pin_login_forgot_pin).setVisibility(View.VISIBLE);
                        }
                    }
                    break;

                case RESET_NEW:
                    // Remember first try of new PIN
                    newPinString = pin;
                    this.setResult(ResultCodes.PIN_NEWTWO);
                    // And now finish ourself to be relaunched
                    finish();
                    break;

                case RESET_NEWTWO:
                    // Validate second PIN
                    if (pin.equals(newPinString))
                    {
                        // Make it real!
                        Provisioning.getInstance(getApplicationContext()).setPin(newPinString);
                        Provisioning.getInstance(getApplicationContext()).setSecurityMode(true);
                        // We need this call to make sure we don't fire the pin login right away in the startup case
                        SystemState.setResume();
                        InactivityAlarmManager.getInstance(getApplicationContext()).activate();
                        Toast.makeText(getApplicationContext(), R.string.change_pin_toast, Toast.LENGTH_SHORT).show();
                        this.setResult(ResultCodes.PIN_OK);
                        newPinString = ""; // reset new Pin String
                        finish();
                    }
                    else
                    {
                        newPinString = ""; // reset new Pin String
                        showValidationError(R.string.errormessage_pins_donotmatch);
                    }                    
                    break;
            }
            return;
        }
    }

    private void showError(int errorMessageId)
    {
        AlertDialog alertDialog = new AlertDialog.Builder(this)
        .setTitle(R.string.pin_error_title)
        .setMessage(errorMessageId)
        .setIcon(getResources().getDrawable(R.drawable.error_icon))
        .setCancelable(false)
        .setPositiveButton(R.string.ok_button_text, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                // Clear the PIN *after* dismissing error dialog so that the UI shows all four dots.
                PinManActivity.this.pinControl.clearPin();
                dialog.cancel();
            } })
        .create();
        alertDialog.show();
    }
    
    private void showValidationError(int errorMessageId)
    {
        AlertDialog alertDialog = new AlertDialog.Builder(this)
        .setTitle(R.string.pin_error_title)
        .setMessage(errorMessageId)
        .setIcon(getResources().getDrawable(R.drawable.error_icon))
        .setCancelable(false)
        .setPositiveButton(R.string.ok_button_text, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                // Clear the PIN *after* dismissing error dialog so that the UI shows all four dots.
                PinManActivity.this.pinControl.clearPin();
                dialog.cancel();
                Intent intent = new Intent(getApplicationContext(), PinManActivity.class);
                int pinAction = PinManActivity.ACTION_SETNEW;
                intent.putExtra(PinManActivity.ACTION, pinAction);
                startActivityForResult(intent, RequestCodes.SETTINGS_CHANGE_PIN);
            } })
        .create();
        alertDialog.show();
    }

    private boolean validatePIN(String pin) {
        boolean result = false;
        if (pin.equals(Provisioning.getInstance(getApplicationContext()).getPin()) || Provisioning.getInstance(getApplicationContext()).getPin().length() != 4) {
            result = true;
        }
        return result;
    }

    /*
     * Method to open the forgot PIN activity
     */
    private void onForgotPIN(boolean forceReset)
    {
        AlertDialog alertDialog = new AlertDialog.Builder(this)
        .setTitle(R.string.forgot_password_dialog_title)
        .setMessage(R.string.forgot_password_dialog_body)
        .setIcon(getResources().getDrawable(R.drawable.error_icon))
        .setCancelable(false)
        .setPositiveButton(R.string.ok_button_text, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
                // sign user out       
                MainSettingsActivity.signOff(PinManActivity.this);            
            } })
        .setNegativeButton(R.string.no_button_text, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            } })
        .create();
        alertDialog.show();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RequestCodes.VALIDATE_PIN_WITH_PASSWORD) {
            switch(resultCode)
            {
                case ResultCodes.PIN_PASSWORD_SUCCESSFUL:
                    // User used password to authenticate. Finish task.
                    if (actionRequested == ACTION_VALIDATE)
                    {
                        this.setResult(ResultCodes.PIN_OK);
                        finish();
                    }
                    else
                    {
                        this.setResult(ResultCodes.PIN_NEW);
                        
                        // And now finish ourself to be relaunched
                        finish();
                    }
                    break;
                case ResultCodes.PIN_PASSWORD_UNSUCCESSFUL:
                    // Forgot PIN failed. Still stuck here.
                    break;
                default:
                    LogUtil.error(this, "onResult - unknown resultCode: " + resultCode);
            }
        }
    }
}
