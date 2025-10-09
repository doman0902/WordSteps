package com.example.androidapp.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.androidapp.ui.screens.*

@Composable
fun NavGraph(navController:NavHostController=rememberNavController()) {
    NavHost(navController, startDestination = "main_menu"){
        composable("main_menu") {MainMenuScreen(navController)}
        //composable("quiz") { QuizScreen(navController) }
    }

}