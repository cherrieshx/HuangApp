package it.progmob.huangapp.ui.data.model

data class Comment(
    var id: String = "",
    val userId: String = "",
    val username: String = "",
    val userImage: String = "",
    val text: String = "",
    val timestamp: Long = 0
)
