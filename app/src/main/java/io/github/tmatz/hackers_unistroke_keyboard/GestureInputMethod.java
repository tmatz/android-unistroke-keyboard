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
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.view.inputmethod.*;

public class GestureInputMethod
extends InputMethodService
implements IKeyboardService
{
    private ApplicationResources mResources;
    private KeyboardCommandHandler mCommandHandler;
    private KeyboardView mKeyboardView;
    private final Handler mHandler = new Handler();

    @Override
    public void onCreate()
    {
        super.onCreate();
        mResources = new ApplicationResources(getApplicationContext());
        mCommandHandler = new KeyboardCommandHandler(this, this, mResources);
        mKeyboardView = new KeyboardView(this, mResources, mCommandHandler, mCommandHandler);
    }

    @Override
    public void onDestroy()
    {
        mKeyboardView.destroy();
        super.onDestroy();
    }

    @Override
    public View onCreateInputView()
    {
        final View view = getLayoutInflater().inflate(R.layout.input_method, null);
        mKeyboardView.setup(view);
        mKeyboardView.update();
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
        mKeyboardView.update();
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
        mKeyboardView.update();
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
}

