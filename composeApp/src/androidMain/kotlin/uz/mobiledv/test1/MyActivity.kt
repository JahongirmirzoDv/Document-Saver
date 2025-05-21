package uz.mobiledv.test1

import android.app.Application
import com.google.firebase.FirebaseApp
import org.koin.android.ext.koin.androidContext
import uz.mobiledv.test1.di.initKoin
import uz.mobiledv.test1.repository.FirebaseInitializer

class MyActivity: Application() {

    override fun onCreate() {
        super.onCreate()
        initKoin {
            androidContext(this@MyActivity)
        }
        if (FirebaseApp.getApps(this).isEmpty()) {
            FirebaseApp.initializeApp(this)
            println("Native FirebaseApp Initialized on Android (MainApplication).")
        }


        // Provide context to your shared FirebaseInitializer
        FirebaseInitializer.AppContextHolder.context = applicationContext
        // Initialize GitLive KMP Firebase wrapper
        FirebaseInitializer.initialize()
    }
}