package io.github.tmatz.hackers_unistroke_keyboard;

import android.content.Context;
import android.content.res.Resources;

class Resources
{
    private final Resources mResources;

    Resources(Context context)
    {
        mResources = context.getResources();
    }

    public float getCursorTolerance()
    {
        return mResources.getDimension(R.dimen.cursor_tolerance);
    }
}

