package com.example.reactiontap

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ListView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONArray
import org.json.JSONObject

class LeaderboardActivity : AppCompatActivity() {
    private lateinit var highScoresTable: android.widget.TableLayout
    private lateinit var yourScoreLabel: TextView
    private lateinit var yourScoreValue: TextView
    private lateinit var leaderboardBackButton: Button
    private lateinit var prefs: SharedPreferences

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

        val allScores = getAllScoresWithUsernames()
        val best10 = allScores.take(10)
        val userBest = prefs.getInt("user_best", -1)
        val username = prefs.getString("username", "User") ?: "User"

        // Populate the table with scores
        highScoresTable.removeAllViews()
        for ((i, entry) in best10.withIndex()) {
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
            // Alternate row background color for clarity
            val bgColor = if (i % 2 == 0) android.graphics.Color.parseColor("#0e3440") else android.graphics.Color.parseColor("#08252d")
            row.setBackgroundColor(bgColor)
            // Add a bottom divider
            val divider = android.view.View(this)
            divider.layoutParams = android.widget.TableRow.LayoutParams(android.widget.TableRow.LayoutParams.MATCH_PARENT, 2)
            divider.setBackgroundColor(android.graphics.Color.parseColor("#222222"))
            row.addView(rankView)
            row.addView(nameView)
            row.addView(scoreView)
            highScoresTable.addView(row)
            highScoresTable.addView(divider)
        }

        // Show user's best at the bottom if not in top 10
        val userInTop10 = best10.any { it.username == username && it.score == userBest }
        if (userBest != -1 && !userInTop10) {
            yourScoreLabel.text = "Your Best (not in top 10):"
            yourScoreValue.text = "$username: $userBest ms"
            yourScoreLabel.visibility = TextView.VISIBLE
            yourScoreValue.visibility = TextView.VISIBLE
        } else if (userBest != -1) {
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

        leaderboardBackButton.setOnClickListener {
            finish()
        }
    }

    private fun getAllScoresWithUsernames(): List<ScoreEntry> {
        val allJson = prefs.getString("all_scores_json", "[]")
        val arr = JSONArray(allJson)
        val entries = mutableListOf<ScoreEntry>()
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            val username = obj.optString("username", "User")
            val score = obj.optInt("score", -1)
            if (score != -1) {
                entries.add(ScoreEntry(username, score))
            }
        }
        return entries.sortedBy { it.score }
    }
}
