package com.example.studentgigs.data

import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await

interface Repository {
    val currentUid: String?
    suspend fun register(name: String, email: String, password: String, role: String): Result<User>
    suspend fun login(email: String, password: String): Result<User>
    fun logout()
    suspend fun currentUser(): User?
    suspend fun userById(uid: String): Result<User?>
    suspend fun updateProfile(user: User): Result<Unit>

    suspend fun activeGigs(): Result<List<Gig>>
    suspend fun gigsOf(studentId: String): Result<List<Gig>>
    suspend fun saveGig(gig: Gig): Result<Gig>
    suspend fun deleteGig(gigId: String): Result<Unit>

    suspend fun createJob(job: Job): Result<Job>
    suspend fun jobsOf(user: User): Result<List<Job>>
    suspend fun changeStatus(job: Job, newStatus: String): Result<Unit>
    suspend fun submitReview(jobId: String, rating: Int, review: String): Result<Unit>
    suspend fun reviewsFor(gigId: String): Result<List<Job>>

    suspend fun allUsers(): Result<List<User>>
    suspend fun setSuspended(uid: String, suspended: Boolean): Result<Unit>
}

object RepositoryProvider {
    private var instance: Repository? = null

    fun get(): Repository {
        return instance ?: synchronized(this) {
            instance ?: create().also { instance = it }
        }
    }

    private fun create(): Repository {
        return try {
            FirebaseApp.getInstance()
            FirebaseRepository()
        } catch (e: Throwable) {
            InMemoryRepository()
        }
    }
}

class FirebaseRepository(
    private val authProvider: () -> FirebaseAuth = { FirebaseAuth.getInstance() },
    private val dbProvider: () -> FirebaseFirestore = { FirebaseFirestore.getInstance() }
) : Repository {

    private val auth by lazy { authProvider() }
    private val db by lazy { dbProvider() }

    private val users get() = db.collection("users")
    private val gigs get() = db.collection("gigs")
    private val jobs get() = db.collection("jobs")

    override val currentUid: String? get() = try { auth.currentUser?.uid } catch (e: Throwable) { null }

    private inline fun <T> safe(block: () -> T): Result<T> = try {
        Result.success(block())
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(Exception(friendly(e), e))
    }

    private fun friendly(e: Exception): String = when (e) {
        is FirebaseAuthWeakPasswordException -> "Password is too weak"
        is FirebaseAuthUserCollisionException -> "This email is already registered"
        is FirebaseAuthInvalidUserException,
        is FirebaseAuthInvalidCredentialsException -> "Incorrect email or password"
        is FirebaseNetworkException -> "No internet connection. Please try again"
        else -> e.message ?: "Something went wrong"
    }

    private suspend fun fetchUser(uid: String): User? =
        users.document(uid).get().await().toObject(User::class.java)

    // ---------- Auth & profile ----------
    override suspend fun register(name: String, email: String, password: String, role: String) = safe {
        val uid = auth.createUserWithEmailAndPassword(email, password).await().user!!.uid
        val user = User(uid = uid, name = name, email = email, role = role)
        users.document(uid).set(user).await()
        user
    }

    override suspend fun login(email: String, password: String) = safe {
        val uid = auth.signInWithEmailAndPassword(email, password).await().user!!.uid
        val user = fetchUser(uid) ?: error("Profile not found. Please register again")
        if (user.suspended) {
            auth.signOut()
            error("This account has been suspended")
        }
        user
    }

    override fun logout() {
        try { auth.signOut() } catch (_: Throwable) {}
    }

    override suspend fun currentUser(): User? {
        val uid = currentUid ?: return null
        return try {
            val u = fetchUser(uid)
            if (u?.suspended == true) { logout(); null } else u
        } catch (e: CancellationException) { throw e } catch (e: Exception) { null }
    }

    override suspend fun userById(uid: String) = safe { fetchUser(uid) }

    override suspend fun updateProfile(user: User) = safe {
        users.document(user.uid).update(
            mapOf(
                "name" to user.name, "bio" to user.bio,
                "skills" to user.skills, "phone" to user.phone
            )
        ).await()
        Unit
    }

    // ---------- Gigs ----------
    override suspend fun activeGigs() = safe {
        gigs.whereEqualTo("isActive", true).get().await()
            .toObjects(Gig::class.java).sortedByDescending { it.createdAt }
    }

    override suspend fun gigsOf(studentId: String) = safe {
        gigs.whereEqualTo("studentId", studentId).get().await()
            .toObjects(Gig::class.java).sortedByDescending { it.createdAt }
    }

    override suspend fun saveGig(gig: Gig) = safe {
        val ref = if (gig.gigId.isBlank()) gigs.document() else gigs.document(gig.gigId)
        val saved = gig.copy(gigId = ref.id)
        ref.set(saved).await()
        saved
    }

    override suspend fun deleteGig(gigId: String) = safe { gigs.document(gigId).delete().await(); Unit }

    // ---------- Jobs ----------
    override suspend fun createJob(job: Job) = safe {
        val ref = jobs.document()
        val saved = job.copy(jobId = ref.id, createdAt = System.currentTimeMillis())
        ref.set(saved).await()
        saved
    }

    override suspend fun jobsOf(user: User) = safe {
        val field = if (user.role == Role.STUDENT) "studentId" else "customerId"
        jobs.whereEqualTo(field, user.uid).get().await()
            .toObjects(Job::class.java).sortedByDescending { it.createdAt }
    }

    override suspend fun changeStatus(job: Job, newStatus: String) = safe {
        val jobRef = jobs.document(job.jobId)
        if (newStatus == JobStatus.CONFIRMED) {
            db.batch().apply {
                update(jobRef, "status", newStatus)
                update(users.document(job.studentId), "completedJobs", FieldValue.increment(1))
            }.commit().await()
        } else {
            jobRef.update("status", newStatus).await()
        }
        Unit
    }

    override suspend fun submitReview(jobId: String, rating: Int, review: String) = safe {
        db.runTransaction { tx ->
            val jobRef = jobs.document(jobId)
            val job = tx.get(jobRef).toObject(Job::class.java) ?: error("Job not found")
            check(job.rating == 0) { "This job has already been reviewed" }
            val studentRef = users.document(job.studentId)
            val student = tx.get(studentRef).toObject(User::class.java) ?: error("Student not found")
            tx.update(jobRef, mapOf("rating" to rating, "review" to review))
            tx.update(
                studentRef, mapOf(
                    "rating" to RatingMath.newAverage(student.rating, student.ratingCount, rating),
                    "ratingCount" to student.ratingCount + 1
                )
            )
            null
        }.await()
        Unit
    }

    override suspend fun reviewsFor(gigId: String) = safe {
        jobs.whereEqualTo("gigId", gigId).get().await()
            .toObjects(Job::class.java).filter { it.rating > 0 }.sortedByDescending { it.createdAt }
    }

    // ---------- Admin ----------
    override suspend fun allUsers() = safe {
        users.get().await().toObjects(User::class.java).sortedBy { it.name.lowercase() }
    }

    override suspend fun setSuspended(uid: String, suspended: Boolean) = safe {
        users.document(uid).update("suspended", suspended).await(); Unit
    }
}

