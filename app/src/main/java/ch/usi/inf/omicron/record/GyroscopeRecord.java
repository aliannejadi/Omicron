package ch.usi.inf.omicron.record;

import java.io.Serializable;
import java.util.List;

public class GyroscopeRecord implements Record, Serializable {

    public List<Float> x;
    public List<Float> y;
    public List<Float> z;
    public List<Long> timestamp;

    public GyroscopeRecord() {

    }

    public GyroscopeRecord(List<Float> x,
                           List<Float> y,
                           List<Float> z,
                           List<Long> timestamp) {

        this.x = x;
        this.y = y;
        this.z = z;
        this.timestamp = timestamp;

    }

    public String toString() {

        StringBuilder res = new StringBuilder("Gyroscope record:\n");
        res.append("x: [ ");
        for (Float c : x)
            res.append(c).append(" ");
        res.append("] y: [ ");
        for (Float c : y)
            res.append(c).append(" ");
        res.append("] z: [ ");
        for (Float c : z)
            res.append(c).append(" ");
        res.append("] timestamp: [ ");
        for (Long tmp : timestamp)
            res.append(tmp).append(" ");
        res.append("]");
        return res.toString();

    }

}
