package uz.mobiledv.test1.repository

import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.FirebaseOptions
import dev.gitlive.firebase.initialize

actual object FirebaseInitializer {
    private var appInitialized = false

    actual fun initialize() {
        if (!appInitialized) {
            try {
                // For Desktop, you typically need to provide FirebaseOptions explicitly.
                // These details come from your Firebase project settings (Web app config).
                // Go to Firebase Console -> Project Settings -> General tab -> Your apps -> Add app -> Web (</>)
                // Copy the config values.
                val options = FirebaseOptions(
                    applicationId = "YOUR_WEB_APP_ID_OR_ANY_UNIQUE_ID_FOR_DESKTOP", // e.g., "1:1234567890:web:abcdef123456" or a custom one like "my-desktop-app"
                    apiKey = "YOUR_WEB_API_KEY", // Found in Firebase console config
                    projectId = "YOUR_FIREBASE_PROJECT_ID", // Found in Firebase console config
                    // Optional, if you use Realtime Database or Storage and want to specify them:
                    // databaseUrl = "https://YOUR_PROJECT_ID.firebaseio.com",
                    // storageBucket = "YOUR_PROJECT_ID.appspot.com"
                )
                Firebase.initialize(options = options) // Initialize GitLive Firebase with options for JVM
                println("GitLive Firebase KMP Initialized on Desktop.")
                appInitialized = true
            } catch (e: IllegalStateException) {
                // Firebase may have already been initialized by some other means or a previous call
                println("GitLive Firebase KMP on Desktop might already be initialized: ${e.message}")
                appInitialized = true // Assume it's usable
            } catch (e: Exception) {
                println("Error initializing GitLive Firebase KMP on Desktop: ${e.message} ${e.cause}")
                // Handle error: your app might not function correctly without Firebase.
            }
        } else {
            println("GitLive Firebase KMP already initialized on Desktop.")
        }
    }

    actual fun getAppContext(): Any? {
        return null // No specific "context" like Android for desktop in this setup
    }
}