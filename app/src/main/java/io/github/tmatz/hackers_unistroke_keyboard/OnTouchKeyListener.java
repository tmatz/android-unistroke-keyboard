package io.github.tmatz.hackers_unistroke_keyboard;

import android.content.Context;
import android.view.GestureDetector;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;

public class OnTouchKeyListener
implements OnTouchListener
{
    public enum FlickDirection
    {
        FLICK_LEFT,
        FLICK_RIGHT,
        FLICK_UP,
        FLICK_DOWN,
    }

    private final ApplicationResources mResources;
    private final int mKeyCode;
    private final PrivateKeyGestureDetector mDetector;

    public OnTouchKeyListener(Context context, ApplicationResources resources, int keyCode)
    {
        mResources = resources;
        mKeyCode = keyCode;
        mDetector = new PrivateKeyGestureDetector(context);
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

    protected void onFlick(int keyCode, FlickDirection direction)
    {
        // override
    }

    @Override
    public boolean onTouch(View view, MotionEvent e)
    {
        return mDetector.onTouchEvent(view, e);
    }

    private class PrivateKeyGestureDetector
    extends GestureDetector.SimpleOnGestureListener
    implements Runnable
    {
        private final GestureDetector mGestureDetector;
        private View mView;
        public boolean mLongPress = false;

        public PrivateKeyGestureDetector(Context context)
        {
            mGestureDetector = new GestureDetector(context, this);
        }

        @Override
        public void run()
        {
            onKeyRepeat(mKeyCode);
            mView.postDelayed(this, mResources.KEYREPEAT_DELAY_MS);
        }

        public boolean onTouchEvent(View view, MotionEvent e)
        {
            boolean result = mGestureDetector.onTouchEvent(e);
            switch (e.getAction())
            {
                case MotionEvent.ACTION_DOWN:
                    if (result)
                    {
                        onDown(view);
                    }
                    break;

                case MotionEvent.ACTION_UP:
                    onUp();
                    break;

                default:
                    break;
            }
            return result;
        }

        private void onDown(View view)
        {
            mView = view;
        }

        private void onUp()
        {
            if (mView == null)
            {
                return;
            }

            if (mLongPress)
            {
                onKeyUp(mKeyCode);
                mView.removeCallbacks(this);
            }

            mView = null;
        }

        @Override
        public boolean onDown(MotionEvent e)
        {
            return true;
        }

        @Override
        public boolean onSingleTapUp(MotionEvent e)
        {
            onKeyDown(mKeyCode);
            onKeyUp(mKeyCode);
            return true;
        }

        @Override
        public boolean onDoubleTap(MotionEvent e)
        {
            onKeyDown(mKeyCode);
            onKeyUp(mKeyCode);
            return true;
        }

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY)
        {
            return false;
        }

        @Override
        public void onLongPress(MotionEvent e)
        {
            mLongPress = true;
            onKeyDown(mKeyCode);
            if (!KeyEvent.isModifierKey(mKeyCode))
            {
                mView.postDelayed(this, mResources.KEYREPEAT_DELAY_MS);
            }
        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY)
        {
            onFlick(mKeyCode, getFlickDirection(velocityX, velocityY));
            return true;
        }

        private FlickDirection getFlickDirection(float vx, float vy)
        {
            if (Math.abs(vx) > Math.abs(vy))
            {
                return (vx < 0) ? FlickDirection.FLICK_LEFT : FlickDirection.FLICK_RIGHT;
            }
            else
            {
                return (vy < 0) ? FlickDirection.FLICK_UP : FlickDirection.FLICK_DOWN;
            }
        }
    }
}

