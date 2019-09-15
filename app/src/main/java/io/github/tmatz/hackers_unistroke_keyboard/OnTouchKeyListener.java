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
    public enum FlickDirection
    {
        FLICK_LEFT,
        FLICK_RIGHT,
        FLICK_UP,
        FLICK_DOWN,
    }

    private final int keyCode;
    private final ApplicationResources resources;
    private final StateMachine stateMachine = new StateMachine();

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

    protected void onFlick(int keyCode, FlickDirection direction)
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
        private enum State
        {
            Sleep,
            Watch,
            KeyDown,
            Flick,
        }

        private final Handler handler = new Handler();
        private final MotionTrack track = new MotionTrack();
        private State mState = State.Sleep;

        public boolean onTouch(View v, MotionEvent e)
        {
            switch (e.getAction())
            {
                case MotionEvent.ACTION_DOWN:
                    if (mState != State.Sleep)
                    {
                        break;
                    }

                    track.set(v, e);
                    start();
                    return true;

                case MotionEvent.ACTION_MOVE:
                    if (mState == State.Sleep)
                    {
                        break;
                    }

                    track.set(v, e);
                    break;

                case MotionEvent.ACTION_UP:
                    if (mState == State.Sleep)
                    {
                        break;
                    }

                    track.set(v, e);
                    finish();
                    return true;

                default:
                    break;
            }

            return false;
        }

        @Override
        public void run()
        {
            switch (mState)
            {
                case Watch:
                    onWatched();
                    break;

                case KeyDown:
                    onRepeated();
                    break;
            }
        }

        private void start()
        {
            mState = State.Watch;
            track.reset();
            handler.postDelayed(this, 200);
        }

        private void onWatched()
        {
            if (maybeFlick())
            {
                mState = State.Flick;
            }
            else
            {
                mState = State.KeyDown;
                onKeyDown(keyCode);

                if (!KeyEvent.isModifierKey(keyCode))
                {
                    handler.postDelayed(this, resources.KEYREPEAT_DELAY_FIRST_MS);
                }
            }
        }

        public void onRepeated()
        {
            onKeyRepeat(keyCode);
            handler.postDelayed(this, resources.KEYREPEAT_DELAY_MS);
        }

        private void finish()
        {
            switch (mState)
            {
                case Watch:
                case Flick:
                    if (isFlick())
                    {
                        flick();
                    }
                    else
                    {
                        onKeyDown(keyCode);
                        onKeyUp(keyCode);
                    }
                    break;

                case KeyDown:
                    onKeyUp(keyCode);
                    break;

                default:
                    break;
            }

            mState = State.Sleep;
            handler.removeCallbacks(this);
            track.clear();
        }

        private void flick()
        {
            FlickDirection direction = getFlickDirection();
            onFlick(keyCode, direction);
        }

        private FlickDirection getFlickDirection()
        {
            VectorF v;

            if (contains(track.view(), track.event()))
            {
                v = track.vector();
            }
            else
            {
                VectorF pos = VectorF.fromEvent(track.event());
                RectF viewRect = ViewUtils.getViewRect(track.view());
                v = pos.cutoff(viewRect);
            }

            if (Math.abs(v.x) > Math.abs(v.y))
            {
                return (v.x < 0) ? FlickDirection.FLICK_LEFT : FlickDirection.FLICK_RIGHT;
            }
            else
            {
                return (v.y < 0) ? FlickDirection.FLICK_UP : FlickDirection.FLICK_DOWN;
            }
        }

        private boolean maybeFlick()
        {
            double distance = track.distance();
            return (distance > resources.getCursorTolerance());
        }

        private boolean isFlick()
        {
            double distance = track.distance();
            if (distance > resources.getCursorTolerance() * 2)
            {
                return true;
            }

            if ((distance > resources.getCursorTolerance()) &&
                !contains(track.view(), track.event()))
            {
                return true;
            }

            return false;
        }

        private boolean contains(View v, MotionEvent e)
        {
            return ViewUtils.getViewRect(v).contains(e.getRawX(), e.getRawY());
        }
    }
}

