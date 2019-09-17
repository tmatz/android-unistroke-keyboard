package io.github.tmatz.hackers_unistroke_keyboard;

import android.view.MotionEvent;
import android.view.View;

class MotionTrack
{
    private VectorF mBasePosition = VectorF.Zero;
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

    public void set(MotionEvent e)
    {
        mEvent = e;
    }

    public double trackPosition()
    {
        VectorF pos = position();
        mTrackDistance += pos.sub(basePosition()).fastLength();
        mBasePosition = pos;
        return mTrackDistance;
    }

    public void setBasePosition(VectorF basePosition)
    {
        mBasePosition = basePosition;
        mTrackDistance = 0;
    }
    
    public View view()
    {
        return mView;
    }

    public MotionEvent event()
    {
        return mEvent;
    }

    public VectorF position()
    {
        return VectorF.fromEvent(event());
    }
    
    public VectorF basePosition()
    {
        return mBasePosition;
    }
    
    public double trackDistance()
    {
        return mTrackDistance;
    }

    public VectorF difference()
    {
        return position().sub(basePosition());
    }
}