class InMemoryRepository : Repository {
    private val usersMap = mutableMapOf<String, User>()
    private val gigsMap = mutableMapOf<String, Gig>()
    private val jobsMap = mutableMapOf<String, Job>()
    private var currentUserId: String? = null

    init {
        val student = User(
            uid = "student_1",
            name = "Mercy Phiri",
            email = "mercy@student.mw",
            role = Role.STUDENT,
            bio = "Student offering graphic design and typing services.",
            skills = "Poster Design, Typing",
            phone = "+265999123456",
            rating = 5.0,
            ratingCount = 1,
            completedJobs = 1
        )
        usersMap[student.uid] = student

        val customer = User(
            uid = "customer_1",
            name = "John Banda",
            email = "john@example.com",
            role = Role.CUSTOMER,
            phone = "+265888987654"
        )
        usersMap[customer.uid] = customer

        val admin = User(
            uid = "admin_1",
            name = "System Admin",
            email = "admin@studentgigs.mw",
            role = Role.ADMIN
        )
        usersMap[admin.uid] = admin

        val g1 = Gig(
            gigId = "gig_1",
            studentId = student.uid,
            studentName = student.name,
            title = "Poster Design",
            category = "Poster Design",
            description = "Professional A3 posters for events and campaigns.",
            price = 5000.0,
            location = "Chichiri, Blantyre",
            isActive = true,
            createdAt = System.currentTimeMillis() - 86400000L * 2
        )
        val g2 = Gig(
            gigId = "gig_2",
            studentId = student.uid,
            studentName = student.name,
            title = "Fast Typing",
            category = "Typing",
            description = "Fast typing of assignments, reports, and dissertations.",
            price = 2000.0,
            location = "Library Block",
            isActive = true,
            createdAt = System.currentTimeMillis() - 86400000L
        )

        gigsMap[g1.gigId] = g1
        gigsMap[g2.gigId] = g2

        val j1 = Job(
            jobId = "job_1",
            gigId = g1.gigId,
            gigTitle = g1.title,
            studentId = student.uid,
            studentName = student.name,
            customerId = customer.uid,
            customerName = customer.name,
            note = "Need an A3 poster for a church event",
            status = JobStatus.CONFIRMED,
            price = g1.price,
            rating = 5,
            review = "Brilliant work",
            createdAt = System.currentTimeMillis() - 86400000L
        )
        jobsMap[j1.jobId] = j1
    }

    override val currentUid: String? get() = currentUserId

