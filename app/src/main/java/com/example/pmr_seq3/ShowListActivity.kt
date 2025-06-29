package com.example.pmr_seq3

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.pmr_seq3.adapters.ShowListAdapter
import com.example.pmr_seq3.api.ApiClient
import com.example.pmr_seq3.models.ApiModels
import com.example.pmr_seq3.models.AppDatabase
import com.example.pmr_seq3.models.ItemEntity
import com.example.pmr_seq3.utils.NetworkUtils
import kotlinx.coroutines.launch

class ShowListActivity : AppCompatActivity() {

    private var listId: Int = -1
    private lateinit var listName: String
    private lateinit var recyclerView: RecyclerView
    private lateinit var btnAddItem: Button
    private lateinit var adapter: ShowListAdapter
    private val todoItems = mutableListOf<ApiModels.ItemResponse>()
    private lateinit var authToken: String
    private lateinit var sharedPrefs: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_show_list)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        sharedPrefs = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
        
        listId = intent.getIntExtra("listId", -1)
        listName = intent.getStringExtra("listName") ?: "Liste"
        
        if (listId == -1) {
            Toast.makeText(this, "ID de liste invalide", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Récupérer le token d'authentification
        authToken = sharedPrefs.getString("auth_token", "") ?: ""
        
        if (authToken.isEmpty()) {
            Toast.makeText(this, "Token d'authentification manquant", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        recyclerView = findViewById(R.id.recyclerItems)
        btnAddItem = findViewById(R.id.btnAddItem)

        adapter = ShowListAdapter(todoItems) { position, checked ->
            updateItemStatus(todoItems[position].id, checked)
        }

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        loadTodoItems()

        btnAddItem.setOnClickListener {
            promptForNewItem()
        }

        // Appeler syncOfflineChanges() dans onCreate si réseau OK
        syncOfflineChanges()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return when (item.itemId) {
            R.id.action_preferences -> {
                val intent = Intent(this, SettingsActivity::class.java)
                startActivity(intent)
                true
            }
            R.id.action_sync -> {
                if (NetworkUtils.isNetworkAvailable(this)) {
                    syncOfflineChanges()
                    loadTodoItems() // Recharger les données
                } else {
                    Toast.makeText(this, "Pas de connexion réseau", Toast.LENGTH_SHORT).show()
                }
                true
            }
            R.id.action_logout -> {
                // Déconnexion
                sharedPrefs.edit()
                    .remove("auth_token")
                    .remove("user_id")
                    .remove("user_pseudo")
                    .apply()
                Toast.makeText(this, "Déconnexion effectuée", Toast.LENGTH_SHORT).show()
                
                // Retour à l'activité de connexion
                val intent = Intent(this, MainActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                startActivity(intent)
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun loadTodoItems() {
        lifecycleScope.launch {
            val db = AppDatabase.getInstance(this@ShowListActivity)
            // 1. Charger d'abord depuis Room
            val cachedItems = db.itemDao().getItemsForList(listId)
            todoItems.clear()
            todoItems.addAll(cachedItems.map {
                ApiModels.ItemResponse(it.id, it.label, it.url, it.check, it.list_id)
            })
            adapter.notifyDataSetChanged()
            // 2. Si réseau, update Room avec l'API (en fusionnant les changements locaux)
            if (NetworkUtils.isNetworkAvailable(this@ShowListActivity)) {
                try {
                    val response = ApiClient.todoApiService.getItems(authToken, listId)
                    if (response.isSuccessful) {
                        val itemsResponse = response.body()
                        if (itemsResponse != null) {
                            val items = itemsResponse.items ?: emptyList()
                            // Fusionner : si un item local a needsSync, on garde son check local
                            val localChanges = cachedItems.filter { it.needsSync }
                            val mergedItems = items.map { serverItem ->
                                val local = localChanges.find { it.id == serverItem.id }
                                if (local != null) {
                                    ItemEntity(serverItem.id, serverItem.label, serverItem.url, local.check, serverItem.list_id, true)
                                } else {
                                    ItemEntity(serverItem.id, serverItem.label, serverItem.url, serverItem.check, serverItem.list_id, false)
                                }
                            }
                            db.itemDao().clearItemsForList(listId)
                            db.itemDao().insertItems(mergedItems)
                            todoItems.clear()
                            todoItems.addAll(mergedItems.map {
                                ApiModels.ItemResponse(it.id, it.label, it.url, it.check, it.list_id)
                            })
                            adapter.notifyDataSetChanged()
                        }
                    }
                } catch (_: Exception) {}
            }
        }
    }

    private fun handleApiError(code: Int) {
        when (code) {
            401 -> {
                Toast.makeText(this@ShowListActivity, "Token expiré, veuillez vous reconnecter", Toast.LENGTH_SHORT).show()
                val intent = Intent(this@ShowListActivity, MainActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                startActivity(intent)
                finish()
            }
            else -> {
                Toast.makeText(this@ShowListActivity, "Erreur lors du chargement des items: $code", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun promptForNewItem() {
        val editText = EditText(this)
        editText.hint = "Contenu de l'item"
        
        val linkEditText = EditText(this)
        linkEditText.hint = "Lien (optionnel)"
        
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            addView(editText)
            addView(linkEditText)
        }
        
        AlertDialog.Builder(this)
            .setTitle("Nouvel item")
            .setView(layout)
            .setPositiveButton("Ajouter") { _, _ ->
                val content = editText.text.toString().trim()
                val link = linkEditText.text.toString().trim()
                if (content.isNotEmpty()) {
                    createNewItem(content, if (link.isNotEmpty()) link else null)
                }
            }
            .setNegativeButton("Annuler", null)
            .show()
    }

    private fun createNewItem(content: String, link: String?) {
        lifecycleScope.launch {
            try {
                val response = ApiClient.todoApiService.createItem(authToken, listId, content, link)
                if (response.isSuccessful) {
                    val newItem = response.body()
                    if (newItem != null) {
                        todoItems.add(newItem)
                        adapter.notifyItemInserted(todoItems.size - 1)
                        Toast.makeText(this@ShowListActivity, "Item ajouté avec succès", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this@ShowListActivity, "Erreur lors de l'ajout de l'item: ${response.code()}", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@ShowListActivity, "Erreur réseau: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updateItemStatus(itemId: Int, done: Boolean) {
        lifecycleScope.launch {
            val db = AppDatabase.getInstance(this@ShowListActivity)
            // 1. Update Room immédiatement et marquer needsSync
            val cached = db.itemDao().getItemsForList(listId).find { it.id == itemId }
            if (cached != null) {
                val updated = cached.copy(check = if (done) 1 else 0, needsSync = true)
                db.itemDao().updateItem(updated)
            }
            // 2. Update UI
            val index = todoItems.indexOfFirst { it.id == itemId }
            if (index != -1) {
                todoItems[index] = todoItems[index].copy(check = if (done) 1 else 0)
                adapter.notifyItemChanged(index)
            }
            // 3. Si réseau, synchroniser tout de suite
            if (NetworkUtils.isNetworkAvailable(this@ShowListActivity)) {
                syncOfflineChanges()
            }
        }
    }

    private fun syncOfflineChanges() {
        lifecycleScope.launch {
            if (!NetworkUtils.isNetworkAvailable(this@ShowListActivity)) return@launch
            val db = AppDatabase.getInstance(this@ShowListActivity)
            val toSync = db.itemDao().getItemsToSync()
            for (item in toSync) {
                try {
                    val response = ApiClient.todoApiService.updateItem(authToken, item.list_id, item.id, check = item.check)
                    if (response.isSuccessful) {
                        db.itemDao().updateItem(item.copy(needsSync = false))
                    }
                } catch (_: Exception) {}
            }
            // Recharger la liste pour refléter la synchro
            loadTodoItems()
        }
    }

    fun openLink(url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "Impossible d'ouvrir le lien", Toast.LENGTH_SHORT).show()
        }
    }
} 