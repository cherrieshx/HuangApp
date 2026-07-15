package it.progmob.huangapp.fragments

import android.graphics.Color
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import it.progmob.huangapp.R
import it.progmob.huangapp.adapter.MyAdapter
import it.progmob.huangapp.ui.data.model.Recipes
import it.progmob.huangapp.viewmodel.HomeViewModel
import it.progmob.huangapp.viewmodel.SortMode
import it.progmob.huangapp.databinding.FragmentHomeBinding
import androidx.navigation.fragment.findNavController
import com.google.firebase.auth.FirebaseAuth

class HomeFragment : Fragment() {
    private val listaRicette = mutableListOf<Recipes>()
    private lateinit var adapter: MyAdapter
    private val HomeVM: HomeViewModel by activityViewModels()
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        binding.homevm = HomeVM
        binding.lifecycleOwner = viewLifecycleOwner
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.rv.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(context)
        adapter = MyAdapter(listaRicette) { ricettaCliccata ->
            val bundle = Bundle().apply {
                putString("recipeId", ricettaCliccata.id)
                putString("recipeName", ricettaCliccata.name)
            }
            findNavController().navigate(R.id.action_HomeFragment_to_RecipeDetailFragment, bundle)
        }
        binding.rv.adapter = adapter

        HomeVM.recipesList.observe(viewLifecycleOwner) { listaDB ->
            if (listaDB != null) adapter.updateData(listaDB)
        }

        // Chip seguiti
        val currentUid = FirebaseAuth.getInstance().currentUser?.uid
        if (currentUid != null) {
            HomeVM.loadFollowingIds(currentUid)
            HomeVM.loadUserInterests(currentUid)
            binding.chipFollowing.visibility = View.VISIBLE
        } else {
            HomeVM.uploadDB()
            binding.chipFollowing.visibility = View.GONE
        }

        binding.chipFollowing.setOnCheckedChangeListener { _, isChecked ->
            HomeVM.toggleFollowingFilter(isChecked)
            if (isChecked) {
                binding.chipFollowing.setChipBackgroundColorResource(R.color.rosso_salmone)
                binding.chipFollowing.setTextColor(Color.WHITE)
            } else {
                binding.chipFollowing.setChipBackgroundColorResource(R.color.white)
                binding.chipFollowing.setTextColor(Color.BLACK)
            }
        }

        // Dropdown categoria
        val categories = arrayOf("Tutte", "Antipasto", "Primo", "Secondo", "Contorno", "Dolce")
        val adapterCategory = object : android.widget.ArrayAdapter<String>(
            requireContext(), android.R.layout.simple_list_item_1, categories
        ) {
            override fun getFilter(): android.widget.Filter {
                return object : android.widget.Filter() {
                    override fun performFiltering(constraint: CharSequence?) = FilterResults().apply {
                        values = categories; count = categories.size
                    }
                    override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
                        notifyDataSetChanged()
                    }
                }
            }
        }
        binding.autoCompleteCategory.setAdapter(adapterCategory)
        binding.autoCompleteCategory.setText(HomeVM.currentCategory, false)
        binding.autoCompleteCategory.setOnItemClickListener { parent, _, position, _ ->
            HomeVM.applyFilters(category = parent.getItemAtPosition(position).toString())
        }

        //Dropdown di ordinamento
        val sortOptions: Array<String>
        val sortModes: Array<SortMode>

        if (currentUid != null) {
            sortOptions = arrayOf("Per te", "Popolari", "Tempo ↑", "Tempo ↓")
            sortModes = arrayOf(SortMode.FOR_YOU, SortMode.POPULAR, SortMode.TIME_ASC, SortMode.TIME_DESC)
        } else {
            sortOptions = arrayOf("Popolari", "Tempo ↑", "Tempo ↓")
            sortModes = arrayOf(SortMode.POPULAR, SortMode.TIME_ASC, SortMode.TIME_DESC)
        }

        val adapterSort = object : android.widget.ArrayAdapter<String>(
            requireContext(), android.R.layout.simple_list_item_1, sortOptions
        ) {
            override fun getFilter(): android.widget.Filter {
                return object : android.widget.Filter() {
                    override fun performFiltering(constraint: CharSequence?) = FilterResults().apply {
                        values = sortOptions; count = sortOptions.size
                    }
                    override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
                        notifyDataSetChanged()
                    }
                }
            }
        }
        binding.autoCompleteSort.setAdapter(adapterSort)

        val currentSortIndex = sortModes.indexOf(HomeVM.currentSortMode)
        val initialIndex = if (currentSortIndex >= 0) currentSortIndex else 0
        binding.autoCompleteSort.setText(sortOptions[initialIndex], false)

        // Forza in popolari se l'utente non è loggato
        if (currentUid == null && HomeVM.currentSortMode == SortMode.FOR_YOU) {
            HomeVM.applyFilters(sortMode = SortMode.POPULAR)
        }

        // Aggiorna la recycleview in base alla selezione dell'utente
        binding.autoCompleteSort.setOnItemClickListener { _, _, position, _ ->
            HomeVM.applyFilters(sortMode = sortModes[position])
        }

        HomeVM.initOrRefresh()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
