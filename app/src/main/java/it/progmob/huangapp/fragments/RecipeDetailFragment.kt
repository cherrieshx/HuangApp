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

        // Setup della RecyclerView per i commenti
        commentAdapter = CommentAdapter()
        binding.comments.layoutManager = LinearLayoutManager(context)
        binding.comments.adapter = commentAdapter

        // Carica i dati della ricetta corrispodente
        viewModel.loadRecipeData(recipeId)

        // Aggiorna i dati della ricetta
        viewModel.recipe.observe(viewLifecycleOwner) { ricetta ->
            binding.recipeDetail = ricetta
            val currentUid = FirebaseAuth.getInstance().currentUser?.uid
            if (ricetta.userID == currentUid) {
                // Mostra i bottoni di modifica e cancellazione se l'utente è l'autore
                binding.btnEdit.visibility = View.VISIBLE
                binding.btnDelete.visibility = View.VISIBLE

                // Logica del bottone modifica ricetta
                binding.btnEdit.setOnClickListener {
                    val bundle = Bundle().apply { putString("recipeId", ricetta.id) }
                    findNavController().navigate(R.id.action_RecipeDetailFragment_to_NewRecipeFragment, bundle)
                }

                //Logica del bottone elimina ricetta
                binding.btnDelete.setOnClickListener {
                    // Avviso di conferma
                    android.app.AlertDialog.Builder(requireContext())
                        .setTitle("Elimina ricetta")
                        .setMessage("Sei sicuro di voler eliminare questa ricetta?")
                        .setPositiveButton("Elimina") { _, _ ->
                            Firebase.firestore.collection("recipes").document(ricetta.id).delete()
                                .addOnSuccessListener {
                                    Toast.makeText(context, "Ricetta eliminata", Toast.LENGTH_SHORT)
                                        .show()
                                    // Torna alla pagina principale
                                    findNavController().navigate(R.id.action_RecipeDetailFragment_to_HomeFragment)
                                }
                        }
                        .setNegativeButton("Annulla", null)
                        .show()
                }
            }
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
}