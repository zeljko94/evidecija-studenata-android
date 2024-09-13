package com.example.evidencijastudenata.data.model

data class Lecture(
    val id: String = "",
    val subjectId: String = "",
    val name: String = "",
    val date: String = "",
    val duration: Int = 0,
    val studentsCheckedIn: Int = 0,
    val totalStudents: Int = 0
) {
    // Helper method to calculate the attendance percentage
    val attendancePercentage: Int
        get() = if (totalStudents > 0) (studentsCheckedIn * 100) / totalStudents else 0
}
