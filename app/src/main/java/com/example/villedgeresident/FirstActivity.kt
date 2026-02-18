package com.example.villedgeresident

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener

class FirstActivity : AppCompatActivity() {
    private lateinit var database: DatabaseReference
    private var isButtonEnabled = true
    private val handler = Handler(Looper.getMainLooper())
    private var isAdmin: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_first)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Initialize Firebase Database reference
        database = FirebaseDatabase.getInstance().getReference("Residents/201EAA59/Gate")

        val backButton: ImageButton = findViewById(R.id.back_button)
        val gateButton: ImageButton = findViewById(R.id.gate_button)

        backButton.setOnClickListener {
            val intent = Intent()
            intent.putExtra("isAdmin", isAdmin)
            setResult(RESULT_OK, intent)
            finish()
        }

        // Listen for real-time changes in gate status
        database.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val gateStatus = snapshot.getValue(Boolean::class.java) ?: false
                updateGateImage(gateStatus, gateButton)
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@FirstActivity, "Error fetching gate status", Toast.LENGTH_SHORT).show()
            }
        })

        // Handle gate button clicks
        gateButton.setOnClickListener {
            if (isButtonEnabled) {
                toggleGateStatus()
                isButtonEnabled = false
                handler.postDelayed({ isButtonEnabled = true }, 5000)
            } else {
                showGateStatusMessage()
            }
        }
    }

    private fun updateGateImage(gateStatus: Boolean, gateButton: ImageButton) {
        val imageResource = if (gateStatus) R.drawable.close_button else R.drawable.open_button
        gateButton.setImageResource(imageResource)
    }

    private fun toggleGateStatus() {
        database.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val currentStatus = snapshot.getValue(Boolean::class.java) ?: false
                database.setValue(!currentStatus)
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@FirstActivity, "Error toggling gate status", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun showGateStatusMessage() {
        database.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val currentStatus = snapshot.getValue(Boolean::class.java) ?: false
                val message = if (currentStatus) "Gate is currently opening" else "Gate is currently closing"
                Toast.makeText(this@FirstActivity, message, Toast.LENGTH_SHORT).show()
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@FirstActivity, "Error fetching gate status", Toast.LENGTH_SHORT).show()
            }
        })
    }
}
