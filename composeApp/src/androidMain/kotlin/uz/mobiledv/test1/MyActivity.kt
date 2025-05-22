package uz.mobiledv.test1

import android.app.Application
import org.koin.android.ext.koin.androidContext
import uz.mobiledv.test1.appwrite.AppContextHolder
import uz.mobiledv.test1.di.initKoin

class MyActivity: Application() {

    override fun onCreate() {
        super.onCreate()
        initKoin {
            androidContext(this@MyActivity)
        }


        AppContextHolder.context = applicationContext
    }
}