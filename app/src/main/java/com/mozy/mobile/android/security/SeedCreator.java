/* Copyright 2009 Tactel AB, Sweden. All rights reserved.
*                                   _           _
*       _                 _        | |         | |
*     _| |_ _____  ____ _| |_ _____| |    _____| |__
*    (_   _|____ |/ ___|_   _) ___ | |   (____ |  _ \
*      | |_/ ___ ( (___  | |_| ____| |   / ___ | |_) )
*       \__)_____|\____)  \__)_____)\_)  \_____|____/
*
*/

package com.mozy.mobile.android.security;

import com.mozy.mobile.android.provisioning.Provisioning;

import android.content.Context;
import android.telephony.TelephonyManager;

/**
 * Creates seeds.
 * 
 * @author Daniel Olofsson (daniel.olofsson@tactel.se)
 *
 */
public class SeedCreator {
    public static String getDefaultSeed(Context context, boolean with_imei) {
        String seed = "f30483hthuir23h1243f3f2deu3fbvbu";
        if (context != null && with_imei) {
            TelephonyManager mTelephonyMgr = 
                (TelephonyManager)context.getSystemService(Context.TELEPHONY_SERVICE);  
     
            String imei = mTelephonyMgr.getDeviceId();
            if (imei == null) {
                return null;
            }
            
            seed += imei;
        }
        
        seed += Provisioning.getUsername(context);
        
        return seed;
    }
    
    public static String getProvisioningSeed(Context context, String pre_seed) {
        return pre_seed + Provisioning.getUsername(context);
    }
}
