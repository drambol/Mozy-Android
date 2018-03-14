package com.mozy.mobile.android.client;

public class MipHttpClient extends DechoHttpClient 
{
    private static final int HTTPS_PORT = 443;    
    private int m_overridePort = 0;

    public MipHttpClient(boolean bAcceptAllCerts)
    {
        super(bAcceptAllCerts);
    }
    public MipHttpClient(boolean bAcceptAllCerts, String portString)
    {
        this(bAcceptAllCerts);
        if (portString.length() == 0)
        {
            m_overridePort = 0;
        }
        else
        {
            try
            {
                m_overridePort = Integer.parseInt(portString);
            }
            catch (NumberFormatException nfe)
            {
                m_overridePort = 0;
            }
        }
    }

    // Overrides base class method
    protected int getHttpsPort()
    {
        return (m_overridePort == 0 ? MipHttpClient.HTTPS_PORT : m_overridePort);
    }    
    
}
