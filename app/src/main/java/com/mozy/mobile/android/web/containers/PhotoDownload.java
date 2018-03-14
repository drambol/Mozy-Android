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

import android.graphics.Bitmap;

public class PhotoDownload extends Download
{
    public String id;
    public Bitmap bitmap;
    public String name;
    
    public PhotoDownload(String id)
    {
        this.id = id;
    }
    
    public PhotoDownload(Bitmap bitmap, String id, String name)
    {
        this.id = id;
        this.bitmap = bitmap;
        this.name = name;
    }
}
