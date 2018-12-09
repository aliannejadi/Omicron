package ch.usi.inf.omicron;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.Toast;


public class TaskBrowserActivity extends BrowserActivity {
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_taskbrowser);
        WebView wb = initWebView(new WebViewClient() {
            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                //Show button when Qualtrix questionnaire ends
                if (request.getUrl().toString().contains("WRSiteInterceptEngine")) {
                    showCompleteButton();
                    markQuestionnaireDone();
                }
                return null;
            }
        });
    }

    private void showCompleteButton() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Button complete = findViewById(R.id.completeQuest);
                complete.setVisibility(View.VISIBLE);
            }
        });
    }

    @Override
    public void onBackPressed() {
        Toast.makeText(this, "Complete questionnaire first", Toast.LENGTH_LONG).show();

    }

    public void FinishActivity(View view) {
        //questionnaire ended without result
        setResult(Activity.RESULT_CANCELED);
        //kill activity
        finish();
    }

    /*
     * Write in the shared preference the completion of the questionnaire when the user
     * tap the complete button at the end of the survey
     * */
    private void markQuestionnaireDone() {
        SharedPreferences sharedPref = getSharedPreferences("shared_preferences",
                Context.MODE_PRIVATE);

        Intent intent = getIntent();
        Bundle extras = intent.getExtras();
        String type = null;
        if (extras != null) {
            type = extras.getString("ch.usi.jacopofidacaro.unimobile.QUESTIONNAIRE_TYPE");
        }
        SharedPreferences.Editor editor = sharedPref.edit();

        assert type != null;
        switch (type) {
            case "SURVEY":
                editor.putBoolean("isSurveyDone", true);
                editor.apply();
                break;
            case "PRE":
                editor.putBoolean("pre_task_questionnaire_opened", true);
                editor.putBoolean("post_task_questionnaire_opened", false);
                editor.apply();
                break;
            case "POST":
                editor.putBoolean("post_task_questionnaire_opened", true);
                editor.apply();
                break;
        }

    }
}
