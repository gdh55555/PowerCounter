package com.lab.powercounter.component;

import android.util.Log;
import android.util.SparseArray;

import com.lab.powercounter.phone.PhoneConstants;
import com.lab.powercounter.service.IterationData;
import com.lab.powercounter.service.PowerData;
import com.lab.powercounter.util.Recycler;
import com.lab.powercounter.util.SystemInfo;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;

/**
 * Created by Admin on 2016/7/5.
 */
public class CPU extends PowerComponent {

    public static class CpuData extends PowerData {
        private static Recycler<CpuData> recycler = new Recycler<>();

        public static CpuData obtain() {
            CpuData result = recycler.obtain();
            if (result != null)
                return result;
            return new CpuData();
        }

        public void recycle() {
            recycler.recycle(this);
        }

        public double sysPerc;      //Sys time
        public double usrPerc;      //User time
        public double freq;

        private CpuData() {

        }

        public void init(double sysPerc, double usrPerc, double freq) {
            this.sysPerc = sysPerc;
            this.usrPerc = usrPerc;
            this.freq = freq;
        }

        @Override
        public void writeLogDataInfo(OutputStreamWriter out) throws IOException {
            StringBuilder res = new StringBuilder();
            res.append("CPU-sys ").append((long) Math.round(sysPerc))
                    .append("\nCPU-usr ").append((long) Math.round(usrPerc))
                    .append("\nCPU-freq ").append(freq)
                    .append("\n");
            out.write(res.toString());
        }
    }

    private static final String TAG = "CPU";
    private static final String CPU_FREQ_FILE = "/proc/cpuinfo";
    private static final String STAT_FILE = "/proc/stat";

    private CpuStateKeeper cpuState;
    private SparseArray<CpuStateKeeper> pidStates;
    private SparseArray<CpuStateKeeper> uidLinks;

    private int[] pids;
    private long[] statsBuf;

    private PhoneConstants constants;

    public CPU(PhoneConstants constants) {
        this.constants = constants;
        cpuState = new CpuStateKeeper(SystemInfo.AID_ALL);
        pidStates = new SparseArray<>();
        uidLinks = new SparseArray<>();
        statsBuf = new long[7];
    }


    @Override
    public IterationData calculateIteration(long iteration) {
        IterationData result = IterationData.obtain();
        SystemInfo sysInfo = SystemInfo.getInstance();
        double freq = readCpuFreq(sysInfo);
        if (freq < 0) {
            Log.w(TAG, "Failed to read cpu frequency");
            return result;
        }
        if (!sysInfo.getUsrSysTotalTime(statsBuf)) {
            Log.w(TAG, "Failed to read cpu times");
            return result;
        }
        long usrTime = statsBuf[SystemInfo.INDEX_USER_TIME];
        long sysTime = statsBuf[SystemInfo.INDEX_SYS_TIME];
        long totalTime = statsBuf[SystemInfo.INDEX_TOTAL_TIME];

        boolean init = cpuState.isInitialized();

        cpuState.updateState(usrTime, sysTime, totalTime, iteration);
        if (init) {
            CpuData data = CpuData.obtain();
            data.init(cpuState.getUsrPerc(), cpuState.getSysPerc(), freq);
            result.setPowerData(data);
        }
        uidLinks.clear();
        pids = sysInfo.getPids(pids);
        int pidInd = 0;

        if (pids != null) {
            for (int pid : pids) {
                if (pid < 0)
                    break;
                CpuStateKeeper pidState;
                if (pidInd < pidStates.size() && pidStates.keyAt(pidInd) == pid) {
                    pidState = pidStates.valueAt(pidInd);
                } else {
                    int uid = sysInfo.getUidForPid(pid);
                    if (uid >= 0) {
                        pidState = new CpuStateKeeper(uid);
                        pidStates.put(pid, pidState);
                    } else {
                        continue;
                    }
                }
                pidInd++;
                if (!pidState.isStale(iteration)) {
                    /* Nothing much is going on with this pid recently.  We'll just
                     * assume that it's not using any of the cpu for this iteration.
                    */
                    pidState.updateIteration(iteration, totalTime);
                } else if (sysInfo.getPidUsrSysTime(pid, statsBuf)) {
                    usrTime = statsBuf[SystemInfo.INDEX_USER_TIME];
                    sysTime = statsBuf[SystemInfo.INDEX_SYS_TIME];

                    init = pidState.isInitialized();
                    pidState.updateState(usrTime, sysTime, totalTime, iteration);
                    if (!init) {
                        continue;
                    }

                }
                CpuStateKeeper linkState = uidLinks.get(pidState.getUid());
                if (linkState == null) {
                    uidLinks.put(pidState.getUid(), pidState);
                } else {
                    linkState.absorb(pidState);
                }
            }
        }

        /* Remove processes that are no longer active. */
        for (int i = 0; i < pidStates.size(); i++) {
            if (!pidStates.valueAt(i).isAlive(iteration)) {
                pidStates.remove(pidStates.keyAt(i--));
            }
        }
        /* Collect the summed uid information. */
        for (int i = 0; i < uidLinks.size(); i++) {
            int uid = uidLinks.keyAt(i);
            CpuStateKeeper linkState = uidLinks.valueAt(i);

            CpuData uidData = CpuData.obtain();
            predictAppUidState(uidData, linkState.getUsrPerc(),
                    linkState.getSysPerc(), freq);
            result.addUidPowerData(uid, uidData);
        }

        return result;
    }


