package it.progmob.huangapp.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import com.google.firebase.firestore.toObject
import it.progmob.huangapp.ui.data.model.Recipes

class HomeViewModel : ViewModel() {
    private val _recipesList = MutableLiveData<List<Recipes>>()
    val recipesList: LiveData<List<Recipes>> = _recipesList
    var allRecipes = mutableListOf<Recipes>()
    private val _userRecipes = MutableLiveData<List<Recipes>>()
    val userRecipes: LiveData<List<Recipes>> = _userRecipes
    private val db = Firebase.firestore

    fun uploadDB() {
        db.collection("recipes")
            .addSnapshotListener { result, exception ->
                if (exception != null) {
                    Log.w("Firestore", "Errore nel caricamento dei dati", exception)
                    return@addSnapshotListener
                }

                if (result != null) {
                    val list = mutableListOf<Recipes>()
                    for (document in result) {
                        val recipe = document.toObject<Recipes>()
                        // Assegniamo l'ID del documento Firestore all'oggetto
                        recipe.id = document.id
                        list.add(recipe)
                        Log.d("Firestore", "${document.id} => $recipe")
                    }
                    allRecipes = list
                    _recipesList.postValue(list) // Aggiorna la lista di ricette
                }
            }
    }

    fun searchRecipes(query: String) {
        //Se la query è vuota, mostriamo la lista completa originale
        if (query.isEmpty()) {
            _recipesList.postValue(allRecipes)
            return
        }
        //Filtra le ricette in base alla query
        val list = allRecipes.filter { recipe ->
            recipe.name.contains(query, ignoreCase = true)
        }
        _recipesList.postValue(list)
    }

    //Funzione per il profilo
    fun loadUserRecipes(userId: String) {
        db.collection("recipes")
            .whereEqualTo("userID", userId) // Filtra per ID utente
            .addSnapshotListener { result, e ->
                if (e != null) return@addSnapshotListener

                val list = mutableListOf<Recipes>()
                result?.forEach { doc ->
                    val recipe = doc.toObject<Recipes>()
                    recipe.id = doc.id
                    list.add(recipe)
                }
                _userRecipes.postValue(list) // Aggiorna la lista di ricette dell'utente
            }
    }

}