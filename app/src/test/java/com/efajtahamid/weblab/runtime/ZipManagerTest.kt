package com.efajtahamid.weblab.runtime

import com.efajtahamid.weblab.data.repository.ProjectResult
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class ZipManagerTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun `export then import round-trips project files`() = runTest {
        val projectDir = tempFolder.newFolder("my-site")
        File(projectDir, "index.html").writeText("<h1>Hello</h1>")
        val zipFile = File(tempFolder.newFolder("exports"), "my-site.zip")

        val exportResult = ZipManager.exportProject(projectDir, zipFile)
        assertTrue(exportResult is ProjectResult.Success)

        val importDir = File(tempFolder.newFolder("imports"), "my-site")
        val importResult = ZipManager.importProject(zipFile, importDir)
        assertTrue(importResult is ProjectResult.Success)
        assertTrue(File(importDir, "index.html").exists())
        assertTrue(File(importDir, "index.html").readText().contains("Hello"))
    }

    @Test
    fun `importProject rejects a zip-slip entry`() = runTest {
        val maliciousZip = File(tempFolder.newFolder("malicious"), "evil.zip")
        ZipOutputStream(maliciousZip.outputStream()).use { zos ->
            zos.putNextEntry(ZipEntry("../../evil.sh"))
            zos.write("echo pwned".toByteArray())
            zos.closeEntry()
        }

        val destination = File(tempFolder.newFolder("safe-destination"), "project")
        val result = ZipManager.importProject(maliciousZip, destination)

        assertTrue(result is ProjectResult.Failure)
        assertFalse(File(destination.parentFile, "evil.sh").exists())
    }

    @Test
    fun `importProject rejects a malformed archive`() = runTest {
        val notReallyAZip = tempFolder.newFile("notazip.zip")
        notReallyAZip.writeText("this is just plain text, not a zip")

        val destination = File(tempFolder.newFolder("dest2"), "project")
        val result = ZipManager.importProject(notReallyAZip, destination)

        assertTrue(result is ProjectResult.Failure)
    }
}
