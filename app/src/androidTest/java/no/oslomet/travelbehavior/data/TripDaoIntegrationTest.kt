package no.oslomet.travelbehavior.data

import android.content.Context
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Instrumented test for the [TripDao].
 * Verifies database operations for reiseøkter (trips) using an in-memory database. */

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
class TripDaoIntegrationTest {

    @get:Rule
    var instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var database: AppDatabase
    private lateinit var tripDao: TripDao

    @Before
    fun createDb() {
        // Initialize an in-memory database for isolated testing
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        tripDao = database.tripDao()
    }

    @After
    fun closeDb() {
        database.close()
    }

    /** INT-03: Verifies that a new trip can be inserted and correctly retrieved
     * from the list of unsynced trips. */
    @Test
    fun insertTripAndGetUnsynced() = runTest {
        // Create a test trip that is not yet synced
        val unsyncedTrip = Trip(
            id = "test-trip-1",
            endTimestamp = System.currentTimeMillis(),
            overallRating = null,
            delayRating = null,
            delayMinutes = null,
            delayComment = null,
            isSynced = false
        )

        // Insert the trip into the database
        tripDao.insert(unsyncedTrip)
        val unsyncedTrips = tripDao.getUnsyncedTrips()

        // Verify that the trip was saved and is in the unsynced list
        assertEquals(1, unsyncedTrips.size)
        assertEquals(unsyncedTrip, unsyncedTrips[0])
    }

    /** INT-04: Verifies that marking a trip as synced correctly updates its
     * status and removes it from the unsynced list. */
    @Test
    fun markAsSynced_removesTripFromUnsyncedList() = runTest {
        // Insert a trip that is initially not synced
        val trip = Trip(
            id = "test-trip-2",
            endTimestamp = System.currentTimeMillis(),
            overallRating = null,
            delayRating = null,
            delayMinutes = null,
            delayComment = null,
            isSynced = false
        )
        tripDao.insert(trip)
        assertEquals(1, tripDao.getUnsyncedTrips().size)

        // Update the trip's status to synced
        tripDao.markAsSynced(trip.id, "firebase-id-123")
        val unsyncedTrips = tripDao.getUnsyncedTrips()

        // Verify that the trip is no longer considered unsynced
        assertTrue(unsyncedTrips.isEmpty())
    }

    /** INT-05: Verifies that a trip can be permanently deleted from the
     * database by its unique ID. */
    @Test
    fun deleteById_removesTripFromDatabase() = runTest {
        // Insert a test trip
        val trip = Trip(
            id = "test-trip-3",
            endTimestamp = System.currentTimeMillis(),
            overallRating = null,
            delayRating = null,
            delayMinutes = null,
            delayComment = null,
            isSynced = false
        )
        tripDao.insert(trip)
        assertEquals(1, tripDao.getUnsyncedTrips().size)

        // Perform deletion by ID
        tripDao.deleteById(trip.id)
        val trips = tripDao.getUnsyncedTrips()

        // Verify the database is empty after deletion
        assertTrue(trips.isEmpty())
    }
}
