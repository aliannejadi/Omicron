package ch.usi.inf.omicron.taskManager;

import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.gson.Gson;

import java.util.Calendar;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import ch.usi.inf.omicron.Log;
import ch.usi.inf.omicron.Utils;

import static android.content.Context.MODE_PRIVATE;
import static ch.usi.inf.omicron.UMob.iid;
import static ch.usi.inf.omicron.Utils.resetSharedForNewTask;


public class TaskManager {

    private static final String TASK = "umob.Main.task";
    // tasks retrieved from the database (Date -> Hash of Tasks)
    private HashMap<String, LinkedHashMap<String, UmobTask>> DateTasks = new HashMap<>();
    // reference to the json branch that will hold the user's tasks
    private DatabaseReference idRef;
    // active task to be done by the user
    private UmobTask activeTask;
    private String activeTaskId;
    private String activeTaskDate;

    private SharedPreferences sharedPref;

    public TaskManager(final Context ctx) {

        init(ctx);

        //Start the task scheduler that checks every hour that the tasks are still in the window
        schedule(ctx);
    }

    private static long utcMillisNextHour() {
        Calendar now = Calendar.getInstance();

        //Set current time at the start of the next hour
        Calendar startNextHour = Calendar.getInstance();
        startNextHour.add(Calendar.HOUR, 1);
        startNextHour.set(Calendar.MINUTE, 0);

        return (Math.abs(startNextHour.getTimeInMillis() - now.getTimeInMillis()));
    }

    private void init(Context ctx) {
        sharedPref = ctx.getSharedPreferences("shared_preferences",
                MODE_PRIVATE);
        boolean permissionsGranted = sharedPref.getBoolean("permissionsGranted", false);

        Log.i(TASK, "Retrieving tasks from database at id: " + iid);
        idRef = FirebaseDatabase.getInstance().getReference(String.format("%s/tasks", iid));

        //attach the Firebase listener
        startTaskListener(ctx);

    }

