package com.vaiinilla.app.ui.order

import com.vaiinilla.app.ui.screenshot.ScreenshotFixtures
import org.junit.Assert.assertEquals
import org.junit.Test

/** Problema 4 del QA: la categoría del negocio anterior no vacía el menú del nuevo. */
class StaleCategoryFilterTest {
    @Test
    fun `una categoria que no existe en este negocio no filtra`() {
        val state = ScreenshotFixtures.catalogLoadedState()
        val all = state.filteredProducts

        val stale = state.copy(selectedCategoryId = Int.MAX_VALUE)

        assertEquals(all, stale.filteredProducts)
    }

    @Test
    fun `una categoria de este negocio si filtra`() {
        val state = ScreenshotFixtures.catalogLoadedState()
        val category = state.catalog!!.categories.first()

        val filtered = state.copy(selectedCategoryId = category.id).filteredProducts

        assertEquals(state.catalog!!.products.count { it.categoryId == category.id }, filtered.size)
    }
}
