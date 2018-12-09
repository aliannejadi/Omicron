package ch.usi.inf.omicron.recordCommuter;

import android.os.SystemClock;

import java.util.ArrayList;
import java.util.List;

import ch.usi.inf.omicron.record.InputRecord;

public class InputRecordCommuter extends RecordCommuter {

    // constructor automatically converts the passed object to POJO
    public InputRecordCommuter(List<String> events,
                               List<Long> timestamps,
                               List<String> rawType,
                               List<Integer> rawX,
                               List<Integer> rawY,
                               List<Long> rawTimestamp) {

        super("input");

        List<Long> convertedrawTS = new ArrayList<>();
        for (long raw_ts : rawTimestamp) {
            long rawtimeInMillis = System.currentTimeMillis() + ((raw_ts * 1000L -
                    SystemClock.elapsedRealtimeNanos()) / 1000000L);
            convertedrawTS.add(rawtimeInMillis);
        }


        record = new InputRecord(events, timestamps, rawType, rawX, rawY, convertedrawTS);

    }

    public String toString() {
        String res = "InputRecordCommuter " + key + "\n";
        res += "holding " + record.toString();
        return res;
    }

}
