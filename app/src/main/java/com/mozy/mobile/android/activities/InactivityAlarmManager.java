/* Copyright 2009 Tactel AB, Sweden. All rights reserved.
 *                                    _           _
 *       _                 _        | |         | |
 *     _| |_ _____  ____ _| |_ _____| |    _____| |__
 *    (_   _|____ |/ ___|_   _) ___ | |   (____ |  _ \
 *      | |_/ ___ ( (___  | |_| ____| |   / ___ | |_) )
 *       \__)_____|\____)  \__)_____)\_)  \_____|____/
 *
 */

package com.mozy.mobile.android.activities;

import com.mozy.mobile.android.utils.LogUtil;
import com.mozy.mobile.android.utils.SystemState;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

public class InactivityAlarmManager {

    private static final String ACTION_INACTIVITY_ALARM = "com.mozy.mobile.android.action.inactivity_alarm";

    private static InactivityAlarmManager instance = null;
    private static final Object lock = new Object();

    private Context context;
    private boolean alarmTriggered;
    private boolean alarmNotified;
    private boolean active;
    private Activity listener;
    private BroadcastReceiver receiver;
    
    private InactivityAlarmManager(Context context) {
        this.context = context;
        alarmTriggered = false;
        alarmNotified = false;
        active = false;
        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();

                if (action.equals(ACTION_INACTIVITY_ALARM)) {
                    triggerSecurity();
                }
                else if (action.equals(Intent.ACTION_TIME_CHANGED)) {
                    triggerSecurity();
                }
                else if (action.equals(Intent.ACTION_SCREEN_OFF)) {
                    triggerSecurity();
                }
            }
        };
    }

    public static InactivityAlarmManager getInstance(Context context) {
        synchronized(lock) {
            if (instance == null) {
                instance = new InactivityAlarmManager(context.getApplicationContext());
            }
        }

        return instance;
    }

    public synchronized void activate() {
        LogUtil.enter(this, "activate");
        
        if (!active) {
            active = true;
            alarmTriggered = false;
            alarmNotified = false;
            
            IntentFilter filter = new IntentFilter();
            filter.addAction(ACTION_INACTIVITY_ALARM);
            filter.addAction(Intent.ACTION_TIME_CHANGED);
            filter.addAction(Intent.ACTION_SCREEN_OFF);
            context.registerReceiver(receiver, filter);
            if (SystemState.getStopped())
            {
                triggerSecurity();
            }
        }
    }

    public synchronized void deactivate() {
        LogUtil.enter(this, "deactivate");
        alarmTriggered = false;
        alarmNotified = false;

        if (active) {
            active = false;

            AlarmManager am = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
            am.cancel(buildIntent(context));

            context.unregisterReceiver(receiver);
        }
    }

    public synchronized void update(long millis) {
        LogUtil.enter(this, "update(millis: " + millis + ")");
        if (active) {
            AlarmManager am = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);

            am.set(AlarmManager.RTC, System.currentTimeMillis() + millis, buildIntent(context));
        }
    }

    public synchronized void reset() {
        alarmTriggered = false;
        alarmNotified = false;
    }
    
    public boolean isAlarmReset()
    {
        return !alarmTriggered && !alarmNotified;
    }
    
    public boolean isAlarmTriggered()
    {
        return alarmNotified;
    }

    public synchronized void resetNotification() {
        alarmNotified = false;
    }
    
    public synchronized void registerActivity(Activity listener) {
        this.listener = listener;

        if (active && listener != null && alarmTriggered) {
            SecuredActivity.triggerSecurity(listener, alarmNotified);
            alarmNotified = true;
        }
    }

    public synchronized void unregisterActivity(Activity listener) {
        if (this.listener.equals(listener)) {
            this.listener = null;
        }
    }

    private synchronized void triggerSecurity() {
        LogUtil.enter(this, "triggerSecurity");
        if (active) {
            alarmTriggered = true;
            if (listener != null) {
                SecuredActivity.triggerSecurity(listener, alarmNotified);
                alarmNotified = true;
            }
        }
    }

    private static PendingIntent buildIntent(Context context) {
        Intent intent = new Intent(ACTION_INACTIVITY_ALARM);
        PendingIntent sender = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        return sender;
    }
}
