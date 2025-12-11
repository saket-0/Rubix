```kotlin
package com.example.rubix

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
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
                    ) {
                        HomeScreen(
                            onNodeClick = { node ->
                                if (node.type == com.example.rubix.data.local.NodeType.FOLDER) {
                                  navController.navigate(Screen.Home.createRoute(node.id))
                                } else {
                                  navController.navigate(Screen.Viewer.createRoute(node.id))
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
```