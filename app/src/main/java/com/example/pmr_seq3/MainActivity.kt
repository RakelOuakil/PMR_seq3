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
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.lifecycleScope
import com.example.pmr_seq3.api.ApiClient
import com.example.pmr_seq3.utils.NetworkUtils
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var etPseudo: EditText
    private lateinit var etPassword: EditText
    private lateinit var btnOk: Button
    private lateinit var btnTestApi: Button
    private lateinit var btnOffline: Button
    private lateinit var sharedPrefs: SharedPreferences
    private val TAG = "MainActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        etPseudo = findViewById(R.id.etPseudo)
        etPassword = findViewById(R.id.etPassword)
        btnOk = findViewById(R.id.btnOk)
        btnTestApi = findViewById(R.id.btnTestApi)
        btnOffline = findViewById(R.id.btnOffline)
        sharedPrefs = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        // Vérifier la connexion automatique AVANT de vérifier le réseau
        val autoLogin = sharedPrefs.getBoolean("auto_login", false)
        val savedToken = sharedPrefs.getString("auth_token", null)
        
        if (autoLogin && !savedToken.isNullOrEmpty()) {
            Log.d(TAG, "Connexion automatique avec token existant")
            // Connexion automatique même hors ligne
            val intent = Intent(this, ChoixListActivity::class.java)
            startActivity(intent)
            finish()
            return
        }

        // Vérifier la connectivité réseau seulement si pas de token valide
        if (!NetworkUtils.isNetworkAvailable(this)) {
            Log.e(TAG, "Aucune connexion réseau disponible")
            
            // Vérifier s'il y a des données locales pour permettre l'accès hors ligne
            if (!savedToken.isNullOrEmpty()) {
                Log.d(TAG, "Token disponible - Accès hors ligne autorisé")
                Toast.makeText(this, "Mode hors ligne - Utilisation des données locales", Toast.LENGTH_LONG).show()
                val intent = Intent(this, ChoixListActivity::class.java)
                startActivity(intent)
                finish()
                return
            } else {
                Toast.makeText(this, "Mode hors ligne - Connectez-vous d'abord avec le réseau", Toast.LENGTH_LONG).show()
                btnOk.isEnabled = false
                btnTestApi.isEnabled = false
            }
        } else {
            Log.d(TAG, "Connexion réseau disponible")
            // Test de connectivité
            testNetworkConnection()
        }

        // Pré-remplir avec le dernier pseudo et mot de passe
        val lastPseudo = sharedPrefs.getString("last_pseudo", "")
        val lastPassword = sharedPrefs.getString("last_password", "")
        etPseudo.setText(lastPseudo)
        etPassword.setText(lastPassword)

        btnOk.setOnClickListener {
            if (!NetworkUtils.isNetworkAvailable(this)) {
                Toast.makeText(this, "Connexion impossible hors-ligne", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val pseudo = etPseudo.text.toString().trim()
            val password = etPassword.text.toString().trim()
            if (pseudo.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Veuillez entrer un pseudo et un mot de passe", Toast.LENGTH_SHORT).show()
            } else {
                Log.d(TAG, "Tentative de connexion pour: $pseudo")
                Log.d(TAG, "URL de l'API: ${ApiClient.getCurrentBaseUrl()}")
                authenticateUser(pseudo, password)
            }
        }

        btnTestApi.setOnClickListener {
            testApiConnection()
        }

        btnOffline.setOnClickListener {
            val savedToken = sharedPrefs.getString("auth_token", null)
            if (!savedToken.isNullOrEmpty()) {
                Log.d(TAG, "Accès hors ligne demandé")
                Toast.makeText(this, "Mode hors ligne - Utilisation des données locales", Toast.LENGTH_SHORT).show()
                val intent = Intent(this, ChoixListActivity::class.java)
                startActivity(intent)
                finish()
            } else {
                Toast.makeText(this, "Aucune donnée locale disponible. Connectez-vous d'abord.", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun testNetworkConnection() {
        Log.d(TAG, "Test de connectivité réseau...")
        lifecycleScope.launch {
            try {
                val response = ApiClient.testApiService.testConnection()
                Log.d(TAG, "Test de connectivité réussi: ${response.code()}")
                Toast.makeText(this@MainActivity, "Connectivité OK", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Log.e(TAG, "Test de connectivité échoué", e)
                Toast.makeText(this@MainActivity, "Problème de connectivité: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun authenticateUser(pseudo: String, password: String) {
        Log.d(TAG, "Début de l'authentification avec query parameters")
        
        lifecycleScope.launch {
            try {
                val response = ApiClient.todoApiService.authenticate(pseudo, password)
                Log.d(TAG, "Réponse reçue: ${response.code()}")
                
                if (response.isSuccessful) {
                    val authResponse = response.body()
                    if (authResponse != null) {
                        // Extraire le token (peut être dans 'token' ou 'hash')
                        val token = authResponse.token ?: authResponse.hash
                        if (!token.isNullOrEmpty()) {
                            Log.d(TAG, "Connexion réussie, token: $token")
                            // Sauvegarder le token et les informations utilisateur
                            sharedPrefs.edit()
                                .putString("auth_token", token)
                                .putString("last_pseudo", pseudo)
                                .putString("last_password", password)
                                .apply()

                            Toast.makeText(this@MainActivity, "Connexion réussie", Toast.LENGTH_SHORT).show()
                            
                            // Aller à l'activité suivante
                            val intent = Intent(this@MainActivity, ChoixListActivity::class.java)
                            startActivity(intent)
                            finish()
                        } else {
                            Log.e(TAG, "Token vide dans la réponse")
                            Toast.makeText(this@MainActivity, "Token vide du serveur", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Log.e(TAG, "Réponse vide")
                        Toast.makeText(this@MainActivity, "Réponse vide du serveur", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Log.e(TAG, "Erreur HTTP: ${response.code()}")
                    
                    // Essayer de lire le message d'erreur
                    try {
                        val errorBody = response.errorBody()?.string()
                        Log.e(TAG, "Corps de l'erreur: $errorBody")
                        
                        when (response.code()) {
                            400 -> Toast.makeText(this@MainActivity, "Requête incorrecte - vérifiez les identifiants", Toast.LENGTH_LONG).show()
                            401 -> Toast.makeText(this@MainActivity, "Identifiants incorrects", Toast.LENGTH_SHORT).show()
                            500 -> Toast.makeText(this@MainActivity, "Erreur serveur", Toast.LENGTH_SHORT).show()
                            else -> Toast.makeText(this@MainActivity, "Erreur de connexion: ${response.code()}", Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Erreur lors du parsing de l'erreur", e)
                        when (response.code()) {
                            400 -> Toast.makeText(this@MainActivity, "Requête incorrecte", Toast.LENGTH_SHORT).show()
                            401 -> Toast.makeText(this@MainActivity, "Identifiants incorrects", Toast.LENGTH_SHORT).show()
                            500 -> Toast.makeText(this@MainActivity, "Erreur serveur", Toast.LENGTH_SHORT).show()
                            else -> Toast.makeText(this@MainActivity, "Erreur de connexion: ${response.code()}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Échec de la requête", e)
                Toast.makeText(this@MainActivity, "Erreur réseau: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun testApiConnection() {
        Log.d(TAG, "Test de connexion à l'API: ${ApiClient.getCurrentBaseUrl()}")
        Toast.makeText(this, "Test de connexion à l'API...", Toast.LENGTH_SHORT).show()
        
        lifecycleScope.launch {
            try {
                // Test avec les identifiants de la documentation Postman
                val response = ApiClient.todoApiService.authenticate("tom", "web")
                Log.d(TAG, "Test API - Réponse: ${response.code()}")
                if (response.isSuccessful) {
                    val authResponse = response.body()
                    val token = authResponse?.token ?: authResponse?.hash
                    Toast.makeText(this@MainActivity, "API accessible ! Token: ${token?.take(10)}...", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(this@MainActivity, "API accessible mais erreur: ${response.code()}", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Test API - Échec", e)
                Toast.makeText(this@MainActivity, "API inaccessible: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
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
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
} 