package io.github.tmatz.hackers_unistroke_keyboard;

import android.graphics.RectF;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;

abstract class OnTouchCursorGestureListener
implements OnTouchListener
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

    protected final ApplicationResources mResources;
    private final StateMachine mStateMachine = new StateMachine();

    public OnTouchCursorGestureListener(ApplicationResources resources)
    {
        mResources = resources;
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
        return mStateMachine.onTouch(v, e);
    }

    private class StateMachine implements Runnable
    {
        private final MotionTrack mTrack = new MotionTrack();
        private State mState = State.SLEEP;

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
                    mTrack.set(v, e);
                    return onTouchDown();

                case MotionEvent.ACTION_MOVE:
                    if (mTrack.view() == null)
                    {
                        break;
                    }
                    mTrack.set(e);
                    switch (mState)
                    {
                        case START:
                            onTouchMoveStart();
                            break;

                        case MOVE:
                            onTouchMoveMove();
                            break;

                        case REPEAT:
                            onTouchMoveRepeat();
                            break;

                        case BACK_TO_MOVE:
                            onTouchMoveBackToMove();
                            break;

                        default:
                            break;
                    }
                    break;

                case MotionEvent.ACTION_UP:
                    if (mTrack.view() == null)
                    {
                        break;
                    }
                    mTrack.set(e);
                    onTouchUp();
                    break;

                default:
                    break;
            }

            return false;
        }

        private boolean onTouchDown()
        {
            if (isSpecialOn())
            {
                mTrack.clear();
                return false;
            }

            mState = State.START;
            mTrack.setBasePosition(mTrack.position());
            mTrack.view().postDelayed(this, CURSOR_GESTURE_START_MS);
            return true;
        }

        private void onTouchMoveStart()
        {
            mTrack.trackPosition();
            if (mTrack.trackDistance() > mResources.getCursorTolerance())
            {
                sleep();
            }
        }

        private void onRunStart()
        {
            onStart();
            mState = State.MOVE;
        }

        private void onTouchMoveMove()
        {
            boolean isModifierOn = isModifierOn();

            if (!isInGestureArea(mTrack.event()) && !isModifierOn)
            {
                mState = State.REPEAT;
                mTrack.view().postDelayed(this, CURSOR_GESTURE_REPEAT_MS);
                return;
            }

            float cursorTolerance = mResources.getCursorTolerance();
            VectorF delta = mTrack.difference().cutoff(cursorTolerance);

            if (delta.x != 0)
            {
                onKey(delta.x < 0 ? KeyEvent.KEYCODE_DPAD_LEFT : KeyEvent.KEYCODE_DPAD_RIGHT);
                delta = new VectorF(delta.x, 0);
            }
            else if (delta.y != 0)
            {
                onKey(delta.y < 0 ? KeyEvent.KEYCODE_DPAD_UP : KeyEvent.KEYCODE_DPAD_DOWN);
            }
            else
            {
                return;
            }

            VectorF move = delta.normalizeEach().mult(cursorTolerance);
            mTrack.setBasePosition(mTrack.basePosition().add(move));

            if (isModifierOn)
            {
                sleep();
            }
        }

        private void onTouchMoveRepeat()
        {
            if (isInGestureArea(mTrack.event()))
            {
                mState = State.BACK_TO_MOVE;
                mTrack.view().removeCallbacks(this);
                mTrack.view().postDelayed(this, CURSOR_GESTURE_PAUSE_MS);
            }
        }

        private void onRunRepeat()
        {
            RectF bounds = getBounds();
            VectorF pos = mTrack.position();

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

            mTrack.view().postDelayed(this, CURSOR_GESTURE_REPEAT_MS);
        }

        private void onTouchMoveBackToMove()
        {
            // nop
        }

        private void onRunBackToMove()
        {
            mState = State.MOVE;
            mTrack.setBasePosition(mTrack.position());
        }

        private void onTouchUp()
        {
            sleep();
        }

        private void sleep()
        {
            mTrack.view().removeCallbacks(this);
            mTrack.clear();
            if (mState != mState.START)
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
