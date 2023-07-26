package com.media.jwvideoplayer.utils;

import android.content.Context;
import com.media.jwvideoplayer.R;
import com.media.jwvideoplayer.lib.utils.DensityUtils;

/**
 * Created by Joyce.wang on 2023/6/28.
 */
public class VideoUtils {
    public static float getTextSizeInPortrait(Context context, int sizePos) {
        float size =  context.getResources().getDimension(R.dimen.m_play_dimen_font_size_portrait_small);
        switch (sizePos) {
            case 0:
                size = context.getResources().getDimension(R.dimen.m_play_dimen_font_size_portrait_small);
                break;
            case 1:
                size = context.getResources().getDimension(R.dimen.m_play_dimen_font_size_portrait_middle);
                break;
            case 2:
                size = context.getResources().getDimension(R.dimen.m_play_dimen_font_size_portrait_large);
                break;
        }
        return DensityUtils.px2sp(context, size);
    }

    public static float getTextSizeInLandscape(Context context, int sizePos) {
        float size =  context.getResources().getDimension(R.dimen.m_play_dimen_font_size_landscape_small);
        switch (sizePos) {
            case 0:
                size = context.getResources().getDimension(R.dimen.m_play_dimen_font_size_landscape_small);
                break;
            case 1:
                size = context.getResources().getDimension(R.dimen.m_play_dimen_font_size_landscape_middle);
                break;
            case 2:
                size = context.getResources().getDimension(R.dimen.m_play_dimen_font_size_landscape_large);
                break;
        }
        return DensityUtils.px2sp(context, size);
    }
}
