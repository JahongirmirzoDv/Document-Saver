package uz.mobiledv.test1

import android.app.Application
import android.content.Context
import org.koin.android.ext.koin.androidContext
import uz.mobiledv.test1.di.initKoin

class MyActivity: Application() {

    override fun onCreate() {
        super.onCreate()
        AppContextHolder.appContext = applicationContext

        initKoin {
            androidContext(this@MyActivity)
        }
    }

    object AppContextHolder {
        lateinit var appContext: Context

        fun isInitialized(): Boolean = ::appContext.isInitialized
    }
}