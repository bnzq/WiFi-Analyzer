package com.example.dominik.wifi_analyzer;

import android.content.Context;
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
    final static short SIZE_TAB = 3;
    final static short ROOM_NAME_TAB = 0;
    final static short LINK_SPEED_TAB = 1;
    final static short RSSI_TAB = 2;

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

        setValuesToListView(position, viewHolder);

        return view;
    }

    private void setValuesToListView(int position, ViewHolder viewHolder)
    {
        for (int i = 0; i < this.values.size(); i++)
        {
            String[] tab = getItem(position);

            viewHolder.roomNameView.setText(tab[ROOM_NAME_TAB]);
            viewHolder.linkSpeedView.setText(tab[LINK_SPEED_TAB]);
            viewHolder.rssiView.setText(tab[RSSI_TAB]);
        }
    }

    public static class ViewHolder
    {
        public final TextView roomNameView;
        public final TextView linkSpeedView;
        public final TextView rssiView;

        public ViewHolder(View view)
        {
            roomNameView = (TextView) view.findViewById(R.id.room_name_textView);
            linkSpeedView = (TextView) view.findViewById(R.id.link_speed_textView);
            rssiView = (TextView) view.findViewById(R.id.rssi_textView);
        }
    }
}
