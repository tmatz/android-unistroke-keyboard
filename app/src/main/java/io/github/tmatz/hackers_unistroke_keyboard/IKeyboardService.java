package io.github.tmatz.hackers_unistroke_keyboard;

interface IKeyboardService
{
    void updateView();

    void sendText(String text);

    void sendKey(int action, int keyCode, int metaState);

    void sendKeyRepeat(int keyCode, int metaState);

    boolean isEditorActionRequested();

    void performEditorAction();
    
    void showInputMethodPicker();
    
    boolean vibrate(boolean strong);
    
    void toast(String message);
}

