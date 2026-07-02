package it.progmob.huangapp.ui

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import com.google.android.gms.common.SignInButton
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import androidx.lifecycle.lifecycleScope
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore
import it.progmob.huangapp.MainActivity
import it.progmob.huangapp.R
import kotlinx.coroutines.launch

class WelcomeActivity : AppCompatActivity() {
    private val auth: FirebaseAuth = Firebase.auth
    private val db = Firebase.firestore
    private lateinit var credentialManager: CredentialManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_welcome)
        enableEdgeToEdge()

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.welcomeRoot)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        credentialManager = CredentialManager.create(this)

        findViewById<Button>(R.id.loginButton).setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
        }
        findViewById<Button>(R.id.signupButton).setOnClickListener {
            startActivity(Intent(this, SignUpActivity::class.java))
        }
        findViewById<SignInButton>(R.id.googleSignInButton).setOnClickListener {
            signInWithGoogle()
        }
    }

    private fun signInWithGoogle() {
        val googleIdOption = GetGoogleIdOption.Builder()
            .setServerClientId(getString(R.string.default_web_client_id))
            .setFilterByAuthorizedAccounts(false) // false = mostra tutti gli account Google
            .build()

        // Crea una richiesta di credenziali per Google ID
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        lifecycleScope.launch {
            try {
                val result = credentialManager.getCredential(this@WelcomeActivity, request)
                val credential = result.credential

                // Verifica il tipo di credenziale e passa l'ID Token al metodo di autenticazione
                if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data) // Crea un oggetto GoogleIdTokenCredential
                    firebaseAuthWithGoogle(googleIdTokenCredential.idToken)// passa l'ID Token al metodo di autenticazione
                } else {
                    Toast.makeText(this@WelcomeActivity, "Tipo di credenziale non supportato", Toast.LENGTH_SHORT).show()
                }
            } catch (e: GetCredentialException) {
                Log.e("GoogleSignIn", "Errore: ${e.message}")
                Toast.makeText(this@WelcomeActivity, "Accesso Google non riuscito", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential).addOnCompleteListener(this) { task ->
            if (task.isSuccessful) {
                val user = auth.currentUser!!
                // Crea profilo Firestore solo al primo accesso
                db.collection("users").document(user.uid).get()
                    .addOnSuccessListener { doc ->
                        if (!doc.exists()) {
                            db.collection("users").document(user.uid).set(
                                mapOf(
                                    "username" to (user.displayName ?: "Utente"),
                                    "email" to (user.email ?: ""),
                                    "userID" to user.uid,
                                    "userImage" to (user.photoUrl?.toString() ?: "")
                                )
                            )
                        }
                        startActivity(Intent(this, MainActivity::class.java))
                        finish()
                    }
            } else {
                Toast.makeText(this, "Autenticazione fallita: ${task.exception?.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
}
