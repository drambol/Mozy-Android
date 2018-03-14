package com.mozy.mobile.android.catch_release;

public class CRResultCodes {
    /**
     * Successful results
     */
    public static final int CR_RESULT_OK = 0x0000;
    
    /**
     * Work errors
     */
    public static final int CR_ERROR_INTERRUPTED = 0x0011;
    public static final int CR_ERROR_FILE_NOT_FOUND = 0x0012;
    public static final int CR_ERROR_OUTDATED_QUEUE = 0x0013;
    public static final int CR_ERROR_GENERIC = 0x0014;
    public static final int CR_ERROR_NO_PRIVATE_KEY = 0x0015;
    
    /**
     * True if following error is a communication error. 
     * (server response or network connection problems)
     * @param error error code.
     * @return
     */
    public static boolean isCommunicationError(int error) {
        return error > 0x0100;
    }
    
    /**
     * True if following error is a network connection error. 
     * @param error
     * @return
     */
    public static boolean isConnectionError(int error) {
        return error > 0x0100 && error < 0x0200;
    }
    
    /** 
     * Connection errors
     */
    public static final int CR_ERROR_CONNECTION_FAILED = 0x0101;
    public static final int CR_ERROR_WIFI_CONNECTION_ONLY = 0x0102;
    public static final int CR_ERROR_ROAMING_NETWORK = 0x0103;
    
    /**
     * Server errors
     */
    public static final int CR_ERROR_UNAUTHORIZED = 0x0201;
    public static final int CR_ERROR_UNKNOWN_RESPONSE_PARSER = 0x0202;
    public static final int CR_ERROR_SERVER_GENERIC = 0x0203;
    public static final int CR_ERROR_EXCEEDED_QUOTA = 0x0204;
    public static final int CR_ERROR_INVALID_VER = 0x0205;
    public static final int CR_ERROR_INVALID_TOKEN = 0x206;

}
