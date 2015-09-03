package com.example.dominik.wifi_analyzer;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
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

public class StrengthGraph extends Fragment
{
    public static final String LOG_TAG = StrengthGraph.class.getSimpleName();

    private WifiScanReceiver mWifiReceiver;
    private WifiManager mWifiManager;
    private HashMap<String, Integer> mSsidColorMap = new HashMap<String, Integer>();
    private HashMap<String, XYSeries> series = new HashMap<String, XYSeries>();

    LinearLayout mChartChannels;

    TextView connectedView;

    private long timeStart = 0;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.graph_tab, container, false);

        mChartChannels = (LinearLayout) rootView.findViewById(R.id.sg_channel_chart);

        connectedView = (TextView) rootView.findViewById(R.id.sg_connected_textview);

        timeStart = System.nanoTime();

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

        timeStart = System.nanoTime();
        series.clear();

        super.onPause();
    }

    @Override
    public void onResume()
    {
        getActivity().registerReceiver(mWifiReceiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));

        timeStart = System.nanoTime();
        series.clear();

        super.onResume();
    }

    //update info bar & wifi chart
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

        WifiChart wifiChart = new WifiChart(wifiScanList);
        wifiChart.init();
        wifiChart.setValues(wifiInfo.getSSID(), (System.nanoTime() - timeStart) / 1000000000);

        mChartChannels.addView(wifiChart.getmChartView(), 0);

        if(wifiInfo.getBSSID() == null)
            return;

        connectedView.setText(String.format(
                getResources().getString(R.string.connected_bar), wifiInfo.getSSID()
        ));

    }

    private class WifiChart
    {
        private XYMultipleSeriesDataset mDataset;
        private XYMultipleSeriesRenderer mRenderer;

        private GraphicalView mChartView;

        private final String LABEL_X = getString(R.string.sg_labelx);
        private final String LABEL_Y = getString(R.string.sg_labely);
        private final List<ScanResult> mWifiScanList;

        public WifiChart(List<ScanResult> wifiScanList)
        {
            mDataset = new XYMultipleSeriesDataset();
            mRenderer = new XYMultipleSeriesRenderer();
            this.mWifiScanList = wifiScanList;

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
            mRenderer.setXAxisMin(0);
            mRenderer.setXAxisMax(120);
            mRenderer.setXLabels(30);
            mRenderer.setXTitle(LABEL_X);
            mRenderer.setShowGrid(true);
            mRenderer.setShowLabels(true);
            mRenderer.setFitLegend(true);
            mRenderer.setShowCustomTextGrid(true);
        }

        public void setValues( String currentSSID, long time)
        {
            if(time > 120)
            {
                timeStart = System.nanoTime();
                time = 0;
                series.clear();
            }

            for(int i = 0; i < mWifiScanList.size(); i++)
            {
                XYSeriesRenderer renderer = new XYSeriesRenderer();
                renderer.setColor(getColorForConnection(mSsidColorMap, mWifiScanList.get(i).SSID));
                renderer.setDisplayBoundingPoints(true);

                if(currentSSID.equals(mWifiScanList.get(i).SSID))
                {
                    renderer.setLineWidth(3);
                    renderer.setPointStyle(PointStyle.DIAMOND);
                    renderer.setDisplayChartValues(false);
                    renderer.setChartValuesTextSize(15);
                    renderer.setPointStrokeWidth(3);
                }
                else
                {
                    renderer.setLineWidth(2);
                    renderer.setPointStyle(PointStyle.CIRCLE);
                    renderer.setPointStrokeWidth(3);
                }

                XYSeries xySerieseries;

                if(series.containsKey(mWifiScanList.get(i).BSSID))
                {
                    xySerieseries = series.get(mWifiScanList.get(i).BSSID);
                    xySerieseries.add(time, mWifiScanList.get(i).level);
                    series.remove(mWifiScanList.get(i).BSSID);
                    series.put(mWifiScanList.get(i).BSSID, xySerieseries);
                }
                else
                {
                    xySerieseries = new XYSeries(mWifiScanList.get(i).SSID );
                    xySerieseries.add(time ,mWifiScanList.get(i).level);
                    series.put(mWifiScanList.get(i).BSSID, xySerieseries);
               }

                mDataset.addSeries(i, xySerieseries);
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
