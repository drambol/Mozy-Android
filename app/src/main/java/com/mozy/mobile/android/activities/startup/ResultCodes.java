/* Copyright 2009 Tactel AB, Sweden. All rights reserved.
*                                    _           _
*       _                 _        | |         | |
*     _| |_ _____  ____ _| |_ _____| |    _____| |__
*    (_   _|____ |/ ___|_   _) ___ | |   (____ |  _ \
*      | |_/ ___ ( (___  | |_| ____| |   / ___ | |_) )
*       \__)_____|\____)  \__)_____)\_)  \_____|____/
*
*/

package com.mozy.mobile.android.activities.startup;

import android.app.Activity;

public class ResultCodes {
    
    /** * Notifies the caller that the activity was cancelled/back button
     */
    public static final int CANCEL = Activity.RESULT_CANCELED;

    /**
     * Notifies that application is about to be terminated.
     */
    public static final int EXIT = Activity.RESULT_FIRST_USER + 1;
    
    /**
     * Notifies that PIN has been entered successfully.
     */
    public static final int PIN_OK = Activity.RESULT_FIRST_USER + 2;
    
    /**
     * Notifies that account has been linked to device successfully.
     */
    public static final int ACCOUNT_LINKED_OK = Activity.RESULT_FIRST_USER + 3;
    
    /**
     * Notifies that email has been verified successfully.
     */
    public static final int EMAIL_VERIFICATION_SUCCESSFUL = Activity.RESULT_FIRST_USER + 4;
    
    /**
     * Notifies that account has been created.
     */
    public static final int ACCOUNT_CREATED = Activity.RESULT_FIRST_USER + 5;
    
    /**
     * Notifies that user has signed in successfully.
     */
    public static final int SIGNED_IN_OK = Activity.RESULT_FIRST_USER + 6;
    
    /**
     * Notifies that user has pressed Yes on prompt.
     */
    public static final int PROMPT_YES = Activity.RESULT_FIRST_USER + 7;
    
    /**
     * Notifies that user has pressed No on prompt.
     */
    public static final int PROMPT_NO = Activity.RESULT_FIRST_USER + 8;
    
    public static final int NEXT_BUTTON_CLICKED = Activity.RESULT_FIRST_USER + 9;
    
    public static final int BACK_BUTTON_CLICKED = Activity.RESULT_FIRST_USER + 10;       
    
    public static final int REGISTRATION_COMPLETE = Activity.RESULT_FIRST_USER + 11;
    /*
     * Notifies that password-based validation succeeded.
     */
    public static final int PIN_PASSWORD_SUCCESSFUL = Activity.RESULT_FIRST_USER + 12;
    /*
     * Notifies that password-based validation failed.
     */
    public static final int PIN_PASSWORD_UNSUCCESSFUL = Activity.RESULT_FIRST_USER + 13;
    
    public static final int INVALID_CREDENTIALS = Activity.RESULT_FIRST_USER + 14;

    public static final int EULA_REFUSED = Activity.RESULT_FIRST_USER + 15;
    
    public static final int PIN_NEW = Activity.RESULT_FIRST_USER + 16;
    
    public static final int PIN_NEWTWO = Activity.RESULT_FIRST_USER + 17;
    
    public static final int HOME_SCREEN_BACKPRESSED = Activity.RESULT_FIRST_USER + 18;
    
}
