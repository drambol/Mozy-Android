package com.mozy.mobile.android.web.containers;

import java.io.Serializable;

import com.mozy.mobile.android.activities.ErrorCodes;

public class Quota implements Serializable
{
    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    
    public String mContainerName;
    public long mQuotaTotal;
    public long mQuotaUsed;
    public String mQuotaTotalFormatted;
    public String mQuotaUsedFormatted;
    public int mErrorCode;
    
    public Quota(String strContainerName, long quotaUsed, long quotaTotal, String strQuotaUsedFormatted, String strQuotaTotalFormatted, int errorCode)
    {
        mContainerName = strContainerName;
        mQuotaUsed = quotaUsed;
        mQuotaTotal = quotaTotal;
        mErrorCode = errorCode;
        mQuotaUsedFormatted = strQuotaUsedFormatted;
        mQuotaTotalFormatted = strQuotaTotalFormatted;
    }
    
    public Quota(String strContainerName)
    {
        mContainerName = strContainerName;
        mErrorCode = ErrorCodes.NO_ERROR;
        mQuotaUsedFormatted = "0";
        mQuotaTotalFormatted = "0";        
        mQuotaUsed = 0;
        mQuotaTotal = 1;
    }
}
