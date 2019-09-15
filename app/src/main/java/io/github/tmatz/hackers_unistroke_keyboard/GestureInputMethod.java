package io.github.tmatz.hackers_unistroke_keyboard;

import android.gesture.Gesture;
import android.gesture.GestureOverlayView;
import android.graphics.RectF;
import android.inputmethodservice.InputMethodService;
import android.os.Handler;
import android.os.SystemClock;
import android.os.Vibrator;
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

public class GestureInputMethod
extends InputMethodService
implements IKeyboardService
{
    private static final int KEYREPEAT_DELAY_FIRST_MS = 400;
    private static final int KEYREPEAT_DELAY_MS = 100;
    private static final int VIBRATION_MS = 15;
    private static final int VIBRATION_STRONG_MS = 30;

    private ApplicationResources mResources;
    private ViewController mViewController;
    private final KeyboardViewModel mViewModel = new KeyboardViewModel(this);
    private final Handler mHandler = new Handler();

    @Override
    public void onCreate()
    {
        super.onCreate();
        mResources = new ApplicationResources(getApplicationContext());
        mViewController = new ViewController();
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

    public void updateView()
    {
        mViewController.update();
    }

    private int getEditorAction()
    {
        return getCurrentInputEditorInfo().imeOptions & (EditorInfo.IME_MASK_ACTION | EditorInfo.IME_FLAG_NO_ENTER_ACTION);
    }

    public boolean isEditorActionRequested()
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

    public void performEditorAction()
    {
        getCurrentInputConnection().performEditorAction(getEditorAction());
    }

    public void sendText(String str)
    {
        getCurrentInputConnection().commitText(str, str.length());
        mViewController.update();
    }

    public void sendKey(int action, int keyCode, int metaState)
    {
        long time = SystemClock.uptimeMillis();
        KeyEvent event = new KeyEvent(time, time, action, keyCode, 0, metaState);
        getCurrentInputConnection().sendKeyEvent(event);
    }

    public void sendKeyRepeat(int keyCode, int metaState)
    {
        long time = SystemClock.uptimeMillis();
        KeyEvent event = new KeyEvent(time, time, KeyEvent.ACTION_DOWN, keyCode, 1, metaState);
        getCurrentInputConnection().sendKeyEvent(event);
    }

    private boolean vibrate(boolean strong)
    {
        Vibrator vibrator = (Vibrator)getSystemService(VIBRATOR_SERVICE);
        if (vibrator == null || !vibrator.hasVibrator())
        {
            return false;
        }
        vibrator.vibrate(strong ? VIBRATION_STRONG_MS : VIBRATION_MS);
        return true;
    }

    private static class KeyEventUtils
    {
        public static int keyCodeFromTag(String tag)
        {
            return KeyEvent.keyCodeFromString("KEYCODE_" + tag.toUpperCase());
        }
    }

    private class ViewController
    {
        private ViewGroup mCenterPanel;
        private Button mButtonShift;
        private Button mButtonCtrl;
        private Button mButtonAlt;
        private final InfoView mInfoView = new InfoView();

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
            mInfoView.setup(mainView);

            update();

            return mainView;
        }

        private void setupMainView(View view)
        {
            mCenterPanel = view.findViewById(R.id.center_panel);
            mButtonShift = view.findViewById(R.id.button_shift);
            mButtonCtrl = view.findViewById(R.id.button_ctrl);
            mButtonAlt = view.findViewById(R.id.button_alt);

            setupGestureOverlays(view);
            setupButtonKey(view, R.id.button_shift);
            setupButtonKey(view, R.id.button_ctrl);
            setupButtonKey(view, R.id.button_alt);
            setupButtonKey(view, R.id.button_del);
            setupButtonKey(view, R.id.button_enter);
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
                new OnGestureUnistrokeListener(GestureStore.FLAG_CATEGORY_ALPHABET)
                {
                    @Override
                    public void onGestureEnded(GestureOverlayView overlay, MotionEvent e)
                    {
                        mViewController.setAlphabetActive();
                        super.onGestureEnded(overlay, e);
                    }
                });

            overlayNum.addOnGestureListener(
                new OnGestureUnistrokeListener(GestureStore.FLAG_CATEGORY_NUMBER)
                {
                    @Override
                    public void onGestureEnded(GestureOverlayView overlay, MotionEvent e)
                    {
                        mViewController.setNumberActive();
                        super.onGestureEnded(overlay, e);
                    }
                });

            final OnTouchCursorGestureListener onTouchCursorGestureListener =
                new GestureAreaOnTouchCursorGestureListener(mResources, overlay, overlayNum);

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
            final int keyCode = KeyEventUtils.keyCodeFromTag(tag);
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

        public void update()
        {
            if (mViewModel.isCapsLockOn())
            {
                mButtonShift.setBackgroundResource(R.drawable.button_locked);
            }
            else
            {
                mButtonShift.setBackgroundResource(mViewModel.isShiftOn() ? R.drawable.button_active : R.drawable.button);
            }

            mButtonCtrl.setBackgroundResource(mViewModel.isCtrlOn() ? R.drawable.button_active : R.drawable.button);
            mButtonAlt.setBackgroundResource(mViewModel.isAltOn() ? R.drawable.button_active : R.drawable.button);
            mInfoView.setText(mViewModel.isSpecialOn() ? "special" : "");
        }

        public void setAlphabetActive()
        {
            mInfoView.setAlphabetActive();
        }

        public void setNumberActive()
        {
            mInfoView.setNumberActive();
        }

        public RectF getCenterRect()
        {
            return ViewUtils.getViewRect(mCenterPanel);
        }

        private class InfoView
        {
            private TextView mInfo;
            private TextView mInfoNum;
            private TextView mInfoCurrent;

            public void setup(View view)
            {
                mInfo = view.findViewById(R.id.info);
                mInfoNum = view.findViewById(R.id.info_num);
                mInfoCurrent = mInfo;
            }

            public void setText(String text)
            {
                mInfoView.mInfoCurrent.setText(text);
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
                if (!mInfoView.mInfoCurrent.equals(info))
                {
                    info.setText(mInfoView.mInfoCurrent.getText());
                    mInfoView.mInfoCurrent.setText("");
                    mInfoView.mInfoCurrent = info;
                }
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
                    mViewModel.keyRepeat(mKeyCode);
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
                RectF rect = ViewUtils.getViewRect(v);
                switch (e.getAction())
                {
                    case MotionEvent.ACTION_DOWN:
                        if (!rect.contains(e.getRawX(), e.getRawY()))
                        {
                            return false;
                        }

                        mViewModel.keyDown(mKeyCode);
                        mKeyDown = true;
                        mHandler.postDelayed(mRunnable, KEYREPEAT_DELAY_FIRST_MS);
                        return true;

                    case MotionEvent.ACTION_UP:
                        if (!mKeyDown)
                        {
                            return false;
                        }

                        mHandler.removeCallbacks(mRunnable);
                        mViewModel.keyUp(mKeyCode);
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
                RectF rect = ViewUtils.getViewRect(v);
                switch (e.getAction())
                {
                    case MotionEvent.ACTION_DOWN:
                        if (!rect.contains(e.getRawX(), e.getRawY()))
                        {
                            return false;
                        }

                        mViewModel.keyDown(mKeyCode);
                        mKeyDown = true;
                        return true;

                    case MotionEvent.ACTION_UP:
                        if (!mKeyDown)
                        {
                            return false;
                        }

                        mViewModel.keyUp(mKeyCode);
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
                super(GestureInputMethod.this);
                mKeyCode = keyCode;
            }

            @Override
            public boolean onTouch(View v, MotionEvent e)
            {

                RectF rect = ViewUtils.getViewRect(v);
                switch (e.getAction())
                {
                    case MotionEvent.ACTION_DOWN:
                        if (!rect.contains(e.getRawX(), e.getRawY()))
                        {
                            break;
                        }

                        mViewModel.keyDown(mKeyCode);
                        mKeyDown = true;
                        break;

                    case MotionEvent.ACTION_UP:
                        if (!mKeyDown)
                        {
                            break;
                        }

                        mViewModel.keyUp(mKeyCode);
                        mKeyDown = false;
                        break;

                    default:
                        break;
                }

                super.onTouch(v, e);
                return false;
            }
        }

        private class OnGestureUnistrokeListener
        extends GestureOverlayViewOnGestureListener
        {
            private final int mFlags;

            public OnGestureUnistrokeListener(int flags)
            {
                mFlags = flags;
            }

            @Override
            public void onGestureEnded(GestureOverlayView overlay, MotionEvent e)
            {
                Gesture gesture = overlay.getGesture();
                PredictionResult prediction = mResources.gestures.recognize(gesture, makeFlags());
                if (prediction.score == 0)
                {
                    vibrate(true);
                    return;
                }

                String name = prediction.name;
                int keyCode = KeyEventUtils.keyCodeFromTag(name);
                if (keyCode == KeyEvent.KEYCODE_UNKNOWN)
                {
                    mViewModel.sendText(name);
                    return;
                }

                mViewModel.key(keyCode);
            }

            private int makeFlags()
            {
                int flags = mFlags;

                if (mViewModel.isSpecialOn())
                {
                    flags = GestureStore.FLAG_CATEGORY_SPECIAL;
                }
                else
                {
                    flags = mFlags | GestureStore.FLAG_CATEGORY_CONTROL;
                }

                if (mViewModel.isCtrlOn() || mViewModel.isAltOn())
                {
                    flags |= GestureStore.FLAG_STRICT;
                }

                return flags;
            }
        }

        private class GestureAreaOnTouchCursorGestureListener
        extends OnTouchCursorGestureListener
        {
            private final GestureOverlayView[] overlay;

            public GestureAreaOnTouchCursorGestureListener(ApplicationResources resources, GestureOverlayView ...overlay)
            {
                super(resources);
                this.overlay = overlay;
            }

            @Override
            protected boolean isSpecialOn()
            {
                return mViewModel.isSpecialOn();
            }

            @Override
            protected boolean isModifierOn()
            {
                return mViewModel.isCtrlOn() || mViewModel.isAltOn();
            }

            @Override
            protected void onKey(int keyCode)
            {
                mViewModel.key(keyCode);
            }

            @Override
            protected RectF getBounds()
            {
                return getCenterRect();
            }

            @Override
            protected void onStart()
            {
                mViewController.update();

                if (!vibrate(false))
                {
                    Toast.makeText(GestureInputMethod.this, "cursor mode", Toast.LENGTH_SHORT).show();
                }

                for (GestureOverlayView v: overlay)
                {
                    v.clear(false);
                }

                super.onStart();
            }
        };

    }
}
