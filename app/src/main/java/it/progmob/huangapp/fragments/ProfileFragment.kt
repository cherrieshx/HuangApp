package it.progmob.huangapp.fragments

import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
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
    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { selectedUri ->
            profileVM.uploadProfileImage(selectedUri) { url ->
                if (url != null) Toast.makeText(context, "Immagine di profilo caricata!", Toast.LENGTH_SHORT).show()
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

        val currentUid = FirebaseAuth.getInstance().currentUser?.uid
        val targetUserId = arguments?.getString("userId") ?: currentUid ?: return
        val isOwnProfile = targetUserId == currentUid

        binding.profileVM = profileVM
        binding.lifecycleOwner = viewLifecycleOwner

        // Carica il profilo dell'utente target
        profileVM.loadUserProfileById(targetUserId)

        profileVM.userProfile.observe(viewLifecycleOwner) { info ->
            binding.userInfo = info
            if (!isOwnProfile && info != null) {
                (activity as? AppCompatActivity)?.supportActionBar?.title = info.username
            }
        }

        profileVM.followInfo.observe(viewLifecycleOwner) { info ->
            binding.followInfo = info
        }

        // Mostra i bottoni solo per il profilo dell'utente corrente
        if (isOwnProfile) {
            binding.welcomeText.visibility = View.VISIBLE
            binding.cardSettings.visibility = View.VISIBLE
            binding.cardSignOut.visibility = View.VISIBLE
            binding.btnFollow.visibility = View.GONE
            binding.tabLayoutProfile.getTabAt(1)?.view?.visibility = View.VISIBLE
        } else { //Nasconde bottoni per altri profili
            binding.welcomeText.visibility = View.GONE
            binding.cardSettings.visibility = View.GONE
            binding.cardSignOut.visibility = View.GONE
            binding.tabLayoutProfile.getTabAt(1)?.view?.visibility = View.GONE

            profileVM.checkIfFollowing(targetUserId)
            binding.btnFollow.visibility = View.VISIBLE
            binding.btnFollow.setOnClickListener {
                val user = FirebaseAuth.getInstance().currentUser
                if (user == null) {
                    Toast.makeText(context, "Accedi per poter seguire", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(requireContext(), MainActivity::class.java))
                    return@setOnClickListener
                }
                profileVM.clickFollow(targetUserId)
            }
        }

        // Configurazione del bottone segui/seguito
        profileVM.isFollowing.observe(viewLifecycleOwner) { isFollowing ->
            if (isFollowing) {
                binding.btnFollow.text = "Seguito"
                binding.btnFollow.setTextColor(Color.GRAY)
                binding.btnFollow.setStrokeColorResource(android.R.color.darker_gray)
                binding.btnFollow.backgroundTintList = androidx.core.content.ContextCompat.getColorStateList(requireContext(), android.R.color.transparent)
            } else {
                binding.btnFollow.text = "Segui"
                binding.btnFollow.setTextColor(Color.WHITE)
                binding.btnFollow.setStrokeColorResource(R.color.rosso_salmone)
                binding.btnFollow.backgroundTintList = androidx.core.content.ContextCompat.getColorStateList(requireContext(), R.color.rosso_salmone)
            }
        }

        // Click su Followers
        val openFollowers = {
            val ids = profileVM.followInfo.value?.followers ?: emptyList()
            profileVM.loadUsersByIds(ids)
            val bundle = Bundle().apply { putString("title", "Followers") }
            findNavController().navigate(R.id.action_ProfileFragment_to_ProfileListFragment, bundle)
        }

        // Click su Seguiti
        val openFollowing = {
            val ids = profileVM.followInfo.value?.following ?: emptyList()
            profileVM.loadUsersByIds(ids)
            val bundle = Bundle().apply { putString("title", "Seguiti") }
            findNavController().navigate(R.id.action_ProfileFragment_to_ProfileListFragment, bundle)
        }
        binding.layoutFollowers.setOnClickListener { openFollowers() }
        binding.layoutFollowing.setOnClickListener { openFollowing() }

        // Setup RecyclerView ricette
        binding.userRecipes.layoutManager = LinearLayoutManager(context)
        adapter = MyAdapter(emptyList()) { ricettaCliccata ->
            val bundle = Bundle().apply {
                putString("recipeId", ricettaCliccata.id)
                putString("recipeName", ricettaCliccata.name)
            }
            findNavController().navigate(R.id.action_ProfileFragment_to_RecipeDetailFragment, bundle)
        }
        binding.userRecipes.adapter = adapter

        homeVM.loadUserRecipes(targetUserId)
        if (isOwnProfile) homeVM.loadFavorites(targetUserId)

        homeVM.userRecipes.observe(viewLifecycleOwner) { listaMieRicette ->
            if (binding.tabLayoutProfile.selectedTabPosition == 0) {
                adapter.updateData(listaMieRicette ?: emptyList())
                updateEmptyState(listaMieRicette?.isEmpty() ?: true, isFavorites = false)
            }
        }

        homeVM.favorites.observe(viewLifecycleOwner) { listaPreferiti ->
            if (binding.tabLayoutProfile.selectedTabPosition == 1) {
                adapter.updateData(listaPreferiti ?: emptyList())
                updateEmptyState(listaPreferiti?.isEmpty() ?: true, isFavorites = true)
            }
        }

        // Gestisce il cambio di tab nel TabLayout
        binding.tabLayoutProfile.addOnTabSelectedListener(object : com.google.android.material.tabs.TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: com.google.android.material.tabs.TabLayout.Tab?) {
                when (tab?.position) {
                    0 -> {
                        val mie = homeVM.userRecipes.value ?: emptyList()
                        adapter.updateData(mie)
                        updateEmptyState(mie.isEmpty(), isFavorites = false)
                    }
                    1 -> {
                        val favs = homeVM.favorites.value ?: emptyList()
                        adapter.updateData(favs)
                        updateEmptyState(favs.isEmpty(), isFavorites = true)
                    }
                }
            }
            override fun onTabUnselected(tab: com.google.android.material.tabs.TabLayout.Tab?) {}
            override fun onTabReselected(tab: com.google.android.material.tabs.TabLayout.Tab?) {}
        })

        // Azioni solo per il proprio profilo
        if (isOwnProfile) {
            binding.changeImage.setOnClickListener {
                pickImageLauncher.launch("image/*")
            }
            binding.changeUsername.setOnClickListener {
                findNavController().navigate(R.id.action_ProfileFragment_to_ChangeUsernameFragment)
            }
            binding.signOut.setOnClickListener {
                val uid = FirebaseAuth.getInstance().currentUser?.uid
                if (uid != null) {
                    Firebase.firestore.collection("users").document(uid)
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
    }

    // Aggiorna il profilo ogni volta che torna alla pagina
    override fun onResume() {
        super.onResume()
        val currentUid = FirebaseAuth.getInstance().currentUser?.uid
        val targetUserId = arguments?.getString("userId") ?: currentUid ?: return
        profileVM.loadUserProfileById(targetUserId)
    }

    override fun onPause() {
        super.onPause()
        profileVM.stopListener()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun updateEmptyState(isEmpty: Boolean, isFavorites: Boolean) {
        if (isFavorites) {
            binding.emptyFavorites.visibility = if (isEmpty) View.VISIBLE else View.GONE
            binding.emptyMyRecipes.visibility = View.GONE
        } else {
            binding.emptyMyRecipes.visibility = if (isEmpty) View.VISIBLE else View.GONE
            binding.emptyFavorites.visibility = View.GONE
        }
        binding.userRecipes.visibility = if (isEmpty) View.GONE else View.VISIBLE
    }
}
