package com.efajtahamid.weblab.data.model

/** Mirrors spec section 23 exactly — the UI must never claim RUNNING for a process that died. */
enum class ProcessState { STOPPED, STARTING, RUNNING, STOPPING, CRASHED }

data class ManagedProcessInfo(
    val id: String,
    val label: String,
    val command: List<String>,
    val workingDirectory: String,
    val port: Int? = null,
    val state: ProcessState = ProcessState.STOPPED,
    val pid: Long? = null
)

data class OutputLine(
    val stream: OutputStreamKind,
    val text: String,
    val timestampMs: Long = System.currentTimeMillis()
)

enum class OutputStreamKind { STDOUT, STDERR, SYSTEM }
