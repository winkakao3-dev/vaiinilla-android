package com.vaiinilla.app.data.fixture

interface FixtureSource {
    fun read(path: String): String
}
