package com.example.dominik.wifi_analyzer;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.util.List;

public class NetworkStatusAdapter extends ArrayAdapter<String[]>
{
    final static short SIZE_TAB = 6;
    final static short BSSID_TAB = 0;
    final static short SSID_TAB = 1;
    final static short CAPABILITIES_TAB = 2;
    final static short FREQUENCY_TAB = 3;
    final static short LEVEL_TAB = 4;
    final static short IS_CONNECTED = 5;

    private final Context context;
    private final List<String[]> values;

    public NetworkStatusAdapter(Context context, List<String[]> objects)
    {
        super(context, R.layout.network_status_listview, objects);
        this.context = context;
        this.values = objects;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent)
    {
        View view = null;
        //TODO check that !
//
//        if(convertView != null)
//        {
            LayoutInflater inflater = (LayoutInflater) getContext().
                    getSystemService(Context.LAYOUT_INFLATER_SERVICE);

            view = inflater.inflate(R.layout.network_status_listview, parent, false);

            ViewHolder viewHolder = new ViewHolder(view);

            setValuesToListView(position, viewHolder);
//        }
//
        return view;
    }

    private void setValuesToListView(int position, ViewHolder viewHolder)
    {

        for (int i = 0; i < this.values.size(); i++)
        {
            String[] tab = getItem(position);

            viewHolder.ssidView.setText(String.format(
                    context.getString(R.string.two_strings_textView), tab[SSID_TAB], tab[BSSID_TAB]
            ));
            viewHolder.channelView.setText(Integer.toString(
                        Utility.convertFrequencyToChannel(Integer.valueOf(tab[FREQUENCY_TAB]))
            ));
            viewHolder.levelView.setText(tab[LEVEL_TAB]);
            viewHolder.frequencyView.setText(tab[FREQUENCY_TAB]);
            viewHolder.capabilitiesView.setText(String.format(
                    context.getResources().getString(R.string.ns_capabilities_textView), Utility.getEncryptionFromCapabilities(tab[CAPABILITIES_TAB])
            ));

            int quality = Utility.convertRssiToQuality(Integer.valueOf(tab[LEVEL_TAB]));

            viewHolder.progressBar.setMax(100);
            viewHolder.progressBar.setProgress(quality);
            viewHolder.strengthProgressBarView.setText(String.format(
                    context.getResources().getString(R.string.percent_textView), quality
            ));

            if(tab[IS_CONNECTED].equals("1"))
            {
                viewHolder.ssidView.setTextColor(getContext().getResources().getColor(R.color.dark_yellow));
            }

            if(Utility.convertQualityToStepsQuality(quality,5) == 1)
            {
                viewHolder.imageView.setImageResource(R.mipmap.wireless_0);
            }
            else if (Utility.convertQualityToStepsQuality(quality,5) == 2)
            {
                viewHolder.imageView.setImageResource(R.mipmap.wireless_1);
            }
            else if (Utility.convertQualityToStepsQuality(quality,5) == 3)
            {
                viewHolder.imageView.setImageResource(R.mipmap.wireless_2);
            }
            else if (Utility.convertQualityToStepsQuality(quality,5) == 4)
            {
                viewHolder.imageView.setImageResource(R.mipmap.wireless_3);
            }
            else if (Utility.convertQualityToStepsQuality(quality,5) == 5)
            {
                viewHolder.imageView.setImageResource(R.mipmap.wireless_4);
            }
        }
    }

    public static class ViewHolder
    {
        public final TextView ssidView;
        public final TextView capabilitiesView;
        public final TextView frequencyView;
        public final TextView levelView;
        public final TextView channelView;
        public final TextView strengthProgressBarView;
        public final ImageView imageView;
        public final ProgressBar progressBar;

        public ViewHolder(View view)
        {
            ssidView = (TextView) view.findViewById(R.id.ssid__bssid_textView);
            capabilitiesView = (TextView) view.findViewById(R.id.capabilities_textView);
            frequencyView = (TextView) view.findViewById(R.id.frequency_textView);
            levelView = (TextView) view.findViewById(R.id.strength_percent_textView);
            channelView = (TextView) view.findViewById(R.id.channel_textView);
            strengthProgressBarView = (TextView) view.findViewById(R.id.strength_percent_progressbar_textView);

            imageView = (ImageView) view.findViewById(R.id.ns_wifi_strength_imageview);

            progressBar =  (ProgressBar) view.findViewById(R.id.ns_quality_progressbar);
        }
    }
}