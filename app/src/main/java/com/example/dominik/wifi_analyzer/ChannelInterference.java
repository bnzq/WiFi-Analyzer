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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.achartengine.ChartFactory;
import org.achartengine.GraphicalView;
import org.achartengine.chart.PointStyle;
import org.achartengine.model.XYMultipleSeriesDataset;
import org.achartengine.model.XYSeries;
import org.achartengine.renderer.XYMultipleSeriesRenderer;
import org.achartengine.renderer.XYSeriesRenderer;

import java.util.HashMap;
import java.util.List;

public class ChannelInterference extends Fragment
{
    public static final String LOG_TAG = ChannelInterference.class.getSimpleName();

    final static short FREQUENCY_CONNECTED_CHANNEL = 0;
    final static short NETWORKS_ON_CONNECTED_CHANNEL = 1;
    final static short SSID_CONNECTED_NETWORK = 2;

    final private short numberOfChannels = 13;

    private WifiScanReceiver mWifiReceiver;
    private WifiManager mWifiManager;
    private LinearLayout mChartChannels;
    private HashMap<String, Integer> mSsidColorMap = new HashMap<String, Integer>();

    TextView connectedView;
    TextView valuationProgressBarView;
    TextView channelView;
    TextView networksOnThisChannelView;
    TextView recommendedView;

    ProgressBar valuationProgressBar;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.channel_interference, container, false);
        mChartChannels = (LinearLayout) rootView.findViewById(R.id.ci_channel_chart);

        connectedView = (TextView) rootView.findViewById(R.id.ci_connected_textview);
        valuationProgressBarView = (TextView) rootView.findViewById(R.id.ci_valuation_progressbar_textView);
        channelView = (TextView) rootView.findViewById(R.id.ci_channel_textview);
        networksOnThisChannelView = (TextView) rootView.findViewById(R.id.ci_numbers_of_networks_on_this_ch_textview);
        recommendedView = (TextView) rootView.findViewById(R.id.ci_recommended_textview);

        valuationProgressBar = (ProgressBar) rootView.findViewById(R.id.ci_valuation_progressbar);

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
        WifiInfo wifiInfo = mWifiManager.getConnectionInfo();
        HashMap<Short, String> connectionInfo =  getInfoAboutCurrentConnection(wifiInfo, wifiScanList);

        updateInfoBar(Integer.valueOf(connectionInfo.get(NETWORKS_ON_CONNECTED_CHANNEL)),
                Integer.valueOf(connectionInfo.get(FREQUENCY_CONNECTED_CHANNEL)),
                connectionInfo.get(SSID_CONNECTED_NETWORK));

        updateChart(wifiScanList, connectionInfo);

    }

    private void updateChart(List<ScanResult> wifiScanList, HashMap<Short, String> connectionInfo)
    {
        WifiChart wifiChart = new WifiChart(wifiScanList, numberOfChannels);
        wifiChart.init();
        wifiChart.setValues(connectionInfo.get(SSID_CONNECTED_NETWORK));

        mChartChannels.addView(wifiChart.getmChartView(), 0);
    }

    private void updateInfoBar(int size, int freq , String ssid)
    {
        Resources resources = getResources();

        connectedView.setText(String.format(resources.getString(R.string.connected_bar), ssid));
        valuationProgressBarView.setText("2/10");
        channelView.setText(String.format(resources.getString(R.string.ci_channel_bar), Utility.convertFrequencyToChannel(freq)));
        networksOnThisChannelView.setText(String.format(resources.getString(R.string.ci_number_of_networks_on_this_channel_bar), size));
        recommendedView.setText(String.format(resources.getString(R.string.ci_recommended_bar), 3, 3, 3));
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
        private final short numberOfChannels;
        private XYMultipleSeriesDataset mDataset;
        private XYMultipleSeriesRenderer mRenderer;

        private GraphicalView mChartView;

        private final String LABEL_X = "Channel";
        private final String LABEL_Y = "Strength (dBm)";
        private final List<ScanResult> mWifiScanList;

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
}
