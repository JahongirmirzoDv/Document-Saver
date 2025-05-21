package uz.mobiledv.test1.di

import org.koin.core.module.Module
import org.koin.dsl.module
import uz.mobiledv.test1.repository.DocumentRepository
import uz.mobiledv.test1.repository.DocumentRepositoryImpl
import uz.mobiledv.test1.repository.FolderRepository
import uz.mobiledv.test1.repository.FolderRepositoryImpl
import uz.mobiledv.test1.repository.UserRepository
import uz.mobiledv.test1.repository.UserRepositoryImpl

expect val platformModule: Module

val sharedModule = module {
//    singleOf(::MyRepositoryImpl).bind<MyRepository>()
//    viewModelOf(::MyViewModel)
    single<UserRepository> { UserRepositoryImpl() }
    single<FolderRepository> { FolderRepositoryImpl() }
    single<DocumentRepository> { DocumentRepositoryImpl() }
}