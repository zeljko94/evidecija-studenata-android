package com.example.evidencijastudenata.ui.screens

import android.Manifest
import android.content.Context
import android.graphics.BitmapFactory
import android.os.Environment
import android.util.Base64
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import com.example.evidencijastudenata.R
import com.example.evidencijastudenata.data.model.Subject
import com.example.evidencijastudenata.data.repository.SubjectRepository
import com.example.evidencijastudenata.data.repository.UserRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.File
import java.io.FileOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(navController: NavController) {
    var subjects by remember { mutableStateOf<List<Subject>>(emptyList()) }
    var currentUserDisplayName by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var subjectToDelete by remember { mutableStateOf<Subject?>(null) }
    val auth = FirebaseAuth.getInstance()
    val subjectRepository = remember { SubjectRepository() }
    val userRepository = remember { UserRepository() }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        isLoading = true
        try {
            subjects = subjectRepository.getSubjects(auth.currentUser?.uid ?: "")
            val currentUser = userRepository.getUserById(auth.currentUser?.uid ?: "")
            currentUserDisplayName = "${currentUser?.name} ${currentUser?.surname}"
        } finally {
            isLoading = false
        }
    }

    if (showDeleteDialog && subjectToDelete != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Brisanje kolegija") },
            text = { Text("Jeste li sigurni da želite obrisati odabrani kolegij?") },
            confirmButton = {
                Button(onClick = {
                    isLoading = true
                    coroutineScope.launch {
                        subjectToDelete?.let {
                            subjectRepository.deleteSubject(it.id)
                            // Update subjects list
                            subjects = subjectRepository.getSubjects(auth.currentUser?.uid ?: "")
                        }
                        isLoading = false
                        showDeleteDialog = false
                    }
                }) {
                    Text("Da")
                }
            },
            dismissButton = {
                Button(onClick = { showDeleteDialog = false }) {
                    Text("No")
                }
            }
        )
    }

    val context = LocalContext.current
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) {
            isLoading = true
            coroutineScope.launch {
                exportSubjectsToExcel(context, subjects) {
                    isLoading = false
                }
            }
        } else {
            Toast.makeText(context, "Permission denied. Cannot export file.", Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("") },
                actions = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.AccountCircle,
                            contentDescription = "User Icon",
                            modifier = Modifier
                                .size(40.dp)
                                .padding(8.dp)
                        )
                        Text(text = currentUserDisplayName, modifier = Modifier.padding(end = 16.dp))
                        IconButton(onClick = {
                            auth.signOut()
                            navController.navigate("login") {
                                popUpTo(navController.graph.findStartDestination().id) { inclusive = true }
                            }
                        }) {
                            Icon(Icons.Filled.ExitToApp, contentDescription = "Logout")
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            Column {
                FloatingActionButton(
                    onClick = { navController.navigate("add_subject") },
                    modifier = Modifier.padding(bottom = 16.dp),
                    shape = CircleShape
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "Dodaj kolegij")
                }
                FloatingActionButton(
                    onClick = {
                        // Request permission before exporting
                        permissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    },
                    modifier = Modifier.padding(bottom = 16.dp),
                    shape = CircleShape
                ) {
                    Icon(Icons.Filled.Download, contentDescription = "Export u Excel")
                }
                FloatingActionButton(
                    onClick = { navController.navigate("student_list") }, // Navigate to the Student List screen
                    shape = CircleShape
                ) {
                    Icon(Icons.Filled.Person, contentDescription = "Studenti")
                }
            }
        },
        content = { paddingValues ->
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.Top
                ) {
                    if (subjects.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = "Nema dodanih kolegija", style = MaterialTheme.typography.bodyMedium)
                        }
                    } else {
                        Text("Kolegiji", style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(16.dp))
                        LazyColumn {
                            items(subjects) { subject ->
                                SubjectCard(subject = subject, onClick = {
                                    navController.navigate("subject_detail/${subject.id}")
                                }, onLongPress = {
                                    subjectToDelete = subject
                                    showDeleteDialog = true
                                })
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                        }
                    }
                }
            }
        }
    )
}

fun exportSubjectsToExcel(context: Context, subjects: List<Subject>, onCompletion: () -> Unit) {
    CoroutineScope(Dispatchers.IO).launch {
        try {
            val workbook = XSSFWorkbook()
            val sheet = workbook.createSheet("Kolegiji")

            val headerRow = sheet.createRow(0)
            headerRow.createCell(0).setCellValue("#")
            headerRow.createCell(1).setCellValue("Naziv kolegija")
            headerRow.createCell(2).setCellValue("Broj studenata")

            subjects.forEachIndexed { index, subject ->
                val row = sheet.createRow(index + 1)
                row.createCell(0).setCellValue((index + 1).toDouble())
                row.createCell(1).setCellValue(subject.name)
                row.createCell(2).setCellValue(subject.numberOfStudents.toDouble())
            }

            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val fileName = "Kolegiji_${System.currentTimeMillis()}.xlsx"
            val file = File(downloadsDir, fileName)
            val outputStream = FileOutputStream(file)
            workbook.write(outputStream)
            outputStream.close()
            workbook.close()

            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Dokument exportiran: ${file.absolutePath}", Toast.LENGTH_LONG).show()
                onCompletion()
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Greška pri izvozu dokumenta: ${e.message}", Toast.LENGTH_LONG).show()
                onCompletion()
            }
        }
    }
}




@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SubjectCard(subject: Subject, onClick: () -> Unit, onLongPress: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .combinedClickable(
                onClick = { onClick() },
                onLongClick = { onLongPress() }
            )
            .padding(8.dp),
    ) {
        Row(modifier = Modifier.padding(16.dp)) {
            val imageBitmap = remember {
                if (subject.imageUrl.isNotEmpty()) {
                    try {
                        val imageBytes = Base64.decode(subject.imageUrl, Base64.DEFAULT)
                        BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)?.asImageBitmap()
                    } catch (e: Exception) {
                        null
                    }
                } else {
                    null
                }
            }

            Image(
                bitmap = imageBitmap ?: ImageBitmap.imageResource(id = R.drawable.placeholder_image), // Use a placeholder image if null
                contentDescription = "Subject Image",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(64.dp)
                    .padding(end = 16.dp)
            )
            Column {
                Text(text = subject.name, style = MaterialTheme.typography.headlineLarge)
                Text(text = "Broj studenata: ${subject.numberOfStudents}")
            }
        }
    }
}


