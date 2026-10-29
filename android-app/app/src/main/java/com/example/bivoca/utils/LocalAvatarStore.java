package com.example.bivoca.utils;

import android.content.ContentResolver;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;

import androidx.annotation.Nullable;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

/** App-private avatar storage. Files are isolated by the authenticated user id. */
public final class LocalAvatarStore {
    private static final int MAX_SIDE = 1024;
    private final File root;

    public LocalAvatarStore(Context context) {
        root = new File(context.getFilesDir(), "avatars");
    }

    public File avatarFile(int userId) {
        return avatarFile(root, userId);
    }

    public static File avatarFile(File root, int userId) {
        if (userId <= 0) throw new IllegalArgumentException("A valid user id is required");
        return new File(root, "user_" + userId + ".jpg");
    }

    @Nullable public Bitmap load(int userId) {
        File file = avatarFile(userId);
        return file.isFile() ? BitmapFactory.decodeFile(file.getAbsolutePath()) : null;
    }

    @Nullable public Bitmap decodePreview(ContentResolver resolver, Uri uri) throws IOException {
        BitmapFactory.Options bounds = new BitmapFactory.Options();
        bounds.inJustDecodeBounds = true;
        try (InputStream stream = resolver.openInputStream(uri)) {
            if (stream == null) throw new IOException("Không thể mở ảnh đã chọn");
            BitmapFactory.decodeStream(stream, null, bounds);
        }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) throw new IOException("Định dạng ảnh không hợp lệ");
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = sampleSize(bounds.outWidth, bounds.outHeight, MAX_SIDE);
        try (InputStream stream = resolver.openInputStream(uri)) {
            if (stream == null) throw new IOException("Không thể mở ảnh đã chọn");
            Bitmap bitmap = BitmapFactory.decodeStream(stream, null, options);
            if (bitmap == null) throw new IOException("Không thể giải mã ảnh");
            return bitmap;
        }
    }

    public void save(int userId, Bitmap bitmap) throws IOException {
        if (bitmap == null) throw new IOException("Ảnh trống");
        if (!root.exists() && !root.mkdirs()) throw new IOException("Không thể tạo vùng lưu avatar");
        File target = avatarFile(userId);
        File temp = new File(root, target.getName() + ".tmp");
        try (FileOutputStream output = new FileOutputStream(temp)) {
            if (!bitmap.compress(Bitmap.CompressFormat.JPEG, 88, output)) throw new IOException("Không thể lưu ảnh");
        }
        if (target.exists() && !target.delete()) throw new IOException("Không thể thay avatar cũ");
        if (!temp.renameTo(target)) throw new IOException("Không thể hoàn tất lưu avatar");
    }

    public boolean reset(int userId) {
        File file = avatarFile(userId);
        return !file.exists() || file.delete();
    }

    static int sampleSize(int width, int height, int maxSide) {
        int sample = 1;
        while (width / sample > maxSide * 2 || height / sample > maxSide * 2) sample *= 2;
        return sample;
    }
}
