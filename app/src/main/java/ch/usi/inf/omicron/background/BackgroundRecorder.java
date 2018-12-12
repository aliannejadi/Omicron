package ch.usi.inf.omicron.background;

import android.Manifest;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.app.usage.UsageEvents;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.net.wifi.WifiManager;
import android.os.BatteryManager;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.telephony.CellInfo;
import android.telephony.TelephonyManager;
import android.util.Base64;

import com.crashlytics.android.answers.Answers;
import com.crashlytics.android.answers.CustomEvent;
import com.google.android.gms.location.ActivityRecognition;
import com.google.android.gms.location.ActivityRecognitionClient;
import com.google.android.gms.location.ActivityTransition;
import com.google.android.gms.location.ActivityTransitionRequest;
import com.google.android.gms.location.DetectedActivity;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.UploadTask;

import java.io.File;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

import ch.usi.inf.omicron.BuildConfig;
import ch.usi.inf.omicron.Log;
import ch.usi.inf.omicron.Utils;
import ch.usi.inf.omicron.recordCommuter.AccelerometerRecordCommuter;
import ch.usi.inf.omicron.recordCommuter.ActivitiesRecordCommuter;
import ch.usi.inf.omicron.recordCommuter.BatteryRecordCommuter;
import ch.usi.inf.omicron.recordCommuter.CellRecordCommuter;
import ch.usi.inf.omicron.recordCommuter.GyroscopeRecordCommuter;
import ch.usi.inf.omicron.recordCommuter.LightRecordCommuter;
import ch.usi.inf.omicron.recordCommuter.LocationRecordCommuter;
import ch.usi.inf.omicron.recordCommuter.ScreenRecordCommuter;
import ch.usi.inf.omicron.recordCommuter.UsageRecordCommuter;
import ch.usi.inf.omicron.recordCommuter.WLANRecordCommuter;

import static android.app.usage.UsageStatsManager.INTERVAL_DAILY;
import static ch.usi.inf.omicron.Configuration.USAGE_INTERVAL;
import static ch.usi.inf.omicron.Configuration.locationRate;
import static ch.usi.inf.omicron.Configuration.recordRate;
import static ch.usi.inf.omicron.Configuration.sampleRate;
import static ch.usi.inf.omicron.Configuration.shouldRecordAccelerometer;
import static ch.usi.inf.omicron.Configuration.shouldRecordActivities;
import static ch.usi.inf.omicron.Configuration.shouldRecordAppUsage;
import static ch.usi.inf.omicron.Configuration.shouldRecordBattery;
import static ch.usi.inf.omicron.Configuration.shouldRecordCell;
import static ch.usi.inf.omicron.Configuration.shouldRecordGyroscope;
import static ch.usi.inf.omicron.Configuration.shouldRecordLight;
import static ch.usi.inf.omicron.Configuration.shouldRecordLocation;
import static ch.usi.inf.omicron.Configuration.shouldRecordScreen;
import static ch.usi.inf.omicron.Configuration.shouldRecordWLAN;
import static ch.usi.inf.omicron.UMob.rsm;
import static ch.usi.inf.omicron.background.InstallationReceiver.installationReceiverOnline;
import static ch.usi.inf.omicron.background.RecordStorageManager.PENDING_DIR;
import static ch.usi.inf.omicron.background.RecordStorageManager.WORKER_DIR;
import static ch.usi.inf.omicron.background.RecordStorageManager.moveFile;
import static ch.usi.inf.omicron.background.ScreenReceiver.screenReceiverOnline;
import static ch.usi.inf.omicron.background.TransitionReceiver.TRANR;

/**
 * Thr BackgroundRecorder class handles all the main recording features of the application which do
 * not require user input and/or interaction, which means all the hardware sensor data of the device
 * is here recorded. It gets the data from the sensors, instantiates the relative RecordCommuter
 * objects producing Record objects and passes the Records to the RecordStorageManager leaving the
 * actual local storage and Firebase submission tasks to it.
 */

public class BackgroundRecorder extends Service implements SensorEventListener {

