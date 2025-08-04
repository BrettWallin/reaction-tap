
package com.example.reactiontap

import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.ProductDetails

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
import com.google.firebase.firestore.FirebaseFirestore
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError

class MainActivity : AppCompatActivity() {
    // Billing
    private lateinit var billingClient: BillingClient
    private var removeAdsProductDetails: ProductDetails? = null
    private val REMOVE_ADS_PREF = "remove_ads_purchased"

    private lateinit var rootLayout: RelativeLayout
    private lateinit var reactionButton: Button
    private lateinit var playAgainButton: Button
    private lateinit var mainMenuButton: Button
    private lateinit var mainMenuLayout: LinearLayout
    private lateinit var startButton: Button
    private lateinit var removeAdsButton: Button
    private lateinit var viewHighScoresButton: Button
    private lateinit var endLogoImage: android.widget.ImageView

    // Interstitial Ad
    private var interstitialAd: InterstitialAd? = null
    private var gamesPlayed = 0

    private val handler = Handler(Looper.getMainLooper())
    private lateinit var prefs: SharedPreferences
    private var gameStarted = false
    private var canTap = false
    private var startTime: Long = 0L
    private var delayRunnable: Runnable? = null
    private val firestore by lazy { FirebaseFirestore.getInstance() }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Billing setup
        setupBillingClient()
        super.onCreate(savedInstanceState)
        prefs = getSharedPreferences("reactiontap_scores", Context.MODE_PRIVATE)

        // Initialize Google Mobile Ads SDK
        MobileAds.initialize(this) { }

        // Load first interstitial ad
        loadInterstitialAd()

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
        // Disable Remove Ads button until product details are loaded or already purchased
        if (prefs.getBoolean(REMOVE_ADS_PREF, false)) {
            removeAdsButton.isEnabled = false
            removeAdsButton.text = "Purchased"
        } else {
            removeAdsButton.isEnabled = false
        }
        // Remove Ads button launches purchase flow
        removeAdsButton.setOnClickListener {
            if (prefs.getBoolean(REMOVE_ADS_PREF, false)) {
                // Already purchased
                showMessage("Ads already removed!")
            } else if (removeAdsProductDetails != null) {
                val billingFlowParams = BillingFlowParams.newBuilder()
                    .setProductDetailsParamsList(
                        listOf(
                            BillingFlowParams.ProductDetailsParams.newBuilder()
                                .setProductDetails(removeAdsProductDetails!!)
                                .build()
                        )
                    )
                    .build()
                billingClient.launchBillingFlow(this, billingFlowParams)
            } else {
                showMessage("Store not ready. Try again in a moment.")
            }
        }
        viewHighScoresButton = findViewById(R.id.viewHighScoresButton)
        mainMenuButton = findViewById(R.id.mainMenuButton)
        endLogoImage = findViewById(R.id.endLogoImage)

        // Banner Ad setup (use XML ad size only)
        val adView = findViewById<AdView>(R.id.adView)
        if (!prefs.getBoolean(REMOVE_ADS_PREF, false)) {
            val adRequest = AdRequest.Builder().build()
            adView.loadAd(adRequest)
            adView.visibility = View.VISIBLE
        } else {
            adView.visibility = View.GONE
        }

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

        // Reaction button also handles taps during the game
        reactionButton.setOnClickListener {
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

        // Interstitial ad logic: show after every 3 games (if not purchased)
        if (!prefs.getBoolean(REMOVE_ADS_PREF, false)) {
            gamesPlayed++
            if (gamesPlayed % 3 == 0) {
                showInterstitialAd()
            }
        }

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

            // --- FIRESTORE UPLOAD ---
            // Use username as document ID so each user only has one score
            val scoreData = hashMapOf(
                "username" to username,
                "score" to score,
                "timestamp" to System.currentTimeMillis()
            )
            firestore.collection("scores")
                .document(username)
                .set(scoreData)
                .addOnSuccessListener { /* Optionally log or show success */ }
                .addOnFailureListener { /* Optionally log or show error */ }
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
        // Hide banner ad if purchased
        val adView = findViewById<AdView>(R.id.adView)
        if (prefs.getBoolean(REMOVE_ADS_PREF, false)) {
            adView.visibility = View.GONE
        } else {
            adView.visibility = View.VISIBLE
        }
    }

