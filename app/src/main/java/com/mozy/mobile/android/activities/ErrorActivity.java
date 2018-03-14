/* Copyright 201 Tactel AB, Sweden. All rights reserved.
 *                                    _           _
 *       _                 _        | |         | |
 *     _| |_ _____  ____ _| |_ _____| |    _____| |__
 *    (_   _|____ |/ ___|_   _) ___ | |   (____ |  _ \
 *      | |_/ ___ ( (___  | |_| ____| |   / ___ | |_) )
 *       \__)_____|\____)  \__)_____)\_)  \_____|____/
 *
 */
package com.mozy.mobile.android.activities;

import java.util.Vector;

import com.mozy.mobile.android.R;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;

public abstract class ErrorActivity extends BarActivity {
    public static final int DIALOG_ERROR_NO_FINISH_ID = ErrorCodes.MAX_AMOUNT_OF_ERRORS;
    public static final int DIALOG_ERROR_ID = ErrorCodes.MAX_AMOUNT_OF_ERRORS + 1;
    public static final int UNAUTHORIZED_USER_DIALOG = ErrorCodes.MAX_AMOUNT_OF_ERRORS + 2;
    public static final int FIRST_USER_DIALOG = ErrorCodes.MAX_AMOUNT_OF_ERRORS + 3;


    protected static final int ACTION_NONE = ErrorManager.NO_FLAG;
    protected static final int ACTION_UI_ERROR = ErrorManager.LAYOUT_ERROR_FLAG;
    protected static final int ACTION_DIALOG_ERROR = ErrorManager.DIALOG_ERROR_FLAG;
    protected static final int ACTION_UI_AND_DIALOG_ERROR = ErrorManager.DIALOG_ERROR_FLAG | ErrorManager.LAYOUT_ERROR_FLAG;
    protected static final int ACTION_UNHANDLED_NONE = ErrorManager.UNHANDLED_FLAG;
    protected static final int ACTION_UNHANDLED_UI_ERROR = ErrorManager.LAYOUT_ERROR_FLAG | ErrorManager.UNHANDLED_FLAG;
    protected static final int ACTION_UNHANDLED_DIALOG_ERROR = ErrorManager.DIALOG_ERROR_FLAG | ErrorManager.UNHANDLED_FLAG;
    protected static final int ACTION_CUSTOMIZED_UI_ERROR = ErrorManager.LAYOUT_ERROR_FLAG | ErrorManager.CUSTOMIZED_FLAG;
    protected static final int ACTION_DIALOG_AND_CUSTOMIZED_UI_ERROR = ErrorManager.DIALOG_ERROR_FLAG | ErrorManager.LAYOUT_ERROR_FLAG | ErrorManager.CUSTOMIZED_FLAG;

    protected static final int BUTTON_ONE = 0;
    protected static final int BUTTON_TWO = 1;
    protected static final int BUTTON_THREE = 2;

