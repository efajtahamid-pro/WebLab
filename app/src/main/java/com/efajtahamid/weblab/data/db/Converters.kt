package com.efajtahamid.weblab.data.db

import androidx.room.TypeConverter
import com.efajtahamid.weblab.data.model.ProjectType

class Converters {
    @TypeConverter
    fun fromProjectType(type: ProjectType): String = type.name

    @TypeConverter
    fun toProjectType(value: String): ProjectType =
        runCatching { ProjectType.valueOf(value) }.getOrDefault(ProjectType.STATIC_SITE)
}