    private fun showGameUI() {
        mainMenuLayout.visibility = View.GONE
        reactionButton.visibility = View.VISIBLE
        playAgainButton.visibility = View.GONE
        mainMenuButton.visibility = View.GONE
        endLogoImage.visibility = View.GONE
        rootLayout.setBackgroundColor(Color.parseColor("#08252d"))
    }

    // --- Billing Setup ---
    private fun setupBillingClient() {
        billingClient = BillingClient.newBuilder(this)
            .setListener(purchasesUpdatedListener)
            .enablePendingPurchases()
            .build()
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingServiceDisconnected() {
                // Try to restart connection if needed
            }
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    queryRemoveAdsProduct()
                    checkRemoveAdsPurchase()
                }
            }
        })
    }

    private fun queryRemoveAdsProduct() {
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(
                listOf(
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId("remove_ads") // Set this to your actual product ID in Play Console
                        .setProductType(BillingClient.ProductType.INAPP)
                        .build()
                )
            )
            .build()
        billingClient.queryProductDetailsAsync(params) { billingResult, productDetailsList ->
            runOnUiThread {
                if (prefs.getBoolean(REMOVE_ADS_PREF, false)) {
                    removeAdsButton.isEnabled = false
                    removeAdsButton.text = "Purchased"
                } else if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && productDetailsList.isNotEmpty()) {
                    removeAdsProductDetails = productDetailsList[0]
                    removeAdsButton.isEnabled = true
                } else {
                    // Enable button to allow retry if loading fails
                    removeAdsButton.isEnabled = true
                }
            }
        }
    }

    private val purchasesUpdatedListener = PurchasesUpdatedListener { billingResult, purchases ->
        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
            for (purchase in purchases) {
                if (purchase.products.contains("remove_ads")) {
                    handleRemoveAdsPurchase(purchase)
                }
            }
        }
    }

    private fun handleRemoveAdsPurchase(purchase: Purchase) {
        // Acknowledge purchase if needed
        if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED && !purchase.isAcknowledged) {
            billingClient.acknowledgePurchase(
                com.android.billingclient.api.AcknowledgePurchaseParams.newBuilder()
                    .setPurchaseToken(purchase.purchaseToken)
                    .build()
            ) { ackResult ->
                if (ackResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    prefs.edit().putBoolean(REMOVE_ADS_PREF, true).apply()
                    hideAdsForever()
                    runOnUiThread {
                        removeAdsButton.isEnabled = false
                        removeAdsButton.text = "Purchased"
                    }
                }
            }
        } else if (purchase.isAcknowledged) {
            prefs.edit().putBoolean(REMOVE_ADS_PREF, true).apply()
            hideAdsForever()
            runOnUiThread {
                removeAdsButton.isEnabled = false
                removeAdsButton.text = "Purchased"
            }
        }
    }

    private fun checkRemoveAdsPurchase() {
        billingClient.queryPurchasesAsync(BillingClient.SkuType.INAPP) { billingResult, purchases ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                for (purchase in purchases) {
                    if (purchase.products.contains("remove_ads")) {
                        handleRemoveAdsPurchase(purchase)
                    }
                }
            }
        }
    }

    private fun hideAdsForever() {
        val adView = findViewById<AdView>(R.id.adView)
        adView.visibility = View.GONE
        showMessage("Ads removed! Thank you for your support.")
    }

    private fun showMessage(msg: String) {
        android.widget.Toast.makeText(this, msg, android.widget.Toast.LENGTH_SHORT).show()
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
        interstitialAd = null
    }

    // --- Interstitial Ad Methods ---
    private fun loadInterstitialAd() {
        val adRequest = AdRequest.Builder().build()
        InterstitialAd.load(
            this,
            "ca-app-pub-4723028926463332/2262743746", // Real interstitial ad unit ID
            adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitialAd = ad
                    interstitialAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
                        override fun onAdDismissedFullScreenContent() {
                            loadInterstitialAd() // Preload next ad
                        }
                        override fun onAdFailedToShowFullScreenContent(adError: com.google.android.gms.ads.AdError) {
                            loadInterstitialAd()
                        }
                    }
                }
                override fun onAdFailedToLoad(adError: LoadAdError) {
                    interstitialAd = null
                }
            }
        )
    }

    private fun showInterstitialAd() {
        if (interstitialAd != null) {
            interstitialAd?.show(this)
        } else {
            loadInterstitialAd()
        }
    }
}