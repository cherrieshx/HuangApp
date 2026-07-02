package it.progmob.huangapp.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.databinding.DataBindingUtil
import it.progmob.huangapp.MainActivity
import it.progmob.huangapp.R
import it.progmob.huangapp.databinding.ActivityLoginBinding
import it.progmob.huangapp.viewmodel.LoginViewModel

class LoginActivity : AppCompatActivity() {
    private val viewModel: LoginViewModel by viewModels()
    private lateinit var binding: ActivityLoginBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        binding = DataBindingUtil.setContentView(this, R.layout.activity_login)
        binding.vm = viewModel
        binding.lifecycleOwner = this
        // Gestisce l'invio del form di login
        ViewCompat.setOnApplyWindowInsetsListener(binding.loginRoot) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Gestisce l'errore
        viewModel.error.observe(this) { msg ->
            if (msg != null) {
                binding.error.text = msg
                binding.error.visibility = View.VISIBLE
            } else {
                binding.error.visibility = View.GONE
            }
        }

        viewModel.loginSuccess.observe(this) { success ->
            if (success) { // Se il login è andato a buon fine vai alla MainActivity
                startActivity(Intent(this, MainActivity::class.java))
                finish()
            }
        }
    }
}
