package uz.mobiledv.test1.di

import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.module
import uz.mobiledv.test1.AppViewModel
import uz.mobiledv.test1.screens.FoldersViewModel

val viewModelsModule = module {
    //factoryOf(::AppViewModel)      // Was: factory { AppViewModel(get()) }
    //factoryOf(::FoldersViewModel)
    factory { FoldersViewModel(get(),get()) }
    factory { AppViewModel(get()) }
    factory { uz.mobiledv.test1.screens.SimpleViewModel() }
}