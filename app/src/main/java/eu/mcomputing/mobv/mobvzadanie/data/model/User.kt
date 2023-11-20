package eu.mcomputing.mobv.mobvzadanie.data.model

import com.google.gson.Gson
import okio.IOException

data class User(
    val username: String,
    val email: String,
    val id: String,
    val access: String,
    val refresh: String,
    val photo: String = ""
) {

    fun toJson(): String? {
        return try {
            Gson().toJson(this)
        } catch (ex: IOException) {
            ex.printStackTrace()
            null
        }
    }

    companion object {
        fun fromJson(string: String): User? {
            return try {
                Gson().fromJson(string, User::class.java)
            } catch (ex: IOException) {
                ex.printStackTrace()
                null
            }
        }
    }
}