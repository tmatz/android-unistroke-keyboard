package io.github.tmatz.hackers_unistroke_keyboard;

import android.gesture.Gesture;
import android.gesture.GestureOverlayView;
import android.graphics.RectF;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.content.Context;

class KeyboardViewController
{
    private final Context mContext;
    private final ApplicationResources mResources;
    private final IKeyboardService mKeyboardService;
    private final KeyboardViewModel mViewModel;
    private ViewGroup mCenterPanel;
    private Button mButtonShift;
    private Button mButtonCtrl;
    private Button mButtonAlt;
    private final InfoView mInfoView = new InfoView();

    public KeyboardViewController(
        Context context,
        ApplicationResources resources,
        IKeyboardService keyboardService,
        KeyboardViewModel viewModel
    )
    {
        mContext = context;
        mResources = resources;
        mKeyboardService = keyboardService;
        mViewModel = viewModel;
    }

    public void destroy()
    {
        // nop
    }

    public void setup(View view)
    {
        setupMainView(view);
        setupKeyboardView(view);
        setupExtendKey(view);
        setupInfoView(view);
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

    private void setupInfoView(View view)
    {
        mInfoView.setup(view);
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
                    setAlphabetActive();
                    super.onGestureEnded(overlay, e);
                }
            });

        overlayNum.addOnGestureListener(
            new OnGestureUnistrokeListener(GestureStore.FLAG_CATEGORY_NUMBER)
            {
                @Override
                public void onGestureEnded(GestureOverlayView overlay, MotionEvent e)
                {
                    setNumberActive();
                    super.onGestureEnded(overlay, e);
                }
            });

        final OnTouchCursorGestureListener onTouchCursorGestureListener =
            new GestureAreaOnTouchCursorGestureListener(mResources, overlay, overlayNum);

        overlay.setOnTouchListener(onTouchCursorGestureListener);
        overlayNum.setOnTouchListener(onTouchCursorGestureListener);
    }

    private void setupExtendKey(final View view)
    {
        final View keyboardArea = view.findViewById(R.id.keyboard_area);
        final View gestureArea = view.findViewById(R.id.gesture_area);
        final Button extendKey = view.findViewById(R.id.button_key);

        keyboardArea.setVisibility(View.INVISIBLE);

        extendKey.setOnTouchListener(
            new OnTouchGestureListener(view.getContext())
            {           
                @Override
                public boolean onSingleTapUp(MotionEvent e)
                {
                    toggleKeyboadOn();
                    return true;
                }

                @Override
                public void onLongPress(MotionEvent e)
                {
                    mKeyboardService.showInputMethodPicker();
                }

                private void toggleKeyboadOn()
                {
                    if (keyboardArea.getVisibility() == View.VISIBLE)
                    {
                        keyboardArea.setVisibility(View.INVISIBLE);
                        gestureArea.setVisibility(View.VISIBLE);
                    }
                    else
                    {
                        keyboardArea.setVisibility(View.VISIBLE);
                        gestureArea.setVisibility(View.INVISIBLE);
                    }
                }
            });
    }

    private void setupButtonKey(View view, int id)
    {
        final Button button = view.findViewById(id);
        final String tag = (String)button.getTag();
        final int keyCode = KeyEventUtils.keyCodeFromTag(tag);
        if (keyCode != KeyEvent.KEYCODE_UNKNOWN)
        {
            button.setOnTouchListener(
                new OnTouchKeyListener(mContext, mResources, keyCode)
                {
                    @Override
                    protected void onKeyDown(int keyCode)
                    {
                        mViewModel.keyDown(keyCode);
                    }

                    @Override
                    protected void onKeyUp(int keyCode)
                    {
                        mViewModel.keyUp(keyCode);
                    }

                    @Override
                    protected void onKeyRepeat(int keyCode)
                    {
                        mViewModel.keyRepeat(keyCode);
                    }

                    @Override
                    protected void onFlick(int keyCode, FlickDirection direction)
                    {
                        if (keyCode == KeyEvent.KEYCODE_DEL && direction == FlickDirection.FLICK_LEFT)
                        {
                            mViewModel.key(KeyEvent.KEYCODE_FORWARD_DEL);
                        }
                        else if (keyCode == KeyEvent.KEYCODE_SHIFT_LEFT && direction == FlickDirection.FLICK_RIGHT)
                        {
                            mViewModel.setShiftOn(false);
                        }
                        else
                        {
                            mViewModel.key(keyCode);
                        }
                    }
                });
        }
    }

    public void update()
    {
        mButtonShift.setBackgroundResource(
            mViewModel.isCapsLockOn() ? R.drawable.button_locked :
            mViewModel.isShiftOn() ? R.drawable.button_active :
            R.drawable.button);
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

    private class OnGestureUnistrokeListener
    extends GestureOverlayViewOnGestureListener
    {
        private final int category;

        public OnGestureUnistrokeListener(int category)
        {
            this.category = category;
        }

        @Override
        public void onGestureEnded(GestureOverlayView overlay, MotionEvent e)
        {
            Gesture gesture = overlay.getGesture();
            PredictionResult prediction = mResources.gestures.recognize(gesture, makeFlags());
            if (prediction.score == 0)
            {
                mKeyboardService.vibrate(true);
                return;
            }

            int keyCode = KeyEventUtils.keyCodeFromTag(prediction.name);
            if (keyCode == KeyEvent.KEYCODE_UNKNOWN)
            {
                mViewModel.sendText(prediction.name);
            }
            else
            {
                mViewModel.key(keyCode);
            }
        }

        private int makeFlags()
        {
            int flags;

            if (mViewModel.isSpecialOn())
            {
                flags = GestureStore.FLAG_CATEGORY_SPECIAL;
            }
            else
            {
                flags = this.category | GestureStore.FLAG_CATEGORY_CONTROL;
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
            update();

            if (!mKeyboardService.vibrate(false))
            {
                mKeyboardService.toast("cursor mode");
            }

            for (GestureOverlayView v: overlay)
            {
                v.clear(false);
            }

            super.onStart();
        }
    };
}
