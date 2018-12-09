package ch.usi.inf.omicron.record;

import java.io.Serializable;
import java.util.List;

public class ScreenRecord implements Record, Serializable {

    public List<String> event;
    public List<Long> timestamp;
    public List<String> taskid;

    public ScreenRecord(List<String> event,
                        List<Long> timestamp,
                        List<String> taskid) {

        this.event = event;
        this.timestamp = timestamp;
        this.taskid = taskid;

    }

    public String toString() {

        StringBuilder res = new StringBuilder("Usage record:\n");
        res.append("event list: [ ");
        for (String e : event)
            res.append(e).append(" ");
        res.append("] timestamp: [ ");
        for (Long tmp : timestamp)
            res.append(tmp).append(" ");
        res.append("]");
        return res.toString();

    }

}
