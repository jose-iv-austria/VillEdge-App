package com.example.villedgeresident

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.*
import java.text.SimpleDateFormat
import java.util.*

class ThirdActivity : AppCompatActivity() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var entrantAdapter: EntrantAdapter
    private lateinit var entrantList: MutableList<Entrant>
    private lateinit var databaseReference: DatabaseReference
    private lateinit var firstInput: EditText
    private lateinit var secondInput: EditText
    private lateinit var thirdInputSpinner: Spinner
    private lateinit var submitButton: Button
    private var isAdmin: Boolean = false


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_third)

        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        entrantList = mutableListOf()
        entrantAdapter = EntrantAdapter(entrantList)
        recyclerView.adapter = entrantAdapter

        databaseReference = FirebaseDatabase.getInstance().getReference("EntrantsList")

        isAdmin = intent.getBooleanExtra("isAdmin", false)
        val backButton: ImageButton = findViewById(R.id.topLeftButton)

        backButton.setOnClickListener {
            val intent = Intent()
            intent.putExtra("isAdmin", isAdmin)
            setResult(RESULT_OK, intent)
            finish() // Finish the activity and return to MainActivity
        }



        firstInput = findViewById(R.id.firstInput)
        secondInput = findViewById(R.id.secondInput)
        thirdInputSpinner = findViewById(R.id.thirdInputSpinner)
        submitButton = findViewById(R.id.middleButton)

        // Populate spinner
        val addresses = listOf("111 Raspberry Street", "112 Raspberry Street")
        val spinnerAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, addresses)
        thirdInputSpinner.adapter = spinnerAdapter

        submitButton.setOnClickListener {
            addApprovalToFirebase()
        }
        fetchEntrantsData()
    }

    private fun fetchEntrantsData() {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val calendar = Calendar.getInstance()
        val today = dateFormat.format(calendar.time)
        calendar.add(Calendar.DAY_OF_YEAR, -1)
        val yesterday = dateFormat.format(calendar.time)

        databaseReference.orderByChild("Date").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                entrantList.clear()
                for (data in snapshot.children.reversed()) { // Reverse order for latest first
                    val entrant = data.getValue(Entrant::class.java)
                    if (entrant != null && (entrant.Date == today || entrant.Date == yesterday)) {
                        entrantList.add(entrant)
                    }
                }
                entrantAdapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("Firebase", "Database error: ${error.message}")
            }
        })
    }


    private fun addApprovalToFirebase() {
        val name = firstInput.text.toString().trim()
        val purpose = secondInput.text.toString().trim()
        val address = thirdInputSpinner.selectedItem?.toString() ?: ""

        if (name.isEmpty() || purpose.isEmpty() || address.isEmpty()) {
            return
        }

        val currentDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val currentTime = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        val uniqueId = databaseReference.push().key ?: return

        val approval = mapOf(
            "Name" to name,
            "Purpose" to purpose,
            "Date" to currentDate,
            "Time" to currentTime,
            "Address" to address,
            "Status" to "Pending"
        )

        databaseReference.child(uniqueId).setValue(approval)
            .addOnSuccessListener {
                firstInput.text.clear()
                secondInput.text.clear()
                Toast.makeText(this, "Submitted successfully", Toast.LENGTH_SHORT).show()
                Log.d("Firebase", "Approval successfully added.")
            }
            .addOnFailureListener { e ->
                Log.e("Firebase", "Failed to add approval: $e")
            }
    }
}

// Data Model
data class Entrant(
    val Name: String = "",
    val Address: String = "",
    val Purpose: String = "",
    val Date: String = "",
    val Time: String = "",
    val Status: String = ""
) {
    fun formattedDateTime(): String {
        return "$Date ($Time)"
    }
}

class EntrantAdapter(private val entrantList: List<Entrant>) : RecyclerView.Adapter<EntrantAdapter.ViewHolder>() {
    private val databaseReference: DatabaseReference = FirebaseDatabase.getInstance().reference

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val itemName: TextView = view.findViewById(R.id.item_name)
        val itemDetail1: TextView = view.findViewById(R.id.item_detail1)
        val itemDetail2: TextView = view.findViewById(R.id.item_detail2)
        val itemDetail3: TextView = view.findViewById(R.id.item_detail3)
        val bottomRightText: TextView = view.findViewById(R.id.bottom_right_text)
        val button1: ImageButton = view.findViewById(R.id.button1)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.entrants_manager_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val entrant = entrantList[position]
        holder.itemName.text = entrant.Name
        holder.itemDetail1.text = entrant.Address
        holder.itemDetail2.text = "Purpose: ${entrant.Purpose}"
        holder.itemDetail3.text = entrant.formattedDateTime()
        holder.bottomRightText.text = entrant.Status

        if (entrant.Status == "Pending" || entrant.Status == "Denied" || entrant.Status == "Exited") {
            holder.button1.visibility = View.GONE
        } else {
            holder.button1.visibility = View.VISIBLE

            val imageResource = when (entrant.Status) {
                "Approved" -> R.drawable.entry_button
                "Entered" -> R.drawable.exit_button
                else -> R.drawable.entry_button
            }
            holder.button1.setImageResource(imageResource)

            holder.button1.setOnClickListener {
                val entrantRef = databaseReference.child("EntrantsList").orderByChild("Name").equalTo(entrant.Name)
                val adminRef = databaseReference.child("Administration")
                val historyRef = FirebaseDatabase.getInstance().getReference("EntrantsHistory")

                entrantRef.addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        for (data in snapshot.children) {
                            val key = data.key ?: return
                            val specificEntrantRef = databaseReference.child("EntrantsList").child(key)
                            val currentDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                            val currentTime = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
                            val historyId = historyRef.push().key ?: return

                            val historyEntry = mapOf(
                                "ID" to historyId,
                                "Name" to entrant.Name,
                                "Address" to entrant.Address,
                                "Purpose" to entrant.Purpose,
                                "Date" to currentDate,
                                "Time" to currentTime
                            )

                            adminRef.addListenerForSingleValueEvent(object : ValueEventListener {
                                override fun onDataChange(adminSnapshot: DataSnapshot) {
                                    val inIR = adminSnapshot.child("InIR").getValue(Boolean::class.java) ?: false
                                    val outIR = adminSnapshot.child("OutIR").getValue(Boolean::class.java) ?: false

                                    when (entrant.Status) {
                                        "Approved" -> {
                                            if (inIR) {
                                                adminRef.child("InGate").setValue(true)
                                                specificEntrantRef.child("Status").setValue("Entered")
                                                historyRef.child(historyId).setValue(historyEntry + ("Status" to "Entered"))
                                            } else {
                                                Toast.makeText(holder.itemView.context, "No vehicle is present at the Entry Gate", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                        "Entered" -> {
                                            if (outIR) {
                                                adminRef.child("OutGate").setValue(true)
                                                specificEntrantRef.child("Status").setValue("Exited")
                                                historyRef.child(historyId).setValue(historyEntry + ("Status" to "Exited"))
                                            } else {
                                                Toast.makeText(holder.itemView.context, "No vehicle is present at the Exit Gate", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    }
                                }

                                override fun onCancelled(adminError: DatabaseError) {
                                    Log.e("Firebase", "Error checking admin settings: ${adminError.message}")
                                }
                            })
                        }
                    }
                    override fun onCancelled(error: DatabaseError) {
                        Log.e("Firebase", "Error updating status: ${error.message}")
                    }
                })
            }
        }
    }




    override fun getItemCount(): Int {
        return entrantList.size
    }
}
