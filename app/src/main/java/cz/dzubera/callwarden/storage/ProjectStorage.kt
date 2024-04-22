package cz.dzubera.callwarden.storage

import android.content.Context
import cz.dzubera.callwarden.utils.PreferencesUtils
import org.json.JSONObject

class ProjectStorage {

    companion object{
         val EMPTY_PROJECT = ProjectObject("-1","<žádný>")
    }
    fun setProjects(newProjects: List<ProjectObject>) {
        projects.clear()
        projects.addAll(newProjects)
        projects.add(0, EMPTY_PROJECT)
    }

    fun setProject(context: Context, po: ProjectObject){
        selectedProject = po
        PreferencesUtils.saveProjectId(context, po.id)
        PreferencesUtils.saveProjectName(context, po.name)
    }

    fun getProject(): ProjectObject? {
        return selectedProject
    }

    private var selectedProject: ProjectObject? = null
    val projects: MutableList<ProjectObject> = mutableListOf(EMPTY_PROJECT)
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
