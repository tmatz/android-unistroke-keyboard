package io.github.tmatz.hackers_unistroke_keyboard;

import android.graphics.RectF;
import android.os.Handler;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;

abstract class OnTouchCursorGestureListener
implements OnTouchListener
{
    private static final int CURSOR_GESTURE_START_MS = 300;

    private enum State
    {
        SLEEP,
        START,
        MOVE,
        REPEAT,
        BACK_TO_MOVE,
    }

    protected final ApplicationResources resources;

    private State mState = State.SLEEP;
    private VectorF mBasePos = new VectorF();
    private float mMoveDistance;
    private MotionEvent mLastEvent;
    private final Handler mHandler = new Handler();

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

    public OnTouchCursorGestureListener(ApplicationResources resources)
    {
        this.resources = resources;
    }

    protected abstract boolean isSpecialOn();

    protected abstract boolean isModifierOn()

    protected abstract void key(int keyCode)

    protected abstract RectF getBounds();
    
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
        if (isSpecialOn())
        {
            return;
        }

        mState = State.START;
        mLastEvent = e;
        mBasePos = VectorF.fromEvent(e);
        mMoveDistance = 0;
        mHandler.postDelayed(mRunnable, CURSOR_GESTURE_START_MS);
    }

    protected void onTouchMoveStart(MotionEvent e)
    {
        mLastEvent = e;
        VectorF pos = VectorF.fromEvent(e);
        float length = pos.sub(mBasePos).fastLength();
        mBasePos = pos;
        mMoveDistance += length;

        if (mMoveDistance > resources.getCursorTolerance())
        {
            gotoSleep();
        }
    }

    protected void onRunStart()
    {
        mState = State.MOVE;
    }

    protected void onTouchMoveMove(MotionEvent e)
    {
        mLastEvent = e;

        boolean isModifierOn = isModifierOn();

        if (!isInGestureArea(e) && !isModifierOn)
        {
            mState = State.REPEAT;
            mHandler.postDelayed(mRunnable, 100);
            return;
        }

        float cursorTolerance = resources.getCursorTolerance();
        VectorF delta = VectorF.fromEvent(e).sub(mBasePos).cutoff(cursorTolerance);

        if (delta.x != 0)
        {
            key(delta.x < 0 ? KeyEvent.KEYCODE_DPAD_LEFT : KeyEvent.KEYCODE_DPAD_RIGHT);
            mBasePos.x += Math.copySign(cursorTolerance, delta.x);
        }
        else if (delta.y != 0)
        {
            key(delta.y < 0 ? KeyEvent.KEYCODE_DPAD_UP : KeyEvent.KEYCODE_DPAD_DOWN);
            mBasePos.y += Math.copySign(cursorTolerance, delta.y);
        }
        else
        {
            return;
        }

        if (isModifierOn)
        {
            gotoSleep();
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
        RectF bounds = getBounds();
        VectorF pos = VectorF.fromEvent(mLastEvent);

        if (!bounds.contains(pos.x, bounds.top))
        {
            key(pos.x < bounds.left ? KeyEvent.KEYCODE_DPAD_LEFT : KeyEvent.KEYCODE_DPAD_RIGHT);
        }
        else if (!bounds.contains(bounds.left, pos.y))
        {
            key(pos.y < bounds.top ? KeyEvent.KEYCODE_DPAD_UP : KeyEvent.KEYCODE_DPAD_DOWN);
        }

        if (isModifierOn())
        {
            gotoSleep();
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
        mBasePos = VectorF.fromEvent(mLastEvent);
    }

    protected void onTouchUp(MotionEvent e)
    {
        gotoSleep();
    }

    private void gotoSleep()
    {
        mState = State.SLEEP;
        mHandler.removeCallbacks(mRunnable);
        mLastEvent = null;
    }

    private boolean isInGestureArea(MotionEvent e)
    {
        return getBounds().contains(e.getRawX(), e.getRawY());
    }
}
