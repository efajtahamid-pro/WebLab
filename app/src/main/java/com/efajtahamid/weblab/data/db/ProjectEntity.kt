package com.efajtahamid.weblab.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.efajtahamid.weblab.data.model.ProjectType
import java.util.UUID

/**
 * Room only stores project metadata (section 25 of the spec). The actual source
 * files for a project live under [dirPath] on disk and are never duplicated into
 * the database.
 */
@Entity(tableName = "projects")
data class ProjectEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    val dirPath: String,
    val projectType: ProjectType,
    val createdAt: Long,
    val updatedAt: Long,
    val lastOpenedFilePath: String? = null,
    val defaultPort: Int = 3000
)
