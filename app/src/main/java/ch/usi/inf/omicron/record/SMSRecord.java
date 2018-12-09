package ch.usi.inf.omicron.record;

import java.io.Serializable;
import java.util.List;

public class SMSRecord implements Record, Serializable {
    public List<String> numbers_hashes;
    public List<String> names;
    //length in characters
    public List<Integer> characters;
    public List<String> sms_types;
    public List<Long> timestamps;

    public SMSRecord(List<String> numbers_hashes, List<String> names, List<Integer> characters,
                     List<String> sms_types, List<Long> timestamps) {
        this.numbers_hashes = numbers_hashes;
        this.names = names;
        this.characters = characters;
        this.sms_types = sms_types;
        this.timestamps = timestamps;
    }

    public String toString() {
        StringBuilder res = new StringBuilder("sms record:\n");
        res.append("number list: [ ");
        for (String n : numbers_hashes)
            res.append(n).append(" ");
        res.append("] name list: [ ");
        for (String na : names)
            res.append(na).append(" ");
        res.append("]");
        res.append("] characters list: [ ");
        for (int ch : characters)
            res.append(ch).append(" ");
        res.append("]");
        res.append("] type list: [ ");
        for (String ty : sms_types)
            res.append(ty).append(" ");
        res.append("]");
        res.append("] timestamp: [ ");
        for (Long tmp : timestamps)
            res.append(tmp).append(" ");
        res.append("]");
        return res.toString();
    }

}
