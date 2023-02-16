package io.github.tmatz.hackers_unistroke_keyboard;

import android.view.KeyEvent;

class KeyEventUtils
{
    public static int keyCodeFromTag(String tag)
    {
        return KeyEvent.keyCodeFromString("KEYCODE_" + tag.toUpperCase());
    }

    private KeyEventUtils()
    {}
}

