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

    private WifiScanReceiver mWifiReceiver;
    private WifiManager mWifiManager;
    private LinearLayout mChartChannels;
    private HashMap<String, Integer> mSsidColorMap = new HashMap<String, Integer>();

    TextView connectedView;
    TextView valuationProgressBarView;
    TextView channelView;
    TextView networksOnThisChannelView;
    TextView recommendedView;

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

        mChartChannels.addView(updateChart(), 0);

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

    private View updateChart()
    {
        WifiChart wifiChart = new WifiChart(mWifiManager);
        wifiChart.init();
        wifiChart.setValues();

        updateInfoBar(wifiChart.getmOnConnectedChannel(), wifiChart.getmFrequency());

        return wifiChart.getmChartView();
    }

    private void updateInfoBar(int size, int freq)
    {
        Resources resources = getResources();
        WifiInfo wifiInfo = mWifiManager.getConnectionInfo();

        connectedView.setText(String.format(resources.getString(R.string.connected_bar), wifiInfo.getSSID()));
        valuationProgressBarView.setText("2/10");
        channelView.setText(String.format(resources.getString(R.string.ci_channel_bar), Utility.convertFrequencyToChannel(freq)));
        networksOnThisChannelView.setText(String.format(resources.getString(R.string.ci_number_of_networks_on_this_channel_bar), size));
        recommendedView.setText(String.format(resources.getString(R.string.ci_recommended_bar), 3, 3, 3));
    }

    private int getColorForConnection(HashMap<String, Integer> hashMap, String ssid)
    {
            if (!hashMap.containsKey(ssid))
            {
                hashMap.put(ssid, Utility.randColor());
            }

        return hashMap.get(ssid);
    }

    private class WifiScanReceiver extends BroadcastReceiver
    {
        public void onReceive(Context c, Intent intent)
        {
            mChartChannels.addView(updateChart(), 0);
        }
    }

    private class WifiChart
    {
        private XYMultipleSeriesDataset mDataset;
        private XYMultipleSeriesRenderer mRenderer;

        private GraphicalView mChartView;
        private List<ScanResult> mWifiScanList;

        private final String LABEL_X = "Channel";
        private final String LABEL_Y = "Strength (dBm)";

        private int mOnConnectedChannel = 0;
        private int mFrequency = 0;

        public WifiChart(WifiManager wifiManager)
        {
            mDataset = new XYMultipleSeriesDataset();
            mRenderer = new XYMultipleSeriesRenderer();

            Utility.enableWifi(wifiManager);

            wifiManager.startScan();

            mWifiScanList = wifiManager.getScanResults();
            mChartView =  ChartFactory.getLineChartView(getActivity(), mDataset, mRenderer);
        }

        public View getmChartView()
        {
            mChartView =  ChartFactory.getLineChartView(getActivity(), mDataset, mRenderer);
            return mChartView;
        }

        public int getmOnConnectedChannel()
        {
            return mOnConnectedChannel;
        }

        public int getmFrequency()
        {
            return mFrequency;
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
            mRenderer.setXAxisMax(15);
            mRenderer.setXLabels(0);
            mRenderer.setXTitle(LABEL_X);
            mRenderer.setShowGrid(true);
            mRenderer.setShowLabels(true);
            mRenderer.setFitLegend(true);
            mRenderer.setShowCustomTextGrid(true);

            for (int i = -2; i < 15; i++)
            {
                if(i > 0 && i < 14)
                    mRenderer.addXTextLabel(i, String.valueOf(i));
                else
                    mRenderer.addXTextLabel(i, "");
            }
        }

        public void setValues()
        {
            WifiInfo wifiInfo = mWifiManager.getConnectionInfo();

            for(int i = 0; i < mWifiScanList.size(); i++)
            {
                if(wifiInfo.getBSSID().equals(mWifiScanList.get(i).BSSID))
                {
                    mFrequency = mWifiScanList.get(i).frequency;
                }
            }

            for(int i = 0; i < mWifiScanList.size(); i++)
            {
                if (mFrequency == mWifiScanList.get(i).frequency)
                {
                    ++mOnConnectedChannel;
                }

                XYSeriesRenderer renderer = new XYSeriesRenderer();
                renderer.setLineWidth(2);
                renderer.setColor(getColorForConnection(mSsidColorMap, mWifiScanList.get(i).SSID));
                renderer.setDisplayBoundingPoints(true);
                renderer.setPointStyle(PointStyle.CIRCLE);
                renderer.setPointStrokeWidth(3);

                XYSeries series = new XYSeries(mWifiScanList.get(i).SSID );

                int channel = Utility.convertFrequencyToChannel(mWifiScanList.get(i).frequency);
                series.add( channel - 2, -100);
                series.add( channel, mWifiScanList.get(i).level);
                series.add( channel + 2, -100);

                mDataset.addSeries(i, series);
                mRenderer.addSeriesRenderer(i, renderer);

            }
        }

    }

}
