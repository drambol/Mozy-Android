package com.mozy.mobile.android.utils;

import android.util.Log;

/**
 * Handles use of logs.
 */
public class LogUtil {

    /**
     * Log enter function.
     * 
     * @param arg1 Class object.
     * @param arg2 Enter function text.
     */
    public static void enter(Object arg1, String arg2) {
        if (UtilFlags.isEnterFunctionEnabled) {
            Log.d(arg1 != null ? arg1.getClass().getSimpleName() : "", "ENTER: " + arg2);
        }
    }

    /**
     * Log enter function.
     * 
     * @param arg1 Class object name.
     * @param arg2 Enter function text.
     */
    public static void enter(String arg1, String arg2) {
        if (UtilFlags.isEnterFunctionEnabled) {
            Log.d(arg1, "ENTER: " + arg2);
        }
    }

    /**
     * Log exit function.
     * 
     * @param arg1 Class object.
     * @param arg2 Exit function text.
     */
    public static void exit(Object arg1, String arg2) {
        if (UtilFlags.isExitFunctionEnabled) {
            Log.d(arg1 != null ? arg1.getClass().getSimpleName() : "", "EXIT: " + arg2);
        }
    }

    /**
     * Log exit function.
     * 
     * @param arg1 Class object name.
     * @param arg2 Exit function text.
     */
    public static void exit(String arg1, String arg2) {
        if (UtilFlags.isExitFunctionEnabled) {
            Log.d(arg1, "EXIT: " + arg2);
        }
    }

    /**
     * Log debug.
     * 
     * @param arg1 Class object.
     * @param arg2 Debug text.
     */
    public static void debug(Object arg1, String arg2) {
        if (UtilFlags.isDebugEnabled) {
            Log.d(arg1 != null ? arg1.getClass().getSimpleName() : "", "DEBUG: " + arg2);
        }
    }

    /**
     * Log debug.
     * 
     * @param arg1 Class object name.
     * @param arg2 Debug text.
     */
    public static void debug(String arg1, String arg2) {
        if (UtilFlags.isDebugEnabled) {
            Log.d(arg1, "DEBUG: " + arg2);
        }
    }

    /**
     * Log info.
     * 
     * @param arg1 Class object.
     * @param arg2 Info text.
     */
    public static void info(Object arg1, String arg2) {
        if (UtilFlags.isInfoEnabled) {
            Log.i(arg1 != null ? arg1.getClass().getSimpleName() : "", "INFO: " + arg2);
        }
    }

    /**
     * Log info.
     * 
     * @param arg1 Class object name.
     * @param arg2 Info text.
     */
    public static void info(String arg1, String arg2) {
        if (UtilFlags.isInfoEnabled) {
            Log.i(arg1, "INFO: " + arg2);
        }
    }

    /**
     * Log info.
     * 
     * @param arg1 Class object.
     * @param arg2 Function name.
     * @param arg3 Info text.
     */
    public static void info(Object arg1, String arg2, String arg3) {
        if (UtilFlags.isInfoEnabled) {
            Log.i(arg1 != null ? arg1.getClass().getSimpleName() : "", "INFO - " + arg2 + ": "
                    + arg3);
        }
    }

    /**
     * Log info.
     * 
     * @param arg1 Class object name.
     * @param arg2 Function name.
     * @param arg3 Info text.
     */
    public static void info(String arg1, String arg2, String arg3) {
        if (UtilFlags.isInfoEnabled) {
            Log.i(arg1, "INFO - " + arg2 + ": " + arg3);
        }
    }

    /**
     * Log error.
     * 
     * @param arg1 Class object.
     * @param arg2 Error text.
     */
    public static void error(Object arg1, String arg2) {
        if (UtilFlags.isErrorsEnabled) {
            Log.e(arg1 != null ? arg1.getClass().getSimpleName() : "", "ERROR: " + arg2);
        }
    }

    /**
     * Log error.
     * 
     * @param arg1 Class object name.
     * @param arg2 Error text.
     */
    public static void error(String arg1, String arg2) {
        if (UtilFlags.isErrorsEnabled) {
            Log.e(arg1, "ERROR: " + arg2);
        }
    }

    /**
     * Log warning.
     * 
     * @param arg1 Class object.
     * @param arg2 Warning text.
     */
    public static void warning(Object arg1, String arg2) {
        if (UtilFlags.isWarningsEnabled) {
            Log.w(arg1 != null ? arg1.getClass().getSimpleName() : "", "WARNING: " + arg2);
        }
    }

    /**
     * Log warning.
     * 
     * @param arg1 Class object name.
     * @param arg2 Warning text.
     */
    public static void warning(String arg1, String arg2) {
        if (UtilFlags.isWarningsEnabled) {
            Log.w(arg1, "WARNING: " + arg2);
        }
    }

    /**
     * Log exception.
     * 
     * @param arg1 Class object.
     * @param arg2 Exception text.
     * @param arg3 Throwable object.
     */
    public static void exception(Object arg1, String arg2, Throwable arg3) {
        if (UtilFlags.isExceptionsEnabled) {
            Log.e(arg1 != null ? arg1.getClass().getSimpleName() : "", "EXCEPTION: " + arg2, arg3);
        }
    }

    /**
     * Log exception.
     * 
     * @param arg1 Class object name.
     * @param arg2 Exception text.
     * @param arg3 Throwable object.
     */
    public static void exception(String arg1, String arg2, Throwable arg3) {
        if (UtilFlags.isExceptionsEnabled) {
            Log.e(arg1, "EXCEPTION: " + arg2, arg3);
        }
    }
}
