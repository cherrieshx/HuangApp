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
import com.airbnb.lottie.LottieProperty
import com.airbnb.lottie.model.KeyPath
import com.airbnb.lottie.value.LottieValueCallback
import it.progmob.huangapp.viewmodel.ProfileViewModel

class RecipeDetailFragment : Fragment() {
    private val viewModel: RecipeDetailViewModel by activityViewModels()
    private val profileVM: ProfileViewModel by activityViewModels()
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

        val recipeId = arguments?.getString("recipeId") ?: return

        val goToComments = arguments?.getBoolean("goToComments") ?: false
        if (goToComments) {
            binding.recipeScrollView.postDelayed({
                binding.recipeScrollView.smoothScrollTo(0, binding.comments.top + binding.recipeItem.top)
            }, 1200)
        }

        commentAdapter = CommentAdapter(
            onDeleteComment = { commento ->
                android.app.AlertDialog.Builder(requireContext())
                    .setTitle("Elimina commento")
                    .setMessage("Vuoi eliminare questo commento?")
                    .setPositiveButton("Elimina") { _, _ ->
                        viewModel.deleteComment(recipeId, commento.id)
                    }
                    .setNegativeButton("Annulla", null)
                    .show()
            },
            onAuthorClick = { userId ->
                val bundle = Bundle().apply { putString("userId", userId) }
                findNavController().navigate(R.id.action_RecipeDetailFragment_to_ProfileFragment, bundle)
            }
        )


        viewModel.loadRecipeData(recipeId)

        // Click sull'autore → naviga al suo profilo
        val goToAuthorProfile = {
            val authorId = viewModel.recipe.value?.userID
            if (authorId != null) {
                val bundle = Bundle().apply { putString("userId", authorId) }
                findNavController().navigate(R.id.action_RecipeDetailFragment_to_ProfileFragment, bundle)
            }
        }
        binding.authorImage.setOnClickListener { goToAuthorProfile() }
        binding.text.setOnClickListener { goToAuthorProfile() }

        // authorId corrente — aggiornato dall'observer ma il click listener è registrato una volta sola
        var currentAuthorId: String? = null
        binding.btnFollow.setOnClickListener {
            currentAuthorId?.let { profileVM.clickFollow(it) }
        }

        // Unico observer per recipe: gestisce visibilità bottoni in base all'autore
        viewModel.recipe.observe(viewLifecycleOwner) { ricetta ->
            binding.recipeDetail = ricetta
            val currentUid = FirebaseAuth.getInstance().currentUser?.uid
            val authorId = ricetta?.userID ?: return@observe

            if (authorId == currentUid) {
                // Ricetta mia: modifica/elimina visibili, follow e preferiti nascosti
                binding.btnEdit.visibility = View.VISIBLE
                binding.btnDelete.visibility = View.VISIBLE
                binding.btnFavorite.visibility = View.GONE
                binding.btnFollow.visibility = View.GONE

                binding.btnEdit.setOnClickListener {
                    val bundle = Bundle().apply { putString("recipeId", ricetta.id) }
                    findNavController().navigate(R.id.action_RecipeDetailFragment_to_NewRecipeFragment, bundle)
                }
                binding.btnDelete.setOnClickListener {
                    android.app.AlertDialog.Builder(requireContext())
                        .setTitle("Elimina ricetta")
                        .setMessage("Sei sicuro di voler eliminare questa ricetta?")
                        .setPositiveButton("Elimina") { _, _ ->
                            ricetta.id?.let { id ->
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
            } else {
                // Ricetta altrui: follow e preferiti visibili, modifica/elimina nascosti
                binding.btnEdit.visibility = View.GONE
                binding.btnDelete.visibility = View.GONE
                binding.btnFavorite.visibility = View.VISIBLE
                binding.btnFollow.visibility = View.VISIBLE
                currentAuthorId = authorId
                profileVM.checkIfFollowing(authorId)
            }
        }

        viewModel.checkIfFavorite(recipeId)
        viewModel.isFavorite.observe(viewLifecycleOwner) { isFav ->
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
                binding.btnFavorite.setMinAndMaxFrame(0, 10)
                binding.btnFavorite.speed = -2f
                binding.btnFavorite.playAnimation()
            } else {
                binding.btnFavorite.setMinAndMaxFrame(0, 10)
                binding.btnFavorite.speed = 2f
                binding.btnFavorite.playAnimation()
            }
            viewModel.saveFavorite(recipeId)
        }
            profileVM.isFollowing.observe(viewLifecycleOwner) { isFollowing ->
            if (isFollowing) {
                binding.btnFollow.text = "Seguito"
                binding.btnFollow.setTextColor(Color.GRAY)
                binding.btnFollow.setBackgroundTintList(androidx.core.content.ContextCompat.getColorStateList(requireContext(), android.R.color.transparent))
                binding.btnFollow.setStrokeColorResource(android.R.color.darker_gray)
            } else {
                binding.btnFollow.text = "Segui"
                binding.btnFollow.setTextColor(androidx.core.content.ContextCompat.getColor(requireContext(), R.color.white))
                binding.btnFollow.setBackgroundTintList(androidx.core.content.ContextCompat.getColorStateList(requireContext(), R.color.rosso_salmone))
                binding.btnFollow.setStrokeColorResource(R.color.rosso_salmone)
            }
        }

        binding.comments.layoutManager = LinearLayoutManager(context)
        binding.comments.adapter = commentAdapter
        viewModel.comments.observe(viewLifecycleOwner) { listaCommenti ->
            commentAdapter.updateData(listaCommenti)
        }

        binding.btnSendComment.setOnClickListener {
            val user = FirebaseAuth.getInstance().currentUser
            if (user == null) {
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