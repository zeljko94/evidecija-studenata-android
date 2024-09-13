package com.example.evidencijastudenata.data.repository

import android.util.Log
import com.example.evidencijastudenata.data.model.Lecture
import com.example.evidencijastudenata.data.model.User
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class UserRepository {
    private val db = FirebaseFirestore.getInstance()
    private val usersCollection = db.collection("users")

    suspend fun getUserById(userId: String): User? {
        return try {
            val snapshot = usersCollection.document(userId).get().await()
            snapshot.toObject(User::class.java)
        } catch (e: Exception) {
            Log.e("UserRepository", "Error fetching user by id: ${e.message}");
            null
        }
    }

    suspend fun addUser(user: User) {
        try {
            usersCollection.document(user.id).set(user).await()
        } catch (e: Exception) {
            Log.e("ERROR", e.toString())
        }
    }
}
