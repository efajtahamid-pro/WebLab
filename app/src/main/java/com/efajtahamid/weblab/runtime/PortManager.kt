package com.efajtahamid.weblab.runtime

import com.efajtahamid.weblab.security.PathValidator
import java.io.IOException
import java.net.InetAddress
import java.net.ServerSocket

/**
 * Real local port probing — no simulated port state. Section 13 of the spec.
 */
object PortManager {

    fun isPortInUse(port: Int): Boolean {
        return try {
            ServerSocket(port, 1, InetAddress.getByName("127.0.0.1")).use { false }
        } catch (e: IOException) {
            true
        }
    }

    /** Finds the first free port starting at [preferred], scanning forward within the valid range. */
    fun findAvailablePort(preferred: Int): Int {
        if (PathValidator.isValidPort(preferred) && !isPortInUse(preferred)) return preferred
        var candidate = preferred
        var attempts = 0
        while (attempts < 200) {
            candidate = if (candidate >= 65535) 1024 else candidate + 1
            if (PathValidator.isValidPort(candidate) && !isPortInUse(candidate)) return candidate
            attempts++
        }
        // Fall back to an ephemeral port chosen by the OS itself.
        return ServerSocket(0).use { it.localPort }
    }
}
