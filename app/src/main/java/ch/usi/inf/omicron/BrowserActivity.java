package ch.usi.inf.omicron;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.webkit.WebBackForwardList;
import android.webkit.WebChromeClient;
import android.webkit.WebHistoryItem;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;

import java.util.ArrayList;

import static ch.usi.inf.omicron.MainActivity.eventTimestamps;
import static ch.usi.inf.omicron.MainActivity.events;
import static ch.usi.inf.omicron.MainActivity.rawInpuTimestamps;
import static ch.usi.inf.omicron.MainActivity.rawInputTypes;
import static ch.usi.inf.omicron.MainActivity.rawInputXs;
import static ch.usi.inf.omicron.MainActivity.rawInputYs;
import static ch.usi.inf.omicron.UMob.tm;

/**
 * This Activity allows UMob administrators to create, delete and assign tasks the users will need
 * to perform. It connects to Firebase Database to retrieve the tasks and synchronise the displayed
 * task list. Thi activity requires an internet connection to have any use.
 */
public class BrowserActivity extends AppCompatActivity {

    // history list
    protected static ArrayList<String> historyTitles = new ArrayList<>();
    protected static ArrayList<String> historyUrls = new ArrayList<>();
    protected static ArrayList<Long> historyTimestamp = new ArrayList<>();
    protected static ArrayList<String> historyTaskID = new ArrayList<>();
    // input events identifier constants
    private final String I_BBB = "bbb";
    // log tags
    private final String INPUT = "umob.Browser.input";
    private final String HISTORY = "umob.Browser.history";
    // custom web view
    private WebView browser;
    //Progress bar
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().requestFeature(Window.FEATURE_PROGRESS);
        setContentView(R.layout.activity_browser);

        progressBar = findViewById(R.id.progress_bar_browser);

        initWebView(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                view.loadUrl(url);
                return false;
            }

        });

    }

    protected WebView initWebView(WebViewClient wb) {

        browser = findViewById(R.id.browserView);
        browser.getSettings().setJavaScriptEnabled(true);
        browser.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int progress) {
                progressBar.setProgress(progress);
                if (progress > 80) {
                    progressBar.setVisibility(View.GONE);

                } else {
                    progressBar.setVisibility(View.VISIBLE);

                }
            }
        });
        browser.setVisibility(View.VISIBLE);
        browser.setWebViewClient(wb);

        Intent intent = getIntent();
        Bundle extras = intent.getExtras();
        String url = extras.getString("ch.usi.jacopofidacaro.unimobile.QUERY");

        // Load the webpage
        browser.loadUrl(url);

        return browser;
    }

    @Override
    public void onBackPressed() {

        Log.i(INPUT, "> BACK BUTTON (BROWSER) [bbm]");
        eventTimestamps.add(System.currentTimeMillis());
        events.add(I_BBB);

        super.onBackPressed();

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

    @Override
    protected void onStop() {

        browser = (WebView) findViewById(R.id.browserView);
        Log.i(HISTORY, "browser: " + browser.getTitle());
        WebBackForwardList history = browser.copyBackForwardList();
        Log.i(HISTORY, "history: " + history.toString());
        int size = history.getSize();
        Log.i(HISTORY, "history size: " + size);

        for (int i = 0; i < size; i++) {
            WebHistoryItem item = history.getItemAtIndex(i);
            Log.i(HISTORY, "history item: " + item.getUrl());
            historyTitles.add(item.getTitle());
            historyUrls.add(item.getUrl());
            historyTimestamp.add(System.currentTimeMillis());
            historyTaskID.add(tm.getActiveTaskId());
        }

        super.onStop();

    }


    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        browser.saveState(outState);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        browser.restoreState(savedInstanceState);
    }

}
