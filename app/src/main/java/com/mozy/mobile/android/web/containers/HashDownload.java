package com.mozy.mobile.android.web.containers;

import java.util.ArrayList;

public class HashDownload extends Download {

    public ArrayList<Object> list;
    
    public HashDownload()
    {
        list = null;
    }
    
    public HashDownload(ArrayList<Object> list)
    {
        this.list = list;
    }
}
