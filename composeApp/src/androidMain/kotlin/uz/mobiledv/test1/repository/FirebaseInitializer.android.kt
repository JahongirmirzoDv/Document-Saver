package uz.mobiledv.test1.repository

import android.content.Context
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.initialize

actual object FirebaseInitializer {
    private var appInitialized = false

    // Simple object to hold Android Context.
    // Initialize this from your Application class.
    object AppContextHolder {
        lateinit var context: Context

        fun isContextInitialized(): Boolean {
            return ::context.isInitialized // This works because it's within the same object
        }
    }

    actual fun initialize() {
        if (!appInitialized && AppContextHolder.isContextInitialized()) {
            try {
                // The primary FirebaseApp initialization for Android is usually handled by
                // the google-services plugin automatically or by calling
                // com.google.firebase.FirebaseApp.initializeApp(context)
                // GitLive's Firebase.initialize is for its KMP wrapper.
                Firebase.initialize(AppContextHolder.context) // Initialize GitLive Firebase with Android context
                println("GitLive Firebase KMP Initialized on Android.")
                appInitialized = true
            } catch (e: Exception) {
                // This can happen if com.google.firebase.FirebaseApp.initializeApp is not called
                // or if there's an issue with google-services.json
                println("Error initializing GitLive Firebase KMP on Android: ${e.message}. Make sure native Firebase is initialized.")
                // You might want to ensure native Firebase is initialized first:
                // if (com.google.firebase.FirebaseApp.getApps(AppContextHolder.context).isEmpty()) {
                //     com.google.firebase.FirebaseApp.initializeApp(AppContextHolder.context)
                //     println("Native FirebaseApp initialized on Android.")
                // }
                // Firebase.initialize(AppContextHolder.context) // Retry GitLive init
            }
        } else if (appInitialized) {
            println("GitLive Firebase KMP already initialized on Android.")
        } else {
            println("GitLive Firebase KMP Android not initialized: Context not available.")
        }
    }

    actual fun getAppContext(): Any? {
        return if (AppContextHolder.isContextInitialized()) AppContextHolder.context else null
    }
}