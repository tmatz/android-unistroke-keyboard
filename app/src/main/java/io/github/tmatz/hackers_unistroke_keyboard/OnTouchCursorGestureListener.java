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

    protected final ApplicationResources resources;
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
        private final MotionTrack track = new MotionTrack();
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
                    track.set(v, e);
                    return onTouchDown();

                case MotionEvent.ACTION_MOVE:
                    if (track.view() == null)
                    {
                        break;
                    }
                    track.set(e);
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
                    if (track.view() == null)
                    {
                        break;
                    }
                    track.set(e);
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
                track.clear();
                return false;
            }

            mState = State.START;
            track.setBasePosition(track.position());
            track.view().postDelayed(this, CURSOR_GESTURE_START_MS);
            return true;
        }

        private void onTouchMoveStart()
        {
            track.trackPosition();
            if (track.trackDistance() > resources.getCursorTolerance())
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

            if (!isInGestureArea(track.event()) && !isModifierOn)
            {
                mState = State.REPEAT;
                track.view().postDelayed(this, CURSOR_GESTURE_REPEAT_MS);
                return;
            }

            float cursorTolerance = resources.getCursorTolerance();
            VectorF delta = track.difference().cutoff(cursorTolerance);

            if (delta.x != 0)
            {
                onKey(delta.x < 0 ? KeyEvent.KEYCODE_DPAD_LEFT : KeyEvent.KEYCODE_DPAD_RIGHT);
                VectorF move = new VectorF(Math.copySign(cursorTolerance, delta.x), 0);
                track.setBasePosition(track.basePosition().add(move));
            }
            else if (delta.y != 0)
            {
                onKey(delta.y < 0 ? KeyEvent.KEYCODE_DPAD_UP : KeyEvent.KEYCODE_DPAD_DOWN);
                VectorF move = new VectorF(0, Math.copySign(cursorTolerance, delta.y));
                track.setBasePosition(track.basePosition().add(move));
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

        private void onTouchMoveRepeat()
        {
            if (isInGestureArea(track.event()))
            {
                mState = State.BACK_TO_MOVE;
                track.view().removeCallbacks(this);
                track.view().postDelayed(this, CURSOR_GESTURE_PAUSE_MS);
            }
        }

        private void onRunRepeat()
        {
            RectF bounds = getBounds();
            VectorF pos = track.position();

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

            track.view().postDelayed(this, CURSOR_GESTURE_REPEAT_MS);
        }

        private void onTouchMoveBackToMove()
        {
            // nop
        }

        private void onRunBackToMove()
        {
            mState = State.MOVE;
            track.setBasePosition(track.position());
        }

        private void onTouchUp()
        {
            sleep();
        }

        private void sleep()
        {
            track.view().removeCallbacks(this);
            track.clear();
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
