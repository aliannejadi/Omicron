package ch.usi.inf.omicron;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.View;
import android.widget.TextView;

import com.github.paolorotolo.appintro.AppIntro;
import com.github.paolorotolo.appintro.AppIntroFragment;

import static ch.usi.inf.omicron.Configuration.FIRST_INSTALLATION_SURVEY;
import static ch.usi.inf.omicron.MainActivity.eventTimestamps;
import static ch.usi.inf.omicron.MainActivity.events;
import static ch.usi.inf.omicron.UMob.iid;

public class TutorialActivity extends AppIntro {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        eventTimestamps.add(System.currentTimeMillis());
        events.add("tut");

        // Add your slide fragments here.
        // AppIntro will automatically generate the dots indicator and buttons.
        showSkipButton(false);

        addSlide(AppIntroFragment.newInstance("Welcome to Omicron!",
                "This short tutorial will teach you the main functionalities of the app",
                R.mipmap.ic_launcher, Color.parseColor("#27ae60")));

        addSlide(AppIntroFragment.newInstance("Tasks",
                "Answer the questionnaires and follow task instructions",
                R.drawable.common_full_open_on_phone, Color.parseColor("#2ecc71")));

        addSlide(AppIntroFragment.newInstance("Search",
                "Star links that helped you to complete each task",
                android.R.drawable.btn_star_big_on, Color.parseColor("#16a085")));

        addSlide(SurveySlide.newInstance(R.layout.activity_tutorial_survey));

        addSlide(AppIntroFragment.newInstance("Great", "You are ready!",
                R.drawable.ic_done_white, Color.parseColor("#3498db")));

        setSlideOverAnimation();
    }

    @Override
    public void onSkipPressed(android.support.v4.app.Fragment currentFragment) {
        super.onSkipPressed(currentFragment);
        // Do something when users tap on Skip button.
    }

    @Override
    public void onDonePressed(android.support.v4.app.Fragment currentFragment) {
        super.onDonePressed(currentFragment);
        // Do something when users tap on Done button.
        finish();
    }

    @Override
    public void onSlideChanged(@Nullable android.support.v4.app.Fragment oldFragment, @Nullable android.support.v4.app.Fragment newFragment) {
        super.onSlideChanged(oldFragment, newFragment);
        // Do something when the slide changes.
    }

    public void openSurveyWizard(View view) {

        eventTimestamps.add(System.currentTimeMillis());
        events.add("li" + " survey button");

        String targetUrl = FIRST_INSTALLATION_SURVEY + "?umobID=" + iid;

        // open up the browser activity to the specified link
        Bundle extras = new Bundle();
        extras.putString("ch.usi.jacopofidacaro.unimobile.QUERY", targetUrl);
        extras.putString("ch.usi.jacopofidacaro.unimobile.QUESTIONNAIRE_TYPE", "SURVEY");

        startActivityForResult(new Intent(this, TaskBrowserActivity.class)
                .putExtras(extras), 0);

        view.findViewById(R.id.surveyWizard).setVisibility(View.INVISIBLE);
        TextView text = view.findViewById(R.id.surveyDescription);

        if (text != null) {
            text.setText(R.string.survey_done);
        }

    }
}