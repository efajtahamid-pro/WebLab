package com.efajtahamid.weblab

import android.app.Application
import com.efajtahamid.weblab.data.db.WebLabDatabase
import com.efajtahamid.weblab.data.repository.ProjectRepository

/**
 * Minimal manual dependency container. The spec asks to avoid unnecessary
 * libraries, and this app's dependency graph is small enough that a DI
 * framework would add more ceremony than it saves.
 */
class WebLabApp : Application() {

    lateinit var projectRepository: ProjectRepository
        private set

    override fun onCreate() {
        super.onCreate()
        val db = WebLabDatabase.getInstance(this)
        projectRepository = ProjectRepository(this, db.projectDao())
    }
}
