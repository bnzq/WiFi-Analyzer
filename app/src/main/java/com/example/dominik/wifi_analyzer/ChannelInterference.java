package com.example.dominik.wifi_analyzer;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.graphics.Color;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.achartengine.ChartFactory;
import org.achartengine.GraphicalView;
import org.achartengine.chart.PointStyle;
import org.achartengine.model.XYMultipleSeriesDataset;
import org.achartengine.model.XYSeries;
import org.achartengine.renderer.XYMultipleSeriesRenderer;
import org.achartengine.renderer.XYSeriesRenderer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class ChannelInterference extends Fragment
{
    @SuppressWarnings("unused")
    public static final String LOG_TAG = ChannelInterference.class.getSimpleName();

    final static short FREQUENCY_CONNECTED_CHANNEL = 0;
    final static short NETWORKS_ON_CONNECTED_CHANNEL = 1;
    final static short SSID_CONNECTED_NETWORK = 2;

    final private static short sNumberOfChannels = 13;
    final private static short sFirstChannel = 1;
    final private static short sLastChannel = sNumberOfChannels;

    final private static short sAllChannels = sNumberOfChannels + 4;
    final private static short s1stFactor = 80;
    final private static short s2ndFactor = 72;
    final private static short s3rdFactor = 64;
    final private static float sDBmFactor = 0.2f;
    final private static short sDBm2ndFactor = 20;
    final private static short sDBm3rdFactor = 40;

    final private static String sNN = "N/N";

    private ViewHolder viewHolder;
    private WifiScanReceiver mWifiReceiver;
    private WifiManager mWifiManager;
    private HashMap<String, Integer> mSsidColorMap = new HashMap<>();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.channel_interference_tab, container, false);

        viewHolder = new ViewHolder(rootView);

        update();

        return rootView;
    }

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        mWifiManager =(WifiManager)getActivity().getSystemService(Context.WIFI_SERVICE);
        mWifiReceiver = new WifiScanReceiver();

        Utility.enableWifi(mWifiManager);

        mWifiManager.startScan();

        super.onCreate(savedInstanceState);
    }

    @Override
    public void onPause()
    {
        getActivity().unregisterReceiver(mWifiReceiver);
        super.onPause();
    }

    @Override
    public void onResume()
    {
        getActivity().registerReceiver(mWifiReceiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
        super.onResume();
    }

    //update all items in gui
    private void update()
    {
        Utility.enableWifi(mWifiManager);
        mWifiManager.startScan();

        List<ScanResult> wifiScanList = mWifiManager.getScanResults();

        if(wifiScanList == null)
        {
            return;
        }

        WifiInfo wifiInfo = mWifiManager.getConnectionInfo();

        HashMap<Short, String> connectionInfo =  getInfoAboutCurrentConnection(wifiInfo, wifiScanList);

        updateChart(wifiScanList, connectionInfo);

        updateValuationList(wifiScanList, connectionInfo);

        if(wifiInfo.getBSSID() == null)
            return;

        updateInfoBar(Integer.valueOf(connectionInfo.get(NETWORKS_ON_CONNECTED_CHANNEL)),
                Integer.valueOf(connectionInfo.get(FREQUENCY_CONNECTED_CHANNEL)),
                connectionInfo.get(SSID_CONNECTED_NETWORK));
    }

    //draw graph with networks
    private void updateChart(List<ScanResult> wifiScanList, HashMap<Short, String> connectionInfo)
    {
        WifiChart wifiChart = new WifiChart(wifiScanList, sNumberOfChannels);
        wifiChart.init();
        if(connectionInfo.get(SSID_CONNECTED_NETWORK) != null)
            wifiChart.setValues(connectionInfo.get(SSID_CONNECTED_NETWORK));
        else
            wifiChart.setValues("tmp");

        viewHolder.mChartChannels.addView(wifiChart.getmChartView(), 0);
    }

    //update info in bar that don't need valuation
    private void updateInfoBar(int size, int freq , String ssid)
    {
        Resources resources = getResources();

        viewHolder.connectedView.setText(String.format(resources.getString(R.string.connected_bar), ssid));
        viewHolder.channelView.setText(String.format(resources.getString(R.string.ci_channel_bar), Utility.convertFrequencyToChannel(freq)));
        viewHolder.networksOnThisChannelView.setText(String.format(resources.getString(R.string.ci_number_of_networks_on_this_channel_bar), size));
    }

    //draw and find best channels
    private void updateValuationList(List<ScanResult> wifiScanList, HashMap<Short, String> connectionInfo)
    {
        List<String[]> list = new ArrayList<>();

        int[] networksOnChannel = getNumberOfNetworksOnChannels(wifiScanList);
        int[] valuation_percent = valuateChannels(wifiScanList);
        int[] valuation_percent_recommended = valuateChannelsWithoutConnected(wifiScanList);

        for (int i = 0; i < sNumberOfChannels; i++)
        {
            String [] wifiDetails = new String[ValuationChannelAdapter.SIZE_TAB];

            wifiDetails[ValuationChannelAdapter.CHANNEL_TAB] = String.valueOf(i + 1);
            wifiDetails[ValuationChannelAdapter.VALUATION_PERCENT_TAB] = String.valueOf(valuation_percent[i + 2]);
            wifiDetails[ValuationChannelAdapter.NETWORKS_ON_THIS_CHANNEL] = String.valueOf(networksOnChannel[i]);
            wifiDetails[ValuationChannelAdapter.VALUATION_PERCENT_RECOMMENDED_TAB] = String.valueOf(valuation_percent_recommended[i]);

            list.add(wifiDetails);
        }

        int recommendedChannels[] = getRecommendedChannels(valuation_percent_recommended);
        viewHolder.recommendedView.setText(String.format(getResources().getString(R.string.ci_recommended_bar),
                recommendedChannels[0], recommendedChannels[1], recommendedChannels[2]
        ));

        ValuationChannelAdapter mValuationChannelAdapter = new ValuationChannelAdapter(getActivity(), list);
        viewHolder.listView.setAdapter(mValuationChannelAdapter);

        if (connectionInfo.get(SSID_CONNECTED_NETWORK).equals(sNN))
            return;

        int index = Utility.convertFrequencyToChannel(Integer.valueOf(connectionInfo.get(FREQUENCY_CONNECTED_CHANNEL))) - 1;
        Log.d("tak", "updateValuationList: " + index + " / " + valuation_percent_recommended.length);
        if (index < valuation_percent_recommended.length && index > 0) {
            viewHolder.valuationProgressBarView.setText(String.format(getResources().getString(R.string.valuation), valuation_percent_recommended[index] / 10));

            viewHolder.valuationProgressBar.setProgress(valuation_percent_recommended[
                    Utility.convertFrequencyToChannel(Integer.valueOf(connectionInfo.get(FREQUENCY_CONNECTED_CHANNEL))) - 1]
            );
        }
    }

    //vevaluation of all bandwidths on the basis of available networks
    private int[] valuateChannels(List<ScanResult> wifiScanList)
    {
        int[] result = new int[sAllChannels];

        for (int i = 0; i < sAllChannels; i++)
        {
            result[i] = 500;
        }

        for (int i = 0; i < wifiScanList.size(); i++)
        {
            int channel = Utility.convertFrequencyToChannel(wifiScanList.get(i).frequency);

            if( channel  < sFirstChannel || channel > sLastChannel)
                continue;

            int dBm = wifiScanList.get(i).level;

            result[channel + 1] -= (s1stFactor + sDBmFactor * Utility.convertRssiToQualityWithSub(dBm, 0));
            result[channel] -= (s2ndFactor + sDBmFactor * Utility.convertRssiToQualityWithSub(dBm, sDBm2ndFactor));
            result[channel - 1] -= (s3rdFactor + sDBmFactor * Utility.convertRssiToQualityWithSub(dBm, sDBm3rdFactor));
            result[channel + 2] -= (s2ndFactor + sDBmFactor * Utility.convertRssiToQualityWithSub(dBm, sDBm2ndFactor));
            result[channel + 3] -= (s3rdFactor + sDBmFactor * Utility.convertRssiToQualityWithSub(dBm, sDBm3rdFactor));
        }

        for (int i = 0; i < sAllChannels; i++)
        {
            result[i] /= 5;

            if(result[i] < 0)
                result[i] = 0;
        }

        return result;
    }

    //evaluation in way that connected network is not taken into account
    private int[] valuateChannelsWithoutConnected(List<ScanResult> wifiScanList)
    {
        int[] result = new int[sAllChannels];

        for (int i = 0; i < sAllChannels; i++)
        {
            result[i] = 500;
        }

        WifiInfo wifiInfo = mWifiManager.getConnectionInfo();
        String bssid = wifiInfo.getBSSID();
        if(bssid == null)
            bssid = "tmp";

        for (int i = 0; i < wifiScanList.size(); i++)
        {
            if(!wifiScanList.get(i).BSSID.equals(bssid))
            {
                int channel = Utility.convertFrequencyToChannel(wifiScanList.get(i).frequency);
                int dBm = wifiScanList.get(i).level;

                if( channel  < sFirstChannel || channel > sLastChannel)
                    continue;

                result[channel + 1] -= (s1stFactor + sDBmFactor * Utility.convertRssiToQualityWithSub(dBm, 0));
                result[channel] -= (s2ndFactor + sDBmFactor * Utility.convertRssiToQualityWithSub(dBm, sDBm2ndFactor));
                result[channel - 1] -= (s3rdFactor + sDBmFactor * Utility.convertRssiToQualityWithSub(dBm, sDBm3rdFactor));
                result[channel + 2] -= (s2ndFactor + sDBmFactor * Utility.convertRssiToQualityWithSub(dBm, sDBm2ndFactor));
                result[channel + 3] -= (s3rdFactor + sDBmFactor * Utility.convertRssiToQualityWithSub(dBm, sDBm3rdFactor));
            }
        }

        for (int i = 0; i < sAllChannels; i++)
        {
            result[i] /= 5;

            if(result[i] < 0)
                result[i] = 0;
        }

        int averages[] = new int[sNumberOfChannels];

        for (int i = 2; i < sAllChannels - 2; i++)
        {
            int sum = result[i - 2] + result[i - 2] + result[i] + result[i + 1] + result[i + 2];
            averages[i - 2] = sum / 5;
        }

        return averages;
    }

    //get numbers of networks on each channel
    private int[] getNumberOfNetworksOnChannels(List<ScanResult> wifiScanList)
    {
        int[] result = new int[sNumberOfChannels];

        for (int i = 0; i < sNumberOfChannels; i++)
        {
            result[i] = 0;
        }

        for (int i = 0; i < wifiScanList.size(); i++)
        {
            int res = Utility.convertFrequencyToChannel(wifiScanList.get(i).frequency) - 1;


            if( res  < sFirstChannel - 1 || res > sLastChannel - 1)
                continue;

            result[ res ] += 1;
        }

        return result;
    }

    //get three best channels on the basis of valuateChannelsWithoutConnected
    private int [] getRecommendedChannels(int [] ints)
    {
        int best1 = 0, best2 = 0, best3 = 0;
        int ch1 = 0, ch2 = 0, ch3 = 0;

        for (int i = 0; i < ints.length; i++)
        {
            if(best1 < ints[i])
            {
                best3 = best2;
                best2 = best1;
                best1 = ints[i];
                ch3 = ch2;
                ch2 = ch1;
                ch1 = i;
            }
            else if (best2 < ints[i])
            {
                best3 = best2;
                best2 = ints[i];
                ch3 = ch2;
                ch2 = i;
            }
            else if (best3 < ints[i])
            {
                best3 = ints[i];
                ch3 = i;
            }
        }

        int [] rec = new int[3];

        rec[0] = ch1 + 1;
        rec[1] = ch2 + 1;
        rec[2] = ch3 + 1;

        return rec;
    }

    //get parameters about conneced network
    private HashMap<Short, String> getInfoAboutCurrentConnection(WifiInfo wifiInfo, List<ScanResult> wifiScanList)
    {
        HashMap<Short, String> hashMap = new HashMap<>();

        int onConnectedChannel = 0;
        int frequency = 0;

        String bssid = wifiInfo.getBSSID();
        if(bssid == null)
            bssid = sNN;

        for(int i = 0; i < wifiScanList.size(); i++)
        {
            if (bssid.equals(wifiScanList.get(i).BSSID)) {
                frequency = wifiScanList.get(i).frequency;
            }
        }

        for(int i = 0; i < wifiScanList.size(); i++)
        {
            if (frequency == wifiScanList.get(i).frequency)
            {
                ++onConnectedChannel;
            }
        }

        hashMap.put(FREQUENCY_CONNECTED_CHANNEL, String.valueOf(frequency));
        hashMap.put(NETWORKS_ON_CONNECTED_CHANNEL, String.valueOf(onConnectedChannel));
        if(bssid.equals(sNN))
            hashMap.put(SSID_CONNECTED_NETWORK, bssid);
        else
            hashMap.put(SSID_CONNECTED_NETWORK, wifiInfo.getSSID());

        return hashMap;
    }

    private class WifiChart
    {
        private final String LABEL_X = getString(R.string.ci_labelx);
        private final String LABEL_Y = getString(R.string.ci_labely);
        private final List<ScanResult> mWifiScanList;
        private final short numberOfChannels;
        private XYMultipleSeriesDataset mDataset;
        private XYMultipleSeriesRenderer mRenderer;
        private GraphicalView mChartView;

        public WifiChart(List<ScanResult> wifiScanList, short numberOfChannels)
        {
            mDataset = new XYMultipleSeriesDataset();
            mRenderer = new XYMultipleSeriesRenderer();
            this.mWifiScanList = wifiScanList;
            this.numberOfChannels = numberOfChannels;

            mChartView =  ChartFactory.getLineChartView(getActivity(), mDataset, mRenderer);
        }

        public void init()
        {
            mRenderer.setMarginsColor(Color.argb(0x00, 0xff, 0x00, 0x00)); // transparent margins
            mRenderer.setPanEnabled(false, false);
            mRenderer.setYAxisMax(-40);
            mRenderer.setYAxisMin(-100);
            mRenderer.setYLabels(6);
            mRenderer.setYTitle(LABEL_Y);
            mRenderer.setXAxisMin(-2);
            mRenderer.setXAxisMax(numberOfChannels + 2);
            mRenderer.setXLabels(0);
            mRenderer.setXTitle(LABEL_X);
            mRenderer.setShowGrid(true);
            mRenderer.setShowLabels(true);
            mRenderer.setFitLegend(true);
            mRenderer.setShowCustomTextGrid(true);

            for (int i = -2; i < numberOfChannels + 2; i++)
            {
                if(i > 0 && i < numberOfChannels + 1)
                    mRenderer.addXTextLabel(i, String.valueOf(i));
                else
                    mRenderer.addXTextLabel(i, "");
            }
        }

        public void setValues( String currentSSID)
        {
            int index = 0;

            for(int i = 0; i < mWifiScanList.size(); i++)
            {
                XYSeriesRenderer renderer = new XYSeriesRenderer();
                renderer.setColor(getColorForConnection(mSsidColorMap, mWifiScanList.get(i).SSID));
                renderer.setDisplayBoundingPoints(true);

                if(mWifiScanList.get(i).SSID.equals(currentSSID))
                {
                    renderer.setLineWidth(5);
                    renderer.setPointStyle(PointStyle.DIAMOND);
                    renderer.setDisplayChartValues(true);
                    renderer.setChartValuesTextSize(15);
                    renderer.setPointStrokeWidth(10);
                }
                else
                {
                    renderer.setLineWidth(2);
                    renderer.setPointStyle(PointStyle.CIRCLE);
                    renderer.setPointStrokeWidth(3);
                }

                XYSeries series = new XYSeries(mWifiScanList.get(i).SSID );

                int channel = Utility.convertFrequencyToChannel(mWifiScanList.get(i).frequency);

                if( channel  < sFirstChannel || channel > sLastChannel)
                    continue;

                series.add(channel - 2, -100);
                series.add(channel, mWifiScanList.get(i).level);
                series.add(channel + 2, -100);

                mDataset.addSeries(index, series);
                mRenderer.addSeriesRenderer(index, renderer);
                index++;

            }
        }

        private int getColorForConnection(HashMap<String, Integer> hashMap, String ssid)
        {
            if (!hashMap.containsKey(ssid))
            {
                hashMap.put(ssid, Utility.randColor());
            }

            return hashMap.get(ssid);
        }

        public View getmChartView()
        {
            mChartView =  ChartFactory.getLineChartView(getActivity(), mDataset, mRenderer);
            return mChartView;
        }
    }

    private class ValuationChannelAdapter extends ArrayAdapter<String[]>
    {
        final static short SIZE_TAB = 4;
        final static short CHANNEL_TAB = 0;
        final static short VALUATION_PERCENT_TAB = 1;
        final static short NETWORKS_ON_THIS_CHANNEL = 2;
        final static short VALUATION_PERCENT_RECOMMENDED_TAB = 3;

        private final Context context;
        private final List<String[]> values;

        public ValuationChannelAdapter(Context context, List<String[]> objects)
        {
            super(context, R.layout.channel_interference_listview, objects);
            this.context = context;
            this.values = objects;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent)
        {
            View view = null;

            if(convertView == null)
            {
                LayoutInflater inflater = (LayoutInflater) context.
                        getSystemService(Context.LAYOUT_INFLATER_SERVICE);

                view = inflater.inflate(R.layout.channel_interference_listview, parent, false);
            }

            ViewHolder viewHolder = new ViewHolder(view);

            setValuesToListView(position, viewHolder);

            return view;
        }

        private void setValuesToListView(int position, ViewHolder viewHolder)
        {
            for (int i = 0; i < this.values.size(); i++)
            {
                String[] tab = getItem(position);

                viewHolder.channelView.setText(String.format(
                        getResources().getString(R.string.cia_channel), Integer.valueOf(tab[CHANNEL_TAB])
                ));
                viewHolder.valuationProgressBarView.setText(String.format(
                        getResources().getString(R.string.cia_valuation_progressbar),Integer.valueOf(tab[VALUATION_PERCENT_TAB])
                ));
                viewHolder.valuationView.setText(String.format(
                        getResources().getString(R.string.cia_valuation_progressbar),Integer.valueOf(tab[VALUATION_PERCENT_RECOMMENDED_TAB])
                ));
                viewHolder.networksOnThisChannelView.setText(Integer.valueOf(tab[NETWORKS_ON_THIS_CHANNEL]).toString());

                viewHolder.progressBar.setProgress(Integer.valueOf(tab[VALUATION_PERCENT_TAB]));
            }

        }

        public class ViewHolder
        {
            public final TextView channelView;
            public final TextView valuationProgressBarView;
            public final TextView valuationView;
            public final TextView networksOnThisChannelView;
            public final ProgressBar progressBar;

            public ViewHolder(View view)
            {
                channelView = (TextView) view.findViewById(R.id.cia_channel_textview);
                valuationProgressBarView = (TextView) view.findViewById(R.id.cia_valuation_progressbar_textView);
                valuationView = (TextView) view.findViewById(R.id.cia_valuation_textview);
                networksOnThisChannelView = (TextView) view.findViewById(R.id.cia_number_of_networks_textview);

                progressBar =  (ProgressBar) view.findViewById(R.id.cia_valuation_progressbar);
                progressBar.setMax(100);
            }
        }

    }

    private class WifiScanReceiver extends BroadcastReceiver
    {
        public void onReceive(Context c, Intent intent)
        {
            update();
        }
    }

    public class ViewHolder
    {
        public final TextView connectedView;
        public final TextView valuationProgressBarView;
        public final TextView channelView;
        public final TextView networksOnThisChannelView;
        public final TextView recommendedView;

        public final ProgressBar valuationProgressBar;

        public final ListView listView;

        public final LinearLayout mChartChannels;

        public ViewHolder(View rootView)
        {
            mChartChannels = (LinearLayout) rootView.findViewById(R.id.ci_channel_chart);

            connectedView = (TextView) rootView.findViewById(R.id.ci_connected_textview);
            valuationProgressBarView = (TextView) rootView.findViewById(R.id.ci_valuation_progressbar_textView);
            channelView = (TextView) rootView.findViewById(R.id.ci_channel_textview);
            networksOnThisChannelView = (TextView) rootView.findViewById(R.id.ci_numbers_of_networks_on_this_ch_textview);
            recommendedView = (TextView) rootView.findViewById(R.id.ci_recommended_textview);

            valuationProgressBar = (ProgressBar) rootView.findViewById(R.id.ci_valuation_progressbar);

            listView = (ListView) rootView.findViewById(R.id.valuation_channels_listview);
        }
    }
}
