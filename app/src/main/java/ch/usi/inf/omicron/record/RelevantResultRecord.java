package ch.usi.inf.omicron.record;

import java.io.Serializable;
import java.util.List;

public class RelevantResultRecord implements Record, Serializable {

    public List<Integer> index;
    public List<String> title;
    public List<String> link;
    public List<String> taskid;
    public List<Long> timestamp;

    public RelevantResultRecord(List<Integer> index,
                                List<String> title,
                                List<String> link,
                                List<Long> timestamp,
                                List<String> taskid) {

        this.index = index;
        this.title = title;
        this.link = link;
        this.timestamp = timestamp;
        this.taskid = taskid;

    }

    public String toString() {
        StringBuilder res = new StringBuilder("relevant result record:\n");
        res.append("index: [ ");
        for (Integer i : index)
            res.append(i).append(" ");
        res.append("] title: [ ");
        for (String t : title)
            res.append(t).append(" ");
        res.append("] link: [ ");
        for (String l : link)
            res.append(l).append(" ");
        res.append("] timestamp: [ ");
        for (Long tmp : timestamp)
            res.append(tmp).append(" ");
        res.append("]");
        res.append("] id: [ ");
        for (String ta : taskid)
            res.append(ta).append(" ");
        res.append("]");
        return res.toString();
    }

}
