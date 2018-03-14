package com.mozy.mobile.android.client;

import org.apache.http.HttpVersion;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.CoreConnectionPNames;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.protocol.HTTP;

import com.mozy.mobile.android.ssl.EasySSLSocketFactory;
import com.mozy.mobile.android.ssl.TLSv12SocketFactory;

public class DechoHttpClient extends DefaultHttpClient
{
    private static final int HTTP_PORT = 80;
    private static final int HTTPS_PORT = 443;
    private static final int TIME_OUT = 30000;
    
    private boolean m_bAcceptAllCerts;
        
    public DechoHttpClient(boolean bAcceptAllCerts)
    {
        m_bAcceptAllCerts = bAcceptAllCerts;
    }
    
    protected int getHttpsPort()
    {
        return DechoHttpClient.HTTPS_PORT;
    }
    
    protected ClientConnectionManager createClientConnectionManager() 
    {
        HttpParams params = new BasicHttpParams(); 
        HttpProtocolParams.setVersion(params, HttpVersion.HTTP_1_1); 
        HttpProtocolParams.setContentCharset(params, HTTP.DEFAULT_CONTENT_CHARSET); 
        HttpProtocolParams.setUseExpectContinue(params, true);
        HttpConnectionParams.setConnectionTimeout(params, TIME_OUT);
        HttpConnectionParams.setSoTimeout(params, TIME_OUT);
        params.setParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, TIME_OUT);

        SchemeRegistry schReg = new SchemeRegistry(); 
        schReg.register(new Scheme("http", PlainSocketFactory.getSocketFactory(), HTTP_PORT));
        
        if (m_bAcceptAllCerts)
        {
            schReg.register(new Scheme("https", new EasySSLSocketFactory(), this.getHttpsPort()));
        }
        else
        {
            schReg.register(new Scheme("https", TLSv12SocketFactory.getSocketFactory(), this.getHttpsPort()));
        }
        
        ClientConnectionManager conMgr = new ThreadSafeClientConnManager(params, schReg);
        return conMgr;
    }
    
}
