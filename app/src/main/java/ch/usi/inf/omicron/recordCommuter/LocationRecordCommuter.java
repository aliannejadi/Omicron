package ch.usi.inf.omicron.recordCommuter;

import android.location.Location;

import ch.usi.inf.omicron.record.LocationRecord;

import static ch.usi.inf.omicron.UMob.tm;

public class LocationRecordCommuter extends RecordCommuter {

    // log tag
    private static String LCACHE = "umob.Rec.cache.location";

    // constructor automatically converts the passed object to POJO
    public LocationRecordCommuter(Location location) {

        super("location");

        record = new LocationRecord(
                location.getLatitude(),
                location.getLongitude(),
                location.getAccuracy(),
                location.getSpeed(),
                location.getBearing(),
                location.getProvider(),
                System.currentTimeMillis(),
                tm.getActiveTaskId());
    }

    // display commuter in a readable format
    public String toString() {
        String res = "LocationCommuter " + key + "\n";
        res += "holding " + record.toString();
        return res;
    }

}
