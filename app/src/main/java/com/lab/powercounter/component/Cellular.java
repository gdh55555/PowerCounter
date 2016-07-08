package com.lab.powercounter.component;

import android.content.Context;
import android.net.TrafficStats;
import android.os.SystemClock;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.util.SparseArray;

import com.lab.powercounter.phone.PhoneConstants;
import com.lab.powercounter.service.IterationData;
import com.lab.powercounter.service.PowerData;
import com.lab.powercounter.util.Recycler;
import com.lab.powercounter.util.SystemInfo;

import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;

/**
 * Created by Admin on 2016/7/7.
 */
public class Cellular extends PowerComponent {

    private static final String TAG = "Cellular";

    public static class CellularData extends PowerData {
        private static Recycler<CellularData> recycler = new Recycler<>();

        public static CellularData obtain() {
            CellularData result = recycler.obtain();
            if (result != null) return result;
            return new CellularData();
        }

        @Override
        public void recycle() {
            recycler.recycle(this);
        }

        public boolean cellularOn;
        public long packets;
        public long uplinkBytes;
        public long downlinkBytes;
        public int powerState;
        public String oper;

        private CellularData() {
        }

        public void init() {
            cellularOn = false;
        }

        public void init(long packets, long uplinkBytes, long downlinkBytes,
                         int powerState, String oper) {
            cellularOn = true;
            this.packets = packets;
            this.uplinkBytes = uplinkBytes;
            this.downlinkBytes = downlinkBytes;
            this.powerState = powerState;
            this.oper = oper;
        }

        public void writeLogDataInfo(OutputStreamWriter out) throws IOException {
            StringBuilder res = new StringBuilder();
            res.append("Cellular-on ").append(cellularOn).append("\n");
            if (cellularOn) {
                res.append("Cellular-uplinkBytes ").append(uplinkBytes)
                        .append("\nCellular-downlinkBytes ").append(downlinkBytes)
                        .append("\nCellular-packets ").append(packets)
                        .append("\nCellular-state ").append(Cellular.POWER_STATE_NAMES[powerState])
                        .append("\nCellular-oper ").append(oper)
                        .append("\n");
            }
            out.write(res.toString());
        }
    }

    public static final int POWER_STATE_IDLE = 0;
    public static final int POWER_STATE_FACH = 1;
    public static final int POWER_STATE_DCH = 2;
    public static final String[] POWER_STATE_NAMES = {"IDLE", "FACH", "DCH"};

    private PhoneConstants phoneConstants;
    private TelephonyManager telephonyManager;
    private SystemInfo sysInfo;

    private String oper;
    private int dchFachDelay;
    private int fachIdleDelay;
    private int uplinkQueueSize;
    private int downlinkQueueSize;

    private int[] lastUids;
    private CellularStateKeeper cellularState;
    private SparseArray<CellularStateKeeper> uidStates;

    private String transPacketsFile;
    private String readPacketsFile;
    private String readBytesFile;
    private String transBytesFile;
    private File uidStatsFolder;

    public Cellular(Context context, PhoneConstants phoneConstants) {
        this.phoneConstants = phoneConstants;
        telephonyManager = (TelephonyManager) context.getSystemService(
                Context.TELEPHONY_SERVICE);

        String interfaceName = phoneConstants.cellularInterface();
        cellularState = new CellularStateKeeper();
        uidStates = new SparseArray<>();
        transPacketsFile = "/sys/class/net/" +
                interfaceName + "/statistics/tx_packets";
        readPacketsFile = "/sys/class/net/" +
                interfaceName + "/statistics/rx_packets";
        readBytesFile = "/sys/class/net/" +
                interfaceName + "/statistics/rx_bytes";
        transBytesFile = "/sys/class/net/" +
                interfaceName + "/statistics/tx_bytes";
        uidStatsFolder = new File("/proc/uid_stat");
        sysInfo = SystemInfo.getInstance();
    }


