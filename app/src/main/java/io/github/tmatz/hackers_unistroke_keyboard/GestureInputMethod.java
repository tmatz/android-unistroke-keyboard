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
import android.view.View.*;
import android.content.*;
import android.view.GestureDetector.*;

public class GestureInputMethod extends InputMethodService
{
    private boolean mStoreReady;

    private final File mFileAlpabet = getGesturePath("gestures.alphabet");
    private final File mFileNumber = getGesturePath("gestures.number");
    private final File mFileSpecial = getGesturePath("gestures.special");
    private final File mFileControl = getGesturePath("gestures.control");
    private final File mFileControlSingle = getGesturePath("gestures.control.single");

    private GestureLibrary mStoreAlpabet;
    private GestureLibrary mStoreNumber;
    private GestureLibrary mStoreSpecial;
    private GestureLibrary mStoreControl;
    private GestureLibrary mStoreControlSingle;

    private long mTimestampAlpabet;
    private long mTimestampNumber;
    private long mTimestampSpecial;
    private long mTimestampControl;
    private long mTimestampControlSingle;

    private View mView;
    private TextView mState;
    private boolean mSpecial;
    private int mMetaState;

    @Override
    public void onCreate()
    {
        super.onCreate();

        mStoreAlpabet = createGesture(mFileAlpabet);
        mStoreNumber = createGesture(mFileNumber);
        mStoreSpecial = createGesture(mFileSpecial);
        mStoreControl = createGesture(mFileControl);
        mStoreControlSingle = createGesture(mFileControlSingle);

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
        reloadGestures();

        mView = getLayoutInflater().inflate(R.layout.input_method, null);
        mState = mView.findViewById(R.id.state);

        final GestureOverlayView overlay = mView.findViewById(R.id.gestures_overlay);
        final TextView info = mView.findViewById(R.id.info);
        overlay.addOnGestureListener(new OnGestureListener(mStoreAlpabet, info));

        final GestureOverlayView overlayNum = mView.findViewById(R.id.gestures_overlay_num);
        final TextView infoNum = mView.findViewById(R.id.info_num);
        overlayNum.addOnGestureListener(new OnGestureListener(mStoreNumber, infoNum));

        final View leftPanelGesture = mView.findViewById(R.id.left_panel_gesture);
        leftPanelGesture.setOnTouchListener(
            new OnSwipeTouchListener(this)
            {
                @Override
                public void onSwipeRight()
                {
                    info.setText("swipe right");
                }
            });

        final View rightPanelGesture = mView.findViewById(R.id.right_panel_gesture);
        rightPanelGesture.setOnTouchListener(
            new OnSwipeTouchListener(this)
            {
                @Override
                public void onSwipeLeft()
                {
                    info.setText("swipe left");
                }
            });

        final Button buttonShift = mView.findViewById(R.id.button_shift);
        buttonShift.setOnClickListener(
            new OnClickListener()
            {
                @Override
                public void onClick(View p1)
                {
                    mMetaState ^= (KeyEvent.META_SHIFT_ON | KeyEvent.META_SHIFT_LEFT_ON);
                    mSpecial = false;
                    setState();
                }
            });

        final Button buttonCtrl = mView.findViewById(R.id.button_ctrl);
        buttonCtrl.setOnClickListener(
            new OnClickListener()
            {
                @Override
                public void onClick(View p1)
                {
                    mMetaState ^= (KeyEvent.META_CTRL_ON | KeyEvent.META_CTRL_LEFT_ON);
                    mSpecial = false;
                    setState();
                }
            });


        final Button buttonDel = mView.findViewById(R.id.button_del);
        buttonDel.setOnClickListener(
            new OnClickListener()
            {
                @Override
                public void onClick(View p1)
                {
                    key(KeyEvent.KEYCODE_DEL);
                    mMetaState = 0;
                    mSpecial = false;
                    setState();
                }
            });

        final Button buttonEnter = mView.findViewById(R.id.button_enter);
        buttonEnter.setOnClickListener(
            new OnClickListener()
            {
                @Override
                public void onClick(View p1)
                {
                    key(KeyEvent.KEYCODE_ENTER);
                    mMetaState = 0;
                    mSpecial = false;
                    setState();
                }
            });

        return mView;
    }

    @Override
    public void onStartInput(EditorInfo attribute, boolean restarting)
    {
        reloadGestures();
    }

    @Override
    public void onStartInputView(EditorInfo info, boolean restarting)
    {
        reloadGestures();
    }

    private static File getGesturePath(String fileName)
    {
        File baseDir = Environment.getExternalStorageDirectory();
        return new File(baseDir, fileName);
    }

