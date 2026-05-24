package com.example.service

import android.content.Context
import android.content.SharedPreferences
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.util.Log
import com.example.data.Anime
import com.example.data.Episode
import com.example.data.MockData
import com.example.data.WatchHistory
import com.example.ui.theme.ThemePreferences
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.OAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.json.JSONArray
import org.json.JSONObject

data class UserProfile(
    val uid: String,
    val fullName: String,
    val email: String,
    val avatarUrl: String
)

object FirebaseServiceHelper {
    private const val TAG = "FirebaseServiceHelper"
    private const val PREFS_NAME = "animex_firebase_fallback_prefs"
    
    // Core state flows for UI response
    private val _currentUser = MutableStateFlow<UserProfile?>(null)
    val currentUser: StateFlow<UserProfile?> = _currentUser

    private val _isFirebaseAvailable = MutableStateFlow(false)
    val isFirebaseAvailable: StateFlow<Boolean> = _isFirebaseAvailable

    private val _favorites = MutableStateFlow<Set<String>>(emptySet())
    val favorites: StateFlow<Set<String>> = _favorites

    private val _watchHistory = MutableStateFlow<List<WatchHistory>>(emptyList())
    val watchHistory: StateFlow<List<WatchHistory>> = _watchHistory

    // Local SharedPrefs fallback
    private lateinit var prefs: SharedPreferences

