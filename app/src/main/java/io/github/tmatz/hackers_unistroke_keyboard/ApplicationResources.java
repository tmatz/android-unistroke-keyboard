package io.github.tmatz.hackers_unistroke_keyboard;

import android.content.Context;
import android.content.res.Resources;

class ApplicationResources
{
    public final Resources resources;
    public final GestureStore gestures;

    public ApplicationResources(Context context)
    {
        resources = context.getApplicationContext().getResources();
        gestures = new GestureStore(context, this);
    }

    public float getCursorTolerance()
    {
        return resources.getDimension(R.dimen.cursor_tolerance);
    }

    public float getPeriodTolerance()
    {
        return resources.getDimension(R.dimen.period_tolerance);
    }
}

