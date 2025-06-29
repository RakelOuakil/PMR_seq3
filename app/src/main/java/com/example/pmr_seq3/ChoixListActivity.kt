package com.example.pmr_seq3

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.pmr_seq3.adapters.ListAdapter
import com.example.pmr_seq3.api.ApiClient
import com.example.pmr_seq3.models.ApiModels
import com.example.pmr_seq3.models.AppDatabase
import com.example.pmr_seq3.models.ListEntity
import com.example.pmr_seq3.utils.NetworkUtils
import kotlinx.coroutines.launch

class ChoixListActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var btnAddList: Button
    private lateinit var adapter: ListAdapter
    private val todoLists = mutableListOf<ApiModels.ListResponse>()
    private lateinit var authToken: String
    private lateinit var sharedPrefs: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_choix_list)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        sharedPrefs = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
        
        // Récupérer le token d'authentification
        authToken = sharedPrefs.getString("auth_token", "") ?: ""
        
        if (authToken.isEmpty()) {
            Toast.makeText(this, "Token d'authentification manquant", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        recyclerView = findViewById(R.id.recyclerLists)
        btnAddList = findViewById(R.id.btnAddList)

        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = ListAdapter(todoLists) { position ->
            val intent = Intent(this, ShowListActivity::class.java)
            intent.putExtra("listId", todoLists[position].id)
            intent.putExtra("listName", todoLists[position].label)
            startActivity(intent)
        }
        recyclerView.adapter = adapter

        loadTodoLists()

        btnAddList.setOnClickListener {
            showAddListDialog()
        }
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

    private fun loadTodoLists() {
        lifecycleScope.launch {
            val db = AppDatabase.getInstance(this@ChoixListActivity)
            if (NetworkUtils.isNetworkAvailable(this@ChoixListActivity)) {
                try {
                    val response = ApiClient.todoApiService.getLists(authToken)
                    if (response.isSuccessful) {
                        val listsResponse = response.body()
                        if (listsResponse != null) {
                            val lists = listsResponse.lists ?: emptyList()
                            // Mettre à jour le cache local
                            db.listDao().clearAll()
                            db.listDao().insertLists(lists.map { ListEntity(it.id, it.label, it.user_id) })
                            todoLists.clear()
                            todoLists.addAll(lists)
                            adapter.notifyDataSetChanged()
                            Log.d("ChoixListActivity", "Listes chargées: ${lists.size}")
                        } else {
                            Log.e("ChoixListActivity", "Réponse vide")
                            Toast.makeText(this@ChoixListActivity, "Aucune liste trouvée", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        handleApiError(response.code())
                    }
                } catch (e: Exception) {
                    Log.e("ChoixListActivity", "Erreur réseau", e)
                    Toast.makeText(this@ChoixListActivity, "Erreur réseau: ${e.message}", Toast.LENGTH_SHORT).show()
                    // Fallback : charger depuis Room
                    loadListsFromCache(db)
                }
            } else {
                // Mode offline : charger depuis Room
                loadListsFromCache(db)
            }
        }
    }

    private fun loadListsFromCache(db: AppDatabase) {
        lifecycleScope.launch {
            val cachedLists = db.listDao().getAllLists()
            todoLists.clear()
            todoLists.addAll(cachedLists.map { ApiModels.ListResponse(it.id, it.label, it.user_id) })
            adapter.notifyDataSetChanged()
            Toast.makeText(this@ChoixListActivity, "Mode hors-ligne : données locales", Toast.LENGTH_SHORT).show()
        }
    }

    private fun handleApiError(code: Int) {
        when (code) {
            401 -> {
                Toast.makeText(this@ChoixListActivity, "Token expiré, veuillez vous reconnecter", Toast.LENGTH_SHORT).show()
                val intent = Intent(this@ChoixListActivity, MainActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                startActivity(intent)
                finish()
            }
            else -> {
                Toast.makeText(this@ChoixListActivity, "Erreur lors du chargement des listes: $code", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showAddListDialog() {
        val input = EditText(this)
        AlertDialog.Builder(this)
            .setTitle("Nouvelle liste")
            .setView(input)
            .setPositiveButton("OK") { _, _ ->
                val name = input.text.toString().trim()
                if (name.isNotBlank()) {
                    createNewList(name)
                }
            }
            .setNegativeButton("Annuler", null)
            .show()
    }

    private fun createNewList(name: String) {
        lifecycleScope.launch {
            try {
                Log.d("ChoixListActivity", "Création de liste: $name")
                val response = ApiClient.todoApiService.createList(authToken, name)
                if (response.isSuccessful) {
                    // Recharger les listes pour s'assurer que l'UI est à jour
                    loadTodoLists()
                    Toast.makeText(this@ChoixListActivity, "Liste créée avec succès", Toast.LENGTH_SHORT).show()
                } else {
                    Log.e("ChoixListActivity", "Erreur HTTP lors de la création: ${response.code()}")
                    val errorBody = response.errorBody()?.string()
                    Log.e("ChoixListActivity", "Corps de l'erreur: $errorBody")
                    Toast.makeText(this@ChoixListActivity, "Erreur lors de la création de la liste: ${response.code()}", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e("ChoixListActivity", "Erreur réseau lors de la création", e)
                Toast.makeText(this@ChoixListActivity, "Erreur réseau: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
} 