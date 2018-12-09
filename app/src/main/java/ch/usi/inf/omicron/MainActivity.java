package ch.usi.inf.omicron;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.AppOpsManager;
import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.constraint.ConstraintLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.CardView;
import android.support.v7.widget.PopupMenu;
import android.view.KeyEvent;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewParent;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Cache;
import com.android.volley.Network;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.BasicNetwork;
import com.android.volley.toolbox.DiskBasedCache;
import com.android.volley.toolbox.HurlStack;
import com.android.volley.toolbox.JsonObjectRequest;
import com.crashlytics.android.answers.Answers;
import com.crashlytics.android.answers.CustomEvent;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.gson.Gson;
import com.gun0912.tedpermission.PermissionListener;
import com.gun0912.tedpermission.TedPermission;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import ch.usi.inf.omicron.background.BackgroundRecorder;
import ch.usi.inf.omicron.queryRendering.QueryResult;
import ch.usi.inf.omicron.queryRendering.QueryResultAdapter;
import ch.usi.inf.omicron.recordCommuter.HistoryRecordCommuter;
import ch.usi.inf.omicron.recordCommuter.InputRecordCommuter;
import ch.usi.inf.omicron.recordCommuter.QueryRecordCommuter;
import ch.usi.inf.omicron.recordCommuter.RelevantResultRecordCommuter;
import ch.usi.inf.omicron.recordCommuter.SelectionItemRecordCommuter;
import ch.usi.inf.omicron.taskManager.UmobTask;

import static android.app.AppOpsManager.OPSTR_GET_USAGE_STATS;
import static ch.usi.inf.omicron.BrowserActivity.historyTaskID;
import static ch.usi.inf.omicron.BrowserActivity.historyTimestamp;
import static ch.usi.inf.omicron.BrowserActivity.historyTitles;
import static ch.usi.inf.omicron.BrowserActivity.historyUrls;
import static ch.usi.inf.omicron.Configuration.BING_KEY;
import static ch.usi.inf.omicron.Configuration.FIRST_INSTALLATION_SURVEY;
import static ch.usi.inf.omicron.Configuration.POST_TASK_URL;
import static ch.usi.inf.omicron.Configuration.PRE_TASK_URL;
import static ch.usi.inf.omicron.UMob.iid;
import static ch.usi.inf.omicron.UMob.rsm;
import static ch.usi.inf.omicron.UMob.tm;
import static ch.usi.inf.omicron.Utils.createTestTask;
import static ch.usi.inf.omicron.Utils.resetSharedForNewTask;
import static ch.usi.inf.omicron.Utils.updateNotification;
import static ch.usi.inf.omicron.background.BackgroundRecorder.keepStalking;

/**
 * The MainActivity class handles the opening screen of the app. It handles the querying
 * functionality, allows the user to open up the browser, registers the user input and manages the
 * background recording service creation. It also declares the Record Storage Manager static
 * variable to handle offline background recording. It also has the task of checking for the needed
 * user permissions in order for the recording to be possible. Browsing history, search query,
 * query result selection and relevant result feedback are recorded here.
 */

public class MainActivity extends AppCompatActivity {

    // activity request codes
    static final int BROWSER_REQUEST_CODE = 0;
    static final int TASK_DONE_REQUEST_CODE = 1;
    // log tags
    private static final String START = "umob.Main.start";
    private static final String QUERY = "umob.Main.query";
    private static final String RENDER = "umob.Main.render";
    private static final String INPUT = "umob.Main.input";
    private static final String FEEDBACK = "umob.Main.feedback";
    private static final String LOGIN = "umob.Main.login";
    private static final String TASK = "umob.Main.task";
    // lists of relevant results data
    public static ArrayList<Long> relevantResultTimestamps = new ArrayList<>();
    public static ArrayList<Integer> relevantResultIndeces = new ArrayList<>();
    public static ArrayList<String> relevantResultTitles = new ArrayList<>();
    public static ArrayList<String> relevantResultLinks = new ArrayList<>();
    public static ArrayList<String> relevantResultTaskID = new ArrayList<>();
    // lists of input events and their timestamp
    protected static ArrayList<String> events = new ArrayList<>();
    protected static ArrayList<Long> eventTimestamps = new ArrayList<>();
    protected static ArrayList<String> rawInputTypes = new ArrayList<>();
    protected static ArrayList<Integer> rawInputXs = new ArrayList<>();
    protected static ArrayList<Integer> rawInputYs = new ArrayList<>();
    protected static ArrayList<Long> rawInpuTimestamps = new ArrayList<>();
    // input events identifier constants
    private final String I_CREATE = "c";
    private final String I_BSON = "bs1";
    private final String I_BSOFF = "bs0";
    private final String I_STOP = "s";
    private final String I_SB = "sb";
    private final String I_ST = "st";
    private final String I_LI = "li";
    private final String I_QF = "qf";
    private final String I_BBM = "bbm";
    private final String I_END = "et";
    // search engine custom search key
    //String GOOGLE_KEY = "";
    // runtime permissions
    String[] PERMISSIONS = {Manifest.permission.READ_CONTACTS,
            Manifest.permission.READ_CALL_LOG, Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.READ_SMS};
    // Firebase authorisation
    private FirebaseAuth mAuth;
    // used to communicate with the task manager
    private BroadcastReceiver receiver;
    private BackgroundRecorder mBoundService;
    private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            // This is called when the connection with the service has been
            // established, giving us the service object we can use to
            // interact with the service.  Because we have bound to a explicit
            // service that we know is running in our own process, we can
            // cast its IBinder to a concrete class and directly access it.
            mBoundService = ((BackgroundRecorder.RecorderBinder) service).getService();

