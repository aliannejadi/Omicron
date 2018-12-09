package ch.usi.inf.omicron.record;

import java.io.Serializable;
import java.util.List;

public class CellRecord implements Record, Serializable {


    public List<String> protocol;
    public List<Long> MCC;
    public List<Long> MNC;
    public List<Long> LAC;
    public List<Long> CID;
    public List<Long> timestamp;

    public CellRecord() {

    }

    public CellRecord(List<String> protocol,
                      List<Long> mcc,
                      List<Long> mnc,
                      List<Long> lac,
                      List<Long> cid,
                      List<Long> timestamp) {

        this.protocol = protocol;
        this.MCC = mcc;
        this.MNC = mnc;
        this.LAC = lac;
        this.CID = cid;
        this.timestamp = timestamp;

    }

    public String toString() {
        StringBuilder res = new StringBuilder("cell record:\n");
        res.append("protocol: [ ");
        for (String p : protocol)
            res.append(p).append(" ");
        res.append("] MCC: [ ");
        for (Long mcc : MCC)
            res.append(mcc).append(" ");
        res.append("] MNC: [ ");
        for (Long mnc : MNC)
            res.append(mnc).append(" ");
        res.append("] LAC: [ ");
        for (Long lac : LAC)
            res.append(lac).append(" ");
        res.append("] CID: [ ");
        for (Long cid : CID)
            res.append(cid).append(" ");
        res.append("] timestamp: [ ");
        for (Long tmp : timestamp)
            res.append(tmp).append(" ");
        res.append("]");
        return res.toString();
    }
}
