package ch.usi.inf.omicron.taskManager;

import java.io.Serializable;
import java.util.Calendar;

/**
 * UmobTask object as stored in Firebase database
 */

public class UmobTask implements Serializable {

    //all public fields are serialised
    public String title;
    public String description;
    public int windowStart;
    public int windowEnd;
    public boolean done;
    public boolean rescheduled = false;
    public long doneTimestamp;
    public long startTimestamp;

    public UmobTask(String title,
                    String description,
                    int windowStart,
                    int windowEnd,
                    boolean done,
                    long doneTimestamp,
                    long startTimestamp) {

        this.title = title;
        this.description = description;
        this.windowStart = windowStart;
        this.windowEnd = windowEnd;
        this.done = done;
        this.doneTimestamp = doneTimestamp;
        this.startTimestamp = startTimestamp;

    }

    public UmobTask() {

    }

    private boolean inTimeWindow() {

        Calendar rightNow = Calendar.getInstance();
        int currentHour = rightNow.get(Calendar.HOUR_OF_DAY);
        //Log.v("UmobTask", String.format("c:%d - w: s %d  e %d", currentHour, windowStart,  windowEnd));
        return currentHour >= windowStart && currentHour < windowEnd;
    }

    public boolean isDone() {
        return this.done;
    }

    public boolean isNotDoneOrExpired() {
        return !this.isDone() && this.inTimeWindow();
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public void finish() {
        doneTimestamp = System.currentTimeMillis();
        done = true;
    }
}
