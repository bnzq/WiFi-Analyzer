package com.example.dominik.wifi_analyzer;

import android.graphics.Color;
import android.net.wifi.WifiManager;

import java.util.Random;

public class Utility
{

    public static int convertFrequencyToChannel(int freq)
    {
        if (freq >= 2412 && freq <= 2484)
        {
            return (freq - 2412) / 5 + 1;
        }
        else if (freq >= 5170 && freq <= 5825)
        {
            return (freq - 5170) / 5 + 34;
        }
        else
        {
            return -1;
        }
    }

    public static int convertRssiToQuality(int dBm)
    {

        if( dBm <= - 100)
        {
           return 0;
        }
        else if (dBm == 0)
        {
            return 0;
        }
        else if (dBm >= -50)
        {
            return  100;
        }
        else
        {
            return  2 * (dBm + 100);
        }
    }

    public static int convertRssiToQualityWithSub(int dBm, int sub)
    {
        int dBmFirst;

        if( dBm <= - 100)
        {
            dBmFirst = 0;
        }
        else if (dBm == 0)
        {
            dBmFirst = 0;
        }
        else if (dBm >= -50)
        {
            dBmFirst =  100;
        }
        else
        {
            dBmFirst = 2 * (dBm + 100);
        }

        dBmFirst -= sub;

        if (dBmFirst < 0)
            return 0;
        else
            return dBmFirst;
    }

    public static int convertQualityToStepsQuality(int quality, int steps)
    {
        int a = 100 / steps;

        for (int i = 0; i < steps; i++)
        {
            if(quality >= a * i && quality < a * (i + 1))
            {
                return i + 1;
            }
        }

        return -1;
    }

    public static String getEncryptionFromCapabilities(String cap)
    {
        final String WEP = "WEP";
        final String WPA = "WPA";
        final String WPA2 = "WPA2";
        final String OPEN = "Open WiFi";

        String result = "";

        if (cap.toUpperCase().contains(WEP.toUpperCase()))
            result += WEP;

        if (cap.toUpperCase().contains(WPA.toUpperCase()))
        {
            if (!result.equals(""))
                result += " / ";

            result += WPA;
        }

        if (cap.toUpperCase().contains(WPA2.toUpperCase()))
        {
            if (!result.equals(""))
                result += " / ";

            result += WPA2;
        }

        if (result.equals(""))
            return OPEN;
        else
            return result;
    }

    public static int randColor()
    {
        Random rand = new Random();
        float[] hsv = new float[3];
        Color.RGBToHSV(rand.nextInt(255), rand.nextInt(255),rand.nextInt(255), hsv);
        return Color.HSVToColor(hsv);
    }

    public static void enableWifi(WifiManager wifiManager)
    {
        if (wifiManager.isWifiEnabled() == false)
        {
            wifiManager.setWifiEnabled(true);
        }
    }
}