    private Handler handler = null;
    private Vector<ErrorItem> errors = new Vector<ErrorItem>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        synchronized (this) {
            if (handler == null) {
                handler = new Handler();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        synchronized (this) {
            handler = null;
        }
    }

    @Override
    protected Dialog onCreateDialog(int dialog_id) {
        Dialog dialog = super.onCreateDialog(dialog_id);
        if (dialog_id < FIRST_USER_DIALOG) {
            dialog = createErrorDialog(dialog_id);
        }
        return dialog;
    }

    @Override
    protected void onResume() {
        super.onResume();

        int[] ids = getErrorIds();
        if (ids != null) {
            for (int i = 0; i < ids.length; ++i) {
                ErrorManager.getInstance().registerActivity(this, ids[i]);
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        int[] ids = getErrorIds();
        if (ids != null) {
            for (int i = 0; i < ids.length; ++i) {
                ErrorManager.getInstance().unregisterActivity(this, ids[i]);
            }
        }
    }

    /**
     * Loads a dialog based on error code.
     * @param error_code error code
     */
    public synchronized boolean triggerError(int error_code, int error_type) {
        boolean handled = true;
        int action = handleError(error_code, error_type);
        int code = error_code;
        int type = error_type;

        if (error_code == ErrorCodes.NO_ERROR) {
            if (errors.size() > 0) {
                int i = 0;
                while (i < errors.size()) {
                    if (errors.get(i).errorType == error_type) {
                        errors.remove(i);
                    } else {
                        i++;
                    }
                }
            }
            boolean hideError = true;
            if (errors.size() > 0) {
                if (errors.lastElement() != null && errors.lastElement().errorCode != ErrorCodes.NO_ERROR) {
                    code = errors.lastElement().errorCode;
                    type = errors.lastElement().errorType;
                    action = errors.lastElement().errorAction;
                    hideError = false;
                }
            }
            if (hideError) {
                if (handler != null) {
                    handler.post(new Runnable() {

                        @Override
                        public void run() {
                            hideError();
                        }
                    });
                }
            }
        }
        if (action != ACTION_NONE) {
            try {
                if (code != ErrorCodes.NO_ERROR) {
                    errors.add(new ErrorItem(code, type, action));
                }
            } catch (Exception e) {}
            
            final int final_error_code = code;
            final int final_error_action = action;

            if (ErrorManager.isLayoutFlag(action)) {
                if (handler != null && final_error_code != ErrorCodes.NO_ERROR) {
                    handler.post(new Runnable() {

                        @Override
                        public void run() {
                            if (ErrorManager.isCustomizedFlag(final_error_action)) {
                                CharSequence cs = getCustomizedErrorViewText(final_error_code);
                                if (cs != null) {
                                    displayError(cs);
                                }
                            } else {
                                int error_text_res_id = getErrorViewTextResource(final_error_code);
                                if (error_text_res_id != 0) {
                                    displayError(error_text_res_id);
                                }
                            }
                        }
                    });
                }
            }
            if (ErrorManager.isDialogFlag(action)) {
                if (handler != null) {
                    handler.post(new Runnable() {

                        @Override
                        public void run() {
                            showDialog(final_error_code);
                        }
                    });
                }
            }
            if (ErrorManager.isUnhandledFlag(action)) {
                handled = false;
            }
        }
        return handled;
    }

    /**
     * Retrieves error Ids to listen to.
     * @return returns an array of error ids. (Do not confuse with error codes)
     */
    protected int[] getErrorIds() {
        return null;
    }

    /**
     * Creates dialog for specified error code.
     * @param error_code error code.
     * @return null if no dialog is to be displayed, else dialog to show.
     */
    protected Dialog createErrorDialog(int error_code) {
        return null;
    }

    /**
     * Gets error resource text to display in error view.
     * @param error_code error code to request text for.
     * @return resource id to text.
     */
    protected int getErrorViewTextResource(int error_code) {
        return 0;
    }

    /**
     * Gets error resource text to display in error view.
     * @param error_code error code to request text for.
     * @return resource id to text.
     */
    protected CharSequence getCustomizedErrorViewText(int error_code) {
        return null;
    }

    /**
     * Specifies if error code should be handled. False will pass on error to next activity listening for it.
     * By returning true the error will not be reset but will be flagged as handled.
     * @return
     */
    protected int handleError(int error_code, int error_type) {
        return ErrorManager.NO_FLAG;
    }

    /**
     * Creates a 3 button generic error dialog. 
     * Will do onButtonClick() callbacks.
     * @param title_res resource id for title string.
     * @param message_res resource id for message string.
     * @param positive_button_res resource id for positive button string.
     * @param neutral_button_res resource id for neutral button string.
     * @param negative_button_res resource id for negative button string.
     * @return created dialog.
     */
    public Dialog createGenericErrorDialog(final int dialog_id, int title_res, int message_res, int positive_button_res, int neutral_button_res, int negative_button_res) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setIcon(getResources().getDrawable(R.drawable.error_icon));        
        builder.setPositiveButton(positive_button_res, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                onButtonClick(dialog_id, BUTTON_ONE);
            }
        });
        builder.setNegativeButton(negative_button_res, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                onButtonClick(dialog_id, BUTTON_THREE);
            }
        });
        builder.setNeutralButton(neutral_button_res, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                onButtonClick(dialog_id, BUTTON_TWO);
            }
        });
        builder.setTitle(title_res);
        builder.setMessage(message_res);

        return builder.create();
    }

    /**
     * Creates a 2 button generic error dialog. 
     * Will do onButtonClick() callbacks.
     * @param title_res resource id for title string.
     * @param message_res resource id for message string.
     * @param positive_button_res resource id for positive button string.
     * @param negative_button_res resource id for negative button string.
     * @return created dialog.
     */
    public Dialog createGenericErrorDialog(final int dialog_id, int title_res, int message_res, int positive_button_res, int negative_button_res) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setIcon(getResources().getDrawable(R.drawable.error_icon));
        builder.setPositiveButton(positive_button_res, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                onButtonClick(dialog_id, BUTTON_ONE);
            }
        });
        builder.setNegativeButton(negative_button_res, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                onButtonClick(dialog_id, BUTTON_TWO);
            }
        });
        builder.setTitle(title_res);
        builder.setMessage(message_res);

        return builder.create();
    }
    

    /**
     * Creates a 1 button generic error dialog. 
     * Will do onButtonClick() callbacks.
     * @param int dialog_id id of dialog
     * @param title_res resource id for title string.
     * @param message_res resource id for message string.
     * @param positive_button_res resource id for positive button string.
     * @return created dialog.
     */
    public Dialog createGenericErrorDialog(final int dialog_id, int title_res, int message_res, int positive_button_res) {
        
        
        final int errorDlgId = (message_res == R.string.device_revoked_body)  ? ErrorActivity.UNAUTHORIZED_USER_DIALOG : dialog_id;
        
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setIcon(getResources().getDrawable(R.drawable.error_icon));
        builder.setPositiveButton(positive_button_res, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                onButtonClick(errorDlgId, 0);
            }
        });
        builder.setTitle(title_res);
        builder.setMessage(message_res);

        return builder.create();
    }
    
   
    /**
     * Callback for positive button being clicked.
     */
    protected void onButtonClick(int dialog_id, int button_id) 
    {
        // Wipe Mozy data and signout 
        if(dialog_id == ErrorActivity.UNAUTHORIZED_USER_DIALOG)
        {
            MainSettingsActivity.remoteWipeAndSignOff(ErrorActivity.this);  
        }
    }

    private static class ErrorItem {
        int errorCode;
        int errorType;
        int errorAction;
        ErrorItem(int errorCode, int errorType, int errorAction) {
            this.errorCode = errorCode;
            this.errorType = errorType;
            this.errorAction = errorAction;
        }
    }
}
