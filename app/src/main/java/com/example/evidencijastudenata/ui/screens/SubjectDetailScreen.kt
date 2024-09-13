package com.example.evidencijastudenata.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.evidencijastudenata.data.model.Lecture
import com.example.evidencijastudenata.data.repository.LectureRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
@Composable
fun SubjectDetailScreen(navController: NavController, subjectId: String) {
    val lectureRepository = LectureRepository()
    var lectures by remember { mutableStateOf<List<Lecture>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var lectureToDelete by remember { mutableStateOf<Lecture?>(null) }

    LaunchedEffect(subjectId, navController.currentBackStackEntry) {
        try {
            isLoading = true
            lectures = lectureRepository.getLecturesBySubject(subjectId)
        } catch (e: Exception) {
            errorMessage = "Greška prilikom učitavanja predavanja: ${e.message}"
        } finally {
            isLoading = false
        }
    }

    if (showDeleteDialog && lectureToDelete != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Brisanje predavanja") },
            text = { Text("Jeste li sigurni da želite obrisati odabrano predavanje?") },
            confirmButton = {
                Button(onClick = {
                    lectureToDelete?.let {
                        CoroutineScope(Dispatchers.IO).launch {
                            lectureRepository.deleteLecture(it.id)
                            // Update lectures list
                            lectures = lectureRepository.getLecturesBySubject(subjectId)
                        }
                        showDeleteDialog = false
                    }
                }) {
                    Text("Da")
                }
            },
            dismissButton = {
                Button(onClick = { showDeleteDialog = false }) {
                    Text("Ne")
                }
            }
        )
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { navController.navigate("add_lecture/$subjectId") },
                modifier = Modifier.padding(bottom = 16.dp),
                shape = CircleShape
            ) {
                Icon(imageVector = Icons.Filled.Add, contentDescription = "Dodaj predavanje")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .padding(innerPadding)
                .verticalScroll(rememberScrollState()), // Make entire screen scrollable
            verticalArrangement = Arrangement.Top
        ) {
            if (isLoading) {
                CircularProgressIndicator()
            } else if (errorMessage != null) {
                Text(errorMessage ?: "", color = MaterialTheme.colorScheme.error)
            } else if (lectures.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "Nema dostupnih predavanja", style = MaterialTheme.typography.bodyMedium)
                }
            } else {
                Text("Predavanja", style = MaterialTheme.typography.headlineMedium)
                Spacer(modifier = Modifier.height(16.dp))
                Column {
                    lectures.forEach { lecture ->
                        LectureItem(
                            lecture = lecture,
                            onClick = {
                                navController.navigate("lecture_detail/${lecture.id}")
                            },
                            onLongPress = {
                                lectureToDelete = lecture
                                showDeleteDialog = true
                            }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LectureItem(lecture: Lecture, onClick: () -> Unit, onLongPress: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .combinedClickable(
                onClick = { onClick() },
                onLongClick = { onLongPress() }
            ),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(text = lecture.name, style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = "Datum: ${lecture.date}", style = MaterialTheme.typography.bodySmall)
                Text(text = "Trajanje: ${lecture.duration} minuta", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
