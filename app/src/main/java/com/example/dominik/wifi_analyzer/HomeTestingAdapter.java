package com.example.dominik.wifi_analyzer;

import android.content.Context;
import android.content.res.Resources;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.List;

/**
 * Created by Dominik on 8/19/2015.
 */

//TODO implements adapter by objects

public class HomeTestingAdapter extends ArrayAdapter<String[]>
{
    final static short SIZE_TAB = 5;
    final static short ROOM_NAME_TAB = 0;
    final static short LINK_SPEED_TAB = 1;
    final static short RSSI_TAB = 2;
    final static short LAST_LINK_SPEED_TAB = 3;
    final static short LAST_RSSI_TAB = 4;


    private final Context context;
    private final List<String[]> values;

    public HomeTestingAdapter(Context context, List<String[]> objects)
    {
        super(context, R.layout.home_testing_listview, objects);
        this.context = context;
        this.values = objects;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent)
    {
        LayoutInflater inflater = (LayoutInflater) getContext().
                getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        View view = inflater.inflate(R.layout.home_testing_listview, parent, false);

        ViewHolder viewHolder = new ViewHolder(view);
        view.setTag(viewHolder);

        setValuesToListView(position, viewHolder, view);

        return view;
    }

    private void setValuesToListView(int position, ViewHolder viewHolder, View view)
    {
        for (int i = 0; i < this.values.size(); i++)
        {
            String[] tab = getItem(position);
            Resources res = context.getResources();

            String last_link_speed = String.format(res.getString(R.string.hta_last_to_udate), tab[LAST_LINK_SPEED_TAB]);
            String last_rssi = String.format(res.getString(R.string.hta_last_to_udate),tab[LAST_RSSI_TAB]);

            String link_speed = String.format(res.getString(R.string.hta_now_to_udate), tab[LINK_SPEED_TAB]);
            String rssi = String.format(res.getString(R.string.hta_now_to_udate), tab[RSSI_TAB]);

            viewHolder.roomNameView.setText(tab[ROOM_NAME_TAB]);
            viewHolder.linkSpeedView.setText(link_speed);
            viewHolder.rssiView.setText(rssi);
            viewHolder.lastLinkSpeedView.setText(last_link_speed);
            viewHolder.lastRssiView.setText(last_rssi);
        }
    }

    public static class ViewHolder
    {
        public final TextView roomNameView;
        public final TextView linkSpeedView;
        public final TextView rssiView;
        public final TextView lastLinkSpeedView;
        public final TextView lastRssiView;

        public ViewHolder(View view)
        {
            roomNameView = (TextView) view.findViewById(R.id.room_name_textView);
            linkSpeedView = (TextView) view.findViewById(R.id.link_speed_now_textView);
            rssiView = (TextView) view.findViewById(R.id.rssi_now_textView);
            lastLinkSpeedView = (TextView) view.findViewById(R.id.link_speed_last_textView);
            lastRssiView = (TextView) view.findViewById(R.id.rssi_last_textView);
        }
    }
}
