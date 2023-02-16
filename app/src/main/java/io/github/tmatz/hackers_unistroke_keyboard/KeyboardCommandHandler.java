package io.github.tmatz.hackers_unistroke_keyboard;

import android.view.KeyEvent;
import java.util.HashMap;
import android.content.Context;
import android.os.Vibrator;
import android.widget.Toast;
import android.view.inputmethod.InputMethodManager;

interface IKeyboardState
{
    boolean isCapsLockOn();
    boolean isShiftOn();
    boolean isCtrlOn();
    boolean isAltOn();
    boolean isSpecialOn();
}

interface IKeyboardCommandHandler
{
    void clearState();
    void setShiftOn(boolean on);
    void sendText(String str);
    void key(int keyCode);
    void keyDown(int keyCode);
    void keyUp(int keyCode);
    void keyRepeat(int keyCode)
    void showInputMethodPicker();
    boolean vibrate(boolean strong)
    void toast(String message);
}

class KeyboardCommandHandler implements IKeyboardState, IKeyboardCommandHandler
{
    private static final int META_CAPS_LOCK = KeyEvent.META_CAPS_LOCK_ON;
    private static final int META_SHIFT = KeyEvent.META_SHIFT_ON | KeyEvent.META_SHIFT_LEFT_ON;
    private static final int META_CTRL = KeyEvent.META_CTRL_ON | KeyEvent.META_CTRL_LEFT_ON;
    private static final int META_ALT = KeyEvent.META_ALT_ON | KeyEvent.META_ALT_LEFT_ON;

    private final Context mContext;
    private final IKeyboardService mService;
    private final ApplicationResources mResources;
    private final KeyHandler mKeyHandler = new CompositeKeyHandler();
    private int mMetaState;
    private boolean mSpecialOn;
    private boolean mShiftUsed;

    public KeyboardCommandHandler(Context context, IKeyboardService service, ApplicationResources resources)
    {
        mContext = context;
        mService = service;
        mResources = resources;
    }

    public void clearState()
    {
        mMetaState = 0;
        mSpecialOn = false;
        mShiftUsed = false;
    }

    public boolean isCapsLockOn()
    {
        return (mMetaState & META_CAPS_LOCK) != 0;
    }

    public boolean isShiftOn()
    {
        return (mMetaState & META_SHIFT) != 0;
    }

    public void setShiftOn(boolean on)
    {
        if (on)
        {
            mMetaState &= ~META_CAPS_LOCK;
            mMetaState |= META_SHIFT;
            mShiftUsed = false;
        }
        else
        {
            mMetaState &= ~(META_SHIFT | META_CAPS_LOCK);
        }

        mService.updateView();
    }

    public boolean isCtrlOn()
    {
        return (mMetaState & META_CTRL) != 0;
    }

    public boolean isAltOn()
    {
        return (mMetaState & META_ALT) != 0;
    }

    public boolean isSpecialOn()
    {
        return mSpecialOn;
    }

    public void sendText(String str)
    {
        mMetaState &= META_CAPS_LOCK;
        mSpecialOn = false;
        mService.sendText(str);
    }

    public void key(int keyCode)
    {
        keyDown(keyCode);
        keyUp(keyCode);
    }

    public void keyDown(int keyCode)
    {
        mKeyHandler.down(keyCode);
        mService.updateView();
    }

    public void keyUp(int keyCode)
    {
        mKeyHandler.up(keyCode);
        mService.updateView();
    }

    public void keyRepeat(int keyCode)
    {
        if (keyCode == KeyEvent.KEYCODE_UNKNOWN || KeyEvent.isModifierKey(keyCode))
        {
            return;
        }

        mService.sendKeyRepeat(keyCode, mMetaState);
    }

    private static boolean isShiftKey(int keyCode)
    {
        return keyCode == KeyEvent.KEYCODE_SHIFT_LEFT || keyCode == KeyEvent.KEYCODE_SHIFT_RIGHT;
    }

    private void sendKeyDown(int keyCode)
    {
        if (isShiftOn() && !isShiftKey(keyCode))
        {
            mService.sendKey(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_SHIFT_LEFT, mMetaState & ~META_SHIFT);
        }

        mService.sendKey(KeyEvent.ACTION_DOWN, keyCode, mMetaState);
    }

    private void sendKeyUp(int keyCode)
    {
        mService.sendKey(KeyEvent.ACTION_UP, keyCode, mMetaState);

        if (isShiftOn() && !isShiftKey(keyCode))
        {
            mService.sendKey(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_SHIFT_LEFT, mMetaState & ~META_SHIFT);
        }
    }

    public void showInputMethodPicker()
    {
        InputMethodManager manager = (InputMethodManager)mContext.getSystemService(mContext.INPUT_METHOD_SERVICE);
        if (manager != null)
        {
            manager.showInputMethodPicker();
        }
    }


    public boolean vibrate(boolean strong)
    {
        Vibrator vibrator = (Vibrator)mContext.getSystemService(mContext.VIBRATOR_SERVICE);
        if (vibrator == null || !vibrator.hasVibrator())
        {
            return false;
        }
        vibrator.vibrate(strong ? mResources.VIBRATION_STRONG_MS : mResources.VIBRATION_MS);
        return true;
    }

    public void toast(String message)
    {
        Toast.makeText(mContext, message, Toast.LENGTH_SHORT).show();
    }

    private class KeyHandler
    {
        public void down(int keyCode)
        {
            // nop
        }

