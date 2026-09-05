package com.efajtahamid.weblab.runtime

import android.content.Context
import com.efajtahamid.weblab.data.model.ManagedProcessInfo
import com.efajtahamid.weblab.data.model.OutputLine
import com.efajtahamid.weblab.data.model.OutputStreamKind
import com.efajtahamid.weblab.data.model.ProcessState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.File
import java.io.IOException
import java.io.InputStreamReader
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Manages the local Node.js runtime and any processes started from it (`node`,
 * `npm`, `npx`, or a raw shell command). This class never fabricates output —
 * if the Node binary asset described in [nodeBinary] isn't present on the
 * device, every start attempt fails with a real, specific error instead of
 * pretending a server is running.
 *
 * PACKAGING NOTE (spec section 4 / 41): Android only allows executing native
 * binaries that were bundled as JNI libraries so they land in a directory the
 * OS marks executable. To ship real Node support, place an ARM64 Node build at:
 *     app/src/main/jniLibs/arm64-v8a/libnode.so
 * (a regular Node executable, just renamed/packaged to satisfy Android's
 * jniLibs convention — this is the same technique terminal-emulator apps use).
 * Until that asset is added, [detectRuntime] correctly reports NOT_INSTALLED.
 */
class NodeRuntimeManager(private val context: Context) {

    enum class RuntimeStatus { NOT_INSTALLED, READY }

    data class RuntimeInfo(val status: RuntimeStatus, val version: String?, val binaryPath: String?)

    private val nodeBinary: File
        get() = File(context.applicationInfo.nativeLibraryDir, "libnode.so")

    private val npmScript: File
        get() = File(context.applicationInfo.nativeLibraryDir, "libnpm_cli.so")

    private val processes = ConcurrentHashMap<String, Process>()
    private val processInfoFlow = MutableStateFlow<Map<String, ManagedProcessInfo>>(emptyMap())
    val activeProcesses: StateFlow<Map<String, ManagedProcessInfo>> = processInfoFlow

    private val _output = MutableSharedFlow<Pair<String, OutputLine>>(extraBufferCapacity = 512)
    val output: SharedFlow<Pair<String, OutputLine>> = _output

    suspend fun detectRuntime(): RuntimeInfo = withContext(Dispatchers.IO) {
        if (!nodeBinary.exists()) {
            return@withContext RuntimeInfo(RuntimeStatus.NOT_INSTALLED, null, null)
        }
        val version = runCatching {
            val process = ProcessBuilder(nodeBinary.absolutePath, "--version").start()
            process.waitFor()
            process.inputStream.bufferedReader().readText().trim()
        }.getOrNull()
        RuntimeInfo(RuntimeStatus.READY, version, nodeBinary.absolutePath)
    }

    /**
     * Starts a process. [command] should NOT include the interpreter — pass
     * the script/args only (e.g. ["server.js"]) and set [useNode]/[useNpm]
     * to pick the interpreter, or pass a raw shell-style [command] with
     * [useNode] = false and [useNpm] = false for plain filesystem commands.
     */
    suspend fun startProcess(
        id: String = UUID.randomUUID().toString(),
        label: String,
        command: List<String>,
        workingDirectory: File,
        environment: Map<String, String> = emptyMap(),
        port: Int? = null,
        useNode: Boolean = false,
        useNpm: Boolean = false
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val interpreter = when {
            useNode -> nodeBinary
            useNpm -> npmScript
            else -> null
        }
        if ((useNode || useNpm) && interpreter?.exists() != true) {
            emitSystemLine(id, "Node runtime is not installed on this device. Add the runtime asset described in NodeRuntimeManager.")
            return@withContext Result.failure(IOException("Node runtime asset not found: ${interpreter?.absolutePath}"))
        }

        val fullCommand = when {
            useNode -> listOf(nodeBinary.absolutePath) + command
            useNpm -> listOf(nodeBinary.absolutePath, npmScript.absolutePath) + command
            else -> command
        }

        updateInfo(id, label, fullCommand, workingDirectory.absolutePath, port, ProcessState.STARTING)

        try {
            val builder = ProcessBuilder(fullCommand)
                .directory(workingDirectory)
                .redirectErrorStream(false)
            builder.environment().putAll(environment)
            port?.let { builder.environment()["PORT"] = it.toString() }

            val process = builder.start()
            processes[id] = process

            // Obtain PID in a way that works on Java 8 (reflection) and on newer JVMs.
            val pid: Long? = try {
                // Try reflectively calling pid() if available at runtime
                val pidMethod = process.javaClass.getMethod("pid")
                val raw = pidMethod.invoke(process)
                when (raw) {
                    is Long -> raw
                    is Int -> raw.toLong()
                    is Number -> raw.toLong()
                    else -> null
                }
            } catch (e: Exception) {
                // Fallback: some Java 8 implementations expose a private 'pid' field
                try {
                    val pidField = process.javaClass.getDeclaredField("pid")
                    pidField.isAccessible = true
                    val raw = pidField.get(process)
                    when (raw) {
                        is Int -> raw.toLong()
                        is Long -> raw
                        is Number -> raw.toLong()
                        else -> null
                    }
                } catch (ex: Exception) {
                    null
                }
            }

            updateInfo(id, label, fullCommand, workingDirectory.absolutePath, port, ProcessState.RUNNING, pid)

            pumpStream(id, process.inputStream, OutputStreamKind.STDOUT)
            pumpStream(id, process.errorStream, OutputStreamKind.STDERR)
            watchForExit(id, process)

            Result.success(Unit)
        } catch (e: IOException) {
            updateInfo(id, label, fullCommand, workingDirectory.absolutePath, port, ProcessState.CRASHED)
            emitSystemLine(id, "Failed to start: ${e.message}")
            Result.failure(e)
        }
    }

    fun stopProcess(id: String) {
        val process = processes[id] ?: return
        updateInfo(id, currentInfo(id)?.label ?: id, currentInfo(id)?.command ?: emptyList(),
            currentInfo(id)?.workingDirectory ?: "", currentInfo(id)?.port, ProcessState.STOPPING)
        process.destroy()
    }

    fun killAll() {
        processes.keys.toList().forEach { stopProcess(it) }
    }

    private fun currentInfo(id: String) = processInfoFlow.value[id]

    private fun pumpStream(id: String, stream: java.io.InputStream, kind: OutputStreamKind) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                BufferedReader(InputStreamReader(stream)).useLines { lines ->
                    lines.forEach { line ->
                        _output.tryEmit(id to OutputLine(kind, line))
                    }
                }
            } catch (_: IOException) {
                // Stream closed because the process exited — watchForExit handles state.
            }
        }
    }

    private fun watchForExit(id: String, process: Process) {
        CoroutineScope(Dispatchers.IO).launch {
            val exitCode = process.waitFor()
            processes.remove(id)
            val info = currentInfo(id) ?: return@launch
            val finalState = if (info.state == ProcessState.STOPPING || exitCode == 0) ProcessState.STOPPED else ProcessState.CRASHED
            updateInfo(id, info.label, info.command, info.workingDirectory, info.port, finalState)
            emitSystemLine(id, "Process exited with code $exitCode")
        }
    }

    private fun updateInfo(
        id: String, label: String, command: List<String>, workingDirectory: String,
        port: Int?, state: ProcessState, pid: Long? = null
    ) {
        processInfoFlow.value = processInfoFlow.value + (id to ManagedProcessInfo(id, label, command, workingDirectory, port, state, pid))
    }

    private fun emitSystemLine(id: String, text: String) {
        _output.tryEmit(id to OutputLine(OutputStreamKind.SYSTEM, text))
    }
}
