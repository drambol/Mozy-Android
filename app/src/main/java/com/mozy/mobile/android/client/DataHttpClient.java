package com.mozy.mobile.android.client;

// This client will be used to connect to URLs returned by the MIP API. These URLs address a different server
// than the MIP, and need to use a different port.

public class DataHttpClient extends DechoHttpClient {
    
    private static final int HTTPS_PORT = 443;    
    

    public DataHttpClient(boolean bAcceptAllCerts)
    {
        super(bAcceptAllCerts);
    }

    // Overrides base class method
    protected int getHttpsPort()
    {
        return DataHttpClient.HTTPS_PORT;
    }        

}
