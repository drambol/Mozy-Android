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
import java.net.URI;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.HttpContext;


import se.tactel.datahandler.DataRequest;
import se.tactel.datahandler.HttpException;
import se.tactel.datahandler.DataManager.HttpParams;

public class HttpAPI implements API {
    
    private final DefaultHttpClient httpClient;
    private final HttpParams httpParams;
    
    public HttpAPI(final HttpParams httpParams) {
        this.httpParams = httpParams;
        
        if (httpParams != null && httpParams.getHttpClient() != null) {
            httpClient = httpParams.getHttpClient();
        } else {
            httpClient = new DefaultHttpClient();
        }
        
        if (httpParams != null) {
            /**
             * Add request interceptors.
             */
            HttpRequestInterceptor[] interceptors = httpParams.getHttpRequestInterceptors();
            if (interceptors != null) {
                int index = 0;
                for (int i = 0; i < interceptors.length; ++i) {
                    if (interceptors[i] != null) {
                        httpClient.addRequestInterceptor(interceptors[i], index);
                        index++;
                    }
                }
            }
            
            /**
             * Sets http client credentials.
             */
            AuthScope authScope = httpParams.getAuthScope();
            Credentials credentials = httpParams.getCredentials();
            if (authScope != null && credentials != null) {
                httpClient.getCredentialsProvider().setCredentials(authScope, credentials);
            }
        }
    }
    
    public HttpConnectionItem doSimpleHttpRequest(final DataRequest request) {
        HttpConnectionItem result = null;
        if (request != null) {
            URI uri = URI.create(request.getURI());
            HttpUriRequest httpRequest = null;
            if (request.getMethod() == DataRequest.METHOD_POST) {
                httpRequest = new HttpPost(uri);
                HttpEntity entity = request.getEntity();
                if (entity != null) {
                    ((HttpPost)httpRequest).setEntity(entity);
                }
            } else if (request.getMethod() == DataRequest.METHOD_GET) {
                httpRequest = new HttpGet(uri);
            } else if (request.getMethod() == DataRequest.METHOD_HEAD) {
                httpRequest = new HttpHead(uri);
            } else if (request.getMethod() == DataRequest.METHOD_PUT) {
                httpRequest = new HttpPut(uri);
                HttpEntity entity = request.getEntity();
                if (entity != null) {
                    ((HttpPut)httpRequest).setEntity(entity);
                }                
            } else if (request.getMethod() == DataRequest.METHOD_DELETE) {
                httpRequest = new HttpDelete(uri);
            }
            

            if (httpRequest != null) {
                Header[] headers = request.getHeaders();
                if (headers != null) {
                    for (int i = 0; i < headers.length; ++i) {
                        if (headers[i] != null) {
                            httpRequest.addHeader(headers[i]);
                        }
                    }
                }
                
                result = new HttpConnectionItem(httpRequest, request.getOffset(), this);
            }
        }
        return result;
    }
    
    public HttpResponse execute(HttpUriRequest httpRequest, HttpContext context) throws HttpException, IOException {
        
        HttpResponse response = null;

        response = httpParams.getHttpClient().execute(httpRequest, context);

        int statusCode = response.getStatusLine().getStatusCode();

        switch (statusCode) {
            case HttpStatus.SC_CONTINUE:
            case HttpStatus.SC_OK:
            case HttpStatus.SC_CREATED:
            case HttpStatus.SC_ACCEPTED:
            case HttpStatus.SC_NON_AUTHORITATIVE_INFORMATION:
            case HttpStatus.SC_NO_CONTENT:
            case HttpStatus.SC_RESET_CONTENT:
            case HttpStatus.SC_PARTIAL_CONTENT:
            case HttpStatus.SC_TEMPORARY_REDIRECT: {
                return response;
            }
            case HttpStatus.SC_INTERNAL_SERVER_ERROR:
            case HttpStatus.SC_BAD_GATEWAY:
            case HttpStatus.SC_SERVICE_UNAVAILABLE:
            case HttpStatus.SC_GATEWAY_TIMEOUT:
            case HttpStatus.SC_FORBIDDEN:
            case HttpStatus.SC_BAD_REQUEST:
            case HttpStatus.SC_UNAUTHORIZED:
            case HttpStatus.SC_NOT_FOUND: {
                Header[] headers = null;
                if (response != null) {
                    /* DEBUG CODE * /
                    BufferedReader in = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
                    StringBuffer sb = new StringBuffer("");
                    String line = "";
                    String NL = System.getProperty("line.separator");
                    while ((line = in.readLine()) != null) {
                        sb.append(line + NL);
                    }
                    in.close();
                    String page = sb.toString();                
                    / * END DEBUG CODE */                    
                    
                    headers = response.getAllHeaders();
                    response.getEntity().consumeContent();
                }

                throw new HttpException(statusCode, headers);
            }
            default: {
                Header[] headers = null;
                if (response != null) {
                    headers = response.getAllHeaders();
                    response.getEntity().consumeContent();
                }
                throw new HttpException("Unknown HTTP status code.", statusCode, headers);
            }
        }
    }
    
    @Override
    public ConnectionItem getConnection(DataRequest request) throws IOException {
        return doSimpleHttpRequest(request);
    }
}
