package ch.usi.inf.omicron;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.support.annotation.NonNull;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.Calendar;

import ch.usi.inf.omicron.taskManager.UmobTask;

import static android.content.Context.MODE_PRIVATE;
import static ch.usi.inf.omicron.UMob.iid;

public class Utils {
    private static final String service_channel_id = "service_channel_id";
    private static final String task_channel_id = "task_channel_id";

    public static void updateNotification(String text, Context ctx) {


        Notification.Builder builder = buildNotification(text, ctx);


        Notification notification = builder.build();
        NotificationManager notificationManager = (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
        int notId = 1;
        assert notificationManager != null;
        notificationManager.notify(notId, notification);

    }

    @NonNull
    public static Notification.Builder buildNotification(String text, Context ctx) {
        Intent notificationIntent = new Intent(ctx, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent
                .getActivity(ctx, 0, notificationIntent, 0);

        String displayText;
        String channelId;

        if (text.isEmpty()) {
            displayText = (String) ctx.getText(R.string.notification_message);
            channelId = service_channel_id;

        } else {
            displayText = text;
            channelId = task_channel_id;
            ;
        }

        Notification.Builder builder;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager notificationManager = (NotificationManager) ctx
                    .getSystemService(Context.NOTIFICATION_SERVICE);
            assert notificationManager != null;


            if (notificationManager.getNotificationChannel(service_channel_id) == null) {
                createNotificationChannel(ctx, "Service", "Service Channel", NotificationManager.IMPORTANCE_LOW, service_channel_id, false);
            }

            if (notificationManager.getNotificationChannel(task_channel_id) == null) {
                createNotificationChannel(ctx, "Task", "Task Channel", NotificationManager.IMPORTANCE_HIGH, task_channel_id, true);
            }


            builder = new Notification.Builder(ctx, channelId);
            builder.setChannelId(channelId);
        } else {
            //Update Service notification
            builder = new Notification.Builder(ctx);
        }

        builder.setContentText(displayText)
                .setSmallIcon(R.drawable.simplesearch)
                .setLargeIcon(BitmapFactory.decodeResource(ctx.getResources(),
                        R.mipmap.ic_launcher))
                .setContentIntent(pendingIntent);


        if (!text.isEmpty()) {
            //display notification with sound if we have a task and the activity is in background
            //if (!this.hasWindowFocus()) {
            builder.setPriority(Notification.PRIORITY_HIGH).setDefaults(Notification.DEFAULT_ALL);

            //}

            builder.setContentTitle(ctx.getText(R.string.notification_new_task));
            builder.addAction(android.R.drawable.sym_action_email, "Open Task", pendingIntent);
        } else {
            builder.setContentTitle(ctx.getText(R.string.notification_title))
                    .setPriority(Notification.PRIORITY_LOW);
        }
        return builder;
    }

    public static void resetSharedForNewTask(Context ctx) {
        SharedPreferences sharedPref = ctx.getSharedPreferences("shared_preferences",
                MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putBoolean("pre_task_questionnaire_opened", false);
        editor.putBoolean("post_task_questionnaire_opened", false);
        editor.putBoolean("end_task_button_pressed", false);
        editor.apply();

    }

    public static void startService(Context context, Intent intent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent);
        } else {
            context.startService(intent);
        }
    }

    static void createTestTask(Context ctx) {
        String ref = String.format("%s/tasks", iid);
        DatabaseReference idRef = FirebaseDatabase.getInstance().getReference(ref);
        SharedPreferences sharedPref = ctx.getSharedPreferences("shared_preferences",
                MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putBoolean("testTaskCreated", true);
        editor.apply();

        int endTime = Calendar.getInstance().get(Calendar.HOUR_OF_DAY) + 4;
        if (endTime > 24) {
            endTime = 24;
        }

        final UmobTask testTask = new UmobTask("Radio waves", "Search for evidence that radio waves from " +
                "cell towers or mobile phones affect brain cancer incidence.",
                Calendar.getInstance().get(Calendar.HOUR_OF_DAY),
                endTime, false,
                -1, -1);

        idRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (snapshot.getValue() == null) {
                    final String isoDate = String.format("%tF", Calendar.getInstance());
                    //user does not exist create test task
                    snapshot.getRef().child(isoDate).child("test-task").setValue(testTask);
                }  //user exists

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    static void hideSurveyButtonIfAlreadyDone(Context ctx, Activity act) {
        SharedPreferences sharedPref = ctx.getSharedPreferences("shared_preferences",
                MODE_PRIVATE);
        boolean isDone = sharedPref.getBoolean("isSurveyDone", false);
        if (isDone) {
            Button button = act.findViewById(R.id.surveyWizard);
            button.setVisibility(View.INVISIBLE);

            TextView text = act.findViewById(R.id.surveyDescription);
            text.setText(R.string.survey_done);
        }
    }

    private static void createNotificationChannel(Context ctx, String channel_name, String channel_description, int importance, String channelId, boolean badge) {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(channelId, channel_name, importance);
            channel.setDescription(channel_description);
            channel.setShowBadge(badge);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            NotificationManager notificationManager = ctx.getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

}
