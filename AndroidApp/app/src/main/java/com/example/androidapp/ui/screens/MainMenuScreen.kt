package com.example.androidapp.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController



@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainMenuScreen(
    navController: NavController,
    viewModel: MainViewModel
)
{
    val response by viewModel.apiResponse.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("WordStep")
                }
            )
        }
    ) {padding ->
        Column(modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center)
        {
            Button(
                onClick = {
                    //navController.navigate("quiz")
                    viewModel.testApiCall()
                },
                modifier = Modifier.fillMaxWidth())
            {
                //Text("Start Quiz")
                Text("Start Api Test")
            }
            Spacer(modifier = Modifier.height(32.dp))

            Text(text = "Szerver válasza:")
            Text(text = response)
        }

    }



}