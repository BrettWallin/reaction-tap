package com.example.reactiontap

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

        // Fetch top 10 global scores from Firestore
        fetchTopScoresFromFirestore(neonYellow)

        leaderboardBackButton.setOnClickListener {
            finish()
        }
    }


    private fun fetchTopScoresFromFirestore(neonYellow: Int) {
        firestore.collection("scores")
            .orderBy("score", Query.Direction.ASCENDING)
            .limit(10)
            .get()
            .addOnSuccessListener { result ->
                val topScores = result.documents.mapNotNull { doc ->
                    val username = doc.getString("username") ?: "User"
                    val score = doc.getLong("score")?.toInt() ?: return@mapNotNull null
                    ScoreEntry(username, score)
                }
                displayScores(topScores, neonYellow)
            }
            .addOnFailureListener {
                // Optionally show error or fallback to local
                yourScoreLabel.text = "Could not load global scores."
                yourScoreValue.text = "-"
                yourScoreLabel.visibility = TextView.VISIBLE
                yourScoreValue.visibility = TextView.VISIBLE
            }
    }

    private fun displayScores(scores: List<ScoreEntry>, neonYellow: Int) {
        highScoresTable.removeAllViews()
        for ((i, entry) in scores.withIndex()) {
            val row = android.widget.TableRow(this)
            val rankView = android.widget.TextView(this)
            val nameView = android.widget.TextView(this)
            val scoreView = android.widget.TextView(this)
            rankView.text = (i + 1).toString()
            nameView.text = entry.username
            scoreView.text = "${entry.score} ms"
            rankView.setTextColor(neonYellow)
            nameView.setTextColor(neonYellow)
            scoreView.setTextColor(neonYellow)
            rankView.setPadding(12, 24, 12, 24)
            nameView.setPadding(12, 24, 12, 24)
            scoreView.setPadding(12, 24, 12, 24)
            rankView.typeface = android.graphics.Typeface.MONOSPACE
            nameView.typeface = android.graphics.Typeface.MONOSPACE
            scoreView.typeface = android.graphics.Typeface.MONOSPACE
            rankView.textSize = 20f
            nameView.textSize = 20f
            scoreView.textSize = 20f
            rankView.gravity = android.view.Gravity.CENTER
            nameView.gravity = android.view.Gravity.CENTER
            scoreView.gravity = android.view.Gravity.CENTER
            val lpRank = android.widget.TableRow.LayoutParams(0, android.widget.TableRow.LayoutParams.WRAP_CONTENT, 1f)
            val lpName = android.widget.TableRow.LayoutParams(0, android.widget.TableRow.LayoutParams.WRAP_CONTENT, 2f)
            val lpScore = android.widget.TableRow.LayoutParams(0, android.widget.TableRow.LayoutParams.WRAP_CONTENT, 2f)
            rankView.layoutParams = lpRank
            nameView.layoutParams = lpName
            scoreView.layoutParams = lpScore
            val bgColor = if (i % 2 == 0) android.graphics.Color.parseColor("#0e3440") else android.graphics.Color.parseColor("#08252d")
            row.setBackgroundColor(bgColor)
            val divider = android.view.View(this)
            divider.layoutParams = android.widget.TableRow.LayoutParams(android.widget.TableRow.LayoutParams.MATCH_PARENT, 2)
            divider.setBackgroundColor(android.graphics.Color.parseColor("#222222"))
            row.addView(rankView)
            row.addView(nameView)
            row.addView(scoreView)
            highScoresTable.addView(row)
            highScoresTable.addView(divider)
        }

        // Show user's best (local) at the bottom
        val userBest = prefs.getInt("user_best", -1)
        val username = prefs.getString("username", "User") ?: "User"
        if (userBest != -1) {
            yourScoreLabel.text = "Your Best:"
            yourScoreValue.text = "$username: $userBest ms"
            yourScoreLabel.visibility = TextView.VISIBLE
            yourScoreValue.visibility = TextView.VISIBLE
        } else {
            yourScoreLabel.text = "No score yet."
            yourScoreValue.text = "-"
            yourScoreLabel.visibility = TextView.VISIBLE
            yourScoreValue.visibility = TextView.VISIBLE
        }
    }
}
