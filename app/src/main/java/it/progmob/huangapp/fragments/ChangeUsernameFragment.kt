package it.progmob.huangapp.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import it.progmob.huangapp.databinding.FragmentChangeUsernameBinding
import it.progmob.huangapp.viewmodel.ProfileViewModel

class ChangeUsernameFragment : Fragment() {
    private lateinit var binding: FragmentChangeUsernameBinding
    private val profileVM: ProfileViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = FragmentChangeUsernameBinding.inflate(
            inflater,
            container,
            false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Collega il click del bottone "Salva"
        binding.saveUsernameButton.setOnClickListener {
            val newUsername = binding.editUsername.text.toString()

            if (newUsername.isNotBlank()) {
                profileVM.changeUsername(newUsername) {
                    Toast.makeText(context, "Username aggiornato!", Toast.LENGTH_SHORT)
                        .show()
                    // Torna indietro al profilo
                    findNavController().popBackStack()
                }
            } else {
                Toast.makeText(context, "Inserisci un username valido", Toast.LENGTH_SHORT).show()
            }
        }
    }
}