    @Override
    public IterationData calculateIteration(long iteration) {
        IterationData result = IterationData.obtain();

        int netType = telephonyManager.getNetworkType();

        if ((netType != TelephonyManager.NETWORK_TYPE_UMTS &&
                netType != 8/* TelephonyManager.NETWORK_TYPE_HSDPA */)) {
            // TODO: Actually get models for the different network types.
            netType = TelephonyManager.NETWORK_TYPE_UMTS;
        }

        if (telephonyManager.getDataState() != TelephonyManager.DATA_CONNECTED ||
                (netType != TelephonyManager.NETWORK_TYPE_UMTS &&
                        netType != 8/* TelephonyManager.NETWORK_TYPE_HSDPA */)) {
          /* We need to allow the real iterface state keeper to reset it's state
           * so that the next update it knows it's coming back from an off state.
           * We also need to clear all the uid information.
           */
            oper = null;
            cellularState.interfaceOff();
            uidStates.clear();

            CellularData data = CellularData.obtain();
            data.init();
            result.setPowerData(data);
            return result;
        }

        if (oper == null) {
            oper = telephonyManager.getNetworkOperatorName();
            dchFachDelay = phoneConstants.cellularDchFachDelay(oper);
            fachIdleDelay = phoneConstants.cellularFachIdleDelay(oper);
            uplinkQueueSize = phoneConstants.cellularUplinkQueue(oper);
            downlinkQueueSize = phoneConstants.cellularDownlinkQueue(oper);
        }
        long transmitPackets;
        long receivePackets;
        long transmitBytes;
        long receiveBytes;
        if (new File(transPacketsFile).exists()) {
            transmitPackets = readLongFromFile(transPacketsFile);
            receivePackets = readLongFromFile(readPacketsFile);
            transmitBytes = readLongFromFile(transBytesFile);
            receiveBytes = readLongFromFile(readBytesFile);
        } else {
            transmitPackets = TrafficStats.getMobileTxPackets();
            receivePackets = TrafficStats.getMobileRxPackets();
            transmitBytes = TrafficStats.getMobileTxBytes();
            receiveBytes = TrafficStats.getMobileRxBytes();
        }
        if (transmitBytes == -1 || receiveBytes == -1) {
            /* Couldn't read interface data files. */
            Log.w(TAG, "Failed to read packet and byte counts from wifi interface");
            return result;
        }

        if (cellularState.isInitialized()) {
            cellularState.updateState(transmitPackets, receivePackets,
                    transmitBytes, receiveBytes,
                    dchFachDelay, fachIdleDelay,
                    uplinkQueueSize, downlinkQueueSize);
            CellularData data = CellularData.obtain();
            data.init(cellularState.getPackets(), cellularState.getUplinkBytes(),
                    cellularState.getDownlinkBytes(), cellularState.getPowerState(),
                    oper);
            result.setPowerData(data);
        } else {
            cellularState.updateState(transmitPackets, receivePackets,
                    transmitBytes, receiveBytes,
                    dchFachDelay, fachIdleDelay,
                    uplinkQueueSize, downlinkQueueSize);
        }

        lastUids = sysInfo.getUids(lastUids);
        if (lastUids != null) for (int uid : lastUids) {
            if (uid == -1) {
                continue;
            }
            try {
                CellularStateKeeper uidState = uidStates.get(uid);
                if (uidState == null) {
                    uidState = new CellularStateKeeper();
                    uidStates.put(uid, uidState);
                }

                if (!uidState.isStale()) {
                  /* We use a huerstic here so that we don't poll for uids that haven't
                   * had much activity recently.
                   */
                    continue;
                }

                /* These read operations are the expensive part of polling. */
                String tcp_rcvFile = "/proc/uid_stat/" + uid + "/tcp_rcv";
                if (new File(tcp_rcvFile).exists()) {
//                    receiveBytes = readLongFromFile("/proc/uid_stat/" + uid + "/tcp_rcv");
                    receiveBytes = readLongFromFile(tcp_rcvFile);
//                    transmitBytes = readLongFromFile("/proc/uid_stat/" + uid + "/tcp_snd");
                    transmitBytes = readLongFromFile(tcp_rcvFile);
                } else {
                    receiveBytes = TrafficStats.getUidRxBytes(uid);
                    transmitBytes = TrafficStats.getUidRxBytes(uid);
                }
                ;

                if (receiveBytes == -1 || transmitBytes == -1) {
                    Log.w(TAG, "Failed to read uid read/write byte counts");
                } else if (uidState.isInitialized()) {
                    uidState.updateState(-1, -1, transmitBytes, receiveBytes,
                            dchFachDelay, fachIdleDelay,
                            uplinkQueueSize, downlinkQueueSize);

                    if (uidState.getUplinkBytes() + uidState.getDownlinkBytes() != 0 ||
                            uidState.getPowerState() != POWER_STATE_IDLE) {
                        CellularData uidData = CellularData.obtain();
                        uidData.init(uidState.getPackets(),
                                uidState.getUplinkBytes(), uidState.getDownlinkBytes(),
                                uidState.getPowerState(), oper);
                        result.addUidPowerData(uid, uidData);
                    }
                } else {
                    uidState.updateState(-1, -1, transmitBytes, receiveBytes,
                            dchFachDelay, fachIdleDelay,
                            uplinkQueueSize, downlinkQueueSize);
                }
            } catch (NumberFormatException e) {
                Log.w(TAG, "Non-uid files in /proc/uid_stat");
            }
        }

        return result;
    }

