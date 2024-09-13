package com.example.evidencijastudenata.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.evidencijastudenata.ui.components.AuthButton
import com.example.evidencijastudenata.ui.components.AuthTextField
import com.google.firebase.auth.FirebaseAuth

@Composable
fun LoginScreen(navController: NavController) {
    val emailState = remember { mutableStateOf("test1@gmail.com") }
    val passwordState = remember { mutableStateOf("asdd1233") }
    val auth = remember { FirebaseAuth.getInstance() }
    val loginState = remember { mutableStateOf<LoginState>(LoginState.Idle) }
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center
    ) {
        AuthTextField(valueState = emailState, label = "Email")
        AuthTextField(valueState = passwordState, label = "Password", isPassword = true)
        Spacer(modifier = Modifier.height(16.dp))
        AuthButton(onClick = {
            loginState.value = LoginState.Loading
            loginWithFirebase(
                auth,
                email = emailState.value,
                password = passwordState.value,
                onSuccess = {
                    navController.navigate("dashboard")
                },
                onFailure = {
                    Toast.makeText(context, "Došlo je do pogreške", Toast.LENGTH_SHORT).show()
                    loginState.value = LoginState.Idle
                }
            )
        }, text = "Login")
        Spacer(modifier = Modifier.height(8.dp))
        TextButton(onClick = { navController.navigate("register") }) {
            Text("Don't have an account? Sign up")
        }

        when (val state = loginState.value) {
            is LoginState.Loading -> CircularProgressIndicator(modifier = Modifier.padding(top = 16.dp))
            is LoginState.Failure -> {
                Text(
                    text = "Error: ${state.errorMessage}",
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 16.dp)
                )
            }
            else -> Unit
        }
    }
}

private fun loginWithFirebase(
    auth: FirebaseAuth,
    email: String,
    password: String,
    onSuccess: () -> Unit,
    onFailure: (String) -> Unit
) {
    auth.signInWithEmailAndPassword(email, password)
        .addOnCompleteListener { task ->
            if (task.isSuccessful) {
                onSuccess()
            } else {
                onFailure(task.exception?.localizedMessage ?: "Unknown error occurred")
            }
        }
}

sealed class LoginState {
    object Idle : LoginState()
    object Loading : LoginState()
    data class Failure(val errorMessage: String) : LoginState()
}
