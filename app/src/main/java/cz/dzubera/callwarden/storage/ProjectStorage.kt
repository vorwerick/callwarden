package cz.dzubera.callwarden.storage

import org.json.JSONObject

class ProjectStorage {
    fun setProjects(newProjects: List<ProjectObject>) {
        projects.clear()
        projects.addAll(newProjects)
    }

    fun setProject(po: ProjectObject){
        selectedProject = po

    }

    fun getProject(): ProjectObject? {
        return selectedProject
    }

    private var selectedProject: ProjectObject? = null
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