    public void startTaskListener(final Context ctx) {
        Query query = idRef;

        query.addValueEventListener(new ValueEventListener() {

            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                //check for the new tasks
                updateTaskList(dataSnapshot, ctx);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.w(TASK, "Failed to read task.");
            }

        });
    }

    void schedule(Context ctx) {
        JobScheduler mJobScheduler = (JobScheduler)
                ctx.getSystemService(Context.JOB_SCHEDULER_SERVICE);
        JobInfo.Builder builder = new JobInfo.Builder(1,
                new ComponentName(ctx.getPackageName(),
                        TaskScheduler.class.getName()));

        //10 minutes interval to consume less energy
        int delay = 10 * 60 * 1000;
        builder.setMinimumLatency(utcMillisNextHour()); // wait at least
        builder.setOverrideDeadline(utcMillisNextHour() + delay); // maximum delay
        assert mJobScheduler != null;
        mJobScheduler.schedule(builder.build());

        Log.v(TASK, "New in window check in ms:" + utcMillisNextHour());
    }

    private void updateTaskList(DataSnapshot dataSnapshot, Context ctx) {
        SharedPreferences sharedPref = ctx.getSharedPreferences("shared_preferences",
                MODE_PRIVATE);
        boolean taskStarted = sharedPref.getBoolean("pre_task_questionnaire_opened", false);

        boolean update_task = true;

        //don't update the tasks if we are doing a task
        if (taskStarted && this.isTaskAvailable()) {
            //and the task must not be expired
            if (this.getActiveTask().isNotDoneOrExpired()) {
                update_task = false;
            }
        }

        String prv_task_id = getActiveTaskId();
        UmobTask prv_task = getActiveTask();

        Log.i(TASK, "Tasks on database got updated.");
        resetTaskModel();

        //build Date-Task model
        //for every date
        buildTaskModel(dataSnapshot);

        findSuitableTask(ctx, update_task, prv_task_id, prv_task);


    }

    private void buildTaskModel(DataSnapshot dataSnapshot) {
        for (DataSnapshot date : dataSnapshot.getChildren()) {
            //set for tasks
            LinkedHashMap<String, UmobTask> tasks = new LinkedHashMap<>();
            //for every task
            for (DataSnapshot ds : date.getChildren()) {
                try {

                    UmobTask task = ds.getValue(UmobTask.class);

                    if (task != null) {

                        String taskId = ds.getKey();
                        tasks.put(taskId, task);
                        Log.i(TASK, "Retrieved task: " + taskId + " done " + task.done + " title " + task.getTitle()
                                + "window:" + task.windowStart + "-" + task.windowEnd);
                    }
                } catch (Exception ignored) {
                    Log.e(TASK, "Malformatted task");
                }
            }
            DateTasks.put(date.getKey(), tasks);
        }
    }

    void findSuitableTask(Context ctx, boolean update_task, String prv_task_id, UmobTask prv_task) {
        //temporary values before updating
        UmobTask temp_task = null;
        String temp_taskid = null;
        String temp_date = null;
        boolean task_found = false;

        String isoDate = String.format("%tF", Calendar.getInstance());
        try {

            for (Map.Entry<String, UmobTask> taskEntry : DateTasks.get(isoDate).entrySet()) {
                UmobTask task = taskEntry.getValue();
                String taskId = taskEntry.getKey();

                //find task not done and inside the time window
                if (task.isNotDoneOrExpired() && !task_found && update_task) {
                    Log.i(TASK, "Selected task: " + taskId + " done " + task.done + " title " + task.getTitle()
                            + "window:" + task.windowStart + "-" + task.windowEnd);

                    temp_task = task;
                    temp_taskid = taskId;
                    temp_date = isoDate;
                    task_found = true;
                }

            }
        } catch (Exception ignore) {
            Log.e(TASK, "No task for key " + isoDate);
        }

        //update the active task
        updateActiveTask(temp_task, temp_taskid, temp_date);

        //update main activity
        broadcastTask(ctx, update_task, prv_task_id, prv_task);
    }

    private void broadcastTask(Context ctx, boolean update_task, String prv_task_id, UmobTask prv_task) {
        if (update_task) {
            //No task can be done and I was doing one but not completed before the update
            if (getActiveTask() == null && prv_task_id != null && !prv_task.done) {
                Intent intent = new Intent("TASK_EXPIRED");


                //task was already rescheduled no more task
                if (prv_task.rescheduled) {
                    resetSharedForNewTask(ctx);
                    Utils.updateNotification("", ctx);
                    ctx.sendBroadcast(intent);
                } else {
                    //reschedule task if we can
                    Utils.updateNotification("", ctx);
                    ctx.sendBroadcast(intent);
                    reschedule(prv_task_id, prv_task);
                }
            }

            //No more task and completed the previous one
            else if (getActiveTask() == null && prv_task_id != null && prv_task.done) {
                //can we show a previous undone task?
                revive(prv_task_id, prv_task);
            }

            //new task arrived, it's different than what I see
            else if (getActiveTask() != null && (prv_task_id == null || !prv_task_id.equals(getActiveTaskId()))) {
                Utils.resetSharedForNewTask(ctx);
                Intent intent = new Intent("REFRESH_TASK_HINT");
                ctx.sendBroadcast(intent);
                Utils.updateNotification(this.getActiveTask().getTitle(), ctx);
            }
        }
    }

    /*
     * Reschedule a previous undone task
     */

    private void revive(String prv_task_id, UmobTask prv_task) {
        String isoDate = String.format("%tF", Calendar.getInstance());
        try {
            for (Map.Entry<String, UmobTask> taskEntry : DateTasks.get(isoDate).entrySet()) {
                if (!taskEntry.getKey().equals(prv_task_id)) {
                    UmobTask task = taskEntry.getValue();
                    String task_id = taskEntry.getKey();

                    //there's an earlier task not done/rescheduled
                    if (task.windowEnd <= prv_task.windowEnd && !task.done && !task.rescheduled) {

                        task.windowStart = prv_task.windowStart;
                        task.windowEnd = prv_task.windowEnd;
                        task.rescheduled = true;

                        //reissue on Firebase
                        idRef.child(isoDate).child(task_id).setValue(task);
                        //we found it we can stop
                        return;
                    }
                }
            }
        } catch (Exception ignore) {
            Log.e(TASK, "Revive: No task for key " + isoDate);
        }
    }

    /*
     * we issue task again as soon as it expires if it doesn't overlap with another task.
     * if it does overlap the user see it if it completes the next task.
     */
    private void reschedule(String taskToRescheduleID, UmobTask taskToReschedule) {


        int interval = taskToReschedule.windowEnd - taskToReschedule.windowStart;
        //new intervals
        int newStart = taskToReschedule.windowStart + interval;
        //can't reschedule task to a new day
        if (newStart > 24) {
            return;
        }
        int newEnd = taskToReschedule.windowEnd + interval;
        if (newEnd > 24)
            newEnd = 24;

        boolean overlaps = false;

        //check if it overlaps
        String isoDate = String.format("%tF", Calendar.getInstance());
        try {
            for (Map.Entry<String, UmobTask> taskEntry : DateTasks.get(isoDate).entrySet()) {
                if (!taskEntry.getKey().equals(taskToRescheduleID)) {
                    UmobTask task = taskEntry.getValue();
                    if (newStart <= task.windowEnd && newEnd > task.windowStart) {
                        overlaps = true;
                    }
                }
            }
        } catch (Exception ignore) {
            Log.e(TASK, "Reschedule: No task for key " + isoDate);
        }

        if (!overlaps) {
            taskToReschedule.windowStart = newStart;
            taskToReschedule.windowEnd = newEnd;
            taskToReschedule.rescheduled = true;

            //reissue on Firebase
            idRef.child(isoDate).child(taskToRescheduleID).setValue(taskToReschedule);
        }
    }

    private void resetTaskModel() {
        DateTasks.clear();
    }

    public void updateActiveTask(UmobTask task, String id, String date) {
        activeTask = task;
        activeTaskId = id;
        activeTaskDate = date;

        //save in case this object is killed
        exportTM();
    }


    private void exportTM() {
        Gson gson = new Gson();
        SharedPreferences.Editor prefsEditor = sharedPref.edit();
        String json = gson.toJson(activeTask);
        prefsEditor.putString("activeTask", json);
        prefsEditor.putString("activeTaskId", activeTaskId);
        prefsEditor.putString("activeTaskDate", activeTaskDate);
        prefsEditor.apply();
    }


    public boolean isTaskAvailable() {
        return activeTask != null && !activeTask.isDone();
    }

    public UmobTask getActiveTask() {
        return activeTask;
    }

    public String getActiveTaskId() {
        return activeTaskId;
    }

    public void startTask() {
        activeTask.startTimestamp = System.currentTimeMillis();

        //save in case this object is killed
        exportTM();
    }

    public void finishTask() {
        activeTask.finish();
        idRef.child(activeTaskDate).child(activeTaskId).setValue(activeTask);

        //reset persistent state
        SharedPreferences.Editor prefsEditor = sharedPref.edit();
        prefsEditor.putString("activeTask", "");
        prefsEditor.putString("activeTaskId", "");
        prefsEditor.putString("activeTaskDate", "");
        prefsEditor.apply();
    }
}
