package com.example.rubix.ui.navigation

sealed class Screen(val route: String) {
    object Home : Screen("home?folderId={folderId}") {
        fun createRoute(folderId: String?) = if (folderId != null) "home?folderId=$folderId" else "home"
    }
    data class Viewer(val nodeId: String) : Screen("viewer/{nodeId}") {
        companion object {
            const val route = "viewer/{nodeId}"
            fun createRoute(nodeId: String) = "viewer/$nodeId"
        }
    }
    object NoteEditor : Screen("note_editor?nodeId={nodeId}&parentId={parentId}") {
        fun createRoute(nodeId: String? = null, parentId: String? = null): String {
            return if (nodeId != null) {
                "note_editor?nodeId=$nodeId"
            } else if (parentId != null) {
                "note_editor?parentId=$parentId"
            } else {
                "note_editor"
            }
        }
    }
    object Search : Screen("search")
    object Camera : Screen("camera?folderId={folderId}") {
        fun createRoute(folderId: String?) = if (folderId != null) "camera?folderId=$folderId" else "camera"
    }
    object Archive : Screen("archive")
    object Trash : Screen("trash")
}

