```kotlin
package com.example.rubix

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
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
                    composable(Screen.Home.route) {
                        HomeScreen(
                            onNodeClick = { nodeId ->
                                navController.navigate(Screen.Viewer.createRoute(nodeId))
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