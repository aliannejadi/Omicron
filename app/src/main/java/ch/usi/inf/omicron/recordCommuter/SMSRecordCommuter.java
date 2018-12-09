package ch.usi.inf.omicron.recordCommuter;

import android.provider.Telephony;

import java.util.ArrayList;
import java.util.List;

import ch.usi.inf.omicron.record.SMSRecord;

public class SMSRecordCommuter extends RecordCommuter {

    public SMSRecordCommuter(List<String> numbers_hashes, List<String> names, List<String> bodies,
                             List<Integer> types, List<Long> timestamps) {
        super("sms");


        List<String> decoded_types = new ArrayList<>();
        List<Integer> characters = new ArrayList<>();


        for (int i = 0; i < numbers_hashes.size(); i++) {

            String sms_type = "";
            switch (types.get(i)) {
                case Telephony.Sms.MESSAGE_TYPE_INBOX:
                    sms_type = "INBOX";
                    break;
                case Telephony.Sms.MESSAGE_TYPE_OUTBOX:
                    sms_type = "OUTBOX";
                    break;
                case Telephony.Sms.MESSAGE_TYPE_DRAFT:
                    sms_type = "DRAFT";
                    break;
                case Telephony.Sms.MESSAGE_TYPE_SENT:
                    sms_type = "SENT";
                    break;
                default:
                    sms_type = "";
            }

            decoded_types.add(sms_type);

            int length = bodies.get(i).length();
            characters.add(length);
        }

        record = new SMSRecord(numbers_hashes, names, characters, decoded_types, timestamps);

    }

    // display commuter in a readable format
    public String toString() {
        String res = "SMSCommuter " + key + "\n";
        res += "holding " + record.toString();
        return res;
    }

}
