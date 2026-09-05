package com.efajtahamid.weblab.data.repository

import android.content.Context
import com.efajtahamid.weblab.data.db.ProjectDao
import com.efajtahamid.weblab.data.db.ProjectEntity
import com.efajtahamid.weblab.data.model.ProjectType
import com.efajtahamid.weblab.security.PathValidator
import com.efajtahamid.weblab.template.ProjectTemplates
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException

sealed class ProjectResult<out T> {
    data class Success<T>(val value: T) : ProjectResult<T>()
    data class Failure(val message: String) : ProjectResult<Nothing>()
}

/**
 * Owns the lifecycle of a WebLab project: its metadata row in Room, and its real
 * directory under the app's private files dir (`projects/<id>`).
 */
class ProjectRepository(
    private val appContext: Context,
    private val projectDao: ProjectDao
) {
    private val projectsRoot: File
        get() = File(appContext.filesDir, "projects").apply { if (!exists()) mkdirs() }

    fun observeProjects(): Flow<List<ProjectEntity>> = projectDao.observeAll()

    suspend fun getProject(id: String): ProjectEntity? = projectDao.findById(id)

    fun directoryFor(project: ProjectEntity): File = File(project.dirPath)

    suspend fun createProject(name: String, type: ProjectType): ProjectResult<ProjectEntity> =
        withContext(Dispatchers.IO) {
            val trimmed = name.trim()
            if (!PathValidator.isValidEntryName(trimmed)) {
                return@withContext ProjectResult.Failure("Enter a valid project name (no slashes, not empty).")
            }
            if (projectDao.countByName(trimmed) > 0) {
                return@withContext ProjectResult.Failure("A project named \"$trimmed\" already exists.")
            }

            val dir = File(projectsRoot, trimmed)
            if (dir.exists()) {
                return@withContext ProjectResult.Failure("A folder for \"$trimmed\" already exists on disk.")
            }

            try {
                dir.mkdirs()
                ProjectTemplates.scaffold(dir, type)
            } catch (e: IOException) {
                dir.deleteRecursively()
                return@withContext ProjectResult.Failure("Could not create project files: ${e.message}")
            }

            val now = System.currentTimeMillis()
            val entity = ProjectEntity(
                name = trimmed,
                dirPath = dir.absolutePath,
                projectType = type,
                createdAt = now,
                updatedAt = now
            )
            projectDao.upsert(entity)
            ProjectResult.Success(entity)
        }

    suspend fun deleteProject(project: ProjectEntity): ProjectResult<Unit> = withContext(Dispatchers.IO) {
        try {
            File(project.dirPath).deleteRecursively()
            projectDao.delete(project)
            ProjectResult.Success(Unit)
        } catch (e: IOException) {
            ProjectResult.Failure("Could not delete project: ${e.message}")
        }
    }

    suspend fun touch(project: ProjectEntity, lastOpenedFilePath: String? = project.lastOpenedFilePath) {
        withContext(Dispatchers.IO) {
            projectDao.update(
                project.copy(updatedAt = System.currentTimeMillis(), lastOpenedFilePath = lastOpenedFilePath)
            )
        }
    }

    /** Re-detects a project's type by inspecting its current file set (spec section 5). */
    suspend fun refreshDetectedType(project: ProjectEntity): ProjectEntity = withContext(Dispatchers.IO) {
        val names = File(project.dirPath).list()?.toSet() ?: emptySet()
        val detected = ProjectType.detect(names)
        if (detected != project.projectType) {
            val updated = project.copy(projectType = detected, updatedAt = System.currentTimeMillis())
            projectDao.update(updated)
            updated
        } else project
    }
}
