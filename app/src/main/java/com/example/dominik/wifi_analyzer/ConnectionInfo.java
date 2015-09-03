package com.example.dominik.wifi_analyzer;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.net.DhcpInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.format.Formatter;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.cardiomood.android.controls.gauge.SpeedometerGauge;

import org.achartengine.ChartFactory;
import org.achartengine.GraphicalView;
import org.achartengine.chart.PointStyle;
import org.achartengine.model.XYMultipleSeriesDataset;
import org.achartengine.model.XYSeries;
import org.achartengine.renderer.XYMultipleSeriesRenderer;
import org.achartengine.renderer.XYSeriesRenderer;

import java.util.HashMap;
import java.util.List;

public class ConnectionInfo extends Fragment
{
    public static final String LOG_TAG = ConnectionInfo.class.getSimpleName();

    private static final String SERIES_KEY = "KEY";

    private WifiScanReceiver mWifiReceiver;
    private WifiManager mWifiManager;
    private HashMap<String, XYSeries> series = new HashMap<String, XYSeries>();

    ViewHolder viewHolder;

    private long timeStart = 0;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState)
    {
        View rootView = inflater.inflate(R.layout.connection_info_tab, container, false);

        viewHolder = new ViewHolder(rootView);

        initSpeedometer();
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

    //update info bar, speedometer, connection info
    private void update()
    {
        WifiInfo wifiInfo = mWifiManager.getConnectionInfo();

        String ssid = wifiInfo.getSSID();
        if(wifiInfo.getBSSID() == null)
        {
            ssid = "EXAMPLE";
        }

        double rssi = wifiInfo.getRssi();

        if(rssi < -100)
            rssi = -100;

        WifiChart wifiChart = new WifiChart();
        wifiChart.init();
        wifiChart.setValues(
                (System.nanoTime() - timeStart) / 1000000000,
                (int) rssi,
                ssid
        );

        viewHolder.mChartChannels.addView(wifiChart.getmChartView(), 0);

        if(wifiInfo.getBSSID() == null)
            return;

        viewHolder.speedometer.setSpeed(rssi + 100, 1000, 0);
        viewHolder.strengthBarView.setText(String.format(getResources().getString(R.string.cinf_strength_bar_view), wifiInfo.getRssi()));
        viewHolder.strengthOnProgressBarView.setText(String.format(
                getResources().getString(R.string.percent_textView), String.valueOf(Utility.convertRssiToQuality(wifiInfo.getRssi()))));
        viewHolder.progressBar.setProgress(Utility.convertRssiToQuality(wifiInfo.getRssi()));
        viewHolder.connectedView.setText(String.format(getString(R.string.connected_bar), wifiInfo.getSSID()));

        List<ScanResult> wifiScanList = mWifiManager.getScanResults();

        if(wifiScanList == null)
        {
            return;
        }

        String cap = "";
        int freq = 1;

        for (int i = 0; i < wifiScanList.size(); i++)
        {
            if(wifiScanList.get(i).BSSID.equals(wifiInfo.getBSSID()))
            {
                cap = wifiScanList.get(i).capabilities;
                freq = wifiScanList.get(i).frequency;
            }
        }

        DhcpInfo dhcpInfo = mWifiManager.getDhcpInfo();

        viewHolder.ssidView.setText(wifiInfo.getSSID());
        viewHolder.bssidView.setText(wifiInfo.getBSSID());
        viewHolder.macView.setText(wifiInfo.getMacAddress());
        viewHolder.speedView.setText(String.format(getString(R.string.Mbps_textView), wifiInfo.getLinkSpeed()));
        viewHolder.strengthbView.setText(String.format(getString(R.string.dBm_textView), wifiInfo.getRssi()));
        viewHolder.encryptionView.setText(Utility.getEncryptionFromCapabilities(cap));
        viewHolder.channelView.setText(String.valueOf(Utility.convertFrequencyToChannel(freq)));
        viewHolder.frequecyView.setText(String.valueOf(freq));
        viewHolder.ipView.setText(Formatter.formatIpAddress(wifiInfo.getIpAddress()));
        viewHolder.netmaskView.setText(Formatter.formatIpAddress( dhcpInfo.netmask));
        viewHolder.gatewayView.setText(Formatter.formatIpAddress(dhcpInfo.gateway));
        viewHolder.dhcpView.setText(Formatter.formatIpAddress(dhcpInfo.serverAddress));
        viewHolder.dns1View.setText(Formatter.formatIpAddress(dhcpInfo.dns1));
        viewHolder.dns2View.setText(Formatter.formatIpAddress(dhcpInfo.dns2));
        viewHolder.dhsp1View.setText(String.valueOf(dhcpInfo.leaseDuration));
    }

    //init needle gauge
    private void initSpeedometer()
    {
        viewHolder.speedometer.setLabelConverter(new SpeedometerGauge.LabelConverter()
        {
            @Override
            public String getLabelFor(double progress, double maxProgress)
            {
                return String.valueOf((int) Math.round(progress) - 100);
            }
        });

        viewHolder.speedometer.setMaxSpeed(100);
        viewHolder.speedometer.setMajorTickStep(10);
        viewHolder.speedometer.setMinorTicks(5);

        viewHolder.speedometer.addColoredRange(0, 25, Color.RED);
        viewHolder.speedometer.addColoredRange(25, 50, Color.YELLOW);
        viewHolder.speedometer.addColoredRange(50, 100, Color.GREEN);
        viewHolder.speedometer.setLabelTextSize(15);
        viewHolder.speedometer.setSpeed(0);
    }

    private class WifiChart
    {
        private XYMultipleSeriesDataset mDataset;
        private XYMultipleSeriesRenderer mRenderer;

        private GraphicalView mChartView;

        private final String LABEL_X = getString(R.string.sg_labelx);
        private final String LABEL_Y = getString(R.string.sg_labely);

        public WifiChart()
        {
            mDataset = new XYMultipleSeriesDataset();
            mRenderer = new XYMultipleSeriesRenderer();

            mChartView =  ChartFactory.getLineChartView(getActivity(), mDataset, mRenderer);
        }

        public void init()
        {
            mRenderer.setMarginsColor(Color.argb(0x00, 0xff, 0x00, 0x00)); // transparent margins
            mRenderer.setPanEnabled(false, false);
            mRenderer.setYAxisMax(-40);
            mRenderer.setYAxisMin(-100);
            mRenderer.setYLabels(3);
            mRenderer.setYTitle(LABEL_Y);
            mRenderer.setXAxisMin(0);
            mRenderer.setXAxisMax(300);
            mRenderer.setXLabels(15);
            mRenderer.setXTitle(LABEL_X);
            mRenderer.setShowGrid(true);
            mRenderer.setShowLabels(true);
            mRenderer.setShowLegend(false);
            mRenderer.setShowCustomTextGrid(true);
        }

        public void setValues(long time, int dBm, String ssid)
        {
            if(time > 300)
            {
                timeStart = System.nanoTime();
                time = 0;
                series.clear();
            }

            XYSeriesRenderer renderer = new XYSeriesRenderer();
            renderer.setColor(Color.BLUE);
            renderer.setDisplayBoundingPoints(true);

            renderer.setLineWidth(1);
            renderer.setPointStyle(PointStyle.CIRCLE);
            renderer.setPointStrokeWidth(1);

            XYSeries xySerieseries;

            if(series.containsKey(SERIES_KEY))
            {
                xySerieseries = series.get(SERIES_KEY);
                xySerieseries.add(time, dBm);
                series.remove(SERIES_KEY);
                series.put(SERIES_KEY, xySerieseries);
            }
            else
            {
                xySerieseries = new XYSeries(ssid);
                xySerieseries.add(time ,dBm);
                series.put(SERIES_KEY, xySerieseries);
            }

            mDataset.addSeries(0, xySerieseries);
            mRenderer.addSeriesRenderer(0, renderer);
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
        };
    }

    public class ViewHolder
    {
        public final SpeedometerGauge speedometer;

        public final TextView connectedView;
        public final TextView strengthBarView;
        public final TextView strengthOnProgressBarView;
        public final TextView ssidView;
        public final TextView bssidView;
        public final TextView macView;
        public final TextView speedView;
        public final TextView strengthbView;
        public final TextView encryptionView;
        public final TextView channelView;
        public final TextView frequecyView;
        public final TextView ipView;
        public final TextView netmaskView;
        public final TextView gatewayView;
        public final TextView dhcpView;
        public final TextView dns1View;
        public final TextView dns2View;
        public final TextView dhsp1View;

        public final ProgressBar progressBar;

        public final LinearLayout mChartChannels;


        public ViewHolder(View rootView)
        {
            speedometer = (SpeedometerGauge) rootView.findViewById(R.id.speedometer);

            connectedView = (TextView) rootView.findViewById(R.id.cinf_connected_textview);
            strengthBarView = (TextView) rootView.findViewById(R.id.cinf_strength_bar_view);
            strengthOnProgressBarView = (TextView) rootView.findViewById(R.id.cinf_strength_percent_progressbar_now_textView);

            progressBar = (ProgressBar) rootView.findViewById(R.id.cinf_quality_progressbar_now);
            progressBar.setMax(100);

            mChartChannels = (LinearLayout) rootView.findViewById(R.id.cinf_channel_chart);

            ssidView = (TextView) rootView.findViewById(R.id.cinf_ssid_view);
            bssidView = (TextView) rootView.findViewById(R.id.cinf_bssid_view);
            macView = (TextView) rootView.findViewById(R.id.cinf_mac_view);
            speedView = (TextView) rootView.findViewById(R.id.cinf_speed_view);
            strengthbView = (TextView) rootView.findViewById(R.id.cinf_strength_view);
            encryptionView = (TextView) rootView.findViewById(R.id.cinf_encryption_view);
            channelView = (TextView) rootView.findViewById(R.id.cinf_channel_view);
            frequecyView = (TextView) rootView.findViewById(R.id.cinf_frequency_view);
            ipView = (TextView) rootView.findViewById(R.id.cinf_ip_view);
            netmaskView = (TextView) rootView.findViewById(R.id.cinf_netmask_view);
            gatewayView = (TextView) rootView.findViewById(R.id.cinf_gateway_view);
            dhcpView = (TextView) rootView.findViewById(R.id.cinf_dhcp_view);
            dns1View = (TextView) rootView.findViewById(R.id.cinf_dns1_view);
            dns2View = (TextView) rootView.findViewById(R.id.cinf_dns2_view);
            dhsp1View = (TextView) rootView.findViewById(R.id.cinf_dhspl_view);
        }
    }
}
