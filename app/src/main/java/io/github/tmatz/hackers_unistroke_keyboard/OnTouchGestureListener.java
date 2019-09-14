package io.github.tmatz.hackers_unistroke_keyboard;

import android.content.Context;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;

abstract class OnTouchGestureListener
extends GestureDetector.SimpleOnGestureListener
implements OnTouchListener
{
    private final GestureDetector mGestureDetector;

    public OnTouchGestureListener(Context context)
    {
        mGestureDetector = new GestureDetector(context, this);
    }

    public GestureDetector getGetGestureDetector()
    {
        return mGestureDetector;
    }

    @Override
    public boolean onTouch(View view, MotionEvent e)
    {
        return mGestureDetector.onTouchEvent(e);
    }
}

