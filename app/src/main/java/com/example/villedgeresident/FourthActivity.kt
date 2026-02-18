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

class FourthActivity : AppCompatActivity() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var entrantAdapter: EntrantHistoryAdapter
    private lateinit var entrantList: MutableList<EntrantHistory>
    private lateinit var databaseReference: DatabaseReference
    private var isAdmin: Boolean = false


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_fourth)

        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        entrantList = mutableListOf()
        entrantAdapter = EntrantHistoryAdapter(entrantList)
        recyclerView.adapter = entrantAdapter
        databaseReference = FirebaseDatabase.getInstance().getReference("EntrantsHistory")

        val backButton: ImageButton = findViewById(R.id.topLeftButton)
        isAdmin = intent.getBooleanExtra("isAdmin", false)
        backButton.setOnClickListener {
            val intent = Intent()
            intent.putExtra("isAdmin", isAdmin)
            setResult(RESULT_OK, intent)
            finish() // Finish the activity and return to MainActivity
        }

        fetchEntrantsData()
    }

    private fun fetchEntrantsData() {
        databaseReference.orderByChild("Date").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                entrantList.clear()
                for (data in snapshot.children.reversed()) { // Reverse order for latest first
                    val entrant = data.getValue(EntrantHistory::class.java)
                    if (entrant != null) {
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
}

// Data Model
data class EntrantHistory(
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

class EntrantHistoryAdapter(private val entrantList: List<EntrantHistory>) : RecyclerView.Adapter<EntrantHistoryAdapter.ViewHolder>() {
    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val itemName: TextView = view.findViewById(R.id.item_name)
        val itemDetail1: TextView = view.findViewById(R.id.item_detail1)
        val itemDetail2: TextView = view.findViewById(R.id.item_detail2)
        val itemDetail3: TextView = view.findViewById(R.id.item_detail3)
        val bottomRightText: TextView = view.findViewById(R.id.bottom_right_text)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.entrants_history_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val entrant = entrantList[position]
        holder.itemName.text = entrant.Name
        holder.itemDetail1.text = entrant.Address
        holder.itemDetail2.text = "Purpose: ${entrant.Purpose}"
        holder.itemDetail3.text = entrant.formattedDateTime()
        holder.bottomRightText.text = entrant.Status
    }

    override fun getItemCount(): Int {
        return entrantList.size
    }
}
