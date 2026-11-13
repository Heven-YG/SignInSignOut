package com.example.chatapp.utilities;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Unit tests for {@link ImageSizeCalculator}.
 */
public class ImageSizeCalculatorTest {

    /** Small images are decoded at full resolution. */
    @Test
    public void inSampleSize_isOneForSmallImages() {
        assertEquals(1, ImageSizeCalculator.calculateInSampleSize(200, 150, 256));
    }

    /** Large images are sampled down without dropping below the target size. */
    @Test
    public void inSampleSize_keepsBothSidesAtLeastTarget() {
        int sample = ImageSizeCalculator.calculateInSampleSize(4000, 3000, 256);
        assertEquals(8, sample);
        assertTrue(3000 / sample >= 256);
    }

    /** Invalid sizes fall back to no sampling. */
    @Test
    public void inSampleSize_handlesInvalidInput() {
        assertEquals(1, ImageSizeCalculator.calculateInSampleSize(0, 100, 256));
        assertEquals(1, ImageSizeCalculator.calculateInSampleSize(100, 100, 0));
    }

    /** Landscape and portrait images keep their aspect ratio. */
    @Test
    public void scaledSize_preservesAspectRatio() {
        assertArrayEquals(new int[]{256, 192},
                ImageSizeCalculator.calculateScaledSize(4000, 3000, 256));
        assertArrayEquals(new int[]{144, 256},
                ImageSizeCalculator.calculateScaledSize(1080, 1920, 256));
    }

    /** Images that already fit are not enlarged. */
    @Test
    public void scaledSize_doesNotUpscale() {
        assertArrayEquals(new int[]{100, 80},
                ImageSizeCalculator.calculateScaledSize(100, 80, 256));
    }

    /** Extreme aspect ratios never produce a zero-pixel side. */
    @Test
    public void scaledSize_neverReturnsZero() {
        int[] size = ImageSizeCalculator.calculateScaledSize(10000, 1, 256);
        assertEquals(256, size[0]);
        assertEquals(1, size[1]);
    }
}
