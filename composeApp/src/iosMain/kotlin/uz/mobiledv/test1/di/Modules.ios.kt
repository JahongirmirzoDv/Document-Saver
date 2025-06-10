package uz.mobiledv.test1.di

import com.russhwolf.settings.NSUserDefaultsSettings
import com.russhwolf.settings.Settings
import org.koin.core.module.Module
import org.koin.dsl.module
import platform.Foundation.NSUserDefaults
import uz.mobiledv.test1.util.FileSaver

actual val platformModule: Module = module {
    single<Settings> {
        NSUserDefaultsSettings(NSUserDefaults.standardUserDefaults)
    }
}

actual val platformFileSaverModule: Module = module {
    single { FileSaver() }
}