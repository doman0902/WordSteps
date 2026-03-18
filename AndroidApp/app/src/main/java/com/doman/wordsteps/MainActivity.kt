package com.doman.wordsteps

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.doman.wordsteps.data.api.MLApiService
import com.doman.wordsteps.data.database.SpellDatabase
import com.doman.wordsteps.data.preferences.UserPreferences
import com.doman.wordsteps.data.repository.DatasetLoader
import com.doman.wordsteps.data.repository.SpellRepository
import com.doman.wordsteps.ui.home.HomeScreen
import com.doman.wordsteps.ui.home.HomeViewModel
import com.doman.wordsteps.ui.home.HomeViewModelFactory
import com.doman.wordsteps.ui.patternhunt.PatternHuntScreen
import com.doman.wordsteps.ui.patternhunt.PatternHuntViewModel
import com.doman.wordsteps.ui.patternhunt.PatternHuntViewModelFactory
import com.doman.wordsteps.ui.practice.PracticeMode
import com.doman.wordsteps.ui.practice.PracticeScreen
import com.doman.wordsteps.ui.practice.PracticeViewModel
import com.doman.wordsteps.ui.practice.PracticeViewModelFactory
import com.doman.wordsteps.ui.settings.SettingsScreen
import com.doman.wordsteps.ui.settings.SettingsViewModel
import com.doman.wordsteps.ui.settings.SettingsViewModelFactory
import com.doman.wordsteps.ui.stats.StatsScreen
import com.doman.wordsteps.ui.stats.StatsViewModel
import com.doman.wordsteps.ui.stats.StatsViewModelFactory
import com.doman.wordsteps.ui.theme.WordStepsTheme
import com.doman.wordsteps.ui.typing.TypingScreen
import com.doman.wordsteps.ui.typing.TypingViewModel
import com.doman.wordsteps.ui.typing.TypingViewModelFactory
import com.doman.wordsteps.ui.reconstruction.ReconstructionScreen
import com.doman.wordsteps.ui.reconstruction.ReconstructionViewModel
import com.doman.wordsteps.ui.reconstruction.ReconstructionViewModelFactory
import com.doman.wordsteps.ui.timed.TimedScreen
import com.doman.wordsteps.ui.timed.TimedViewModel
import com.doman.wordsteps.ui.timed.TimedViewModelFactory

object Routes {
    const val HOME     = "home"
    const val PRACTICE = "practice"
    const val ADAPTIVE = "adaptive"
    const val STATS    = "stats"
    const val TYPING   = "typing"

    const val TIMED="timed"
    const val SETTINGS = "settings"

    const val RECONSTRUCTION = "reconstruction"

    const val PATTERN_HUNT     = "pattern_hunt"
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val prefs      = UserPreferences(applicationContext)
        val database   = SpellDatabase.getDatabase(applicationContext)
        val mlApi      = MLApiService(prefs)
        val loader     = DatasetLoader(applicationContext).also { it.loadDataset() }
        val repository = SpellRepository(database, mlApi, loader)


        setContent {
            WordStepsTheme {
                Surface(color = MaterialTheme.colorScheme.background) {
                    WordStepsApp(repository, applicationContext, prefs, database)
                }
            }
        }
    }
}

@Composable
fun WordStepsApp(
    repository: SpellRepository,
    appContext: Context,
    prefs: UserPreferences,
    database: SpellDatabase
) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Routes.HOME) {

        composable(Routes.HOME) {
            val vm: HomeViewModel = viewModel(factory = HomeViewModelFactory(repository,prefs))
            HomeScreen(
                viewModel       = vm,
                onStartPractice = { navController.navigate(Routes.PRACTICE) },
                onStartAdaptive = { navController.navigate(Routes.ADAPTIVE) },
                onOpenStats     = { navController.navigate(Routes.STATS) },
                onStartTyping   = { navController.navigate(Routes.TYPING) },
                onStartReconstruction = { navController.navigate(Routes.RECONSTRUCTION) },
                onStartTimed = { navController.navigate(Routes.TIMED) },
                onStartPatternHunt    = { navController.navigate(Routes.PATTERN_HUNT) },
                onOpenSettings  = { navController.navigate(Routes.SETTINGS) }
            )
        }

        composable(Routes.PRACTICE) { backStackEntry ->
            val vm: PracticeViewModel = viewModel(
                viewModelStoreOwner = backStackEntry,
                factory = PracticeViewModelFactory(repository)
            )
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
            LaunchedEffect(Unit) {
                vm.loadQuestions(PracticeMode.ADAPTIVE)
            }
            PracticeScreen(
                viewModel      = vm,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Routes.STATS) {
            val vm: StatsViewModel = viewModel(factory = StatsViewModelFactory(repository,database))
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

        composable(Routes.RECONSTRUCTION) { backStackEntry ->
            val vm: ReconstructionViewModel = viewModel(
                viewModelStoreOwner = backStackEntry,
                factory = ReconstructionViewModelFactory(repository, database)
            )
            ReconstructionScreen(
                viewModel      = vm,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(Routes.TIMED) {
            val vm: TimedViewModel = viewModel(factory = TimedViewModelFactory(repository,database))
            TimedScreen(viewModel = vm, onNavigateBack = { navController.popBackStack() })
        }

        composable(Routes.PATTERN_HUNT) { backStackEntry ->
            val vm: PatternHuntViewModel = viewModel(
                viewModelStoreOwner = backStackEntry,
                factory = PatternHuntViewModelFactory(repository)
            )
            PatternHuntScreen(
                viewModel      = vm,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Routes.SETTINGS) {
            val vm: SettingsViewModel = viewModel(
                factory = SettingsViewModelFactory(prefs, database)
            )
            SettingsScreen(
                viewModel      = vm,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}