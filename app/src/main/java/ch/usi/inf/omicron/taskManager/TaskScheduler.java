package ch.usi.inf.omicron.taskManager;

import android.app.job.JobParameters;
import android.app.job.JobService;
import android.content.Context;
import android.content.SharedPreferences;

import ch.usi.inf.omicron.Log;

import static ch.usi.inf.omicron.UMob.tm;

public class TaskScheduler extends JobService {
    private static final String TAG = "TaskScheduler";

    @Override
    public boolean onStartJob(JobParameters params) {
        Log.v(TAG, "Checking the task windows");
        updateTaskList(getApplicationContext());
        //reschedule
        tm.schedule(getApplicationContext());

        return false;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        return false;
    }


    private void updateTaskList(Context ctx) {
        SharedPreferences sharedPref = ctx.getSharedPreferences("shared_preferences",
                Context.MODE_PRIVATE);
        boolean taskStarted = sharedPref.getBoolean("pre_task_questionnaire_opened", false);

        boolean update_task = true;

        //don't update the tasks if we are doing a task
        if (taskStarted && tm.isTaskAvailable()) {
            //and the task must not be expired
            if (tm.getActiveTask().isNotDoneOrExpired()) {
                update_task = false;
            }
        }

        String prv_task_id = tm.getActiveTaskId();
        UmobTask prv_task = tm.getActiveTask();


        tm.findSuitableTask(ctx, update_task, prv_task_id, prv_task);
    }
}
