package com.example.findmycafe.data

import android.content.Context
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.tasks.await

interface LocationRepository { suspend fun currentLocation(): Coordinate? }

class FusedLocationRepository(context: Context) : LocationRepository {
    private val client = LocationServices.getFusedLocationProviderClient(context.applicationContext)
    override suspend fun currentLocation(): Coordinate? {
        val last = client.lastLocation.await()
        val location = last ?: client.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, CancellationTokenSource().token).await()
        return location?.let { Coordinate(it.latitude, it.longitude) }
    }
}
