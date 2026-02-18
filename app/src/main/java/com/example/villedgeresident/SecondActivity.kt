package com.example.villedgeresident

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.*
import android.widget.Button
import android.widget.EditText
import androidx.activity.enableEdgeToEdge

import com.google.firebase.database.*
import java.text.SimpleDateFormat
import java.util.*

// RecyclerView Adapter
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView as AndroidRecyclerView

class SecondActivity : AppCompatActivity() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var approvalAdapter: ApprovalAdapter
    private lateinit var approvalList: MutableList<Approval>
    private lateinit var entrantsReference: DatabaseReference
    private lateinit var firstInput: EditText
    private lateinit var secondInput: EditText
    private lateinit var submitButton: Button
    private var isAdmin: Boolean = false


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_second)

        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        approvalList = mutableListOf()
        approvalAdapter = ApprovalAdapter(approvalList)
        recyclerView.adapter = approvalAdapter


        entrantsReference = FirebaseDatabase.getInstance().getReference("EntrantsList")

        val backButton: ImageButton = findViewById(R.id.topLeftButton)

        backButton.setOnClickListener {
            val intent = Intent()
            intent.putExtra("isAdmin", isAdmin)
            setResult(RESULT_OK, intent)
            finish() // Finish the activity and return to MainActivity
        }

        firstInput = findViewById(R.id.firstInput)
        secondInput = findViewById(R.id.secondInput)
        submitButton = findViewById(R.id.bottomButton)

        submitButton.setOnClickListener {
            addEntrantToFirebase()
        }
        fetchApprovalData()
    }

    private fun fetchApprovalData() {
        entrantsReference.orderByChild("Date").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                approvalList.clear()
                for (data in snapshot.children.reversed()) { // Reverse order for latest first
                    val approval = data.getValue(Approval::class.java)
                    val status = data.child("Status").getValue(String::class.java) // Fetch status field
                    if (approval != null && status == "Pending") {
                        approvalList.add(approval)
                    }
                }
                approvalAdapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("Firebase", "Database error: ${error.message}")
            }
        })
    }


    private fun addEntrantToFirebase() {
        val name = firstInput.text.toString().trim()
        val purpose = secondInput.text.toString().trim()

        if (name.isEmpty() || purpose.isEmpty()) {
            return
        }

        val currentDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val currentTime = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        val uniqueId = entrantsReference.push().key ?: return

        val entrant = mapOf(
            "Name" to name,
            "Purpose" to purpose,
            "Date" to currentDate,
            "Time" to currentTime,
            "Address" to "111 Raspberry Street",
            "Status" to "Approved"
        )

        entrantsReference.child(uniqueId).setValue(entrant)
            .addOnSuccessListener {
                firstInput.text.clear()
                secondInput.text.clear()
                Log.d("Firebase", "Entrant successfully added.")
            }
            .addOnFailureListener { e ->
                Log.e("Firebase", "Failed to add entrant: ${'$'}e")
            }
    }



}



// Data Model
data class Approval(
    val Name: String = "",
    val Address: String = "",
    val Purpose: String = "",
    val Date: String = "",
    val Time: String = ""
) {
    fun formattedDateTime(): String {
        return "$Date ($Time)"
    }
}

class ApprovalAdapter(private val approvalList: MutableList<Approval>) : RecyclerView.Adapter<ApprovalAdapter.ViewHolder>() {
    private val databaseReference: DatabaseReference = FirebaseDatabase.getInstance().getReference("EntrantsList")

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val itemName: TextView = view.findViewById(R.id.item_name)
        val itemDetail1: TextView = view.findViewById(R.id.item_detail1)
        val itemDetail2: TextView = view.findViewById(R.id.item_detail2)
        val itemDetail3: TextView = view.findViewById(R.id.item_detail3)
        val button1: ImageButton = view.findViewById(R.id.button1)
        val button2: ImageButton = view.findViewById(R.id.button2)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.visitor_manager_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val approval = approvalList[position]
        holder.itemName.text = approval.Name
        holder.itemDetail1.text = approval.Address
        holder.itemDetail2.text = "Purpose: ${approval.Purpose}"
        holder.itemDetail3.text = approval.formattedDateTime()

        holder.button1.setOnClickListener {
            updateStatus(approval, "Approved")
        }

        holder.button2.setOnClickListener {
            updateStatus(approval, "Denied")
        }
    }

    override fun getItemCount(): Int {
        return approvalList.size
    }

    private fun updateStatus(approval: Approval, newStatus: String) {
        val query = databaseReference.orderByChild("Name").equalTo(approval.Name)
        query.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                for (data in snapshot.children) {
                    data.ref.child("Status").setValue(newStatus)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("Firebase", "Error updating status: ${error.message}")
            }
        })
    }
}