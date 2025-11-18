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
import java.util.Date

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
class TrackPointDaoTest {

    @get:Rule
    var instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var database: AppDatabase
    private lateinit var trackPointDao: TrackPointDao
    private lateinit var tripDao: TripDao

    @Before
    fun createDb() {
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

    @Test
    fun insertAndGetTrackPointsForTrip() = runTest {
        // ARRANGE - Insert a parent trip first
        val trip = Trip(
            id = "test-trip-1",
            endTimestamp = 0L,
            overallRating = 5,
            delayRating = 1,
            delayMinutes = 0,
            delayComment = "No delay",
            isSynced = false
        )
        tripDao.insert(trip)

        // ARRANGE - Create track points for the trip
        val point1 = TrackPoint(tripId = trip.id, lat = 1.0, lon = 1.0, acc = 10f, timestamp = Date(1000L))
        val point2 = TrackPoint(tripId = trip.id, lat = 2.0, lon = 2.0, acc = 10f, timestamp = Date(2000L))

        // ACT - Insert the track points one by one
        trackPointDao.insert(point1)
        trackPointDao.insert(point2)

        // ACT - Retrieve the track points for the specific trip
        val retrievedPoints = trackPointDao.getTrackPointsForTrip(trip.id)

        // ASSERT - Verify that the correct points are returned
        assertEquals(2, retrievedPoints.size)
        assertEquals(point1.copy(id = 1), retrievedPoints[0]) // Room auto-generates IDs
        assertEquals(point2.copy(id = 2), retrievedPoints[1])
    }
}
