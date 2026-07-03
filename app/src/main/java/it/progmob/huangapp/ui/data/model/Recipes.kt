package it.progmob.huangapp.ui.data.model

data class Recipes (

    var id: String = "",
    val name: String = "",
    val image: String = "",
    val description: String = "",
    val ingredients: List<String> = emptyList(),
    val steps: List<String> = emptyList(),
    val cooktime: String = "",
    val category: String = "",
    val userID: String = "",
    val username: String = "",
    val userImage: String = "",
    val comments: List<String> = emptyList()

)