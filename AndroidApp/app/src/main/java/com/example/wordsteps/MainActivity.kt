package com.example.wordsteps

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.geometry.RoundRect
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.wordsteps.data.api.MLApiService
import com.example.wordsteps.data.database.SpellDatabase
import com.example.wordsteps.data.repository.DatasetLoader
import com.example.wordsteps.data.repository.SpellRepository
import com.example.wordsteps.ui.home.HomeScreen
import com.example.wordsteps.ui.home.HomeViewModel
import com.example.wordsteps.ui.home.HomeViewModelFactory
import com.example.wordsteps.ui.practice.PracticeMode
import com.example.wordsteps.ui.practice.PracticeScreen
import com.example.wordsteps.ui.practice.PracticeViewModel
import com.example.wordsteps.ui.practice.PracticeViewModelFactory
import com.example.wordsteps.ui.stats.StatsScreen
import com.example.wordsteps.ui.stats.StatsViewModel
import com.example.wordsteps.ui.stats.StatsViewModelFactory
import com.example.wordsteps.ui.theme.WordStepsTheme
import com.example.wordsteps.ui.typing.TypingScreen
import com.example.wordsteps.ui.typing.TypingViewModel
import com.example.wordsteps.ui.typing.TypingViewModelFactory

object Routes {
    const val HOME     = "home"
    const val PRACTICE = "practice"
    const val ADAPTIVE = "adaptive"
    const val STATS    = "stats"

    const val TYPING = "typing"
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val database   = SpellDatabase.getDatabase(applicationContext)
        val mlApi      = MLApiService()
        val loader     = DatasetLoader(applicationContext).also { it.loadDataset() }
        val repository = SpellRepository(database, mlApi, loader)

        setContent {
            WordStepsTheme {
                Surface(color = MaterialTheme.colorScheme.background) {
                    WordStepsApp(repository,applicationContext)
                }
            }
        }
    }
}

@Composable
fun WordStepsApp(repository: SpellRepository,appContext: Context) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Routes.HOME) {

        composable(Routes.HOME) {
            val vm: HomeViewModel = viewModel(factory = HomeViewModelFactory(repository))
            HomeScreen(
                viewModel       = vm,
                onStartPractice = { navController.navigate(Routes.PRACTICE) },
                onStartAdaptive = { navController.navigate(Routes.ADAPTIVE) },
                onOpenStats     = { navController.navigate(Routes.STATS)},
                onStartTyping = {navController.navigate(Routes.TYPING)}
            )
        }

        composable(Routes.PRACTICE) { backStackEntry ->
            val vm: PracticeViewModel = viewModel(
                viewModelStoreOwner = backStackEntry,
                factory = PracticeViewModelFactory(repository)
            )
            // init already calls NORMAL mode, nothing extra needed
            PracticeScreen(
                viewModel      = vm,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Routes.ADAPTIVE) { backStackEntry ->
            val vm: PracticeViewModel = viewModel(
                viewModelStoreOwner = backStackEntry,
                factory = PracticeViewModelFactory(repository)
            )
            // Trigger adaptive mode once when first composed
            LaunchedEffect(Unit) {
                vm.loadQuestions(PracticeMode.ADAPTIVE)
            }
            PracticeScreen(
                viewModel      = vm,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Routes.STATS) {
            val vm: StatsViewModel = viewModel(factory = StatsViewModelFactory(repository))
            StatsScreen(
                viewModel      = vm,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Routes.TYPING) { backStackEntry ->
            val vm: TypingViewModel = viewModel(
                viewModelStoreOwner = backStackEntry,
                factory = TypingViewModelFactory(repository, appContext)
            )
            TypingScreen(
                viewModel      = vm,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}