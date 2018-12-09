package ch.usi.inf.omicron.queryRendering;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;

import ch.usi.inf.omicron.Log;
import ch.usi.inf.omicron.R;

import static ch.usi.inf.omicron.MainActivity.relevantResultIndeces;
import static ch.usi.inf.omicron.MainActivity.relevantResultLinks;
import static ch.usi.inf.omicron.MainActivity.relevantResultTaskID;
import static ch.usi.inf.omicron.MainActivity.relevantResultTimestamps;
import static ch.usi.inf.omicron.MainActivity.relevantResultTitles;
import static ch.usi.inf.omicron.UMob.tm;

public class QueryResultAdapter extends BaseAdapter {

    // log tags
    private static String ADAPTER = "umob.Adapter.query";
    private static LayoutInflater inflater = null;
    public Context context;
    private QueryResult[] data;

    public QueryResultAdapter(Context context, QueryResult[] data) {

        this.context = context;
        this.data = data;
        inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

    }

    @Override
    public int getCount() {
        return data.length;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {

        View vi = convertView;
        if (vi == null)
            vi = inflater.inflate(R.layout.search_result_item, null);

        final String title = data[position].getTitle();
        TextView titleField = vi.findViewById(R.id.resultTitle);
        titleField.setText(title);

        final String link = data[position].getLink();
        TextView linkField = vi.findViewById(R.id.resultLink);
        linkField.setText(link);

        final String description = data[position].getDescription();
        TextView descriptionField = vi.findViewById(R.id.resultDescription);
        descriptionField.setText(description);

        CheckBox cb = vi.findViewById(R.id.checkBox);
        //preserve links checked for query modification!
        cb.setChecked(relevantResultLinks.contains(link));

        cb.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

                Log.i(ADAPTER, "clicked on checkbox " + position + " of result " + title);

                if (isChecked) {

                    Log.i(ADAPTER, "adding relevant result: ");
                    Log.i(ADAPTER, "index: " + position);
                    Log.i(ADAPTER, "title: " + title);
                    Log.i(ADAPTER, "link: " + link);

                    relevantResultTimestamps.add(System.currentTimeMillis());
                    relevantResultIndeces.add(position);
                    relevantResultTitles.add(title);
                    relevantResultLinks.add(link);
                    relevantResultTaskID.add(tm.getActiveTaskId());

                } else {

                    Log.i(ADAPTER, "removing relevant result: ");
                    Log.i(ADAPTER, "index: " + position);
                    Log.i(ADAPTER, "title: " + title);
                    Log.i(ADAPTER, "link: " + link);

                    int index = relevantResultTitles.indexOf(title);

                    if (index >= 0) {

                        relevantResultTimestamps.remove(index);
                        relevantResultTitles.remove(index);
                        relevantResultIndeces.remove(index);
                        relevantResultLinks.remove(index);
                        relevantResultTaskID.remove(index);
                    }

                }

            }

        });
        return vi;

    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public Object getItem(int position) {
        return data[position];
    }

    @Override

    public int getViewTypeCount() {

        return getCount();
    }

    @Override
    public int getItemViewType(int position) {

        return position;
    }

}
