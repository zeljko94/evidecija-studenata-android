package com.example.evidencijastudenata.data.repository

import android.util.Log
import com.example.evidencijastudenata.data.model.Lecture
import com.example.evidencijastudenata.data.model.Subject
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class SubjectRepository {
    private val db = FirebaseFirestore.getInstance()
    private val subjectsCollection = db.collection("subjects")
    private val lecturesCollection = db.collection("lectures")
    private val attendancesCollection = db.collection("attendances")
    private val studentSubjectCollection = db.collection("student_subjects")

    suspend fun getSubjectById(subjectId: String): Subject? {
        return try {
            val subjectSnapshot = subjectsCollection.document(subjectId).get().await()
            subjectSnapshot.toObject(Subject::class.java)
        } catch (e: Exception) {
            Log.e("SubjectRepository", "Error fetching subject details: ${e.message}")
            null
        }
    }

    suspend fun getSubjects(userId: String): List<Subject> {
        return try {
            val snapshot = subjectsCollection
                .whereEqualTo("userId", userId)
                .get()
                .await()
            snapshot.documents.mapNotNull { it.toObject(Subject::class.java) }
        } catch (e: Exception) {
            Log.e("SubjectRepository", "Error fetching subjects: ${e.message}")
            emptyList()
        }
    }

    suspend fun addSubject(subject: Subject) {
        try {
            subjectsCollection.document(subject.id).set(subject).await()
        } catch (e: Exception) {
            Log.e("ERROR", e.toString())
        }
    }

    suspend fun deleteSubject(subjectId: String) {
        try {
            // Delete related lectures
            lecturesCollection.whereEqualTo("subjectId", subjectId).get().await().forEach { doc ->
                doc.reference.delete().await()
            }

            // Delete related attendances
            attendancesCollection.whereEqualTo("subjectId", subjectId).get().await().forEach { doc ->
                doc.reference.delete().await()
            }

            // Delete related StudentSubject entries
            studentSubjectCollection.whereEqualTo("subjectId", subjectId).get().await().forEach { doc ->
                doc.reference.delete().await()
            }

            // Delete the subject
            subjectsCollection.document(subjectId).delete().await()
        } catch (e: Exception) {
            Log.e("SubjectRepository", "Greška prilikom brisanja kolegija: ${e.message}")
        }
    }
}
