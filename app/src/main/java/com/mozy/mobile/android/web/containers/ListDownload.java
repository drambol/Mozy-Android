/*  Copyright Tactel AB 2009
 * 
 *  All copyrights in this software are created and owned by Tactel AB. 
 *  This software, or related intellectual property, may under no 
 *  circumstances be used, distributed or modified without written 
 *  authorization from Tactel AB. 
 *  This copyright notice may not be removed or modified and  shall be 
 *  displayed in all materials that include the software or portions of such.
 */

package com.mozy.mobile.android.web.containers;

import java.util.ArrayList;

import com.mozy.mobile.android.activities.ErrorCodes;

public class ListDownload extends Download
{
    public int totalCount;
    public ArrayList<Object> list;
    // The following is used to support pagination in MIP. When paging this property will be set to the id that starts
    // the next page (list of entries) to be returned from the server.
    public String nextIndex = null;

    public ListDownload()
    {
        this.errorCode = ErrorCodes.NO_ERROR;
        totalCount = 0;
        list = null;
    }

    public ListDownload(int totalCount, ArrayList<Object> list)
    {
        this.totalCount = totalCount;
        this.list = list;
    }

    public ListDownload(int totalCount, ArrayList<Object> list, String index)
    {
        this.totalCount = totalCount;
        this.list = list;
        this.nextIndex = index;
    }

    public void setData(int totalCount, ArrayList<Object> list)
    {
        this.totalCount = totalCount;
        this.list = list;
    }

    public void setData(int totalCount, ArrayList<Object> list, String index)
    {
        this.totalCount = totalCount;
        this.list = list;
        this.nextIndex = index;
    }
}
