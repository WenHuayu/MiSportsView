package com.why94.misportsview;

import android.content.res.Resources;

/**
 * DisplayUtils
 * Created by WenHuayu(why94@qq.com) on 2017/10/16.
 */
public class DisplayUtils {
    private static float density = Resources.getSystem().getDisplayMetrics().density;
    private static float scaledDensity = Resources.getSystem().getDisplayMetrics().scaledDensity;

    public static int dp2px(float dip) {
        return (int) (dip * density + 0.5f * (dip >= 0 ? 1 : -1));
    }

    public static int sp2px(float spValue) {
        return (int) (spValue * scaledDensity + 0.5f);
    }
}