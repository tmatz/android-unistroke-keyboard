package io.github.tmatz.hackers_unistroke_keyboard;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.gesture.Gesture;
import android.gesture.GestureOverlayView;
import android.graphics.RectF;
import android.inputmethodservice.InputMethodService;
import android.os.Build;
import android.os.Handler;
import android.os.SystemClock;
import android.os.Vibrator;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.view.inputmethod.*;
import androidx.core.app.NotificationChannelCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.PackageManagerCompat;

public class GestureInputMethod
extends InputMethodService
implements IKeyboardService
{
    static private final String DEFAULT_NOTIFICATION_CANNEL = "default";
    static private final int DEFAULT_NOTIFICATION = 0;
    static private final String ACTION_OPEN_INPUT_METHOD = "custom.action.OPEN_INPUT_METHOD";

    private ApplicationResources resources;
    private ViewController mViewController;
    private final KeyboardViewModel mViewModel = new KeyboardViewModel(this);
    private BroadcastReceiver mReceiver;

    @Override
    public void onCreate()
    {
        super.onCreate();
        resources = new ApplicationResources(getApplicationContext());
        mViewController = new ViewController();
    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();
    }

    @Override
    public void onBindInput() {
        super.onBindInput();
    }

    @Override
    public void onUnbindInput() {
        super.onUnbindInput();
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
    public void onFinishInput() {
        super.onFinishInput();
        teardownNotification();
     }

    @Override
    public void onStartInputView(EditorInfo info, boolean restarting)
    {
        super.onStartInputView(info, restarting);
    }

    @Override
    public void onFinishInputView(boolean finishingInput)
    {
        super.onFinishInputView(finishingInput);
    }

    @Override
    public void onWindowHidden() {
        super.onWindowHidden();
        setupNotification();
    }

    @Override
    public void onWindowShown() {
        super.onWindowShown();
        teardownNotification();
    }

    public void updateView()
    {
        mViewController.update();
    }

    private void setupNotification()
    {
        if (Build.VERSION.SDK_INT < 28) {
            return;
        }
        mReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                switch (intent.getAction()) {
                    case ACTION_OPEN_INPUT_METHOD:
                        if (Build.VERSION.SDK_INT >= 28) {
                            requestShowSelf(0);
                        }
                        break;
                    default:
                        break;
                }
            }
        };
        var filter = new IntentFilter();
        filter.addAction(ACTION_OPEN_INPUT_METHOD);
        registerReceiver(mReceiver, filter);
        var notificationManager = NotificationManagerCompat.from(getApplicationContext());
        var channel = new NotificationChannelCompat
            .Builder(DEFAULT_NOTIFICATION_CANNEL, NotificationManagerCompat.IMPORTANCE_LOW)
            .setName(getString(R.string.default_notification_channel))
            .build();
        notificationManager.createNotificationChannel(channel);
        var notification = new NotificationCompat.Builder(getApplicationContext(), DEFAULT_NOTIFICATION_CANNEL)
            .setSmallIcon(android.R.drawable.ic_menu_edit)
            .setContentTitle(getString(R.string.open_input_method))
            .setContentIntent(PendingIntent.getBroadcast(this, 0, new Intent(ACTION_OPEN_INPUT_METHOD), PendingIntent.FLAG_IMMUTABLE))
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build();
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            notificationManager.notify(DEFAULT_NOTIFICATION, notification);
        }
    }

    private void teardownNotification() {
        var notififationManager = NotificationManagerCompat.from(getApplicationContext());
        notififationManager.cancel(DEFAULT_NOTIFICATION);
        if (mReceiver != null) {
            unregisterReceiver(mReceiver);
            mReceiver = null;
        }
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

    public void showInputMethodPicker()
    {
        InputMethodManager manager = (InputMethodManager)getSystemService(INPUT_METHOD_SERVICE);
        if (manager != null)
        {
            manager.showInputMethodPicker();
        }
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
        vibrator.vibrate(strong ? resources.VIBRATION_STRONG_MS : resources.VIBRATION_MS);
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

            setupMainView(mainView);
            setupKeyboardView(mainView);
            setupExtendKey(mainView);
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
                new GestureAreaOnTouchCursorGestureListener(resources, overlay, overlayNum);

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
                        showInputMethodPicker();
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

        private void setupButtonKey(View rootView, int id)
        {
            final Button button = rootView.findViewById(id);
            final String tag = (String)button.getTag();
            final int keyCode = KeyEventUtils.keyCodeFromTag(tag);
            if (keyCode != KeyEvent.KEYCODE_UNKNOWN)
            {
                button.setOnTouchListener(
                    new OnTouchKeyListener(GestureInputMethod.this, resources, keyCode)
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
                PredictionResult prediction = resources.gestures.recognize(gesture, makeFlags());
                if (prediction.score == 0)
                {
                    vibrate(true);
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

