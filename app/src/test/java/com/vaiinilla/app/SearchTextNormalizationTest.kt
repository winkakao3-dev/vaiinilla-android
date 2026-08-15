package com.vaiinilla.app

import com.vaiinilla.app.core.text.normalizeForSearch
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchTextNormalizationTest {
    @Test
    fun `matches accented product names when query omits accents`() {
        assertTrue("Café americano".normalizeForSearch().contains("cafe".normalizeForSearch()))
    }
}
