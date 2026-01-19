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
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Instrumented test for the [TrackPointDao].
 * Verifies that location points are correctly persisted and linked to their respective trips. */

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
class TrackPointDaoIntegrationTest {

    @get:Rule
    @JvmField
    var instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var database: AppDatabase
    private lateinit var trackPointDao: TrackPointDao
    private lateinit var tripDao: TripDao

    @Before
    fun createDb() {
        // Initialize an in-memory database
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        trackPointDao = database.trackPointDao()
        tripDao = database.tripDao()
    }

    @After
    fun closeDb() {
        database.close()
    }

    /** INT-06: Verifies that multiple track points can be inserted and correctly
     * retrieved for a specific trip, maintaining the parent-child relationship. */
    @Test
    fun insertAndGetTrackPointsForTrip() = runTest {
        // Arrange - Insert a parent trip first
        val trip = Trip(
            id = "test-trip-1",
            startTimestamp = 1000L,
            endTimestamp = 2000L,
            overallRating = 5,
            delayRating = 1,
            delayMinutes = 0,
            delayComment = "No delay",
            isSynced = false
        )
        tripDao.insert(trip)

        // Arrange - Create coordinate points for this trip (Using Long for timestamp)
        val point1 = TrackPoint(tripId = trip.id, lat = 1.0, lon = 1.0, acc = 10f, timestamp = 1000L)
        val point2 = TrackPoint(tripId = trip.id, lat = 2.0, lon = 2.0, acc = 10f, timestamp = 2000L)

        // Act - Insert the track points into the database
        trackPointDao.insert(point1)
        trackPointDao.insert(point2)

        // Act - Retrieve all points linked to the test trip
        val retrievedPoints = trackPointDao.getTrackPointsForTrip(trip.id)

        // Assert - Verify that both points were retrieved correctly with auto-generated IDs
        assertEquals(2, retrievedPoints.size)
        // Note: Room auto-generates IDs starting from 1 in an empty DB
        assertEquals(point1.copy(id = 1), retrievedPoints[0])
        assertEquals(point2.copy(id = 2), retrievedPoints[1])
    }
}
