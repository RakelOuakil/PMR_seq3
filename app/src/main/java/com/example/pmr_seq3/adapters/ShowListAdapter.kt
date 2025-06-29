package com.example.pmr_seq3.adapters

import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.pmr_seq3.R
import com.example.pmr_seq3.models.ApiModels

class ShowListAdapter(
    private val items: List<ApiModels.ItemResponse>,
    private val onItemCheckedChange: (Int, Boolean) -> Unit
) : RecyclerView.Adapter<ShowListAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val checkBox: CheckBox = view.findViewById(R.id.checkBox)
        val textView: TextView = view.findViewById(R.id.textView)
        val linkTextView: TextView = view.findViewById(R.id.linkTextView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_show_list, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        
        holder.textView.text = item.label
        holder.checkBox.isChecked = item.check == 1
        
        // Supprimer l'ancien listener pour éviter les appels multiples
        holder.checkBox.setOnCheckedChangeListener(null)
        
        // Gérer l'affichage du lien
        if (!item.url.isNullOrEmpty()) {
            holder.linkTextView.visibility = View.VISIBLE
            holder.linkTextView.text = "🔗 Lien"
            holder.linkTextView.setOnClickListener {
                try {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(item.url))
                    holder.itemView.context.startActivity(intent)
                } catch (e: Exception) {
                    // Gérer l'erreur si le lien ne peut pas être ouvert
                }
            }
        } else {
            holder.linkTextView.visibility = View.GONE
        }
        
        // Ajouter le listener après avoir défini l'état initial
        holder.checkBox.setOnCheckedChangeListener { _, isChecked ->
            // Utiliser l'ID de l'item au lieu de la position
            onItemCheckedChange(item.id, isChecked)
        }
        
        // Permettre de cliquer sur l'item pour ouvrir le lien
        holder.itemView.setOnClickListener {
            if (!item.url.isNullOrEmpty()) {
                try {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(item.url))
                    holder.itemView.context.startActivity(intent)
                } catch (e: Exception) {
                    // Gérer l'erreur si le lien ne peut pas être ouvert
                }
            }
        }
    }

    override fun getItemCount() = items.size
} 