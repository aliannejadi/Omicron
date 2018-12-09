package ch.usi.inf.omicron.background;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import ch.usi.inf.omicron.Log;
import ch.usi.inf.omicron.Utils;

public class BootReceiver extends BroadcastReceiver {

    // log tag
    static private String BOOTR = "umob.Receiver.boot";

    @Override
    public void onReceive(Context context, Intent i) {

        Intent intent = new Intent(context, BackgroundRecorder.class);
        Utils.startService(context, intent);
        Log.d(BOOTR, "background service started");

    }

}
