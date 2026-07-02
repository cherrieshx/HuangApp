package it.progmob.huangapp.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import it.progmob.huangapp.databinding.CommentRowBinding
import it.progmob.huangapp.ui.data.model.Comment

class CommentAdapter(private var list: List<Comment> = emptyList()) : RecyclerView.Adapter<CommentAdapter.CommentViewHolder>() {

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
        val commentItem = list[position] // Ottieni l'elemento corrente dalla lista
        holder.binding.comment = commentItem
        holder.binding.executePendingBindings() // Aggiorna i dati
    }

    override fun getItemCount(): Int = list.size
    fun updateData(newList: List<Comment>) {
        list = newList
        notifyDataSetChanged() // Notifica l'adapter che i dati sono cambiati
    }
}
