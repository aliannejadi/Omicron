package ch.usi.inf.omicron.record;

import java.io.Serializable;
import java.util.List;

public class UsageRecord implements Record, Serializable {

    public List<String> appList;
    public List<Long> usageList;
    public List<String> eventsAppName;
    public List<Integer> eventType;
    public List<Long> eventTimestamp;
    public List<String> installationAppName;
    public List<String> installationEvent;
    public List<Long> installationTimestamp;
    public long timestamp;

    public UsageRecord() {

    }

    public UsageRecord(List<String> appList,
                       List<Long> usageList,
                       long timestamp,
                       List<String> eventsAppName,
                       List<Integer> eventType,
                       List<Long> eventTimestamp,
                       List<String> installationName,
                       List<String> installationEvent,
                       List<Long> installationTimestamp) {

        this.appList = appList;
        this.usageList = usageList;
        this.eventsAppName = eventsAppName;
        this.eventType = eventType;
        this.eventTimestamp = eventTimestamp;
        this.timestamp = timestamp;
        this.installationAppName = installationName;
        this.installationEvent = installationEvent;
        this.installationTimestamp = installationTimestamp;

    }

    public String toString() {
        StringBuilder res = new StringBuilder("Usage record:\n");
        res.append("app list: [ ");
        for (String app : appList)
            res.append(app).append(" ");
        res.append("] usage list: [ ");
        for (Long usage : usageList)
            res.append(usage).append(" ");
        res.append("] timestamp: ").append(timestamp);
        res.append(" event apps: [ ");
        for (String eApp : eventsAppName)
            res.append(eApp).append(" ");
        res.append("] event types: [ ");
        for (Integer eType : eventType)
            res.append(eType).append(" ");
        res.append("] event timestamps: [ ");
        for (Integer eTimestamp : eventType)
            res.append(eTimestamp).append(" ");
        res.append("]");
        return res.toString();
    }

}
