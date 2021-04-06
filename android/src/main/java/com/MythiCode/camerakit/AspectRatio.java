package com.MythiCode.camerakit;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.collection.SparseArrayCompat;

/**
 * Immutable class for describing proportional relationship between width and height.
 */
public class AspectRatio implements Comparable<AspectRatio>, Parcelable {

    private final static SparseArrayCompat<SparseArrayCompat<AspectRatio>> sCache
            = new SparseArrayCompat<>(16);

    private final int mX;
    private final int mY;

    /**
     * Returns an instance of {@link AspectRatio} specified by {@code x} and {@code y} values.
     * The values {@code x} and {@code} will be reduced by their greatest common divider.
     *
     * @param x The width
     * @param y The height
     * @return An instance of {@link AspectRatio}
     */
    public static AspectRatio of(int x, int y) {
        int gcd = gcd(x, y);
        x /= gcd;
        y /= gcd;
        SparseArrayCompat<AspectRatio> arrayX = sCache.get(x);
        AspectRatio ratio;
        if (arrayX == null) {
            ratio = new AspectRatio(x, y);
            arrayX = new SparseArrayCompat<>();
            arrayX.put(y, ratio);
            sCache.put(x, arrayX);
        } else {
            ratio = arrayX.get(y);
            if (ratio == null) {
                ratio = new AspectRatio(x, y);
                arrayX.put(y, ratio);
            }
        }
        return ratio;
    }

    private AspectRatio(int x, int y) {
        mX = x;
        mY = y;
    }


    @Override
    public boolean equals(Object o) {
        if (o == null) {
            return false;
        }
        if (this == o) {
            return true;
        }
        if (o instanceof AspectRatio) {
            AspectRatio ratio = (AspectRatio) o;
            return mX == ratio.mX && mY == ratio.mY;
        }
        return false;
    }

    @Override
    public String toString() {
        return mX + ":" + mY;
    }

    public float toFloat() {
        return (float) mX / mY;
    }

    @Override
    public int hashCode() {
        // assuming most sizes are <2^16, doing a rotate will give us perfect hashing
        return mY ^ ((mX << (Integer.SIZE / 2)) | (mX >>> (Integer.SIZE / 2)));
    }

    @Override
    public int compareTo(AspectRatio another) {
        if (equals(another)) {
            return 0;
        } else if (toFloat() - another.toFloat() > 0) {
            return 1;
        }
        return -1;
    }

    private static int gcd(int a, int b) {
        while (b != 0) {
            int c = b;
            b = a % b;
            a = c;
        }
        return a;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(mX);
        dest.writeInt(mY);
    }

    public static final Creator<AspectRatio> CREATOR
            = new Creator<AspectRatio>() {

        @Override
        public AspectRatio createFromParcel(Parcel source) {
            int x = source.readInt();
            int y = source.readInt();
            return AspectRatio.of(x, y);
        }

        @Override
        public AspectRatio[] newArray(int size) {
            return new AspectRatio[size];
        }
    };

}
