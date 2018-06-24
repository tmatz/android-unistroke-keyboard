package io.github.tmatz.hackers_unistroke_keyboard;

import java.util.ArrayList;

import android.app.Activity;
import android.gesture.Gesture;
import android.gesture.GestureLibrary;
import android.gesture.GestureOverlayView;
import android.gesture.Prediction;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;
import android.widget.*;
import android.inputmethodservice.*;
import android.view.inputmethod.*;
import java.io.*;
import android.os.*;
import android.gesture.*;
import android.view.*;
import android.content.pm.*;
import android.*;
import java.lang.reflect.*;

public class GestureInputMethod extends InputMethodService
{
    private boolean mStoreReady;
    private final GestureLibrary mStoreAlpabet = createGesture("gestures");
    private final GestureLibrary mStoreNumber = createGesture("gestures.number");
    private final GestureLibrary mStoreSpecial = createGesture("gestures.special");
    private final GestureLibrary mStoreControl = createGesture("gestures.control");
    private final GestureLibrary mStoreControlSingle = createGesture("gestures.control.single");
    private View mView;
    private TextView mState;
    private boolean mSpecial;
    private int mMetaState;

    @Override
    public void onCreate()
    {
        super.onCreate();

        loadGestures();
    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();
    }

    @Override
    public View onCreateInputView()
    {
        mView = getLayoutInflater().inflate(R.layout.input_method, null);
        mState = mView.findViewById(R.id.state);

        final GestureOverlayView overlay = mView.findViewById(R.id.gestures_overlay);
        final TextView info = mView.findViewById(R.id.info);
        overlay.addOnGestureListener(new OnGestureListener(mStoreAlpabet, info));

        final GestureOverlayView overlayNum = mView.findViewById(R.id.gestures_overlay_num);
        final TextView infoNum = mView.findViewById(R.id.info_num);
        overlayNum.addOnGestureListener(new OnGestureListener(mStoreNumber, infoNum));

        return mView;
    }

    @Override
    public void onStartInput(EditorInfo attribute, boolean restarting)
    {
    }

    private static GestureLibrary createGesture(String fileName)
    {
        File baseDir = Environment.getExternalStorageDirectory();
        File file = new File(baseDir, fileName);
        GestureLibrary store = GestureLibraries.fromFile(file);
        store.setOrientationStyle(8);
        return store;
    }

    private void loadGestures()
    {
        if (mStoreReady)
        {
            return;
        }

        if (!Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState()))
        {
            Toast.makeText(getApplicationContext(), "external storage not mounted", Toast.LENGTH_SHORT).show();
            return;
        }

        mStoreAlpabet.load();
        mStoreNumber.load();
        mStoreSpecial.load();
        mStoreControl.load();
        mStoreControlSingle.load();

