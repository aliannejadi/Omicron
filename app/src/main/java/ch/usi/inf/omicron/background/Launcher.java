package ch.usi.inf.omicron.background;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import ch.usi.inf.omicron.Utils;

public class Launcher extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Intent service = new Intent(context, BackgroundRecorder.class);
        Utils.startService(context, service);
    }
}
