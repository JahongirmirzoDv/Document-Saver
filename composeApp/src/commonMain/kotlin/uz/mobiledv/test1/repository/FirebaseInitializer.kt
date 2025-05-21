package uz.mobiledv.test1.repository

// Common expect declaration for Firebase initialization
expect object FirebaseInitializer{
    fun initialize()
    fun getAppContext(): Any? // To pass context if needed, more relevant for Android
}