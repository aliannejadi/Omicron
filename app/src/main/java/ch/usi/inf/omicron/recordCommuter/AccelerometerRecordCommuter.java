package ch.usi.inf.omicron.recordCommuter;

import java.util.List;

import ch.usi.inf.omicron.record.AccelerometerRecord;

public class AccelerometerRecordCommuter extends RecordCommuter {

    // constructor automatically converts the passed object to POJO
    public AccelerometerRecordCommuter(List<Float> x,
                                       List<Float> y,
                                       List<Float> z,
                                       List<Long> timestamp) {

        super("accelerometer");

        record = new AccelerometerRecord(x, y, z, timestamp);

    }

    // display commuter in a readable format
    public String toString() {
        String res = "AccelerometerCommuter " + key + "\n";
        res += "holding " + record.toString();
        return res;
    }
}
