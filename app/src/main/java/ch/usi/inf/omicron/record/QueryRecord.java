package ch.usi.inf.omicron.record;

import java.io.Serializable;

public class QueryRecord implements Record, Serializable {

    public String content;
    public String jsonResult;
    public long timestamp;
    public String taskid;

    public QueryRecord(String content, String jsonResult, long timestamp, String taskid) {
        this.content = content;
        this.jsonResult = jsonResult;
        this.timestamp = timestamp;
        this.taskid = taskid;
    }

    public String toString() {
        String res = "Query record:\n";
        res += content;
        return res;
    }

}
