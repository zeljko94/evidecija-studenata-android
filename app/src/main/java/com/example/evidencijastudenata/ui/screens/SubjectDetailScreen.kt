package com.example.evidencijastudenata.ui.screens

import android.content.Context
import android.os.Environment
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.evidencijastudenata.data.model.Lecture
import com.example.evidencijastudenata.data.model.Student
import com.example.evidencijastudenata.data.repository.AttendanceRepository
import com.example.evidencijastudenata.data.repository.LectureRepository
import com.example.evidencijastudenata.data.repository.StudentRepository
import com.example.evidencijastudenata.data.repository.StudentSubjectRepository
import com.example.evidencijastudenata.data.repository.SubjectRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.apache.poi.ss.usermodel.Row
import org.apache.poi.ss.usermodel.Sheet
import org.apache.poi.ss.usermodel.Workbook
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.File
import java.io.FileOutputStream

@Composable
fun SubjectDetailScreen(navController: NavController, subjectId: String) {
    val context = LocalContext.current
    val lectureRepository = remember { LectureRepository() }
    val studentRepository = remember { StudentRepository() }
    val studentSubjectRepository = remember { StudentSubjectRepository() }
    val attendanceRepository = remember { AttendanceRepository() }
    var lectures by remember { mutableStateOf<List<Lecture>>(emptyList()) }
    var students by remember { mutableStateOf<List<Student>>(emptyList()) }
    var isLoadingLectures by remember { mutableStateOf(true) }
    var isLoadingStudents by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var lectureToDelete by remember { mutableStateOf<Lecture?>(null) }
    var showAddStudentMenu by remember { mutableStateOf(false) }

    // Load lectures and students on screen start
    LaunchedEffect(subjectId) {
        try {
            isLoadingLectures = true
            lectures = lectureRepository.getLecturesBySubject(subjectId)
        } catch (e: Exception) {
            errorMessage = "Greška prilikom učitavanja predavanja: ${e.message}"
        } finally {
            isLoadingLectures = false
        }

        try {
            isLoadingStudents = true
            students = studentRepository.getStudentsBySubjectId(subjectId)
        } catch (e: Exception) {
            errorMessage = "Greška prilikom učitavanja studenata: ${e.message}"
        } finally {
            isLoadingStudents = false
        }
    }

    // Delete lecture dialog
    if (showDeleteDialog && lectureToDelete != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Brisanje predavanja") },
            text = { Text("Jeste li sigurni da želite obrisati odabrano predavanje?") },
            confirmButton = {
                Button(onClick = {
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            lectureRepository.deleteLecture(lectureToDelete!!.id)
                            lectures = lectureRepository.getLecturesBySubject(subjectId)
                        } catch (e: Exception) {
                            Log.e("SubjectDetailScreen", "Greška prilikom brisanja predavanja: ${e.message}")
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

    // Add student menu
    var availableStudents by remember { mutableStateOf<List<Student>>(emptyList()) }
    LaunchedEffect(Unit) {
        try {
            availableStudents = studentRepository.getStudents(FirebaseAuth.getInstance().currentUser?.uid ?: "")
        } catch (e: Exception) {
            Log.e("SubjectDetailScreen", "Greška prilikom učitavanja dostupnih studenata: ${e.message}")
        }
    }

    Scaffold(
        floatingActionButton = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                contentAlignment = Alignment.BottomEnd
            ) {
                FloatingActionButton(
                    onClick = {
                        navController.navigate("add_lecture/$subjectId")
                    },
                    shape = CircleShape, // Make FAB round
                    modifier = Modifier
                        .padding(bottom = 80.dp) // Adjust padding to stack buttons
                ) {
                    Icon(imageVector = Icons.Filled.Add, contentDescription = "Dodaj predavanje")
                }
                FloatingActionButton(
                    onClick = {
                        CoroutineScope(Dispatchers.IO).launch {
                            try {
                                exportAttendancesToExcel(subjectId, students, lectures, attendanceRepository, context)
                            } catch (e: Exception) {
                                Log.e("SubjectDetailScreen", "Greška prilikom izvoza podataka: ${e.message}")
                            }
                        }
                    },
                    shape = CircleShape, // Make FAB round
                    modifier = Modifier
                        .padding(bottom = 16.dp) // Adjust padding to stack buttons
                ) {
                    Icon(imageVector = Icons.Filled.Download, contentDescription = "Izvezi u Excel")
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .padding(innerPadding)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.Top
        ) {
            // Display loading spinner if any data is loading
            if (isLoadingLectures || isLoadingStudents) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .wrapContentSize(Alignment.Center)
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(48.dp) // Adjust size here
                    )
                }
            } else if (errorMessage != null) {
                Text(errorMessage ?: "", color = MaterialTheme.colorScheme.error)
            } else {
                // Display list of students
                Text("Studenti", style = MaterialTheme.typography.headlineMedium)
                Spacer(modifier = Modifier.height(8.dp))
                if (students.isEmpty()) {
                    Text("Nema dodanih studenata", style = MaterialTheme.typography.bodyMedium)
                } else {
                    Column {
                        students.forEach { student: Student ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(text = "${student.name} ${student.surname} - ${student.cardUid}")
                                IconButton(onClick = {
                                    CoroutineScope(Dispatchers.IO).launch {
                                        try {
                                            studentSubjectRepository.removeStudentSubject(student.id, subjectId)
                                            students = studentRepository.getStudentsBySubjectId(subjectId) // Refresh student list
                                        } catch (e: Exception) {
                                            Log.e("SubjectDetailScreen", "Greška prilikom uklanjanja studenta: ${e.message}")
                                        }
                                    }
                                }) {
                                    Icon(imageVector = Icons.Filled.Close, contentDescription = "Remove Student")
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))

                // Add student dropdown menu
                Box {
                    Button(onClick = { showAddStudentMenu = true }) {
                        Text("Dodaj studenta")
                    }
                    DropdownMenu(
                        expanded = showAddStudentMenu,
                        onDismissRequest = { showAddStudentMenu = false }
                    ) {
                        availableStudents.forEach { student ->
                            DropdownMenuItem(
                                text = { Text("${student.name} ${student.surname} - ${student.cardUid}") },
                                onClick = {
                                    CoroutineScope(Dispatchers.IO).launch {
                                        try {
                                            if (student !in students) {
                                                students = students + student
                                                studentSubjectRepository.syncStudentSubjects(subjectId, students)
                                            }
                                        } catch (e: Exception) {
                                            Log.e("SubjectDetailScreen", "Greška prilikom dodavanja studenta: ${e.message}")
                                        }
                                    }
                                    showAddStudentMenu = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Text("Predavanja", style = MaterialTheme.typography.headlineMedium)
                Spacer(modifier = Modifier.height(16.dp))
                if (lectures.isEmpty()) {
                    Text("Nema dodanih predavanja", style = MaterialTheme.typography.bodyMedium)
                } else {
                    Column {
                        lectures.forEach { lecture ->
                            LectureItem(
                                lecture = lecture,
                                onDeleteClick = {
                                    lectureToDelete = lecture
                                    showDeleteDialog = true
                                },
                                onClick = { clickedLecture ->
                                    navController.navigate("lecture_detail/${clickedLecture.id}") // Navigate to LectureDetailsScreen
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LectureItem(
    lecture: Lecture,
    onDeleteClick: () -> Unit,
    onClick: (Lecture) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .combinedClickable(
                onClick = { onClick(lecture) },
                onLongClick = { onDeleteClick() }
            ),
        elevation = CardDefaults.elevatedCardElevation(4.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(text = lecture.name, style = MaterialTheme.typography.headlineMedium)
                Text(text = "Datum: ${lecture.date}", style = MaterialTheme.typography.bodyMedium)
                Text(text = "Trajanje: ${lecture.duration} minuta", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
fun exportAttendancesToExcel(
    subjectId: String,
    students: List<Student>,
    lectures: List<Lecture>,
    attendanceRepository: AttendanceRepository,
    context: Context
) {
    val workbook: Workbook = XSSFWorkbook()
    val sheet: Sheet = workbook.createSheet("Attendances")

    // Create header row with lecture names
    val headerRow: Row = sheet.createRow(0)
    headerRow.createCell(0).setCellValue("Ime")
    headerRow.createCell(1).setCellValue("Prezime")
    lectures.forEachIndexed { index, lecture ->
        headerRow.createCell(index + 2).setCellValue(lecture.name)
    }

    val attendanceMap = mutableMapOf<String, MutableSet<String>>()

    CoroutineScope(Dispatchers.IO).launch {
        try {
            // Build a map of student IDs to attended lectures
            for (student in students) {
                val attendances = attendanceRepository.getAttendancesByStudent(student.id)
                val attendedLectures = attendances.map { it.lectureId }.toMutableSet()
                attendanceMap[student.id] = attendedLectures
            }

            // Write student rows with attendance data
            students.forEachIndexed { studentIndex, student ->
                val row = sheet.createRow(studentIndex + 1)
                row.createCell(0).setCellValue(student.name)
                row.createCell(1).setCellValue(student.surname)

                lectures.forEachIndexed { lectureIndex, lecture ->
                    val attended = if (attendanceMap[student.id]?.contains(lecture.id) == true) "DA" else "NE"
                    row.createCell(lectureIndex + 2).setCellValue(attended)
                }
            }

            // Add some spacing between the two tables (e.g., 5 empty rows)
            val spaceBetweenTables = 5
            val percentageTableStartRow = students.size + 2 + spaceBetweenTables

            // Create second table header with attendance percentages
            val percentageHeaderRow = sheet.createRow(percentageTableStartRow)
            percentageHeaderRow.createCell(0).setCellValue("Ime")
            percentageHeaderRow.createCell(1).setCellValue("Prezime")
            percentageHeaderRow.createCell(2).setCellValue("Prisustvo")

            // Write percentage data
            students.forEachIndexed { studentIndex, student ->
                val row = sheet.createRow(percentageTableStartRow + studentIndex + 1)

                val attendedLecturesCount = attendanceMap[student.id]?.size ?: 0
                val totalLectures = lectures.size
                val attendancePercentage = if (totalLectures > 0) {
                    (attendedLecturesCount.toDouble() / totalLectures) * 100
                } else {
                    0.0
                }

                row.createCell(0).setCellValue(student.name)
                row.createCell(1).setCellValue(student.surname)
                row.createCell(2).setCellValue(String.format("%.2f%%", attendancePercentage))
            }

            // Save the file
            val subject = SubjectRepository().getSubjectById(subjectId)
            val path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val filename = "Prisustva_${subject?.name}_${System.currentTimeMillis()}.xlsx"
            val file = File(path, filename)
            FileOutputStream(file).use { outputStream ->
                workbook.write(outputStream)
            }
            workbook.close()

            // Show success message
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Podaci su uspješno eksportirani: ${file.path}", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Log.e("SubjectDetailScreen", "Greška prilikom izvoza podataka: ${e.message}")
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Greška prilikom izvoza podataka", Toast.LENGTH_SHORT).show()
            }
        }
    }
}

