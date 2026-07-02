package it.progmob.huangapp.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.firestore
import it.progmob.huangapp.ui.data.model.Comment
import it.progmob.huangapp.ui.data.model.Recipes
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class RecipeDetailViewModel : ViewModel() {

    private val db = Firebase.firestore
    private val auth = Firebase.auth
    private val _recipe = MutableLiveData<Recipes>()
    val recipe: LiveData<Recipes> = _recipe
    private val _comments = MutableLiveData<List<Comment>>()
    val comments: LiveData<List<Comment>> = _comments
    val commentText = MutableLiveData("")
    private val _isSending = MutableLiveData(false)
    val isSending: LiveData<Boolean> = _isSending

    fun loadRecipeData(recipeId: String) {
        viewModelScope.launch {
            try {
                val document = db.collection("recipes").document(recipeId).get().await()
                val recipeData = document.toObject(Recipes::class.java) ?: return@launch
                recipeData.id = document.id

                val userDoc = db.collection("users").document(recipeData.userID).get().await()
                _recipe.value = recipeData.copy(
                    id = document.id,
                    username = userDoc.getString("username") ?: recipeData.username,
                    userImage = userDoc.getString("userImage") ?: recipeData.userImage
                )
            } catch (_: Exception) { }
        }

        db.collection("recipes").document(recipeId).collection("comments")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    val rawComments = snapshot.toObjects(Comment::class.java)

                    if (rawComments.isEmpty()) {
                        _comments.value = emptyList()
                        return@addSnapshotListener
                    }

                    val updatedComments = mutableListOf<Comment>()
                    var processedCount = 0

                    rawComments.forEach { comment ->
                        db.collection("users").document(comment.userId).get()
                            .addOnCompleteListener { task ->
                                processedCount++
                                updatedComments.add(comment.copy(
                                    username = if (task.isSuccessful) task.result.getString("username") ?: comment.username else comment.username,
                                    userImage = if (task.isSuccessful) task.result.getString("userImage") ?: comment.userImage else comment.userImage
                                ))
                                if (processedCount == rawComments.size) {
                                    _comments.value = updatedComments.sortedBy { it.timestamp }
                                }
                            }
                    }
                }
            }
    }

    fun sendComment(recipeId: String, onComplete: () -> Unit) {
        if (_isSending.value == true) return // Se sta inviando esce

        val text = commentText.value ?: ""
        val user = auth.currentUser ?: return
        if (text.isBlank()) return

        _isSending.value = true

        db.collection("users").document(user.uid).get()
            .addOnSuccessListener { doc ->
                val username = doc.getString("username") ?: "Anonimo"
                val userImage = doc.getString("userImage") ?: ""

                val newComment = Comment(
                    userId = user.uid,
                    username = username,
                    userImage = userImage,
                    text = text,
                    timestamp = System.currentTimeMillis()
                )

                db.collection("recipes").document(recipeId)
                    .collection("comments").add(newComment)
                    .addOnSuccessListener {
                        commentText.value = ""
                        _isSending.value = false
                        onComplete()
                    }
                    .addOnFailureListener {
                        _isSending.value = false
                    }
            }
            .addOnFailureListener {
                _isSending.value = false
            }
    }
}