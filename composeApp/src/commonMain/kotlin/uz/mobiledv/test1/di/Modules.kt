package uz.mobiledv.test1.di

import io.appwrite.services.Account
import io.appwrite.services.Databases
import io.appwrite.services.Storage
import org.koin.core.module.Module
import org.koin.dsl.module
import uz.mobiledv.test1.appwrite.AppwriteInstance
import uz.mobiledv.test1.appwrite.KtorClientInstance
import uz.mobiledv.test1.repository.DocumentRepository
import uz.mobiledv.test1.repository.DocumentRepositoryImpl
import uz.mobiledv.test1.repository.FolderRepository
import uz.mobiledv.test1.repository.FolderRepositoryImpl
import uz.mobiledv.test1.screens.LoginViewModel

expect val platformModule: Module

val viewModelModule = module {
    // For AndroidX ViewModel (if koin-androidx-viewmodel is setup for commonMain, or via expect/actual)
    // viewModel { LoginViewModel(get()) }
    // viewModel { FoldersViewModel(get()) }
    // viewModel { DocumentsViewModel(get(), get()) } // If it takes two repositories

    // If LoginViewModel is a plain Kotlin class (not AndroidX ViewModel)
    // or using a KMP ViewModel library without specific Koin extensions:
    factory { LoginViewModel(get()) } // Use factory for ViewModels usually
//    factory { FoldersViewModel(get()) } // Example
//    factory { DocumentsViewModel(get(), get()) } // Example
}

val sharedModule = module {
    single { AppwriteInstance.client } // Provide Appwrite Client
    single { Account(get()) }         // Provide Appwrite Account service
    single { Databases(get()) } // Provide Appwrite Databases service
    single { Storage(get()) }          // Provide Appwrite Storage service
    single { KtorClientInstance.httpClient } // Provide Ktor HttpClient

    //single<UserRepository> { UserRepositoryImpl(get(), get()) } // Pass Account and Ktor client
    single<FolderRepository> { FolderRepositoryImpl(get()) } // Pass Databases
    single<DocumentRepository> {
        DocumentRepositoryImpl(get(), get(), get(), get(), get(), get())
    } // Pass Databases, Storage, Ktor client
}