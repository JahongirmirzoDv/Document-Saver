package uz.mobiledv.test1.di

import org.koin.dsl.module
import uz.mobiledv.test1.AppViewModel
import uz.mobiledv.test1.screens.FoldersViewModel

val viewModelsModule = module {
    factory { AppViewModel(get()) }
    factory { FoldersViewModel(get()) }
}