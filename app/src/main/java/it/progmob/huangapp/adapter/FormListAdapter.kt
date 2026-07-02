package it.progmob.huangapp.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import it.progmob.huangapp.databinding.RowItemBinding
import androidx.core.widget.doOnTextChanged

class FormListAdapter(
    private var items: MutableList<String>,
    private val onRemove: (Int) -> Unit,
    private val onTextChanged: (Int, String) -> Unit
) : RecyclerView.Adapter<FormListAdapter.ViewHolder>() {

    class ViewHolder(val binding: RowItemBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = RowItemBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val editText = holder.binding.editTextItem
        if (editText.text.toString() != items[position]) {
            editText.setText(items[position])
        }

        editText.doOnTextChanged { text, _, _, _ ->
            // Aggiorna il ViewModel solo se il campo ha il focus (l'utente ci sta scrivendo)
            if (editText.hasFocus()) {
                onTextChanged(holder.bindingAdapterPosition, text.toString())
            }
        }
        // Gestisce il click sul bottone rimuovi
        holder.binding.btnRemove.setOnClickListener {
            onRemove(holder.bindingAdapterPosition)
        }
    }

    override fun getItemCount(): Int = items.size

    fun updateData(newList: List<String>) {
        items = newList.toMutableList()
        notifyDataSetChanged() // Notifica l'adapter che i dati sono cambiati
    }
}