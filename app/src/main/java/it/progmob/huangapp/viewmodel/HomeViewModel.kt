package it.progmob.huangapp.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import com.google.firebase.firestore.toObject
import it.progmob.huangapp.ui.data.model.Recipes

enum class SortMode { FOR_YOU, POPULAR, TIME_ASC, TIME_DESC }

class HomeViewModel : ViewModel() {
    private val _recipesList = MutableLiveData<List<Recipes>>()
    val recipesList: MutableLiveData<List<Recipes>> = _recipesList
    private val _userRecipes = MutableLiveData<List<Recipes>>()
    val userRecipes: LiveData<List<Recipes>> = _userRecipes
    private val _favorites = MutableLiveData<List<Recipes>>()
    val favorites: LiveData<List<Recipes>> = _favorites
    private var followingIds = listOf<String>()
    var isFollowActive = false
        private set
    private val db = Firebase.firestore

    private var fullList = listOf<Recipes>()
    private var isListenerRegistered = false
    private var isInterestsLoaded = false

    var currentCategory = "Tutte"
        private set
    private var currentQuery = ""
    private var userFeedCount : Map<String, Int> = emptyMap()
    var currentSortMode = SortMode.FOR_YOU
        private set


    // Carica e aggiorna la lista di ricette in tempo reale
    fun uploadDB() {
        if (isListenerRegistered) return
        isListenerRegistered = true

        db.collection("recipes")
            .addSnapshotListener { result, exception ->
                if (exception != null) {
                    Log.w("Firestore", "Errore nel caricamento", exception)
                    return@addSnapshotListener
                }

                if (result != null) {
                    val list = result.mapNotNull { document ->
                        val recipe = document.toObject(Recipes::class.java)
                        recipe?.apply { id = document.id }
                    }
                    fullList = list
                    applyFilters()
                }
            }
    }

    fun loadFollowingIds(myUid: String) {
        db.collection("users").document(myUid)
            .addSnapshotListener { doc, _ ->
                if (doc != null && doc.exists()) {
                    followingIds = (doc.get("following") as? List<*>)?.filterIsInstance<String>() ?: emptyList()
                    if (isFollowActive) applyFilters()
                }
            }
    }

    fun toggleFollowingFilter(active: Boolean) {
        isFollowActive = active
        applyFilters()
    }

    fun initOrRefresh() {
        if (isListenerRegistered) {
            applyFilters()
        } else {
            uploadDB()
        }
    }

    fun loadUserInterests(myUid: String) {
        if (isInterestsLoaded) return
        db.collection("users").document(myUid)
            .get()
            .addOnSuccessListener { doc ->
                if (doc != null && doc.exists()) {
                    userFeedCount = (doc.get("feedcount") as? Map<*, *>)
                        ?.mapKeys { it.key.toString() }
                        ?.mapValues { (it.value as? Long)?.toInt() ?: 0 } ?: emptyMap<String, Int>()
                }
                isInterestsLoaded = true
                if (fullList.isNotEmpty()) applyFilters()
            }
    }

    // Gestisce la logica dei filtri di categoria, ordinamento seguiti e ricerca
    fun applyFilters(
        category: String = currentCategory,
        query: String = currentQuery,
        onlyFollowing: Boolean = isFollowActive,
        sortMode: SortMode = currentSortMode
    ) {
        currentCategory = category
        currentQuery = query
        isFollowActive = onlyFollowing
        currentSortMode = sortMode

        var filtered = fullList //Tutte le ricette da firebase

        if (query.isNotEmpty()) {
            filtered = filtered.filter { it.name.contains(query, ignoreCase = true) }
        }

        if (isFollowActive) {
            filtered = filtered.filter { followingIds.contains(it.userID) }
        }

        if (category != "Tutte") {
            filtered = filtered.filter { it.category == category }
        }

        val sorted = when (sortMode) {
            SortMode.TIME_ASC -> filtered.sortedBy { it.cooktime.toIntOrNull() ?: 0 }
            SortMode.TIME_DESC -> filtered.sortedByDescending { it.cooktime.toIntOrNull() ?: 0 }
            SortMode.POPULAR -> filtered.sortedByDescending { it.favoriteCount+it.commentCount }
            SortMode.FOR_YOU -> filtered.sortedByDescending { recipe ->
                var score = 0
                if (followingIds.contains(recipe.userID)) score += 50
                val interestCount = userFeedCount[recipe.category] ?: 0
                score += (interestCount * 5)
                score += (recipe.favoriteCount * 10)
                score += (recipe.commentCount * 8)
                score
            }
        }

        _recipesList.postValue(sorted)
    }

    fun searchRecipes(query: String) {
        applyFilters(query = query)
    }

    // Logica per ricette dell'utente ricicalndo dalla home
    fun loadUserRecipes(userId: String) {
        db.collection("recipes")
            .whereEqualTo("userID", userId)
            .addSnapshotListener { result, e ->
                if (e != null) return@addSnapshotListener
                val list = result?.mapNotNull { doc ->
                    val recipe = doc.toObject(Recipes::class.java)
                    recipe?.apply { id = doc.id }
                } ?: emptyList()
                _userRecipes.postValue(list)
            }
    }

    // Logica per ricette preeferitw dell'utente ricicalndo dalla home
    fun loadFavorites(userId: String) {
        db.collection("users").document(userId).collection("favorites")
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.e("FAV", "Errore listener preferiti: ${e.message}")
                    return@addSnapshotListener
                }

                val favoriteIds = snapshot?.documents?.map { it.id } ?: emptyList()
                if (favoriteIds.isEmpty()) {
                    _favorites.postValue(emptyList())
                    return@addSnapshotListener
                }
                db.collection("recipes")
                    .whereIn(com.google.firebase.firestore.FieldPath.documentId(), favoriteIds)
                    .get()
                    .addOnSuccessListener { result ->
                        val recipes = result.mapNotNull { doc ->
                            val recipe = doc.toObject(Recipes::class.java)
                            recipe?.apply { id = doc.id }
                        }
                        _favorites.postValue(recipes)
                    }
            }
        }

}
