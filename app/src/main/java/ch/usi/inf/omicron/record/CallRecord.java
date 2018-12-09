package ch.usi.inf.omicron.record;

import java.io.Serializable;
import java.util.List;

public class CallRecord implements Record, Serializable {
    public List<String> numbers_hashes;
    public List<String> names;
    //duration in seconds
    public List<Integer> durations;
    public List<String> call_types;
    public List<Long> timestamps;

    public CallRecord(List<String> numbers_hashes, List<String> names, List<Integer> durations,
                      List<String> call_types, List<Long> timestamps) {
        this.numbers_hashes = numbers_hashes;
        this.names = names;
        this.durations = durations;
        this.call_types = call_types;
        this.timestamps = timestamps;
    }

    public String toString() {
        StringBuilder res = new StringBuilder("call record:\n");
        res.append("number list: [ ");
        for (String n : numbers_hashes)
            res.append(n).append(" ");
        res.append("] name list: [ ");
        for (String na : names)
            res.append(na).append(" ");
        res.append("]");
        res.append("] durations list: [ ");
        for (int ch : durations)
            res.append(ch).append(" ");
        res.append("]");
        res.append("] type list: [ ");
        for (String ty : call_types)
            res.append(ty).append(" ");
        res.append("]");
        res.append("] timestamp: [ ");
        for (Long tmp : timestamps)
            res.append(tmp).append(" ");
        res.append("]");
        return res.toString();
    }

}
