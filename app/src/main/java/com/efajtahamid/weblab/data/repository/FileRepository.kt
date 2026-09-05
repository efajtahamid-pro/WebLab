package com.efajtahamid.weblab.data.repository

import com.efajtahamid.weblab.data.model.FileNode
import com.efajtahamid.weblab.security.PathValidator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException

/**
 * All filesystem mutations for a single project go through here, and every one
 * of them is resolved through [PathValidator] first — this is the sandbox
 * boundary described in section 10 and 22 of the spec.
 */
class FileRepository(private val projectRoot: File) {

    suspend fun readTree(): FileNode = withContext(Dispatchers.IO) {
        FileNode.buildTree(projectRoot)
    }

    suspend fun readTextFile(relativePath: String): ProjectResult<String> = withContext(Dispatchers.IO) {
        try {
            val target = PathValidator.resolveSafe(projectRoot, relativePath)
            if (!target.exists() || target.isDirectory) {
                return@withContext ProjectResult.Failure("File not found: $relativePath")
            }
            if (target.length() > FileNode.MAX_EDITABLE_FILE_BYTES) {
                return@withContext ProjectResult.Failure(
                    "This file is too large to safely edit in WebLab."
                )
            }
            ProjectResult.Success(target.readText())
        } catch (e: PathValidator.UnsafePathException) {
            ProjectResult.Failure(e.message ?: "Unsafe path")
        } catch (e: IOException) {
            ProjectResult.Failure("Could not read file: ${e.message}")
        }
    }

    suspend fun writeTextFile(relativePath: String, content: String): ProjectResult<Unit> =
        withContext(Dispatchers.IO) {
            try {
                val target = PathValidator.resolveSafe(projectRoot, relativePath)
                target.parentFile?.mkdirs()
                target.writeText(content)
                ProjectResult.Success(Unit)
            } catch (e: PathValidator.UnsafePathException) {
                ProjectResult.Failure(e.message ?: "Unsafe path")
            } catch (e: IOException) {
                ProjectResult.Failure("Could not save file: ${e.message}")
            }
        }

    suspend fun createFile(relativeDir: String, fileName: String): ProjectResult<File> =
        withContext(Dispatchers.IO) {
            if (!PathValidator.isValidEntryName(fileName)) {
                return@withContext ProjectResult.Failure("Invalid file name.")
            }
            try {
                val dir = PathValidator.resolveSafe(projectRoot, relativeDir)
                val target = File(dir, fileName)
                if (target.exists()) return@withContext ProjectResult.Failure("\"$fileName\" already exists.")
                dir.mkdirs()
                target.createNewFile()
                ProjectResult.Success(target)
            } catch (e: PathValidator.UnsafePathException) {
                ProjectResult.Failure(e.message ?: "Unsafe path")
            } catch (e: IOException) {
                ProjectResult.Failure("Could not create file: ${e.message}")
            }
        }

    suspend fun createFolder(relativeDir: String, folderName: String): ProjectResult<File> =
        withContext(Dispatchers.IO) {
            if (!PathValidator.isValidEntryName(folderName)) {
                return@withContext ProjectResult.Failure("Invalid folder name.")
            }
            try {
                val dir = PathValidator.resolveSafe(projectRoot, relativeDir)
                val target = File(dir, folderName)
                if (target.exists()) return@withContext ProjectResult.Failure("\"$folderName\" already exists.")
                target.mkdirs()
                ProjectResult.Success(target)
            } catch (e: PathValidator.UnsafePathException) {
                ProjectResult.Failure(e.message ?: "Unsafe path")
            } catch (e: IOException) {
                ProjectResult.Failure("Could not create folder: ${e.message}")
            }
        }

    suspend fun rename(relativePath: String, newName: String): ProjectResult<File> = withContext(Dispatchers.IO) {
        if (!PathValidator.isValidEntryName(newName)) {
            return@withContext ProjectResult.Failure("Invalid name.")
        }
        try {
            val target = PathValidator.resolveSafe(projectRoot, relativePath)
            val renamed = File(target.parentFile, newName)
            if (renamed.exists()) return@withContext ProjectResult.Failure("\"$newName\" already exists.")
            if (!target.renameTo(renamed)) return@withContext ProjectResult.Failure("Rename failed.")
            ProjectResult.Success(renamed)
        } catch (e: PathValidator.UnsafePathException) {
            ProjectResult.Failure(e.message ?: "Unsafe path")
        }
    }

    suspend fun delete(relativePath: String): ProjectResult<Unit> = withContext(Dispatchers.IO) {
        try {
            val target = PathValidator.resolveSafe(projectRoot, relativePath)
            if (target == projectRoot) return@withContext ProjectResult.Failure("Cannot delete the project root.")
            val ok = if (target.isDirectory) target.deleteRecursively() else target.delete()
            if (!ok) return@withContext ProjectResult.Failure("Delete failed.")
            ProjectResult.Success(Unit)
        } catch (e: PathValidator.UnsafePathException) {
            ProjectResult.Failure(e.message ?: "Unsafe path")
        }
    }

    suspend fun duplicate(relativePath: String): ProjectResult<File> = withContext(Dispatchers.IO) {
        try {
            val target = PathValidator.resolveSafe(projectRoot, relativePath)
            val copyName = "${target.nameWithoutExtension}_copy${
                if (target.extension.isNotEmpty()) ".${target.extension}" else ""
            }"
            val dest = File(target.parentFile, copyName)
            target.copyRecursively(dest, overwrite = false)
            ProjectResult.Success(dest)
        } catch (e: PathValidator.UnsafePathException) {
            ProjectResult.Failure(e.message ?: "Unsafe path")
        } catch (e: IOException) {
            ProjectResult.Failure("Could not duplicate: ${e.message}")
        }
    }

    suspend fun move(relativeSourcePath: String, relativeDestDir: String): ProjectResult<File> =
        withContext(Dispatchers.IO) {
            try {
                val source = PathValidator.resolveSafe(projectRoot, relativeSourcePath)
                val destDir = PathValidator.resolveSafe(projectRoot, relativeDestDir)
                val dest = File(destDir, source.name)
                if (dest.exists()) return@withContext ProjectResult.Failure("\"${source.name}\" already exists there.")
                if (!source.renameTo(dest)) return@withContext ProjectResult.Failure("Move failed.")
                ProjectResult.Success(dest)
            } catch (e: PathValidator.UnsafePathException) {
                ProjectResult.Failure(e.message ?: "Unsafe path")
            }
        }

    /** Global search across file names and text-file contents (spec section 28). */
    data class SearchMatch(val relativePath: String, val lineNumber: Int?, val lineText: String?)

    suspend fun search(query: String): List<SearchMatch> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext emptyList()
        val results = mutableListOf<SearchMatch>()
        projectRoot.walkTopDown()
            .filter { it.isFile }
            .forEach { file ->
                val rel = file.relativeTo(projectRoot).path
                if (file.name.contains(query, ignoreCase = true)) {
                    results += SearchMatch(rel, null, null)
                }
                if (file.length() <= FileNode.MAX_EDITABLE_FILE_BYTES) {
                    runCatching {
                        file.readLines().forEachIndexed { index, line ->
                            if (line.contains(query, ignoreCase = true)) {
                                results += SearchMatch(rel, index + 1, line.trim().take(160))
                            }
                        }
                    }
                }
            }
        results
    }
}
