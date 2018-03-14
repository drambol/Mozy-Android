package com.mozy.mobile.android.ssl;

import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;




import org.apache.http.conn.ssl.SSLSocketFactory;


public class TLSv12SocketFactory extends SSLSocketFactory {
    SSLContext sslContext = SSLContext.getInstance("TLS");


    public TLSv12SocketFactory(KeyStore truststore)
            throws NoSuchAlgorithmException, KeyManagementException,
            KeyStoreException, UnrecoverableKeyException {
        super(truststore);

        /*
        TrustManager tm = new X509TrustManager() {
            public java.security.cert.X509Certificate[] getAcceptedIssuers() {return null;}

            @Override
            public void checkClientTrusted( java.security.cert.X509Certificate[] chain, String authType)
                    throws java.security.cert.CertificateException {}

            @Override
            public void checkServerTrusted(java.security.cert.X509Certificate[] chain, String authType)
                    throws java.security.cert.CertificateException {}

        };

        sslContext.init(null, new TrustManager[] { tm }, null);
        */

        sslContext.init(null, null, null);
    }




    @Override
    public Socket createSocket(Socket socket, String host, int port, boolean autoClose)
            throws IOException, UnknownHostException {
        return enableTLSOnSocket(sslContext.getSocketFactory().createSocket(socket, host, port,autoClose));

    }

    @Override
    public Socket createSocket()
            throws IOException {
        return enableTLSOnSocket(sslContext.getSocketFactory().createSocket());
    }

    private Socket enableTLSOnSocket(Socket socket) {
        ((SSLSocket) socket).setEnabledProtocols(new String[] {"TLSv1.2"});
        return socket;
    }

    public static SSLSocketFactory getSocketFactory() {
        KeyStore trustStore;
        try {
            trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
            trustStore.load(null, null);
            return new TLSv12SocketFactory(trustStore);
        } catch (Exception e) {
            return null;
        }
    }

}
