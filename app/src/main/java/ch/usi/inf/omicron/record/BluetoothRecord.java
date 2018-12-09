package ch.usi.inf.omicron.record;

import java.io.Serializable;
import java.util.List;

public class BluetoothRecord implements Record, Serializable {

    public List<String> address;
    public List<String> name;
    public List<Long> RSSI;
    public List<Long> timestamp;

    public BluetoothRecord() {

    }

    public BluetoothRecord(List<String> address,
                           List<String> name,
                           List<Long> rssi,
                           List<Long> timestamp) {

        this.address = address;
        this.name = name;
        this.RSSI = rssi;
        this.timestamp = timestamp;

    }

    public String toString() {

        StringBuilder res = new StringBuilder("Bluetooth record:\n");
        res.append("address: [ ");
        for (String ad : address)
            res.append(ad).append(" ");
        res.append("] name: [ ");
        for (String n : name)
            res.append(n).append(" ");
        res.append("] RSSI: [ ");
        for (Long rssi : RSSI)
            res.append(rssi).append(" ");
        res.append("] level: [ ");
        for (Long tmp : timestamp)
            res.append(tmp).append(" ");
        res.append("]");
        return res.toString();

    }

}
