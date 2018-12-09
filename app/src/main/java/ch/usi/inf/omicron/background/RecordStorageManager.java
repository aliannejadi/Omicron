package ch.usi.inf.omicron.background;

import android.content.Context;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.support.annotation.NonNull;
import android.telephony.TelephonyManager;

import com.crashlytics.android.answers.Answers;
import com.crashlytics.android.answers.CustomEvent;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.google.gson.Gson;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;

import ch.usi.inf.omicron.Log;
import ch.usi.inf.omicron.record.Record;

import static ch.usi.inf.omicron.Configuration.filesNamesList;
import static ch.usi.inf.omicron.Configuration.lightFilesNamesList;
import static ch.usi.inf.omicron.UMob.iid;
import static ch.usi.inf.omicron.background.RecordStorageManager.PENDING_DIR;
import static ch.usi.inf.omicron.background.RecordStorageManager.UPLOAD;
import static ch.usi.inf.omicron.background.RecordStorageManager.WORKER_DIR;
import static ch.usi.inf.omicron.background.RecordStorageManager.moveFile;
import static ch.usi.inf.omicron.background.WifiReceiver.wifiReceiverOnline;

/**
 * Manages the storage and uploading of the records.
 */
public class RecordStorageManager {
    // record files names
    public static final String ACCELEROMETER_FILE = "accelerometer_records";
    public static final String BATTERY_FILE = "battery_records";
    //Bluetooth disabled for excessive energy consumption
    public static final String BLUETOOTH_FILE = "bluetooth_records";
    public static final String CELL_FILE = "cell_records";
    public static final String LIGHT_FILE = "light_records";
    public static final String GYROSCOPE_FILE = "gyroscope_records";
    public static final String HISTORY_FILE = "history_records";
    public static final String INPUT_FILE = "input_records";
    public static final String LOCATION_FILE = "location_records";
    public static final String QUERY_FILE = "query_records";
    public static final String RELEVANT_RESULTS_FILE = "relevant_result_records";
    public static final String SCREEN_FILE = "screen_records";
    public static final String SELECTION_ITEM_FILE = "selection_item_records";
    public static final String USAGE_FILE = "usage_records";
    public static final String WLAN_FILE = "wlan_records";
    public static final String POST_ANSWERS_FILE = "post_answers_records";
    public static final String CALL_FILE = "call_records";
    public static final String SMS_FILE = "sms_records";
    public static final String ACTIVITIES_FILE = "activities_records";
    static final String PENDING_DIR = "/pending";
    static final String WORKER_DIR = "/workerTemp";
    private static final String LOGIN = "umob.Main.login";
    static String UPLOAD = "umob.Rsm.upload";
    // log tags
    private static String CREATE = "umob.Rsm.create";
    private static String STORE = "umob.Rsm.store";
    private static String NET = "umob.Rsm.net";
    private FirebaseAuth mAuth;
    // Firebase storage reference to send the data to
    private StorageReference storage;

    public RecordStorageManager(Context ctx) {

        Log.i(CREATE, "RecordStorageManager creation:");
        mAuth = FirebaseAuth.getInstance();
        storage = FirebaseStorage.getInstance().getReference();

        Log.i(CREATE, "  fields initialized");

        Log.i(CREATE, "  list of files already in internal storage:");
        for (String f : ctx.fileList())
            Log.i(CREATE, "  - " + f);

        // register wifi state change receiver for uploading of offline records
        if (!wifiReceiverOnline) {

            WifiReceiver mWifiReceiver = new WifiReceiver();
            IntentFilter filter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
            ctx.getApplicationContext().registerReceiver(mWifiReceiver, filter);
            Log.i(CREATE, "  wifi broadcast receiver registered");

        }

        Log.i(CREATE, "  RecordStorageManager creation complete.");

    }

    // check if device is connected to wifi network
    public static boolean isConnectedToWifiAndHasInternetAccess(Context ctx) {

        ConnectivityManager cm =
                (ConnectivityManager) ctx.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        if (activeNetwork != null && activeNetwork.isConnected() &&
                activeNetwork.getType() == ConnectivityManager.TYPE_WIFI) {

            ConnectivityAsyncTask net = new ConnectivityAsyncTask();
            Boolean hasInternetAccess = false;
            try {
                hasInternetAccess = net.execute().get();
                Log.i(NET, "Device connected to the internet: " + hasInternetAccess);
            } catch (Exception e) {
                Log.w(NET, "Failed testing internet connectivity: " + e);
            }
            return hasInternetAccess; // Internet connectivity status

        } else {
            Log.w(NET, "WiFi is off.");
            return false; // WiFi is off
        }

    }

