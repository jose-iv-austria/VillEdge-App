package com.example.villedgeresident

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.database.*
class MainActivity : AppCompatActivity() {

    private lateinit var database: DatabaseReference
    private lateinit var humidityText: TextView
    private lateinit var temperatureText: TextView
    private lateinit var solarText: TextView
    private lateinit var userLastname: TextView
    private lateinit var userAddress: TextView
    private lateinit var button1: ImageButton
    private lateinit var button2: ImageButton
    private lateinit var button3: Button  // Added button3
    private var isAdmin: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Retrieve isAdmin flag from intent or saved state
        isAdmin = savedInstanceState?.getBoolean("isAdmin") ?: intent.getBooleanExtra("isAdmin", false)

        // Initialize TextViews and Buttons
        humidityText = findViewById(R.id.humidityText)
        temperatureText = findViewById(R.id.temperatureText)
        solarText = findViewById(R.id.solarText)
        userLastname = findViewById(R.id.userLastname)
        userAddress = findViewById(R.id.userAddress)
        button1 = findViewById(R.id.button1)
        button2 = findViewById(R.id.button2)
        button3 = findViewById(R.id.button3) // Initialize button3

        // Initialize Firebase Database reference
        database = FirebaseDatabase.getInstance().getReference("Administration")
        updateButtonImages()
        // Fetch and update data
        fetchData()

        button1.setOnClickListener {
            val nextActivity = if (isAdmin) ThirdActivity::class.java else FirstActivity::class.java
            val intent = Intent(this, nextActivity)
            intent.putExtra("isAdmin", isAdmin)
            startActivityForResult(intent, 1)
        }

        button2.setOnClickListener {
            val nextActivity = if (isAdmin) FourthActivity::class.java else SecondActivity::class.java
            val intent = Intent(this, nextActivity)
            intent.putExtra("isAdmin", isAdmin)
            startActivityForResult(intent, 2)
        }

        button3.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK // Clears back stack
            startActivity(intent)
            finish()
        }
    }

    private fun updateButtonImages() {
        if (isAdmin) {
            button1.setImageResource(R.drawable.entrants_button) // Set button1 image for admin
            button2.setImageResource(R.drawable.history_button) // Set button2 image for admin
            userAddress.text = "Administration"
            userLastname.text = "Admin 1"
        } else {
            button1.setImageResource(R.drawable.remote_button) // Set button1 image for non-admin
            button2.setImageResource(R.drawable.visitor_button) // Set button2 image for non-admin
            userAddress.text = "111 Raspberry Street"
            userLastname.text = "Austria"
        }
    }

    private fun fetchData() {
        database.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val humidity = snapshot.child("Humidity").getValue(Int::class.java) ?: 0
                val temperature = snapshot.child("Temperature").getValue(Int::class.java) ?: 0
                val solarStatus = snapshot.child("SolarStatus").getValue(String::class.java) ?: "Unknown"

                humidityText.text = "$humidity%"
                temperatureText.text = "$temperature°C"
                solarText.text = "$solarStatus"
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle database error
            }
        })
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == RESULT_OK && data != null) {
            isAdmin = data.getBooleanExtra("isAdmin", isAdmin)
        }
    }

    // Save state to retain isAdmin on configuration changes
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean("isAdmin", isAdmin)
    }
}
