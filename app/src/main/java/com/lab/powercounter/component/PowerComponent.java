package com.lab.powercounter.component;

import android.os.Process;
import android.util.Log;

import com.lab.powercounter.service.IterationData;

/**
 * Created by Admin on 2016/7/5.
 */
public abstract class PowerComponent extends Thread {
    private final String TAG = "PowerComponent";

    /**
     * 需要重写， 周期性统计组件电量
     *
     * @param iteration
     * @return
     */
    public abstract IterationData calculateIteration(long iteration);

    /**
     * 获得组件名称
     *
     * @return
     */
    public abstract String getComponentName();

    /* 如果可以统计uid信息，则返回true */
    public boolean hasUidInformation() {
        return false;
    }

    protected void onExit() {
    }

    private IterationData data1;
    private IterationData data2;
    private long iteration1;
    private long iteration2;

    protected long beginTime;
    protected long iterationInterval;

    public PowerComponent() {
        setDaemon(true);
    }

    public void init(long beginTime, long iterationInterval) {
        this.beginTime = beginTime;
        this.iterationInterval = iterationInterval;
        data1 = data2 = null;
        iteration1 = iteration2 = -1;
    }

    public void run() {
        android.os.Process.setThreadPriority(Process.THREAD_PRIORITY_MORE_FAVORABLE);
        for (long iter = 0; !Thread.interrupted(); ) {
            IterationData data = calculateIteration(iter);
            if (data != null) {
                synchronized (this) {
                    if (iteration1 < iteration2) {
                        iteration1 = iter;
                        data1 = data;
                    } else {
                        iteration2 = iter;
                        data2 = data;
                    }
                }
            }
            if (interrupted())
                break;

            long curTime = System.currentTimeMillis();
            long oldIter = iter;

            iter = Math.max(iter + 1, 1 + (curTime - beginTime) / iterationInterval);
            if (oldIter + 1 != iter) {
                Log.w(TAG, "[" + getComponentName() + "] Had to skip from iteration " +
                        oldIter + " to " + iter);
            }
            try {
                sleep(beginTime + iter * iterationInterval - curTime);
            } catch (InterruptedException e) {
                break;
            }
        }
        onExit();
    }
}
