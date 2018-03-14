package com.mozy.mobile.android.catch_release.queue;

import android.content.Context;

public class QueueManager {
    
    public static Queue getQueue(Context context, int uploadType) {
        QueueDatabase db_helper = new QueueDatabase(context, uploadType);
        Queue queue = new Queue(db_helper, uploadType);
        return queue;
    }
}
