package ch.usi.inf.omicron;

import android.app.Application;
import android.provider.Settings;

import com.crashlytics.android.Crashlytics;

import ch.usi.inf.omicron.background.BackgroundSemaphore;
import ch.usi.inf.omicron.background.RecordStorageManager;
import ch.usi.inf.omicron.taskManager.TaskManager;
import io.fabric.sdk.android.Fabric;

/**
 * This class is needed for instantiating the RecordStorageManager each time the device starts any
 * UMob activity or service. This allows the RecordStorageManager to always be available to the app
 * when needed.
 */

public class UMob extends Application {

    // instance id
    public static String iid;

    // record storage manager for offline recording
    public static RecordStorageManager rsm;

    // task manager
    public static TaskManager tm;

    // recording semaphore to avoid having too many records queued
    public static BackgroundSemaphore semaphore;

    @Override
    public void onCreate() {

        super.onCreate();
        Fabric.with(this, new Crashlytics());

        iid = Settings.Secure.getString(getApplicationContext().getContentResolver(),
                Settings.Secure.ANDROID_ID);

        // initialize record storage manager
        rsm = new RecordStorageManager(this.getApplicationContext());

        // initialize task manager
        tm = new TaskManager(this.getApplicationContext());

        // initialize background semaphore
        semaphore = new BackgroundSemaphore();

    }

}
