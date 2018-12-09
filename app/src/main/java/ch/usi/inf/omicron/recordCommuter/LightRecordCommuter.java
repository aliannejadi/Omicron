package ch.usi.inf.omicron.recordCommuter;

import java.util.List;

import ch.usi.inf.omicron.record.LightRecord;

public class LightRecordCommuter extends RecordCommuter {

    // constructor automatically converts the passed object to POJO
    public LightRecordCommuter(List<Float> lumL,
                               List<Long> timestamp) {

        super("light");

        record = new LightRecord(lumL, timestamp);

    }

    // display commuter in a readable format
    public String toString() {
        String res = "LightCommuter " + key + "\n";
        res += "holding " + record.toString();
        return res;
    }

}
