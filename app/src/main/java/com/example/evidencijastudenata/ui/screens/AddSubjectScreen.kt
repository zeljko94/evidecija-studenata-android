package com.example.evidencijastudenata.ui.screens
import android.graphics.Bitmap
import android.util.Base64
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.evidencijastudenata.data.model.Student
import com.example.evidencijastudenata.data.model.Subject
import com.example.evidencijastudenata.data.repository.StudentRepository
import com.example.evidencijastudenata.data.repository.StudentSubjectRepository
import com.example.evidencijastudenata.data.repository.SubjectRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.util.*
@Composable
fun AddSubjectScreen(navController: NavController) {
    val nameState = remember { mutableStateOf("") }
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var imageBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var imageBase64 by remember { mutableStateOf<String?>(null) }
    val selectedStudents = remember { mutableStateListOf<Student>() }
    var availableStudents by remember { mutableStateOf(listOf<Student>()) }
    var filteredStudents by remember { mutableStateOf(listOf<Student>()) }
    var searchQuery by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isDropdownExpanded by remember { mutableStateOf(false) }
    var isSubmitting by remember { mutableStateOf(false) }
    val isFormValid by remember { derivedStateOf { nameState.value.isNotEmpty() } }

    val auth = FirebaseAuth.getInstance()
    val studentRepository = remember { StudentRepository() }
    val subjectRepository = remember { SubjectRepository() }
    val studentSubjectRepository = remember { StudentSubjectRepository() }
    val coroutineScope = rememberCoroutineScope()

    // Fetch students
    LaunchedEffect(Unit) {
        availableStudents = studentRepository.getStudents(auth.currentUser?.uid ?: "")
        filteredStudents = availableStudents
    }

    // File chooser launcher
    val launcher = rememberLauncherForActivityResult(contract = ActivityResultContracts.GetContent()) { uri: Uri? ->
        imageUri = uri
        uri?.let {
            val inputStream = navController.context.contentResolver.openInputStream(uri)
            imageBitmap = BitmapFactory.decodeStream(inputStream)
            imageBase64 = imageBitmap?.let { bitmap ->
                val outputStream = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
                Base64.encodeToString(outputStream.toByteArray(), Base64.DEFAULT)
            }
        }
    }

    // Function to filter students based on query
    fun filterStudents(query: String) {
        filteredStudents = if (query.isBlank()) {
            availableStudents
        } else {
            availableStudents.filter { student ->
                student.name.contains(query, ignoreCase = true) || student.surname.contains(query, ignoreCase = true)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text("Dodaj kolegij", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = nameState.value,
            onValueChange = { nameState.value = it },
            label = { Text("Naziv kolegija") },
            modifier = Modifier.fillMaxWidth(),
            isError = nameState.value.isEmpty()
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Select and preview image
        Button(onClick = { launcher.launch("image/*") }) {
            Text("Izaberi sliku")
        }

        imageBitmap?.let { bitmap ->
            Spacer(modifier = Modifier.height(16.dp))
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier.size(128.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))



        Box {
            Button(onClick = { isDropdownExpanded = true }) {
                Text("Dodaj studenta")
            }
            DropdownMenu(
                expanded = isDropdownExpanded,
                onDismissRequest = { isDropdownExpanded = false }
            ) {
                availableStudents.forEach { student ->
                    DropdownMenuItem(
                        text = { Text("${student.name} ${student.surname} - ${student.cardUid}") },
                        onClick = {
                            CoroutineScope(Dispatchers.IO).launch {
                                try {
                                    if (student !in selectedStudents) {
                                        selectedStudents.add(student)
                                    }
                                } catch (e: Exception) {
                                    Log.e("SubjectDetailScreen", "Greška prilikom dodavanja studenta: ${e.message}")
                                }
                            }
                            isDropdownExpanded = false
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Preview and remove selected students
        if (selectedStudents.isNotEmpty()) {
            Text("Odabrani studenti", style = MaterialTheme.typography.titleSmall)
            Spacer(modifier = Modifier.height(8.dp))
            selectedStudents.forEach { student ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("${student.name} ${student.surname}")
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Ukloni",
                        modifier = Modifier.clickable {
                            selectedStudents.remove(student)
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Show progress indicator when submitting
        if (isSubmitting) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .wrapContentSize(Alignment.Center)
            ) {
                CircularProgressIndicator()
            }
        } else {
            Button(
                onClick = {
                    if (isFormValid) {
                        isSubmitting = true
                        coroutineScope.launch {
                            try {
                                val newSubject = Subject(
                                    id = UUID.randomUUID().toString(),
                                    name = nameState.value,
                                    numberOfStudents = selectedStudents.size,
                                    userId = FirebaseAuth.getInstance().currentUser?.uid ?: "",
                                    imageUrl = imageBase64 ?: ""
                                )

                                // Save the subject first
                                subjectRepository.addSubject(newSubject)

                                // Sync the students with the subject
                                studentSubjectRepository.syncStudentSubjects(
                                    subjectId = newSubject.id,
                                    selectedStudents = selectedStudents
                                )

                                navController.popBackStack()
                            } catch (e: Exception) {
                                errorMessage = "Greška prilikom dodavanja kolegija: ${e.message}"
                            } finally {
                                isSubmitting = false
                            }
                        }
                    } else {
                        errorMessage = "Molimo ispravite greške prije nego što pošaljete obrazac."
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = isFormValid
            ) {
                Text("Dodaj kolegij")
            }
        }

        errorMessage?.let {
            Spacer(modifier = Modifier.height(16.dp))
            Text(it, color = MaterialTheme.colorScheme.error)
        }
    }
}