package com.why94.misportsview;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PathEffect;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.SweepGradient;
import android.support.annotation.IntDef;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.view.animation.OvershootInterpolator;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * MiSportsView
 * Created by WenHuayu(why94@qq.com) on 2017/10/18.
 */
public class MiSportsView extends View {

    private static final String TAG = "MiSportsView";

    @IntDef({State.Connect, State.Connected})
    @Retention(RetentionPolicy.SOURCE)
    @interface State {
        int Connect = 0;
        int Connected = 1;
    }

    @State
    private int mState = State.Connect;

    private RectF mDrawArea;
    private Random mRandom = new Random();

    private Paint mColorPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Paint mShaderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private RectFPlus mTempOffsetRectF = new RectFPlus();
    private float mAngles;

    //连接环参数
    private int mConnectRingAreaOffset = DisplayUtils.dp2px(36);//连接环与绘制区的偏移
    private int mConnectRingStrokeWidth = DisplayUtils.dp2px(1.5f);//连接环宽
    private int mConnectRingSpacing = DisplayUtils.dp2px(0.5f);//连接环间距
    private int mConnectRingDitherOffsetMultiplier = 2;//连接环外接矩形抖动倍数，值为 mConnectRingStrokeWidth + mConnectRingSpacing
    private int CONNECT_RING_DEGREES_OFFSET = 2;//连接环旋转角度偏移
    private int CONNECT_RING_NUMBER = 8;//连接环数量
    private Paint mConnectRingPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private List<Ring> mConnectRings;

    //连接环头部的星星参数
    private int mConnectStarDegrees = 45;
    private int mConnectStarNum = 45;
    private float mConnectStarSize = DisplayUtils.dp2px(5);
    private float mConnectStarDegreesPreOffset = CONNECT_RING_DEGREES_OFFSET * CONNECT_RING_NUMBER;
    private float mConnectStarRadiusMaxOffset = DisplayUtils.dp2px(12);
    private float mConnectStarRadiusMinOffset = DisplayUtils.dp2px(-12);

    //已连接环参数
    private int mConnectedRingStrokeWidth = DisplayUtils.dp2px(16);
    private int mConnectedRingMaxColor = 0xEEFFFFFF;
    private int mConnectedRingMinColor = 0x77FFFFFF;
    private int mConnectedRingNumber = 5;
    private PorterDuffXfermode mConnectedRingShadowXfermode = new PorterDuffXfermode(PorterDuff.Mode.SRC_OUT);
    private float mConnectedRingOvershoot = 0;
    private ValueAnimator mConnectedRingOvershootAnimator;

    //文字参数
    private String mContentText = "2274", mInfoText = "1.5公里 | 34千卡";
    private int mContentTextSize = DisplayUtils.sp2px(48), mInfoTextSize = DisplayUtils.sp2px(12);
    private float mProgress = 0.75f;
    private int mProgressRingColor = 0xEEFFFFFF;
    private int mProgressRingSpace = DisplayUtils.dp2px(44);
    private int mProgressRingWidth = DisplayUtils.dp2px(2);
    private float[] intervals = new float[]{DisplayUtils.dp2px(0.5f), DisplayUtils.dp2px(2)};
    private Paint mDrawTextOnPointPaint;

    //手表图标
    private Path mWatchPath;
    private Paint mDrawWatchPaint;

    private int mConnectDuration = 2_000;
    private int mConnectedDuration = 6_000;

    private ValueAnimator mAnglesAnimator;

    public MiSportsView(Context context) {
        this(context, null);
    }

    public MiSportsView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        mConnectRingPaint.setStyle(Paint.Style.STROKE);
        mConnectRingPaint.setStrokeWidth(mConnectRingStrokeWidth);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        //取短边长度的居中矩形区域作为绘制区
        float offX, offY;
        if (w > h) {
            offX = (w - h) / 2f;
            offY = 0;
        } else {
            offX = 0;
            offY = (h - w) / 2f;
        }
        mDrawArea = new RectF(offX, offY, w - offX, h - offY);
        //生成连接环
        mConnectRings = createConnectRings(new RectFPlus(mDrawArea).resize(mConnectRingAreaOffset), null, null);

