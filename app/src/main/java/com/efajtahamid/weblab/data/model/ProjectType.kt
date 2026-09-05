package com.efajtahamid.weblab.data.model

/**
 * The kinds of projects WebLab understands. Detection and templates key off this.
 */
enum class ProjectType(val label: String, val hasPackageJson: Boolean) {
    STATIC_SITE("Static Website", hasPackageJson = false),
    NODE_APP("Node.js App", hasPackageJson = true),
    VITE_APP("Vite App", hasPackageJson = true);

    companion object {
        /**
         * Best-effort detection of a project's type by inspecting which files it contains.
         * Falls back to STATIC_SITE when nothing more specific is found.
         */
        fun detect(fileNames: Set<String>): ProjectType {
            return when {
                fileNames.contains("vite.config.js") || fileNames.contains("vite.config.ts") -> VITE_APP
                fileNames.contains("package.json") -> NODE_APP
                else -> STATIC_SITE
            }
        }
    }
}
