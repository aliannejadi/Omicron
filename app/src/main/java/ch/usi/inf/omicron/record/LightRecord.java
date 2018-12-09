package ch.usi.inf.omicron.record;

import java.io.Serializable;
import java.util.List;

public class LightRecord implements Record, Serializable {

    public List<Float> lumList;
    public List<Long> timestamp;

    public LightRecord(List<Float> lumList,
                       List<Long> timestamp) {

        this.lumList = lumList;
        this.timestamp = timestamp;

    }

    public String toString() {

        StringBuilder res = new StringBuilder("Light record:\n");
        res.append("lumList: [ ");
        for (Float l : lumList)
            res.append(l).append(" ");
        res.append("] timestamp: [ ");
        for (Long tmp : timestamp)
            res.append(tmp).append(" ");
        res.append("]");
        return res.toString();

    }

}
