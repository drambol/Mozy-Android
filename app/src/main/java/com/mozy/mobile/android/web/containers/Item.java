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

import java.util.HashMap;

import android.os.Parcel;
import android.os.Parcelable;

public class Item implements Parcelable
{
    public String id;
    public String name;
    public int category;
    
    public HashMap<String, String> metadata;
    
    public Item()
    {
        this.metadata = new HashMap<String, String>();
    }
    
    public String getDisplayName()
    {
        return name;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(id);
        dest.writeString(name);
        dest.writeInt(category);
        dest.writeInt(metadata.size());
        for (String s: metadata.keySet()) {
            dest.writeString(s);
            dest.writeString(metadata.get(s));
        }
    }
    
    public Item(Parcel in) {
        metadata = new HashMap<String, String>();
        readFromParcel(in);
    }
 
    @SuppressWarnings({ "rawtypes" })
    public static final Parcelable.Creator CREATOR = new Parcelable.Creator() {
        public Item createFromParcel(Parcel in) {
            return new Item(in);
        }
 
        public Item[] newArray(int size) {
            return new Item[size];
        }
    };
    
    public void readFromParcel(Parcel in) {
        id = in.readString();
        name = in.readString();
        category = in.readInt();
        int count = in.readInt();
        for (int i = 0; i < count; i++) {
            metadata.put(in.readString(), in.readString());
        }
    }
}
