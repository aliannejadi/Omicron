package ch.usi.inf.omicron.record;

import java.io.Serializable;

public class SelectionItemRecord implements Record, Serializable {

    public String title;
    public String link;
    public String description;
    public String taskid;
    public long timestamp;

    public SelectionItemRecord(String title,
                               String link,
                               String description,
                               long timestamp,
                               String taskid) {

        this.title = title;
        this.link = link;
        this.description = description;
        this.timestamp = timestamp;
        this.taskid = taskid;

    }

    public String toString() {
        String res = "Selection item record:\n";
        res += this.link;
        return res;
    }

}
