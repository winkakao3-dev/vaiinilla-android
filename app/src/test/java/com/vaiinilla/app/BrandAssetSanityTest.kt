package com.vaiinilla.app

import android.content.Context
import android.content.res.Configuration
import android.graphics.BitmapFactory
import android.graphics.Color
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class BrandAssetSanityTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun `dark Vaiinilla mark keeps transparent exterior`() {
        val bitmap = BitmapFactory.decodeResource(context.resources, R.drawable.v_mark_dark)
        assertTrue("dark mark must decode", bitmap != null)
        assertEquals(0, Color.alpha(bitmap.getPixel(0, 0)))
        assertEquals(0, Color.alpha(bitmap.getPixel(bitmap.width - 1, bitmap.height - 1)))
    }

    @Test
    fun `launcher background follows day and night configuration`() {
        fun launcherBackground(night: Boolean): Int {
            val configuration =
                Configuration(context.resources.configuration).apply {
                    uiMode =
                        (uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or
                        if (night) Configuration.UI_MODE_NIGHT_YES else Configuration.UI_MODE_NIGHT_NO
                }
            val themedContext = context.createConfigurationContext(configuration)
            return themedContext.getColor(R.color.ic_launcher_bg)
        }

        assertEquals(Color.rgb(244, 241, 231), launcherBackground(night = false))
        assertEquals(Color.rgb(23, 24, 23), launcherBackground(night = true))
    }
}
