package com.example.android.spaceContext;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.widget.Toast;
public class OrientationBCastReceiver extends BroadcastReceiver {
    String ACTION_CONTEXT_ORIENTATION= "ACTION_CONTEXT_ORIENTATION";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (ConnectivityManager.CONNECTIVITY_ACTION.equals(intent.getAction())) {
            boolean noConnectivity = intent.getBooleanExtra(
                    ConnectivityManager.EXTRA_NO_CONNECTIVITY, false
            );
            if (noConnectivity) {
                Toast.makeText(context, "Disconnected", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(context, "Connected", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
