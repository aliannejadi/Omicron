package ch.usi.inf.omicron.recordCommuter;

import ch.usi.inf.omicron.record.QueryRecord;

public class QueryRecordCommuter extends RecordCommuter {

    // constructor automatically converts the passed object to POJO
    public QueryRecordCommuter(String content, String jsonResult, long timestamp, String taskid) {

        super("query");

        record = new QueryRecord(content, jsonResult, timestamp, taskid);
    }

    public String toString() {
        String res = "QueryCommuter " + key + "\n";
        res += "holding " + record.toString();
        return res;
    }

}
