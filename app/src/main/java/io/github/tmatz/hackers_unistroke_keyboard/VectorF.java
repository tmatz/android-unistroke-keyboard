package io.github.tmatz.hackers_unistroke_keyboard;

import android.graphics.RectF;
import android.view.MotionEvent;

class VectorF
{
    public static final VectorF Zero = new VectorF();

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

    public double length()
    {
        return Math.sqrt(x * x + y * y);
    }

    public float fastLength()
    {
        return Math.abs(x) + Math.abs(y);
    }

    public VectorF cutoff(float threshold)
    {
        return new VectorF(
            Math.min(0, x + threshold) + Math.max(0, x - threshold),
            Math.min(0, y + threshold) + Math.max(0, y - threshold));
    }

    public VectorF cutoff(RectF rect)
    {
        return new VectorF(
            Math.min(0, x - rect.left) + Math.max(0, x - rect.right),
            Math.min(0, y - rect.top) + Math.max(0, y - rect.bottom));
    }

    public static VectorF fromEvent(MotionEvent e)
    {
        return new VectorF(e.getRawX(), e.getRawY());
    }

    public boolean isContained(RectF rect)
    {
        return rect.contains(x, y);
    }

    @Override
    public String toString()
    {
        return String.format("(%f,%f)", x, y);
    }
}

