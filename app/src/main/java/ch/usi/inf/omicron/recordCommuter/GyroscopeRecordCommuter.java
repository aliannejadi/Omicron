package ch.usi.inf.omicron.recordCommuter;

import java.util.List;

import ch.usi.inf.omicron.record.GyroscopeRecord;

public class GyroscopeRecordCommuter extends RecordCommuter {

    // constructor automatically converts the passed object to POJO
    public GyroscopeRecordCommuter(List<Float> x,
                                   List<Float> y,
                                   List<Float> z,
                                   List<Long> timestamp) {

        super("gyroscope");

        record = new GyroscopeRecord(x, y, z, timestamp);

    }

    // display commuter in a readable format
    public String toString() {
        String res = "GyroscopeCommuter " + key + "\n";
        res += "holding " + record.toString();
        return res;
    }

}
