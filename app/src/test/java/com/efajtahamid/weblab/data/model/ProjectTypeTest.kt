package com.efajtahamid.weblab.data.model

import org.junit.Assert.assertEquals
import org.junit.Test

class ProjectTypeTest {

    @Test
    fun `detects a static site when no package json is present`() {
        assertEquals(ProjectType.STATIC_SITE, ProjectType.detect(setOf("index.html", "style.css")))
    }

    @Test
    fun `detects a node app when package json is present without vite config`() {
        assertEquals(ProjectType.NODE_APP, ProjectType.detect(setOf("package.json", "server.js")))
    }

    @Test
    fun `detects a vite app when a vite config file is present`() {
        assertEquals(ProjectType.VITE_APP, ProjectType.detect(setOf("package.json", "vite.config.js")))
        assertEquals(ProjectType.VITE_APP, ProjectType.detect(setOf("package.json", "vite.config.ts")))
    }
}
