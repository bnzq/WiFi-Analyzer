package com.example.dominik.wifi_analyzer;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.format.Formatter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class NetworkStatus extends Fragment
{
    public static final String LOG_TAG = NetworkStatus.class.getSimpleName();

    WifiScanReceiver wifiReceiver;
    WifiManager wifiManager;
    private NetworkStatusAdapter mNetworkStatusAdapter;
    private ListView mListView;

    private int refreshRateInSec = 2;
    private int refreshRate = 1000 * refreshRateInSec;
    private Timer timer;
    private TimerTask updateTask;

    Button refreshButton;
    Button refreshRateButton;

    TextView connectedInfoView;
    TextView intervalView;
    TextView ipView;
    TextView speedView;
    TextView wirelessNetworks;

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        wifiManager=(WifiManager)getActivity().getSystemService(Context.WIFI_SERVICE);
        wifiReceiver = new WifiScanReceiver();

        enableWifi();

        wifiManager.startScan();

        super.onCreate(savedInstanceState);
    }

    @Override
    public void onPause()
    {
        setParametersReceiverBefore();
        super.onPause();
    }

    @Override
    public void onResume()
    {
        setParametersReceiverAfter();
        super.onResume();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState)
    {

        View rootView = inflater.inflate(R.layout.network_status_tab, container, false);

        refreshButton = (Button) rootView.findViewById(R.id.refresh_button);
        refreshRateButton = (Button) rootView.findViewById(R.id.scanning_time_button);

        connectedInfoView = (TextView) rootView.findViewById(R.id.ns_connected_textview);
        intervalView = (TextView) rootView.findViewById(R.id.ns_interval_textview);
        ipView = (TextView) rootView.findViewById(R.id.ns_ip_textView);
        speedView = (TextView) rootView.findViewById(R.id.ns_speed_textView);
        wirelessNetworks = (TextView) rootView.findViewById(R.id.ns_number_of_available_network_textView);

        refreshButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                updateNetworkStatus();
            }
        });

        refreshRateButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                createRefreshIntervalDialog();
            }
        });

        mListView = (ListView) rootView.findViewById(R.id.network_status_listview);

        return rootView;
    }

    //unregister receiver & close timer
    private void setParametersReceiverBefore()
    {
        getActivity().unregisterReceiver(wifiReceiver);

        if (timer != null)
        {
            timer.cancel();
            timer.purge();
            timer = null;
        }
    }

    //register receiver & schedule timer
    private void setParametersReceiverAfter()
    {
        getActivity().registerReceiver(wifiReceiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));

        //schedule timer
        if(refreshRate > 0)
        {
            timer = new Timer();
            updateTask = new TimerTask() {
                @Override
                public void run() {
                    getActivity().runOnUiThread(new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            updateNetworkStatus();
                        }
                    });
                }
            };
            timer.scheduleAtFixedRate(updateTask, 0, refreshRate);
        }
    }

    //update view in simple info bar
    private void updateInfoBar(int size)
    {
        Resources resources = getResources();
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();

        connectedInfoView.setText(String.format(resources.getString(R.string.connected_bar), wifiInfo.getSSID()));
        intervalView.setText(String.format(resources.getString(R.string.ns_interval_bar), refreshRateInSec));
        ipView.setText(String.format(resources.getString(R.string.ns_ip), Formatter.formatIpAddress(wifiInfo.getIpAddress())));
        speedView.setText(String.format(resources.getString(R.string.ns_speed), wifiInfo.getLinkSpeed()));
        wirelessNetworks.setText(String.format(resources.getString(R.string.ns_number_of_available_network),size));
    }

    //update network status for each signal
    private void updateNetworkStatus()
    {
        enableWifi();

        wifiManager.startScan();

        List<ScanResult> wifiScanList = wifiManager.getScanResults();
        List<String[]> list = new ArrayList<String[]>();


        if(wifiScanList == null)
        {
            return;
        }

        WifiInfo wifiInfo = wifiManager.getConnectionInfo();

        //add each []wifiDetails to list
        for (int i = 0; i < wifiScanList.size(); i++)
        {
            String [] wifiDetails = new String[NetworkStatusAdapter.SIZE_TAB];

            for (int j = 0; j < NetworkStatusAdapter.SIZE_TAB; j++)
            {
                wifiDetails[NetworkStatusAdapter.BSSID_TAB] = wifiScanList.get(i).BSSID;
                wifiDetails[NetworkStatusAdapter.SSID_TAB] = wifiScanList.get(i).SSID;
                wifiDetails[NetworkStatusAdapter.CAPABILITIES_TAB] = wifiScanList.get(i).capabilities;
                wifiDetails[NetworkStatusAdapter.FREQUENCY_TAB] = String.valueOf(wifiScanList.get(i).frequency);
                wifiDetails[NetworkStatusAdapter.LEVEL_TAB] = String.valueOf(wifiScanList.get(i).level);

                if(wifiInfo.getBSSID().equals(wifiScanList.get(i).BSSID))
                {
                    wifiDetails[NetworkStatusAdapter.IS_CONNECTED] = "1";
                }
                else
                {
                    wifiDetails[NetworkStatusAdapter.IS_CONNECTED] = "0";
                }
            }

            list.add(wifiDetails);
        }

        mNetworkStatusAdapter = new NetworkStatusAdapter(getActivity().getApplicationContext(), list);
        mListView.setAdapter(mNetworkStatusAdapter);

        updateInfoBar(list.size());
    }

    //create dialog to choice refresh interval
    private void createRefreshIntervalDialog()
    {
        final String[] stringsRefreshRateValues = getResources().getStringArray(R.array.pref_refresh_rate_values);
        final String[] stringsRefreshRateOptions = getResources().getStringArray(R.array.pref_refresh_rate_options);

        final AlertDialog.Builder alertDialog = new AlertDialog.Builder(getActivity());
        alertDialog.setTitle(R.string.ns_title_dialog_refresh_rate);

        alertDialog.setSingleChoiceItems(stringsRefreshRateOptions, 1,
                new DialogInterface.OnClickListener()
                {

                    @Override
                    public void onClick(DialogInterface dialog, int which)
                    {
                        refreshRateInSec = Integer.parseInt(stringsRefreshRateValues[which]);
                        refreshRate = 1000 * refreshRateInSec;

                        setParametersReceiverBefore();
                        setParametersReceiverAfter();
                        updateNetworkStatus();

                        dialog.dismiss();
                    }
                });

        alertDialog.show();

    }

    //enable wifi if is disabled
    private void enableWifi()
    {
        if (wifiManager.isWifiEnabled() == false)
        {
            wifiManager.setWifiEnabled(true);
        }
    }

    //update if AS FAST AS POSSIBLE
    private class WifiScanReceiver extends BroadcastReceiver
    {
        public void onReceive(Context c, Intent intent)
        {
            if( refreshRate == 0)
            {
                updateNetworkStatus();
            }

        }
    }

}
