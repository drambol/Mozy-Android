/* Copyright 2009 Tactel AB, Sweden. All rights reserved.
*                                    _           _
*       _                 _        | |         | |
*     _| |_ _____  ____ _| |_ _____| |    _____| |__
*    (_   _|____ |/ ___|_   _) ___ | |   (____ |  _ \
*      | |_/ ___ ( (___  | |_| ____| |   / ___ | |_) )
*       \__)_____|\____)  \__)_____)\_)  \_____|____/
*
*/
package se.tactel.datahandler.api;

import java.io.IOException;

import org.apache.http.Header;
import org.apache.http.StatusLine;

import se.tactel.datahandler.DataManager.Params;

public interface ConnectionItem {
    
    public int read(final Params params) throws IOException;
    public int read(byte[] b, final Params params) throws IOException;
    public int read(byte[] b, int off, int len, final Params params) throws IOException;
    public long getContentLength();
    public StatusLine getHttpStatus();
    public Header[] getAllHeaders();
    public void abort();
}
