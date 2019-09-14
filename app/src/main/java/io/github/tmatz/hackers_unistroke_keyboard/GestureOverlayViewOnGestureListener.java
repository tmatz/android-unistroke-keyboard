package io.github.tmatz.hackers_unistroke_keyboard;

import android.gesture.GestureOverlayView;
import android.view.MotionEvent;

abstract class GestureOverlayViewOnGestureListener
implements GestureOverlayView.OnGestureListener
{
    @Override
    public void onGesture(GestureOverlayView overlay, MotionEvent e)
    {
    }

    @Override
    public void onGestureCancelled(GestureOverlayView overlay, MotionEvent e)
    {
    }

    @Override
    public void onGestureStarted(GestureOverlayView overlay, MotionEvent e)
    {
    }

    @Override
    public void onGestureEnded(GestureOverlayView overlay, MotionEvent e)
    {
    }
}

