package com.example.user.unlockview;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PathMeasure;
import android.graphics.RectF;
import android.graphics.SweepGradient;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.animation.Transformation;

/**
 * Created by dxs on 2018/3/11.
 */

public class LockView extends View {
    /* 刻度圆弧的外接矩形 */
    private RectF mScaleArcRectF;
    /* 渐变矩阵，作用在SweepGradient */
    private Matrix mGradientMatrix;
    /* 梯度扫描渐变 */
    private SweepGradient mSweepGradient;
    /* 亮色，用于分针、秒针、渐变终止色 */
    private int mLightColor;
    /* 暗色，圆弧、刻度线、时针、渐变起始色 */
    private int mDarkColor;
    /* 刻度圆弧画笔 */
    private Paint mScaleArcPaint;
    /* 时钟半径，不包括padding值 */
    private float mRadius;
    /* 刻度线长度 */
    private float mScaleLength;
    /* 刻度线画笔 */
    private Paint mScaleLinePaint;
    /* 内圆画笔 */
    private Paint mCirclePaint;
    /* 背景色 */
    private int mBackgroundColor;
    private int count = 200;
    private float progress;

    private defaultAnimal animal;

    /*内圆半径*/
    private int radius;

    private Bitmap bitmap;
    private int state;

    //对勾与打叉动画相关
    private PathMeasure mPathMeasure;


    public LockView(Context context) {
        this(context, null);
    }

    public LockView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        mLightColor = Color.parseColor("#ffffff");
        mDarkColor = Color.parseColor("#80ffffff");
        mBackgroundColor = Color.parseColor("#ff7f00");
        setBackgroundColor(mBackgroundColor);
        initData();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                //触摸时
                state=1;
                return true;
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
                state=0;
                return true;
        }
        return super.onTouchEvent(event);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        //宽和高分别去掉padding值，取min的一半即表盘的半径
        mRadius = Math.min(w - getPaddingLeft() - getPaddingRight(),
                h - getPaddingTop() - getPaddingBottom()) / 2;
        mScaleLength = 0.1f * mRadius;//根据比例确定刻度线长度
        radius = (int) (0.65 * mRadius);
        mScaleArcPaint.setStrokeWidth(mScaleLength);
        mScaleLinePaint.setStrokeWidth(0.06f * mRadius);
        //梯度扫描渐变，以(w/2,h/2)为中心点，两种起止颜色梯度渐变
        //float数组表示，[0,0.75)为起始颜色所占比例，[0.75,1}为起止颜色渐变所占比例
        mSweepGradient = new SweepGradient(w / 2, h / 2,
                new int[]{mDarkColor, mLightColor}, new float[]{0.55f, 1});
    }

    private void initData() {
        mScaleArcRectF = new RectF();
        mGradientMatrix = new Matrix();
        mScaleArcPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mScaleArcPaint.setStyle(Paint.Style.STROKE);

        mScaleLinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mScaleLinePaint.setStyle(Paint.Style.STROKE);
        mScaleLinePaint.setColor(mBackgroundColor);

        mCirclePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mCirclePaint.setColor(mLightColor);
        mCirclePaint.setAlpha(30);

        animal = new defaultAnimal();
        animal.setDuration(800);
        animal.setRepeatCount(-1);
        animal.setInterpolator(new LinearInterpolator());
        animal.setRepeatMode(Animation.RESTART);
        setAnimation(animal);

        bitmap = BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        drawCircle(canvas);
        if(state==0){
            drawPicture(canvas);
        }
        drawScaleLine(canvas);
    }

    private void drawCircle(Canvas canvas) {
        canvas.drawCircle(getWidth() / 2, getHeight() / 2, radius, mCirclePaint);
    }

    private void drawPicture(Canvas canvas) {
        if (bitmap != null) {
            canvas.drawBitmap(bitmap, (getWidth() - bitmap.getWidth()) / 2, (getHeight() - bitmap.getHeight()) / 2, null);
        }
    }

    /**
     * 画一圈梯度渲染的亮暗色渐变圆弧，重绘时不断旋转，上面盖一圈背景色的刻度线
     */
    private void drawScaleLine(Canvas canvas) {
        mScaleArcRectF.set(1.5f * mScaleLength,
                1.5f * mScaleLength,
                getWidth() - 1.5f * mScaleLength,
                getHeight() - 1.5f * mScaleLength);

        //matrix默认会在三点钟方向开始颜色的渐变，为了吻合
        //钟表十二点钟顺时针旋转的方向，把秒针旋转的角度减去90度
        mGradientMatrix.setRotate(progress * 360 - 90, getWidth() / 2, getHeight() / 2);
        mSweepGradient.setLocalMatrix(mGradientMatrix);
        mScaleArcPaint.setShader(mSweepGradient);
        canvas.drawArc(mScaleArcRectF, 0, 360, false, mScaleArcPaint);
        //画背景色刻度线
        canvas.save();
        for (int i = 0; i < 60; i++) {
            canvas.drawLine(getWidth() / 2, mScaleLength,
                    getWidth() / 2, 2.01f * mScaleLength, mScaleLinePaint);
            canvas.rotate(6f, getWidth() / 2, getHeight() / 2);
        }
        canvas.restore();
    }

    public void animalStart() {
        if (animal != null) {
            Log.e("dxsTest", "animal not null");
            animal.cancel();
            animal.start();
        }
    }

    public void animalStop() {
        if (animal != null) {
            animal.cancel();
        }
    }

    private void initRightPath(){
        Path path=new Path();

        //起点
        float starx= (float) (0.3*getWidth());
        float stary= (float) (0.3*getWidth());
        path.moveTo(starx,stary);

        //拐点
        float centerx= (float) (0.43*getWidth());
        float centery= (float) (0.66*getWidth());
        path.lineTo(centerx,centery);

        //终点
        float endx= (float) (0.43*getWidth());
        float endy= (float) (0.66*getWidth());
        path.lineTo(endx,endy);

        mPathMeasure.setPath(path,false);
    }

    class defaultAnimal extends Animation {
        @Override
        protected void applyTransformation(float interpolatedTime, Transformation t) {
            super.applyTransformation(interpolatedTime, t);
            if(state==0){

            }else if(state==1) {
                progress = interpolatedTime;
                postInvalidate();
            }
        }
    }
}
