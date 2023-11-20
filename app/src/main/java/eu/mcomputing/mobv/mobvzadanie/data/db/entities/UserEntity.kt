package eu.mcomputing.mobv.mobvzadanie.data.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
class UserEntity(
    @PrimaryKey val uid: String,
    val name: String,
    val updated: String,
    val lat: Double,
    val lon: Double,
    val radius: Double,
    val photo: String = ""
) {


    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as UserEntity

        if (uid != other.uid) return false
        if (name != other.name) return false
        if (updated != other.updated) return false
        if (lat != other.lat) return false
        if (lon != other.lon) return false
        if (radius != other.radius) return false
        if (photo != other.photo) return false

        return true
    }

    override fun hashCode(): Int {
        var result = uid.hashCode()
        result = 31 * result + name.hashCode()
        result = 31 * result + updated.hashCode()
        result = 31 * result + lat.hashCode()
        result = 31 * result + lon.hashCode()
        result = 31 * result + radius.hashCode()
        result = 31 * result + photo.hashCode()
        return result
    }

    override fun toString(): String {
        return "UserEntity(uid='$uid', name='$name', updated='$updated', lat=$lat, lon=$lon, radius=$radius, photo='$photo')"
    }
}