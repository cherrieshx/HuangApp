package it.progmob.huangapp

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.firestore
import it.progmob.huangapp.databinding.ActivityMainBinding
import it.progmob.huangapp.ui.WelcomeActivity
import it.progmob.huangapp.viewmodel.HomeViewModel


class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private val homeViewModel: HomeViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        com.google.firebase.messaging.FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            // In MainActivity.kt, dentro il task del token:
            if (task.isSuccessful) {
                val token = task.result
                val userId = FirebaseAuth.getInstance().currentUser?.uid
                if (userId != null) {
                    val tokenData = mapOf("fcmToken" to token)
                    Firebase.firestore.collection("users").document(userId)
                        .set(tokenData, com.google.firebase.firestore.SetOptions.merge())
                }
            }
        }


        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Gestisce lo spazio della barra di stato
        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0)
            insets
        }

        // Configura toolbar search e toolbar per RecipeDetailFragment
        setSupportActionBar(binding.toolbarDetail)
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host) as NavHostFragment
        val navController = navHostFragment.navController
        val searchEditText = binding.toolbar.findViewById<EditText>(R.id.search_edit_text)

        // Collega NavController alla toolbar_detail per back button automatico
        val appBarConfig = androidx.navigation.ui.AppBarConfiguration(
            setOf(R.id.HomeFragment, R.id.ProfileFragment, R.id.NewRecipeFragment)
        )
        androidx.navigation.ui.NavigationUI.setupActionBarWithNavController(this, navController, appBarConfig)

        // Collega la Bottom Navigation
        binding.bottomNav.setupWithNavController(navController)
        binding.bottomNav.setOnItemSelectedListener { item ->
            val user = FirebaseAuth.getInstance().currentUser

            when (item.itemId) {
                R.id.HomeFragment -> {
                    navController.navigate(R.id.HomeFragment)
                    true
                }
                R.id.NewRecipeFragment, R.id.ProfileFragment -> {
                    if (user == null) {
                        Toast.makeText(this, "Effettua prima l'accesso", Toast.LENGTH_SHORT).show()
                        startActivity(Intent(this, WelcomeActivity::class.java))
                        false
                    } else {
                        navController.navigate(item.itemId)
                        true
                    }
                }
                else -> false
            }
        }

        // Logica search
        searchEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                homeViewModel.searchRecipes(s.toString())
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        navController.addOnDestinationChangedListener { _, destination, _ ->
            if (destination.id == R.id.HomeFragment) {
                binding.toolbar.visibility = View.VISIBLE
                binding.toolbarDetail.visibility = View.GONE
            } else {
                binding.toolbar.visibility = View.GONE
                binding.toolbarDetail.visibility = View.VISIBLE
            }
            when (destination.id) {
                R.id.RecipeDetailFragment -> {
                    binding.bottomNav.visibility = View.GONE
                }
                else -> {
                    // Mostra la navbar in Home, Profilo e Nuova Ricetta
                    binding.bottomNav.visibility = View.VISIBLE
                }
            }
        }

        createNotificationChannel()
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU){
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                0
            )
        }
        handleNotificationIntent(intent)
    }
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent) // Aggiorna l'intent dell'activity con quello della notifica
        handleNotificationIntent(intent)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Create the NotificationChannel.
            val name = "Notification Channel"
            val descriptionText = "Channel for notifications"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val mChannel = NotificationChannel("CHANNEL_ID_V3", name, importance)
            mChannel.description = descriptionText
            val notificationManager = getSystemService(NOTIFICATION_SERVICE) as
                    NotificationManager
            notificationManager.createNotificationChannel(mChannel)
        }
    }
    private fun handleNotificationIntent(intent: Intent) {
        val recipeId = intent.getStringExtra("recipeId")
        if (recipeId != null) {
            // Recupera il valore 'goToComments' che può arrivare come String (da FCM) o Boolean (da Service)
            val extras = intent.extras
            val rawString = extras?.getString("goToComments")
            val rawBoolean = extras?.getBoolean("goToComments", false) ?: false

            val shouldScroll = if (rawString != null) {
                rawString.toBoolean()
            } else {
                rawBoolean
            }
            val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host) as NavHostFragment
            val navController = navHostFragment.navController

            val bundle = Bundle().apply {
                putString("recipeId", recipeId)
                putBoolean("goToComments", shouldScroll)
            }
            navController.navigate(R.id.RecipeDetailFragment, bundle)
        }
    }

    // Gestisce della freccia indietro in RecipeDetailFragment
    override fun onSupportNavigateUp(): Boolean {
        val navController = (supportFragmentManager.findFragmentById(R.id.nav_host) as NavHostFragment).navController
        return navController.navigateUp() || super.onSupportNavigateUp()
    }
}