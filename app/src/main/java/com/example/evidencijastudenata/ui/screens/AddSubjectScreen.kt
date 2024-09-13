package com.example.evidencijastudenata.ui.screens
import android.graphics.Bitmap
import android.util.Base64
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.evidencijastudenata.data.model.Subject
import com.example.evidencijastudenata.data.repository.SubjectRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.util.*

@Composable
fun AddSubjectScreen(navController: NavController) {
    val nameState = remember { mutableStateOf("") }
    val numberOfStudentsState = remember { mutableStateOf("") }
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var imageBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var imageBase64 by remember { mutableStateOf<String?>(null) }

    // Validation states
    val isNameValid by remember { derivedStateOf { nameState.value.isNotEmpty() } }
    val isNumberOfStudentsValid by remember {
        derivedStateOf {
            numberOfStudentsState.value.toIntOrNull() != null
        }
    }
    val isFormValid by remember { derivedStateOf { isNameValid && isNumberOfStudentsValid } }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val subjectRepository = remember { SubjectRepository() }
    val coroutineScope = rememberCoroutineScope()

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
            isError = !isNameValid
        )
        if (!isNameValid) {
            Text("Naziv kolegija je obavezan", color = MaterialTheme.colorScheme.error)
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = numberOfStudentsState.value,
            onValueChange = { numberOfStudentsState.value = it },
            label = { Text("Broj studenata") },
            modifier = Modifier.fillMaxWidth(),
            isError = !isNumberOfStudentsValid
        )
        if (!isNumberOfStudentsValid) {
            Text("Broj studenata mora biti broj", color = MaterialTheme.colorScheme.error)
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = { launcher.launch("image/*") }) {
            Text("Izaberi sliku")
        }

        // Preview the selected image
        imageBitmap?.let { bitmap ->
            Spacer(modifier = Modifier.height(16.dp))
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier.size(128.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = {
                if (isFormValid) {
                    coroutineScope.launch {
                        val newSubject = Subject(
                            id = UUID.randomUUID().toString(),
                            name = nameState.value,
                            numberOfStudents = numberOfStudentsState.value.toIntOrNull() ?: 0,
                            userId = FirebaseAuth.getInstance().currentUser?.uid ?: "",
                            imageUrl = imageBase64 ?: ""
                        )
                        subjectRepository.addSubject(newSubject)
                        navController.popBackStack()
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

        errorMessage?.let {
            Spacer(modifier = Modifier.height(16.dp))
            Text(it, color = MaterialTheme.colorScheme.error)
        }
    }
}