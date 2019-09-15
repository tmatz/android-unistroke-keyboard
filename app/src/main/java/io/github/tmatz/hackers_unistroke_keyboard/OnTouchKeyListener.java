package io.github.tmatz.hackers_unistroke_keyboard;

import android.graphics.RectF;
import android.os.Handler;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;

class OnTouchKeyListener
implements OnTouchListener
{
    private final int keyCode;
    private final ApplicationResources resources;
    private final KeyRepeatRunnable runnable = new KeyRepeatRunnable();
    private boolean mKeyPress;

    public OnTouchKeyListener(ApplicationResources resources, int keyCode)
    {
        this.resources = resources;
        this.keyCode = keyCode;
    }

    protected void onKeyDown(int keyCode)
    {
        // override
    }

    protected void onKeyUp(int keyCode)
    {
        // override
    }

    protected void onKeyRepeat(int keyCode)
    {
        // override
    }

    @Override
    public boolean onTouch(View v, MotionEvent e)
    {
        switch (e.getAction())
        {
            case MotionEvent.ACTION_DOWN:
                if (mKeyPress)
                {
                    return false;
                }
                RectF rect = ViewUtils.getViewRect(v);
                if (!rect.contains(e.getRawX(), e.getRawY()))
                {
                    return false;
                }

                mKeyPress = true;
                onKeyDown(keyCode);
                runnable.startRepeat();
                return true;

            case MotionEvent.ACTION_UP:
                if (!mKeyPress)
                {
                    return false;
                }

                mKeyPress = false;
                runnable.stopRepeat();
                onKeyUp(keyCode);
                return true;
        }

        return false;
    }

    private class KeyRepeatRunnable implements Runnable
    {
        private final Handler handler = new Handler();

        @Override
        public void run()
        {
            onKeyRepeat(keyCode);
            handler.postDelayed(this, resources.KEYREPEAT_DELAY_MS);
        }

        public void startRepeat()
        {
            if (!KeyEvent.isModifierKey(keyCode))
            {
                handler.postDelayed(this, resources.KEYREPEAT_DELAY_FIRST_MS);
            }
        }

        public void stopRepeat()
        {
            handler.removeCallbacks(this);
        }
    }
}

