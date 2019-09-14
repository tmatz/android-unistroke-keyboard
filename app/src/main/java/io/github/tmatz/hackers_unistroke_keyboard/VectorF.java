package io.github.tmatz.hackers_unistroke_keyboard;

import android.view.MotionEvent;

class VectorF
{
    public float x;
    public float y;

    public VectorF()
    {
        x = 0;
        y = 0;
    }

    public VectorF(float x, float y)
    {
        this.x = x;
        this.y = y;
    }

    public VectorF add(VectorF v)
    {
        return new VectorF(x + v.x, y + v.y);
    }

    public VectorF mult(float f)
    {
        return new VectorF(x * f, y * f);
    }

    public VectorF sub(VectorF v)
    {
        return add(v.mult(-1));
    }

    public float fastLength()
    {
        return Math.abs(x) + Math.abs(y);
    }

    public VectorF cutoff(float threshold)
    {
        return new VectorF(
            x >= 0 ? Math.max(0, x - threshold) : Math.min(0, x + threshold),
            y >= 0 ? Math.max(0, y - threshold) : Math.min(0, y + threshold));
    }

    public static VectorF fromEvent(MotionEvent e)
    {
        return new VectorF(e.getRawX(), e.getRawY());
    }
}

