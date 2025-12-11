package com.example.rubix.ui.navigation

sealed class Screen(val route: String) {
    object Home : Screen("home")
    data class Viewer(val nodeId: String) : Screen("viewer/{nodeId}") {
        companion object {
            const val route = "viewer/{nodeId}"
            fun createRoute(nodeId: String) = "viewer/$nodeId"
        }
    }
}
