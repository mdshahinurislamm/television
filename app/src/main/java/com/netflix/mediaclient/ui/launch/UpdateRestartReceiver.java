package com.netflix.mediaclient.ui.launch;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class UpdateRestartReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context ctx, Intent intent) {
        Intent i = new Intent(ctx, UIWebViewActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        ctx.startActivity(i);
    }
}
