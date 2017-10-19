package com.why94.misportsview;

import android.graphics.RectF;

/**
 * OffsetRectF
 * Created by WenHuayu(why94@qq.com) on 2017/10/18.
 */
class RectFPlus extends RectF {
    public RectFPlus() {
    }

    public RectFPlus(RectF r) {
        super(r);
    }

    public RectFPlus resize(float offset) {
        return resize(offset, offset, -offset, -offset);
    }

    public RectFPlus resize(float offsetLeft, float offsetTop, float offsetRight, float offsetBottom) {
        left += offsetLeft;
        top += offsetTop;
        right += offsetRight;
        bottom += offsetBottom;
        return this;
    }

    public RectFPlus valueOf(RectF src) {
        super.set(src);
        return this;
    }

    public RectFPlus offsetY(float offset) {
        offset(0, offset);
        return this;
    }
}
