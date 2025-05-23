package uz.mobiledv.test1

import android.app.Application
import android.content.Context
import org.koin.android.ext.koin.androidContext
import uz.mobiledv.test1.di.initKoin

class MyActivity: Application() {

    object AppContextHolder {
        lateinit var appContext: Context // Renamed for clarity

        fun isInitialized(): Boolean {
            return ::appContext.isInitialized
        }
    }


    override fun onCreate() {
        super.onCreate()
        initKoin {
            androidContext(this@MyActivity)
        }


        AppContextHolder.appContext = applicationContext
    }
}