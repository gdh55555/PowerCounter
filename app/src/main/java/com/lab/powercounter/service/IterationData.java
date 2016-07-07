package com.lab.powercounter.service;

import android.util.SparseArray;

import com.lab.powercounter.util.Recycler;
import com.lab.powercounter.util.SystemInfo;

/**
 * Created by Admin on 2016/7/5.
 */
public class IterationData {
    private static Recycler<IterationData> recycler =
            new Recycler<>();

    private SparseArray<PowerData> uidPower;

    public static IterationData obtain() {
        IterationData result = recycler.obtain();
        if (result != null) return result;
        return new IterationData();
    }

    private IterationData() {
        uidPower = new SparseArray<>();
    }

    public void init() {
        uidPower.clear();
    }

    public void recycle() {
        for (int i = 0; i < uidPower.size(); ++i)
            uidPower.valueAt(i).recycle();
        uidPower.clear();
        recycler.recycle(this);
    }

    public void setPowerData(PowerData power) {
        addUidPowerData(SystemInfo.AID_ALL, power);
    }

    public void

    addUidPowerData(int uid, PowerData power) {
        uidPower.put(uid, power);
    }

    public SparseArray<PowerData> getUidPowerData() {
        return uidPower;
    }
}
