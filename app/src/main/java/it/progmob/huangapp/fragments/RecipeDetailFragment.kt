package it.progmob.huangapp.fragments

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.firestore
import it.progmob.huangapp.R
import it.progmob.huangapp.adapter.CommentAdapter
import it.progmob.huangapp.databinding.FragmentRecipeDetailBinding
import it.progmob.huangapp.ui.WelcomeActivity
import it.progmob.huangapp.viewmodel.RecipeDetailViewModel
import android.graphics.Color
import androidx.core.text.color
import com.airbnb.lottie.LottieProperty
import com.airbnb.lottie.model.KeyPath
import com.airbnb.lottie.value.LottieValueCallback

class RecipeDetailFragment : Fragment() {
    private val viewModel: RecipeDetailViewModel by activityViewModels()
    private var _binding: FragmentRecipeDetailBinding? = null
    private val binding get() = _binding!!
    private lateinit var commentAdapter: CommentAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRecipeDetailBinding.inflate(inflater, container, false)
        binding.detailVM = viewModel
        binding.lifecycleOwner = viewLifecycleOwner

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Recupera l'ID della ricetta passato dal HomeFragment
        val recipeId = arguments?.getString("recipeId") ?: return

        val goToComments = arguments?.getBoolean("goToComments") ?: false
        if (goToComments) {
            binding.recipeScrollView.postDelayed({
                // Calcola la posizione dei commenti e scrolla dolcemente
                binding.recipeScrollView.smoothScrollTo(0, binding.comments.top + binding.recipeItem.top)
            }, 1200)
        }
        // Setup della RecyclerView per i commenti
        commentAdapter = CommentAdapter(onDeleteComment = { commento ->
            android.app.AlertDialog.Builder(requireContext())
                .setTitle("Elimina commento")
                .setMessage("Vuoi eliminare questo commento?")
                .setPositiveButton("Elimina") { _, _ ->
                    viewModel.deleteComment(recipeId, commento.id)
                }
                .setNegativeButton("Annulla", null)
                .show()
            }
        )
        binding.comments.layoutManager = LinearLayoutManager(context)
        binding.comments.adapter = commentAdapter

        // Carica i dati della ricetta corrispodente
        viewModel.loadRecipeData(recipeId)

        // Aggiorna i dati della ricetta
        viewModel.recipe.observe(viewLifecycleOwner) { ricetta ->
            binding.recipeDetail = ricetta
            val currentUid = FirebaseAuth.getInstance().currentUser?.uid
            if (ricetta?.userID == currentUid) {
                // Mostra i bottoni di modifica e cancellazione se l'utente è l'autore
                binding.btnEdit.visibility = View.VISIBLE
                binding.btnDelete.visibility = View.VISIBLE
                binding.btnFavorite.visibility = View.GONE

                // Logica del bottone modifica ricetta
                binding.btnEdit.setOnClickListener {
                    val bundle = Bundle().apply { putString("recipeId", ricetta?.id) }
                    findNavController().navigate(R.id.action_RecipeDetailFragment_to_NewRecipeFragment, bundle)
                }

                //Logica del bottone elimina ricetta
                binding.btnDelete.setOnClickListener {
                    // Avviso di conferma
                    android.app.AlertDialog.Builder(requireContext())
                        .setTitle("Elimina ricetta")
                        .setMessage("Sei sicuro di voler eliminare questa ricetta?")
                        .setPositiveButton("Elimina") { _, _ ->
                            ricetta?.id?.let { id ->
                                Firebase.firestore.collection("recipes").document(id).delete()
                                    .addOnSuccessListener {
                                        Toast.makeText(context, "Ricetta eliminata", Toast.LENGTH_SHORT).show()
                                        findNavController().navigate(R.id.action_RecipeDetailFragment_to_HomeFragment)
                                    }
                            }
                        }
                        .setNegativeButton("Annulla", null)
                        .show()
                }
            }
        }

        viewModel.checkIfFavorite(recipeId)

        viewModel.isFavorite.observe(viewLifecycleOwner) { isFav ->
            // Imposta il frame iniziale dell'animazione
            if (isFav) {
                setLottieColor(androidx.core.content.ContextCompat.getColor(requireContext(), R.color.rosso_salmone))
                binding.btnFavorite.progress = 0.5f
            } else {
                setLottieColor(androidx.core.content.ContextCompat.getColor(requireContext(), R.color.rosso_salmone))
                binding.btnFavorite.progress = 0.0f
            }
        }

        binding.btnFavorite.setOnClickListener {
            val currentlyFavorite = viewModel.isFavorite.value ?: false

            if (currentlyFavorite) {
                // Animazione per "Togliere"
                binding.btnFavorite.setMinAndMaxFrame(0, 10)
                binding.btnFavorite.speed = -2f // Gira l'animazione al contrario
                binding.btnFavorite.playAnimation()

            } else {
                // Animazione per "Aggiungere"
                binding.btnFavorite.setMinAndMaxFrame(0, 10)
                binding.btnFavorite.speed = 2f
                binding.btnFavorite.playAnimation()

            }

            viewModel.saveFavorite(recipeId)
        }


        // Aggiorna la lista dei commenti quando ne arrivano di nuovi
        viewModel.comments.observe(viewLifecycleOwner) { listaCommenti ->
            commentAdapter.updateData(listaCommenti)
        }

        //Logica del tasto invio commento
        binding.btnSendComment.setOnClickListener {
            val user = FirebaseAuth.getInstance().currentUser

            if (user == null) {
                // Se non è loggato, avvisa e manda alla WelcomeActivity
                Toast.makeText(context, "Accedi per poter commentare", Toast.LENGTH_SHORT).show()
                startActivity(Intent(requireContext(), WelcomeActivity::class.java))
            } else {
                viewModel.sendComment(recipeId) {
                    Toast.makeText(context, "Commento inviato!", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun setLottieColor(color: Int) {
        binding.btnFavorite.addValueCallback(
            KeyPath("**"), // Il simbolo "**" indica di applicare il colore a tutti i livelli del JSON
            LottieProperty.COLOR_FILTER,
            LottieValueCallback(android.graphics.PorterDuffColorFilter(color, android.graphics.PorterDuff.Mode.SRC_ATOP))
        )
    }
}