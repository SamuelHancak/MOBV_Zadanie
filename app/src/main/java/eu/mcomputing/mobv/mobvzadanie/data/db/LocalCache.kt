package venaka.bioapp.data.db

import androidx.lifecycle.LiveData
import eu.mcomputing.mobv.mobvzadanie.data.db.DbDao
import eu.mcomputing.mobv.mobvzadanie.data.db.entities.GeofenceEntity
import eu.mcomputing.mobv.mobvzadanie.data.db.entities.UserEntity

class LocalCache(private val dao: DbDao) {

    suspend fun logoutUser() {
        deleteUserItems()
    }

    suspend fun insertUserItems(items: List<UserEntity>) {
        dao.deleteUserItems()
        if (items.isNotEmpty()) {
            dao.insertUserItems(items)
        }
    }

    fun getUserItem(uid: String): LiveData<UserEntity?> {
        return dao.getUserItem(uid)
    }

    fun getUsers(): LiveData<List<UserEntity>?> = dao.getUsers()

    suspend fun getUsersList(): List<UserEntity>? = dao.getUsersList()

    suspend fun deleteUserItems() {
        dao.deleteUserItems()
    }

    suspend fun insertGeofence(item: GeofenceEntity) {
        dao.insertGeofence(item)
    }

}