    override suspend fun register(name: String, email: String, password: String, role: String): Result<User> {
        val existing = usersMap.values.find { it.email.equals(email.trim(), ignoreCase = true) }
        if (existing != null) {
            return Result.failure(Exception("This email is already registered"))
        }
        val uid = "user_" + System.currentTimeMillis()
        val user = User(uid = uid, name = name, email = email, role = role)
        usersMap[uid] = user
        currentUserId = uid
        return Result.success(user)
    }

    override suspend fun login(email: String, password: String): Result<User> {
        val user = usersMap.values.find { it.email.equals(email.trim(), ignoreCase = true) }
            ?: return Result.failure(Exception("Incorrect email or password"))
        if (user.suspended) {
            return Result.failure(Exception("This account has been suspended"))
        }
        currentUserId = user.uid
        return Result.success(user)
    }

    override fun logout() {
        currentUserId = null
    }

    override suspend fun currentUser(): User? {
        val uid = currentUserId ?: return null
        val u = usersMap[uid]
        if (u?.suspended == true) { currentUserId = null; return null }
        return u
    }

    override suspend fun userById(uid: String): Result<User?> = Result.success(usersMap[uid])

    override suspend fun updateProfile(user: User): Result<Unit> {
        val existing = usersMap[user.uid] ?: return Result.failure(Exception("User not found"))
        val updated = existing.copy(name = user.name, bio = user.bio, skills = user.skills, phone = user.phone)
        usersMap[user.uid] = updated
        return Result.success(Unit)
    }

    override suspend fun activeGigs(): Result<List<Gig>> =
        Result.success(gigsMap.values.filter { it.isActive }.sortedByDescending { it.createdAt })

    override suspend fun gigsOf(studentId: String): Result<List<Gig>> =
        Result.success(gigsMap.values.filter { it.studentId == studentId }.sortedByDescending { it.createdAt })

    override suspend fun saveGig(gig: Gig): Result<Gig> {
        val id = if (gig.gigId.isBlank()) "gig_" + System.currentTimeMillis() else gig.gigId
        val saved = gig.copy(gigId = id, createdAt = if (gig.createdAt == 0L) System.currentTimeMillis() else gig.createdAt)
        gigsMap[id] = saved
        return Result.success(saved)
    }

    override suspend fun deleteGig(gigId: String): Result<Unit> {
        gigsMap.remove(gigId)
        return Result.success(Unit)
    }

    override suspend fun createJob(job: Job): Result<Job> {
        val id = "job_" + System.currentTimeMillis()
        val saved = job.copy(jobId = id, createdAt = System.currentTimeMillis())
        jobsMap[id] = saved
        return Result.success(saved)
    }

    override suspend fun jobsOf(user: User): Result<List<Job>> {
        val field = if (user.role == Role.STUDENT) "studentId" else "customerId"
        val list = jobsMap.values.filter { j ->
            if (field == "studentId") j.studentId == user.uid else j.customerId == user.uid
        }.sortedByDescending { it.createdAt }
        return Result.success(list)
    }

    override suspend fun changeStatus(job: Job, newStatus: String): Result<Unit> {
        val existing = jobsMap[job.jobId] ?: return Result.failure(Exception("Job not found"))
        val updated = existing.copy(status = newStatus)
        jobsMap[job.jobId] = updated
        if (newStatus == JobStatus.CONFIRMED) {
            val student = usersMap[job.studentId]
            if (student != null) {
                usersMap[job.studentId] = student.copy(completedJobs = student.completedJobs + 1)
            }
        }
        return Result.success(Unit)
    }

    override suspend fun submitReview(jobId: String, rating: Int, review: String): Result<Unit> {
        val job = jobsMap[jobId] ?: return Result.failure(Exception("Job not found"))
        if (job.rating > 0) return Result.failure(Exception("This job has already been reviewed"))
        val student = usersMap[job.studentId] ?: return Result.failure(Exception("Student not found"))

        val newAvg = RatingMath.newAverage(student.rating, student.ratingCount, rating)
        val updatedStudent = student.copy(rating = newAvg, ratingCount = student.ratingCount + 1)
        usersMap[job.studentId] = updatedStudent

        val updatedJob = job.copy(rating = rating, review = review)
        jobsMap[jobId] = updatedJob
        return Result.success(Unit)
    }

    override suspend fun reviewsFor(gigId: String): Result<List<Job>> =
        Result.success(jobsMap.values.filter { it.gigId == gigId && it.rating > 0 }.sortedByDescending { it.createdAt })

    override suspend fun allUsers(): Result<List<User>> =
        Result.success(usersMap.values.sortedBy { it.name.lowercase() })

    override suspend fun setSuspended(uid: String, suspended: Boolean): Result<Unit> {
        val u = usersMap[uid] ?: return Result.failure(Exception("User not found"))
        usersMap[uid] = u.copy(suspended = suspended)
        return Result.success(Unit)
    }
}
