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

    public static final Long TRACKING_PAGE_ID_VIDEO_DETAILS = 22L;
    public static final Long TRACKING_PAGE_ID_AD_ACTIVITY = 40L;


    public final static class EXTRA_KEY {
        public final static String VIDEO_ID_LONG = "video_id";
        public final static String EXTERNAL_VIDEO_META_INFO = "external_video_meta_info";
        public final static String CACHE_REMOVE_POSITION = "cache_remov_position";
    }

    public static class ENavType {
        public final static String MOVIE = "MOVIE";
        public final static String TV = "SERIES";
        public final static String SHORT_VIDEO = "SHORT_VIDEO";
        public final static String VIDEO = "VIDEO";
        public final static String DOCUMENTARY = "DOCUMENTARY";
    }

    public static class ResponseCode {
        public final static int FAVOR_CODE_23 = 23;//添加favor超限
        public final static int WATCH_CODE_24 = 24;//添加watch超限
    }

    //Prpd stream config
    public static final String ENABLE_PRPD_STREAM_KEY = "enable_prpd_stream_key";
    public static final boolean PRPD_STREAM_DEFAULT_VALUE = true;

    public static final String URL_TRACKER_HOST = "url_tracker_host";
    public static final String URL_FEEDBACK_HOST = "url_feedback_host";

    //是否是推荐字幕门限值
    public static final long RECOMMENDED_CAPTION_THRESHOLD_DEFAULT = 300;
    public static final String KEY_RECOMMENDED_CAPTION_THRESHOLD = "key_recommended_caption_threshold";

    public static final String KEY_EPISODE_AUTO_PLAY = "key_episode_auto_play";
    public static final String KEY_VIDEO_METADO = "key_video_metado";
    public static final String KEY_EPISODE_LIST_TOTAL_SIZE = "key_episode_list_total_size";
    public static final String KEY_EPISODE_LIST = "key_episode_list";
    public static final String KEY_SESSION = "key_session";
    public static final String KEY_IS_DOWNLOAD = "key_is_download";

    // local cache size
    public static final String UI_LOCAL_CACHE_SIZE_KEY = "ui_local_cache_size";
    public static final int UI_LOCAL_CACHE_SIZE_DEFAULT = 10;

    //记录上一次播放类型
    public static final String KEY_LAST_PLAYER_TYPE = "key_last_player_type";

    //首页trailer
    public static final String KEY_TRAILER_CACHE_EXPIRE_TIME = "key_trailer_cache_expire_time";
    public static final long TRAILER_CACHE_EXPIRE_TIME_DEFAULT_VALUE = 7 * 24 * 60 * 60 * 1000;

    public static final String KEY_TRAILER_MAX_CACHE_SIZE = "key_trailer_max_cache_size";
    public static final long TRAILER_MAX_CACHE_SIZE_DEFAULT_VALUE = 200 * 1024 * 1024;

    public static boolean isMobileTraffic = false;//移动流量是否播放
}
