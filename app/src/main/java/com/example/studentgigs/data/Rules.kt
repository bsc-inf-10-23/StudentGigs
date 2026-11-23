package com.example.studentgigs.data

/** Pure business rules (no Android dependencies, so they are easy to unit test). */

object Validators {
    private val EMAIL = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
    private val PHONE = Regex("^\\+?[0-9]{7,15}$")

    fun email(v: String): String? = when {
        v.isBlank() -> "Email is required"
        !EMAIL.matches(v.trim()) -> "Enter a valid email address"
        else -> null
    }

    fun password(v: String): String? = when {
        v.isEmpty() -> "Password is required"
        v.length < 6 -> "Password must be at least 6 characters"
        else -> null
    }

    fun name(v: String): String? = when {
        v.isBlank() -> "Name is required"
        v.trim().length < 2 -> "Name is too short"
        else -> null
    }

    fun phone(v: String): String? =
        if (v.isBlank() || PHONE.matches(v.replace(Regex("[\\s\\-\\(\\)]"), ""))) null else "Enter a valid phone number"

    fun title(v: String): String? = when {
        v.isBlank() -> "Title is required"
        v.trim().length > 60 -> "Title must be 60 characters or less"
        else -> null
    }

    fun description(v: String): String? = when {
        v.isBlank() -> "Description is required"
        v.trim().length < 10 -> "Describe the service in at least 10 characters"
        else -> null
    }

    fun price(v: String): String? {
        val p = v.trim().toDoubleOrNull() ?: return "Enter a valid price"
        return if (p <= 0.0) "Price must be greater than zero" else null
    }

    fun note(v: String): String? = when {
        v.isBlank() -> "Add a short note for the student"
        v.trim().length > 200 -> "Keep the note under 200 characters"
        else -> null
    }

    fun rating(v: Int): String? = if (v in 1..5) null else "Choose a rating from 1 to 5 stars"
}

enum class JobAction(val label: String, val target: String, val doneMessage: String) {
    ACCEPT("Accept job", JobStatus.ACCEPTED, "Job accepted"),
    DECLINE("Decline", JobStatus.DECLINED, "Job declined"),
    COMPLETE("Mark as completed", JobStatus.COMPLETED, "Marked as completed"),
    CONFIRM("Confirm completed work", JobStatus.CONFIRMED, "Work confirmed. Please leave a review")
}

object JobWorkflow {
    val OPEN_STATUSES = listOf(JobStatus.PENDING, JobStatus.ACCEPTED, JobStatus.COMPLETED)

    /** Which buttons a role may see for a job in a given status. */
    fun actionsFor(status: String, role: String): List<JobAction> = when {
        role == Role.STUDENT && status == JobStatus.PENDING -> listOf(JobAction.ACCEPT, JobAction.DECLINE)
        role == Role.STUDENT && status == JobStatus.ACCEPTED -> listOf(JobAction.COMPLETE)
        role == Role.CUSTOMER && status == JobStatus.COMPLETED -> listOf(JobAction.CONFIRM)
        else -> emptyList()
    }

    fun canTransition(from: String, to: String, role: String) =
        actionsFor(from, role).any { it.target == to }

    fun canReview(job: Job, role: String) =
        role == Role.CUSTOMER && job.status == JobStatus.CONFIRMED && job.rating == 0

    /** 0 = Pending, 1 = Active, 2 = Completed (including declined) */
    fun bucket(status: String) = when (status) {
        JobStatus.PENDING -> 0
        JobStatus.ACCEPTED, JobStatus.COMPLETED -> 1
        else -> 2
    }

    fun label(status: String) = when (status) {
        JobStatus.PENDING -> "Pending"
        JobStatus.ACCEPTED -> "Accepted"
        JobStatus.DECLINED -> "Declined"
        JobStatus.COMPLETED -> "Awaiting confirmation"
        JobStatus.CONFIRMED -> "Completed"
        else -> status
    }
}

object RatingMath {
    fun newAverage(oldAverage: Double, oldCount: Int, newRating: Int): Double {
        val avg = (oldAverage * oldCount + newRating) / (oldCount + 1)
        return Math.round(avg * 100) / 100.0
    }
}

object Search {
    fun filter(gigs: List<Gig>, query: String, category: String): List<Gig> {
        val q = query.trim().lowercase()
        return gigs.filter { g ->
            g.isActive &&
                (category == "All" || g.category == category) &&
                (q.isEmpty() || listOf(g.title, g.description, g.category, g.studentName, g.location)
                    .any { it.lowercase().contains(q) })
        }
    }
}
