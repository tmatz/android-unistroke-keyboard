package io.github.tmatz.hackers_unistroke_keyboard;

import android.content.Context;
import android.gesture.Gesture;
import android.gesture.GestureLibraries;
import android.gesture.GestureLibrary;
import android.gesture.Prediction;
import java.util.ArrayList;

class GestureStore
{
    public static final int FLAG_CATEGORY_ALPHABET = 1;
    public static final int FLAG_CATEGORY_NUMBER = 2;
    public static final int FLAG_CATEGORY_SPECIAL = 4;
    public static final int FLAG_CATEGORY_CONTROL = 8;
    public static final int FLAG_STRICT = 16;

    private final ApplicationResources resources;
    private final ArrayList<WeightedGestureLibrary> libraries = new ArrayList<>();

    public GestureStore(Context context, ApplicationResources resources)
    {
        this.resources = resources;
        libraries.add(loadGestureLibrary(context, R.raw.gestures_alphabet, 1.0, FLAG_CATEGORY_ALPHABET));
        libraries.add(loadGestureLibrary(context, R.raw.gestures_number, 1.0, FLAG_CATEGORY_NUMBER));
        libraries.add(loadGestureLibrary(context, R.raw.gestures_special, 1.0, FLAG_CATEGORY_SPECIAL));
        libraries.add(loadGestureLibrary(context, R.raw.gestures_control, 0.7, FLAG_CATEGORY_CONTROL));
    }

    private WeightedGestureLibrary loadGestureLibrary(Context context, int rawId, double weight, int category)
    {
        GestureLibrary library = GestureLibraries.fromRawResource(context, rawId);
        library.setOrientationStyle(8);
        library.load();
        return new WeightedGestureLibrary(library, weight, category);
    }

    public PredictionResult recognize(Gesture gesture, int flags)
    {
        PredictionResult prediction = PredictionResult.Zero;

        for (WeightedGestureLibrary library: libraries)
        {
            if ((flags & library.category) != 0)
            {
                prediction = prediction.choose(library.recognize(gesture, flags));
            }
        }

        return prediction;
    }

    private class WeightedGestureLibrary
    {
        private final GestureLibrary library;
        private final double weight;
        private final int category;

        public WeightedGestureLibrary(GestureLibrary library, double weight, int category)
        {
            this.library = library;
            this.weight = weight;
            this.category = category;
        }

        public PredictionResult recognize(Gesture gesture, int flags)
        {
            if (gesture.getLength() < resources.getPeriodTolerance())
            {
                return new PredictionResult("period", Double.POSITIVE_INFINITY);
            }

            ArrayList<Prediction> predictions = library.recognize(gesture);
            if (predictions.size() == 0)
            {
                return PredictionResult.Zero;
            }

            PredictionResult first = new PredictionResult(predictions.get(0), weight);

            if ((flags & FLAG_STRICT) == 0)
            {
                return first;
            }

            if (first.score < 1.5)
            {
                return PredictionResult.Zero;
            }

            if (predictions.size() == 1)
            {
                return first;
            }

            PredictionResult next = new PredictionResult(predictions.get(1), weight);

            if (first.score < next.score + 0.2)
            {
                return PredictionResult.Zero;
            }

            return first;
        }
    }
}

