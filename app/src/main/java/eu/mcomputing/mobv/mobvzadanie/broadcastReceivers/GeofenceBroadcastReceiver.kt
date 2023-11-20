package eu.mcomputing.mobv.mobvzadanie.broadcastReceivers

import android.Manifest
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.util.Log
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofenceStatusCodes
import com.google.android.gms.location.GeofencingEvent
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices
import eu.mcomputing.mobv.mobvzadanie.data.DataRepository
import eu.mcomputing.mobv.mobvzadanie.data.db.entities.GeofenceEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class GeofenceBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        Log.d("GeofenceBroadcastReceiver", "start")
        if (intent == null) {
            // no geofence exit
            Log.e("GeofenceBroadcastReceiver", "error 1")
            return
        }

        val geofencingEvent = GeofencingEvent.fromIntent(intent)

        if (geofencingEvent == null) {
            // error
            Log.e("GeofenceBroadcastReceiver", "error 2")
            return
        }

        if (geofencingEvent.hasError()) {
            val errorMessage = GeofenceStatusCodes
                .getStatusCodeString(geofencingEvent.errorCode)
            //send error message to user using notification
            Log.e("GeofenceBroadcastReceiver", "error 3")
            return
        }

        // Get the transition type.
        val geofenceTransition = geofencingEvent.geofenceTransition

        if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_EXIT) {
            val triggeringLocation = geofencingEvent.triggeringLocation
            if (context == null || triggeringLocation == null) {
                // error
                Log.e("GeofenceBroadcastReceiver", "error 4")
                return
            }
            Log.d("GeofenceBroadcastReceiver", "event")
            setupGeofence(triggeringLocation, context)
        }
    }

    private fun setupGeofence(location: Location, context: Context) {

        val geofencingClient = LocationServices.getGeofencingClient(context)

        val geofence = Geofence.Builder()
            .setRequestId("my-geofence")
            .setCircularRegion(location.latitude, location.longitude, 100f) // 100m polomer
            .setExpirationDuration(Geofence.NEVER_EXPIRE)
            .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_EXIT)
            .build()

        val geofencingRequest = GeofencingRequest.Builder()
            .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
            .addGeofence(geofence)
            .build()

        val intent = Intent(context, GeofenceBroadcastReceiver::class.java)
        val geofencePendingIntent =
            PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)

        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // permission issue, geofence not created
            Log.e("GeofenceBroadcastReceiver", "error 5")
            return
        }
        geofencingClient.addGeofences(geofencingRequest, geofencePendingIntent).run {
            addOnSuccessListener {
                // Geofences boli úspešne pridané
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        // you code here
                        Log.d("GeofenceBroadcastReceiver", "novy geofence vytvoreny")
                        DataRepository.getInstance(context).insertGeofence(
                            GeofenceEntity(
                                location.latitude,
                                location.longitude,
                                100.0
                            )
                        )
                    } catch (e: Exception) {
                        Log.e("GeofenceBroadcastReceiver", "error 7")
                    }
                }

            }
            addOnFailureListener {
                // Chyba pri pridaní geofences
                Log.e("GeofenceBroadcastReceiver", "error 6")
            }
        }

    }
}