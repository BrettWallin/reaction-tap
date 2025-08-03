package com.example.reactiontap

import android.graphics.Color
import android.content.Intent
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Button
import android.widget.RelativeLayout
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import kotlin.random.Random
import org.json.JSONArray
import org.json.JSONObject

class MainActivity : AppCompatActivity() {

    private lateinit var rootLayout: RelativeLayout
    private lateinit var reactionButton: Button
    private lateinit var playAgainButton: Button
    private lateinit var mainMenuButton: Button
    private lateinit var mainMenuLayout: LinearLayout
    private lateinit var startButton: Button
    private lateinit var removeAdsButton: Button
    private lateinit var viewHighScoresButton: Button
    private lateinit var endLogoImage: android.widget.ImageView

    private val handler = Handler(Looper.getMainLooper())
    private lateinit var prefs: SharedPreferences
    private var gameStarted = false
    private var canTap = false
    private var startTime: Long = 0L
    private var delayRunnable: Runnable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = getSharedPreferences("reactiontap_scores", Context.MODE_PRIVATE)

        // If username not set, go to UsernameActivity
        val username = prefs.getString("username", null)
        if (username.isNullOrEmpty()) {
            val intent = Intent(this, UsernameActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            return
        }

        setContentView(R.layout.activity_main)

        rootLayout = findViewById(R.id.rootLayout)
        reactionButton = findViewById(R.id.reactionButton)
        playAgainButton = findViewById(R.id.playAgainButton)
        mainMenuLayout = findViewById(R.id.mainMenuLayout)
        startButton = findViewById(R.id.startButton)
        removeAdsButton = findViewById(R.id.removeAdsButton)
        viewHighScoresButton = findViewById(R.id.viewHighScoresButton)
        mainMenuButton = findViewById(R.id.mainMenuButton)
        endLogoImage = findViewById(R.id.endLogoImage)

        // View High Scores launches leaderboard
        viewHighScoresButton.setOnClickListener {
            val intent = Intent(this, LeaderboardActivity::class.java)
            startActivity(intent)
        }

        // Start button launches the game
        startButton.setOnClickListener {
            showGameUI()
            startGame()
        }

        // Play again button starts a new game
        playAgainButton.setOnClickListener {
            showGameUI()
            startGame()
        }

        // Main menu button also returns to menu
        mainMenuButton.setOnClickListener {
            showMenuUI()
        }

        // Root layout handles all taps during the game
        rootLayout.setOnClickListener {
            if (reactionButton.visibility == View.VISIBLE) {
                if (gameStarted && canTap) {
                    val reactionTime = System.currentTimeMillis() - startTime
                    showResult("Reaction Time: ${reactionTime} ms")
                } else if (gameStarted && !canTap) {
                    showResult("Too soon! Try again.")
                }
            }
        }

        // Hide game UI and show menu on launch
        showMenuUI()
    }

    private fun startGame() {
        gameStarted = true
        canTap = false
        reactionButton.text = "Wait for green..."
        reactionButton.setBackgroundColor(Color.GRAY)
        rootLayout.setBackgroundColor(Color.parseColor("#08252d"))
        playAgainButton.visibility = View.GONE

        val delay = Random.nextLong(2000, 5000)
        delayRunnable = Runnable {
            canTap = true
            startTime = System.currentTimeMillis()
            reactionButton.text = "TAP!"
            reactionButton.setBackgroundColor(Color.GREEN)
            rootLayout.setBackgroundColor(Color.GREEN)
        }
        handler.postDelayed(delayRunnable!!, delay)
    }

    private fun showResult(message: String) {
        handler.removeCallbacks(delayRunnable ?: Runnable { })
        gameStarted = false
        canTap = false
        reactionButton.text = message
        reactionButton.setBackgroundColor(Color.DKGRAY)
        rootLayout.setBackgroundColor(Color.parseColor("#08252d"))
        playAgainButton.visibility = View.VISIBLE
        mainMenuButton.visibility = View.VISIBLE
        endLogoImage.visibility = View.VISIBLE

        // Save score if it's a valid reaction time
        if (message.startsWith("Reaction Time:")) {
            val regex = Regex("Reaction Time: (\\d+) ms")
            val match = regex.find(message)
            val score = match?.groupValues?.get(1)?.toIntOrNull()
            if (score != null) {
                saveScore(score)
            }
        }
    }

    private fun saveScore(score: Int) {
        val username = prefs.getString("username", "User") ?: "User"
        // Store as JSON array of objects [{username, score}]
        val allJson = prefs.getString("all_scores_json", "[]")
        val arr = JSONArray(allJson)
        val obj = JSONObject()
        obj.put("username", username)
        obj.put("score", score)
        arr.put(obj)
        prefs.edit().putString("all_scores_json", arr.toString()).apply()

        // Save user's best
        val userBest = prefs.getInt("user_best", -1)
        if (userBest == -1 || score < userBest) {
            prefs.edit().putInt("user_best", score).apply()
        }
    }

    private fun showMenuUI() {
        mainMenuLayout.visibility = View.VISIBLE
        reactionButton.visibility = View.GONE
        playAgainButton.visibility = View.GONE
        mainMenuButton.visibility = View.GONE
        endLogoImage.visibility = View.GONE
        rootLayout.setBackgroundColor(Color.parseColor("#08252d"))
        handler.removeCallbacks(delayRunnable ?: Runnable { })
        gameStarted = false
        canTap = false
    }

    private fun showGameUI() {
        mainMenuLayout.visibility = View.GONE
        reactionButton.visibility = View.VISIBLE
        playAgainButton.visibility = View.GONE
        mainMenuButton.visibility = View.GONE
        endLogoImage.visibility = View.GONE
        rootLayout.setBackgroundColor(Color.parseColor("#08252d"))
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
    }
}