        public void up(int keyCode)
        {
            // nop
        }
    }

    private class CompositeKeyHandler extends KeyHandler
    {
        private final HashMap<Integer, KeyHandler> mKeyHandlers = new HashMap<>();
        private final KeyHandler mDefaultKeyHandler;

        public CompositeKeyHandler()
        {
            mDefaultKeyHandler = new DefaultKeyHandler();
            add(new KeyHandler(), KeyEvent.KEYCODE_UNKNOWN);
            add(new CtrlKeyHandler(), KeyEvent.KEYCODE_CTRL_LEFT, KeyEvent.KEYCODE_CTRL_RIGHT);
            add(new ShiftKeyHandler(), KeyEvent.KEYCODE_SHIFT_LEFT, KeyEvent.KEYCODE_SHIFT_RIGHT);
            add(new AltKeyHandler(), KeyEvent.KEYCODE_ALT_LEFT, KeyEvent.KEYCODE_ALT_RIGHT);
            add(new EnterKeyHandler(), KeyEvent.KEYCODE_ENTER);
            add(new PeriodKeyHandler(), KeyEvent.KEYCODE_PERIOD);
            add(new DelKeyHandler(), KeyEvent.KEYCODE_DEL, KeyEvent.KEYCODE_FORWARD_DEL);
            add(new MoveKeyHandler(), KeyEvent.KEYCODE_MOVE_HOME, KeyEvent.KEYCODE_MOVE_END, KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_DPAD_RIGHT, KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_DPAD_DOWN);
        }

        private void add(KeyHandler handler, int ...keyCode)
        {
            for (int k: keyCode)
            {
                mKeyHandlers.put(k, handler);
            }
        }

        @Override
        public void down(int keyCode)
        {
            getKeyHandler(keyCode).down(keyCode);
        }

        @Override
        public void up(int keyCode)
        {
            getKeyHandler(keyCode).up(keyCode);
        }

        private KeyHandler getKeyHandler(int keyCode)
        {
            if (mKeyHandlers.containsKey(keyCode))
            {
                return mKeyHandlers.get(keyCode);
            }
            else
            {
                return mDefaultKeyHandler;
            }
        }

        private class CtrlKeyHandler extends KeyHandler
        {
            @Override
            public void down(int keyCode)
            {
                mMetaState ^= META_CTRL;

                // clear used SHIFT
                if (isShiftOn() && mShiftUsed)
                {
                    mMetaState &= ~META_SHIFT;
                    mShiftUsed = false;
                }
            }

            @Override
            public void up(int keyCode)
            {
                mSpecialOn = false;
            }
        }

        private class ShiftKeyHandler extends KeyHandler
        {
            @Override
            public void down(int keyCode)
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

            @Override
            public void up(int keyCode)
            {
                mSpecialOn = false;
            }
        }

        private class AltKeyHandler extends KeyHandler
        {
            @Override
            public void down(int keyCode)
            {
                mMetaState ^= META_ALT;
            }

            @Override
            public void up(int keyCode)
            {
                mSpecialOn = false;
            }
        }

        private class EnterKeyHandler extends KeyHandler
        {
            @Override
            public void down(int keyCode)
            {
                if (!mService.isEditorActionRequested())
                {
                    mDefaultKeyHandler.down(keyCode);
                }
                else
                {
                    mService.performEditorAction();
                }
            }

            @Override
            public void up(int keyCode)
            {
                if (!mService.isEditorActionRequested())
                {
                    mDefaultKeyHandler.up(keyCode);
                }
            }
        }

        private class PeriodKeyHandler extends KeyHandler
        {
            @Override
            public void down(int keyCode)
            {
                if (isSpecialOn())
                {
                    mDefaultKeyHandler.down(keyCode);
                }
            }

            @Override
            public void up(int keyCode)
            {
                if (isSpecialOn())
                {
                    mDefaultKeyHandler.up(keyCode);
                }
                else
                {
                    mSpecialOn = true;
                }
            }
        }

        private class DelKeyHandler extends KeyHandler
        {
            @Override
            public void down(int keyCode)
            {
                if (!isSpecialOn() && !isShiftOn() && !isCtrlOn() && !isAltOn())
                {
                    mDefaultKeyHandler.down(keyCode);
                }
            }

            @Override
            public void up(int keyCode)
            {
                if (!isSpecialOn() && !isShiftOn() && !isCtrlOn() && !isAltOn())
                {
                    mDefaultKeyHandler.up(keyCode);
                }

                mMetaState &= META_CAPS_LOCK;
                mSpecialOn = false;
            }
        }

        private class MoveKeyHandler extends KeyHandler
        {
            @Override
            public void down(int keyCode)
            {
                sendKeyDown(keyCode);
            }

            @Override
            public void up(int keyCode)
            {
                sendKeyUp(keyCode);
                if (isCtrlOn() || isAltOn())
                {
                    mMetaState &= ~(META_SHIFT | META_CTRL | META_ALT);
                }
                else
                {
                    mMetaState &= ~(META_CTRL | META_ALT);
                }
                mSpecialOn = false;
                if (isShiftOn())
                {
                    mShiftUsed = true;
                }
            }
        }

        private class DefaultKeyHandler extends KeyHandler
        {
            @Override
            public void down(int keyCode)
            {
                sendKeyDown(keyCode);
            }

            @Override
            public void up(int keyCode)
            {
                sendKeyUp(keyCode);
                mMetaState &= META_CAPS_LOCK;
                mSpecialOn = false;
            }
        }
    }
}

