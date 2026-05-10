package com.testplus.app.utils;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import java.io.OutputStream;

/** Optik form önizlemesini PNG olarak sistem galerisine yazar. */
public final class GalleryBitmapSaver {

    private GalleryBitmapSaver() {}

    /**
     * @param baseFileName uzantısız güvenli dosya adı (ör. form_adı)
     * @return başarılı ise true
     */
    public static boolean savePngToGallery(ContentResolver resolver, Bitmap bitmap, String baseFileName) {
        if (bitmap == null || resolver == null) return false;
        String safe = sanitizeFileName(baseFileName);
        if (safe.isEmpty()) safe = "optik_form";
        String displayName = safe.endsWith(".png") ? safe : safe + ".png";

        ContentValues values = new ContentValues();
        values.put(MediaStore.MediaColumns.DISPLAY_NAME, displayName);
        values.put(MediaStore.MediaColumns.MIME_TYPE, "image/png");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            values.put(MediaStore.MediaColumns.RELATIVE_PATH,
                Environment.DIRECTORY_PICTURES + "/TestPlus");
            values.put(MediaStore.MediaColumns.IS_PENDING, 1);
        }

        Uri uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
        if (uri == null) return false;

        try (OutputStream os = resolver.openOutputStream(uri)) {
            if (os == null) {
                resolver.delete(uri, null, null);
                return false;
            }
            if (!bitmap.compress(Bitmap.CompressFormat.PNG, 100, os)) {
                resolver.delete(uri, null, null);
                return false;
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                values.clear();
                values.put(MediaStore.MediaColumns.IS_PENDING, 0);
                resolver.update(uri, values, null, null);
            }
            return true;
        } catch (Exception e) {
            try {
                resolver.delete(uri, null, null);
            } catch (Exception ignored) {}
            return false;
        }
    }

    private static String sanitizeFileName(String name) {
        if (name == null) return "";
        String s = name.trim().replaceAll("[\\\\/:*?\"<>|]", "_");
        return s.length() > 120 ? s.substring(0, 120) : s;
    }
}