    private static GestureLibrary createGesture(File file)
    {
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
        mTimestampAlpabet = mFileAlpabet.lastModified();

        mStoreNumber.load();
        mTimestampNumber = mFileNumber.lastModified();

        mStoreSpecial.load();
        mTimestampSpecial = mFileSpecial.lastModified();

        mStoreControl.load();
        mTimestampControl = mFileControl.lastModified();

        mStoreControlSingle.load();
        mTimestampControlSingle = mFileControlSingle.lastModified();

        mStoreReady = true;
    }

    private void reloadGestures()
    {
        if (!mStoreReady)
        {
            return;
        }

        long mtime;

        mtime = mFileAlpabet.lastModified();
        if (mtime != mTimestampAlpabet)
        {
            mStoreAlpabet.load();
            mTimestampAlpabet = mtime;
        }

        mtime = mFileNumber.lastModified();
        if (mtime != mTimestampNumber)
        {
            mStoreNumber.load();
            mTimestampNumber = mtime;
        }

        mtime = mFileSpecial.lastModified();
        if (mtime != mTimestampSpecial)
        {
            mStoreSpecial.load();
            mTimestampSpecial = mtime;
        }

        mtime = mFileControl.lastModified();
        if (mtime != mTimestampControl)
        {
            mStoreControl.load();
            mTimestampControl = mtime;
        }

        mtime = mFileControlSingle.lastModified();
        if (mtime != mTimestampControlSingle)
        {
            mStoreControlSingle.load();
            mTimestampControlSingle = mtime;
        }
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
        }

        @Override
        public void onGestureCancelled(GestureOverlayView overlay, MotionEvent event)
        {
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
                prediction = getPrediction(null, gesture, mStoreSpecial, 0.7);
            }
            else
            {
                prediction = getPrediction(null, gesture, mMainStore, 0.7);
                prediction = getPrediction(prediction, gesture, mStoreControl, 0.7);
                prediction = getPrediction(prediction, gesture, mStoreControlSingle, 0.2);
            }

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

                case KeyEvent.KEYCODE_DPAD_LEFT:
                case KeyEvent.KEYCODE_DPAD_RIGHT:
                case KeyEvent.KEYCODE_DPAD_UP:
                case KeyEvent.KEYCODE_DPAD_DOWN:
                    key(keyCode);
                    mMetaState &= ~KeyEvent.META_CTRL_MASK;
                    mSpecial = false;
                    break;

                case KeyEvent.KEYCODE_ENTER:
                    int action = getCurrentInputEditorInfo().actionId;
                    key(keyCode);
                    mMetaState = 0;
                    mSpecial = false;
                    if (action == EditorInfo.IME_ACTION_DONE)
                    {
                        getCurrentInputConnection().closeConnection();
                    }
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
    }

    private class OnSwipeTouchListener implements OnTouchListener
    {

        private final GestureDetector mGestureDetector;

        public OnSwipeTouchListener(Context context)
        {
            mGestureDetector = new GestureDetector(context, new GestureListener());
        }

        @Override
        public boolean onTouch(View p1, MotionEvent p2)
        {
            return mGestureDetector.onTouchEvent(p2);
        }

        public void onSwipeRight()
        {
        }

        public void onSwipeLeft()
        {
        }

        public void onSwipeTop()
        {
        }

        public void onSwipeBottom()
        {
        }

        private class GestureListener extends SimpleOnGestureListener
        {

            private static final int SWIPE_THRESHOLD = 100;
            private static final int SWIPE_VELOCITY_THRESHOLD = 100;

            @Override
            public boolean onDown(MotionEvent e)
            {
                return true;
            }

            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY)
            {
                boolean result = false;
                try
                {
                    float diffY = e2.getY() - e1.getY();
                    float diffX = e2.getX() - e1.getX();
                    if (Math.abs(diffX) > Math.abs(diffY))
                    {
                        if (Math.abs(diffX) > SWIPE_THRESHOLD && Math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD)
                        {
                            if (diffX > 0)
                            {
                                onSwipeRight();
                            }
                            else
                            {
                                onSwipeLeft();
                            }
                            result = true;
                        }
                    }
                    else if (Math.abs(diffY) > SWIPE_THRESHOLD && Math.abs(velocityY) > SWIPE_VELOCITY_THRESHOLD)
                    {
                        if (diffY > 0)
                        {
                            onSwipeBottom();
                        }
                        else
                        {
                            onSwipeTop();
                        }
                        result = true;
                    }
                }
                catch (Exception exception)
                {
                    exception.printStackTrace();
                }
                return result;
            }
        }
    }
}
