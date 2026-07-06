package it.progmob.huangapp.ui.data.model

/**
 * Data class that captures user information for logged in users retrieved from LoginRepository
 */
data class LoggedInUser(
    val userId: String,
    val username: String = "",
    val userImage: String = "",
    val favorites: List<String> = emptyList(),
    val following: List<String> = emptyList(),
    val followers: List<String> = emptyList()

)