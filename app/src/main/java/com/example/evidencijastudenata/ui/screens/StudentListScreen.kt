package com.example.evidencijastudenata.ui.screens

import android.Manifest
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.evidencijastudenata.data.model.Student
import com.example.evidencijastudenata.data.repository.StudentRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun StudentListScreen(navController: NavController, profesorId: String) {
    var students by remember { mutableStateOf<List<Student>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var studentToDelete by remember { mutableStateOf<Student?>(null) }
    var selectedCsvUri by remember { mutableStateOf<Uri?>(null) }
    val studentRepository = remember { StudentRepository() }

    LaunchedEffect(Unit) {
        isLoading = true
        try {
            students = studentRepository.getStudents(profesorId)
        } finally {
            isLoading = false
        }
    }

    if (showDeleteDialog && studentToDelete != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Brisanje studenta") },
            text = { Text("Jeste li sigurni da želite obrisati ovog studenta?") },
            confirmButton = {
                Button(onClick = {
                    isLoading = true
                    CoroutineScope(Dispatchers.IO).launch {
                        studentRepository.deleteStudent(studentToDelete!!.id)
                        students = studentRepository.getStudents(profesorId)
                        withContext(Dispatchers.Main) {
                            showDeleteDialog = false
                            isLoading = false
                        }
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

    val context = LocalContext.current
    val exportPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) {
            isLoading = true
            exportStudentsToExcel(context, students) {
                isLoading = false
            }
        } else {
            Toast.makeText(context, "Permission denied. Cannot export file.", Toast.LENGTH_SHORT).show()
        }
    }

    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            selectedCsvUri = it
            importCSV(context, it)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Studenti") }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { navController.navigate("add_student") },
                shape = CircleShape,
                modifier = Modifier.padding(end = 72.dp) // Adjust for position
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Dodaj studenta")
            }
        },
        content = { paddingValues ->
            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .verticalScroll(rememberScrollState()) // Make the whole screen scrollable
                ) {
                    if (students.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Nema dodanih studenata", style = MaterialTheme.typography.bodyMedium)
                        }
                    } else {
                        students.forEach { student ->
                            StudentCard(
                                student = student,
                                onLongPress = {
                                    studentToDelete = student
                                    showDeleteDialog = true
                                }
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }
            }
        },
        floatingActionButtonPosition = FabPosition.End
    )

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.BottomEnd
    ) {
        FloatingActionButton(
            onClick = {
                exportPermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            },
            shape = CircleShape,
            modifier = Modifier
                .padding(end = 16.dp, bottom = 80.dp)
                .align(Alignment.BottomEnd)
        ) {
            Icon(Icons.Filled.Download, contentDescription = "Export u Excel")
        }
        Spacer(modifier = Modifier.height(56.dp))
        FloatingActionButton(
            onClick = {
                importLauncher.launch("text/csv")
            },
            shape = CircleShape,
            modifier = Modifier
                .padding(end = 16.dp, bottom = 16.dp) // Adjust position
                .align(Alignment.BottomEnd)
        ) {
            Icon(Icons.Filled.Upload, contentDescription = "Uvoz iz CSV-a")
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun StudentCard(student: Student, onLongPress: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = { /* Handle student click if needed */ },
                onLongClick = { onLongPress() }
            )
            .padding(8.dp)
    ) {
        Row(modifier = Modifier.padding(16.dp)) {
            Column {
                Text(text = "${student.name} ${student.surname}", style = MaterialTheme.typography.titleMedium)
                Text(text = "Broj iksice: ${student.cardUid}", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

fun exportStudentsToExcel(context: Context, students: List<Student>, onCompletion: () -> Unit) {
    CoroutineScope(Dispatchers.IO).launch {
        try {
            val workbook = XSSFWorkbook()
            val sheet = workbook.createSheet("Studenti")

            val headerRow = sheet.createRow(0)
            headerRow.createCell(0).setCellValue("#")
            headerRow.createCell(1).setCellValue("Ime")
            headerRow.createCell(2).setCellValue("Prezime")
            headerRow.createCell(3).setCellValue("Broj iksice")

            students.forEachIndexed { index, student ->
                val row = sheet.createRow(index + 1)
                row.createCell(0).setCellValue((index + 1).toDouble())
                row.createCell(1).setCellValue(student.name)
                row.createCell(2).setCellValue(student.surname)
                row.createCell(3).setCellValue(student.cardUid)
            }

            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val fileName = "Studenti_${System.currentTimeMillis()}.xlsx"
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


fun importCSV(context: Context, uri: Uri) {
    CoroutineScope(Dispatchers.IO).launch {
        try {
            val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
            val reader = inputStream?.bufferedReader()
            val csvData = reader?.readLines()
            inputStream?.close()

            csvData?.let {
                val students = mutableListOf<Student>()
                val header = it.first().split(",")
                val rows = it.drop(1)

                rows.forEach { row ->
                    val columns = row.split(",")
                    if (columns.size == header.size) {
                        val student = Student(
                            id = columns[0].trim(),
                            name = columns[1].trim(),
                            surname = columns[2].trim(),
                            cardUid = columns[0].trim()
                        )
                        students.add(student)
                    }
                }

                withContext(Dispatchers.Main) {
                    val studentRepo = StudentRepository()
                    studentRepo.addStudents(students)
                    Toast.makeText(context, "Uvoz CSV-a uspješan", Toast.LENGTH_LONG).show()
                }
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Greška pri uvozu CSV-a: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
}
