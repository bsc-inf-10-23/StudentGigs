package com.example.studentgigs.data

import com.google.firebase.firestore.PropertyName

object Role {
    const val STUDENT = "STUDENT"
    const val CUSTOMER = "CUSTOMER"
    const val ADMIN = "ADMIN"
}

object JobStatus {
    const val PENDING = "PENDING"       // customer sent request
    const val ACCEPTED = "ACCEPTED"     // student accepted
    const val DECLINED = "DECLINED"     // student declined
    const val COMPLETED = "COMPLETED"   // student marked work done
    const val CONFIRMED = "CONFIRMED"   // customer confirmed work
}

object Categories {
    val all = listOf(
        "Printing", "Photocopying", "Typing", "Tutoring",
        "Poster Design", "Laundry", "Hair & Beauty", "Other"
    )
}

/** Firestore collection: users */
data class User(
    val uid: String = "",
    val name: String = "",
    val email: String = "",
    val role: String = Role.CUSTOMER,
    val bio: String = "",
    val skills: String = "",
    val phone: String = "",
    val rating: Double = 0.0,
    val ratingCount: Int = 0,
    val completedJobs: Int = 0,
    val suspended: Boolean = false
)

/** Firestore collection: gigs */
data class Gig(
    val gigId: String = "",
    val studentId: String = "",
    val studentName: String = "",
    val title: String = "",
    val category: String = "Other",
    val description: String = "",
    val price: Double = 0.0,
    val location: String = "",
    @get:PropertyName("isActive") @set:PropertyName("isActive")
    var isActive: Boolean = true,
    val createdAt: Long = 0L
)

/** Firestore collection: jobs (a customer's request for a gig) */
data class Job(
    val jobId: String = "",
    val gigId: String = "",
    val gigTitle: String = "",
    val studentId: String = "",
    val studentName: String = "",
    val customerId: String = "",
    val customerName: String = "",
    val note: String = "",
    val status: String = JobStatus.PENDING,
    val price: Double = 0.0,
    val rating: Int = 0,
    val review: String = "",
    val createdAt: Long = 0L
)
