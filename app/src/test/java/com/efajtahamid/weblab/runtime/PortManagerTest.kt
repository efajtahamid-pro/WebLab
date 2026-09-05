package com.efajtahamid.weblab.runtime

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.InetAddress
import java.net.ServerSocket

class PortManagerTest {

    @Test
    fun `isPortInUse detects a genuinely bound port`() {
        val socket = ServerSocket(0, 1, InetAddress.getByName("127.0.0.1"))
        try {
            assertTrue(PortManager.isPortInUse(socket.localPort))
        } finally {
            socket.close()
        }
    }

    @Test
    fun `findAvailablePort returns a port that is not in use`() {
        val port = PortManager.findAvailablePort(50123)
        assertFalse(PortManager.isPortInUse(port))
    }

    @Test
    fun `findAvailablePort skips forward when the preferred port is occupied`() {
        val socket = ServerSocket(0, 1, InetAddress.getByName("127.0.0.1"))
        try {
            val chosen = PortManager.findAvailablePort(socket.localPort)
            assertTrue(chosen != socket.localPort)
        } finally {
            socket.close()
        }
    }
}
