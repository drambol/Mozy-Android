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
import java.util.Vector;

import android.content.Context;

import se.tactel.datahandler.DataManager.HttpParams;
import se.tactel.datahandler.DataManager.Params;
import se.tactel.datahandler.api.API;
import se.tactel.datahandler.api.CacheAPI;
import se.tactel.datahandler.api.ConnectionItem;
import se.tactel.datahandler.api.HttpAPI;
import se.tactel.datahandler.api.HttpConnectionItem;

public class DataConnectionManager {
    
    private final Vector<API> apis;
    private final HttpAPI httpApi;
    private final CacheAPI cacheApi;
    
    DataConnectionManager(final API[] apis, final HttpParams httpParams, final Context context) {
        this.apis = new Vector<API>();
        if (apis != null) {
            for (int i = 0; i < apis.length; ++i) {
                this.apis.add(apis[i]);
            }
        }
        this.httpApi = new HttpAPI(httpParams);
        this.cacheApi = new CacheAPI(httpApi, context);
    }
    
    public ConnectionItem connect(final DataRequest request, final Params params, int offset) throws IOException {
        if (offset > 0) {
            request.setOffset(offset + request.getInitialOffset());
        }
        ConnectionItem result = null;
        if (request.isCachable()) {
            if (apis != null) {
                for (int i = 0; i < apis.size() && result == null; ++i) {
                    try {
                        result = apis.get(i).getConnection(request);
                    } catch (IOException e) {
                        result = null;
                    }
                }
            }
            if (result == null) {
                result = cacheApi.getConnection(request);
            }
            
        } else {
            result = httpApi.getConnection(request);
        }
        request.setConnectionItem(result);
        
        return result;
    }
    
    public HttpConnectionItem executeSimpleHttp(final DataRequest request, 
                                                final Params params, 
                                                final boolean saveReturnData) throws IOException {
        HttpConnectionItem connection = httpApi.doSimpleHttpRequest(request);
        
        if (connection != null) {
            try {
                request.setConnectionItem(connection);
                connection.execute(params, saveReturnData);
            } catch (IOException ie) {
                if (!request.isAborted()) {
                    throw ie;
                }
            }
        }
        
        return connection;
    }
}
