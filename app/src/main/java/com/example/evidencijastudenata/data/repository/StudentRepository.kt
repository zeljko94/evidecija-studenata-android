package com.example.evidencijastudenata.data.repository

import android.util.Log
import com.example.evidencijastudenata.data.model.Student
import com.example.evidencijastudenata.data.model.StudentSubject
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.WriteBatch
import kotlinx.coroutines.tasks.await

class StudentRepository {
    private val db = FirebaseFirestore.getInstance()
    private val studentsCollection = db.collection("students")
    private val attendancesCollection = db.collection("attendances")
    private val studentSubjectsCollection = db.collection("student_subjects")

    suspend fun getStudentById(studentId: String): Student? {
        return try {
            val subjectSnapshot = studentsCollection.document(studentId).get().await()
            subjectSnapshot.toObject(Student::class.java)
        } catch (e: Exception) {
            Log.e("StudentRepository", "Error fetching student: ${e.message}")
            null
        }
    }

    suspend fun getStudentByCardUid(cardUid: String): Student? {
        return try {
            val snapshot = studentsCollection
                .whereEqualTo("cardUid", cardUid)
                .limit(1)
                .get()
                .await()

            if (snapshot.documents.isNotEmpty()) {
                val document = snapshot.documents[0]
                document.toObject(Student::class.java)
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e("StudentRepository", "Error fetching student: ${e.message}")
            null
        }
    }

    suspend fun getStudents(profesorId: String): List<Student> {
        return try {
            val snapshot = studentsCollection
                .whereEqualTo("profesorId", profesorId)
                .get()
                .await()
            snapshot.documents.mapNotNull { it.toObject(Student::class.java) }
        } catch (e: Exception) {
            Log.e("StudentRepository", "Error fetching students: ${e.message}")
            emptyList()
        }
    }


    private suspend fun getStudentIdsBySubjectId(subjectId: String): List<String> {
        return try {
            val snapshot = studentSubjectsCollection
                .whereEqualTo("subjectId", subjectId)
                .get()
                .await()
            snapshot.documents.mapNotNull { it.toObject(StudentSubject::class.java)?.studentId }
        } catch (e: Exception) {
            Log.e("StudentRepository", "Error fetching student IDs by subjectId: ${e.message}")
            emptyList()
        }
    }

    suspend fun getStudentsBySubjectId(subjectId: String): List<Student> {
        return try {
            val studentIds = getStudentIdsBySubjectId(subjectId)
            if (studentIds.isEmpty()) return emptyList()

            val snapshot = studentsCollection
                .whereIn("id", studentIds)
                .get()
                .await()
            snapshot.documents.mapNotNull { it.toObject(Student::class.java) }
        } catch (e: Exception) {
            Log.e("StudentRepository", "Error fetching students by subjectId: ${e.message}")
            emptyList()
        }
    }

    suspend fun addStudent(student: Student) {
        try {
            studentsCollection.document(student.id).set(student).await()
        } catch (e: Exception) {
            Log.e("ERROR", e.toString())
        }
    }

    suspend fun addStudents(students: List<Student>) {
        val batch: WriteBatch = db.batch()

        try {
            students.forEach { student ->
                val studentRef = studentsCollection.document(student.id)
                batch.set(studentRef, student)
            }

            batch.commit().await()
        } catch (e: Exception) {
            Log.e("ERROR", "Greška prilikom dodavanja studenata: ${e.message}", e)
        }
    }

    suspend fun deleteStudent(studentId: String) {
        try {
            // Delete related Attendances entries
            attendancesCollection.whereEqualTo("studentId", studentId).get().await().forEach { doc ->
                doc.reference.delete().await()
            }

            // Delete related StudentSubject entries
            studentSubjectsCollection.whereEqualTo("studentId", studentId).get().await().forEach { doc ->
                doc.reference.delete().await()
            }

            studentsCollection.document(studentId).delete().await()
        } catch (e: Exception) {
            Log.e("StudentRepository", "Greška prilikom brisanja studenta: ${e.message}")
        }
    }
}