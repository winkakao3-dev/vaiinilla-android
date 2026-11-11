package com.vaiinilla.app.core.image

import android.media.ExifInterface
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExifOrientationTest {
    @Test
    fun aPhoneShotOnItsSideTurnsAQuarter() {
        assertEquals(ExifTransform(90, false), exifTransform(ExifInterface.ORIENTATION_ROTATE_90))
        assertEquals(ExifTransform(270, false), exifTransform(ExifInterface.ORIENTATION_ROTATE_270))
        assertEquals(ExifTransform(180, false), exifTransform(ExifInterface.ORIENTATION_ROTATE_180))
    }

    @Test
    fun mirroredOrientationsAlsoFlip() {
        assertEquals(ExifTransform(0, true), exifTransform(ExifInterface.ORIENTATION_FLIP_HORIZONTAL))
        assertEquals(ExifTransform(90, true), exifTransform(ExifInterface.ORIENTATION_TRANSPOSE))
        assertEquals(ExifTransform(270, true), exifTransform(ExifInterface.ORIENTATION_TRANSVERSE))
    }

    @Test
    fun withoutATagThePhotoStaysAsItIs() {
        assertTrue(exifTransform(ExifInterface.ORIENTATION_NORMAL).isIdentity)
        assertTrue(exifTransform(ExifInterface.ORIENTATION_UNDEFINED).isIdentity)
        assertTrue(exifTransform(42).isIdentity)
    }
}
