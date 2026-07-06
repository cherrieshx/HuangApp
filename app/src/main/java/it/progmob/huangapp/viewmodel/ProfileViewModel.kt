package it.progmob.huangapp.viewmodel

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import it.progmob.huangapp.ui.data.model.Recipes
import com.google.firebase.firestore.toObject


class ProfileViewModel : ViewModel() {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()

    private val _username = MutableLiveData<String>()
    val username: LiveData<String> = _username

    private val _userProfile = MutableLiveData<Recipes>()
    val userProfile: LiveData<Recipes> = _userProfile
    
    val isLoading = MutableLiveData(false)

    fun loadUserProfile() {
        val userId = auth.currentUser?.uid ?: return
        db.collection("users").document(userId)
            .addSnapshotListener { document, _ ->
                if (document != null && document.exists()) {
                    val username = document.getString("username") ?: "Utente"
                    val userImage = document.getString("userImage") ?: ""
                    _userProfile.postValue(Recipes(username = username, userImage = userImage))
                }
            }
    }
    
    fun uploadProfileImage(fileUri: Uri, onResult: (String?) -> Unit) {
        val userId = auth.currentUser?.uid ?: return
        val fileRef = storage.reference.child("profile_images/$userId")
        
        isLoading.value = true // Mostra il progress bar

        fileRef.putFile(fileUri)
            .addOnSuccessListener {
                fileRef.downloadUrl.addOnSuccessListener { downloadUri ->
                    val url = downloadUri.toString()
                    db.collection("users").document(userId)
                        .update("userImage", url)
                        .addOnSuccessListener { 
                            isLoading.value = false
                            onResult(url) 
                        }
                        .addOnFailureListener { 
                            isLoading.value = false
                            onResult(null) 
                        }
                }
            }
    }

    fun changeUsername(newUsername: String, onSuccess: () -> Unit){
        val userId = auth.currentUser?.uid ?: return

        db.collection("users").document(userId)
            .update("username", newUsername)
            .addOnSuccessListener {
                _username.value = newUsername
                updateUsernameRecipes(userId, newUsername,onSuccess) //Aggiorna anche le ricette dell'autore
            }
    }

    fun updateUsernameRecipes(userId: String, newUsername: String, onComplete: () -> Unit) {
        db.collection("recipes")
            .whereEqualTo("userID", userId)
            .get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    onComplete()
                    return@addOnSuccessListener
                }

                val batch = db.batch() //Mette insieme in un pacchetto gli aggiornamenti
                for (document in documents) {
                    batch.update(db.collection("recipes").document(document.id), "username", newUsername)
                }
                batch.commit()
                    .addOnSuccessListener { onComplete() }
                    .addOnFailureListener { onComplete() }
            }
            .addOnFailureListener { onComplete() }
    }

}