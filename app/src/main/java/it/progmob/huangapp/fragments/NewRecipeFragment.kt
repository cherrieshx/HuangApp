package it.progmob.huangapp.fragments

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import it.progmob.huangapp.R
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import it.progmob.huangapp.adapter.FormListAdapter
import it.progmob.huangapp.databinding.FragmentNewRecipeBinding
import it.progmob.huangapp.viewmodel.NewRecipeViewModel

class NewRecipeFragment : Fragment() {

    private val viewModel: NewRecipeViewModel by viewModels()
    private var _binding: FragmentNewRecipeBinding? = null
    private val binding get() = _binding!!

    // Gestisce la selezione dell'immagine
    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            viewModel.uploadImage(it) { url ->
                if (url != null) {
                    Toast.makeText(context, "Immagine caricata!", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNewRecipeBinding.inflate(inflater, container, false)
        binding.newRecipeVM = viewModel
        binding.lifecycleOwner = viewLifecycleOwner
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val recipeId = arguments?.getString("recipeId")
        if (recipeId != null) { //Carica dati per la modifica
            viewModel.loadRecipeEdit(recipeId)
            binding.btnSave.text = "Salva modifiche"
        }

        // Bottone per scegliere l'immagine da galleria
        binding.btnEditImage.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }

        //Configurazione selezione per la categoria
        val categorie = arrayOf("Seleziona categoria", "Antipasto", "Primo", "Secondo", "Contorno", "Dolce")
        val adapter =
            ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, categorie)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerCategory.adapter = adapter

        binding.spinnerCategory.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (position > 0) {
                    viewModel.category.value = categorie[position]
                } else {
                    viewModel.category.value = "" // Ritorna vuoto se seleziona l'istruzione
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // Se stiamo modificando una ricetta, impostiamo lo spinner sul valore corretto
        viewModel.category.observe(viewLifecycleOwner) { currentCat ->
            val index = categorie.indexOf(currentCat)
            if (index >= 0) {
                binding.spinnerCategory.setSelection(index)
            }
        }

        // Configurazione per ingredienti
        val ingAdapter = FormListAdapter(
            viewModel.ingredients.value!!,
            onRemove = { pos -> viewModel.removeItem(pos, viewModel.ingredients) },
            onTextChanged = { pos, text -> viewModel.updateItem(pos, text, viewModel.ingredients) }
        )
        binding.rvIngredients.layoutManager = LinearLayoutManager(context)
        binding.rvIngredients.adapter = ingAdapter
        viewModel.ingredients.observe(viewLifecycleOwner) { lista ->
            binding.rvIngredients.post {
                ingAdapter.updateData(lista)
            }
        }

        // Configurazione per i passaggi
        val stepsAdapter = FormListAdapter(
            viewModel.steps.value!!,
            onRemove = { pos -> viewModel.removeItem(pos, viewModel.steps) },
            onTextChanged = { pos, text -> viewModel.updateItem(pos, text, viewModel.steps) }
        )
        binding.rvSteps.layoutManager = LinearLayoutManager(context)
        binding.rvSteps.adapter = stepsAdapter
        viewModel.steps.observe(viewLifecycleOwner) { lista ->
            binding.rvSteps.post {
                stepsAdapter.updateData(lista)
            }
        }

        // Logica per il bottone "+" per ingredienti e passaggi
        binding.btnAddIngredient.setOnClickListener { viewModel.addItem(viewModel.ingredients) }
        binding.btnAddStep.setOnClickListener { viewModel.addItem(viewModel.steps) }

        //  Logica per il bottone di salvataggio
        binding.btnSave.setOnClickListener {
            viewModel.saveRecipe {
                Toast.makeText(context, "Ricetta pubblicata!", Toast.LENGTH_SHORT).show()
                findNavController().navigate(R.id.action_NewRecipeFragment_to_HomeFragment)// Torna indietro alla lista delle ricette
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}