    static void moveFile(File src, File dst) throws IOException {
        copyFile(src, dst);
        src.delete();
    }

    private static void copyFile(File src, File dst) throws IOException {
        try (InputStream in = new FileInputStream(src)) {
            try (OutputStream out = new FileOutputStream(dst)) {
                // Transfer bytes from in to out
                byte[] buf = new byte[1024];
                int len;
                while ((len = in.read(buf)) > 0) {
                    out.write(buf, 0, len);
                }
            }
        }
    }

    public void store(Record record, String recordId, Context ctx, String recordType) {

        Log.i(STORE, "store():");

        try {

            // convert the POJO to JSON
            Gson gson = new Gson();
            String json = gson.toJson(record);

            String filePath = recordType + "_records";
            File file = new File(ctx.getFilesDir(), filePath);
            Log.i(STORE, "  target file: " + file.getAbsolutePath());
            Log.i(STORE, "  locally storing record: " + recordId);

            if (!file.exists()) file.createNewFile();
            FileWriter fw = new FileWriter(file.getAbsolutePath(), true);
            BufferedWriter bw = new BufferedWriter(fw);
            bw.write(json);
            bw.close();
            Log.i(STORE, "  record stored.");

        } catch (Exception e) {

            e.printStackTrace();
            Answers.getInstance().logCustom(new CustomEvent("Local store exception")
                    .putCustomAttribute("OS", Build.VERSION.RELEASE)
                    .putCustomAttribute("StackTrace", String.valueOf(e.getStackTrace()))
                    .putCustomAttribute("Model", Build.MODEL)
                    .putCustomAttribute("AppID", iid));

        }

    }

    /**
     * Upload the records stored in the application's internal storage to Google Firebase Storage
     */
    public void uploadRecords(final Context ctx) {

        Log.i(UPLOAD, "uploadRecords():");
        if (mAuth.getCurrentUser() == null) {
            mAuth.signInAnonymously();
            //make sure we are logged in
            Log.e(LOGIN, "Not logged in Firebase");
            return;
        }

        if (isConnectedToWifiAndHasInternetAccess(ctx)) {

            Log.i(UPLOAD, "  device connected to the internet via WiFi");
            for (String fileName : filesNamesList) {

                try {
                    uploadFile(fileName, ctx);
                } catch (java.io.IOException e) {
                    Log.e(UPLOAD, "Failed to find file " + fileName);
                    e.printStackTrace();
                }

            }

        } else if (hasMobileInternetAccess(ctx)) {

            Log.i(UPLOAD, "  device connected to the internet via mobile");
            for (String fileName : lightFilesNamesList) {

                try {
                    uploadFile(fileName, ctx);
                } catch (java.io.IOException e) {
                    Log.e(UPLOAD, "Failed to find file " + fileName);
                    e.printStackTrace();
                }
            }

        } else {

            Log.w(UPLOAD, "  device not connected to the internet, aborting.");

        }

    }

    private void uploadFile(String fileName, Context ctx) throws java.io.IOException {

        Log.i(UPLOAD, "  uploading " + fileName + " file");
        File file = new File(ctx.getFilesDir(), fileName);

        if (file.exists()) {

            // create the upload task which will handle the communication with Firebase
            // Storage
            Uri fileUri = Uri.fromFile(file);
            String uploadFileName = fileName + "_" +
                    System.currentTimeMillis() + ".txt";

            // duplicate the uploaded records file into a temp file to save the data in
            // case of upload failure

            File pendingDir = new File(ctx.getFilesDir() + PENDING_DIR);
            if (!pendingDir.exists())
                pendingDir.mkdirs();

            File pendingFile = new File(ctx.getFilesDir() + PENDING_DIR, uploadFileName);
            copyFile(file, pendingFile);

            //Create worker dir where currently uploading files are moved
            File workerDir = new File(ctx.getFilesDir() + WORKER_DIR);
            if (!workerDir.exists())
                workerDir.mkdirs();
            File workerFile = new File(ctx.getFilesDir() + WORKER_DIR, uploadFileName);

            //move record to workerTmp so we don't upload it two times in case of failure
            moveFile(pendingFile, workerFile);

            //Upload task move the file to the worker folder
            UploadTask uploadTask = storage.child("user" + iid).child(uploadFileName)
                    .putFile(fileUri);
            NamedUploadTask namedUploadTask = new NamedUploadTask(fileName,
                    uploadFileName, uploadTask, ctx);

            //Pending not empty means there are previous uncommitted records, we send them again
            File[] pendingFiles = pendingDir.listFiles();
            if (pendingFiles.length > 0) {
                for (File resendFile : pendingFiles) {
                    //move to workerTmp
                    File movedFile = new File(ctx.getFilesDir() + WORKER_DIR, resendFile.getName());
                    moveFile(resendFile, movedFile);

                    //resend record
                    Uri resendfileUri = Uri.fromFile(movedFile);
                    UploadTask resendTask = storage.child("user" + iid).child(resendFile.getName())
                            .putFile(resendfileUri);
                    NamedUploadTask namedResendTask = new NamedUploadTask(fileName,
                            resendFile.getName(), resendTask, ctx);
                }
            }

            // clear file containing the uploaded record to make space for future
            // records, if the upload fails, the temp file will be re-uploaded
            file.delete();

            Log.i(UPLOAD, "   done.");

        } else Log.w(UPLOAD, "  no " + fileName + " to upload");

    }

