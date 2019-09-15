package io.github.tmatz.hackers_unistroke_keyboard;

import android.view.MotionEvent;
import android.view.View;

class MotionTrack
{
    private VectorF mBasePos = VectorF.Zero;
    private double mTrackDistance;
    private MotionEvent mEvent;
    private View mView;

    public void clear()
    {
        mView = null;
        mEvent = null;
    }

    public void set(View v, MotionEvent e)
    {
        mView = v;
        mEvent = e;
    }

    public void reset()
    {
        if (mEvent == null)
        {
            return;
        }
        mBasePos = VectorF.fromEvent(mEvent);
        mTrackDistance = 0;
    }

    public double track(MotionEvent e)
    {
        VectorF pos = VectorF.fromEvent(e);
        mTrackDistance += pos.sub(mBasePos).fastLength();
        mBasePos = pos;
        return mTrackDistance;
    }

    public View view()
    {
        return mView;
    }

    public MotionEvent event()
    {
        return mEvent;
    }

    public VectorF basePosition()
    {
        return mBasePos;
    }
    
    public double trackDistance()
    {
        return mTrackDistance;
    }

    public double distance()
    {
        return VectorF.fromEvent(event()).sub(mBasePos).length();
    }

    public VectorF vector()
    {
        return VectorF.fromEvent(event()).sub(basePosition());
    }
}

