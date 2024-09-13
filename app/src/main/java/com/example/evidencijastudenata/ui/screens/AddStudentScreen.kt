package com.example.evidencijastudenata.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.evidencijastudenata.data.model.Student
import com.example.evidencijastudenata.data.repository.StudentRepository
import kotlinx.coroutines.launch


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddStudentScreen(navController: NavController, profesorId: String) {
    // State for input fields
    val nameState = remember { mutableStateOf("") }
    val surnameState = remember { mutableStateOf("") }
    val cardUidState = remember { mutableStateOf("") }

    // Validation states
    val isNameValid by remember { derivedStateOf { nameState.value.isNotEmpty() } }
    val isSurnameValid by remember { derivedStateOf { surnameState.value.isNotEmpty() } }
    val isCardUidValid by remember { derivedStateOf { cardUidState.value.isNotEmpty() } }
    val isFormValid by remember { derivedStateOf { isNameValid && isSurnameValid && isCardUidValid } }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val studentRepository = remember { StudentRepository() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Dodaj studenta") })
        },
        content = { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Text("Dodaj novog studenta", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = nameState.value,
                    onValueChange = { nameState.value = it },
                    label = { Text("Ime") },
                    modifier = Modifier.fillMaxWidth(),
                    isError = !isNameValid
                )
                if (!isNameValid) {
                    Text("Ime je obavezno", color = MaterialTheme.colorScheme.error)
                }
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = surnameState.value,
                    onValueChange = { surnameState.value = it },
                    label = { Text("Prezime") },
                    modifier = Modifier.fillMaxWidth(),
                    isError = !isSurnameValid
                )
                if (!isSurnameValid) {
                    Text("Prezime je obavezno", color = MaterialTheme.colorScheme.error)
                }
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = cardUidState.value,
                    onValueChange = { cardUidState.value = it },
                    label = { Text("Card UID") },
                    modifier = Modifier.fillMaxWidth(),
                    isError = !isCardUidValid
                )
                if (!isCardUidValid) {
                    Text("Card UID je obavezan", color = MaterialTheme.colorScheme.error)
                }
                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        if (isFormValid) {
                            scope.launch {
                                var existingStudent = studentRepository.getStudentByCardUid(cardUidState.value)
                                if(existingStudent != null) {
                                    Toast.makeText(context, "Uneseni broj iksice već postoji u sustavu", Toast.LENGTH_SHORT).show()
                                }
                                else {
                                    val student = Student(
                                        id = cardUidState.value,
                                        cardUid = cardUidState.value,
                                        name = nameState.value,
                                        surname = surnameState.value,
                                        profesorId = profesorId
                                    )
                                    studentRepository.addStudent(student)
                                    Toast.makeText(context, "Student uspješno dodan", Toast.LENGTH_SHORT).show()
                                    navController.popBackStack()
                                }
                            }
                        } else {
                            errorMessage = "Molimo ispravite greške prije nego što pošaljete obrazac."
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = isFormValid
                ) {
                    Text("Dodaj studenta")
                }

                errorMessage?.let {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(it, color = MaterialTheme.colorScheme.error)
                }
            }
        }
    )
}