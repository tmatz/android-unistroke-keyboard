package io.github.tmatz.hackers_unistroke_keyboard;

import android.graphics.RectF;
import android.view.View;

public class ViewUtils
{
    public static RectF getViewRect(View view)
    {
        int[] location = new int[2];
        view.getLocationOnScreen(location);

        int x = location[0];
        int y = location[1];
        int w = view.getWidth();
        int h = view.getHeight();

        return new RectF(x, y, x + w, y + h);
    }
}
