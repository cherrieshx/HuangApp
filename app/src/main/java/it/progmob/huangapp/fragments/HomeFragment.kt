package it.progmob.huangapp.fragments


import android.os.Bundle
import android.widget.Filter
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import it.progmob.huangapp.R
import it.progmob.huangapp.adapter.MyAdapter
import it.progmob.huangapp.ui.data.model.Recipes
import it.progmob.huangapp.viewmodel.HomeViewModel
import it.progmob.huangapp.databinding.FragmentHomeBinding
import androidx.navigation.fragment.findNavController

class HomeFragment : Fragment() {

    private val listaRicette = mutableListOf<Recipes>()
    private lateinit var adapter: MyAdapter
    private val HomeVM: HomeViewModel by activityViewModels()
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
        }
    }


    override fun onCreateView( inflater: LayoutInflater, container: ViewGroup?,savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(
            inflater,
            container,
            false
        )
        binding.homevm = HomeVM
        binding.lifecycleOwner = viewLifecycleOwner
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Il LayoutManager dice alla lista di andare in verticale
        binding.rv.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(context)
        adapter = MyAdapter(listaRicette) { ricettaCliccata -> // Gestisci il click su una ricetta
            val bundle = Bundle()
            bundle.putString("recipeId", ricettaCliccata.id)
            bundle.putString("recipeName", ricettaCliccata.name)

            // Esegue la navigazione al dettaglio della ricetta
            findNavController().navigate(
                R.id.action_HomeFragment_to_RecipeDetailFragment,
                bundle
            )
        }
        binding.rv.adapter = adapter
        HomeVM.recipesList.observe(viewLifecycleOwner) { listaDB ->
            if (listaDB != null) {
                adapter.updateData(listaDB)
            }
        }
        val categories = arrayOf("Tutte", "Antipasto", "Primo", "Secondo", "Contorno", "Dolce")

        //Sovrascrive il metodo getFilter per avere sempre la stessa lista di categorie
        val adapterCategory = object : android.widget.ArrayAdapter<String>(
            requireContext(),
            android.R.layout.simple_list_item_1,
            categories
        ) {
            override fun getFilter(): android.widget.Filter {
                return object : android.widget.Filter() {
                    override fun performFiltering(constraint: CharSequence?) = FilterResults().apply {
                        values = categories
                        count = categories.size
                    }
                    override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
                        notifyDataSetChanged()
                    }
                }
            }
        }
        binding.autoCompleteCategory.setAdapter(adapterCategory)
        binding.autoCompleteCategory.setText(HomeVM.currentCategory, false)

        // Listener per il cambio di categoria
        binding.autoCompleteCategory.setOnItemClickListener { parent, _, position, _ ->
            val selectedCategory = parent.getItemAtPosition(position).toString()
            HomeVM.applyFilters(category = selectedCategory)
        }

        var tempoCrescente = HomeVM.isAscending
        binding.btnSortTime.text = if (tempoCrescente) "Tempo: Crescente" else "Tempo: Decrescente"
        binding.btnSortTime.setOnClickListener {
            tempoCrescente = !tempoCrescente
            binding.btnSortTime.text = if (tempoCrescente) "Tempo: Crescente" else "Tempo: Decrescente"
            HomeVM.applyFilters(ascending = tempoCrescente)
        }

        HomeVM.initOrRefresh()
    }
    override fun onDestroyView() {
        super.onDestroyView()
        // Pulisce il binding per evitare memory leak
        _binding = null
    }
}