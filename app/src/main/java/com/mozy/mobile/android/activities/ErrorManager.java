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

public class ErrorManager {

    private static ErrorManager instance = null;
    private static final Object lock = new Object();
    
    public static final int DIALOG_ERROR_FLAG = 0x010000;
    
    public static final int LAYOUT_ERROR_FLAG = 0x020000;
    
    public static final int UNHANDLED_FLAG = 0x040000;
    
    public static final int CUSTOMIZED_FLAG = 0x080000;
    
    public static final int NO_FLAG = 0x00;
    
    public static final int ERROR_TYPE_GENERIC = 0;
    public static final int ERROR_TYPE_BACKUP = 1;
    public static final int ERROR_TYPE_STREAMING = 2;
    private static final int AMOUNT_OF_ERROR_TYPES = 3;
    
    private ErrorActivity[] listener;
    private int[] errorCode;
    private boolean[] errorNotified;
    
    private ErrorManager() {
        listener = new ErrorActivity[AMOUNT_OF_ERROR_TYPES];
        errorCode = new int[AMOUNT_OF_ERROR_TYPES];
        errorNotified = new boolean[AMOUNT_OF_ERROR_TYPES];
        
        for (int i = 0; i < AMOUNT_OF_ERROR_TYPES; ++i) {
            listener[i] = null;
            errorCode[i] = ErrorCodes.NO_ERROR;
            errorNotified[i] = false;
        }
    }

    public static ErrorManager getInstance() {
        synchronized(lock) {
            if (instance == null) {
                instance = new ErrorManager();
            }
        }

        return instance;
    }
    
    public synchronized void registerActivity(ErrorActivity listener, int error_type) {
        if (error_type >= 0 && error_type < AMOUNT_OF_ERROR_TYPES) {
            this.listener[error_type] = listener;

            if (listener != null) {
                if (!errorNotified[error_type] && listener.triggerError(errorCode[error_type], error_type)) {
                    errorNotified[error_type] = true;
                }
            }
        }
    }

    public synchronized void unregisterActivity(ErrorActivity listener, int error_type) {
        if (error_type >= 0 && error_type < AMOUNT_OF_ERROR_TYPES) {
            if (this.listener[error_type].equals(listener)) {
                this.listener[error_type] = null;
            }
        }
    }
    
    public synchronized int getError(int error_type) {
        int ret = ErrorCodes.NO_ERROR;
        if (error_type >= 0 && error_type < AMOUNT_OF_ERROR_TYPES) {
            ret = errorCode[error_type];
        }
        return ret;
    }
    
    public synchronized void reportError(int errorCode, int error_type) {
        if (error_type >= 0 && error_type < AMOUNT_OF_ERROR_TYPES) {
            this.errorCode[error_type] = errorCode;
            errorNotified[error_type] = false;
            
            if (listener[error_type] != null) {
                if (!errorNotified[error_type] && listener[error_type].triggerError(errorCode, error_type)) {
                    errorNotified[error_type] = true;
                }
            }
        }
    }
    
    public synchronized void resetError(int error_type) {
        if (error_type >= 0 && error_type < AMOUNT_OF_ERROR_TYPES && errorCode[error_type] != ErrorCodes.NO_ERROR) {
            errorCode[error_type] = ErrorCodes.NO_ERROR;
            errorNotified[error_type] = false;
            
            if (listener[error_type] != null) {
                if (!errorNotified[error_type] && listener[error_type].triggerError(ErrorCodes.NO_ERROR, error_type)) {
                    errorNotified[error_type] = true;
                }
            }
        }
    }
    
    public static boolean isDialogFlag(int flags) {
        boolean isDialog = false;
        if ((flags & DIALOG_ERROR_FLAG) == DIALOG_ERROR_FLAG) {
            isDialog = true;
        }
        return isDialog;
    }
    
    public static boolean isLayoutFlag(int flags) {
        boolean isLayout = false;
        if ((flags & LAYOUT_ERROR_FLAG) == LAYOUT_ERROR_FLAG) {
            isLayout = true;
        }
        return isLayout;
    }
    
    public static boolean isCustomizedFlag(int flags) {
        boolean isCustomized = false;
        if ((flags & CUSTOMIZED_FLAG) == CUSTOMIZED_FLAG) {
            isCustomized = true;
        }
        return isCustomized;
    }
    
    public static boolean isUnhandledFlag(int flags) {
        boolean isUnhandled = false;
        if ((flags & UNHANDLED_FLAG) == UNHANDLED_FLAG) {
            isUnhandled = true;
        }
        return isUnhandled;
    }
}
