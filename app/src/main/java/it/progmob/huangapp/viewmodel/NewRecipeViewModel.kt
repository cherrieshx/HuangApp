package it.progmob.huangapp.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore
import android.net.Uri
import com.google.firebase.storage.FirebaseStorage
import java.util.UUID
import it.progmob.huangapp.ui.data.model.Recipes

class NewRecipeViewModel : ViewModel() {
    val name = MutableLiveData("")
    val description = MutableLiveData("")
    val image = MutableLiveData("")
    private val _errorMsg = MutableLiveData("")
    val errorMsg: LiveData<String> = _errorMsg
    val ingredients = MutableLiveData<MutableList<String>>(mutableListOf(""))
    val steps = MutableLiveData<MutableList<String>>(mutableListOf(""))
    val isLoading = MutableLiveData(false)
    private val db = Firebase.firestore
    private val auth = Firebase.auth
    private val storage = FirebaseStorage.getInstance()
    var isEditing = false
    var recipeIdToEdit: String? = null

    fun loadRecipeForEditing(recipeId: String) {
        isEditing = true
        recipeIdToEdit = recipeId
        
        db.collection("recipes").document(recipeId).get()
            .addOnSuccessListener { document ->
                val recipe = document.toObject(Recipes::class.java)
                if (recipe != null) {
                    name.value = recipe.name
                    description.value = recipe.description
                    image.value = recipe.image
                    ingredients.value = recipe.ingredients.toMutableList()
                    steps.value = recipe.steps.toMutableList()
                }
            }
            .addOnFailureListener {
                _errorMsg.value = "Errore nel caricamento della ricetta: ${it.message}"
            }
    }

    fun uploadImage(fileUri: Uri, onResult: (String?) -> Unit) {
        isLoading.value = true
        image.value = fileUri.toString()
        val fileName = UUID.randomUUID().toString()
        val ref = storage.reference.child("recipes_images/$fileName")

        ref.putFile(fileUri)
            .addOnSuccessListener {
                ref.downloadUrl.addOnSuccessListener { uri ->
                    val url = uri.toString()
                    image.value = url
                    isLoading.value = false
                    onResult(url)
                }
            }
            .addOnFailureListener {
                _errorMsg.value = "Errore caricamento immagine: ${it.message}"
                onResult(null)
            }
    }

    fun addItem(list: MutableLiveData<MutableList<String>>) {
        val current = list.value ?: mutableListOf()

        if (current.isNotEmpty() && current.last().trim().isEmpty()) {
            _errorMsg.value = "Compila la riga precedente prima di aggiungerne una nuova"
            return
        }

        current.add("")
        list.value = current
        _errorMsg.value = ""
    }

    fun removeItem(pos: Int, list: MutableLiveData<MutableList<String>>) {
        list.value?.let {
            if (it.size > 1) {
                it.removeAt(pos)
                list.value = it
            }
        }
    }

    fun updateItem(pos: Int, text: String, list: MutableLiveData<MutableList<String>>) {
        val current = list.value
        if (current != null && pos in current.indices) {
            current[pos] = text
            // Se l'utente scrive, togliamo l'errore
            if (text.isNotBlank() && _errorMsg.value?.startsWith("Compila") == true) {
                _errorMsg.value = ""
            }
        }
    }

    fun saveRecipe(onSuccess: () -> Unit) {
        val user = auth.currentUser ?: return

        val n = name.value ?: ""
        val d = description.value ?: ""
        val img = image.value ?: ""
        val ing = ingredients.value?.filter { it.isNotBlank() } ?: emptyList()
        val stp = steps.value?.filter { it.isNotBlank() } ?: emptyList()

        if (n.isBlank() || d.isBlank() || img.isBlank() || ing.isEmpty() || stp.isEmpty()) {
            _errorMsg.value = "Compila tutti i campi obbligatori"
            return
        }

        db.collection("users").document(user.uid).get()
            .addOnCompleteListener { task ->
                val nameuser = if (task.isSuccessful && task.result?.exists() == true) {
                    task.result?.getString("username") ?: "Anonimo"
                } else {
                    "Anonimo"
                }

                val userImg = if (task.isSuccessful && task.result?.exists() == true) {
                    task.result?.getString("userImage") ?: ""
                } else {
                    ""
                }

                val recipeData = Recipes(
                    id = recipeIdToEdit ?: "",
                    name = n,
                    description = d,
                    image = img,
                    ingredients = ing,
                    steps = stp,
                    userID = user.uid,
                    username = nameuser,
                    userImage = userImg
                )

                if (isEditing && recipeIdToEdit != null) {
                    db.collection("recipes").document(recipeIdToEdit!!).set(recipeData)
                        .addOnSuccessListener { onSuccess() }
                        .addOnFailureListener { _errorMsg.value = "Errore aggiornamento: ${it.message}" }
                } else {
                    db.collection("recipes").add(recipeData)
                        .addOnSuccessListener { onSuccess() }
                        .addOnFailureListener { _errorMsg.value = "Errore salvataggio: ${it.message}" }
                }
            }
    }

}
