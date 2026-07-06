package it.progmob.huangapp.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import it.progmob.huangapp.R
import it.progmob.huangapp.adapter.ProfileListAdapter
import it.progmob.huangapp.databinding.ProfileItemListBinding
import it.progmob.huangapp.viewmodel.ProfileViewModel

/**
 * A fragment representing a list of Items.
 */
class ProfileListFragment : Fragment() {    private val profileVM: ProfileViewModel by activityViewModels()

    // CAMBIA QUI: Usa ProfileItemListBinding invece di ProfileItemBinding
    private var _binding: ProfileItemListBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: ProfileListAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        // CAMBIA QUI: Inflate di ProfileItemListBinding
        _binding = ProfileItemListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Imposta il titolo (es. "Followers" o "Seguiti")
        val title = arguments?.getString("title") ?: "Lista Utenti"
        (activity as? AppCompatActivity)?.supportActionBar?.title = title

        adapter = ProfileListAdapter(emptyList(), profileVM) { selectedUserId ->
            val bundle = Bundle().apply { putString("userId", selectedUserId) }
            findNavController().navigate(R.id.ProfileFragment, bundle)
        }

        // Ora binding.list funzionerà correttamente
        binding.list.adapter = adapter

        profileVM.usersList.observe(viewLifecycleOwner) { users ->
            adapter.updateData(users)
        }
        profileVM.followInfo.observe(viewLifecycleOwner) {
            adapter.notifyDataSetChanged() // Forza l'adapter a ricontrollare chi seguiamo
        }

    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}