    @Override
    public String getComponentName() {
        return "Cellular";
    }

    private static class CellularStateKeeper {
        private long lastTransmitPackets;
        private long lastReceivePackets;
        private long lastTransmitBytes;
        private long lastReceiveBytes;
        private long lastTime;

        private long deltaPackets;
        private long deltaUplinkBytes;
        private long deltaDownlinkBytes;

        private int powerState;
        private int stateTime;

        private long inactiveTime;

        public CellularStateKeeper() {
            lastTransmitBytes = lastReceiveBytes = lastTime = -1;
            deltaUplinkBytes = deltaDownlinkBytes = -1;
            powerState = POWER_STATE_IDLE;
            stateTime = 0;
            inactiveTime = 0;
        }

        public void interfaceOff() {
            lastTime = SystemClock.elapsedRealtime();
            powerState = POWER_STATE_IDLE;
        }

        public boolean isInitialized() {
            return lastTime != -1;
        }

        public void updateState(long transmitPackets, long receivePackets,
                                long transmitBytes, long receiveBytes,
                                int dchFachDelay, int fachIdleDelay,
                                int uplinkQueueSize, int downlinkQueueSize) {
            long curTime = SystemClock.elapsedRealtime();
            if (lastTime != -1 && curTime > lastTime) {
                double deltaTime = curTime - lastTime;
                deltaPackets = transmitPackets + receivePackets -
                        lastTransmitPackets - lastReceivePackets;
                deltaUplinkBytes = transmitBytes - lastTransmitBytes;
                deltaDownlinkBytes = receiveBytes - lastReceiveBytes;
                boolean inactive = deltaUplinkBytes == 0 && deltaDownlinkBytes == 0;
                inactiveTime = inactive ? inactiveTime + curTime - lastTime : 0;

                // TODO: make this always work.
                int timeMult = 1;
                if (1000 % PowerEstimator.ITERATION_INTERVAL != 0) {
                    Log.w(TAG,
                            "Cannot handle iteration intervals that are a factor of 1 second");
                } else {
                    timeMult = 1000 / PowerEstimator.ITERATION_INTERVAL;
                }

                switch (powerState) {
                    case POWER_STATE_IDLE:
                        if (!inactive) {
                            powerState = POWER_STATE_FACH;
                        }
                        break;
                    case POWER_STATE_FACH:
                        if (inactive) {
                            stateTime++;
                            if (stateTime >= fachIdleDelay * timeMult) {
                                stateTime = 0;
                                powerState = POWER_STATE_IDLE;
                            }
                        } else {
                            stateTime = 0;
                            if (deltaUplinkBytes > 0 ||
                                    deltaDownlinkBytes > 0) {
                                powerState = POWER_STATE_DCH;
                            }
                        }
                        break;
                    default: // case POWER_STATE_DCH:
                        if (inactive) {
                            stateTime++;
                            if (stateTime >= dchFachDelay * timeMult) {
                                stateTime = 0;
                                powerState = POWER_STATE_FACH;
                            }
                        } else {
                            stateTime = 0;
                        }
                }
            }
            lastTime = curTime;
            lastTransmitPackets = transmitPackets;
            lastReceivePackets = receivePackets;
            lastTransmitBytes = transmitBytes;
            lastReceiveBytes = receiveBytes;
        }

        public int getPowerState() {
            return powerState;
        }

        public long getPackets() {
            return deltaPackets;
        }

        public long getUplinkBytes() {
            return deltaUplinkBytes;
        }

        public long getDownlinkBytes() {
            return deltaDownlinkBytes;
        }

        /* The idea here is that we don't want to have to read uid information
         * every single iteration for each uid as it just takes too long.  So here
         * we are designing a hueristic that helps us avoid polling for too many
         * uids.
         */
        public boolean isStale() {
            if (powerState != POWER_STATE_IDLE) return true;
            long curTime = SystemClock.elapsedRealtime();
            return curTime - lastTime > (long) Math.min(10000, inactiveTime);
        }
    }

    private final static byte[] buf = new byte[16];

    private long readLongFromFile(String filePath) {
        return sysInfo.readLongFromFile(filePath);
    }

    @Override
    public boolean hasUidInformation() {
        return uidStatsFolder.exists();
    }

}
