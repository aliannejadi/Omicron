package ch.usi.inf.omicron.recordCommuter;

import java.util.List;

import ch.usi.inf.omicron.record.RelevantResultRecord;

public class RelevantResultRecordCommuter extends RecordCommuter {

    // constructor automatically converts the passed object to POJO
    public RelevantResultRecordCommuter(List<Integer> indeces,
                                        List<String> titles,
                                        List<String> links,
                                        List<Long> timestamps,
                                        List<String> taskid) {

        super("relevant_result");

        record = new RelevantResultRecord(indeces, titles, links, timestamps, taskid);

    }

    // display commuter in a readable format
    public String toString() {
        String res = "RelevantResultCommuter " + key + "\n";
        res += "holding " + record.toString();
        return res;
    }

}
