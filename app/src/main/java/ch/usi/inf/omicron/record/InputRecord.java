package ch.usi.inf.omicron.record;

import java.io.Serializable;
import java.util.List;

public class InputRecord implements Record, Serializable {

    public List<String> event;
    public List<Long> timestamp;
    public List<String> rawType;
    public List<Integer> rawX;
    public List<Integer> rawY;
    public List<Long> rawTimestamp;

    public InputRecord() {

    }

    public InputRecord(List<String> events,
                       List<Long> timestamps,
                       List<String> rawType,
                       List<Integer> rawX,
                       List<Integer> rawY,
                       List<Long> rawTimestamp) {

        this.event = events;
        this.timestamp = timestamps;
        this.rawType = rawType;
        this.rawX = rawX;
        this.rawY = rawY;
        this.rawTimestamp = rawTimestamp;

    }

    public String toString() {

        StringBuilder res = new StringBuilder("input record:\n");
        res.append("event: [ ");
        for (String e : event)
            res.append(e).append(" ");
        res.append("] timestamp: [ ");
        for (Long tmp : timestamp)
            res.append(tmp).append(" ");
        res.append("]");

        return res.toString();
    }

}
