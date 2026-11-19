package io.github.ddiling.mydayboard.core.database

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Only checks that the version 1 schema file sits where migration tests will look for it, so
 * deleting or moving it fails here. It cannot catch `exportSchema = false`: the file stays.
 * That it is committed is checked by CI ("Check that Room schemas are committed").
 * Unit tests run in the module directory, so the path is relative to `core/database/`.
 */
class MyDayboardDatabaseSchemaTest {
    @Test
    fun `the version 1 schema file is in the schemas directory`() {
        val version = 1
        val schema = File("schemas/${MyDayboardDatabase::class.java.name}/$version.json")

        assertTrue("Missing ${schema.path}: build the module and commit it", schema.isFile)
    }
}
