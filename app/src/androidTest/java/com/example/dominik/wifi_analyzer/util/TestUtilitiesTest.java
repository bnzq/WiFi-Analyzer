package com.example.dominik.wifi_analyzer.util;

import com.example.dominik.wifi_analyzer.Utility;

import junit.framework.TestCase;

public class TestUtilitiesTest extends TestCase
{
    public void setUp() throws Exception
    {
        super.setUp();
    }

    public void testMyUtil()
    {
        int result = Utility.convertQualityToStepsQuality(100,8);
        assertTrue("Result is not in channels range ! ", result != -1);
        result = Utility.convertQualityToStepsQuality(100, 5);
        assertTrue("Result is not in channels range ! ", result != -1);
        result = Utility.convertQualityToStepsQuality(100, -2);
        assertTrue("Result is not in channels range ! ", result == -1);
        result = Utility.convertQualityToStepsQuality(-100, 5);
        assertTrue("Result is not in channels range ! ", result == -1);
        result = Utility.convertQualityToStepsQuality(100, 1200);
        assertTrue("Result is not in channels range ! ", result != -1);
        result = Utility.convertQualityToStepsQuality(0, 0);
        assertTrue("Result is not in channels range ! ", result == -1);
        result = Utility.convertQualityToStepsQuality(0, 1);
        assertTrue("Result is not in channels range ! ", result == 1);
        result = Utility.convertQualityToStepsQuality(1, 0);
        assertTrue("Result is not in channels range ! ", result == -1);
        result = Utility.convertQualityToStepsQuality(51, 2);
        assertTrue("Result is not in channels range ! ", result == 2);
        result = Utility.convertQualityToStepsQuality(49, 2);
        assertTrue("Result is not in channels range ! ", result == 1);
    }
}