        mStoreReady = true;
    }

    private void setState()
    {
        String state = "";
        if ((mMetaState & KeyEvent.META_SHIFT_MASK) != 0)
        {
            state = " shift";
        }

        if ((mMetaState & KeyEvent.META_CTRL_MASK) != 0)
        {
            state += " crtl";
        }

        mState.setText(state);
    }

    static class PredictionResult
    {
        public double score;
        public String name;
        public Prediction prediction;

        public PredictionResult()
        {
            this.score = 0;
        }

        public PredictionResult(Prediction prediction, double scale)
        {
            this.prediction = prediction;
            this.name = prediction.name;
            this.score = prediction.score * scale;
        }
    }

    class OnGestureListener implements GestureOverlayView.OnGestureListener
    {
        private final GestureLibrary mMainStore;
        private final TextView mInfo;

        public OnGestureListener(GestureLibrary mainStore, TextView info)
        {
            mMainStore = mainStore;
            mInfo = info;
        }

        @Override
        public void onGesture(GestureOverlayView overlay, MotionEvent event)
        {
            // TODO: Implement this method
        }

        @Override
        public void onGestureCancelled(GestureOverlayView overlay, MotionEvent event)
        {
            // TODO: Implement this method
        }

        @Override
        public void onGestureStarted(GestureOverlayView overlay, MotionEvent event)
        {
        }

        @Override
        public void onGestureEnded(GestureOverlayView overlay, MotionEvent event)
        {
            if (!mStoreReady)
            {
                mInfo.setText("not ready");
                return;
            }

            final Gesture gesture = overlay.getGesture();
            processGesture(gesture);

            setState();
        }

        private void processGesture(Gesture gesture)
        {
            PredictionResult prediction;

            if (mSpecial)
            {
                prediction = getPrediction(null, gesture, mStoreSpecial, 0.5);
            }
            else
            {
                prediction = getPrediction(null, gesture, mMainStore, 0.5);
            }

            prediction = getPrediction(prediction, gesture, mStoreControl, 0.5);
            prediction = getPrediction(prediction, gesture, mStoreControlSingle, 0.1);

            if (Double.isNaN(prediction.score))
            {
                if (mSpecial)
                {
                    mInfo.setText("period");
                    key(KeyEvent.KEYCODE_PERIOD);
                    mSpecial = false;
                }
                else
                {
                    mInfo.setText("special");
                    mSpecial = true;
                }

                return;
            }

            if (prediction.score < 1.0)
            {
                return;
            }

            String name = prediction.name;
            int keyCode = KeyEvent.keyCodeFromString("KEYCODE_" + name.toUpperCase());
            mInfo.setText(String.format("%s %d", name, keyCode));
            switch (keyCode)
            {
                case KeyEvent.KEYCODE_UNKNOWN:
                    getCurrentInputConnection().commitText(name, name.length());
                    mMetaState = 0;
                    mSpecial = false;
                    break;

                case KeyEvent.KEYCODE_SHIFT_LEFT:
                case KeyEvent.KEYCODE_SHIFT_RIGHT:
                    mMetaState ^= (KeyEvent.META_SHIFT_ON | KeyEvent.META_SHIFT_LEFT_ON);
                    mSpecial = false;
                    break;

                case KeyEvent.KEYCODE_CTRL_LEFT:
                case KeyEvent.KEYCODE_CTRL_RIGHT:
                    mMetaState ^= (KeyEvent.META_CTRL_ON | KeyEvent.META_CTRL_LEFT_ON);
                    mSpecial = false;
                    break;

                default:
                    key(keyCode);
                    mMetaState = 0;
                    mSpecial = false;
                    break;
            }
        }

        private PredictionResult getPrediction(PredictionResult previous, Gesture gesture, GestureLibrary store, double scale)
        {
            PredictionResult current;

            ArrayList<Prediction> predictions = store.recognize(gesture);
            if (predictions.size() > 0)
            {
                current = new PredictionResult(predictions.get(0), scale);
            }
            else
            {
                current = new PredictionResult();
            }

            if (previous == null || current.score > previous.score)
            {
                return current;
            }

            return previous;
        }

        private void sendEvent(KeyEvent event)
        {
            getCurrentInputConnection().sendKeyEvent(event);
        }

        private void key(int keyCode)
        {
            KeyEvent shift = null;
            if ((mMetaState & KeyEvent.META_SHIFT_MASK) != 0)
            {
                shift = toKeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_SHIFT_LEFT, 0);
                sendEvent(shift);
            }

            KeyEvent ctrl = null;
            if ((mMetaState & KeyEvent.META_CTRL_MASK) != 0)
            {
                ctrl = toKeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_CTRL_LEFT, mMetaState & KeyEvent.META_SHIFT_MASK);
                sendEvent(ctrl);
            }

            KeyEvent event = toKeyEvent(KeyEvent.ACTION_DOWN, keyCode, mMetaState);
            sendEvent(event);
            sendEvent(KeyEvent.changeAction(event, KeyEvent.ACTION_UP));

            if (ctrl != null)
            {
                sendEvent(KeyEvent.changeAction(ctrl, KeyEvent.ACTION_UP));
            }

            if (shift != null)
            {
                sendEvent(KeyEvent.changeAction(shift, KeyEvent.ACTION_UP));
            }
        }

        private KeyEvent toKeyEvent(int action, int keyCode, int metaState)
        {
            long eventTime = SystemClock.uptimeMillis();
            return new KeyEvent(
                eventTime, // downTime
                eventTime,
                action,
                keyCode,
                0, //repeat
                metaState);
        }
    }
}
