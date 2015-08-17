package com.example.dominik.wifi_analyzer;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.List;

/**
 * Created by Dominik on 8/9/2015.
 */
public class NetworkStatusAdapter extends ArrayAdapter<String[]>
{
    final static short SIZE_TAB = 5;
    final static short BSSID_TAB = 0;
    final static short SSID_TAB = 1;
    final static short CAPABILITIES_TAB = 2;
    final static short FREQUENCY_TAB = 3;
    final static short LEVEL_TAB = 4;

    private final Context context;
    private final List<String[]> values;

    public NetworkStatusAdapter(Context context, List<String[]> objects)
    {
        super(context, R.layout.network_status, objects);
        this.context = context;
        this.values = objects;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent)
    {
        LayoutInflater inflater = (LayoutInflater) getContext().
                getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        View view = inflater.inflate(R.layout.network_status, parent, false);

        ViewHolder viewHolder = new ViewHolder(view);
        view.setTag(viewHolder);

        setValuesToListView(position, viewHolder);

        return view;
    }

    private void setValuesToListView(int position, ViewHolder viewHolder)
    {
        for (int i = 0; i < this.values.size(); i++)
        {
            String[] tab = getItem(position);

            viewHolder.bssidView.setText(tab[BSSID_TAB]);
            viewHolder.ssidView.setText(tab[SSID_TAB]);
            viewHolder.capabilitiesView.setText(tab[CAPABILITIES_TAB]);
            viewHolder.frequencyView.setText(tab[FREQUENCY_TAB]);
            viewHolder.levelView.setText(tab[LEVEL_TAB]);
        }
    }

    public static class ViewHolder
    {

        public final TextView bssidView;
        public final TextView ssidView;
        public final TextView capabilitiesView;
        public final TextView frequencyView;
        public final TextView levelView;
        public final TextView timestampView;

        public ViewHolder(View view) {

            bssidView = (TextView) view.findViewById(R.id.bssid_textView);
            ssidView = (TextView) view.findViewById(R.id.ssid_textView);
            capabilitiesView = (TextView) view.findViewById(R.id.capabilities_textView);
            frequencyView = (TextView) view.findViewById(R.id.frequency_textView);
            levelView = (TextView) view.findViewById(R.id.level_textView);
            timestampView = (TextView) view.findViewById(R.id.timestamp_textView);

        }
    }
}