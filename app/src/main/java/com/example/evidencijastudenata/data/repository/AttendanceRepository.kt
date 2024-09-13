package com.example.evidencijastudenata.data.repository

import android.util.Log
import com.example.evidencijastudenata.data.model.Attendance
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await



class AttendanceRepository {
    private val db = FirebaseFirestore.getInstance()
    private val attendancesCollection = db.collection("attendances")


    suspend fun getAttendancesByLecture(lectureId: String): List<Attendance> {
        return try {
            val querySnapshot = attendancesCollection
                .whereEqualTo("lectureId", lectureId)
                .get()
                .await()
            querySnapshot.documents.map { document ->
                document.toObject(Attendance::class.java)!!
            }
        } catch (e: Exception) {
            Log.e("ERROR", "Error fetching attendances: ${e.message}")
            emptyList()
        }
    }

    suspend fun getAttendancesByStudent(studentId: String): List<Attendance> {
        return try {
            val querySnapshot = attendancesCollection
                .whereEqualTo("studentId", studentId)
                .get()
                .await()
            querySnapshot.documents.map { document ->
                document.toObject(Attendance::class.java)!!
            }
        } catch (e: Exception) {
            Log.e("ERROR", "Error fetching attendances: ${e.message}")
            emptyList()
        }
    }


    suspend fun logAttendance(attendance: Attendance) {
        try {
            attendancesCollection.add(attendance).await()
        } catch (e: Exception) {
            Log.e("ERROR", "Error logging attendance: ${e.message}")
        }
    }
}
