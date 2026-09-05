package com.efajtahamid.weblab.runtime

import com.efajtahamid.weblab.data.repository.ProjectResult
import com.efajtahamid.weblab.security.PathValidator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * Project export/import as ZIP (spec section 19). Every entry is validated
 * through [PathValidator] before it touches disk, and import enforces hard
 * caps on entry count and total uncompressed size so a malformed or hostile
 * archive can't exhaust device storage (a basic archive-bomb defense).
 */
object ZipManager {

    private const val MAX_ENTRIES = 20_000
    private const val MAX_TOTAL_UNCOMPRESSED_BYTES = 500L * 1024 * 1024 // 500 MB

    suspend fun exportProject(projectDir: File, destinationZip: File): ProjectResult<File> =
        withContext(Dispatchers.IO) {
            try {
                destinationZip.parentFile?.mkdirs()
                ZipOutputStream(destinationZip.outputStream()).use { zos ->
                    projectDir.walkTopDown().filter { it.isFile }.forEach { file ->
                        val entryName = file.relativeTo(projectDir).path
                        zos.putNextEntry(ZipEntry(entryName))
                        file.inputStream().use { it.copyTo(zos) }
                        zos.closeEntry()
                    }
                }
                ProjectResult.Success(destinationZip)
            } catch (e: IOException) {
                ProjectResult.Failure("Export failed: ${e.message}")
            }
        }

    suspend fun importProject(sourceZip: File, destinationDir: File): ProjectResult<File> =
        withContext(Dispatchers.IO) {
            if (!isValidZip(sourceZip)) {
                return@withContext ProjectResult.Failure("Invalid or corrupted ZIP archive.")
            }

            var entryCount = 0
            var totalBytes = 0L

            try {
                destinationDir.mkdirs()
                ZipInputStream(sourceZip.inputStream()).use { zis ->
                    var entry = zis.nextEntry
                    while (entry != null) {
                        entryCount++
                        if (entryCount > MAX_ENTRIES) {
                            throw IOException("Archive has too many entries (limit $MAX_ENTRIES).")
                        }

                        val safeTarget = try {
                            PathValidator.resolveZipEntry(destinationDir, entry.name)
                        } catch (e: PathValidator.UnsafePathException) {
                            throw IOException("Rejected unsafe entry: ${entry.name}")
                        }

                        if (entry.isDirectory) {
                            safeTarget.mkdirs()
                        } else {
                            safeTarget.parentFile?.mkdirs()
                            safeTarget.outputStream().use { out ->
                                val buffer = ByteArray(8 * 1024)
                                var read = zis.read(buffer)
                                while (read >= 0) {
                                    totalBytes += read
                                    if (totalBytes > MAX_TOTAL_UNCOMPRESSED_BYTES) {
                                        throw IOException("Archive exceeds the maximum allowed size.")
                                    }
                                    out.write(buffer, 0, read)
                                    read = zis.read(buffer)
                                }
                            }
                        }
                        zis.closeEntry()
                        entry = zis.nextEntry
                    }
                }
                ProjectResult.Success(destinationDir)
            } catch (e: IOException) {
                destinationDir.deleteRecursively()
                ProjectResult.Failure(e.message ?: "Import failed")
            }
        }

    private fun isValidZip(file: File): Boolean = try {
        ZipFile(file).use { true }
    } catch (e: IOException) {
        false
    }
}
