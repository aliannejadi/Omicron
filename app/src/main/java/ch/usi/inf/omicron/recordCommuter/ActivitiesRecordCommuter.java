package ch.usi.inf.omicron.recordCommuter;

import java.util.List;

import ch.usi.inf.omicron.record.ActivitiesRecord;

public class ActivitiesRecordCommuter extends RecordCommuter {

    // constructor automatically converts the passed object to POJO
    public ActivitiesRecordCommuter(List<String> events,
                                    List<Long> timestamps,
                                    List<String> taskid) {

        super("activities");

        record = new ActivitiesRecord(events, timestamps, taskid);

    }

    // display commuter in a readable format
    public String toString() {
        String res = "ActivitiesCommuter " + key + "\n";
        res += "holding " + record.toString();
        return res;
    }

}
