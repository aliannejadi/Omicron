package ch.usi.inf.omicron.recordCommuter;

import android.provider.CallLog;

import java.util.ArrayList;
import java.util.List;

import ch.usi.inf.omicron.record.CallRecord;

public class CallRecordCommuter extends RecordCommuter {

    public CallRecordCommuter(List<String> numbers_hashes, List<String> names, List<Integer> durations,
                              List<Integer> types, List<Long> timestamps) {
        super("call");

        List<String> decoded_types = new ArrayList<>();

        for (int i = 0; i < numbers_hashes.size(); i++) {

            String call_type = "";
            switch (types.get(i)) {
                case CallLog.Calls.INCOMING_TYPE:
                    call_type = "INCOMING";
                    break;
                case CallLog.Calls.OUTGOING_TYPE:
                    call_type = "OUTGOING";
                    break;
                case CallLog.Calls.MISSED_TYPE:
                    call_type = "MISSED";
                    break;
            }

            decoded_types.add(call_type);
        }
        record = new CallRecord(numbers_hashes, names, durations, decoded_types, timestamps);

    }

    // display commuter in a readable format
    public String toString() {
        String res = "CallCommuter " + key + "\n";
        res += "holding " + record.toString();
        return res;
    }

}
