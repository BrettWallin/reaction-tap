package com.walleyestudios.reactiontap

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ListView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class LeaderboardActivity : AppCompatActivity() {
    private lateinit var highScoresTable: android.widget.TableLayout
    private lateinit var yourScoreLabel: TextView
    private lateinit var yourScoreValue: TextView
    private lateinit var leaderboardBackButton: Button
    private lateinit var prefs: SharedPreferences
    private val firestore by lazy { FirebaseFirestore.getInstance() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_leaderboard_table)
        highScoresTable = findViewById(R.id.highScoresTable)
        yourScoreLabel = findViewById(R.id.yourScoreLabel)
        yourScoreValue = findViewById(R.id.yourScoreValue)
        leaderboardBackButton = findViewById(R.id.leaderboardBackButton)
        prefs = getSharedPreferences("reactiontap_scores", Context.MODE_PRIVATE)

        // Set retro background and text color
        val root = findViewById<android.widget.TableLayout>(R.id.leaderboardRoot)
        root.setBackgroundColor(android.graphics.Color.parseColor("#08252d"))
        val neonYellow = android.graphics.Color.parseColor("#fcc321")
        findViewById<TextView>(R.id.leaderboardTitle).setTextColor(neonYellow)
        yourScoreLabel.setTextColor(neonYellow)
        yourScoreValue.setTextColor(neonYellow)
        leaderboardBackButton.setTextColor(android.graphics.Color.WHITE)
        highScoresTable.setBackgroundColor(android.graphics.Color.parseColor("#08252d"))

        // Show user's best score
        val userBest = prefs.getInt("user_best", -1)
        if (userBest != -1) {
            yourScoreValue.text = userBest.toString() + " ms"
        } else {
            yourScoreValue.text = "-"
        }

        // Back button closes leaderboard
        leaderboardBackButton.setOnClickListener {
            finish()
        }

        // Load top scores from Firestore and display
        loadGlobalLeaderboard()
    }

    private fun loadGlobalLeaderboard() {
        // Remove all rows except header
        while (highScoresTable.childCount > 1) {
            highScoresTable.removeViewAt(1)
        }
        firestore.collection("scores")
            .orderBy("score", Query.Direction.ASCENDING)
            .limit(10)
            .get()
            .addOnSuccessListener { result ->
                var rank = 1
                for (doc in result) {
                    val username = doc.getString("username") ?: "-"
                    val score = doc.getLong("score")?.toInt() ?: -1
                    if (score != -1) {
                        val row = android.widget.TableRow(this)
                        val rankView = TextView(this)
                        val nameView = TextView(this)
                        val scoreView = TextView(this)
                        rankView.text = rank.toString()
                        nameView.text = username
                        scoreView.text = "$score ms"
                        val neonYellow = android.graphics.Color.parseColor("#fcc321")
                        rankView.setTextColor(neonYellow)
                        nameView.setTextColor(neonYellow)
                        scoreView.setTextColor(neonYellow)
                        rankView.setPadding(12, 12, 12, 12)
                        nameView.setPadding(12, 12, 12, 12)
                        scoreView.setPadding(12, 12, 12, 12)
                        rankView.textSize = 18f
                        nameView.textSize = 18f
                        scoreView.textSize = 18f
                        row.addView(rankView)
                        row.addView(nameView)
                        row.addView(scoreView)
                        highScoresTable.addView(row)
                        rank++
                    }
                }
                if (rank == 1) {
                    val row = android.widget.TableRow(this)
                    val emptyView = TextView(this)
                    emptyView.text = "No scores yet. Play to set a record!"
                    emptyView.setTextColor(android.graphics.Color.LTGRAY)
                    row.addView(emptyView)
                    highScoresTable.addView(row)
                }
            }
            .addOnFailureListener {
                val row = android.widget.TableRow(this)
                val errorView = TextView(this)
                errorView.text = "Failed to load leaderboard."
                errorView.setTextColor(android.graphics.Color.RED)
                row.addView(errorView)
                highScoresTable.addView(row)
            }
    }

    private fun loadLocalLeaderboard() {
        // Remove all rows except header
        while (highScoresTable.childCount > 1) {
            highScoresTable.removeViewAt(1)
        }
        val allJson = prefs.getString("all_scores_json", "[]")
        val arr = try { org.json.JSONArray(allJson) } catch (e: Exception) { org.json.JSONArray() }
        // Build a list of (username, score) pairs
        val scores = mutableListOf<Pair<String, Int>>()
        for (i in 0 until arr.length()) {
            val obj = arr.optJSONObject(i)
            if (obj != null) {
                val username = obj.optString("username", "-")
                val score = obj.optInt("score", -1)
                if (score != -1) {
                    scores.add(Pair(username, score))
                }
            }
        }
        // Sort by score ascending (lower is better)
        val sorted = scores.sortedBy { it.second }.take(10)
        var rank = 1
        for ((username, score) in sorted) {
            val row = android.widget.TableRow(this)
            val rankView = TextView(this)
            val nameView = TextView(this)
            val scoreView = TextView(this)
            rankView.text = rank.toString()
            nameView.text = username
            scoreView.text = "$score ms"
            val neonYellow = android.graphics.Color.parseColor("#fcc321")
            rankView.setTextColor(neonYellow)
            nameView.setTextColor(neonYellow)
            scoreView.setTextColor(neonYellow)
            rankView.setPadding(12, 12, 12, 12)
            nameView.setPadding(12, 12, 12, 12)
            scoreView.setPadding(12, 12, 12, 12)
            rankView.textSize = 18f
            nameView.textSize = 18f
            scoreView.textSize = 18f
            row.addView(rankView)
            row.addView(nameView)
            row.addView(scoreView)
            highScoresTable.addView(row)
            rank++
        }
        if (sorted.isEmpty()) {
            val row = android.widget.TableRow(this)
            val emptyView = TextView(this)
            emptyView.text = "No scores yet. Play to set a record!"
            emptyView.setTextColor(android.graphics.Color.LTGRAY)
            row.addView(emptyView)
            highScoresTable.addView(row)
        }
    }
}
