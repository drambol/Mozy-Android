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

public class ErrorCodes {

    public static final int MAX_AMOUNT_OF_ERRORS = 0x100000;
    public static final int NO_ERROR = 0;
    
    /**
     * Pin errors
     */
    public static final int ERROR_PIN_NOT_YET_INITIALIZED = 6;
    public static final int ERROR_EMAIL_APPLICATION_NOT_CONFIGURED = 7;
    
    /**
     * Backup errors
     */
    public static final int ERROR_FILE_NOT_FOUND = 81;
    public static final int ERROR_BACKUP_UNKNOWN = 82;
    public static final int ERROR_SYNC_SERVICE_NOT_BOUND = 83;
    public static final int ERROR_ILLEGAL_SYNC_ABORT = 84;
    public static final int ERROR_QUOTA_EXCEEDED = 85;
    
    /**
     * Http errors
     */
    public static final int ERROR_HTTP_UNKNOWN = 161; //(500)
    public static final int ERROR_HTTP_SERVER = 162;
    public static final int ERROR_HTTP_UNAUTHORIZED = 163;
    public static final int ERROR_HTTP_IO = 164;
    public static final int ERROR_HTTP_PARSER = 165;
    public static final int ERROR_HTTP_NOT_FOUND = 166;
    public static final int ERROR_HTTP_FORBIDDEN = 167;// (403)
    public static final int ERROR_HTTP_BAD_GATEWAY = 168;// (502)
    public static final int ERROR_HTTP_SERVICE_UNAVAILABLE = 169;// (503)
    public static final int ERROR_HTTP_GATEWAY_TIMEOUT = 170;// (504)
    public static final int ERROR_HTTP_RETRIABLE = 171;
    public static final int ERROR_HTTP_NOTRETRIABLE = 172;
    public static final int ERROR_HTTP_REQUEST_TIMEOUT = 173;
    public static final int ERROR_HTTP_OTHER = 174;
    public static final int ERROR_HTTP_TRY_AGAIN = 175;
    
    // Signin related special errors
    public static final int ERROR_AUTH_INVALID_PARTNER = 190;
    public static final int ERROR_AUTH_ACCOUNT_CONFLICT = 191;
    
    /**
     * Download errors
     */
    public static final int ERROR_NO_DISK_SPACE = 241;
    public static final int ERROR_NO_EXTERNAL_MEMORY = 242;
    public static final int ERROR_ENCRYPTED_FILE = 243;
    public static final int ERROR_DOWNLOAD_UNKNOWN = 244;
    
    /**
     * Music streaming errors
     */

    public static final int ERROR_STREAMING_UNKNOWN = 50;
    public static final int ERROR_STREAMING_UNSUPPORTED = 51;
    public static final int ERROR_NO_SOCIAL_SITES = 301;
    
    /**
     * No third party application errors
     */
    
    public static final int ERROR_NO_APPLICATION_TOOPEN = 360;
    
    /**
     * Incorrect Version error
     */
    public static final int ERROR_INVALID_CLIENT_VER = 365;
    
    
    /**
     * OAuth specific errors
     *
     */
     
     public static final int ERROR_INVALID_TOKEN = 366;
     public static final int ERROR_AUTHORIZATION_ERROR = 367;
     public static final int ERROR_INVALID_USER = 368;
 
}