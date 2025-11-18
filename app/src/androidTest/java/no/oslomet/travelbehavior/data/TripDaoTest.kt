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

/**
 * Instrumented test for the [TripDao],
 * which will execute on an Android device.
 */
@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
class TripDaoTest {

    // This rule swaps the background executor used by the Architecture Components with a different one which executes each task synchronously.
    @get:Rule
    var instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var database: AppDatabase
    private lateinit var tripDao: TripDao

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        // Using an in-memory database because the information stored here disappears when the
        // process is killed.
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            // Allowing main thread queries, just for testing.
            .allowMainThreadQueries()
            .build()
        tripDao = database.tripDao()
    }

    @After
    fun closeDb() {
        database.close()
    }

    @Test
    fun insertTripAndGetUnsynced() = runTest {
        // ARRANGE - Create a trip that is not synced.
        val unsyncedTrip = Trip(
            id = "test-trip-1",
            endTimestamp = System.currentTimeMillis(),
            overallRating = null,
            delayRating = null,
            delayMinutes = null,
            delayComment = null,
            isSynced = false
        )

        // ACT - Insert the trip into the database.
        tripDao.insert(unsyncedTrip)
        val unsyncedTrips = tripDao.getUnsyncedTrips()

        // ASSERT - Verify that the retrieved list contains our trip.
        assertEquals(1, unsyncedTrips.size)
        assertEquals(unsyncedTrip, unsyncedTrips[0])
    }

    @Test
    fun markAsSynced_removesTripFromUnsyncedList() = runTest {
        // ARRANGE - Insert a trip that is not synced.
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

        // ACT - Mark the trip as synced.
        tripDao.markAsSynced(trip.id, "firebase-id-123")
        val unsyncedTrips = tripDao.getUnsyncedTrips()

        // ASSERT - Verify that the trip is no longer in the unsynced list.
        assertTrue(unsyncedTrips.isEmpty())
    }

    @Test
    fun deleteById_removesTripFromDatabase() = runTest {
        // ARRANGE - Insert a trip.
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

        // ACT - Delete the trip by its ID.
        tripDao.deleteById(trip.id)
        val trips = tripDao.getUnsyncedTrips()

        // ASSERT - Verify that the trip is no longer in the database.
        assertTrue(trips.isEmpty())
    }
}
