package com.example.evidencijastudenata.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.evidencijastudenata.ui.screens.AddSubjectScreen
import com.example.evidencijastudenata.ui.screens.DashboardScreen

@Composable
fun NavGraph(navController: NavHostController) {
    NavHost(navController, startDestination = "dashboard") {
        composable("dashboard") { DashboardScreen(navController) }
        composable("add_subject") { AddSubjectScreen(navController) }
        // Add other routes here
    }
}
