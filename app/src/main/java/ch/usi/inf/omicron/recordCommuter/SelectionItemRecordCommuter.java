package ch.usi.inf.omicron.recordCommuter;

import ch.usi.inf.omicron.record.SelectionItemRecord;

public class SelectionItemRecordCommuter extends RecordCommuter {

    // constructor automatically converts the passed object to POJO
    public SelectionItemRecordCommuter(String title,
                                       String link,
                                       String description,
                                       long timestamp, String task_id) {

        super("selection_item");

        record = new SelectionItemRecord(title, link, description, timestamp, task_id);

    }

    // display commuter in a readable format
    public String toString() {
        String res = "SelectionItemCommuter " + key + "\n";
        res += "holding " + record.toString();
        return res;
    }

}
