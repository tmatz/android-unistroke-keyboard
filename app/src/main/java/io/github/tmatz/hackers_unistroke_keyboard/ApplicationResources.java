package io.github.tmatz.hackers_unistroke_keyboard;

import android.content.Context;
import android.content.res.Resources;

class ApplicationResources
{
    public final Resources Resources;
    public final GestureStore GestureStore;

    public ApplicationResources(Context context)
    {
        Resources = context.getApplicationContext().getResources();
        GestureStore = new GestureStore(context, this);
    }

    public float getCursorTolerance()
    {
        return Resources.getDimension(R.dimen.cursor_tolerance);
    }

    public float getPeriodTolerance()
    {
        return Resources.getDimension(R.dimen.period_tolerance);
    }
}

