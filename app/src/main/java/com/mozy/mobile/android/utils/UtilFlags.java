package com.mozy.mobile.android.utils;

public class UtilFlags {
    static final boolean isUtilEnabled = true;

    static final boolean isLogEnabled = false;//isUtilEnabled;

    public static final boolean isDebugEnabled = false;//isLogEnabled;

    static final boolean isEnterFunctionEnabled = true && isLogEnabled;

    static final boolean isExitFunctionEnabled = true && isLogEnabled;

    static final boolean isInfoEnabled = true && isLogEnabled;

    static final boolean isErrorsEnabled = true && isLogEnabled;

    static final boolean isWarningsEnabled = true && isLogEnabled;

    static final boolean isExceptionsEnabled = true && isLogEnabled;

    static final boolean isToastEnabled = true && isLogEnabled;

    static final boolean isCallbackEnabled = true && isUtilEnabled;
}
