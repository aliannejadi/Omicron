package ch.usi.inf.omicron.background;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.google.android.gms.location.ActivityTransition;
import com.google.android.gms.location.ActivityTransitionEvent;
import com.google.android.gms.location.ActivityTransitionResult;
import com.google.android.gms.location.DetectedActivity;

import ch.usi.inf.omicron.Log;

import static ch.usi.inf.omicron.UMob.tm;
import static ch.usi.inf.omicron.background.BackgroundRecorder.actE;
import static ch.usi.inf.omicron.background.BackgroundRecorder.actT;
import static ch.usi.inf.omicron.background.BackgroundRecorder.actTaskid;
import static ch.usi.inf.omicron.background.BackgroundRecorder.keepStalking;

/**
 * Activities recognition
 **/

public class TransitionReceiver extends BroadcastReceiver {

    // log tag
    public static final String TRANR = "umob.Receiver.transition";
    public static boolean transitionReceiverOnline = false;

//    public TransitionReceiver() {
//        super();
//        transitionReceiverOnline = true;
//        Log.v(TRANR, "Starting Transition Receiver");
//    }

    private String getActivityString(int detectedActivityType) {
        switch (detectedActivityType) {
            case DetectedActivity.IN_VEHICLE:
                return "DRIVING";
            case DetectedActivity.ON_BICYCLE:
                return "BICYCLING";
            case DetectedActivity.ON_FOOT:
                return "STANDING";
            case DetectedActivity.RUNNING:
                return "RUNNING";
            case DetectedActivity.STILL:
                return "STILL";
            case DetectedActivity.TILTING:
                return "TILTING";
            case DetectedActivity.UNKNOWN:
                return "UNKNOWN";
            case DetectedActivity.WALKING:
                return "WALKING";
            default:
                return "NOT_DETECTED";
        }
    }

    private String getTransitionString(int transitionType) {
        switch (transitionType) {
            case ActivityTransition.ACTIVITY_TRANSITION_ENTER:
                return "START";
            case ActivityTransition.ACTIVITY_TRANSITION_EXIT:
                return "STOP";
            default:
                return "";
        }
    }

    @Override
    public void onReceive(final Context context, final Intent intent) {
        if (intent != null) {
            if (ActivityTransitionResult.hasResult(intent)) {
                ActivityTransitionResult result = ActivityTransitionResult.extractResult(intent);
                assert result != null;
                for (ActivityTransitionEvent event : result.getTransitionEvents()) {
                    //Toast.makeText(this, event.getTransitionType() + "-" + event.getActivityType(), Toast.LENGTH_LONG).show();
                    String e = getTransitionString(event.getTransitionType()) + "_" + getActivityString(event.getActivityType());
                    Log.i(TRANR, e);

                    if (keepStalking) {
                        actT.add(System.currentTimeMillis());
                        actE.add(e);
                        actTaskid.add(tm.getActiveTaskId());
                    }
                }
            }
        }

    }
}
