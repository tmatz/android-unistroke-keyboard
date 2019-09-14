package io.github.tmatz.hackers_unistroke_keyboard;

import android.content.Context;
import android.gesture.GestureLibraries;
import android.gesture.GestureLibrary;
import android.gesture.Gesture;
import java.util.ArrayList;
import android.gesture.Prediction;
import android.content.res.Resources;

class GestureStore
{
    public static final int FLAG_GESTURE_ALPHABET = 1;
    public static final int FLAG_GESTURE_NUMBER = 2;
    public static final int FLAG_GESTURE_SPECIAL = 4;
    public static final int FLAG_GESTURE_CONTROL = 8;
    public static final int FLAG_STRICT = 16;

    private static final PredictionResult sPredictionFailed = new PredictionResult();

    private final Resources mResources;
    private final GestureLibrary mAlpabet;
    private final GestureLibrary mNumber;
    private final GestureLibrary mSpecial;
    private final GestureLibrary mControl;

    public GestureStore(Context context)
    {
        mResources = context.getResources();
        mAlpabet = createGesture(context, R.raw.gestures_alphabet);
        mNumber = createGesture(context, R.raw.gestures_number);
        mSpecial = createGesture(context, R.raw.gestures_special);
        mControl = createGesture(context, R.raw.gestures_control);
    }

    private GestureLibrary createGesture(Context context, int rawId)
    {
        GestureLibrary store = GestureLibraries.fromRawResource(context, rawId);
        store.setOrientationStyle(8);
        store.load();
        return store;
    }

    public PredictionResult predict(Gesture gesture, int flags)
    {
        PredictionResult prediction = sPredictionFailed;

        if ((flags & FLAG_GESTURE_CONTROL) != 0)
        {
            prediction = prediction.choose(getPrediction(gesture, mControl, 0.7, flags));
        }

        if ((flags & FLAG_GESTURE_SPECIAL) != 0)
        {
            prediction = prediction.choose(getPrediction(gesture, mSpecial, 1.0, flags));
        }

        if ((flags & FLAG_GESTURE_ALPHABET) != 0)
        {
            prediction = prediction.choose(getPrediction(gesture, mAlpabet, 1.0, flags));
        }

        if ((flags & FLAG_GESTURE_NUMBER) != 0)
        {
            prediction = prediction.choose(getPrediction(gesture, mNumber, 1.0, flags));
        }

        return prediction;
    }

    private PredictionResult getPrediction(Gesture gesture, GestureLibrary store, double scale, int flags)
    {
        ArrayList<Prediction> predictions = store.recognize(gesture);
        if (predictions.size() == 0)
        {
            return sPredictionFailed;
        }

        if (gesture.getLength() < mResources.getDimension(R.dimen.period_tolerance))
        {
            return new PredictionResult("period", Double.POSITIVE_INFINITY);
        }

        PredictionResult prediction = new PredictionResult(predictions.get(0), scale);

        if ((flags & FLAG_STRICT) == 0)
        {
            return prediction;
        }

        if (prediction.score < 1.5)
        {
            return sPredictionFailed;
        }

        if (predictions.size() == 1)
        {
            return prediction;
        }

        PredictionResult next = new PredictionResult(predictions.get(1), scale);
        if (prediction.score < next.score + 0.2)
        {
            return sPredictionFailed;
        }

        return prediction;
    }
}

