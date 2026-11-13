package com.example.chatapp.utilities;

/**
 * Pure size calculations used when shrinking profile images, kept free of Android types so
 * they can be unit tested.
 */
public final class ImageSizeCalculator {

    /** Prevents instantiation of this utility class. */
    private ImageSizeCalculator() {
    }

    /**
     * Finds the largest power-of-two sample size that keeps both decoded dimensions at or
     * above the requested maximum, so decoding uses little memory without losing quality
     * needed for the final resize.
     *
     * @param width        source width in pixels
     * @param height       source height in pixels
     * @param maxDimension target maximum width or height in pixels
     * @return a sample size of 1 or greater, suitable for {@code BitmapFactory.Options}
     */
    public static int calculateInSampleSize(int width, int height, int maxDimension) {
        if (width <= 0 || height <= 0 || maxDimension <= 0) {
            return 1;
        }
        int sampleSize = 1;
        while (width / (sampleSize * 2) >= maxDimension
                && height / (sampleSize * 2) >= maxDimension) {
            sampleSize *= 2;
        }
        return sampleSize;
    }

    /**
     * Scales dimensions down proportionally so the longer side equals {@code maxDimension}.
     * Images that already fit are left unchanged; neither side drops below one pixel.
     *
     * @param width        source width in pixels
     * @param height       source height in pixels
     * @param maxDimension maximum allowed width or height in pixels
     * @return a two-element array holding the new width and height
     */
    public static int[] calculateScaledSize(int width, int height, int maxDimension) {
        if (width <= 0 || height <= 0 || maxDimension <= 0) {
            return new int[]{Math.max(width, 1), Math.max(height, 1)};
        }
        int longest = Math.max(width, height);
        if (longest <= maxDimension) {
            return new int[]{width, height};
        }
        float scale = (float) maxDimension / longest;
        int scaledWidth = Math.max(1, Math.round(width * scale));
        int scaledHeight = Math.max(1, Math.round(height * scale));
        return new int[]{scaledWidth, scaledHeight};
    }
}
