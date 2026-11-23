package com.example.studentgigs

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.studentgigs.data.*
import kotlinx.coroutines.launch

class AppViewModel(private val repo: Repository) : ViewModel() {

    var user by mutableStateOf<User?>(null); private set
    var sessionChecked by mutableStateOf(false); private set
    var loading by mutableStateOf(false); private set
    var message by mutableStateOf<String?>(null); private set

    var gigs by mutableStateOf<List<Gig>>(emptyList()); private set
    var myGigs by mutableStateOf<List<Gig>>(emptyList()); private set
    var jobs by mutableStateOf<List<Job>>(emptyList()); private set
    var reviews by mutableStateOf<List<Job>>(emptyList()); private set
    var allUsers by mutableStateOf<List<User>>(emptyList()); private set
    var viewedStudent by mutableStateOf<User?>(null); private set

    var query by mutableStateOf("")
    var category by mutableStateOf("All")

    val visibleGigs: List<Gig> get() = Search.filter(gigs, query, category)

    init {
        viewModelScope.launch {
            try {
                user = repo.currentUser()
                if (user != null) refreshAll()
            } catch (e: Throwable) {
                // Ignore initial auth error
            } finally {
                sessionChecked = true
            }
        }
    }

    fun findGig(id: String) = (gigs + myGigs).firstOrNull { it.gigId == id }
    fun findJob(id: String) = jobs.firstOrNull { it.jobId == id }
    fun clearMessage() { message = null }

    private fun launchOp(block: suspend () -> Unit) = viewModelScope.launch {
        loading = true
        try { block() } finally { loading = false }
    }

    private fun fail(e: Throwable) { message = e.message ?: "Something went wrong" }

    private suspend fun refreshAll() {
        val me = repo.currentUser() ?: user ?: return
        user = me
        repo.activeGigs().onSuccess { gigs = it }.onFailure(::fail)
        when (me.role) {
            Role.STUDENT -> {
                repo.gigsOf(me.uid).onSuccess { myGigs = it }
                repo.jobsOf(me).onSuccess { jobs = it }
            }
            Role.CUSTOMER -> repo.jobsOf(me).onSuccess { jobs = it }
            Role.ADMIN -> repo.allUsers().onSuccess { allUsers = it }
        }
    }

    fun refresh() = launchOp { refreshAll() }

    // ---------- Auth ----------
    fun register(name: String, email: String, password: String, role: String) = launchOp {
        // Admin accounts can never be self-registered
        val safeRole = if (role == Role.STUDENT) Role.STUDENT else Role.CUSTOMER
        repo.register(name.trim(), email.trim(), password, safeRole)
            .onSuccess { user = it; refreshAll(); message = "Welcome to Student Gigs, ${it.name}!" }
            .onFailure(::fail)
    }

    fun login(email: String, password: String) = launchOp {
        repo.login(email.trim(), password)
            .onSuccess { user = it; refreshAll() }
            .onFailure(::fail)
    }

    fun logout() {
        repo.logout()
        user = null; gigs = emptyList(); myGigs = emptyList(); jobs = emptyList()
        reviews = emptyList(); allUsers = emptyList(); viewedStudent = null
        query = ""; category = "All"
    }

    fun saveProfile(updated: User) = launchOp {
        repo.updateProfile(updated).onSuccess { user = updated; message = "Profile updated" }.onFailure(::fail)
    }

    // ---------- Gigs ----------
    fun saveGig(gig: Gig, onDone: () -> Unit = {}) = launchOp {
        val me = user ?: return@launchOp
        if (me.role != Role.STUDENT) { message = "Only students can create gigs"; return@launchOp }
        val toSave = gig.copy(
            studentId = me.uid, studentName = me.name,
            createdAt = if (gig.createdAt == 0L) System.currentTimeMillis() else gig.createdAt
        )
        repo.saveGig(toSave).onSuccess { refreshAll(); message = "Gig saved"; onDone() }.onFailure(::fail)
    }

    fun deleteGig(gigId: String) = launchOp {
        val me = user ?: return@launchOp
        val gig = findGig(gigId)
        if (me.role != Role.ADMIN && gig?.studentId != me.uid) { message = "You can only delete your own gigs"; return@launchOp }
        repo.deleteGig(gigId).onSuccess { refreshAll(); message = "Gig removed" }.onFailure(::fail)
    }

    fun loadStudent(uid: String) = launchOp {
        viewedStudent = null
        repo.userById(uid).onSuccess { viewedStudent = it }
    }

    fun loadReviews(gigId: String) = launchOp {
        repo.reviewsFor(gigId).onSuccess { reviews = it }
    }

    // ---------- Jobs ----------
    fun requestService(gig: Gig, note: String, onDone: () -> Unit = {}) = launchOp {
        val me = user ?: return@launchOp
        if (me.role != Role.CUSTOMER) { message = "Only customers can request services"; return@launchOp }
        Validators.note(note)?.let { message = it; return@launchOp }
        if (jobs.any { it.gigId == gig.gigId && it.status in JobWorkflow.OPEN_STATUSES }) {
            message = "You already have an open request for this gig"; return@launchOp
        }
        val job = Job(
            gigId = gig.gigId, gigTitle = gig.title,
            studentId = gig.studentId, studentName = gig.studentName,
            customerId = me.uid, customerName = me.name,
            note = note.trim(), status = JobStatus.PENDING, price = gig.price
        )
        repo.createJob(job).onSuccess { refreshAll(); message = "Request sent to ${gig.studentName}"; onDone() }.onFailure(::fail)
    }

    fun changeStatus(job: Job, action: JobAction) = launchOp {
        val me = user ?: return@launchOp
        val owner = if (me.role == Role.STUDENT) job.studentId else job.customerId
        if (owner != me.uid || !JobWorkflow.canTransition(job.status, action.target, me.role)) {
            message = "You can't do that right now"; return@launchOp
        }
        repo.changeStatus(job, action.target).onSuccess { refreshAll(); message = action.doneMessage }.onFailure(::fail)
    }

    fun submitReview(job: Job, rating: Int, text: String) = launchOp {
        val me = user ?: return@launchOp
        Validators.rating(rating)?.let { message = it; return@launchOp }
        if (job.customerId != me.uid || !JobWorkflow.canReview(job, me.role)) {
            message = "This job can't be reviewed"; return@launchOp
        }
        repo.submitReview(job.jobId, rating, text.trim()).onSuccess { refreshAll(); message = "Thanks for your review!" }.onFailure(::fail)
    }

    // ---------- Admin ----------
    fun setSuspended(uid: String, suspended: Boolean) = launchOp {
        if (user?.role != Role.ADMIN) { message = "Admins only"; return@launchOp }
        repo.setSuspended(uid, suspended).onSuccess {
            refreshAll(); message = if (suspended) "User suspended" else "User reinstated"
        }.onFailure(::fail)
    }
}
