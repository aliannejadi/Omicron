package ch.usi.inf.omicron.recordCommuter;

import java.util.List;

import ch.usi.inf.omicron.record.HistoryRecord;

public class HistoryRecordCommuter extends RecordCommuter {

    // constructor automatically converts the passed object to POJO
    public HistoryRecordCommuter(List<String> urls,
                                 List<String> titles,
                                 List<Long> timestamp,
                                 List<String> task_id) {

        super("history");

        record = new HistoryRecord(urls, titles, timestamp, task_id);
    }


    // display commuter in a readable format
    public String toString() {
        String res = "LocationCommuter " + key + "\n";
        res += "holding " + record.toString();
        return res;
    }

}
