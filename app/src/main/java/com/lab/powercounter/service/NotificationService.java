package com.lab.powercounter.service;

import android.os.Binder;
import android.os.IBinder;
import android.os.Parcel;
import android.os.RemoteException;
import android.util.Log;

import com.lab.powercounter.PowerNotifications;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.Vector;

/**
 * Created by Admin on 2016/7/8.
 */
public class NotificationService {
    private static final String TAG = "NotificationService";

    /* We haven't tried to install the hook yet. */
    private static final int STATE_INIT = 0;
    /* The hook was installed successfully and we should be receiving power
     * related notifications from the battery service.
     */
    private static final int STATE_HOOK_INSTALLED = 1;
    /* The hook failed to install.  This should be the case for most phones as a
     * hack is required to get this to work.
     */
    private static final int STATE_HOOK_FAILED = 2;

    private static int hookState = STATE_INIT;
    private static Binder notifier = new NotificationForwarder();
    private static Vector<PowerNotifications> hooks =
            new Vector<>();

    private static Method methodGetService;

    static {
        try {
            Class classServiceManager = Class.forName("android.os.ServiceManager");
            methodGetService = classServiceManager.getMethod("getService", String.class);
        } catch (NoSuchMethodException e) {
            Log.w(TAG, "Could not find method gerService");
        } catch (ClassNotFoundException e) {
            Log.w(TAG, "Could not find class android.os.ServiceManager");
        }
    }

    private static IBinder getBatteryService() {
        if (methodGetService == null) return null;
        try {
            return (IBinder) methodGetService.invoke(null, "batteryhook");
        } catch (InvocationTargetException e) {
            Log.w(TAG, "Call to get service failed");
        } catch (IllegalAccessException e) {
            Log.w(TAG, "Call to get service failed");
        }
        return null;
    }

    public static boolean available() {
        synchronized (hooks) {
            if (hookState == STATE_INIT) {
                return getBatteryService() != null;
            }
            return hookState == STATE_HOOK_INSTALLED;
        }
    }

    public static void addHook(PowerNotifications notif) {
        synchronized (hooks) {
            if (hookState == STATE_INIT) {
                installHook();
            }
            if (hookState != STATE_HOOK_INSTALLED) {
                Log.w(TAG, "Attempted to add hook though no " +
                        "notification service available");
            } else {
                hooks.add(notif);
            }
        }
    }

    public static void removeHook(PowerNotifications notif) {
        synchronized (hooks) {
            hooks.remove(notif);
        }
    }

    private static void installHook() {
        Parcel outBinder = Parcel.obtain();
        outBinder.writeStrongBinder(notifier);
        hookState = STATE_HOOK_FAILED;
        try {
            IBinder batteryHook = getBatteryService();
            if (batteryHook == null) {
            /* This should be the case on un-hacked phone.  Maybe one day
             * phones will support this service or similar by default.
             */
                Log.i(TAG, "No power notification hook service installed");
            } else if (!batteryHook.transact(0, outBinder, null, 0)) {
                Log.w(TAG, "Failed to register forwarder");
            } else {
                hookState = STATE_HOOK_INSTALLED;
            }
        } catch (RemoteException e) {
            Log.w(TAG, "Failed to register forwarder");
        }
        outBinder.recycle();
    }

    public static class DefaultReceiver extends PowerNotifications.Stub {
        public void noteSystemMediaCall(int uid) {
        }

        public void noteStartMedia(int uid, int id) {
        }

        public void noteStopMedia(int uid, int id) {
        }

        public void noteVideoSize(int uid, int id, int width, int height) {
        }

        public void noteStartWakelock(int uid, String name, int type) {
        }

        public void noteStopWakelock(int uid, String name, int type) {
        }

        public void noteStartSensor(int uid, int sensor) {
        }

        public void noteStopSensor(int uid, int sensor) {
        }

        public void noteStartGps(int uid) {
        }

        public void noteStopGps(int uid) {
        }

        public void noteScreenOn() {
        }

        public void noteScreenBrightness(int brightness) {
        }

        public void noteScreenOff() {
        }

        public void noteInputEvent() {
        }

        public void noteUserActivity(int uid, int event) {
        }

        public void notePhoneOn() {
        }

        public void notePhoneOff() {
        }

        public void notePhoneDataConnectionState(int dataType, boolean hasData) {
        }

        public void noteWifiOn(int uid) {
        }

        public void noteWifiOff(int uid) {
        }

        public void noteWifiRunning() {
        }

        public void noteWifiStopped() {
        }

        public void noteBluetoothOn() {
        }

        public void noteBluetoothOff() {
        }

        public void noteFullWifiLockAcquired(int uid) {
        }

        public void noteFullWifiLockReleased(int uid) {
        }

        public void noteScanWifiLockAcquired(int uid) {
        }

        public void noteScanWifiLockReleased(int uid) {
        }

        public void noteWifiMulticastEnabled(int uid) {
        }

        public void noteWifiMulticastDisabled(int uid) {
        }

        public void setOnBattery(boolean onBattery, int level) {
        }

        public void recordCurrentLevel(int level) {
        }

        public void noteVideoOn(int uid) {
        }

        public void noteVideoOff(int uid) {
        }

        public void noteAudioOn(int uid) {
        }

        public void noteAudioOff(int uid) {
        }
    }

