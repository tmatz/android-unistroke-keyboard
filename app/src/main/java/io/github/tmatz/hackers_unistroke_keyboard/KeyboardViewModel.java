package io.github.tmatz.hackers_unistroke_keyboard;

import android.view.KeyEvent;
import java.util.HashMap;

class KeyboardViewModel
{
    private static final int META_CAPS_LOCK = KeyEvent.META_CAPS_LOCK_ON;
    private static final int META_SHIFT = KeyEvent.META_SHIFT_ON | KeyEvent.META_SHIFT_LEFT_ON;
    private static final int META_CTRL = KeyEvent.META_CTRL_ON | KeyEvent.META_CTRL_LEFT_ON;
    private static final int META_ALT = KeyEvent.META_ALT_ON | KeyEvent.META_ALT_LEFT_ON;

    private final IKeyboardService mService;
    private final KeyHandler mKeyHandler = new CompositeKeyHandler();
    private int mMetaState;
    private boolean mSpecialOn;
    private boolean mShiftUsed;

    public KeyboardViewModel(IKeyboardService service)
    {
        mService = service;
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

    private boolean isShiftKey(int keyCode)
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
            add(new DPadKeyHandler(), KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_DPAD_RIGHT, KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_DPAD_DOWN);
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
            mKeyHandlers.getOrDefault(keyCode, mDefaultKeyHandler).down(keyCode);
        }

        @Override
        public void up(int keyCode)
        {
            mKeyHandlers.getOrDefault(keyCode, mDefaultKeyHandler).up(keyCode);
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

        private class DPadKeyHandler extends KeyHandler
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

