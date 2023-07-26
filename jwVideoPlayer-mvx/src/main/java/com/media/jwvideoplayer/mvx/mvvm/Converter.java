package com.media.jwvideoplayer.mvx.mvvm;

import android.widget.EditText;

public class Converter {
    //TODO：wait Converts
//    @InverseMethod("stringToDate")
    public static String dateToString(EditText view, long oldValue,
                                      long value) {
        // Converts long to String.
        return String.valueOf(oldValue) + String.valueOf(value);
    }

    public static long stringToDate(EditText view, String oldValue,
                                    String value) {
        // Converts String to long.
        return Long.valueOf(oldValue) + Long.valueOf(value);
    }
}