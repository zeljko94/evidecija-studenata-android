package com.example.evidencijastudenata.auth

import androidx.compose.runtime.*
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import com.google.firebase.auth.FirebaseAuth

@Composable
fun AuthenticationProvider(navController: NavController, content: @Composable () -> Unit) {
    val auth = FirebaseAuth.getInstance()
    val isAuthenticated by remember { derivedStateOf { auth.currentUser != null } }

    LaunchedEffect(isAuthenticated) {
        if (!isAuthenticated) {
            navController.navigate("login") {
                popUpTo(navController.graph.findStartDestination().id) { inclusive = true }
            }
        }
    }

    if (isAuthenticated) {
        content()
    }
}