package com.efajtahamid.weblab.data.model

import java.io.File

/**
 * A single entry in the project file explorer. Wraps a real java.io.File on disk —
 * WebLab never stores file contents anywhere but the filesystem itself.
 */
data class FileNode(
    val file: File,
    val isDirectory: Boolean,
    val children: List<FileNode> = emptyList()
) {
    val name: String get() = file.name
    val path: String get() = file.absolutePath
    val sizeBytes: Long get() = if (isDirectory) 0L else runCatching { file.length() }.getOrDefault(0L)

    companion object {
        /** Maximum file size WebLab will open directly in the Compose editor. See section 37 of the spec. */
        const val MAX_EDITABLE_FILE_BYTES: Long = 2L * 1024 * 1024 // 2 MB

        fun buildTree(root: File): FileNode {
            val children = root.listFiles()
                ?.sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() }))
                ?.map { buildTree(it) }
                ?: emptyList()
            return FileNode(root, root.isDirectory, children)
        }
    }
}

enum class EditorLanguage {
    HTML, CSS, JAVASCRIPT, TYPESCRIPT, JSON, MARKDOWN, PLAIN;

    companion object {
        fun fromFileName(name: String): EditorLanguage = when (name.substringAfterLast('.', "").lowercase()) {
            "html", "htm" -> HTML
            "css" -> CSS
            "js", "mjs", "cjs", "jsx" -> JAVASCRIPT
            "ts", "tsx" -> TYPESCRIPT
            "json" -> JSON
            "md", "markdown" -> MARKDOWN
            else -> PLAIN
        }
    }
}
