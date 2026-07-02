package it.progmob.huangapp.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import it.progmob.huangapp.databinding.RecipeItemBinding
import it.progmob.huangapp.ui.data.model.Recipes

class MyAdapter(var list: List<Recipes>, private val onClick:(Recipes) -> Unit ) :
    RecyclerView.Adapter<MyAdapter.MyViewHolder>() {

    class MyViewHolder(val binding: RecipeItemBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val binding = RecipeItemBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return MyViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int
    ) {
        val recipeItem = list[position]
        holder.binding.recipe = recipeItem
        holder.binding.executePendingBindings() //Aggiorna i dati
        holder.itemView.setOnClickListener {
            onClick(recipeItem)
        }
    }

    override fun getItemCount(): Int = list.size
    fun updateData(newList: List<Recipes>) {
        list = newList
        notifyDataSetChanged()
    }
}