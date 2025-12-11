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
}
