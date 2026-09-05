package com.efajtahamid.weblab.security

import java.io.File
import java.io.IOException

/**
 * Central guard for every filesystem operation WebLab performs.
 *
 * Rule: nothing WebLab writes, reads, deletes, moves, or extracts may ever land
 * outside the project's own sandbox directory. This is what stands between a
 * malicious project (or a malicious ZIP import) and the rest of the device.
 */
object PathValidator {

    class UnsafePathException(message: String) : IOException(message)

    /**
     * Resolves [childPath] against [sandboxRoot] and throws [UnsafePathException]
     * if the resolved, canonical path would escape the sandbox — e.g. via "../../",
     * absolute path overrides, or symlink tricks.
     */
    @Throws(UnsafePathException::class)
    fun resolveSafe(sandboxRoot: File, childPath: String): File {
        val rootCanonical = sandboxRoot.canonicalFile
        val candidate = File(sandboxRoot, childPath)
        val candidateCanonical = try {
            candidate.canonicalFile
        } catch (e: IOException) {
            throw UnsafePathException("Could not resolve path: $childPath")
        }

        val rootPath = rootCanonical.path + File.separator
        val candidatePath = candidateCanonical.path

        if (candidatePath != rootCanonical.path && !candidatePath.startsWith(rootPath)) {
            throw UnsafePathException("Path escapes project sandbox: $childPath")
        }
        return candidateCanonical
    }

    /**
     * Validates a ZIP entry name before extraction (defends against zip-slip).
     * Returns the safe destination file, or throws.
     */
    @Throws(UnsafePathException::class)
    fun resolveZipEntry(destinationRoot: File, entryName: String): File {
        if (entryName.contains("..")) {
            throw UnsafePathException("Rejected ZIP entry with traversal segment: $entryName")
        }
        return resolveSafe(destinationRoot, entryName)
    }

    /** Validates a user-chosen project or file name: no separators, no empty, sane length. */
    fun isValidEntryName(name: String): Boolean {
        if (name.isBlank()) return false
        if (name == "." || name == "..") return false
        if (name.contains("/") || name.contains("\\")) return false
        if (name.length > 255) return false
        return true
    }

    /** Validates a port number is in the safe, non-privileged, non-reserved range. */
    fun isValidPort(port: Int): Boolean = port in 1024..65535
}
