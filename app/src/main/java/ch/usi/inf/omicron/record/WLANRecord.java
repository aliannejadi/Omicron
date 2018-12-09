package ch.usi.inf.omicron.record;

import java.io.Serializable;
import java.util.List;

public class WLANRecord implements Record, Serializable {

    public List<String> BSSID;
    public List<String> SSID;
    public List<Long> frequency;
    public List<Long> level;
    public List<Long> timestamp;

    public WLANRecord(List<String> bssid,
                      List<String> ssid,
                      List<Long> frequency,
                      List<Long> level,
                      List<Long> timestamp) {

        this.BSSID = bssid;
        this.SSID = ssid;
        this.frequency = frequency;
        this.level = level;
        this.timestamp = timestamp;
    }

    public String toString() {
        StringBuilder res = new StringBuilder("WLAN record:\n");
        res.append("BSSID: [ ");
        for (String bssid : BSSID)
            res.append(bssid).append(" ");
        res.append("] SSID: [ ");
        for (String ssid : SSID)
            res.append(ssid).append(" ");
        res.append("] frequency: [ ");
        for (Long fr : frequency)
            res.append(fr).append(" ");
        res.append("] level: [ ");
        for (Long lv : level)
            res.append(lv).append(" ");
        res.append("] timestamp: [ ");
        for (Long tmp : timestamp)
            res.append(tmp).append(" ");
        res.append("]");
        return res.toString();
    }

}
