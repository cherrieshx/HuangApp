package it.progmob.huangapp.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import it.progmob.huangapp.databinding.CommentRowBinding
import it.progmob.huangapp.ui.data.model.Comment

class CommentAdapter(
    private var list: List<Comment> = emptyList(),
    private val onDeleteComment: (Comment) -> Unit = {},
    private val onAuthorClick: (userId: String) -> Unit = {}
) : RecyclerView.Adapter<CommentAdapter.CommentViewHolder>() {

    private val currentUserId = FirebaseAuth.getInstance().currentUser?.uid

    class CommentViewHolder(val binding: CommentRowBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CommentViewHolder {
        val binding = CommentRowBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return CommentViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CommentViewHolder, position: Int) {
        val commentItem = list[position]
        holder.binding.comment = commentItem
        
        // Verifica se l'utente loggato è l'autore del commento
        val isAuthor = commentItem.userId == currentUserId
        holder.binding.isAuthor = isAuthor

        // Gestisce il click sul tasto elimina (che aggiungeremo nel layout)
        holder.binding.btnDelete.setOnClickListener {
            onDeleteComment(commentItem)
        }

        // Click su avatar o username → profilo autore del commento
        val goToProfile = {
            commentItem.userId?.let { uid -> onAuthorClick(uid) }
        }
        holder.binding.userComment.setOnClickListener { goToProfile() }
        holder.binding.commentUsername.setOnClickListener { goToProfile() }

        holder.binding.executePendingBindings()
    }

    override fun getItemCount(): Int = list.size

    fun updateData(newList: List<Comment>) {
        list = newList
        notifyDataSetChanged()
    }
}
