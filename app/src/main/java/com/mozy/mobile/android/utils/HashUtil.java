package com.mozy.mobile.android.utils;

import java.security.MessageDigest;

public class HashUtil
{
    public static String md5(String str) throws Exception
    {
        StringBuilder sb = new StringBuilder();
        for (byte b : md5(str.getBytes()))
        sb.append(Integer.toHexString(0x100 + (b & 0xff)).substring(1));
        return sb.toString();
    }

    public static byte[] md5(byte[] data) throws Exception
    {
        MessageDigest md5 = MessageDigest.getInstance("MD5");
        md5.update(data);
        return md5.digest();
    }
}