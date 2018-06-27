package io.github.tmatz.hackers_unistroke_keyboard;

import android.content.*;
import android.gesture.*;
import android.inputmethodservice.*;
import android.os.*;
import android.view.*;
import android.view.GestureDetector.*;
import android.view.View.*;
import android.view.inputmethod.*;
import android.widget.*;
import java.io.*;
import java.util.*;

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
    private View mKeyboard;
    private Button mShift;
    private Button mCtrl;
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
        final ViewGroup gestureArea = mView.findViewById(R.id.gesture_area);
        final View unistrokeArea = mView.findViewById(R.id.unistroke_area);

        mKeyboard = getLayoutInflater().inflate(R.layout.keyboard, null);
        gestureArea.addView(mKeyboard);
        mKeyboard.setVisibility(View.INVISIBLE);

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
                    if (mKeyboard.getVisibility() == View.VISIBLE)
                    {
                        mKeyboard.setVisibility(View.INVISIBLE);
                        unistrokeArea.setVisibility(View.VISIBLE);
                    }
                    else
                    {
                        mKeyboard.setVisibility(View.VISIBLE);
                        unistrokeArea.setVisibility(View.INVISIBLE);
                    }
                }
            });

        final View rightPanelGesture = mView.findViewById(R.id.right_panel_gesture);
        rightPanelGesture.setOnTouchListener(
            new OnSwipeTouchListener(this)
            {
                @Override
                public void onSwipeLeft()
                {
                    if (mKeyboard.getVisibility() == View.VISIBLE)
                    {
                        mKeyboard.setVisibility(View.INVISIBLE);
                        unistrokeArea.setVisibility(View.VISIBLE);
                    }
                    else
                    {
                        mKeyboard.setVisibility(View.VISIBLE);
                        unistrokeArea.setVisibility(View.INVISIBLE);
                    }
                }
            });

        mShift = mView.findViewById(R.id.button_shift);
        mShift.setOnClickListener(
            new OnClickListener()
            {
                @Override
                public void onClick(View v)
                {
                    mMetaState ^= (KeyEvent.META_SHIFT_ON | KeyEvent.META_SHIFT_LEFT_ON);
                    mSpecial = false;
                    setState();
                }
            });

        mCtrl = mView.findViewById(R.id.button_ctrl);
        mCtrl.setOnClickListener(
            new OnClickListener()
            {
                @Override
                public void onClick(View v)
                {
                    mMetaState ^= (KeyEvent.META_CTRL_ON | KeyEvent.META_CTRL_LEFT_ON);
                    mSpecial = false;
                    setState();
                }
            });

        final Button buttonDel = mView.findViewById(R.id.button_del);
        buttonDel.setOnClickListener(new OnKeyListener(KeyEvent.KEYCODE_DEL));

        final Button buttonEnter = mView.findViewById(R.id.button_enter);
        buttonEnter.setOnClickListener(new OnKeyListener(KeyEvent.KEYCODE_ENTER));

        final Button keyboardButtonH = mKeyboard.findViewById(R.id.keyboard_button_h);
        keyboardButtonH.setOnClickListener(new OnKeyListener(KeyEvent.KEYCODE_H));

        final Button keyboardButtonJ = mKeyboard.findViewById(R.id.keyboard_button_j);
        keyboardButtonJ.setOnClickListener(new OnKeyListener(KeyEvent.KEYCODE_J));

        final Button keyboardButtonK = mKeyboard.findViewById(R.id.keyboard_button_k);
        keyboardButtonK.setOnClickListener(new OnKeyListener(KeyEvent.KEYCODE_K));

        final Button keyboardButtonL = mKeyboard.findViewById(R.id.keyboard_button_l);
        keyboardButtonL.setOnClickListener(new OnKeyListener(KeyEvent.KEYCODE_L));

        final Button keyboardButtonZ = mKeyboard.findViewById(R.id.keyboard_button_z);
        keyboardButtonZ.setOnClickListener(new OnKeyListener(KeyEvent.KEYCODE_Z));

        final Button keyboardButtonX = mKeyboard.findViewById(R.id.keyboard_button_x);
        keyboardButtonX.setOnClickListener(new OnKeyListener(KeyEvent.KEYCODE_X));

        final Button keyboardButtonC = mKeyboard.findViewById(R.id.keyboard_button_c);
        keyboardButtonC.setOnClickListener(new OnKeyListener(KeyEvent.KEYCODE_C));

        final Button keyboardButtonV = mKeyboard.findViewById(R.id.keyboard_button_v);
        keyboardButtonV.setOnClickListener(new OnKeyListener(KeyEvent.KEYCODE_V));

        final Button keyboardButtonHome = mKeyboard.findViewById(R.id.keyboard_button_home);
        keyboardButtonHome.setOnClickListener(new OnKeyListener(KeyEvent.KEYCODE_MOVE_HOME));

        final Button keyboardButtonEnd = mKeyboard.findViewById(R.id.keyboard_button_move_end);
        keyboardButtonEnd.setOnClickListener(new OnKeyListener(KeyEvent.KEYCODE_MOVE_END));

        final Button keyboardButtonLeft = mKeyboard.findViewById(R.id.keyboard_button_dpad_left);
        keyboardButtonLeft.setOnClickListener(new OnKeyListener(KeyEvent.KEYCODE_DPAD_LEFT));

        final Button keyboardButtonRight = mKeyboard.findViewById(R.id.keyboard_button_dpad_right);
        keyboardButtonRight.setOnClickListener(new OnKeyListener(KeyEvent.KEYCODE_DPAD_RIGHT));

        final Button keyboardButtonUp = mKeyboard.findViewById(R.id.keyboard_button_dpad_up);
        keyboardButtonUp.setOnClickListener(new OnKeyListener(KeyEvent.KEYCODE_DPAD_UP));

        final Button keyboardButtonDown = mKeyboard.findViewById(R.id.keyboard_button_dpad_down);
        keyboardButtonDown.setOnClickListener(new OnKeyListener(KeyEvent.KEYCODE_DPAD_DOWN));

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

    private void key(int keyCode)
    {
        switch (keyCode)
        {
            case KeyEvent.KEYCODE_UNKNOWN:
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
                sendKey(keyCode);
                mMetaState &= ~KeyEvent.META_CTRL_MASK;
                mSpecial = false;
                break;

            case KeyEvent.KEYCODE_ENTER:
                int action = getCurrentInputEditorInfo().actionId;
                sendKey(keyCode);
                mMetaState = 0;
                mSpecial = false;
                if (action == EditorInfo.IME_ACTION_DONE)
                {
                    getCurrentInputConnection().closeConnection();
                }
                break;

            case KeyEvent.KEYCODE_DEL:
                if (!mSpecial)
                {
                    sendKey(keyCode);
                }
                mMetaState = 0;
                mSpecial = false;
                break;

            default:
                sendKey(keyCode);
                mMetaState = 0;
                mSpecial = false;
                break;
        }

        setState();
    }

    private void setState()
    {
        if ((mMetaState & KeyEvent.META_SHIFT_MASK) != 0)
        {
            mShift.setBackgroundResource(R.drawable.button_active);
        }
        else
        {
            mShift.setBackgroundResource(R.drawable.button);
        }

        if ((mMetaState & KeyEvent.META_CTRL_MASK) != 0)
        {
            mCtrl.setBackgroundResource(R.drawable.button_active);
        }
        else
        {
            mCtrl.setBackgroundResource(R.drawable.button);
        }
    }

    private void sendEvent(KeyEvent event)
    {
        getCurrentInputConnection().sendKeyEvent(event);
    }

    private void sendKey(int keyCode)
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

    class OnKeyListener implements OnClickListener
    {
        private final int mKeyCode;

        public OnKeyListener(int keyCode)
        {
            mKeyCode = keyCode;
        }

        @Override
        public void onClick(View v)
        {
            key(mKeyCode);
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
                    sendKey(KeyEvent.KEYCODE_PERIOD);
                    mSpecial = false;
                }
                else
                {
                    mInfo.setText("special");
                    mSpecial = true;
                }

                setState();
                return;
            }

            if (prediction.score < 1.0)
            {
                return;
            }

            String name = prediction.name;
            int keyCode = KeyEvent.keyCodeFromString("KEYCODE_" + name.toUpperCase());
            mInfo.setText(String.format("%s %d", name, keyCode));

            if (keyCode ==  KeyEvent.KEYCODE_UNKNOWN)
            {
                getCurrentInputConnection().commitText(name, name.length());
                mMetaState = 0;
                mSpecial = false;
                setState();
                return;
            }

            key(keyCode);
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

    private class OnKeyTouchGestureListener
    implements OnTouchListener
    ,GestureDetector.OnGestureListener, GestureDetector.OnDoubleTapListener
    {
        private final GestureDetector mGestureDetector;

        public OnKeyTouchGestureListener()
        {
            mGestureDetector = new GestureDetector(GestureInputMethod.this, this);
            mGestureDetector.setOnDoubleTapListener(this);
        }

        @Override
        public boolean onTouch(View p1, MotionEvent p2)
        {
            return mGestureDetector.onTouchEvent(p2);
        }

        @Override
        public boolean onSingleTapConfirmed(MotionEvent p1)
        {
            return false;
        }

        @Override
        public boolean onDoubleTap(MotionEvent p1)
        {
            return false;
        }

        @Override
        public boolean onDoubleTapEvent(MotionEvent p1)
        {
            return false;
        }

        @Override
        public boolean onDown(MotionEvent p1)
        {
            return false;
        }

        @Override
        public void onShowPress(MotionEvent p1)
        {
        }

        @Override
        public boolean onSingleTapUp(MotionEvent p1)
        {
            return false;
        }

        @Override
        public boolean onScroll(MotionEvent p1, MotionEvent p2, float p3, float p4)
        {
            return false;
        }

        @Override
        public void onLongPress(MotionEvent p1)
        {
        }

        @Override
        public boolean onFling(MotionEvent p1, MotionEvent p2, float p3, float p4)
        {
            return false;
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