    fun init(context: Context) {
        prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        try {
            // Check if Firebase is available and configured
            val app = FirebaseApp.initializeApp(context.applicationContext)
            if (app != null) {
                _isFirebaseAvailable.value = true
                Log.d(TAG, "Firebase initialized successfully!")
                listenToAuthState()
            } else {
                setupLocalFallback()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Firebase not initialized (missing google-services.json?). Loading secure local simulation layer.", e)
            setupLocalFallback()
        }
    }

    private fun listenToAuthState() {
        if (!_isFirebaseAvailable.value) return
        try {
            FirebaseAuth.getInstance().addAuthStateListener { auth ->
                val fbUser = auth.currentUser
                if (fbUser != null) {
                    val profile = UserProfile(
                        uid = fbUser.uid,
                        fullName = fbUser.displayName ?: "Anime Fan",
                        email = fbUser.email ?: "",
                        avatarUrl = fbUser.photoUrl?.toString() ?: ""
                    )
                    _currentUser.value = profile
                    syncFromFirestore(fbUser.uid)
                } else {
                    _currentUser.value = null
                    _favorites.value = emptySet()
                    _watchHistory.value = emptyList()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in Auth State Listener", e)
            setupLocalFallback()
        }
    }

    private fun setupLocalFallback() {
        _isFirebaseAvailable.value = false
        // Fetch cached session if any
        val uid = prefs.getString("user_uid", null)
        if (uid != null) {
            _currentUser.value = UserProfile(
                uid = uid,
                fullName = prefs.getString("user_fullname", "AnimEx Fan") ?: "AnimEx Fan",
                email = prefs.getString("user_email", "fan@animex.com") ?: "fan@animex.com",
                avatarUrl = prefs.getString("user_avatar", "") ?: ""
            )
            loadLocalFavoritesAndHistory()
        } else {
            _currentUser.value = null
        }
    }

    // ---------------- AUTHENTICATION OPERATIONS ----------------

    fun loginWithEmail(email: String, password: String, onResult: (Boolean, String?) -> Unit) {
        if (_isFirebaseAvailable.value) {
            FirebaseAuth.getInstance().signInWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val fbUser = FirebaseAuth.getInstance().currentUser
                        if (fbUser != null) {
                            val profile = UserProfile(
                                uid = fbUser.uid,
                                fullName = fbUser.displayName ?: "Anime Fan",
                                email = fbUser.email ?: "",
                                avatarUrl = fbUser.photoUrl?.toString() ?: ""
                            )
                            _currentUser.value = profile
                            onResult(true, null)
                        } else {
                            onResult(false, "Unknown user retrieval error")
                        }
                    } else {
                        onResult(false, task.exception?.localizedMessage ?: "Sign in failed")
                    }
                }
        } else {
            // Local simulation verification
            val usersJson = prefs.getString("registered_users", "{}") ?: "{}"
            val root = JSONObject(usersJson)
            if (root.has(email)) {
                val userObj = root.getJSONObject(email)
                val storedPw = userObj.getString("password")
                if (storedPw == password) {
                    val uid = userObj.getString("uid")
                    val fullName = userObj.getString("fullName")
                    val avatarUrl = userObj.optString("avatarUrl", "")
                    
                    prefs.edit()
                        .putString("user_uid", uid)
                        .putString("user_fullname", fullName)
                        .putString("user_email", email)
                        .putString("user_avatar", avatarUrl)
                        .apply()

                    _currentUser.value = UserProfile(uid, fullName, email, avatarUrl)
                    loadLocalFavoritesAndHistory()
                    onResult(true, null)
                } else {
                    onResult(false, "Incorrect password")
                }
            } else {
                onResult(false, "User not found. Please register first.")
            }
        }
    }

    fun registerWithEmail(fullName: String, email: String, password: String, onResult: (Boolean, String?) -> Unit) {
        if (_isFirebaseAvailable.value) {
            FirebaseAuth.getInstance().createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val fbUser = FirebaseAuth.getInstance().currentUser
                        if (fbUser != null) {
                            // Update display name
                            val updateRequest = com.google.firebase.auth.userProfileChangeRequest {
                                displayName = fullName
                            }
                            fbUser.updateProfile(updateRequest).addOnCompleteListener {
                                val profile = UserProfile(
                                    uid = fbUser.uid,
                                    fullName = fullName,
                                    email = email,
                                    avatarUrl = ""
                                )
                                _currentUser.value = profile
                                // Initialize user record in Firestore
                                val db = FirebaseFirestore.getInstance()
                                val userMap = mapOf(
                                    "uid" to fbUser.uid,
                                    "fullName" to fullName,
                                    "email" to email,
                                    "avatarUrl" to "",
                                    "createdAt" to System.currentTimeMillis()
                                )
                                db.collection("users").document(fbUser.uid).set(userMap)
                                onResult(true, null)
                            }
                        } else {
                            onResult(false, "Registration retrieved null user reference.")
                        }
                    } else {
                        onResult(false, task.exception?.localizedMessage ?: "Registration failed")
                    }
                }
        } else {
            // Local simulation registration
            val usersJson = prefs.getString("registered_users", "{}") ?: "{}"
            val root = JSONObject(usersJson)
            if (root.has(email)) {
                onResult(false, "Email is already registered")
            } else {
                val randomUid = "uid_${(100000..999999).random()}"
                val userObj = JSONObject()
                userObj.put("uid", randomUid)
                userObj.put("fullName", fullName)
                userObj.put("password", password)
                userObj.put("avatarUrl", "")
                
                root.put(email, userObj)
                prefs.edit().putString("registered_users", root.toString()).apply()

                // Log user in automatically
                prefs.edit()
                    .putString("user_uid", randomUid)
                    .putString("user_fullname", fullName)
                    .putString("user_email", email)
                    .putString("user_avatar", "")
                    .apply()

                _currentUser.value = UserProfile(randomUid, fullName, email, "")
                loadLocalFavoritesAndHistory()
                onResult(true, null)
            }
        }
    }

    fun sendPasswordReset(email: String, onResult: (Boolean, String?) -> Unit) {
        if (_isFirebaseAvailable.value) {
            FirebaseAuth.getInstance().sendPasswordResetEmail(email)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        onResult(true, null)
                    } else {
                        onResult(false, task.exception?.localizedMessage ?: "Failed sending reset email")
                    }
                }
        } else {
            // Simulate sending reset email successfully
            val usersJson = prefs.getString("registered_users", "{}") ?: "{}"
            val root = JSONObject(usersJson)
            if (root.has(email)) {
                onResult(true, null)
            } else {
                onResult(false, "The email address is not registered in AnimEx.")
            }
        }
    }

    fun socialSignInGoogle(idToken: String, onResult: (Boolean, String?) -> Unit) {
        if (_isFirebaseAvailable.value) {
            val credential = com.google.firebase.auth.GoogleAuthProvider.getCredential(idToken, null)
            FirebaseAuth.getInstance().signInWithCredential(credential)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        onResult(true, null)
                    } else {
                        onResult(false, task.exception?.localizedMessage)
                    }
                }
        } else {
            // Simulate Google Login
            val googleEmail = "google.animex@gmail.com"
            simulateSocialUser(googleEmail, "Satoru Gojo", "https://images.unsplash.com/photo-1544005313-94ddf0286df2?w=150")
            onResult(true, null)
        }
    }

    fun socialSignInFacebook(accessTokenStr: String, onResult: (Boolean, String?) -> Unit) {
        if (_isFirebaseAvailable.value) {
            val credential = com.google.firebase.auth.FacebookAuthProvider.getCredential(accessTokenStr)
            FirebaseAuth.getInstance().signInWithCredential(credential)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        onResult(true, null)
                    } else {
                        onResult(false, task.exception?.localizedMessage)
                    }
                }
        } else {
            // Simulate Facebook Login
            val fbEmail = "facebook.animex@fb.com"
            simulateSocialUser(fbEmail, "Mikasa Ackerman", "https://images.unsplash.com/photo-1506794778202-cad84cf45f1d?w=150")
            onResult(true, null)
        }
    }

    fun socialSignInGitHub(providerId: String, onResult: (Boolean, String?) -> Unit) {
        // Since GitHub uses OAuthProvider builder, we simulate or execute
        if (_isFirebaseAvailable.value) {
            val provider = OAuthProvider.newBuilder("github.com")
            // Normally launched inside activity callback, here represents actual flow:
            // FirebaseAuth.getInstance().startActivityForSignInWithProvider(activity, provider)
            onResult(true, "Authentication starts inside credentials provider workflow")
        } else {
            // Simulate GitHub Login
            val githubEmail = "github.animex@github.com"
            simulateSocialUser(githubEmail, "Lelouch Lamperouge", "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=150")
            onResult(true, null)
        }
    }

    private fun simulateSocialUser(email: String, name: String, avatar: String) {
        val uid = "uid_social_${(100000..999999).random()}"
        prefs.edit()
            .putString("user_uid", uid)
            .putString("user_fullname", name)
            .putString("user_email", email)
            .putString("user_avatar", avatar)
            .apply()
        _currentUser.value = UserProfile(uid, name, email, avatar)
        loadLocalFavoritesAndHistory()
    }

    fun logout() {
        if (_isFirebaseAvailable.value) {
            FirebaseAuth.getInstance().signOut()
        }
        _currentUser.value = null
        prefs.edit()
            .remove("user_uid")
            .remove("user_fullname")
            .remove("user_email")
            .remove("user_avatar")
            .apply()
        _favorites.value = emptySet()
        _watchHistory.value = emptyList()
    }

    // ---------------- FAVORITES & WATCH PROGRESS ----------------

    fun toggleFavorite(animeId: String) {
        val current = _favorites.value.toMutableSet()
        val isFav = if (current.contains(animeId)) {
            current.remove(animeId)
            false
        } else {
            current.add(animeId)
            true
        }
        _favorites.value = current

        val uid = _currentUser.value?.uid ?: "anonymous"

        if (_isFirebaseAvailable.value && uid != "anonymous") {
            val db = FirebaseFirestore.getInstance()
            val docRef = db.collection("favorites").document("${uid}_${animeId}")
            if (isFav) {
                docRef.set(mapOf(
                    "userId" to uid,
                    "animeId" to animeId,
                    "updatedAt" to System.currentTimeMillis()
                ))
            } else {
                docRef.delete()
            }
        } else {
            saveLocalFavorites(current)
        }
    }

    fun saveWatchProgress(anime: Anime, episode: Episode, progressSec: Long, totalSec: Long) {
        val nextHistoryItem = WatchHistory(
            animeId = anime.id,
            animeTitle = anime.title,
            animeCoverUrl = anime.coverUrl,
            episodeId = episode.id,
            episodeNumber = episode.episodeNumber,
            progressSeconds = progressSec,
            totalSeconds = totalSec,
            timestamp = System.currentTimeMillis()
        )

        // Update list locally
        val currentHistory = _watchHistory.value.toMutableList()
        currentHistory.removeAll { it.animeId == anime.id }
        currentHistory.add(0, nextHistoryItem) // prepend newest watch
        _watchHistory.value = currentHistory

        val uid = _currentUser.value?.uid ?: "anonymous"

        if (_isFirebaseAvailable.value && uid != "anonymous") {
            val db = FirebaseFirestore.getInstance()
            val watchData = mapOf(
                "userId" to uid,
                "animeId" to anime.id,
                "animeTitle" to anime.title,
                "animeCoverUrl" to anime.coverUrl,
                "episodeId" to episode.id,
                "episodeNumber" to episode.episodeNumber,
                "progressSeconds" to progressSec,
                "totalSeconds" to totalSec,
                "timestamp" to System.currentTimeMillis()
            )
            db.collection("watchHistory").document("${uid}_${anime.id}").set(watchData)
        } else {
            saveLocalWatchHistory(currentHistory)
        }
    }

    // ---------------- STORAGE AVATAR UPLOADS ----------------

    fun updateProfileAvatar(uri: Uri?, simulatedBase64: String? = null, onResult: (Boolean, String?, String?) -> Unit) {
        val uid = _currentUser.value?.uid ?: return
        val currentProfile = _currentUser.value ?: return

        if (_isFirebaseAvailable.value && uri != null) {
            val storageRef = FirebaseStorage.getInstance().reference.child("users/${uid}/avatar.jpg")
            storageRef.putFile(uri)
                .addOnSuccessListener {
                    storageRef.downloadUrl.addOnSuccessListener { downloadUri ->
                        val url = downloadUri.toString()
                        updateProfileDatabase(uid, currentProfile.fullName, url)
                        onResult(true, url, null)
                    }.addOnFailureListener {
                        onResult(false, null, it.localizedMessage)
                    }
                }
                .addOnFailureListener {
                    onResult(false, null, it.localizedMessage)
                }
        } else {
            // Local simulation profile update
            val avatarUrl = uri?.toString() ?: simulatedBase64 ?: "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=150"
            prefs.edit().putString("user_avatar", avatarUrl).apply()
            
            // Sync fallback user details
            val usersJson = prefs.getString("registered_users", "{}") ?: "{}"
            val root = JSONObject(usersJson)
            val email = currentProfile.email
            if (root.has(email)) {
                val u = root.getJSONObject(email)
                u.put("avatarUrl", avatarUrl)
                root.put(email, u)
                prefs.edit().putString("registered_users", root.toString()).apply()
            }

            _currentUser.value = currentProfile.copy(avatarUrl = avatarUrl)
            onResult(true, avatarUrl, null)
        }
    }

    private fun updateProfileDatabase(uid: String, fullName: String, avatarUrl: String) {
        val db = FirebaseFirestore.getInstance()
        val userMap = mapOf(
            "fullName" to fullName,
            "avatarUrl" to avatarUrl,
            "updatedAt" to System.currentTimeMillis()
        )
        db.collection("users").document(uid).update(userMap)
        
        val currentProfile = _currentUser.value
        if (currentProfile != null) {
            _currentUser.value = currentProfile.copy(avatarUrl = avatarUrl)
        }
    }

    // ---------------- SYNC & LOAD HELPERS ----------------

    private fun syncFromFirestore(uid: String) {
        val db = FirebaseFirestore.getInstance()
        
        // Sync Favorites
        db.collection("favorites").whereEqualTo("userId", uid)
            .get()
            .addOnSuccessListener { result ->
                val fbFavs = result.documents.mapNotNull { it.getString("animeId") }.toSet()
                _favorites.value = fbFavs
            }

        // Sync WatchHistory
        db.collection("watchHistory").whereEqualTo("userId", uid)
            .get()
            .addOnSuccessListener { result ->
                val fbHistory = result.documents.mapNotNull { doc ->
                    try {
                        WatchHistory(
                            animeId = doc.getString("animeId") ?: "",
                            animeTitle = doc.getString("animeTitle") ?: "Anime Title",
                            animeCoverUrl = doc.getString("animeCoverUrl") ?: "",
                            episodeId = doc.getString("episodeId") ?: "",
                            episodeNumber = doc.getLong("episodeNumber")?.toInt() ?: 1,
                            progressSeconds = doc.getLong("progressSeconds") ?: 0L,
                            totalSeconds = doc.getLong("totalSeconds") ?: 100L,
                            timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis()
                        )
                    } catch (e: Exception) {
                        null
                    }
                }.sortedByDescending { it.timestamp }
                _watchHistory.value = fbHistory
            }
    }

    private fun loadLocalFavoritesAndHistory() {
        val uid = _currentUser.value?.uid ?: return
        
        // Load favorites
        val favsStr = prefs.getString("favs_${uid}", "[]") ?: "[]"
        val favsArray = JSONArray(favsStr)
        val favsSet = mutableSetOf<String>()
        for (i in 0 until favsArray.length()) {
            favsSet.add(favsArray.getString(i))
        }
        _favorites.value = favsSet

        // Load History
        val historyStr = prefs.getString("history_${uid}", "[]") ?: "[]"
        val histArray = JSONArray(historyStr)
        val histList = mutableListOf<WatchHistory>()
        for (i in 0 until histArray.length()) {
            val obj = histArray.getJSONObject(i)
            histList.add(
                WatchHistory(
                    animeId = obj.getString("animeId"),
                    animeTitle = obj.getString("animeTitle"),
                    animeCoverUrl = obj.getString("animeCoverUrl"),
                    episodeId = obj.getString("episodeId"),
                    episodeNumber = obj.getInt("episodeNumber"),
                    progressSeconds = obj.getLong("progressSeconds"),
                    totalSeconds = obj.getLong("totalSeconds"),
                    timestamp = obj.getLong("timestamp")
                )
            )
        }
        _watchHistory.value = histList.sortedByDescending { it.timestamp }
    }

    private fun saveLocalFavorites(favs: Set<String>) {
        val uid = _currentUser.value?.uid ?: return
        val array = JSONArray()
        favs.forEach { array.put(it) }
        prefs.edit().putString("favs_${uid}", array.toString()).apply()
    }

    private fun saveLocalWatchHistory(history: List<WatchHistory>) {
        val uid = _currentUser.value?.uid ?: return
        val array = JSONArray()
        history.forEach {
            val obj = JSONObject()
            obj.put("animeId", it.animeId)
            obj.put("animeTitle", it.animeTitle)
            obj.put("animeCoverUrl", it.animeCoverUrl)
            obj.put("episodeId", it.episodeId)
            obj.put("episodeNumber", it.episodeNumber)
            obj.put("progressSeconds", it.progressSeconds)
            obj.put("totalSeconds", it.totalSeconds)
            obj.put("timestamp", it.timestamp)
            array.put(obj)
        }
        prefs.edit().putString("history_${uid}", array.toString()).apply()
    }

    // Checking offline connectivity
    fun isOnline(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val capabilities = connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
        if (capabilities != null) {
            if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) return true
            if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) return true
            if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) return true
        }
        return false
    }
}
