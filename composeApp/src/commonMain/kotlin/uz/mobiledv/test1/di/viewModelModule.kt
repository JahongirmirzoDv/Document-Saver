// Located in: jahongirmirzodv/test.1.2/Test.1.2-e8bc22d6ec882d29fdc4fa507b210d7398d64cde/composeApp/src/commonMain/kotlin/uz/mobiledv/test1/di/viewModelModule.kt
package uz.mobiledv.test1.di

// import org.koin.core.module.dsl.factoryOf // Not used here
import org.koin.dsl.module
import uz.mobiledv.test1.AppViewModel
import uz.mobiledv.test1.repository.AppUpdateViewModel
import uz.mobiledv.test1.screens.FoldersViewModel
import uz.mobiledv.test1.screens.SimpleViewModel // Ensure this is used or remove

val viewModelsModule = module {
    factory { AppViewModel(get(), get()) } // AppViewModel now needs SupabaseClient and AuthSettings
    factory { FoldersViewModel(get(), get(), get()) } // FoldersViewModel needs SupabaseClient, FileSaver, AuthSettings
    factory { SimpleViewModel() } // Assuming SimpleViewModel has no dependencies or uses get()

    factory { AppUpdateViewModel(get(),get()) }
}