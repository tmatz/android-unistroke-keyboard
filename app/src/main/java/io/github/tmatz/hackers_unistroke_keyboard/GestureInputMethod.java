package io.github.tmatz.hackers_unistroke_keyboard;

import android.content.Context;
import android.gesture.Gesture;
import android.gesture.GestureLibraries;
import android.gesture.GestureLibrary;
import android.gesture.GestureOverlayView;
import android.gesture.Prediction;
import android.graphics.RectF;
import android.inputmethodservice.InputMethodService;
import android.os.Handler;
import android.os.SystemClock;
import android.os.Vibrator;
import android.view.GestureDetector;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import java.util.ArrayList;

public class GestureInputMethod
extends InputMethodService
{
    private static final int CURSOR_GESTURE_START_MS = 300;
    private static final int KEYREPEAT_DELAY_FIRST_MS = 400;
    private static final int KEYREPEAT_DELAY_MS = 100;
    private static final int VIBRATION_MS = 15;
    private static final int VIBRATION_STRONG_MS = 30;

    private static final int META_CAPS_LOCK = KeyEvent.META_CAPS_LOCK_ON;
    private static final int META_SHIFT = KeyEvent.META_SHIFT_ON | KeyEvent.META_SHIFT_LEFT_ON;
    private static final int META_CTRL = KeyEvent.META_CTRL_ON | KeyEvent.META_CTRL_LEFT_ON;
    private static final int META_ALT = KeyEvent.META_ALT_ON | KeyEvent.META_ALT_LEFT_ON;

    private final GestureStore mGestureStore = new GestureStore();
    private final ViewController mViewController = new ViewController();
    private final KeyboardController mKeyboardController = new KeyboardController();
    private final Handler mHandler = new Handler();

    @Override
    public void onCreate()
    {
        super.onCreate();
        mGestureStore.onCreate();
    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();
    }

    @Override
    public View onCreateInputView()
    {
        View view = mViewController.onCreateInputView();
        mKeyboardController.setState();
        return view;
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

    private void sendEvent(KeyEvent e)
    {
        getCurrentInputConnection().sendKeyEvent(e);
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

    private boolean vibrate()
    {
        Vibrator vibrator = (Vibrator)getSystemService(VIBRATOR_SERVICE);
        if (vibrator == null || !vibrator.hasVibrator())
        {
            return false;
        }
        vibrator.vibrate(VIBRATION_MS);
        return true;
    }

    private boolean vibrateStrong()
    {
        Vibrator vibrator = (Vibrator)getSystemService(VIBRATOR_SERVICE);
        if (vibrator == null || !vibrator.hasVibrator())
        {
            return false;
        }
        vibrator.vibrate(VIBRATION_STRONG_MS);
        return true;
    }

    private class GestureStore
    {
        private GestureLibrary mStoreAlpabet;
        private GestureLibrary mStoreNumber;
        private GestureLibrary mStoreSpecial;
        private GestureLibrary mStoreControl;

        public void onCreate()
        {
            mStoreAlpabet = createGesture(R.raw.gestures_alphabet);
            mStoreNumber = createGesture(R.raw.gestures_number);
            mStoreSpecial = createGesture(R.raw.gestures_special);
            mStoreControl = createGesture(R.raw.gestures_control);
        }

        private GestureLibrary createGesture(int rawId)
        {
            GestureLibrary store = GestureLibraries.fromRawResource(getApplicationContext(), rawId);
            store.setOrientationStyle(8);

            store.load();
            return store;
        }
    }

    private class ViewController
    {
        private ViewGroup mCenterPanel;
        private Button mButtonShift;
        private Button mButtonCtrl;
        private Button mButtonAlt;
        private TextView mInfo;
        private TextView mInfoNum;
        private TextView mInfoCurrent;

        public View onCreateInputView()
        {
            final View mainView = getLayoutInflater().inflate(R.layout.input_method, null);

            final ViewGroup keyboardArea = mainView.findViewById(R.id.keyboard_area);
            final View keyboardView = getLayoutInflater().inflate(R.layout.keyboard, keyboardArea);

            final View gestureArea = mainView.findViewById(R.id.gesture_area);
            final Button extendKey = mainView.findViewById(R.id.button_key);

            setupMainView(mainView);
            setupKeyboardView(keyboardView);
            setupExtendKey(extendKey, gestureArea, keyboardArea);

            return mainView;
        }

        private void setupMainView(View view)
        {
            mCenterPanel = view.findViewById(R.id.center_panel);
            mInfo = view.findViewById(R.id.info);
            mInfoNum = view.findViewById(R.id.info_num);
            mButtonShift = view.findViewById(R.id.button_shift);
            mButtonCtrl = view.findViewById(R.id.button_ctrl);
            mButtonAlt = view.findViewById(R.id.button_alt);

            setupGestureOverlays(view);
            setupButtonKey(view, R.id.button_shift);
            setupButtonKey(view, R.id.button_ctrl);
            setupButtonKey(view, R.id.button_alt);
            setupButtonKey(view, R.id.button_del);
            setupButtonKey(view, R.id.button_enter);
            mInfoCurrent = mInfo;
        }

        private void setupKeyboardView(final View view)
        {
            setupButtonKey(view, R.id.keyboard_button_h);
            setupButtonKey(view, R.id.keyboard_button_j);
            setupButtonKey(view, R.id.keyboard_button_k);
            setupButtonKey(view, R.id.keyboard_button_l);
            setupButtonKey(view, R.id.keyboard_button_z);
            setupButtonKey(view, R.id.keyboard_button_x);
            setupButtonKey(view, R.id.keyboard_button_c);
            setupButtonKey(view, R.id.keyboard_button_v);
            setupButtonKey(view, R.id.keyboard_button_home);
            setupButtonKey(view, R.id.keyboard_button_move_end);
            setupButtonKey(view, R.id.keyboard_button_dpad_left);
            setupButtonKey(view, R.id.keyboard_button_dpad_right);
            setupButtonKey(view, R.id.keyboard_button_dpad_up);
            setupButtonKey(view, R.id.keyboard_button_dpad_down);
            setupButtonKey(view, R.id.keyboard_button_forward_del);
        }

        private void setupGestureOverlays(View view)
        {
            final GestureOverlayView overlay = view.findViewById(R.id.gestures_overlay);
            final GestureOverlayView overlayNum = view.findViewById(R.id.gestures_overlay_num);

            overlay.addOnGestureListener(
                new OnGestureUnistrokeListener(mGestureStore.mStoreAlpabet)
                {
                    @Override
                    public void onGestureEnded(GestureOverlayView overlay, MotionEvent e)
                    {
                        mViewController.setAlphabetActive();
                        super.onGestureEnded(overlay, e);
                    }
                });

            overlayNum.addOnGestureListener(
                new OnGestureUnistrokeListener(mGestureStore.mStoreNumber)
                {
                    @Override
                    public void onGestureEnded(GestureOverlayView overlay, MotionEvent e)
                    {
                        mViewController.setNumberActive();
                        super.onGestureEnded(overlay, e);
                    }
                });

            final OnTouchCursorGestureListener onTouchCursorGestureListener =                 new OnTouchCursorGestureListener()
            {
                @Override
                protected void onRunStart()
                {
                    overlay.clear(false);
                    overlayNum.clear(false);

                    super.onRunStart();
                }
            };

            overlay.setOnTouchListener(onTouchCursorGestureListener);
            overlayNum.setOnTouchListener(onTouchCursorGestureListener);
        }

        private void setupExtendKey(final Button extendKey, final View unistrokeArea, final View keyboardArea)
        {
            keyboardArea.setVisibility(View.INVISIBLE);

            extendKey.setOnClickListener(
                new OnClickListener()
                {
                    @Override
                    public void onClick(View v)
                    {
                        toggleKeyboadOn();
                    }

                    private void toggleKeyboadOn()
                    {
                        if (keyboardArea.getVisibility() == View.VISIBLE)
                        {
                            keyboardArea.setVisibility(View.INVISIBLE);
                            unistrokeArea.setVisibility(View.VISIBLE);
                        }
                        else
                        {
                            keyboardArea.setVisibility(View.VISIBLE);
                            unistrokeArea.setVisibility(View.INVISIBLE);
                        }
                    }
                });
        }

        private void setupButtonKey(View rootView, int id)
        {
            final Button button = rootView.findViewById(id);
            final String tag = (String)button.getTag();
            final int keyCode = keyCodeFromTag(tag);
            switch (keyCode)
            {
                case KeyEvent.KEYCODE_UNKNOWN:
                    break;

                case KeyEvent.KEYCODE_SHIFT_LEFT:
                case KeyEvent.KEYCODE_SHIFT_RIGHT:
                    button.setOnTouchListener(new OnShiftKeyListener(keyCode));
                    break;

                case KeyEvent.KEYCODE_CTRL_LEFT:
                case KeyEvent.KEYCODE_CTRL_RIGHT:
                case KeyEvent.KEYCODE_ALT_LEFT:
                case KeyEvent.KEYCODE_ALT_RIGHT:
                    button.setOnTouchListener(new OnModifierKeyListener(keyCode));
                    break;

                default:
                    button.setOnTouchListener(new OnKeyListener(keyCode));
                    break;
            }
        }

        public void setShiftOn(boolean on)
        {
            mButtonShift.setBackgroundResource(on ? R.drawable.button_active : R.drawable.button);
        }

        public void setCapsOn(boolean on)
        {
            mButtonShift.setBackgroundResource(on ? R.drawable.button_locked : R.drawable.button);
        }

        public void setCtrlOn(boolean on)
        {
            mButtonCtrl.setBackgroundResource(on ? R.drawable.button_active : R.drawable.button);
        }

        public void setAltOn(boolean on)
        {
            mButtonAlt.setBackgroundResource(on ? R.drawable.button_active : R.drawable.button);
        }

        public void setSpecialOn(boolean on)
        {
            mInfoCurrent.setText(on ? "special" : "");
        }

        public void setAlphabetActive()
        {
            setActiveInfo(mInfo);
        }

        public void setNumberActive()
        {
            setActiveInfo(mInfoNum);
        }

        private void setActiveInfo(TextView info)
        {
            if (!mInfoCurrent.equals(info))
            {
                info.setText(mInfoCurrent.getText());
                mInfoCurrent.setText("");
                mInfoCurrent = info;
            }
        }

        public RectF getViewRect(View view)
        {
            int[] location = new int[2];
            view.getLocationOnScreen(location);

            int x = location[0];
            int y = location[1];
            int w = view.getWidth();
            int h = view.getHeight();

            return new RectF(x, y, x + w, y + h);
        }

        public RectF getCenterRect()
        {
            return getViewRect(mCenterPanel);
        }
    }

    private class KeyboardController
    {
        private int mMetaState;
        private boolean mSpecialOn;
        private boolean mShiftUsed;

        public void clearState()
        {
            mMetaState = 0;
            mSpecialOn = false;
            mShiftUsed = false;
        }

        private boolean isCapsLockOn()
        {
            return (mMetaState & META_CAPS_LOCK) != 0;
        }

        private boolean isShiftOn()
        {
            return (mMetaState & META_SHIFT) != 0;
        }

        private boolean isCtrlOn()
        {
            return (mMetaState & META_CTRL) != 0;
        }

        private boolean isAltOn()
        {
            return (mMetaState & META_ALT) != 0;
        }

        private boolean isSpecialOn()
        {
            return mSpecialOn;
        }

        private boolean isShiftUsed()
        {
            return mShiftUsed;
        }

        private void sendText(String str)
        {
            getCurrentInputConnection().commitText(str, str.length());
            mMetaState &= META_CAPS_LOCK;
            mSpecialOn = false;
            setState();
        }

        private void key(int keyCode)
        {
            keyDown(keyCode);
            keyUp(keyCode);
        }

        private void keyDown(int keyCode)
        {
            switch (keyCode)
            {
                case KeyEvent.KEYCODE_UNKNOWN:
                    break;

                case KeyEvent.KEYCODE_CTRL_LEFT:
                case KeyEvent.KEYCODE_CTRL_RIGHT:
                    onCtrlDown();
                    break;

                case KeyEvent.KEYCODE_SHIFT_LEFT:
                case KeyEvent.KEYCODE_SHIFT_RIGHT:
                    onShiftDown();
                    break;

                case KeyEvent.KEYCODE_ALT_LEFT:
                case KeyEvent.KEYCODE_ALT_RIGHT:
                    onAltDown();
                    break;

                case KeyEvent.KEYCODE_PERIOD:
                    onPeriodDown();
                    break;

                case KeyEvent.KEYCODE_DEL:
                case KeyEvent.KEYCODE_FORWARD_DEL:
                    onDelDown(keyCode);
                    break;

                case KeyEvent.KEYCODE_ENTER:
                    onEnterDown();
                    break;

                default:
                    sendKeyDown(keyCode);
                    break;
            }

            setState();
        }

        private void keyUp(int keyCode)
        {
            switch (keyCode)
            {
                case KeyEvent.KEYCODE_UNKNOWN:
                    break;

                case KeyEvent.KEYCODE_CTRL_LEFT:
                case KeyEvent.KEYCODE_CTRL_RIGHT:
                case KeyEvent.KEYCODE_SHIFT_LEFT:
                case KeyEvent.KEYCODE_SHIFT_RIGHT:
                case KeyEvent.KEYCODE_ALT_LEFT:
                case KeyEvent.KEYCODE_ALT_RIGHT:
                    mSpecialOn = false;
                    break;

                case KeyEvent.KEYCODE_PERIOD:
                    onPeriodUp();
                    break;

                case KeyEvent.KEYCODE_DPAD_LEFT:
                case KeyEvent.KEYCODE_DPAD_RIGHT:
                case KeyEvent.KEYCODE_DPAD_UP:
                case KeyEvent.KEYCODE_DPAD_DOWN:
                    onDPadUp(keyCode);
                    break;

                case KeyEvent.KEYCODE_DEL:
                case KeyEvent.KEYCODE_FORWARD_DEL:
                    onDelUp(keyCode);
                    break;

                case KeyEvent.KEYCODE_ENTER:
                    onEnterUp();
                    break;

                default:
                    sendKeyUp(keyCode);
                    mMetaState &= META_CAPS_LOCK;
                    mSpecialOn = false;
                    break;
            }

            setState();
        }

        private void onCtrlDown()
        {
            mMetaState ^= META_CTRL;

            // clear used SHIFT
            if (isShiftOn() && isShiftUsed())
            {
                mMetaState &= ~META_SHIFT;
                mShiftUsed = false;
            }
        }

        private void onShiftDown()
        {
            mShiftUsed = false;
            if (isCapsLockOn())
            {
                mMetaState &= ~META_CAPS_LOCK;
            }
            else if (isShiftOn())
            {
                mMetaState &= ~META_SHIFT;
                mMetaState |= META_CAPS_LOCK;
            }
            else
            {
                mMetaState |= META_SHIFT;
            }
        }

        private void onAltDown()
        {
            mMetaState ^= META_ALT;
        }

        private void onPeriodDown()
        {
            if (isSpecialOn())
            {
                sendKeyDown(KeyEvent.KEYCODE_PERIOD);
            }
        }

        private void onPeriodUp()
        {
            if (isSpecialOn())
            {
                sendKeyUp(KeyEvent.KEYCODE_PERIOD);
                mMetaState &= META_CAPS_LOCK;
                mSpecialOn = false;
            }
            else
            {
                mSpecialOn = true;
            }
        }

        private void onDelDown(int keyCode)
        {
            if (!isSpecialOn() && !isShiftOn() && !isCtrlOn() && !isAltOn())
            {
                sendKeyDown(keyCode);
            }
        }

        private void onDelUp(int keyCode)
        {
            if (!isSpecialOn() && !isShiftOn() && !isCtrlOn() && !isAltOn())
            {
                sendKeyUp(keyCode);
            }

            mMetaState &= META_CAPS_LOCK;
            mSpecialOn = false;
        }

        private void onEnterDown()
        {
            if (isEditorActionRequested())
            {
                getCurrentInputConnection().performEditorAction(getEditorAction());
                return;
            }

            sendKeyDown(KeyEvent.KEYCODE_ENTER);
        }

        private void onEnterUp()
        {
            if (isEditorActionRequested())
            {
                return;
            }

            sendKeyUp(KeyEvent.KEYCODE_ENTER);
            mMetaState &= META_CAPS_LOCK;
            mSpecialOn = false;
        }

        private void onDPadUp(int keyCode)
        {
            sendKeyUp(keyCode);
            mMetaState &= ~(META_CTRL | META_ALT);
            mSpecialOn = false;
            if (isShiftOn())
            {
                mShiftUsed = true;
            }
        }

        private void keyRepeat(int keyCode)
        {
            if (keyCode == KeyEvent.KEYCODE_UNKNOWN || KeyEvent.isModifierKey(keyCode))
            {
                return;
            }

            sendKeyRepeat(keyCode, mMetaState);
        }

        private boolean isShiftKey(int keyCode)
        {
            return keyCode == KeyEvent.KEYCODE_SHIFT_LEFT || keyCode == KeyEvent.KEYCODE_SHIFT_RIGHT;
        }

        private void sendKeyDown(int keyCode)
        {
            if (isShiftOn() && !isShiftKey(keyCode))
            {
                sendEvent(toKeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_SHIFT_LEFT, mMetaState & ~META_SHIFT));
            }

            sendEvent(toKeyEvent(KeyEvent.ACTION_DOWN, keyCode, mMetaState));
        }

        private void sendKeyUp(int keyCode)
        {
            sendEvent(toKeyEvent(KeyEvent.ACTION_UP, keyCode, mMetaState));

            if (mKeyboardController.isShiftOn() && !isShiftKey(keyCode))
            {
                sendEvent(toKeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_SHIFT_LEFT, mMetaState & ~META_SHIFT));
            }
        }

        private void setState()
        {
            if (isCapsLockOn())
            {
                mViewController.setCapsOn(true);
            }
            else
            {
                mViewController.setShiftOn(isShiftOn());
            }

            mViewController.setCtrlOn(isCtrlOn());
            mViewController.setAltOn(isAltOn());
            mViewController.setSpecialOn(isSpecialOn());
        }
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
                mKeyboardController.keyRepeat(mKeyCode);
                mHandler.postDelayed(mRunnable, KEYREPEAT_DELAY_MS);
            }
        };

        public OnKeyListener(int keyCode)
        {
            mKeyCode = keyCode;
        }

        @Override
        public boolean onTouch(View v, MotionEvent e)
        {
            final RectF rect = mViewController.getViewRect(v);
            switch (e.getAction())
            {
                case MotionEvent.ACTION_DOWN:
                    if (!rect.contains(e.getRawX(), e.getRawY()))
                    {
                        return false;
                    }

                    mKeyboardController.keyDown(mKeyCode);
                    mKeyDown = true;
                    mHandler.postDelayed(mRunnable, KEYREPEAT_DELAY_FIRST_MS);
                    return true;

                case MotionEvent.ACTION_UP:
                    if (!mKeyDown)
                    {
                        return false;
                    }

                    mHandler.removeCallbacks(mRunnable);
                    mKeyboardController.keyUp(mKeyCode);
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

        public OnModifierKeyListener(int keyCode)
        {
            mKeyCode = keyCode;
        }

        @Override
        public boolean onTouch(View v, MotionEvent e)
        {
            final RectF rect = mViewController.getViewRect(v);
            switch (e.getAction())
            {
                case MotionEvent.ACTION_DOWN:
                    if (!rect.contains(e.getRawX(), e.getRawY()))
                    {
                        return false;
                    }

                    mKeyboardController.keyDown(mKeyCode);
                    mKeyDown = true;
                    return true;

                case MotionEvent.ACTION_UP:
                    if (!mKeyDown)
                    {
                        return false;
                    }

                    mKeyboardController.keyUp(mKeyCode);
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

        public OnShiftKeyListener(int keyCode)
        {
            mKeyCode = keyCode;
        }

        @Override
        public boolean onDoubleTap(MotionEvent e)
        {
            mKeyboardController.key(KeyEvent.KEYCODE_CAPS_LOCK);
            return false;
        }

        @Override
        public boolean onTouch(View v, MotionEvent e)
        {

            final RectF rect = mViewController.getViewRect(v);
            switch (e.getAction())
            {
                case MotionEvent.ACTION_DOWN:
                    if (!rect.contains(e.getRawX(), e.getRawY()))
                    {
                        break;
                    }

                    mKeyboardController.keyDown(mKeyCode);
                    mKeyDown = true;
                    break;

                case MotionEvent.ACTION_UP:
                    if (!mKeyDown)
                    {
                        break;
                    }

                    mKeyboardController.keyUp(mKeyCode);
                    mKeyDown = false;
                    break;
            }

            super.onTouch(v, e);
            return false;
        }
    }

    private class OnGestureUnistrokeListener
    extends GestureOverlayViewOnGestureListener
    {
        private final float mPeriodTolerance = getResources().getDimension(R.dimen.period_tolerance);
        private static final PredictionResult sPredictionFailed = new PredictionResult();

        private final GestureLibrary mMainStore;

        public OnGestureUnistrokeListener(GestureLibrary mainStore)
        {
            mMainStore = mainStore;
        }

        @Override
        public void onGestureEnded(GestureOverlayView overlay, MotionEvent e)
        {
            final Gesture gesture = overlay.getGesture();
            PredictionResult prediction = sPredictionFailed;

            if (mKeyboardController.isSpecialOn())
            {
                prediction = getPrediction(prediction, gesture, mGestureStore.mStoreSpecial, 1.0);
            }
            else
            {
                prediction = getPrediction(prediction, gesture, mGestureStore.mStoreControl, 0.7);
                prediction = getPrediction(prediction, gesture, mMainStore, 1.0);
            }

            if (prediction.score == 0)
            {
                vibrateStrong();
                return;
            }

            String name = prediction.name;
            int keyCode = keyCodeFromTag(name);
            if (keyCode == KeyEvent.KEYCODE_UNKNOWN)
            {
                mKeyboardController.sendText(name);
                return;
            }

            mKeyboardController.key(keyCode);
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

            if (mKeyboardController.isCtrlOn())
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
    }

    private abstract class OnTouchCursorGestureListener
    implements OnTouchListener
    {
        private enum State
        {
            START,
            MOVE,
            REPEAT,
            BACK_TO_MOVE,
            FINISH,
        }

        private final float mCursorTolerance = getResources().getDimension(R.dimen.cursor_tolerance);

        private State mState;
        private float mCursorX;
        private float mCursorY;
        private float mMoveDistance;
        private MotionEvent mLastEvent;

        private final Runnable mRunnable = new Runnable()
        {
            @Override
            public void run()
            {
                switch (mState)
                {
                    case START:
                        onRunStart();
                        break;

                    case REPEAT:
                        onRunRepeat();
                        break;

                    case BACK_TO_MOVE:
                        onRunBackToMove();
                        break;

                    default:
                        break;
                }
            }
        };

        @Override
        public boolean onTouch(View v, MotionEvent e)
        {
            switch (e.getAction())
            {
                case MotionEvent.ACTION_DOWN:
                    onTouchDown(e);
                    break;

                case MotionEvent.ACTION_MOVE:
                    switch (mState)
                    {
                        case START:
                            onTouchMoveStart(e);
                            break;

                        case MOVE:
                            onTouchMoveMove(e);
                            break;

                        case REPEAT:
                            onTouchMoveRepeat(e);
                            break;

                        case BACK_TO_MOVE:
                            onTouchMoveBackToMove(e);
                            break;

                        default:
                            break;
                    }
                    break;

                case MotionEvent.ACTION_UP:
                    onTouchUp(e);
                    break;

                default:
                    break;
            }

            return true;
        }

        protected void onTouchDown(MotionEvent e)
        {
            if (mKeyboardController.isSpecialOn())
            {
                return;
            }

            mState = State.START;
            mLastEvent = e;
            mCursorX = e.getRawX();
            mCursorY = e.getRawY();
            mMoveDistance = 0;
            mHandler.postDelayed(mRunnable, CURSOR_GESTURE_START_MS);
        }

        protected void onTouchMoveStart(MotionEvent e)
        {
            float dx = Math.abs(e.getRawX() - mCursorX);
            float dy = Math.abs(e.getRawY() - mCursorY);

            mMoveDistance += dx + dy;
            mLastEvent = e;
            mCursorX = e.getRawX();
            mCursorY = e.getRawY();

            if (mMoveDistance > mCursorTolerance)
            {
                mState = State.FINISH;
                mHandler.removeCallbacks(mRunnable);
            }
        }

        protected void onRunStart()
        {
            mKeyboardController.mMetaState &= ~META_CTRL;
            mKeyboardController.setState();

            if (!vibrate())
            {
                Toast.makeText(GestureInputMethod.this, "cursor mode", Toast.LENGTH_SHORT).show();
            }

            mState = State.MOVE;
        }

        protected void onTouchMoveMove(MotionEvent e)
        {
            mLastEvent = e;

            if (!isInGestureArea(e))
            {
                mState = State.REPEAT;
                mHandler.postDelayed(mRunnable, 100);
                return;
            }

            final float dx = mLastEvent.getRawX() - mCursorX;
            final float dy = mLastEvent.getRawY() - mCursorY;

            if (Math.abs(dx) >= mCursorTolerance)
            {
                mKeyboardController.key(dx < 0 ? KeyEvent.KEYCODE_DPAD_LEFT : KeyEvent.KEYCODE_DPAD_RIGHT);
                mCursorX += Math.copySign(mCursorTolerance, dx);
            }

            if (Math.abs(dy) >= mCursorTolerance)
            {
                mKeyboardController.key(dy < 0 ? KeyEvent.KEYCODE_DPAD_UP : KeyEvent.KEYCODE_DPAD_DOWN);
                mCursorY += Math.copySign(mCursorTolerance, dy);
            }
        }

        protected void onTouchMoveRepeat(MotionEvent e)
        {
            mLastEvent = e;

            if (isInGestureArea(e))
            {
                mState = State.BACK_TO_MOVE;
                mHandler.removeCallbacks(mRunnable);
                mHandler.postDelayed(mRunnable, 200);
            }
        }

        protected void onRunRepeat()
        {
            final RectF centerArea = mViewController.getCenterRect();
            final float ex = mLastEvent.getRawX();
            final float ey = mLastEvent.getRawY();

            if (!centerArea.contains(ex, centerArea.top))
            {
                mKeyboardController.key(ex < centerArea.left ? KeyEvent.KEYCODE_DPAD_LEFT : KeyEvent.KEYCODE_DPAD_RIGHT);
            }

            if (!centerArea.contains(centerArea.left, ey))
            {
                mKeyboardController.key(ey < centerArea.top ? KeyEvent.KEYCODE_DPAD_UP : KeyEvent.KEYCODE_DPAD_DOWN);
            }

            mHandler.postDelayed(mRunnable, 100);
        }

        protected void onTouchMoveBackToMove(MotionEvent e)
        {
            mLastEvent = e;
        }

        protected void onRunBackToMove()
        {
            mState = State.MOVE;
            mCursorX = mLastEvent.getRawX();
            mCursorY = mLastEvent.getRawY();
        }

        protected void onTouchUp(MotionEvent e)
        {
            mState = State.FINISH;
            mHandler.removeCallbacks(mRunnable);
            mLastEvent = null;
        }

        private boolean isInGestureArea(MotionEvent e)
        {
            return mViewController.getCenterRect().contains(e.getRawX(), e.getRawY());
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
