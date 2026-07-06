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

    private val _userRecipes = MutableLiveData<List<Recipes>>()
    val userRecipes: LiveData<List<Recipes>> = _userRecipes
    private val _favorites = MutableLiveData<List<Recipes>>()
    val favorites: LiveData<List<Recipes>> = _favorites

    private val db = Firebase.firestore

    private var fullList = listOf<Recipes>()
    private var isListenerRegistered = false

    var currentCategory = "Tutte"
        private set
    private var currentQuery = ""
    var isAscending = true
        private set

    fun uploadDB() {
        // Evita di registrare listener multipli ad ogni ritorno sul fragment
        if (isListenerRegistered) return
        isListenerRegistered = true

        db.collection("recipes")
            .addSnapshotListener { result, exception ->
                if (exception != null) {
                    Log.w("Firestore", "Errore nel caricamento", exception)
                    return@addSnapshotListener
                }

                if (result != null) {
                    val list = result.map { document ->
                        val recipe = document.toObject<Recipes>()
                        recipe.id = document.id
                        recipe
                    }
                    fullList = list
                    applyFilters()
                }
            }
    }

    // Chiamato ad ogni onViewCreated: se i dati sono già in memoria riapplica i filtri subito,
    // altrimenti registra il listener Firestore per la prima volta
    fun initOrRefresh() {
        if (isListenerRegistered) {
            // Dati già presenti, riapplica filtri correnti per aggiornare la UI
            applyFilters()
        } else {
            uploadDB()
        }
    }

    // Funzione unica per gestire Ricerca, Categoria e Tempo
    fun applyFilters(
        category: String = currentCategory,
        query: String = currentQuery,
        ascending: Boolean = isAscending
    ) {
        currentCategory = category
        currentQuery = query
        isAscending = ascending

        val filtered = if (query.isNotEmpty()) {
            // Ricerca indipendentemente dalla categoria
            fullList.filter { it.name.contains(query, ignoreCase = true) }
        } else {
            if (category == "Tutte") fullList
            else fullList.filter { it.category == category }
        }

        // Ordinamento sempre per cookitime
        val sorted = filtered.let { list ->
            if (ascending) {
                list.sortedBy { it.cooktime.toIntOrNull() ?: 0 }
            } else {
                list.sortedByDescending { it.cooktime.toIntOrNull() ?: 0 }
            }
        }

        _recipesList.postValue(sorted)
    }

    fun searchRecipes(query: String) {
        applyFilters(query = query)
    }

    fun loadUserRecipes(userId: String) {
        db.collection("recipes")
            .whereEqualTo("userID", userId)
            .addSnapshotListener { result, e ->
                if (e != null) return@addSnapshotListener
                val list = result?.map { doc ->
                    val recipe = doc.toObject<Recipes>()
                    recipe.id = doc.id
                    recipe
                } ?: emptyList()
                _userRecipes.postValue(list)
            }
    }
    fun loadFavorites(userId: String) {
        db.collection("users").document(userId).collection("favorites")
            .addSnapshotListener { snapshot,e ->
                if (e != null) {
                    Log.e("FAV", "Errore listener preferiti: ${e.message}")
                    return@addSnapshotListener
                }

                val favoriteIds = snapshot?.documents?.map { it.id } ?: emptyList()
                if (favoriteIds.isEmpty()) {
                    _favorites.postValue(emptyList())
                    return@addSnapshotListener
                }
                // Recupera i dettagli di ogni ricetta preferita
                db.collection("recipes")
                    .whereIn(com.google.firebase.firestore.FieldPath.documentId(), favoriteIds)
                    .get()
                    .addOnSuccessListener { result ->
                        val recipes = result.map { doc ->
                            val recipe = doc.toObject<Recipes>()
                            recipe.id = doc.id
                            recipe
                        }
                        _favorites.postValue(recipes)
                    }
            }
    }
}