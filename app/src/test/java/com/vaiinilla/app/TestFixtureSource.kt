package com.vaiinilla.app

import com.vaiinilla.app.data.fixture.FixtureSource
import java.io.File

class TestFixtureSource : FixtureSource {
    private val root = File(
        checkNotNull(System.getProperty("vaiinilla.fixtureDir")) {
            "Falta la propiedad vaiinilla.fixtureDir para ejecutar pruebas."
        },
    )

    override fun read(path: String): String = File(root, path.removePrefix("fixtures/"))
        .readText(Charsets.UTF_8)
}
