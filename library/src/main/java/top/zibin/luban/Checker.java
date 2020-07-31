package top.zibin.luban;

import android.content.Context;
import android.net.Uri;
import android.text.TextUtils;
import android.webkit.MimeTypeMap;

import androidx.documentfile.provider.DocumentFile;
import androidx.exifinterface.media.ExifInterface;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

enum Checker {
    SINGLE;

    private static final String TAG = "Luban";

    private static final String JPG = ".jpg";

    private final byte[] JPEG_SIGNATURE = new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};

    /**
     * Determine if it is JPG.
     *
     * @param is image file input stream
     */
    boolean isJPG(InputStream is) {
        return isJPG(toByteArray(is));
    }

    /**
     * Returns the degrees in clockwise. Values are 0, 90, 180, or 270.
     *
     * @param inputStream
     */
    int getOrientation(InputStream inputStream) {
        ExifInterface localExifInterface;
        int angle = 0;
        try {
            localExifInterface = new ExifInterface(inputStream);
            int orientation = localExifInterface.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
            switch (orientation) {
                case ExifInterface.ORIENTATION_ROTATE_90:
                    angle = 90;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    angle = 180;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_270:
                    angle = 270;
                    break;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return angle;
    }

    private boolean isJPG(byte[] data) {
        if (data == null || data.length < 3) {
            return false;
        }
        byte[] signatureB = new byte[]{data[0], data[1], data[2]};
        return Arrays.equals(JPEG_SIGNATURE, signatureB);
    }

    String extSuffix(String mimeType) {
        if (TextUtils.isEmpty(mimeType)) {
            return JPG;
        } else {
            return "." + MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType);
        }
    }

    String getFileRealNameFromUri(Context context, Uri fileUri) {
        if (context == null || fileUri == null) return null;
        DocumentFile documentFile = DocumentFile.fromSingleUri(context, fileUri);
        if (documentFile == null) return null;
        return documentFile.getName();
    }

    boolean needCompress(int leastCompressSize, long length) {
        if (leastCompressSize > 0) {
            return length > (leastCompressSize << 10);
        }
        return true;
    }

    private int pack(byte[] bytes, int offset, int length, boolean littleEndian) {
        int step = 1;
        if (littleEndian) {
            offset += length - 1;
            step = -1;
        }

        int value = 0;
        while (length-- > 0) {
            value = (value << 8) | (bytes[offset] & 0xFF);
            offset += step;
        }
        return value;
    }

    private byte[] toByteArray(InputStream is) {
        if (is == null) {
            return new byte[0];
        }

        ByteArrayOutputStream buffer = new ByteArrayOutputStream();

        byte[] data = new byte[3];

        try {
            if (is.read(data, 0, 3) != -1) {
                buffer.write(data, 0, 3);
            } else {
                return new byte[0];
            }
        } catch (Exception e) {
            return new byte[0];
        } finally {
            try {
                buffer.close();
            } catch (IOException ignored) {
            }
        }

        return buffer.toByteArray();
    }
}
