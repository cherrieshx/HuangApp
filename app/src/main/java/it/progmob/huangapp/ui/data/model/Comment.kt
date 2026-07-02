package it.progmob.huangapp.ui.data.model

data class Comment(
    val userId: String = "",
    val username: String = "",
    val userImage: String = "",
    val text: String = "",
    val timestamp: Long = 0
)
