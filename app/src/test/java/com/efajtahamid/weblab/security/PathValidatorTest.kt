package com.efajtahamid.weblab.security

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class PathValidatorTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun `resolveSafe allows a normal nested path`() {
        val root = tempFolder.newFolder("project")
        File(root, "src").mkdirs()

        val resolved = PathValidator.resolveSafe(root, "src/index.js")

        assertEquals(File(root, "src/index.js").canonicalFile, resolved)
    }

    @Test
    fun `resolveSafe rejects parent directory traversal`() {
        val root = tempFolder.newFolder("project")

        assertThrows(PathValidator.UnsafePathException::class.java) {
            PathValidator.resolveSafe(root, "../../etc/passwd")
        }
    }

    @Test
    fun `resolveSafe rejects traversal buried inside a deeper path`() {
        val root = tempFolder.newFolder("project")

        assertThrows(PathValidator.UnsafePathException::class.java) {
            PathValidator.resolveSafe(root, "src/../../outside.txt")
        }
    }

    @Test
    fun `resolveZipEntry rejects entries containing double dot segments`() {
        val root = tempFolder.newFolder("extracted")

        assertThrows(PathValidator.UnsafePathException::class.java) {
            PathValidator.resolveZipEntry(root, "../../evil.sh")
        }
    }

    @Test
    fun `resolveZipEntry allows a normal nested entry`() {
        val root = tempFolder.newFolder("extracted")

        val resolved = PathValidator.resolveZipEntry(root, "src/main.js")

        assertTrue(resolved.path.startsWith(root.canonicalFile.path))
    }

    @Test
    fun `isValidEntryName rejects separators and reserved names`() {
        assertFalse(PathValidator.isValidEntryName(""))
        assertFalse(PathValidator.isValidEntryName("."))
        assertFalse(PathValidator.isValidEntryName(".."))
        assertFalse(PathValidator.isValidEntryName("a/b"))
        assertFalse(PathValidator.isValidEntryName("a\\b"))
        assertTrue(PathValidator.isValidEntryName("server.js"))
    }

    @Test
    fun `isValidPort enforces the non-privileged range`() {
        assertFalse(PathValidator.isValidPort(80))
        assertFalse(PathValidator.isValidPort(70000))
        assertTrue(PathValidator.isValidPort(3000))
    }
}
