package it.progmob.huangapp.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import it.progmob.huangapp.databinding.ProfileItemBinding
import it.progmob.huangapp.ui.data.model.LoggedInUser
import it.progmob.huangapp.viewmodel.ProfileViewModel

class ProfileListAdapter(
    private var users: List<LoggedInUser> = emptyList(),
    private val profileVM: ProfileViewModel, // Aggiunto per gestire la logica dei tasti
    private val onClick: (userId: String) -> Unit = {}
) : RecyclerView.Adapter<ProfileListAdapter.ProfileViewHolder>() {

    class ProfileViewHolder(val binding: ProfileItemBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProfileViewHolder {
        // CORRETTO: Usiamo ProfileItemBinding per la riga della lista
        val binding = ProfileItemBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ProfileViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ProfileViewHolder, position: Int) {
        val user = users[position]
        val currentUid = FirebaseAuth.getInstance().currentUser?.uid

        holder.binding.user = user
        holder.binding.isMe = user.userId == currentUid

        // Recupera la lista di chi seguiamo per impostare correttamente il tasto "Segui/Seguito"
        val followingList = profileVM.followInfo.value?.following ?: emptyList()
        holder.binding.isFollowing = followingList.contains(user.userId)

        // Gestione del click sul tasto Segui direttamente dalla lista
        holder.binding.btnFollow.setOnClickListener {
            profileVM.clickFollow(user.userId)
        }

        holder.itemView.setOnClickListener { onClick(user.userId) }
        holder.binding.executePendingBindings()
    }

    override fun getItemCount(): Int = users.size

    fun updateData(newList: List<LoggedInUser>) {
        this.users = newList
        notifyDataSetChanged()
    }
}