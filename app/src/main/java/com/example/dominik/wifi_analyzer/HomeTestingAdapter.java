package com.example.dominik.wifi_analyzer;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.util.List;

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
        View view = null;

        if(convertView == null)
        {
            LayoutInflater inflater = (LayoutInflater) getContext().
                    getSystemService(Context.LAYOUT_INFLATER_SERVICE);

            view = inflater.inflate(R.layout.home_testing_listview, parent, false);

            ViewHolder viewHolder = new ViewHolder(view);

            setValuesToListView(position, viewHolder);
        }

        return view;
    }

    private void setValuesToListView(int position, ViewHolder viewHolder)
    {
        for (int i = 0; i < this.values.size(); i++)
        {
            String[] tab = getItem(position);

            int qualityLast = Utility.convertRssiToQuality(Float.valueOf(tab[LAST_RSSI_TAB]).intValue());
            int qualityNow = Utility.convertRssiToQuality(Float.valueOf(tab[RSSI_TAB]).intValue());

            viewHolder.roomNameView.setText(tab[ROOM_NAME_TAB]);

            viewHolder.progressBarQualityNow.setProgress(qualityNow);
            viewHolder.progressBarQualityLast.setProgress(qualityLast);
            viewHolder.progressBarQualityNowView.setText(String.format(
                    context.getResources().getString(R.string.percent_textView), qualityNow
            ));
            viewHolder.progressBarQualityLastView.setText(String.format(
                    context.getResources().getString(R.string.percent_textView), qualityLast
            ));

            viewHolder.progressBarSpeedNow.setProgress(Float.valueOf(tab[LINK_SPEED_TAB]).intValue());
            viewHolder.progressBarSpeedLast.setProgress(Float.valueOf(tab[LAST_LINK_SPEED_TAB]).intValue());
            viewHolder.progressBarSpeedNowView.setText(String.format(
                    context.getResources().getString(R.string.Mbps_textView), Float.valueOf(tab[LINK_SPEED_TAB]).intValue()));
            viewHolder.progressBarSpeedLastView.setText(String.format(
                    context.getResources().getString(R.string.Mbps_textView), Float.valueOf(tab[LAST_LINK_SPEED_TAB]).intValue()));
        }

    }

    public static class ViewHolder
    {
        public final TextView roomNameView;

        public final ProgressBar progressBarSpeedLast;
        public final ProgressBar progressBarSpeedNow;
        public final ProgressBar progressBarQualityLast;
        public final ProgressBar progressBarQualityNow;

        public final TextView progressBarQualityNowView;
        public final TextView progressBarQualityLastView;
        public final TextView progressBarSpeedNowView;
        public final TextView progressBarSpeedLastView;

        public ViewHolder(View view)
        {
            roomNameView = (TextView) view.findViewById(R.id.room_name_textView);

            progressBarQualityNow = (ProgressBar) view.findViewById(R.id.ht_quality_progressbar_now);
            progressBarQualityLast = (ProgressBar) view.findViewById(R.id.ht_quality_progressbar_last);
            progressBarSpeedNow = (ProgressBar) view.findViewById(R.id.ht_speed_progressbar_now);
            progressBarSpeedLast = (ProgressBar) view.findViewById(R.id.ht_speed_progressbar_last);

            progressBarQualityNowView = (TextView) view.findViewById(R.id.ht_strength_percent_progressbar_now_textView);
            progressBarQualityLastView = (TextView) view.findViewById(R.id.ht_strength_percent_progressbar_last_textView);
            progressBarSpeedNowView = (TextView) view.findViewById(R.id.ht_speed_percent_progressbar_now_textView3);
            progressBarSpeedLastView = (TextView) view.findViewById(R.id.ht_speed_percent_progressbar_last_textView);

            progressBarQualityNow.setMax(100);
            progressBarQualityLast.setMax(100);
            progressBarSpeedNow.setMax(54);
            progressBarSpeedLast.setMax(54);
        }
    }
}
