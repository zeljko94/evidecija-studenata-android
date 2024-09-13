package com.example.evidencijastudenata.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.evidencijastudenata.data.model.Lecture
import com.example.evidencijastudenata.data.repository.LectureRepository
import kotlinx.coroutines.launch
import java.util.UUID


@Composable
fun AddLectureScreen(navController: NavController, subjectId: String) {
    val lectureNameState = remember { mutableStateOf("") }
    val dateState = remember { mutableStateOf("") }
    val durationState = remember { mutableStateOf("") }
    val totalStudentsState = remember { mutableStateOf("") }

    // Validation states
    val isLectureNameValid by remember { derivedStateOf { lectureNameState.value.isNotEmpty() } }
    val isDateValid by remember { derivedStateOf { dateState.value.matches(Regex("\\d{2}\\.\\d{2}\\.\\d{4}")) } }
    val isDurationValid by remember { derivedStateOf { durationState.value.toIntOrNull() != null } }
    val isTotalStudentsValid by remember { derivedStateOf { totalStudentsState.value.toIntOrNull() != null } }
    val isFormValid by remember { derivedStateOf { isLectureNameValid && isDateValid && isDurationValid && isTotalStudentsValid } }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val lectureRepository = LectureRepository()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text("Dodaj predavanje", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = lectureNameState.value,
            onValueChange = { lectureNameState.value = it },
            label = { Text("Naziv predavanja") },
            modifier = Modifier.fillMaxWidth(),
            isError = !isLectureNameValid
        )
        if (!isLectureNameValid) {
            Text("Naziv predavanja je obavezan", color = MaterialTheme.colorScheme.error)
        }
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = dateState.value,
            onValueChange = { dateState.value = it },
            label = { Text("Datum (dd.mm.yyyy.)") },
            modifier = Modifier.fillMaxWidth(),
            isError = !isDateValid
        )
        if (!isDateValid) {
            Text("Datum mora biti u formatu dd.mm.yyyy.", color = MaterialTheme.colorScheme.error)
        }
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = durationState.value,
            onValueChange = { durationState.value = it },
            label = { Text("Trajanje (u minutama)") },
            modifier = Modifier.fillMaxWidth(),
            isError = !isDurationValid
        )
        if (!isDurationValid) {
            Text("Trajanje mora biti broj", color = MaterialTheme.colorScheme.error)
        }
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = totalStudentsState.value,
            onValueChange = { totalStudentsState.value = it },
            label = { Text("Broj studenata") },
            modifier = Modifier.fillMaxWidth(),
            isError = !isTotalStudentsValid
        )
        if (!isTotalStudentsValid) {
            Text("Broj studenata mora biti broj", color = MaterialTheme.colorScheme.error)
        }
        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                if (isFormValid) {
                    scope.launch {
                        val newLecture = Lecture(
                            id = UUID.randomUUID().toString(),
                            subjectId = subjectId,
                            name = lectureNameState.value,
                            date = dateState.value,
                            duration = durationState.value.toInt(),
                            totalStudents = totalStudentsState.value.toInt()
                        )
                        lectureRepository.addLecture(newLecture)
                        Toast.makeText(context, "Predavanje uspješno dodano", Toast.LENGTH_SHORT).show()
                        navController.popBackStack() // Go back to the previous screen
                    }
                } else {
                    errorMessage = "Molimo ispravite greške prije nego što pošaljete obrazac."
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = isFormValid
        ) {
            Text("Spremi")
        }

        errorMessage?.let {
            Spacer(modifier = Modifier.height(16.dp))
            Text(it, color = MaterialTheme.colorScheme.error)
        }
    }
}