    private static class NotificationForwarder extends DefaultReceiver {
        public boolean onTransact(int code, Parcel data,
                                  Parcel reply, int flags) throws RemoteException {
            synchronized (hooks) {
                for (Iterator<PowerNotifications> iter = hooks.iterator();
                     iter.hasNext(); ) {
                    Parcel junk = Parcel.obtain();
                    try {
                        iter.next().asBinder().transact(code, data, junk, flags);
                    } catch (RemoteException e) {
                        iter.remove();
                    }
                    data.setDataPosition(0);
                    junk.recycle();
                }
            }
            return super.onTransact(code, data, reply, flags);
        }
    }

    /* Useful for debugging purposes. */
    public static class PrintNotifications extends PowerNotifications.Stub {
        public void noteSystemMediaCall(int uid) {
            System.out.println("System media call[uid=" + uid + "]");
        }

        public void noteStartMedia(int uid, int id) {
            System.out.println("Start media[uid=" + uid + ", id=" + id + "]");
        }

        public void noteStopMedia(int uid, int id) {
            System.out.println("Stop media[uid=" + uid + ", id=" + id + "]");
        }

        public void noteVideoSize(int uid, int id, int width, int height) {
            System.out.println("Video size[uid=" + uid + ", id=" + id +
                    ", width=" + width + ", height=" + height + "]");
        }

        public void noteStartWakelock(int uid, String name, int type) {
            System.out.println("Start wakelock[uid=" + uid + ", name=" + name +
                    ", type=" + type + "]");
        }

        public void noteStopWakelock(int uid, String name, int type) {
            System.out.println("Stop wakelock[uid=" + uid + ", name=" + name +
                    ", type=" + type + "]");
        }

        public void noteStartSensor(int uid, int sensor) {
            System.out.println("noteStartSensor[uid=" + uid + ", sensor=" + sensor +
                    "]");
        }

        public void noteStopSensor(int uid, int sensor) {
            System.out.println("noteStopSensor[uid=" + uid + ", sensor=" + sensor +
                    "]");
        }

        public void noteStartGps(int uid) {
            System.out.println("noteStartGps[uid=" + uid + "]");
        }

        public void noteStopGps(int uid) {
            System.out.println("noteStopGps[uid=" + uid + "]");
        }

        public void noteScreenOn() {
            System.out.println("noteScreenOn");
        }

        public void noteScreenBrightness(int brightness) {
            System.out.println("noteScreenBrightness[brightness=" + brightness + "]");
        }

        public void noteScreenOff() {
            System.out.println("noteScreenOff");
        }

        public void noteInputEvent() {
            System.out.println("noteInputEvent");
        }

        public void noteUserActivity(int uid, int event) {
            System.out.println("noteUserActivity[uid=" + uid + ", event=" + event +
                    "]");
        }

        public void notePhoneOn() {
            System.out.println("notePhoneOn");
        }

        public void notePhoneOff() {
            System.out.println("notePhoneOff");
        }

        public void notePhoneDataConnectionState(int dataType, boolean hasData) {
            System.out.println("notePhoneDataConnectionState[dataType=" + dataType +
                    ", hasData=" + hasData + "]");
        }

        public void notePhoneState(int phoneState) {
            System.out.println("notePhoneState[phoneState=" + phoneState + "]");
        }

        public void noteWifiOn(int uid) {
            System.out.println("noteWifiOn[uid=" + uid + "]");
        }

        public void noteWifiOff(int uid) {
            System.out.println("noteWifiOff[uid=" + uid + "]");
        }

        public void noteWifiRunning() {
            System.out.println("noteWifiRunning");
        }

        public void noteWifiStopped() {
            System.out.println("noteWifiStopped");
        }

        public void noteBluetoothOn() {
            System.out.println("noteBluetoothOn");
        }

        public void noteBluetoothOff() {
            System.out.println("noteBluetoothOff");
        }

        public void noteFullWifiLockAcquired(int uid) {
            System.out.println("noteFullWifiLockAcquired[uid=" + uid + "]");
        }

        public void noteFullWifiLockReleased(int uid) {
            System.out.println("noteFullWifiLockReleased[uid=" + uid + "]");
        }

        public void noteScanWifiLockAcquired(int uid) {
            System.out.println("noteScanWifiLockAcquired[uid=" + uid + "]");
        }

        public void noteScanWifiLockReleased(int uid) {
            System.out.println("noteScanWifiLockReleased[uid=" + uid + "]");
        }

        public void noteWifiMulticastEnabled(int uid) {
            System.out.println("noteWifiMulticastEnabled[uid=" + uid + "]");
        }

        public void noteWifiMulticastDisabled(int uid) {
            System.out.println("noteWifiMulticastDisabled[uid=" + uid + "]");

        }

        public void setOnBattery(boolean onBattery, int level) {
            System.out.println("setOnBattery[onBattery=" + onBattery + ", level=" +
                    level + "]");
        }

        public void recordCurrentLevel(int level) {
            System.out.println("recordCurrentLevel[level=" + level + "]");
        }

        public void noteVideoOn(int uid) {
            System.out.println("noteVideoOn[uid=" + uid + "]");
        }

        public void noteVideoOff(int uid) {
            System.out.println("noteVideoOff[uid=" + uid + "]");
        }

        public void noteAudioOn(int uid) {
            System.out.println("noteAudioOn[uid=" + uid + "]");
        }

        public void noteAudioOff(int uid) {
            System.out.println("noteAudioOff[uid=" + uid + "]");
        }
    }
}
