/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.sunshine.app;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.wearable.watchface.CanvasWatchFaceService;
import android.support.wearable.watchface.WatchFaceStyle;
import android.text.format.Time;
import android.view.SurfaceHolder;
import android.view.WindowInsets;

import com.example.android.sunshine.app.Utility.Utility;

import java.lang.ref.WeakReference;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

/**
 * Digital watch face with seconds. In ambient mode, the seconds aren't displayed. On devices with
 * low-bit ambient mode, the text is drawn without anti-aliasing in ambient mode.
 */
public class MyWatchFace extends CanvasWatchFaceService {
    private static final int MSG_UPDATE_TIME = 0;
    private static final long INTERACTIVE_UPDATE_RATE_MS = TimeUnit.SECONDS.toMillis(1);

    private static final Typeface NORMAL_TYPEFACE =
            Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL);
    private boolean mRegisteredTimeZoneReceiver;
    private boolean mLowBitAmbient;
    private boolean mAmbient;

    private String showText = "24C 16C";
    private Bitmap weatherIcon;
    private Paint mTemPaint;
    private String lowtemp = "--";
    private String hightemp = "--";
    private String desString = "--";
    private int weatherID = -1;

    private int[] colorArray = {R.color.backgroundRed, R.color.backgroundPink, R.color.backgroundPurple, R.color.BackgroundDeepPurple, R.color.backgroundIndigo, R.color.backgroundBlue,
            R.color.backgroundLightBlue, R.color.backgroundCyan, R.color.backgroundTeal,
            R.color.backgroundGreen, R.color.backgroundLightGreen, R.color.backgroundLime, R.color.backgroundYellow, R.color.backgroundAmber,
            R.color.backgroundOrange, R.color.backgroundDarkOrange};

    private int colorCount = 0;
    private float mCenterHeight;
    private float mCenterWidth;
    private float mCenterWidthforWeatherIcon;
    private float weatherY;
    private float HihTempX;
    private float LowTempX;

    @Override
    public Engine onCreateEngine() {
        /* provide your watch face implementation */
        return new Engine();
    }

    private static class EngineHandler extends Handler {
        private final WeakReference<MyWatchFace.Engine> mWeakReference;


        public EngineHandler(MyWatchFace.Engine reference) {
            mWeakReference = new WeakReference<>(reference);
        }

        @Override
        public void handleMessage(Message msg) {
            MyWatchFace.Engine engine = mWeakReference.get();
            if (engine != null) {
                switch (msg.what) {
                    case MSG_UPDATE_TIME:
                        engine.handleUpdateTimeMessage();
                        break;
                }
            }
        }
    }


    /* implement service callback methods */
    private class Engine extends CanvasWatchFaceService.Engine {
        final Handler mUpdateTimeHandler = new EngineHandler(this);
        final BroadcastReceiver messageReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String text = intent.getStringExtra(getString(R.string.BRODCAST_INTENT_KEY));
                lowtemp = intent.getStringExtra(getString(R.string.LOW_TEMP_KEY));
                hightemp = intent.getStringExtra(getString(R.string.HIGH_TEMP_KEY));
                desString = intent.getStringExtra(getString(R.string.DESC_KEY));
                weatherID = intent.getIntExtra(getString(R.string.WEATID_KEY), -1);
                showText = text;
                invalidate();//focus update
            }
        };
        private float mYOffset;
        private float mXOffset;
        private float mTextYOffset;
        private float mTextXOffset;
        private String TEXT_KEY = "showText_key";
        private Paint mBackgroundPaint;
        private Paint mTextPaint;
        private Time mTime;
        final BroadcastReceiver mTimeZoneReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                mTime.clear(intent.getStringExtra("time-zone"));
                mTime.setToNow();
            }
        };
        private int mTapCount;
        private float mCenterHeightforWeatherIcon;

        /**
         * Update rate in milliseconds for interactive mode. We update once a second since seconds are
         * displayed in interactive mode.
         */

        private boolean shouldTimerBeRunning() {
            return isVisible() && !isInAmbientMode();
        }

        /**
         * Handle updating the time periodically in interactive mode.
         */
        private void handleUpdateTimeMessage() {
            invalidate();
            if (shouldTimerBeRunning()) {
                long timeMs = System.currentTimeMillis();
                long delayMs = INTERACTIVE_UPDATE_RATE_MS
                        - (timeMs % INTERACTIVE_UPDATE_RATE_MS);
                mUpdateTimeHandler.sendEmptyMessageDelayed(MSG_UPDATE_TIME, delayMs);
            }
        }

        @Override
        public void onCreate(SurfaceHolder holder) {
            super.onCreate(holder);
            /* initialize your watch face */
            setWatchFaceStyle(new WatchFaceStyle.Builder(MyWatchFace.this)
                    .setCardPeekMode(WatchFaceStyle.PEEK_MODE_VARIABLE)
                    .setBackgroundVisibility(WatchFaceStyle.BACKGROUND_VISIBILITY_INTERRUPTIVE)
                    .setShowSystemUiTime(false)
                    .setAcceptsTapEvents(true)
                    .build());
            Resources resources = MyWatchFace.this.getResources();
            mYOffset = resources.getDimension(R.dimen.digital_y_offset);

            mTextYOffset = resources.getDimension(R.dimen.digital_text_y_offset);

            mBackgroundPaint = new Paint();
            mBackgroundPaint.setColor(resources.getColor(R.color.background));

            mTextPaint = new Paint();
            mTextPaint = createTextPaint(resources.getColor(R.color.digital_text));

            mTemPaint = new Paint();
            mTemPaint = createTextPaint(resources.getColor(R.color.digital_text));

            mTime = new Time();
        }

        private Paint createTextPaint(int textColor) {
            Paint paint = new Paint();
            paint.setColor(textColor);
            paint.setTypeface(NORMAL_TYPEFACE);
            paint.setAntiAlias(true);
            return paint;
        }

        @Override
        public void onApplyWindowInsets(WindowInsets insets) {
            super.onApplyWindowInsets(insets);

            // Load resources that have alternate values for round watches.
            Resources resources = MyWatchFace.this.getResources();
            boolean isRound = insets.isRound();
            mXOffset = resources.getDimension(isRound
                    ? R.dimen.digital_x_offset_round : R.dimen.digital_x_offset);
            float textSize = resources.getDimension(isRound
                    ? R.dimen.digital_text_size_round : R.dimen.digital_text_size);

            mTextPaint.setTextSize(textSize);
            mTemPaint.setTextSize(36);


        }

        @Override
        public void onDestroy() {
            mUpdateTimeHandler.removeMessages(MSG_UPDATE_TIME);
            super.onDestroy();
        }

        @Override
        public void onPropertiesChanged(Bundle properties) {
            super.onPropertiesChanged(properties);
            /* get device features (burn-in, low-bit ambient) */
            mLowBitAmbient = properties.getBoolean(PROPERTY_LOW_BIT_AMBIENT, false);
        }

        @Override
        public void onTimeTick() {
            super.onTimeTick();
            /* the time changed */
            invalidate();
        }

        @Override
        public void onAmbientModeChanged(boolean inAmbientMode) {

            super.onAmbientModeChanged(inAmbientMode);
            if (mAmbient != inAmbientMode) {
                mAmbient = inAmbientMode;
                if (mLowBitAmbient) {
                    mTextPaint.setAntiAlias(!inAmbientMode);
                }
                invalidate();
            }

            // Whether the timer should be running depends on whether we're visible (as well as
            // whether we're in ambient mode), so we may need to start or stop the timer.
            updateTimer();
        }

        @Override
        public void onTapCommand(int tapType, int x, int y, long eventTime) {
            Resources resources = MyWatchFace.this.getResources();
            switch (tapType) {
                case TAP_TYPE_TOUCH:
                    // The user has started touching the screen.
                    break;
                case TAP_TYPE_TOUCH_CANCEL:
                    // The user has started a different gesture or otherwise cancelled the tap.
                    break;
                case TAP_TYPE_TAP:
                    // The user has completed the tap gesture.
                    mTapCount++;
                    mBackgroundPaint.setColor(resources.getColor(getBackgroundColor()));
                    break;
            }
            invalidate();
        }

        private int getBackgroundColor() {


            int color = colorArray[colorCount];
            colorCount++;
            if (colorCount >= colorArray.length)
                colorCount = 0;

            return color;

        }

        @Override
        public void onSurfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            super.onSurfaceChanged(holder, format, width, height);

            mCenterHeight = height / 2f;
            mCenterWidth = width / 2f;

        }

        @Override
        public void onDraw(Canvas canvas, Rect bounds) {
            /* draw your watch face */
            // Draw the background.

            //TODO AmbientMode changing at here
            if (isInAmbientMode()) {
                canvas.drawColor(Color.BLACK);
            } else {
                canvas.drawRect(0, 0, bounds.width(), bounds.height(), mBackgroundPaint);
            }

            // Draw H:MM in ambient mode or H:MM:SS in interactive mode.
            mTime.setToNow();
            String text = mAmbient
                    ? String.format("%d:%02d", mTime.hour, mTime.minute)
                    : String.format("%d:%02d:%02d", mTime.hour, mTime.minute, mTime.second);
            canvas.drawText(text, mXOffset, mYOffset, mTextPaint);

            //getThe hright of the Time text
            Rect timebounds = new Rect();
            mTextPaint.getTextBounds(text, 0, text.length() - 1, timebounds);
            int textHeight = timebounds.height();
            //draw the weatcher icon
            if (weatherID != -1) {
                weatherIcon = BitmapFactory.decodeResource(getResources(), Utility.getIconResourceForWeatherCondition(weatherID));
                mCenterWidthforWeatherIcon = mCenterWidth - weatherIcon.getWidth() / 2f;
                mCenterHeightforWeatherIcon = mCenterHeight - weatherIcon.getHeight() / 2f;

                weatherY = mCenterHeightforWeatherIcon + textHeight;

                canvas.drawBitmap(weatherIcon, mCenterWidthforWeatherIcon, weatherY, null);
            }

            Rect temBounds = new Rect();
            mTemPaint.getTextBounds(hightemp, 0, hightemp.length() - 1, temBounds);
            int heightOfTemp = temBounds.height();
            float TempY;

            //set the Y of the temp
            if (weatherIcon != null) {
                TempY = weatherY + weatherIcon.getHeight();
                 HihTempX = mCenterWidth-weatherIcon.getWidth()/2f-mTemPaint.measureText(hightemp);
                LowTempX = mCenterWidth+weatherIcon.getWidth()/2f;
                //draw the high temp
                canvas.drawText(hightemp, HihTempX, TempY, mTemPaint);

                //draw the low temp
                canvas.drawText(lowtemp, LowTempX, TempY, mTemPaint);


            } else {
                TempY = weatherY;

            }


            //set the x of the high tem
            //draw the tempecture and humidity

//            canvas.drawText(lowtemp + " " + hightemp, mXOffset, TempY, mTemPaint);


//            canvas.drawText(lowtemp + " " + hightemp, mXOffset, TempY, mTemPaint);
        }

        @Override
        public void onVisibilityChanged(boolean visible) {
            super.onVisibilityChanged(visible);
            /* the watch face became visible or invisible */
            if (visible) {
                registerReceiver();
                registerTextReceiver();

                // Update time zone in case it changed while we weren't visible.
                mTime.clear(TimeZone.getDefault().getID());
                mTime.setToNow();
            } else {
                unregisterReceiver();
                unregisterTextReceiver();
            }

            // Whether the timer should be running depends on whether we're visible (as well as
            // whether we're in ambient mode), so we may need to start or stop the timer.
            updateTimer();
        }

        private void updateTimer() {
            mUpdateTimeHandler.removeMessages(MSG_UPDATE_TIME);
            if (shouldTimerBeRunning()) {
                mUpdateTimeHandler.sendEmptyMessage(MSG_UPDATE_TIME);
            }
        }

        private void registerReceiver() {
            if (mRegisteredTimeZoneReceiver) {
                return;
            }
            mRegisteredTimeZoneReceiver = true;
            IntentFilter filter = new IntentFilter(Intent.ACTION_TIMEZONE_CHANGED);
            MyWatchFace.this.registerReceiver(mTimeZoneReceiver, filter);
        }

        private void unregisterReceiver() {
            if (!mRegisteredTimeZoneReceiver) {
                return;
            }
            mRegisteredTimeZoneReceiver = false;
            MyWatchFace.this.unregisterReceiver(mTimeZoneReceiver);
        }

        private void registerTextReceiver() {

            IntentFilter filter = new IntentFilter();
            filter.addAction(getResources().getString(R.string.Text_RECEIVER_ACTION));
            MyWatchFace.this.registerReceiver(messageReceiver, filter);
        }

        private void unregisterTextReceiver() {

            if (messageReceiver != null)
                MyWatchFace.this.unregisterReceiver(messageReceiver);
        }
    }
}
