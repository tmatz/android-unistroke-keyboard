//package android_unistroke_keyboard;

import java.io.FileInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;

public class Hello {
  static class Store {
    public ArrayList<Entry> mEntries;

    public Store(ArrayList<Entry> entries) {
      mEntries = entries;
    }

    public void print() {
      System.out.print("numberEntries:");
      System.out.println(mEntries.size());
      System.out.flush();
      for (int i = 0; i < mEntries.size(); i++) {
        final Entry entry = mEntries.get(i);
        System.out.print("i:");
        System.out.println(i);
        System.out.print("name:");
        System.out.println(entry.mName);
        System.out.print("numberGestures:");
        System.out.println(entry.mGestures.size());
        System.out.flush();
        for (int j = 0; j < entry.mGestures.size(); j++) {
          final Gesture gesture = entry.mGestures.get(j);
          System.out.print("j:");
          System.out.println(j);
          System.out.print("gestureId:");
          System.out.println(gesture.mGestureId);
          System.out.print("numberStrokes:");
          System.out.println(gesture.mStrokes.size());
          System.out.flush();
          for (int k = 0; k < gesture.mStrokes.size(); k++) {
            final Stroke stroke = gesture.mStrokes.get(k);
            System.out.print("k:");
            System.out.println(k);
            System.out.print("numberPoints:");
            System.out.println(stroke.mPoints.size());
            System.out.flush();
            long timestamp0 = 0;
            for (int l = 0; l < stroke.mPoints.size(); l++) {
              final Point point = stroke.mPoints.get(l);
              if (l == 0) {
                timestamp0 = point.mTimestamp;
              }
              System.out.print("l:");
              System.out.println(l);
              System.out.print("x:");
              System.out.println(point.mX);
              System.out.print("y:");
              System.out.println(point.mY);
              System.out.print("timestamp:");
              System.out.println(point.mTimestamp - timestamp0);
              System.out.flush();
            }
          }
        }
      }
    }

    public void toSvg() {
      System.out.print(
          "<svg xmlns=\"http://www.w3.org/2000/svg\" fill=\"none\" viewBox=\"0 0 1000 1000\"><path stroke=\"#000\" d=\"");

      boolean first = true;
      for (int i = 0; i < mEntries.size(); i++) {
        final Entry entry = mEntries.get(i);
        for (int j = 0; j < entry.mGestures.size(); j++) {
          final Gesture gesture = entry.mGestures.get(j);
          for (int k = 0; k < gesture.mStrokes.size(); k++) {
            final Stroke stroke = gesture.mStrokes.get(k);
            for (int l = 0; l < stroke.mPoints.size(); l++) {
              final Point point = stroke.mPoints.get(l);
              if (first) {
                first = false;
                System.out.print("M ");
              } else {
                System.out.print(" L ");
              }
              System.out.printf("%f %f", point.mX, point.mY);
            }
          }
        }
        break;
      }
      System.out.println("\"/></svg>");
    }
  }

  static class Entry {
    public String mName;
    public ArrayList<Gesture> mGestures;

    public Entry(String name, ArrayList<Gesture> gestures) {
      mName = name;
      mGestures = gestures;
    }
  }

  static class Gesture {
    public long mGestureId;
    public ArrayList<Stroke> mStrokes;

    public Gesture(long gestureId, ArrayList<Stroke> strokes) {
      mGestureId = gestureId;
      mStrokes = strokes;
    }
  }

  static class Stroke {
    public ArrayList<Point> mPoints;

    public Stroke(ArrayList<Point> points) {
      this.mPoints = points;
    }
  }

  static class Point {
    public float mX;
    public float mY;
    public long mTimestamp;

    public Point(float x, float y, long timestamp) {
      this.mX = x;
      this.mY = y;
      this.mTimestamp = timestamp;
    }
  }

  public static void main(String[] args) throws Exception {
    try (DataInputStream stream = new DataInputStream(new FileInputStream(args[0]))) {
      final short versionNumber = stream.readShort();
      final int numberEntries = stream.readInt();
      final ArrayList<Entry> entries = new ArrayList<Entry>(numberEntries);
      for (int i = 0; i < numberEntries; i++) {
        final String name = stream.readUTF();
        final int numberGestures = stream.readInt();
        final ArrayList<Gesture> gestures = new ArrayList<Gesture>(numberGestures);
        for (int j = 0; j < numberGestures; j++) {
          final long gestureId = stream.readLong();
          final int numberStrokes = stream.readInt();
          final ArrayList<Stroke> strokes = new ArrayList<Stroke>(numberStrokes);
          for (int k = 0; k < numberStrokes; k++) {
            final int numberPoints = stream.readInt();
            final ArrayList<Point> points = new ArrayList<Point>(numberPoints);
            long timestamp0 = 0;
            for (int l = 0; l < numberPoints; l++) {
              final float x = stream.readFloat();
              final float y = stream.readFloat();
              final long timestamp = stream.readLong();
              if (l == 0) {
                timestamp0 = timestamp;
              }
              points.add(new Point(x, y, timestamp));
            }
            strokes.add(new Stroke(points));
          }
          gestures.add(new Gesture(gestureId, strokes));
        }
        entries.add(new Entry(name, gestures));
      }
      final Store store = new Store(entries);
      store.toSvg();
    }
  }
}