    private boolean hasMobileInternetAccess(Context ctx) {

        TelephonyManager tm = (TelephonyManager) ctx.getSystemService(Context.TELEPHONY_SERVICE);
        int networkType = TelephonyManager.NETWORK_TYPE_UNKNOWN;
        if (tm != null) networkType = tm.getNetworkType();
        return networkType != TelephonyManager.NETWORK_TYPE_UNKNOWN;

    }

}

/**
 * AsyncTask used to test internet connectivity by pinging a Google server; AsyncTask is required
 * since no network operation should be performed on the main UI thread.
 */
class ConnectivityAsyncTask extends AsyncTask<Void, Void, Boolean> {

    protected Boolean doInBackground(Void... params) {
        Socket sock = new Socket();

        try {
            SocketAddress sa = new InetSocketAddress("8.8.8.8", 53);
            sock.connect(sa, 1000);
            sock.close();
            return Boolean.TRUE;

        } catch (IOException e) {
            e.printStackTrace();
            return Boolean.FALSE;
        } finally {
            try {
                sock.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

}

/**
 * Used to encapsulate a file uploading task giving it access to the name of the uploaded file, so
 * that we can delete the file in case of correct upload, but keeping it intact if upload errors
 * occur. (the original UploadTask provided by Firebase Storage does not have access to the file's
 * name and cannot handle correctly uploads failures)
 */
class NamedUploadTask {

    private Context ctx;
    private String fileName;
    private String uploadFileName;
    // listener triggered by Firebase Storage upload success
    private final OnSuccessListener<UploadTask.TaskSnapshot> storageOnSuccessListener =
            new OnSuccessListener<UploadTask.TaskSnapshot>() {

                @Override
                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                    Log.i(UPLOAD, "Successfully sent record " + uploadFileName);
                    File file = new File(ctx.getFilesDir() + WORKER_DIR, uploadFileName);
                    if (file.delete())
                        Log.i(UPLOAD, "Correctly deleted " + uploadFileName + " from device's storage");
                    else
                        Log.i(UPLOAD, "Failed deleting " + uploadFileName + " from device's storage");
                }

            };
    // listener triggered by Firebase Storage upload failure
    private final OnFailureListener storageOnFailureListener = new OnFailureListener() {

        @Override
        public void onFailure(@NonNull Exception e) {

            Log.e(UPLOAD, "Failed sending record " + uploadFileName + " with error: " + e +
                    ".");
            e.printStackTrace();

            //move the record to the Pending folder to be resent
            File fileToResend = new File(ctx.getFilesDir() + WORKER_DIR, uploadFileName);
            File PendingFolderFile = new File(ctx.getFilesDir() + PENDING_DIR, uploadFileName);

            try {
                moveFile(fileToResend, PendingFolderFile);
            } catch (IOException e1) {
                e1.printStackTrace();
            }

            Answers.getInstance().logCustom(new CustomEvent("Upload Error")
                    .putCustomAttribute("OS", Build.VERSION.RELEASE)
                    .putCustomAttribute("Model", Build.VERSION.RELEASE)
                    .putCustomAttribute("AppID", iid)
                    .putCustomAttribute("fileName", uploadFileName)
                    .putCustomAttribute("StackTrace", e.toString())
            );

        }

    };
    private UploadTask uploadTask;

    public NamedUploadTask(String fileName, String uploadFileName, UploadTask uploadTask,
                           Context ctx) {

        this.ctx = ctx;
        this.fileName = fileName;
        this.uploadTask = uploadTask;
        this.uploadFileName = uploadFileName;

        uploadTask.addOnSuccessListener(storageOnSuccessListener)
                .addOnFailureListener(storageOnFailureListener);

    }

}
