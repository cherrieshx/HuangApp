package it.progmob.huangapp.viewmodel

import android.net.Uri
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.storage.FirebaseStorage
import it.progmob.huangapp.ui.data.model.Recipes
import it.progmob.huangapp.ui.data.model.LoggedInUser
import kotlin.collections.remove


class ProfileViewModel : ViewModel() {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()

    private val _username = MutableLiveData<String>()
    val username: LiveData<String> = _username

    private val _userProfile = MutableLiveData<Recipes>()
    val userProfile: LiveData<Recipes> = _userProfile

    private val _followInfo = MutableLiveData<LoggedInUser>()
    val followInfo: LiveData<LoggedInUser> = _followInfo

    val isLoading = MutableLiveData(false)
    private var followingListener: ListenerRegistration? = null

    private var profileListener: ListenerRegistration? = null
    private val _isFollowing = MutableLiveData<Boolean>(false)
    val isFollowing: LiveData<Boolean> = _isFollowing
    private val _usersList = MutableLiveData<List<LoggedInUser>>()
    val usersList: LiveData<List<LoggedInUser>> = _usersList

    fun loadUsersByIds(userIds: List<String>) {
        _usersList.value = emptyList()
        if (userIds.isEmpty()) return
        // Firestore permette fino a 30 ID per query whereIn
        val chunkedIds = userIds.take(30)

        db.collection("users")
            .whereIn(com.google.firebase.firestore.FieldPath.documentId(), chunkedIds)
            .get()
            .addOnSuccessListener { querySnapshot ->
                val loadedUsers = querySnapshot.documents.mapNotNull { doc ->
                    LoggedInUser(
                        userId = doc.id,
                        username = doc.getString("username") ?: "Utente",
                        userImage = doc.getString("userImage") ?: ""
                    )
                }
                _usersList.postValue(loadedUsers)
            }
            .addOnFailureListener {
                Log.e("ProfileVM", "Errore caricamento lista: ${it.message}")
            }
    }

    fun loadUserProfileById(userId: String) {
        // Cancella il listener precedente prima di crearne uno nuovo
        profileListener?.remove()

        profileListener = db.collection("users").document(userId)
            .addSnapshotListener { document, error ->
                if (error != null) {
                    Log.e("ProfileVM", "Errore listener: ${error.message}")
                    return@addSnapshotListener
                }
                if (document != null && document.exists()) {
                    val username = document.getString("username") ?: "Utente"
                    val userImage = document.getString("userImage") ?: ""

                    // Mapping manuale sicuro per evitare l'errore di deserializzazione
                    val followers = (document.get("followers") as? List<*>)
                        ?.mapNotNull { it as? String } ?: emptyList()
                    val following = (document.get("following") as? List<*>)
                        ?.mapNotNull { it as? String } ?: emptyList()

                    Log.d("ProfileVM", "AGGIORNAMENTO: followers=${followers.size} following=${following.size}")

                    _userProfile.postValue(Recipes(username = username, userImage = userImage))
                    _followInfo.postValue(
                        LoggedInUser(
                            userId = userId,
                            username = username,
                            userImage = userImage,
                            followers = followers,
                            following = following
                        )
                    )
                }
            }
    }

    fun checkIfFollowing(authorId: String) {
        val myUid = auth.currentUser?.uid ?: return
        if (myUid == authorId) return

        // Reset immediato per evitare che un valore stale del ViewModel influenzi il click
        _isFollowing.value = false

        followingListener?.remove()
        followingListener = db.collection("users").document(myUid)
            .addSnapshotListener { doc, _ ->
                if (doc != null && doc.exists()) {
                    val following = (doc.get("following") as? List<*>)?.filterIsInstance<String>() ?: emptyList()
                    _isFollowing.postValue(following.contains(authorId))
                }
            }
    }

    fun clickFollow(authorId: String) {
        val myUid = auth.currentUser?.uid ?: return
        val isCurrentlyFollowing = _isFollowing.value ?: false

        val myRef = db.collection("users").document(myUid)
        val authorRef = db.collection("users").document(authorId)
        val batch = db.batch()

        if (isCurrentlyFollowing) {
            // UNFOLLOW
            batch.update(myRef, "following", com.google.firebase.firestore.FieldValue.arrayRemove(authorId))
            batch.update(authorRef, "followers", com.google.firebase.firestore.FieldValue.arrayRemove(myUid))
        } else {
            // FOLLOW
            batch.update(myRef, "following", com.google.firebase.firestore.FieldValue.arrayUnion(authorId))
            batch.update(authorRef, "followers", com.google.firebase.firestore.FieldValue.arrayUnion(myUid))
        }

        // Eseguiamo il commit e aggiorniamo il valore locale immediatamente per evitare il flicker visivo
        batch.commit().addOnSuccessListener {
            _isFollowing.value = !isCurrentlyFollowing
        }.addOnFailureListener { e ->
            Log.e("ClickFollow", "Errore nel commit: ${e.message}")
        }
    }

    fun stopListener() {
        profileListener?.remove()
        profileListener = null
    }

    override fun onCleared() {
        super.onCleared()
        profileListener?.remove()
        followingListener?.remove()
    }

    fun uploadProfileImage(fileUri: Uri, onResult: (String?) -> Unit) {
        val userId = auth.currentUser?.uid ?: return
        val fileRef = storage.reference.child("profile_images/$userId")

        isLoading.value = true

        fileRef.putFile(fileUri)
            .addOnSuccessListener {
                fileRef.downloadUrl.addOnSuccessListener { downloadUri ->
                    val url = downloadUri.toString()
                    // Aggiorna Firestore
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
                }.addOnFailureListener {
                    isLoading.value = false
                    onResult(null)
                }
            }
            .addOnFailureListener {
                isLoading.value = false
                onResult(null)
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