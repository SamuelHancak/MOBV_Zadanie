package eu.mcomputing.mobv.mobvzadanie.data.api.model

data class RegistrationResponse(val uid: String, val access: String, val refresh: String)

data class LoginResponse(val uid: String, val access: String, val refresh: String)

data class UserResponse(val id: String, val name: String, val photo: String)

data class RefreshTokenResponse(val uid: String, val access: String, val refresh: String)

data class GeofenceListResponse(
    val me: GeofenceListMeResponse,
    val list: List<GeofenceListAllResponse>
)

data class GeofenceListMeResponse(
    val uid: String,
    val lat: Double,
    val lon: Double,
    val radius: Double,
)

data class GeofenceListAllResponse(
    val uid: String,
    val name: String,
    val updated: String,
    val radius: Double,
    val photo: String
)

data class GeofenceUpdateResponse(val success: String)