package com.media.jwvideoplayer.player;

public class VodMediaConstant {
    private static final int MESSAGE_BASE = 0x9000;
    public static final int MESSAGE_UPDATE_PROGRESS = MESSAGE_BASE + 0;
    public static final int MESSAGE_SHOW_BOTTOM = MESSAGE_BASE + 1;//展示底部进度棒
    public static final int MESSAGE_HIDE_BOTTOM = MESSAGE_BASE + 2;//隐藏底部进度棒
    public static final int MESSAGE_HIDE_GESTURE_BOX = MESSAGE_BASE + 3;
    public static final int MESSAGE_HIDE_BOTTOM_AND_TOP_BOX = MESSAGE_BASE + 4;
    public static final int MESSAGE_SHOW_LOADING = MESSAGE_BASE + 5;//展示loading
    public static final int MESSAGE_HIDE_LOADING = MESSAGE_BASE + 6;//隐藏loading
    public static final int MESSAGE_SEEK_NEW_POSITION = MESSAGE_BASE + 7;
    public static final int MESSAGE_HIDE_CENTER_BOX = MESSAGE_BASE + 8;
    public static final int MESSAGE_HIDE_USE_MOBILE_DATA_TIPS =  MESSAGE_BASE + 9;
    public static final int MESSAGE_UPDATE_SUBTITLE = MESSAGE_BASE + 10;//更新字幕
    public static final int MESSAGE_CHANGE_PLAY_STATUS = MESSAGE_BASE + 11;
    public static final int MESSAGE_SHOW_PLAY_ICON = MESSAGE_BASE + 12;//展示视频中间的播放按钮
    public static final int MESSAGE_HIDE_PLAY_ICON = MESSAGE_BASE + 13;//隐藏视频中间的播放按钮
    public static final int MESSAGE_UPDATE_LOADING_EXTRA_INFO = MESSAGE_BASE + 14;
    public static final int MESSAGE_SHOW_NEXT_EPISODE = MESSAGE_BASE + 15;//展示next episode 按钮
    public static final int MESSAGE_HIDE_NEXT_EPISODE = MESSAGE_BASE + 16;//隐藏next episode 按钮

    public static boolean isMobileTraffic = false;//移动流量是否播放
}
