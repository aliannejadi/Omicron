package ch.usi.inf.omicron.recordCommuter;

import java.util.List;

import ch.usi.inf.omicron.record.ScreenRecord;

public class ScreenRecordCommuter extends RecordCommuter {

    // constructor automatically converts the passed object to POJO
    public ScreenRecordCommuter(List<String> events,
                                List<Long> timestamps,
                                List<String> taskid) {

        super("screen");

        record = new ScreenRecord(events, timestamps, taskid);

    }

    // display commuter in a readable format
    public String toString() {
        String res = "ScreenCommuter " + key + "\n";
        res += "holding " + record.toString();
        return res;
    }

}