    // log tags
    public static final String CREATE = "umob.Rec.create";
    public static final String DESTROY = "umob.Rec.destroy";
    public static final String START = "umob.Rec.start";
    public static final String RRUN = "umob.Rec.s";
    public static final String WLAN = "umob.Rec.s.wlan";
    public static final String CALL = "umob.Rec.s.call";
    public static final String SMS = "umob.Rec.s.sms";
    public static final String LOCATION = "umob.Rec.s.location";
    public static final String CELL = "umob.Rec.s.cell";
    public static final String ACCELEROMETER = "umob.Rec.s.accel";
    public static final String GYROSCOPE = "umob.Rec.s.gyroscope";
    public static final String LIGHT = "umob.Rec.s.light";
    public static final String BATTERY = "umob.Rec.s.battery";
    public static final String SCREEN = "umob.Rec.s.screen";
    public static final String ACTIVITIES = "umob.Rec.s.activities";
    public static final String URUN = "umob.Rec.u";
    public static final String USAGE = "umob.Rec.u.usage";
    // intent accessed by the main activity
    public static Intent serviceIntent;
    // flag used to stop the service
    public static boolean keepStalking = false;
    // screen sample list
    protected static List<String> scrE = new ArrayList<>();
    protected static List<Long> scrT = new ArrayList<>();
    protected static List<String> scrTaskid = new ArrayList<>();
    // installation sample list
    protected static List<String> insE = new ArrayList<>();
    protected static List<String> insN = new ArrayList<>();
    protected static List<Long> insT = new ArrayList<>();
    protected static List<String> insTaskid = new ArrayList<>();
    // activities sample list
    protected static List<String> actE = new ArrayList<>();
    protected static List<Long> actT = new ArrayList<>();
    protected static List<String> actTaskid = new ArrayList<>();
    // The intent action which will be fired when transitions are triggered.
    private final String TRANSITIONS_RECEIVER_ACTION =
            BuildConfig.APPLICATION_ID + "TRANSITIONS_RECEIVER_ACTION";
    private final IBinder rBinder = new RecorderBinder();
    SharedPreferences prefs;
    SharedPreferences sharedPref;
    //Mohammad: to keep the phone awake...
    PowerManager pm;
    PowerManager.WakeLock wl;
    // instance ID of the user the activity that created this service
    private String iid;
    // Firebase Runnable started
    private boolean firebaseIsRunning = false;
    // handler that manages the sample rate
    private Handler h = new Handler();
    // the runnable that will be called each usageRate milliseconds
    private final Runnable u = new Runnable() {

        // the sample rate for apps usage
        private long usageRate = USAGE_INTERVAL;

        @Override
        public void run() {

            if (shouldRecordAppUsage && keepStalking) {

                Log.d(URUN, "recording usage");
                recordUsageStats();

            }
            h.postDelayed(this, usageRate);

        }

    };
    // the runnable that will tell fabric each aliveRate that the background service is running
    private final Runnable a = new Runnable() {

        // the alive notification rate (4 hours)
        private long aliveRate = 1000 * 60 * 60 * 4;

        @Override
        public void run() {

            if (keepStalking) {

                Answers.getInstance().logCustom(new CustomEvent("Alive")
                        .putCustomAttribute("OS", Build.VERSION.RELEASE)
                        .putCustomAttribute("Model", Build.MODEL)
                        .putCustomAttribute("AppID", iid));

                Log.d("ALIVE", "Background service alive.");

            }
            h.postDelayed(this, aliveRate);

        }

    };
    // the runnable that is in charge of submitting the records to Firebase Storage
    private final Runnable r = new Runnable() {

        @Override
        public void run() {

            if (!firebaseIsRunning) {
                firebaseIsRunning = true;
            }

            if (keepStalking)
                Log.i("umob.moli.upload", "i am in runnable now still running baby");
            rsm.uploadRecords(getApplicationContext());
            h.postDelayed(this, recordRate);

        }

    };
    // receivers for android O
    private BroadcastReceiver mTransitionsReceiver;

