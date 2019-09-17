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
    protected final ApplicationResources resources;
    private final Handler mHandler = new Handler();
    private final StateMachine stateMachine = new StateMachine();

    public OnTouchCursorGestureListener(ApplicationResources resources)
    {
        this.resources = resources;
    }

    protected abstract boolean isSpecialOn();

    protected abstract boolean isModifierOn();

    protected abstract RectF getBounds();

    protected void onStart()
    {
        // override
    }

    protected void onFinish()
    {
        // override
    }

    protected void onKey(int keyCode)
    {
        // override
    }

    @Override
    public boolean onTouch(View v, MotionEvent e)
    {
        return stateMachine.onTouch(v, e);
    }

    private class StateMachine implements Runnable
    {
        private static final int CURSOR_GESTURE_START_MS = 300;
        private static final int CURSOR_GESTURE_REPEAT_MS = 100;
        private static final int CURSOR_GESTURE_PAUSE_MS = 200;

        private enum State
        {
            SLEEP,
            START,
            MOVE,
            REPEAT,
            BACK_TO_MOVE,
        }

        private State mState = State.SLEEP;
        private VectorF mBasePos = new VectorF();
        private float mMoveDistance;
        private MotionEvent mLastEvent;

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
            mHandler.postDelayed(this, CURSOR_GESTURE_START_MS);
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
                sleep();
            }
        }

        protected void onRunStart()
        {
            onStart();
            mState = State.MOVE;
        }

        protected void onTouchMoveMove(MotionEvent e)
        {
            mLastEvent = e;

            boolean isModifierOn = isModifierOn();

            if (!isInGestureArea(e) && !isModifierOn)
            {
                mState = State.REPEAT;
                mHandler.postDelayed(this, CURSOR_GESTURE_REPEAT_MS);
                return;
            }

            float cursorTolerance = resources.getCursorTolerance();
            VectorF delta = VectorF.fromEvent(e).sub(mBasePos).cutoff(cursorTolerance);

            if (delta.x != 0)
            {
                onKey(delta.x < 0 ? KeyEvent.KEYCODE_DPAD_LEFT : KeyEvent.KEYCODE_DPAD_RIGHT);
                mBasePos.x += Math.copySign(cursorTolerance, delta.x);
            }
            else if (delta.y != 0)
            {
                onKey(delta.y < 0 ? KeyEvent.KEYCODE_DPAD_UP : KeyEvent.KEYCODE_DPAD_DOWN);
                mBasePos.y += Math.copySign(cursorTolerance, delta.y);
            }
            else
            {
                return;
            }

            if (isModifierOn)
            {
                sleep();
            }
        }

        protected void onTouchMoveRepeat(MotionEvent e)
        {
            mLastEvent = e;

            if (isInGestureArea(e))
            {
                mState = State.BACK_TO_MOVE;
                mHandler.removeCallbacks(this);
                mHandler.postDelayed(this, CURSOR_GESTURE_PAUSE_MS);
            }
        }

        protected void onRunRepeat()
        {
            RectF bounds = getBounds();
            VectorF pos = VectorF.fromEvent(mLastEvent);

            if (!bounds.contains(pos.x, bounds.top))
            {
                onKey(pos.x < bounds.left ? KeyEvent.KEYCODE_DPAD_LEFT : KeyEvent.KEYCODE_DPAD_RIGHT);
            }
            else if (!bounds.contains(bounds.left, pos.y))
            {
                onKey(pos.y < bounds.top ? KeyEvent.KEYCODE_DPAD_UP : KeyEvent.KEYCODE_DPAD_DOWN);
            }

            if (isModifierOn())
            {
                sleep();
            }

            mHandler.postDelayed(this, CURSOR_GESTURE_REPEAT_MS);
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
            sleep();
        }

        private void sleep()
        {
            mHandler.removeCallbacks(this);
            mLastEvent = null;
            if (mState != State.SLEEP && mState != mState.START)
            {
                onFinish();
            }
            mState = State.SLEEP;
        }

        private boolean isInGestureArea(MotionEvent e)
        {
            return getBounds().contains(e.getRawX(), e.getRawY());
        }
    }
}
