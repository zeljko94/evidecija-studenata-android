package com.example.evidencijastudenata

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.evidencijastudenata.auth.AuthenticationProvider
import com.example.evidencijastudenata.ui.screens.*
import com.example.evidencijastudenata.ui.screens.LoginScreen
import com.example.evidencijastudenata.ui.screens.RegisterScreen
import com.google.firebase.auth.FirebaseAuth

@Composable
fun CheckPointApp() {
    val navController = rememberNavController()
    CheckPointNavHost(navController = navController)
}

@Composable
fun CheckPointNavHost(navController: NavHostController) {

    NavHost(navController = navController, startDestination = "login") {
        composable("login") { LoginScreen(navController) }
        composable("register") { RegisterScreen(navController) }
        composable("dashboard") {
            AuthenticationProvider(navController) {
                DashboardScreen(navController)
            }
        }
        composable("subject_detail/{subjectId}") { backStackEntry ->
            val subjectId = backStackEntry.arguments?.getString("subjectId")
            subjectId?.let { SubjectDetailScreen(navController, it) }
        }
        composable("lecture_detail/{lectureId}") { backStackEntry ->
            val lectureId = backStackEntry.arguments?.getString("lectureId")
            lectureId?.let { LectureDetailScreen(navController, it) }
        }
        composable("add_subject") { AddSubjectScreen(navController) }
        composable("add_lecture/{subjectId}") { backStackEntry ->
            val subjectId = backStackEntry.arguments?.getString("subjectId")
            subjectId?.let { AddLectureScreen(navController, it) }
        }


        composable("student_list") {
            StudentListScreen(navController, profesorId = FirebaseAuth.getInstance().currentUser?.uid ?: "")
        }
        composable("add_student") {
            AddStudentScreen(navController = navController, profesorId = FirebaseAuth.getInstance().currentUser?.uid ?: "")
        }
    }
}

@Preview(showBackground = true)
@Composable
fun CheckPointAppPreview() {
    CheckPointApp()
}
