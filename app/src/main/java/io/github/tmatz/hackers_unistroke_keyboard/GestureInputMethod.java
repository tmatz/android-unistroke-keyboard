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
import android.mtp.*;
import android.graphics.*;

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
    private final Handler mHandler = new Handler();

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

        final GestureOverlayView overlay = mView.findViewById(R.id.gestures_overlay);
        final TextView info = mView.findViewById(R.id.info);
        overlay.addOnGestureListener(new OnGestureUnistrokeListener(mStoreAlpabet, info));

        final GestureOverlayView overlayNum = mView.findViewById(R.id.gestures_overlay_num);
        final TextView infoNum = mView.findViewById(R.id.info_num);
        overlayNum.addOnGestureListener(new OnGestureUnistrokeListener(mStoreNumber, infoNum));

        overlay.setOnTouchListener(
            new OnTouchCursorGestureListener()
            {
                @Override
                public void onStartCursor(MotionEvent event)
                {
                    overlay.clear(false);
                    overlayNum.clear(false);

                    super.onStartCursor(event);
                }
            });

        overlayNum.setOnTouchListener(
            new OnTouchCursorGestureListener()
            {
                @Override
                public void onStartCursor(MotionEvent event)
                {
                    overlay.clear(false);
                    overlayNum.clear(false);

                    super.onStartCursor(event);
                }
            });

        final View leftPanelGesture = mView.findViewById(R.id.left_panel_gesture);
        leftPanelGesture.setOnTouchListener(
            new OnTouchSwipeListener()
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
            new OnTouchSwipeListener()
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
        mCtrl = mView.findViewById(R.id.button_ctrl);
        setKeyClickedListener(mView, R.id.button_shift);
        setKeyClickedListener(mView, R.id.button_ctrl);
        setKeyClickedListener(mView, R.id.button_del);
        setKeyClickedListener(mView, R.id.button_enter);

        setKeyClickedListener(mKeyboard, R.id.keyboard_button_h);
        setKeyClickedListener(mKeyboard, R.id.keyboard_button_j);
        setKeyClickedListener(mKeyboard, R.id.keyboard_button_k);
        setKeyClickedListener(mKeyboard, R.id.keyboard_button_l);
        setKeyClickedListener(mKeyboard, R.id.keyboard_button_z);
        setKeyClickedListener(mKeyboard, R.id.keyboard_button_x);
        setKeyClickedListener(mKeyboard, R.id.keyboard_button_c);
        setKeyClickedListener(mKeyboard, R.id.keyboard_button_v);
        setKeyClickedListener(mKeyboard, R.id.keyboard_button_home);
        setKeyClickedListener(mKeyboard, R.id.keyboard_button_move_end);
        setKeyClickedListener(mKeyboard, R.id.keyboard_button_dpad_left);
        setKeyClickedListener(mKeyboard, R.id.keyboard_button_dpad_right);
        setKeyClickedListener(mKeyboard, R.id.keyboard_button_dpad_up);
        setKeyClickedListener(mKeyboard, R.id.keyboard_button_dpad_down);

        gestureArea.addView(mKeyboard);
        mKeyboard.setVisibility(View.INVISIBLE);

        return mView;
    }

    private void setKeyClickedListener(View root, int id)
    {
        final Button button = root.findViewById(id);
        final Object tag = button.getTag();
        if (tag == null)
        {
            return;
        }

        button.setOnClickListener(
            new OnKeyListener((String)tag));
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

    @Override
    public void onFinishInputView(boolean finishingInput)
    {
        mHandler.removeCallbacks(null);
        super.onFinishInputView(finishingInput);
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

    private void sendEvent(KeyEvent e)
    {
        getCurrentInputConnection().sendKeyEvent(e);
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

    private int keyCodeFromTag(String tag)
    {
        return KeyEvent.keyCodeFromString("KEYCODE_" + tag.toUpperCase());
    }

    private static RectF getViewRect(View view)
    {
        int[] location = new int[2];
        view.getLocationOnScreen(location);

        int x = location[0];
        int y = location[1];
        int w = view.getWidth();
        int h = view.getHeight();

        return new RectF(x, y, x + w, y + h);
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
        private final String mTag;
        private final int mKeyCode;

        public OnKeyListener(String tag)
        {
            mTag = tag;
            mKeyCode = keyCodeFromTag(tag);
        }

        @Override
        public void onClick(View v)
        {
            if (mKeyCode != KeyEvent.KEYCODE_UNKNOWN)
            {
                key(mKeyCode);
            }
            else
            {
                getCurrentInputConnection().commitText(mTag, mTag.length());
                mMetaState = 0;
                mSpecial = false;
                setState();
            }
        }
    }

    class OnGestureUnistrokeListener extends GestureOverlayViewOnGestureListener
    {
        private final GestureLibrary mMainStore;
        private final TextView mInfo;

        public OnGestureUnistrokeListener(GestureLibrary mainStore, TextView info)
        {
            mMainStore = mainStore;
            mInfo = info;
        }

        @Override
        public void onGestureEnded(GestureOverlayView overlay, MotionEvent e)
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
            int keyCode = keyCodeFromTag(name);
            mInfo.setText(String.format("%s %d", name, keyCode));

            if (keyCode == KeyEvent.KEYCODE_UNKNOWN)
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

    private class OnTouchSwipeListener extends OnTouchGestureListener
    {
        private static final int SWIPE_THRESHOLD = 100;
        private static final int SWIPE_VELOCITY_THRESHOLD = 100;

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

        @Override
        public boolean onDown(MotionEvent e)
        {
            return true;
        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float vx, float vy)
        {
            boolean result = false;
            try
            {
                float diffY = e2.getY() - e1.getY();
                float diffX = e2.getX() - e1.getX();
                if (Math.abs(diffX) > Math.abs(diffY))
                {
                    if (Math.abs(diffX) > SWIPE_THRESHOLD && Math.abs(vx) > SWIPE_VELOCITY_THRESHOLD)
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
                else if (Math.abs(diffY) > SWIPE_THRESHOLD && Math.abs(vy) > SWIPE_VELOCITY_THRESHOLD)
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

    private abstract class OnTouchCursorGestureListener
    implements OnTouchListener
    {
        private final float mCursorTolerance = getResources().getDimension(R.dimen.cursor_tolerance);

        private final GestureDetector mLongPressDetector;
        private boolean mLongPress;
        private float mCursorX;
        private float mCursorY;
        private boolean mRepeating;
        private boolean mRepeated;
        private MotionEvent mLastEvent;
        private View mView;

        private final Runnable mRunnable = new Runnable()
        {
            @Override
            public void run()
            {
                repeatMoveCursor();
            }
        };

        public OnTouchCursorGestureListener()
        {
            mLongPressDetector = new GestureDetector(
                GestureInputMethod.this,
                new GestureDetectorOnGestureListener()
                {
                    @Override
                    public boolean onDown(MotionEvent e)
                    {
                        mLongPress = false;
                        return false;
                    }

                    @Override
                    public void onLongPress(MotionEvent e)
                    {
                        mLongPress = true;
                        onStartCursor(e);
                    }
                });
        }

        @Override
        public boolean onTouch(View v, MotionEvent e)
        {
            mLongPressDetector.onTouchEvent(e);

            if (!mLongPress)
            {
                return true;
            }

            switch (e.getAction())
            {
                case MotionEvent.ACTION_MOVE:
                    mView = v;
                    onMoveCursor(e);
                    break;

                case MotionEvent.ACTION_UP:
                    onFinishCursor(e);
                    mLongPress = false;
                    break;
            }

            return true;
        }

        public void onStartCursor(MotionEvent e)
        {
            mMetaState &= ~KeyEvent.META_CTRL_MASK;
            mSpecial = false;
            setState();

            Vibrator vibrator = (Vibrator)getSystemService(VIBRATOR_SERVICE);
            if (vibrator != null)
            {
                vibrator.vibrate(25);
            }

            mCursorX = e.getRawX();
            mCursorY = e.getRawY();
            mLastEvent = e;
            mRepeating = false;
            mRepeated = false;
        }

        public void onMoveCursor(MotionEvent e)
        {
            mLastEvent = e;
            moveCursor();
        }

        public void onFinishCursor(MotionEvent e)
        {
            mHandler.removeCallbacks(mRunnable);
            mRepeating = false;
            mRepeated = false;
        }

        private void moveCursor()
        {
            if (mRepeating || mRepeated)
            {
                return;
            }

            repeatMoveCursor();

            if (!mRepeating)
            {
                final float dx = mLastEvent.getRawX() - mCursorX;
                final float dy = mLastEvent.getRawY() - mCursorY;

                if (Math.abs(dx) >= mCursorTolerance)
                {
                    key(dx < 0 ? KeyEvent.KEYCODE_DPAD_LEFT : KeyEvent.KEYCODE_DPAD_RIGHT);
                    mCursorX += Math.copySign(mCursorTolerance, dx);
                }

                if (Math.abs(dy) >= mCursorTolerance)
                {
                    key(dy < 0 ? KeyEvent.KEYCODE_DPAD_UP : KeyEvent.KEYCODE_DPAD_DOWN);
                    mCursorY += Math.copySign(mCursorTolerance, dy);
                }
            }
        }

        private void repeatMoveCursor()
        {
            final RectF viewRect = getViewRect(mView);
            final float ex = mLastEvent.getRawX();
            final float ey = mLastEvent.getRawY();

            mRepeated = mRepeating;
            mRepeating = false;
            if (!viewRect.contains(ex, viewRect.top))
            {
                key(ex < viewRect.left ? KeyEvent.KEYCODE_DPAD_LEFT : KeyEvent.KEYCODE_DPAD_RIGHT);
                mRepeating = true;
            }
            if (!viewRect.contains(viewRect.left, ey))
            {
                key(ey < viewRect.top ? KeyEvent.KEYCODE_DPAD_UP : KeyEvent.KEYCODE_DPAD_DOWN);
                mRepeating = true;
            }

            if (mRepeating)
            {
                mHandler.postDelayed(mRunnable, 100);
            }
            else if (mRepeated)
            {
                mCursorX = mLastEvent.getRawX();
                mCursorY = mLastEvent.getRawY();
                mHandler.postDelayed(mRunnable, 100);
            }
        }
    }

    private abstract class OnTouchGestureListener
    extends GestureDetectorOnGestureListener
    implements OnTouchListener
    {
        private final GestureDetector mGestureDetector;

        public OnTouchGestureListener()
        {
            mGestureDetector = new GestureDetector(GestureInputMethod.this, this);
            mGestureDetector.setOnDoubleTapListener(this);
        }

        public GestureDetector getGetGestureDetector()
        {
            return mGestureDetector;
        }

        @Override
        public boolean onTouch(View view, MotionEvent e)
        {
            return mGestureDetector.onTouchEvent(e);
        }
    }


    private abstract class GestureDetectorOnGestureListener
    implements
    GestureDetector.OnGestureListener,
    GestureDetector.OnDoubleTapListener
    {
        @Override
        public boolean onSingleTapConfirmed(MotionEvent e)
        {
            return false;
        }

        @Override
        public boolean onDoubleTap(MotionEvent e)
        {
            return false;
        }

        @Override
        public boolean onDoubleTapEvent(MotionEvent e)
        {
            return false;
        }

        @Override
        public boolean onDown(MotionEvent e)
        {
            return false;
        }

        @Override
        public void onShowPress(MotionEvent e)
        {
        }

        @Override
        public boolean onSingleTapUp(MotionEvent e)
        {
            return false;
        }

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float dx, float dy)
        {
            return false;
        }

        @Override
        public void onLongPress(MotionEvent e)
        {
        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float vx, float vy)
        {
            return false;
        }
    }

    private abstract class GestureOverlayViewOnGestureListener implements GestureOverlayView.OnGestureListener
    {
        @Override
        public void onGesture(GestureOverlayView overlay, MotionEvent e)
        {
        }

        @Override
        public void onGestureCancelled(GestureOverlayView overlay, MotionEvent e)
        {
        }

        @Override
        public void onGestureStarted(GestureOverlayView overlay, MotionEvent e)
        {
        }

        @Override
        public void onGestureEnded(GestureOverlayView overlay, MotionEvent e)
        {
        }
    }
}
