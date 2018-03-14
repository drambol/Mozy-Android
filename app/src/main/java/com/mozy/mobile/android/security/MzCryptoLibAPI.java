
package com.mozy.mobile.android.security;

import android.os.Build;

import com.mozy.mobile.android.utils.LogUtil;
import com.mozy.mobile.android.utils.SystemState;


public class MzCryptoLibAPI 
{
    private long AESFileRangeStart;
    private long AESFileRangeLen;
    private long BFFileRangeStart;
    private long BFFileRangeLen;
    
    private int cipher;
    
    public MzCryptoLibAPI()
    {
        AESFileRangeStart = 0;
        AESFileRangeLen = 0;
        BFFileRangeStart = 0;
        BFFileRangeLen = 0;
        cipher = -1;
    }
      
    public long getFileRangeStart(int cipher) {
        if(cipher == SystemState.CIPHER_BLOWFISH)
            return BFFileRangeStart;
        else if (cipher == SystemState.CIPHER_AES)
            return AESFileRangeStart;
        return -1;
    }


    public long getFileRangeLen(int cipher) {
        if(cipher == SystemState.CIPHER_BLOWFISH)
            return BFFileRangeLen;
        else if (cipher == SystemState.CIPHER_AES)
            return AESFileRangeLen;
        return -1;
    }


    public int getCipher() {
        return cipher;
    }



    public native boolean initializeAndSetupHints(int platform, int srcType, int cipher, long fileSize);
    
    public native boolean performDetection(String passPhrase);
    
    public native boolean GetByterangeForValidate(long filelen);
    
    public native byte[] validateKey( byte[] AESbuffer, byte[] BFbuffer, long filelen);
    
    public native byte[] getCkey( byte[] busdata);
    
    public native void cleanUp();
    
    static
    {  
        try
        {
            System.loadLibrary("iconv");
            
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.FROYO) 
            {
                 System.loadLibrary("mzcryptostatic");
            }
             else
             {
                 System.loadLibrary("crypto");
                 System.loadLibrary("mzcrypto");
             }
        }
        catch(Throwable e)
        {
            LogUtil.error("MzCryptoLibAPI", e.toString());
            throw new RuntimeException(e);
        }
    }
   
}

