package top.zibin.luban;

import android.content.ContentResolver;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.ParcelFileDescriptor;

import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

public class InputStreamAdapter2 implements InputStreamProvider2 {

    private Uri uri;
    private ContentResolver contentResolver;

    private Bitmap bitmap;

    InputStreamAdapter2(Uri uri, ContentResolver contentResolver) {
        this.uri = uri;
        this.contentResolver = contentResolver;
    }

    @Override
    public Uri getUri() {
        return uri;
    }

    @Override
    public InputStream open() throws IOException {
        return contentResolver.openInputStream(uri);
    }

    @Override
    public Bitmap getBitmap() throws IOException {
        if (bitmap == null) {
            ParcelFileDescriptor parcelFileDescriptor = contentResolver.openFileDescriptor(uri, "r");
            if (parcelFileDescriptor != null) {
                FileDescriptor fileDescriptor = parcelFileDescriptor.getFileDescriptor();
                bitmap = BitmapFactory.decodeFileDescriptor(fileDescriptor);
                parcelFileDescriptor.close();
            }
        }
        if (bitmap == null) {
            throw new FileNotFoundException("Resource does not exist: " + uri);
        }
        return bitmap;
    }

    @Override
    public String getMimeType() {
        return contentResolver.getType(uri);
    }
}