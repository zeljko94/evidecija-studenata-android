package com.example.evidencijastudenata.data.repository

import android.util.Log
import com.example.evidencijastudenata.data.model.Lecture
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class LectureRepository() {
    private val db = FirebaseFirestore.getInstance()
    private val lecturesCollection = db.collection("lectures")
    private val attendancesCollection = db.collection("attendances")


    suspend fun getLectureDetails(lectureId: String): Lecture? {
        return try {
            val lectureSnapshot = lecturesCollection.document(lectureId).get().await()
            lectureSnapshot.toObject(Lecture::class.java)
        } catch (e: Exception) {
            Log.e("LectureRepository", "Error fetching lecture details: ${e.message}")
            null
        }
    }

    suspend fun getLecturesBySubject(subjectId: String): List<Lecture> {
        return try {
            val querySnapshot = lecturesCollection
                .whereEqualTo("subjectId", subjectId)
                .get()
                .await()
            querySnapshot.documents.map { document ->
                document.toObject(Lecture::class.java)!!
            }
        } catch (e: Exception) {
            Log.e("LectureRepository", "Error fetching lectures by subject: ${e.message}")
            emptyList()
        }
    }

    suspend fun addLecture(lecture: Lecture) {
        try {
            lecturesCollection.document(lecture.id).set(lecture).await()
        } catch (e: Exception) {
            Log.e("ERROR", e.toString())
        }
    }

    suspend fun deleteLecture(lectureId: String) {
        try {
            // Delete related attendances
            attendancesCollection
                .whereEqualTo("lectureId", lectureId)
                .get()
                .await()
                .documents.forEach { document ->
                    document.reference.delete().await()
                }

            // Delete the lecture
            lecturesCollection.document(lectureId).delete().await()
        } catch (e: Exception) {
            Log.e("LectureRepository", "Error deleting lecture: ${e.message}")
        }
    }
}
