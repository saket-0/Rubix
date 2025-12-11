
package com.example.rubix

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.rubix.ui.editor.NoteEditorScreen
import com.example.rubix.ui.home.HomeScreen
import com.example.rubix.ui.navigation.Screen
import com.example.rubix.ui.theme.RubixTheme
import com.example.rubix.ui.viewer.ViewerScreen
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            RubixTheme {
                val navController = rememberNavController()
                
                NavHost(navController = navController, startDestination = Screen.Home.route) {
                    composable(
                        route = Screen.Home.route,
                        arguments = listOf(
                            navArgument("folderId") {
                                type = NavType.StringType
                                nullable = true
                            }
                        )
                    ) { backStackEntry ->
                        val folderId = backStackEntry.arguments?.getString("folderId")
                        HomeScreen(
                            onNodeClick = { node ->
                                when (node.type) {
                                    com.example.rubix.data.local.NodeType.FOLDER -> {
                                        navController.navigate(Screen.Home.createRoute(node.id))
                                    }
                                    com.example.rubix.data.local.NodeType.NOTE -> {
                                        navController.navigate(Screen.NoteEditor.createRoute(nodeId = node.id))
                                    }
                                    else -> {
                                        navController.navigate(Screen.Viewer.createRoute(node.id))
                                    }
                                }
                            },
                            onCreateNote = {
                                navController.navigate(Screen.NoteEditor.createRoute(parentId = folderId))
                            },
                            onSearchClick = {
                                navController.navigate(Screen.Search.route)
                            }
                        )
                    }
                    
                    composable(
                        route = Screen.NoteEditor.route,
                        arguments = listOf(
                            navArgument("nodeId") {
                                type = NavType.StringType
                                nullable = true
                            },
                            navArgument("parentId") {
                                type = NavType.StringType
                                nullable = true
                            }
                        ),
                        enterTransition = { slideInVertically { it } },
                        exitTransition = { slideOutVertically { it } },
                        popEnterTransition = { slideInVertically { -it } },
                        popExitTransition = { slideOutVertically { it } }
                    ) {
                        NoteEditorScreen(navController = navController)
                    }

                    composable(Screen.Search.route) {
                        com.example.rubix.ui.search.SearchScreen(
                            navController = navController,
                            onNodeClick = { node ->
                                when (node.type) {
                                    com.example.rubix.data.local.NodeType.FOLDER -> {
                                        navController.navigate(Screen.Home.createRoute(node.id))
                                    }
                                    com.example.rubix.data.local.NodeType.NOTE -> {
                                        navController.navigate(Screen.NoteEditor.createRoute(nodeId = node.id))
                                    }
                                    else -> {
                                        navController.navigate(Screen.Viewer.createRoute(node.id))
                                    }
                                }
                            }
                        )
                    }

                    composable(Screen.Viewer.route) { backStackEntry ->
                        ViewerScreen()
                    }
                }
            }
        }
    }
}