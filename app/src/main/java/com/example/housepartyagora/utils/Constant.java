package com.example.housepartyagora.utils;

import io.agora.rtc2.RtcEngine;

public class Constant {

    public static final String MEDIA_SDK_VERSION;
    static {
        String sdk = "undefined";
        try {
            sdk = RtcEngine.getSdkVersion();
        } catch (Throwable e) {
        }
        MEDIA_SDK_VERSION = sdk;
    }

    public static boolean SHOW_VIDEO_INFO = true;

    public static final String USER_STATE_OPEN = "Open";
    public static final String USER_STATE_LOCK = "Lock";
}
