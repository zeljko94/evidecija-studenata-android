package com.example.evidencijastudenata.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.evidencijastudenata.data.model.User
import com.example.evidencijastudenata.data.repository.UserRepository
import com.example.evidencijastudenata.ui.components.AuthButton
import com.example.evidencijastudenata.ui.components.AuthTextField
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

@Composable
fun RegisterScreen(navController: NavController) {
    val nameState = remember { mutableStateOf("") }
    val surnameState = remember { mutableStateOf("") }
    val emailState = remember { mutableStateOf("") }
    val passwordState = remember { mutableStateOf("") }
    val auth = remember { FirebaseAuth.getInstance() }
    val registerState = remember { mutableStateOf<RegisterState>(RegisterState.Idle) }
    val userRepository = remember { UserRepository() }
    val coroutineScope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center
    ) {
        AuthTextField(valueState = nameState, label = "Ime")
        AuthTextField(valueState = surnameState, label = "Prezime")
        AuthTextField(valueState = emailState, label = "Email")
        AuthTextField(valueState = passwordState, label = "Password", isPassword = true)
        Spacer(modifier = Modifier.height(16.dp))
        AuthButton(onClick = {
            if (emailState.value.isNotEmpty() && passwordState.value.isNotEmpty()) {
                registerState.value = RegisterState.Loading
                registerWithFirebase(
                    auth,
                    email = emailState.value,
                    password = passwordState.value,
                    onSuccess = {
                        coroutineScope.launch {
                            val userDetails = User(
                                name = nameState.value,
                                surname = surnameState.value,
                                email = emailState.value,
                                password = passwordState.value,
                                displayName = auth.currentUser?.displayName ?: "",
                                id = auth.currentUser?.uid ?: ""
                            )
                            userRepository.addUser(userDetails)
                            navController.navigate("dashboard")
                        }
                    },
                    onFailure = {
                        registerState.value = RegisterState.Failure(it)
                    }
                )
            } else {
                registerState.value = RegisterState.Failure("Email and password cannot be empty")
            }
        }, text = "Register")

        when (val state = registerState.value) {
            is RegisterState.Loading -> CircularProgressIndicator(modifier = Modifier.padding(top = 16.dp))
            is RegisterState.Failure -> {
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

private fun registerWithFirebase(
    auth: FirebaseAuth,
    email: String,
    password: String,
    onSuccess: () -> Unit,
    onFailure: (String) -> Unit
) {
    auth.createUserWithEmailAndPassword(email, password)
        .addOnCompleteListener { task ->
            if (task.isSuccessful) {
                onSuccess()
            } else {
                onFailure(task.exception?.localizedMessage ?: "Unknown error occurred")
            }
        }
}

sealed class RegisterState {
    object Idle : RegisterState()
    object Loading : RegisterState()
    data class Failure(val errorMessage: String) : RegisterState()
}
