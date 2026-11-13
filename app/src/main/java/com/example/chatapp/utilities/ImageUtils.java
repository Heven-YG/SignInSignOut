package com.example.chatapp.utilities;

import android.content.ContentResolver;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ImageDecoder;
import android.net.Uri;
import android.os.Build;
import android.util.Base64;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Converts picked images into small Base64 JPEG strings for the Firestore profile, and back
 * into bitmaps for display.
 */
public final class ImageUtils {

    private static final int START_QUALITY = 80;
    private static final int MIN_QUALITY = 40;
    private static final int QUALITY_STEP = 10;
    private static final float DOWNSCALE_FACTOR = 0.75f;
    private static final int MIN_DIMENSION = 64;

    /** Prevents instantiation of this utility class. */
    private ImageUtils() {
    }

    /**
     * Decodes, resizes, and compresses the image at {@code uri} into a Base64 JPEG string that
     * fits within {@link Constants#PROFILE_IMAGE_MAX_BASE64_LENGTH}. On Android 9 and later the
     * platform decoder also applies the photo's EXIF rotation.
     *
     * @param resolver content resolver used to open the image
     * @param uri      content URI returned by the photo picker
     * @return the encoded image, without line breaks
     * @throws IOException if the image cannot be read, decoded, or made small enough
     */
    @WorkerThread
    @NonNull
    public static String encodeProfileImage(@NonNull ContentResolver resolver, @NonNull Uri uri)
            throws IOException {
        Bitmap decoded = decodeSampled(resolver, uri, Constants.PROFILE_IMAGE_MAX_DIMENSION);
        int maxDimension = Constants.PROFILE_IMAGE_MAX_DIMENSION;
        try {
            while (maxDimension >= MIN_DIMENSION) {
                Bitmap scaled = scaleToFit(decoded, maxDimension);
                try {
                    for (int quality = START_QUALITY; quality >= MIN_QUALITY;
                         quality -= QUALITY_STEP) {
                        String encoded = compressToBase64(scaled, quality);
                        if (encoded.length() <= Constants.PROFILE_IMAGE_MAX_BASE64_LENGTH) {
                            return encoded;
                        }
                    }
                } finally {
                    if (scaled != decoded) {
                        scaled.recycle();
                    }
                }
                maxDimension = Math.round(maxDimension * DOWNSCALE_FACTOR);
            }
        } finally {
            decoded.recycle();
        }
        throw new IOException("Image could not be compressed small enough");
    }

    /**
     * Decodes a Base64 avatar string into a bitmap.
     *
     * @param encodedImage Base64 JPEG string, possibly empty
     * @return the decoded bitmap, or {@code null} if the value is empty or invalid
     */
    @Nullable
    public static Bitmap decodeBase64(@Nullable String encodedImage) {
        if (encodedImage == null || encodedImage.isEmpty()) {
            return null;
        }
        try {
            byte[] bytes = Base64.decode(encodedImage, Base64.DEFAULT);
            return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /**
     * Decodes the image at a reduced resolution close to {@code maxDimension}. Uses
     * {@link ImageDecoder} on Android 9+, and two-pass {@link BitmapFactory} sampling on older
     * versions.
     *
     * @param resolver     content resolver used to open the image
     * @param uri          image location
     * @param maxDimension desired maximum width or height in pixels
     * @return a mutable-or-immutable software bitmap
     * @throws IOException if the stream cannot be opened or the data is not an image
     */
    @NonNull
    private static Bitmap decodeSampled(@NonNull ContentResolver resolver, @NonNull Uri uri,
                                        int maxDimension) throws IOException {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            ImageDecoder.Source source = ImageDecoder.createSource(resolver, uri);
            return ImageDecoder.decodeBitmap(source, (decoder, info, src) -> {
                int[] size = ImageSizeCalculator.calculateScaledSize(
                        info.getSize().getWidth(), info.getSize().getHeight(), maxDimension);
                decoder.setTargetSize(size[0], size[1]);
                // Software bitmaps can be compressed; hardware bitmaps cannot.
                decoder.setAllocator(ImageDecoder.ALLOCATOR_SOFTWARE);
            });
        }

        BitmapFactory.Options bounds = new BitmapFactory.Options();
        bounds.inJustDecodeBounds = true;
        try (InputStream input = openStream(resolver, uri)) {
            BitmapFactory.decodeStream(input, null, bounds);
        }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
            throw new IOException("Selected file is not a supported image");
        }

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = ImageSizeCalculator.calculateInSampleSize(
                bounds.outWidth, bounds.outHeight, maxDimension);
        Bitmap bitmap;
        try (InputStream input = openStream(resolver, uri)) {
            bitmap = BitmapFactory.decodeStream(input, null, options);
        }
        if (bitmap == null) {
            throw new IOException("Selected image could not be decoded");
        }
        return bitmap;
    }

    /**
     * Opens an input stream for a content URI, converting a {@code null} result into an
     * exception.
     *
     * @param resolver content resolver
     * @param uri      image location
     * @return an open stream that the caller must close
     * @throws IOException if the stream cannot be opened
     */
    @NonNull
    private static InputStream openStream(@NonNull ContentResolver resolver, @NonNull Uri uri)
            throws IOException {
        InputStream input = resolver.openInputStream(uri);
        if (input == null) {
            throw new IOException("Unable to open selected image");
        }
        return input;
    }

    /**
     * Returns the bitmap scaled so its longer side is at most {@code maxDimension}.
     *
     * @param bitmap       source bitmap
     * @param maxDimension maximum width or height in pixels
     * @return the original bitmap if it already fits, otherwise a new scaled bitmap
     */
    @NonNull
    private static Bitmap scaleToFit(@NonNull Bitmap bitmap, int maxDimension) {
        int[] size = ImageSizeCalculator.calculateScaledSize(
                bitmap.getWidth(), bitmap.getHeight(), maxDimension);
        if (size[0] == bitmap.getWidth() && size[1] == bitmap.getHeight()) {
            return bitmap;
        }
        return Bitmap.createScaledBitmap(bitmap, size[0], size[1], true);
    }

    /**
     * Compresses a bitmap to JPEG and encodes the bytes as Base64 without line breaks.
     *
     * @param bitmap  bitmap to compress
     * @param quality JPEG quality from 0 to 100
     * @return the Base64 string
     */
    @NonNull
    private static String compressToBase64(@NonNull Bitmap bitmap, int quality) {
        Bitmap opaque = bitmap.hasAlpha() ? flattenOnWhite(bitmap) : bitmap;
        try {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            opaque.compress(Bitmap.CompressFormat.JPEG, quality, output);
            return Base64.encodeToString(output.toByteArray(), Base64.NO_WRAP);
        } finally {
            if (opaque != bitmap) {
                opaque.recycle();
            }
        }
    }

    /**
     * Draws a bitmap that may contain transparency onto a white background, because JPEG
     * has no alpha channel and transparent pixels would otherwise turn black.
     *
     * @param bitmap source bitmap with an alpha channel
     * @return a new opaque bitmap of the same size
     */
    @NonNull
    private static Bitmap flattenOnWhite(@NonNull Bitmap bitmap) {
        Bitmap result = Bitmap.createBitmap(
                bitmap.getWidth(), bitmap.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(result);
        canvas.drawColor(Color.WHITE);
        canvas.drawBitmap(bitmap, 0f, 0f, null);
        return result;
    }
}
