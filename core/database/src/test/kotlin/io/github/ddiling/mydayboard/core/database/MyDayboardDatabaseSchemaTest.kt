package io.github.ddiling.mydayboard.core.database

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Guards the export itself: `exportSchema = false` or a moved schema directory would leave
 * the next migration with no old schema to test against. That the file is also committed is
 * checked by CI ("Check that Room schemas are committed"), because compiling recreates it.
 * Unit tests run in the module directory, so the path is relative to `core/database/`.
 */
class MyDayboardDatabaseSchemaTest {
    @Test
    fun `the schema of the current version is exported`() {
        val version = 1
        val schema = File("schemas/${MyDayboardDatabase::class.java.name}/$version.json")

        assertTrue("Missing ${schema.path}: build the module and commit it", schema.isFile)
    }
}
