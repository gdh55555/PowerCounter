package com.lab.powercounter.util;

/**
 * Created by Admin on 2016/7/5.
 */
public class SystemInfo {
    private static final String TAG = "SystemInfo";
    private static SystemInfo instance = new SystemInfo();

    public static SystemInfo getInstance() {
        return instance;
    }

    /* Uids as listed in android_filesystem_config.h */
    public static final int AID_ALL = -1; /* A special constant we will
                                                   * use to indicate a request
                                                   * for global information. */
    public static final int AID_ROOT = 0; /* traditional unix root user
                                                   */
    public static final int AID_SYSTEM = 1000; /* system server */
    public static final int AID_RADIO = 1001; /* telephony subsystem, RIL */
    public static final int AID_BLUETOOTH = 1002; /* bluetooth subsystem */
    public static final int AID_GRAPHICS = 1003; /* graphics devices */
    public static final int AID_INPUT = 1004; /* input devices */
    public static final int AID_AUDIO = 1005; /* audio devices */
    public static final int AID_CAMERA = 1006; /* camera devices */
    public static final int AID_LOG = 1007; /* log devices */
    public static final int AID_COMPASS = 1008; /* compass device */
    public static final int AID_MOUNT = 1009; /* mountd socket */
    public static final int AID_WIFI = 1010; /* wifi subsystem */
    public static final int AID_ADB = 1011; /* android debug bridge
                                                     (adbd) */
    public static final int AID_INSTALL = 1012; /* group for installing
                                                     packages */
    public static final int AID_MEDIA = 1013; /* mediaserver process */
    public static final int AID_DHCP = 1014; /* dhcp client */
    public static final int AID_SHELL = 2000; /* adb and debug shell user */
    public static final int AID_CACHE = 2001; /* cache access */
    public static final int AID_DIAG = 2002; /* access to diagnostic
                                                     resources */
    /* The 3000 series are intended for use as supplemental group id's only.
     * They indicate special Android capabilities that the kernel is aware of. */
    public static final int AID_NET_BT_ADMIN = 3001; /* bluetooth: create any
                                                     socket */
    public static final int AID_NET_BT = 3002; /* bluetooth: create sco,
                                                     rfcomm or l2cap sockets */
    public static final int AID_INET = 3003; /* can create AF_INET and
                                                     AF_INET6 sockets */
    public static final int AID_NET_RAW = 3004; /* can create raw INET sockets
                                                   */
    public static final int AID_MISC = 9998; /* access to misc storage */
    public static final int AID_NOBODY = 9999;
    public static final int AID_APP = 10000; /* first app user */
}
