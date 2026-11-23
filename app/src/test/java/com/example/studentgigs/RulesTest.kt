package com.example.studentgigs

import com.example.studentgigs.data.*
import org.junit.Assert.*
import org.junit.Test

class RulesTest {

    // ---------------------------------------------------- Validators
    @Test
    fun testEmailValidation() {
        assertNotNull(Validators.email(""))
        assertNotNull(Validators.email("invalid-email"))
        assertNotNull(Validators.email("test@domain"))
        assertNull(Validators.email("student@example.com"))
        assertNull(Validators.email("  customer@univ.ac.mw  "))
    }

    @Test
    fun testPasswordValidation() {
        assertNotNull(Validators.password(""))
        assertNotNull(Validators.password("12345"))
        assertNull(Validators.password("123456"))
        assertNull(Validators.password("securePassword123"))
    }

    @Test
    fun testNameValidation() {
        assertNotNull(Validators.name(""))
        assertNotNull(Validators.name(" A "))
        assertNull(Validators.name("John Doe"))
        assertNull(Validators.name("Alice"))
    }

    @Test
    fun testPhoneValidation() {
        assertNull(Validators.phone("")) // Optional field
        assertNull(Validators.phone("+265999123456"))
        assertNull(Validators.phone("265 999 123 456"))
        assertNull(Validators.phone("+265-999-123-456"))
        assertNull(Validators.phone("(265) 999 1234"))
        assertNotNull(Validators.phone("abc123456"))
        assertNotNull(Validators.phone("1234")) // Too short
    }

    @Test
    fun testTitleValidation() {
        assertNotNull(Validators.title(""))
        assertNotNull(Validators.title("a".repeat(61)))
        assertNull(Validators.title("Poster Design"))
    }

    @Test
    fun testDescriptionValidation() {
        assertNotNull(Validators.description(""))
        assertNotNull(Validators.description("Short")) // < 10 chars
        assertNull(Validators.description("Full printing and photocopy service"))
    }

    @Test
    fun testPriceValidation() {
        assertNotNull(Validators.price(""))
        assertNotNull(Validators.price("abc"))
        assertNotNull(Validators.price("0"))
        assertNotNull(Validators.price("-50"))
        assertNull(Validators.price("500"))
        assertNull(Validators.price("1500.50"))
    }

    @Test
    fun testNoteValidation() {
        assertNotNull(Validators.note(""))
        assertNotNull(Validators.note("a".repeat(201)))
        assertNull(Validators.note("Need 10 copies printed by tomorrow"))
    }

    @Test
    fun testRatingValidation() {
        assertNotNull(Validators.rating(0))
        assertNotNull(Validators.rating(6))
        assertNull(Validators.rating(1))
        assertNull(Validators.rating(5))
    }

    // ---------------------------------------------------- JobWorkflow
    @Test
    fun testJobWorkflowActions() {
        // Student actions
        val studentPending = JobWorkflow.actionsFor(JobStatus.PENDING, Role.STUDENT)
        assertEquals(listOf(JobAction.ACCEPT, JobAction.DECLINE), studentPending)

        val studentAccepted = JobWorkflow.actionsFor(JobStatus.ACCEPTED, Role.STUDENT)
        assertEquals(listOf(JobAction.COMPLETE), studentAccepted)

        val studentCompleted = JobWorkflow.actionsFor(JobStatus.COMPLETED, Role.STUDENT)
        assertTrue(studentCompleted.isEmpty())

        // Customer actions
        val customerCompleted = JobWorkflow.actionsFor(JobStatus.COMPLETED, Role.CUSTOMER)
        assertEquals(listOf(JobAction.CONFIRM), customerCompleted)

        val customerConfirmed = JobWorkflow.actionsFor(JobStatus.CONFIRMED, Role.CUSTOMER)
        assertTrue(customerConfirmed.isEmpty())

        // Admin actions (no direct workflow actions)
        val adminPending = JobWorkflow.actionsFor(JobStatus.PENDING, Role.ADMIN)
        assertTrue(adminPending.isEmpty())
    }