    // variable used to lower the accelerometer sample rate
    private int accelFilterCounter = 0;
    // location Services client used for retrieving current device position
    private FusedLocationProviderClient mFusedLocationClient;
    // the runnable that will record location each locationRate milliseconds
    private final Runnable l = new Runnable() {

        @Override
        public void run() {

            if (shouldRecordLocation && keepStalking) {

                Log.d(URUN, "recording location");
                recordLocation();

            }
            h.postDelayed(this, locationRate);

        }

    };
    // accelerometer samples lists
    private List<Long> accT = new ArrayList<>();
    private List<Float> accX = new ArrayList<>();
    private List<Float> accY = new ArrayList<>();
    private List<Float> accZ = new ArrayList<>();
    // gyroscope samples lists
    private List<Long> gyrT = new ArrayList<>();
    private List<Float> gyrX = new ArrayList<>();
    private List<Float> gyrY = new ArrayList<>();
    private List<Float> gyrZ = new ArrayList<>();
    // light sample list
    private List<Long> lumT = new ArrayList<>();
    private List<Float> lumL = new ArrayList<>();
    // the runnable that will be called each sampleRate milliseconds
    private final Runnable s = new Runnable() {

        @Override
        public void run() {

            sharedPref = getSharedPreferences("shared_preferences",
                    MODE_PRIVATE);

            boolean permissionsGranted = sharedPref.getBoolean("permissionsGranted", false);

            if (keepStalking && permissionsGranted) {
                Log.i(RRUN, "recording data");
                if (shouldRecordWLAN) {
                    recordWLAN();
                }
                if (shouldRecordCell) {
                    recordCell();
                }
                if (shouldRecordAccelerometer) {
                    recordAccelerometer();
                }
                if (shouldRecordGyroscope) {
                    recordGyroscope();
                }
                if (shouldRecordLight) {
                    recordLight();
                }
                if (shouldRecordBattery) {
                    recordBattery();
                }
                if (shouldRecordScreen) {
                    recordScreen();
                }
                if (shouldRecordActivities) {
                    recordActivities();
                }
                //FIXME:lastsms/call is updated only after the first scheduling
                //Disabled for the play store
                /*
                //if (shouldRecordCall) {
                    recordCall();
                }
                if (shouldRecordSMS) {
                    recordSMS();
                }
                */
            }

            h.postDelayed(this, sampleRate);

        }

    };
    //Activities recognition
    private ActivityRecognitionClient activityRecognitionClient;
    private PendingIntent transitionPendingIntent;
    private Context mContext;
    // installation receiver
    private InstallationReceiver ir;
    // screen receiver
    private ScreenReceiver sr;

    public static String SHA256(String text) throws NoSuchAlgorithmException {

        MessageDigest md = MessageDigest.getInstance("SHA-256");

        md.update(text.getBytes());
        byte[] digest = md.digest();

        return Base64.encodeToString(digest, Base64.DEFAULT);
    }

