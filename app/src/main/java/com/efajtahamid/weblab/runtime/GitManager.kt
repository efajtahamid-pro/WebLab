package com.efajtahamid.weblab.runtime

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException

data class GitCommandResult(val output: String, val exitCode: Int, val isError: Boolean)

/**
 * Clean runtime abstraction for Git (spec section 20). WebLab does not bundle
 * a Git binary in this build, and this class never fabricates command output —
 * every call really invokes `git` via ProcessBuilder and reports the real
 * result. On a stock Android device (no `git` on PATH) that means a clear,
 * honest "Git is not available" result rather than fake success.
 *
 * Adding real Git support later only requires bundling a Git binary the same
 * way described in NodeRuntimeManager (as a jniLibs asset) and pointing
 * [gitBinaryPath] at it — no other code in this class needs to change.
 */
class GitManager(private val gitBinaryPath: String = "git") {

    suspend fun isAvailable(): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            val process = ProcessBuilder(gitBinaryPath, "--version").start()
            process.waitFor() == 0
        }.getOrDefault(false)
    }

    suspend fun init(repoDir: File): GitCommandResult = run(repoDir, "init")
    suspend fun status(repoDir: File): GitCommandResult = run(repoDir, "status", "--short")
    suspend fun add(repoDir: File, path: String): GitCommandResult = run(repoDir, "add", path)
    suspend fun commit(repoDir: File, message: String): GitCommandResult = run(repoDir, "commit", "-m", message)
    suspend fun log(repoDir: File): GitCommandResult = run(repoDir, "log", "--oneline", "-n", "50")

    private suspend fun run(repoDir: File, vararg args: String): GitCommandResult = withContext(Dispatchers.IO) {
        try {
            val process = ProcessBuilder(listOf(gitBinaryPath) + args)
                .directory(repoDir)
                .redirectErrorStream(true)
                .start()
            val text = process.inputStream.bufferedReader().readText()
            val exitCode = process.waitFor()
            GitCommandResult(text.trim(), exitCode, isError = exitCode != 0)
        } catch (e: IOException) {
            GitCommandResult(
                "Git is not available on this device yet. WebLab's Git support is ready to enable once a Git binary is bundled.",
                exitCode = -1,
                isError = true
            )
        }
    }
}
