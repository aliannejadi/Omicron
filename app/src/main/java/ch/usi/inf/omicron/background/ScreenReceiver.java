package ch.usi.inf.omicron.background;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import ch.usi.inf.omicron.Log;

import static ch.usi.inf.omicron.UMob.tm;
import static ch.usi.inf.omicron.background.BackgroundRecorder.keepStalking;
import static ch.usi.inf.omicron.background.BackgroundRecorder.scrE;
import static ch.usi.inf.omicron.background.BackgroundRecorder.scrT;
import static ch.usi.inf.omicron.background.BackgroundRecorder.scrTaskid;

public class ScreenReceiver extends BroadcastReceiver {

    // log tag
    private static final String SCREENR = "umob.Receiver.screen";
    public static boolean screenReceiverOnline = false;

    public ScreenReceiver() {
        super();
        screenReceiverOnline = true;
    }

    @Override
    public void onReceive(final Context context, final Intent intent) {

        if (intent.getAction().equals(Intent.ACTION_SCREEN_OFF)) {

            Log.i(SCREENR, "screen shut down");

            if (keepStalking) {
                scrT.add(System.currentTimeMillis());
                scrE.add("off");
                scrTaskid.add(tm.getActiveTaskId());
            }

        } else if (intent.getAction().equals(Intent.ACTION_SCREEN_ON)) {

            Log.i(SCREENR, "screen turned on");

            if (keepStalking) {
                scrT.add(System.currentTimeMillis());
                scrE.add("on");
            }

        }

    }

}
