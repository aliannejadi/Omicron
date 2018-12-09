package ch.usi.inf.omicron.record;

import java.io.Serializable;
import java.util.List;

public class HistoryRecord implements Record, Serializable {

    public List<String> title;
    public List<String> URL;
    public List<String> taskid;
    public List<Long> timestamp;

    public HistoryRecord(List<String> titles,
                         List<String> urls,
                         List<Long> timestamp,
                         List<String> taskid) {

        this.title = titles;
        this.URL = urls;
        this.timestamp = timestamp;
        this.taskid = taskid;

    }

    public String toString() {

        StringBuilder res = new StringBuilder("history record:\n");
        res.append("titles: [ ");
        for (String t : title)
            res.append(t).append(" ");
        res.append("] urls: [ ");
        for (String url : URL)
            res.append(url).append(" ");
        res.append("] timestamp: [ ");
        for (Long tmp : timestamp)
            res.append(tmp).append(" ");
        res.append("]");

        return res.toString();
    }

}