    @Test
    fun testJobWorkflowTransitions() {
        assertTrue(JobWorkflow.canTransition(JobStatus.PENDING, JobStatus.ACCEPTED, Role.STUDENT))
        assertTrue(JobWorkflow.canTransition(JobStatus.PENDING, JobStatus.DECLINED, Role.STUDENT))
        assertTrue(JobWorkflow.canTransition(JobStatus.ACCEPTED, JobStatus.COMPLETED, Role.STUDENT))
        assertTrue(JobWorkflow.canTransition(JobStatus.COMPLETED, JobStatus.CONFIRMED, Role.CUSTOMER))

        assertFalse(JobWorkflow.canTransition(JobStatus.PENDING, JobStatus.CONFIRMED, Role.STUDENT))
        assertFalse(JobWorkflow.canTransition(JobStatus.ACCEPTED, JobStatus.CONFIRMED, Role.CUSTOMER))
    }

    @Test
    fun testCanReview() {
        val jobConfirmedUnreviewed = Job(status = JobStatus.CONFIRMED, rating = 0)
        val jobConfirmedReviewed = Job(status = JobStatus.CONFIRMED, rating = 5)
        val jobCompleted = Job(status = JobStatus.COMPLETED, rating = 0)

        assertTrue(JobWorkflow.canReview(jobConfirmedUnreviewed, Role.CUSTOMER))
        assertFalse(JobWorkflow.canReview(jobConfirmedReviewed, Role.CUSTOMER))
        assertFalse(JobWorkflow.canReview(jobConfirmedUnreviewed, Role.STUDENT))
        assertFalse(JobWorkflow.canReview(jobCompleted, Role.CUSTOMER))
    }

    @Test
    fun testJobBucketsAndLabels() {
        assertEquals(0, JobWorkflow.bucket(JobStatus.PENDING))
        assertEquals(1, JobWorkflow.bucket(JobStatus.ACCEPTED))
        assertEquals(1, JobWorkflow.bucket(JobStatus.COMPLETED))
        assertEquals(2, JobWorkflow.bucket(JobStatus.CONFIRMED))
        assertEquals(2, JobWorkflow.bucket(JobStatus.DECLINED))

        assertEquals("Pending", JobWorkflow.label(JobStatus.PENDING))
        assertEquals("Accepted", JobWorkflow.label(JobStatus.ACCEPTED))
        assertEquals("Awaiting confirmation", JobWorkflow.label(JobStatus.COMPLETED))
        assertEquals("Completed", JobWorkflow.label(JobStatus.CONFIRMED))
        assertEquals("Declined", JobWorkflow.label(JobStatus.DECLINED))
    }

    // ---------------------------------------------------- RatingMath
    @Test
    fun testRatingMath() {
        // First rating ever: 5 stars
        assertEquals(5.0, RatingMath.newAverage(0.0, 0, 5), 0.001)

        // Second rating: 4 stars after initial 5 stars
        assertEquals(4.5, RatingMath.newAverage(5.0, 1, 4), 0.001)

        // Third rating: 4 stars after 4.5 avg (2 ratings)
        assertEquals(4.33, RatingMath.newAverage(4.5, 2, 4), 0.001)
    }

    // ---------------------------------------------------- Search
    @Test
    fun testSearchFilter() {
        val gigs = listOf(
            Gig(gigId = "1", title = "Fast Photocopying", category = "Photocopying", studentName = "Alice", location = "Library", isActive = true),
            Gig(gigId = "2", title = "Math Tutoring", category = "Tutoring", studentName = "Bob", location = "Chichiri", isActive = true),
            Gig(gigId = "3", title = "Poster Design", category = "Poster Design", studentName = "Charlie", location = "Blantyre", isActive = false)
        )

        // Filter active only
        val allActive = Search.filter(gigs, "", "All")
        assertEquals(2, allActive.size)
        assertFalse(allActive.any { it.gigId == "3" })

        // Filter by category
        val tutoring = Search.filter(gigs, "", "Tutoring")
        assertEquals(1, tutoring.size)
        assertEquals("Math Tutoring", tutoring.first().title)

        // Filter by query (title)
        val photo = Search.filter(gigs, "photo", "All")
        assertEquals(1, photo.size)
        assertEquals("1", photo.first().gigId)

        // Filter by query (location)
        val chichiri = Search.filter(gigs, "Chichiri", "All")
        assertEquals(1, chichiri.size)
        assertEquals("2", chichiri.first().gigId)
    }
}
