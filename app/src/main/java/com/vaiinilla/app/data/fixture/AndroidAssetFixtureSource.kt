package com.vaiinilla.app.data.fixture

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class AndroidAssetFixtureSource
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) : FixtureSource {
        override fun read(path: String): String =
            context.assets.open(path).bufferedReader().use {
                it.readText()
            }
    }
