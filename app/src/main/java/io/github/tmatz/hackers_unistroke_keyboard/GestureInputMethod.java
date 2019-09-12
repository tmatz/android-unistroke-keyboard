package io.github.tmatz.hackers_unistroke_keyboard;

import android.content.*;
import android.gesture.*;
import android.graphics.*;
import android.inputmethodservice.*;
import android.os.*;
import android.view.*;
import android.view.View.*;
import android.view.inputmethod.*;
import android.widget.*;
import java.util.*;

public class GestureInputMethod
extends InputMethodService
{
    private static final int CURSOR_GESTURE_START_MS = 300;
    private static final int KEYREPEAT_DELAY_FIRST_MS = 400;
    private static final int KEYREPEAT_DELAY_MS = 100;
    private static final int VIBRATION_MS = 15;
    private static final int VIBRATION_STRONG_MS = 30;

    private GestureLibrary mStoreAlpabet;
    private GestureLibrary mStoreNumber;
    private GestureLibrary mStoreSpecial;
    private GestureLibrary mStoreControl;

    private View mView;
    private ViewGroup mGestureArea;
    private View mKeyboard;
    private Button mShift;
    private Button mCtrl;
    private Button mAlt;
    private TextView mInfoAlphabet;
    private TextView mInfoNum;
    private boolean mSpecial;
    private int mMetaState;
    private boolean mShiftUsed;
    private final Handler mHandler = new Handler();

    @Override
    public void onCreate()
    {
        super.onCreate();

        mStoreAlpabet = createGesture(this, R.raw.gestures_alphabet);
        mStoreNumber = createGesture(this, R.raw.gestures_number);
        mStoreSpecial = createGesture(this, R.raw.gestures_special);
        mStoreControl = createGesture(this, R.raw.gestures_control);
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
        mGestureArea = mView.findViewById(R.id.gesture_area);
        final View unistrokeArea = mView.findViewById(R.id.unistroke_area);

        final GestureOverlayView overlay = mView.findViewById(R.id.gestures_overlay);
        mInfoAlphabet = mView.findViewById(R.id.info);
        overlay.addOnGestureListener(new OnGestureUnistrokeListener(mStoreAlpabet, mInfoAlphabet));

        final GestureOverlayView overlayNum = mView.findViewById(R.id.gestures_overlay_num);
        mInfoNum = mView.findViewById(R.id.info_num);
        overlayNum.addOnGestureListener(new OnGestureUnistrokeListener(mStoreNumber, mInfoNum));

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

        mShift = mView.findViewById(R.id.button_shift);
        mCtrl = mView.findViewById(R.id.button_ctrl);
        mAlt = mView.findViewById(R.id.button_alt);

        setupKey(mView, R.id.button_shift);
        setupKey(mView, R.id.button_ctrl);
        setupKey(mView, R.id.button_alt);
        setupKey(mView, R.id.button_del);
        setupKey(mView, R.id.button_enter);

        final Button extendKey = mView.findViewById(R.id.button_key);
        extendKey.setOnClickListener(
            new OnClickListener()
            {
                @Override
                public void onClick(View p1)
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

        mKeyboard = getLayoutInflater().inflate(R.layout.keyboard, null);

        setupKey(mKeyboard, R.id.keyboard_button_h);
        setupKey(mKeyboard, R.id.keyboard_button_j);
        setupKey(mKeyboard, R.id.keyboard_button_k);
        setupKey(mKeyboard, R.id.keyboard_button_l);
        setupKey(mKeyboard, R.id.keyboard_button_z);
        setupKey(mKeyboard, R.id.keyboard_button_x);
        setupKey(mKeyboard, R.id.keyboard_button_c);
        setupKey(mKeyboard, R.id.keyboard_button_v);
        setupKey(mKeyboard, R.id.keyboard_button_home);
        setupKey(mKeyboard, R.id.keyboard_button_move_end);
        setupKey(mKeyboard, R.id.keyboard_button_dpad_left);
        setupKey(mKeyboard, R.id.keyboard_button_dpad_right);
        setupKey(mKeyboard, R.id.keyboard_button_dpad_up);
        setupKey(mKeyboard, R.id.keyboard_button_dpad_down);
        setupKey(mKeyboard, R.id.keyboard_button_forward_del);

        mGestureArea.addView(mKeyboard);
        mKeyboard.setVisibility(View.INVISIBLE);

        return mView;
    }

    @Override
    public void onStartInput(EditorInfo attribute, boolean restarting)
    {
        super.onStartInput(attribute, restarting);
    }

    @Override
    public void onStartInputView(EditorInfo info, boolean restarting)
    {
        super.onStartInputView(info, restarting);
    }

    @Override
    public void onFinishInputView(boolean finishingInput)
    {
        mHandler.removeCallbacks(null);
        super.onFinishInputView(finishingInput);
    }

    private int getEditorAction()
    {
        return getCurrentInputEditorInfo().imeOptions & (EditorInfo.IME_MASK_ACTION | EditorInfo.IME_FLAG_NO_ENTER_ACTION);
    }

    private boolean isEditorActionRequested()
    {
        int action = getEditorAction();

        if ((action & EditorInfo.IME_FLAG_NO_ENTER_ACTION) != 0)
        {
            return false;
        }

        switch (action)
        {
            case EditorInfo.IME_ACTION_NONE:
            case EditorInfo.IME_ACTION_UNSPECIFIED:
                return false;

            default:
                return true;
        }
    }

    private void setupKey(View root, int id)
    {
        final Button button = root.findViewById(id);
        final String tag = (String)button.getTag();
        if (tag == null)
        {
            return;
        }

        final int keyCode = keyCodeFromTag(tag);
        if (KeyEvent.isModifierKey(keyCode))
        {
            switch (keyCode)
            {
                case KeyEvent.KEYCODE_SHIFT_LEFT:
                case KeyEvent.KEYCODE_SHIFT_RIGHT:
                    button.setOnTouchListener(new OnShiftKeyListener(tag));
                    break;

                default:
                    button.setOnTouchListener(new OnModifierKeyListener(tag));
                    break;
            }
        }
        else
        {
            button.setOnTouchListener(new OnKeyListener(tag));
        }
    }

    private static GestureLibrary createGesture(Context context, int rawId)
    {
        GestureLibrary store = GestureLibraries.fromRawResource(context, rawId);
        store.setOrientationStyle(8);

        store.load();
        return store;
    }

    private static boolean isCapsLockOn(int metaState)
    {
        return (metaState & KeyEvent.META_CAPS_LOCK_ON) != 0;
    }

    private boolean isCapsLockOn()
    {
        return isCapsLockOn(mMetaState);
    }

    private static boolean isShiftOn(int metaState)
    {
        return (metaState & KeyEvent.META_SHIFT_MASK) != 0;
    }

    private boolean isShiftOn()
    {
        return isShiftOn(mMetaState);
    }

    private static boolean isCtrlOn(int metaState)
    {
        return (metaState & KeyEvent.META_CTRL_MASK) != 0;
    }

    private boolean isCtrlOn()
    {
        return isCtrlOn(mMetaState);
    }

    private static boolean isAltOn(int metaState)
    {
        return (metaState & KeyEvent.META_ALT_MASK) != 0;
    }

    private boolean isAltOn()
    {
        return isAltOn(mMetaState);
    }

    private boolean isSpecialOn()
    {
        return mSpecial;
    }

    private void key(int keyCode)
    {
        keyDown(keyCode);
        keyUp(keyCode);
    }

    private void keyDown(int keyCode)
    {
        if (keyCode == KeyEvent.KEYCODE_UNKNOWN)
        {
            return;
        }
        else if (KeyEvent.isModifierKey(keyCode))
        {
            switch (keyCode)
            {
                case KeyEvent.KEYCODE_CTRL_LEFT:
                case KeyEvent.KEYCODE_CTRL_RIGHT:
                    if (isShiftOn() && mShiftUsed)
                    {
                        mMetaState &= ~KeyEvent.META_SHIFT_MASK;
                        mShiftUsed = false;
                    }

                    mMetaState ^= (KeyEvent.META_CTRL_ON | KeyEvent.META_CTRL_LEFT_ON);
                    break;

                case KeyEvent.KEYCODE_SHIFT_LEFT:
                case KeyEvent.KEYCODE_SHIFT_RIGHT:
                    if (isCapsLockOn())
                    {
                        mMetaState &= ~KeyEvent.META_SHIFT_MASK;
                        mMetaState &= ~KeyEvent.META_CAPS_LOCK_ON;
                    }
                    else if (isShiftOn())
                    {
                        mMetaState |= KeyEvent.META_CAPS_LOCK_ON;
                    }
                    else
                    {
                        mMetaState |= (KeyEvent.META_SHIFT_ON | KeyEvent.META_SHIFT_LEFT_ON);
                    }
                    mShiftUsed = false;
                    break;

                case KeyEvent.KEYCODE_ALT_LEFT:
                case KeyEvent.KEYCODE_ALT_RIGHT:
                    mMetaState ^= (KeyEvent.META_ALT_ON | KeyEvent.META_ALT_LEFT_ON);
                    break;
            }
        }
        else
        {
            switch (keyCode)
            {
                case KeyEvent.KEYCODE_DEL:
                case KeyEvent.KEYCODE_FORWARD_DEL:
                    if (!isSpecialOn() && !isShiftOn() && !isCtrlOn() && !isAltOn())
                    {
                        sendKeyDown(keyCode, mMetaState);
                    }
                    break;

                case KeyEvent.KEYCODE_ENTER:
                    if (isEditorActionRequested())
                    {
                        getCurrentInputConnection().performEditorAction(getEditorAction());
                        return;
                    }

                    if (isShiftOn(mMetaState))
                    {
                        sendKeyDown(KeyEvent.KEYCODE_SHIFT_LEFT, mMetaState & ~KeyEvent.META_SHIFT_MASK);
                    }

                    sendKeyDown(keyCode, mMetaState);
                    break;

                default:
                    if (isShiftOn(mMetaState))
                    {
                        sendKeyDown(KeyEvent.KEYCODE_SHIFT_LEFT, mMetaState & ~KeyEvent.META_SHIFT_MASK);
                    }

                    sendKeyDown(keyCode, mMetaState);
                    break;
            }
        }

        setState();
    }

    private void keyUp(int keyCode)
    {
        if (keyCode == KeyEvent.KEYCODE_UNKNOWN)
        {
            return;
        }
        else if (KeyEvent.isModifierKey(keyCode))
        {
            switch (keyCode)
            {
                case KeyEvent.KEYCODE_CTRL_LEFT:
                case KeyEvent.KEYCODE_CTRL_RIGHT:
                case KeyEvent.KEYCODE_SHIFT_LEFT:
                case KeyEvent.KEYCODE_SHIFT_RIGHT:
                case KeyEvent.KEYCODE_ALT_LEFT:
                case KeyEvent.KEYCODE_ALT_RIGHT:
                    mSpecial = false;
                    break;
            }
        }
        else
        {
            final int lastMetaState = mMetaState;
            switch (keyCode)
            {
                case KeyEvent.KEYCODE_DPAD_LEFT:
                case KeyEvent.KEYCODE_DPAD_RIGHT:
                case KeyEvent.KEYCODE_DPAD_UP:
                case KeyEvent.KEYCODE_DPAD_DOWN:
                    sendKeyUp(keyCode, mMetaState);
                    // clear CTRL, ALT
                    mMetaState &= KeyEvent.META_SHIFT_MASK | KeyEvent.META_CAPS_LOCK_ON;
                    if (isShiftOn())
                    {
                        mShiftUsed = true;
                    }
                    mSpecial = false;
                    break;

                case KeyEvent.KEYCODE_DEL:
                case KeyEvent.KEYCODE_FORWARD_DEL:
                    if (!isSpecialOn() && !isShiftOn() && !isCtrlOn() && !isAltOn())
                    {
                        sendKeyUp(keyCode, mMetaState);
                    }
                    // clear SHIFT, CTRL, ALT
                    mMetaState &= KeyEvent.META_CAPS_LOCK_ON;
                    mSpecial = false;
                    break;

                case KeyEvent.KEYCODE_ENTER:
                    if (isEditorActionRequested())
                    {
                        return;
                    }

                    sendKeyUp(keyCode, mMetaState);
                    mMetaState &= KeyEvent.META_CAPS_LOCK_ON;
                    mSpecial = false;
                    break;

                default:
                    sendKeyUp(keyCode, mMetaState);
                    mMetaState &= KeyEvent.META_CAPS_LOCK_ON;
                    mSpecial = false;
                    break;
            }

            if (isShiftOn(lastMetaState))
            {
                sendKeyUp(KeyEvent.KEYCODE_SHIFT_LEFT, mMetaState & ~KeyEvent.META_SHIFT_MASK);
            }
        }

        setState();
    }

    private void keyRepeat(int keyCode)
    {
        if (KeyEvent.isModifierKey(keyCode))
        {
            return;
        }

        switch (keyCode)
        {
            case KeyEvent.KEYCODE_UNKNOWN:
                break;

            default:
                sendKeyRepeat(keyCode, mMetaState);
                break;
        }
    }

    private void setState()
    {
        if (isCapsLockOn())
        {
            mShift.setBackgroundResource(R.drawable.button_locked);
        }
        else if (isShiftOn())
        {
            mShift.setBackgroundResource(R.drawable.button_active);
        }
        else
        {
            mShift.setBackgroundResource(R.drawable.button);
        }

        if (isCtrlOn())
        {
            mCtrl.setBackgroundResource(R.drawable.button_active);
        }
        else
        {
            mCtrl.setBackgroundResource(R.drawable.button);
        }

        if (isAltOn())
        {
            mAlt.setBackgroundResource(R.drawable.button_active);
        }
        else
        {
            mAlt.setBackgroundResource(R.drawable.button);
        }
    }

    private void sendEvent(KeyEvent e)
    {
        getCurrentInputConnection().sendKeyEvent(e);
    }

    private void sendKeyDown(int keyCode, int metaState)
    {
        sendEvent(toKeyEvent(KeyEvent.ACTION_DOWN, keyCode, metaState));
    }

    private void sendKeyUp(int keyCode, int metaState)
    {
        sendEvent(toKeyEvent(KeyEvent.ACTION_UP, keyCode, metaState));
    }

    private void sendKeyRepeat(int keyCode, int metaState)
    {
        sendEvent(toKeyEvent(KeyEvent.ACTION_MULTIPLE, keyCode, metaState));
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

    private void vibrate()
    {
        Vibrator vibrator = (Vibrator)getSystemService(VIBRATOR_SERVICE);
        if (vibrator != null)
        {
            vibrator.vibrate(VIBRATION_MS);
        }
    }

    private void vibrateStrong()
    {
        Vibrator vibrator = (Vibrator)getSystemService(VIBRATOR_SERVICE);
        if (vibrator != null)
        {
            vibrator.vibrate(VIBRATION_STRONG_MS);
        }
    }

    private void setInfo(TextView textView, String info)
    {
        mInfoAlphabet.setText("");
        mInfoNum.setText("");
        textView.setText(info);
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

    private static class PredictionResult
    {
        public final double score;
        public final String name;

        public PredictionResult()
        {
            this.score = 0;
            this.name = null;
        }

        public PredictionResult(String name, double score)
        {
            this.name = name;
            this.score = score;
        }

        public PredictionResult(Prediction prediction, double scale)
        {
            this.name = prediction.name;
            this.score = prediction.score * scale;
        }
    }

    private class OnKeyListener
    implements OnTouchListener
    {
        private final int mKeyCode;
        private boolean mKeyDown;

        private final Runnable mRunnable = new Runnable()
        {
            @Override
            public void run()
            {
                keyRepeat(mKeyCode);
                mHandler.postDelayed(mRunnable, KEYREPEAT_DELAY_MS);
            }
        };

        public OnKeyListener(String tag)
        {
            mKeyCode = keyCodeFromTag(tag);
        }

        @Override
        public boolean onTouch(View v, MotionEvent e)
        {
            final RectF rect = getViewRect(v);
            switch (e.getAction())
            {
                case MotionEvent.ACTION_DOWN:
                    if (!rect.contains(e.getRawX(), e.getRawY()))
                    {
                        return false;
                    }

                    keyDown(mKeyCode);
                    mKeyDown = true;
                    mHandler.postDelayed(mRunnable, KEYREPEAT_DELAY_FIRST_MS);
                    return true;

                case MotionEvent.ACTION_UP:
                    if (!mKeyDown)
                    {
                        return false;
                    }


                    mHandler.removeCallbacks(mRunnable);
                    keyUp(mKeyCode);
                    mKeyDown = false;
                    return true;
            }

            return false;
        }
    }

    private class OnModifierKeyListener
    implements OnTouchListener
    {
        private final int mKeyCode;
        private boolean mKeyDown;

        public OnModifierKeyListener(String tag)
        {
            mKeyCode = keyCodeFromTag(tag);
        }

        @Override
        public boolean onTouch(View v, MotionEvent e)
        {
            final RectF rect = getViewRect(v);
            switch (e.getAction())
            {
                case MotionEvent.ACTION_DOWN:
                    if (!rect.contains(e.getRawX(), e.getRawY()))
                    {
                        return false;
                    }

                    keyDown(mKeyCode);
                    mKeyDown = true;
                    return true;

                case MotionEvent.ACTION_UP:
                    if (!mKeyDown)
                    {
                        return false;
                    }

                    keyUp(mKeyCode);
                    mKeyDown = false;
                    return true;
            }

            return false;
        }
    }

    private class OnShiftKeyListener
    extends OnTouchGestureListener
    {
        private final int mKeyCode;
        private boolean mKeyDown;

        public OnShiftKeyListener(String tag)
        {
            mKeyCode = keyCodeFromTag(tag);
        }

        @Override
        public boolean onDoubleTap(MotionEvent e)
        {
            key(KeyEvent.KEYCODE_CAPS_LOCK);
            return false;
        }

        @Override
        public boolean onTouch(View v, MotionEvent e)
        {

            final RectF rect = getViewRect(v);
            switch (e.getAction())
            {
                case MotionEvent.ACTION_DOWN:
                    if (!rect.contains(e.getRawX(), e.getRawY()))
                    {
                        break;
                    }

                    keyDown(mKeyCode);
                    mKeyDown = true;
                    break;

                case MotionEvent.ACTION_UP:
                    if (!mKeyDown)
                    {
                        break;
                    }

                    keyUp(mKeyCode);
                    mKeyDown = false;
                    break;
            }

            super.onTouch(v, e);
            return false;
        }
    }

    class OnGestureUnistrokeListener
    extends GestureOverlayViewOnGestureListener
    {
        private final float mPeriodTolerance = getResources().getDimension(R.dimen.period_tolerance);
        private static final PredictionResult sPredictionFailed = new PredictionResult();

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
            final Gesture gesture = overlay.getGesture();
            PredictionResult prediction = sPredictionFailed;

            if (isSpecialOn())
            {
                prediction = getPrediction(prediction, gesture, mStoreSpecial, 1.0);
            }
            else
            {
                prediction = getPrediction(prediction, gesture, mStoreControl, 0.7);
                prediction = getPrediction(prediction, gesture, mMainStore, 1.0);
            }

            if (Double.isNaN(prediction.score))
            {
                if (isSpecialOn())
                {
                    setInfo("");
                    key(KeyEvent.KEYCODE_PERIOD);
                    return;
                }
                else
                {
                    setInfo("special");
                    mSpecial = true;
                    setState();
                    return;
                }
            }

            if (prediction.score == 0)
            {
                vibrateStrong();
                return;
            }

            String name = prediction.name;
            int keyCode = keyCodeFromTag(name);
            setInfo("");

            if (keyCode == KeyEvent.KEYCODE_UNKNOWN)
            {
                getCurrentInputConnection().commitText(name, name.length());
                mMetaState &= KeyEvent.META_CAPS_LOCK_ON;
                mSpecial = false;
                setState();
                return;
            }

            key(keyCode);
        }

        private PredictionResult getPrediction(PredictionResult previous, Gesture gesture, GestureLibrary store, double scale)
        {
            ArrayList<Prediction> predictions = store.recognize(gesture);
            if (predictions.size() == 0)
            {
                return previous;
            }

            if (gesture.getLength() < mPeriodTolerance)
            {
                return new PredictionResult("period", Double.NaN);
            }

            PredictionResult current = new PredictionResult(predictions.get(0), scale);
            if (previous.score > current.score)
            {
                return previous;
            }

            if (isCtrlOn())
            {
                if (current.score < 1.5)
                {
                    return previous;
                }

                if (predictions.size() > 1)
                {
                    PredictionResult next = new PredictionResult(predictions.get(1), scale);
                    if (current.score < next.score + 0.2)
                    {
                        return previous;
                    }
                }
            }

            return current;
        }

        private void setInfo(String info)
        {
            GestureInputMethod.this.setInfo(mInfo, info);
        }
    }

    private abstract class OnTouchCursorGestureListener
    implements OnTouchListener
    {
        private final float mCursorTolerance = getResources().getDimension(R.dimen.cursor_tolerance);

        private boolean mLongPress;
        private float mCursorX;
        private float mCursorY;
        private float mMoveDistance;
        private boolean mRepeating;
        private boolean mRepeated;
        private MotionEvent mLastEvent;

        private final Runnable mRunnable = new Runnable()
        {
            @Override
            public void run()
            {
                if (!mLongPress)
                {
                    if (!isSpecialOn())
                    {
                        mLongPress = true;
                        onStartCursor(mLastEvent);
                    }
                }
                else
                {
                    repeatMoveCursor(true);
                }
            }
        };

        @Override
        public boolean onTouch(View v, MotionEvent e)
        {
            switch (e.getAction())
            {
                case MotionEvent.ACTION_DOWN:
                    mLongPress = false;
                    mLastEvent = e;
                    mCursorX = e.getRawX();
                    mCursorY = e.getRawY();
                    mMoveDistance = 0;
                    mHandler.postDelayed(mRunnable, CURSOR_GESTURE_START_MS);
                    break;

                case MotionEvent.ACTION_MOVE:
                    if (!mLongPress)
                    {
                        float dx = Math.abs(e.getRawX() - mCursorX);
                        float dy = Math.abs(e.getRawY() - mCursorY);
                        mMoveDistance += dx + dy;
                        mCursorX = e.getRawX();
                        mCursorY = e.getRawY();
                        if (mMoveDistance > mCursorTolerance)
                        {
                            mHandler.removeCallbacks(mRunnable);
                        }
                    }
                    else
                    {
                        onMoveCursor(e);
                    }
                    break;

                case MotionEvent.ACTION_UP:
                    if (!mLongPress)
                    {
                        mHandler.removeCallbacks(mRunnable);
                    }
                    else
                    {
                        onFinishCursor(e);
                        mLongPress = false;
                    }
                    mLastEvent = null;
                    break;
            }

            return true;
        }

        public void onStartCursor(MotionEvent e)
        {
            mMetaState &= ~KeyEvent.META_CTRL_MASK;
            mSpecial = false;
            setState();

            Toast.makeText(GestureInputMethod.this, "cursor mode", Toast.LENGTH_SHORT).show();

            vibrate();

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

            repeatMoveCursor(false);

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

        private void repeatMoveCursor(boolean fromPost)
        {
            final RectF viewRect = getViewRect(mGestureArea);
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
                mHandler.postDelayed(mRunnable, 200);
            }
            else if (fromPost)
            {
                mCursorX = mLastEvent.getRawX();
                mCursorY = mLastEvent.getRawY();
            }
        }
    }

    private abstract class OnTouchGestureListener
    extends GestureDetector.SimpleOnGestureListener
    implements OnTouchListener
    {
        private final GestureDetector mGestureDetector;

        public OnTouchGestureListener()
        {
            mGestureDetector = new GestureDetector(GestureInputMethod.this, this);
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

    private abstract class GestureOverlayViewOnGestureListener
    implements GestureOverlayView.OnGestureListener
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
