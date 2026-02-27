package com.example.maquirentapp.Utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface; // Importante: Usar esta librería
import android.net.Uri;
import android.os.Build;
import android.util.Base64;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class ImageUtils {

    public static byte[] comprimirImagen(Context context, Uri imageUri) {
        InputStream imageStream = null;
        InputStream exifStream = null;
        try {
            imageStream = context.getContentResolver().openInputStream(imageUri);
            Bitmap original = BitmapFactory.decodeStream(imageStream);

            if (original == null) return null;

            exifStream = context.getContentResolver().openInputStream(imageUri);
            if (exifStream != null) {
                original = rotarImagenSiEsNecesario(original, exifStream);
            }

            int maxWidth = 1280;
            if (original.getWidth() > maxWidth) {
                float aspectRatio = (float) original.getHeight() / original.getWidth();
                int newHeight = (int) (maxWidth * aspectRatio);
                original = Bitmap.createScaledBitmap(original, maxWidth, newHeight, true);
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            original.compress(Bitmap.CompressFormat.JPEG, 70, baos);

            return baos.toByteArray();

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } finally {
            try {
                if (imageStream != null) imageStream.close();
                if (exifStream != null) exifStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private static Bitmap rotarImagenSiEsNecesario(Bitmap img, InputStream input) {
        try {
            ExifInterface ei;
            if (Build.VERSION.SDK_INT > 23) {
                ei = new ExifInterface(input);
            } else {
                return img;
            }

            int orientation = ei.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);

            switch (orientation) {
                case ExifInterface.ORIENTATION_ROTATE_90:
                    return rotateImage(img, 90);
                case ExifInterface.ORIENTATION_ROTATE_180:
                    return rotateImage(img, 180);
                case ExifInterface.ORIENTATION_ROTATE_270:
                    return rotateImage(img, 270);
                default:
                    return img;
            }
        } catch (IOException e) {
            e.printStackTrace();
            return img;
        }
    }

    private static Bitmap rotateImage(Bitmap img, int degree) {
        Matrix matrix = new Matrix();
        matrix.postRotate(degree);
        Bitmap rotatedImg = Bitmap.createBitmap(img, 0, 0, img.getWidth(), img.getHeight(), matrix, true);

        if (rotatedImg != img) {
            img.recycle();
        }
        return rotatedImg;
    }
    public static String convertirDrawableABase64(Context context, int resourceId) {
        Bitmap bitmap = BitmapFactory.decodeResource(context.getResources(), resourceId);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
        byte[] byteArray = outputStream.toByteArray();
        return Base64.encodeToString(byteArray, Base64.NO_WRAP);
    }
}