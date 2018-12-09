package ch.usi.inf.omicron;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.github.paolorotolo.appintro.ISlidePolicy;

import java.util.Objects;

import static android.content.Context.MODE_PRIVATE;

public class SurveySlide extends Fragment implements ISlidePolicy {

    private static final String ARG_LAYOUT_RES_ID = "layoutResId";
    private int layoutResId;

    public static SurveySlide newInstance(int layoutResId) {
        SurveySlide surveySlide = new SurveySlide();

        Bundle args = new Bundle();
        args.putInt(ARG_LAYOUT_RES_ID, layoutResId);
        surveySlide.setArguments(args);

        return surveySlide;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null && getArguments().containsKey(ARG_LAYOUT_RES_ID)) {
            layoutResId = getArguments().getInt(ARG_LAYOUT_RES_ID);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(layoutResId, container, false);
    }

    @Override
    public boolean isPolicyRespected() {
        SharedPreferences sharedPref = Objects.requireNonNull(this.getActivity()).getSharedPreferences("shared_preferences",
                MODE_PRIVATE);
        return sharedPref.getBoolean("isSurveyDone", false);
    }

    @Override
    public void onUserIllegallyRequestedNextPage() {
        Toast.makeText(this.getActivity(), "Complete survey first", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        Utils.hideSurveyButtonIfAlreadyDone(getContext(), getActivity());
    }


}