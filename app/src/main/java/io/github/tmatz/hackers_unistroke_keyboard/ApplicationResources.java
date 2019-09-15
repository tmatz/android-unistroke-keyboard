package io.github.tmatz.hackers_unistroke_keyboard;

import android.content.Context;
import android.content.res.Resources;

class ApplicationResources
{
    public static final int KEYREPEAT_DELAY_FIRST_MS = 400;
    public static final int KEYREPEAT_DELAY_MS = 100;
    public static final int VIBRATION_MS = 15;
    public static final int VIBRATION_STRONG_MS = 30;

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

