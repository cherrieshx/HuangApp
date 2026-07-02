package it.progmob.huangapp.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class SignUpViewModel : ViewModel() {
    val email = MutableLiveData("")
    val username = MutableLiveData("")
    val password = MutableLiveData("")
    val password2 = MutableLiveData("")
    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error
    private val _signUpSuccess = MutableLiveData(false)
    val signUpSuccess: LiveData<Boolean> = _signUpSuccess

    fun signUp() {
        val em = email.value.orEmpty()
        val us = username.value.orEmpty()
        val pas = password.value.orEmpty()
        val pas2 = password2.value.orEmpty()

        if (em.isBlank() || us.isBlank() || pas.isBlank() || pas2.isBlank()) {
            _error.value = "Compila tutti i campi"
            return
        }
        if (pas != pas2) {
            _error.value = "Le password non corrispondono"
            return
        }

        FirebaseAuth.getInstance().createUserWithEmailAndPassword(em, pas)
            .addOnSuccessListener { result ->
                val uid = result.user?.uid ?: return@addOnSuccessListener // Ottieni l'ID dell'utente
                FirebaseFirestore.getInstance().collection("users").document(uid)
                    .set(mapOf("username" to us, "email" to em, "userID" to uid)) // Salva i dati dell'utente nel database Firestore
                    .addOnSuccessListener { _signUpSuccess.value = true }
                    .addOnFailureListener { _error.value = it.localizedMessage }
            }
            .addOnFailureListener { _error.value = it.localizedMessage }
    }
}