    private void recoveryFirebase() {
        //check that the folder exists
        File workerDir = new File(getApplicationContext().getFilesDir() + WORKER_DIR);
        if (!workerDir.exists()) {
            workerDir.mkdirs();
            //First run of the app, dir has not yet been created
            Log.w("Recovery", "First run of the app, dir has not yet been created");
            return;
        }

        File[] workerFiles = workerDir.listFiles();
        if (workerFiles.length > 0) {
            for (File resendFile : workerFiles) {
                Log.i("Recovery", "Recovering " + resendFile + " file");
                //move to workerTmp
                File movedFile = new File(getApplicationContext().getFilesDir() + PENDING_DIR, resendFile.getName());
                try {
                    moveFile(resendFile, movedFile);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        //send records
        rsm.uploadRecords(getApplicationContext());
    }

    @Override
    public IBinder onBind(Intent intent) {
        return rBinder;
    }

    @Override
    public void onCreate() {

        Log.i(CREATE, "onCreate()");
        super.onCreate();

        // get the iid
        iid = Settings.Secure.getString(this.getContentResolver(), Settings.Secure.ANDROID_ID);
        Log.i(CREATE, "  iid retrieved: " + iid);

        prefs = PreferenceManager.getDefaultSharedPreferences(this);

        if (!prefs.getBoolean("firstLaunch", false)) {

            Log.i(CREATE, "  first launch: recording device model");

            //save last sms/call: initially -1 to avoid old records
            SharedPreferences.Editor editor = prefs.edit();
            editor.putLong("lastSMS", -1);
            editor.putLong("lastCALL", -1);
            editor.apply();

            if (RecordStorageManager.isConnectedToWifiAndHasInternetAccess(this)) {

                Log.i(CREATE, "  first launch: internet access confirmed");

                UploadTask firstUploadTask = FirebaseStorage.getInstance().getReference()
                        .child("user" + iid).child("model.info").putBytes(Build.MODEL.getBytes());
                firstUploadTask.addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {

                        SharedPreferences.Editor editor = prefs.edit();
                        editor.putBoolean("firstLaunch", true);
                        editor.apply();

                        Log.i(CREATE, "  first launch: completed");

                    }
                });

            } else Log.i(CREATE, "  first launch: no internet access, aborting.");

        }

        // foreground notification
        startNotification();

        // background service initialization
        Log.i(CREATE, "  setting up service");
        keepStalking = true;
        serviceIntent = new Intent(getApplicationContext(), BackgroundRecorder.class);
        h.postDelayed(s, sampleRate);
        h.post(u);
        h.post(a);
        h.post(l);
        h.postDelayed(r, recordRate);

        // retrieve Location Services client
        Log.i(CREATE, "  retrieving location client");
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // get sensors and register them
        Log.i(CREATE, "  registering sensors");
        SensorManager mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        assert mSensorManager != null;
        Sensor mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        Sensor mGyroscope = mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        Sensor mLight = mSensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
        mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        mSensorManager.registerListener(this, mGyroscope, SensorManager.SENSOR_DELAY_NORMAL);
        mSensorManager.registerListener(this, mLight, SensorManager.SENSOR_DELAY_NORMAL);

        // installation receiver registration
        if (!installationReceiverOnline) {
            Log.i(CREATE, "  registering installation receiver.");
            IntentFilter installFilter = new IntentFilter();
            installFilter.addAction(Intent.ACTION_PACKAGE_ADDED);
            installFilter.addAction(Intent.ACTION_PACKAGE_REMOVED);
            installFilter.addDataScheme("package");
            registerReceiver(ir = new InstallationReceiver(), installFilter);
        }

        // screen receiver registration
        if (!screenReceiverOnline) {
            Log.i(CREATE, "  registering screen receiver");
            IntentFilter filter = new IntentFilter(Intent.ACTION_SCREEN_ON);
            filter.addAction(Intent.ACTION_SCREEN_OFF);
            registerReceiver(sr = new ScreenReceiver(), filter);
        }

        //Mohammad
        //TODO:a more optimum way to have more accurate scheduled events.
//        Log.i(CREATE, " Setting up PowerManager to keep the phone awake.");
//        pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
//        wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "BackgroundRecorder");
//        wl.acquire();

        Answers.getInstance().logCustom(new CustomEvent("Background Running")
                .putCustomAttribute("OS", Build.VERSION.RELEASE)
                .putCustomAttribute("Model", Build.VERSION.RELEASE)
                .putCustomAttribute("AppID", iid));


        Log.i(CREATE, "  complete.");

        if (activityRecognitionClient == null) {
            startActivityTracking();
        }

        //Recovery Firebase unsent records if crashed
        recoveryFirebase();
    }

    public void startNotification() {
        Log.i(CREATE, "  setting up notification");
        Log.i(CREATE, "  starting service in foreground");

        Notification notification = Utils.buildNotification("", getApplicationContext()
        ).build();
        startForeground(1, notification);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        Log.i(START, "onStartCommand()");
        serviceIntent = new Intent(getApplicationContext(), BackgroundRecorder.class);

        return START_STICKY;

    }


    @Override
    public void onDestroy() {

        Log.i(DESTROY, "onDestroy()");
        keepStalking = false;
        h.removeCallbacks(s);
        h.removeCallbacks(u);
        h.removeCallbacks(a);
        h.removeCallbacks(l);
        h.removeCallbacks(r);
        if (ir != null)
            unregisterReceiver(ir);
        if (sr != null)
            unregisterReceiver(sr);
        if (mTransitionsReceiver != null)
            unregisterReceiver(mTransitionsReceiver);
        stopForeground(true);


        stopActivityTracking();

        super.onDestroy();

    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {
        Log.i("umob.Rec", sensor.getName() + " accuracy changed");
    }

    @Override
    public void onSensorChanged(SensorEvent event) {

        if (keepStalking) {

            if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {

                // accelerometer sampling factor (multiply it by 200000 to get the delay in microseconds)
//                final int ACCELEROMETER_DELAY_FACTOR = 1;

//                accelFilterCounter++;

//                if (accelFilterCounter % ACCELEROMETER_DELAY_FACTOR == 0) {

                long timeInMillis = System.currentTimeMillis() + ((event.timestamp -
                        SystemClock.elapsedRealtimeNanos()) / 1000000L);


                accT.add(timeInMillis);
                accX.add(event.values[0]);
                accY.add(event.values[1]);
                accZ.add(event.values[2]);

//                    accelFilterCounter = 0;

//                }

            }

            if (event.sensor.getType() == Sensor.TYPE_GYROSCOPE) {

                long timeInMillis = System.currentTimeMillis() + ((event.timestamp -
                        SystemClock.elapsedRealtimeNanos()) / 1000000L);

                gyrT.add(timeInMillis);
                gyrX.add(event.values[0]);
                gyrY.add(event.values[1]);
                gyrZ.add(event.values[2]);

            }

            if (event.sensor.getType() == Sensor.TYPE_LIGHT) {

                long timeInMillis = System.currentTimeMillis() + ((event.timestamp -
                        SystemClock.elapsedRealtimeNanos()) / 1000000L);

                lumT.add(timeInMillis);
                lumL.add(event.values[0]);

            }

        }

    }

    private void stopActivityTracking() {
        Task<Void> task = activityRecognitionClient.removeActivityTransitionUpdates(transitionPendingIntent);
        task.addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                transitionPendingIntent.cancel();
                //Toast.makeText(mContext, "Remove Activity Transition Successfully", Toast.LENGTH_LONG).show();
                Log.i(TRANR, "Remove Activity Transition Successfully");
            }
        });

