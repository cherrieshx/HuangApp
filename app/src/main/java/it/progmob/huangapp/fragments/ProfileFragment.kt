package it.progmob.huangapp.fragments

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.firestore
import it.progmob.huangapp.MainActivity
import it.progmob.huangapp.R
import it.progmob.huangapp.adapter.MyAdapter
import it.progmob.huangapp.databinding.FragmentProfileBinding
import it.progmob.huangapp.viewmodel.HomeViewModel
import it.progmob.huangapp.viewmodel.ProfileViewModel


class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!
    private val profileVM: ProfileViewModel by activityViewModels()
    private val homeVM: HomeViewModel by activityViewModels()
    
    private lateinit var adapter: MyAdapter

    //Come in NewRecipeFragment gestisce la selezione dell'immagine
    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { selectedUri ->
            profileVM.uploadProfileImage(selectedUri) { url ->
                if (url != null) {
                    Toast.makeText(context, "Immagine di profilo caricata!", Toast.LENGTH_SHORT).show()
                }
                else Toast.makeText(context, "Errore durante il caricamento", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        //Carica dati utente
        profileVM.loadUserProfile()

        // Osserva i dati dell'utente per aggiornare l'interfaccia
        profileVM.userProfile.observe(viewLifecycleOwner) { info ->
            binding.userInfo = info
        }

        binding.profileVM = profileVM
        binding.lifecycleOwner = viewLifecycleOwner
        binding.userRecipes.layoutManager = LinearLayoutManager(context)

        adapter = MyAdapter(emptyList()) { ricettaCliccata ->
            val bundle = Bundle().apply {
                putString("recipeId", ricettaCliccata.id)
                putString("recipeName", ricettaCliccata.name)
            }
            findNavController().navigate(R.id.action_ProfileFragment_to_RecipeDetailFragment, bundle)
        }
        binding.userRecipes.adapter = adapter


        homeVM.loadUserRecipes(userId)
        homeVM.loadFavorites(userId)
        homeVM.userRecipes.observe(viewLifecycleOwner) { listaMieRicette ->
            // Aggiorna solo se il tab selezionato è "Le mie Ricette" (indice 0)
            if (binding.tabLayoutProfile.selectedTabPosition == 0) {
                adapter.updateData(listaMieRicette)
                updateEmptyState(listaMieRicette.isEmpty(), isFavorites = false)
            }
        }

        homeVM.favorites.observe(viewLifecycleOwner) { listaPreferiti ->
            // Aggiorna solo se il tab selezionato è "Preferiti" (indice 1)
            if (binding.tabLayoutProfile.selectedTabPosition == 1) {
                adapter.updateData(listaPreferiti)
                updateEmptyState(listaPreferiti.isEmpty(), isFavorites = true)
            }
        }

        binding.tabLayoutProfile.addOnTabSelectedListener(object : com.google.android.material.tabs.TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: com.google.android.material.tabs.TabLayout.Tab?) {
                when (tab?.position) {
                    0 -> {
                        // Switch a Mie Ricette
                        val mie = homeVM.userRecipes.value ?: emptyList()
                        adapter.updateData(mie)
                        updateEmptyState(mie.isEmpty(), isFavorites = false)
                    }
                    1 -> {
                        // Switch a Preferiti
                        val favs = homeVM.favorites.value ?: emptyList()
                        adapter.updateData(favs)
                        updateEmptyState(favs.isEmpty(), isFavorites = true)
                    }
                }
            }
            override fun onTabUnselected(tab: com.google.android.material.tabs.TabLayout.Tab?) {}
            override fun onTabReselected(tab: com.google.android.material.tabs.TabLayout.Tab?) {}
        })


        binding.changeImage.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }
         //Indirizza alla pagina di modifica username
        binding.changeUsername.setOnClickListener {
            findNavController().navigate(R.id.action_ProfileFragment_to_ChangeUsernameFragment)
        }

        // Logica per il bottone di logout, dopo il logout torna alla pagina principale
        binding.signOut.setOnClickListener {
            val userId = FirebaseAuth.getInstance().currentUser?.uid
            if (userId != null) {
                // Rimuove il token dal DB prima di uscire
                Firebase.firestore.collection("users").document(userId)
                    .update("fcmToken", null)
                    .addOnCompleteListener {
                        FirebaseAuth.getInstance().signOut()
                        val intent = Intent(requireContext(), MainActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        startActivity(intent)
                    }
            } else {
                FirebaseAuth.getInstance().signOut()
                val intent = Intent(requireContext(), MainActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
    private fun updateEmptyState(isEmpty: Boolean, isFavorites: Boolean) {
        if (isFavorites) {
            // Siamo nel tab Preferiti
            binding.emptyFavorites.visibility = if (isEmpty) View.VISIBLE else View.GONE
            binding.emptyMyRecipes.visibility = View.GONE
        } else {
            // Siamo nel tab Mie Ricette
            binding.emptyMyRecipes.visibility = if (isEmpty) View.VISIBLE else View.GONE
            binding.emptyFavorites.visibility = View.GONE
        }

        // Nascondi la lista se è vuota per far vedere bene il messaggio
        binding.userRecipes.visibility = if (isEmpty) View.GONE else View.VISIBLE
    }
}
