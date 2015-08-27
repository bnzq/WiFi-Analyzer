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
    public static final String LOG_TAG = ChannelInterference.class.getSimpleName();

    final static short FREQUENCY_CONNECTED_CHANNEL = 0;
    final static short NETWORKS_ON_CONNECTED_CHANNEL = 1;
    final static short SSID_CONNECTED_NETWORK = 2;

    final private static short sNumberOfChannels = 13;

    final private static short sAllChannels = sNumberOfChannels + 4;
    final private static short s1stFactor = 80;
    final private static short s2ndFactor = 72;
    final private static short s3rdFactor = 64;
    final private static float sDBmFactor = 0.2f;
    final private static short sDBm2ndFactor = 20;
    final private static short sDBm3rdFactor = 40;


    private WifiScanReceiver mWifiReceiver;
    private WifiManager mWifiManager;
    private LinearLayout mChartChannels;
    private ValuationChannelAdapter mValuationChannelAdapter;
    private HashMap<String, Integer> mSsidColorMap = new HashMap<String, Integer>();

    TextView connectedView;
    TextView valuationProgressBarView;
    TextView channelView;
    TextView networksOnThisChannelView;
    TextView recommendedView;

    ProgressBar valuationProgressBar;

    ListView listView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.channel_interference_tab, container, false);
        mChartChannels = (LinearLayout) rootView.findViewById(R.id.ci_channel_chart);

        connectedView = (TextView) rootView.findViewById(R.id.ci_connected_textview);
        valuationProgressBarView = (TextView) rootView.findViewById(R.id.ci_valuation_progressbar_textView);
        channelView = (TextView) rootView.findViewById(R.id.ci_channel_textview);
        networksOnThisChannelView = (TextView) rootView.findViewById(R.id.ci_numbers_of_networks_on_this_ch_textview);
        recommendedView = (TextView) rootView.findViewById(R.id.ci_recommended_textview);

        valuationProgressBar = (ProgressBar) rootView.findViewById(R.id.ci_valuation_progressbar);

        listView = (ListView) rootView.findViewById(R.id.valuation_channels_listview);

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

        updateInfoBar(Integer.valueOf(connectionInfo.get(NETWORKS_ON_CONNECTED_CHANNEL)),
                Integer.valueOf(connectionInfo.get(FREQUENCY_CONNECTED_CHANNEL)),
                connectionInfo.get(SSID_CONNECTED_NETWORK));

        updateChart(wifiScanList, connectionInfo);

        updateValuationList(wifiScanList, connectionInfo);
    }

    private void updateChart(List<ScanResult> wifiScanList, HashMap<Short, String> connectionInfo)
    {
        WifiChart wifiChart = new WifiChart(wifiScanList, sNumberOfChannels);
        wifiChart.init();
        wifiChart.setValues(connectionInfo.get(SSID_CONNECTED_NETWORK));

        mChartChannels.addView(wifiChart.getmChartView(), 0);
    }

    private void updateInfoBar(int size, int freq , String ssid)
    {
        Resources resources = getResources();

        connectedView.setText(String.format(resources.getString(R.string.connected_bar), ssid));
        channelView.setText(String.format(resources.getString(R.string.ci_channel_bar), Utility.convertFrequencyToChannel(freq)));
        networksOnThisChannelView.setText(String.format(resources.getString(R.string.ci_number_of_networks_on_this_channel_bar), size));
    }

    private void updateValuationList(List<ScanResult> wifiScanList, HashMap<Short, String> connectionInfo)
    {
        List<String[]> list = new ArrayList<String[]>();

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

        valuationProgressBar.setProgress(valuation_percent_recommended[
                Utility.convertFrequencyToChannel(Integer.valueOf(connectionInfo.get(FREQUENCY_CONNECTED_CHANNEL))) - 1]);
        valuationProgressBarView.setText(String.format(
                getResources().getString(R.string.cia_valuation),
                valuation_percent_recommended[
                        Utility.convertFrequencyToChannel(Integer.valueOf(connectionInfo.get(FREQUENCY_CONNECTED_CHANNEL))) - 1]/10
        ));

        int tab[] = getRecommendedChannels(valuation_percent_recommended);
        recommendedView.setText(String.format(getResources().getString(R.string.ci_recommended_bar), tab[0], tab[1], tab[2]));

        mValuationChannelAdapter = new ValuationChannelAdapter( getActivity(), list);
        listView.setAdapter(mValuationChannelAdapter);

    }

    private int[] valuateChannels(List<ScanResult> wifiScanList)
    {

        int[] result = new int[sAllChannels];

        for (int i = 0; i < sAllChannels; i++)
        {
            result[i] = 500;
        }

        for (int i = 0; i < wifiScanList.size(); i++)
        {
            int ch = Utility.convertFrequencyToChannel(wifiScanList.get(i).frequency);
            int dBm = wifiScanList.get(i).level;

            result[ch + 1] -= (s1stFactor + sDBmFactor * Utility.convertRssiToQualityWithSub(dBm, 0));
            result[ch] -= (s2ndFactor + sDBmFactor * Utility.convertRssiToQualityWithSub(dBm, sDBm2ndFactor));
            result[ch - 1] -= (s3rdFactor + sDBmFactor * Utility.convertRssiToQualityWithSub(dBm, sDBm3rdFactor));
            result[ch + 2] -= (s2ndFactor + sDBmFactor * Utility.convertRssiToQualityWithSub(dBm, sDBm2ndFactor));
            result[ch + 3] -= (s3rdFactor + sDBmFactor * Utility.convertRssiToQualityWithSub(dBm, sDBm3rdFactor));
        }

        for (int i = 0; i < sAllChannels; i++)
        {
            result[i] /= 5;
        }

        return result;
    }

    private int[] valuateChannelsWithoutConnected(List<ScanResult> wifiScanList)
    {

        int[] result = new int[sAllChannels];

        for (int i = 0; i < sAllChannels; i++)
        {
            result[i] = 500;
        }

        WifiInfo wifiInfo = mWifiManager.getConnectionInfo();

        for (int i = 0; i < wifiScanList.size(); i++)
        {
            if(!wifiScanList.get(i).BSSID.equals(wifiInfo.getBSSID()))
            {
                int ch = Utility.convertFrequencyToChannel(wifiScanList.get(i).frequency);
                int dBm = wifiScanList.get(i).level;

                result[ch + 1] -= (s1stFactor + sDBmFactor * Utility.convertRssiToQualityWithSub(dBm, 0));
                result[ch] -= (s2ndFactor + sDBmFactor * Utility.convertRssiToQualityWithSub(dBm, sDBm2ndFactor));
                result[ch - 1] -= (s3rdFactor + sDBmFactor * Utility.convertRssiToQualityWithSub(dBm, sDBm3rdFactor));
                result[ch + 2] -= (s2ndFactor + sDBmFactor * Utility.convertRssiToQualityWithSub(dBm, sDBm2ndFactor));
                result[ch + 3] -= (s3rdFactor + sDBmFactor * Utility.convertRssiToQualityWithSub(dBm, sDBm3rdFactor));
            }
        }

        for (int i = 0; i < sAllChannels; i++)
        {
            result[i] /= 5;
        }

        int med[] = new int[sNumberOfChannels];

        for (int i = 2; i < sAllChannels - 2; i++)
        {
            int sum = result[i - 2] + result[i - 2] + result[i] + result[i + 1] + result[i + 2];
            med[i - 2] = sum / 5;
        }

        return med;
    }

    private int[] getNumberOfNetworksOnChannels(List<ScanResult> wifiScanList)
    {
        int[] result = new int[sNumberOfChannels];

        for (int i = 0; i < sNumberOfChannels; i++)
        {
            result[i] = 0;
        }

        for (int i = 0; i < wifiScanList.size(); i++)
        {
            result[Utility.convertFrequencyToChannel(wifiScanList.get(i).frequency) - 1] += 1;
        }

        return result;
    }

    private int [] getRecommendedChannels(int [] ints)
    {
        int [] rec = new int[3];

        int tmp1 = 0, tmp2 = 0, tmp3 = 0;
        int tmp11 = 0, tmp22 = 0, tmp33 = 0;

        for (int i = 0; i < ints.length; i++)
        {
            if(tmp1 < ints[i])
            {
                tmp3 = tmp2;
                tmp2 = tmp1;
                tmp1 = ints[i];
                tmp33 = tmp22;
                tmp22 = tmp11;
                tmp11 = i;
            }
            else if (tmp2 < ints[i])
            {
                tmp3 = tmp2;
                tmp2 = ints[i];
                tmp33 = tmp22;
                tmp22 = i;
            }
            else if (tmp3 < ints[i])
            {
                tmp3 = ints[i];
                tmp33 = i;
            }
        }

        rec[0] = tmp11 + 1;
        rec[1] = tmp22 + 1;
        rec[2] = tmp33 + 1;

        return rec;
    }

    private HashMap<Short, String> getInfoAboutCurrentConnection(WifiInfo wifiInfo, List<ScanResult> wifiScanList)
    {
        HashMap<Short, String> hashMap = new HashMap<>();

        int onConnectedChannel = 0;
        int frequency = 0;

        for(int i = 0; i < wifiScanList.size(); i++)
        {
            if(wifiInfo.getBSSID().equals(wifiScanList.get(i).BSSID))
            {
                frequency = wifiScanList.get(i).frequency;
            }

            if (frequency == wifiScanList.get(i).frequency)
            {
                ++onConnectedChannel;
            }
        }

        hashMap.put(FREQUENCY_CONNECTED_CHANNEL, String.valueOf(frequency));
        hashMap.put(NETWORKS_ON_CONNECTED_CHANNEL, String.valueOf(onConnectedChannel));
        hashMap.put(SSID_CONNECTED_NETWORK, wifiInfo.getSSID());

        return hashMap;
    }

    private class WifiChart
    {
        private XYMultipleSeriesDataset mDataset;
        private XYMultipleSeriesRenderer mRenderer;

        private GraphicalView mChartView;

        private final String LABEL_X = "Channel";
        private final String LABEL_Y = "Strength (dBm)";
        private final List<ScanResult> mWifiScanList;

        private final short numberOfChannels;

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
            for(int i = 0; i < mWifiScanList.size(); i++)
            {
                XYSeriesRenderer renderer = new XYSeriesRenderer();
                renderer.setColor(getColorForConnection(mSsidColorMap, mWifiScanList.get(i).SSID));
                renderer.setDisplayBoundingPoints(true);

                if(currentSSID.equals(mWifiScanList.get(i).SSID))
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
                series.add(channel - 2, -100);
                series.add(channel, mWifiScanList.get(i).level);
                series.add(channel + 2, -100);

                mDataset.addSeries(i, series);
                mRenderer.addSeriesRenderer(i, renderer);
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

    private class WifiScanReceiver extends BroadcastReceiver
    {
        public void onReceive(Context c, Intent intent)
        {
            update();
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
            LayoutInflater inflater = (LayoutInflater) context.
                    getSystemService(Context.LAYOUT_INFLATER_SERVICE);

            View view = inflater.inflate(R.layout.channel_interference_listview, parent, false);

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
}
