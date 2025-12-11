```kotlin
package com.example.rubix

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.rubix.ui.home.HomeScreen
import com.example.rubix.ui.theme.RubixTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            RubixTheme {
                HomeScreen()
            }
        }
    }
}
```