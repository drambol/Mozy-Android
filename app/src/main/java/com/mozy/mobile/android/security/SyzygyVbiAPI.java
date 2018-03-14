package com.mozy.mobile.android.security;

import com.mozy.mobile.android.utils.LogUtil;

public class SyzygyVbiAPI {
	public native int restore(String infile, String outfile, byte [] decryptKey);
	public native int baseline(String infile, String outfile, byte [] decryptKey);
	public native byte [] compressUserKey(String passPhrase);
	public native int test();

    static {
        try {
            System.loadLibrary("syzygyvbi");
        }
        catch(Throwable e)
        {
            LogUtil.error("SyzygyVbiAPI", e.toString());
            throw new RuntimeException(e);
        }
    }
}
