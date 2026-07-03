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

    private val db = Firebase.firestore

    // SORGENTE DATI UNICA
    private var fullList = listOf<Recipes>()

    // STATO CORRENTE
    private var currentCategory = "Tutte"
    private var currentQuery = ""
    private var isAscending = true

    fun uploadDB() {
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
                    fullList = list // Aggiorniamo la sorgente dati principale
                    applyFilters()  // Applichiamo i filtri attivi sui nuovi dati
                }
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

        // 1. FILTRAGGIO (Ricerca indipendente o Categoria)
        val filtered = if (query.isNotEmpty()) {
            // Se cerchi qualcosa, cerca ovunque (Ricerca Indipendente)
            fullList.filter { it.name.contains(query, ignoreCase = true) }
        } else {
            // Se non cerchi, filtra per categoria
            if (category == "Tutte") fullList
            else fullList.filter { it.category == category }
        }

        // 2. ORDINAMENTO (Sempre applicato)
        val sorted = filtered.let { list ->
            if (ascending) {
                list.sortedBy { it.cooktime.toIntOrNull() ?: 0 }
            } else {
                list.sortedByDescending { it.cooktime.toIntOrNull() ?: 0 }
            }
        }

        _recipesList.postValue(sorted)
    }

    // La ricerca in MainActivity deve chiamare questa
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
}