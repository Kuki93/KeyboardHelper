package top.zibin.luban;

import android.graphics.Bitmap;
import android.net.Uri;

import java.io.IOException;
import java.io.InputStream;

public interface InputStreamProvider2 {

    Uri getUri();

    InputStream open() throws IOException;

    Bitmap getBitmap() throws IOException;

    String getMimeType();
}
