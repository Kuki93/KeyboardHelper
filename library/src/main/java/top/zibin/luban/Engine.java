package top.zibin.luban;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Build;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import top.zibin.luban.turbo.TurboCompressor;

/**
 * Responsible for starting compress and managing active and cached resources.
 */
class Engine {
    private InputStreamProvider2 srcImg;
    private File tagImg;
    private int srcWidth;
    private int srcHeight;
    private boolean focusAlpha;

    private static final int quality = 60;

    Engine(InputStreamProvider2 srcImg, File tagImg, boolean focusAlpha) throws IOException {
        this.tagImg = tagImg;
        this.srcImg = srcImg;
        this.focusAlpha = focusAlpha;

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        options.inSampleSize = 1;

        InputStream inputStream = srcImg.open();
        BitmapFactory.decodeStream(inputStream, null, options);
        this.srcWidth = options.outWidth;
        this.srcHeight = options.outHeight;
        inputStream.close();
    }

    private int computeSize() {
        srcWidth = srcWidth % 2 == 1 ? srcWidth + 1 : srcWidth;
        srcHeight = srcHeight % 2 == 1 ? srcHeight + 1 : srcHeight;

        int longSide = Math.max(srcWidth, srcHeight);
        int shortSide = Math.min(srcWidth, srcHeight);

        float scale = ((float) shortSide / longSide);
        if (scale <= 1 && scale > 0.5625) {
            if (longSide < 1664) {
                return 1;
            } else if (longSide < 4990) {
                return 2;
            } else if (longSide > 4990 && longSide < 10240) {
                return 4;
            } else {
                return longSide / 1280 == 0 ? 1 : longSide / 1280;
            }
        } else if (scale <= 0.5625 && scale > 0.5) {
            return longSide / 1280 == 0 ? 1 : longSide / 1280;
        } else {
            return (int) Math.ceil(longSide / (1280.0 / scale));
        }
    }

    private Bitmap rotatingImage(Bitmap bitmap, int angle) {
        Matrix matrix = new Matrix();

        matrix.postRotate(angle);

        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
    }

    Uri compress() throws IOException {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = computeSize();

        Bitmap tagBitmap = srcImg.getBitmap();
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        FileOutputStream fos = new FileOutputStream(tagImg);

        InputStream inputStream = srcImg.open();
        if (Checker.SINGLE.isJPG(inputStream)) {
            tagBitmap = rotatingImage(tagBitmap, Checker.SINGLE.getOrientation(inputStream));
        }

        if (focusAlpha) {
            tagBitmap.compress(Bitmap.CompressFormat.PNG, quality, stream);
            fos.write(stream.toByteArray());
        } else if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            TurboCompressor.compress(tagBitmap, quality, tagImg.getAbsolutePath());
        } else {
            tagBitmap.compress(Bitmap.CompressFormat.JPEG, quality, stream);
            fos.write(stream.toByteArray());
        }

        tagBitmap.recycle();

        fos.flush();
        fos.close();
        stream.close();
        inputStream.close();
        return Uri.fromFile(tagImg);
    }
}