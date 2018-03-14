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

import java.io.InputStream;

import org.apache.http.Header;
import org.apache.http.HttpEntity;

public class PostRequest extends DataRequest {

    public PostRequest(String uri, Header[] headers) {
        super(uri, headers);
        setMethod(METHOD_POST);
    }
    
    public PostRequest(String uri, Header[] headers, InputStream dataInputStream, int dataLength) {
        super(uri, headers, dataInputStream, dataLength);
        setMethod(METHOD_POST);
    }
    
    public PostRequest(String uri, Header[] headers, HttpEntity entity) {
        super(uri, headers, entity);
        setMethod(METHOD_POST);
    }
    
    @Override
    public void calculateRange() {
        //
    }
    
    @Override
    public void setOffset(int offset) {
        //
    }
}
