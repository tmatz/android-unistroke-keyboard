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

    public PredictionResult(Prediction prediction)
    {
        this.name = prediction.name;
        this.score = prediction.score;
    }

    public PredictionResult mult(double f)
    {
        return new PredictionResult(name, score * f);
    }

    public PredictionResult choose(PredictionResult other)
    {
        return (score > other.score) ? this : other;
    }
}

