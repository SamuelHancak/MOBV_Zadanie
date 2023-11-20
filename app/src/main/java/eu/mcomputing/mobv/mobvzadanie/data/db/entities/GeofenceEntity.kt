package eu.mcomputing.mobv.mobvzadanie.data.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "geofence")
class GeofenceEntity(
    val lat: Double,
    val lon: Double,
    val radius: Double,
    @PrimaryKey val time: Long = System.currentTimeMillis(),
    var uploaded: Boolean = false
)