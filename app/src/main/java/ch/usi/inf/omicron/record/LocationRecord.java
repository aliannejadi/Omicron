package ch.usi.inf.omicron.record;

import java.io.Serializable;

public class LocationRecord implements Record, Serializable {

    public double latitude;
    public double longitude;
    public double accuracy;
    public double speed;
    public double bearing;
    public String provider;
    public long timestamp;
    public String taskid;

    public LocationRecord() {

    }

    public LocationRecord(double latitude, double longitude, double accuracy, double speed, double bearing, String provider, long timestamp, String taskid) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.accuracy = accuracy;
        this.speed = speed;
        this.bearing = bearing;
        this.provider = provider;
        this.timestamp = timestamp;
        this.taskid = taskid;
    }

    public String toString() {
        String res = "Location record:\n";
        res += "latitude: " + latitude;
        res += "longitude: " + longitude;
        res += "accuracy: " + accuracy;
        res += "speed: " + speed;
        res += "bearing: " + bearing;
        res += "provider: " + provider;
        res += "timestamp: " + timestamp;
        res += "taskid: " + taskid;
        return res;
    }

}
