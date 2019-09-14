package io.github.tmatz.hackers_unistroke_keyboard;

import android.gesture.Prediction;

class PredictionResult
{
    public final double score;
    public final String name;

    public PredictionResult()
    {
        this.score = 0;
        this.name = null;
    }

    public PredictionResult(String name, double score)
    {
        this.name = name;
        this.score = score;
    }

    public PredictionResult(Prediction prediction, double scale)
    {
        this.name = prediction.name;
        this.score = prediction.score * scale;
    }

    public PredictionResult choose(PredictionResult other)
    {
        return (score > other.score) ? this : other;
    }
}

