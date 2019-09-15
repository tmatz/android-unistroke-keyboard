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
        Loader loader = new Loader(context);
        libraries.add(loader.load(1.0, R.raw.gestures_alphabet, FLAG_CATEGORY_ALPHABET));
        libraries.add(loader.load(1.0, R.raw.gestures_number, FLAG_CATEGORY_NUMBER));
        libraries.add(loader.load(1.0, R.raw.gestures_special, FLAG_CATEGORY_SPECIAL));
        libraries.add(loader.load(0.7, R.raw.gestures_control, FLAG_CATEGORY_CONTROL));
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

    private class Loader
    {
        final Context context;

        Loader(Context context)
        {
            this.context = context;
        }

        WeightedGestureLibrary load(double weight, int rawId, int category)
        {
            GestureLibrary library = GestureLibraries.fromRawResource(context, rawId);
            library.setOrientationStyle(8);
            library.load();
            return new WeightedGestureLibrary(library, category, weight);
        }
    }

    private class WeightedGestureLibrary
    {
        final GestureLibrary library;
        final int category;
        final double weight;

        WeightedGestureLibrary(GestureLibrary library, int category, double weight)
        {
            this.library = library;
            this.category = category;
            this.weight = weight;
        }

        PredictionResult recognize(Gesture gesture, int flags)
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

