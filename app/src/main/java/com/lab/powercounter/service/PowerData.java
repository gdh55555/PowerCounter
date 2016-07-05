package com.lab.powercounter.service;

import java.io.IOException;
import java.io.OutputStreamWriter;

/**
 * Created by Admin on 2016/7/5.
 */
public abstract class PowerData {
    private int cachedPower;

    public void setCachedPower(int cachedPower) {
        this.cachedPower = cachedPower;
    }

    public PowerData() {

    }

    public int getCachedPower() {
        return cachedPower;
    }

    public void recycle() {

    }

    public abstract void writeLogDataInfo(OutputStreamWriter out) throws IOException;
}