            // Tell the user about this for our demo.
//            Toast.makeText(MainActivity.this, "local service connected",
//                    Toast.LENGTH_SHORT).show();
        }

        public void onServiceDisconnected(ComponentName className) {
            // This is called when the connection with the service has been
            // unexpectedly disconnected -- that is, its process crashed.
            // Because it is running in our same process, we should never
            // see this happen.
            mBoundService = null;
//            Toast.makeText(MainActivity.this, "local service disconnected",
//                    Toast.LENGTH_SHORT).show();
        }
    };
    private boolean mIsBound;

    public static void checkGooglePlay(Activity activity) {
        GoogleApiAvailability googleApiAvailability = GoogleApiAvailability.getInstance();
        if (googleApiAvailability.isGooglePlayServicesAvailable(activity) != ConnectionResult.SUCCESS) {
            googleApiAvailability.makeGooglePlayServicesAvailable(activity);
        }
    }

    public static boolean hasPermissions(Context context, String... permissions) {
        if (context != null && permissions != null) {
            for (String permission : permissions) {
                if (ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                    return false;
                }
            }
        }
        return true;
    }

    void doBindService() {
        // Establish a connection with the service.  We use an explicit
        // class name because we want a specific service implementation that
        // we know will be running in our own process (and thus won't be
        // supporting component replacement by other applications).
        bindService(new Intent(MainActivity.this,
                BackgroundRecorder.class), mConnection, Context.BIND_AUTO_CREATE);
        mIsBound = true;
    }

    void doUnbindService() {
        if (mIsBound) {
            // Detach our existing connection.
            unbindService(mConnection);
            mIsBound = false;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Check Google Play Before using it
        checkGooglePlay(this);

        //launch the tutorial the first time
        startTutorial();

        Log.i(START, "Main activity started");

        Log.i(INPUT, "> CREATE [c]");
        eventTimestamps.add(System.currentTimeMillis());
        events.add(I_CREATE);

        // Instance ID to identify user (application instance)
        Log.d(START, "  instance id: " + iid);

        // Unauthenticated Firebase login
        mAuth = FirebaseAuth.getInstance();

        // submit the query on keyboard input confirm
        final EditText queryField = findViewById(R.id.queryField);
        queryField.setOnEditorActionListener(new TextView.OnEditorActionListener() {

            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {

                if ((event != null && (event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) ||
                        (actionId == EditorInfo.IME_ACTION_DONE))
                    submitQuery(findViewById(R.id.searchButton));

                return false;

            }

        });


        this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

        //Start BackgroundService to collect data
        startBackgroundService();

        //check Permissions
        checkPermissions(getApplicationContext());

        // Set the emptyView to the ListView
        getListView().setEmptyView(findViewById(R.id.emptyElement));

        // check for tasks
        SharedPreferences sharedPref = getSharedPreferences("shared_preferences",
                MODE_PRIVATE);
        //Don't check task immediately on the first run after the installation
        boolean permissionsGranted = sharedPref.getBoolean("permissionsGranted", false);

        if (permissionsGranted) {
            //listen for new tasks
            startTaskListener();
        }

    }

    private void startBackgroundService() {
        // bind the background service creation and destruction to the switch button
        Switch backgroundSwitch = findViewById(R.id.backgroundSwitch);

        // start the background recording
        if (!keepStalking) {
            startService(new Intent(getApplicationContext(), BackgroundRecorder.class));
            backgroundSwitch.setChecked(true);
        }

        // bind the background service creation and destruction to the switch
        backgroundSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

                if (isChecked) {

                    Log.i(INPUT, "> BACKGROUND SWITCH ON [BS1]");
                    eventTimestamps.add(System.currentTimeMillis());
                    events.add(I_BSON);
                    startService(new Intent(getApplicationContext(), BackgroundRecorder.class));

                } else {

                    Log.i(INPUT, "> BACKGROUND SWITCH OFF [BS0]");
                    eventTimestamps.add(System.currentTimeMillis());
                    events.add(I_BSOFF);
                    stopService(new Intent(getApplicationContext(), BackgroundRecorder.class));

                }

            }

        });
    }

    private void startTutorial() {
        //  Declare a new thread to do a preference check
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                //  Initialize SharedPreferences
                SharedPreferences getPrefs = PreferenceManager
                        .getDefaultSharedPreferences(getBaseContext());

                //  Create a new boolean and preference and set it to true
                boolean isFirstStart = getPrefs.getBoolean("firstStart", true);

                //  If the activity has never started before...
                Log.v("Tutorial", "First start? " + isFirstStart);
                if (isFirstStart) {
                    Log.v("Tutorial", "Starting the tutorial");
                    //  Launch app intro
                    final Intent i = new Intent(MainActivity.this, TutorialActivity.class);

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            startActivity(i);
                        }
                    });

                    //  Make a new preferences editor
                    SharedPreferences.Editor e = getPrefs.edit();

                    //  Edit preference to make it false because we don't want this to run again
                    e.putBoolean("firstStart", false);

                    //  Apply changes
                    e.apply();
                }
            }
        });

        // Start the thread
        t.start();
    }

    private void startTaskListener() {
        Log.i(TASK, "Retrieving tasks from database at id: " + iid);

        IntentFilter filter = new IntentFilter();
        filter.addAction("REFRESH_TASK_HINT");
        filter.addAction("TASK_EXPIRED");

        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {

                String action = intent.getAction();
                if (action != null) {
                    //Task expired
                    if (action.equals("TASK_EXPIRED")) {
                        Log.v(TASK, "Task expired");
                        taskExpired();
                        return;
                    }
                }

                //Change main activity to display task
                showTaskHome();
                refreshTaskHint();
            }
        };
        registerReceiver(receiver, filter);

    }

    @Override
    protected void onStart() {
        super.onStart();

        firebaseAuthentication();

        doBindService();
        showOrHideCrossMenu();
    }

    private void firebaseAuthentication() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        Log.d(LOGIN, "user: " + currentUser);

        if (currentUser == null) {
            mAuth.signInAnonymously()
                    .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {

                            if (task.isSuccessful()) {
                                // Sign in success
                                Log.d(LOGIN, "signInAnonymously:success");
                                FirebaseUser user = mAuth.getCurrentUser();
                            } else {
                                // If sign in fails, display a message to the user.
                                Log.w(LOGIN, "signInAnonymously:failure");
                            }

                        }

                    });

        }
    }

    @Override
    public void onResume() {
        super.onResume();

        //Check Google Play before using it
        checkGooglePlay(this);

        //if there are task show them after the app return in foreground
        SharedPreferences sharedPref = getSharedPreferences("shared_preferences",
                MODE_PRIVATE);
        boolean permissionsGranted = sharedPref.getBoolean("permissionsGranted", false);

        boolean taskStarted = sharedPref.getBoolean("pre_task_questionnaire_opened", false);

        //task started but the task manager is empty, restore content
        if (taskStarted && tm.getActiveTask() == null) {
            restoreTM();
        }

        if (permissionsGranted) {
            showTaskHome();
            refreshTaskHint();
            showOrHideCrossMenu();
        }
    }

    /*
     * Set task as home view
     * */
    private void showTaskHome() {
        if (tm.isTaskAvailable()) {
            Log.v(TASK, "Show task on home activity");

            Log.i(INPUT, "> Showing Task view");
            eventTimestamps.add(System.currentTimeMillis());
            events.add(I_LI + " task-view");

            SharedPreferences sharedPref = getSharedPreferences("shared_preferences",
                    Context.MODE_PRIVATE);

            UmobTask task = tm.getActiveTask();
            boolean preQuestionnaireFinished = sharedPref.getBoolean("pre_task_questionnaire_opened", false);
            boolean postQuestionnaireFinished = sharedPref.getBoolean("post_task_questionnaire_opened", false);
            boolean endTaskPressed = sharedPref.getBoolean("end_task_button_pressed", false);

            //Hide query field to prevent searching while the start has not started
            if (!preQuestionnaireFinished && getListAdapter() == null) {
                findViewById(R.id.queryField).setVisibility(View.INVISIBLE);
                findViewById(R.id.searchButton).setVisibility(View.INVISIBLE);
            }

            //Set text and description
            TextView title = findViewById(R.id.title);
            TextView description = findViewById(R.id.description);
            title.setText(task.getTitle());
            description.setText(task.getDescription());


            //hide and show correct view
            LinearLayout welcomeMessage = findViewById(R.id.welcomeScreen);
            welcomeMessage.setVisibility(View.GONE);
            ConstraintLayout taskMessage = findViewById(R.id.taskScreen);
            taskMessage.setVisibility(View.VISIBLE);

            //Hide startTask after pre-task questionnaire
            Button startTaskButton = findViewById(R.id.startTask);

            //change description
            TextView howto = findViewById(R.id.how_start_task);
            TextView how_title = findViewById(R.id.how_title);

            if (preQuestionnaireFinished && !postQuestionnaireFinished && !endTaskPressed) {
                startTaskButton.setVisibility(View.GONE);
                howto.setText(R.string.how_to_after_questionnaire);
                how_title.setText(R.string.how_title);
            } else if (endTaskPressed) {
                howto.setText(R.string.how_to_after_post_questionnaire);
                how_title.setText(R.string.how_to_after_post_questionnaire_title);
                startTaskButton.setVisibility(View.GONE);
                findViewById(R.id.queryField).setVisibility(View.INVISIBLE);
                findViewById(R.id.searchButton).setVisibility(View.INVISIBLE);
            } else {
                startTaskButton.setVisibility(View.VISIBLE);
                howto.setText(R.string.how_to_start_task);
                how_title.setText(R.string.cur_task);
            }
        }
    }


    @Override
    protected void onStop() {

        Log.i(INPUT, "> STOP [s]");
        eventTimestamps.add(System.currentTimeMillis());
        events.add(I_STOP);

        InputRecordCommuter comm = new InputRecordCommuter(events, eventTimestamps, rawInputTypes,
                rawInputXs, rawInputYs, rawInpuTimestamps);
        comm.storeLocally(iid, this.getApplicationContext());

        rsm.uploadRecords(this.getApplicationContext());

        super.onStop();

    }

    private void clearSearchLinks() {
        if (!historyTitles.isEmpty()) {
            historyTitles.clear();
        }
        if (!historyUrls.isEmpty()) {
            historyUrls.clear();
        }

        if (!historyTimestamp.isEmpty()) {
            historyTimestamp.clear();
        }

        if (!relevantResultTimestamps.isEmpty()) {
            relevantResultTimestamps.clear();
        }
        if (!relevantResultIndeces.isEmpty()) {
            relevantResultIndeces.clear();
        }
        if (!relevantResultTitles.isEmpty()) {
            relevantResultTitles.clear();
        }
        if (!relevantResultLinks.isEmpty()) {
            relevantResultLinks.clear();
        }
    }

    private void storeSearchLinks() {
        if (!historyTitles.isEmpty()) {

            ArrayList<String> copy_historyTitles = (ArrayList<String>) historyTitles.clone();
            ArrayList<String> copy_historyUrls = (ArrayList<String>) historyUrls.clone();
            ArrayList<Long> copy_historyTimestamp = (ArrayList<Long>) historyTimestamp.clone();
            ArrayList<String> copy_historyTaskID = (ArrayList<String>) historyTaskID.clone();

            HistoryRecordCommuter hComm = new HistoryRecordCommuter(copy_historyTitles,
                    copy_historyUrls, copy_historyTimestamp, copy_historyTaskID);
            hComm.storeLocally(iid, this.getApplicationContext());
        }

        if (!relevantResultTimestamps.isEmpty()) {
            RelevantResultRecordCommuter rComm = new RelevantResultRecordCommuter(relevantResultIndeces,
                    relevantResultTitles, relevantResultLinks, relevantResultTimestamps, relevantResultTaskID);
            rComm.storeLocally(iid, this.getApplicationContext());
        }

    }

    // open Google Search on the browser activity with the user input query
    public void submitQuery(View view) {

        Log.i(INPUT, "> SEARCH BUTTON [sb]");
        eventTimestamps.add(System.currentTimeMillis());
        events.add(I_SB);


        // get the query content and
        EditText editText = findViewById(R.id.queryField);
        final String query = editText.getText().toString();

        switch (query) {
            case "":
                Toast.makeText(this, "Please enter a query...", Toast.LENGTH_LONG).show();
                return;
            case "umob:showswitch":
                Switch s = findViewById(R.id.backgroundSwitch);
                if (keepStalking) s.setChecked(true);
                s.setVisibility(View.VISIBLE);
                return;
            case "umob:hideswitch":
                findViewById(R.id.backgroundSwitch).setVisibility(View.GONE);
                return;
            case "umob:switchon":
                ((Switch) findViewById(R.id.backgroundSwitch)).setChecked(true);
                return;
            case "umob:switchoff":
                ((Switch) findViewById(R.id.backgroundSwitch)).setChecked(false);
                return;
            case "umob:id":
                showId();
                return;
            case "umob:survey":
                openSurvey(view);
                return;
            case "umob:upload":
                rsm.uploadRecords(this.getApplicationContext());
                Toast.makeText(MainActivity.this, "Upload in progress...",
                        Toast.LENGTH_LONG).show();
                return;
        }

        // check device internet connection
//        if (!isOnline()) {
//            Toast.makeText(this, "Your device is offline.", Toast.LENGTH_LONG).show();
//            return;
//        }

        //Mohammad: get the sensor data


        ListView listView = findViewById(R.id.resultsList);

        // clear the query result list by setting a null adapter
        setListAdapter(null);

        // show progress bar to display while the list loads
        findViewById(R.id.progressBar).setVisibility(View.VISIBLE);

        View focused = this.getCurrentFocus();
        if (focused != null) {
            InputMethodManager imm = (InputMethodManager)
                    getSystemService(Context.INPUT_METHOD_SERVICE);
            assert imm != null;
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
        editText.clearFocus();

        // instantiate the RequestQueue
        RequestQueue mRequestQueue;

        // Instantiate the cache
        Cache cache = new DiskBasedCache(getCacheDir(), 1024 * 1024); // 1MB cap

        // Set up the network to use HttpURLConnection as the HTTP client.
        Network network = new BasicNetwork(new HurlStack());

        // Instantiate the RequestQueue with the cache and network.
        mRequestQueue = new RequestQueue(cache, network);

        // Start the queue
        mRequestQueue.start();

        // compose the custom search url
        //String customUrl = "https://www.googleapis.com/customsearch/v1?key=" + GOOGLE_KEY +
        //        "&cx=006640561896987010571:gwmljcffhxe&q=" + query.replaceAll(" ", "%20") +
        //        "&gl=uk&alt=json";
        //BING

        String encoded_query = null;
        try {
            encoded_query = URLEncoder.encode(query, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        String customUrl = "https://api.cognitive.microsoft.com/bing/v7.0/search" +
                "?q=" + encoded_query + "&setlang=en&lf=1&count=50";


        // Request a string response from the provided URL.
        JsonObjectRequest stringRequest = new JsonObjectRequest
                (Request.Method.GET, customUrl, null, new Response.Listener<JSONObject>() {

                    @Override
                    public void onResponse(JSONObject response) {

                        Log.d(QUERY, "response: " + response);
                        findViewById(R.id.progressBar).setVisibility(View.INVISIBLE);
                        displayResults(response);

                        // send the record to Firebase
                        QueryRecordCommuter comm = new QueryRecordCommuter(query, response.toString(),
                                System.currentTimeMillis(), tm.getActiveTaskId());
                        comm.storeLocally(iid, getApplicationContext());

                    }

                }, new Response.ErrorListener() {

                    @Override
                    public void onErrorResponse(VolleyError error) {

                        findViewById(R.id.progressBar).setVisibility(View.INVISIBLE);
                        Log.d(QUERY, "error: " + error);

                    }

                }) {
            @Override
            public Map<String, String> getHeaders() {
                HashMap<String, String> headers = new HashMap<>();
                headers.put("Accept-Language", "en-GB,en;q=0.5");
                headers.put("Ocp-Apim-Subscription-Key", BING_KEY);
                return headers;
            }
        };

        // Add the request to the RequestQueue.
        mRequestQueue.add(stringRequest);

        rsm.uploadRecords(this.getApplicationContext());

    }

    public void onStartTask(View view) {

        if (tm.isTaskAvailable() && tm.getActiveTask().isNotDoneOrExpired()) {
            String targetUrl = PRE_TASK_URL + "?appid=" + iid +
                    "&taskid=" + tm.getActiveTaskId();

            Log.i(INPUT, "> start task button");
            eventTimestamps.add(System.currentTimeMillis());
            events.add(I_ST);

            //enable query field and search button
            findViewById(R.id.queryField).setVisibility(View.VISIBLE);
            findViewById(R.id.searchButton).setVisibility(View.VISIBLE);

            // open up the browser activity to the specified link
            Log.i(INPUT, "> Opening the pre-task questionnaire");
            eventTimestamps.add(System.currentTimeMillis());
            events.add(I_LI + " pre-task questionnaire button");

            Bundle extras = new Bundle();
            extras.putString("ch.usi.jacopofidacaro.unimobile.QUERY", targetUrl);
            extras.putString("ch.usi.jacopofidacaro.unimobile.QUESTIONNAIRE_TYPE", "PRE");
            startActivityForResult(new Intent(this, TaskBrowserActivity.class)
                    .putExtras(extras), BROWSER_REQUEST_CODE);

            //mark the task as started
            tm.startTask();


            //store and send checked links
            //it's a new task we want to clear previous links
            storeSearchLinks();
            rsm.uploadRecords(this.getApplicationContext());
            clearSearchLinks();
        } else {
            taskExpired();
        }
    }

    private void taskExpired() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Sorry, the task you were doing has expired")
                .setTitle("Expired")
                .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {

                    }
                })
                .setCancelable(false);
        builder.create().show();


        setListAdapter(null);
        showWelcomeHome();
        refreshTaskHint();
    }


    /*
     * Shows Menu if I'm not searching,
     * shows cross when I'm searching
     */
    private void showOrHideCrossMenu() {
        if (getListAdapter() != null && !getListAdapter().isEmpty()) {
            findViewById(R.id.reset).setVisibility(View.VISIBLE);
            findViewById(R.id.menu).setVisibility(View.INVISIBLE);

        } else {
            findViewById(R.id.reset).setVisibility(View.INVISIBLE);
            findViewById(R.id.menu).setVisibility(View.VISIBLE);
        }
    }

    // once the JSON object is retrieved, render the results in the list view
    private void displayResults(JSONObject results) {

        Log.i(RENDER, "displayResults()");

        /* Spellchecker on Bing is another API that we don't use
        try {

            final String spelling = ((JSONObject) results.get("spelling"))
                    .get("correctedQuery").toString();

            Log.i(RENDER, "Did you mean " + spelling + "?");

            TextView spellingBox = (TextView) findViewById(R.id.spelling);
            spellingBox.setText("Did you mean " + spelling + "?");
            spellingBox.setVisibility(View.VISIBLE);
            spellingBox.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    EditText queryField = (EditText) findViewById(R.id.queryField);
                    queryField.setText(spelling);
                    submitQuery(queryField);
                    v.setVisibility(View.GONE);
                }
            });

        } catch (JSONException e) {

            Log.i(RENDER, "  no spelling issue");

        }
        */

        try {

            JSONArray itemsList = results.getJSONObject("webPages").getJSONArray("value");
            Log.i(RENDER, "  got items from response");

            QueryResult[] queryResults = new QueryResult[itemsList.length()];

            for (int i = 0; i < itemsList.length(); i++) {
                JSONObject item = (JSONObject) itemsList.get(i);
                queryResults[i] = new QueryResult(
                        (String) item.get("name"),
                        (String) item.get("url"),
                        (String) item.get("snippet"));
            }

            QueryResultAdapter adapter = new QueryResultAdapter(this, queryResults);
            setListAdapter(adapter);
            //show cross to go back
            showOrHideCrossMenu();

            Log.i(RENDER, "  render complete.");

        } catch (JSONException e) {

            Log.e(RENDER, "  failed render JSON object: " + e);
            //shows a message in case of no results
            Toast.makeText(this, "No results!", Toast.LENGTH_LONG).show();

            EditText editText = findViewById(R.id.queryField);
            String query = editText.getText().toString();
            Answers.getInstance().logCustom(new CustomEvent("Results Display Error")
                    .putCustomAttribute("Query", query)
                    .putCustomAttribute("OS", Build.VERSION.RELEASE)
                    .putCustomAttribute("Model", Build.MODEL));

        }

    }

    // retrieve the link from the selected item and open the browser view at that page
    public void openBrowser(View view) {

        ViewParent viewGroup = view.getParent();
        int index = ((ListView) viewGroup.getParent()).indexOfChild((CardView) viewGroup);

        Log.i(INPUT, "> LIST ITEM [li" + index + "]");
        eventTimestamps.add(System.currentTimeMillis());
        events.add(I_LI + index);

        // get the url
        String targetUrl = ((TextView) view.findViewById(R.id.resultLink)).getText().toString();
        String targetTitle = ((TextView) view.findViewById(R.id.resultTitle)).getText().toString();
        String targetDescription = ((TextView) view.findViewById(R.id.resultDescription))
                .getText().toString();

        SelectionItemRecordCommuter comm = new SelectionItemRecordCommuter(targetTitle,
                targetUrl, targetDescription, System.currentTimeMillis(), tm.getActiveTaskId());
        comm.storeLocally(iid, this.getApplicationContext());

        // open up the browser activity to the specified link
        startActivityForResult(new Intent(this, BrowserActivity.class)
                .putExtra("ch.usi.jacopofidacaro.unimobile.QUERY", targetUrl), BROWSER_REQUEST_CODE);

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.i(INPUT, "> DESTROY [s]");

        SharedPreferences sharedPref = getSharedPreferences("shared_preferences",
                Context.MODE_PRIVATE);
        boolean preQuestionnaireFinished = sharedPref.getBoolean("pre_task_questionnaire_opened", false);

        //even if the user close the app the stars should stay if a task is in progress
        if (!preQuestionnaireFinished) {
            storeSearchLinks();
            rsm.uploadRecords(this.getApplicationContext());
            clearSearchLinks();
        }

        doUnbindService();

        try {
            unregisterReceiver(receiver);
        } catch (IllegalArgumentException ignored) {

        }

    }

    /* Check the required Android permissions that have to be granted by the user in order for the
     * app to work properly; if some permission is missing, take the user to the settings page that
     * allows him to grant it.
     */
    public void checkPermissions(Context context) {

        PermissionListener permissionlistener = new PermissionListener() {
            @Override
            public void onPermissionGranted() {
                Toast.makeText(MainActivity.this, "Permission Granted", Toast.LENGTH_SHORT).show();
                Log.v("Permissions", "Permissions granted");

                startTaskListener();

                SharedPreferences sharedPref = getSharedPreferences("shared_preferences",
                        MODE_PRIVATE);

                SharedPreferences.Editor edit = sharedPref.edit();
                edit.putBoolean("permissionsGranted", true);
                edit.apply();

                //Listen for tasks
                tm.startTaskListener(getApplicationContext());
            }

            @Override
            public void onPermissionDenied(ArrayList<String> deniedPermissions) {
                //we don't need all permissions
                Log.v("Permissions", "Permissions granted");
                //Wait a bit to check for task to not overwhelm the user with notifications

                startTaskListener();

                SharedPreferences sharedPref = getSharedPreferences("shared_preferences",
                        MODE_PRIVATE);

                SharedPreferences.Editor edit = sharedPref.edit();
                edit.putBoolean("permissionsGranted", true);
                edit.apply();

                //Listen for tasks
                tm.startTaskListener(getApplicationContext());

            }

        };

        SharedPreferences sharedPref = getSharedPreferences("shared_preferences",
                MODE_PRIVATE);
        boolean permissionsGranted = sharedPref.getBoolean("permissionsGranted", false);


        //check all runtime permission
        if (!hasPermissions(this, PERMISSIONS) && !permissionsGranted) {
            TedPermission.with(this)
                    .setPermissionListener(permissionlistener)
                    .setRationaleMessage("For the duration of the experiment we collect data anonymously, please accept the following permissions requests that you are ok giving consent")
                    //.setDeniedMessage("We need this permissions to collect data for the experiment \n\nPlease turn on permissions at [Setting] > [Permission]")
                    .setPermissions(PERMISSIONS)
                    .check();
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            //Oreo Enable permissions before
            if (!permissionsGranted) {
                SharedPreferences.Editor edit = sharedPref.edit();
                edit.putBoolean("permissionsGranted", true);
                edit.apply();

                startTaskListener();
            }
        }

        // check for Usage Access permission
        AppOpsManager appOps = (AppOpsManager) this.getSystemService(Context.APP_OPS_SERVICE);
        assert appOps != null;
        int mode = appOps.checkOpNoThrow(OPSTR_GET_USAGE_STATS, android.os.Process.myUid(),
                this.getPackageName());
        if (mode != AppOpsManager.MODE_ALLOWED) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage("This app needs usage access to function properly.")
                    .setTitle("UniMobile")
                    .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            startActivity(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS));
                        }
                    })
                    .setCancelable(false);
            builder.create().show();
        }

        /*
        if (!RecordStorageManager.isConnectedToWifiAndHasInternetAccess(this)) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage("Please connect to a WiFi network and make sure you are connected to the Internet!")
                    .setTitle("UniMobile")
                    .setPositiveButton("Open WiFi Settings", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS));
                                }
                            }
                    )
                    .setCancelable(true);
            builder.create().show();
        }
    */

    }

    public void queryFieldClicked(View view) {

        Log.i(INPUT, "> QUERY FIELD [qf]");
        eventTimestamps.add(System.currentTimeMillis());
        events.add(I_QF);

    }

    @Override
    public void onBackPressed() {

        Log.i(INPUT, "> BACK BUTTON (MAIN) [bbm]");
        eventTimestamps.add(System.currentTimeMillis());
        events.add(I_BBM);
        //go back from the search to the mainactivity
        if (getListAdapter() != null && !getListAdapter().isEmpty()) {
            returnToMain();
        } else {
            super.onBackPressed();
        }

    }

    // check device internet connection
    public boolean isOnline() {

        ConnectivityManager cm =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        assert cm != null;
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        return netInfo != null && netInfo.isConnectedOrConnecting();

    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {

        int x = (int) event.getX();
        int y = (int) event.getY();
        rawInputXs.add(x);
        rawInputYs.add(y);
        rawInpuTimestamps.add(event.getEventTime());
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                Log.i(INPUT, "finger down: x:" + x + " y: " + y);
                rawInputTypes.add("d");
                break;
            case MotionEvent.ACTION_MOVE:
                rawInputTypes.add("m");
                Log.i(INPUT, "finger swipe: x:" + x + " y: " + y);
                break;
            case MotionEvent.ACTION_UP:
                rawInputTypes.add("u");
                Log.i(INPUT, "finger up: x:" + x + " y: " + y);
                break;
        }
        return super.dispatchTouchEvent(event);

    }

    private void showId() {

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(iid)
                .setTitle("User ID")
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                    }
                })
                .setNeutralButton("Copy", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        ClipboardManager clipboard = (ClipboardManager)
                                getSystemService(Context.CLIPBOARD_SERVICE);
                        ClipData clip = ClipData.newPlainText("id", iid);
                        assert clipboard != null;
                        clipboard.setPrimaryClip(clip);
                    }
                });
        builder.create().show();

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        //Post-Questionnaire ended
        if (requestCode == TASK_DONE_REQUEST_CODE) {

            if (resultCode == Activity.RESULT_CANCELED) {
                //conclude task
                taskConcluded();
            }
        }
    }

    public void openSurvey(View view) {

        Log.i(INPUT, "> Opening the survey");
        eventTimestamps.add(System.currentTimeMillis());
        events.add(I_LI + " survey button");

        String targetUrl = FIRST_INSTALLATION_SURVEY + "?umobID=" + iid;

        // open up the browser activity to the specified link
        Bundle extras = new Bundle();
        extras.putString("ch.usi.jacopofidacaro.unimobile.QUERY", targetUrl);
        extras.putString("ch.usi.jacopofidacaro.unimobile.QUESTIONNAIRE_TYPE", "SURVEY");

        startActivityForResult(new Intent(this, TaskBrowserActivity.class)
                .putExtras(extras), BROWSER_REQUEST_CODE);
    }

    public void clearText() {
        EditText searchInput = findViewById(R.id.queryField);
        searchInput.setText("");
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        assert imm != null;
        imm.showSoftInput(searchInput, InputMethodManager.SHOW_IMPLICIT);
    }

    public void showID(View view) {
        events.add(I_LI + " showID");
        showId();
    }


    /*
     * Set Welcome message as home view
     * */
    private void showWelcomeHome() {
        //hide and show correct view
        LinearLayout welcomeMessage = findViewById(R.id.welcomeScreen);
        welcomeMessage.setVisibility(View.VISIBLE);
        ConstraintLayout taskMessage = findViewById(R.id.taskScreen);
        taskMessage.setVisibility(View.GONE);

        //enable query field and search button
        findViewById(R.id.queryField).setVisibility(View.VISIBLE);
        findViewById(R.id.searchButton).setVisibility(View.VISIBLE);
    }


    public void completeTask(View view) {
        if (tm.isTaskAvailable() && tm.getActiveTask().isNotDoneOrExpired()) {

            DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    switch (which) {
                        case DialogInterface.BUTTON_POSITIVE:
                            //Yes button clicked
                            endTask();
                            break;

                        case DialogInterface.BUTTON_NEGATIVE:
                            //No button clicked
                            break;
                    }
                }
            };

            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage("Are you sure you want to end the current task?").setPositiveButton("Yes", dialogClickListener)
                    .setNegativeButton("No", dialogClickListener).show();

        } else {
            taskExpired();
        }
    }

    protected void endTask() {
        SharedPreferences sharedPref = getApplicationContext().getSharedPreferences("shared_preferences",
                MODE_PRIVATE);

        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putBoolean("end_task_button_pressed", true);
        editor.apply();

        Log.i(INPUT, "> End task button");
        eventTimestamps.add(System.currentTimeMillis());
        events.add(I_END + " end task");


        boolean postQuestionnaireFinished = sharedPref.getBoolean("post_task_questionnaire_opened", false);
        boolean preQuestionnaireFinished = sharedPref.getBoolean("pre_task_questionnaire_opened", false);

        if (preQuestionnaireFinished && !postQuestionnaireFinished) {
            Toast.makeText(MainActivity.this, "Thank you, please answer this questionnaire",
                    Toast.LENGTH_SHORT).show();

            openPostTaskQuestionnaire();
        } else {
            taskConcluded();
        }
    }

    private void taskConcluded() {
        //shows welcome message again
        showWelcomeHome();

        //change back the notification to the default
        updateNotification("", this);
        clearText();

        //send data on task completed
        mBoundService.recordOnQuerySubmit();
        // clear the query result list by setting a null adapter
        setListAdapter(null);
        refreshTaskHint();

        //hide task hint
        TextView taskHint = findViewById(R.id.taskHint);
        Button taskCompleteButton = findViewById(R.id.taskComplete);
        taskHint.setVisibility(View.GONE);
        taskCompleteButton.setVisibility(View.GONE);

        //store and send checked links
        storeSearchLinks();
        rsm.uploadRecords(this.getApplicationContext());
        clearSearchLinks();

        resetSharedForNewTask(getApplicationContext());

        //end the task

        if (tm.getActiveTask() != null) {
            tm.finishTask();
        } else {
            //In this case android killed the task manager context, we restore it
            restoreTM();
            tm.finishTask();
        }

    }


    private void restoreTM() {
        Gson gson = new Gson();
        SharedPreferences sharedPref = getSharedPreferences("shared_preferences",
                Context.MODE_PRIVATE);
        String json = sharedPref.getString("activeTask", "");
        String activeTaskId = sharedPref.getString("activeTaskId", "");
        String activeTaskDate = sharedPref.getString("activeTaskDate", "");


        if (!json.equals("")) {
            UmobTask activeTask = gson.fromJson(json, UmobTask.class);
            tm.updateActiveTask(activeTask, activeTaskId, activeTaskDate);
        }
    }

    private void openPostTaskQuestionnaire() {
        if (tm.isTaskAvailable()) {
            Log.i(INPUT, "> Opening the post-task questionnaire");
            eventTimestamps.add(System.currentTimeMillis());
            events.add(I_LI + " post-task questionnaire button");

            String targetUrl = POST_TASK_URL + "?appid=" + iid +
                    "&taskid=" + tm.getActiveTaskId();

            // open up the browser activity to the specified link
            Bundle extras = new Bundle();
            extras.putString("ch.usi.jacopofidacaro.unimobile.QUERY", targetUrl);
            extras.putString("ch.usi.jacopofidacaro.unimobile.QUESTIONNAIRE_TYPE", "POST");

            startActivityForResult(new Intent(this, TaskBrowserActivity.class)
                    .putExtras(extras), TASK_DONE_REQUEST_CODE);
        }
    }

    private void refreshTaskHint() {

        SharedPreferences sharedPref = getSharedPreferences("shared_preferences",
                Context.MODE_PRIVATE);
        boolean permissionsGranted = sharedPref.getBoolean("permissionsGranted", false);
        if (!permissionsGranted) {
            //no tasks yet
            return;
        }

        boolean preQuestionnaireFinished = sharedPref.getBoolean("pre_task_questionnaire_opened", false);
        //boolean postQuestionnaireFinished = sharedPref.getBoolean("post_task_questionnaire_opened", false);

        TextView taskHint = findViewById(R.id.taskHint);
        Button taskCompleteButton = findViewById(R.id.taskComplete);


        if (tm.isTaskAvailable()) {
            UmobTask task = tm.getActiveTask();
            taskHint.setText(task.getTitle());
            if (preQuestionnaireFinished && tm.isTaskAvailable()) {
                taskHint.setVisibility(View.VISIBLE);
                taskCompleteButton.setVisibility(View.VISIBLE);
            }
        } else {
            taskHint.setVisibility(View.GONE);
            taskCompleteButton.setVisibility(View.GONE);
        }
    }

    public ListAdapter getListAdapter() {
        ListView listView = findViewById(R.id.resultsList);
        return listView.getAdapter();
    }

    public void setListAdapter(ListAdapter listAdapter) {
        ListView listView = findViewById(R.id.resultsList);
        listView.setAdapter(listAdapter);
    }

    public ListView getListView() {
        return findViewById(R.id.resultsList);
    }

    /*
     *Cross button callback
     */
    public void onResetButton(View view) {
        //remove query field text
        clearText();
    }

    public void returnToMain() {
        SharedPreferences sharedPref = getSharedPreferences("shared_preferences",
                Context.MODE_PRIVATE);
        boolean taskStarted = sharedPref.getBoolean("pre_task_questionnaire_opened", false);

        if (taskStarted) {
            Toast.makeText(MainActivity.this, "Please complete the task first",
                    Toast.LENGTH_SHORT).show();
        } else {
            //remove query field text
            clearText();

            //send data on task completed
            mBoundService.recordOnQuerySubmit();
            // clear the query result list by setting a null adapter
            setListAdapter(null);
            refreshTaskHint();

            //store and send checked links
            storeSearchLinks();
            rsm.uploadRecords(this.getApplicationContext());
            clearSearchLinks();

            //show menu
            showOrHideCrossMenu();
        }
    }

    public void showPopup(View view) {
        PopupMenu popup = new PopupMenu(this, view);
        MenuInflater inflater = popup.getMenuInflater();
        inflater.inflate(R.menu.actions, popup.getMenu());
        popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {

                switch (item.getItemId()) {
                    case R.id.menu_app_id:
                        showId();
                        return true;
                    case R.id.start_tutorial:
                        Intent i = new Intent(MainActivity.this, TutorialActivity.class);
                        startActivity(i);
                        return true;
                    case R.id.test_task:
                        showTestTask(getApplicationContext());
                        return true;
                    case R.id.app_version:
                        getAppVersion();
                        return true;
                    default:
                        return false;
                }
            }
        });
        popup.show();
    }

    private void getAppVersion() {
        int versionCode = BuildConfig.VERSION_CODE;
        String versionName = BuildConfig.VERSION_NAME;

        eventTimestamps.add(System.currentTimeMillis());
        events.add(I_LI + " app version");

        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setMessage(iid)
                .setTitle("App version")
                .setMessage(String.format("You currently have installed the Omicron App version %s (%d)", versionName, versionCode))
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                    }
                });
        builder.create().show();
    }


    private void showTestTask(Context ctx) {
        eventTimestamps.add(System.currentTimeMillis());
        events.add(I_LI + " test task");

        SharedPreferences sharedPref = getSharedPreferences("shared_preferences",
                Context.MODE_PRIVATE);
        boolean testTaskCreated = sharedPref.getBoolean("testTaskCreated", false);
        if (!testTaskCreated) {
            createTestTask(getApplicationContext());
        } else {
            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
            builder.setMessage(iid)
                    .setTitle("Test task done")
                    .setMessage("You have already done the test task")
                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                        }
                    });
            builder.create().show();
        }
    }

    public void showHint(View view) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        if (tm.getActiveTask() != null) {

            Log.i(INPUT, "> Showing task hint");
            eventTimestamps.add(System.currentTimeMillis());
            events.add(I_LI + " task hint");

            UmobTask task = tm.getActiveTask();
            builder.setMessage(task.getDescription())
                    .setTitle(task.getTitle())
                    .setCancelable(true)
                    .setPositiveButton(android.R.string.ok,
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    dialog.cancel();
                                }
                            })
                    .show();
        }
    }
}
