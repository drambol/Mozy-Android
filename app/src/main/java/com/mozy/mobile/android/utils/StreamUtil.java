package com.mozy.mobile.android.utils;

import java.io.*;

public class StreamUtil 
{
    // This conversion will remove any new-lines, but the JSON parser does not care.
    public static String JsonStreamToString(InputStream data) throws IOException
    {
        InputStreamReader streamReader = new InputStreamReader(data);
        BufferedReader reader = new BufferedReader(streamReader);
        StringBuilder stringBuilder = new StringBuilder(256);
        String read=null;
        
        while ((read = reader.readLine()) != null)
        {
            stringBuilder.append(read);
        }
        
        data.close();
        
        return stringBuilder.toString();
    }
}
