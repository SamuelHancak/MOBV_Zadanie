package eu.mcomputing.mobv.mobvzadanie.data

import android.content.Context
import android.net.Uri
import eu.mcomputing.mobv.mobvzadanie.data.api.ApiService
import eu.mcomputing.mobv.mobvzadanie.data.api.model.ChangePasswordRequest
import eu.mcomputing.mobv.mobvzadanie.data.api.model.ForgottenPasswordRequest
import eu.mcomputing.mobv.mobvzadanie.data.api.model.GeofenceUpdateRequest
import eu.mcomputing.mobv.mobvzadanie.data.api.model.UserLoginRequest
import eu.mcomputing.mobv.mobvzadanie.data.api.model.UserRegistrationRequest
import eu.mcomputing.mobv.mobvzadanie.data.db.AppRoomDatabase
import eu.mcomputing.mobv.mobvzadanie.data.db.LocalCache
import eu.mcomputing.mobv.mobvzadanie.data.db.entities.GeofenceEntity
import eu.mcomputing.mobv.mobvzadanie.data.db.entities.UserEntity
import eu.mcomputing.mobv.mobvzadanie.data.model.User
import eu.mcomputing.mobv.mobvzadanie.utils.URIPathHelper
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.IOException
import java.security.MessageDigest

class DataRepository private constructor(
    private val service: ApiService,
    private val cache: LocalCache
) {
    companion object {
        @Volatile
        private var INSTANCE: DataRepository? = null
        private val lock = Any()

        fun getInstance(context: Context): DataRepository =
            INSTANCE ?: synchronized(lock) {
                INSTANCE
                    ?: DataRepository(
                        ApiService.create(context),
                        LocalCache(AppRoomDatabase.getInstance(context).appDao())
                    ).also { INSTANCE = it }
            }

        fun hashPassword(plainPassword: String): String {
            val bytes = plainPassword.toByteArray()
            val digest = MessageDigest.getInstance("SHA-256")
            val hashedBytes = digest.digest(bytes)
            return hashedBytes.joinToString("") { "%02x".format(it) }
        }
    }

    suspend fun apiRegisterUser(
        username: String,
        email: String,
        password: String,
        repeatPassword: String
    ): Pair<String, User?> {
        if (username.isEmpty()) {
            return Pair("Username can not be empty", null)
        }
        if (email.isEmpty()) {
            return Pair("Email can not be empty", null)
        }
        if (password.isEmpty()) {
            return Pair("Password can not be empty", null)
        }
        if (repeatPassword.isEmpty()) {
            return Pair("Repeat the password please", null)
        }
        if (password != repeatPassword) {
            return Pair("Passwords do not match", null)
        }
        try {
            val response = service.registerUser(
                UserRegistrationRequest(
                    username,
                    email,
                    hashPassword(password)
                )
            )
            if (response.isSuccessful) {
                response.body()?.let { jsonResponse ->
                    when (jsonResponse.uid) {
                        "-1" -> {
                            return Pair("Username is already registered.", null)
                        }

                        "-2" -> {
                            return Pair("Email is already registered.", null)
                        }

                        else -> {
                            return Pair(
                                "",
                                User(
                                    username,
                                    email,
                                    jsonResponse.uid,
                                    jsonResponse.access,
                                    jsonResponse.refresh
                                )
                            )
                        }
                    }
                }
            }
            return Pair("Failed to create user", null)
        } catch (ex: IOException) {
            ex.printStackTrace()
            return Pair("Check internet connection. Failed to create user.", null)
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
        return Pair("Fatal error. Failed to create user.", null)
    }

    suspend fun apiLoginUser(
        username: String,
        password: String
    ): Pair<String, User?> {
        if (username.isEmpty()) {
            return Pair("Username can not be empty", null)
        }
        if (password.isEmpty()) {
            return Pair("Password can not be empty", null)
        }
        try {
            val response = service.loginUser(
                UserLoginRequest(
                    username,
                    hashPassword(password)
                )
            )
            if (response.isSuccessful) {
                response.body()?.let { jsonResponse ->
                    if (jsonResponse.uid == "-1") {
                        return Pair("Wrong password or username.", null)
                    }
                    return Pair(
                        "",
                        User(
                            username,
                            "",
                            jsonResponse.uid,
                            jsonResponse.access,
                            jsonResponse.refresh
                        )
                    )
                }
            }
            return Pair("Failed to login user", null)
        } catch (ex: IOException) {
            ex.printStackTrace()
            return Pair("Check internet connection. Failed to login user.", null)
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
        return Pair("Fatal error. Failed to login user.", null)
    }

    suspend fun apiChangePassword(
        old_password: String,
        new_password: String
    ): String {
        if (old_password.isEmpty()) {
            return "Old password can not be empty"
        }
        if (new_password.isEmpty()) {
            return "New password can not be empty"
        }
        try {
            val response = service.changePassword(
                ChangePasswordRequest(
                    hashPassword(old_password),
                    hashPassword(new_password)
                )
            )
            if (response.isSuccessful) {
                response.body()?.let {
                    return "Success! Password was changed."
                }
            }
            return "Failed to change password."
        } catch (ex: IOException) {
            ex.printStackTrace()
            return "Check internet connection. Failed to change password."
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
        return "Fatal error. Failed to change password."
    }

    suspend fun uploadImage(
        uri: Uri?, context: Context
    ): String {
        try {
            if (uri == null || uri.path == null || uri.path!!.isEmpty()) {
                return ("Select image first.")
            }

            val uriPathHelper = URIPathHelper()
            val filePath = uriPathHelper.getPath(context, uri)
            val file = File(filePath!!)
            val requestFile: RequestBody =
                file.asRequestBody("multipart/form-data".toMediaTypeOrNull())
            val multiPartBody = MultipartBody.Part.createFormData("image", file.name, requestFile)

            val response = service.uploadPhoto(multiPartBody)

            if (response.isSuccessful) {
                response.body()?.let {
                    return "Success! Image was uploaded."
                }
            }
            return "Failed to upload image."
        } catch (ex: IOException) {
            ex.printStackTrace()
            return "Check internet connection. Failed to upload image."
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
        return "Fatal error. Failed to upload image."
    }

    suspend fun apiGetUser(
        uid: String
    ): Pair<String, User?> {
        try {
            val response = service.getUser(uid)

            if (response.isSuccessful) {
                response.body()?.let {
                    return Pair("", User(it.name, "", it.id, "", "", it.photo))
                }
            }

            return Pair("Failed to load user", null)
        } catch (ex: IOException) {
            ex.printStackTrace()
            return Pair("Check internet connection. Failed to load user.", null)
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
        return Pair("Fatal error. Failed to load user.", null)
    }

    suspend fun apiListGeofence(): String {
        try {
            val response = service.listGeofence()

            if (response.isSuccessful) {
                response.body()?.list?.let {
                    val users = it.map {
                        UserEntity(
                            it.uid, it.name, it.updated,
                            0.0, 0.0, it.radius, it.photo
                        )
                    }
                    cache.insertUserItems(users)
                    return ""
                }
            }

            return "Failed to load users"
        } catch (ex: IOException) {
            ex.printStackTrace()
            return "Check internet connection. Failed to load users."
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
        return "Fatal error. Failed to load users."
    }

    fun getUsers() = cache.getUsers()

    fun getGeofence() = cache.getUserGeofence()

    suspend fun getUsersList() = cache.getUsersList()

    suspend fun insertGeofence(item: GeofenceEntity) {
        cache.insertGeofence(item)
        try {
            val response =
                service.updateGeofence(GeofenceUpdateRequest(item.lat, item.lon, item.radius))

            if (response.isSuccessful) {
                response.body()?.let {

                    item.uploaded = true
                    cache.insertGeofence(item)
                    return
                }
            }

            return
        } catch (ex: IOException) {
            ex.printStackTrace()
            return
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }

    suspend fun removeGeofence() {
        try {
            val response = service.deleteGeofence()

            if (response.isSuccessful) {
                response.body()?.let {
                    return
                }
            }

            return
        } catch (ex: IOException) {
            ex.printStackTrace()
            return
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }

    suspend fun logoutUser() {
        cache.logoutUser()
    }

    suspend fun removeImage(): String {
        try {
            val response = service.deletePhoto()

            if (response.isSuccessful) {
                response.body()?.let {
                    return "Success! Profile picture was deleted."
                }
            }

            return "Failed to delete profile picture."
        } catch (ex: IOException) {
            ex.printStackTrace()
            return "Check internet connection. Failed to delete profile picture."
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
        return "Fatal error. Failed to delete profile picture."
    }

    suspend fun forgottenPassword(email: String?): Pair<String, String> {
        try {
            if (email.isNullOrEmpty()) {
                return Pair("failure", "Insert you email into username field.")
            }

            val response = service.forgottenPassword(ForgottenPasswordRequest(email))
            if (response.isSuccessful) {
                response.body()?.let {
                    return Pair(
                        it.status,
                        "Email for password recovery was sent. Check your inbox."
                    )
                }
            }

            return Pair("failure", "Failed to send email")
        } catch (ex: IOException) {
            ex.printStackTrace()
            return Pair("failure", "Check internet connection. Failed to send email.")
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
        return Pair("failure", "Fatal error. Failed to send email.")
    }
}

