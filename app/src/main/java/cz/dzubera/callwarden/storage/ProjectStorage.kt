package cz.dzubera.callwarden.storage

import android.content.Context
import cz.dzubera.callwarden.utils.PreferencesUtils
import org.json.JSONObject

class ProjectStorage {

    fun setProjects(newProjects: List<ProjectObject>) {
        projects.clear()
        projects.addAll(newProjects)
    }

    fun setProject(context: Context, po: ProjectObject) {
        PreferencesUtils.saveProjectId(context, po.id)
        PreferencesUtils.saveProjectName(context, po.name)
    }

    fun getProject(context: Context): ProjectObject? {
        var projectId = PreferencesUtils.loadProjectId(context)
        var projectName = PreferencesUtils.loadProjectName(context)
        if (projectId == null || projectName == null) {
            return null
        }
        return ProjectObject(projectId, projectName)
    }

    val projects: MutableList<ProjectObject> = mutableListOf()
}

class ProjectObject(val id: String, val name: String) {


}

fun JSONObject.getProjectObject(): List<ProjectObject> {
    val projects = mutableListOf<ProjectObject>()
    val projectsJson = this.getJSONObject("projects")
    projectsJson.keys().forEach {
        val name = projectsJson.getString(it)
        projects.add(ProjectObject(it, name))
    }
    return projects
}
