package it.progmob.huangapp.adapter

import android.widget.ImageView
import android.widget.TextView
import androidx.databinding.BindingAdapter
import com.bumptech.glide.Glide
import it.progmob.huangapp.R

// Carica le immagini con Glide inizailizando con immagine di default
@BindingAdapter("imageUrl")
fun loadImage(view: ImageView, url: String?) {
    if (!url.isNullOrEmpty()) {
        Glide.with(view.context)
            .load(url)
            .placeholder(R.drawable.ic_newimage)
            .error(R.drawable.ic_newimage)
            .into(view)
    } else {
        view.setImageResource(R.drawable.ic_newimage)
    }
}

@BindingAdapter("imageProfileUrl")
fun loadProfileImage(view: ImageView, url: String?) {
    if (!url.isNullOrEmpty()) {
        Glide.with(view.context)
            .load(url)
            .placeholder(R.drawable.ic_imageprofile)
            .error(R.drawable.ic_imageprofile)
            .into(view)
    } else {
        view.setImageResource(R.drawable.ic_imageprofile)
    }
}

// Unisce gli elementi della lista andando a capo e aggiungendo il pallino "•"
@BindingAdapter("listaPuntata")
fun listaPuntata(view: TextView, list: List<String>?) {
    if (!list.isNullOrEmpty()) {
        view.text = list.joinToString(separator = "\n\n") { "• $it" }
    } else {
        view.text = ""
    }
}

// Mappa gli elementi della lista al suo indice e li unisce con un a capo /n/n
@BindingAdapter("listaNumerata")
fun listaNumerata(view: TextView, list: List<String>?) {
    if (!list.isNullOrEmpty()) {
        view.text = list.mapIndexed { index, item -> "${index + 1}. $item" }
            .joinToString(separator = "\n\n")
    } else {
        view.text = ""
    }
}
