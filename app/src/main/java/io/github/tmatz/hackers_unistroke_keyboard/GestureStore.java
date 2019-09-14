package io.github.tmatz.hackers_unistroke_keyboard;

import android.content.Context;
import android.gesture.GestureLibraries;
import android.gesture.GestureLibrary;

class GestureStore
{
    public final GestureLibrary alpabet;
    public final GestureLibrary number;
    public final GestureLibrary special;
    public final GestureLibrary control;

    public GestureStore(Context context)
    {
        alpabet = createGesture(context, R.raw.gestures_alphabet);
        number = createGesture(context, R.raw.gestures_number);
        special = createGesture(context, R.raw.gestures_special);
        control = createGesture(context, R.raw.gestures_control);
    }

    private GestureLibrary createGesture(Context context, int rawId)
    {
        GestureLibrary store = GestureLibraries.fromRawResource(context, rawId);
        store.setOrientationStyle(8);
        store.load();
        return store;
    }
}

