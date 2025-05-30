package com.example.appclima.network

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.os.Looper
import com.google.android.gms.location.LocationAvailability
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class LocationService(context: Context) {


    @SuppressLint("MissingPermission")
    suspend fun getCurrentLocationSuspend(context: Context): Location? =
        suspendCancellableCoroutine { cont ->

            val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)

            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    cont.resume(location)
                } else {
                    val locationRequest = LocationRequest.Builder(
                        Priority.PRIORITY_HIGH_ACCURACY,
                        1000L
                    ).build()

                    val locationCallback = object : LocationCallback() {
                        override fun onLocationResult(locationResult: LocationResult) {
                            fusedLocationClient.removeLocationUpdates(this)
                            cont.resume(locationResult.lastLocation)
                        }

                        override fun onLocationAvailability(availability: LocationAvailability) {
                            if (!availability.isLocationAvailable) {
                                cont.resume(null)
                            }
                        }
                    }

                    fusedLocationClient.requestLocationUpdates(
                        locationRequest,
                        locationCallback,
                        Looper.getMainLooper()
                    )

                    // Cancelar petición si corutina se cancela
                    cont.invokeOnCancellation {
                        fusedLocationClient.removeLocationUpdates(locationCallback)
                    }
                }
            }.addOnFailureListener {
                cont.resumeWithException(it)
            }
        }


}