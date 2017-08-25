package com.yzy.supercleanmaster.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.yzy.supercleanmaster.service.CoreService;

public class UserPresentReceiver extends BroadcastReceiver {
    private static final String TAG = "UserPresentReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        context.startService(new Intent(CoreService.ACTION_CLEAN_BG_PROCESSES, null, context, CoreService.class));
    }

}
