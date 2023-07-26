package com.media.jwvideoplayer.lib.preferences;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.annotation.Nullable;

import com.media.jwvideoplayer.lib.provider.ContextProvider;

import java.util.Set;

/**
 * Created by Joyce.wang on 2022/10/11.
 */
public class CommonPreference {
    private static final String TAG = CommonPreference.class.getSimpleName();
    private final static String APP_PREFERENCE_NAME = "VALOR_MFC";
    private SharedPreferences mSharedPreferences;

    public static CommonPreference getInstance() {
        return SingletonInnerClass.INSTANCE;
    }

    static class SingletonInnerClass {
        final static CommonPreference INSTANCE = new CommonPreference();
    }
    public CommonPreference() {
        this.mSharedPreferences = ContextProvider.getContext().getSharedPreferences(APP_PREFERENCE_NAME, Context.MODE_PRIVATE);
    }

    public void putStringSet(String key, @Nullable Set<String> values) {
        mSharedPreferences.edit().putStringSet(key, values).apply();
    }

    public void putString(String key, @Nullable String value) {
        mSharedPreferences.edit().putString(key, value).apply();
    }

    public void putInt(String key, int value) {
        mSharedPreferences.edit().putInt(key, value).apply();
    }

    public void putLong(String key, long value) {
        mSharedPreferences.edit().putLong(key, value).apply();
    }

    public void putFloat(String key, float value) {
        mSharedPreferences.edit().putFloat(key, value).apply();
    }

    public void putBoolean(String key, boolean value) {
        mSharedPreferences.edit().putBoolean(key, value).apply();
    }

    public String getString(String key) {
        return getString(key, "");
    }

    public String getString(String key, @Nullable String defValue) {
        return mSharedPreferences.getString(key, defValue);
    }

    public Set<String> getStringSet(String key, @Nullable Set<String> defValues) {
        return mSharedPreferences.getStringSet(key, defValues);
    }

    public int getInt(String key, int defValue) {
        return mSharedPreferences.getInt(key, defValue);
    }

    public long getLong(String key, long defValue) {
        return mSharedPreferences.getLong(key, defValue);
    }

    public float getFloat(String key, float defValue) {
        return mSharedPreferences.getFloat(key, defValue);
    }

    public boolean getBoolean(String key, boolean defValue) {
        return mSharedPreferences.getBoolean(key, defValue);
    }

    public void remove(String key) {
        mSharedPreferences.edit().remove(key);
    }
}
