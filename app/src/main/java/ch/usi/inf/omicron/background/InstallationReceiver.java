package ch.usi.inf.omicron.background;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import ch.usi.inf.omicron.Log;

import static ch.usi.inf.omicron.UMob.tm;
import static ch.usi.inf.omicron.background.BackgroundRecorder.insE;
import static ch.usi.inf.omicron.background.BackgroundRecorder.insN;
import static ch.usi.inf.omicron.background.BackgroundRecorder.insT;
import static ch.usi.inf.omicron.background.BackgroundRecorder.insTaskid;
import static ch.usi.inf.omicron.background.BackgroundRecorder.keepStalking;

public class InstallationReceiver extends BroadcastReceiver {

    // log tag
    private static final String INSATLLR = "umob.Receiver.install";
    public static boolean installationReceiverOnline = false;

    public InstallationReceiver() {
        super();
        Log.i(INSATLLR, "Installation receiver created.");
        installationReceiverOnline = true;
    }

    @Override
    public void onReceive(Context context, Intent intent) {

        if (intent.getAction().equals(Intent.ACTION_PACKAGE_ADDED)) {

            Log.i(INSATLLR, "Installed new app: " + intent.getDataString());

            if (keepStalking) {
                insT.add(System.currentTimeMillis());
                insE.add("i");
                insN.add(intent.getDataString());
            }

        } else if (intent.getAction().equals(Intent.ACTION_PACKAGE_REMOVED)) {

            Log.i(INSATLLR, "Uninstalled app: " + intent.getDataString());

            if (keepStalking) {
                insT.add(System.currentTimeMillis());
                insE.add("u");
                insN.add(intent.getDataString());
                insTaskid.add(tm.getActiveTaskId());
            }

        }

    }

}