        mWatchPath = null;
    }

    /**
     * 创建固定的连接环
     *
     * @param area         限制区域
     * @param ditherValues 抖动值数组 [left,top,bottom,...]
     */
    private List<Ring> createConnectRings(RectF area, float[] ditherValues, int[] colors) {
        float centerX = area.centerX();
        float centerY = area.centerY();
        if (ditherValues == null || ditherValues.length < CONNECT_RING_NUMBER * 3) {
            int value = mConnectRingDitherOffsetMultiplier * (mConnectRingStrokeWidth + mConnectRingSpacing);
            ditherValues = createRandomFloat(-value, value, CONNECT_RING_NUMBER * 3);
        }
        if (colors == null || colors.length < CONNECT_RING_NUMBER) {
            colors = createRandomColor(0x77FFFFFF, 0xDDFFFFFF, CONNECT_RING_NUMBER);
        }
        List<Ring> connectRings = new ArrayList<>(CONNECT_RING_NUMBER);
        int offset;
        connectRings.add(new Ring(new RectF(area), new SweepGradient(centerX, centerY, 0x00FFFFFF, 0xDDFFFFFF)));//最外环不抖动
        for (int i = 1; i < CONNECT_RING_NUMBER; i++) {
            offset = i * (mConnectRingStrokeWidth + mConnectRingSpacing);
            connectRings.add(new Ring(new RectF(
                    area.left + offset + ditherValues[i * 3],//左边随机抖动
                    area.top + offset + ditherValues[i * 3 + 1],//顶边边随机抖动
                    area.right - offset,//为了让起点不重合，所以这里不进行抖动
                    area.bottom - offset + ditherValues[i * 3 + 2] //底边随机抖动
            ), new SweepGradient(centerX, centerY, colors[i] & 0x00FFFFFF, colors[i])));
        }
        return connectRings;
    }

    private float[] createRandomFloat(float min, float max, int num) {
        float[] floats = new float[num];
        StringBuilder builder = new StringBuilder("new float[]{");
        for (int i = 0; i < floats.length; i++) {
            floats[i] = min + (max - min) * mRandom.nextFloat();
            builder.append(floats[i]).append("f, ");
        }
        builder.delete(builder.length() - 2, builder.length()).append("}");
        Log.d(TAG, "createRandomFloat: " + builder);
        return floats;
    }

    private int[] createRandomColor(int minColor, int maxColor, int num) {
        int[] colors = new int[num];
        int minAlpha = Color.alpha(minColor), maxAlpha = Color.alpha(maxColor);
        int minRed = Color.red(minColor), maxRed = Color.red(maxColor);
        int minGreen = Color.green(minColor), maxGreen = Color.green(maxColor);
        int minBlue = Color.blue(minColor), maxBlue = Color.blue(maxColor);
        StringBuilder builder = new StringBuilder("new int[]{");
        for (int i = 0; i < colors.length; i++) {
            colors[i] = Color.argb(
                    minAlpha + mRandom.nextInt(maxAlpha - minAlpha),
                    maxRed == minRed ? maxRed : minRed + mRandom.nextInt(maxRed - minRed),
                    maxGreen == minGreen ? maxGreen : minGreen + mRandom.nextInt(maxGreen - minGreen),
                    maxBlue == minBlue ? maxBlue : minBlue + mRandom.nextInt(maxBlue - minBlue));
            builder.append(String.format("0x%08X, ", colors[i]));
        }
        builder.delete(builder.length() - 2, builder.length()).append("};");
        Log.d(TAG, "createRandomColor: " + builder);
        return colors;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        switch (mState) {
            case State.Connect:
                drawConnectRing(canvas);
                drawConnectRingStar(canvas);
                break;
            case State.Connected:
                drawConnectedRing(canvas);
                drawProgressRing(canvas);
                break;
        }

        drawTextOnPoint(canvas, mContentText, 0xDDFFFFFF, mContentTextSize, mDrawArea.centerX(), mDrawArea.centerY());
        drawTextOnPoint(canvas, mInfoText, 0xAAFFFFFF, mInfoTextSize, mDrawArea.centerX(), mDrawArea.centerY() + mContentTextSize / 2 * 1.6f);
        drawWatch(canvas);
    }

    private void drawProgressRing(Canvas canvas) {
        mColorPaint.setColor(mProgressRingColor);
        mColorPaint.setStyle(Paint.Style.STROKE);
        mColorPaint.setStrokeCap(Paint.Cap.BUTT);
        mColorPaint.setStrokeWidth(mProgressRingWidth);
        RectF rectF = mTempOffsetRectF.valueOf(mDrawArea).resize(mProgressRingSpace);
        //实线
        canvas.drawArc(rectF, -90, mProgress * 360, false, mColorPaint);
        //虚线
        PathEffect pathEffect = new DashPathEffect(intervals, 0);
        mColorPaint.setPathEffect(pathEffect);
        canvas.drawArc(rectF, -90 + mProgress * 360, 360 - mProgress * 360, false, mColorPaint);
        mColorPaint.setPathEffect(null);
        //点
        mColorPaint.setStrokeWidth(mProgressRingWidth * 3);
        mColorPaint.setStrokeCap(Paint.Cap.ROUND);
        canvas.drawArc(rectF, -90 + mProgress * 360, 0.1f, false, mColorPaint);
    }

    /**
     * 将文字居中绘制到指定位置
     */
    private void drawTextOnPoint(Canvas canvas, String text, int color, int size, float x, float y) {
        if (mDrawTextOnPointPaint == null) {
            mDrawTextOnPointPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            mDrawTextOnPointPaint.setTextAlign(Paint.Align.CENTER);
        }
        mDrawTextOnPointPaint.setTextSize(size);
        mDrawTextOnPointPaint.setColor(color);
        y -= mDrawTextOnPointPaint.descent() - (mDrawTextOnPointPaint.descent() - mDrawTextOnPointPaint.ascent()) / 2;
        canvas.drawText(text, x, y, mDrawTextOnPointPaint);
    }

    /**
     * 已连接环
     */
    private void drawConnectedRing(Canvas canvas) {
        //阴影
        mColorPaint.setStyle(Paint.Style.STROKE);
        mColorPaint.setStrokeWidth(mConnectedRingStrokeWidth);
        RectFPlus rectF = mTempOffsetRectF.valueOf(mDrawArea)
                .resize(mConnectedRingStrokeWidth * 1.5f + mConnectedRingOvershoot)//绘制区大小向内缩小
                .offsetY(-mConnectedRingStrokeWidth * 1.5f + mConnectedRingStrokeWidth / (mConnectedRingNumber - 1) * 2);//向上偏移使上边重合
        int offset = mConnectedRingStrokeWidth / (mConnectedRingNumber - 1);
        int alpha = Color.alpha(mConnectedRingMinColor) / mConnectedRingNumber;
        int red = Color.red(mConnectedRingMaxColor);
        int green = Color.green(mConnectedRingMaxColor);
        int blue = Color.blue(mConnectedRingMaxColor);
        int color;
        int saved = canvas.saveLayer(null, null, Canvas.ALL_SAVE_FLAG);
        canvas.rotate(mAngles, mDrawArea.centerX(), mDrawArea.centerY());
        for (int i = 0, count = mConnectedRingNumber - 1; i < count; i++) {
            int saved2 = canvas.saveLayer(new RectF(mDrawArea.left, mDrawArea.top, mDrawArea.right, mDrawArea.bottom / 2), null, Canvas.ALL_SAVE_FLAG);
            color = Color.argb((i + 1) * alpha, red, green, blue);
            mColorPaint.setColor(color);
            canvas.drawOval(rectF, mColorPaint);
            rectF.offset(0, offset);
            mColorPaint.setXfermode(mConnectedRingShadowXfermode);
            mColorPaint.setColor(Color.TRANSPARENT);
            canvas.drawOval(rectF, mColorPaint);
            mColorPaint.setXfermode(null);
            canvas.restoreToCount(saved2);
        }
        //外环
        mShaderPaint.setStyle(Paint.Style.STROKE);
        mShaderPaint.setStrokeWidth(mConnectedRingStrokeWidth);
        mShaderPaint.setShader(new LinearGradient(rectF.centerX(), rectF.top, rectF.centerX(), rectF.bottom, mConnectedRingMaxColor, mConnectedRingMinColor, Shader.TileMode.CLAMP));
        canvas.drawOval(rectF, mShaderPaint);
        canvas.restoreToCount(saved);
    }

    private void drawConnectRing(Canvas canvas) {
        for (int i = 0, size = mConnectRings.size(); i < size; i++) {
            Ring connectRing = mConnectRings.get(i);
            mConnectRingPaint.setShader(connectRing.shader);
            drawRotateArc(canvas, mConnectRingPaint, mAngles + i * CONNECT_RING_DEGREES_OFFSET, connectRing.area);
        }
    }

    private void drawConnectRingStar(Canvas canvas) {
        float radius = mDrawArea.right - mDrawArea.centerX() - mConnectRingAreaOffset - (mConnectRingStrokeWidth + mConnectRingSpacing) * CONNECT_RING_NUMBER / 2;
        float x, y;
        float centerX = mDrawArea.centerX();
        float centerY = mDrawArea.centerY();
        mColorPaint.setStrokeCap(Paint.Cap.ROUND);
        mColorPaint.setStrokeWidth(mConnectStarSize);
        mColorPaint.setColor(Color.RED);
        int offsetDegrees;
        float offsetDegreesWeight;
        float offsetRadius;
        for (int i = 0; i < mConnectStarNum; i++) {
            offsetDegrees = mRandom.nextInt(mConnectStarDegrees);//随机取出星星落在多少角度上
            offsetDegreesWeight = (float) offsetDegrees / mConnectStarDegrees;
            offsetRadius = 2 * (0.5f - mRandom.nextFloat()) * offsetDegreesWeight * (mConnectStarRadiusMaxOffset - mConnectStarRadiusMinOffset);//根据随机角度偏移，在给定范围内随机出半径偏移，角度偏移越小，半径偏移范围越小
            mColorPaint.setColor(Color.argb(0xFF - (int) (offsetDegreesWeight * 0xFF), 0xFF, 0xFF, 0xFF));//角度偏移越大，透明度越大
            x = (float) (centerX + (radius + offsetRadius) * Math.cos((mAngles + mConnectStarDegreesPreOffset - offsetDegrees) / 180f * Math.PI));
            y = (float) (centerY + (radius + offsetRadius) * Math.sin((mAngles + mConnectStarDegreesPreOffset - offsetDegrees) / 180f * Math.PI));
            canvas.drawPoint(x, y, mColorPaint);
        }
    }

    /**
     * 绘制旋转椭圆
     */
    private void drawRotateArc(Canvas canvas, Paint paint, float degrees, RectF area) {
        int saved = canvas.saveLayer(null, null, Canvas.ALL_SAVE_FLAG);
        canvas.rotate(degrees, area.centerX(), area.centerY());
        area = mTempOffsetRectF.valueOf(area).resize(paint.getStrokeWidth() / 2);
        canvas.drawOval(area, paint);
        canvas.restoreToCount(saved);
    }

    /**
     * 圆环参数
     */
    private static class Ring {
        final RectF area;
        final Shader shader;

        Ring(RectF area, Shader shader) {
            this.area = area;
            this.shader = shader;
        }
    }

    private void drawWatch(Canvas canvas) {
        if (mWatchPath == null) {
            float centerX = mDrawArea.centerX();
            float width = mDrawArea.width() / 40 * 2f / 2;
            float height = mDrawArea.height() / 40 * 2.5f;
            mWatchPath = createWatchPath(new RectF(centerX - width, mDrawArea.bottom - height * 4.5f, centerX + width, mDrawArea.bottom - height * 3.5f));
        }
        if (mDrawWatchPaint == null) {
            mDrawWatchPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            mDrawWatchPaint.setStyle(Paint.Style.FILL);
            mDrawWatchPaint.setColor(Color.WHITE);
        }
        canvas.drawPath(mWatchPath, mDrawWatchPaint);
    }

    private Path createWatchPath(RectF area) {
        float hv = area.height() / 7f;//高度尺寸：表带1表盘5
        float wv = area.width() / 4f;//宽度尺寸：表带2表盘4
        float hl = area.height() / 10f;//纵向长度
        float wl = area.width() / 10f;//横向长度
        float centerX = area.centerX();
        float centerY = area.centerY();

        Path path = new Path();
        path.setFillType(Path.FillType.EVEN_ODD);
        //表盘
        path.moveTo(area.left + wv, area.top);
        path.lineTo(area.right - wv, area.top);
        path.lineTo(area.right - wv, area.top + hv);
        path.quadTo(area.right + wv, centerY, area.right - wv, area.bottom - hv);
        path.lineTo(area.right - wv, area.bottom);
        path.lineTo(area.left + wv, area.bottom);
        path.lineTo(area.left + wv, area.bottom - hv);
        path.quadTo(area.left - wv, centerY, area.left + wv, area.top + hv);
        path.close();
        path.addOval(mTempOffsetRectF.valueOf(area).resize(wl, hv + hl / 2, -wl, -hv - hl / 2), Path.Direction.CCW);
        path.moveTo(centerX, centerY);
        path.lineTo(centerX, -hv);
        //指针
        path.addRect(centerX - wl / 2, centerY - hv, centerX + wl / 2, centerY + hl / 2, Path.Direction.CCW);
        path.addRect(centerX + wl / 2, centerY - hl / 2, centerX + wv, centerY + hl / 2, Path.Direction.CCW);
        return path;
    }

    public void connect() {
        if (mState == State.Connect) {
            return;
        }
        mState = State.Connect;
        if (mAnglesAnimator == null) {
            startAnglesAnimator(mConnectDuration);
        } else {
            mAnglesAnimator.setDuration(mConnectDuration);
        }
    }

    public void connected() {
        if (mState == State.Connected) {
            return;
        }
        mState = State.Connected;
        if (mAnglesAnimator == null) {
            startAnglesAnimator(mConnectedDuration);
        } else {
            mAnglesAnimator.setDuration(mConnectedDuration);
        }
        if (mConnectedRingOvershootAnimator != null) {
            mConnectedRingOvershootAnimator.cancel();
        }
        mConnectedRingOvershoot = DisplayUtils.dp2px(30);
        mConnectedRingOvershootAnimator = ValueAnimator.ofFloat(mConnectedRingOvershoot, 0);
        mConnectedRingOvershootAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                mConnectedRingOvershoot = (float) animation.getAnimatedValue();
                if (mConnectedRingOvershoot == 0) {
                    mConnectedRingOvershootAnimator.cancel();
                    mConnectedRingOvershootAnimator = null;
                }
            }
        });
        mConnectedRingOvershootAnimator.setDuration(mConnectedDuration / 10);
        mConnectedRingOvershootAnimator.setInterpolator(new OvershootInterpolator(3));
        mConnectedRingOvershootAnimator.start();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        startAnglesAnimator(mConnectDuration);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        stopAnglesAnimator();
    }

    private void startAnglesAnimator(int duration) {
        stopAnglesAnimator();
        mAnglesAnimator = ValueAnimator.ofFloat(0, 360);
        mAnglesAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                mAngles = (float) animation.getAnimatedValue();
                invalidate();
            }
        });
        mAnglesAnimator.setInterpolator(new LinearInterpolator());
        mAnglesAnimator.setRepeatCount(Integer.MAX_VALUE - 1);
        mAnglesAnimator.setRepeatMode(ValueAnimator.RESTART);
        mAnglesAnimator.setDuration(duration);
        mAnglesAnimator.start();
    }

    private void stopAnglesAnimator() {
        if (mAnglesAnimator != null) {
            mAnglesAnimator.cancel();
            mAnglesAnimator = null;
        }
    }
}
