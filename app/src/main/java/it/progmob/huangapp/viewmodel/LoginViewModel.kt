package it.progmob.huangapp.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException

class LoginViewModel : ViewModel() {

    val email = MutableLiveData("")
    val password = MutableLiveData("")
    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error
    private val _loginSuccess = MutableLiveData(false)
    val loginSuccess: LiveData<Boolean> = _loginSuccess


    fun login() {
        val em = email.value.orEmpty().trim() // Rimuove spazi bianchi inutili
        val pas = password.value.orEmpty()

        if (em.isBlank() || pas.isBlank()) {
            _error.value = "Compila tutti i campi"
            return
        }

        FirebaseAuth.getInstance().signInWithEmailAndPassword(em, pas)
            .addOnSuccessListener {
                _error.value = null // Resetta l'errore in caso di successo
                _loginSuccess.value = true
            }
            .addOnFailureListener { exception ->
                // Stamopa un messaggio di errore in base all'eccezione
                _error.value = when (exception) {
                    is FirebaseAuthInvalidUserException -> "L'email inserita non corrisponde a nessun account"
                    is FirebaseAuthInvalidCredentialsException -> "Password errata o email non valida"
                    else -> "Errore durante l'accesso: ${exception.localizedMessage}"
                }
            }
    }

    fun forgotPassword() {
        val em = email.value.orEmpty().trim()
        if (em.isBlank()) {
            _error.value = "Inserisci l'email per recuperare la password e riclicca il link"
            return
        }
        FirebaseAuth.getInstance().sendPasswordResetEmail(em)
            .addOnSuccessListener {
                _error.value = "Email di reset inviata con successo!"
            }
            .addOnFailureListener { exception ->
                _error.value = "Errore: ${exception.localizedMessage}"
            }
    }
}