    /* This is the function that is responsible for predicting the cpu frequency
   * state of the individual uid as though it were the only thing running.  It
   * simply is finding the lowest frequency that keeps the cpu usage under
   * 70% assuming there is a linear relationship to the cpu utilization at
   * different frequencies.
   */
    private void predictAppUidState(CpuData uidData, double usrPerc,
                                    double sysPerc, double freq) {
        double[] freqs = constants.cpuFreqs();
        if (usrPerc + sysPerc < 1e-6) {
      /* Don't waste time with the binary search if there is no utilization
       * which will be the case a lot.
       */
            uidData.init(sysPerc, usrPerc, freqs[0]);
            return;
        }
        int lo = 0;
        int hi = freqs.length - 1;
        double perc = sysPerc + usrPerc;
        while (lo < hi) {
            int mid = (lo + hi) / 2;
            double nperc = perc * freq / freqs[mid];
            if (nperc < 70) {
                hi = mid;
            } else {
                lo = mid + 1;
            }
        }
        uidData.init(sysPerc * freq / freqs[lo], usrPerc * freq / freqs[lo],
                freqs[lo]);
    }
    
    @Override
    public String getComponentName() {
        return "CPU";
    }


    private static class CpuStateKeeper {
        private int uid;
        private long iteration;
        private long lastUpdateIteration;
        private long inactiveIterations;

        private long lastUsr;
        private long lastSys;
        private long lastTotal;

        private long sumUsr;
        private long sumSys;
        private long deltaTotal;

        private CpuStateKeeper(int uid) {
            this.uid = uid;
            lastUsr = lastSys = -1;
            lastUpdateIteration = iteration = -1;
            inactiveIterations = 0;
        }

        public void updateIteration(long iteration, long totalTime) {
          /* Process is still running but actually reading the cpu utilization has
           * been skipped this iteration to avoid wasting cpu cycles as this process
           * has not been very active recently. */
            sumUsr = 0;
            sumSys = 0;
            deltaTotal = totalTime - lastTotal;
            if (deltaTotal < 1) deltaTotal = 1;
            lastTotal = totalTime;
            this.iteration = iteration;
        }

        public void updateState(long usrTime, long sysTime, long totalTime,
                                long iteration) {
            sumUsr = usrTime - lastUsr;
            sumSys = sysTime - lastSys;
            deltaTotal = totalTime - lastTotal;
            if (deltaTotal < 1) deltaTotal = 1;
            lastUsr = usrTime;
            lastSys = sysTime;
            lastTotal = totalTime;
            lastUpdateIteration = this.iteration = iteration;

            if (getUsrPerc() + getSysPerc() < 0.1) {
                inactiveIterations++;
            } else {
                inactiveIterations = 0;
            }
        }

        public boolean isInitialized() {
            return lastUsr != -1;
        }

        public int getUid() {
            return uid;
        }

        public void absorb(CpuStateKeeper s) {
            sumUsr += s.sumUsr;
            sumSys += s.sumSys;
        }

        public double getUsrPerc() {
            return 100.0 * sumUsr / Math.max(sumUsr + sumSys, deltaTotal);
        }

        public double getSysPerc() {
            return 100.0 * sumSys / Math.max(sumUsr + sumSys, deltaTotal);
        }

        public boolean isAlive(long iteration) {
            return this.iteration == iteration;
        }

        public boolean isStale(long iteration) {
            return 1L << (iteration - lastUpdateIteration) >
                    inactiveIterations * inactiveIterations;
        }
    }

    /* Returns the frequency of the processor in Mhz.  If the frequency cannot
    * be determined returns a negative value instead.
    */
    private double readCpuFreq(SystemInfo sysInfo) {
    /* Try to read from the /sys/devices file first.  If that doesn't work
     * try manually inspecting the /proc/cpuinfo file.
     */
        long cpuFreqKhz = sysInfo.readLongFromFile(
                "/sys/devices/system/cpu/cpu0/cpufreq/scaling_cur_freq");
        if (cpuFreqKhz != -1) {
            return cpuFreqKhz / 1000.0;
        }

        FileReader fstream;
        try {
            fstream = new FileReader(CPU_FREQ_FILE);
        } catch (FileNotFoundException e) {
            Log.w(TAG, "Could not read cpu frequency file");
            return -1;
        }
        BufferedReader in = new BufferedReader(fstream, 500);
        String line;
        try {
            while ((line = in.readLine()) != null) {
                if (line.startsWith("BogoMIPS")) {
                    return Double.parseDouble(line.trim().split("[ :]+")[1]);
                }
            }
        } catch (IOException e) {
      /* Failed to read from the cpu freq file. */
        } catch (NumberFormatException e) {
      /* Frequency not formatted properly as a double. */
        }
        Log.w(TAG, "Failed to read cpu frequency");
        return -1;
    }
}
