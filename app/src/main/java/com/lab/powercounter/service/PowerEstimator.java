package com.lab.powercounter.service;

import android.content.SharedPreferences;

import com.lab.powercounter.component.PowerComponent;
import com.lab.powercounter.phone.PowerFunction;

import java.util.Map;
import java.util.Vector;

/**
 * Created by Admin on 2016/7/15.
 */
public class PowerEstimator implements Runnable {
    private static final String TAG = "PowerEstimator";

    private static final String DEFLATE_DICTIONARY =
            "onoffidleoff-hookringinglowairplane-modebatteryedgeGPRS3Gunknown" +
                    "in-serviceemergency-onlyout-of-servicepower-offdisconnectedconnecting" +
                    "associateconnectedsuspendedphone-callservicenetworkbegin.0123456789" +
                    "GPSAudioWifi3GLCDCPU-power ";

    public static final int ALL_COMPONENTS = -1;
    public static final int ITERATION_INTERVAL = 1000; // 1 second

    private LoggerService context;
    private SharedPreferences prefs;
    private boolean plugged;

    private Vector<PowerComponent> powerComponents;
    private Vector<PowerFunction> powerFunctions;
    //    private Vector<HistoryBuffer> histories;
    private Map<Integer, String> uidAppIds;

    @Override
    public void run() {

    }
}
