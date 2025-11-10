package com.example.androidapp.navigation

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.androidapp.ui.screens.*

@Composable
fun NavGraph(navController:NavHostController=rememberNavController()) {
    NavHost(navController, startDestination = "main_menu"){
        composable("main_menu") {
            val mainViewModel: MainViewModel = viewModel()

            MainMenuScreen(
                navController=navController,
                viewModel = mainViewModel
            )
        }
        //composable("quiz") { QuizScreen(navController) }
    }

}