        task.addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                //Toast.makeText(mContext, "Remove Activity Transition Failed", Toast.LENGTH_LONG).show();
                Log.i(TRANR, "Remove Activity Transition Failed");
                e.printStackTrace();
            }
        });
    }

    private void startActivityTracking() {
        Log.i(TRANR, "Starting activity tracking");
        ArrayList<ActivityTransition> transitions = new ArrayList<>();
        //start activity recognition
        mContext = getApplicationContext();
        activityRecognitionClient = ActivityRecognition.getClient(mContext);

        transitions.add(new ActivityTransition.Builder()
                .setActivityType(DetectedActivity.WALKING)
                .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_ENTER)
                .build());

        transitions.add(new ActivityTransition.Builder()
                .setActivityType(DetectedActivity.WALKING)
                .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_EXIT)
                .build());

        transitions.add(new ActivityTransition.Builder()
                .setActivityType(DetectedActivity.RUNNING)
                .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_ENTER)
                .build());

        transitions.add(new ActivityTransition.Builder()
                .setActivityType(DetectedActivity.RUNNING)
                .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_EXIT)
                .build());

        transitions.add(new ActivityTransition.Builder()
                .setActivityType(DetectedActivity.STILL)
                .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_ENTER)
                .build());

        transitions.add(new ActivityTransition.Builder()
                .setActivityType(DetectedActivity.STILL)
                .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_EXIT)
                .build());

        transitions.add(new ActivityTransition.Builder()
                .setActivityType(DetectedActivity.IN_VEHICLE)
                .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_ENTER)
                .build());

        transitions.add(new ActivityTransition.Builder()
                .setActivityType(DetectedActivity.IN_VEHICLE)
                .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_EXIT)
                .build());

        transitions.add(new ActivityTransition.Builder()
                .setActivityType(DetectedActivity.ON_BICYCLE)
                .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_ENTER)
                .build());

        transitions.add(new ActivityTransition.Builder()
                .setActivityType(DetectedActivity.ON_BICYCLE)
                .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_EXIT)
                .build());

        ActivityTransitionRequest activityTransitionRequest = new ActivityTransitionRequest(transitions);

        Intent intent = new Intent(TRANSITIONS_RECEIVER_ACTION);
        transitionPendingIntent = PendingIntent.getBroadcast(mContext, 0, intent, 0);

        mTransitionsReceiver = new TransitionReceiver();
        registerReceiver(mTransitionsReceiver, new IntentFilter(TRANSITIONS_RECEIVER_ACTION));

        Task<Void> task = activityRecognitionClient.requestActivityTransitionUpdates(activityTransitionRequest, transitionPendingIntent);

        task.addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                //Toast.makeText(mContext, "Transition update set up", Toast.LENGTH_LONG).show();
                Log.i(TRANR, "Transition update set up");
            }
        });

        task.addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                //Toast.makeText(mContext, "Transition update Failed to set up", Toast.LENGTH_LONG).show();
                Log.i(TRANR, "Transition update Failed to set up");
                e.printStackTrace();
            }
        });
    }


    // get the last sms information
 /*   private void recordSMS() {
        Log.i(SMS, "Recording sms");

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_SMS) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            Log.i(CALL, "Permission denied");
            return;
        }
        //save last sms/call: initially -1 to avoid old records
        SharedPreferences.Editor editor = prefs.edit();
        long lastSMS_TS = prefs.getLong("lastSMS", -1);

        //sms time, length, type, and the contact
        //the first time we seek the last call so we only collect present data
        if (lastSMS_TS == -1) {
            Cursor retLastTS = getContentResolver().query(Telephony.Sms.CONTENT_URI,
                    new String[]{CallLog.Calls.DATE},
                    null,
                    null,
                    Telephony.Sms.Inbox.DATE + " desc limit 1");

            //I have received a new sms
            assert retLastTS != null;
            if (retLastTS.moveToNext()) {
                lastSMS_TS = Long.parseLong(retLastTS.getString(retLastTS.getColumnIndex(Telephony.Sms.Inbox.DATE)));
                editor.putLong("lastSMS", lastSMS_TS);
                editor.apply();
            } else {
                editor.putLong("lastSMS", System.currentTimeMillis());
                editor.apply();
            }
            retLastTS.close();
            return;
        }

        //query the sms provider

        Cursor c = getContentResolver().query(Telephony.Sms.CONTENT_URI,
                new String[]{Telephony.Sms.ADDRESS, Telephony.Sms.DATE,
                        Telephony.Sms.TYPE, Telephony.Sms.BODY},
                Telephony.Sms.DATE + ">?",
                new String[]{String.valueOf(lastSMS_TS)},
                Telephony.Sms.Inbox.DATE + " asc");

        //columns indexes
        assert c != null;
        if (c == null) {
            return;
        }

        int num_id = c.getColumnIndex(Telephony.Sms.ADDRESS);// number column
        int body_id = c.getColumnIndex(Telephony.Sms.BODY); //body column
        int type_id = c.getColumnIndex(Telephony.Sms.TYPE); //call type column
        //int contact_id = c.getColumnIndex(Telephony.Sms.PERSON); //name column
        int ts_id = c.getColumnIndex(Telephony.Sms.DATE);
        //int sms_id = c.getColumnIndex(Telephony.Sms._ID); //call id

        ArrayList<String> numbers_hashes = new ArrayList<>();
        ArrayList<String> names = new ArrayList<>();
        ArrayList<String> bodies = new ArrayList<>();
        ArrayList<Integer> sms_types = new ArrayList<>();
        ArrayList<Long> timestamps = new ArrayList<>();

        boolean newResults = false;

        while (c.moveToNext()) {
            newResults = true;

            String number = c.getString(num_id);
            String body = c.getString(body_id);
            int sms_type = c.getInt(type_id);
            long timestamp = c.getLong(ts_id);
            //String contact = c.getString(contact_id);
            String name = "";

            //search in the contacts the contact name associated with the sms if we have one
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED) {

                Uri uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(number));
                Cursor cnt = getContentResolver().query(uri, new String[]{ContactsContract.PhoneLookup.DISPLAY_NAME,
                        ContactsContract.PhoneLookup._ID}, null, null, null);

                assert cnt != null;
                if (cnt.moveToFirst())
                    name = cnt.getString(cnt.getColumnIndex(ContactsContract.PhoneLookup.DISPLAY_NAME));

                cnt.close();
            }
            //hash the number + salt with the iid
            String number_hash = "";

            try {
                number_hash = SHA256(iid + number);
            } catch (NoSuchAlgorithmException e) {
                Log.i(CALL, e.getMessage());
            }

            Log.i(SMS, "number_hash " + number_hash + //" body " + body +
                    " type " + sms_type + " name " + name + "date " + timestamp);

            numbers_hashes.add(number_hash);
            names.add(name);
            bodies.add(body);
            sms_types.add(sms_type);
            timestamps.add(timestamp);


            //record the last call id for the next iteration
            lastSMS_TS = timestamp;
            editor.putLong("lastSMS", lastSMS_TS);
            editor.apply();

        }

        //I have found new SMS
        if (newResults) {
            SMSRecordCommuter comm = new SMSRecordCommuter(numbers_hashes, names, bodies, sms_types, timestamps);
            comm.storeLocally(iid, this.getApplicationContext());
        }
        c.close();
    }*/

    // get the last calls information


   /* public void recordCall() {
        Log.i(CALL, "Recording call history");

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_CALL_LOG) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            Log.i(CALL, "Permission denied");
            return;
        }

        SharedPreferences.Editor editor = prefs.edit();
        long lastCall_TS = prefs.getLong("lastCALL", -1);
        //the first time we seek the last call so we only collect present data
        if (lastCall_TS == -1) {
            Cursor retLastTS = getContentResolver().query(CallLog.Calls.CONTENT_URI,
                    new String[]{CallLog.Calls.DATE},
                    null,
                    null,
                    CallLog.Calls.DATE + " desc limit 1");
            assert retLastTS != null;
            if (retLastTS.moveToNext()) {
                lastCall_TS = Long.parseLong(retLastTS.getString(retLastTS.getColumnIndex(CallLog.Calls.DATE)));
                editor.putLong("lastCALL", lastCall_TS);
                editor.apply();
            } else {
                editor.putLong("lastCALL", System.currentTimeMillis());
                editor.apply();
            }
            retLastTS.close();
            return;
        }

        //query the call log provider
        Cursor c = getContentResolver().query(CallLog.Calls.CONTENT_URI,
                new String[]{CallLog.Calls.DATE, CallLog.Calls.DURATION,
                        CallLog.Calls.NUMBER, CallLog.Calls.CACHED_NAME,
                        CallLog.Calls.TYPE},
                CallLog.Calls.DATE + ">?",
                new String[]{String.valueOf(lastCall_TS)},
                CallLog.Calls.DATE + " asc");


        //Log.i(CALL, "elements query" +" - "+ c.getCount());

        //columns indexes
        assert c != null;
        if (c == null) {
            return;
        }

        int num_id = c.getColumnIndex(CallLog.Calls.NUMBER);// number column
        int name_id = c.getColumnIndex(CallLog.Calls.CACHED_NAME); //name column
        int duration_id = c.getColumnIndex(CallLog.Calls.DURATION); //duration column
        int type_id = c.getColumnIndex(CallLog.Calls.TYPE); //call type column
        int ts_id = c.getColumnIndex(CallLog.Calls.DATE);
        //int call_id = c.getColumnIndex(CallLog.Calls._ID); //call id

        ArrayList<String> numbers_hashes = new ArrayList<>();
        ArrayList<String> names = new ArrayList<>();
        ArrayList<Integer> durations = new ArrayList<>();
        ArrayList<Integer> call_types = new ArrayList<>();
        ArrayList<Long> timestamps = new ArrayList<>();

        boolean newResults = false;

        while (c.moveToNext()) {
            newResults = true;

            String number = c.getString(num_id);
            String name = c.getString(name_id);
            int duration = c.getInt(duration_id);
            int call_type = c.getInt(type_id);
            long timestamp = c.getLong(ts_id);


            //hash the number + salt with the iid
            String number_hash = "";

            try {
                number_hash = SHA256(iid + number);
            } catch (NoSuchAlgorithmException e) {
                Log.i(CALL, e.getMessage());
            }

            Log.i(CALL, "num " + number_hash + " name " + name + " duration "
                    + duration + " type " + call_type);

            numbers_hashes.add(number_hash);
            names.add(name);
            durations.add(duration);
            call_types.add(call_type);
            timestamps.add(timestamp);

            //record the last call id for the next iteration
            lastCall_TS = timestamp;
            editor.putLong("lastCALL", lastCall_TS);
            editor.apply();
        }

        if (newResults) {
            CallRecordCommuter comm = new CallRecordCommuter(numbers_hashes, names,
                    durations, call_types, timestamps);
            comm.storeLocally(iid, this.getApplicationContext());
        }
        c.close();

    }*/

    // get the list of all currently available access points
    public void recordWLAN() {

        Log.i(WLAN, "Recording wlan");
        // check for right location permissions and, if granted, retrieve WLANs
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                        == PackageManager.PERMISSION_GRANTED) {

            WifiManager wifiManager = (WifiManager) this.getApplicationContext()
                    .getSystemService(Context.WIFI_SERVICE);
            assert wifiManager != null;
            if (wifiManager.startScan()) {
                WLANRecordCommuter comm = new WLANRecordCommuter(wifiManager.getScanResults());
                comm.storeLocally(iid, this.getApplicationContext());
            }
        }

    }

    // get current coordinates of the device
    public void recordLocation() {

        Log.i(LOCATION, "Recording location");

        // check for right location permissions and, if granted, retrieve location
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                        == PackageManager.PERMISSION_GRANTED) {

            mFusedLocationClient.getLastLocation()
                    .addOnSuccessListener(new OnSuccessListener<Location>() {

                        @Override
                        public void onSuccess(Location location) {

                            // location correctly retrieved
                            if (location != null) {

                                LocationRecordCommuter comm = new LocationRecordCommuter(location);
                                comm.storeLocally(iid, getApplicationContext());

                            } else {

                                Log.d(LOCATION, "location retrieved was null.");

                            }

                        }
                    });
        }

    }

    // get the list of all currently reachable GSM cell towers
    public void recordCell() {

        Log.i(CELL, "Recording cell");

        // list of neraby cell towers info
        List<CellInfo> nearbyCellTowers;

        if (PackageManager.PERMISSION_GRANTED == ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION)) {

            TelephonyManager mTelephonyManager = (TelephonyManager) this.getApplicationContext()
                    .getSystemService(Context.TELEPHONY_SERVICE);
            assert mTelephonyManager != null;
            nearbyCellTowers = mTelephonyManager.getAllCellInfo();


            if (nearbyCellTowers == null) {

                Log.d(CELL, "failed to get cellInfo");

            } else {

                CellRecordCommuter comm = new CellRecordCommuter(nearbyCellTowers);
                comm.storeLocally(iid, this.getApplicationContext());

            }

        }

    }

    // get app usage stats
    public void recordUsageStats() {

        Log.i(USAGE, "Recording usage");

        UsageStatsManager mUsageStatsManager = (UsageStatsManager) this.getApplicationContext()
                .getSystemService(Context.USAGE_STATS_SERVICE);
        long startTime = System.currentTimeMillis() - USAGE_INTERVAL;
        long endTime = System.currentTimeMillis();

        assert mUsageStatsManager != null;
        List<UsageStats> usageList = mUsageStatsManager.queryUsageStats(INTERVAL_DAILY, startTime,
                endTime);
        UsageEvents usageEvents = mUsageStatsManager.queryEvents(startTime, endTime);
        UsageEvents.Event event = new UsageEvents.Event();

        ArrayList<String> eventApps = new ArrayList<>();
        ArrayList<Integer> eventTypes = new ArrayList<>();
        ArrayList<Long> eventTimestamps = new ArrayList<>();

        while (usageEvents.hasNextEvent()) {

            usageEvents.getNextEvent(event);

            eventApps.add(event.getPackageName());
            eventTypes.add(event.getEventType());
            eventTimestamps.add(event.getTimeStamp());

        }

        PackageManager packageManager = this.getApplicationContext().getPackageManager();

        UsageRecordCommuter comm = new UsageRecordCommuter(usageList, endTime, eventApps,
                eventTypes, eventTimestamps, packageManager, insN, insE, insT);
        comm.storeLocally(iid, this.getApplicationContext());

        insE.clear();
        insN.clear();
        insT.clear();
        insTaskid.clear();

    }

    // get accelerometer samples
    public void recordAccelerometer() {

        Log.i(ACCELEROMETER, "Recording accelerometer");

        AccelerometerRecordCommuter comm = new AccelerometerRecordCommuter(accX, accY, accZ, accT);
        comm.storeLocally(iid, this.getApplicationContext());

        accT.clear();
        accX.clear();
        accY.clear();
        accZ.clear();

    }

    // get gyroscope samples
    public void recordGyroscope() {

        Log.i(GYROSCOPE, "Recording gyroscope");

        GyroscopeRecordCommuter comm = new GyroscopeRecordCommuter(gyrX, gyrY, gyrZ, gyrT);
        comm.storeLocally(iid, this.getApplicationContext());

        gyrT.clear();
        gyrX.clear();
        gyrY.clear();
        gyrZ.clear();

    }

    // get light samples
    public void recordLight() {

        Log.i(LIGHT, "Recording light");

        LightRecordCommuter comm = new LightRecordCommuter(lumL, lumT);
        comm.storeLocally(iid, this.getApplicationContext());

        lumL.clear();
        lumT.clear();

    }

    // get battery status
    public void recordBattery() {

        Log.i(BATTERY, "Recording battery");

        // battery status receiver
        IntentFilter ifilter;
        Intent batteryStatus;

        // battery status receiver registration
        ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        batteryStatus = getApplicationContext().registerReceiver(null, ifilter);

        assert batteryStatus != null;
        int level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
        int temperature = batteryStatus.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1);
        int voltage = batteryStatus.getIntExtra(BatteryManager.EXTRA_VOLTAGE, -1);
        int plugged = batteryStatus.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1);
        int status = batteryStatus.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
        int health = batteryStatus.getIntExtra(BatteryManager.EXTRA_HEALTH, -1);

        BatteryRecordCommuter comm = new BatteryRecordCommuter(level, scale, temperature, voltage,
                plugged, status, health, System.currentTimeMillis());
        comm.storeLocally(iid, this.getApplicationContext());

    }

    // get screen events
    public void recordScreen() {

        Log.i(SCREEN, "Recording screen events");

        if (!scrT.isEmpty()) {

            ScreenRecordCommuter comm = new ScreenRecordCommuter(scrE, scrT, scrTaskid);
            comm.storeLocally(iid, this.getApplicationContext());

            scrE.clear();
            scrT.clear();
            scrTaskid.clear();

        }

    }

    // get activities events
    public void recordActivities() {

        Log.i(ACTIVITIES, "Recording Transition Activities events");

        if (!actT.isEmpty()) {

            ActivitiesRecordCommuter comm = new ActivitiesRecordCommuter(actE, actT, actTaskid);
            comm.storeLocally(iid, this.getApplicationContext());

            actE.clear();
            actT.clear();
            actTaskid.clear();
        }

    }

    public void recordOnQuerySubmit() {
        Log.i("umob.Rec.s.OnQuerySubmit", "Recording various sensors on query submission");

        recordBattery();
        //recordCall();
        //recordSMS();
        if (shouldRecordLocation) {
            recordLocation();
        }
        if (shouldRecordAppUsage) {
            recordUsageStats();
        }
    }

    public class RecorderBinder extends Binder {
        public BackgroundRecorder getService() {
            return BackgroundRecorder.this;
        }
    }

}