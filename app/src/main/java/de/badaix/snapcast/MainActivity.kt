package de.badaix.snapcast

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import dagger.hilt.android.AndroidEntryPoint
import de.badaix.snapcast.data.datasource.DataStoreDataSource
import de.badaix.snapcast.ui.MainScreen
import de.badaix.snapcast.ui.PermissionsHandler
import de.badaix.snapcast.ui.PlayerLogsScreen
import de.badaix.snapcast.ui.navigation.SnapcastDestination
import de.badaix.snapcast.ui.theme.SnapdroidTheme
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    
    @Inject
    lateinit var dataStoreDataSource: DataStoreDataSource
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SnapdroidTheme {
                var hasCompletedOnboarding by remember { mutableStateOf<Boolean?>(null) }
                val scope = rememberCoroutineScope()
                
                // Check onboarding status
                LaunchedEffect(Unit) {
                    hasCompletedOnboarding = dataStoreDataSource.hasCompletedOnboarding()
                }
                
                when (hasCompletedOnboarding) {
                    null -> {
                        // Loading state
                    }
                    false -> {
                        // Show permissions screen
                        PermissionsHandler(
                            onPermissionsGranted = {
                                scope.launch {
                                    dataStoreDataSource.setOnboardingCompleted(true)
                                    hasCompletedOnboarding = true
                                }
                            }
                        )
                    }
                    true -> {
                        // Show main app
                        val navController = rememberNavController()
                        
                        NavHost(
                            navController = navController,
                            startDestination = SnapcastDestination.Home.route
                        ) {
                            composable(SnapcastDestination.Home.route) {
                                MainScreen(
                                    onNavigateToLogs = {
                                        navController.navigate(SnapcastDestination.PlayerLogs.route)
                                    }
                                )
                            }
                            
                            composable(SnapcastDestination.PlayerLogs.route) {
                                PlayerLogsScreen(
                                    onNavigateBack = {
                                        navController.popBackStack()
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
