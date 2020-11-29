package me.nathanp.bubbledrop;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public class Util {
    public static Bitmap getBitmapFromVectorDrawable(Context context, int drawableId, float scale) {
        Drawable drawable = ContextCompat.getDrawable(context, drawableId);
        Bitmap bitmap = Bitmap.createBitmap((int) (drawable.getIntrinsicWidth() * scale),
                (int) (drawable.getIntrinsicHeight() * scale), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);

        return bitmap;
    }

    public static Bitmap getBitmapFromVectorDrawable(Context context, int drawableId) {
        return getBitmapFromVectorDrawable(context, drawableId, 1);
    }

    public void logBytes(File file) {
        try {
            FileInputStream fis = new FileInputStream(file);
            StringBuilder out = new StringBuilder();
            int data = fis.read();
            while (data != -1) {
                out.append(data);
                data = fis.read();
            }
            Log.e("UTIL", out.toString());
        } catch (IOException e) {
            Log.e("UTIL", e.getMessage());
        }

    }
}
