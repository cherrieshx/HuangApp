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
import com.google.firebase.auth.FirebaseAuth
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

        // Osserva ricette personali dell'utente
        homeVM.userRecipes.observe(viewLifecycleOwner) { mieRicette ->
            if (mieRicette != null) {
                adapter.updateData(mieRicette)
                if (mieRicette.isEmpty()) {
                    binding.myRecipesTitle.visibility = View.GONE
                } else {
                    binding.myRecipesTitle.visibility = View.VISIBLE
                }
            }
        }

        // Carica ricette dell'utente attuale
        FirebaseAuth.getInstance().currentUser?.uid?.let { uid ->
            homeVM.loadUserRecipes(uid)
        }

        binding.changeImage.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }
         //Indirizza alla pagina di modifica username
        binding.changeUsername.setOnClickListener {
            findNavController().navigate(R.id.action_ProfileFragment_to_ChangeUsernameFragment)
        }

        // Logica per il bottone di logout, dopo il logout torna alla pagina principale
        binding.signOut.setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            val intent = Intent(requireContext(), MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
