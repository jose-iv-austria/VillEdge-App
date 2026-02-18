package com.example.villedgeresident

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.*

class LoginActivity : AppCompatActivity() {
    private lateinit var database: DatabaseReference
    private lateinit var serialNumberInput: EditText
    private lateinit var passwordInput: EditText
    private lateinit var loginButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Hide status bar
        setContentView(R.layout.activity_login)

        database = FirebaseDatabase.getInstance().reference.child("UserManagement")
        serialNumberInput = findViewById(R.id.serialNumber)
        passwordInput = findViewById(R.id.password)
        loginButton = findViewById(R.id.loginButton)

        loginButton.setOnClickListener {
            val serialNumber = serialNumberInput.text.toString().trim()
            val password = passwordInput.text.toString().trim()

            if (serialNumber.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            authenticateUser(serialNumber, password)
        }


    }

    private fun authenticateUser(serialNumber: String, password: String) {
        database.child(serialNumber).get().addOnSuccessListener { snapshot ->
            if (!snapshot.exists()) {
                Toast.makeText(this, "User does not exist", Toast.LENGTH_SHORT).show()
            } else {
                val storedPassword = snapshot.child("Password").getValue(String::class.java)
                if (storedPassword == password) {
                    val intent = Intent(this, MainActivity::class.java)
                    intent.putExtra("isAdmin", serialNumber == "VilledgeAdmin")
                    startActivity(intent)
                    finish()
                } else {
                    Toast.makeText(this, "Incorrect password", Toast.LENGTH_SHORT).show()
                }
            }
        }.addOnFailureListener {
            Toast.makeText(this, "Error connecting to database", Toast.LENGTH_SHORT).show()
        }
    }
}
