/* Copyright 2009 Tactel AB, Sweden. All rights reserved.
*                                    _           _
*       _                 _        | |         | |
*     _| |_ _____  ____ _| |_ _____| |    _____| |__
*    (_   _|____ |/ ___|_   _) ___ | |   (____ |  _ \
*      | |_/ ___ ( (___  | |_| ____| |   / ___ | |_) )
*       \__)_____|\____)  \__)_____)\_)  \_____|____/
*
*/
package se.tactel.datahandler;

import java.io.IOException;

import org.apache.http.Header;

public class HttpException extends IOException {

    /**
     * HttpException serial version UID.
     */
    private static final long serialVersionUID = 7916294186521601527L;
    private int errorCode = 0;
    private Header[] headers = null;

    public HttpException(String message, int errorCode, Header[] headers) {
        super(message + " Http status(" + errorCode + ")");
        this.errorCode = errorCode;
        this.headers = headers;
    }

    public HttpException(int errorCode, Header[] headers) {
        super("Http status(" + errorCode + ")");
        this.errorCode = errorCode;
        this.headers = headers;
    }

    public int getHttpErrorCode() {
        return errorCode;
    }

    public Header[] getAllHeaders() {
        return headers;
    }
}
