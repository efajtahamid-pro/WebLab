package com.efajtahamid.weblab.data.repository

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class FileRepositoryTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var projectRoot: File
    private lateinit var repository: FileRepository

    @Before
    fun setUp() {
        projectRoot = tempFolder.newFolder("project")
        repository = FileRepository(projectRoot)
    }

    @Test
    fun `createFile then readTextFile round-trips content`() = runTest {
        repository.createFile("", "index.html")
        val writeResult = repository.writeTextFile("index.html", "<h1>Hi</h1>")
        assertTrue(writeResult is ProjectResult.Success)

        val readResult = repository.readTextFile("index.html")
        assertTrue(readResult is ProjectResult.Success)
        assertEquals("<h1>Hi</h1>", (readResult as ProjectResult.Success).value)
    }

    @Test
    fun `createFile rejects a name that already exists`() = runTest {
        repository.createFile("", "style.css")
        val second = repository.createFile("", "style.css")
        assertTrue(second is ProjectResult.Failure)
    }

    @Test
    fun `writeTextFile rejects escaping the project sandbox`() = runTest {
        val result = repository.writeTextFile("../../outside.txt", "malicious")
        assertTrue(result is ProjectResult.Failure)
        assertFalse(File(projectRoot.parentFile, "outside.txt").exists())
    }

    @Test
    fun `delete refuses to remove the project root`() = runTest {
        val result = repository.delete("")
        assertTrue(result is ProjectResult.Failure)
        assertTrue(projectRoot.exists())
    }

    @Test
    fun `rename produces the new file and removes the old one`() = runTest {
        repository.createFile("", "old.js")
        val result = repository.rename("old.js", "new.js")
        assertTrue(result is ProjectResult.Success)
        assertTrue(File(projectRoot, "new.js").exists())
        assertFalse(File(projectRoot, "old.js").exists())
    }

    @Test
    fun `search finds matches by filename and by content`() = runTest {
        repository.createFile("", "server.js")
        repository.writeTextFile("server.js", "const port = 3000;\nconsole.log('ready');")

        val results = repository.search("port")

        assertTrue(results.any { it.relativePath == "server.js" && it.lineNumber == 1 })
    }
}
