package com.example.evidencijastudenata.data.repository

import com.example.evidencijastudenata.data.model.Student
import com.example.evidencijastudenata.data.model.StudentSubject
import com.example.evidencijastudenata.data.model.Subject
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.util.UUID

class StudentSubjectRepository {
    private val studentSubjectList = mutableListOf<StudentSubject>()
    private val db = FirebaseFirestore.getInstance()
    private val studentSubjectCollection = db.collection("student_subjects")
    private val subjectsCollection = db.collection("subjects")

    fun addStudentSubject(studentSubject: StudentSubject) {
        studentSubjectList.add(studentSubject)
    }

    suspend fun removeStudentSubject(studentId: String, subjectId: String) {
        // Query to find the student_subject documents by studentId and subjectId
        val querySnapshot = studentSubjectCollection
            .whereEqualTo("studentId", studentId)
            .whereEqualTo("subjectId", subjectId)
            .get()
            .await()

        // Delete each document found in the query
        querySnapshot.documents.forEach { documentSnapshot ->
            studentSubjectCollection.document(documentSnapshot.id).delete().await()
        }

        // Retrieve the current number of StudentSubject documents for the subject
        val remainingStudentSubjects = studentSubjectCollection
            .whereEqualTo("subjectId", subjectId)
            .get()
            .await()
        val newNumberOfStudents = remainingStudentSubjects.size()

        // Retrieve the subject document to update the number of students
        val subjectDocument = subjectsCollection.document(subjectId)
        val subject = subjectDocument.get().await().toObject(Subject::class.java)

        // Update the subject document with the new number of students
        subjectDocument.update("numberOfStudents", newNumberOfStudents).await()
    }

    fun getStudentSubjectsBySubjectId(subjectId: String): List<StudentSubject> {
        return studentSubjectList.filter { it.subjectId == subjectId }
    }


    suspend fun syncStudentSubjects(
        subjectId: String,
        selectedStudents: List<Student>
    ) {
        // Retrieve existing student-subject documents
        val existingStudents = studentSubjectCollection
            .whereEqualTo("subjectId", subjectId)
            .get()
            .await()
            .toObjects(StudentSubject::class.java)

        val existingStudentIds = existingStudents.map { it.studentId }

        // Determine students to add and remove
        val studentsToAdd = selectedStudents.filter { it.id !in existingStudentIds }
        val studentsToRemove = existingStudents.filter { it.studentId !in selectedStudents.map { student -> student.id } }

        // Add new students to the subject
        studentsToAdd.forEach { student ->
            val studentSubject = StudentSubject(
                id = UUID.randomUUID().toString(),
                studentId = student.id,
                subjectId = subjectId,
                studentName = student.name,
                studentSurname = student.surname,
                cardUid = student.cardUid,
                subjectName = ""
            )
            studentSubjectCollection.add(studentSubject).await()
        }

        // Remove students no longer associated with the subject
        studentsToRemove.forEach { studentSubject ->
            studentSubjectCollection.document(studentSubject.id).delete().await()
        }

        // Retrieve and update the subject document
        val subjectDocument = subjectsCollection.document(subjectId)
        val subject = subjectDocument.get().await().toObject(Subject::class.java)
        val newNumberOfStudents = existingStudents.size + studentsToAdd.size - studentsToRemove.size

        // Update the subject document with the new number of students
        subjectDocument.update("numberOfStudents", newNumberOfStudents).await()
    }

}
