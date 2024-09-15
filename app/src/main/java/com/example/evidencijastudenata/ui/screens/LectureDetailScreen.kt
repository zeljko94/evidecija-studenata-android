package com.example.evidencijastudenata.ui.screens

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.os.Environment
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.evidencijastudenata.data.model.Attendance
import com.example.evidencijastudenata.data.model.Student
import com.example.evidencijastudenata.data.repository.AttendanceRepository
import com.example.evidencijastudenata.data.repository.LectureRepository
import com.example.evidencijastudenata.data.repository.StudentRepository
import com.example.evidencijastudenata.data.repository.UserRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.*

// UUID for Bluetooth communication (standard SPP UUID)
private val MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

@Composable
fun LectureDetailScreen(navController: NavController, lectureId: String) {
    var lectureName by remember { mutableStateOf<String?>(null) }
    var attendanceData by remember { mutableStateOf<List<Attendance>>(emptyList()) }
    var totalStudents by remember { mutableStateOf(0) }
    var cardUid by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var attendanceToDelete by remember { mutableStateOf<Attendance?>(null) }

    val coroutineScope = rememberCoroutineScope()
    val lectureRepository = remember { LectureRepository() }
    val attendanceRepository = remember { AttendanceRepository() }
    val studentRepository = remember { StudentRepository() }
    val context = LocalContext.current

    // Bluetooth setup
    val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
    var bluetoothSocket by remember { mutableStateOf<BluetoothSocket?>(null) }
    var receivedUID by remember { mutableStateOf("") }

    fun logAttendance(cardUid: String) {
        coroutineScope.launch {
            if (cardUid.isNotEmpty()) {
                try {
                    val student = studentRepository.getStudentByCardUid(cardUid)
                    if (student != null) {
                        val currentTimestamp =
                            SimpleDateFormat("dd.MM.yyyy. HH:mm:ss", Locale.getDefault()).format(
                                Date()
                            )

                        val attendance = Attendance(
                            id = UUID.randomUUID().toString(),
                            studentId = cardUid,
                            userName = student.name,
                            userSurname = student.surname,
                            lectureId = lectureId,
                            timestamp = currentTimestamp,
                            lectureName = lectureName ?: ""
                        )
                        attendanceRepository.logAttendance(attendance)
                        Log.d("ATTENDANCE ADDED", attendance.toString())

                        Toast.makeText(context, "Dolazak zabilježen: $cardUid", Toast.LENGTH_SHORT)
                            .show()

                        attendanceData = attendanceRepository.getAttendancesByLecture(lectureId)
                            .sortedByDescending { it.timestamp }
                    } else {
                        Toast.makeText(
                            context,
                            "Student nije pronađen u bazi: $cardUid",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } catch (e: Exception) {
                    Log.d("ERROR", e.toString())
                    Toast.makeText(context, "Došlo je do pogreške!", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    fun listenForUID(socket: BluetoothSocket) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val inputStream: InputStream = socket.inputStream
                val buffer = ByteArray(1024)
                var bytes: Int

                while (true) {
                    bytes = inputStream.read(buffer)
                    val incomingMessage = String(buffer, 0, bytes)
                    receivedUID = incomingMessage.trim()

                    logAttendance(receivedUID)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun connectToBluetoothDevice(deviceName: String) {
        bluetoothAdapter?.bondedDevices?.find { it.name == deviceName }?.let { device ->
            coroutineScope.launch(Dispatchers.IO) {
                try {
                    bluetoothSocket = device.createRfcommSocketToServiceRecord(MY_UUID)
                    bluetoothSocket?.connect()
                    listenForUID(bluetoothSocket!!)
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            context,
                            "Povezivanje s bluetooth uređajem nije uspjelo",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    e.printStackTrace()
                }
            }
        }
    }

    LaunchedEffect(lectureId) {
        connectToBluetoothDevice("ESP32_NFC")

        isLoading = true
        try {
            val lecture = lectureRepository.getLectureDetails(lectureId)
            lectureName = lecture?.name ?: "Unknown"
            attendanceData = attendanceRepository.getAttendancesByLecture(lectureId)
                .sortedByDescending { it.timestamp }
            Log.d("FETCH ATTENDANCE ON LAUNCH", attendanceData.toString())
            totalStudents = lecture?.totalStudents ?: 0
        } finally {
            isLoading = false
        }
    }

    fun exportAttendanceToExcel(
        context: Context,
        attendances: List<Attendance>,
        onCompletion: () -> Unit
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val workbook = XSSFWorkbook()
                val sheet = workbook.createSheet("Prisutnost")

                val headerRow = sheet.createRow(0)
                headerRow.createCell(0).setCellValue("Redni broj")
                headerRow.createCell(1).setCellValue("Ime")
                headerRow.createCell(2).setCellValue("Prezime")
                headerRow.createCell(3).setCellValue("Email")
                headerRow.createCell(4).setCellValue("Datum")

                attendances.forEachIndexed { index, attendance ->
                    val row = sheet.createRow(index + 1)
                    row.createCell(0).setCellValue((index + 1).toDouble())
                    row.createCell(1).setCellValue(attendance.userName)
                    row.createCell(2).setCellValue(attendance.userSurname)
                    row.createCell(3).setCellValue(attendance.userEmail)
                    row.createCell(4).setCellValue(attendance.timestamp)
                }

                val downloadsDir =
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val fileName = "Prisutnost_${System.currentTimeMillis()}.xlsx"
                val file = File(downloadsDir, fileName)
                val outputStream = FileOutputStream(file)
                workbook.write(outputStream)
                outputStream.close()
                workbook.close()

                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        context,
                        "Dokument exportiran: ${file.absolutePath}",
                        Toast.LENGTH_LONG
                    ).show()
                }

            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        context,
                        "Greška prilikom exportiranja podataka u Excel",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } finally {
                withContext(Dispatchers.Main) {
                    onCompletion()
                }
            }
        }
    }

    // Permission launcher to request WRITE_EXTERNAL_STORAGE permission
    val permissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                isLoading = true
                exportAttendanceToExcel(context, attendanceData) {
                    isLoading = false
                }
            } else {
                Toast.makeText(
                    context,
                    "Permission denied. Cannot export file.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    permissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                },
                modifier = Modifier.padding(16.dp),
                shape = CircleShape
            ) {
                Icon(imageVector = Icons.Filled.Download, contentDescription = "Export u Excel")
            }
        }
    ) { paddingValues ->
        LazyColumn(
            contentPadding = paddingValues,
            verticalArrangement = Arrangement.spacedBy(8.dp) // Reduced spacing between items
        ) {
            item {
                OutlinedTextField(
                    value = cardUid,
                    onValueChange = { cardUid = it },
                    label = { Text("Unesite ID studenta") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp) // Adjusted padding
                )
            }

            item {
                Button(
                    onClick = {
                        if (cardUid.isNotEmpty()) {
                            logAttendance(cardUid)
                            cardUid = ""
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp) // Adjusted padding
                ) {
                    Text("Log Attendance")
                }
            }

            item {
                val attendancePercentage = if (totalStudents > 0) {
                    (attendanceData.size.toFloat() / totalStudents) * 100
                } else {
                    0f
                }
                Text(
                    text = "Procenat prisutnosti: ${"%.2f".format(attendancePercentage)}%",
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            item {
                attendanceData.forEach { attendance ->
                    AttendanceCard(
                        attendance = attendance,
                        onLongPress = {
                            attendanceToDelete = attendance
                            showDeleteDialog = true
                        }
                    )
                }
            }

        }

        // Dialog for confirming attendance deletion
        if (showDeleteDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                title = { Text("Brisanje prisutnosti") },
                text = { Text("Jeste li sigurni da želite izbrisati ovu prisutnost?") },
                confirmButton = {
                    Button(
                        onClick = {
                            isLoading = true
                            CoroutineScope(Dispatchers.IO).launch {
                                attendanceRepository.deleteAttendance(attendanceToDelete!!.id)
                                attendanceData =
                                    attendanceRepository.getAttendancesByLecture(lectureId)
                                        .sortedByDescending { it.timestamp }
                                withContext(Dispatchers.Main) {
                                    showDeleteDialog = false
                                    isLoading = false
                                    Toast.makeText(
                                        context,
                                        "Prisutnost izbrisana",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        }
                    ) {
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
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AttendanceCard(attendance: Attendance, onLongPress: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = { /* Handle student click if needed */ },
                onLongClick = { onLongPress() }
            )
            .padding(4.dp),
        elevation = CardDefaults.elevatedCardElevation(4.dp),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "${attendance.userName} ${attendance.userSurname}",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Vrijeme dolaska: ${attendance.timestamp}",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}
